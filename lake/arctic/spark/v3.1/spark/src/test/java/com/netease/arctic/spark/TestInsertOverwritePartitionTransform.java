/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import com.netease.arctic.spark.util.RecordGenerator;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TestInsertOverwritePartitionTransform extends SparkTestBase {

  private final String table = "tbl";
  private final String database = "database";

  private final String source = "_tmp_source";
  private final TableIdentifier tableIdent = TableIdentifier.of(catalogNameArctic, database, table);

  private List<GenericRecord> records;
  private final String transform;
  private final String createTableDDL = "CREATE TABLE " + database + "." + table + "( " +
      "id int, \n" +
      "data string, \n" +
      "ts timestamp, \n" +
      "ts_long long, \n" +
      "PRIMARY KEY(id) \n" +
      ") using arctic \n" +
      "PARTITIONED BY ( {0} )";

  @Parameterized.Parameters(name = "Transform = {0}")
  public static List<String> parameters() {
    return Lists.newArrayList(
        "days(ts) ",
        "date(ts) ",
        "months(ts)",
        "years(ts)",
        "hours(ts) ",
        "date_hour(ts)",
        "bucket(8, id)",
        "truncate(86400, ts_long)"
    );
  }

  public TestInsertOverwritePartitionTransform(String transform) {
    this.transform = transform;
  }

  @Before
  public void before() {
    sql("use " + catalogNameArctic);
    sql("CREATE DATABASE IF NOT EXISTS " + database);
    sql(createTableDDL, transform);

    ArcticTable arcticTable = loadTable(tableIdent);
    RecordGenerator recordGenerator = RecordGenerator
        .buildFor(arcticTable.schema())
        .withSequencePrimaryKey(arcticTable.asKeyedTable().primaryKeySpec())
        .build();
    records = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      GenericRecord record = recordGenerator.newRecord();
      records.add(record);
    }
    List<Row> datas = records.stream()
        .map(SparkTestContext::recordToRow)
        .collect(Collectors.toList());

    Dataset<Row> dataset = spark.createDataFrame(datas, SparkSchemaUtil.convert(arcticTable.schema()));
    dataset.registerTempTable(source);
  }

  @After
  public void after() {
    sql("use " + catalogNameArctic);
    sql("DROP TABLE IF EXISTS " + database + "." + table);
    sql("DROP TABLE IF EXISTS " + source);
    sql("DROP DATABASE IF EXISTS " + database);
  }

  @Test
  public void testInsertOverwrite() {
    sql("SELECT * FROM " + source);
    sql("INSERT OVERWRITE " + database + "." + table + " SELECT * FROM  " + source);
    rows = sql("SELECT * FROM " + database + "." + table);

    Assert.assertEquals(records.size(), rows.size());
  }

  public List<GenericRecord> records(Schema schema, int count) {
    List<GenericRecord> records = Lists.newArrayList();
    List<Types.NestedField> columns = Lists.newArrayList(schema.columns());
    for (int i = 0; i < count; i++) {
      GenericRecord record = GenericRecord.create(schema);

      records.add(record);
    }
    return records;
  }
}
