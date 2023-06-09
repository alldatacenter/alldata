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
package org.apache.drill.exec.physical.impl.TopN;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.test.TestBuilder;
import org.apache.drill.test.SubDirTestWatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

@Category(OperatorTest.class)
public class TestTopNSchemaChanges extends BaseTestQuery {
  private static final Path TABLE = Paths.get("table");

  private static File tableDir;

  @BeforeClass
  public static void setupTestFiles() {
    tableDir = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(TABLE)
      .toFile();
  }

  @Rule
  public final SubDirTestWatcher localDirTestWatcher =
    new SubDirTestWatcher.Builder(dirTestWatcher.getRootDir())
      .addSubDir(TABLE)
      .build();

  @Test
  public void testNumericTypes() throws Exception {
    // left side int and floats
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(tableDir, "d1.json")));
    for (int i = 0; i < 10000; i+=2) {
      writer.write(String.format("{ \"kl\": %d, \"vl\": %d }\n", i, i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(tableDir, "d2.json")));
    for (int i = 1; i < 10000; i+=2) {
      writer.write(String.format("{ \"kl\": %f, \"vl\": %f }\n", (float)i, (float)i));
    }
    writer.close();

    TestBuilder builder = testBuilder()
      .sqlQuery("select * from dfs.`%s` order by kl limit 12", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl");

    for (long i = 0; i < 12; ++i) {
      if (i %2 == 0) {
        builder.baselineValues(i, i);
      } else {
        builder.baselineValues((double)i, (double)i);
      }
    }
    builder.go();
  }

  @Test
  public void testNumericAndStringTypes() throws Exception {
    // left side int and strings
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(tableDir, "d1.json")));
    for (int i = 0; i < 1000; i+=2) {
      writer.write(String.format("{ \"kl\": %d, \"vl\": %d }\n", i, i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(tableDir, "d2.json")));
    for (int i = 1; i < 1000; i+=2) {
      writer.write(String.format("{ \"kl\": \"%s\", \"vl\": \"%s\" }\n", i, i));
    }
    writer.close();

    TestBuilder builder = testBuilder()
      .sqlQuery("select * from dfs.`%s` order by kl limit 12", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl");

    for (long i = 0; i < 24; i+=2) {
        builder.baselineValues(i, i);
    }

    builder = testBuilder()
      .sqlQuery("select * from dfs.`%s` order by kl desc limit 12", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl")
      .baselineValues("999", "999")
      .baselineValues("997", "997")
      .baselineValues("995", "995")
      .baselineValues("993", "993")
      .baselineValues("991", "991")
      .baselineValues("99", "99")
      .baselineValues("989", "989")
      .baselineValues("987", "987")
      .baselineValues("985", "985")
      .baselineValues("983", "983")
      .baselineValues("981", "981")
      .baselineValues("979", "979");
    builder.go();
  }

  @Test
  public void testUnionTypes() throws Exception {
    // union of int and float and string.
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(tableDir, "d1.json")));
    for (int i = 0; i <= 9; ++i) {
      switch (i%3) {
        case 0: // 0, 3, 6, 9
          writer.write(String.format("{ \"kl\": %d, \"vl\": %d }\n", i, i));
          break;
        case 1: // 1, 4, 7
          writer.write(String.format("{ \"kl\": %f, \"vl\": %f }\n", (float)i, (float)i));
          break;
        case 2: // 2, 5, 8
          writer.write(String.format("{ \"kl\": \"%s\", \"vl\": \"%s\" }\n", i, i));
          break;
      }
    }
    writer.close();

    TestBuilder builder = testBuilder()
      .sqlQuery("select * from dfs.`%s` order by kl limit 8", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl");

    builder.baselineValues(0l, 0l);
    builder.baselineValues(1.0d, 1.0d);
    builder.baselineValues("2", "2");
    builder.baselineValues(3l, 3l);
    builder.baselineValues(4.0d, 4.0d);
    builder.baselineValues("5", "5");
    builder.baselineValues(6l, 6l);
    builder.baselineValues(7.0d, 7.0d);
    builder.go();
  }

  @Test
  public void testMissingColumn() throws Exception {
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(tableDir, "d1.json")));
    for (int i = 0; i < 100; i++) {
      writer.write(String.format("{ \"kl1\": %d, \"vl1\": %d }\n", i, i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(tableDir, "d2.json")));
    for (int i = 100; i < 200; i++) {
      writer.write(String.format("{ \"kl\": %f, \"vl\": %f }\n", (float)i, (float)i));
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(new File(tableDir, "d3.json")));
    for (int i = 200; i < 300; i++) {
      writer.write(String.format("{ \"kl2\": \"%s\", \"vl2\": \"%s\" }\n", i, i));
    }
    writer.close();

    TestBuilder builder = testBuilder()
      .sqlQuery("select kl, vl, kl1, vl1, kl2, vl2 from dfs.`%s` order by kl limit 3", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl", "kl1", "vl1", "kl2", "vl2")
      .baselineValues(100.0d, 100.0d, null, null, null, null)
      .baselineValues(101.0d, 101.0d, null, null, null, null)
      .baselineValues(102.0d, 102.0d, null, null, null, null);
    builder.go();

    builder = testBuilder()
      .sqlQuery("select kl, vl, kl1, vl1, kl2, vl2  from dfs.`%s` order by kl1 limit 3", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl", "kl1", "vl1", "kl2", "vl2")
      .baselineValues(null, null, 0l, 0l, null, null)
      .baselineValues(null, null, 1l, 1l, null, null)
      .baselineValues(null, null, 2l, 2l, null, null);
    builder.go();

    builder = testBuilder()
      .sqlQuery("select kl, vl, kl1, vl1, kl2, vl2 from dfs.`%s` order by kl2 limit 3", TABLE)
      .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
      .ordered()
      .baselineColumns("kl", "vl", "kl1", "vl1", "kl2", "vl2")
      .baselineValues(null, null, null, null, "200", "200")
      .baselineValues(null, null, null, null, "201", "201")
      .baselineValues(null, null, null, null, "202", "202");
    builder.go();

    // Since client can't handle new columns which are not in first batch, we won't test output of query.
    // Query should run w/o any errors.
    test("select * from dfs.`%s` order by kl limit 3", TABLE);
  }
}
