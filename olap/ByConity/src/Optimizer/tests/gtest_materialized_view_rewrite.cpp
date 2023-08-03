/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "gtest_base_tpcds_plan_test.h"
#include "gtest_base_materialized_view_test.h"

#include <gtest/gtest.h>

using namespace DB;

class MaterializedViewRewriteTest : public ::testing::Test
{
public:
    static void SetUpTestSuite()
    {
        std::unordered_map<String, Field> settings;
#ifndef NDEBUG
        // debug mode may time out.
        settings.emplace("cascades_optimizer_timeout", "300000");
#endif
        settings.emplace("enable_materialized_view_join_rewriting", "1");
        settings.emplace("enable_materialized_view_rewrite_verbose_log", "1");

        tester = std::make_shared<BaseMaterializedViewTest>(settings);
    }

    void SetUp() override {
        GTEST_SKIP() << "Skipping all tests for this fixture";
    }

    static MaterializedViewRewriteTester sql(const String & materialize, const String & query)
    {
        return tester->sql(materialize, query);
    }

    static std::shared_ptr<BaseMaterializedViewTest> tester;
};

std::shared_ptr<BaseMaterializedViewTest> MaterializedViewRewriteTest::tester;

TEST_F(MaterializedViewRewriteTest, testFilter)
{
    sql("select * from emps where deptno = 10",
        "select empid + 1 from emps where deptno = 10")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testFilterToProject0)
{
    sql("select *, empid * 2 from emps",
        "select * from emps where (empid * 2) > 3")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testFilterToProject1)
{
    sql("select deptno, salary from emps",
        "select empid, deptno, salary\n"
            "from emps where (salary * 0.8) > 10000")
        .noMat();
}

TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView)
{
    sql("select deptno, empid from emps",
        "select empid + 1 as x from emps where deptno = 10")
        .ok();
}

/** Runs the same test as {@link #testFilterQueryOnProjectView()} but more
   * concisely. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView0)
{
    sql("select deptno, empid from emps",
        "select empid + 1 as x from emps where deptno = 10")
        .ok();
}

/** As {@link #testFilterQueryOnProjectView()} but with extra column in
   * materialized view. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView1)
{
    sql("select deptno, empid, name from emps",
        "select empid + 1 as x from emps where deptno = 10")
        .ok();
}

/** As {@link #testFilterQueryOnProjectView()} but with extra column in both
   * materialized view and query. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView2)
{
    sql("select deptno, empid, name from emps",
        "select empid + 1 as x, name from emps where deptno = 10")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView3)
{
    sql("select deptno - 10 as x, empid + 1, name from emps",
        "select name from emps where deptno - 10 = 0")
        .ok();
}

/** As {@link #testFilterQueryOnProjectView3()} but materialized view cannot
   * be used because it does not contain required expression. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView4)
{
    sql(
        "select deptno - 10 as x, empid + 1, name from emps",
        "select name from emps where deptno + 10 = 20")
        .noMat();
}

/** As {@link #testFilterQueryOnProjectView3()} but also contains an
   * expression column. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView5)
{
    sql("select deptno - 10 as x, empid + 1 as ee, name from emps",
        "select name, empid + 1 as e from emps where deptno - 10 = 2")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [name], e:=ee\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: x = 2\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [ee, name, x]")
        .ok();
}

/** Cannot materialize because "name" is not projected in the MV. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView6)
{
    sql("select deptno - 10 as x, empid  from emps",
        "select name from emps where deptno - 10 = 0")
        .noMat();
}

/** As {@link #testFilterQueryOnProjectView3()} but also contains an
   * expression column. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView7)
{
    sql("select deptno - 10 as x, empid + 1, name from emps",
        "select name, empid + 2 from emps where deptno - 10 = 0")
        .noMat();
}

/** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-988">[CALCITE-988]
   * FilterToProjectUnifyRule.invert(MutableRel, MutableRel, MutableProject)
   * works incorrectly</a>. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnProjectView8)
{
    String mv = ""
        "select salary, commission,\n"
        "deptno, empid, name from emps";
    String query = ""
        "select *\n"
        "from (select * from emps where name is null)\n"
        "where commission is null";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView)
{
    sql("select deptno, empid, name from emps where deptno = 10",
        "select empid + 1 as x, name from emps where deptno = 10")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is stronger in
   * query. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView2)
{
    sql("select deptno, empid, name from emps where deptno = 10",
        "select empid + 1 as x, name from emps "
            "where deptno = 10 and empid < 150")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is weaker in
   * view. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView3)
{
    sql("select deptno, empid, name from emps\n"
            "where deptno = 10 or deptno = 20 or empid < 160",
        "select empid + 1 as x, name from emps where deptno = 10")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [name], x:=empid + 1\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: deptno = 10\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [deptno, empid, name]")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is stronger in
   * query. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView4)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select * from emps where deptno > 10",
        "select name from emps where deptno > 30")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is stronger in
   * query and columns selected are subset of columns in materialized view. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView5)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select name, deptno from emps where deptno > 10",
        "select name from emps where deptno > 30")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is stronger in
   * query and columns selected are subset of columns in materialized view. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView6)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select name, deptno, salary from emps "
            "where salary > 2000.5",
        "select name from emps where deptno > 30 and salary > 3000")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is stronger in
   * query and columns selected are subset of columns in materialized view.
   * Condition here is complex. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView7)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select * from emps where "
            "((salary < 1111.9 and deptno > 10)"
            "or (empid > 400 and salary > 5000) "
            "or salary > 500)",
        "select name from emps where (salary > 1000 "
            "or (deptno >= 30 and salary <= 500))")
        .ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is stronger in
   * query. However, columns selected are not present in columns of materialized
   * view, Hence should not use materialized view. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView8)
{
    sql("select name, deptno from emps where deptno > 10",
        "select name, empid from emps where deptno > 30")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is weaker in
   * query. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView9)
{
    sql("select name, deptno from emps where deptno > 10",
        "select name, empid from emps "
            "where deptno > 30 or empid > 10")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView()} but condition currently
   * has unsupported type being checked on query. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView10)
{
    sql("select name, deptno from emps where deptno > 10 "
            "and name = 'calcite'",
        "select name, empid from emps where deptno > 30 "
            "or empid > 10")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is weaker in
   * query and columns selected are subset of columns in materialized view.
   * Condition here is complex. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView11)
{
    sql("select name, deptno from emps where "
            "(salary < 1111.9 and deptno > 10)"
            "or (empid > 400 and salary > 5000)",
        "select name from emps where deptno > 30 and salary > 3000")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView()} but condition of
   * query is stronger but is on the column not present in MV (salary).
   */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView12)
{
    sql("select name, deptno from emps where salary > 2000.5",
        "select name from emps where deptno > 30 and salary > 3000")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is weaker in
   * query and columns selected are subset of columns in materialized view.
   * Condition here is complex. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView13)
{
    sql("select * from emps where "
            "(salary < 1111.9 and deptno > 10)"
            "or (empid > 400 and salary > 5000)",
        "select name from emps where salary > 1000 "
            "or (deptno > 30 and salary > 3000)")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView7()} but columns in materialized
   * view are a permutation of columns in the query. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView14)
{
    GTEST_SKIP() << "or predicates rewrite is not supported.";
    String q = "select * from emps where (salary > 1000 "
        "or (deptno >= 30 and salary <= 500))";
    String m = "select deptno, empid, name, salary, commission "
        "from emps as em where "
        "((salary < 1111.9 and deptno > 10)"
        "or (empid > 400 and salary > 5000) "
        "or salary > 500)";
    sql(m, q).ok();
}

