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

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.shuffle.RssSparkConfig;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.Tuple2;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShuffleUnregisterWithHdfsTest extends SparkIntegrationTestBase {

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    Map<String, String> dynamicConf = Maps.newHashMap();
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), HDFS_URI + "rss/test");
    dynamicConf.put(RssSparkConfig.RSS_STORAGE_TYPE.key(), StorageType.HDFS.name());
    addDynamicConf(coordinatorConf, dynamicConf);
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    shuffleServerConf.setString("rss.storage.type", StorageType.HDFS.name());
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @Override
  public void updateSparkConfCustomer(SparkConf sparkConf) {
  }

  private int runCounter = 0;

  @Test
  public void unregisterShuffleTest() throws Exception {
    run();
  }

  @Override
  public Map runTest(SparkSession spark, String fileName) throws Exception {
    // take a rest to make sure shuffle server is registered
    Thread.sleep(3000);
    JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
    JavaPairRDD<String, String> javaPairRDD1 = jsc.parallelizePairs(Lists.newArrayList(
        new Tuple2<>("a", "1"), new Tuple2<>("b", "2"),
        new Tuple2<>("c", "3"), new Tuple2<>("d", "4")), 2);
    JavaPairRDD<String, Iterable<String>> javaPairRDD = javaPairRDD1.groupByKey().sortByKey();
    Map map = javaPairRDD.collectAsMap();

    // The second run will use the rss. and we should check the effectiveness of unregisterShuffle method.
    if (runCounter == 1) {
      String basePath = HDFS_URI + "rss/test";
      String appPath = fs.listStatus(new Path(basePath))[0].getPath().toUri().getPath();

      String shufflePath = appPath + "/0";
      assertTrue(fs.exists(new Path(shufflePath)));

      spark.sparkContext().env().blockManager().master().removeShuffle(0, true);

      // Wait some time to cleanup the shuffle resource for shuffle-server
      Thread.sleep(1000);
      assertFalse(fs.exists(new Path(shufflePath)));
      assertTrue(fs.exists(new Path(appPath)));

      // After unregistering partial shuffle, newly shuffle could work
      map = javaPairRDD.collectAsMap();
      shufflePath = appPath + "/1";
      assertTrue(fs.exists(new Path(shufflePath)));
    } else {
      runCounter++;
    }
    return map;
  }
}
