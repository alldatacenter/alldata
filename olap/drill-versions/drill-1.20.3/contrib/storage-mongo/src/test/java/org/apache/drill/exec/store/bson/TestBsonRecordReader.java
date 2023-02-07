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
package org.apache.drill.exec.store.bson;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;

import org.apache.drill.exec.memory.RootAllocator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.BufferManager;
import org.apache.drill.exec.ops.BufferManagerImpl;
import org.apache.drill.exec.store.TestOutputMutator;
import org.apache.drill.exec.vector.complex.impl.SingleMapReaderImpl;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.test.BaseTest;
import org.bson.BsonBinary;
import org.bson.BsonBinarySubType;
import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonDouble;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.BsonWriter;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBsonRecordReader extends BaseTest {
  private BufferAllocator allocator;
  private VectorContainerWriter writer;

  private BufferManager bufferManager;
  private BsonRecordReader bsonReader;

  @Before
  public void setUp() {
    allocator = new RootAllocator(Long.MAX_VALUE);
    TestOutputMutator mutator = new TestOutputMutator(allocator);
    writer = new VectorContainerWriter(mutator);
    bufferManager = new BufferManagerImpl(allocator);
    bsonReader = new BsonRecordReader(bufferManager.getManagedBuffer(1024), false, false);
  }

  @Test
  public void testIntType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("seqNo", new BsonInt64(10));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals(10L, mapReader.reader("seqNo").readLong().longValue());
  }

  @Test
  public void testTimeStampType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("ts_small", new BsonTimestamp(1000, 10));
    bsonDoc.append("ts_large", new BsonTimestamp(1000000000, 10));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals(1000000L, mapReader.reader("ts_small").readLocalDateTime().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
    assertEquals(1000000000000L, mapReader.reader("ts_large").readLocalDateTime().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
  }

  @Test
  public void testSymbolType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("symbolKey", new BsonSymbol("test_symbol"));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals("test_symbol", mapReader.reader("symbolKey").readText().toString());
  }

  @Test
  public void testStringType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("stringKey", new BsonString("test_string"));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals("test_string", mapReader.reader("stringKey").readText().toString());
  }

  @Test
  public void testSpecialCharStringType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("stringKey", new BsonString("§§§§§§§§§1"));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals("§§§§§§§§§1",
        mapReader.reader("stringKey").readText().toString());
  }

  @Test
  public void testObjectIdType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    BsonObjectId value = new BsonObjectId(new ObjectId());
    bsonDoc.append("_idKey", value);
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    byte[] readByteArray = mapReader.reader("_idKey").readByteArray();
    assertArrayEquals(value.getValue().toByteArray(), readByteArray);
  }

  @Test
  public void testNullType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("nullKey", new BsonNull());
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertNull(mapReader.reader("nullKey").readObject());
  }

  @Test
  public void testDoubleType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("doubleKey", new BsonDouble(12.35));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals(12.35d, mapReader.reader("doubleKey").readDouble(), 0.00001);
  }

  @Test
  public void testArrayOfDocumentType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    BsonWriter bw = new BsonDocumentWriter(bsonDoc);
    bw.writeStartDocument();
    bw.writeName("a");
    bw.writeString("MongoDB");
    bw.writeName("b");
    bw.writeStartArray();
    bw.writeStartDocument();
    bw.writeName("c");
    bw.writeInt32(1);
    bw.writeEndDocument();
    bw.writeEndArray();
    bw.writeEndDocument();
    bw.flush();
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    FieldReader reader = writer.getMapVector().getReader();
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) reader;
    FieldReader reader3 = mapReader.reader("b");
    assertEquals("MongoDB", mapReader.reader("a").readText().toString());
  }

  @Test
  public void testRecursiveDocuments() throws IOException {
    BsonDocument topDoc = new BsonDocument();
    final int count = 3;
    for (int i = 0; i < count; ++i) {
      BsonDocument bsonDoc = new BsonDocument();
      BsonWriter bw = new BsonDocumentWriter(bsonDoc);
      bw.writeStartDocument();
      bw.writeName("k1" + i);
      bw.writeString("drillMongo1" + i);
      bw.writeName("k2" + i);
      bw.writeString("drillMongo2" + i);
      bw.writeEndDocument();
      bw.flush();
      topDoc.append("doc" + i, bsonDoc);
    }
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(topDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    for (int i = 0; i < count; ++i) {
      SingleMapReaderImpl reader = (SingleMapReaderImpl) mapReader.reader("doc" + i);
      assertEquals("drillMongo1" + i, reader.reader("k1" + i).readText().toString());
      assertEquals("drillMongo2" + i, reader.reader("k2" + i).readText().toString());
    }
  }

  @Test
  public void testDateTimeType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("dateTimeKey", new BsonDateTime(5262729712L));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertEquals(5262729712L, mapReader.reader("dateTimeKey").readLocalDateTime().atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
  }

  @Test
  public void testBooleanType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    bsonDoc.append("booleanKey", new BsonBoolean(true));
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertTrue(mapReader.reader("booleanKey").readBoolean());
  }

  @Test
  public void testBinaryTypes() throws IOException {
    // test with different binary types
    BsonDocument bsonDoc = new BsonDocument();
    // Binary
    // String
    byte[] bytes = "binaryValue".getBytes();
    bsonDoc.append("binaryKey", new BsonBinary(BsonBinarySubType.BINARY, bytes));
    // String
    byte[] bytesString = "binaryStringValue".getBytes();
    bsonDoc.append("binaryStringKey", new BsonBinary((byte) 2, bytesString));
    // Double
    byte[] bytesDouble = new byte[8];
    java.nio.ByteBuffer.wrap(bytesDouble).putDouble(23.0123);
    BsonBinary bsonDouble = new BsonBinary((byte) 1, bytesDouble);
    bsonDoc.append("binaryDouble", bsonDouble);
    // Boolean
    byte[] booleanBytes = new byte[8];
    java.nio.ByteBuffer.wrap(booleanBytes).put((byte) 1);
    BsonBinary bsonBoolean = new BsonBinary((byte) 8, booleanBytes);
    bsonDoc.append("bsonBoolean", bsonBoolean);
    writer.reset();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    assertArrayEquals(bytes, mapReader.reader("binaryKey").readByteArray());
    assertEquals("binaryStringValue", mapReader.reader("binaryStringKey").readText().toString());
    assertEquals(23.0123, mapReader.reader("binaryDouble").readDouble(), 0);
    FieldReader reader = mapReader.reader("bsonBoolean");
    assertEquals(true, reader.readBoolean());
  }

  @Test
  public void testArrayType() throws IOException {
    BsonDocument bsonDoc = new BsonDocument();
    BsonWriter bw = new BsonDocumentWriter(bsonDoc);
    bw.writeStartDocument();
    bw.writeName("arrayKey");
    bw.writeStartArray();
    bw.writeInt32(1);
    bw.writeInt32(2);
    bw.writeInt32(3);
    bw.writeEndArray();
    bw.writeEndDocument();
    bw.flush();
    bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
    SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
    FieldReader reader = mapReader.reader("arrayKey");
    assertEquals(3, reader.size());
  }

    @Test
    public void testDecimal128Type() throws IOException {
        BsonDocument bsonDoc = new BsonDocument();
        bsonDoc.append("decimal128Key", new BsonDecimal128(Decimal128.parse("12.12345624")));
        writer.reset();
        bsonReader.write(writer, new BsonDocumentReader(bsonDoc));
        SingleMapReaderImpl mapReader = (SingleMapReaderImpl) writer.getMapVector().getReader();
        assertEquals(new BigDecimal("12.12345624"), mapReader.reader("decimal128Key").readBigDecimal());
    }

  @After
  public void cleanUp() {
    try {
      writer.close();
    } catch (Exception e) {
      // noop
    }

    bufferManager.close();
    allocator.close();
  }
}
