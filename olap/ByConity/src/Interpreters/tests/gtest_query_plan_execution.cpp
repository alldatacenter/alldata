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

#include <gtest/gtest.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/ArrayJoinStep.h>
#include <QueryPlan/CreatingSetsStep.h>
#include <QueryPlan/CubeStep.h>
#include <QueryPlan/DistinctStep.h>
#include <QueryPlan/ExpressionStep.h>
#include <QueryPlan/ExtremesStep.h>
#include <QueryPlan/FillingStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/FinishSortingStep.h>
#include <QueryPlan/ISourceStep.h>
#include <QueryPlan/ITransformingStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitByStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/MergingAggregatedStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/OffsetStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <QueryPlan/ReadFromMergeTree.h>
#include <QueryPlan/ReadFromPreparedSource.h>
#include <QueryPlan/ReadNothingStep.h>
#include <QueryPlan/RollupStep.h>
#include <QueryPlan/SettingQuotaAndLimitsStep.h>
#include <QueryPlan/TotalsHavingStep.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/WindowStep.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadBufferFromString.h>
#include <Common/tests/gtest_global_context.h>
#include <Parsers/ParserQuery.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/InterpreterDistributedStages.h>

namespace UnitTest
{

using namespace DB;

ASTPtr parseTestQuery(const String & query)
{
    const char * begin = query.data();
    const char * end = query.data() + query.size();
    ParserQuery parser(end);
    return parseQuery(parser, begin, end, "", 0, 0);
}

void checkPlan(PlanSegment * lhs, PlanSegment * rhs)
{
    auto lhs_str = lhs->toString();
    auto rhs_str = rhs->toString();

    std::cout<<" <<< lhs:\n" << lhs_str << std::endl;
    std::cout<<" <<< rhs:\n" << rhs_str << std::endl;

    EXPECT_EQ(lhs_str, rhs_str);
}

void executeTestQuery(const ASTPtr & query)
{
    auto context = getContext().context;
    context->setQueryContext(context);


    auto interpreter = std::make_shared<InterpreterDistributedStages>(query, context);

    PlanSegmentTree * plan_segment_tree = interpreter->getPlanSegmentTree();

    /**
     * serialize to buffer
     */
    WriteBufferFromOwnString write_buffer;
    writeBinary(plan_segment_tree->getNodes().size(), write_buffer);
    for (auto & node : plan_segment_tree->getNodes())
        node.plan_segment->serialize(write_buffer);

    ReadBufferFromString read_buffer(write_buffer.str());
    size_t plan_size;
    readBinary(plan_size, read_buffer);
    std::vector<std::shared_ptr<PlanSegment>> plansegments;

    for (size_t i = 0; i < plan_size; ++i)
    {
        auto plan = std::make_shared<PlanSegment>(context);
        plan->deserialize(read_buffer);
        plansegments.push_back(plan);
    }

    /**
     * check results
     */
    std::vector<PlanSegment *> old_plans;
    for (auto & node : plan_segment_tree->getNodes())
        old_plans.push_back(node.plan_segment.get());

    for (size_t i = 0; i < plan_size; ++i)
    {
        auto lhs = old_plans[i];
        auto rhs = plansegments[i].get();
        checkPlan(lhs, rhs);
    }
}

TEST(TestQueryPlan, TestQueryPlanExecution)
{
    String query = "select sum(number) from (select 1 as number) settings enable_distributed_stages = 1";
    auto query_ast = parseTestQuery(query);
    //executeTestQuery(query_ast);
}

}
