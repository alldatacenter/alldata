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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.status.AppStatusStore;
import org.apache.spark.status.api.v1.StageData;
import org.junit.jupiter.api.Test;
import scala.collection.Seq;

public class WriteAndReadMetricsTest extends SimpleTestBase {

  @Test
  public void test() throws Exception {
    run();
  }

  @Override
  public Map runTest(SparkSession spark, String fileName) throws Exception {
    // take a rest to make sure shuffle server is registered
    Thread.sleep(3000);

    Dataset<Row> df1 = spark.range(0, 100, 1, 10)
        .select(functions.when(functions.col("id").$less$eq(50), 1)
            .otherwise(functions.col("id")).as("key1"), functions.col("id").as("value1"));
    df1.createOrReplaceTempView("table1");

    List list = spark.sql("select count(value1) from table1 group by key1").collectAsList();
    Map<String, Long> result = new HashMap<>();
    result.put("size", Long.valueOf(list.size()));

    for (int stageId : spark.sparkContext().statusTracker().getJobInfo(0).get().stageIds()) {
      long writeRecords = getFirstStageData(spark, stageId).shuffleWriteRecords();
      long readRecords = getFirstStageData(spark, stageId).shuffleReadRecords();
      result.put(stageId + "-write-records", writeRecords);
      result.put(stageId + "-read-records", readRecords);
    }

    return result;
  }

  private StageData getFirstStageData(SparkSession spark, int stageId)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    AppStatusStore statestore = spark.sparkContext().statusStore();
    try {
      return ((Seq<StageData>)statestore
          .getClass()
          .getDeclaredMethod(
              "stageData",
              int.class,
              boolean.class
          ).invoke(statestore, stageId, false)).toList().head();
    } catch (Exception e) {
      return ((Seq<StageData>)statestore
          .getClass()
          .getDeclaredMethod(
              "stageData",
              int.class,
              boolean.class,
              List.class,
              boolean.class,
              double[].class
          ).invoke(
              statestore, stageId, false, new ArrayList<>(), true, new double[]{})).toList().head();
    }
  }
}
