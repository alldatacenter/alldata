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

#include <Interpreters/DistributedStages/ExchangeStepVisitor.h>
#include <Interpreters/DistributedStages/ExchangeMode.h>
#include <Interpreters/Context.h>
#include <Interpreters/IJoin.h>
#include <Interpreters/TableJoin.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/MergingAggregatedStep.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <QueryPlan/JoinStep.h>

namespace DB
{

ExchangeStepResult ExchangeStepVisitor::visitNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context)
{
    for (size_t i = 0; i < node->children.size(); ++i)
    {
        auto result_node = visitChild(node->children[i], exchange_context);
        if (result_node)
            node->children[i] = result_node;
    }

    return nullptr;
}

ExchangeStepResult ExchangeStepVisitor::visitChild(QueryPlan::Node * node, ExchangeStepContext & exchange_context)
{
    return VisitorUtil::accept(node, *this, exchange_context);
}

void ExchangeStepVisitor::addExchange(QueryPlan::Node * node, ExchangeMode mode, const Partitioning & partition, ExchangeStepContext & exchange_context)
{
    auto exchange_step = std::make_unique<ExchangeStep>(node->step->getInputStreams(), mode, partition);
    exchange_step->setStepDescription(exchangeModeToString(mode));
    QueryPlan::Node exchange_node{.step = std::move(exchange_step)};
    exchange_context.query_plan.addNode(std::move(exchange_node));
    auto last_node = exchange_context.query_plan.getLastNode();
    last_node->children.push_back(last_node);
    node->children.swap(last_node->children);
    /**
     * always set gather = false to make repartition works, if no repartition node, gather will be set.
     */
    exchange_context.has_gathered = false;
}

ExchangeStepResult ExchangeStepVisitor::visitMergingAggregatedNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context)
{
    visitNode(node, exchange_context);

    MergingAggregatedStep * step = dynamic_cast<MergingAggregatedStep *>(node->step.get());

    auto params = step->getParams()->params;

    Block result_header = params.getHeader(false);

    Names keys;
    for (auto & index : params.keys)
        keys.push_back(result_header.safeGetByPosition(index).name);

    if (keys.empty())
    {
        addExchange(node, ExchangeMode::GATHER, Partitioning(keys), exchange_context);
        exchange_context.has_gathered = true;
    }
    else
        addExchange(node, ExchangeMode::REPARTITION, Partitioning(keys), exchange_context);


    return nullptr;
}

ExchangeStepResult ExchangeStepVisitor::visitJoinNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context)
{
    visitNode(node, exchange_context);

    JoinStep * step = dynamic_cast<JoinStep *>(node->step.get());
    auto join = step->getJoin();
    auto join_infos = join->getTableJoin();

    auto left_keys = join_infos.keyNamesLeft();
    auto right_keys = join_infos.keyNamesRight();

    auto locality = join_infos.locality();

    if (node->children.size() != 2)
        throw Exception("Join must have two children", ErrorCodes::LOGICAL_ERROR);

    /**
     * Left join is always index 0 when query plan is generated
     */
    auto left_stream = step->getInputStreams()[0];
    auto right_stream = step->getInputStreams()[1];

    auto add_exchange = [&](DataStream & data_stream, size_t index, ExchangeMode mode, const Names & keys)
    {
        auto exchange_step = std::make_unique<ExchangeStep>(DataStreams{data_stream}, mode, Partitioning(keys));
        exchange_step->setStepDescription(exchangeModeToString(mode));
        QueryPlan::Node exchange_node{.step = std::move(exchange_step), .children = {node->children[index]}};

        exchange_context.query_plan.addNode(std::move(exchange_node));
        node->children[index] = exchange_context.query_plan.getLastNode();
    };

    if (locality == ASTTableJoin::Locality::Local)
    {
        add_exchange(left_stream, 0, ExchangeMode::LOCAL_NO_NEED_REPARTITION, Names{});
        add_exchange(right_stream, 1, ExchangeMode::LOCAL_NO_NEED_REPARTITION, Names{});
    }
    else if (locality == ASTTableJoin::Locality::Global)
    {
        add_exchange(left_stream, 0, ExchangeMode::LOCAL_NO_NEED_REPARTITION, Names{});
        add_exchange(right_stream, 1, ExchangeMode::BROADCAST, Names{});
    }
    else
    {
        add_exchange(left_stream, 0, ExchangeMode::REPARTITION, left_keys);
        add_exchange(right_stream, 1, ExchangeMode::REPARTITION, right_keys);
    }

    return nullptr;
}

ExchangeStepResult ExchangeStepVisitor::visitLimitNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context)
{
    visitNode(node, exchange_context);

    if (!exchange_context.has_gathered)
    {
        addExchange(node, ExchangeMode::GATHER, Partitioning(Names{}), exchange_context);
        exchange_context.has_gathered = true;
    }

    return nullptr;
}

ExchangeStepResult ExchangeStepVisitor::visitMergingSortedNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context)
{
    visitNode(node, exchange_context);

    if (!exchange_context.has_gathered)
    {
        addExchange(node, ExchangeMode::GATHER, Partitioning(Names{}), exchange_context);
        exchange_context.has_gathered = true;
    }

    return nullptr;
}

void ExchangeStepVisitor::addGather(QueryPlan & query_plan, ExchangeStepContext & exchange_context)
{
    if (!exchange_context.has_gathered)
    {
        DataStreams data_streams = {query_plan.getCurrentDataStream()};
        auto max_threads = exchange_context.context->getSettingsRef().max_threads;
        auto union_step = std::make_unique<UnionStep>(std::move(data_streams), max_threads);
        query_plan.addStep(std::move(union_step));

        addExchange(query_plan.getRoot(), ExchangeMode::GATHER, Partitioning(Names{}), exchange_context);
        exchange_context.has_gathered = true;
    }
}

void AddExchangeRewriter::rewrite(QueryPlan & query_plan, ExchangeStepContext & exchange_context)
{
    ExchangeStepVisitor visitor{};
    ExchangeStepResult result_node = VisitorUtil::accept(query_plan.getRoot(), visitor, exchange_context);
    if (result_node)
        query_plan.setRoot(result_node);

    visitor.addGather(query_plan, exchange_context);
}


}
