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
package org.apache.drill.exec.store.ischema;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(SqlTest.class)
public class TestFilesTable extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);

    // create one workspace named files
    File filesWorkspace = cluster.makeDataDir("files", null, null);

    /*
      Add data to the workspace:
       ../files
       ../files/file0.txt
       ../files/folder1
       ../files/folder1/file1.txt
       ../files/folder1/folder2
       ../files/folder1/folder2/file2.txt
     */
    assertTrue(new File(filesWorkspace, "file0.txt").createNewFile());
    File folder1 = new File(filesWorkspace, "folder1");
    assertTrue(folder1.mkdir());
    assertTrue(new File(folder1, "file1.txt").createNewFile());
    File folder2 = new File(folder1, "folder2");
    assertTrue(folder2.mkdir());
    assertTrue(new File(folder2, "file2.txt").createNewFile());
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testSelectWithoutRecursion() throws Exception {
    client.testBuilder()
        .sqlQuery("select schema_name, root_schema_name, workspace_name, file_name, relative_path, is_directory, is_file " +
            "from INFORMATION_SCHEMA.`FILES`")
        .unOrdered()
        .baselineColumns("schema_name", "root_schema_name", "workspace_name", "file_name", "relative_path", "is_directory", "is_file")
        .baselineValues("dfs.files", "dfs", "files", "file0.txt", "file0.txt", false, true)
        .baselineValues("dfs.files", "dfs", "files", "folder1", "folder1", true, false)
        .go();
  }

  @Test
  public void testSelectWithRecursion() throws Exception {
    try {
      client.alterSession(ExecConstants.LIST_FILES_RECURSIVELY, true);
      client.testBuilder()
          .sqlQuery("select schema_name, root_schema_name, workspace_name, file_name, relative_path, is_directory, is_file " +
              "from INFORMATION_SCHEMA.`FILES`")
          .unOrdered()
          .baselineColumns("schema_name", "root_schema_name", "workspace_name", "file_name", "relative_path", "is_directory", "is_file")
          .baselineValues("dfs.files", "dfs", "files", "file0.txt", "file0.txt", false, true)
          .baselineValues("dfs.files", "dfs", "files", "folder1", "folder1", true, false)
          .baselineValues("dfs.files", "dfs", "files", "file1.txt", "folder1/file1.txt", false, true)
          .baselineValues("dfs.files", "dfs", "files", "folder2", "folder1/folder2", true, false)
          .baselineValues("dfs.files", "dfs", "files", "file2.txt", "folder1/folder2/file2.txt", false, true)
          .go();
    } finally {
      client.resetSession(ExecConstants.LIST_FILES_RECURSIVELY);
    }

  }

  @Test
  public void testShowFilesWithInCondition() throws Exception {
    checkCounts("show files in dfs.`files`",
        "select * from INFORMATION_SCHEMA.`FILES` where schema_name = 'dfs.files'");
  }

  @Test
  public void testShowFilesForSpecificDirectory() throws Exception {
    try {
      client.alterSession(ExecConstants.LIST_FILES_RECURSIVELY, false);
      QueryBuilder queryBuilder = client.queryBuilder().sql("show files in dfs.`files`.folder1");
      QueryBuilder.QuerySummary querySummary = queryBuilder.run();
      assertTrue(querySummary.succeeded());
      assertEquals(2, querySummary.recordCount());
      // option has no effect
      client.alterSession(ExecConstants.LIST_FILES_RECURSIVELY, true);
      querySummary = queryBuilder.run();
      assertTrue(querySummary.succeeded());
      assertEquals(2, querySummary.recordCount());
    } finally {
      client.resetSession(ExecConstants.LIST_FILES_RECURSIVELY);
    }
  }

  @Test
  public void testShowFilesWithUseClause() throws Exception {
    queryBuilder().sql("use dfs.`files`").run();
    checkCounts("show files",
        "select * from INFORMATION_SCHEMA.`FILES` where schema_name = 'dfs.files'");
  }

  @Test
  public void testShowFilesWithPartialUseClause() throws Exception {
    queryBuilder().sql("use dfs").run();
    checkCounts("show files in `files`",
        "select * from INFORMATION_SCHEMA.`FILES` where schema_name = 'dfs.files'");
  }

  @Test
  public void testShowFilesForDefaultSchema() throws Exception {
    queryBuilder().sql("use dfs").run().succeeded();
    checkCounts("show files",
        "select * from INFORMATION_SCHEMA.`FILES` where schema_name = 'dfs.default'");
  }

  @Test
  public void testFilterPushDown_None() throws Exception {
    String plan = queryBuilder().sql("select * from INFORMATION_SCHEMA.`FILES` where file_name = 'file1.txt'").explainText();
    assertTrue(plan.contains("filter=null"));
    assertTrue(plan.contains("Filter(condition="));
  }

  @Test
  public void testFilterPushDown_Partial() throws Exception {
    String plan = queryBuilder().sql("select * from INFORMATION_SCHEMA.`FILES` where schema_name = 'dfs.files' and file_name = 'file1.txt'").explainText();
    assertTrue(plan.contains("filter=booleanand(equal(Field=SCHEMA_NAME,Literal=dfs.files))"));
    assertTrue(plan.contains("Filter(condition="));
  }

  @Test
  public void testFilterPushDown_Full() throws Exception {
    String plan = queryBuilder().sql("select * from INFORMATION_SCHEMA.`FILES` where schema_name = 'dfs.files'").explainText();
    assertTrue(plan.contains("filter=equal(Field=SCHEMA_NAME,Literal=dfs.files)"));
    assertFalse(plan.contains("Filter(condition="));
  }

  private void checkCounts(String testQuery, String baseQuery) throws Exception {
    QueryBuilder.QuerySummary testQuerySummary = queryBuilder().sql(testQuery).run();
    assertTrue(testQuerySummary.succeeded());
    QueryBuilder.QuerySummary baseQuerySummary = queryBuilder().sql(baseQuery).run();
    assertTrue(baseQuerySummary.succeeded());
    assertEquals(testQuerySummary.recordCount(), baseQuerySummary.recordCount());
  }

}

