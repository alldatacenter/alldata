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

package com.bytedance.bitsail.base.packages;

import com.bytedance.bitsail.common.BitSailException;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PluginManagerTest {
  PluginManager pluginManager;

  @Before
  public void init() throws URISyntaxException {
    String confPathRoot = Paths.get(PluginManagerTest.class.getResource("/classloader/").toURI()).toString();
    pluginManager = new PluginManager(Paths.get(confPathRoot), false, true, "plugin", "plugin_conf");
  }

  @Test
  public void getPluginsFromConfFilesTest() {
    List<Plugin> pluginList = pluginManager.getPluginsFromConfFiles();
    assertEquals(3, pluginList.size());
  }

  @Test
  public void getPluginLibsTest() {
    List<URL> pluginLibs = pluginManager.getPluginLibs("test1");
    assertEquals(3, pluginLibs.size());
    assertTrue(pluginLibs.get(0).getPath().endsWith("test1"));

    pluginLibs = pluginManager.getPluginLibs("com.bytedance.bitsail.batch.test2");
    assertEquals(3, pluginLibs.size());
  }

  @Test(expected = BitSailException.class)
  public void pluginLibsNotFountTest() {
    // bitsail-io test
    String testClass = "com.bytedance.bitsail.batch.jdbc.test";
    pluginManager.getPluginLibs(testClass);
  }
}
