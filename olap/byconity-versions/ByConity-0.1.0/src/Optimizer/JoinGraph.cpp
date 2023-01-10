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

#include <Optimizer/JoinGraph.h>

#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/PushProjectionThroughJoin.h>
#include <Optimizer/SymbolUtils.h>
#include <QueryPlan/FilterStep.h>
#include <Optimizer/Utils.h>

namespace DB
{
JoinGraph JoinGraph::build(
    const PlanNodePtr & plan_ptr, ContextMutablePtr & context, bool support_cross_join, bool use_equality, bool ignore_columns_not_join_condition)
{
    JoinGraphContext join_graph_context{.use_equality = use_equality, .context = context};
    JoinGraphVisitor visitor{join_graph_context, support_cross_join, ignore_columns_not_join_condition};
    NameSet required_columns;
    return VisitorUtil::accept(plan_ptr, visitor, required_columns);
}

JoinGraph JoinGraph::withFilter(const ConstASTPtr & expression)
{
    std::vector<ConstASTPtr> filters;
    filters.insert(filters.end(), filter.begin(), filter.end());
    filters.emplace_back(expression);
    return JoinGraph{nodes, edges, filters, root, contains_cross_join, original_node};
}

JoinGraph JoinGraph::withJoinGraph(
    JoinGraph & other,
    std::vector<std::pair<String, String>> & join_clauses,
    JoinGraphContext & context,
    PlanNodeId new_root,
    bool contains_cross_join_)
{
    for (auto & node : other.nodes)
        if (edges.contains(node->getId()))
            throw Exception("Node appeared in two JoinGraphs, id : " + std::to_string(node->getId()), ErrorCodes::LOGICAL_ERROR);

    std::vector<PlanNodePtr> nodes_merged;
    nodes_merged.insert(nodes_merged.end(), nodes.begin(), nodes.end());
    nodes_merged.insert(nodes_merged.end(), other.nodes.begin(), other.nodes.end());

    std::map<PlanNodeId, std::vector<Edge>> edges_merged;
    edges_merged.insert(edges.begin(), edges.end());
    edges_merged.insert(other.edges.begin(), other.edges.end());

    std::vector<ConstASTPtr> filters_merged;
    filters_merged.insert(filters_merged.end(), filter.begin(), filter.end());
    filters_merged.insert(filters_merged.end(), other.filter.begin(), other.filter.end());

    std::unordered_map<PlanNodeId, PlanNodePtr> new_original_node;
    new_original_node.insert(original_node.begin(), original_node.end());
    new_original_node.insert(other.original_node.begin(), other.original_node.end());

    for (std::pair<String, String> & edge : join_clauses)
    {
        auto & left_symbol = edge.first;
        auto & right_symbol = edge.second;
        const auto & left = context.getSymbolSource(left_symbol);
        const auto & right = context.getSymbolSource(right_symbol);

        edges_merged[left->getId()].emplace_back(Edge{right, left_symbol, right_symbol});
        edges_merged[right->getId()].emplace_back(Edge{left, right_symbol, left_symbol});
    }

    if (context.use_equality)
    {
        context.addEquivalentEdges(edges_merged, join_clauses);
        context.setEquivalentSymbols(join_clauses);
    }

    bool is_contains_cross_join = contains_cross_join || contains_cross_join_;
    return JoinGraph{nodes_merged, edges_merged, filters_merged, new_root, is_contains_cross_join, new_original_node};
}

String JoinGraph::toString() // NOLINT
{
    std::stringstream details;
    return details.str();
}

void JoinGraphContext::addEquivalentEdges(
    std::map<PlanNodeId, std::vector<Edge>> & edges, const std::vector<std::pair<String, String>> & join_clauses)
{
    auto equivalent_symbol_sets = union_find.getSets();
    for (const auto & edge : join_clauses)
    {
        const auto & left_symbol = edge.first;
        const auto & right_symbol = edge.second;
        const auto & left = getSymbolSource(left_symbol);
        const auto & right = getSymbolSource(right_symbol);

        for (auto & set : equivalent_symbol_sets)
        {
            if (set.contains(left_symbol))
                for (const auto & symbol : set)
                {
                    if (symbol == left_symbol)
                        continue;
                    // add edge to right node for each node in set
                    PlanNodePtr node = getSymbolSource(symbol);
                    if (node->getId() == right->getId())
                        continue;
                    edges[node->getId()].emplace_back(Edge{right, symbol, right_symbol});
                    edges[right->getId()].emplace_back(Edge{node, right_symbol, symbol});
                }

            if (set.contains(right_symbol))
                for (const auto & symbol : set)
                {
                    if (symbol == right_symbol)
                        continue;
                    // add edge to left node for each node in set
                    PlanNodePtr node = getSymbolSource(symbol);
                    if (node->getId() == left->getId())
                        continue;
                    edges[left->getId()].emplace_back(Edge{node, left_symbol, symbol});
                    edges[node->getId()].emplace_back(Edge{left, symbol, left_symbol});
                }
        }
    }
}

void JoinGraphContext::setEquivalentSymbols(const std::vector<std::pair<String, String>> & join_clauses)
{
    for (const auto & edge : join_clauses)
        union_find.add(edge.first, edge.second);
}

JoinGraph JoinGraphVisitor::visitPlanNode(PlanNodeBase & node, NameSet &)
{
    for (const auto & column : node.getStep()->getOutputStream().header)
        join_graph_context.setSymbolSource(column.name, node.shared_from_this());
    return JoinGraph{PlanNodes{node.shared_from_this()}};
}

JoinGraph JoinGraphVisitor::visitJoinNode(JoinNode & node, NameSet & required_columns)
{
    const auto & step = *node.getStep();
    if (step.supportReorder(true, support_cross_join))
    {
        const Names & left_keys = step.getLeftKeys();
        auto left_required_columns = required_columns;
        left_required_columns.insert(left_keys.begin(), left_keys.end());
        JoinGraph left = VisitorUtil::accept(node.getChildren()[0], *this, left_required_columns);

        const Names & right_keys = step.getRightKeys();
        auto right_required_columns = required_columns;
        right_required_columns.insert(right_keys.begin(), right_keys.end());
        JoinGraph right = VisitorUtil::accept(node.getChildren()[1], *this, right_required_columns);

        std::vector<std::pair<String, String>> join_clauses;
        for (size_t i = 0; i < left_keys.size(); ++i)
        {
            join_clauses.emplace_back(std::pair<String, String>{left_keys[i], right_keys[i]});
        }
        bool contains_cross_join
            = step.getKind() == ASTTableJoin::Kind::Cross || (step.getKind() == ASTTableJoin::Kind::Inner && left_keys.empty());
        JoinGraph graph = left.withJoinGraph(right, join_clauses, join_graph_context, node.getId(), contains_cross_join);
        return graph.withFilter(step.getFilter());
    }
    return visitPlanNode(node, required_columns);
}

JoinGraph JoinGraphVisitor::visitFilterNode(FilterNode & node, NameSet & required_columns)
{
    JoinGraph graph = VisitorUtil::accept(node.getChildren()[0], *this, required_columns);
    if (graph.getNodes().size() == 1)
    {
        graph.setOriginalNode(graph.getNodes()[0]->getId(), node.shared_from_this());
    }
    const auto & step = *node.getStep();
    const auto & predicate = step.getFilter();
    return graph.withFilter(predicate);
}

JoinGraph JoinGraphVisitor::visitProjectionNode(ProjectionNode & node, NameSet & required_columns)
{
    if (ignore_columns_not_join_condition)
    {
        auto step = dynamic_cast<const ProjectionStep *>(node.getStep().get());
        bool contains_required_columns = false;
        for (auto & assigment : step->getAssignments())
        {
            if (Utils::isIdentity(assigment))
                continue;
            if (!required_columns.count(assigment.first))
                continue;
            contains_required_columns = true;
            break;
        }
        if (!contains_required_columns)
            return VisitorUtil::accept(node.getChildren()[0], *this, required_columns);
    }

    JoinGraph graph;
    std::optional<PlanNodePtr> rewritten_node = PushProjectionThroughJoin::pushProjectionThroughJoin(node, join_graph_context.context);
    if (rewritten_node.has_value())
        graph = VisitorUtil::accept(rewritten_node.value(), *this, required_columns);
    else
        graph = visitPlanNode(node, required_columns);

    if (graph.getNodes().size() == 1)
        graph.setOriginalNode(graph.getNodes()[0]->getId(), node.shared_from_this());
    return graph;
}

}
