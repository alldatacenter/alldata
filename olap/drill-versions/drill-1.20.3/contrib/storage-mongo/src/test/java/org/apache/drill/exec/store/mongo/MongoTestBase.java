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
package org.apache.drill.exec.store.mongo;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class MongoTestBase extends ClusterTest implements MongoTestConstants {
  private static StoragePluginRegistry pluginRegistry;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    pluginRegistry = cluster.drillbit().getContext().getStorage();

    MongoTestSuite.initMongo();
    initMongoStoragePlugin(MongoTestSuite.getConnectionURL());
  }

  private static void initMongoStoragePlugin(String connectionURI) throws Exception {
    MongoStoragePluginConfig storagePluginConfig = new MongoStoragePluginConfig(connectionURI,
        null, 100, false, PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    storagePluginConfig.setEnabled(true);
    pluginRegistry.put(MongoStoragePluginConfig.NAME, storagePluginConfig);

    client.testBuilder()
        .sqlQuery("alter session set `%s` = %s",
            ExecConstants.MONGO_BSON_RECORD_READER,
            System.getProperty("drill.mongo.tests.bson.reader", "true"))
        .unOrdered()
        .expectsEmptyResultSet();
  }

  @AfterClass
  public static void tearDownMongoTestBase() throws Exception {
    pluginRegistry.remove(MongoStoragePluginConfig.NAME);
    MongoTestSuite.tearDownCluster();
  }
}
