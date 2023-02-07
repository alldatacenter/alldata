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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.PlanTestBase;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TestParquetFilterPushDownForComplexTypes extends PlanTestBase {

  private static final String TABLE_PATH = "parquet/users";
  private static final String TABLE_NAME = String.format("%s.`%s`", StoragePluginTestUtils.DFS_PLUGIN_NAME, TABLE_PATH);

  @BeforeClass
  public static void copyData() {
    /*
    Parquet schema:
      message complex_users {
        required group user {
          required int32 id;
          optional int32 age;
          repeated int32 hobby_ids;
          optional boolean active;
        }
      }

    Data set:
    users_1.parquet
    {"id":1,"age":25,"hobby_ids":[1,2,3],"active":true}
    {"id":2,"age":28,"hobby_ids":[1,2,5],"active":true}

    users_2.parquet
    {"id":3,"age":31,"hobby_ids":[1,2,3],"active":true}
    {"id":4,"age":32,"hobby_ids":[4,10,18],"active":false}

    users_3.parquet
    {"id":5,"hobby_ids":[11,12,13,14,15]}

    users_4.parquet
    {"id":6,"age":41,"hobby_ids":[20,21,22],"active":true}
    {"id":7,"hobby_ids":[20,21,22,24]}

    users_5.parquet
    {"id":8,"age":41,"hobby_ids":[],"active":false}

    users_6.parquet
    {"id":9,"age":20,"hobby_ids":[],"active":false}
    {"id":10,"age":21,"hobby_ids":[26,28,29]}

    users_7.parquet
    {"id":11,"age":23,"hobby_ids":[10,11,12],"active":true}
    {"id":12,"age":35,"hobby_ids":[22,23,24],"active":false}
    {"id":13,"age":25,"hobby_ids":[14,22,26]}

     */
    dirTestWatcher.copyResourceToRoot(Paths.get(TABLE_PATH));
  }

  @Test
  public void testPushDownArray() throws Exception {
    testParquetFilterPushDown("t.`user`.hobby_ids[0] = 1", 3, 2);
    testParquetFilterPushDown("t.`user`.hobby_ids[0] = 100", 0, 1);
    testParquetFilterPushDown("t.`user`.hobby_ids[0] <> 1", 8, 6);
    testParquetFilterPushDown("t.`user`.hobby_ids[2] > 20", 5, 3);
    testParquetFilterPushDown("t.`user`.hobby_ids[0] between 10 and 20", 5, 4);
    testParquetFilterPushDown("t.`user`.hobby_ids[4] = 15", 1, 3);
    testParquetFilterPushDown("t.`user`.hobby_ids[2] is not null", 11, 6);
    testParquetFilterPushDown("t.`user`.hobby_ids[3] is null", 11, 7);
  }

  @Test
  public void testPushDownComplexIntColumn() throws Exception {
    testParquetFilterPushDown("t.`user`.age = 31", 1, 2);
    testParquetFilterPushDown("t.`user`.age = 1", 0, 1);
    testParquetFilterPushDown("t.`user`.age <> 20", 10, 6);
    testParquetFilterPushDown("t.`user`.age > 30", 5, 4);
    testParquetFilterPushDown("t.`user`.age between 20 and 30", 6, 3);
    testParquetFilterPushDown("t.`user`.age is not null", 11, 6);
    testParquetFilterPushDown("t.`user`.age is null", 2, 2);
  }

  @Test
  public void testPushDownComplexBooleanColumn() throws Exception {
    testParquetFilterPushDown("t.`user`.active is true", 5, 4);
    testParquetFilterPushDown("t.`user`.active is not true", 8, 6);
    testParquetFilterPushDown("t.`user`.active is false", 4, 4);
    testParquetFilterPushDown("t.`user`.active is not false", 9, 6);
    testParquetFilterPushDown("t.`user`.active is not null", 9, 6);
    testParquetFilterPushDown("t.`user`.active is null", 4, 4);
  }


  private void testParquetFilterPushDown(String predicate, int expectedRowCount, int expectRowGroupsNumber) throws Exception {
    String query = String.format("select * from %s t where %s", TABLE_NAME, predicate);

    int actualRowCount = testSql(query);
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    String expectRowGroupsNumberPattern = "numRowGroups=" + expectRowGroupsNumber;
    testPlanMatchingPatterns(query, new String[] {expectRowGroupsNumberPattern}, new String[] {});
  }

}