/** As {@link #testFilterQueryOnFilterView()} but condition is not matched. */
TEST_F(MaterializedViewRewriteTest, testFilterQueryOnFilterView15)
{
    sql("select deptno, empid, name from emps\n"
        "where deptno = 10 or deptno = 20 or empid < 160",
        "select empid + 1 as x, name from emps where deptno = 10 or deptno = 21")
        .noMat();
}

/** As {@link #testFilterQueryOnFilterView13()} but using alias
   * and condition of query is stronger. */
TEST_F(MaterializedViewRewriteTest, testAlias)
{
    GTEST_SKIP() << "or predicates rewrite is not supported && syntax is not supported.";
    sql("select * from emps as em where "
            "(em.salary < 1111.9 and em.deptno > 10)"
            "or (em.empid > 400 and em.salary > 5000)",
        "select name as n from emps as e where "
            "(e.empid > 500 and e.salary > 6000)").ok();
}

/** Aggregation query at same level of aggregation as aggregation
   * materialization. */
TEST_F(MaterializedViewRewriteTest, testAggregate0)
{
    sql("select count(*) as c from emps group by empid",
        "select count(*) + 1 as c from emps group by empid")
        .ok();
}

/**
   * Aggregation query at same level of aggregation as aggregation
   * materialization but with different row types. */
