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

package com.bytedance.bitsail.client.entry;

import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.client.api.command.CommandAction;
import com.bytedance.bitsail.client.api.command.CommandArgsParser;
import com.bytedance.bitsail.client.api.engine.EngineRunner;
import com.bytedance.bitsail.client.entry.constants.EntryConstants;
import com.bytedance.bitsail.client.entry.security.SecurityContextFactory;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.configuration.BitSailSystemConfiguration;
import com.bytedance.bitsail.common.configuration.ConfigParser;

import com.beust.jcommander.internal.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Created 2022/8/1
 */
public class Entry {
  private static final Logger LOG = LoggerFactory.getLogger(Entry.class);

  private static final Map<String, EngineRunner> RUNNERS = Maps.newHashMap();
  private static final Class<EngineRunner> ENGINE_SPI_CLASS = EngineRunner.class;

  private static final Object LOCK = new Object();
  private static Process process;
  private static volatile boolean running;

  private final URLClassLoader classloader;
  private final BitSailConfiguration sysConfiguration;
  private final BaseCommandArgs baseCommandArgs;

  private static void loadAllEngines() {
    for (EngineRunner runner : ServiceLoader.load(ENGINE_SPI_CLASS)) {
      String engineName = runner.engineName();
      LOG.info("Load engine {} from classpath.", engineName);
      RUNNERS.put(StringUtils.upperCase(engineName), runner);
    }
  }

  private Entry(BitSailConfiguration sysConfiguration,
                BaseCommandArgs baseCommandArgs) {
    this.sysConfiguration = sysConfiguration;
    this.baseCommandArgs = baseCommandArgs;
    this.classloader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
    loadAllEngines();
  }

  @SuppressWarnings("checkstyle:EmptyLineSeparator")
  public static void main(String[] args) {

    //load system configuration.
    BitSailConfiguration sysConfiguration = BitSailSystemConfiguration.loadSysConfiguration();

    //load command arguments.
    BaseCommandArgs baseCommandArgs = loadCommandArguments(args);

    int exit;
    try {
      Entry entry = new Entry(sysConfiguration, baseCommandArgs);

      SecurityContextFactory securityContext = SecurityContextFactory
          .load(entry.sysConfiguration, entry.baseCommandArgs);

      exit = securityContext.doAs(
        () -> entry.runCommand());

      System.exit(exit);
    } catch (Exception e) {
      LOG.error("Exception occurred when run command .", e);
      exit = EntryConstants.ERROR_EXIT_CODE_UNKNOWN_FAILED;
      System.exit(exit);
    }
  }

  @SuppressWarnings("checkstyle:RegexpSingleline")
  public static BaseCommandArgs loadCommandArguments(String[] args) {
    if (args.length < 1) {
      CommandArgsParser.printHelp();
      System.out.println("Please specify an action. Supported action are" + CommandAction.RUN_COMMAND);
      System.exit(EntryConstants.ERROR_EXIT_CODE_COMMAND_ERROR);
    }
    final String mainCommand = args[0];
    final String[] params = Arrays.copyOfRange(args, 1, args.length);
    BaseCommandArgs baseCommandArgs = new BaseCommandArgs();
    baseCommandArgs.setUnknownOptions(CommandArgsParser.parseArguments(params, baseCommandArgs));
    baseCommandArgs.setMainAction(mainCommand);

    return baseCommandArgs;
  }

  private int runCommand() throws IOException, InterruptedException {
    ProcessBuilder processBuilder = buildProcessBuilder(sysConfiguration, baseCommandArgs);
    return startProcessBuilder(processBuilder, baseCommandArgs);
  }

  private ProcessBuilder buildProcessBuilder(BitSailConfiguration sysConfiguration,
                                             BaseCommandArgs baseCommandArgs) throws IOException {
    BitSailConfiguration jobConfiguration =
        ConfigParser.fromRawConfPath(baseCommandArgs.getJobConf());

    String engineName = baseCommandArgs.getEngineName();
    LOG.info("Final engine: {}.", engineName);
    EngineRunner engineRunner = RUNNERS.get(StringUtils.upperCase(engineName));
    if (Objects.isNull(engineRunner)) {
      throw new IllegalArgumentException(String.format("Engine %s not support now.", engineName));
    }
    engineRunner.initializeEngine(sysConfiguration);
    engineRunner.loadLibrary(classloader);

    ProcessBuilder procBuilder = engineRunner.getProcBuilder(
        jobConfiguration,
        baseCommandArgs);
    LOG.info("Engine {}'s command: {}.", baseCommandArgs.getEngineName(), procBuilder.command());
    return procBuilder;
  }

  private int startProcessBuilder(ProcessBuilder procBuilder,
                                  BaseCommandArgs baseCommandArgs) throws IOException, InterruptedException {
    procBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT);

    Thread hook = new Thread(() -> {
      synchronized (LOCK) {
        if (running) {
          if (Objects.nonNull(process) && process.isAlive()) {
            LOG.info("Shutdown the running proc with pid = {}.", process);
            process.destroy();
          }
        }
      }
    });

    Runtime.getRuntime().addShutdownHook(hook);
    int i = internalRunProcess(procBuilder, baseCommandArgs);
    Runtime.getRuntime().removeShutdownHook(hook);
    return i;
  }

  private static int internalRunProcess(ProcessBuilder procBuilder,
                                        BaseCommandArgs runCommandArgs) throws IOException, InterruptedException {
    synchronized (LOCK) {
      running = true;
      process = procBuilder.start();
    }
    return process.waitFor();
  }
}
