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
package org.apache.drill.metastore.iceberg.write;

import org.apache.drill.metastore.iceberg.IcebergBaseTest;
import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Tables;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.hadoop.HadoopTables;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.types.Types;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestParquetFileWriter extends IcebergBaseTest {

  private static Tables tables;

  @BeforeClass
  public static void init() {
    tables = new HadoopTables(baseHadoopConfig());
  }

  @Test
  public void testAllTypes() throws Exception {
    Schema schema = new Schema(
      Types.NestedField.optional(1, "int_field", Types.IntegerType.get()),
      Types.NestedField.optional(2, "long_field", Types.LongType.get()),
      Types.NestedField.optional(3, "float_field", Types.FloatType.get()),
      Types.NestedField.optional(4, "double_field", Types.DoubleType.get()),
      Types.NestedField.optional(5, "string_field", Types.StringType.get()),
      Types.NestedField.optional(6, "boolean_field", Types.BooleanType.get()),
      Types.NestedField.optional(7, "list_field", Types.ListType.ofOptional(
        9, Types.StringType.get())),
      Types.NestedField.optional(8, "map_field", Types.MapType.ofOptional(
        10, 11, Types.StringType.get(), Types.FloatType.get())));

    List<String> listValue = Arrays.asList("a", "b", "c");

    Map<String, Float> mapValue = new HashMap<>();
    mapValue.put("a", 0.1F);
    mapValue.put("b", 0.2F);

    Record record = GenericRecord.create(schema);
    record.setField("int_field", 1);
    record.setField("long_field", 100L);
    record.setField("float_field", 0.5F);
    record.setField("double_field", 1.5D);
    record.setField("string_field", "abc");
    record.setField("boolean_field", true);
    record.setField("list_field", listValue);
    record.setField("map_field", mapValue);

    String location = defaultFolder.newFolder("testAllTypes").toURI().getPath();
    String fileName = "allTypes";
    Table table = tables.create(schema, location);

    org.apache.drill.metastore.iceberg.write.File result = new ParquetFileWriter(table)
      .records(Collections.singletonList(record))
      .location(location)
      .name(fileName)
      .write();

    String writePath = new Path(location, FileFormat.PARQUET.addExtension(fileName)).toUri().getPath();
    assertEquals(new Path( FileFormat.PARQUET.addExtension(writePath)), new Path(result.location()));
    assertEquals(Long.valueOf(1), result.metrics().recordCount());

    List<Record> rows = readData(result.input(), schema);

    assertEquals(1, rows.size());

    Record row = rows.get(0);
    assertEquals(1, row.getField("int_field"));
    assertEquals(100L, row.getField("long_field"));
    assertEquals(0.5F, row.getField("float_field"));
    assertEquals(1.5D, row.getField("double_field"));
    assertEquals("abc", row.getField("string_field"));
    assertEquals(true, row.getField("boolean_field"));
    assertEquals(listValue, row.getField("list_field"));
    assertEquals(mapValue, row.getField("map_field"));
  }

  @Test
  public void testNullAndEmptyValues() throws Exception {
    Schema schema = new Schema(
      Types.NestedField.optional(1, "int_null_field", Types.IntegerType.get()),
      Types.NestedField.optional(2, "string_null_field", Types.StringType.get()),
      Types.NestedField.optional(3, "string_empty_field", Types.StringType.get()),
      Types.NestedField.optional(4, "boolean_null_field", Types.BooleanType.get()),
      Types.NestedField.optional(5, "list_null_field", Types.ListType.ofOptional(
        9, Types.StringType.get())),
      Types.NestedField.optional(6, "list_empty_field", Types.ListType.ofOptional(
        10, Types.StringType.get())),
      Types.NestedField.optional(7, "map_null_field", Types.MapType.ofOptional(
      11, 12, Types.StringType.get(), Types.FloatType.get())),
    Types.NestedField.optional(8, "map_empty_field", Types.MapType.ofOptional(
      13, 14, Types.StringType.get(), Types.FloatType.get())));

    Record record = GenericRecord.create(schema);
    record.setField("int_null_field", null);
    record.setField("string_null_field", null);
    record.setField("string_empty_field", "");
    record.setField("boolean_null_field", null);
    record.setField("list_null_field", null);
    record.setField("list_empty_field", Collections.emptyList());
    record.setField("map_null_field", null);
    record.setField("map_empty_field", Collections.emptyMap());

    String location = defaultFolder.newFolder("testNullAndEmptyValues").toURI().getPath();
    Table table = tables.create(schema, location);

    org.apache.drill.metastore.iceberg.write.File result = new ParquetFileWriter(table)
      .records(Collections.singletonList(record))
      .location(location)
      .name("nullEmptyValues")
      .write();

    assertEquals(Long.valueOf(1), result.metrics().recordCount());

    List<Record> rows = readData(result.input(), schema);
    assertEquals(1, rows.size());

    Record row = rows.get(0);
    assertNull(row.getField("int_null_field"));
    assertNull(row.getField("string_null_field"));
    assertEquals("", row.getField("string_empty_field"));
    assertNull(row.getField("boolean_null_field"));
    assertNull(row.getField("list_null_field"));
    assertEquals(Collections.emptyList(), row.getField("list_empty_field"));
    assertNull(row.getField("map_null_field"));
    assertEquals(Collections.emptyMap(), row.getField("map_empty_field"));
  }

  @Test
  public void testEmptyFile() throws Exception {
    Schema schema = new Schema(
      Types.NestedField.optional(1, "int_field", Types.IntegerType.get()));

    String location = defaultFolder.newFolder("testEmptyFile").toURI().getPath();
    Table table = tables.create(schema, location);

    org.apache.drill.metastore.iceberg.write.File result = new ParquetFileWriter(table)
      .location(location)
      .name("emptyFile")
      .write();

    assertEquals(Long.valueOf(0), result.metrics().recordCount());

    List<Record> rows = readData(result.input(), schema);
    assertEquals(0, rows.size());
  }

  @Test
  public void testSeveralRecords() throws Exception {
    int fieldIndex = 1;
    Schema schema = new Schema(
      Types.NestedField.optional(fieldIndex, "int_field", Types.IntegerType.get()));

    List<Integer> values = Arrays.asList(1, 2, 3, 3, null, null, null);

    List<Record> records = values.stream()
      .map(value -> {
        Record record = GenericRecord.create(schema);
        record.setField("int_field", value);
        return record;
      })
      .collect(Collectors.toList());

    String location = defaultFolder.newFolder("testSeveralRecords").toURI().getPath();
    Table table = tables.create(schema, location);

    org.apache.drill.metastore.iceberg.write.File result = new ParquetFileWriter(table)
      .records(records)
      .location(location)
      .name("severalRecords")
      .write();

    assertEquals(Long.valueOf(7), result.metrics().recordCount());
    assertEquals(Long.valueOf(7), result.metrics().valueCounts().get(fieldIndex));
    assertEquals(Long.valueOf(3), result.metrics().nullValueCounts().get(fieldIndex));

    List<Record> rows = readData(result.input(), schema);
    assertEquals(7, rows.size());
    List<Integer> actual = rows.stream()
      .map(row -> (Integer) row.getField("int_field"))
      .collect(Collectors.toList());
    assertEquals(values, actual);
  }

  @Test
  public void testTypeMismatch() throws Exception {
    Schema schema = new Schema(
      Types.NestedField.optional(1, "int_field", Types.IntegerType.get()));

    Record record = GenericRecord.create(schema);
    record.setField("int_field", 1);
    record.setField("int_field", "abc");

    String location = defaultFolder.newFolder("testTypeMismatch").toURI().getPath();
    Table table = tables.create(schema, location);

    thrown.expect(IcebergMetastoreException.class);

    new ParquetFileWriter(table)
      .records(Collections.singletonList(record))
      .location(location)
      .name("typeMismatch")
      .write();
  }

  @Test
  public void testWriteIntoExistingFile() throws Exception {
    Schema schema = new Schema(
      Types.NestedField.optional(1, "int_field", Types.IntegerType.get()));

    Record record = GenericRecord.create(schema);
    record.setField("int_field", 1);

    String fileName = "existingFile";
    String location = defaultFolder.newFolder("testWriteIntoExistingFile").toURI().getPath();
    Table table = tables.create(schema, location);

    java.nio.file.Path file = Paths.get(new File(location, FileFormat.PARQUET.addExtension(fileName)).getPath());
    Files.write(file, Collections.singletonList("abc"));

    thrown.expect(IcebergMetastoreException.class);

    new ParquetFileWriter(table)
      .records(Collections.singletonList(record))
      .location(location)
      .name(fileName)
      .write();
  }

  private List<Record> readData(InputFile inputFile, Schema schema) throws IOException {
    try (CloseableIterable<Record> reader = Parquet.read(inputFile)
      .project(schema)
      .createReaderFunc(fileSchema -> GenericParquetReaders.buildReader(schema, fileSchema))
      .build()) {
      return Lists.newArrayList(reader);
    }
  }
}
