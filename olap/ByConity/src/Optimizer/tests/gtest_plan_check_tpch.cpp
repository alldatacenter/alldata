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

#include <QueryPlan/PlanPrinter.h>
#include <Optimizer/tests/gtest_base_tpch_plan_test.h>

#include <gtest/gtest.h>

using namespace DB;

class PlanCheckTpch : public ::testing::Test
{
public:
    static void SetUpTestCase()
    {
        std::unordered_map<std::string, DB::Field> settings;
#ifndef NDEBUG
        // debug mode may time out.
        settings.emplace("cascades_optimizer_timeout", "300000");
#endif

        settings.emplace("enable_left_join_to_right_join", "false");
        tester = std::make_shared<DB::BaseTpchPlanTest>(settings);
    }

    static std::string explain(const std::string & name)
    {
        return tester->explain(name);
    }

    static std::string expected(const std::string & name)
    {
        return tester->loadExplain(name);
    }

    static testing::AssertionResult equals(const std::string & actual, const std::string & expected)
    {
        if (actual == expected)
            return testing::AssertionSuccess();
        else
            return testing::AssertionFailure() << "\nExpected:\n" << expected << "\nActual:\n" << actual;
    }

    static std::shared_ptr<DB::BaseTpchPlanTest> tester;
};

std::shared_ptr<DB::BaseTpchPlanTest> PlanCheckTpch::tester;

TEST_F(PlanCheckTpch, generate)
{
    if (!AbstractPlanTestSuite::enforce_regenerate())
        GTEST_SKIP() << "skip generate. set env REGENERATE=1 to regenerate explains.";
    for (auto & query : tester->loadQueries())
    {
        try
        {
            std::cout << " try generate for " + query + "." << std::endl;
            tester->saveExplain(query, explain(query));
        }
        catch (...)
        {
            std::cerr << " generate for " + query + " failed." << std::endl;
            tester->saveExplain(query, "");
        }
    }
}

TEST_F(PlanCheckTpch, q1)
{
    EXPECT_TRUE(equals(explain("q1"), expected("q1")));
}

TEST_F(PlanCheckTpch, q2)
{
    EXPECT_TRUE(equals(explain("q2"), expected("q2")));
}

TEST_F(PlanCheckTpch, q3)
{
    EXPECT_TRUE(equals(explain("q3"), expected("q3")));
}

TEST_F(PlanCheckTpch, q4)
{
    EXPECT_TRUE(equals(explain("q4"), expected("q4")));
}

TEST_F(PlanCheckTpch, q5)
{
    EXPECT_TRUE(equals(explain("q5"), expected("q5")));
}

TEST_F(PlanCheckTpch, q6)
{
    EXPECT_TRUE(equals(explain("q6"), expected("q6")));
}

TEST_F(PlanCheckTpch, q7)
{
    EXPECT_TRUE(equals(explain("q7"), expected("q7")));
}

TEST_F(PlanCheckTpch, q8)
{
    EXPECT_TRUE(equals(explain("q8"), expected("q8")));
}

TEST_F(PlanCheckTpch, q9)
{
    EXPECT_TRUE(equals(explain("q9"), expected("q9")));
}

TEST_F(PlanCheckTpch, q10)
{
    EXPECT_TRUE(equals(explain("q10"), expected("q10")));
}

TEST_F(PlanCheckTpch, q11)
{
    EXPECT_TRUE(equals(explain("q11"), expected("q11")));
}

TEST_F(PlanCheckTpch, q12)
{
    EXPECT_TRUE(equals(explain("q12"), expected("q12")));
}

TEST_F(PlanCheckTpch, q13)
{
    EXPECT_TRUE(equals(explain("q13"), expected("q13")));
}

TEST_F(PlanCheckTpch, q14)
{
    EXPECT_TRUE(equals(explain("q14"), expected("q14")));
}

TEST_F(PlanCheckTpch, q15)
{
    EXPECT_TRUE(equals(explain("q15"), expected("q15")));
}

TEST_F(PlanCheckTpch, q16)
{
    EXPECT_TRUE(equals(explain("q16"), expected("q16")));
}

TEST_F(PlanCheckTpch, q17)
{
    EXPECT_TRUE(equals(explain("q17"), expected("q17")));
}

TEST_F(PlanCheckTpch, q18)
{
    EXPECT_TRUE(equals(explain("q18"), expected("q18")));
}

TEST_F(PlanCheckTpch, q19)
{
    EXPECT_TRUE(equals(explain("q19"), expected("q19")));
}

TEST_F(PlanCheckTpch, q20)
{
    EXPECT_TRUE(equals(explain("q20"), expected("q20")));
}

TEST_F(PlanCheckTpch, q21)
{
    EXPECT_TRUE(equals(explain("q21"), expected("q21")));
}

TEST_F(PlanCheckTpch, q22)
{
    EXPECT_TRUE(equals(explain("q22"), expected("q22")));
}
