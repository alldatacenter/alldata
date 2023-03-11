/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive.catalog;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.BaseCatalogTest;
import com.netease.arctic.catalog.CatalogTestHelpers;
import com.netease.arctic.hive.TestHMS;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;

@RunWith(Parameterized.class)
public class HiveBasedCatalogTest extends BaseCatalogTest {

  @Parameterized.Parameters(name = "testFormat = {0}")
  public static Object[] parameters() {
    return new Object[] {TableFormat.ICEBERG, TableFormat.MIXED_HIVE};
  }

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public HiveBasedCatalogTest(TableFormat testFormat) {
    super(testFormat);
  }

  @Override
  protected CatalogMeta buildCatalogMeta() {
    Map<String, String> properties = Maps.newHashMap();
    return CatalogTestHelpers.buildHiveCatalogMeta(TEST_CATALOG_NAME,
        properties, TEST_HMS.getHiveConf(), getTestFormat());
  }
}