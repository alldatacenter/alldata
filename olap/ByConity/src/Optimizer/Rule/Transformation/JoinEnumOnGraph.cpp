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

#include <Optimizer/Rule/Transformation/JoinEnumOnGraph.h>

#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/EqualityInference.h>
#include <Optimizer/Rule/Pattern.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/AnyStep.h>

#include <boost/range/adaptor/map.hpp>
#include <boost/range/algorithm/copy.hpp>

namespace DB
{
PatternPtr JoinEnumOnGraph::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([&](const JoinStep & s) { return s.supportReorder(support_filter); })
        ->with({Patterns::tree(), Patterns::tree()});
}

static std::pair<Names, Names> createJoinCondition(UnionFind<String> & union_find, const std::vector<std::pair<String, String>> & edges)
{
    // extract equivalent map{representative symbol, all the symbols in the same equivalent set}
    std::unordered_map<String, std::vector<String>> left_set_to_symbols;
    std::unordered_map<String, std::vector<String>> right_set_to_symbols;


    // join key to equivalent symbols set
    for (const auto & edge : edges)
    {
        left_set_to_symbols[union_find.find(edge.first)].emplace_back(edge.first);
        right_set_to_symbols[union_find.find(edge.second)].emplace_back(edge.second);
    }

    auto extract_sorted_keys = [&](auto map) {
        std::vector<String> keys;
        boost::copy(map | boost::adaptors::map_keys, std::back_inserter(keys));
        std::sort(keys.begin(), keys.end());
        return keys;
    };
    // extract sorted representative symbol, sort the symbols because we need the rule is stable.
    auto left_sets = extract_sorted_keys(left_set_to_symbols);
    auto right_sets = extract_sorted_keys(right_set_to_symbols);

    // common equivalent symbols
    std::vector<String> intersect_set;
    std::set_intersection(
        left_sets.begin(), left_sets.end(), right_sets.begin(), right_sets.end(), std::inserter(intersect_set, intersect_set.begin()));

    // create join key using the common equivalent symbols, each equivalent set create one join criteria.
    std::vector<std::pair<String, String>> criteria;
    criteria.reserve(intersect_set.size());
    for (const auto & set : intersect_set)
    {
        criteria.emplace_back(
            *std::min_element(left_set_to_symbols[set].begin(), left_set_to_symbols[set].end()),
            *std::min_element(right_set_to_symbols[set].begin(), right_set_to_symbols[set].end()));
    }
    std::sort(criteria.begin(), criteria.end(), [](auto & a, auto & b) { return a.first < b.first; });

    Names left_join_keys;
    Names right_join_keys;
    for (const auto & item : criteria)
    {
        left_join_keys.emplace_back(item.first);
        right_join_keys.emplace_back(item.second);
    }
    return {left_join_keys, right_join_keys};
}

static PlanNodePtr createJoinNode(
    OptContextPtr & context,
    GroupId left_id,
    GroupId right_id,
    const std::pair<Names, Names> & join_keys,
    const std::set<String> & require_names,
    ASTPtr filter)
{
    if (!filter)
    {
        filter = PredicateConst::TRUE_VALUE;
    }
    auto left = context->getOptimizerContext().getMemo().getGroupById(left_id)->createLeafNode(context->getOptimizerContext().getContext());
    auto right
        = context->getOptimizerContext().getMemo().getGroupById(right_id)->createLeafNode(context->getOptimizerContext().getContext());


    NamesAndTypes output;
    NameToType name_to_type;
    for (const auto & item : left->getStep()->getOutputStream().header)
    {
        name_to_type[item.name] = item.type;
    }
    for (const auto & item : right->getStep()->getOutputStream().header)
    {
        name_to_type[item.name] = item.type;
    }
    for (const auto & name : require_names)
    {
        if (name_to_type.contains(name))
        {
            output.emplace_back(name, name_to_type[name]);
        }
    }

    auto join_step = std::make_shared<JoinStep>(
        DataStreams{left->getStep()->getOutputStream(), right->getStep()->getOutputStream()},
        DataStream{output},
        ASTTableJoin::Kind::Inner,
        ASTTableJoin::Strictness::All,
        join_keys.first,
        join_keys.second,
        filter,
        false,
        std::nullopt,
        ASOF::Inequality::GreaterOrEquals,
        DistributionType::UNKNOWN);

    return PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(join_step), {left, right});
}

