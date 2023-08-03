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

#include <Optimizer/Property/PropertyEnforcer.h>

#include <Optimizer/Cascades/GroupExpression.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/UnionStep.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int ILLEGAL_ENFORCE;
}

PlanNodePtr PropertyEnforcer::enforceNodePartitioning(
    const PlanNodePtr & node, const Property & required, const Property & property, Context & context)
{
    QueryPlanStepPtr step_ptr = enforceNodePartitioning(node->getStep(), required, property, context);
    if (!step_ptr)
    {
        return node;
    }
    auto exchange = PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(step_ptr), std::vector{node});
    return exchange;
}

PlanNodePtr PropertyEnforcer::enforceStreamPartitioning(
    const PlanNodePtr & node, const Property & required, const Property & property, Context & context)
{
    QueryPlanStepPtr step_ptr = enforceStreamPartitioning(node->getStep(), required, property, context);
    auto exchange = PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(step_ptr), std::vector{node});
    return exchange;
}

GroupExprPtr PropertyEnforcer::enforceNodePartitioning(
    const GroupExprPtr & group_expr, const Property & required, const Property & property, const Context & context)
{
    QueryPlanStepPtr step_ptr = enforceNodePartitioning(group_expr->getStep(), required, property, context);
    if (!step_ptr)
    {
        return nullptr;
    }
    std::vector<GroupId> children = {group_expr->getGroupId()};
    auto result = std::make_shared<GroupExpression>(std::move(step_ptr), children);
    return result;
}

GroupExprPtr PropertyEnforcer::enforceStreamPartitioning(
    const GroupExprPtr & group_expr, const Property & required, const Property & property, const Context & context)
{
    QueryPlanStepPtr step_ptr = enforceStreamPartitioning(group_expr->getStep(), required, property, context);
    std::vector<GroupId> children = {group_expr->getGroupId()};
    auto result = std::make_shared<GroupExpression>(std::move(step_ptr), children);
    return result;
}

QueryPlanStepPtr PropertyEnforcer::enforceNodePartitioning(
    ConstQueryPlanStepPtr step, const Property & required, const Property &, const Context & context)
{
    const auto & output_stream = step->getOutputStream();
    DataStreams streams{output_stream};
    Partitioning partitioning = required.getNodePartitioning();

    // if the stream is ordered, we need keep order when exchange data.
    bool keep_order = context.getSettingsRef().enable_shuffle_with_order;

    if (partitioning.getPartitioningHandle() == Partitioning::Handle::SINGLE)
    {
        QueryPlanStepPtr step_ptr = std::make_unique<ExchangeStep>(streams, ExchangeMode::GATHER, partitioning, keep_order);
        return step_ptr;
    }
    if (partitioning.getPartitioningHandle() == Partitioning::Handle::FIXED_HASH)
    {
        QueryPlanStepPtr step_ptr = std::make_unique<ExchangeStep>(streams, ExchangeMode::REPARTITION, partitioning, keep_order);
        return step_ptr;
    }
    if (partitioning.getPartitioningHandle() == Partitioning::Handle::FIXED_BROADCAST)
    {
        QueryPlanStepPtr step_ptr = std::make_unique<ExchangeStep>(streams, ExchangeMode::BROADCAST, partitioning, keep_order);
        return step_ptr;
    }
    if (partitioning.getPartitioningHandle() == Partitioning::Handle::FIXED_ARBITRARY)
    {
        QueryPlanStepPtr step_ptr
            = std::make_unique<ExchangeStep>(streams, ExchangeMode::LOCAL_NO_NEED_REPARTITION, partitioning, keep_order);
        return step_ptr;
    }
    if (partitioning.getPartitioningHandle() == Partitioning::Handle::ARBITRARY)
    {
        return nullptr;
    }
    throw Exception("Property Enforce error", ErrorCodes::ILLEGAL_ENFORCE);
}

QueryPlanStepPtr
PropertyEnforcer::enforceStreamPartitioning(ConstQueryPlanStepPtr step, const Property & required, const Property &, const Context &)
{
    DataStreams streams;
    const DataStream & input_stream = step->getOutputStream();
    streams.emplace_back(input_stream);

    if (required.getStreamPartitioning().getPartitioningHandle() == Partitioning::Handle::SINGLE)
    {
        QueryPlanStepPtr step_ptr = std::make_unique<UnionStep>(streams, 0, true);
        return step_ptr;
    }
    throw Exception("Property Enforce error", ErrorCodes::ILLEGAL_ENFORCE);
}
}
