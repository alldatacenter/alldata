/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.VersionMismatchException;
import org.apache.drill.exec.expr.fn.registry.RemoteFunctionRegistry;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.parser.SqlDropFunction;
import org.apache.drill.exec.proto.UserBitShared.Jar;
import org.apache.drill.exec.proto.UserBitShared.Registry;
import org.apache.drill.exec.store.sys.store.DataChangeVersion;
import org.apache.drill.exec.util.JarUtil;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class DropFunctionHandler extends DefaultSqlHandler {
  private static Logger logger = LoggerFactory.getLogger(DropFunctionHandler.class);

  public DropFunctionHandler(SqlHandlerConfig config) {
    super(config);
  }

  /**
   * Unregisters UDFs dynamically. Process consists of several steps:
   * <ol>
   * <li>Registering jar in jar registry to ensure that several jars with the same name is not being unregistered.</li>
   * <li>Starts remote unregistration process, gets list of all jars and excludes jar to be deleted.</li>
   * <li>Signals drill bits to start local unregistration process.</li>
   * <li>Removes source and binary jars from registry area.</li>
   * </ol>
   *
   * UDFs unregistration is allowed only if dynamic UDFs support is enabled.
   * Only jars registered dynamically can be unregistered,
   * built-in functions loaded at start up are not allowed to be unregistered.
   *
   * Limitation: before jar unregistration make sure no one is using functions from this jar.
   * There is no guarantee that running queries will finish successfully or give correct result.
   *
   * @return - Single row indicating list of unregistered UDFs, raise exception otherwise
   */
  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException, IOException {
    if (!context.getOption(ExecConstants.DYNAMIC_UDF_SUPPORT_ENABLED).bool_val) {
      throw UserException.validationError()
          .message("Dynamic UDFs support is disabled.")
          .build(logger);
    }

    SqlDropFunction node = unwrap(sqlNode, SqlDropFunction.class);
    String jarName = ((SqlCharStringLiteral) node.getJar()).toValue();
    RemoteFunctionRegistry remoteFunctionRegistry = context.getRemoteFunctionRegistry();

    boolean inProgress = false;
    try {
      final String action = remoteFunctionRegistry.addToJars(jarName, RemoteFunctionRegistry.Action.UNREGISTRATION);
      if (!(inProgress = action == null)) {
        return DirectPlan.createDirectPlan(context, false, String.format("Jar with %s name is used. Action: %s", jarName, action));
      }

      Jar deletedJar = unregister(jarName, remoteFunctionRegistry);
      if (deletedJar == null) {
        return DirectPlan.createDirectPlan(context, false, String.format("Jar %s is not registered in remote registry", jarName));
      }
      remoteFunctionRegistry.submitForUnregistration(jarName);

      removeJarFromArea(jarName, remoteFunctionRegistry.getFs(), remoteFunctionRegistry.getRegistryArea());
      removeJarFromArea(JarUtil.getSourceName(jarName), remoteFunctionRegistry.getFs(), remoteFunctionRegistry.getRegistryArea());

      return DirectPlan.createDirectPlan(context, true,
          String.format("The following UDFs in jar %s have been unregistered:\n%s", jarName, deletedJar.getFunctionSignatureList()));

    } catch (Exception e) {
      logger.error("Error during UDF unregistration", e);
      return DirectPlan.createDirectPlan(context, false, e.getMessage());
    } finally {
      if (inProgress) {
        remoteFunctionRegistry.finishUnregistration(jarName);
        remoteFunctionRegistry.removeFromJars(jarName);
      }
    }
  }

  /**
   * Gets remote function registry with version.
   * Version is used to ensure that we update the same registry we removed jars from.
   * Looks for a jar to be deleted, if founds one,
   * attempts to update remote registry with list of jars, that excludes jar to be deleted.
   * If during update {@link VersionMismatchException} was detected,
   * attempts to repeat unregistration process till retry attempts exceeds the limit.
   * If retry attempts number hits 0, throws exception that failed to update remote function registry.
   *
   * @param jarName jar name
   * @param remoteFunctionRegistry remote function registry
   * @return jar that was unregistered, null otherwise
   */
  private Jar unregister(String jarName, RemoteFunctionRegistry remoteFunctionRegistry) {
    int retryAttempts = remoteFunctionRegistry.getRetryAttempts();
    while (retryAttempts >= 0) {
      DataChangeVersion version = new DataChangeVersion();
      Registry registry = remoteFunctionRegistry.getRegistry(version);
      Jar jarToBeDeleted = null;
      List<Jar> jars = Lists.newArrayList();
      for (Jar j : registry.getJarList()) {
        if (j.getName().equals(jarName)) {
          jarToBeDeleted = j;
        } else {
          jars.add(j);
        }
      }
      if (jarToBeDeleted == null) {
        return null;
      }
      Registry updatedRegistry = Registry.newBuilder().addAllJar(jars).build();
      try {
        remoteFunctionRegistry.updateRegistry(updatedRegistry, version);
        return jarToBeDeleted;
      } catch (VersionMismatchException ex) {
        logger.debug("Failed to update function registry during unregistration, version mismatch was detected.", ex);
        retryAttempts--;
      }
    }
    throw new DrillRuntimeException("Failed to update remote function registry. Exceeded retry attempts limit.");
  }

  /**
   * Removes jar from indicated area, in case of error log it and proceeds.
   *
   * @param jarName jar name
   * @param fs file system
   * @param area path to area
   */
  private void removeJarFromArea(String jarName, FileSystem fs, Path area) {
    try {
      fs.delete(new Path(area, jarName), false);
    } catch (IOException e) {
      logger.error("Error removing jar {} from area {}", jarName, area.toUri().getPath());
    }
  }
}