static std::set<String> createPossibleSymbols(const std::vector<GroupId> & groups, OptContextPtr & context)
{
    std::set<String> result;
    for (auto id : groups)
    {
        for (const auto & col : context->getMemo().getGroupById(id)->getStep()->getOutputStream().header)
        {
            result.insert(col.name);
        }
    }
    return result;
}


static ASTPtr getJoinFilter(const ASTPtr & all_filter, std::set<String> & left_symbols, std::set<String> & right_symbols, ContextMutablePtr & context)
{
    auto all_filter_inference = EqualityInference::newInstance(all_filter, context);
    auto non_inferrable_conjuncts = EqualityInference::nonInferrableConjuncts(all_filter, context);

    std::set<String> union_scope;
    union_scope.insert(left_symbols.begin(), left_symbols.end());
    union_scope.insert(right_symbols.begin(), right_symbols.end());

    std::vector<ConstASTPtr> join_predicates_builder;
    for (auto & tmp_conjunct : non_inferrable_conjuncts)
    {
        auto conjunct = all_filter_inference.rewrite(tmp_conjunct, union_scope);
        if (conjunct && all_filter_inference.rewrite(tmp_conjunct, left_symbols) == nullptr
            && all_filter_inference.rewrite(tmp_conjunct, right_symbols) == nullptr)
        {
            join_predicates_builder.emplace_back(conjunct);
        }
    }

    auto join_equalities = all_filter_inference.partitionedBy(union_scope).getScopeEqualities();
    EqualityInference join_inference = EqualityInference::newInstance(join_equalities, context);
    auto straddling = join_inference.partitionedBy(left_symbols).getScopeStraddlingEqualities();
    join_predicates_builder.insert(join_predicates_builder.end(), straddling.begin(), straddling.end());

    return PredicateUtils::combineConjuncts(join_predicates_builder);
}


static GroupId buildJoinNode(
    OptContextPtr & context,
    std::vector<GroupId> groups,
    UnionFind<String> & union_find,
    Graph & graph,
    const std::set<String> & require_names,
    const ASTPtr & all_filter)
{
    // if only one group, return leaf node.
    if (groups.size() == 1)
    {
        return groups[0];
    }

    auto left_groups = std::vector<GroupId>(groups.begin(), groups.begin() + 1);
    auto right_groups = std::vector<GroupId>(groups.begin() + 1, groups.end());

    auto join_keys = createJoinCondition(union_find, graph.bridges(left_groups, right_groups));
    auto left_possible_output = createPossibleSymbols(left_groups, context);
    auto right_possible_output = createPossibleSymbols(right_groups, context);
    // create join filter
    ContextMutablePtr ptr = context->getOptimizerContext().getContext();
    auto filter = getJoinFilter(all_filter, left_possible_output, right_possible_output, ptr);
    auto filter_symbols = SymbolsExtractor::extract(filter);

    // build left node, using first group
    std::set<String> require_left = require_names;
    require_left.insert(join_keys.first.begin(), join_keys.first.end());
    for (const auto & symbol : filter_symbols)
    {
        if (left_possible_output.contains(symbol))
        {
            require_left.insert(symbol);
        }
    }
    auto left_id = buildJoinNode(context, left_groups, union_find, graph, require_left, all_filter);

    // build right node, using [1:] groups
    std::set<String> require_right = require_names;
    require_right.insert(join_keys.second.begin(), join_keys.second.end());
    for (const auto & symbol : filter_symbols)
    {
        if (right_possible_output.contains(symbol))
        {
            require_right.insert(symbol);
        }
    }
    auto right_id = buildJoinNode(context, right_groups, union_find, graph, require_right, all_filter);

    auto join_node = createJoinNode(context, left_id, right_id, join_keys, require_names, filter);
    GroupExprPtr join_expr;
    context->getOptimizerContext().recordPlanNodeIntoGroup(join_node, join_expr, RuleType::JOIN_ENUM_ON_GRAPH);

    return join_expr->getGroupId();
}

