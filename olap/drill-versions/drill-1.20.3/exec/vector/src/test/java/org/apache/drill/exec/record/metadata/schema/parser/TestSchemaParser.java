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
package org.apache.drill.exec.record.metadata.schema.parser;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DictColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSchemaParser extends BaseTest {

  @Test
  public void checkQuotedIdWithEscapes() throws Exception {
    String schemaWithEscapes = "`a\\\\b\\`c` INT";
    assertEquals(schemaWithEscapes, SchemaExprParser.parseSchema(schemaWithEscapes).metadata(0).columnString());

    String schemaWithKeywords = "`INTEGER` INT";
    assertEquals(schemaWithKeywords, SchemaExprParser.parseSchema(schemaWithKeywords).metadata(0).columnString());
  }

  @Test
  public void testSchemaWithParen() throws Exception {
    String schemaWithParen = "(`a` INT NOT NULL, `b` VARCHAR(10))";
    TupleMetadata schema = SchemaExprParser.parseSchema(schemaWithParen);
    assertEquals(2, schema.size());
    assertEquals("`a` INT NOT NULL", schema.metadata("a").columnString());
    assertEquals("`b` VARCHAR(10)", schema.metadata("b").columnString());
  }

  @Test
  public void testSkip() throws Exception {
    String schemaString = "id\n/*comment*/int\r,//comment\r\nname\nvarchar\t\t\t";
    TupleMetadata schema = SchemaExprParser.parseSchema(schemaString);
    assertEquals(2, schema.size());
    assertEquals("`id` INT", schema.metadata("id").columnString());
    assertEquals("`name` VARCHAR", schema.metadata("name").columnString());
  }

  @Test
  public void testCaseInsensitivity() throws Exception {
    String schema = "`Id` InTeGeR NoT NuLl";
    assertEquals("`Id` INT NOT NULL", SchemaExprParser.parseSchema(schema).metadata(0).columnString());
  }

  @Test
  public void testParseColumn() throws Exception {
    ColumnMetadata column = SchemaExprParser.parseColumn("col int not null");
    assertEquals("`col` INT NOT NULL", column.columnString());
  }

  @Test
  public void testNumericTypes() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addNullable("int_col", TypeProtos.MinorType.INT)
      .add("integer_col", TypeProtos.MinorType.INT)
      .addNullable("bigint_col", TypeProtos.MinorType.BIGINT)
      .add("float_col", TypeProtos.MinorType.FLOAT4)
      .addNullable("double_col", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    checkSchema("int_col int, integer_col integer not null, bigint_col bigint, " +
        "float_col float not null, double_col double", schema);
  }

  @Test
  public void testDecimalTypes() {
    TupleMetadata schema = new SchemaBuilder()
      .addNullable("col", TypeProtos.MinorType.VARDECIMAL)
      .add("col_p", TypeProtos.MinorType.VARDECIMAL, 5)
      .addDecimal("col_ps", TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 10, 2)
      .buildSchema();

    List<String> schemas = Arrays.asList(
      "col dec, col_p dec(5) not null, col_ps dec(10, 2)",
      "col decimal, col_p decimal(5) not null, col_ps decimal(10, 2)",
      "col numeric, col_p numeric(5) not null, col_ps numeric(10, 2)"
    );

    schemas.forEach(
      s -> {
        try {
          checkSchema(s, schema);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    );
  }

  @Test
  public void testBooleanType() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addNullable("col", TypeProtos.MinorType.BIT)
      .buildSchema();

    checkSchema("col boolean", schema);
  }

  @Test
  public void testCharacterTypes() {
    String schemaPattern = "col %1$s, col_p %1$s(50) not null";

    Map<String, TypeProtos.MinorType> properties = new HashMap<>();
    properties.put("char", TypeProtos.MinorType.VARCHAR);
    properties.put("character", TypeProtos.MinorType.VARCHAR);
    properties.put("character varying", TypeProtos.MinorType.VARCHAR);
    properties.put("varchar", TypeProtos.MinorType.VARCHAR);
    properties.put("binary", TypeProtos.MinorType.VARBINARY);
    properties.put("varbinary", TypeProtos.MinorType.VARBINARY);

    properties.forEach((key, value) -> {

      TupleMetadata schema = new SchemaBuilder()
        .addNullable("col", value)
        .add("col_p", value, 50)
        .buildSchema();

      try {
        checkSchema(String.format(schemaPattern, key), schema);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testTimeTypes() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addNullable("time_col", TypeProtos.MinorType.TIME)
      .addNullable("time_prec_col", TypeProtos.MinorType.TIME, 3)
      .add("date_col", TypeProtos.MinorType.DATE)
      .addNullable("timestamp_col", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("timestamp_prec_col", TypeProtos.MinorType.TIMESTAMP, 3)
      .buildSchema();

    checkSchema("time_col time, time_prec_col time(3), date_col date not null, " +
        "timestamp_col timestamp, timestamp_prec_col timestamp(3)", schema);
  }

  @Test
  public void testInterval() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addNullable("interval_year_col", TypeProtos.MinorType.INTERVALYEAR)
      .addNullable("interval_month_col", TypeProtos.MinorType.INTERVALYEAR)
      .addNullable("interval_day_col", TypeProtos.MinorType.INTERVALDAY)
      .addNullable("interval_hour_col", TypeProtos.MinorType.INTERVALDAY)
      .addNullable("interval_minute_col", TypeProtos.MinorType.INTERVALDAY)
      .addNullable("interval_second_col", TypeProtos.MinorType.INTERVALDAY)
      .addNullable("interval_col", TypeProtos.MinorType.INTERVAL)
      .buildSchema();

    checkSchema("interval_year_col interval year, interval_month_col interval month, " +
        "interval_day_col interval day, interval_hour_col interval hour, interval_minute_col interval minute, " +
        "interval_second_col interval second, interval_col interval", schema);
  }

  @Test
  public void testAdditionalParquetTypes() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addNullable("uint1_col", TypeProtos.MinorType.UINT1)
      .addNullable("uint2_col", TypeProtos.MinorType.UINT2)
      .addNullable("uint4_col", TypeProtos.MinorType.UINT4)
      .addNullable("uint8_col", TypeProtos.MinorType.UINT8)
      .addNullable("tinyint_col", TypeProtos.MinorType.TINYINT)
      .addNullable("smallint_col", TypeProtos.MinorType.SMALLINT)
      .buildSchema();

    checkSchema("uint1_col uint1, uint2_col uint2, uint4_col uint4, uint8_col uint8, " +
      "tinyint_col tinyint, smallint_col smallint", schema);
  }

  @Test
  public void testArray() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addArray("simple_array", TypeProtos.MinorType.INT)
      .addRepeatedList("nested_array")
        .addArray(TypeProtos.MinorType.INT)
      .resumeSchema()
      .addMapArray("struct_array")
        .addNullable("s1", TypeProtos.MinorType.INT)
        .addNullable("s2", TypeProtos.MinorType.VARCHAR)
      .resumeSchema()
      .addRepeatedList("nested_array_struct")
        .addMapArray()
          .addNullable("ns1", TypeProtos.MinorType.INT)
          .addNullable("ns2", TypeProtos.MinorType.VARCHAR)
        .resumeList()
      .resumeSchema()
      .addDictArray("map_array", TypeProtos.MinorType.VARCHAR)
        .nullableValue(TypeProtos.MinorType.INT)
      .resumeSchema()
      .addRepeatedList("nested_map_array")
        .addDictArray()
          .key(TypeProtos.MinorType.VARCHAR)
          .nullableValue(TypeProtos.MinorType.INT)
        .resumeList()
      .resumeSchema()
      .addList("union_array")
        .addType(TypeProtos.MinorType.BIGINT)
        .addType(TypeProtos.MinorType.DATE)
      .resumeSchema()
      .buildSchema();

    checkSchema("simple_array array<int>"
        + ", nested_array array<array<int>>"
        + ", struct_array array<struct<s1 int, s2 varchar>>"
        + ", nested_array_struct array<array<struct<ns1 int, ns2 varchar>>>"
        + ", map_array array<map<varchar, int>>"
        + ", nested_map_array array<array<map<varchar, int>>>"
        + ", union_array array<union>",
      schema);
  }

  @Test
  public void testStruct() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addMap("struct_col")
        .addNullable("int_col", TypeProtos.MinorType.INT)
        .addArray("array_col", TypeProtos.MinorType.INT)
        .addMap("nested_struct")
          .addNullable("s1", TypeProtos.MinorType.INT)
          .addNullable("s2", TypeProtos.MinorType.VARCHAR)
        .resumeMap()
        .addDict("map_col", TypeProtos.MinorType.VARCHAR)
          .nullableValue(TypeProtos.MinorType.INT)
        .resumeMap()
        .addUnion("union_col")
          .addType(TypeProtos.MinorType.INT)
        .resumeMap()
      .resumeSchema()
      .buildSchema();

    checkSchema("struct_col struct<int_col int"
      + ", array_col array<int>"
      + ", nested_struct struct<s1 int, s2 varchar>"
      + ", map_col map<varchar, int>"
      + ", union_col union"
      + ">", schema);
  }

  @Test
  public void testMap() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
        .addDict("dict_col_simple", TypeProtos.MinorType.VARCHAR)
          .nullableValue(TypeProtos.MinorType.INT)
        .resumeSchema()
        .addDict("dict_col_simple_ps", TypeProtos.MajorType.newBuilder()
            .setMinorType(TypeProtos.MinorType.VARCHAR)
            .setPrecision(50)
            .setMode(TypeProtos.DataMode.REQUIRED)
            .build())
          .value(TypeProtos.MajorType.newBuilder()
            .setMinorType(TypeProtos.MinorType.VARDECIMAL)
            .setPrecision(10)
            .setScale(2)
            .setMode(TypeProtos.DataMode.REQUIRED)
            .build())
        .resumeSchema()
        .addDict("dict_col_struct", TypeProtos.MinorType.INT)
          .mapValue()
            .add("sb", TypeProtos.MinorType.BIT)
            .addNullable("si", TypeProtos.MinorType.INT)
          .resumeDict()
        .resumeSchema()
        .addDict("dict_col_dict", TypeProtos.MinorType.VARCHAR)
          .dictValue()
            .key(TypeProtos.MinorType.INT)
            .nullableValue(TypeProtos.MinorType.BIT)
          .resumeDict()
        .resumeSchema()
        .addDict("dict_col_array", TypeProtos.MinorType.BIGINT)
          .dictArrayValue()
            .key(TypeProtos.MinorType.DATE)
            .nullableValue(TypeProtos.MinorType.FLOAT8)
          .resumeDict()
        .resumeSchema()
        .addDict("dict_col_union", TypeProtos.MinorType.BIGINT)
          .unionValue()
            .addType(TypeProtos.MinorType.INT)
         .resumeDict()
        .resumeSchema()
      .buildSchema();

    checkSchema("dict_col_simple map<varchar, int>"
      + ", dict_col_simple_ps map<varchar(50), decimal(10, 2) not null>"
      + ", dict_col_struct map<int, struct<sb boolean not null, si int>>"
      + ", dict_col_dict map<varchar, map<int, boolean>>"
      + ", dict_col_array map<bigint, array<map<date, double>>>"
      + ", dict_col_union map<bigint, union>",
      schema);
  }

  @Test
  public void testUnion() throws Exception {
    TupleMetadata schema = new SchemaBuilder()
      .addUnion("col_union_not_null")
        .addType(TypeProtos.MinorType.INT)
        .addType(TypeProtos.MinorType.VARCHAR)
      .resumeSchema()
      .addUnion("col_union_null")
        .addType(TypeProtos.MinorType.INT)
      .resumeSchema()
      .buildSchema();

    checkSchema("col_union_not_null union not null, col_union_null union", schema);
  }

  @Test
  public void testModeForSimpleType() throws Exception {
    TupleMetadata schema = SchemaExprParser.parseSchema("id int not null, name varchar");
    assertFalse(schema.metadata("id").isNullable());
    assertTrue(schema.metadata("name").isNullable());
  }

  @Test
  public void testModeForStructType() throws Exception {
    TupleMetadata schema  = SchemaExprParser.parseSchema("s struct<s1 int not null, s2 varchar>");
    ColumnMetadata struct = schema.metadata("s");
    assertTrue(struct.isMap());
    assertEquals(TypeProtos.DataMode.REQUIRED, struct.mode());

    TupleMetadata mapSchema = struct.tupleSchema();
    assertFalse(mapSchema.metadata("s1").isNullable());
    assertTrue(mapSchema.metadata("s2").isNullable());
  }

  @Test
  public void testModeForMapType() throws Exception {
    TupleMetadata schema  = SchemaExprParser.parseSchema("m1 map<varchar, int>, m2 map<varchar not null, int not null>");

    ColumnMetadata mapOptional = schema.metadata("m1");
    assertTrue(mapOptional.isDict());
    assertEquals(TypeProtos.DataMode.REQUIRED, mapOptional.mode());
    DictColumnMetadata dictOptional = (DictColumnMetadata) mapOptional;
    assertEquals(TypeProtos.DataMode.REQUIRED, dictOptional.keyColumnMetadata().mode());
    assertEquals(TypeProtos.DataMode.OPTIONAL, dictOptional.valueColumnMetadata().mode());

    ColumnMetadata mapRequired = schema.metadata("m2");
    assertTrue(mapRequired.isDict());
    assertEquals(TypeProtos.DataMode.REQUIRED, mapRequired.mode());
    DictColumnMetadata dictRequired = (DictColumnMetadata) mapRequired;
    assertEquals(TypeProtos.DataMode.REQUIRED, dictRequired.keyColumnMetadata().mode());
    assertEquals(TypeProtos.DataMode.REQUIRED, dictRequired.valueColumnMetadata().mode());
  }

  @Test
  public void testModeForRepeatedType() throws Exception {
    TupleMetadata schema = SchemaExprParser.parseSchema("a array<int>"
      + ", aa array<array<int>>"
      + ", sa array<struct<s1 int not null, s2 varchar>>"
      + ", ma array<map<varchar, array<int>>>");

    assertTrue(schema.metadata("a").isArray());

    ColumnMetadata nestedArray = schema.metadata("aa");
    assertTrue(nestedArray.isArray());
    assertTrue(nestedArray.childSchema().isArray());

    ColumnMetadata structArray = schema.metadata("sa");
    assertTrue(structArray.isArray());
    assertTrue(structArray.isMap());
    TupleMetadata structSchema = structArray.tupleSchema();
    assertFalse(structSchema.metadata("s1").isNullable());
    assertTrue(structSchema.metadata("s2").isNullable());

    ColumnMetadata mapArray = schema.metadata("ma");
    assertTrue(mapArray.isArray());
    assertTrue(mapArray.isDict());
    DictColumnMetadata dictMetadata = (DictColumnMetadata) mapArray;
    assertFalse(dictMetadata.keyColumnMetadata().isNullable());
    assertTrue(dictMetadata.valueColumnMetadata().isArray());
  }

  @Test
  public void testFormat() throws Exception {
    String value = "`a` DATE NOT NULL FORMAT 'yyyy-MM-dd'";
    TupleMetadata schema = SchemaExprParser.parseSchema(value);
    ColumnMetadata columnMetadata = schema.metadata("a");
    assertEquals("yyyy-MM-dd", columnMetadata.format());
    assertEquals(value, columnMetadata.columnString());
  }

  @Test
  public void testDefault() throws Exception {
    String value = "`a` INT NOT NULL DEFAULT '12'";
    TupleMetadata schema = SchemaExprParser.parseSchema(value);
    ColumnMetadata columnMetadata = schema.metadata("a");
    assertTrue(columnMetadata.decodeDefaultValue() instanceof Integer);
    assertEquals(12, columnMetadata.decodeDefaultValue());
    assertEquals("12", columnMetadata.defaultValue());
    assertEquals(value, columnMetadata.columnString());
  }

  @Test
  public void testFormatAndDefault() throws Exception {
    String value = "`a` DATE NOT NULL FORMAT 'yyyy-MM-dd' DEFAULT '2018-12-31'";
    TupleMetadata schema = SchemaExprParser.parseSchema(value);
    ColumnMetadata columnMetadata = schema.metadata("a");
    assertTrue(columnMetadata.decodeDefaultValue() instanceof LocalDate);
    assertEquals(LocalDate.of(2018, 12, 31), columnMetadata.decodeDefaultValue());
    assertEquals("2018-12-31", columnMetadata.defaultValue());
    assertEquals(value, columnMetadata.columnString());
  }

  @Test
  public void testColumnProperties() throws Exception {
    String value = "`a` INT NOT NULL PROPERTIES { 'k1' = 'v1', 'k2' = 'v2' }";
    TupleMetadata schema = SchemaExprParser.parseSchema(value);

    ColumnMetadata columnMetadata = schema.metadata("a");

    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("k1", "v1");
    properties.put("k2", "v2");

    assertEquals(properties, columnMetadata.properties());
    assertEquals(value, columnMetadata.columnString());
  }

  @Test
  public void testEmptySchema() throws Exception {
    assertEquals(0, SchemaExprParser.parseSchema("").size());
    assertEquals(0, SchemaExprParser.parseSchema("  ").size());
    assertEquals(0, SchemaExprParser.parseSchema("\n").size());
    assertEquals(0, SchemaExprParser.parseSchema("  \n").size());
  }

  @Test
  public void testEmptySchemaWithParentheses() throws Exception {
    assertEquals(0, SchemaExprParser.parseSchema("()").size());
    assertEquals(0, SchemaExprParser.parseSchema("(  )").size());
    assertEquals(0, SchemaExprParser.parseSchema("(\n)\n").size());
  }

  @Test
  public void testEmptySchemaWithProperties() throws Exception {
    String value = "() properties { `drill.strict` = `false` }";
    TupleMetadata schema = SchemaExprParser.parseSchema(value);
    assertTrue(schema.isEmpty());
    assertEquals("false", schema.property("drill.strict"));
  }

  @Test
  public void testSchemaWithProperties() throws Exception {
    String value = "(col int properties { `drill.blank-as` = `0` } ) " +
      "properties { `drill.strict` = `false`, `drill.my-prop` = `abc` }";
    TupleMetadata schema = SchemaExprParser.parseSchema(value);
    assertEquals(1, schema.size());

    ColumnMetadata column = schema.metadata("col");
    assertEquals("0", column.property("drill.blank-as"));

    assertEquals("false", schema.property("drill.strict"));
    assertEquals("abc", schema.property("drill.my-prop"));
  }

  private void checkSchema(String schemaString, TupleMetadata expectedSchema) throws IOException {
    TupleMetadata actualSchema = SchemaExprParser.parseSchema(schemaString);

    assertEquals(expectedSchema.size(), actualSchema.size());
    assertEquals(expectedSchema.properties(), actualSchema.properties());

    expectedSchema.toMetadataList().forEach(
      expectedMetadata -> {
        ColumnMetadata actualMetadata = actualSchema.metadata(expectedMetadata.name());
        assertEquals(expectedMetadata.columnString(), actualMetadata.columnString());
      }
    );
  }

  @Test
  public void testDynamicColumn() throws IOException {
    String input = "(`a` LATE, `b` DYNAMIC)";
    TupleMetadata schema = SchemaExprParser.parseSchema(input);
    assertEquals(2, schema.size());
    assertEquals("`a` DYNAMIC", schema.metadata("a").columnString());
    assertEquals("`b` DYNAMIC", schema.metadata("b").columnString());
  }
}
