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

#include <Optimizer/tests/gtest_optimizer_test_utils.h>

#include <gtest/gtest.h>

using namespace DB;
using namespace DB::Patterns;

TEST(OptimizerPatternsTest, GetPatternTargetType)
{
    PatternPtr pattern1 = filter();
    PatternPtr pattern2 = join()->withSingle(filter()->withSingle(tableScan()));

    ASSERT_TRUE(pattern1->getTargetType() == IQueryPlanStep::Type::Filter);
    ASSERT_TRUE(pattern2->getTargetType() == IQueryPlanStep::Type::Join);
}

TEST(OptimizerPatternsTest, PredicateNot)
{
    PatternPredicate t = [](const PlanNodePtr &, const Captures &) { return true; };
    PatternPredicate f = predicateNot(t);
    PlanNodePtr node;
    Captures captures;
    ASSERT_TRUE(t(node, captures));
    ASSERT_TRUE(!f(node, captures));
}

TEST(OptimizerPatternsTest, ToString)
{
    Capture cap1;
    PatternPtr pattern
        = Patterns::any()->matching([](const auto &, const auto &) { return true; })->capturedAs(cap1)->withSingle(Patterns::any());

    std::cout << pattern->toString() << std::endl;
}
