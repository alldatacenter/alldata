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

#include <queue>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Rewriter/SimplifyCrossJoin.h>
#include <Optimizer/SymbolUtils.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/PlanPattern.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
void SimplifyCrossJoin::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    if (!context->getSettingsRef().eliminate_cross_joins)
        return;
    if (!PlanPattern::hasCrossJoin(plan))
        return;
    SimplifyCrossJoinVisitor visitor{context, plan.getCTEInfo()};
    Void v;
    auto result = VisitorUtil::accept(plan.getPlanNode(), visitor, v);
    plan.update(result);
}

PlanNodePtr SimplifyCrossJoinVisitor::visitJoinNode(JoinNode & node, Void & v)
{
    auto join_ptr = node.shared_from_this();
    JoinGraph join_graph = JoinGraph::build(join_ptr, context);
    if (join_graph.size() < 3 || !join_graph.isContainsCrossJoin())
    {
        join_ptr = visitPlanNode(*join_ptr, v);
        return join_ptr;
    }

    std::vector<UInt32> join_order = getJoinOrder(join_graph);
    if (isOriginalOrder(join_order))
    {
        join_ptr = visitPlanNode(*join_ptr, v);
        return join_ptr;
    }

    std::vector<String> output_symbols;
    for (const auto & column : node.getStep()->getOutputStream().header)
    {
        output_symbols.emplace_back(column.name);
    }

    PlanNodePtr replacement = buildJoinTree(output_symbols, join_graph, join_order);
    replacement = visitPlanNode(*replacement, v);
    return replacement;
}

bool SimplifyCrossJoinVisitor::isOriginalOrder(std::vector<UInt32> & join_order)
{
    for (size_t i = 0; i < join_order.size(); i++)
    {
        if (join_order[i] != i)
        {
            return false;
        }
    }
    return true;
}

std::vector<UInt32> SimplifyCrossJoinVisitor::getJoinOrder(JoinGraph & graph)
{
    std::unordered_map<PlanNodeId, UInt32> priorities;
    for (size_t i = 0; i < graph.size(); i++)
    {
        priorities[graph.getNode(i)->getId()] = i;
    }
    ComparePlanNode compare_plan_node(priorities);
    std::priority_queue<PlanNodePtr, std::vector<PlanNodePtr>, ComparePlanNode> nodes_to_visit(compare_plan_node);

    std::set<PlanNodePtr> visited;
    std::vector<PlanNodePtr> join_order;
    nodes_to_visit.push(graph.getNode(0));
    while (!nodes_to_visit.empty())
    {
        PlanNodePtr node = nodes_to_visit.top();
        nodes_to_visit.pop();
        if (!visited.contains(node))
        {
            visited.emplace(node);
            join_order.emplace_back(node);
            for (const Edge & edge : graph.getEdges(node))
            {
                nodes_to_visit.push(edge.getTargetNode());
            }
        }
        if (nodes_to_visit.empty() && visited.size() < graph.size())
        {
            // disconnected graph, find new starting point
            for (auto & graph_node : graph.getNodes())
            {
                if (!visited.contains(graph_node))
                {
                    nodes_to_visit.push(graph_node);
                }
            }
        }
    }

    Utils::checkState(visited.size() == graph.size());

    std::vector<UInt32> id;
    for (auto & order : join_order)
    {
        id.emplace_back(priorities[order->getId()]);
    }
    return id;
}

PlanNodePtr
SimplifyCrossJoinVisitor::buildJoinTree(std::vector<String> & expected_output_symbols, JoinGraph & graph, std::vector<UInt32> & join_order)
{
    Utils::checkArgument(join_order.size() >= 2);

    PlanNodePtr result = graph.getNode(join_order[0]);
    std::set<PlanNodeId> already_joined_nodes;
    already_joined_nodes.emplace(result->getId());

    for (size_t i = 1; i < join_order.size(); i++)
    {
        PlanNodePtr right_node = graph.getNode(join_order[i]);
        already_joined_nodes.emplace(right_node->getId());

        Names left_keys;
        Names right_keys;
        for (auto & edge : graph.getEdges(right_node))
        {
            PlanNodePtr target_node = edge.getTargetNode();
            if (already_joined_nodes.contains(target_node->getId()))
            {
                left_keys.emplace_back(edge.getTargetSymbol());
                right_keys.emplace_back(edge.getSourceSymbol());
            }
        }

        const DataStream & left_data_stream = result->getStep()->getOutputStream();
        const DataStream & right_data_stream = right_node->getStep()->getOutputStream();
        DataStreams streams = {left_data_stream, right_data_stream};

        auto left_header = left_data_stream.header;
        auto right_header = right_data_stream.header;
        NamesAndTypes output;
        for (const auto & item : left_header)
        {
            output.emplace_back(NameAndTypePair{item.name, item.type});
        }
        for (const auto & item : right_header)
        {
            output.emplace_back(NameAndTypePair{item.name, item.type});
        }

        auto new_join_step = std::make_shared<JoinStep>(
            streams,
            DataStream{.header = output},
            ASTTableJoin::Kind::Inner,
            ASTTableJoin::Strictness::All,
            left_keys,
            right_keys);
        result = std::make_shared<JoinNode>(context->nextNodeId(), std::move(new_join_step), PlanNodes{result, right_node});
    }

    auto filters = graph.getFilters();
    ASTPtr predicate = PredicateUtils::combineConjuncts(filters);
    if (!PredicateUtils::isTruePredicate(predicate))
    {
        auto filter_step = std::make_shared<FilterStep>(result->getStep()->getOutputStream(), predicate);
        result = std::make_shared<FilterNode>(context->nextNodeId(), std::move(filter_step), PlanNodes{result});
    }

    // If needed, introduce a projection to constrain the outputs to what was originally expected
    // Some nodes are sensitive to what's produced (e.g., DistinctLimit node)
    std::vector<String> restricted_outputs;
    Assignments assignments;
    NameToType name_to_type;
    for (const auto & column : result->getStep()->getOutputStream().header)
    {
        if (SymbolUtils::contains(expected_output_symbols, column.name))
        {
            restricted_outputs.emplace_back(column.name);
            assignments.emplace_back(Assignment{column.name, std::make_shared<ASTIdentifier>(column.name)});
            name_to_type[column.name] = column.type;
        }
    }

    if (restricted_outputs.size() == result->getStep()->getOutputStream().header.columns())
    {
        return result;
    }

    auto restricted_outputs_step = std::make_shared<ProjectionStep>(result->getStep()->getOutputStream(), assignments, name_to_type);
    auto restricted_outputs_node
        = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(restricted_outputs_step), PlanNodes{result});

    return restricted_outputs_node;
}

}
