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
package org.apache.drill.exec.store.easy.text.compliant;

import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;

import java.io.File;
import java.io.IOException;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestCsvIgnoreHeaders extends BaseCsvTest{

  private static String withHeaders[] = {
      "a,b,c",
      "10,foo,bar",
      "20,fred,wilma"
  };

  private static String raggedRows[] = {
      "a,b,c",
      "10,dino",
      "20,foo,bar",
      "30"
  };

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(true,  false);
  }

  @Test
  public void testColumns() throws IOException {
    String fileName = "simple.csv";
    buildFile(fileName, withHeaders);
    String sql = "SELECT columns FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, fileName).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("10", "foo", "bar"))
        .addSingleCol(strArray("20", "fred", "wilma"))
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testRaggedRows() throws IOException {
    String fileName = "ragged.csv";
    TestCsvWithHeaders.buildFile(new File(testDir, fileName), raggedRows);
    String sql = "SELECT columns FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, fileName).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("10", "dino"))
        .addSingleCol(strArray("20", "foo", "bar"))
        .addSingleCol(strArray("30"))
        .build();
    RowSetUtilities.verify(expected, actual);
  }
}