TransformResult JoinEnumOnGraph::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    auto group_id = context.group_id;
    auto group = context.optimization_context->getOptimizerContext().getMemo().getGroupById(group_id);

    auto left_group_id = dynamic_cast<const AnyStep *>(node->getChildren()[0]->getStep().get())->getGroupId();
    auto right_group_id = dynamic_cast<const AnyStep *>(node->getChildren()[1]->getStep().get())->getGroupId();

    auto left_group = context.optimization_context->getOptimizerContext().getMemo().getGroupById(left_group_id);
    auto right_group = context.optimization_context->getOptimizerContext().getMemo().getGroupById(right_group_id);

    const auto * join_step = dynamic_cast<const JoinStep *>(node->getStep().get());

    std::set<String> output_names;
    for (const auto & item : group->getStep()->getOutputStream().header)
    {
        output_names.insert(item.name);
    }

    PlanNodes result;
    for (const auto & left_join_set : left_group->getJoinSets())
    {
        for (const auto & right_join_set : right_group->getJoinSets())
        {
            std::vector<ConstASTPtr> conjuncts;
            if (join_step->getFilter() && !PredicateUtils::isTruePredicate(join_step->getFilter()))
            {
                conjuncts.emplace_back(join_step->getFilter());
            }
            if (left_join_set.getFilter() && !PredicateUtils::isTruePredicate(left_join_set.getFilter()))
            {
                conjuncts.emplace_back(left_join_set.getFilter());
            }
            if (right_join_set.getFilter() && !PredicateUtils::isTruePredicate(right_join_set.getFilter()))
            {
                conjuncts.emplace_back(right_join_set.getFilter());
            }


            JoinSet merged_join_set{
                left_join_set,
                right_join_set,
                join_step->getLeftKeys(),
                join_step->getRightKeys(),
                PredicateUtils::combineConjuncts(conjuncts)};
            if (!group->containsJoinSet(merged_join_set))
            {
                group->addJoinSet(merged_join_set);
                auto graph = Graph::fromJoinSet(context, merged_join_set);
                for (auto & partition : graph.cutPartitions())
                {
                    auto left_groups = graph.getDFSOrder(partition.left);
                    std::reverse(left_groups.begin(), left_groups.end());
                    auto right_groups = graph.getDFSOrder(partition.right);
                    std::reverse(right_groups.begin(), right_groups.end());

                    // create join keys
                    auto join_keys = createJoinCondition(merged_join_set.getUnionFind(), graph.bridges(left_groups, right_groups));

                    auto left_possible_output = createPossibleSymbols(left_groups, context.optimization_context);
                    auto right_possible_output = createPossibleSymbols(right_groups, context.optimization_context);
                    // create join filter
                    auto filter = getJoinFilter(merged_join_set.getFilter(), left_possible_output, right_possible_output, context.context);
                    auto filter_symbols = SymbolsExtractor::extract(filter);

                    // build left node
                    std::set<String> require_left = output_names;
                    require_left.insert(join_keys.first.begin(), join_keys.first.end());
                    for (const auto & symbol : filter_symbols)
                    {
                        if (left_possible_output.contains(symbol))
                        {
                            require_left.insert(symbol);
                        }
                    }
                    auto new_left_id = buildJoinNode(
                        context.optimization_context,
                        left_groups,
                        merged_join_set.getUnionFind(),
                        graph,
                        require_left,
                        merged_join_set.getFilter());

                    // build right node
                    std::set<String> require_right = output_names;
                    require_right.insert(join_keys.second.begin(), join_keys.second.end());
                    for (const auto & symbol : filter_symbols)
                    {
                        if (right_possible_output.contains(symbol))
                        {
                            require_right.insert(symbol);
                        }
                    }
                    auto new_right_id = buildJoinNode(
                        context.optimization_context,
                        right_groups,
                        merged_join_set.getUnionFind(),
                        graph,
                        require_right,
                        merged_join_set.getFilter());


                    if (new_left_id != left_group_id)
                    {
                        result.emplace_back(
                            createJoinNode(context.optimization_context, new_left_id, new_right_id, join_keys, output_names, filter));
                    }
                    if (new_left_id != right_group_id)
                    {
                        result.emplace_back(createJoinNode(
                            context.optimization_context,
                            new_right_id,
                            new_left_id,
                            std::make_pair(join_keys.second, join_keys.first),
                            output_names,
                            filter));
                    }
                }
            }
        }
    }

    return TransformResult{result};
}

const std::vector<RuleType> & JoinEnumOnGraph::blockRules() const
{
    static std::vector<RuleType> block{RuleType::JOIN_ENUM_ON_GRAPH, RuleType::INNER_JOIN_REORDER};
    return block;
}


