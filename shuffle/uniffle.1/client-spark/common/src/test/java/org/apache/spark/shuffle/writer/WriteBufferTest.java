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

package org.apache.spark.shuffle.writer;

import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.serializer.SerializationStream;
import org.apache.spark.serializer.Serializer;
import org.junit.jupiter.api.Test;
import scala.reflect.ClassTag$;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WriteBufferTest {

  private SparkConf conf = new SparkConf(false);
  private Serializer kryoSerializer = new KryoSerializer(conf);
  private WrappedByteArrayOutputStream arrayOutputStream = new WrappedByteArrayOutputStream(32);
  private SerializationStream serializeStream = kryoSerializer.newInstance().serializeStream(arrayOutputStream);
  private byte[] serializedData;
  private int serializedDataLength;

  @Test
  public void test() {
    WriterBuffer wb = new WriterBuffer(32);
    assertEquals(0, wb.getMemoryUsed());
    assertEquals(0, wb.getDataLength());

    serializeData("key", "value");
    // size of serialized kv is 12
    wb.addRecord(serializedData, serializedDataLength);
    assertEquals(32, wb.getMemoryUsed());
    assertEquals(12, wb.getDataLength());
    wb.addRecord(serializedData, serializedDataLength);
    assertEquals(32, wb.getMemoryUsed());
    // case: data size < output buffer size, when getData(), [] + buffer with 24b = 24b
    assertEquals(24, wb.getData().length);
    wb.addRecord(serializedData, serializedDataLength);
    // case: data size > output buffer size, when getData(), [1 buffer] + buffer with 12 = 36b
    assertEquals(36, wb.getData().length);
    assertEquals(64, wb.getMemoryUsed());
    wb.addRecord(serializedData, serializedDataLength);
    wb.addRecord(serializedData, serializedDataLength);
    // case: data size > output buffer size, when getData(), 2 buffer + output with 12b = 60b
    assertEquals(60, wb.getData().length);
    assertEquals(96, wb.getMemoryUsed());

    wb = new WriterBuffer(32);

    serializeData("key1111111111111111111111111111", "value222222222222222222222222222");
    wb.addRecord(serializedData, serializedDataLength);
    assertEquals(67, wb.getMemoryUsed());
    assertEquals(67, wb.getDataLength());

    serializeData("key", "value");
    wb.addRecord(serializedData, serializedDataLength);
    // 67 + 32
    assertEquals(99, wb.getMemoryUsed());
    // 67 + 12
    assertEquals(79, wb.getDataLength());
    assertEquals(79, wb.getData().length);

    wb.addRecord(serializedData, serializedDataLength);
    assertEquals(99, wb.getMemoryUsed());
    assertEquals(91, wb.getDataLength());
    assertEquals(91, wb.getData().length);
  }

  private void serializeData(Object key, Object value) {
    arrayOutputStream.reset();
    serializeStream.writeKey(key, ClassTag$.MODULE$.apply(key.getClass()));
    serializeStream.writeValue(value, ClassTag$.MODULE$.apply(value.getClass()));
    serializeStream.flush();
    serializedData = arrayOutputStream.getBuf();
    serializedDataLength = arrayOutputStream.size();
  }
}
