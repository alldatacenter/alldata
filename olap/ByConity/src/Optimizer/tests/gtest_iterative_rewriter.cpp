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

#include <chrono>
#include <thread>
#include <unordered_set>
#include <Interpreters/Context.h>
#include <Optimizer/tests/gtest_optimizer_test_utils.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <Common/tests/gtest_global_context.h>

#include <gtest/gtest.h>

using namespace DB;
using namespace DB::Patterns;

namespace DB::ErrorCodes
{
extern const int OPTIMIZER_TIMEOUT;
}

struct MergeRule : public Rule
{
    static const Capture subNodeCap;

    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override { return filter()->withSingle(filter()->capturedAs(subNodeCap)); }
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext &) override
    {
        auto subNode = captures.at<PlanNodePtr>(subNodeCap);
        int i = dynamic_cast<const MockedStepForRewriterTest *>(node->getStep().get())->i;
        if (i == dynamic_cast<const MockedStepForRewriterTest *>(subNode->getStep().get())->i)
        {
            return {createRewriteTestNode(i * 2, subNode->getChildren())};
        }
        else
            return {};
    }
};

const Capture MergeRule::subNodeCap{"sub"}; // NOLINT(cert-err58-cpp)

struct RemoveRule : public Rule
{
    int target;
    RemoveRule(int target_) : target(target_) { }
    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override { return filter(); }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override
    {
        if (node->getChildren().size() == 1 && dynamic_cast<const MockedStepForRewriterTest *>(node->getStep().get())->i == target)
            return {node->getChildren()[0]};
        else
            return {};
    }
};

struct RecorderRule : public Rule
{
    int calls = 0;
    std::unordered_set<PlanNodePtr> seen;
    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override { return DB::Patterns::any(); }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override
    {
        ++calls;
        seen.insert(node);
        return {};
    }
};

struct AddExchangeRule : public Rule
{
    const static Capture subNodeCap;

    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override
    {
        static PatternPtr pat = join()->withAny(
            DB::Patterns::any()
                ->matchingStep<IQueryPlanStep>([](auto & step) { return step.getType() != IQueryPlanStep::Type::Exchange; })
                ->capturedAs(subNodeCap));
        return pat;
    }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext & context) override
    {
        PlanNodes replaced;

        for (auto & child : node->getChildren())
        {
            if (node->getStep()->getType() != IQueryPlanStep::Type::Exchange)
            {
                auto exchange = createExchangeNode(ExchangeMode::BROADCAST, child->getStep()->getOutputStream(), {child});
                replaced.emplace_back(exchange);
            }
            else
            {
                replaced.emplace_back(child);
            }
        }

        replaceChildrenAndInputStream(node, replaced);
        auto new_step = node->getStep()->copy(context.context);
        DataStreams input_streams_;
        std::transform(replaced.begin(), replaced.end(), std::back_inserter(input_streams_), [](PlanNodePtr & node_) {
            return node_->getStep()->getOutputStream();
        });
        new_step->setInputStreams(input_streams_);
        node->setStep(new_step);
        return node;
    }
};
const Capture AddExchangeRule::subNodeCap;

struct FillDbNameRule : public Rule
{
    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override
    {
        static PatternPtr pat = tableScan()->matchingStep<MockedTableScanStep>([](auto & step) { return step.database == ""; });
        return pat;
    }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override
    {
        auto step = std::make_shared<MockedTableScanStep>(
            "db1",
            (dynamic_cast<const MockedTableScanStep *>(node->getStep().get()))->table,
            (dynamic_cast<const MockedTableScanStep *>(node->getStep().get()))->getOutputStream());
        return PlanNodeBase::createPlanNode(node->getId(), step, {});
    }
};

struct RemoveFilterNodeRule : public Rule
{
    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override { return filter(); }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override
    {
        return node->getChildren().empty() ? nullptr : node->getChildren()[0];
    }
};

// the iterative version of bubble sort
struct SortRule : public Rule
{
    RuleType getType() const override { return RuleType::NUM_RULES; }

    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override
    {
        static Capture outerValCap;
        static Capture innerValCap;

        static PatternPtr pattern
            = filter()
                  ->capturedStepAs<MockedStepForRewriterTest>(outerValCap, &MockedStepForRewriterTest::i)
                  ->withSingle(filter()->capturedStepAs<MockedStepForRewriterTest>(innerValCap, &MockedStepForRewriterTest::i))
                  ->matchingCapture([](const Captures & caps) { return caps.at<int>(outerValCap) > caps.at<int>(innerValCap); });
        return pattern;
    }

    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override
    {
        PlanNodePtr subnode = node->getChildren()[0];
        node->getChildren() = std::move(subnode->getChildren());
        subnode->getChildren().emplace_back(node);
        return subnode;
    }
};

