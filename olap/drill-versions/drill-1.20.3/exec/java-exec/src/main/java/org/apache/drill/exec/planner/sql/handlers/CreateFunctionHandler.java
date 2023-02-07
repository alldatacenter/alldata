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
import org.apache.commons.io.FileUtils;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.FunctionValidationException;
import org.apache.drill.exec.exception.JarValidationException;
import org.apache.drill.exec.exception.VersionMismatchException;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.registry.RemoteFunctionRegistry;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.parser.SqlCreateFunction;
import org.apache.drill.exec.proto.UserBitShared.Jar;
import org.apache.drill.exec.proto.UserBitShared.Registry;
import org.apache.drill.exec.store.sys.store.DataChangeVersion;
import org.apache.drill.exec.util.JarUtil;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class CreateFunctionHandler extends DefaultSqlHandler {
  private static Logger logger = LoggerFactory.getLogger(CreateFunctionHandler.class);

  public CreateFunctionHandler(SqlHandlerConfig config) {
    super(config);
  }

  /**
   * Registers UDFs dynamically. Process consists of several steps:
   * <ol>
   * <li>Registering jar in jar registry to ensure that several jars with the same name is not registered.</li>
   * <li>Binary and source jars validation and back up.</li>
   * <li>Validation against local function registry.</li>
   * <li>Validation against remote function registry.</li>
   * <li>Remote function registry update.</li>
   * <li>Copying of jars to registry area and clean up.</li>
   * </ol>
   *
   * UDFs registration is allowed only if dynamic UDFs support is enabled.
   *
   * @return - Single row indicating list of registered UDFs, or error message otherwise.
   */
  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException, IOException {
    if (!context.getOption(ExecConstants.DYNAMIC_UDF_SUPPORT_ENABLED).bool_val) {
      throw UserException.validationError()
          .message("Dynamic UDFs support is disabled.")
          .build(logger);
    }
    RemoteFunctionRegistry remoteRegistry = context.getRemoteFunctionRegistry();
    JarManager jarManager = new JarManager(sqlNode, remoteRegistry);

    boolean inProgress = false;
    try {
      final String action = remoteRegistry.addToJars(jarManager.getBinaryName(), RemoteFunctionRegistry.Action.REGISTRATION);
      if (!(inProgress = action == null)) {
        return DirectPlan.createDirectPlan(context, false,
            String.format("Jar with %s name is used. Action: %s", jarManager.getBinaryName(), action));
      }

      jarManager.initRemoteBackup();
      List<String> functions = validateAgainstLocalRegistry(jarManager, context.getFunctionRegistry());
      initRemoteRegistration(functions, jarManager, remoteRegistry);
      jarManager.deleteQuietlyFromStagingArea();

      return DirectPlan.createDirectPlan(context, true,
          String.format("The following UDFs in jar %s have been registered:\n%s", jarManager.getBinaryName(), functions));

    } catch (Exception e) {
      logger.error("Error during UDF registration", e);
      return DirectPlan.createDirectPlan(context, false, e.getMessage());
    } finally {
      if (inProgress) {
        remoteRegistry.removeFromJars(jarManager.getBinaryName());
      }
      jarManager.cleanUp();
    }
  }

  /**
   * Instantiates coping of binary to local file system
   * and validates functions from this jar against local function registry.
   *
   * @param jarManager helps coping binary to local file system
   * @param localFunctionRegistry instance of local function registry to instantiate local validation
   * @return list of validated function signatures
   * @throws IOException in case of problems during copying binary to local file system
   * @throws FunctionValidationException in case duplicated function was found
   */
  private List<String> validateAgainstLocalRegistry(JarManager jarManager,
                                                    FunctionImplementationRegistry localFunctionRegistry) throws IOException {
    Path localBinary = jarManager.copyBinaryToLocal();
    return localFunctionRegistry.validate(localBinary);
  }

  /**
   * Validates jar and its functions against remote jars.
   * First checks if there is no duplicate by jar name and then looks for duplicates among functions.
   *
   * @param remoteJars list of remote jars to validate against
   * @param jarName jar name to be validated
   * @param functions list of functions present in jar to be validated
   * @throws JarValidationException in case of jar with the same name was found
   * @throws FunctionValidationException in case duplicated function was found
   */
  private void validateAgainstRemoteRegistry(List<Jar> remoteJars, String jarName, List<String> functions) {
    for (Jar remoteJar : remoteJars) {
      if (remoteJar.getName().equals(jarName)) {
        throw new JarValidationException(String.format("Jar with %s name has been already registered", jarName));
      }
      for (String remoteFunction : remoteJar.getFunctionSignatureList()) {
        for (String func : functions) {
          if (remoteFunction.equals(func)) {
            throw new FunctionValidationException(
                String.format("Found duplicated function in %s: %s", remoteJar.getName(), remoteFunction));
          }
        }
      }
    }
  }

  /**
   * Instantiates remote registration. First gets remote function registry with version.
   * Version is used to ensure that we update the same registry we validated against.
   * Then validates against list of remote jars.
   * If validation is successful, first copies jars to registry area and starts updating remote function registry.
   * If during update {@link VersionMismatchException} was detected,
   * attempts to repeat remote registration process till retry attempts exceeds the limit.
   * If retry attempts number hits 0, throws exception that failed to update remote function registry.
   * In case of any error, if jars have been already copied to registry area, they will be deleted.
   *
   * @param functions list of functions present in jar
   * @param jarManager helper class for copying jars to registry area
   * @param remoteRegistry remote function registry
   * @throws IOException in case of problems with copying jars to registry area
   */
  private void initRemoteRegistration(List<String> functions,
                  JarManager jarManager,
                  RemoteFunctionRegistry remoteRegistry) throws IOException {
    int retryAttempts = remoteRegistry.getRetryAttempts();
    boolean copyJars = true;
    try {
      while (retryAttempts >= 0) {
        DataChangeVersion version = new DataChangeVersion();
        List<Jar> remoteJars = remoteRegistry.getRegistry(version).getJarList();
        validateAgainstRemoteRegistry(remoteJars, jarManager.getBinaryName(), functions);
        if (copyJars) {
          jarManager.copyToRegistryArea();
          copyJars = false;
        }
        List<Jar> jars = Lists.newArrayList(remoteJars);
        jars.add(Jar.newBuilder().setName(jarManager.getBinaryName()).addAllFunctionSignature(functions).build());
        Registry updatedRegistry = Registry.newBuilder().addAllJar(jars).build();
        try {
          remoteRegistry.updateRegistry(updatedRegistry, version);
          return;
        } catch (VersionMismatchException ex) {
          logger.debug("Failed to update function registry during registration, version mismatch was detected.", ex);
          retryAttempts--;
        }
      }
      throw new DrillRuntimeException("Failed to update remote function registry. Exceeded retry attempts limit.");
    } catch (Exception e) {
      if (!copyJars) {
        jarManager.deleteQuietlyFromRegistryArea();
      }
      throw e;
    }
  }

  /**
   * Inner helper class that encapsulates logic for working with jars.
   * During initialization it creates path to staging jar, local and remote temporary jars, registry jars.
   * Is responsible for validation, copying and deletion actions.
   */
  private class JarManager {

    private final String binaryName;
    private final FileSystem fs;

    private final Path remoteTmpDir;
    private final Path localTmpDir;

    private final Path stagingBinary;
    private final Path stagingSource;

    private final Path tmpRemoteBinary;
    private final Path tmpRemoteSource;

    private final Path registryBinary;
    private final Path registrySource;

    JarManager(SqlNode sqlNode, RemoteFunctionRegistry remoteRegistry) throws ForemanSetupException {
      SqlCreateFunction node = unwrap(sqlNode, SqlCreateFunction.class);
      this.binaryName = ((SqlCharStringLiteral) node.getJar()).toValue();
      String sourceName = JarUtil.getSourceName(binaryName);

      this.stagingBinary = new Path(remoteRegistry.getStagingArea(), binaryName);
      this.stagingSource = new Path(remoteRegistry.getStagingArea(), sourceName);

      this.remoteTmpDir = new Path(remoteRegistry.getTmpArea(), UUID.randomUUID().toString());
      this.tmpRemoteBinary = new Path(remoteTmpDir, binaryName);
      this.tmpRemoteSource = new Path(remoteTmpDir, sourceName);

      this.registryBinary = new Path(remoteRegistry.getRegistryArea(), binaryName);
      this.registrySource = new Path(remoteRegistry.getRegistryArea(), sourceName);

      this.localTmpDir = new Path(DrillFileUtils.createTempDir().toURI());
      this.fs = remoteRegistry.getFs();
    }

    /**
     * @return binary jar name
     */
    String getBinaryName() {
      return binaryName;
    }

    /**
     * Validates that both binary and source jar are present in staging area,
     * it is expected that binary and source have standard naming convention.
     * Backs up both jars to unique folder in remote temporary area.
     *
     * @throws IOException in case of binary or source absence or problems during copying jars
     */
    void initRemoteBackup() throws IOException {
      checkPathExistence(stagingBinary);
      checkPathExistence(stagingSource);
      fs.mkdirs(remoteTmpDir);
      FileUtil.copy(fs, stagingBinary, fs, tmpRemoteBinary, false, true, fs.getConf());
      FileUtil.copy(fs, stagingSource, fs, tmpRemoteSource, false, true, fs.getConf());
    }

    /**
     * Copies binary jar to unique folder on local file system.
     * Source jar is not needed for local validation.
     *
     * @return path to local binary jar
     * @throws IOException in case of problems during copying binary jar
     */
    Path copyBinaryToLocal() throws IOException {
      Path localBinary = new Path(localTmpDir, binaryName);
      fs.copyToLocalFile(tmpRemoteBinary, localBinary);
      return localBinary;
    }

    /**
     * Copies binary and source jars to registry area,
     * in case of {@link IOException} removes copied jar(-s) from registry area
     *
     * @throws IOException is re-thrown in case of problems during copying process
     */
    void copyToRegistryArea() throws IOException {
      FileUtil.copy(fs, tmpRemoteBinary, fs, registryBinary, false, true, fs.getConf());
      try {
        FileUtil.copy(fs, tmpRemoteSource, fs, registrySource, false, true, fs.getConf());
      } catch (IOException e) {
        deleteQuietly(registryBinary, false);
        throw new IOException(e);
      }
    }

    /**
     * Deletes binary and sources jars from staging area, in case of problems, logs warning and proceeds.
     */
    void deleteQuietlyFromStagingArea() {
      deleteQuietly(stagingBinary, false);
      deleteQuietly(stagingSource, false);
    }

    /**
     * Deletes binary and sources jars from registry area, in case of problems, logs warning and proceeds.
     */
    void deleteQuietlyFromRegistryArea() {
      deleteQuietly(registryBinary, false);
      deleteQuietly(registrySource, false);
    }

    /**
     * Removes quietly remote and local unique folders in temporary directories.
     */
    void cleanUp() {
      FileUtils.deleteQuietly(new File(localTmpDir.toUri()));
      deleteQuietly(remoteTmpDir, true);
    }

    /**
     * Checks if passed path exists on predefined file system.
     *
     * @param path path to be checked
     * @throws IOException if path does not exist
     */
    private void checkPathExistence(Path path) throws IOException {
      if (!fs.exists(path)) {
        throw new IOException(String.format("File %s does not exist on file system %s",
            path.toUri().getPath(), fs.getUri()));
      }
    }

    /**
     * Deletes quietly file or directory, in case of errors, logs warning and proceeds.
     *
     * @param path path to file or directory
     * @param isDirectory set to true if we need to delete a directory
     */
    private void deleteQuietly(Path path, boolean isDirectory) {
      try {
        fs.delete(path, isDirectory);
      } catch (IOException e) {
        logger.warn(String.format("Error during deletion [%s]", path.toUri().getPath()), e);
      }
    }
  }
}
