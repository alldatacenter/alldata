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
package org.apache.drill.exec.store.avro;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.util.Text;
import org.apache.drill.test.BaseDirTestWatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilities for generating Avro test data.
 */
public class AvroDataGenerator {

  public static final int RECORD_COUNT = 50;
  public static int ARRAY_SIZE = 4;

  private final BaseDirTestWatcher dirTestWatcher;

  public AvroDataGenerator(BaseDirTestWatcher dirTestWatcher) {
    this.dirTestWatcher = dirTestWatcher;
  }

  /**
   * Class to write records to an Avro file while simultaneously
   * constructing a corresponding list of records in the format taken in
   * by the Drill test builder to describe expected results.
   */
  public static class AvroTestRecordWriter implements Closeable {

    private final List<Map<String, Object>> expectedRecords;
    private final Schema schema;
    private final DataFileWriter<GenericData.Record> writer;
    private final String filePath;
    private final String fileName;

    private GenericData.Record currentAvroRecord;
    private Map<String, Object> currentExpectedRecord;

    public AvroTestRecordWriter(Schema schema, File file) {
      writer = new DataFileWriter<>(new GenericDatumWriter<>(schema));
      try {
        writer.create(schema, file);
      } catch (IOException e) {
        throw new RuntimeException("Error creating file in Avro test setup.", e);
      }
      this.schema = schema;
      currentExpectedRecord = new TreeMap<>();
      expectedRecords = new ArrayList<>();
      filePath = file.getAbsolutePath();
      fileName = file.getName();
    }

    public void startRecord() {
      currentAvroRecord = new GenericData.Record(schema);
      currentExpectedRecord = new TreeMap<>();
    }

    public void put(String key, Object value) {
      currentAvroRecord.put(key, value);
      // convert binary values into byte[], the format they will be given
      // in the Drill result set in the test framework
      currentExpectedRecord.put("`" + key + "`", convertAvroValToDrill(value, true));
    }

    // TODO - fix this the test wrapper to prevent the need for this hack
    // to make the root behave differently than nested fields for String vs. Text
    private Object convertAvroValToDrill(Object value, boolean root) {
      if (value instanceof ByteBuffer) {
        ByteBuffer bb = ((ByteBuffer) value);
        byte[] drillVal = new byte[((ByteBuffer) value).remaining()];
        bb.get(drillVal);
        bb.position(0);
        value = drillVal;
      } else if (!root && value instanceof CharSequence) {
        value = new Text(value.toString());
      } else if (value instanceof GenericData.Array) {
        GenericData.Array<?> array = ((GenericData.Array<?>) value);
         JsonStringArrayList<Object> drillList = new JsonStringArrayList<>();
        for (Object o : array) {
          drillList.add(convertAvroValToDrill(o, false));
        }
        value = drillList;
      } else if (value instanceof GenericData.EnumSymbol) {
        value = value.toString();
      } else if (value instanceof GenericData.Record) {
        GenericData.Record rec = ((GenericData.Record) value);
        JsonStringHashMap<String, Object> newRecord = new JsonStringHashMap<>();
        for (Schema.Field field : rec.getSchema().getFields()) {
          Object val = rec.get(field.name());
          newRecord.put(field.name(), convertAvroValToDrill(val, false));
        }
        value = newRecord;
      }
      return value;
    }

    public void endRecord() throws IOException {
      writer.append(currentAvroRecord);
      expectedRecords.add(currentExpectedRecord);
    }

    @Override
    public void close() throws IOException {
      writer.close();
    }

    public String getFilePath() {
      return filePath;
    }

    public String getFileName() {
      return fileName;
    }

    public List<Map<String, Object>> getExpectedRecords() {
      return expectedRecords;
    }
  }


  public AvroTestRecordWriter generateSimplePrimitiveSchema_NoNullValues() throws Exception {
    return generateSimplePrimitiveSchema_NoNullValues(RECORD_COUNT);
  }

  public AvroTestRecordWriter generateSimplePrimitiveSchema_NoNullValues(int numRecords) throws Exception {
    return generateSimplePrimitiveSchema_NoNullValues(numRecords, "");
  }

