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
#include <Optimizer/tests/gtest_base_tpcds_plan_test.h>

#include <gtest/gtest.h>

using namespace DB;

static constexpr bool WITH_STATISTICS = true;

class PlanCheckTpcds : public ::testing::Test
{
public:
    static void SetUpTestSuite()
    {
        std::unordered_map<std::string, DB::Field> settings;
#ifndef NDEBUG
        // debug mode may time out.
        settings.emplace("cascades_optimizer_timeout", "300000");
#else
        settings.emplace("cascades_optimizer_timeout", "10000");
#endif

        settings.emplace("enable_left_join_to_right_join", "false");
        tester = std::make_shared<DB::BaseTpcdsPlanTest>(WITH_STATISTICS, settings);
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
            return testing::AssertionFailure() << "\nExpected:\n" << expected << "\nActual:\n" << actual ;
    }

    static std::shared_ptr<DB::BaseTpcdsPlanTest> tester;
};

std::shared_ptr<DB::BaseTpcdsPlanTest> PlanCheckTpcds::tester;

TEST_F(PlanCheckTpcds, generate)
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

TEST_F(PlanCheckTpcds, q1)
{
    EXPECT_TRUE(equals(explain("q1"), expected("q1")));
}

TEST_F(PlanCheckTpcds, q2)
{
    EXPECT_TRUE(equals(explain("q2"), expected("q2")));
}

TEST_F(PlanCheckTpcds, q3)
{
    EXPECT_TRUE(equals(explain("q3"), expected("q3")));
}

TEST_F(PlanCheckTpcds, q4)
{
    EXPECT_TRUE(equals(explain("q4"), expected("q4")));
}

TEST_F(PlanCheckTpcds, q5)
{
    EXPECT_TRUE(equals(explain("q5"), expected("q5")));
}

TEST_F(PlanCheckTpcds, q6)
{
    EXPECT_TRUE(equals(explain("q6"), expected("q6")));
}

TEST_F(PlanCheckTpcds, q7)
{
    EXPECT_TRUE(equals(explain("q7"), expected("q7")));
}

TEST_F(PlanCheckTpcds, q8)
{
    EXPECT_TRUE(equals(explain("q8"), expected("q8")));
}

TEST_F(PlanCheckTpcds, q9)
{
    EXPECT_TRUE(equals(explain("q9"), expected("q9")));
}

TEST_F(PlanCheckTpcds, q10)
{
    EXPECT_TRUE(equals(explain("q10"), expected("q10")));
}

TEST_F(PlanCheckTpcds, q11)
{
    EXPECT_TRUE(equals(explain("q11"), expected("q11")));
}

TEST_F(PlanCheckTpcds, q12)
{
    EXPECT_TRUE(equals(explain("q12"), expected("q12")));
}

TEST_F(PlanCheckTpcds, q13)
{
    EXPECT_TRUE(equals(explain("q13"), expected("q13")));
}

TEST_F(PlanCheckTpcds, q14)
{
    EXPECT_TRUE(equals(explain("q14"), expected("q14")));
}

TEST_F(PlanCheckTpcds, q15)
{
    EXPECT_TRUE(equals(explain("q15"), expected("q15")));
}

TEST_F(PlanCheckTpcds, q16)
{
    EXPECT_TRUE(equals(explain("q16"), expected("q16")));
}

TEST_F(PlanCheckTpcds, q17)
{
    EXPECT_TRUE(equals(explain("q17"), expected("q17")));
}

TEST_F(PlanCheckTpcds, q18)
{
    EXPECT_TRUE(equals(explain("q18"), expected("q18")));
}

TEST_F(PlanCheckTpcds, q19)
{
    EXPECT_TRUE(equals(explain("q19"), expected("q19")));
}

TEST_F(PlanCheckTpcds, q20)
{
    EXPECT_TRUE(equals(explain("q20"), expected("q20")));
}

TEST_F(PlanCheckTpcds, q21)
{
    EXPECT_TRUE(equals(explain("q21"), expected("q21")));
}

TEST_F(PlanCheckTpcds, q22)
{
    EXPECT_TRUE(equals(explain("q22"), expected("q22")));
}

TEST_F(PlanCheckTpcds, q23)
{
    EXPECT_TRUE(equals(explain("q23"), expected("q23")));
}

TEST_F(PlanCheckTpcds, q24)
{
    EXPECT_TRUE(equals(explain("q24"), expected("q24")));
}

TEST_F(PlanCheckTpcds, q25)
{
    EXPECT_TRUE(equals(explain("q25"), expected("q25")));
}

TEST_F(PlanCheckTpcds, q26)
{
    EXPECT_TRUE(equals(explain("q26"), expected("q26")));
}

TEST_F(PlanCheckTpcds, q27)
{
    EXPECT_TRUE(equals(explain("q27"), expected("q27")));
}

TEST_F(PlanCheckTpcds, q28)
{
    EXPECT_TRUE(equals(explain("q28"), expected("q28")));
}

