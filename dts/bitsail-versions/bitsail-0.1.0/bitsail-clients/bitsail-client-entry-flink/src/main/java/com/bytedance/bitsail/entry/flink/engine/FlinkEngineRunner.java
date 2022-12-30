/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.entry.flink.engine;

import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.client.api.command.CommandAction;
import com.bytedance.bitsail.client.api.command.CommandArgsParser;
import com.bytedance.bitsail.client.api.engine.EngineRunner;
import com.bytedance.bitsail.client.api.utils.PackageResolver;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;
import com.bytedance.bitsail.entry.flink.configuration.FlinkRunnerConfigOptions;
import com.bytedance.bitsail.entry.flink.deployment.DeploymentSupplier;
import com.bytedance.bitsail.entry.flink.deployment.DeploymentSupplierFactory;
import com.bytedance.bitsail.entry.flink.savepoint.FlinkRunnerSavepointLoader;
import com.bytedance.bitsail.entry.flink.security.FlinkSecurityHandler;
import com.bytedance.bitsail.entry.flink.utils.FlinkPackageResolver;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 2022/8/5
 */
public class FlinkEngineRunner implements EngineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkEngineRunner.class);

  private static final String ENTRY_JAR_NAME = "bitsail-core.jar";

  private DeploymentSupplierFactory deploymentSupplierFactory;

  private BitSailConfiguration sysConfiguration;

  private Path flinkDir;

  @Override
  public void initializeEngine(BitSailConfiguration sysConfiguration) {
    this.deploymentSupplierFactory = new DeploymentSupplierFactory();
    this.sysConfiguration = sysConfiguration;
    this.flinkDir = Paths.get(sysConfiguration.getNecessaryOption(FlinkRunnerConfigOptions.FLINK_HOME, CommonErrorCode.CONFIG_ERROR));
    LOG.info("Find flink dir = {} in System configuration.", flinkDir);
    if (!Files.exists(flinkDir)) {
      LOG.error("Flink dir = {} not exists in fact, plz check the system configuration.", flinkDir);
      throw new IllegalArgumentException(String.format("Flink dir %s not exists.", flinkDir));
    }
  }

  @Override
  @SneakyThrows
  public void loadLibrary(URLClassLoader classLoader) {
    Path flinkLibDir = FlinkPackageResolver.getFlinkLibDir(flinkDir);
    LOG.info("Load flink library from path: {}.", flinkLibDir);

    try (Stream<Path> libraries = Files.list(flinkLibDir)) {
      List<Path> flinkRuntimeLibraries = libraries
          .filter(library -> StringUtils.startsWith(library.getFileName().toString(), FlinkPackageResolver.FLINK_LIB_DIST_JAR_NAME))
          .collect(Collectors.toList());
      Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
      method.setAccessible(true);
      for (Path runtimeLibrary : flinkRuntimeLibraries) {
        method.invoke(classLoader, runtimeLibrary.toFile().toURL());
        LOG.info("Load flink runtime library {} to classpath.", runtimeLibrary);
      }
    }
  }

  @Override
  public ProcessBuilder getProcBuilder(BitSailConfiguration jobConfiguration,
                                       BaseCommandArgs baseCommandArgs) throws IOException {
    String argsMainAction = baseCommandArgs.getMainAction();

    switch (argsMainAction) {
      case CommandAction.RUN_COMMAND:
        return getRunProcBuilder(jobConfiguration, baseCommandArgs);
      default:
        throw new UnsupportedOperationException(String.format("Main action %s not support in flink engine.", argsMainAction));
    }
  }

  ProcessBuilder getRunProcBuilder(BitSailConfiguration jobConfiguration,
                                   BaseCommandArgs baseCommandArgs) throws IOException {
    FlinkRunCommandArgs flinkCommandArgs = new FlinkRunCommandArgs();
    CommandArgsParser.parseArguments(baseCommandArgs.getUnknownOptions(), flinkCommandArgs);

    DeploymentSupplier deploymentSupplier = deploymentSupplierFactory.getDeploymentSupplier(flinkCommandArgs,
        jobConfiguration);

    ProcessBuilder flinkProcBuilder = new ProcessBuilder();
    List<String> flinkCommands = Lists.newArrayList();

    flinkCommands.add(flinkDir + "/bin/flink");
    flinkCommands.add(flinkCommandArgs.getExecutionMode());
    deploymentSupplier.addDeploymentCommands(baseCommandArgs, flinkCommands);

    FlinkRunnerSavepointLoader.loadSavepointPath(sysConfiguration,
        jobConfiguration,
        baseCommandArgs,
        flinkCommandArgs,
        flinkCommands);

    if (!baseCommandArgs.isDetach()) {
      flinkCommands.add("-sae");
    } else {
      flinkCommands.add("--detached");
    }

    if (sysConfiguration.fieldExists(FlinkRunnerConfigOptions.FLINK_DEFAULT_PROPERTIES)) {
      for (Map.Entry<String, String> property : sysConfiguration.getFlattenMap(FlinkRunnerConfigOptions.FLINK_DEFAULT_PROPERTIES.key()).entrySet()) {
        LOG.info("Add System property {} = {}.", property.getKey(), property.getValue());
        flinkCommands.add("-D");
        flinkCommands.add(StringUtils.trim(property.getKey()) + "=" + StringUtils.trim((property.getValue())));
      }
    }

    for (Map.Entry<String, String> property : baseCommandArgs.getProperties().entrySet()) {
      LOG.info("Add Users property {} = {}.", property.getKey(), property.getValue());
      flinkCommands.add("-D");
      flinkCommands.add(StringUtils.trim(property.getKey()) + "=" + StringUtils.trim(property.getValue()));
    }

    flinkCommands.add(PackageResolver.getLibraryDir().resolve(ENTRY_JAR_NAME).toString());
    flinkCommands.add("-xjob_conf");
    flinkCommands.add(baseCommandArgs.getJobConf());

    flinkProcBuilder.command(flinkCommands);

    FlinkSecurityHandler.processSecurity(sysConfiguration, flinkProcBuilder, flinkDir);
    return flinkProcBuilder;
  }

  @Override
  public String engineName() {
    return "flink";
  }

}