TEST_F(MaterializedViewRewriteTest, testAggregate1)
{
    sql("select count(*) as c0 from emps group by empid",
        "select count(*) as c1 from emps group by empid")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregate2)
{
    sql("select deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by deptno",
        "select count(*) + 1 as c, deptno from emps group by deptno")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregate3)
{
    String mv = ""
        "select deptno, sum(salary), sum(commission), sum(k)\n"
        "from\n"
        "  (select deptno, salary, commission, 100 as k\n"
        "  from emps)\n"
        "group by deptno";
    String query = ""
        "select deptno, sum(salary), sum(k)\n"
        "from\n"
        "  (select deptno, salary, 100 as k\n"
        "  from emps)\n"
        "group by deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregate4)
{
    String mv = ""
        "select deptno, commission, sum(salary)\n"
        "from emps\n"
        "group by deptno, commission";
    String query = ""
        "select deptno, sum(salary)\n"
        "from emps\n"
        "where commission = 100\n"
        "group by deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregate5)
{
    String mv = ""
        "select deptno + commission, commission, sum(salary)\n"
        "from emps\n"
        "group by deptno + commission, commission";
    String query = ""
        "select commission, sum(salary)\n"
        "from emps\n"
        "where commission * (deptno + commission) = 100\n"
        "group by commission";
    sql(mv, query).ok();
}

/**
   * Matching failed because the filtering condition under Aggregate
   * references columns for aggregation.
   */
TEST_F(MaterializedViewRewriteTest, testAggregate6)
{
    String mv = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary, sum(commission)\n"
        "from emps\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    String query = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary\n"
        "from emps\n"
        "where salary > 1000\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    sql(mv, query).noMat();
}

/**
   * There will be a compensating Project added after matching of the Aggregate.
   * This rule targets to test if the Calc can be handled.
   */
TEST_F(MaterializedViewRewriteTest,
       DISABLED_testCompensatingCalcWithAggregate0)
{
    String mv = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary, sum(commission)\n"
        "from emps\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    String query = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary\n"
        "from emps\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    sql(mv, query).ok();
}

/**
   * There will be a compensating Project + Filter added after matching of the Aggregate.
   * This rule targets to test if the Calc can be handled.
   */
TEST_F(MaterializedViewRewriteTest, DISABLED_testCompensatingCalcWithAggregate1)
{
    String mv = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary, sum(commission)\n"
        "from emps\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    String query = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary\n"
        "from emps\n"
        "where deptno >=20\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    sql(mv, query).ok();
}

/**
   * There will be a compensating Project + Filter added after matching of the Aggregate.
   * This rule targets to test if the Calc can be handled.
   */
TEST_F(MaterializedViewRewriteTest, testCompensatingCalcWithAggregate2)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    String mv = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary, sum(commission)\n"
        "from emps\n"
        "where deptno >= 10\n"
        "group by deptno)\n"
        "where sum_salary > 10";
    String query = ""
        "select * from\n"
        "(select deptno, sum(salary) as sum_salary\n"
        "from emps\n"
        "where deptno >= 20\n"
        "group by deptno)\n"
        "where sum_salary > 20";
    sql(mv, query).ok();
}

/** Aggregation query at same level of aggregation as aggregation
   * materialization with grouping sets. */
TEST_F(MaterializedViewRewriteTest, testAggregateGroupSets1)
{
    GTEST_SKIP() << "cute rollup rewrite is not implemented.";
    sql("select empid, deptno, count(*) as c, sum(salary) as s\n"
            "from emps group by cube(empid,deptno)",
        "select count(*) + 1 as c, deptno\n"
            "from emps group by cube(empid,deptno)")
        .ok();
}

/** Aggregation query with different grouping sets, should not
   * do materialization. */
TEST_F(MaterializedViewRewriteTest, testAggregateGroupSets2)
{
    sql("select empid, deptno, count(*) as c, sum(salary) as s\n"
            "from emps group by cube(empid,deptno)",
        "select count(*) + 1 as c, deptno\n"
            "from emps group by rollup(empid,deptno)")
        .noMat();
}

/** Aggregation query at coarser level of aggregation than aggregation
   * materialization. Requires an additional aggregate to roll up. Note that
   * COUNT is rolled up using SUM0. */
TEST_F(MaterializedViewRewriteTest, testAggregateRollUp1)
{
    sql("select empid, deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by empid, deptno",
        "select count(*) + 1 as c, deptno from emps group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: c:=`expr#sum()(c)` + 1, deptno:=`expr#deptno`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {expr#deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {expr#deptno}\n"
                                    "            │     Aggregates: expr#sum()(c):=AggNull(sum)(expr#c)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: expr#c:=c, expr#deptno:=deptno\n"
                                    "               └─ Filter\n"
                                    "                  │     Condition: 1\n"
                                    "                  └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                           Outputs: [c, deptno]")
        .ok();
}

/**
   * stddev_pop aggregate function does not support roll up.
   */
TEST_F(MaterializedViewRewriteTest, testAggregateRollUp2)
{
    String mv = ""
        "select empid, stddev_pop(deptno) "
        "from emps "
        "group by empid, deptno";
    String query = ""
        "select empid, stddev_pop(deptno) "
        "from emps "
        "group by empid";
    sql(mv, query).noMat();
}

/** Aggregation query with groupSets at coarser level of aggregation than
   * aggregation materialization. Requires an additional aggregate to roll up.
   * Note that COUNT is rolled up using SUM0. */
TEST_F(MaterializedViewRewriteTest, testAggregateGroupSetsRollUp)
{
    GTEST_SKIP() << "cute rewrite is not implemented.";
    sql("select empid, deptno, count(*) as c, sum(salary) as s\n"
            "from emps group by empid, deptno",
        "select count(*) + 1 as c, deptno\n"
            "from emps group by cube(empid,deptno)")
        .checkingThatResultContains(""
                                    "LogicalCalc(expr#0..2=[{inputs}], expr#3=[1], "
                                    "expr#4=[+($t2, $t3)], C=[$t4], deptno=[$t1])\n"
                                    "  LogicalAggregate(group=[{0, 1}], groups=[[{0, 1}, {0}, {1}, {}]], agg#0=[$SUM0($2)])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregateGroupSetsRollUp2)
{
    GTEST_SKIP() << "cute rewrite is not implemented.";
    sql("select empid, deptno, count(*) as c, sum(empid) as s from emps "
            "group by empid, deptno",
        "select count(*) + 1 as c,  deptno from emps group by cube(empid,deptno)")
        .checkingThatResultContains(""
                                    "LogicalCalc(expr#0..2=[{inputs}], expr#3=[1], "
                                    "expr#4=[+($t2, $t3)], C=[$t4], deptno=[$t1])\n"
                                    "  LogicalAggregate(group=[{0, 1}], groups=[[{0, 1}, {0}, {1}, {}]], agg#0=[$SUM0($2)])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

/** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-3087">[CALCITE-3087]
   * AggregateOnProjectToAggregateUnifyRule ignores Project incorrectly when its
   * Mapping breaks ordering</a>. */
TEST_F(MaterializedViewRewriteTest, testAggregateOnProject1)
{
    sql("select empid, deptno, count(*) as c, sum(empid) as s from emps "
            "group by empid, deptno",
        "select count(*) + 1 as c, deptno from emps group by deptno, empid");
}

TEST_F(MaterializedViewRewriteTest, testAggregateOnProject2)
{
    GTEST_SKIP() << "cute rewrite is not implemented.";
    sql("select empid, deptno, count(*) as c, sum(salary) as s from emps "
            "group by empid, deptno",
        "select count(*) + 1 as c,  deptno from emps group by cube(deptno, empid)")
        .checkingThatResultContains(""
                                    "LogicalCalc(expr#0..2=[{inputs}], expr#3=[1], "
                                    "expr#4=[+($t2, $t3)], C=[$t4], deptno=[$t1])\n"
                                    "  LogicalAggregate(group=[{0, 1}], groups=[[{0, 1}, {0}, {1}, {}]], agg#0=[$SUM0($2)])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregateOnProject3)
{
    GTEST_SKIP() << "cute rewrite is not implemented.";
    sql("select empid, deptno, count(*) as c, sum(salary) as s\n"
            "from emps group by empid, deptno",
        "select count(*) + 1 as c,  deptno\n"
            "from emps group by rollup(deptno, empid)")
        .checkingThatResultContains(""
                                    "LogicalCalc(expr#0..2=[{inputs}], expr#3=[1], "
                                    "expr#4=[+($t2, $t3)], C=[$t4], deptno=[$t1])\n"
                                    "  LogicalAggregate(group=[{0, 1}], groups=[[{0, 1}, {1}, {}]], agg#0=[$SUM0($2)])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregateOnProject4)
{
    GTEST_SKIP() << "cute rewrite is not implemented.";
    sql("select salary, empid, deptno, count(*) as c, sum(commission) as s\n"
            "from emps group by salary, empid, deptno",
        "select count(*) + 1 as c,  deptno\n"
            "from emps group by rollup(empid, deptno, salary)")
        .checkingThatResultContains(""
                                    "LogicalCalc(expr#0..3=[{inputs}], expr#4=[1], "
                                    "expr#5=[+($t3, $t4)], C=[$t5], deptno=[$t2])\n"
                                    "  LogicalAggregate(group=[{0, 1, 2}], groups=[[{0, 1, 2}, {1, 2}, {1}, {}]], agg#0=[$SUM0($3)])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

/** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-3448">[CALCITE-3448]
   * AggregateOnCalcToAggregateUnifyRule ignores Project incorrectly when
   * there's missing grouping or mapping breaks ordering</a>. */
TEST_F(MaterializedViewRewriteTest, testAggregateOnProject5)
{
    sql("select empid, deptno, name, count(*) from emps\n"
            "group by empid, deptno, name",
        "select name, empid, count(*) from emps group by name, empid")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: count():=`expr#sum()(count())`, empid:=`expr#empid`, name:=`expr#name`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {expr#empid, expr#name}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {expr#empid, expr#name}\n"
                                    "            │     Aggregates: expr#sum()(count()):=AggNull(sum)(expr#count()_2)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: expr#count()_2:=`count()`, expr#empid:=empid, expr#name:=name\n"
                                    "               └─ Filter\n"
                                    "                  │     Condition: 1\n"
                                    "                  └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                           Outputs: [count(), empid, name]")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testAggregateOnProjectAndFilter)
{
    String mv = ""
        "select deptno, sum(salary), count(1)\n"
        "from emps\n"
        "group by deptno";
    String query = ""
        "select deptno, count(1)\n"
        "from emps\n"
        "where deptno = 10\n"
        "group by deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testProjectOnProject)
{
    String mv = ""
        "select deptno, sum(salary) + 2, sum(commission)\n"
        "from emps\n"
        "group by deptno";
    String query = ""
        "select deptno, sum(salary) + 2\n"
        "from emps\n"
        "group by deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testPermutationError)
{
    sql("select min(salary), count(*), max(salary), sum(salary), empid "
            "from emps group by empid",
        "select count(*), empid from emps group by empid")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnLeftProjectToJoin)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select * from\n"
        "  (select deptno, sum(salary), sum(commission)\n"
        "  from emps\n"
        "  group by deptno) A\n"
        "  join\n"
        "  (select deptno, count(name)\n"
        "  from depts\n"
        "  group by deptno) B\n"
        "  on A.deptno = B.deptno";
    String query = ""
        "select * from\n"
        "  (select deptno, sum(salary)\n"
        "  from emps\n"
        "  group by deptno) A\n"
        "  join\n"
        "  (select deptno, count(name)\n"
        "  from depts\n"
        "  group by deptno) B\n"
        "  on A.deptno = B.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnRightProjectToJoin)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select * from\n"
        "  (select deptno, sum(salary), sum(commission)\n"
        "  from emps\n"
        "  group by deptno) A\n"
        "  join\n"
        "  (select deptno, count(name)\n"
        "  from depts\n"
        "  group by deptno) B\n"
        "  on A.deptno = B.deptno";
    String query = ""
        "select * from\n"
        "  (select deptno, sum(salary), sum(commission)\n"
        "  from emps\n"
        "  group by deptno) A\n"
        "  join\n"
        "  (select deptno\n"
        "  from depts\n"
        "  group by deptno) B\n"
        "  on A.deptno = B.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnProjectsToJoin)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select * from\n"
        "  (select deptno, sum(salary), sum(commission)\n"
        "  from emps\n"
        "  group by deptno) A\n"
        "  join\n"
        "  (select deptno, count(name)\n"
        "  from depts\n"
        "  group by deptno) B\n"
        "  on A.deptno = B.deptno";
    String query = ""
        "select * from\n"
        "  (select deptno, sum(salary)\n"
        "  from emps\n"
        "  group by deptno) A\n"
        "  join\n"
        "  (select deptno\n"
        "  from depts\n"
        "  group by deptno) B\n"
        "  on A.deptno = B.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnCalcToJoin0)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select emps.empid, emps.deptno, depts.deptno from\n"
        "emps join depts\n"
        "on emps.deptno = depts.deptno";
    String query = ""
        "select A.empid, A.deptno, depts.deptno from\n"
        " (select empid, deptno from emps where deptno > 10) A"
        " join depts\n"
        "on A.deptno = depts.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnCalcToJoin1)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select emps.empid, emps.deptno, depts.deptno from\n"
        "emps join depts\n"
        "on emps.deptno = depts.deptno";
    String query = ""
        "select emps.empid, emps.deptno, B.deptno from\n"
        "emps join\n"
        "(select deptno from depts where deptno > 10) B\n"
        "on emps.deptno = B.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnCalcToJoin2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select emps.empid, emps.deptno, depts.deptno from\n"
        "emps join depts\n"
        "on emps.deptno = depts.deptno";
    String query = ""
        "select * from\n"
        "(select empid, deptno from emps where empid > 10) A\n"
        "join\n"
        "(select deptno from depts where deptno > 10) B\n"
        "on A.deptno = B.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnCalcToJoin3)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select emps.empid, emps.deptno, depts.deptno from\n"
        "emps join depts\n"
        "on emps.deptno = depts.deptno";
    String query = ""
        "select * from\n"
        "(select empid, deptno + 1 as deptno from emps where empid > 10) A\n"
        "join\n"
        "(select deptno from depts where deptno > 10) B\n"
        "on A.deptno = B.deptno";
    // Match failure because join condition references non-mapping projects.
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testJoinOnCalcToJoin4)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = ""
        "select emps.empid, emps.deptno, depts.deptno from\n"
        "emps join depts\n"
        "on emps.deptno = depts.deptno";
    String query = ""
        "select * from\n"
        "(select empid, deptno from emps where empid is not null) A\n"
        "full join\n"
        "(select deptno from depts where deptno is not null) B\n"
        "on A.deptno = B.deptno";
    // Match failure because of outer join type but filtering condition in Calc is not empty.
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testJoinMaterialization)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String q = "select *\n"
        "from (select * from emps where empid < 300)\n"
        "join depts using (deptno)";
    sql("select * from emps where empid < 500", q).ok();
}

/** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-891">[CALCITE-891]
   * TableScan without Project cannot be substituted by any projected
   * materialization</a>. */
TEST_F(MaterializedViewRewriteTest, testJoinMaterialization2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String q = "select *\n"
        "from emps\n"
        "join depts using (deptno)";
    String m = "select deptno, empid, name,\n"
        "salary, commission from emps";
    sql(m, q).ok();
}

TEST_F(MaterializedViewRewriteTest, testJoinMaterialization3)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String q = "select empid deptno from emps\n"
        "join depts using (deptno) where empid = 1";
    String m = "select empid deptno from emps\n"
        "join depts using (deptno)";
    sql(m, q).ok();
}

TEST_F(MaterializedViewRewriteTest, testUnionAll)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String q = "select * from emps where empid > 300\n"
        "union all select * from emps where empid < 200";
    String m = "select * from emps where empid < 500";
    sql(m, q)
        .checkingThatResultContains(""
                                    "LogicalUnion(all=[true])\n"
                                    "  LogicalCalc(expr#0..4=[{inputs}], expr#5=[300], expr#6=[>($t0, $t5)], proj#0..4=[{exprs}], $condition=[$t6])\n"
                                    "    LogicalTableScan(table=[[hr, emps]])\n"
                                    "  LogicalCalc(expr#0..4=[{inputs}], expr#5=[200], expr#6=[<($t0, $t5)], proj#0..4=[{exprs}], $condition=[$t6])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testTableModify)
{
    GTEST_SKIP() << "upsert into is not supported";
    String m = "select deptno, empid, name"
        "from emps where deptno = 10";
    String q = "upsert into dependents"
        "select empid + 1 as x, name"
        "from emps where deptno = 10";
    sql(m, q).ok();
}

TEST_F(MaterializedViewRewriteTest, testSingleMaterializationMultiUsage)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    String q = "select *\n"
        "from (select * from emps where empid < 300)\n"
        "join (select * from emps where empid < 200) using (empid)";
    String m = "select * from emps where empid < 500";
    sql(m, q)
        .checkingThatResultContains(""
                                    "LogicalCalc(expr#0..9=[{inputs}], proj#0..4=[{exprs}], deptno0=[$t6], name0=[$t7], salary0=[$t8], commission0=[$t9])\n"
                                    "  LogicalJoin(condition=[=($0, $5)], joinType=[inner])\n"
                                    "    LogicalCalc(expr#0..4=[{inputs}], expr#5=[300], expr#6=[<($t0, $t5)], proj#0..4=[{exprs}], $condition=[$t6])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "    LogicalCalc(expr#0..4=[{inputs}], expr#5=[200], expr#6=[<($t0, $t5)], proj#0..4=[{exprs}], $condition=[$t6])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testMaterializationOnJoinQuery)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select * from emps where empid < 500",
        "select *\n"
            "from emps\n"
            "join depts using (deptno) where empid < 300 ")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testMaterializationAfterTrimingOfUnusedFields)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String sql0 =
        "select y.deptno, y.name, x.sum_salary\n"
        "from\n"
        "  (select deptno, sum(salary) sum_salary\n"
        "  from emps\n"
        "  group by deptno) x\n"
        "  join\n"
        "  depts y\n"
        "  on x.deptno=y.deptno\n";
    sql(sql0, sql0).ok();
}

TEST_F(MaterializedViewRewriteTest, testUnionAllToUnionAll)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String sql0 = "select * from emps where empid < 300";
    String sql1 = "select * from emps where empid > 200";
    sql(sql0 + " union all " + sql1, sql1 + " union all " + sql0).ok();
}

TEST_F(MaterializedViewRewriteTest, testUnionDistinctToUnionDistinct)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String sql0 = "select * from emps where empid < 300";
    String sql1 = "select * from emps where empid > 200";
    sql(sql0 + " union " + sql1, sql1 + " union " + sql0).ok();
}

TEST_F(MaterializedViewRewriteTest, testUnionDistinctToUnionAll)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String sql0 = "select * from emps where empid < 300";
    String sql1 = "select * from emps where empid > 200";
    sql(sql0 + " union " + sql1, sql0 + " union all " + sql1).noMat();
}

TEST_F(MaterializedViewRewriteTest, testUnionOnCalcsToUnion)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String mv = ""
        "select deptno, salary\n"
        "from emps\n"
        "where empid > 300\n"
        "union all\n"
        "select deptno, salary\n"
        "from emps\n"
        "where empid < 100";
    String query = ""
        "select deptno, salary * 2\n"
        "from emps\n"
        "where empid > 300 and salary > 100\n"
        "union all\n"
        "select deptno, salary * 2\n"
        "from emps\n"
        "where empid < 100 and salary > 100";
    sql(mv, query).ok();
}


TEST_F(MaterializedViewRewriteTest, testIntersectOnCalcsToIntersect)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String mv = ""
        "select deptno, salary\n"
        "from emps\n"
        "where empid > 300\n"
        "intersect all\n"
        "select deptno, salary\n"
        "from emps\n"
        "where empid < 100";
    String query = ""
        "select deptno, salary * 2\n"
        "from emps\n"
        "where empid > 300 and salary > 100\n"
        "intersect all\n"
        "select deptno, salary * 2\n"
        "from emps\n"
        "where empid < 100 and salary > 100";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testIntersectToIntersect0)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String mv = ""
        "select deptno from emps\n"
        "intersect\n"
        "select deptno from depts";
    String query = ""
        "select deptno from depts\n"
        "intersect\n"
        "select deptno from emps";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testIntersectToIntersect1)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String mv = ""
        "select deptno from emps\n"
        "intersect all\n"
        "select deptno from depts";
    String query = ""
        "select deptno from depts\n"
        "intersect all\n"
        "select deptno from emps";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testIntersectToCalcOnIntersect)
{
    GTEST_SKIP() << "union rewrite is not implemented.";
    String intersect = ""
        "select deptno,name from emps\n"
        "intersect all\n"
        "select deptno,name from depts";
    String mv = "select name, deptno from (" + intersect + ")";

    String query = ""
        "select name,deptno from depts\n"
        "intersect all\n"
        "select name,deptno from emps";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testConstantFilterInAgg)
{
    String mv = ""
        "select name, count(distinct deptno) as cnt\n"
        "from emps group by name";
    String query = ""
        "select count(distinct deptno) as cnt\n"
        "from emps where name = 'hello'";
    sql(mv, query)
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [cnt]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: name = 'hello'\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [cnt, name]")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testConstantFilterInAgg2)
{
    String mv = ""
        "select name, deptno, count(distinct commission) as cnt\n"
        "from emps\n"
        " group by name, deptno";
    String query = ""
        "select deptno, count(distinct commission) as cnt\n"
        "from emps where name = 'hello'\n"
        "group by deptno";
    sql(mv, query)
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [cnt, deptno]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: name = 'hello'\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [cnt, deptno, name]")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testConstantFilterInAgg3)
{
    String mv = ""
        "select name, deptno, count(distinct commission) as cnt\n"
        "from emps\n"
        " group by name, deptno";
    String query = ""
        "select deptno, count(distinct commission) as cnt\n"
        "from emps where name = 'hello' and deptno = 1\n"
        "group by deptno";
    sql(mv, query)
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [cnt, deptno]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: (name = 'hello') AND (deptno = 1)\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [cnt, deptno, name]")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testConstantFilterInAgg4)
{
    String mv = ""
        "select name, deptno, count(distinct commission) as cnt\n"
        "from emps\n"
        " group by name, deptno";
    String query = ""
        "select deptno, commission, count(distinct commission) as cnt\n"
        "from emps where name = 'hello' and deptno = 1\n"
        "group by deptno, commission";
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testConstantFilterInAggUsingSubquery)
{
    String mv = ""
        "select name, count(distinct deptno) as cnt "
        "from emps group by name";
    String query = ""
        "select cnt from(\n"
        " select name, count(distinct deptno) as cnt "
        " from emps group by name) t\n"
        "where name = 'hello'";
    sql(mv, query).ok();
}
/** Unit test for FilterBottomJoin can be pulled up. */
TEST_F(MaterializedViewRewriteTest, testLeftFilterOnLeftJoinToJoinOk1)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "left join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps where empid > 10) t1\n"
        "left join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testLeftFilterOnLeftJoinToJoinOk2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps where empid > 10) t1\n"
        "left join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps where empid > 30) t1\n"
        "left join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testRightFilterOnLeftJoinToJoinFail)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "left join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "left join (select deptno, name from depts where name is not null) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testRightFilterOnRightJoinToJoinOk)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "right join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "right join (select deptno, name from depts where name is not null) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testLeftFilterOnRightJoinToJoinFail)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "right join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps where empid > 30) t1\n"
        "right join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testLeftFilterOnFullJoinToJoinFail)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "full join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps where empid > 30) t1\n"
        "full join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testRightFilterOnFullJoinToJoinFail)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String mv = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "full join (select deptno, name from depts) t2\n"
        "on t1.deptno = t2.deptno";
    String query = "select * from \n"
        "(select empid, deptno, name from emps) t1\n"
        "full join (select deptno, name from depts where name is not null) t2\n"
        "on t1.deptno = t2.deptno";
    sql(mv, query).noMat();
}

TEST_F(MaterializedViewRewriteTest, testMoreSameExprInMv)
{
    String mv = ""
        "select empid, deptno, sum(empid) as s1, sum(empid) as s2, count(*) as c\n"
        "from emps group by empid, deptno";
    String query = ""
        "select sum(empid), count(*) from emps group by empid, deptno";
    sql(mv, query).ok();
}

/**
   * It's match, distinct agg-call could be expressed by mv's grouping.
   */
TEST_F(MaterializedViewRewriteTest, testAggDistinctInMvGrouping)
{
    String mv = ""
        "select deptno, name\n"
        "from emps group by deptno, name";
    String query = ""
        "select deptno, name, count(distinct name)\n"
        "from emps group by deptno, name";
    sql(mv, query).ok();
}

/**
   * It's match, `Optionality.IGNORED` agg-call could be expressed by mv's grouping.
   */
TEST_F(MaterializedViewRewriteTest, testAggOptionalityInMvGrouping)
{
    String mv = ""
        "select deptno, salary\n"
        "from emps group by deptno, salary";
    String query = ""
        "select deptno, salary, max(salary)\n"
        "from emps group by deptno, salary";
    sql(mv, query).ok();
}

/**
   * It's not match, normal agg-call could be expressed by mv's grouping.
   * Such as: sum, count
   */
TEST_F(MaterializedViewRewriteTest, testAggNormalInMvGrouping)
{
    String mv = ""
        "select deptno, salary\n"
        "from emps group by deptno, salary";
    String query = ""
        "select deptno, sum(salary)\n"
        "from emps group by deptno";
    sql(mv, query).noMat();
}

/**
   * It's not match, which is count(*) with same grouping.
   */
TEST_F(MaterializedViewRewriteTest, testGenerateQueryAggCallByMvGroupingForEmptyArg1)
{
    String mv = ""
        "select deptno\n"
        "from emps group by deptno";
    String query = ""
        "select deptno, count(*)\n"
        "from emps group by deptno";
    sql(mv, query).noMat();
}

/**
   * It's not match, which is count(*) with rollup grouping.
   */
TEST_F(MaterializedViewRewriteTest, testGenerateQueryAggCallByMvGroupingForEmptyArg2)
{
    String mv = ""
        "select deptno, commission, salary\n"
        "from emps group by deptno, commission, salary";
    String query = ""
        "select deptno, commission, count(*)\n"
        "from emps group by deptno, commission";
    sql(mv, query).noMat();
}

/**
   * It's match, when query's agg-calls could be both rollup and expressed by mv's grouping.
   */
TEST_F(MaterializedViewRewriteTest, testAggCallBothGenByMvGroupingAndRollupOk)
{
    String mv = ""
        "select name, deptno, empid, min(commission)\n"
        "from emps group by name, deptno, empid";
    String query = ""
        "select name, max(deptno), count(distinct empid), min(commission)\n"
        "from emps group by name";
    sql(mv, query).ok();
}

/** Unit test for logic functions
   * {@link org.apache.calcite.plan.SubstitutionVisitor#mayBeSatisfiable} and
   * {@link RexUtil#simplify}. */
TEST_F(MaterializedViewRewriteTest, testSatisfiable)
{
    GTEST_SKIP();
//    final SatisfiabilityFixture f = new SatisfiabilityFixture();
//    final RexBuilder rexBuilder = f.rexBuilder;
//
//    // TRUE may be satisfiable
//    f.checkSatisfiable(rexBuilder.makeLiteral(true), "true");
//
//    // FALSE is not satisfiable
//    f.checkNotSatisfiable(rexBuilder.makeLiteral(false));
//
//    // The expression "$0 = 1".
//    final RexNode i0_eq_0 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.EQUALS,
//            rexBuilder.makeInputRef(
//                f.typeFactory.createType(int.class), 0),
//            rexBuilder.makeExactLiteral(BigDecimal.ZERO));
//
//    // "$0 = 1" may be satisfiable
//    f.checkSatisfiable(i0_eq_0, "=($0, 0)");
//
//    // "$0 = 1 AND TRUE" may be satisfiable
//    final RexNode e0 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeLiteral(true));
//    f.checkSatisfiable(e0, "=($0, 0)");
//
//    // "$0 = 1 AND FALSE" is not satisfiable
//    final RexNode e1 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeLiteral(false));
//    f.checkNotSatisfiable(e1);
//
//    // "$0 = 0 AND NOT $0 = 0" is not satisfiable
//    final RexNode e2 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.NOT,
//                i0_eq_0));
//    f.checkNotSatisfiable(e2);
//
//    // "TRUE AND NOT $0 = 0" may be satisfiable. Can simplify.
//    final RexNode e3 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            rexBuilder.makeLiteral(true),
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.NOT,
//                i0_eq_0));
//    f.checkSatisfiable(e3, "<>($0, 0)");
//
//    // The expression "$1 = 1".
//    final RexNode i1_eq_1 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.EQUALS,
//            rexBuilder.makeInputRef(
//                f.typeFactory.createType(int.class), 1),
//            rexBuilder.makeExactLiteral(BigDecimal.ONE));
//
//    // "$0 = 0 AND $1 = 1 AND NOT $0 = 0" is not satisfiable
//    final RexNode e4 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.AND,
//                i1_eq_1,
//                rexBuilder.makeCall(
//                    SqlStdOperatorTable.NOT, i0_eq_0)));
//    f.checkNotSatisfiable(e4);
//
//    // "$0 = 0 AND NOT $1 = 1" may be satisfiable. Can't simplify.
//    final RexNode e5 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.NOT,
//                i1_eq_1));
//    f.checkSatisfiable(e5, "AND(=($0, 0), <>($1, 1))");
//
//    // "$0 = 0 AND NOT ($0 = 0 AND $1 = 1)" may be satisfiable. Can simplify.
//    final RexNode e6 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.NOT,
//                rexBuilder.makeCall(
//                    SqlStdOperatorTable.AND,
//                    i0_eq_0,
//                    i1_eq_1)));
//    f.checkSatisfiable(e6, "AND(=($0, 0), <>($1, 1))");
//
//    // "$0 = 0 AND ($1 = 1 AND NOT ($0 = 0))" is not satisfiable.
//    final RexNode e7 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.AND,
//                i1_eq_1,
//                rexBuilder.makeCall(
//                    SqlStdOperatorTable.NOT,
//                    i0_eq_0)));
//    f.checkNotSatisfiable(e7);
//
//    // The expression "$2".
//    final RexInputRef i2 =
//        rexBuilder.makeInputRef(
//            f.typeFactory.createType(boolean.class), 2);
//
//    // The expression "$3".
//    final RexInputRef i3 =
//        rexBuilder.makeInputRef(
//            f.typeFactory.createType(boolean.class), 3);
//
//    // The expression "$4".
//    final RexInputRef i4 =
//        rexBuilder.makeInputRef(
//            f.typeFactory.createType(boolean.class), 4);
//
//    // "$0 = 0 AND $2 AND $3 AND NOT ($2 AND $3 AND $4) AND NOT ($2 AND $4)" may
//    // be satisfiable. Can't simplify.
//    final RexNode e8 =
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.AND,
//            i0_eq_0,
//            rexBuilder.makeCall(
//                SqlStdOperatorTable.AND,
//                i2,
//                rexBuilder.makeCall(
//                    SqlStdOperatorTable.AND,
//                    i3,
//                    rexBuilder.makeCall(
//                        SqlStdOperatorTable.NOT,
//                        rexBuilder.makeCall(
//                            SqlStdOperatorTable.AND,
//                            i2,
//                            i3,
//                            i4)),
//                    rexBuilder.makeCall(
//                        SqlStdOperatorTable.NOT,
//                        i4))));
//    f.checkSatisfiable(e8,
//                       "AND(=($0, 0), $2, $3, OR(NOT($2), NOT($3), NOT($4)), NOT($4))");
}

TEST_F(MaterializedViewRewriteTest, testSplitFilter)
{
    GTEST_SKIP();
//    final SatisfiabilityFixture f = new SatisfiabilityFixture();
//    final RexBuilder rexBuilder = f.rexBuilder;
//    final RexSimplify simplify = f.simplify;
//
//    final RexLiteral i1 = rexBuilder.makeExactLiteral(BigDecimal.ONE);
//    final RexLiteral i2 = rexBuilder.makeExactLiteral(BigDecimal.valueOf(2));
//    final RexLiteral i3 = rexBuilder.makeExactLiteral(BigDecimal.valueOf(3));
//
//    final RelDataType intType = f.typeFactory.createType(int.class);
//    final RexInputRef x = rexBuilder.makeInputRef(intType, 0); // $0
//    final RexInputRef y = rexBuilder.makeInputRef(intType, 1); // $1
//    final RexInputRef z = rexBuilder.makeInputRef(intType, 2); // $2
//
//    final RexNode x_eq_1 =
//        rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, x, i1); // $0 = 1
//    final RexNode x_eq_1_b =
//        rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, i1, x); // 1 = $0
//    final RexNode x_eq_2 =
//        rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, x, i2); // $0 = 2
//    final RexNode y_eq_2 =
//        rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, y, i2); // $1 = 2
//    final RexNode z_eq_3 =
//        rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, z, i3); // $2 = 3
//
//    final RexNode x_plus_y_gt =  // x + y > 2
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.GREATER_THAN,
//            rexBuilder.makeCall(SqlStdOperatorTable.PLUS, x, y),
//            i2);
//    final RexNode y_plus_x_gt =  // y + x > 2
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.GREATER_THAN,
//            rexBuilder.makeCall(SqlStdOperatorTable.PLUS, y, x),
//            i2);
//
//    final RexNode x_times_y_gt = // x*y > 2
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.GREATER_THAN,
//            rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY, x, y),
//            i2);
//
//    final RexNode y_times_x_gt = // 2 < y*x
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.LESS_THAN,
//            i2,
//            rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY, y, x));
//
//    final RexNode x_plus_x_gt =  // x + x > 2
//        rexBuilder.makeCall(
//            SqlStdOperatorTable.GREATER_THAN,
//            rexBuilder.makeCall(SqlStdOperatorTable.PLUS, x, y),
//            i2);
//
//    RexNode newFilter;
//
//    // Example 1.
//    //   condition: x = 1 or y = 2
//    //   target:    y = 2 or 1 = x
//    // yields
//    //   residue:   true
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, y_eq_2),
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, y_eq_2, x_eq_1_b));
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
//
//    // Example 2.
//    //   condition: x = 1,
//    //   target:    x = 1 or z = 3
//    // yields
//    //   residue:   x = 1
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_eq_1,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, z_eq_3));
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.toString(), equalTo("=($0, 1)"));
//
//    // 2b.
//    //   condition: x = 1 or y = 2
//    //   target:    x = 1 or y = 2 or z = 3
//    // yields
//    //   residue:   x = 1 or y = 2
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, y_eq_2),
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, y_eq_2, z_eq_3));
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.toString(), equalTo("OR(=($0, 1), =($1, 2))"));
//
//    // 2c.
//    //   condition: x = 1
//    //   target:    x = 1 or y = 2 or z = 3
//    // yields
//    //   residue:   x = 1
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_eq_1,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, y_eq_2, z_eq_3));
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.toString(),
//               equalTo("=($0, 1)"));
//
//    // 2d.
//    //   condition: x = 1 or y = 2
//    //   target:    y = 2 or x = 1
//    // yields
//    //   residue:   true
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, y_eq_2),
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, y_eq_2, x_eq_1));
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
//
//    // 2e.
//    //   condition: x = 1
//    //   target:    x = 1 (different object)
//    // yields
//    //   residue:   true
//    newFilter = SubstitutionVisitor.splitFilter(simplify, x_eq_1, x_eq_1_b);
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
//
//    // 2f.
//    //   condition: x = 1 or y = 2
//    //   target:    x = 1
//    // yields
//    //   residue:   null
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.OR, x_eq_1, y_eq_2),
//                                                x_eq_1);
//    assertNull(newFilter);
//
//    // Example 3.
//    // Condition [x = 1 and y = 2],
//    // target [y = 2 and x = 1] yields
//    // residue [true].
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.AND, x_eq_1, y_eq_2),
//                                                rexBuilder.makeCall(SqlStdOperatorTable.AND, y_eq_2, x_eq_1));
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
//
//    // Example 4.
//    //   condition: x = 1 and y = 2
//    //   target:    y = 2
//    // yields
//    //   residue:   x = 1
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.AND, x_eq_1, y_eq_2),
//                                                y_eq_2);
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.toString(), equalTo("=($0, 1)"));
//
//    // Example 5.
//    //   condition: x = 1
//    //   target:    x = 1 and y = 2
//    // yields
//    //   residue:   null
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_eq_1,
//                                                rexBuilder.makeCall(SqlStdOperatorTable.AND, x_eq_1, y_eq_2));
//    assertNull(newFilter);
//
//    // Example 6.
//    //   condition: x = 1
//    //   target:    y = 2
//    // yields
//    //   residue:   null
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_eq_1,
//                                                y_eq_2);
//    assertNull(newFilter);
//
//    // Example 7.
//    //   condition: x = 1
//    //   target:    x = 2
//    // yields
//    //   residue:   null
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_eq_1,
//                                                x_eq_2);
//    assertNull(newFilter);
//
//    // Example 8.
//    //   condition: x + y > 2
//    //   target:    y + x > 2
//    // yields
//    //   residue:  true
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_plus_y_gt,
//                                                y_plus_x_gt);
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
//
//    // Example 9.
//    //   condition: x + x > 2
//    //   target:    x + x > 2
//    // yields
//    //   residue:  true
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_plus_x_gt,
//                                                x_plus_x_gt);
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
//
//    // Example 10.
//    //   condition: x * y > 2
//    //   target:    2 < y * x
//    // yields
//    //   residue:  true
//    newFilter = SubstitutionVisitor.splitFilter(simplify,
//                                                x_times_y_gt,
//                                                y_times_x_gt);
//    assertThat(newFilter, notNullValue());
//    assertThat(newFilter.isAlwaysTrue(), equalTo(true));
}