TEST_F(PlanCheckTpcds, q29)
{
    EXPECT_TRUE(equals(explain("q29"), expected("q29")));
}

TEST_F(PlanCheckTpcds, q30)
{
    EXPECT_TRUE(equals(explain("q30"), expected("q30")));
}

TEST_F(PlanCheckTpcds, q31)
{
    EXPECT_TRUE(equals(explain("q31"), expected("q31")));
}

TEST_F(PlanCheckTpcds, q32)
{
    EXPECT_TRUE(equals(explain("q32"), expected("q32")));
}

TEST_F(PlanCheckTpcds, q33)
{
    EXPECT_TRUE(equals(explain("q33"), expected("q33")));
}

TEST_F(PlanCheckTpcds, q34)
{
    EXPECT_TRUE(equals(explain("q34"), expected("q34")));
}

TEST_F(PlanCheckTpcds, q35)
{
    EXPECT_TRUE(equals(explain("q35"), expected("q35")));
}

TEST_F(PlanCheckTpcds, q36)
{
    EXPECT_TRUE(equals(explain("q36"), expected("q36")));
}

TEST_F(PlanCheckTpcds, q37)
{
    EXPECT_TRUE(equals(explain("q37"), expected("q37")));
}

TEST_F(PlanCheckTpcds, q38)
{
    EXPECT_TRUE(equals(explain("q38"), expected("q38")));
}

TEST_F(PlanCheckTpcds, q39)
{
    EXPECT_TRUE(equals(explain("q39"), expected("q39")));
}

TEST_F(PlanCheckTpcds, q40)
{
    EXPECT_TRUE(equals(explain("q40"), expected("q40")));
}

TEST_F(PlanCheckTpcds, q41)
{
    EXPECT_TRUE(equals(explain("q41"), expected("q41")));
}

TEST_F(PlanCheckTpcds, q42)
{
    EXPECT_TRUE(equals(explain("q42"), expected("q42")));
}

TEST_F(PlanCheckTpcds, q43)
{
    EXPECT_TRUE(equals(explain("q43"), expected("q43")));
}

TEST_F(PlanCheckTpcds, q44)
{
    EXPECT_TRUE(equals(explain("q44"), expected("q44")));
}

TEST_F(PlanCheckTpcds, q45)
{
    EXPECT_TRUE(equals(explain("q45"), expected("q45")));
}

TEST_F(PlanCheckTpcds, q46)
{
    EXPECT_TRUE(equals(explain("q46"), expected("q46")));
}

TEST_F(PlanCheckTpcds, q47)
{
    EXPECT_TRUE(equals(explain("q47"), expected("q47")));
}

TEST_F(PlanCheckTpcds, q48)
{
    EXPECT_TRUE(equals(explain("q48"), expected("q48")));
}

TEST_F(PlanCheckTpcds, q49)
{
    EXPECT_TRUE(equals(explain("q49"), expected("q49")));
}

TEST_F(PlanCheckTpcds, q50)
{
    EXPECT_TRUE(equals(explain("q50"), expected("q50")));
}

TEST_F(PlanCheckTpcds, q51)
{
    EXPECT_TRUE(equals(explain("q51"), expected("q51")));
}

TEST_F(PlanCheckTpcds, q52)
{
    EXPECT_TRUE(equals(explain("q52"), expected("q52")));
}

TEST_F(PlanCheckTpcds, q53)
{
    EXPECT_TRUE(equals(explain("q53"), expected("q53")));
}

TEST_F(PlanCheckTpcds, q54)
{
    EXPECT_TRUE(equals(explain("q54"), expected("q54")));
}

TEST_F(PlanCheckTpcds, q55)
{
    EXPECT_TRUE(equals(explain("q55"), expected("q55")));
}

TEST_F(PlanCheckTpcds, q56)
{
    EXPECT_TRUE(equals(explain("q56"), expected("q56")));
}

TEST_F(PlanCheckTpcds, q57)
{
    EXPECT_TRUE(equals(explain("q57"), expected("q57")));
}

TEST_F(PlanCheckTpcds, q58)
{
    EXPECT_TRUE(equals(explain("q58"), expected("q58")));
}

TEST_F(PlanCheckTpcds, q59)
{
    EXPECT_TRUE(equals(explain("q59"), expected("q59")));
}

TEST_F(PlanCheckTpcds, q60)
{
    EXPECT_TRUE(equals(explain("q60"), expected("q60")));
}

TEST_F(PlanCheckTpcds, q61)
{
    EXPECT_TRUE(equals(explain("q61"), expected("q61")));
}

TEST_F(PlanCheckTpcds, q62)
{
    EXPECT_TRUE(equals(explain("q62"), expected("q62")));
}

TEST_F(PlanCheckTpcds, q63)
{
    EXPECT_TRUE(equals(explain("q63"), expected("q63")));
}

