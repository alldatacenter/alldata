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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import scala.None$;
import scala.Option;
import scala.Product2;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.ListBuffer;

import org.apache.spark.HashPartitioner;
import org.apache.spark.Partitioner;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkEnv;
import org.apache.spark.TaskContext;
import org.apache.spark.executor.ShuffleWriteMetrics;
import org.apache.spark.executor.TaskMetrics;
import org.apache.spark.memory.TaskMemoryManager;
import org.apache.spark.memory.UnifiedMemoryManager;
import org.apache.spark.scheduler.MapStatus;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.shuffle.ShuffleWriter;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.UnsafeProjection;
import org.apache.spark.sql.catalyst.expressions.UnsafeRow;
import org.apache.spark.sql.execution.PartitionIdPassthrough;
import org.apache.spark.sql.execution.UnsafeRowSerializer;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.IntegerType$;
import org.apache.spark.sql.types.StringType$;
import org.apache.spark.storage.BlockManager;
import org.apache.spark.storage.BlockManagerId;
import org.apache.spark.unsafe.types.UTF8String;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.DummyShuffleClient;
import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.util.JavaUtils;
import org.apache.celeborn.common.util.Utils;

public abstract class CelebornShuffleWriterSuiteBase {

  private static final Logger LOG = LoggerFactory.getLogger(CelebornShuffleWriterSuiteBase.class);
  private static final String NORMAL_RECORD = "hello, world";
  private static final String GIANT_RECORD = getGiantRecord();

  private final Random rand = new Random(System.currentTimeMillis());

  private final String appId = "appId";
  private final String host = "host";
  private final int port = 0;
  private final int shuffleId = 0;

  private final UserIdentifier userIdentifier = new UserIdentifier("mock", "mock");

  private final int numMaps = 10;
  protected final int numPartitions = 10;
  private final SparkConf sparkConf = new SparkConf(false);
  private final BlockManagerId bmId = BlockManagerId.apply("execId", "host", 1, None$.empty());

  private final TaskMemoryManager taskMemoryManager =
      new TaskMemoryManager(UnifiedMemoryManager.apply(sparkConf, 1), 0);
  private final MapStatus mapStatus =
      SparkUtils.createMapStatus(bmId, new long[numPartitions], new long[numPartitions]);

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private TaskContext taskContext = null;

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private ShuffleDependency<Integer, String, String> dependency = null;

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private SparkEnv env = null;

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private BlockManager blockManager = null;

  private TaskMetrics metrics = null;

  private static File tempDir = null;

  public CelebornShuffleWriterSuiteBase() throws IOException {}

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

  @Before
  public void beforeEach() {
    metrics = new TaskMetrics();
    MockitoAnnotations.initMocks(this);
    Mockito.doReturn(shuffleId).when(dependency).shuffleId();

    Mockito.doReturn(metrics).when(taskContext).taskMetrics();
    Mockito.doReturn(taskMemoryManager).when(taskContext).taskMemoryManager();

    Mockito.doReturn(bmId).when(blockManager).shuffleServerId();
    Mockito.doReturn(blockManager).when(env).blockManager();
    Mockito.doReturn(sparkConf).when(env).conf();
    SparkEnv.set(env);
  }

  @Test
  public void testEmptyBlock() throws Exception {
    final KryoSerializer serializer = new KryoSerializer(sparkConf);
    final CelebornConf conf = new CelebornConf();
    check(0, conf, serializer);
  }

  @Test
  public void testEmptyBlockWithFastWrite() throws Exception {
    final UnsafeRowSerializer serializer = new UnsafeRowSerializer(2, null);
    final CelebornConf conf = new CelebornConf();
    check(0, conf, serializer);
  }

