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

import static org.apache.drill.test.TestBuilder.mapOf;

@Category({SlowTest.class, HbaseStorageTest.class})
public class HBaseRecordReaderTest extends BaseHBaseTest {

  @Test
  public void testLocalDistributed() throws Exception {
    String planName = "/hbase/hbase_scan_screen_physical.json";
    runHBasePhysicalVerifyCount(planName, HBaseTestsSuite.TEST_TABLE_1.getNameAsString(), 8);
  }

  @Test
  public void testLocalDistributedColumnSelect() throws Exception {
    String planName = "/hbase/hbase_scan_screen_physical_column_select.json";
    runHBasePhysicalVerifyCount(planName, HBaseTestsSuite.TEST_TABLE_1.getNameAsString(), 3);
  }

  @Test
  public void testLocalDistributedFamilySelect() throws Exception {
    String planName = "/hbase/hbase_scan_screen_physical_family_select.json";
    runHBasePhysicalVerifyCount(planName, HBaseTestsSuite.TEST_TABLE_1.getNameAsString(), 4);
  }

  @Test
  public void testOrderBy() throws Exception {
    testBuilder()
      .sqlQuery("select cast(row_key AS VARCHAR) as row_key, t.f from hbase.`TestTable2` t order by t.f.c1")
      .ordered()
      .baselineColumns("row_key", "f")
      .baselineValues(
        "a2",
        mapOf(
          "c1", "11".getBytes(),
          "c2", "12".getBytes(),
          "c3", "13".getBytes()))
      .baselineValues(
        "a1",
        mapOf(
          "c1", "21".getBytes(),
          "c2", "22".getBytes(),
          "c3", "23".getBytes()))
      .baselineValues(
        "a3",
        mapOf(
          "c1", "31".getBytes(),
          "c2", "32".getBytes(),
          "c3", "33".getBytes()))
      .go();
  }

  @Test
  public void testMultiCFDifferentCase() throws Exception {
    testBuilder()
      .sqlQuery("select * from hbase.`TestTableMultiCF` t")
      .unOrdered()
      .baselineColumns("row_key", "F", "f0")
      .baselineValues(
        "a1".getBytes(),
        mapOf("c3", "23".getBytes()),
        mapOf("c1", "21".getBytes(), "c2", "22".getBytes()))
      .baselineValues(
        "a2".getBytes(),
        mapOf("c3", "13".getBytes()),
        mapOf("c1", "11".getBytes(), "c2", "12".getBytes()))
      .baselineValues(
        "a3".getBytes(),
        mapOf("c3", "33".getBytes()),
        mapOf("c1", "31".getBytes(), "c2", "32".getBytes()))
      .go();
  }

}