TEST_F(MaterializedViewRewriteTest, testSubQuery)
{
    String q = "select empid, deptno, salary from emps e1\n"
        "where empid = (\n"
        "  select max(empid) from emps\n"
        "  where deptno = e1.deptno)";
    String m = "select empid, deptno from emps\n";
    sql(m, q).ok();
}

/** Tests a complicated star-join query on a complicated materialized
   * star-join query. Some of the features:
   *
   * <ol>
   * <li>query joins in different order;
   * <li>query's join conditions are in where clause;
   * <li>query does not use all join tables (safe to omit them because they are
   *    many-to-mandatory-one joins);
   * <li>query is at higher granularity, therefore needs to roll up;
   * <li>query has a condition on one of the materialization's grouping columns.
   * </ol>
   */
TEST_F(MaterializedViewRewriteTest, testFilterGroupQueryOnStar)
{
    GTEST_SKIP();
    sql("select p.product_name, t.the_year,\n"
            "  sum(f.unit_sales) as sum_unit_sales, count(*) as c\n"
            "from foodmart.sales_fact_1997 as f\n"
            "join (\n"
            "    select time_id, the_year, the_month\n"
            "    from foodmart.time_by_day) as t\n"
            "  on f.time_id = t.time_id\n"
            "join foodmart.product as p\n"
            "  on f.product_id = p.product_id\n"
            "join foodmart.product_class as pc"
            "  on p.product_class_id = pc.product_class_id\n"
            "group by t.the_year,\n"
            " t.the_month,\n"
            " pc.product_department,\n"
            " pc.product_category,\n"
            " p.product_name",
        "select t.the_month, count(*) as x\n"
            "from (\n"
            "  select time_id, the_year, the_month\n"
            "  from foodmart.time_by_day) as t,\n"
            " foodmart.sales_fact_1997 as f\n"
            "where t.the_year = 1997\n"
            "and t.time_id = f.time_id\n"
            "group by t.the_year,\n"
            " t.the_month\n")
        .ok();
}

