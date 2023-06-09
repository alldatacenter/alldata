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
package org.apache.drill.exec.store.hive.schema;

import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.types.DrillRelDataTypeSystem;
import org.apache.drill.exec.planner.types.HiveToRelDataTypeConverter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.PrimitiveColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.store.hive.HiveUtilities;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

@Category({SlowTest.class})
public class TestSchemaConversion extends BaseTest {
  private static final HiveToRelDataTypeConverter dataTypeConverter
      = new HiveToRelDataTypeConverter(new SqlTypeFactoryImpl(new DrillRelDataTypeSystem()));

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testPrimitiveSchema() {
    verifyConversion("int", Types.optional(TypeProtos.MinorType.INT));
    verifyConversion("varchar(123)", TypeProtos.MajorType.newBuilder().setMinorType(TypeProtos.MinorType.VARCHAR).setPrecision(123).build());
    verifyConversion("timestamp", Types.optional(TypeProtos.MinorType.TIMESTAMP));
  }

  @Test
  public void testStructSchema() {
    ColumnMetadata expectedSchema = new SchemaBuilder()
        .addMap("a")
            .addNullable("t1", TypeProtos.MinorType.BIT)
            .addNullable("t2", TypeProtos.MinorType.INT)
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("struct<t1:boolean,t2:int>", expectedSchema);

    expectedSchema = new SchemaBuilder()
        .addMap("a")
            .addNullable("t1", TypeProtos.MinorType.BIT)
            .addMap("t2")
                .addNullable("t3", TypeProtos.MinorType.VARDECIMAL, 38, 8)
                .resumeMap()
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("struct<t1:boolean,t2:struct<t3:decimal(38,8)>>", expectedSchema);
  }

  @Test
  public void testRepeatedSchema() {
    verifyConversion("array<boolean>", Types.repeated(TypeProtos.MinorType.BIT));

    ColumnMetadata expectedSchema = new SchemaBuilder()
        .addRepeatedList("a")
            .addArray(TypeProtos.MinorType.BIT)
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("array<array<boolean>>", expectedSchema);

    expectedSchema = new SchemaBuilder()
        .addRepeatedList("a")
            .addDimension()
                .addArray(TypeProtos.MinorType.BIT)
                .resumeList()
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("array<array<array<boolean>>>", expectedSchema);
  }

  @Test
  public void testRepeatedStructSchema() {
    ColumnMetadata expectedSchema = new SchemaBuilder()
        .addMapArray("a")
            .addNullable("t1", TypeProtos.MinorType.VARCHAR, 999)
            .addNullable("t2", TypeProtos.MinorType.INT)
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("array<struct<t1:varchar(999),t2:int>>", expectedSchema);

    expectedSchema = new SchemaBuilder()
        .addRepeatedList("a")
            .addMapArray()
                .addNullable("t1", TypeProtos.MinorType.VARCHAR, 999)
                .addNullable("t2", TypeProtos.MinorType.INT)
                .resumeList()
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("array<array<struct<t1:varchar(999),t2:int>>>", expectedSchema);

    expectedSchema = new SchemaBuilder()
        .addRepeatedList("a")
            .addDimension()
                .addMapArray()
                    .addRepeatedList("t1")
                        .addArray(TypeProtos.MinorType.VARCHAR, 999)
                        .resumeMap()
                    .addArray("t2", TypeProtos.MinorType.VARDECIMAL, 28, 14)
                    .resumeList()
                .resumeList()
            .resumeSchema()
        .buildSchema()
        .metadata(0);
    verifyConversion("array<array<array<struct<t1:array<array<varchar(999)>>,t2:array<decimal(28,14)>>>>>", expectedSchema);
  }

  @Test
  public void testUnionSchema() {
    thrown.expect(UnsupportedOperationException.class);
    convertType("uniontype<int,double>");
  }

  @Test
  public void testListUnionSchema() {
    thrown.expect(UnsupportedOperationException.class);
    convertType("array<uniontype<int,double>>");
  }

  @Test
  public void testStructUnionSchema() {
    thrown.expect(UnsupportedOperationException.class);
    convertType("struct<a:uniontype<int,double>,b:int>");
  }

  @Test
  public void testMapSchema() {
    thrown.expect(UnsupportedOperationException.class);
    convertType("map<int,varchar(23)>");
  }

  @Test
  public void testRepeatedMapSchema() {
    thrown.expect(UnsupportedOperationException.class);
    convertType("array<map<int,varchar(23)>>");
  }

  private void verifyConversion(String hiveType, TypeProtos.MajorType type) {
    assertEquals(new PrimitiveColumnMetadata("a", type).columnString(), convertType(hiveType).columnString());
  }

  private void verifyConversion(String hiveType, ColumnMetadata expectedSchema) {
    assertEquals(expectedSchema.columnString(), convertType(hiveType).columnString());
  }

  private ColumnMetadata convertType(String type) {
    return HiveUtilities.getColumnMetadata(dataTypeConverter, new FieldSchema("a", type, ""));
  }
}
