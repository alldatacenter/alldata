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

#pragma once

#include <Optimizer/Rule/Transformation/JoinEnumOnGraph.h>
#include <Parsers/IAST_fwd.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/PlanVisitor.h>

#include <utility>

namespace DB
{
class Edge;
struct JoinGraphContext;

/**
 * JoinGraph represents sequence of Joins, where nodes in the graph
 * are PlanNodes that are being joined and edges are all equality join
 * conditions between pair of nodes.
 */
class JoinGraph
{
public:
    /**
     * Builds JoinGraph containing plan node.
     */
    static JoinGraph build(const PlanNodePtr & plan_ptr, ContextMutablePtr & context, bool support_cross_join = true, bool use_equality = false, bool ignore_columns_not_join_condition = false);

    JoinGraph(
        PlanNodes nodes_,
        std::map<PlanNodeId, std::vector<Edge>> edges_,
        std::vector<ConstASTPtr> filter_,
        PlanNodeId root_,
        bool contains_cross_join_,
        std::unordered_map<PlanNodeId, PlanNodePtr> original_node_)
        : nodes(std::move(nodes_))
        , edges(std::move(edges_))
        , filter(std::move(filter_))
        , root(root_)
        , contains_cross_join(contains_cross_join_)
        , original_node(std::move(original_node_))
    {
    }

    explicit JoinGraph(PlanNodes nodes_ = {}) : JoinGraph(std::move(nodes_), {}, {}, {}, false, {}) { }

    JoinGraph withFilter(const ConstASTPtr & expression);
    JoinGraph withJoinGraph(
        JoinGraph & other,
        std::vector<std::pair<String, String>> & join_clauses,
        JoinGraphContext & context,
        PlanNodeId new_root,
        bool contains_cross_join_);

    const std::vector<ConstASTPtr> & getFilters() const { return filter; }
    PlanNodeId getRootId() const { return root; }
    bool isEmpty() const { return nodes.empty(); }
    size_t size() const { return nodes.size(); }
    PlanNodePtr getNode(int index) { return nodes.at(index); }
    const PlanNodes & getNodes() const { return nodes; }
    std::vector<Edge> & getEdges(const PlanNodePtr & node) { return edges[node->getId()]; }
    const std::map<PlanNodeId, std::vector<Edge>> & getEdges() const { return edges; }
    bool isContainsCrossJoin() const { return contains_cross_join; }
    std::unordered_map<PlanNodeId, PlanNodePtr> & getOriginalNode() { return original_node; }
    void setOriginalNode(PlanNodeId id, const PlanNodePtr & node) { original_node[id] = node; }

    String toString();

private:
    PlanNodes nodes;
    std::map<PlanNodeId, std::vector<Edge>> edges;
    std::vector<ConstASTPtr> filter;
    PlanNodeId root;
    bool contains_cross_join;
    std::unordered_map<PlanNodeId, PlanNodePtr> original_node;
};

class Edge
{
public:
    Edge(PlanNodePtr target_node_, String source_symbol_, String target_symbol_)
        : target_node(std::move(target_node_)), source_symbol(std::move(source_symbol_)), target_symbol(std::move(target_symbol_))
    {
    }
    const PlanNodePtr & getTargetNode() const { return target_node; }
    const String & getSourceSymbol() const { return source_symbol; }
    const String & getTargetSymbol() const { return target_symbol; }

private:
    PlanNodePtr target_node;
    String source_symbol;
    String target_symbol;
};

struct JoinGraphContext
{
    void setSymbolSource(const String & symbol, const PlanNodePtr & node) { symbol_sources[symbol] = node; }
    void addEquivalentEdges(std::map<PlanNodeId, std::vector<Edge>> & edges, const std::vector<std::pair<String, String>> & join_clauses);
    void setEquivalentSymbols(const std::vector<std::pair<String, String>> & join_clauses);
    const PlanNodePtr & getSymbolSource(const String & symbol) const
    {
        if (symbol_sources.contains(symbol))
            return symbol_sources.at(symbol);
        throw Exception("Symbol not exists : " + symbol, ErrorCodes::LOGICAL_ERROR);
    }

    std::unordered_map<String, PlanNodePtr> symbol_sources = {};
    UnionFind<String> union_find = {};
    bool use_equality;
    bool ignore_columns_not_join_condition;
    ContextMutablePtr & context;
};

class JoinGraphVisitor : public PlanNodeVisitor<JoinGraph, NameSet>
{
public:
    explicit JoinGraphVisitor(JoinGraphContext & join_graph_context_, bool support_cross_join_, bool ignore_columns_not_join_condition_)
        : join_graph_context(join_graph_context_)
        , support_cross_join(support_cross_join_)
        , ignore_columns_not_join_condition(ignore_columns_not_join_condition_)
    {
    }
    JoinGraph visitPlanNode(PlanNodeBase &, NameSet &) override;
    JoinGraph visitJoinNode(JoinNode &, NameSet &) override;
    JoinGraph visitFilterNode(FilterNode &, NameSet &) override;
    JoinGraph visitProjectionNode(ProjectionNode &, NameSet &) override;

private:
    JoinGraphContext & join_graph_context;
    const bool support_cross_join;
    const bool ignore_columns_not_join_condition;
};

}
