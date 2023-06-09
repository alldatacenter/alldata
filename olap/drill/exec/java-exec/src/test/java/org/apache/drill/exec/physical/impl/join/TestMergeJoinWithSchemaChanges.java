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
package org.apache.drill.exec.physical.impl.join;

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
public class TestMergeJoinWithSchemaChanges extends BaseTestQuery {
  public static final Path LEFT_DIR = Paths.get("mergejoin-schemachanges-left");
  public static final Path RIGHT_DIR = Paths.get("mergejoin-schemachanges-right");

  private static File leftDir;
  private static File rightDir;

  @BeforeClass
  public static void getDirs() {
    leftDir = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(LEFT_DIR)
      .toFile();
    rightDir = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(RIGHT_DIR)
      .toFile();
  }

  @Rule
  public final SubDirTestWatcher subDirTestWatcher = new SubDirTestWatcher.Builder(dirTestWatcher.getRootDir())
    .addSubDir(LEFT_DIR)
    .addSubDir(RIGHT_DIR)
    .build();

  @Test
  public void testNumericTypes() throws Exception {
    // First create data for numeric types.
    // left side int and float vs right side float
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(leftDir, "l1.json")));
    for (int i = 0; i < 5000; ++i) {
      writer.write(String.format("{ \"kl\" : %d , \"vl\": %d }\n", i, i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(leftDir, "l2.json")));
    for (int i = 1000; i < 6000; ++i) {
      writer.write(String.format("{ \"kl\" : %f , \"vl\": %f }\n", (float) i, (float) i));
    }
    writer.close();

    // right side is int and float
    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r1.json")));
    for (int i = 2000; i < 7000; ++i) {
      writer.write(String.format("{ \"kr\" : %d , \"vr\": %d }\n", i, i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r2.json")));
    for (int i = 3000; i < 8000; ++i) {
      writer.write(String.format("{ \"kr\" : %f, \"vr\": %f }\n", (float) i, (float) i));
    }
    writer.close();

    // INNER JOIN
    String query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "inner", RIGHT_DIR);

    TestBuilder builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr");


    for (long i = 2000; i < 3000; ++i) {
      builder.baselineValues(i, i, i, i);
      builder.baselineValues((double)i, (double)i, i, i);
    }
    for (long i = 3000; i < 5000; ++i) {
      builder.baselineValues(i, i, i, i);
      builder.baselineValues(i, i, (double)i, (double)i);
      builder.baselineValues((double)i, (double)i, i, i);
      builder.baselineValues((double)i, (double)i, (double)i, (double)i);
    }
    for (long i = 5000; i < 6000; ++i) {
      builder.baselineValues((double)i, (double)i, i, i);
      builder.baselineValues((double) i, (double) i, (double) i, (double) i);
    }
    builder.go();

    // LEFT JOIN
    query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "left", RIGHT_DIR);

    builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr");

    for (long i = 0; i < 2000; ++i)   {
      builder.baselineValues(i, i, null, null);
    }
    for (long i = 1000; i < 2000; ++i) {
      builder.baselineValues((double)i, (double)i, null, null);
    }
    for (long i = 2000; i < 3000; ++i) {
      builder.baselineValues(i, i, i, i);
      builder.baselineValues((double)i, (double)i, i, i);
    }
    for (long i = 3000; i < 5000; ++i) {
      builder.baselineValues(i, i, i, i);
      builder.baselineValues(i, i, (double)i, (double)i);
      builder.baselineValues((double)i, (double)i, i, i);
      builder.baselineValues((double)i, (double)i, (double)i, (double)i);
    }
    for (long i = 5000; i < 6000; ++i) {
      builder.baselineValues((double) i, (double)i, i, i);
      builder.baselineValues((double)i, (double)i, (double)i, (double)i);
    }
    builder.go();
  }

  @Test
  public void testNumericStringTypes() throws Exception {
    // left side int and strings
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(leftDir, "l1.json")));
    for (int i = 0; i < 5000; ++i) {
      writer.write(String.format("{ \"kl\" : %d , \"vl\": %d }\n", i, i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(leftDir, "l2.json")));
    for (int i = 1000; i < 6000; ++i) {
      writer.write(String.format("{ \"kl\" : \"%s\" , \"vl\": \"%s\" }\n", i, i));
    }
    writer.close();

    // right side is float and strings
    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r1.json")));
    for (int i = 2000; i < 7000; ++i) {
      writer.write(String.format("{ \"kr\" : %f , \"vr\": %f }\n", (float)i, (float)i));
    }
    writer.close();
    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r2.json")));
    for (int i = 3000; i < 8000; ++i) {
      writer.write(String.format("{ \"kr\" : \"%s\", \"vr\": \"%s\" }\n", i, i));
    }
    writer.close();

    // INNER JOIN
    String query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "inner", RIGHT_DIR);

    TestBuilder builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr");

    for (long i = 2000; i < 5000; ++i) {
      builder.baselineValues(i, i, (double)i, (double)i);
    }
    for (long i = 3000; i < 6000; ++i) {
      final String d = Long.toString(i);
      builder.baselineValues(d, d, d, d);
    }
    builder.go();

    // RIGHT JOIN
    query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "right", RIGHT_DIR);

    builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr");

    for (long i = 2000; i < 5000; ++i) {
      builder.baselineValues(i, i, (double)i, (double)i);
    }
    for (long i = 3000; i < 6000; ++i) {
      final String d = Long.toString(i);
      builder.baselineValues(d, d, d, d);
    }
    for (long i = 5000; i < 7000; ++i) {
      builder.baselineValues(null, null, (double)i, (double)i);
    }
    for (long i = 6000; i < 8000; ++i) {
      final String d = Long.toString(i);
      builder.baselineValues(null, null, d, d);
    }
    builder.go();
  }

  @Test // DRILL-5612
  public void testMissingAndNewColumns() throws Exception {
    // missing column kl
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(leftDir, "l1.json")));
    for (int i = 0; i < 50; ++i) {
      writer.write(String.format("{ \"kl1\" : %d , \"vl1\": %d }\n", i, i));
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(new File(leftDir, "l2.json")));
    for (int i = 50; i < 100; ++i) {
      writer.write(String.format("{ \"kl\" : %d , \"vl\": %d }\n", i, i));
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(new File(leftDir, "l3.json")));
    for (int i = 100; i < 150; ++i) {
      writer.write(String.format("{ \"kl2\" : %d , \"vl2\": %d }\n", i, i));
    }
    writer.close();

    // right missing column kr
    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r1.json")));
    for (int i = 0; i < 50; ++i) {
      writer.write(String.format("{ \"kr1\" : %f , \"vr1\": %f }\n", (float)i, (float)i));
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r2.json")));
    for (int i = 50; i < 100; ++i) {
      writer.write(String.format("{ \"kr\" : %f , \"vr\": %f }\n", (float)i, (float)i));
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r3.json")));
    for (int i = 100; i < 150; ++i) {
      writer.write(String.format("{ \"kr2\" : %f , \"vr2\": %f }\n", (float)i, (float)i));
    }
    writer.close();

    // INNER JOIN
    String query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "inner", RIGHT_DIR);

    TestBuilder builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr", "kl1", "vl1", "kl2", "vl2", "kr1", "vr1", "kr2", "vr2");

    for (long i = 50; i < 100; ++i) {
      builder.baselineValues(i, i, (double)i, (double)i, null, null, null, null, null, null, null, null);
    }
    builder.go();

    // LEFT JOIN
    query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "left", RIGHT_DIR);

    builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr", "kl1", "vl1", "kl2", "vl2", "kr1", "vr1", "kr2", "vr2");

    for (long i = 0; i < 50; ++i) {
      builder.baselineValues(null, null, null, null, i, i, null, null, null, null, null, null);
    }
    for (long i = 50; i < 100; ++i) {
      builder.baselineValues(i, i, (double)i, (double)i, null, null, null, null, null, null, null, null);
    }
    for (long i = 100; i < 150; ++i) {
      builder.baselineValues(null, null, null, null, null, null, i, i, null, null, null, null);
    }
    builder.go();

    // RIGHT JOIN
    query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kr",
      LEFT_DIR, "right", RIGHT_DIR);

    builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kr", "vr", "kl1", "vl1", "kl2", "vl2", "kr1", "vr1", "kr2", "vr2");

    for (long i = 0; i < 50; ++i) {
      builder.baselineValues(null, null, null, null, null, null, null, null, (double)i, (double)i, null, null);
    }
    for (long i = 50; i < 100; ++i) {
      builder.baselineValues(i, i, (double)i, (double)i, null, null, null, null, null, null, null, null);
    }
    for (long i = 100; i < 150; ++i) {
      builder.baselineValues(null, null, null, null, null, null, null, null, null, null, (double)i, (double)i);
    }
    builder.go();
  }

  @Test
  public void testOneSideSchemaChanges() throws Exception {
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(leftDir, "l1.json")));
    for (int i = 0; i < 50; ++i) {
      writer.write(String.format("{ \"kl\" : %d , \"vl\": %d }\n", i, i));
    }
    for (int i = 50; i < 100; ++i) {
      writer.write(String.format("{ \"kl\" : %f , \"vl\": %f }\n", (float) i, (float) i));
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(new File(rightDir, "r1.json")));
    for (int i = 0; i < 50; ++i) {
      writer.write(String.format("{ \"kl\" : %d , \"vl\": %d }\n", i, i));
    }
    writer.close();

    String query = String.format("select * from dfs.`%s` L %s join dfs.`%s` R on L.kl=R.kl",
      LEFT_DIR, "inner", RIGHT_DIR);
    TestBuilder builder = testBuilder()
      .sqlQuery(query)
      .optionSettingQueriesForTestQuery("alter session set `planner.enable_hashjoin` = false; alter session set `exec.enable_union_type` = true")
      .unOrdered()
      .baselineColumns("kl", "vl", "kl0", "vl0");

    for (long i = 0; i < 50; ++i) {
      builder.baselineValues(i, i, i, i);
    }
    builder.go();
  }
}
