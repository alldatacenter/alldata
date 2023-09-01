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
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.TaskContextImpl;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.shuffle.RssShuffleHandle;
import org.apache.spark.shuffle.RssShuffleManager;
import org.apache.spark.shuffle.RssSparkConfig;
import org.apache.spark.shuffle.reader.RssShuffleReader;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import scala.Tuple2;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetReaderTest extends IntegrationTestBase {

  @Test
  public void test() throws Exception {
    SparkConf sparkConf = new SparkConf();
    sparkConf.set("spark.shuffle.manager", "org.apache.spark.shuffle.RssShuffleManager");
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
    sparkConf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), COORDINATOR_QUORUM);
    sparkConf.setMaster("local[4]");
    final String remoteStorage1 = "hdfs://h1/p1";
    final String remoteStorage2 = "hdfs://h2/p2";
    final TaskContextImpl mockTaskContextImpl = new TaskContextImpl(
        0, 0,  0, 0, 0,
        null,null,null, null);

    String cfgFile = HDFS_URI + "/test/client_conf";
    Path path = new Path(cfgFile);
    FSDataOutputStream out = fs.create(path);
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(out));
    printWriter.println(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key()
        + " " + String.join(Constants.COMMA_SPLIT_CHAR,  remoteStorage1, remoteStorage2));
    printWriter.println(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_CLUSTER_CONF.key() + " h2,k1=v1,k2=v2");
    printWriter.println("spark.rss.storage.type " + StorageType.MEMORY_LOCALFILE_HDFS.name());
    printWriter.flush();
    printWriter.close();

    CoordinatorConf coordinatorConf = getCoordinatorConf();
    coordinatorConf.setBoolean("rss.coordinator.dynamicClientConf.enabled", true);
    coordinatorConf.setString("rss.coordinator.dynamicClientConf.path", cfgFile);
    coordinatorConf.setInteger("rss.coordinator.dynamicClientConf.updateIntervalSec", 1);
    coordinatorConf.setInteger("rss.coordinator.access.candidates.updateIntervalSec", 1);
    coordinatorConf.setInteger("rss.coordinator.access.loadChecker.serverNum.threshold", 1);
    coordinatorConf.setLong("rss.coordinator.remote.storage.schedule.time", 200);
    coordinatorConf.setInteger("rss.coordinator.remote.storage.schedule.access.times", 1);
    createCoordinatorServer(coordinatorConf);

    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    createShuffleServer(shuffleServerConf);
    startServers();
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

    SparkSession sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
    JavaSparkContext jsc1 = new JavaSparkContext(sparkSession.sparkContext());
    JavaPairRDD<String, Tuple2<Integer, Integer>> javaPairRDD1 = TestUtils.combineByKeyRDD(TestUtils.getRDD(jsc1));
    ShuffleDependency shuffleDependency1 = (ShuffleDependency) javaPairRDD1.rdd().dependencies().head();
    RssShuffleHandle rssShuffleHandle1 = (RssShuffleHandle) shuffleDependency1.shuffleHandle();
    RemoteStorageInfo remoteStorageInfo1 = rssShuffleHandle1.getRemoteStorage();
    assertEquals(remoteStorage1, remoteStorageInfo1.getPath());
    assertTrue(remoteStorageInfo1.getConfItems().isEmpty());

    // emptyRDD case
    JavaPairRDD<String, Tuple2<Integer, Integer>> javaEmptyPairRDD1 = TestUtils.combineByKeyRDD(
        TestUtils.getEmptyRDD(jsc1));
    ShuffleDependency emptyShuffleDependency1 = (ShuffleDependency) javaEmptyPairRDD1.rdd().dependencies().head();
    RssShuffleHandle emptyRssShuffleHandle1 = (RssShuffleHandle) emptyShuffleDependency1.shuffleHandle();
    assertEquals(javaEmptyPairRDD1.rdd().dependencies().head().rdd().getNumPartitions(), 0);
    assertEquals(emptyRssShuffleHandle1.getPartitionToServers(), Collections.emptyMap());
    assertEquals(emptyRssShuffleHandle1.getRemoteStorage(),RemoteStorageInfo.EMPTY_REMOTE_STORAGE);


    // the same app would get the same storage info
    JavaPairRDD<String, Tuple2<Integer, Integer>> javaPairRDD2 = TestUtils.combineByKeyRDD(TestUtils.getRDD(jsc1));
    ShuffleDependency shuffleDependency2 = (ShuffleDependency) javaPairRDD2.rdd().dependencies().head();
    RssShuffleHandle rssShuffleHandle2 = (RssShuffleHandle) shuffleDependency2.shuffleHandle();
    RemoteStorageInfo remoteStorageInfo2 = rssShuffleHandle2.getRemoteStorage();
    assertEquals(remoteStorage1, remoteStorageInfo1.getPath());
    assertTrue(remoteStorageInfo2.getConfItems().isEmpty());

    RssShuffleManager rssShuffleManager = (RssShuffleManager) sparkSession.sparkContext().env().shuffleManager();
    RssShuffleHandle rssShuffleHandle  = (RssShuffleHandle) shuffleDependency2.shuffleHandle();
    RssShuffleReader rssShuffleReader = (RssShuffleReader) rssShuffleManager.getReader(
        rssShuffleHandle, 0, 0, mockTaskContextImpl);
    Configuration hadoopConf =  rssShuffleReader.getHadoopConf();
    assertNull(hadoopConf.get("k1"));
    assertNull(hadoopConf.get("k2"));
    Configuration commonHadoopConf = jsc1.hadoopConfiguration();
    assertNull(commonHadoopConf.get("k1"));
    assertNull(commonHadoopConf.get("k2"));

    rssShuffleManager = (RssShuffleManager) sparkSession.sparkContext().env().shuffleManager();
    rssShuffleManager.setAppId("test2");
    JavaSparkContext jsc2 = new JavaSparkContext(sparkSession.sparkContext());
    JavaPairRDD<String, Tuple2<Integer, Integer>> javaPairRDD = TestUtils.combineByKeyRDD(TestUtils.getRDD(jsc2));
    ShuffleDependency shuffleDependency = (ShuffleDependency) javaPairRDD.rdd().dependencies().head();
    rssShuffleHandle = (RssShuffleHandle) shuffleDependency.shuffleHandle();
    // the reason for sleep here is to ensure that threads can be scheduled normally
    Thread.sleep(500);
    RemoteStorageInfo remoteStorageInfo3 = rssShuffleHandle.getRemoteStorage();
    assertEquals(remoteStorage1, remoteStorageInfo1.getPath());
    assertEquals(2, remoteStorageInfo3.getConfItems().size());
    assertEquals("v1", remoteStorageInfo3.getConfItems().get("k1"));
    assertEquals("v2", remoteStorageInfo3.getConfItems().get("k2"));

    rssShuffleReader = (RssShuffleReader) rssShuffleManager.getReader(
        rssShuffleHandle, 0, 0, mockTaskContextImpl);
    hadoopConf =  rssShuffleReader.getHadoopConf();
    assertEquals("v1", hadoopConf.get("k1"));
    assertEquals("v2", hadoopConf.get("k2"));
    // hadoop conf of reader and spark context should be isolated
    commonHadoopConf = jsc2.hadoopConfiguration();
    assertNull(commonHadoopConf.get("k1"));
    assertNull(commonHadoopConf.get("k2"));

    // mock the scenario that get reader in an executor
    rssShuffleReader = (RssShuffleReader) rssShuffleManager.getReader(
        rssShuffleHandle, 0, 0, mockTaskContextImpl);
    hadoopConf =  rssShuffleReader.getHadoopConf();
    assertEquals("v1", hadoopConf.get("k1"));
    assertEquals("v2", hadoopConf.get("k2"));
    // hadoop conf of reader and spark context should be isolated
    commonHadoopConf = jsc2.hadoopConfiguration();
    assertNull(commonHadoopConf.get("k1"));
    assertNull(commonHadoopConf.get("k2"));
  }
}
