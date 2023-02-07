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
package org.apache.drill;

import org.apache.drill.categories.PlannerTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.ClassCompilerSelector;
import org.apache.drill.exec.compile.ClassTransformer;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Test the optimizer plan in terms of projecting different functions e.g. cast
 */
@Category(PlannerTest.class)
public class TestProjectWithFunctions extends ClusterTest {

  @BeforeClass
  public static void setupFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("view"));
  }

  @Before
  public void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testCastFunctions() throws Exception {
    String sql = "select t1.f from dfs.`view/emp_6212.view.drill` as t inner join dfs.`view/emp_6212.view.drill` as t1 " +
        "on cast(t.f as int) = cast(t1.f as int) and cast(t.f as int) = 10 and cast(t1.f as int) = 10";
    queryBuilder().sql(sql).run();
  }

  @Test // DRILL-6524
  public void testCaseWithColumnsInClause() throws Exception {
    String sql =
        "select\n" +
            "case when a = 3 then a else b end as c,\n" +
            "case when a = 1 then a else b end as d\n" +
        "from (values(1, 2)) t(a, b)";

    try {
      client.alterSession(ExecConstants.SCALAR_REPLACEMENT_OPTION, ClassTransformer.ScalarReplacementOption.ON.name());

      List<String> compilers = Arrays.asList(ClassCompilerSelector.CompilerPolicy.JANINO.name(),
        ClassCompilerSelector.CompilerPolicy.JDK.name());

      for (String compilerName : compilers) {
        client.alterSession(ClassCompilerSelector.JAVA_COMPILER_OPTION, compilerName);

        testBuilder()
            .sqlQuery(sql)
            .unOrdered()
            .baselineColumns("c", "d")
            .baselineValues(2L, 1L)
            .go();
      }
    } finally {
      client.resetSession(ExecConstants.SCALAR_REPLACEMENT_OPTION);
      client.resetSession(ClassCompilerSelector.JAVA_COMPILER_OPTION);
    }
  }

  @Test // DRILL-6722
  public void testCaseWithColumnExprsInClause() throws Exception {
    String sqlCreate =
        "create table dfs.tmp.test as \n" +
            "select 1 as a, 2 as b\n" +
            "union all\n" +
                "select 3 as a, 2 as b\n" +
            "union all\n" +
                "select 1 as a, 4 as b\n" +
            "union all\n" +
                "select 2 as a, 2 as b";
    try {
      run(sqlCreate);
      String sql =
          "select\n" +
              "case when s.a > s.b then s.a else s.b end as b, \n" +
              "abs(s.a - s.b) as d\n" +
          "from dfs.tmp.test s";

      client.alterSession(ExecConstants.SCALAR_REPLACEMENT_OPTION, ClassTransformer.ScalarReplacementOption.ON.name());

      List<String> compilers = Arrays.asList(ClassCompilerSelector.CompilerPolicy.JANINO.name(),
        ClassCompilerSelector.CompilerPolicy.JDK.name());

      for (String compilerName : compilers) {
        client.alterSession(ClassCompilerSelector.JAVA_COMPILER_OPTION, compilerName);

        testBuilder()
            .sqlQuery(sql)
            .unOrdered()
            .baselineColumns("b", "d")
            .baselineValues(2, 1)
            .baselineValues(3, 1)
            .baselineValues(4, 3)
            .baselineValues(2, 0)
            .go();
      }
    } finally {
      run("drop table if exists dfs.tmp.test");
      client.resetSession(ExecConstants.SCALAR_REPLACEMENT_OPTION);
      client.resetSession(ClassCompilerSelector.JAVA_COMPILER_OPTION);
    }
  }

  @Test // DRILL-5581
  public void testCaseWithColumnExprsOnView() throws Exception {
    String sqlCreate =
        "CREATE VIEW dfs.tmp.`vw_order_sample_csv` as\n" +
            "SELECT\n" +
                "a AS `ND`,\n" +
                "CAST(b AS BIGINT) AS `col1`,\n" +
                "CAST(c AS BIGINT) AS `col2`\n" +
            "FROM (values('202634342',20000101,20160301)) as t(a, b, c)";
    try {
      run(sqlCreate);
      String sql =
          "select\n" +
              "case when col1 > col2 then col1 else col2 end as temp_col,\n" +
              "case when col1 = 20000101 and (20170302 - col2) > 10000 then 'D'\n" +
                  "when col2 = 20000101 then 'P' when col1 - col2 > 10000 then '0'\n" +
                  "else 'A' end as status\n" +
          "from dfs.tmp.`vw_order_sample_csv`";

      client.alterSession(ExecConstants.SCALAR_REPLACEMENT_OPTION, ClassTransformer.ScalarReplacementOption.ON.name());

      List<String> compilers = Arrays.asList(ClassCompilerSelector.CompilerPolicy.JANINO.name(),
        ClassCompilerSelector.CompilerPolicy.JDK.name());

      for (String compilerName : compilers) {
        client.alterSession(ClassCompilerSelector.JAVA_COMPILER_OPTION, compilerName);

        testBuilder()
            .sqlQuery(sql)
            .unOrdered()
            .baselineColumns("temp_col", "status")
            .baselineValues(20160301L, "D")
            .go();
      }
    } finally {
      run("drop view if exists dfs.tmp.`vw_order_sample_csv`");
      client.resetSession(ExecConstants.SCALAR_REPLACEMENT_OPTION);
      client.resetSession(ClassCompilerSelector.JAVA_COMPILER_OPTION);
    }
  }
}
