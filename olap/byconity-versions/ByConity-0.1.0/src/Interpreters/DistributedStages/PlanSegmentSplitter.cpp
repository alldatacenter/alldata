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

#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/PlanSegmentSplitter.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/PlanSegmentSourceStep.h>
#include <QueryPlan/RemoteExchangeSourceStep.h>
#include <QueryPlan/TableScanStep.h>
#include <QueryPlan/ValuesStep.h>
#include <QueryPlan/ReadNothingStep.h>
#include <Storages/StorageMemory.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB
{

void PlanSegmentSplitter::split(QueryPlan & query_plan, PlanSegmentContext & plan_segment_context)
{
    PlanSegmentVisitor visitor{plan_segment_context, query_plan.getCTENodes()};
    visitor.createPlanSegment(query_plan.getRoot());

    std::unordered_map<size_t, PlanSegmentTree::Node *> plan_mapping;

    for (auto & node : plan_segment_context.plan_segment_tree->getNodes())
    {
        plan_mapping[node.plan_segment->getPlanSegmentId()] = &node;
        if (node.plan_segment->getPlanSegmentOutput()->getPlanSegmentType() == PlanSegmentType::OUTPUT)
            plan_segment_context.plan_segment_tree->setRoot(&node);
    }

    for (auto & node : plan_segment_context.plan_segment_tree->getNodes())
    {
        auto inputs = node.plan_segment->getPlanSegmentInputs();

        for (auto & input : inputs)
        {
            /***
             * SOURCE input has no plan semgnet id and it shouldn't include any child.
             */
            if (input->getPlanSegmentType() != PlanSegmentType::SOURCE && input->getPlanSegmentType() != PlanSegmentType::UNKNOWN)
            {
                auto child_node = plan_mapping[input->getPlanSegmentId()];
                node.children.push_back(child_node);
                child_node->plan_segment->getPlanSegmentOutput()->setShufflekeys(input->getShufflekeys());
                child_node->plan_segment->getPlanSegmentOutput()->setPlanSegmentId(node.plan_segment->getPlanSegmentId());
                child_node->plan_segment->getPlanSegmentOutput()->setExchangeMode(input->getExchangeMode());
                child_node->plan_segment->getPlanSegmentOutput()->setParallelSize(node.plan_segment->getParallelSize());
                /**
                 * If a output is a gather node, its parallel size is always 1 since we should gather all data.
                 */
                if (child_node->plan_segment->getPlanSegmentOutput()->getExchangeMode() == ExchangeMode::GATHER)
                {
                    node.plan_segment->setParallelSize(1);
                }
            }
        }
    }
}

PlanSegmentResult PlanSegmentVisitor::visitNode(QueryPlan::Node * node, PlanSegmentVisitorContext & split_context)
{
    for (size_t i = 0; i < node->children.size(); ++i)
    {
        auto result_node = visitChild(node->children[i], split_context);
        if (result_node)
            node->children[i] = result_node;
    }

    return nullptr;
}

PlanSegmentResult PlanSegmentVisitor::visitChild(QueryPlan::Node * node, PlanSegmentVisitorContext & split_context)
{
    return VisitorUtil::accept(node, *this, split_context);
}

PlanSegmentResult PlanSegmentVisitor::visitExchangeNode(QueryPlan::Node * node, PlanSegmentVisitorContext & split_context)
{
    ExchangeStep * step = dynamic_cast<ExchangeStep *>(node->step.get());

    PlanSegmentInputs inputs;
    for (auto & child : node->children)
    {
        auto plan_segment = createPlanSegment(child);

        auto input = std::make_shared<PlanSegmentInput>(step->getHeader(), PlanSegmentType::EXCHANGE);
        input->setShufflekeys(step->getSchema().getPartitioningColumns());
        input->setPlanSegmentId(plan_segment->getPlanSegmentId());
        input->setExchangeMode(step->getExchangeMode());

        inputs.push_back(input);

        split_context.inputs.emplace_back(input);
        split_context.children.emplace_back(plan_segment);
    }
    QueryPlanStepPtr remote_step = std::make_unique<RemoteExchangeSourceStep>(inputs, step->getOutputStream());
    remote_step->setStepDescription(step->getStepDescription());
    QueryPlan::Node remote_node{
        .step = std::move(remote_step), .children = {}, .id = plan_segment_context.context->getPlanNodeIdAllocator() ? plan_segment_context.context->getPlanNodeIdAllocator()->nextId() : 1};
    plan_segment_context.query_plan.addNode(std::move(remote_node));
    return plan_segment_context.query_plan.getLastNode();
}

PlanSegmentResult PlanSegmentVisitor::visitCTERefNode(QueryPlan::Node * node, PlanSegmentVisitorContext & split_context)
{
    auto * step = dynamic_cast<CTERefStep *>(node->step.get());
    auto * cte_node = cte_nodes.at(step->getId());
    Block header = cte_node->step->getOutputStream().header;

    PlanSegment * plan_segment;
    ExchangeStep * exchange_step = nullptr;
    if (!cte_plan_segments.contains(step->getId()))
    {
        if (cte_node->step->getType() == IQueryPlanStep::Type::Exchange)
        {
            exchange_step = dynamic_cast<ExchangeStep *>(cte_node->step.get());
            plan_segment = createPlanSegment(cte_node->children[0]);
        }
        else
            plan_segment = createPlanSegment(cte_node);
        cte_plan_segments.emplace(step->getId(), std::make_pair(plan_segment, exchange_step));
    }
    else
        std::tie(plan_segment, exchange_step) = cte_plan_segments.at(step->getId());

    std::shared_ptr<PlanSegmentInput> input = std::make_shared<PlanSegmentInput>(header, PlanSegmentType::EXCHANGE);
    input->setPlanSegmentId(plan_segment->getPlanSegmentId());
    if (exchange_step)
    {
        input->setShufflekeys(exchange_step->getSchema().getPartitioningColumns());
        input->setExchangeMode(exchange_step->getExchangeMode());
    }
    else
        input->setExchangeMode(ExchangeMode::LOCAL_NO_NEED_REPARTITION);

    split_context.inputs.emplace_back(input);
    split_context.children.emplace_back(plan_segment);

    QueryPlanStepPtr remote_step = std::make_unique<RemoteExchangeSourceStep>(PlanSegmentInputs{input}, step->getOutputStream());
    remote_step->setStepDescription(step->getStepDescription());
    QueryPlan::Node remote_node{
        .step = std::move(remote_step),
        .children = {},
        .id
        = plan_segment_context.context->getPlanNodeIdAllocator() ? plan_segment_context.context->getPlanNodeIdAllocator()->nextId() : 1};
    plan_segment_context.query_plan.addNode(std::move(remote_node));

    // add projection to rename symbol
    QueryPlan::Node projection_node{
        .step = step->toProjectionStep(),
        .children = {plan_segment_context.query_plan.getLastNode()},
        .id
        = plan_segment_context.context->getPlanNodeIdAllocator() ? plan_segment_context.context->getPlanNodeIdAllocator()->nextId() : 1};
    plan_segment_context.query_plan.addNode(std::move(projection_node));

    return plan_segment_context.query_plan.getLastNode();
}

PlanSegment * PlanSegmentVisitor::createPlanSegment(QueryPlan::Node * node, size_t segment_id, PlanSegmentVisitorContext & split_context)
{
    /**
     * Be careful, after we create a sub_plan, some nodes in the original plan have been deleted and deconstructed.
     * More precisely, nodes that moved to sub_plan are deleted.
     */
    QueryPlan sub_plan = plan_segment_context.query_plan.getSubPlan(node);
    auto [cluster_name, parallel] = findClusterAndParallelSize(sub_plan.getRoot(), split_context);

    auto plan_segment = std::make_unique<PlanSegment>(segment_id, plan_segment_context.query_id, cluster_name);
    plan_segment->setQueryPlan(std::move(sub_plan));
    plan_segment->setContext(plan_segment_context.context);
    plan_segment->setExchangeParallelSize(plan_segment_context.context->getSettingsRef().exchange_parallel_size);

    PlanSegmentType output_type = segment_id == 0 ? PlanSegmentType::OUTPUT : PlanSegmentType::EXCHANGE;

    auto output = std::make_shared<PlanSegmentOutput>(plan_segment->getQueryPlan().getRoot()->step->getOutputStream().header, output_type);
    if (output_type == PlanSegmentType::OUTPUT)
    {
        plan_segment->setParallelSize(1);
        output->setParallelSize(1);
    }
    else
    {
        plan_segment->setParallelSize(parallel);
        if (output->getExchangeMode() == ExchangeMode::GATHER)
            output->setParallelSize(1);
        else
            output->setParallelSize(parallel);
    }
    output->setExchangeParallelSize(plan_segment_context.context->getSettingsRef().exchange_parallel_size);
    plan_segment->setPlanSegmentOutput(output);

    auto inputs = findInputs(plan_segment->getQueryPlan().getRoot());
    if (inputs.empty())
        inputs.push_back(std::make_shared<PlanSegmentInput>(Block(), PlanSegmentType::UNKNOWN));
    for (auto & input : inputs)
        input->setExchangeParallelSize(plan_segment_context.context->getSettingsRef().exchange_parallel_size);

    if (inputs[0]->getExchangeMode() == ExchangeMode::GATHER)
        plan_segment->setParallelSize(1);

    plan_segment->appendPlanSegmentInputs(inputs);

    plan_segment->setPlanSegmentToQueryPlan(plan_segment->getQueryPlan().getRoot());

    PlanSegmentTree::Node plan_segment_node{.plan_segment = std::move(plan_segment)};
    plan_segment_context.plan_segment_tree->addNode(std::move(plan_segment_node));
    return plan_segment_context.plan_segment_tree->getLastNode()->getPlanSegment();
}

PlanSegment * PlanSegmentVisitor::createPlanSegment(QueryPlan::Node * node)
{
    size_t segment_id = plan_segment_context.getSegmentId();
    PlanSegmentVisitorContext split_context{};
    auto result_node = VisitorUtil::accept(node, *this, split_context);
    if (!result_node)
        result_node = node;

    return createPlanSegment(result_node, segment_id, split_context);
}

PlanSegmentInputs PlanSegmentVisitor::findInputs(QueryPlan::Node * node)
{
    if (!node)
        return {};

    if (auto * join_step = dynamic_cast<JoinStep *>(node->step.get()))
    {
        PlanSegmentInputs inputs;
        for (auto & child : node->children)
        {
            //            if (child->step->getType() == IQueryPlanStep::Type::RemoteExchangeSource)
            //            {
            auto child_input = findInputs(child);
//            if (child_input.size() != 1)
//                throw Exception("Join step should contain one input in each child", ErrorCodes::LOGICAL_ERROR);
            inputs.insert(inputs.end(), child_input.begin(), child_input.end());
            //            }
        }
        return inputs;
    }
    else if (auto * remote_step = dynamic_cast<RemoteExchangeSourceStep *>(node->step.get()))
    {
        return remote_step->getInput();
    }
    else if (auto * source_step = dynamic_cast<PlanSegmentSourceStep *>(node->step.get()))
    {
        auto input = std::make_shared<PlanSegmentInput>(source_step->getOutputStream().header, PlanSegmentType::SOURCE);
        input->setStorageID(source_step->getStorageID());
        return {input};
    }
    else if (auto * table_scan_step = dynamic_cast<TableScanStep *>(node->step.get()))
    {
        auto input = std::make_shared<PlanSegmentInput>(table_scan_step->getOutputStream().header, PlanSegmentType::SOURCE);
        input->setStorageID(table_scan_step->getStorageID());
        return {input};
    }
    else if (auto * read_nothing = dynamic_cast<ReadNothingStep *>(node->step.get()))
    {
        auto input = std::make_shared<PlanSegmentInput>(read_nothing->getOutputStream().header, PlanSegmentType::SOURCE);
        return {input};
    }
    else if (auto * values = dynamic_cast<ValuesStep *>(node->step.get()))
    {
        auto input = std::make_shared<PlanSegmentInput>(values->getOutputStream().header, PlanSegmentType::SOURCE);
        return {input};
    }
    else
    {
        PlanSegmentInputs inputs;
        for (auto & child : node->children)
        {
            auto sub_input = findInputs(child);
            inputs.insert(inputs.end(), sub_input.begin(), sub_input.end());
        }
        return inputs;
    }
}

std::pair<String, size_t> PlanSegmentVisitor::findClusterAndParallelSize(QueryPlan::Node * node, PlanSegmentVisitorContext & split_context)
{
    // if (split_context.coordinator)
    //     return {"", 1}; // dispatch to coordinator if server is empty
    bool input_has_table = false;
    for (auto & input : split_context.inputs)
    {
        if (input->getPlanSegmentType() == PlanSegmentType::SOURCE)
        {
            input_has_table = true;
            break;
        }
    }

    auto partitioning = SourceNodeFinder::find(node, *plan_segment_context.context);
    switch (partitioning)
    {
        case Partitioning::Handle::COORDINATOR:
            return {"", 1}; // dispatch to coordinator if server is empty
        case Partitioning::Handle::SINGLE:
            return {plan_segment_context.cluster_name, 1};
        case Partitioning::Handle::FIXED_PASSTHROUGH:
            for (auto & input : split_context.inputs)
            {
                if (input->getExchangeMode() == ExchangeMode::LOCAL_NO_NEED_REPARTITION)
                {
                    size_t input_segment_id = input->getPlanSegmentId();
                    for (auto & child_segment : split_context.children)
                        if (input_segment_id == child_segment->getPlanSegmentId())
                            return {plan_segment_context.cluster_name, child_segment->getParallelSize()};
                }
            }
            break;
        case Partitioning::Handle::FIXED_HASH:
            /// if all input are not table type, parallel size should respect distributed_max_parallel_size setting
            if (!input_has_table && !split_context.inputs.empty())
            {
                size_t max_parallel_size = plan_segment_context.context->getSettingsRef().distributed_max_parallel_size;
                if (max_parallel_size > 0)
                    return {plan_segment_context.cluster_name, std::min(max_parallel_size, plan_segment_context.shard_number)};
            }
            return {plan_segment_context.cluster_name, plan_segment_context.shard_number};
        default:
            break;
    }
    throw Exception("Unknown partition for PlanSegmentSplitter", ErrorCodes::LOGICAL_ERROR);
}

Partitioning::Handle SourceNodeFinder::find(QueryPlan::Node * node, const Context & context)
{
    SourceNodeFinder visitor;
    auto result = VisitorUtil::accept(node, visitor, context);
    return result.value_or(Partitioning::Handle::FIXED_HASH);
}

std::optional<Partitioning::Handle> SourceNodeFinder::visitNode(QueryPlan::Node * node, const Context & context)
{
    for (const auto & child : node->children)
    {
        if (auto result = VisitorUtil::accept(child, *this, context))
            return result;
    }
    return {};
}

std::optional<Partitioning::Handle> SourceNodeFinder::visitValuesNode(QueryPlan::Node *, const Context &)
{
    return Partitioning::Handle::SINGLE;
}

std::optional<Partitioning::Handle> SourceNodeFinder::visitReadNothingNode(QueryPlan::Node *, const Context &)
{
    return Partitioning::Handle::SINGLE;
}

std::optional<Partitioning::Handle> SourceNodeFinder::visitTableScanNode(QueryPlan::Node * node, const Context & context)
{
    auto * source_step = dynamic_cast<TableScanStep *>(node->step.get());
    // check is bucket table instead of cnch table?
    if (auto cnch_merge_tree = dynamic_pointer_cast<StorageCnchMergeTree>(source_step->getStorage()))
        return Partitioning::Handle::FIXED_HASH;

    // hack for unittest
    else if (context.getSettingsRef().enable_memory_catalog)
        if (auto memory_tree = dynamic_pointer_cast<StorageMemory>(source_step->getStorage()))
            return Partitioning::Handle::FIXED_HASH;
    // if source node is not cnch table, schedule to coordinator. eg, system tables.
    return Partitioning::Handle::COORDINATOR;
}

std::optional<Partitioning::Handle> SourceNodeFinder::visitRemoteExchangeSourceNode(QueryPlan::Node * node, const Context &)
{
    const auto * source_step = dynamic_cast<RemoteExchangeSourceStep *>(node->step.get());
    for (const auto & input : source_step->getInput())
    {
        switch (input->getExchangeMode())
        {
            case ExchangeMode::GATHER:
                return Partitioning::Handle::SINGLE;
            case ExchangeMode::BROADCAST:
                continue;
            case ExchangeMode::REPARTITION:
                return Partitioning::Handle::FIXED_HASH;
            case ExchangeMode::LOCAL_NO_NEED_REPARTITION:
                return Partitioning::Handle::FIXED_PASSTHROUGH;
            case ExchangeMode::LOCAL_MAY_NEED_REPARTITION:
            case ExchangeMode::UNKNOWN:
                throw Exception("Unknown exchange mode", ErrorCodes::LOGICAL_ERROR);
        }
    }
    return {};
}

}