/** Simpler than {@link #testFilterGroupQueryOnStar()}, tests a query on a
   * materialization that is just a join. */
TEST_F(MaterializedViewRewriteTest, testQueryOnStar)
{
    GTEST_SKIP();
    String q = "select *\n"
        "from foodmart.sales_fact_1997 as f\n"
        "join foodmart.time_by_day as t on f.time_id = t.time_id\n"
        "join foodmart.product as p on f.product_id = p.product_id\n"
        "join foodmart.product_class as pc on p.product_class_id = pc.product_class_id\n";
    sql(q, q + "where t.month_of_year = 10")
        .ok();
}

/** A materialization that is a join of a union cannot at present be converted
   * to a star table and therefore cannot be recognized. This test checks that
   * nothing unpleasant happens. */
TEST_F(MaterializedViewRewriteTest, testJoinOnUnionMaterialization)
{
    GTEST_SKIP();
    String q = "select *\n"
        "from (select * from emps union all select * from emps)\n"
        "join depts using (deptno)";
    sql(q, q).noMat();
}

TEST_F(MaterializedViewRewriteTest, testDifferentColumnNames)
{
    GTEST_SKIP();
}

TEST_F(MaterializedViewRewriteTest, testDifferentType)
{
    GTEST_SKIP();
}

