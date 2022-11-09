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
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.configuration.BitSailSystemConfiguration;
import com.bytedance.bitsail.common.configuration.ConfigParser;
import com.bytedance.bitsail.entry.flink.configuration.FlinkRunnerConfigOptions;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * Created 2022/8/5
 */
public class FlinkEngineRunnerTest {

  @Rule
  public EnvironmentVariables variables = new EnvironmentVariables();

  private BaseCommandArgs baseCommandArgs;
  private BitSailConfiguration jobConfiguration;

  @Before
  public void before() throws URISyntaxException, IOException {
    variables.set("BITSAIL_CONF_DIR", Paths.get(FlinkEngineRunnerTest.class.getClassLoader().getResource("").toURI()).toString());

    baseCommandArgs = new BaseCommandArgs();
    baseCommandArgs.setMainAction("run");
    baseCommandArgs.setJobConf(Paths.get(FlinkEngineRunnerTest.class.getClassLoader().getResource("examples/Fake_Print_Example.json").toURI()).toString());
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("blob.fetch.num-concurrent", "32");
    baseCommandArgs.setProperties(properties);

    jobConfiguration = ConfigParser.fromRawConfPath(baseCommandArgs.getJobConf());

    File file = new File("/tmp/embedded/flink/bin/flink");
    Files.createParentDirs(file);
  }

  @Test
  public void testGetFlinkProcBuilder() throws IOException {
    String[] flinkRunCommandArgs = new String[] {"--execution-mode", "run", "--queue", "default", "--deployment-mode", "yarn-per-job"};
    baseCommandArgs.setUnknownOptions(flinkRunCommandArgs);
    BitSailConfiguration sysConfiguration = BitSailSystemConfiguration.loadSysConfiguration();
    FlinkEngineRunner flinkEngineRunner = new FlinkEngineRunner();
    flinkEngineRunner.initializeEngine(sysConfiguration);
    ProcessBuilder runProcBuilder = flinkEngineRunner
        .getRunProcBuilder(jobConfiguration, baseCommandArgs);

    List<String> command = runProcBuilder.command();
    Assert.assertEquals(62, command.size());
  }

  @Test
  public void testLoadLibrary() throws URISyntaxException {
    FlinkEngineRunner flinkEngineRunner = new FlinkEngineRunner();
    String path = Paths.get(FlinkEngineRunnerTest.class.getClassLoader().getResource("").toURI()).toString();
    BitSailConfiguration sysConfiguration = BitSailConfiguration.newDefault();
    sysConfiguration.set(FlinkRunnerConfigOptions.FLINK_HOME, path);
    flinkEngineRunner.initializeEngine(sysConfiguration);
    URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {});
    flinkEngineRunner.loadLibrary(urlClassLoader);
    URL[] urLs = urlClassLoader.getURLs();
    Assert.assertNotNull(urLs);
  }
}