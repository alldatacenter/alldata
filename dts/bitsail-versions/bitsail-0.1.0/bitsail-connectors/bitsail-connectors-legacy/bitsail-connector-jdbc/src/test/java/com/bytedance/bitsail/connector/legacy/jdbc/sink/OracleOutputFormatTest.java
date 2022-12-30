/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.sink;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.WriteModeProxy;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.OracleUpsertUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OracleOutputFormatTest extends OracleOutputFormat {
  @Before
  public void init() {
    this.partitionPatternFormat = "yyyyMMdd";
    this.deleteThreshold = 1000;
    this.partitionName = "DATE";
    this.tableSchema = "BITSAIL";
    this.table = "BITSAIL_TEST";
    this.tableWithSchema = "BITSAIL.BITSAIL_TEST";
    this.primaryKey = "ID";
    this.upsertKeys = new HashMap<>();
    this.extraPartitions = new JDBCOutputExtraPartitions(getDriverName(), getFieldQuote(), getValueQuote());;
    List<String> cols = new ArrayList<>();
    cols.add("PK");
    String[] shardKeys = new String[]{"PK"};
    this.upsertKeys.put("primary_key", cols);
    this.jdbcUpsertUtil = new OracleUpsertUtil(this, shardKeys, this.upsertKeys);
  }

  @Test
  public void testGenClearQuery() {
    String expectClearQuery = "delete from \"BITSAIL\".\"BITSAIL_TEST\" where \"ID\" in (select \"ID\" from \"BITSAIL\"." +
            "\"BITSAIL_TEST\" where \"DATE\"=20201229 and rownum < 1000)";
    Assert.assertEquals(expectClearQuery, this.genClearQuery("20201229", "=", ""));
  }

  @Test
  public void testGenClearQueryWithStringPartition() {
    this.partitionType = "varchar";
    String expectClearQuery = "delete from \"BITSAIL\".\"BITSAIL_TEST\" where \"ID\" in (select \"ID\" from \"BITSAIL\"." +
            "\"BITSAIL_TEST\" where \"DATE\"='20201229' and rownum < 1000)";
    Assert.assertEquals(expectClearQuery, this.genClearQuery("20201229", "=", ""));
  }

  @Test
  public void testGenInsertQuery() {
    List<ColumnInfo> columns = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      columns.add(new ColumnInfo("COL" + i, null));
    }

    // Test Insert query
    String expectedInsertQuery = "INSERT INTO BITSAIL.BITSAIL_TEST (\"COL0\",\"COL1\",\"DATE\")\n VALUES (?,?,?) ";
    Assert.assertEquals(expectedInsertQuery, this.genInsertQuery("BITSAIL_TEST", columns, WriteModeProxy.WriteMode.insert));
    // Test overwrite query MERGE INTO
    String expectedOverwriteQuery = "MERGE INTO \"BITSAIL\".\"BITSAIL_TEST\" T1 USING (SELECT ? \"COL0\",? \"COL1\" FROM DUAL) T2 ON (T1.\"PK\"=T2.\"PK\") " +
            "WHEN MATCHED THEN UPDATE SET \"T1\".COL0=\"T2\".COL0,\"T1\".COL1=\"T2\".COL1 " +
            "WHEN NOT MATCHED THEN INSERT (\"COL0\",\"COL1\") VALUES (\"T2\".\"COL0\",\"T2\".\"COL1\")";
    Assert.assertEquals(expectedOverwriteQuery, this.genInsertQuery("BITSAIL.BITSAIL_TEST", columns, WriteModeProxy.WriteMode.overwrite));
  }
}