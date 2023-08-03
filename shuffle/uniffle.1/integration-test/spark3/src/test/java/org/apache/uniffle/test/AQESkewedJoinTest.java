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
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.execution.adaptive.AdaptiveSparkPlanExec;
import org.apache.spark.sql.execution.joins.SortMergeJoinExec;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.internal.SQLConf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AQESkewedJoinTest extends SparkIntegrationTestBase {

  @BeforeAll
  public static void setupServers() throws Exception {
    CoordinatorConf coordinatorConf = getCoordinatorConf();
    createCoordinatorServer(coordinatorConf);
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    createShuffleServer(shuffleServerConf);
    startServers();
  }

  @Override
  public void updateCommonSparkConf(SparkConf sparkConf) {
    sparkConf.set(SQLConf.ADAPTIVE_EXECUTION_ENABLED().key(), "true");
    sparkConf.set(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD().key(), "-1");
    sparkConf.set(SQLConf.COALESCE_PARTITIONS_MIN_PARTITION_NUM().key(), "1");
    sparkConf.set(SQLConf.SHUFFLE_PARTITIONS().key(), "100");
    sparkConf.set(SQLConf.SKEW_JOIN_SKEWED_PARTITION_THRESHOLD().key(), "800");
    sparkConf.set(SQLConf.ADVISORY_PARTITION_SIZE_IN_BYTES().key(), "800");
  }

  @Override
  public void updateSparkConfCustomer(SparkConf sparkConf) {
    sparkConf.set(RssSparkConfig.RSS_STORAGE_TYPE.key(), StorageType.MEMORY_LOCALFILE_HDFS.name());
    sparkConf.set(RssSparkConfig.RSS_REMOTE_STORAGE_PATH.key(), HDFS_URI + "rss/test");
  }

  @Test
  public void resultCompareTest() throws Exception {
    run();
  }

  @Override
  Map runTest(SparkSession spark, String fileName) throws Exception {
    Thread.sleep(4000);
    Map<Integer, String> map = Maps.newHashMap();
    Dataset<Row> df2 = spark.range(0, 1000, 1, 10)
        .select(functions.when(functions.col("id").$less(250), 249)
            .otherwise(functions.col("id")).as("key2"), functions.col("id").as("value2"));
    Dataset<Row> df1 = spark.range(0, 1000, 1, 10)
        .select(functions.when(functions.col("id").$less(250), 249)
            .when(functions.col("id").$greater(750), 1000)
            .otherwise(functions.col("id")).as("key1"), functions.col("id").as("value2"));
    Dataset<Row> df3 = df1.join(df2, df1.col("key1").equalTo(df2.col("key2")));
    List<String> result = Lists.newArrayList();
    assertTrue(df3.queryExecution().executedPlan().toString().startsWith("AdaptiveSparkPlan isFinalPlan=false"));
    df3.collectAsList().forEach(row -> {
      result.add(row.json());
    });
    assertTrue(df3.queryExecution().executedPlan().toString().startsWith("AdaptiveSparkPlan isFinalPlan=true"));
    AdaptiveSparkPlanExec plan = (AdaptiveSparkPlanExec) df3.queryExecution().executedPlan();
    SortMergeJoinExec joinExec = (SortMergeJoinExec) plan.executedPlan().children().iterator().next();
    assertTrue(joinExec.isSkewJoin());
    result.sort(new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });
    int i = 0;
    for (String str : result) {
      map.put(i, str);
      i++;
    }
    return map;
  }
}
