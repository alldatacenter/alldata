/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.netease.arctic.spark;

import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestCreateKeyedTableAsSelect extends SparkTestBase {

  private final String database = "db_def";
  private final String table = "testA";
  private final String sourceTable = "test_table";
  private final String duplicateDataTable= "dt_table";
  private final TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);

  @Before
  public void prepare() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists " + database);
    sql("create table {0}.{1} ( \n" +
        " id int , data string, pt string ) using arctic \n" +
        " partitioned by (pt) \n" , database, sourceTable);

    sql("insert overwrite {0}.{1} values \n" +
            "( 1, ''aaaa'', ''0001''), \n" +
            "( 2, ''aaaa'', ''0001''), \n" +
            "( 3, ''aaaa'', ''0001''), \n" +
            "( 4, ''aaaa'', ''0001''), \n" +
            "( 5, ''aaaa'', ''0002''), \n" +
            "( 6, ''aaaa'', ''0002''), \n" +
            "( 7, ''aaaa'', ''0002''), \n" +
            "( 8, ''aaaa'', ''0002'') \n" ,
        database, sourceTable);
  }

  @After
  public void removeTables() {
    sql("DROP TABLE IF EXISTS {0}.{1}", database, sourceTable);
    sql("DROP TABLE IF EXISTS {0}.{1}", database, table);
  }

  @Test
  public void testPrimaryKeyCTAS() {
    sql("create table {0}.{1} primary key(id) using arctic  AS SELECT * from {2}.{3}.{4}",
        database, table, catalogNameArctic, database, sourceTable);
    assertTableExist(identifier);
    sql("desc table {0}.{1}", database, table);
    assertDescResult(rows, Lists.newArrayList("id"));
    Schema expectedSchema = new Schema(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "pt", Types.StringType.get())
    );
    Assert.assertEquals("Should have expected nullable schema",
        expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());
    Assert.assertEquals("Should be an unpartitioned table",
        0, loadTable(identifier).spec().fields().size());
    assertEquals("Should have rows matching the source table",
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));
  }

  @Test
  public void testPrimaryKeyCTASHasDuplicateData() {
    sql("insert into {0}.{1} values \n" +
                    "( 1, ''aaaa'', ''0001'')",
            database, sourceTable);
    sql("select * from {0}.{1} group by id, data, pt", database, sourceTable);
    boolean condition = false;
    try {
      sql("create table {0}.{1} primary key(id) using arctic AS SELECT * from {2}.{3}.{4}",
          database, table, catalogNameArctic, database, sourceTable);
    } catch(UnsupportedOperationException e) {
      condition = true;
    }
    Assert.assertTrue(condition);
  }

  @Test
  public void testPartitionedCTAS() {
    sql("CREATE TABLE {0}.{1} USING arctic PARTITIONED BY (id) AS SELECT * FROM {2}.{3} ORDER BY id",
        database, table, database, sourceTable);

    Schema expectedSchema = new Schema(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "pt", Types.StringType.get())
    );

    PartitionSpec expectedSpec = PartitionSpec.builderFor(expectedSchema)
        .identity("id")
        .build();


    Assert.assertEquals("Should have expected nullable schema",
        expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());
    Assert.assertEquals("Should be partitioned by id",
        expectedSpec, loadTable(identifier).spec());
    assertEquals("Should have rows matching the source table",
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));
  }

  @Test
  public void testPropertiesCTAS() {
    sql("CREATE TABLE {0}.{1} USING arctic TBLPROPERTIES (''prop1''=''val1'', ''prop2''=''val2'')" +
        "AS SELECT * FROM {2}.{3}", database, table, database, sourceTable);

    assertEquals("Should have rows matching the source table",
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));

    Schema expectedSchema = new Schema(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "pt", Types.StringType.get())
    );

    Assert.assertEquals("Should have expected nullable schema",
        expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());

    Assert.assertEquals("Should have updated table property",
        "val1", loadTable(identifier).properties().get("prop1"));
    Assert.assertEquals("Should have preserved table property",
        "val2", loadTable(identifier).properties().get("prop2"));
  }

  @Test
  public void testCTASWithPKAndPartition() {
    sql("CREATE TABLE {0}.{1} primary key(id) USING arctic PARTITIONED BY (pt)" +
        "AS SELECT * FROM {2}.{3}", database, table, database, sourceTable);

    List<Object[]> expect = sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable);
    List<Object[]> acture = sql("SELECT * FROM {0}.{1} ORDER BY id", database, table);
    sql("SELECT * FROM {0}.{1}.change ORDER BY id", database, table);

    assertEquals("Should have rows matching the source table",
        acture,
        expect);

    Schema expectedSchema = new Schema(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "pt", Types.StringType.get())
    );

    sql("desc table {0}.{1}", database, table);
    assertDescResult(rows, Lists.newArrayList("id"));
    Assert.assertEquals("Should have expected nullable schema",
        expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());

    PartitionSpec expectedSpec = PartitionSpec.builderFor(expectedSchema)
        .identity("pt")
        .build();
    Assert.assertEquals("Should be partitioned by pt",
        expectedSpec, loadTable(identifier).spec());
  }

  @Test
  public void testCTASWriteToBase() {
    sql("create table {0}.{1} primary key(id) using arctic  AS SELECT * from {2}.{3}.{4}",
        database, table, catalogNameArctic, database, sourceTable);
    assertTableExist(identifier);
    Schema expectedSchema = new Schema(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "pt", Types.StringType.get())
    );
    Assert.assertEquals("Should have expected nullable schema",
        expectedSchema.asStruct(), loadTable(identifier).schema().asStruct());
    Assert.assertEquals("Should be an unpartitioned table",
        0, loadTable(identifier).spec().fields().size());
    assertEquals("Should have rows matching the source table",
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, table),
        sql("SELECT * FROM {0}.{1} ORDER BY id", database, sourceTable));
    rows = sql("select * from {0}.{1}.change", database, table);
    Assert.assertEquals(0, rows.size());
  }
}
