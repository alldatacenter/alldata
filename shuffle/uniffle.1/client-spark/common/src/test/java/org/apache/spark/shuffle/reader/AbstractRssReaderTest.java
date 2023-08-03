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

package org.apache.spark.shuffle.reader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.spark.serializer.SerializationStream;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.serializer.SerializerInstance;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import scala.Product2;
import scala.collection.Iterator;
import scala.reflect.ClassTag$;

import org.apache.uniffle.client.util.ClientUtils;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.compression.Codec;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.storage.HdfsTestBase;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;


public abstract class AbstractRssReaderTest extends HdfsTestBase {

  private AtomicInteger atomicInteger = new AtomicInteger(0);

  protected void validateResult(Iterator iterator,
      Map<String, String> expectedData, int recordNum) {
    Set<String> actualKeys = Sets.newHashSet();
    while (iterator.hasNext()) {
      Product2 product2 = (Product2) iterator.next();
      String key = (String) product2._1();
      String value = (String) product2._2();
      actualKeys.add(key);
      assertEquals(expectedData.get(key), value);
    }
    assertEquals(recordNum, actualKeys.size());
    assertEquals(expectedData.keySet(), actualKeys);
  }

  protected void writeTestData(
      ShuffleWriteHandler handler,
      int blockNum,
      int recordNum,
      Map<String, String> expectedData,
      Roaring64NavigableMap blockIdBitmap,
      String keyPrefix,
      Serializer serializer,
      int partitionID) throws Exception {
    writeTestData(handler, blockNum, recordNum, expectedData, blockIdBitmap, keyPrefix, serializer,
        partitionID, true);
  }

  protected void writeTestData(
      ShuffleWriteHandler handler,
      int blockNum,
      int recordNum,
      Map<String, String> expectedData,
      Roaring64NavigableMap blockIdBitmap,
      String keyPrefix,
      Serializer serializer,
      int partitionID,
      boolean compress) throws Exception {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    SerializerInstance serializerInstance = serializer.newInstance();
    for (int i = 0; i < blockNum; i++) {
      Output output = new Output(1024, 2048);
      SerializationStream serializeStream = serializerInstance.serializeStream(output);
      for (int j = 0; j < recordNum; j++) {
        String key = keyPrefix + "_" + i + "_" + j;
        String value = "valuePrefix_" + i + "_" + j;
        expectedData.put(key, value);
        writeData(serializeStream, key, value);
      }
      long blockId = ClientUtils.getBlockId(partitionID, 0, atomicInteger.getAndIncrement());
      blockIdBitmap.add(blockId);
      blocks.add(createShuffleBlock(output.toBytes(), blockId, compress));
      serializeStream.close();
    }
    handler.write(blocks);
  }

  protected ShufflePartitionedBlock createShuffleBlock(byte[] data, long blockId) {
    return createShuffleBlock(data, blockId, true);
  }

  protected ShufflePartitionedBlock createShuffleBlock(byte[] data, long blockId, boolean compress) {
    byte[] compressData = data;
    if (compress) {
      compressData = Codec.newInstance(new RssConf()).compress(data);
    }
    long crc = ChecksumUtils.getCrc32(compressData);
    return new ShufflePartitionedBlock(compressData.length, data.length, crc, blockId, 0,
        compressData);
  }

  protected void writeData(SerializationStream serializeStream, String key, String value) {
    serializeStream.writeKey(key, ClassTag$.MODULE$.apply(key.getClass()));
    serializeStream.writeValue(value, ClassTag$.MODULE$.apply(value.getClass()));
    serializeStream.flush();
  }
}