  @Test
  public void testMergeSmallBlock() throws Exception {
    final KryoSerializer serializer = new KryoSerializer(sparkConf);
    final CelebornConf conf =
        new CelebornConf().set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "1024");
    check(10000, conf, serializer);
  }

  @Test
  public void testMergeSmallBlockWithFastWrite() throws Exception {
    final UnsafeRowSerializer serializer = new UnsafeRowSerializer(2, null);
    final CelebornConf conf =
        new CelebornConf().set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "1024");
    check(10000, conf, serializer);
  }

  @Test
  public void testGiantRecord() throws Exception {
    final KryoSerializer serializer = new KryoSerializer(sparkConf);
    final CelebornConf conf =
        new CelebornConf().set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "5");
    check(10000, conf, serializer);
  }

  @Test
  public void testGiantRecordWithFastWrite() throws Exception {
    final UnsafeRowSerializer serializer = new UnsafeRowSerializer(2, null);
    final CelebornConf conf =
        new CelebornConf().set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "5");
    check(10000, conf, serializer);
  }

  @Test
  public void testGiantRecordAndMergeSmallBlock() throws Exception {
    final KryoSerializer serializer = new KryoSerializer(sparkConf);
    final CelebornConf conf =
        new CelebornConf().set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "128");
    check(2 << 30, conf, serializer);
  }

  @Test
  public void testGiantRecordAndMergeSmallBlockWithFastWrite() throws Exception {
    final UnsafeRowSerializer serializer = new UnsafeRowSerializer(2, null);
    final CelebornConf conf =
        new CelebornConf().set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "128");
    check(2 << 30, conf, serializer);
  }

  private void check(
      final int approximateSize, final CelebornConf conf, final Serializer serializer)
      throws Exception {
    final boolean useUnsafe = serializer instanceof UnsafeRowSerializer;

    final Partitioner partitioner =
        useUnsafe ? new PartitionIdPassthrough(numPartitions) : new HashPartitioner(numPartitions);
    Mockito.doReturn(partitioner).when(dependency).partitioner();
    Mockito.doReturn(serializer).when(dependency).serializer();

    final File tempFile = new File(tempDir, UUID.randomUUID().toString());
    final CelebornShuffleHandle<Integer, String, String> handle =
        new CelebornShuffleHandle<>(
            appId, host, port, userIdentifier, shuffleId, numMaps, dependency);
    final ShuffleClient client = new DummyShuffleClient(conf, tempFile);
    ((DummyShuffleClient) client).initReducePartitionMap(shuffleId, numPartitions, 1);

    final ShuffleWriter<Integer, String> writer =
        createShuffleWriter(handle, taskContext, conf, client);

    if (writer instanceof SortBasedShuffleWriter) {
      assertEquals(useUnsafe, ((SortBasedShuffleWriter) writer).canUseFastWrite());
    } else if (writer instanceof HashBasedShuffleWriter) {
      assertEquals(useUnsafe, ((HashBasedShuffleWriter) writer).canUseFastWrite());
    }

    AtomicInteger total = new AtomicInteger(0);
    Iterator iterator = getIterator(approximateSize, total, useUnsafe, false);

    int expectChecksum = 0;
    for (int i = 0; i < total.intValue(); ++i) {
      expectChecksum ^= i;
    }

    writer.write(iterator);
    Option<MapStatus> status = writer.stop(true);
    client.shutdown();

    assertNotNull(status);
    assertTrue(status.isDefined());
    assertEquals(bmId, status.get().location());

    ShuffleWriteMetrics metrics = taskContext.taskMetrics().shuffleWriteMetrics();
    assertEquals(metrics.recordsWritten(), total.intValue());
    assertEquals(metrics.bytesWritten(), tempFile.length());

    try (FileInputStream fis = new FileInputStream(tempFile)) {
      Iterator it = serializer.newInstance().deserializeStream(fis).asKeyValueIterator();
      int checksum = 0;
      while (it.hasNext()) {
        Product2<Integer, ?> record;
        if (useUnsafe) {
          record = (Product2<Integer, UnsafeRow>) it.next();
        } else {
          record = (Product2<Integer, String>) it.next();
        }

        assertNotNull(record);
        assertNotNull(record._1());
        assertNotNull(record._2());

        int key;
        String value;

        if (useUnsafe) {
          UnsafeRow row = (UnsafeRow) record._2();

          key = row.getInt(0);
          value = row.getString(1);
        } else {
          key = record._1();
          value = (String) record._2();
        }

        checksum ^= key;
        total.decrementAndGet();

        assertTrue(
            "value should equals to normal record or giant record with key.",
            value.equals(key + ": " + NORMAL_RECORD) || value.equals(key + ": " + GIANT_RECORD));
      }
      assertEquals(0, total.intValue());
      assertEquals(expectChecksum, checksum);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Should read with no exception.");
    }
  }

  private Iterator getIterator(
      final int size, final AtomicInteger total, final boolean unsafe, final boolean mix) {
    if (unsafe) {
      return getUnsafeRowIterator(size, total, mix);
    } else {
      return getNormalIterator(size, total, mix);
    }
  }

  private Iterator<Product2<Integer, String>> getNormalIterator(
      final int size, final AtomicInteger total, final boolean mix) {
    int current = 0;
    ListBuffer<Product2<Integer, String>> list = new ListBuffer<>();
    while (current < size) {
      int key = total.getAndIncrement();
      String value = key + ": " + (mix && rand.nextBoolean() ? GIANT_RECORD : NORMAL_RECORD);
      current += value.length();
      list.$plus$eq(new Tuple2<>(key, value));
    }
    return list.toIterator();
  }

  private Iterator<Product2<Integer, UnsafeRow>> getUnsafeRowIterator(
      final int size, final AtomicInteger total, final boolean mix) {
    int current = 0;
    ListBuffer<Product2<Integer, UnsafeRow>> list = new ListBuffer<>();
    while (current < size) {
      int key = total.getAndIncrement();
      String value = key + ": " + (mix && rand.nextBoolean() ? GIANT_RECORD : NORMAL_RECORD);
      current += value.length();

      ListBuffer<Object> values = new ListBuffer<>();
      values.$plus$eq(key);
      values.$plus$eq(UTF8String.fromString(value));

      InternalRow row = InternalRow.apply(values.toSeq());
      DataType[] types = new DataType[2];
      types[0] = IntegerType$.MODULE$;
      types[1] = StringType$.MODULE$;
      UnsafeRow unsafeRow = UnsafeProjection.create(types).apply(row);

      list.$plus$eq(new Tuple2<>(key % numPartitions, unsafeRow));
    }
    return list.toIterator();
  }

  private static String getGiantRecord() {
    int numCopies = (128 + NORMAL_RECORD.length() - 1) / NORMAL_RECORD.length();
    return String.join("/", Collections.nCopies(numCopies, NORMAL_RECORD));
  }

  protected abstract ShuffleWriter<Integer, String> createShuffleWriter(
      CelebornShuffleHandle handle, TaskContext context, CelebornConf conf, ShuffleClient client)
      throws IOException;
}