TEST_F(PlanCheckTpcds, q64)
{
    EXPECT_TRUE(equals(explain("q64"), expected("q64")));
}

TEST_F(PlanCheckTpcds, q65)
{
    EXPECT_TRUE(equals(explain("q65"), expected("q65")));
}

TEST_F(PlanCheckTpcds, q66)
{
    EXPECT_TRUE(equals(explain("q66"), expected("q66")));
}

TEST_F(PlanCheckTpcds, q67)
{
    EXPECT_TRUE(equals(explain("q67"), expected("q67")));
}

TEST_F(PlanCheckTpcds, q68)
{
    EXPECT_TRUE(equals(explain("q68"), expected("q68")));
}

TEST_F(PlanCheckTpcds, q69)
{
    EXPECT_TRUE(equals(explain("q69"), expected("q69")));
}

TEST_F(PlanCheckTpcds, q70)
{
    EXPECT_TRUE(equals(explain("q70"), expected("q70")));
}

TEST_F(PlanCheckTpcds, q71)
{
    EXPECT_TRUE(equals(explain("q71"), expected("q71")));
}

TEST_F(PlanCheckTpcds, q72)
{
    EXPECT_TRUE(equals(explain("q72"), expected("q72")));
}

TEST_F(PlanCheckTpcds, q73)
{
    EXPECT_TRUE(equals(explain("q73"), expected("q73")));
}

TEST_F(PlanCheckTpcds, q74)
{
    EXPECT_TRUE(equals(explain("q74"), expected("q74")));
}

TEST_F(PlanCheckTpcds, q75)
{
    EXPECT_TRUE(equals(explain("q75"), expected("q75")));
}

TEST_F(PlanCheckTpcds, q76)
{
    EXPECT_TRUE(equals(explain("q76"), expected("q76")));
}

TEST_F(PlanCheckTpcds, q77)
{
    EXPECT_TRUE(equals(explain("q77"), expected("q77")));
}

TEST_F(PlanCheckTpcds, q78)
{
    EXPECT_TRUE(equals(explain("q78"), expected("q78")));
}

TEST_F(PlanCheckTpcds, q79)
{
    EXPECT_TRUE(equals(explain("q79"), expected("q79")));
}

TEST_F(PlanCheckTpcds, q80)
{
    EXPECT_TRUE(equals(explain("q80"), expected("q80")));
}

TEST_F(PlanCheckTpcds, q81)
{
    EXPECT_TRUE(equals(explain("q81"), expected("q81")));
}

TEST_F(PlanCheckTpcds, q82)
{
    EXPECT_TRUE(equals(explain("q82"), expected("q82")));
}

TEST_F(PlanCheckTpcds, q83)
{
    EXPECT_TRUE(equals(explain("q83"), expected("q83")));
}

TEST_F(PlanCheckTpcds, q84)
{
    EXPECT_TRUE(equals(explain("q84"), expected("q84")));
}

TEST_F(PlanCheckTpcds, q85)
{
    EXPECT_TRUE(equals(explain("q85"), expected("q85")));
}

TEST_F(PlanCheckTpcds, q86)
{
    EXPECT_TRUE(equals(explain("q86"), expected("q86")));
}

TEST_F(PlanCheckTpcds, q87)
{
    EXPECT_TRUE(equals(explain("q87"), expected("q87")));
}

TEST_F(PlanCheckTpcds, q88)
{
    EXPECT_TRUE(equals(explain("q88"), expected("q88")));
}

TEST_F(PlanCheckTpcds, q89)
{
    EXPECT_TRUE(equals(explain("q89"), expected("q89")));
}

TEST_F(PlanCheckTpcds, q90)
{
    EXPECT_TRUE(equals(explain("q90"), expected("q90")));
}

TEST_F(PlanCheckTpcds, q91)
{
    EXPECT_TRUE(equals(explain("q91"), expected("q91")));
}

TEST_F(PlanCheckTpcds, q92)
{
    EXPECT_TRUE(equals(explain("q92"), expected("q92")));
}

TEST_F(PlanCheckTpcds, q93)
{
    EXPECT_TRUE(equals(explain("q93"), expected("q93")));
}

TEST_F(PlanCheckTpcds, q94)
{
    EXPECT_TRUE(equals(explain("q94"), expected("q94")));
}

TEST_F(PlanCheckTpcds, q95)
{
    EXPECT_TRUE(equals(explain("q95"), expected("q95")));
}

TEST_F(PlanCheckTpcds, q96)
{
    EXPECT_TRUE(equals(explain("q96"), expected("q96")));
}

TEST_F(PlanCheckTpcds, q97)
{
    EXPECT_TRUE(equals(explain("q97"), expected("q97")));
}

TEST_F(PlanCheckTpcds, q98)
{
    EXPECT_TRUE(equals(explain("q98"), expected("q98")));
}

TEST_F(PlanCheckTpcds, q99)
{
    EXPECT_TRUE(equals(explain("q99"), expected("q99")));
}
