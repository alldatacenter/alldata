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
import org.apache.avro.SchemaBuilder;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AvroSchemaUtilTest extends BaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testExtractSchemaFromNullableNotUnion() {
    Schema schema = SchemaBuilder.builder().stringType();

    thrown.expect(UserException.class);
    thrown.expectMessage("VALIDATION ERROR");

    AvroSchemaUtil.extractSchemaFromNullable(schema, "s");
  }

  @Test
  public void testExtractSchemaFromNullableComplexUnion() {
    Schema schema = SchemaBuilder.unionOf()
      .doubleType().and()
      .longType().and()
      .nullType()
      .endUnion();

    thrown.expect(UserException.class);
    thrown.expectMessage("UNSUPPORTED_OPERATION ERROR");

    AvroSchemaUtil.extractSchemaFromNullable(schema, "u");
  }

  @Test
  public void testExtractSchemaFromNullable() {
    Schema schema = SchemaBuilder.builder().nullable().stringType();
    Schema actual = AvroSchemaUtil.extractSchemaFromNullable(schema, "s");

    assertEquals(SchemaBuilder.builder().stringType(), actual);
  }

  @Test
  public void testConvertSimpleTypes() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .requiredString("col_string")
      .requiredBytes("col_bytes")
      .requiredBoolean("col_boolean")
      .requiredInt("col_int")
      .requiredLong("col_long")
      .requiredFloat("col_float")
      .requiredDouble("col_double")
      .optionalString("col_opt_string")
      .optionalBytes("col_opt_bytes")
      .optionalBoolean("col_opt_boolean")
      .optionalInt("col_opt_int")
      .optionalLong("col_opt_long")
      .optionalFloat("col_opt_float")
      .optionalDouble("col_opt_double")
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .add("col_string", TypeProtos.MinorType.VARCHAR)
      .add("col_bytes", TypeProtos.MinorType.VARBINARY)
      .add("col_boolean", TypeProtos.MinorType.BIT)
      .add("col_int", TypeProtos.MinorType.INT)
      .add("col_long", TypeProtos.MinorType.BIGINT)
      .add("col_float", TypeProtos.MinorType.FLOAT4)
      .add("col_double", TypeProtos.MinorType.FLOAT8)
      .addNullable("col_opt_string", TypeProtos.MinorType.VARCHAR)
      .addNullable("col_opt_bytes", TypeProtos.MinorType.VARBINARY)
      .addNullable("col_opt_boolean", TypeProtos.MinorType.BIT)
      .addNullable("col_opt_int", TypeProtos.MinorType.INT)
      .addNullable("col_opt_long", TypeProtos.MinorType.BIGINT)
      .addNullable("col_opt_float", TypeProtos.MinorType.FLOAT4)
      .addNullable("col_opt_double", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertDecimal() {
    Schema decBytes = LogicalTypes.decimal(10, 2)
      .addToSchema(SchemaBuilder.builder().bytesType());

    Schema decFixed = LogicalTypes.decimal(5, 2)
      .addToSchema(SchemaBuilder.builder().fixed("dec_fixed").size(5));

    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_dec_bytes").type(decBytes).noDefault()
      .name("col_dec_fixed").type(decFixed).noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .add("col_dec_bytes", TypeProtos.MinorType.VARDECIMAL, 10, 2)
      .add("col_dec_fixed", TypeProtos.MinorType.VARDECIMAL, 5, 2)
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertDateTime() {
    Schema timestampMillis = LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder().longType());
    Schema timestampMicros = LogicalTypes.timestampMicros().addToSchema(SchemaBuilder.builder().longType());
    Schema date = LogicalTypes.date().addToSchema(SchemaBuilder.builder().intType());
    Schema timeMillis = LogicalTypes.timeMillis().addToSchema(SchemaBuilder.builder().intType());
    Schema timeMicros = LogicalTypes.timeMicros().addToSchema(SchemaBuilder.builder().longType());
    Schema duration = new LogicalType("duration")
      .addToSchema(SchemaBuilder.builder().fixed("duration_fixed").size(12));

    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_timestamp_millis").type(timestampMillis).noDefault()
      .name("col_timestamp_micros").type(timestampMicros).noDefault()
      .name("col_date").type(date).noDefault()
      .name("col_time_millis").type(timeMillis).noDefault()
      .name("col_time_micros").type(timeMicros).noDefault()
      .name("col_duration").type(duration).noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .add("col_timestamp_millis", TypeProtos.MinorType.TIMESTAMP)
      .add("col_timestamp_micros", TypeProtos.MinorType.TIMESTAMP)
      .add("col_date", TypeProtos.MinorType.DATE)
      .add("col_time_millis", TypeProtos.MinorType.TIME)
      .add("col_time_micros", TypeProtos.MinorType.TIME)
      .add("col_duration", TypeProtos.MinorType.INTERVAL)
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertNullType() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_null").type().nullType().noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .addNullable("col_null", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertEnum() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_enum").type().enumeration("letters").symbols("A", "B", "C").noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .add("col_enum", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertFixed() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_fixed").type().fixed("md5").size(16).noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .add("col_fixed", TypeProtos.MinorType.VARBINARY)
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertArray() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_array").type().array().items().booleanType().noDefault()
      .name("col_opt_array").type().array().items().nullable().longType().noDefault()
      .name("col_nested_array").type().array().items()
        .array().items()
        .stringType()
        .noDefault()
      .name("col_array_map").type().array().items()
          .record("arr_rec")
          .fields()
          .optionalString("s")
          .requiredLong("i")
          .endRecord()
        .noDefault()
      .name("col_array_dict").type().array().items()
        .map().values().intType().noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .addArray("col_array", TypeProtos.MinorType.BIT)
      .addArray("col_opt_array", TypeProtos.MinorType.BIGINT)
      .addArray("col_nested_array", TypeProtos.MinorType.VARCHAR, 2)
      .addMapArray("col_array_map")
        .addNullable("s", TypeProtos.MinorType.VARCHAR)
        .add("i", TypeProtos.MinorType.BIGINT)
        .resumeSchema()
      .addDictArray("col_array_dict", TypeProtos.MinorType.VARCHAR)
        .value(TypeProtos.MinorType.INT)
        .resumeSchema()
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertMap() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_map_simple").type().record("map_simple_rec")
        .fields()
          .optionalInt("i")
          .requiredString("s")
          .endRecord()
          .noDefault()
      .name("col_map_complex").type().record("map_complex_rec")
        .fields()
          .name("col_nested_map").type().record("map_nested_rec")
            .fields()
              .optionalBoolean("nest_b")
              .requiredDouble("nest_d")
              .endRecord()
              .noDefault()
          .name("col_nested_dict").type().map().values().stringType().noDefault()
          .name("col_nested_array").type().array().items().booleanType().noDefault()
          .endRecord()
          .noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .addMap("col_map_simple")
        .addNullable("i", TypeProtos.MinorType.INT)
        .add("s", TypeProtos.MinorType.VARCHAR)
        .resumeSchema()
      .addMap("col_map_complex")
        .addMap("col_nested_map")
          .addNullable("nest_b", TypeProtos.MinorType.BIT)
          .add("nest_d", TypeProtos.MinorType.FLOAT8)
          .resumeMap()
        .addDict("col_nested_dict", TypeProtos.MinorType.VARCHAR)
          .value(TypeProtos.MinorType.VARCHAR)
          .resumeMap()
        .addArray("col_nested_array", TypeProtos.MinorType.BIT)
        .resumeSchema()
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertDict() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .name("col_dict").type().map().values().stringType().noDefault()
      .name("col_opt_dict").type().map().values().nullable().intType().noDefault()
      .name("col_dict_val_array").type().map().values().array().items().stringType().noDefault()
      .name("col_dict_val_dict").type().map().values().map().values().intType().noDefault()
      .name("col_dict_val_map").type().map().values()
        .record("dict_val")
        .fields()
        .optionalInt("i")
        .requiredString("s")
        .endRecord().noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .addDict("col_dict", TypeProtos.MinorType.VARCHAR).value(TypeProtos.MinorType.VARCHAR).resumeSchema()
       .addDict("col_opt_dict", TypeProtos.MinorType.VARCHAR).nullableValue(TypeProtos.MinorType.INT).resumeSchema()
      .addDict("col_dict_val_array", TypeProtos.MinorType.VARCHAR)
        .repeatedValue(TypeProtos.MinorType.VARCHAR).resumeSchema()
      .addDict("col_dict_val_dict", TypeProtos.MinorType.VARCHAR)
        .dictValue()
          .key(TypeProtos.MinorType.VARCHAR)
          .value(TypeProtos.MinorType.INT)
          .resumeDict()
        .resumeSchema()
      .addDict("col_dict_val_map", TypeProtos.MinorType.VARCHAR)
        .mapValue()
          .addNullable("i", TypeProtos.MinorType.INT)
          .add("s", TypeProtos.MinorType.VARCHAR)
          .resumeDict()
        .resumeSchema()
      .buildSchema();

   assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertComplexUnion() {
    Schema schema = SchemaBuilder.record("rec")
      .fields()
      .optionalString("s")
      .name("u").type().unionOf()
      .stringType().and().longType().and().nullType().endUnion().noDefault()
      .endRecord();

    thrown.expect(UserException.class);
    thrown.expectMessage("UNSUPPORTED_OPERATION ERROR");

    AvroSchemaUtil.convert(schema);
  }

  @Test
  public void testConvertWithNamedTypes() {
    Schema schema = SchemaBuilder.record("MixedList")
      .fields()
      .name("rec_l").type().record("LongList")
        .fields()
        .requiredLong("l")
        .name("list_l_1").type("LongList").noDefault()
        .name("list_l_2").type("LongList").noDefault()
        .endRecord()
        .noDefault()
      .name("rec_s").type().record("StringList")
        .fields()
        .requiredString("s")
        .name("list_s_1").type("StringList").noDefault()
        .name("list_s_2").type("StringList").noDefault()
        .endRecord()
        .noDefault()
      .name("rec_m").type().record("rec_m")
        .fields()
        .name("list_l").type("LongList").noDefault()
        .name("list_s").type("StringList").noDefault()
        .name("a").type().array().items().type("MixedList").noDefault()
        .endRecord()
        .noDefault()
      .name("mixed").type("MixedList").noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .addMap("rec_l")
        .add("l", TypeProtos.MinorType.BIGINT)
        .addMap("list_l_1")
          .resumeMap()
        .addMap("list_l_2")
          .resumeMap()
        .resumeSchema()
      .addMap("rec_s")
        .add("s", TypeProtos.MinorType.VARCHAR)
        .addMap("list_s_1")
          .resumeMap()
        .addMap("list_s_2")
          .resumeMap()
        .resumeSchema()
      .addMap("rec_m")
        .addMap("list_l")
          .add("l", TypeProtos.MinorType.BIGINT)
          .addMap("list_l_1")
            .resumeMap()
          .addMap("list_l_2")
            .resumeMap()
          .resumeMap()
        .addMap("list_s")
          .add("s", TypeProtos.MinorType.VARCHAR)
          .addMap("list_s_1")
            .resumeMap()
          .addMap("list_s_2")
            .resumeMap()
          .resumeMap()
        .addMapArray("a")
          .resumeMap()
        .resumeSchema()
      .addMap("mixed")
        .resumeSchema()
      .build();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }

  @Test
  public void testConvertWithNamespaces() {
    Schema schema = SchemaBuilder.record("rec").namespace("n1")
      .fields()
      .requiredString("s")
      .name("m").type().record("rec").namespace("n2")
        .fields()
        .requiredLong("l")
        .endRecord()
        .noDefault()
      .endRecord();

    TupleMetadata tupleMetadata = new org.apache.drill.exec.record.metadata.SchemaBuilder()
      .add("s", TypeProtos.MinorType.VARCHAR)
      .addMap("m")
        .add("l", TypeProtos.MinorType.BIGINT)
        .resumeSchema()
      .buildSchema();

    assertTrue(tupleMetadata.isEquivalent(AvroSchemaUtil.convert(schema)));
  }
}
