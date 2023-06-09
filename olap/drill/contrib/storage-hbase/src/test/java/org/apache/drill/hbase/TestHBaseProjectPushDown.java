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
package org.apache.drill.hbase;

import org.apache.drill.categories.HbaseStorageTest;
import org.apache.drill.categories.SlowTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HbaseStorageTest.class})
public class TestHBaseProjectPushDown extends BaseHBaseTest {

  @Test
  public void testRowKeyPushDown() throws Exception{
    setColumnWidth(8);
    runHBaseSQLVerifyCount("SELECT\n"
        + "row_key\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName",
        8);
  }

  @Test
  public void testColumnWith1RowPushDown() throws Exception{
    setColumnWidth(6);
    runHBaseSQLVerifyCount("SELECT\n"
        + "t.f2.c7 as `t.f2.c7`\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` t",
        1);
  }

  @Test
  public void testRowKeyAndColumnPushDown() throws Exception{
    setColumnWidths(new int[] {8, 9, 6, 2, 6});
    runHBaseSQLVerifyCount("SELECT\n"
        // Note:  Can't currently use period in column alias (not even with
        // qualified identifier) because Drill internals don't currently encode
        // names sufficiently.
        + "row_key, t.f.c1 * 31 as `t dot f dot c1 * 31`, "
        + "t.f.c2 as `t dot f dot c2`, 5 as `5`, 'abc' as `'abc'`\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` t",
        8);
  }

  @Test
  public void testColumnFamilyPushDown() throws Exception{
    setColumnWidths(new int[] {8, 74, 38});
    runHBaseSQLVerifyCount("SELECT\n"
        + "row_key, f, f2\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName",
        8);
  }

}
