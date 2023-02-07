/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.record.vector;

import io.netty.buffer.DrillBuf;
import org.apache.drill.categories.VectorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.impl.SingleDictWriter;
import org.apache.drill.exec.vector.complex.reader.BaseReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.drill.test.TestBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;

@Category(VectorTest.class)
public class TestDictVector extends ExecTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private BufferAllocator allocator;

  @Before
  public void setUp() {
    allocator = RootAllocatorFactory.newRoot(DrillConfig.create());
  }

  @After
  public void tearDown(){
    allocator.close();
  }

  @Test
  public void testVectorCreation() {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 0.3f, 3L, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, 9.2f, 7L),
          TestBuilder.mapOfObject(4.01f, 8L, 9.2f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 11L, 9.73f, 12L, 0.03f, 13L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt((long) entry.getValue());
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      BaseReader.DictReader mapReader = mapVector.getReader();
      index = 0;
      for (Map<Object, Object> map : maps) {
        mapReader.setPosition(index++);
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapReader.next();
          Float actualKey = mapReader.reader(DictVector.FIELD_KEY_NAME).readFloat();
          Long actualValue = mapReader.reader(DictVector.FIELD_VALUE_NAME).readLong();
          assertEquals(entry.getKey(), actualKey);
          assertEquals(entry.getValue(), actualValue);
        }
      }
    }
  }

  @Test
  public void testVectorCreationWithNullValues() {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 0.3f, null, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, 9.2f, 7L),
          TestBuilder.mapOfObject(4.01f, null, 9.2f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 11L, 9.73f, null, 0.03f, 13L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          Long value = (Long) entry.getValue();
          if (value != null) {
            mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt(value);
          } // else skip writing a value. Notice that index was incremented
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      BaseReader.DictReader mapReader = mapVector.getReader();
      index = 0;
      for (Map<Object, Object> map : maps) {
        mapReader.setPosition(index++);
        assertEquals(map.size(), mapReader.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapReader.next();
          Float actualKey = mapReader.reader(DictVector.FIELD_KEY_NAME).readFloat();
          Long actualValue = mapReader.reader(DictVector.FIELD_VALUE_NAME).readLong();
          assertEquals(entry.getKey(), actualKey);
          assertEquals(entry.getValue(), actualValue);
        }
      }
    }
  }

  @Test
  public void testVectorCreationWithNullKeys() {
    thrown.expect(DrillRuntimeException.class);
    thrown.expectMessage(containsString("Key in DICT cannot be null. Index: 1"));

    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 1.23f, 3L, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, null, 7L), // this map contains null key which is not supported
          TestBuilder.mapOfObject(4.01f, 8L, 9.2f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 11L, 9.73f, 12L, 0.03f, 13L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          if (entry.getKey() != null) {
            mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          } // else skip writing a key. Notice that index was incremented.
          // Whether the key is written will be checked in endKeyValuePair()
          // and an Exception will be thrown in case it was not set (i.e. it is null).
          mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt((Long) entry.getValue());
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }
    }
  }

  @Test
  public void testSplitAndTransfer() {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 0.3f, 3L, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, 9.2f, 7L),
          TestBuilder.mapOfObject(4.01f, 8L, 9.2f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 11L, 9.73f, 12L, 0.03f, 13L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt((long) entry.getValue());
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      int start = 1;
      int length = 2;

      DictVector newMapVector = new DictVector(field, allocator, null);
      TransferPair transferPair = mapVector.makeTransferPair(newMapVector);
      transferPair.splitAndTransfer(start, length);

      BaseReader.DictReader mapReader = newMapVector.getReader();
      index = 0;
      for (Map<Object, Object> map : maps.subList(start, start + length)) {
        mapReader.setPosition(index++);
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapReader.next();
          Float actualKey = mapReader.reader(DictVector.FIELD_KEY_NAME).readFloat();
          Long actualValue = mapReader.reader(DictVector.FIELD_VALUE_NAME).readLong();
          assertEquals(entry.getKey(), actualKey);
          assertEquals(entry.getValue(), actualValue);
        }
      }

      newMapVector.clear();
    }
  }

  @Test
  public void testLoadValueVector() {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 0.3f, 3L, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, 9.2f, 7L),
          TestBuilder.mapOfObject(4.01f, 8L, 9.2f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 11L, 9.73f, 12L, 0.03f, 13L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt((long) entry.getValue());
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      UserBitShared.SerializedField oldField = mapVector.getMetadata();
      WritableBatch writableBatch = WritableBatch.getBatchNoHV(oldField.getValueCount(), Collections.singletonList(mapVector), false);
      DrillBuf byteBuf = TestLoad.serializeBatch(allocator, writableBatch);

      DictVector newMapVector = new DictVector(field.clone(), allocator, null);
      newMapVector.load(oldField, byteBuf);

      BaseReader.DictReader mapReader = newMapVector.getReader();
      index = 0;
      for (Map<Object, Object> map : maps) {
        mapReader.setPosition(index++);
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapReader.next();
          Float actualKey = mapReader.reader(DictVector.FIELD_KEY_NAME).readFloat();
          Long actualValue = mapReader.reader(DictVector.FIELD_VALUE_NAME).readLong();
          assertEquals(entry.getKey(), actualKey);
          assertEquals(entry.getValue(), actualValue);
        }
      }

      newMapVector.clear();
      byteBuf.release();

      writableBatch.clear();
    }
  }

  @Test
  public void testLoadBatchLoader() throws Exception {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 0.3f, 3L, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, 9.2f, 7L),
          TestBuilder.mapOfObject(4.01f, 8L, 9.2f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 11L, 9.73f, 12L, 0.03f, 13L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt((long) entry.getValue());
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      WritableBatch writableBatch = WritableBatch.getBatchNoHV(maps.size(), Collections.singletonList(mapVector), false);
      // Serialize the vector
      DrillBuf byteBuf = TestLoad.serializeBatch(allocator, writableBatch);
      RecordBatchLoader batchLoader = new RecordBatchLoader(allocator);
      batchLoader.load(writableBatch.getDef(), byteBuf);

      byteBuf.release();

      assertEquals(maps.size(), batchLoader.getRecordCount());

      writableBatch.clear();
      batchLoader.clear();
    }
  }

  @Test
  public void testGetByKey() {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(4f, 1L, 5.3f, 2L, 0.3f, 3L, -0.2f, 4L, 102.07f, 5L),
          TestBuilder.mapOfObject(45f, 6L, 9.2f, 7L),
          TestBuilder.mapOfObject(4.01f, 8L, 4f, 9L, -2.3f, 10L),
          TestBuilder.mapOfObject(-2.5f, 11L),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(11f, 12L, 9.73f, 13L, 4f, 14L)
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.float4(DictVector.FIELD_KEY_NAME).writeFloat4((float) entry.getKey());
          mapWriter.bigInt(DictVector.FIELD_VALUE_NAME).writeBigInt((long) entry.getValue());
          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      BaseReader.DictReader mapReader = mapVector.getReader();

      float key = 4.0f;
      // Due to limitations of Calcite, we can pass NameSegment and ArraySegment
      // only as String and int respectively hence we need to transform float key
      // to String to be able to use it with DictReader which then
      // will derive appropriate type internally.
      String stringKey = String.valueOf(key);

      NullableBigIntHolder valueHolder = new NullableBigIntHolder();
      index = 0;
      for (Map<Object, Object> map : maps) {
        mapReader.setPosition(index++);
        mapReader.next();
        mapReader.read(stringKey, valueHolder);
        assertEquals(map.get(key), valueHolder.isSet == 1 ? valueHolder.value : null);
        // reset value holder to reuse it for the next row
        valueHolder.isSet = 0;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testVectorCreationListValue() {
    MaterializedField field = MaterializedField.create("map", DictVector.TYPE);
    try (DictVector mapVector = new DictVector(field, allocator, null)) {
      mapVector.allocateNew();

      List<Map<Object, Object>> maps = Arrays.asList(
          TestBuilder.mapOfObject(
              1, TestBuilder.listOf(1.0, 2.3, 3.1),
              2, TestBuilder.listOf(4.9, -5.002)
          ),
          TestBuilder.mapOfObject(
              3, TestBuilder.listOf(6.901),
              4, TestBuilder.listOf(),
              5, TestBuilder.listOf(7.03, -8.973)
          ),
          TestBuilder.mapOfObject(),
          TestBuilder.mapOfObject(
              6, TestBuilder.listOf(9.0, 10.0, 11.0),
              7, TestBuilder.listOf(12.07, 13.01, 14.58, 15.039),
              8, TestBuilder.listOf(-16.0, -17.0, 18.0, 19.23, 20.1234)
          )
      );

      BaseWriter.DictWriter mapWriter = new SingleDictWriter(mapVector, null);
      int index = 0;
      for (Map<Object, Object> map : maps) {
        mapWriter.setPosition(index++);
        mapWriter.start();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapWriter.startKeyValuePair();
          mapWriter.integer(DictVector.FIELD_KEY_NAME).writeInt((int) entry.getKey());
          BaseWriter.ListWriter valueWriter = mapWriter.list(DictVector.FIELD_VALUE_NAME);

          valueWriter.startList();
          for (Object element : (List<Object>) entry.getValue()) {
            valueWriter.float8().writeFloat8((double) element);
          }
          valueWriter.endList();

          mapWriter.endKeyValuePair();
        }
        mapWriter.end();
      }

      BaseReader.DictReader mapReader = mapVector.getReader();
      index = 0;
      for (Map<Object, Object> map : maps) {
        mapReader.setPosition(index++);
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          mapReader.next();
          Integer actualKey = mapReader.reader(DictVector.FIELD_KEY_NAME).readInteger();
          Object actualValue = mapReader.reader(DictVector.FIELD_VALUE_NAME).readObject();
          assertEquals(entry.getKey(), actualKey);
          assertEquals(entry.getValue(), actualValue);
        }
      }
    }
  }
}
