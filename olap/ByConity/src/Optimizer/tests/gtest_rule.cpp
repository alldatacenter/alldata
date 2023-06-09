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

#include <Interpreters/Context.h>
#include <Optimizer/tests/gtest_optimizer_test_utils.h>

#include <Common/tests/gtest_global_context.h>

#include <gtest/gtest.h>

using namespace DB;

struct TestRule : public Rule
{
    const static Capture aggregatingCap;
    const static Capture filterCap;

    RuleType getType() const override{
        return RuleType::NUM_RULES;
    }
    String getName() const override{
        return "NUM_RULES";
    }
    PatternPtr getPattern() const override{
        using namespace DB::Patterns;
        return aggregating()->capturedAs(aggregatingCap)->withSingle(filter()->capturedAs(filterCap));
    }
    TransformResult transformImpl(PlanNodePtr, const Captures & captures, RuleContext &) override
    {
        const auto aggNode = captures.at<PlanNodePtr>(aggregatingCap);
        const auto * aggStep = dynamic_cast<const MockedAggregatingStep *>(aggNode->getStep().get());

        const auto filterNode = captures.at<PlanNodePtr>(filterCap);
        const auto * filterStep = dynamic_cast<const MockedFilterStep *>(filterNode->getStep().get());

        return {createFilterNode(
            filterStep->column,
            filterStep->filter,
            {createAggregatingNode(aggStep->key, aggStep->aggregator, aggStep->final, filterNode->getChildren())})};
    }
};

const Capture TestRule::aggregatingCap{"agg"}; // NOLINT(cert-err58-cpp)
const Capture TestRule::filterCap{"filter"}; // NOLINT(cert-err58-cpp)

TEST(OptimizerRuleTest, Rule)
{
    TestRule rule;
    CTEInfo cte_info;
    auto context = Context::createCopy(getContext().context);
    RuleContext ctx{.context = context, .cte_info = cte_info};

    PlanNodePtr plan1 = createAggregatingNode(
        "k1",
        "sum",
        false,
        {createFilterNode("c2", "eq", {createTableScanNode("db1", "t1", {}), createTableScanNode("db1", "t2", {})})});

    PlanNodePtr plan2 = createAggregatingNode(
        "k1",
        "sum",
        false,
        {createJoinNode(
            ASTTableJoin::Kind::Inner, {"jk1"}, {createTableScanNode("db1", "t1", {}), createTableScanNode("db1", "t2", {})})});
    auto res1 = rule.transform(plan1, ctx);
    ASSERT_TRUE(!res1.empty());
    auto & transformed = res1.getPlans()[0];
    // check filter node
    ASSERT_TRUE(transformed->getStep()->getType() == DB::IQueryPlanStep::Type::Filter);
    ASSERT_TRUE(dynamic_cast<const MockedFilterStep *>(transformed->getStep().get())->column == "c2");
    ASSERT_TRUE(dynamic_cast<const MockedFilterStep *>(transformed->getStep().get())->filter == "eq");
    ASSERT_TRUE(transformed->getChildren().size() == 1);
    // check aggregating node
    ASSERT_TRUE(transformed->getChildren()[0]->getStep()->getType() == DB::IQueryPlanStep::Type::Aggregating);
    ASSERT_TRUE(dynamic_cast<const MockedAggregatingStep *>(transformed->getChildren()[0]->getStep().get())->key == "k1");
    ASSERT_TRUE(dynamic_cast<const MockedAggregatingStep *>(transformed->getChildren()[0]->getStep().get())->aggregator == "sum");
    ASSERT_TRUE(!dynamic_cast<const MockedAggregatingStep *>(transformed->getChildren()[0]->getStep().get())->final);
    // check read nodes
    ASSERT_TRUE(transformed->getChildren()[0]->getChildren().size() == 2);
    ASSERT_TRUE(transformed->getChildren()[0]->getChildren()[0]->getStep()->getType() == DB::IQueryPlanStep::Type::TableScan);
    ASSERT_TRUE(transformed->getChildren()[0]->getChildren()[0] == plan1->getChildren()[0]->getChildren()[0]);
    ASSERT_TRUE(transformed->getChildren()[0]->getChildren()[1] == plan1->getChildren()[0]->getChildren()[1]);

    auto res2 = rule.transform(plan2, ctx);
    ASSERT_TRUE(res2.empty());
}
