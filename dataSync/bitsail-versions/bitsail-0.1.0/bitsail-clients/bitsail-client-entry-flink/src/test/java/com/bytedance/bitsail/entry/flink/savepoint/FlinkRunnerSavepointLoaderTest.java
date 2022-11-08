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

package com.bytedance.bitsail.entry.flink.savepoint;

import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.configuration.BitSailSystemConfiguration;
import com.bytedance.bitsail.common.configuration.ConfigParser;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;
import com.bytedance.bitsail.entry.flink.configuration.FlinkRunnerConfigOptions;
import com.bytedance.bitsail.entry.flink.engine.FlinkEngineRunnerTest;

import com.beust.jcommander.internal.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Created 2022/8/15
 */
public class FlinkRunnerSavepointLoaderTest {
  @Rule
  public EnvironmentVariables variables = new EnvironmentVariables();

  private BitSailConfiguration sysConfiguration;
  private BitSailConfiguration jobConfiguration;

  private Path checkpointDir;

  @Before
  public void before() throws URISyntaxException, IOException {
    variables.set("BITSAIL_CONF_DIR", Paths.get(FlinkEngineRunnerTest.class.getClassLoader().getResource("").toURI()).toString());
    if (SystemUtils.IS_OS_WINDOWS) {
      String dll = Paths.get(FlinkEngineRunnerTest.class.getClassLoader().getResource("hadoop/bin/hadoop.dll")
          .toURI()).toString();
      System.load(dll);
    }
    String path = Paths.get(FlinkEngineRunnerTest.class.getClassLoader().getResource("examples/Fake_Print_Example.json").toURI()).toString();
    jobConfiguration = ConfigParser.fromRawConfPath(path);
    jobConfiguration.set(CommonOptions.JOB_TYPE, "streaming");
    String jobName = jobConfiguration.get(CommonOptions.JOB_NAME);

    sysConfiguration = BitSailSystemConfiguration.loadSysConfiguration();
    String flinkCheckpointBaseDirStr = sysConfiguration.get(FlinkRunnerConfigOptions.FLINK_CHECKPOINT_DIR);
    checkpointDir = Files.createDirectories(Paths.get(flinkCheckpointBaseDirStr, jobName, UUID.randomUUID().toString()));

    checkpointDir.resolve(Paths.get("chk-1"));
    checkpointDir.resolve(Paths.get("chk-3"));
    Path chk2Dir = checkpointDir.resolve(Paths.get("chk-2"));

    Files.createDirectories(chk2Dir);
    Files.createFile(chk2Dir.resolve("_metadata"));
  }

  @After
  public void after() throws IOException {
    FileUtils.deleteDirectory(checkpointDir.toFile());
  }

  @Test
  public void testLoadSavepointAuto() {
    List<String> commands = Lists.newArrayList();
    FlinkRunnerSavepointLoader.loadSavepointPath(sysConfiguration, jobConfiguration, new BaseCommandArgs(),
        new FlinkRunCommandArgs(), commands);

    Assert.assertEquals(2, commands.size());
    Path checkpoint = Paths.get(commands.get(1));
    Assert.assertEquals("chk-2", checkpoint.getFileName().toString());
  }

}