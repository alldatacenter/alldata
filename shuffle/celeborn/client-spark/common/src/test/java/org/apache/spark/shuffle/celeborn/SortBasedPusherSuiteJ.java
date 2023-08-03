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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import scala.collection.mutable.ListBuffer;

import org.apache.spark.SparkConf;
import org.apache.spark.memory.TaskMemoryManager;
import org.apache.spark.memory.UnifiedMemoryManager;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.UnsafeProjection;
import org.apache.spark.sql.catalyst.expressions.UnsafeRow;
import org.apache.spark.sql.types.BinaryType$;
import org.apache.spark.sql.types.DataType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.DummyShuffleClient;
import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.util.JavaUtils;
import org.apache.celeborn.common.util.Utils;

public class SortBasedPusherSuiteJ {

  private static final Logger LOG = LoggerFactory.getLogger(SortBasedPusherSuiteJ.class);
  private final SparkConf sparkConf = new SparkConf(false).set("spark.buffer.pageSize", "2m");

  private final CelebornConf conf = new CelebornConf();

  private final UnifiedMemoryManager unifiedMemoryManager =
      UnifiedMemoryManager.apply(sparkConf, 1);
  private final TaskMemoryManager taskMemoryManager =
      new TaskMemoryManager(unifiedMemoryManager, 0);

  private final File tempFile = new File(tempDir, UUID.randomUUID().toString());
  private static File tempDir = null;

  @BeforeClass
  public static void beforeAll() {
    tempDir = Utils.createTempDir(System.getProperty("java.io.tmpdir"), "celeborn_test");
  }

  @AfterClass
  public static void afterAll() {
    try {
      JavaUtils.deleteRecursively(tempDir);
    } catch (IOException e) {
      LOG.error("Failed to delete temporary directory.", e);
    }
  }

  @Test
  public void testMemoryUsage() throws Exception {
    final ShuffleClient client = new DummyShuffleClient(conf, tempFile);
    SortBasedPusher pusher =
        new SortBasedPusher(
            taskMemoryManager,
            /*shuffleClient=*/ client,
            /*shuffleId=*/ 0,
            /*mapId=*/ 0,
            /*attemptNumber=*/ 0,
            /*taskAttemptId=*/ 0,
            /*numMappers=*/ 0,
            /*numPartitions=*/ 0,
            conf,
            /*afterPush=*/ null,
            /*mapStatusLengths=*/ null,
            /*pushSortMemoryThreshold=*/ Utils.byteStringAsBytes("1m"),
            /*sharedPushLock=*/ null,
            /*executorService=*/ null,
            SendBufferPool.get(4));

    // default page size == 2 MiB
    assertEquals(unifiedMemoryManager.pageSizeBytes(), Utils.byteStringAsBytes("2m"));

    UnsafeRow row9k = genUnsafeRow(1024 * 9); // 9232 B
    assertEquals(row9k.getSizeInBytes(), 9232);

    // if uao = 4, total write size = (9232 B + 4 + 4) * 226 = 2088240 B = 2039.3 KiB
    // if uao = 8, total write size = (9232 B + 4 + 8) * 226 = 2089144 B = 2040.2 KiB
    for (int i = 0; i < 226; i++) {
      assertTrue(
          pusher.insertRecord(
              row9k.getBaseObject(), row9k.getBaseOffset(), row9k.getSizeInBytes(), 0, true));
    }
    // total used memory: sum(pusher.allocatedPages.size()) + pusher.inMemSorter = 2m + 1m = 3m
    assertEquals(pusher.getUsed(), Utils.byteStringAsBytes("3m"));
    // there is not enough space to write a new 9k row
    assertTrue(
        !pusher.insertRecord(
            row9k.getBaseObject(), row9k.getBaseOffset(), row9k.getSizeInBytes(), 0, true));

    UnsafeRow row5k = genUnsafeRow(1024 * 5);
    assertTrue(
        pusher.insertRecord(
            row5k.getBaseObject(), row5k.getBaseOffset(), row5k.getSizeInBytes(), 0, true));
    assertTrue(
        !pusher.insertRecord(
            row5k.getBaseObject(), row5k.getBaseOffset(), row5k.getSizeInBytes(), 0, true));

    pusher.close();
  }

  private static UnsafeRow genUnsafeRow(int size) {
    ListBuffer<Object> values = new ListBuffer<>();
    byte[] bytes = new byte[size];
    values.$plus$eq(bytes);
    InternalRow row = InternalRow.apply(values.toSeq());
    DataType[] types = new DataType[1];
    types[0] = BinaryType$.MODULE$;
    return UnsafeProjection.create(types).apply(row);
  }
}