TEST_F(MaterializedViewRewriteTest, testPartialUnion)
{
    GTEST_SKIP();
}

TEST_F(MaterializedViewRewriteTest, testNonDisjointUnion)
{
    GTEST_SKIP();
}

TEST_F(MaterializedViewRewriteTest, testMaterializationReferencesTableInOtherSchema)
{
    GTEST_SKIP();
}

TEST_F(MaterializedViewRewriteTest, testOrderByQueryOnProjectView)
{
    GTEST_SKIP() << "only read from storage don't rewrite.";
    sql("select deptno, empid from emps", "select empid from emps order by deptno").ok();
}

TEST_F(MaterializedViewRewriteTest, testOrderByQueryOnOrderByView)
{
    GTEST_SKIP();
    sql("select deptno, empid from emps order by deptno", "select empid from emps order by deptno").ok();
}

TEST_F(MaterializedViewRewriteTest, testQueryDistinctColumnInTargetGroupByList0)
{
    String mv = ""
        "select name, commission, deptno\n"
        "from emps group by name, commission, deptno";
    String query = ""
        "select name, commission, count(distinct deptno) as cnt\n"
        "from emps group by name, commission";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testQueryDistinctColumnInTargetGroupByList1)
{
    String mv = ""
        "select name, deptno "
        "from emps group by name, deptno";
    String query = ""
        "select name, count(distinct deptno)\n"
        "from emps group by name";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testQueryDistinctColumnInTargetGroupByList2)
{
    String mv = ""
        "select name, deptno, empid\n"
        "from emps group by name, deptno, empid";
    String query = ""
        "select name, count(distinct deptno), count(distinct empid)\n"
        "from emps group by name";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testQueryDistinctColumnInTargetGroupByList3)
{
    String mv = ""
        "select name, deptno, empid, count(commission)\n"
        "from emps group by name, deptno, empid";
    String query = ""
        "select name, count(distinct deptno), count(distinct empid), count"
        "(commission)\n"
        "from emps group by name";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testQueryDistinctColumnInTargetGroupByList4)
{
    String mv = ""
        "select name, deptno, empid\n"
        "from emps group by name, deptno, empid";
    String query = ""
        "select name, count(distinct deptno)\n"
        "from emps group by name";
    sql(mv, query).ok();
}

