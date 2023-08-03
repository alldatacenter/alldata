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

#include <utility>

using namespace DB;
using namespace std::string_literals;

class MaterializedViewRewriteComplexTest : public ::testing::Test
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

std::shared_ptr<BaseMaterializedViewTest> MaterializedViewRewriteComplexTest::tester;


TEST_F(MaterializedViewRewriteComplexTest, testSwapJoin)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select count(*) as c from foodmart.sales_fact_1997 as s"
            " join foodmart.time_by_day as t on s.time_id = t.time_id",
        "select count(*) as c from foodmart.time_by_day as t"
            " join foodmart.sales_fact_1997 as s on t.time_id = s.time_id")
        .ok();
}

/** Aggregation materialization with a project. */
TEST_F(MaterializedViewRewriteComplexTest, testAggregateProject)
{
    // Note that materialization does not start with the GROUP BY columns.
    // Not a smart way to design a materialization, but people may do it.
    sql("select deptno, count(*) as c, empid + 2, sum(empid) as s "
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

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs1)
{
    sql("select empid, deptno from emps group by empid, deptno",
        "select empid, deptno from emps group by empid, deptno").ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs2)
{
    sql("select empid, deptno from emps group by empid, deptno",
        "select deptno from emps group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: deptno:=`expr#deptno`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {expr#deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {expr#deptno}\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: expr#deptno:=deptno\n"
                                    "               └─ Filter\n"
                                    "                  │     Condition: 1\n"
                                    "                  └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                           Outputs: [deptno]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs3)
{
    sql("select deptno from emps group by deptno",
        "select empid, deptno from emps group by empid, deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs4)
{
    sql("select empid, deptno\n"
            "from emps where deptno = 10 group by empid, deptno",
        "select deptno from emps where deptno = 10 group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [deptno]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: 1\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [deptno]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs5)
{
    sql("select empid, deptno\n"
            "from emps where deptno = 5 group by empid, deptno",
        "select deptno from emps where deptno = 10 group by deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs6)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select empid, deptno\n"
            "from emps where deptno > 5 group by empid, deptno",
        "select deptno from emps where deptno > 10 group by deptno")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{1}])\n"
                                    "  EnumerableCalc(expr#0..1=[{inputs}], expr#2=[10], expr#3=[<($t2, $t1)], proj#0..1=[{exprs}], $condition=[$t3])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs7)
{
    sql("select empid, deptno\n"
            "from emps where deptno > 5 group by empid, deptno",
        "select deptno from emps where deptno < 10 group by deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs8)
{
    sql("select empid from emps group by empid, deptno",
        "select deptno from emps group by deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationNoAggregateFuncs9)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select empid, deptno from emps\n"
            "where salary > 1000 group by name, empid, deptno",
        "select empid from emps\n"
            "where salary > 2000 group by name, empid")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs1)
{
    sql("select empid, deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by empid, deptno",
        "select deptno from emps group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: deptno:=`expr#deptno`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {expr#deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {expr#deptno}\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: expr#deptno:=deptno\n"
                                    "               └─ Filter\n"
                                    "                  │     Condition: 1\n"
                                    "                  └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                           Outputs: [deptno]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs2)
{
    sql("select empid, deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by empid, deptno",
        "select deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: c:=`expr#sum()(c)`, deptno:=`expr#deptno`, s:=`expr#sum()(s)`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {expr#deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {expr#deptno}\n"
                                    "            │     Aggregates: expr#sum()(c):=AggNull(sum)(expr#c), expr#sum()(s):=AggNull(sum)(expr#s)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: expr#c:=c, expr#deptno:=deptno, expr#s:=s\n"
                                    "               └─ Filter\n"
                                    "                  │     Condition: 1\n"
                                    "                  └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                           Outputs: [c, deptno, s]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs3)
{
    sql("select empid, deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by empid, deptno",
        "select deptno, empid, sum(empid) as s, count(*) as c\n"
            "from emps group by empid, deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [c, deptno, empid, s]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: 1\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [c, deptno, empid, s]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs4)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select empid, deptno, count(*) as c, sum(empid) as s\n"
            "from emps where deptno >= 10 group by empid, deptno",
        "select deptno, sum(empid) as s\n"
            "from emps where deptno > 10 group by deptno")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{1}], S=[$SUM0($3)])\n"
                                    "  EnumerableCalc(expr#0..3=[{inputs}], expr#4=[10], expr#5=[<($t4, $t1)], "
                                    "proj#0..3=[{exprs}], $condition=[$t5])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs5)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select empid, deptno, count(*) + 1 as c, sum(empid) as s\n"
            "from emps where deptno >= 10 group by empid, deptno",
        "select deptno, sum(empid) + 1 as s\n"
            "from emps where deptno > 10 group by deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[1], expr#3=[+($t1, $t2)],"
                                    " deptno=[$t0], S=[$t3])\n"
                                    "  EnumerableAggregate(group=[{1}], agg#0=[$SUM0($3)])\n"
                                    "    EnumerableCalc(expr#0..3=[{inputs}], expr#4=[10], expr#5=[<($t4, $t1)], "
                                    "proj#0..3=[{exprs}], $condition=[$t5])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs6)
{
    sql("select empid, deptno, count(*) + 1 as c, sum(empid) + 2 as s\n"
            "from emps where deptno >= 10 group by empid, deptno",
        "select deptno, sum(empid) + 1 as s\n"
            "from emps where deptno > 10 group by deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs7)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    sql("select empid, deptno, count(*) + 1 as c, sum(empid) as s\n"
            "from emps where deptno >= 10 group by empid, deptno",
        "select deptno + 1, sum(empid) + 1 as s\n"
            "from emps where deptno > 10 group by deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[1], expr#3=[+($t0, $t2)], "
                                    "expr#4=[+($t1, $t2)], EXPR$0=[$t3], S=[$t4])\n"
                                    "  EnumerableAggregate(group=[{1}], agg#0=[$SUM0($3)])\n"
                                    "    EnumerableCalc(expr#0..3=[{inputs}], expr#4=[10], expr#5=[<($t4, $t1)], "
                                    "proj#0..3=[{exprs}], $condition=[$t5])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs8)
{
    GTEST_SKIP();
    // TODO: It should work, but top project in the query is not matched by the planner.
    // It needs further checking.
    sql("select empid, deptno + 1, count(*) + 1 as c, sum(empid) as s\n"
            "from emps where deptno >= 10 group by empid, deptno",
        "select deptno + 1, sum(empid) + 1 as s\n"
            "from emps where deptno > 10 group by deptno")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs9)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select empid, floor(cast('1997-01-20 12:34:56' as timestamp) to month), "
            "count(*) + 1 as c, sum(empid) as s\n"
            "from emps\n"
            "group by empid, floor(cast('1997-01-20 12:34:56' as timestamp) to month)",
        "select floor(cast('1997-01-20 12:34:56' as timestamp) to year), sum(empid) as s\n"
            "from emps group by floor(cast('1997-01-20 12:34:56' as timestamp) to year)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs10)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select empid, floor(cast('1997-01-20 12:34:56' as timestamp) to month), "
            "count(*) + 1 as c, sum(empid) as s\n"
            "from emps\n"
            "group by empid, floor(cast('1997-01-20 12:34:56' as timestamp) to month)",
        "select floor(cast('1997-01-20 12:34:56' as timestamp) to year), sum(empid) + 1 as s\n"
            "from emps group by floor(cast('1997-01-20 12:34:56' as timestamp) to year)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs11)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select empid, floor(cast('1997-01-20 12:34:56' as timestamp) to second), "
            "count(*) + 1 as c, sum(empid) as s\n"
            "from emps\n"
            "group by empid, floor(cast('1997-01-20 12:34:56' as timestamp) to second)",
        "select floor(cast('1997-01-20 12:34:56' as timestamp) to minute), sum(empid) as s\n"
            "from emps group by floor(cast('1997-01-20 12:34:56' as timestamp) to minute)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs12)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select empid, floor(cast('1997-01-20 12:34:56' as timestamp) to second), "
            "count(*) + 1 as c, sum(empid) as s\n"
            "from emps\n"
            "group by empid, floor(cast('1997-01-20 12:34:56' as timestamp) to second)",
        "select floor(cast('1997-01-20 12:34:56' as timestamp) to month), sum(empid) as s\n"
            "from emps group by floor(cast('1997-01-20 12:34:56' as timestamp) to month)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs13)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select empid, cast('1997-01-20 12:34:56' as timestamp), "
            "count(*) + 1 as c, sum(empid) as s\n"
            "from emps\n"
            "group by empid, cast('1997-01-20 12:34:56' as timestamp)",
        "select floor(cast('1997-01-20 12:34:56' as timestamp) to year), sum(empid) as s\n"
            "from emps group by floor(cast('1997-01-20 12:34:56' as timestamp) to year)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs14)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select empid, floor(cast('1997-01-20 12:34:56' as timestamp) to month), "
            "count(*) + 1 as c, sum(empid) as s\n"
            "from emps\n"
            "group by empid, floor(cast('1997-01-20 12:34:56' as timestamp) to month)",
        "select floor(cast('1997-01-20 12:34:56' as timestamp) to hour), sum(empid) as s\n"
            "from emps group by floor(cast('1997-01-20 12:34:56' as timestamp) to hour)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs15)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select eventid, floor(cast(ts as timestamp) to second), "
            "count(*) + 1 as c, sum(eventid) as s\n"
            "from events group by eventid, floor(cast(ts as timestamp) to second)",
        "select floor(cast(ts as timestamp) to minute), sum(eventid) as s\n"
            "from events group by floor(cast(ts as timestamp) to minute)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs16)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select eventid, cast(ts as timestamp), count(*) + 1 as c, sum(eventid) as s\n"
            "from events group by eventid, cast(ts as timestamp)",
        "select floor(cast(ts as timestamp) to year), sum(eventid) as s\n"
            "from events group by floor(cast(ts as timestamp) to year)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs17)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    sql("select eventid, floor(cast(ts as timestamp) to month), "
            "count(*) + 1 as c, sum(eventid) as s\n"
            "from events group by eventid, floor(cast(ts as timestamp) to month)",
        "select floor(cast(ts as timestamp) to hour), sum(eventid) as s\n"
            "from events group by floor(cast(ts as timestamp) to hour)")
        .checkingThatResultContains("EnumerableTableScan(table=[[hr, events]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs18)
{
    sql("select empid, deptno, count(*) + 1 as c, sum(empid) as s\n"
            "from emps group by empid, deptno",
        "select empid*deptno, sum(empid) as s\n"
            "from emps group by empid*deptno")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs19)
{
    sql("select empid, deptno, count(*) as c, sum(empid) as s\n"
            "from emps group by empid, deptno",
        "select empid + 10, count(*) + 1 as c\n"
            "from emps group by empid + 10")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationAggregateFuncs20)
{
    GTEST_SKIP() << "ast parser error without optimzier.";
    sql("select 11 as empno, 22 as sal, count(*) from emps group by 11, 22",
        "select * from\n"
            "(select 11 as empno, 22 as sal, count(*)\n"
            "from emps group by 11, 22) tmp\n"
            "where sal = 33")
        .checkingThatResultContains("EnumerableValues(tuples=[[]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs1)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, depts.deptno from emps\n"
            "join depts using (deptno) where depts.deptno > 10\n"
            "group by empid, depts.deptno",
        "select empid from emps\n"
            "join depts using (deptno) where depts.deptno > 20\n"
            "group by empid, depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[20], expr#3=[<($t2, $t1)], "
                                    "empid=[$t0], $condition=[$t3])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, empid from depts\n"
            "join emps using (deptno) where depts.deptno > 10\n"
            "group by empid, depts.deptno",
        "select empid from emps\n"
            "join depts using (deptno) where depts.deptno > 20\n"
            "group by empid, depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[20], expr#3=[<($t2, $t0)], "
                                    "empid=[$t1], $condition=[$t3])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs3)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    // It does not match, Project on top of query
    sql("select empid from emps\n"
            "join depts using (deptno) where depts.deptno > 10\n"
            "group by empid, depts.deptno",
        "select empid from emps\n"
            "join depts using (deptno) where depts.deptno > 20\n"
            "group by empid, depts.deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs4)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, depts.deptno from emps\n"
            "join depts using (deptno) where emps.deptno > 10\n"
            "group by empid, depts.deptno",
        "select empid from emps\n"
            "join depts using (deptno) where depts.deptno > 20\n"
            "group by empid, depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[20], expr#3=[<($t2, $t1)], "
                                    "empid=[$t0], $condition=[$t3])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs5)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, emps.empid from depts\n"
            "join emps using (deptno) where emps.empid > 10\n"
            "group by depts.deptno, emps.empid",
        "select depts.deptno from depts\n"
            "join emps using (deptno) where emps.empid > 15\n"
            "group by depts.deptno, emps.empid")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[15], expr#3=[<($t2, $t1)], "
                                    "deptno=[$t0], $condition=[$t3])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs6)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, emps.empid from depts\n"
            "join emps using (deptno) where emps.empid > 10\n"
            "group by depts.deptno, emps.empid",
        "select depts.deptno from depts\n"
            "join emps using (deptno) where emps.empid > 15\n"
            "group by depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{0}])\n"
                                    "  EnumerableCalc(expr#0..1=[{inputs}], expr#2=[15], expr#3=[<($t2, $t1)], "
                                    "proj#0..1=[{exprs}], $condition=[$t3])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs7)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 11\n"
            "group by depts.deptno, dependents.empid",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10\n"
            "group by dependents.empid")
        .checkingThatResultContains("EnumerableAggregate(group=[{0}])",
                                    "EnumerableUnion(all=[true])",
                                    "EnumerableAggregate(group=[{2}])",
                                    "EnumerableTableScan(table=[[hr, MV0]])",
                                    "expr#5=[Sarg[(10..11]]], expr#6=[SEARCH($t0, $t5)]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs8)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 20\n"
            "group by depts.deptno, dependents.empid",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10 and depts.deptno < 20\n"
            "group by dependents.empid")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs9)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 11 and depts.deptno < 19\n"
            "group by depts.deptno, dependents.empid",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10 and depts.deptno < 20\n"
            "group by dependents.empid")
        .checkingThatResultContains("EnumerableAggregate(group=[{0}])",
                                    "EnumerableUnion(all=[true])",
                                    "EnumerableAggregate(group=[{2}])",
                                    "EnumerableTableScan(table=[[hr, MV0]])",
                                    "expr#5=[Sarg[(10..11], [19..20)]], expr#6=[SEARCH($t0, $t5)]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationNoAggregateFuncs10)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.name, dependents.name as name2, "
            "emps.deptno, depts.deptno as deptno2, "
            "dependents.empid\n"
            "from depts, dependents, emps\n"
            "where depts.deptno > 10\n"
            "group by depts.name, dependents.name, "
            "emps.deptno, depts.deptno, "
            "dependents.empid",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10\n"
            "group by dependents.empid")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{4}])\n"
                                    "  EnumerableCalc(expr#0..4=[{inputs}], expr#5=[=($t2, $t3)], "
                                    "expr#6=[CAST($t1):VARCHAR], "
                                    "expr#7=[CAST($t0):VARCHAR], "
                                    "expr#8=[=($t6, $t7)], expr#9=[AND($t5, $t8)], proj#0..4=[{exprs}], $condition=[$t9])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs1)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    // This test relies on FK-UK relationship
    sql("select empid, depts.deptno, count(*) as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "group by empid, depts.deptno",
        "select deptno from emps group by deptno")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{1}])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, emps.deptno, count(*) as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "group by empid, emps.deptno",
        "select depts.deptno, count(*) as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "group by depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{1}], C=[$SUM0($2)], S=[$SUM0($3)])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs3)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    // This test relies on FK-UK relationship
    sql("select empid, depts.deptno, count(*) as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "group by empid, depts.deptno",
        "select deptno, empid, sum(empid) as s, count(*) as c\n"
            "from emps group by empid, deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..3=[{inputs}], deptno=[$t1], empid=[$t0], S=[$t3], C=[$t2])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs4)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, emps.deptno, count(*) as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "where emps.deptno >= 10 group by empid, emps.deptno",
        "select depts.deptno, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "where emps.deptno > 10 group by depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{1}], S=[$SUM0($3)])\n"
                                    "  EnumerableCalc(expr#0..3=[{inputs}], expr#4=[10], expr#5=[<($t4, $t1)], "
                                    "proj#0..3=[{exprs}], $condition=[$t5])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs5)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, depts.deptno, count(*) + 1 as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "where depts.deptno >= 10 group by empid, depts.deptno",
        "select depts.deptno, sum(empid) + 1 as s\n"
            "from emps join depts using (deptno)\n"
            "where depts.deptno > 10 group by depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], expr#2=[1], expr#3=[+($t1, $t2)], "
                                    "deptno=[$t0], S=[$t3])\n"
                                    "  EnumerableAggregate(group=[{1}], agg#0=[$SUM0($3)])\n"
                                    "    EnumerableCalc(expr#0..3=[{inputs}], expr#4=[10], expr#5=[<($t4, $t1)], "
                                    "proj#0..3=[{exprs}], $condition=[$t5])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs6)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    // This rewriting would be possible if planner generates a pre-aggregation,
    // since the materialized view would match the sub-query.
    // Initial investigation after enabling AggregateJoinTransposeRule.EXTENDED
    // shows that the rewriting with pre-aggregations is generated and the
    // materialized view rewriting happens.
    // However, we end up discarding the plan with the materialized view and still
    // using the plan with the pre-aggregations.
    // TODO: Explore and extend to choose best rewriting.
    String m = "select depts.name, sum(salary) as s\n"
        "from emps\n"
        "join depts on (emps.deptno = depts.deptno)\n"
        "group by depts.name";
    String q = "select dependents.empid, sum(salary) as s\n"
        "from emps\n"
        "join depts on (emps.deptno = depts.deptno)\n"
        "join dependents on (depts.name = dependents.name)\n"
        "group by dependents.empid";
    sql(m, q).ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs7)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select dependents.empid, emps.deptno, sum(salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno",
        "select dependents.empid, sum(salary) as s\n"
            "from emps\n"
            "join depts on (emps.deptno = depts.deptno)\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{0}], S=[$SUM0($2)])\n"
                                    "  EnumerableHashJoin(condition=[=($1, $3)], joinType=[inner])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "    EnumerableTableScan(table=[[hr, depts]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs8)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select dependents.empid, emps.deptno, sum(salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno",
        "select depts.name, sum(salary) as s\n"
            "from emps\n"
            "join depts on (emps.deptno = depts.deptno)\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by depts.name")
        .checkingThatResultContains(""
                                    "EnumerableAggregate(group=[{4}], S=[$SUM0($2)])\n"
                                    "  EnumerableHashJoin(condition=[=($1, $3)], joinType=[inner])\n"
                                    "    EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "    EnumerableTableScan(table=[[hr, depts]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs9)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select dependents.empid, emps.deptno, count(distinct salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno",
        "select emps.deptno, count(distinct salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..2=[{inputs}], deptno=[$t1], S=[$t2])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs10)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select dependents.empid, emps.deptno, count(distinct salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno",
        "select emps.deptno, count(distinct salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by emps.deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs11)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, dependents.empid, count(emps.salary) as s\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 11 and depts.deptno < 19\n"
            "group by depts.deptno, dependents.empid",
        "select dependents.empid, count(emps.salary) + 1\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10 and depts.deptno < 20\n"
            "group by dependents.empid")
        .checkingThatResultContains("EnumerableCalc(expr#0..1=[{inputs}], "
                                        "expr#2=[1], expr#3=[+($t1, $t2)], empid=[$t0], EXPR$1=[$t3])\n"
                                        "  EnumerableAggregate(group=[{0}], agg#0=[$SUM0($1)])",
                                    "EnumerableUnion(all=[true])",
                                    "EnumerableAggregate(group=[{2}], agg#0=[COUNT()])",
                                    "EnumerableAggregate(group=[{1}], agg#0=[$SUM0($2)])",
                                    "EnumerableTableScan(table=[[hr, MV0]])",
                                    "expr#5=[Sarg[(10..11], [19..20)]], expr#6=[SEARCH($t0, $t5)]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs12)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, dependents.empid, "
            "count(distinct emps.salary) as s\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 11 and depts.deptno < 19\n"
            "group by depts.deptno, dependents.empid",
        "select dependents.empid, count(distinct emps.salary) + 1\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10 and depts.deptno < 20\n"
            "group by dependents.empid")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs13)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select dependents.empid, emps.deptno, count(distinct salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno",
        "select emps.deptno, count(salary) as s\n"
            "from emps\n"
            "join dependents on (emps.empid = dependents.empid)\n"
            "group by dependents.empid, emps.deptno")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs14)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, emps.name, emps.deptno, depts.name, "
            "count(*) as c, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "where (depts.name is not null and emps.name = 'a') or "
            "(depts.name is not null and emps.name = 'b')\n"
            "group by empid, emps.name, depts.name, emps.deptno",
        "select depts.deptno, sum(empid) as s\n"
            "from emps join depts using (deptno)\n"
            "where depts.name is not null and emps.name = 'a'\n"
            "group by depts.deptno")
        .ok();
}

/** Test case for
   * <a href="https://issues.apache.org/jira/browse/CALCITE-4276">[CALCITE-4276]
   * If query contains join and rollup function (FLOOR), rewrite to materialized
   * view contains bad field offset</a>. */
TEST_F(MaterializedViewRewriteComplexTest, testJoinAggregateMaterializationAggregateFuncs15)
{
    GTEST_SKIP() << "time function rollup rewrite is not implemented.";
    String m = ""
        "SELECT deptno,\n"
        "  COUNT(*) AS dept_size,\n"
        "  SUM(salary) AS dept_budget\n"
        "FROM emps\n"
        "GROUP BY deptno";
    String q = ""
        "SELECT FLOOR(CREATED_AT TO YEAR) AS by_year,\n"
        "  COUNT(*) AS num_emps\n"
        "FROM (SELECT deptno\n"
        "    FROM emps) AS t\n"
        "JOIN (SELECT deptno,\n"
        "        inceptionDate as CREATED_AT\n"
        "    FROM depts2) using (deptno)\n"
        "GROUP BY FLOOR(CREATED_AT TO YEAR)";
    String plan = ""
        "EnumerableAggregate(group=[{8}], num_emps=[$SUM0($1)])\n"
        "  EnumerableCalc(expr#0..7=[{inputs}], expr#8=[FLAG(YEAR)], "
        "expr#9=[FLOOR($t3, $t8)], proj#0..7=[{exprs}], $f8=[$t9])\n"
        "    EnumerableHashJoin(condition=[=($0, $4)], joinType=[inner])\n"
        "      EnumerableTableScan(table=[[hr, MV0]])\n"
        "      EnumerableTableScan(table=[[hr, depts2]])\n";
    sql(m, q)
        .checkingThatResultContains(plan)
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization1)
{
    GTEST_SKIP() << "predicate tuple domain rewrite is not implemented.";
    String q = "select *\n"
        "from (select * from emps where empid < 300)\n"
        "join depts using (deptno)";
    sql("select * from emps where empid < 500", q).ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String q = "select *\n"
        "from emps\n"
        "join depts using (deptno)";
    String m = "select deptno, empid, name,\n"
        "salary, commission from emps";
    sql(m, q).ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization3)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    String q = "select empid deptno from emps\n"
        "join depts using (deptno) where empid = 1";
    String m = "select empid deptno from emps\n"
        "join depts using (deptno)";
    sql(m, q).ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization4)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid deptno from emps\n"
            "join depts using (deptno)",
        "select empid deptno from emps\n"
            "join depts using (deptno) where empid = 1")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0=[{inputs}], expr#1=[CAST($t0):INTEGER NOT NULL], expr#2=[1], "
                                    "expr#3=[=($t1, $t2)], deptno=[$t0], $condition=[$t3])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization5)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select cast(empid as BIGINT) from emps\n"
            "join depts using (deptno)",
        "select empid deptno from emps\n"
            "join depts using (deptno) where empid > 1")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0=[{inputs}], expr#1=[CAST($t0):JavaType(int) NOT NULL], "
                                    "expr#2=[1], expr#3=[<($t2, $t1)], EXPR$0=[$t1], $condition=[$t3])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization6)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select cast(empid as BIGINT) from emps\n"
            "join depts using (deptno)",
        "select empid deptno from emps\n"
            "join depts using (deptno) where empid = 1")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0=[{inputs}], expr#1=[CAST($t0):JavaType(int) NOT NULL], "
                                    "expr#2=[1], expr#3=[CAST($t1):INTEGER NOT NULL], expr#4=[=($t2, $t3)], "
                                    "EXPR$0=[$t1], $condition=[$t4])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization7)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.name\n"
            "from emps\n"
            "join depts on (emps.deptno = depts.deptno)",
        "select dependents.empid\n"
            "from emps\n"
            "join depts on (emps.deptno = depts.deptno)\n"
            "join dependents on (depts.name = dependents.name)")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..2=[{inputs}], empid=[$t1])\n"
                                    "  EnumerableHashJoin(condition=[=($0, $2)], joinType=[inner])\n"
                                    "    EnumerableCalc(expr#0=[{inputs}], expr#1=[CAST($t0):VARCHAR], name=[$t1])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "    EnumerableCalc(expr#0..1=[{inputs}], expr#2=[CAST($t1):VARCHAR], empid=[$t0], name0=[$t2])\n"
                                    "      EnumerableTableScan(table=[[hr, dependents]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization8)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.name\n"
            "from emps\n"
            "join depts on (emps.deptno = depts.deptno)",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..4=[{inputs}], empid=[$t2])\n"
                                    "  EnumerableHashJoin(condition=[=($1, $4)], joinType=[inner])\n"
                                    "    EnumerableCalc(expr#0=[{inputs}], expr#1=[CAST($t0):VARCHAR], proj#0..1=[{exprs}])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "    EnumerableCalc(expr#0..1=[{inputs}], expr#2=[CAST($t1):VARCHAR], proj#0..2=[{exprs}])\n"
                                    "      EnumerableTableScan(table=[[hr, dependents]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization9)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.name\n"
            "from emps\n"
            "join depts on (emps.deptno = depts.deptno)",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join locations on (locations.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization10)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select depts.deptno, dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 30",
        "select dependents.empid\n"
            "from depts\n"
            "join dependents on (depts.name = dependents.name)\n"
            "join emps on (emps.deptno = depts.deptno)\n"
            "where depts.deptno > 10")
        .checkingThatResultContains("EnumerableUnion(all=[true])",
                                    "EnumerableTableScan(table=[[hr, MV0]])",
                                    "expr#5=[Sarg[(10..30]]], expr#6=[SEARCH($t0, $t5)]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization11)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid from emps\n"
            "join depts using (deptno)",
        "select empid from emps\n"
            "where deptno in (select deptno from depts)")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterialization12)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid, emps.name, emps.deptno, depts.name\n"
            "from emps join depts using (deptno)\n"
            "where (depts.name is not null and emps.name = 'a') or "
            "(depts.name is not null and emps.name = 'b') or "
            "(depts.name is not null and emps.name = 'c')",
        "select depts.deptno, depts.name\n"
            "from emps join depts using (deptno)\n"
            "where (depts.name is not null and emps.name = 'a') or "
            "(depts.name is not null and emps.name = 'b')")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK1)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select a.empid deptno from\n"
            "(select * from emps where empid = 1) a\n"
            "join depts using (deptno)\n"
            "join dependents using (empid)",
        "select a.empid from \n"
            "(select * from emps where empid = 1) a\n"
            "join dependents using (empid)")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK2)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select a.empid, a.deptno from\n"
            "(select * from emps where empid = 1) a\n"
            "join depts using (deptno)\n"
            "join dependents using (empid)",
        "select a.empid from \n"
            "(select * from emps where empid = 1) a\n"
            "join dependents using (empid)\n")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], empid=[$t0])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK3)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select a.empid, a.deptno from\n"
            "(select * from emps where empid = 1) a\n"
            "join depts using (deptno)\n"
            "join dependents using (empid)",
        "select a.name from \n"
            "(select * from emps where empid = 1) a\n"
            "join dependents using (empid)\n")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK4)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select empid deptno from\n"
            "(select * from emps where empid = 1)\n"
            "join depts using (deptno)",
        "select empid from emps where empid = 1\n")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK5)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select emps.empid, emps.deptno from emps\n"
            "join depts using (deptno)\n"
            "join dependents using (empid)"
            "where emps.empid = 1",
        "select emps.empid from emps\n"
            "join dependents using (empid)\n"
            "where emps.empid = 1")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], empid=[$t0])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK6)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select emps.empid, emps.deptno from emps\n"
            "join depts a on (emps.deptno=a.deptno)\n"
            "join depts b on (emps.deptno=b.deptno)\n"
            "join dependents using (empid)"
            "where emps.empid = 1",
        "select emps.empid from emps\n"
            "join dependents using (empid)\n"
            "where emps.empid = 1")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], empid=[$t0])\n"
                                    "  EnumerableTableScan(table=[[hr, MV0]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK7)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select emps.empid, emps.deptno from emps\n"
            "join depts a on (emps.name=a.name)\n"
            "join depts b on (emps.name=b.name)\n"
            "join dependents using (empid)"
            "where emps.empid = 1",
        "select emps.empid from emps\n"
            "join dependents using (empid)\n"
            "where emps.empid = 1")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK8)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select emps.empid, emps.deptno from emps\n"
            "join depts a on (emps.deptno=a.deptno)\n"
            "join depts b on (emps.name=b.name)\n"
            "join dependents using (empid)"
            "where emps.empid = 1",
        "select emps.empid from emps\n"
            "join dependents using (empid)\n"
            "where emps.empid = 1")
        .noMat();
}

