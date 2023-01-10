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

#include <utility>
#include <Optimizer/PlanNodeSearcher.h>
#include <QueryPlan/PlanPrinter.h>
#include <Databases/DatabaseMemory.h>
#include <gtest/gtest.h>
#include "gtest_base_tpcds_plan_test.h"

namespace DB
{

class MaterializedViewRewriteTester
{
public:
    MaterializedViewRewriteTester(std::shared_ptr<BasePlanTest> tester_, String query_, std::vector<String> materialized_views_)
        : tester(std::move(tester_)), query(std::move(query_)), materialized_views(std::move(materialized_views_))
    {
    }

    void ok()
    {
        withChecker([&](QueryPlan & plan) {
            auto tables = PlanNodeSearcher::searchFrom(plan)
                              .where([](auto & node) { return node.getStep()->getType() == IQueryPlanStep::Type::TableScan; })
                              .findAll();
            bool contains_mv = std::any_of(tables.begin(), tables.end(), [&](auto & node) {
                return dynamic_cast<const TableScanStep *>(node->getStep().get())->getTable().find("MV_DATA") != std::string::npos;
            });
            ASSERT_TRUE(contains_mv) << "expect matched materialized views";
        }).execution();
    }

    void noMat()
    {
        withChecker([&](QueryPlan & plan) {
            auto tables = PlanNodeSearcher::searchFrom(plan)
                              .where([](auto & node) { return node.getStep()->getType() == IQueryPlanStep::Type::TableScan; })
                              .findAll();
            bool contains_mv = std::any_of(tables.begin(), tables.end(), [&](auto & node) {
                return dynamic_cast<const TableScanStep *>(node->getStep().get())->getStorage()->getName().find("MV_DATA")
                    != std::string::npos;
            });
            ASSERT_FALSE(contains_mv) << "expect no matched materialized views";
        }).execution();
    }

    void execution()
    {
        for (size_t i = 0; i < materialized_views.size(); i++)
        {
            String mv_name = tester->getDefaultDatabase() + "." + "MV" + std::to_string(i);
            String target_table_name = mv_name + "_MV_DATA";

            tester->execute("DROP TABLE IF EXISTS " + mv_name);
            tester->execute("DROP TABLE IF EXISTS " + target_table_name);
            tester->execute("CREATE TABLE " + target_table_name + " ENGINE=Memory() AS " + materialized_views[i]);
            auto create_mv = String("CREATE MATERIALIZED VIEW ")
                                 .append(mv_name)
                                 .append(" TO ")
                                 .append(target_table_name)
                                 .append(" AS ")
                                 .append(materialized_views[i]);
            tester->execute(create_mv);
        }

        auto plan = tester->plan(query);

        for (size_t i = 0; i < materialized_views.size(); i++)
        {
            String mv_name = "MV" + std::to_string(i);
            String target_table_name = mv_name + "_MV_DATA";

            tester->execute("DROP TABLE IF EXISTS " + mv_name);
            tester->execute("DROP TABLE IF EXISTS " + target_table_name);
        }
        for (auto & checker : checkers)
            checker(*plan);
    }

    MaterializedViewRewriteTester & withChecker(std::function<void(QueryPlan &)> && checker)
    {
        checkers.emplace_back(checker);
        return *this;
    }

    template <class... T>
    MaterializedViewRewriteTester & checkingThatResultContains(const T &... expected_results)
    {
        return withChecker([&](QueryPlan & plan) {
            auto explain = PlanPrinter::textLogicalPlan(plan, tester->createQueryContext(), false, true);
            for (auto & expected_result : std::vector<String>{expected_results...})
            {
                bool contains = explain.find(expected_result) != std::string::npos;
                EXPECT_TRUE(contains) << "Expected result:\n" << expected_result << "\nnot found in plan:\n" << explain;
                (void)contains;
            }

        });
    }

private:
    std::shared_ptr<BasePlanTest> tester;
    String query;
    std::vector<String> materialized_views;
    std::vector<std::function<void(QueryPlan &)>> checkers;
};

class BaseMaterializedViewTest : public BasePlanTest
{
public:
    explicit BaseMaterializedViewTest(const std::unordered_map<String, Field> & settings = {})
        : BasePlanTest("test_mview", settings)
    {
        execute("CREATE TABLE IF NOT EXISTS emps("
                "  empid UInt32,"
                "  deptno UInt32,"
                "  name Nullable(String),"
                "  salary Nullable(Float64),"
                "  commission Nullable(UInt32)"
                ") ENGINE=Memory();");

        execute("CREATE TABLE IF NOT EXISTS depts("
                "  deptno UInt32,"
                "  name Nullable(String)"
                ") ENGINE=Memory();");

        execute("CREATE TABLE IF NOT EXISTS locations("
                "  locationid UInt32,"
                "  name Nullable(String)"
                ") ENGINE=Memory();");

        if (DatabaseCatalog::instance().tryGetDatabase("foodmart"))
            DatabaseCatalog::instance().detachDatabase(session_context, "foodmart", true, false);

        auto database = std::make_shared<DatabaseMemory>("foodmart", session_context);
        DatabaseCatalog::instance().attachDatabase("foodmart", database);

        execute("CREATE TABLE IF NOT EXISTS foodmart.sales_fact_1997 (\n"
                        "  product_id Int32,\n"
                        "  customer_id Int32,\n"
                        "  store_id Int32,\n"
                        "  time_id Int32,\n"
                        "  store_sales Float64,\n"
                        "  store_cost Float64,\n"
                        "  unit_sales Int32"
                        ") ENGINE=Memory();");
        execute("CREATE TABLE IF NOT EXISTS foodmart.time_by_day (\n"
                        "  time_id Int32,\n"
                        "  the_month String,\n"
                        "  quater String,\n"
                        "  the_year Int32"
                        ") ENGINE=Memory();");
    }

    MaterializedViewRewriteTester sql(String materialize, String query)
    {
        return MaterializedViewRewriteTester{this->shared_from_this(), std::move(query), {std::move(materialize)}};
    }
};
}