TEST_F(MaterializedViewRewriteTest, testRexPredicate)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    String mv = ""
        "select name\n"
        "from emps\n"
        "where deptno > 100 and deptno > 50\n"
        "group by name";
    String query = ""
        "select name\n"
        "from emps\n"
        "where deptno > 100"
        "group by name";
    sql(mv, query)
        .checkingThatResultContains(""
                                    "EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteTest, testRexPredicate1)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    String query = ""
        "select name\n"
        "from emps\n"
        "where deptno > 100 and deptno > 50\n"
        "group by name";
    String mv = ""
        "select name\n"
        "from emps\n"
        "where deptno > 100"
        "group by name";
    sql(mv, query)
        .checkingThatResultContains(""
                                    "EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

/** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-4779">[CALCITE-4779]
   * GroupByList contains constant literal, materialized view recognition failed</a>. */
TEST_F(MaterializedViewRewriteTest, testGroupByListContainsConstantLiteral)
{
    // Aggregate operator grouping set contains a literal and count(distinct col) function.
    String mv1 = ""
        "select deptno, empid\n"
        "from emps\n"
        "group by deptno, empid";
    String query1 = ""
        "select 'a', deptno, count(distinct empid)\n"
        "from emps\n"
        "group by 'a', deptno";
    sql(mv1, query1).ok();

    // Aggregate operator grouping set contains a literal and sum(col) function.
    String mv2 = ""
        "select deptno, empid, sum(empid)\n"
        "from emps\n"
        "group by deptno, empid";
    String query2 = ""
        "select 'a', deptno, sum(empid)\n"
        "from emps\n"
        "group by 'a', deptno";
    sql(mv2, query2).ok();
}