struct NeverEndRule : public Rule
{
    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override { return DB::Patterns::any(); }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override { return node; }
};

struct SleepRule : public Rule
{
    SleepRule(int64_t sleep_) : sleep(sleep_) { }

    int runs = 0;
    int64_t sleep;
    RuleType getType() const override { return RuleType::NUM_RULES; }
    String getName() const override { return "NUM_RULES"; }
    PatternPtr getPattern() const override { return DB::Patterns::any(); }
    TransformResult transformImpl(PlanNodePtr node, const Captures &, RuleContext &) override
    {
        ++runs;
        std::this_thread::sleep_for(std::chrono::milliseconds(sleep));
        return node;
    }
};

void check_continuous_nodes(PlanNodePtr node, int index, std::string testname)
{
    ASSERT_EQ(dynamic_cast<const MockedStepForRewriterTest *>(node->getStep().get())->i, index)
        << testname << " fails, index: " + std::to_string(index) << ", reason: "
        << "not expected number";
    if (node->getChildren().size() > 0)
    {
        ASSERT_EQ(node->getChildren().size(), 1) << testname << " fails, index: " + std::to_string(index) << ", reason: "
                                                   << "not single child";
        check_continuous_nodes(node->getChildren()[0], index + 1, testname);
    }
}

TEST(OptimizerIterativeRewriterTest, Coerce)
{
    IterativeRewriter rewriter{{std::make_shared<MergeRule>()}, "test"};

    PlanNodePtr plan = createRewriteTestNode(1, {createRewriteTestNode(1, {createRewriteTestNode(100, {})})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);
    PlanNodePtr result = query_plan.getPlanNode();

    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result->getStep().get())->i == 2);
    ASSERT_TRUE(result->getChildren().size() == 1);
    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result->getChildren()[0]->getStep().get())->i == 100);
    ASSERT_TRUE(result->getChildren()[0]->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, SingleRuleRecursivelyApplication)
{
    IterativeRewriter rewriter{{std::make_shared<MergeRule>()}, "test"};

    PlanNodePtr plan = createRewriteTestNode(
        1, {createRewriteTestNode(1, {createRewriteTestNode(2, {createRewriteTestNode(4, {createRewriteTestNode(8, {})})})})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);
    PlanNodePtr result = query_plan.getPlanNode();

    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result->getStep().get())->i == 16);
    ASSERT_TRUE(result->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, CollaborativeRulesApplication)
{
    IterativeRewriter rewriter{{std::make_shared<MergeRule>(), std::make_shared<RemoveRule>(2)}, "test"};

    PlanNodePtr plan = createRewriteTestNode(
        1, {createRewriteTestNode(1, {createRewriteTestNode(1, {createRewriteTestNode(1, {createRewriteTestNode(1, {})})})})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);
    PlanNodePtr result = query_plan.getPlanNode();
    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result->getStep().get())->i == 1);
    ASSERT_TRUE(result->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, CollaborativeRulesOrder)
{
    IterativeRewriter rewriter1{{std::make_shared<MergeRule>(), std::make_shared<RemoveRule>(1)}, "test"};

    IterativeRewriter rewriter2{{std::make_shared<RemoveRule>(1), std::make_shared<MergeRule>()}, "test"};

    PlanNodePtr plan = createRewriteTestNode(1, {createRewriteTestNode(1, {})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter1.rewrite(query_plan, context);
    PlanNodePtr result1 = query_plan.getPlanNode();
    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result1->getStep().get())->i == 2);
    ASSERT_TRUE(result1->getChildren().empty());

    QueryPlan query_plan2 = createQueryPlan(plan);
    rewriter2.rewrite(query_plan2, context);
    PlanNodePtr result2 = query_plan2.getPlanNode();
    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result2->getStep().get())->i == 1);
    ASSERT_TRUE(result2->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, RuleForArbitraryNodeType)
{
    std::shared_ptr<RecorderRule> rule = std::make_shared<RecorderRule>();
    IterativeRewriter rewriter{{rule}, "test"};
    PlanNodePtr plan = createAggregatingNode(
        "k1",
        "sum",
        true,
        {createJoinNode(
            ASTTableJoin::Kind::Inner, {"names"}, {createTableScanNode("db1", "t1", {}), createTableScanNode("db1", "t2", {})})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);
    PlanNodePtr result = query_plan.getPlanNode();

    ASSERT_TRUE(result == plan);
    ASSERT_TRUE(rule->calls == 4);
    ASSERT_TRUE(rule->seen.contains(plan));
    ASSERT_TRUE(rule->seen.contains(plan->getChildren()[0]));
    ASSERT_TRUE(rule->seen.contains(plan->getChildren()[0]->getChildren()[0]));
    ASSERT_TRUE(rule->seen.contains(plan->getChildren()[0]->getChildren()[1]));
}

TEST(OptimizerIterativeRewriterTest, DISABLED_RuleApplication)
{
    RulePtr fillDBNameRule = std::make_shared<FillDbNameRule>();
    RulePtr addExchangeRule = std::make_shared<AddExchangeRule>();
    IterativeRewriter rewriter{{fillDBNameRule, addExchangeRule}, "test"};

    PlanNodePtr plan = createJoinNode(
        ASTTableJoin::Kind::Inner,
        {},
        createDataStream({"a", "b", "c", "d"}),
        {createTableScanNode("", "t1", createDataStream({"a", "b"}), {}),
         createTableScanNode("db1", "t2", createDataStream({"c", "d"}), {})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);
    PlanNodePtr result = query_plan.getPlanNode();

    ASSERT_TRUE(result == plan);
    check_input_stream(result);
    ASSERT_TRUE(result->getChildren().size() == 2);
    for (auto & exchange : result->getChildren())
    {
        ASSERT_TRUE(exchange->getStep()->getType() == IQueryPlanStep::Type::Exchange);
        check_input_stream(exchange);
        ASSERT_TRUE(exchange->getChildren().size() == 1);

        auto & read = exchange->getChildren()[0];
        ASSERT_TRUE(read->getStep()->getType() == IQueryPlanStep::Type::TableScan);
        auto readStep = dynamic_cast<const MockedTableScanStep *>(read->getStep().get());
        ASSERT_TRUE(readStep->database == "db1");

        if (readStep->table == "t1")
        {
            check_data_stream(readStep->getOutputStream(), createDataStream({"a", "b"}));
        }
        else
        {
            ASSERT_TRUE(readStep->table == "t2");
            check_data_stream(readStep->getOutputStream(), createDataStream({"c", "d"}));
        }

        ASSERT_TRUE(read->getChildren().empty());
        check_input_stream(read);
    }
}

TEST(OptimizerIterativeRewriterTest, DISABLED_RemoveNodeRule)
{
    RulePtr removeNodeRule = std::make_shared<RemoveFilterNodeRule>();
    IterativeRewriter removeNodeRewriter{{removeNodeRule}, "test"};

    PlanNodePtr plan1 = createFilterNode("", "", {});
    PlanNodePtr plan2 = createAggregatingNode(
        "",
        "",
        true,
        {createFilterNode("", "", createDataStream({"a"}), {}),
         createFilterNode("", "", createDataStream({"b"}), {createTableScanNode("", "", createDataStream({"c"}), {})}),
         createFilterNode("", "", createDataStream({"d"}), {createFilterNode("", "", createDataStream({"e"}), {})})});

    QueryPlan query_plan1 = createQueryPlan(plan1);
    auto context = Context::createCopy(getContext().context);
    removeNodeRewriter.rewrite(query_plan1, context);
    PlanNodePtr res1 = query_plan1.getPlanNode();
    ASSERT_TRUE(res1 == nullptr);

    QueryPlan query_plan2 = createQueryPlan(plan2);
    removeNodeRewriter.rewrite(query_plan2, context);
    PlanNodePtr res2 = query_plan2.getPlanNode();
    ASSERT_TRUE(res2 == plan2);
    check_input_stream(res2);
    check_data_stream(res2->getStep()->getInputStreams()[0], createDataStream({"c"}));
    ASSERT_TRUE(res2->getChildren().size() == 1);
    ASSERT_TRUE(res2->getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::TableScan);
    check_input_stream(res2->getChildren()[0]);
    ASSERT_TRUE(res2->getChildren()[0]->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, TimeoutExhaust)
{
    auto context = Context::createCopy(getContext().context);
    context->setSetting("iterative_optimizer_timeout", 3000);
    RulePtr neverEndRule = std::make_shared<NeverEndRule>();
    IterativeRewriter neverEndRewriter{{neverEndRule}, "test"};

    PlanNodePtr plan1 = createFilterNode("", "", {});
    QueryPlan query_plan1 = createQueryPlan(plan1);

    ASSERT_THROW_DB_EXCEPTION_WITH_ERROR_CODE(neverEndRewriter.rewrite(query_plan1, context), ErrorCodes::OPTIMIZER_TIMEOUT);

    std::shared_ptr<SleepRule> sleepRule = std::make_shared<SleepRule>(2500);
    IterativeRewriter sleepRewriter{{sleepRule}, "test"};

    ASSERT_THROW_DB_EXCEPTION_WITH_ERROR_CODE(sleepRewriter.rewrite(query_plan1, context), ErrorCodes::OPTIMIZER_TIMEOUT);
    ASSERT_TRUE(sleepRule->runs > 0 && sleepRule->runs < 5);
}

TEST(OptimizerIterativeRewriterTest, ChildrenRewrite)
{
    IterativeRewriter rewriter{{std::make_shared<MergeRule>()}, "test"};

    PlanNodePtr plan = createRewriteTestNode(100, {createRewriteTestNode(1, {createRewriteTestNode(1, {})})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);

    PlanNodePtr result = query_plan.getPlanNode();

    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result->getStep().get())->i == 100);
    ASSERT_TRUE(result->getChildren().size() == 1);
    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result->getChildren()[0]->getStep().get())->i == 2);
    ASSERT_TRUE(result->getChildren()[0]->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, ChildrenRewriteLeadToNodeRewrite)
{
    IterativeRewriter rewriter{{std::make_shared<MergeRule>()}, "test"};

    PlanNodePtr plan1 = createRewriteTestNode(2, {createRewriteTestNode(1, {createRewriteTestNode(1, {})})});
    QueryPlan query_plan1 = createQueryPlan(plan1);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan1, context);
    PlanNodePtr result1 = query_plan1.getPlanNode();

    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result1->getStep().get())->i == 4);
    ASSERT_TRUE(result1->getChildren().empty());

    // recursive
    PlanNodePtr plan2 = createRewriteTestNode(
        8, {createRewriteTestNode(4, {createRewriteTestNode(2, {createRewriteTestNode(1, {createRewriteTestNode(1, {})})})})});

    QueryPlan query_plan2 = createQueryPlan(plan2);
    rewriter.rewrite(query_plan2, context);
    PlanNodePtr result2 = query_plan2.getPlanNode();

    ASSERT_TRUE(dynamic_cast<const MockedStepForRewriterTest *>(result2->getStep().get())->i == 16);
    ASSERT_TRUE(result2->getChildren().empty());
}

