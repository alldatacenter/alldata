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
import org.apache.spark.shuffle.DelegationRssShuffleManager;
import org.apache.spark.shuffle.RssShuffleManager;
import org.apache.spark.shuffle.RssSparkConfig;
import org.apache.spark.shuffle.ShuffleManager;
import org.apache.spark.shuffle.sort.SortShuffleManager;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoAccessTest extends IntegrationTestBase {

  @Test
  public void test() throws Exception {
    SparkConf sparkConf = new SparkConf();
    sparkConf.set("spark.shuffle.manager", "org.apache.spark.shuffle.DelegationRssShuffleManager");
    sparkConf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), COORDINATOR_QUORUM);
    sparkConf.set("spark.mock.2", "no-overwrite-conf");
    sparkConf.set(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key(), "overwrite-path");
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

    String candidatesFile = HDFS_URI + "/test/candidates";
    Path candidatesPath = new Path(candidatesFile);
    FSDataOutputStream out1 = fs.create(candidatesPath);
    PrintWriter printWriter1 = new PrintWriter(new OutputStreamWriter(out1));
    printWriter1.println(" test-id ");
    printWriter1.println(" ");
    printWriter1.flush();
    printWriter1.close();

    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean("rss.coordinator.dynamicClientConf.enabled", true);
    coordinatorConf.setString("rss.coordinator.dynamicClientConf.path", cfgFile);
    coordinatorConf.setInteger("rss.coordinator.dynamicClientConf.updateIntervalSec", 1);
    coordinatorConf.setInteger("rss.coordinator.access.candidates.updateIntervalSec", 1);
    coordinatorConf.setInteger("rss.coordinator.access.loadChecker.serverNum.threshold", 1);
    coordinatorConf.setString("rss.coordinator.access.candidates.path", candidatesFile);
    coordinatorConf.setString(
        "rss.coordinator.access.checkers",
        "org.apache.uniffle.coordinator.access.checker.AccessCandidatesChecker,"
            + "org.apache.uniffle.coordinator.access.checker.AccessClusterLoadChecker");
    createCoordinatorServer(coordinatorConf);

    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    createShuffleServer(shuffleServerConf);
    startServers();
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

    assertFalse(sparkConf.contains("spark.mock.1"));
    assertEquals("no-overwrite-conf", sparkConf.get("spark.mock.2"));
    assertTrue(sparkConf.getBoolean("spark.shuffle.service.enabled", true));

    // empty access id
    DelegationRssShuffleManager delegationRssShuffleManager = new DelegationRssShuffleManager(sparkConf, true);
    ShuffleManager shuffleManager = delegationRssShuffleManager.getDelegate();
    assertTrue(shuffleManager instanceof SortShuffleManager);
    assertTrue(sparkConf.getBoolean("spark.shuffle.service.enabled", true));
    assertEquals("overwrite-path", sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertFalse(sparkConf.contains("spark.rss.storage.type"));

    // wrong access id
    sparkConf.set("spark.rss.access.id", "9527");
    delegationRssShuffleManager = new DelegationRssShuffleManager(sparkConf, true);
    shuffleManager = delegationRssShuffleManager.getDelegate();
    assertTrue(shuffleManager instanceof SortShuffleManager);
    assertEquals("overwrite-path", sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertTrue(sparkConf.getBoolean("spark.shuffle.service.enabled", true));
    assertFalse(sparkConf.contains("spark.rss.storage.type"));

    // success to access and replace critical client conf
    sparkConf.set("spark.rss.access.id", "test-id");
    delegationRssShuffleManager = new DelegationRssShuffleManager(sparkConf, true);
    shuffleManager = delegationRssShuffleManager.getDelegate();
    assertTrue(shuffleManager instanceof RssShuffleManager);
    assertSame(sparkConf, ((RssShuffleManager) shuffleManager).getSparkConf());
    assertEquals(1234, sparkConf.getInt("spark.mock.1", 0));
    assertEquals("no-overwrite-conf", sparkConf.get("spark.mock.2"));
    assertTrue(sparkConf.getBoolean("spark.mock.3", false));
    assertEquals(StorageType.MEMORY_LOCALFILE_HDFS.name(), sparkConf.get("spark.rss.storage.type"));
    assertEquals("expectedPath", sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertFalse(sparkConf.getBoolean("spark.shuffle.service.enabled", true));

    // update candidates file
    fs.delete(candidatesPath, true);
    assertFalse(fs.exists(candidatesPath));
    Path tmpPath1 = new Path(candidatesPath + ".tmp");
    out1 = fs.create(tmpPath1);
    printWriter1 = new PrintWriter(new OutputStreamWriter(out1));
    printWriter1.println("9527");
    printWriter1.flush();
    printWriter1.close();
    fs.rename(tmpPath1, candidatesPath);
    sleep(1200);
    sparkConf.set("spark.rss.access.id", "test-id");
    delegationRssShuffleManager = new DelegationRssShuffleManager(sparkConf, true);
    shuffleManager = delegationRssShuffleManager.getDelegate();
    assertTrue(shuffleManager instanceof SortShuffleManager);
    sparkConf.set("spark.rss.access.id", "9527");
    delegationRssShuffleManager = new DelegationRssShuffleManager(sparkConf, true);
    shuffleManager = delegationRssShuffleManager.getDelegate();
    assertTrue(shuffleManager instanceof RssShuffleManager);
    assertSame(sparkConf, ((RssShuffleManager) shuffleManager).getSparkConf());
    assertEquals(1234, sparkConf.getInt("spark.mock.1", 0));
    assertEquals("no-overwrite-conf", sparkConf.get("spark.mock.2"));
    assertTrue(sparkConf.getBoolean("spark.mock.3", false));
    assertEquals(StorageType.MEMORY_LOCALFILE_HDFS.name(), sparkConf.get("spark.rss.storage.type"));
    assertEquals("expectedPath", sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertFalse(sparkConf.getBoolean("spark.shuffle.service.enabled", true));

    // update client conf file
    fs.delete(path, true);
    assertFalse(fs.exists(path));
    Path tmpPath = new Path(path + ".tmp");
    out = fs.create(tmpPath);
    printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println("spark.mock.1  404");
    printWriter.println(" spark.mock.2 overwrite-conf ");
    printWriter.println(" spark.mock.3 false ");
    printWriter.println("spark.rss.storage.type " + StorageType.MEMORY_LOCALFILE_HDFS.name());
    printWriter.println(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key() + " expectedPathNew");
    printWriter.flush();
    printWriter.close();
    fs.rename(tmpPath, path);
    sleep(1200);
    sparkConf.remove("spark.mock.1");
    sparkConf.remove("spark.mock.2");
    delegationRssShuffleManager = new DelegationRssShuffleManager(sparkConf, true);
    shuffleManager = delegationRssShuffleManager.getDelegate();
    assertTrue(shuffleManager instanceof RssShuffleManager);
    assertSame(sparkConf, ((RssShuffleManager) shuffleManager).getSparkConf());
    assertEquals(404, sparkConf.getInt("spark.mock.1", 0));
    assertEquals("overwrite-conf", sparkConf.get("spark.mock.2"));
    assertTrue(sparkConf.getBoolean("spark.mock.3", false));
    assertEquals(StorageType.MEMORY_LOCALFILE_HDFS.name(), sparkConf.get("spark.rss.storage.type"));
    assertEquals("expectedPathNew", sparkConf.get(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key()));
    assertFalse(sparkConf.getBoolean("spark.shuffle.service.enabled", true));
  }
}
