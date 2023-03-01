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

package org.apache.spark.shuffle.celeborn;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

import scala.Tuple2;

import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.TaskContext;
import org.apache.spark.scheduler.MapStatus;
import org.apache.spark.scheduler.MapStatus$;
import org.apache.spark.shuffle.ShuffleHandle;
import org.apache.spark.shuffle.ShuffleReadMetricsReporter;
import org.apache.spark.shuffle.ShuffleReader;
import org.apache.spark.shuffle.sort.SortShuffleManager;
import org.apache.spark.sql.execution.UnsafeRowSerializer;
import org.apache.spark.sql.execution.metric.SQLMetric;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.storage.BlockManagerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.reflect.DynConstructors;
import org.apache.celeborn.reflect.DynFields;
import org.apache.celeborn.reflect.DynMethods;

public class SparkUtils {
  private static final Logger LOG = LoggerFactory.getLogger(SparkUtils.class);

  public static MapStatus createMapStatus(
      BlockManagerId loc, long[] uncompressedSizes, long mapTaskId) {
    return MapStatus$.MODULE$.apply(loc, uncompressedSizes, mapTaskId);
  }

  private static final DynFields.UnboundField<SQLMetric> DATA_SIZE_METRIC_FIELD =
      DynFields.builder()
          .hiddenImpl(UnsafeRowSerializer.class, "dataSize")
          .defaultAlwaysNull()
          .build();

  public static SQLMetric getDataSize(UnsafeRowSerializer serializer) {
    SQLMetric dataSizeMetric = DATA_SIZE_METRIC_FIELD.get(serializer);
    if (dataSizeMetric == null) {
      LOG.warn("Failed to get dataSize metric, AQE won't work properly.");
    }
    return dataSizeMetric;
  }

  public static long[] unwrap(LongAdder[] adders) {
    int adderCounter = adders.length;
    long[] res = new long[adderCounter];
    for (int i = 0; i < adderCounter; i++) {
      res[i] = adders[i].longValue();
    }
    return res;
  }

  /** make rss conf from spark conf */
  public static CelebornConf fromSparkConf(SparkConf conf) {
    CelebornConf tmpCelebornConf = new CelebornConf();
    for (Tuple2<String, String> kv : conf.getAll()) {
      if (kv._1.startsWith("spark.celeborn.") || kv._1.startsWith("spark.rss.")) {
        tmpCelebornConf.set(kv._1.substring("spark.".length()), kv._2);
      }
    }
    return tmpCelebornConf;
  }

  public static String genNewAppId(SparkContext context) {
    return context
        .applicationAttemptId()
        .map(id -> context.applicationId() + "_" + id)
        .getOrElse(context::applicationId);
  }

  // Create an instance of the class with the given name, possibly initializing it with our conf
  // Copied from SparkEnv
  public static <T> T instantiateClass(String className, SparkConf conf, Boolean isDriver) {
    DynConstructors.Ctor<T> dynConstructor =
        DynConstructors.builder()
            .impl(className, SparkConf.class, Boolean.TYPE)
            .impl(className, SparkConf.class)
            .impl(className)
            .build();
    return dynConstructor.newInstance(conf, isDriver);
  }

  // Added in SPARK-32055, for Spark 3.1 and above
  private static final DynMethods.UnboundMethod GET_READER_METHOD =
      DynMethods.builder("getReader")
          .impl(
              SortShuffleManager.class,
              ShuffleHandle.class,
              Integer.TYPE,
              Integer.TYPE,
              Integer.TYPE,
              Integer.TYPE,
              TaskContext.class,
              ShuffleReadMetricsReporter.class)
          .orNoop()
          .build();

  // Reserved for Spark 3.0, see detail in SPARK-32055
  private static final DynMethods.UnboundMethod LEGACY_GET_READER_METHOD =
      DynMethods.builder("getReader")
          .impl(
              SortShuffleManager.class,
              ShuffleHandle.class,
              Integer.TYPE,
              Integer.TYPE,
              TaskContext.class,
              ShuffleReadMetricsReporter.class)
          .orNoop()
          .build();

  public static <K, C> ShuffleReader<K, C> getReader(
      SortShuffleManager sortShuffleManager,
      ShuffleHandle handle,
      Integer startMapIndex,
      Integer endMapIndex,
      Integer startPartition,
      Integer endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    ShuffleReader<K, C> shuffleReader =
        GET_READER_METHOD
            .bind(sortShuffleManager)
            .invoke(
                handle, startMapIndex, endMapIndex, startPartition, endPartition, context, metrics);
    if (shuffleReader != null) {
      return shuffleReader;
    }

    shuffleReader =
        LEGACY_GET_READER_METHOD
            .bind(sortShuffleManager)
            .invoke(handle, startPartition, endPartition, context, metrics);
    assert shuffleReader != null;
    return shuffleReader;
  }

  private static final DynFields.UnboundField<StructType> SCHEMA_FIELD =
      DynFields.builder().hiddenImpl(ShuffleDependency.class, "schema").defaultAlwaysNull().build();

  public static StructType getSchema(ShuffleDependency<?, ?, ?> dep) throws IOException {
    StructType schema = SCHEMA_FIELD.bind(dep).get();
    if (schema == null) {
      throw new IOException("Failed to get Schema, columnar shuffle won't work properly.");
    }
    return schema;
  }
}
