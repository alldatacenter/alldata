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

package com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert;

import com.bytedance.bitsail.connector.legacy.jdbc.sink.OracleOutputFormat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OracleUpsertUtilTest extends TestCase {

  @Test
  public void testGenUpsertTemplateWithNullUpsertKeys() {
    String[] shardKeys = new String[]{"byte"};
    OracleUpsertUtil upsertUtil = new OracleUpsertUtil(new OracleOutputFormat(), shardKeys, null);
    // Test overwriting query MERGE INTO
    String expectedOverwriteQuery = "INSERT INTO \"BITSAIL_TEST\" (\"COL0\",\"COL1\") values (?,?)";
    Assert.assertEquals(expectedOverwriteQuery, upsertUtil.genUpsertTemplate("BITSAIL_TEST", provideColumns(2), ""));
  }

  @Test
  public void testGenUpsertTemplateWithEmptyUpdateColumns() {
    String[] shardKeys = new String[]{"byte"};
    Map<String, List<String>> upsertKeys = ImmutableMap.of("primary_key", ImmutableList.of("COL0"));
    OracleUpsertUtil upsertUtil = new OracleUpsertUtil(new OracleOutputFormat(), shardKeys, upsertKeys);
    // Test overwriting query MERGE INTO
    String expectedOverwriteQuery = "MERGE INTO \"BITSAIL_TEST\" T1 USING (SELECT ? \"COL0\" FROM DUAL) T2 ON (T1.\"COL0\"=T2.\"COL0\") " +
            "WHEN NOT MATCHED THEN INSERT (\"COL0\") VALUES (\"T2\".\"COL0\")";
    Assert.assertEquals(expectedOverwriteQuery, upsertUtil.genUpsertTemplate("BITSAIL_TEST", provideColumns(1), ""));
  }

  @Test
  public void testGenUpsertTemplate() {
    String[] shardKeys = new String[]{"byte"};
    Map<String, List<String>> upsertKeys = ImmutableMap.of("primary_key", ImmutableList.of("PK"));
    OracleUpsertUtil upsertUtil = new OracleUpsertUtil(new OracleOutputFormat(), shardKeys, upsertKeys);
    // Test overwriting query MERGE INTO
    String expectedOverwriteQuery = "MERGE INTO \"BITSAIL_TEST\" T1 USING (SELECT ? \"COL0\",? \"COL1\" FROM DUAL) T2 ON (T1.\"PK\"=T2.\"PK\") " +
            "WHEN MATCHED THEN UPDATE SET \"T1\".COL0=\"T2\".COL0,\"T1\".COL1=\"T2\".COL1 " +
            "WHEN NOT MATCHED THEN INSERT (\"COL0\",\"COL1\") VALUES (\"T2\".\"COL0\",\"T2\".\"COL1\")";
    Assert.assertEquals(expectedOverwriteQuery, upsertUtil.genUpsertTemplate("BITSAIL_TEST", provideColumns(2), ""));
  }

  private List<String> provideColumns(final int size) {
    final List<String> columns = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      columns.add("COL" + i);
    }
    return columns;
  }
}