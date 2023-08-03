/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.test.suites.sql;

import com.google.common.collect.Maps;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.spark.SparkSQLProperties;
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.helper.RecordGenerator;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TestArcticSessionCatalog extends SparkTableTestBase {


  Dataset<Row> rs;

  @Override
  public Dataset<Row> sql(String sqlText) {
    rs = super.sql(sqlText);
    return rs;
  }

  public static Stream<Arguments> testCreateTable() {
    return Stream.of(
        Arguments.arguments("arctic", true, ""),
        Arguments.arguments("arctic", false, "pt"),
        Arguments.arguments("arctic", true, "pt"),

        Arguments.arguments("parquet", false, "pt"),
        Arguments.arguments("parquet", false, "dt string")
    );
  }

  @ParameterizedTest(name = "{index} USING {0} WITH PK {1} PARTITIONED BY ({2})")
  @MethodSource
  public void testCreateTable(String provider, boolean pk, String pt) {

    String sqlText = "CREATE TABLE " + target() + "(" +
        " id INT, data string, pt string ";
    if (pk) {
      sqlText += ", PRIMARY KEY(id)";
    }
    sqlText += ") USING " + provider;

    if (StringUtils.isNotBlank(pt)) {
      sqlText += " PARTITIONED BY (" + pt + ")";
    }

    sql(sqlText);

    if ("arctic".equalsIgnoreCase(provider)) {
      Assertions.assertTrue(tableExists());
    }

    Table hiveTable = loadHiveTable();
    Assertions.assertNotNull(hiveTable);
  }

  static final Schema schema = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "data", Types.StringType.get()),
      Types.NestedField.required(3, "pt", Types.StringType.get())
  );

  List<Record> source = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "111", "AAA"),
      RecordGenerator.newRecord(schema, 2, "222", "AAA"),
      RecordGenerator.newRecord(schema, 3, "333", "DDD"),
      RecordGenerator.newRecord(schema, 4, "444", "DDD"),
      RecordGenerator.newRecord(schema, 5, "555", "EEE"),
      RecordGenerator.newRecord(schema, 6, "666", "EEE")
  );

  public static Stream<Arguments> testCreateTableAsSelect() {
    return Stream.of(
        Arguments.arguments("arctic", true, "", true),
        Arguments.arguments("arctic", false, "pt", true),
        Arguments.arguments("arctic", true, "pt", false),

        Arguments.arguments("parquet", false, "pt", false),
        Arguments.arguments("parquet", false, "", false)
    );
  }

  @ParameterizedTest(name = "{index} USING {0} WITH PK {1} PARTITIONED BY ({2})")
  @MethodSource
  public void testCreateTableAsSelect(String provider, boolean pk, String pt, boolean duplicateCheck) {
    spark().conf().set(SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE, duplicateCheck);
    createViewSource(schema, source);
    String sqlText = "CREATE TABLE " + target();
    if (pk) {
      sqlText += " PRIMARY KEY (id, pt) ";
    }
    sqlText += " USING " + provider + " ";
    if (StringUtils.isNotBlank(pt)) {
      sqlText += " PARTITIONED BY (" + pt + ")";
    }
    sqlText += " AS SELECT * FROM " + source();

    sql(sqlText);
    if ("arctic".equalsIgnoreCase(provider)) {
      Assertions.assertTrue(tableExists());

    }

    Table hiveTable = loadHiveTable();
    Assertions.assertNotNull(hiveTable);
  }

  @Test
  public void testLoadLegacyTable() {
    createTarget(schema,
        c -> c.withPrimaryKeySpec(PrimaryKeySpec.builderFor(schema).addColumn("id").build()));
    createViewSource(schema, source);
    Table hiveTable = loadHiveTable();
    Map<String, String> properties = Maps.newHashMap(hiveTable.getParameters());
    properties.remove(HiveTableProperties.ARCTIC_TABLE_FLAG);
    properties.put(HiveTableProperties.ARCTIC_TABLE_FLAG_LEGACY, "true");
    hiveTable.setParameters(properties);
    try {
      context.getHiveClient().alter_table(hiveTable.getDbName(), hiveTable.getTableName(), hiveTable);
    } catch (TException e) {
      throw new RuntimeException(e);
    }

    sql("insert into " + target() + " select * from " + source());
    ArcticTable table = loadTable();
    List<Record> changes = TestTableHelper.changeRecordsWithAction(table.asKeyedTable());
    Assertions.assertTrue(changes.size() > 0);
  }
}
