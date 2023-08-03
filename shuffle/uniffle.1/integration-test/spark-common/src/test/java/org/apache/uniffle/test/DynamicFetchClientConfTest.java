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

package org.apache.uniffle.test;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.shuffle.RssShuffleManager;
import org.apache.spark.shuffle.RssSparkConfig;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicFetchClientConfTest extends IntegrationTestBase {

  @Test
  public void test() throws Exception {
    SparkConf sparkConf = new SparkConf();
    sparkConf.set("spark.shuffle.manager", "org.apache.spark.shuffle.RssShuffleManager");
    sparkConf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), COORDINATOR_QUORUM);
    sparkConf.set("spark.mock.2", "no-overwrite-conf");
    sparkConf.set("spark.shuffle.service.enabled", "true");

    String cfgFile = HDFS_URI + "/test/client_conf";
    Path path = new Path(cfgFile);
    FSDataOutputStream out = fs.create(path);
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("spark.mock.1  1234");
    printWriter.println(" spark.mock.2 overwrite-conf ");
    printWriter.println(" spark.mock.3 true ");
    printWriter.println("spark.rss.storage.type " + StorageType.MEMORY_LOCALFILE_HDFS.name());
    printWriter.println(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key() + " expectedPath");
    printWriter.flush();
    printWriter.close();
    for (String k : RssSparkConfig.RSS_MANDATORY_CLUSTER_CONF) {
      sparkConf.set(k, "Dummy-" + k);
    }
    sparkConf.set("spark.mock.2", "no-overwrite-conf");

    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean("rss.coordinator.dynamicClientConf.enabled", true);
    coordinatorConf.setString("rss.coordinator.dynamicClientConf.path", cfgFile);
    coordinatorConf.setInteger("rss.coordinator.dynamicClientConf.updateIntervalSec", 10);
    createCoordinatorServer(coordinatorConf);
    startServers();

    Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);

    assertFalse(sparkConf.contains("spark.mock.1"));
    assertEquals("no-overwrite-conf", sparkConf.get("spark.mock.2"));
    assertFalse(sparkConf.contains("spark.mock.3"));
    assertEquals("Dummy-" + RssSparkConfig.RSS_STORAGE_TYPE.key(),
        sparkConf.get(RssSparkConfig.RSS_STORAGE_TYPE.key()));
    assertEquals("Dummy-" + RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key(),
        sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertTrue(sparkConf.getBoolean("spark.shuffle.service.enabled", true));

    RssShuffleManager rssShuffleManager = new RssShuffleManager(sparkConf, true);

    SparkConf sparkConf1 = rssShuffleManager.getSparkConf();
    assertEquals(1234, sparkConf1.getInt("spark.mock.1", 0));
    assertEquals("no-overwrite-conf", sparkConf1.get("spark.mock.2"));
    assertEquals(StorageType.MEMORY_LOCALFILE_HDFS.name(), sparkConf.get("spark.rss.storage.type"));
    assertEquals("expectedPath", sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertFalse(sparkConf1.getBoolean("spark.shuffle.service.enabled", true));

    fs.delete(path, true);
    shutdownServers();
    sparkConf = new SparkConf();
    sparkConf.set("spark.shuffle.manager", "org.apache.spark.shuffle.RssShuffleManager");
    sparkConf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), COORDINATOR_QUORUM);
    sparkConf.set("spark.mock.2", "no-overwrite-conf");
    sparkConf.set("spark.shuffle.service.enabled", "true");

    out = fs.create(path);
    printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("spark.mock.1  1234");
    printWriter.println(" spark.mock.2 overwrite-conf ");
    printWriter.println(" spark.mock.3 true ");
    printWriter.flush();
    printWriter.close();
    coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean("rss.coordinator.dynamicClientConf.enabled", true);
    coordinatorConf.setString("rss.coordinator.dynamicClientConf.path", cfgFile);
    coordinatorConf.setInteger("rss.coordinator.dynamicClientConf.updateIntervalSec", 10);
    createCoordinatorServer(coordinatorConf);
    startServers();
    Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);

    Exception expectException = null;
    try {
      new RssShuffleManager(sparkConf, true);
    } catch (IllegalArgumentException e) {
      expectException = e;
    }
    assertEquals(
        "Storage type must be set by the client or fetched from coordinators.",
        expectException.getMessage());
  }
}
