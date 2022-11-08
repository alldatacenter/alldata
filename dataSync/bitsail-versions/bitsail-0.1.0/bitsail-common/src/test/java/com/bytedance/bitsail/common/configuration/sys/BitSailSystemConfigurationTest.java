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

package com.bytedance.bitsail.common.configuration.sys;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.configuration.BitSailSystemConfiguration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.File;

import static com.bytedance.bitsail.common.configuration.BitSailSystemConfiguration.BITSAIL_ENV_CONF_NAME;

/**
 * Created 2022/7/7
 */
public class BitSailSystemConfigurationTest {

  @Rule
  public EnvironmentVariables variables = new EnvironmentVariables();

  private String classpath;

  @Before
  public void before() {
    variables.set(BITSAIL_ENV_CONF_NAME, "bitsail_template.conf");
    classpath = BitSailSystemConfigurationTest.class.getClassLoader()
        .getResource("").getPath();
  }

  @Test
  public void testSysConfigurationReader() {
    BitSailConfiguration sysConfiguration = BitSailSystemConfiguration
        .loadSysConfiguration(classpath);

    BitSailConfiguration from = BitSailConfiguration.from(new File(classpath, "CheckColumnsCorrectnessTest.json"));
    BitSailConfiguration merge = from.merge(sysConfiguration, true);
    Assert.assertNotNull(merge);
  }

}