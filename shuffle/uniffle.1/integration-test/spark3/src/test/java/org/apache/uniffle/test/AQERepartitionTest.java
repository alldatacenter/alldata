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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.spark.SparkConf;
import org.apache.spark.shuffle.RssSparkConfig;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.internal.SQLConf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.collection.JavaConverters;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AQERepartitionTest extends SparkIntegrationTestBase {

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    Map<String, String> dynamicConf = Maps.newHashMap();
    dynamicConf.put(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key(), HDFS_URI + "rss/test");
    dynamicConf.put(RssSparkConfig.RSS_STORAGE_TYPE.key(), StorageType.MEMORY_LOCALFILE_HDFS.name());
    addDynamicConf(coordinatorConf, dynamicConf);
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @Override
  public void updateCommonSparkConf(SparkConf sparkConf) {
    sparkConf.set(SQLConf.ADAPTIVE_EXECUTION_ENABLED().key(), "true");
    sparkConf.set(SQLConf.COALESCE_PARTITIONS_ENABLED(), "true");
    sparkConf.set(SQLConf.COALESCE_PARTITIONS_INITIAL_PARTITION_NUM().key(), "6");
    sparkConf.set(SQLConf.SHUFFLE_PARTITIONS().key(), "10");
  }

  @Override
  public void updateSparkConfCustomer(SparkConf sparkConf) {
  }

  @Test
  public void resultCompareTest() throws Exception {
    run();
  }

  @Override
  Map runTest(SparkSession spark, String fileName) throws Exception {
    Thread.sleep(4000);
    List<Column> repartitionCols = Lists.newArrayList();
    repartitionCols.add(new Column("id"));
    Dataset<Long> df = spark.range(10).repartition(
        JavaConverters.asScalaBuffer(repartitionCols).toList());
    Long[][] result = (Long[][])df.rdd().collectPartitions();
    Map<Integer, List<Long>> map = Maps.newHashMap();
    for (int i = 0; i < result.length; i++) {
      map.putIfAbsent(i, Lists.newArrayList());
      for (int j = 0; j < result[i].length; j++) {
        map.get(i).add(result[i][j]);
      }
    }
    for (int i = 0; i < result.length; i++) {
      map.get(i).sort(new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
          return Long.compare(o1, o2);
        }
      });
    }
    assertTrue(result.length < 10);
    return map;
  }
}