Graph Graph::fromJoinSet(RuleContext & context, JoinSet & join_set)
{
    Graph graph;

    std::unordered_map<String, GroupId> symbol_to_group_id;
    for (auto group_id : join_set.getGroups())
    {
        for (const auto & symbol : context.optimization_context->getMemo().getGroupById(group_id)->getStep()->getOutputStream().header)
        {
            assert(!symbol_to_group_id.contains(symbol.name)); // duplicate symbol
            symbol_to_group_id[symbol.name] = group_id;
        }
        graph.nodes.emplace_back(group_id);
    }

    for (auto & sets : join_set.getUnionFind().getSets())
    {
        for (const auto & source_symbol : sets)
        {
            for (const auto & target_symbol : sets)
            {
                assert(symbol_to_group_id.contains(source_symbol));
                assert(symbol_to_group_id.contains(target_symbol));
                auto source_id = symbol_to_group_id.at(source_symbol);
                auto target_id = symbol_to_group_id.at(target_symbol);
                if (source_id != target_id)
                {
                    graph.edges[source_id][target_id].emplace_back(source_symbol, target_symbol);
                }
            }
        }
    }

    return graph;
}
std::vector<Graph::Partition> Graph::cutPartitions() const
{
    MinCutBranchAlg alg(*this);
    alg.partition();
    return alg.getPartitions();
}

void MinCutBranchAlg::partition()
{
    auto min_node = *std::min_element(graph.getNodes().begin(), graph.getNodes().end());
    auto bit_size = *std::max_element(graph.getNodes().begin(), graph.getNodes().end()) + 1;

    BitSet s{bit_size};
    for (const auto & group_id : graph.getNodes())
        s[group_id] = true;

    BitSet c{bit_size};
    c[min_node] = true;
    minCutBranch(s, c, BitSet{s.size()}, c);
}

BitSet MinCutBranchAlg::minCutBranch(const BitSet & s, const BitSet & c, const BitSet & x, const BitSet & l)
{
    BitSet r{s.size()};
    BitSet r_tmp{s.size()};
    BitSet neighbor_l = neighbor(l);
    BitSet n_l = ((neighbor_l & s) - c) - x;
    BitSet n_x = ((neighbor_l & s) - c) & x;
    BitSet n_b = (((neighbor(c) & s) - c) - n_l) - x;

    BitSet x_tmp{s.size()};
    GroupId v;
    while (n_l.any() || n_x.any() || (n_b & r_tmp).any())
    {
        if (((n_b | n_l) & r_tmp).any())
        {
            v = ((n_b | n_l) & r_tmp).find_first();
            BitSet new_c = c;
            new_c[v] = true;
            BitSet new_l{s.size()};
            new_l[v] = true;
            minCutBranch(s, new_c, x_tmp, new_l);
            n_l[v] = false;
            n_b[v] = false;
        }
        else
        {
            x_tmp = x;
            if (n_l.any())
            {
                v = n_l.find_first();

                BitSet new_c = c;
                new_c[v] = true;
                BitSet new_l{s.size()};
                new_l[v] = true;

                r_tmp = minCutBranch(s, new_c, x_tmp, new_l);
                n_l[v] = false;
            }
            else
            {
                v = n_x.find_first();

                BitSet new_c = c;
                new_c[v] = true;
                BitSet new_l{s.size()};
                new_l[v] = true;

                r_tmp = reachable(new_c, new_l);
            }
            n_x = n_x - r_tmp;
            if ((r_tmp & x).any())
            {
                n_x = n_x | (n_l - r_tmp);
                n_l = n_l & r_tmp;
                n_b = n_b & r_tmp;
            }
            if (((s - r_tmp) & x).any())
            {
                n_l = n_l - r_tmp;
                n_b = n_b - r_tmp;
            }
            else
            {
                partitions.emplace_back(s - r_tmp, r_tmp);
            }
            r = r | r_tmp;
        }
        x_tmp[v] = true;
    }

    return r | l;
}

BitSet MinCutBranchAlg::neighbor(const BitSet & nodes)
{
    BitSet res{nodes.size()};
    auto pos = nodes.find_first();
    while (pos != boost::dynamic_bitset<>::npos)
    {
        if (!graph.getEdges().contains(pos))
        {
            std::cout << "bug";
        }
        for (const auto & item : graph.getEdges().at(pos))
        {
            res[item.first] = true;
        }
        pos = nodes.find_next(pos);
    }
    return res - nodes;
}

BitSet MinCutBranchAlg::reachable(const BitSet & c, const BitSet & l)
{
    BitSet r = l;

    BitSet n = neighbor(l) - c;
    while (n.any())
    {
        r = r | n;
        n = neighbor(n) - r - c;
    }
    return r;
}


}
