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

package org.apache.hadoop.mapred;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.SerializationFactory;
import org.apache.hadoop.io.serializer.Serializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SortWriteBufferTest {

  @Test
  public void testReadWrite() throws IOException {

    String keyStr = "key";
    String valueStr = "value";
    BytesWritable key = new BytesWritable(keyStr.getBytes());
    BytesWritable value = new BytesWritable(valueStr.getBytes());
    JobConf jobConf = new JobConf(new Configuration());
    SerializationFactory serializationFactory = new SerializationFactory(jobConf);
    Serializer<BytesWritable> keySerializer =  serializationFactory.getSerializer(BytesWritable.class);
    Serializer<BytesWritable> valSerializer = serializationFactory.getSerializer(BytesWritable.class);
    SortWriteBuffer<BytesWritable, BytesWritable> buffer =
        new SortWriteBuffer<BytesWritable, BytesWritable>(
            1,
            WritableComparator.get(BytesWritable.class),
            1024L,
            keySerializer,
            valSerializer);

    long recordLength = buffer.addRecord(key, value);
    assertEquals(20, buffer.getData().length);
    assertEquals(16, recordLength);
    assertEquals(1, buffer.getPartitionId());
    byte[] result = buffer.getData();
    Deserializer<BytesWritable> keyDeserializer = serializationFactory.getDeserializer(BytesWritable.class);
    Deserializer<BytesWritable> valDeserializer = serializationFactory.getDeserializer(BytesWritable.class);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(result);
    keyDeserializer.open(byteArrayInputStream);
    valDeserializer.open(byteArrayInputStream);

    DataInputStream dStream = new DataInputStream(byteArrayInputStream);
    int keyLen = readInt(dStream);
    int valueLen = readInt(dStream);
    assertEquals(recordLength, keyLen + valueLen);
    BytesWritable keyRead = keyDeserializer.deserialize(null);
    assertEquals(key, keyRead);
    BytesWritable valueRead = keyDeserializer.deserialize(null);
    assertEquals(value, valueRead);

    buffer = new SortWriteBuffer<BytesWritable, BytesWritable>(
        1,
        WritableComparator.get(BytesWritable.class),
        528L,
        keySerializer,
        valSerializer);
    long start = buffer.getDataLength();
    assertEquals(0, start);
    keyStr = "key3";
    key = new BytesWritable(keyStr.getBytes());
    keySerializer.serialize(key);
    byte[] valueBytes = new byte[200];
    Map<String, BytesWritable> valueMap = Maps.newConcurrentMap();
    Random random = new Random();
    random.nextBytes(valueBytes);
    value = new BytesWritable(valueBytes);
    valueMap.putIfAbsent(keyStr, value);
    valSerializer.serialize(value);
    recordLength = buffer.addRecord(key, value);
    Map<String, Long> recordLenMap = Maps.newConcurrentMap();
    recordLenMap.putIfAbsent(keyStr, recordLength);

    keyStr = "key1";
    key = new BytesWritable(keyStr.getBytes());
    valueBytes = new byte[2032];
    random.nextBytes(valueBytes);
    value = new BytesWritable(valueBytes);
    valueMap.putIfAbsent(keyStr, value);
    recordLength = buffer.addRecord(key, value);
    recordLenMap.putIfAbsent(keyStr, recordLength);

    byte[] bigKey = new byte[555];
    random.nextBytes(bigKey);
    bigKey[0] = 'k';
    bigKey[1] = 'e';
    bigKey[2] = 'y';
    bigKey[3] = '4';
    final BytesWritable bigWritableKey = new BytesWritable(bigKey);
    valueBytes = new byte[253];
    random.nextBytes(valueBytes);
    final BytesWritable bigWritableValue = new BytesWritable(valueBytes);
    final long bigRecordLength = buffer.addRecord(bigWritableKey, bigWritableValue);
    keyStr = "key2";
    key = new BytesWritable(keyStr.getBytes());
    valueBytes = new byte[3100];
    value = new BytesWritable(valueBytes);
    valueMap.putIfAbsent(keyStr, value);
    recordLength = buffer.addRecord(key, value);
    recordLenMap.putIfAbsent(keyStr, recordLength);

    result = buffer.getData();
    byteArrayInputStream = new ByteArrayInputStream(result);
    keyDeserializer.open(byteArrayInputStream);
    valDeserializer.open(byteArrayInputStream);
    for (int i = 1; i <= 3; i++) {
      dStream = new DataInputStream(byteArrayInputStream);
      long keyLenTmp = readInt(dStream);
      long valueLenTmp = readInt(dStream);
      String tmpStr = "key" + i;
      assertEquals(recordLenMap.get(tmpStr).longValue(), keyLenTmp + valueLenTmp);
      keyRead = keyDeserializer.deserialize(null);
      valueRead = valDeserializer.deserialize(null);
      BytesWritable bytesWritable = new BytesWritable(tmpStr.getBytes());
      assertEquals(bytesWritable, keyRead);
      assertEquals(valueMap.get(tmpStr), valueRead);
    }

    dStream = new DataInputStream(byteArrayInputStream);
    long keyLenTmp = readInt(dStream);
    long valueLenTmp = readInt(dStream);
    assertEquals(bigRecordLength, keyLenTmp + valueLenTmp);
    keyRead = keyDeserializer.deserialize(null);
    valueRead = valDeserializer.deserialize(null);
    assertEquals(bigWritableKey, keyRead);
    assertEquals(bigWritableValue, valueRead);
  }

  int readInt(DataInputStream dStream) throws IOException {
    return WritableUtils.readVInt(dStream);
  }
}