TEST(OptimizerIterativeRewriterTest, ChildrenRewriteLeadToNodeRewriteLeadToChildrenRewrite)
{
    IterativeRewriter rewriter{{std::make_shared<SortRule>()}, "test"};

    PlanNodePtr plan = createRewriteTestNode(4, {createRewriteTestNode(3, {createRewriteTestNode(2, {createRewriteTestNode(1, {})})})});

    QueryPlan query_plan = createQueryPlan(plan);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan, context);

    check_continuous_nodes(query_plan.getPlanNode(), 1, "test 4 nodes sorting");
}

TEST(OptimizerIterativeRewriterTest, SortRule)
{
    IterativeRewriter rewriter{{std::make_shared<SortRule>()}, "test"};

    PlanNodePtr plan1 = createRewriteTestNode(
        8,
        {createRewriteTestNode(
            7,
            {createRewriteTestNode(
                6,
                {createRewriteTestNode(
                    5,
                    {createRewriteTestNode(
                        4, {createRewriteTestNode(3, {createRewriteTestNode(2, {createRewriteTestNode(1, {})})})})})})})});

    QueryPlan query_plan1 = createQueryPlan(plan1);
    auto context = Context::createCopy(getContext().context);
    rewriter.rewrite(query_plan1, context);

    check_continuous_nodes(query_plan1.getPlanNode(), 1, "test 8 nodes sorting case 1");

    PlanNodePtr plan2 = createRewriteTestNode(
        4,
        {createRewriteTestNode(
            8,
            {createRewriteTestNode(
                5,
                {createRewriteTestNode(
                    2,
                    {createRewriteTestNode(
                        6, {createRewriteTestNode(7, {createRewriteTestNode(1, {createRewriteTestNode(3, {})})})})})})})});

    QueryPlan query_plan2 = createQueryPlan(plan2);
    rewriter.rewrite(query_plan2, context);

    check_continuous_nodes(query_plan2.getPlanNode(), 1, "test 8 nodes sorting case 2");
}
