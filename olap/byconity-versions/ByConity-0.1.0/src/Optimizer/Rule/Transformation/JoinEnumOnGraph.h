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
#include <Functions/FunctionsHashing.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Rule/Rule.h>
#include <Optimizer/Equivalences.h>
#include <QueryPlan/JoinStep.h>
#include <boost/dynamic_bitset.hpp>

#include <unordered_set>
#include <utility>

namespace DB
{
using GroupId = UInt32;

class JoinEnumOnGraph : public Rule
{
public:
    JoinEnumOnGraph(bool support_filter_) : support_filter(support_filter_) { }
    RuleType getType() const override { return RuleType::JOIN_ENUM_ON_GRAPH; }
    String getName() const override { return "JOIN_ENUM_ON_GRAPH"; }

    PatternPtr getPattern() const override;

    const std::vector<RuleType> & blockRules() const override;

private:
    TransformResult transformImpl(PlanNodePtr node, const Captures & captures, RuleContext & context) override;
    bool support_filter;
};

class JoinSet
{
public:
    JoinSet(const JoinSet & left_, const JoinSet & right_, const Names & left_join_keys, const Names & right_join_keys, ASTPtr filter_)
        : union_find(left_.union_find, right_.union_find), filter(std::move(filter_))
    {
        groups.insert(groups.end(), left_.groups.begin(), left_.groups.end());
        groups.insert(groups.end(), right_.groups.begin(), right_.groups.end());
        std::sort(groups.begin(), groups.end());

        for (size_t index = 0; index < left_join_keys.size(); ++index)
        {
            union_find.add(left_join_keys[index], right_join_keys[index]);
        }
    }

    explicit JoinSet(GroupId group_id_) { groups.emplace_back(group_id_); }

    const std::vector<GroupId> & getGroups() const { return groups; }
    bool operator==(const JoinSet & rhs) const { return groups == rhs.groups; }
    bool operator!=(const JoinSet & rhs) const { return !(rhs == *this); }

    UnionFind<String> & getUnionFind() { return union_find; }

    const ASTPtr & getFilter() const { return filter; }

private:
    std::vector<GroupId> groups;
    UnionFind<String> union_find;
    ASTPtr filter;
};

struct JoinSetHash
{
    std::size_t operator()(JoinSet const & join_set) const
    {
        size_t hash = IntHash64Impl::apply(join_set.getGroups().size());
        for (auto child_group : join_set.getGroups())
        {
            hash = MurmurHash3Impl64::combineHashes(hash, child_group);
        }
        return hash;
    }
};

using JoinSets = std::unordered_set<JoinSet, JoinSetHash>;

using BitSet = boost::dynamic_bitset<>;
class Graph
{
public:
    struct Edge
    {
        Edge(String source_symbol_, String target_symbol_) : source_symbol(std::move(source_symbol_)), target_symbol(std::move(target_symbol_))
        {
        }
        String source_symbol;
        String target_symbol;
    };

    struct Partition
    {
        std::unordered_set<GroupId> left;
        std::unordered_set<GroupId> right;
        Partition(const BitSet & left_bit, const BitSet & right_bit)
        {
            auto pos = left_bit.find_first();
            while (pos != BitSet::npos)
            {
                left.insert(pos);
                pos = left_bit.find_next(pos);
            }
            pos = right_bit.find_first();
            while (pos != BitSet::npos)
            {
                right.insert(pos);
                pos = right_bit.find_next(pos);
            }
        }
    };

    static Graph fromJoinSet(RuleContext & context, JoinSet & join_set);

    std::vector<Partition> cutPartitions() const;

    const std::vector<GroupId> & getNodes() const { return nodes; }
    const std::unordered_map<GroupId, std::unordered_map<GroupId, std::vector<Edge>>> & getEdges() const { return edges; }
    std::vector<std::pair<String, String>> bridges(const std::vector<GroupId> & left, const std::vector<GroupId> & right) const
    {
        std::vector<std::pair<String, String>> result;
        std::unordered_set<GroupId> right_set;
        right_set.insert(right.begin(), right.end());
        for (auto group_id : left)
        {
            for (auto & item : edges.at(group_id))
            {
                if (right_set.contains(item.first))
                {
                    for (const auto & edge : item.second)
                    {
                        result.emplace_back(edge.source_symbol, edge.target_symbol);
                    }
                }
            }
        }
        return result;
    }

    std::vector<GroupId> getDFSOrder(const std::unordered_set<GroupId> & sub_nodes) const
    {
        std::vector<GroupId> res;
        std::unordered_set<GroupId> visited;

        // first visit min node
        auto min = *std::min_element(sub_nodes.begin(), sub_nodes.end());
        dfs(res, min, sub_nodes, visited);

        return res;
    }

private:
    void
    dfs(std::vector<GroupId> & path,
        GroupId node,
        const std::unordered_set<GroupId> & sub_nodes,
        std::unordered_set<GroupId> & visited) const
    {
        path.emplace_back(node);
        visited.insert(node);

        std::vector<GroupId> need_search;
        for (auto & item : edges.at(node))
        {
            if (sub_nodes.contains(item.first) && !visited.contains(item.first))
            {
                need_search.emplace_back(item.first);
            }
        }
        std::sort(need_search.begin(), need_search.end());
        for (auto group_id : need_search)
        {
            if (!visited.contains(group_id))
            {
                dfs(path, group_id, sub_nodes, visited);
            }
        }
    }

    std::vector<GroupId> nodes;
    std::unordered_map<GroupId, std::unordered_map<GroupId, std::vector<Edge>>> edges;
    ASTPtr filter;
};

class MinCutBranchAlg
{
public:
    explicit MinCutBranchAlg(const Graph & graph_) : graph(graph_) { }

    void partition();

    const std::vector<Graph::Partition> & getPartitions() const { return partitions; }

private:
    BitSet minCutBranch(const BitSet & s, const BitSet & c, const BitSet & x, const BitSet & l);

    BitSet neighbor(const BitSet & nodes);
    BitSet reachable(const BitSet & c, const BitSet & l);

    const Graph & graph;
    std::vector<Graph::Partition> partitions;
};

}
