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

package org.apache.drill.exec.store.druid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.categories.DruidStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.store.druid.rest.DruidQueryClientTest;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestDruidQueries.class,
    DruidScanSpecBuilderTest.class,
    DruidStoragePluginConfigTest.class,
    DruidQueryClientTest.class,
    DruidFilterBuilderTest.class,
    DruidScanSpecBuilderTest.class
})
@Category({SlowTest.class, DruidStorageTest.class})
public class DruidTestSuite {
  private static final Logger logger = LoggerFactory.getLogger(DruidTestSuite.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  private static DruidStoragePluginConfig druidStoragePluginConfig = null;

  @BeforeClass
  public static void initDruid() {
    try {
      JsonNode storagePluginJson = mapper.readTree(new File(Resources.getResource("bootstrap-storage-plugins.json").toURI()));
      druidStoragePluginConfig = mapper.treeToValue(storagePluginJson.get("storage").get("druid"), DruidStoragePluginConfig.class);
      druidStoragePluginConfig.setEnabled(true);
      TestDataGenerator.startImport(druidStoragePluginConfig);
    } catch (Exception e) {
      logger.error("Error importing data into DRUID", e);
    }
  }

  public static DruidStoragePluginConfig getDruidStoragePluginConfig() {
    return druidStoragePluginConfig;
  }
}
