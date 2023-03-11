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

package com.netease.arctic.spark;

import com.netease.arctic.spark.delegate.TestArcticSessionCatalog;
import com.netease.arctic.spark.delegate.TestMultiDelegateSessionCatalog;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.util.Map;

/**
 * Test suite for the arctic-spark library. all tests share same ams and hms.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ArcticSparkCatalogTestGroup.class,
    TestArcticSessionCatalog.class,
    TestMultiDelegateSessionCatalog.class,
})
public class MavenTestSuites {

  @BeforeClass
  public static void suiteSetup() throws IOException, ClassNotFoundException {
    Map<String, String> configs = Maps.newHashMap();
    Map<String, String> arcticConfigs = SparkTestContext.setUpTestDirAndArctic();
    Map<String, String> hiveConfigs = SparkTestContext.setUpHMS();
    configs.putAll(arcticConfigs);
    configs.putAll(hiveConfigs);
  }

  @AfterClass
  public static void suiteTeardown() {
    SparkTestContext.cleanUpAms();
    SparkTestContext.cleanUpHive();
  }
}