TEST_F(MaterializedViewRewriteComplexTest, testJoinMaterializationUKFK9)
{
    GTEST_SKIP() << "join rewrite is not implemented.";
    sql("select * from emps\n"
            "join dependents using (empid)",
        "select emps.empid, dependents.empid, emps.deptno\n"
            "from emps\n"
            "join dependents using (empid)"
            "join depts a on (emps.deptno=a.deptno)\n"
            "where emps.name = 'Bill'")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testQueryProjectWithBetween)
{
    sql("select *"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1",
        "select s.time_id between 1 and 3"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: and(greaterOrEquals(s.time_id, 1), lessOrEquals(s.time_id, 3)):=(time_id >= 1) AND (time_id <= 3)\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: 1\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [time_id]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, DISABLED_testJoinQueryProjectWithBetween)
{
    sql("select *"
            " from foodmart.sales_fact_1997 as s"
            " join foodmart.time_by_day as t on s.time_id = t.time_id"
            " where s.store_id = 1",
        "select s.time_id between 1 and 3"
            " from foodmart.sales_fact_1997 as s"
            " join foodmart.time_by_day as t on s.time_id = t.time_id"
            " where s.store_id = 1")
        .checkingThatResultContains("Gather Exchange\n"
                                    "└─ Projection\n"
                                    "   │     Expressions: and(greaterOrEquals(s.time_id, 1), lessOrEquals(s.time_id, 3)):=(time_id >= 1) AND (time_id <= 3)\n"
                                    "   └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "            Outputs: [time_id]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testViewProjectWithBetween)
{
    sql("select s.time_id, s.time_id between 1 and 3"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1",
        "select s.time_id"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [time_id]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: 1\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [time_id]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testQueryAndViewProjectWithBetween)
{
    sql("select s.time_id, s.time_id between 1 and 3"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1",
        "select s.time_id between 1 and 3"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [and(greaterOrEquals(s.time_id, 1), lessOrEquals(s.time_id, 3))]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: 1\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: and(greaterOrEquals(s.time_id, 1), lessOrEquals(s.time_id, 3)):=and(greaterOrEquals(time_id, 1), lessOrEquals(time_id, 3))")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testViewProjectWithMultifieldExpressions)
{
    sql("select s.time_id, s.time_id >= 1 and s.time_id < 3,"
            " s.time_id >= 1 or s.time_id < 3, "
            " s.time_id + s.time_id, "
            " s.time_id * s.time_id"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1",
        "select s.time_id"
            " from foodmart.sales_fact_1997 as s"
            " where s.store_id = 1")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [time_id]\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ Filter\n"
                                    "      │     Condition: 1\n"
                                    "      └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "               Outputs: [time_id]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateOnJoinKeys)
{
    GTEST_SKIP() << "aggregate on join keys rollup rewrite is not implemented.";
    sql("select deptno, empid, salary "
            "from emps\n"
            "group by deptno, empid, salary",
        "select empid, depts.deptno "
            "from emps\n"
            "join depts on depts.deptno = empid group by empid, depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0=[{inputs}], empid=[$t0], empid0=[$t0])\n"
                                    "  EnumerableAggregate(group=[{1}])\n"
                                    "    EnumerableHashJoin(condition=[=($1, $3)], joinType=[inner])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "      EnumerableTableScan(table=[[hr, depts]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateOnJoinKeys2)
{
    GTEST_SKIP() << "aggregate on join keys rollup rewrite is not implemented.";
    sql("select deptno, empid, salary, sum(1) "
            "from emps\n"
            "group by deptno, empid, salary",
        "select sum(1) "
            "from emps\n"
            "join depts on depts.deptno = empid group by empid, depts.deptno")
        .checkingThatResultContains(""
                                    "EnumerableCalc(expr#0..1=[{inputs}], EXPR$0=[$t1])\n"
                                    "  EnumerableAggregate(group=[{1}], EXPR$0=[$SUM0($3)])\n"
                                    "    EnumerableHashJoin(condition=[=($1, $4)], joinType=[inner])\n"
                                    "      EnumerableTableScan(table=[[hr, MV0]])\n"
                                    "      EnumerableTableScan(table=[[hr, depts]])")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationOnCountDistinctQuery1)
{
    // The column empid is already unique, thus DISTINCT is not
    // in the COUNT of the resulting rewriting
    sql("select deptno, empid, salary\n"
            "from emps\n"
            "group by deptno, empid, salary",
        "select deptno, count(distinct empid) as c from (\n"
            "select deptno, empid\n"
            "from emps\n"
            "group by deptno, empid)\n"
            "group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [deptno], c:=`expr#uniqExact(empid)`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {deptno}\n"
                                    "            │     Aggregates: expr#uniqExact(empid):=AggNull(uniqExact)(empid)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: deptno:=`expr#deptno`, empid:=`expr#empid`\n"
                                    "               └─ MergingAggregated\n"
                                    "                  └─ Repartition Exchange\n"
                                    "                     │     Partition by: {expr#deptno, expr#empid}\n"
                                    "                     └─ Aggregating\n"
                                    "                        │     Group by: {expr#deptno, expr#empid}\n"
                                    "                        └─ Projection\n"
                                    "                           │     Expressions: expr#deptno:=deptno, expr#empid:=empid\n"
                                    "                           └─ Filter\n"
                                    "                              │     Condition: 1\n"
                                    "                              └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                                       Outputs: [deptno, empid]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationOnCountDistinctQuery2)
{
    // The column empid is already unique, thus DISTINCT is not
    // in the COUNT of the resulting rewriting
    sql("select deptno, salary, empid\n"
            "from emps\n"
            "group by deptno, salary, empid",
        "select deptno, count(distinct empid) as c from (\n"
            "select deptno, empid\n"
            "from emps\n"
            "group by deptno, empid)\n"
            "group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [deptno], c:=`expr#uniqExact(empid)`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {deptno}\n"
                                    "            │     Aggregates: expr#uniqExact(empid):=AggNull(uniqExact)(empid)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: deptno:=`expr#deptno`, empid:=`expr#empid`\n"
                                    "               └─ MergingAggregated\n"
                                    "                  └─ Repartition Exchange\n"
                                    "                     │     Partition by: {expr#deptno, expr#empid}\n"
                                    "                     └─ Aggregating\n"
                                    "                        │     Group by: {expr#deptno, expr#empid}\n"
                                    "                        └─ Projection\n"
                                    "                           │     Expressions: expr#deptno:=deptno, expr#empid:=empid\n"
                                    "                           └─ Filter\n"
                                    "                              │     Condition: 1\n"
                                    "                              └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                                       Outputs: [deptno, empid]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationOnCountDistinctQuery3)
{
    // The column salary is not unique, thus we end up with
    // a different rewriting
    sql("select deptno, empid, salary\n"
            "from emps\n"
            "group by deptno, empid, salary",
        "select deptno, count(distinct salary) from (\n"
            "select deptno, salary\n"
            "from emps\n"
            "group by deptno, salary)\n"
            "group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [deptno], uniqExact(salary):=`expr#uniqExact(salary)`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {deptno}\n"
                                    "            │     Aggregates: expr#uniqExact(salary):=AggNull(uniqExact)(salary)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: deptno:=`expr#deptno`, salary:=`expr#salary`\n"
                                    "               └─ MergingAggregated\n"
                                    "                  └─ Repartition Exchange\n"
                                    "                     │     Partition by: {expr#deptno, expr#salary}\n"
                                    "                     └─ Aggregating\n"
                                    "                        │     Group by: {expr#deptno, expr#salary}\n"
                                    "                        └─ Projection\n"
                                    "                           │     Expressions: expr#deptno:=deptno, expr#salary:=salary\n"
                                    "                           └─ Filter\n"
                                    "                              │     Condition: 1\n"
                                    "                              └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                                       Outputs: [deptno, salary]")
        .ok();
}

TEST_F(MaterializedViewRewriteComplexTest, testAggregateMaterializationOnCountDistinctQuery4)
{
    // Although there is no DISTINCT in the COUNT, this is
    // equivalent to previous query
    sql("select deptno, salary, empid\n"
            "from emps\n"
            "group by deptno, salary, empid",
        "select deptno, count(salary) from (\n"
            "select deptno, salary\n"
            "from emps\n"
            "group by deptno, salary)\n"
            "group by deptno")
        .checkingThatResultContains("Projection\n"
                                    "│     Expressions: [deptno], count(salary):=`expr#count(salary)`\n"
                                    "└─ Gather Exchange\n"
                                    "   └─ MergingAggregated\n"
                                    "      └─ Repartition Exchange\n"
                                    "         │     Partition by: {deptno}\n"
                                    "         └─ Aggregating\n"
                                    "            │     Group by: {deptno}\n"
                                    "            │     Aggregates: expr#count(salary):=AggNull(count)(salary)\n"
                                    "            └─ Projection\n"
                                    "               │     Expressions: deptno:=`expr#deptno`, salary:=`expr#salary`\n"
                                    "               └─ MergingAggregated\n"
                                    "                  └─ Repartition Exchange\n"
                                    "                     │     Partition by: {expr#deptno, expr#salary}\n"
                                    "                     └─ Aggregating\n"
                                    "                        │     Group by: {expr#deptno, expr#salary}\n"
                                    "                        └─ Projection\n"
                                    "                           │     Expressions: expr#deptno:=deptno, expr#salary:=salary\n"
                                    "                           └─ Filter\n"
                                    "                              │     Condition: 1\n"
                                    "                              └─ TableScan test_mview.MV0_MV_DATA\n"
                                    "                                       Outputs: [deptno, salary]")
        .ok();
}