  /**
   * Generates Avro table with specified rows number.
   *
   * @param numRecords rows number in the table
   * @param tablePath  table path
   * @return AvroTestRecordWriter instance
   */
  public AvroTestRecordWriter generateSimplePrimitiveSchema_NoNullValues(int numRecords, String tablePath) throws Exception {
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_long").type().longType().noDefault()
      .name("d_float").type().floatType().noDefault()
      .name("e_double").type().doubleType().noDefault()
      .name("f_bytes").type().bytesType().noDefault()
      .name("g_null").type().nullType().noDefault()
      .name("h_boolean").type().booleanType().noDefault()
      .endRecord();

    File file = File.createTempFile("avro-simple-primitive-no-nulls-test", ".avro",
     dirTestWatcher.makeRootSubDir(Paths.get(tablePath)));
    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      ByteBuffer bb = ByteBuffer.allocate(2);
      bb.put(0, (byte) 'a');
      for (int i = 0; i < numRecords; i++) {
        bb.put(1, (byte) ('0' + (i % 10)));
        bb.position(0);
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);
        record.put("c_long", (long) i);
        record.put("d_float", (float) i);
        record.put("e_double", (double) i);
        record.put("f_bytes", bb);
        record.put("g_null", null);
        record.put("h_boolean", (i % 2 == 0));
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateUnionSchema_WithNullValues() throws Exception {
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_long").type().longType().noDefault()
      .name("d_float").type().floatType().noDefault()
      .name("e_double").type().doubleType().noDefault()
      .name("f_bytes").type().bytesType().noDefault()
      .name("g_null").type().nullType().noDefault()
      .name("h_boolean").type().booleanType().noDefault()
      .name("i_union").type().optional().doubleType()
      .endRecord();

    File file = File.createTempFile("avro-simple-primitive-with-nulls-test", ".avro", dirTestWatcher.getRootDir());
    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      ByteBuffer bb = ByteBuffer.allocate(1);
      bb.put(0, (byte) 1);
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);
        record.put("c_long", (long) i);
        record.put("d_float", (float) i);
        record.put("e_double", (double) i);
        record.put("f_bytes", bb);
        record.put("g_null", null);
        record.put("h_boolean", (i % 2 == 0));
        record.put("i_union", (i % 2 == 0 ? (double) i : null));
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateUnionSchema_WithNonNullValues() throws Exception {
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("i_union").type().unionOf().doubleType().and().longType().endUnion().noDefault()
      .endRecord();

    File file = File.createTempFile("avro-complex-union-no-nulls-test", ".avro", dirTestWatcher.getRootDir());
    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("i_union", (i % 2 == 0 ? (double) i : (long) i));
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateSimpleEnumSchema_NoNullValues() throws Exception {
    String[] symbols = { "E_SYM_A", "E_SYM_B", "E_SYM_C", "E_SYM_D" };

    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_enum").type().enumeration("my_enum").symbols(symbols).noDefault()
      .endRecord();

    File file = File.createTempFile("avro-primitive-with-enum-no-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema enumSchema = schema.getField("b_enum").schema();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        GenericData.EnumSymbol symbol = new GenericData.EnumSymbol(enumSchema, symbols[(i + symbols.length) % symbols.length]);
        record.put("b_enum", symbol);
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateSimpleArraySchema_NoNullValues() throws Exception {
    File file = File.createTempFile("avro-simple-array-no-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_string_array").type().array().items().stringType().noDefault()
      .name("d_int_array").type().array().items().intType().noDefault()
      .name("e_float_array").type().array().items().floatType().noDefault()
      .endRecord();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);
        {
          GenericArray<String> array = new GenericData.Array<>(ARRAY_SIZE, schema.getField("c_string_array").schema());
          for (int j = 0; j < ARRAY_SIZE; j++) {
            array.add(j, "c_string_array_" + i + "_" + j);
          }
          record.put("c_string_array", array);
        }
        {
          GenericArray<Integer> array = new GenericData.Array<>(ARRAY_SIZE, schema.getField("d_int_array").schema());
          for (int j = 0; j < ARRAY_SIZE; j++) {
            array.add(j, i * j);
          }
          record.put("d_int_array", array);
        }
        {
          GenericArray<Float> array = new GenericData.Array<>(ARRAY_SIZE, schema.getField("e_float_array").schema());
          for (int j = 0; j < ARRAY_SIZE; j++) {
            array.add(j, (float) (i * j));
          }
          record.put("e_float_array", array);
        }
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateSimpleNestedSchema_NoNullValues() throws Exception {
    File file = File.createTempFile("avro-simple-nested-no-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_record").type().record("my_record_1")
        .namespace("foo.blah.org")
        .fields()
        .name("nested_1_string").type().stringType().noDefault()
        .name("nested_1_int").type().intType().noDefault()
        .endRecord()
        .noDefault()
      .endRecord();

    Schema nestedSchema = schema.getField("c_record").schema();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);

        GenericRecord nestedRecord = new GenericData.Record(nestedSchema);
        nestedRecord.put("nested_1_string", "nested_1_string_" +  i);
        nestedRecord.put("nested_1_int", i * i);

        record.put("c_record", nestedRecord);
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateUnionNestedArraySchema_withNullValues() throws Exception {
    File file = File.createTempFile("avro-nested-with-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_array").type().optional().array().items().record("my_record_1")
        .namespace("foo.blah.org").fields()
        .name("nested_1_string").type().optional().stringType()
        .name("nested_1_int").type().optional().intType()
        .endRecord()
      .endRecord();

    Schema nestedSchema = schema.getField("c_array").schema();
    Schema arraySchema = nestedSchema.getTypes().get(1);
    Schema itemSchema = arraySchema.getElementType();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);

        if (i % 2 == 0) {
          GenericArray<GenericRecord> array = new GenericData.Array<>(1, arraySchema);
          GenericRecord nestedRecord = new GenericData.Record(itemSchema);
          nestedRecord.put("nested_1_string", "nested_1_string_" +  i);
          nestedRecord.put("nested_1_int", i * i);
          array.add(nestedRecord);
          record.put("c_array", array);
        }
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateNestedArraySchema() throws IOException {
    return generateNestedArraySchema(RECORD_COUNT, ARRAY_SIZE);
  }

  public AvroTestRecordWriter generateNestedArraySchema(int numRecords, int numArrayItems) throws IOException {
    File file = File.createTempFile("avro-nested-array-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest").namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_int").type().intType().noDefault()
      .name("b_array").type().array().items()
        .record("my_record_1").namespace("foo.blah.org")
        .fields()
        .name("nested_1_int").type().optional().intType()
        .endRecord()
        .arrayDefault(Collections.emptyList())
      .endRecord();

    Schema arraySchema = schema.getField("b_array").schema();
    Schema itemSchema = arraySchema.getElementType();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < numRecords; i++) {
        record.startRecord();
        record.put("a_int", i);
        GenericArray<GenericRecord> array = new GenericData.Array<>(ARRAY_SIZE, arraySchema);

        for (int j = 0; j < numArrayItems; j++) {
          GenericRecord nestedRecord = new GenericData.Record(itemSchema);
          nestedRecord.put("nested_1_int", j);
          array.add(nestedRecord);
        }
        record.put("b_array", array);
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateMapSchema_withNullValues() throws Exception {
    File file = File.createTempFile("avro-map-with-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_map").type().optional().map().values(Schema.create(Type.STRING))
      .endRecord();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);

        if (i % 2 == 0) {
          Map<String, String> strMap = new HashMap<>();
          strMap.put("key1", "nested_1_string_" +  i);
          strMap.put("key2", "nested_1_string_" +  (i + 1 ));
          record.put("c_map", strMap);
        }
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateMapSchemaComplex_withNullValues() throws Exception {
    File file = File.createTempFile("avro-map-complex-with-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_map").type().optional().map().values(Schema.create(Type.STRING))
      .name("d_map").type().optional().map().values(Schema.createArray(Schema.create(Type.DOUBLE)))
      .endRecord();

    Schema arrayMapSchema = schema.getField("d_map").schema();
    Schema arrayItemSchema = arrayMapSchema.getTypes().get(1).getValueType();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);

        if (i % 2 == 0) {
          Map<String, String> c_map = new HashMap<>();
          c_map.put("key1", "nested_1_string_" +  i);
          c_map.put("key2", "nested_1_string_" +  (i + 1 ));
          record.put("c_map", c_map);
        } else {
          Map<String, GenericArray<Double>> d_map = new HashMap<>();
          GenericArray<Double> array = new GenericData.Array<>(ARRAY_SIZE, arrayItemSchema);
          for (int j = 0; j < ARRAY_SIZE; j++) {
            array.add((double)j);
          }
          d_map.put("key1", array);
          d_map.put("key2", array);

          record.put("d_map", d_map);
        }
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateUnionNestedSchema_withNullValues() throws Exception {
    File file = File.createTempFile("avro-nested-with-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_record").type().optional().record("my_record_1")
        .namespace("foo.blah.org").fields()
        .name("nested_1_string").type().optional().stringType()
        .name("nested_1_int").type().optional().intType()
        .endRecord()
      .endRecord();

    Schema nestedSchema = schema.getField("c_record").schema();
    Schema optionalSchema = nestedSchema.getTypes().get(1);

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);

        if (i % 2 == 0) {
          GenericRecord nestedRecord = new GenericData.Record(optionalSchema);
          nestedRecord.put("nested_1_string", "nested_1_string_" +  i);
          nestedRecord.put("nested_1_int", i * i);
          record.put("c_record", nestedRecord);
        }
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateDoubleNestedSchema_NoNullValues() throws Exception {
    File file = File.createTempFile("avro-double-nested-no-nulls-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringType().noDefault()
      .name("b_int").type().intType().noDefault()
      .name("c_record").type().record("my_record_1")
        .namespace("foo.blah.org")
        .fields()
        .name("nested_1_string").type().stringType().noDefault()
        .name("nested_1_int").type().intType().noDefault()
        .name("nested_1_record").type().record("my_double_nested_record_1")
          .namespace("foo.blah.org.rot")
          .fields()
          .name("double_nested_1_string").type().stringType().noDefault()
          .name("double_nested_1_int").type().intType().noDefault()
          .endRecord()
          .noDefault()
        .endRecord()
        .noDefault()
      .endRecord();

    Schema nestedSchema = schema.getField("c_record").schema();
    Schema doubleNestedSchema = nestedSchema.getField("nested_1_record").schema();

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_int", i);

        GenericRecord nestedRecord = new GenericData.Record(nestedSchema);
        nestedRecord.put("nested_1_string", "nested_1_string_" +  i);
        nestedRecord.put("nested_1_int", i * i);

        GenericRecord doubleNestedRecord = new GenericData.Record(doubleNestedSchema);
        doubleNestedRecord.put("double_nested_1_string", "double_nested_1_string_" + i + "_" + i);
        doubleNestedRecord.put("double_nested_1_int", i * i * i);

        nestedRecord.put("nested_1_record", doubleNestedRecord);
        record.put("c_record", nestedRecord);

        record.endRecord();
      }
      return record;
    }
  }

  public String generateLinkedList(int numRows) throws Exception {
    File file = File.createTempFile("avro-linked-list-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("LongList")
      .namespace("org.apache.drill.exec.store.avro")
      .aliases("LinkedLongs")
      .fields()
      .name("value").type().optional().longType()
      .name("next").type().optional().type("LongList")
      .endRecord();

    try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
      writer.create(schema, file);
      GenericRecord previousRecord = null;
      for (int i = 0; i < numRows; i++) {
        GenericRecord record = (GenericRecord) (previousRecord == null ? new GenericData.Record(schema) : previousRecord.get("next"));
        record.put("value", (long) i);
        if (previousRecord != null) {
          writer.append(previousRecord);
        }
        GenericRecord nextRecord = new GenericData.Record(record.getSchema());
        record.put("next", nextRecord);
        previousRecord = record;
      }
      writer.append(previousRecord);
    }
    return file.getName();
  }

  public AvroTestRecordWriter generateStringAndUtf8Data() throws Exception {
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("a_string").type().stringBuilder().prop("avro.java.string", "String").endString().noDefault()
      .name("b_utf8").type().stringType().noDefault()
      .endRecord();

    File file = File.createTempFile("avro-string-utf8-test", ".avro", dirTestWatcher.getRootDir());
    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();
        record.put("a_string", "a_" + i);
        record.put("b_utf8", "b_" + i);
        record.endRecord();
      }
      return record;
    }
  }

  public AvroTestRecordWriter generateMapSchema() throws Exception {
    File file = File.createTempFile("avro-map-test", ".avro", dirTestWatcher.getRootDir());
    Schema schema = SchemaBuilder.record("AvroRecordReaderTest")
      .namespace("org.apache.drill.exec.store.avro")
      .fields()
      .name("map_field").type().optional().map().values(Schema.create(Type.LONG))
      .name("map_array").type().optional().array().items(Schema.createMap(Schema.create(Type.INT)))
      .name("map_array_value").type().optional().map().values(Schema.createArray(Schema.create(Type.DOUBLE)))
      .endRecord();

    Schema mapArraySchema = schema.getField("map_array").schema();
    Schema arrayItemSchema = mapArraySchema.getTypes().get(1);

    try (AvroTestRecordWriter record = new AvroTestRecordWriter(schema, file)) {
      for (int i = 0; i < RECORD_COUNT; i++) {
        record.startRecord();

        // Create map with long values
        Map<String, Long> map = new HashMap<>();
        map.put("key1", (long) i);
        map.put("key2", (long) i + 1);
        record.put("map_field", map);

        // Create list of map with int values
        GenericArray<Map<String, Integer>> array = new GenericData.Array<>(ARRAY_SIZE, arrayItemSchema);
        for (int j = 0; j < ARRAY_SIZE; j++) {
          Map<String, Integer> mapInt = new HashMap<>();
          mapInt.put("key1", (i + 1) * (j + 50));
          mapInt.put("key2", (i + 1) * (j + 100));
          array.add(mapInt);
        }
        record.put("map_array", array);

        // create map with array value
        Map<String, GenericArray<Double>> mapArrayValue = new HashMap<>();
        GenericArray<Double> doubleArray = new GenericData.Array<>(ARRAY_SIZE, arrayItemSchema);
        for (int j = 0; j < ARRAY_SIZE; j++) {
          doubleArray.add((double) (i + 1) * j);
        }
        mapArrayValue.put("key1", doubleArray);
        mapArrayValue.put("key2", doubleArray);
        record.put("map_array_value", mapArrayValue);

        record.endRecord();
      }
      return record;
    }
  }

  public String generateDecimalData(int numRecords) throws Exception {
    File file = File.createTempFile("avro-decimal-test", ".avro", dirTestWatcher.getRootDir());
    Schema decBytes = LogicalTypes.decimal(10, 2)
      .addToSchema(SchemaBuilder.builder().bytesType());

    Schema decFixed = LogicalTypes.decimal(5, 2)
      .addToSchema(SchemaBuilder.builder().fixed("dec_fixed").size(5));

    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_dec_pos_bytes").type(decBytes).noDefault()
      .name("col_dec_neg_bytes").type(decBytes).noDefault()
      .name("col_dec_pos_fixed").type(decFixed).noDefault()
      .name("col_dec_neg_fixed").type(decFixed).noDefault()
      .endRecord();

    try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
      writer.create(schema, file);
      for (int i = 0; i < numRecords; i++) {
        GenericRecord record = new GenericData.Record(schema);

        ByteBuffer posBytes = ByteBuffer.wrap(BigInteger.valueOf(100 + i).toByteArray());
        record.put("col_dec_pos_bytes", posBytes);

        ByteBuffer negBytes = ByteBuffer.wrap(BigInteger.valueOf(-200 + i).toByteArray());
        record.put("col_dec_neg_bytes", negBytes);

        byte[] posFixedBytes = new byte[5];
        byte[] posValueBytes = BigInteger.valueOf(300 + i).toByteArray();
        int posDiff = posFixedBytes.length - posValueBytes.length;
        assert posDiff > -1;
        System.arraycopy(posValueBytes, 0, posFixedBytes, posDiff, posValueBytes.length);
        Arrays.fill(posFixedBytes, 0, posDiff, (byte) 0);

        GenericData.Fixed posFixed = new GenericData.Fixed(decFixed, posFixedBytes);
        record.put("col_dec_pos_fixed", posFixed);

        byte[] negFixedBytes = new byte[5];
        byte[] negValueBytes = BigInteger.valueOf(-400 + i).toByteArray();
        int negDiff = negFixedBytes.length - negValueBytes.length;
        assert negDiff > -1;
        System.arraycopy(negValueBytes, 0, negFixedBytes, negDiff, negValueBytes.length);
        Arrays.fill(negFixedBytes, 0, negDiff, (byte) -1);

        GenericData.Fixed negFixed = new GenericData.Fixed(decFixed, negFixedBytes);
        record.put("col_dec_neg_fixed", negFixed);

        writer.append(record);
      }
    }
    return file.getName();
  }

  public String generateDateTimeData(LocalDateTime dateTime) throws Exception {
    File file = File.createTempFile("avro-date-time-test", ".avro", dirTestWatcher.getRootDir());

    Schema timestampMillis = LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder().longType());
    Schema timestampMicros = LogicalTypes.timestampMicros().addToSchema(SchemaBuilder.builder().longType());
    Schema date = LogicalTypes.date().addToSchema(SchemaBuilder.builder().intType());
    Schema timeMillis = LogicalTypes.timeMillis().addToSchema(SchemaBuilder.builder().intType());
    Schema timeMicros = LogicalTypes.timeMicros().addToSchema(SchemaBuilder.builder().longType());

    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_timestamp_millis").type(timestampMillis).noDefault()
      .name("col_timestamp_micros").type(timestampMicros).noDefault()
      .name("col_date").type(date).noDefault()
      .name("col_time_millis").type(timeMillis).noDefault()
      .name("col_time_micros").type(timeMicros).noDefault()
      .endRecord();

    try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
      writer.create(schema, file);
      GenericRecord record = new GenericData.Record(schema);
      long timestampMillisValue = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
      record.put("col_timestamp_millis", timestampMillisValue);
      record.put("col_timestamp_micros", timestampMillisValue * 1000);
      record.put("col_date", dateTime.toLocalDate().toEpochDay());
      long startOfDayMillis = dateTime.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
      long timeMillisValue = timestampMillisValue - startOfDayMillis;
      record.put("col_time_millis", timeMillisValue);
      record.put("col_time_micros", timeMillisValue * 1000);
      writer.append(record);
    }
    return file.getName();
  }

  public String generateDuration(int numRows) throws Exception {
    File file = File.createTempFile("avro-duration-test", ".avro", dirTestWatcher.getRootDir());

    Schema durationSchema = new LogicalType("duration")
      .addToSchema(SchemaBuilder.builder().fixed("duration_fixed").size(12));

    Schema schema = SchemaBuilder.record("record")
      .fields()
      .name("col_duration").type(durationSchema).noDefault()
      .endRecord();

    try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
      writer.create(schema, file);
      for (int i = 0; i < numRows; i++) {
        GenericRecord record = new GenericData.Record(schema);

        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(10 + i); // month
        bb.putInt(100 + i); // days
        bb.putInt(1000 + i); // milliseconds

        GenericData.Fixed fixed = new GenericData.Fixed(durationSchema, bb.array());
        record.put("col_duration", fixed);
        writer.append(record);
      }
    }
    return file.getName();
  }

  public String generateMultiDimensionalArray(int numRecords, int arraySize) throws Exception {
    File file = File.createTempFile("avro-multi-dimensional-array-test", ".avro", dirTestWatcher.getRootDir());

    String colTwoDimsName = "col_array_two_dims";
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name(colTwoDimsName).type()
        .array().items()
        .array().items()
        .stringType()
        .noDefault()
      .endRecord();

    try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
      writer.create(schema, file);

      for (int i = 0; i < numRecords; i++) {
        GenericRecord record = new GenericData.Record(schema);
        Schema twoDimsSchema = schema.getField(colTwoDimsName).schema();
        GenericArray<GenericArray<String>> arrayTwoDims = new GenericData.Array<>(numRecords, twoDimsSchema);
        for (int a = 0; a < arraySize; a++) {
          GenericArray<String> nestedArray = new GenericData.Array<>(2, twoDimsSchema.getElementType());
          nestedArray.add(String.format("val_%s_%s_0", i, a));
          nestedArray.add(String.format("val_%s_%s_1", i, a));
          arrayTwoDims.add(nestedArray);
        }
        record.put(colTwoDimsName, arrayTwoDims);
        writer.append(record);
      }
    }

    return file.getName();
  }
}
