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

#include <Optimizer/Rule/Rewrite/MergeSetOperationRules.h>
#include <Optimizer/tests/gtest_optimizer_test_utils.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/IntersectStep.h>
#include <Common/tests/gtest_global_context.h>

#include <gtest/gtest.h>

using namespace DB;

void check_children(PlanNodePtr node, size_t children_size)
{
    ASSERT_TRUE(node->getChildren().size() == children_size);
    for (size_t idx = 0; idx < children_size; ++idx)
    {
        ASSERT_TRUE(node->getChildren()[idx]->getStep()->getType() == IQueryPlanStep::Type::TableScan);
        ASSERT_TRUE(dynamic_cast<const MockedTableScanStep *>(node->getChildren()[idx]->getStep().get())->database == "db1");
        ASSERT_TRUE(
            dynamic_cast<const MockedTableScanStep *>(node->getChildren()[idx]->getStep().get())->table == "t" + std::to_string(idx));
    }
}

// test basic, test not first child merge, test nested
TEST(OptimizerMergeSetRuleTest, DISABLED_MergeUnionRule)
{
    IterativeRewriter rewriter{{std::make_shared<MergeUnionRule>()}, "test"};

    // test mergeable union
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createUnionNode(
            {createUnionNode({ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Union);
        check_input_stream(res);
        check_children(res, 3);
    }

    // test unmergeable union
    {
        DataStream ds = createDataStream({"a"});
        auto plan
            = createQueryPlan(createUnionNode({createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res == plan.getPlanNode());
        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Union);
        check_input_stream(res);
        check_children(res, 2);
    }

    // test not first child merge
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createUnionNode(
            {createTableScanNode("db1", "t0", {ds}, {}),
             createUnionNode({ds}, {createTableScanNode("db1", "t1", {ds}, {}), createTableScanNode("db1", "t2", {ds}, {})})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Union);
        check_input_stream(res);
        check_children(res, 3);
    }

    // test nested merge
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createUnionNode(
            {createUnionNode({ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {}),
             createUnionNode(
                 {ds},
                 {createUnionNode({ds}, {createTableScanNode("db1", "t3", {ds}, {}), createTableScanNode("db1", "t4", {ds}, {})}),
                  createTableScanNode("db1", "t5", {ds}, {})})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Union);
        check_input_stream(res);
        check_children(res, 6);
    }
}

// test basic(4 cases), test not first child merge, test nested; check all /distict
TEST(OptimizerMergeSetRuleTest, DISABLED_MergeIntersectRule)
{
    IterativeRewriter rewriter{{std::make_shared<MergeIntersectRule>()}, "test"};

    // test mergeable intersect (distinct -> distinct)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createIntersectNode(
            true,
            {createIntersectNode(true, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test mergeable intersect (distinct -> all)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createIntersectNode(
            true,
            {createIntersectNode(false, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test mergeable intersect (all -> distinct)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createIntersectNode(
            false,
            {createIntersectNode(true, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test mergeable intersect (all -> all)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createIntersectNode(
            false,
            {createIntersectNode(false, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(!dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test unmergeable intersect
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(
            createIntersectNode(true, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res == plan.getPlanNode());
        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 2);
    }

    // test not first child merge
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createIntersectNode(
            true,
            {createTableScanNode("db1", "t0", {ds}, {}),
             createIntersectNode(true, {ds}, {createTableScanNode("db1", "t1", {ds}, {}), createTableScanNode("db1", "t2", {ds}, {})})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test nested merge
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createIntersectNode(
            false,
            {createIntersectNode(false, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {}),
             createIntersectNode(
                 true,
                 {ds},
                 {createIntersectNode(
                      false, {ds}, {createTableScanNode("db1", "t3", {ds}, {}), createTableScanNode("db1", "t4", {ds}, {})}),
                  createTableScanNode("db1", "t5", {ds}, {})})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Intersect);
        ASSERT_TRUE(dynamic_cast<const IntersectStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 6);
    }
}

// test basic(4 cases), test not first child merge, test nested; check all /distict
TEST(OptimizerMergeSetRuleTest, DISABLED_MergeExceptRule)
{
    IterativeRewriter rewriter{{std::make_shared<MergeExceptRule>()}, "test"};

    // test mergeable except (distinct -> distinct)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createExceptNode(
            true,
            {createExceptNode(true, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test mergeable except (distinct -> all)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createExceptNode(
            true,
            {createExceptNode(false, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res == plan.getPlanNode());
        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        ASSERT_TRUE(res->getChildren().size() == 2);
        ASSERT_TRUE(res->getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(res->getChildren()[1]->getStep()->getType() == IQueryPlanStep::Type::TableScan);
    }

    // test mergeable except (all -> distinct)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createExceptNode(
            false,
            {createExceptNode(true, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test mergeable except (all -> all)
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createExceptNode(
            false,
            {createExceptNode(false, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
             createTableScanNode("db1", "t2", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(!dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 3);
    }

    // test unmergeable except
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(
            createExceptNode(true, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res == plan.getPlanNode());
        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 2);
    }

    // test not first child merge
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createExceptNode(
            true,
            {createTableScanNode("db1", "t0", {ds}, {}),
             createExceptNode(true, {ds}, {createTableScanNode("db1", "t1", {ds}, {}), createTableScanNode("db1", "t2", {ds}, {})})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res == plan.getPlanNode());
        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        ASSERT_TRUE(res->getChildren().size() == 2);
        ASSERT_TRUE(res->getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::TableScan);
        ASSERT_TRUE(res->getChildren()[1]->getStep()->getType() == IQueryPlanStep::Type::Except);
    }

    // test nested merge
    {
        DataStream ds = createDataStream({"a"});
        auto plan = createQueryPlan(createExceptNode(
            false,
            {createExceptNode(
                 false,
                 {ds},
                 {createExceptNode(true, {ds}, {createTableScanNode("db1", "t0", {ds}, {}), createTableScanNode("db1", "t1", {ds}, {})}),
                  createTableScanNode("db1", "t2", {ds}, {})}),
             createTableScanNode("db1", "t3", {ds}, {})}));

        auto context = Context::createCopy(getContext().context);
        rewriter.rewrite(plan, context);
        auto res = plan.getPlanNode();

        ASSERT_TRUE(res->getStep()->getType() == IQueryPlanStep::Type::Except);
        ASSERT_TRUE(dynamic_cast<const ExceptStep *>(res->getStep().get())->isDistinct());
        check_input_stream(res);
        check_children(res, 4);
    }
}
