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
package org.apache.drill.exec.store;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.util.Text;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.test.BaseTestQuery;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestImplicitFileColumns extends BaseTestQuery {
  public static final String CSV = "csv";
  public static final String MAIN = "main";
  public static final String NESTED = "nested";
  public static final String MAIN_FILE = MAIN + "." + CSV;
  public static final String NESTED_FILE = NESTED + "." + CSV;
  public static final Path FILES = Paths.get("files");
  public static final Path NESTED_DIR = FILES.resolve(NESTED);
  public static final Path JSON_TBL = Paths.get("scan", "jsonTbl"); // 1990/1.json : {id:100, name: "John"}, 1991/2.json : {id: 1000, name : "Joe"}
  public static final Path PARQUET_TBL = Paths.get("multilevel", "parquet");  // 1990/Q1/orders_1990_q1.parquet, ...
  public static final Path PARQUET_CHANGE_TBL = Paths.get("multilevel", "parquetWithSchemaChange");
  public static final Path CSV_TBL = Paths.get("multilevel", "csv");  // 1990/Q1/orders_1990_q1.csv, ..

  @SuppressWarnings("serial")
  private static final JsonStringArrayList<Text> mainColumnValues = new JsonStringArrayList<Text>() {{
    add(new Text(MAIN));
  }};
  @SuppressWarnings("serial")
  private static final JsonStringArrayList<Text> nestedColumnValues = new JsonStringArrayList<Text>() {{
    add(new Text(NESTED));
  }};

  private static File mainFile;
  private static File nestedFile;

  @BeforeClass
  public static void setup() throws Exception {
    File files = dirTestWatcher.makeRootSubDir(FILES);
    mainFile = new File(files, MAIN_FILE);
    Files.asCharSink(mainFile, Charsets.UTF_8).write(MAIN);
    File nestedFolder = new File(files, NESTED);
    nestedFolder.mkdirs();
    nestedFile = new File(nestedFolder, NESTED_FILE);
    Files.asCharSink(nestedFile, Charsets.UTF_8).write(NESTED);

    dirTestWatcher.copyResourceToRoot(JSON_TBL);
    dirTestWatcher.copyResourceToRoot(PARQUET_TBL);
    dirTestWatcher.copyResourceToRoot(CSV_TBL);
    dirTestWatcher.copyResourceToRoot(PARQUET_CHANGE_TBL);
  }

  @Test
  public void testImplicitColumns() throws Exception {
    testBuilder()
        .sqlQuery("select *, filename, suffix, fqn, filepath from dfs.`%s` order by filename", FILES)
        .ordered()
        .baselineColumns("columns", "dir0", "filename", "suffix", "fqn", "filepath")
        .baselineValues(mainColumnValues, null, mainFile.getName(), CSV, mainFile.getCanonicalPath(), mainFile.getParentFile().getCanonicalPath())
        .baselineValues(nestedColumnValues, NESTED, NESTED_FILE, CSV, nestedFile.getCanonicalPath(), nestedFile.getParentFile().getCanonicalPath())
        .go();
  }

  @Test
  public void testImplicitColumnInWhereClause() throws Exception {
    testBuilder()
        .sqlQuery("select * from dfs.`%s` where filename = '%s'", NESTED_DIR, NESTED_FILE)
        .unOrdered()
        .baselineColumns("columns")
        .baselineValues(nestedColumnValues)
        .go();
  }

  @Test
  public void testImplicitColumnAlone() throws Exception {
    testBuilder()
        .sqlQuery("select filename from dfs.`%s`", NESTED_DIR)
        .unOrdered()
        .baselineColumns("filename")
        .baselineValues(NESTED_FILE)
        .go();
  }

  @Test
  public void testImplicitColumnWithTableColumns() throws Exception {
    testBuilder()
        .sqlQuery("select columns, filename from dfs.`%s`", NESTED_DIR)
        .unOrdered()
        .baselineColumns("columns", "filename")
        .baselineValues(nestedColumnValues, NESTED_FILE)
        .go();
  }

  @Test
  public void testCountStarWithImplicitColumnsInWhereClause() throws Exception {
    testBuilder()
        .sqlQuery("select count(*) as cnt from dfs.`%s` where filename = '%s'", NESTED_DIR, NESTED_FILE)
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(1L)
        .go();
  }

  @Test
  public void testImplicitAndPartitionColumnsInSelectClause() throws Exception {
    testBuilder()
        .sqlQuery("select dir0, filename from dfs.`%s` order by filename", FILES)
        .ordered()
        .baselineColumns("dir0", "filename")
        .baselineValues(null, MAIN_FILE)
        .baselineValues(NESTED, NESTED_FILE)
        .go();
  }

  @Test
  public void testImplicitColumnsForParquet() throws Exception {
    testBuilder()
        .sqlQuery("select filename, suffix from cp.`tpch/region.parquet` limit 1")
        .unOrdered()
        .baselineColumns("filename", "suffix")
        .baselineValues("region.parquet", "parquet")
        .go();
  }

  @Test // DRILL-4733
  public void testMultilevelParquetWithSchemaChange() throws Exception {
    try {
      test("alter session set `planner.enable_decimal_data_type` = true");
      testBuilder()
          .sqlQuery("select max(dir0) as max_dir from dfs.`%s`", PARQUET_CHANGE_TBL)
          .unOrdered()
          .baselineColumns("max_dir")
          .baselineValues("voter50")
          .go();
    } finally {
      test("alter session set `planner.enable_decimal_data_type` = false");
    }
  }

  @Test
  public void testStarColumnJson() throws Exception {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("dir0", TypeProtos.MinorType.VARCHAR)
        .addNullable("id", TypeProtos.MinorType.BIGINT)
        .addNullable("name", TypeProtos.MinorType.VARCHAR);
    final BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    testBuilder()
        .sqlQuery("select * from dfs.`%s` ", JSON_TBL)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test
  public void testStarColumnParquet() throws Exception {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("dir0", TypeProtos.MinorType.VARCHAR)
        .addNullable("dir1", TypeProtos.MinorType.VARCHAR)
        .add("o_orderkey", TypeProtos.MinorType.INT)
        .add("o_custkey", TypeProtos.MinorType.INT)
        .add("o_orderstatus", TypeProtos.MinorType.VARCHAR)
        .add("o_totalprice", TypeProtos.MinorType.FLOAT8)
        .add("o_orderdate", TypeProtos.MinorType.DATE)
        .add("o_orderpriority", TypeProtos.MinorType.VARCHAR)
        .add("o_clerk", TypeProtos.MinorType.VARCHAR)
        .add("o_shippriority", TypeProtos.MinorType.INT)
        .add("o_comment", TypeProtos.MinorType.VARCHAR);
    final BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    testBuilder()
        .sqlQuery("select * from dfs.`%s` ", PARQUET_TBL)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test
  public void testStarColumnCsv() throws Exception {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addArray("columns", TypeProtos.MinorType.VARCHAR)
        .addNullable("dir0", TypeProtos.MinorType.VARCHAR)
        .addNullable("dir1", TypeProtos.MinorType.VARCHAR);
    final BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    testBuilder()
        .sqlQuery("select * from dfs.`%s` ", CSV_TBL)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }
}
