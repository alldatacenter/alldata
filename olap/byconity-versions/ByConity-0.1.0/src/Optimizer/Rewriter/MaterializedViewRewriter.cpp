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

#include <Optimizer/Rewriter/MaterializedViewRewriter.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Analyzers/TypeAnalyzer.h>
#include <Optimizer/CardinalityEstimate/TableScanEstimator.h>
#include <Optimizer/JoinGraph.h>
#include <Optimizer/OptimizerMetrics.h>
#include <Optimizer/MaterializedView/MaterializeViewChecker.h>
#include <Optimizer/MaterializedView/MaterializedViewMemoryCache.h>
#include <Optimizer/MaterializedView/MaterializedViewStructure.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/SimpleExpressionRewriter.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Parsers/ASTTableColumnReference.h>
#include <QueryPlan/SimplePlanRewriter.h>
#include <QueryPlan/SimplePlanVisitor.h>
#include <QueryPlan/TableScanStep.h>
#include <common/logger_useful.h>

#include <map>
#include <utility>

namespace DB
{
namespace ErrorCodes
{
    extern const int PARAMETER_OUT_OF_BOUND;
}

namespace
{
struct RewriterCandidate
{
    StorageID view_database_and_table_name;
    StorageID target_database_and_table_name;
    ASTPtr query_prewhere_expr;
    std::optional<PlanNodeStatisticsPtr> target_table_estimated_stats;
    NamesWithAliases table_output_columns;
    Assignments assignments;
    NameToType name_to_type;
    ASTPtr compensation_predicate;
    bool need_rollup = false;
    std::vector<ASTPtr> rollup_keys;
};
using RewriterCandidates = std::vector<RewriterCandidate>;

struct RewriterCandidateSort
{
    bool operator()(const RewriterCandidate & lhs, const RewriterCandidate & rhs)
    {
        if (!lhs.target_table_estimated_stats)
            return false;
        if (!rhs.target_table_estimated_stats)
            return true;
        return lhs.target_table_estimated_stats.value()->getRowCount() < rhs.target_table_estimated_stats.value()->getRowCount();
    }
};

struct RewriterFailureMessage
{
    StorageID storage;
    String message;
};
using RewriterFailureMessages = std::vector<RewriterFailureMessage>;

struct TableInputRef
{
    StoragePtr storage;
    [[nodiscard]] String getDatabaseTableName() const {
        return storage->getStorageID().getFullTableName();
    }
};

struct TableInputRefHash
{
    size_t operator()(const TableInputRef & ref) const { return std::hash<StoragePtr>()(ref.storage); }
};

struct TableInputRefEqual
{
    bool operator()(const TableInputRef & lhs, const TableInputRef & rhs) const { return lhs.storage == rhs.storage; }
};

struct JoinGraphMatchResult
{
    std::unordered_map<TableInputRef, std::vector<TableInputRef>, TableInputRefHash, TableInputRefEqual> query_to_view_table_mappings;
    std::vector<TableInputRef> view_missing_tables;
    std::unordered_map<String, std::shared_ptr<ASTTableColumnReference>> view_missing_columns;
    std::unordered_map<TableInputRef, const TableScanStep *, TableInputRefHash, TableInputRefEqual> query_table_scans;
};

using TableInputRefMap = std::unordered_map<TableInputRef, TableInputRef, TableInputRefHash, TableInputRefEqual>;
class SwapTableInputRefRewriter : public SimpleExpressionRewriter<const TableInputRefMap>
{
public:
    static ASTPtr rewrite(ASTPtr & expression, const TableInputRefMap & table_mapping)
    {
        SwapTableInputRefRewriter rewriter;
        return ASTVisitorUtil::accept(expression, rewriter, table_mapping);
    }

    ASTPtr visitASTTableColumnReference(ASTPtr & node, const TableInputRefMap & context) override
    {
        auto & table_column_ref = node->as<ASTTableColumnReference &>();
        table_column_ref.storage = context.at(TableInputRef{table_column_ref.storage}).storage;
        return node;
    }
};

using ExpressionEquivalences = Equivalences<ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals>;
using ConstASTMap = std::unordered_map<ConstASTPtr, ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals>;
class EquivalencesRewriter : public SimpleExpressionRewriter<const ConstASTMap>
{
public:
    static ASTPtr rewrite(ASTPtr & expression, const ExpressionEquivalences & expression_equivalences)
    {
        EquivalencesRewriter rewriter;
        return ASTVisitorUtil::accept(expression, rewriter, expression_equivalences.representMap());
    }

    ASTPtr visitNode(ASTPtr & node, const ConstASTMap & context) override
    {
        auto it = context.find(node);
        if (it != context.end())
            return it->second->clone();
        return SimpleExpressionRewriter::visitNode(node, context);
    }
};

class AddTableInputRefRewriter : public SimpleExpressionRewriter<const Void>
{
public:
    static ASTPtr rewrite(ASTPtr expression, StoragePtr storage)
    {
        AddTableInputRefRewriter rewriter(storage);
        return ASTVisitorUtil::accept(expression, rewriter, {});
    }

    ASTPtr visitASTIdentifier(ASTPtr & node, const Void &) override
    {
        return std::make_shared<ASTTableColumnReference>(storage, node->as<ASTIdentifier&>().name());
    }

    AddTableInputRefRewriter(StoragePtr storage_): storage(std::move(storage_)) {}
private:
    StoragePtr storage;
};

/**
 * Extract materialized views related into the query tables.
 */
class RelatedMaterializedViewExtractor : public SimplePlanVisitor<Void>
{
public:
    static std::map<String, std::vector<StorageID>> extract(QueryPlan & plan, ContextMutablePtr context_)
    {
        Void c;
        RelatedMaterializedViewExtractor finder{context_, plan.getCTEInfo()};
        VisitorUtil::accept(plan.getPlanNode(), finder, c);
        return finder.table_based_materialized_views;
    }

protected:
    RelatedMaterializedViewExtractor(ContextMutablePtr context_, CTEInfo & cte_info_) : SimplePlanVisitor(cte_info_), context(context_) { }

    Void visitTableScanNode(TableScanNode & node, Void &) override
    {
        const auto * storage = dynamic_cast<const TableScanStep *>(node.getStep().get());
        auto dependencies = DatabaseCatalog::instance().getDependencies(storage->getStorageID());
        if (!dependencies.empty())
            table_based_materialized_views.emplace(storage->getStorageID().getFullTableName(), std::move(dependencies));
        return Void{};
    }

private:
    ContextMutablePtr context;
    std::map<String, std::vector<StorageID>> table_based_materialized_views;
};

/**
 * tables that children contains, empty if there are unsupported plan nodes
 */
struct BaseTablesAndAggregateNode
{
    bool supported;
    std::vector<StorageID> base_tables;
    const PlanNodePtr top_aggregate_node;

    BaseTablesAndAggregateNode() : supported(false) { }
    BaseTablesAndAggregateNode(bool supported_, std::vector<StorageID> base_tables_, PlanNodePtr top_aggregate_node_)
        : supported(supported_), base_tables(std::move(base_tables_)), top_aggregate_node(std::move(top_aggregate_node_))
    {
    }
};

/**
 * Top-down match whether any node in query can be rewrite using materialized view.
 */
class CandidatesExplorer : public PlanNodeVisitor<BaseTablesAndAggregateNode, bool>
{
public:
    static std::unordered_map<PlanNodePtr, RewriterCandidates> explore(
        QueryPlan & query,
        ContextMutablePtr context,
        const std::map<String, std::vector<MaterializedViewStructurePtr>> & table_based_materialized_views,
        bool verbose)
    {
        CandidatesExplorer explorer{context, table_based_materialized_views, verbose};
        bool skip_match = false;
        VisitorUtil::accept(query.getPlanNode(), explorer, skip_match);

        if (verbose)
        {
            static auto * log = &Poco::Logger::get("CandidatesExplorer");
            for (auto & item : explorer.failure_messages)
                for (auto & message : item.second)
                    LOG_DEBUG(
                        log,
                        "rewrite fail: plan node id: " + std::to_string(item.first->getId()) + ", type: " + item.first->getStep()->getName()
                            + ", mview: " + message.storage.getFullTableName() + ", cause: " + message.message);
            for (auto & item : explorer.candidates)
                for (auto & candidate : item.second)
                    LOG_DEBUG(
                        log,
                        "rewrite success: plan node id: " + std::to_string(item.first->getId())
                            + ", type: " + item.first->getStep()->getName() + ", mview: " +
                            candidate.target_database_and_table_name.getDatabaseName() + "." +
                            candidate.target_database_and_table_name.getTableName());
        }

        return std::move(explorer.candidates);
    }

protected:
    CandidatesExplorer(
        ContextMutablePtr context_,
        const std::map<String, std::vector<MaterializedViewStructurePtr>> & table_based_materialized_views_,
        bool verbose_)
        : context(context_), table_based_materialized_views(table_based_materialized_views_), verbose(verbose_)
    {
    }

    BaseTablesAndAggregateNode visitPlanNode(PlanNodeBase & node, bool & skip_match) override { return process(node, skip_match, true); }

    BaseTablesAndAggregateNode visitFilterNode(FilterNode & node, bool & skip_match) override { return process(node, skip_match, true); }

    BaseTablesAndAggregateNode visitProjectionNode(ProjectionNode & node, bool & skip_match) override
    {
        return process(node, skip_match, true);
    }

    /**
     * Aggregating node is special, don't skip aggregate node match for two case:
     * 1. having filter above aggregate is not supported well
     * 2. nested aggregate node
     */
    BaseTablesAndAggregateNode visitAggregatingNode(AggregatingNode & node, bool &) override { return process(node, false, true); }

    BaseTablesAndAggregateNode visitJoinNode(JoinNode & node, bool & skip_match) override { return process(node, skip_match, false); }

    BaseTablesAndAggregateNode visitTableScanNode(TableScanNode & node, bool & skip_match) override
    {
        auto res = visitPlanNode(node, skip_match);
        if (!res.supported)
            return {};
        const auto * step = dynamic_cast<const TableScanStep *>(node.getStep().get());
        res.base_tables.emplace_back(step->getStorageID());
        return res;
    }

    /**
     * Visit plan tree from the bottom to up, skip unnecessary node to speed up matching.
     * eg. 1.projection - 2.filter - 3.aggregation - 4.filter - 5.table scan, only 1, 3 are necessary to match.
     */
    BaseTablesAndAggregateNode process(PlanNodeBase & node, bool skip_match, bool skip_children_match = false)
    {
        bool supported = MaterializedViewStepChecker::isSupported(node.getStep(), context);
        if (!supported)
            skip_children_match = false;

        std::vector<StorageID> base_tables;
        PlanNodePtr top_aggregate_node = node.getStep()->getType() == IQueryPlanStep::Type::Aggregating ? node.shared_from_this() : nullptr;
        for (auto & child : node.getChildren())
        {
            auto res = VisitorUtil::accept(child, *this, skip_children_match);
            if (!res.supported)
                return {};
            if (res.top_aggregate_node)
            {
                if (node.getChildren().size() != 1)
                    return {};
                if (top_aggregate_node)
                    return {};
                top_aggregate_node = res.top_aggregate_node;
            }
            base_tables.insert(base_tables.end(), res.base_tables.begin(), res.base_tables.end());
        }
        if (!supported)
            return {};

        if (!skip_match)
            matches(node, top_aggregate_node, base_tables);
        return BaseTablesAndAggregateNode{true, base_tables, top_aggregate_node};
    }

    /**
     * matches plan node using all related materialized view.
     */
    void matches(PlanNodeBase & query, const PlanNodePtr & top_aggregate_node, const std::vector<StorageID> & tables)
    {
        std::vector<MaterializedViewStructurePtr> related_materialized_views;
        for (const auto & table : tables)
            if (auto it = table_based_materialized_views.find(table.getFullTableName()); it != table_based_materialized_views.end())
                related_materialized_views.insert(related_materialized_views.end(), it->second.begin(), it->second.end());

        if (related_materialized_views.empty())
            return;

        JoinGraph query_join_graph = JoinGraph::build(
            top_aggregate_node ? top_aggregate_node->getChildren()[0] : query.shared_from_this(), context, true, false, true);
        std::vector<ConstASTPtr> query_other_predicates;
        if (top_aggregate_node)
        {
            auto having_predicates = JoinGraph::build(query.shared_from_this(), context, true, false, true).getFilters();
            query_other_predicates.insert(query_other_predicates.end(), having_predicates.begin(), having_predicates.end());
        }
        std::shared_ptr<const AggregatingStep> query_aggregate = top_aggregate_node
            ? dynamic_pointer_cast<const AggregatingStep>(top_aggregate_node->getStep())
            : std::shared_ptr<const AggregatingStep>{};

        std::optional<SymbolTransformMap> query_map; // lazy initialization later

        for (const auto & view : related_materialized_views)
            if (auto result = match(
                    query,
                    query_join_graph,
                    query_other_predicates,
                    query_map,
                    query_aggregate,
                    view->join_graph,
                    view->other_predicates,
                    view->symbol_map,
                    view->output_columns,
                    view->output_columns_to_table_columns_map,
                    view->expression_equivalences,
                    view->top_aggregating_step,
                    view->view_storage_id,
                    view->target_storage_id))
                candidates[query.shared_from_this()].emplace_back(*result);
    }

    /**
     * main logic for materialized view based query rewriter.
     */
    std::optional<RewriterCandidate> match(
        PlanNodeBase & query,
        const JoinGraph & query_join_graph,
        const std::vector<ConstASTPtr> & query_other_predicates,
        std::optional<SymbolTransformMap> & query_map,
        const std::shared_ptr<const AggregatingStep> & query_aggregate,
        const JoinGraph & view_join_graph,
        const std::vector<ConstASTPtr> & view_other_predicates,
        const SymbolTransformMap & view_map,
        const std::unordered_set<String> & view_outputs,
        const std::unordered_map<String, String> & view_outputs_to_table_columns_map,
        const ExpressionEquivalences & view_equivalences,
        const std::shared_ptr<const AggregatingStep> & view_aggregate,
        const StorageID view_storage_id,
        const StorageID target_storage_id)
    {
        auto add_failure_message = [&](const String & message) {
            if (verbose)
                failure_messages[query.shared_from_this()].emplace_back(
                    RewriterFailureMessage{view_storage_id, message});
        };

        // 0. pre-check

        // 1. check join
        auto join_graph_match_result = matchJoinGraph(query_join_graph, view_join_graph, context);
        if (!join_graph_match_result)
        {
            add_failure_message("join graph rewrite fail.");
            return {};
        }

        // get all predicates from join graph
        auto query_predicates = PredicateUtils::extractEqualPredicates(query_join_graph.getFilters());
        for (const auto & predicate : query_other_predicates)
            query_predicates.second.emplace_back(predicate);
        auto view_predicates = PredicateUtils::extractEqualPredicates(view_join_graph.getFilters());
        for (const auto & predicate : view_other_predicates)
            view_predicates.second.emplace_back(predicate);

        if (!query_map)
        {
            query_map = SymbolTransformMap::buildFrom(query); // lazy initialization here.
            if (!query_map)
            {
                add_failure_message("query transform map invalid, maybe has duplicate symbols");
                return {};
            }
        }

        // 1-2. generate table mapping from join graph
        // If a table is used multiple times, we will create multiple mappings,
        // and we will try to rewrite the query using each of the mappings.
        // Then, we will try to map every source table (query) to a target table (view).
        for (auto & query_to_view_table_mappings : generateFlatTableMappings(join_graph_match_result->query_to_view_table_mappings))
        {
            // 2. check where(predicates)
            std::vector<ConstASTPtr> compensation_predicates;
            ExpressionEquivalences view_based_query_equivalences_map;

            // 2-1. equal-predicates
            // Rewrite query equal predicates to view-based and build new equivalences map. Then check whether
            // these query equal-predicates exists in view equivalences, otherwise, construct missing predicate for view.
            for (const auto & predicate : query_predicates.first)
            {
                auto left_symbol_lineage = normalizeExpression(predicate.first, *query_map, query_to_view_table_mappings);
                auto right_symbol_lineage = normalizeExpression(predicate.second, *query_map, query_to_view_table_mappings);
                view_based_query_equivalences_map.add(left_symbol_lineage, right_symbol_lineage);

                if (!view_equivalences.isEqual(left_symbol_lineage, right_symbol_lineage))
                    compensation_predicates.emplace_back(makeASTFunction("equals", left_symbol_lineage, right_symbol_lineage));
            }

            // check whether view equal-predicates exists in query equivalences, otherwise, give up.
            bool has_missing_predicate = false;
            for (const auto & predicate : view_predicates.first)
            {
                auto left_symbol_lineage = normalizeExpression(predicate.first, view_map);
                auto right_symbol_lineage = normalizeExpression(predicate.second, view_map);
                if (!view_based_query_equivalences_map.isEqual(left_symbol_lineage, right_symbol_lineage))
                {
                    add_failure_message("equal-predicates rewrite fail.");
                    has_missing_predicate = true;
                    break; // bail out
                }
            }
            if (has_missing_predicate)
                continue; // bail out

            // 2-2. range-predicates
            // TupleDomainResult query_domain_result = TupleDomain::buildFrom(query_predicates.first);
            // TupleDomainResult view_domain_result = TupleDomain::buildFrom(view_predicates.first);
            //
            // TupleDomain view_based_query_domain;
            // for (auto & domain : query_domain_result.getDomain())
            //     view_based_query_domain.emplace(domain.first, domain.second);
            //
            // auto result = view_based_query_domain.subtract(view_domain_result.getDomain());
            // if (!result.second)
            //     continue; // bail out // try union rewrite
            //
            // compensation_predicates.emplace_back(result.first.toPredicate());

            // 2-3. other-predicates
            // compensation_predicates.emplace_back(PredicateUtils.splitPredicates(
            //     query_domain_result.getOtherPredicates(), view_domain_result.getOtherPredicates()));
            auto view_based_query_predicates = normalizeExpression(
                PredicateUtils::combineConjuncts(query_predicates.second),
                *query_map,
                query_to_view_table_mappings,
                view_based_query_equivalences_map);
            auto other_compensation_predicate = PredicateUtils::splitPredicates(
                view_based_query_predicates,
                normalizeExpression(PredicateUtils::combineConjuncts(view_predicates.second), view_map, view_based_query_equivalences_map));
            if (!other_compensation_predicate)
            {
                add_failure_message("other-predicates rewrite fail.");
                continue; // bail out
            }
            compensation_predicates.emplace_back(other_compensation_predicate);

            auto compensation_predicate = PredicateUtils::combineConjuncts(compensation_predicates);
            if (PredicateUtils::isFalsePredicate(compensation_predicate))
            {
                add_failure_message("compensation predicates rewrite fail.");
                continue; // bail out
            }

            // 3. check whether rollup is needed.
            bool need_rollup = false;
            if (query_aggregate && !view_aggregate)
                need_rollup = true;

            std::unordered_set<ASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> view_based_query_keys;
            if (query_aggregate)
                for (const auto & query_key : query_aggregate->getKeys())
                    view_based_query_keys.emplace(normalizeExpression(
                        makeASTIdentifier(query_key), *query_map, query_to_view_table_mappings, view_based_query_equivalences_map));

            std::unordered_set<ASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> view_keys;
            if (view_aggregate)
                for (const auto & view_key : view_aggregate->getKeys())
                    view_keys.emplace(normalizeExpression(makeASTIdentifier(view_key), view_map, view_based_query_equivalences_map));

            if (query_aggregate || view_aggregate)
            {
                auto const_expressions = extractConstExpressions(view_based_query_predicates);
                if (!const_expressions.empty())
                {
                    for (auto it = view_keys.begin(); it != view_keys.end();)
                        if (const_expressions.count(*it) || it->get()->getType() == ASTType::ASTLiteral)
                            it = view_keys.erase(it);
                        else
                            it++;

                    for (auto it = view_based_query_keys.begin(); it != view_based_query_keys.end();)
                        if (const_expressions.count(*it) || it->get()->getType() == ASTType::ASTLiteral)
                            it = view_based_query_keys.erase(it);
                        else
                            it++;
                }

                need_rollup = view_based_query_keys.size() != view_keys.size()
                    || !std::all_of(view_based_query_keys.begin(), view_based_query_keys.end(), [&](auto & key) {
                                  return view_keys.count(key);
                              });
            }

            // 4. check output columns
            // 4-1. build lineage expression to output columns map using view-based query equivalences map.
            std::unordered_map<ConstASTPtr, ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> view_output_columns_map;
            for (const auto & view_output : view_outputs)
            {
                // {
                //     auto expr = normalizeExpression(makeASTIdentifier(view_output), view_map, view_based_query_equivalences_map);
                //     std::cout << expr->getColumnName() << " -> " << view_output << std::endl;
                // }
                view_output_columns_map.emplace(
                    normalizeExpression(makeASTIdentifier(view_output), view_map, view_based_query_equivalences_map),
                    makeASTIdentifier(view_output));
            }

            // 4-2. update symbol transform map if there are view missing tables.
            for (const auto & missing_table_column : join_graph_match_result->view_missing_columns)
                view_output_columns_map.emplace(
                    normalizeExpression(
                        missing_table_column.second, *query_map, query_to_view_table_mappings, view_based_query_equivalences_map),
                    makeASTIdentifier(missing_table_column.first));

            // 4-3. check select columns (contains aggregates)
            NameSet required_columns_set;
            Assignments assignments;
            NameToType name_to_type;
            bool enforce_agg_node = false;
            for (const auto & name_and_type : query.getCurrentDataStream().header)
            {
                const auto & output_name = name_and_type.name;

                auto rewrite = rewriteExpressionContainsAggregates(
                    normalizeExpression(
                        makeASTIdentifier(output_name), *query_map, query_to_view_table_mappings, view_based_query_equivalences_map),
                    view_output_columns_map,
                    view_outputs,
                    need_rollup,
                    enforce_agg_node);
                if (!rewrite)
                {
                    add_failure_message("output column `" + output_name + "` rewrite fail.");
                    break; // bail out
                }
                auto columns = SymbolsExtractor::extract(*rewrite);
                required_columns_set.insert(columns.begin(), columns.end());

                assignments.emplace_back(Assignment{output_name, *rewrite});
                name_to_type.emplace(output_name, name_and_type.type);
            }
            if (assignments.size() != query.getCurrentDataStream().header.columns())
                continue; // bail out

            // 4-4. if rollup is needed, check group by keys.
            std::vector<ASTPtr> rollup_keys;
            if (need_rollup || enforce_agg_node)
            {
                for (const auto & query_key : view_based_query_keys)
                {
                    auto rewrite = rewriteExpression(query_key, view_output_columns_map, view_outputs);
                    if (!rewrite)
                    {
                        add_failure_message("group by column `" + query_key->getColumnName() + "` rewrite fail.");
                        break; // bail out
                    }
                    rollup_keys.emplace_back(*rewrite);
                }
                if (rollup_keys.size() != view_based_query_keys.size())
                    continue; // bail out, rollup agg group by column rewrite fail.
            }

            // 4-5. check columns in predicates.
            auto rewrite_compensation_predicate = rewriteExpression(compensation_predicate, view_output_columns_map, view_outputs);
            if (!rewrite_compensation_predicate)
            {
                add_failure_message("compensation predicate rewrite fail.");
                continue;
            }
            auto columns = SymbolsExtractor::extract(*rewrite_compensation_predicate);
            required_columns_set.insert(columns.begin(), columns.end());

            if ((need_rollup || enforce_agg_node) && !query_other_predicates.empty())
            {
                add_failure_message("having predicate rollup rewrite fail.");
                continue;
            }

            // 5. construct candidate
            auto it_stats = materialized_views_stats.find(target_storage_id.getFullTableName());
            if (it_stats == materialized_views_stats.end())
            {
                auto stats = TableScanEstimator::estimate(context, target_storage_id);
                it_stats = materialized_views_stats.emplace(target_storage_id.getFullTableName(), stats).first;
            }

            NamesWithAliases table_columns_with_aliases;
            for (const auto & column : required_columns_set)
            {
                auto it = view_outputs_to_table_columns_map.find(column);
                if (it == view_outputs_to_table_columns_map.end())
                {
                    add_failure_message("Logical error: column `" + column + "` not found in table output columns.");
                    continue; // bail out
                }
                table_columns_with_aliases.emplace_back(it->second, column);
            }

            bool single_table = query_to_view_table_mappings.size() == 1;

            // keep prewhere info for single table rewriting
            ASTPtr query_prewhere_expr;
            if (single_table)
            {
                const auto & query_table_ref = query_to_view_table_mappings.begin()->first;
                const auto & view_table_ref = query_to_view_table_mappings.begin()->second;
                const auto * query_table_scan = join_graph_match_result->query_table_scans.at(query_table_ref);
                if (query_table_scan->getQueryInfo().query)
                {
                    auto & select_query = query_table_scan->getQueryInfo().query->as<ASTSelectQuery &>();
                    if (select_query.prewhere())
                    {
                        // rewrite base table column to mv table column
                        auto normalized_prewhere = AddTableInputRefRewriter::rewrite(select_query.getPrewhere() /* clone */, view_table_ref.storage);
                        auto rewritten_prewhere = rewriteExpression(normalized_prewhere, view_output_columns_map, view_outputs);
                        if (rewritten_prewhere)
                            query_prewhere_expr = *rewritten_prewhere;
                    }
                }
            }

            return RewriterCandidate{
                view_storage_id,
                target_storage_id,
                query_prewhere_expr,
                it_stats->second,
                table_columns_with_aliases,
                assignments,
                name_to_type,
                *rewrite_compensation_predicate,
                need_rollup || enforce_agg_node,
                rollup_keys};
        }
        // no matches
        return {};
    }

    /**
     * First, it rewrite identifiers in expression recursively, get lineage-form expression.
     * Then, it swaps the all TableInputRefs using table_mapping.
     * Finally, it rewrite equivalent expressions to unified expression.
     *
     * <p> it is used to rewrite expression from query to view-based uniform expression.
     */
    static ASTPtr normalizeExpression(
        const ConstASTPtr & expression,
        const SymbolTransformMap & symbol_transform_map,
        const TableInputRefMap & table_mapping,
        const ExpressionEquivalences & expression_equivalences)
    {
        auto lineage = symbol_transform_map.inlineReferences(expression);
        lineage = SwapTableInputRefRewriter::rewrite(lineage, table_mapping);
        lineage = EquivalencesRewriter::rewrite(lineage, expression_equivalences);
        return lineage;
    }

    /**
     * it is used to rewrite expression from view to uniform expression.
     */
    static ASTPtr normalizeExpression(
        const ConstASTPtr & expression,
        const SymbolTransformMap & symbol_transform_map,
        const ExpressionEquivalences & expression_equivalences)
    {
        auto lineage = symbol_transform_map.inlineReferences(expression);
        lineage = EquivalencesRewriter::rewrite(lineage, expression_equivalences);
        return lineage;
    }

    static ASTPtr normalizeExpression(const ConstASTPtr & expression, const SymbolTransformMap & symbol_transform_map)
    {
        auto lineage = symbol_transform_map.inlineReferences(expression);
        return lineage;
    }

    static ASTPtr normalizeExpression(
        const ConstASTPtr & expression, const SymbolTransformMap & symbol_transform_map, const TableInputRefMap & table_mapping)
    {
        auto lineage = symbol_transform_map.inlineReferences(expression);
        SwapTableInputRefRewriter::rewrite(lineage, table_mapping);
        return lineage;
    }

    static inline ASTPtr makeASTIdentifier(const String & name) { return std::make_shared<ASTIdentifier>(name); }

    /**
     * It will flatten a multimap containing table references to table references, producing all possible combinations of mappings.
     */
    static std::vector<std::unordered_map<TableInputRef, TableInputRef, TableInputRefHash, TableInputRefEqual>> generateFlatTableMappings(
        const std::unordered_map<TableInputRef, std::vector<TableInputRef>, TableInputRefHash, TableInputRefEqual> & table_multi_mapping)
    {
        if (table_multi_mapping.empty())
            return {};

        std::vector<std::unordered_map<TableInputRef, TableInputRef, TableInputRefHash, TableInputRefEqual>> table_mappings;
        table_mappings.emplace_back(); // init with empty map
        for (const auto & item : table_multi_mapping)
        {
            const auto & query_table_ref = item.first;
            const auto & view_table_refs = item.second;
            if (view_table_refs.size() == 1)
            {
                for (auto & mappings : table_mappings)
                    mappings.emplace(query_table_ref, view_table_refs[0]);
            }
            else
            {
                std::vector<std::unordered_map<TableInputRef, TableInputRef, TableInputRefHash, TableInputRefEqual>> new_table_mappings;
                for (const auto & view_table_ref : view_table_refs)
                {
                    for (auto & mappings : table_mappings)
                    {
                        bool contains = std::any_of(mappings.begin(), mappings.end(), [&](const auto & m) {
                            return TableInputRefEqual{}.operator()(m.second, view_table_ref);
                        });
                        if (contains)
                            continue;

                        auto new_mappings = mappings;
                        new_mappings.emplace(query_table_ref, view_table_ref);
                        new_table_mappings.emplace_back(std::move(new_mappings));
                    }
                }
                table_mappings.swap(new_table_mappings);
            }
        }
        return table_mappings;
    }

    /**
     * We try to extract table reference and check whether query can be computed from view.
     * if not, try to compensate, e.g., for join queries it might be possible to join missing tables
     * with view to compute result.
     *
     * <p> supported:
     * - Self join
     * - view tables are subset of query tables (add additional tables through joins if possible)
     *
     * <p> todo:
     * - query tables are subset of view tables (we need to check whether they are cardinality-preserving joins)
     * - outer join
     * - semi join and anti join
     */
    static std::optional<JoinGraphMatchResult>
    matchJoinGraph(const JoinGraph & query_graph, const JoinGraph & view_graph, ContextMutablePtr context)
    {
        if (query_graph.isEmpty())
            return {}; // bail out, if there is no tables.
        const auto & query_nodes = query_graph.getNodes();
        const auto & view_nodes = view_graph.getNodes();
        if (query_nodes.size() < view_nodes.size())
            return {}; // bail out, if some tables are missing in the query

        auto extract_table_ref = [](PlanNodeBase & node) -> std::optional<TableInputRef> {
            if (const auto * table_step = dynamic_cast<const TableScanStep *>(node.getStep().get()))
                return TableInputRef{table_step->getStorage()};
            return {};
        };

        std::unordered_map<String, std::vector<std::pair<PlanNodePtr, TableInputRef>>> query_table_map;
        for (const auto & node : query_nodes)
        {
            auto table_input_ref = extract_table_ref(*node);
            if (!table_input_ref)
                return {}; // bail out
            query_table_map[table_input_ref->getDatabaseTableName()].emplace_back(node, *table_input_ref);
        }

        std::unordered_map<String, std::vector<TableInputRef>> view_table_map;
        for (const auto & node : view_nodes)
        {
            auto table_input_ref = extract_table_ref(*node);
            if (!table_input_ref)
                return {}; // bail out
            view_table_map[table_input_ref->getDatabaseTableName()].emplace_back(*table_input_ref);
        }

        bool is_query_missing_table = std::any_of(view_table_map.begin(), view_table_map.end(), [&](const auto & item) {
            return item.second.size() > query_table_map[item.first].size();
        });
        if (is_query_missing_table)
            return {}; // bail out, if some tables are missing in the query

        std::unordered_map<TableInputRef, std::vector<TableInputRef>, TableInputRefHash, TableInputRefEqual> table_mapping;
        std::vector<TableInputRef> view_missing_tables;
        std::unordered_map<String, std::shared_ptr<ASTTableColumnReference>> view_missing_columns;
        std::unordered_map<TableInputRef, const TableScanStep *, TableInputRefHash, TableInputRefEqual> query_table_scans;
        for (auto & item : query_table_map)
        {
            auto & query_table_refs = item.second;
            auto view_table_refs = view_table_map[item.first];

            if (query_table_refs.size() > view_table_refs.size())
            {
                std::unordered_set<String> missing_columns;
                for (const auto & query_table_ref : query_table_refs)
                {
                    const auto * table_step = dynamic_cast<const TableScanStep *>(query_table_ref.first->getStep().get());
                    for (const auto & column : table_step->getColumnAlias())
                        missing_columns.emplace(column.second);
                }

                auto & storage = query_table_refs[0].second.storage;
                for (size_t i = 0; i < query_table_refs.size() - view_table_refs.size(); i++)
                {
                    // fix database is not set in storage in unittest
                    // use storage->cloneFromThis(context) ?
                    auto new_view_table_ref = DatabaseCatalog::instance().tryGetTable(storage->getStorageID(), context);
                    if (!new_view_table_ref)
                        return {}; // bail out, if we can't clone a new table storage.
                    view_table_refs.emplace_back(TableInputRef{new_view_table_ref});
                    for (const auto & column : missing_columns)
                        view_missing_columns.emplace(column, std::make_shared<ASTTableColumnReference>(new_view_table_ref, column));
                }
            }

            for (auto & query_table_ref : query_table_refs)
            {
                table_mapping[query_table_ref.second] = view_table_refs;
                query_table_scans[query_table_ref.second] = dynamic_cast<const TableScanStep *>(query_table_ref.first->getStep().get());
            }
        }

        return JoinGraphMatchResult{table_mapping, view_missing_tables, view_missing_columns, query_table_scans};
    }

    /**
     * Extracts const expression .eg, a = 1.
     * Const expression can be eliminated from group by keys.
     */
    static std::unordered_set<ASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> extractConstExpressions(const ASTPtr & predicates)
    {
        std::unordered_set<ASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> result;
        for (auto & predicate : PredicateUtils::extractConjuncts(predicates))
        {
            if (const auto * equal = predicate->as<ASTFunction>())
            {
                if (equal->name == "equals")
                {
                    if (equal->children.size() == 1 && equal->children[0]->getType() == ASTType::ASTExpressionList)
                    {
                        auto & arguments = equal->children[0]->getChildren();
                        if (arguments.size() == 2 && arguments[1]->getType() == ASTType::ASTLiteral)
                            result.emplace(arguments[0]);
                    }
                }
                else if (equal->name == "isNull" || equal->name == "isNotNull")
                    result.emplace(equal->children[0]);
            }
        }
        return result;
    }

    /**
     * It rewrite input expression using output column.
     * If any of the expressions in the input expression cannot be mapped, it will return null.
     *
     * <p> Compared with rewriteExpression, it supports expression contains aggregates, and do rollup rewrite.
     */
    static std::optional<ASTPtr> rewriteExpressionContainsAggregates(
        ASTPtr expression,
        const std::unordered_map<ConstASTPtr, ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> & view_output_columns_map,
        const std::unordered_set<String> & output_columns,
        bool need_rollup,
        bool & enforce_agg_node)
    {
        class ExpressionWithAggregateRewriter : public ASTVisitor<std::optional<ASTPtr>, Void>
        {
        public:
            ExpressionWithAggregateRewriter(
                const std::unordered_map<ConstASTPtr, ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> & view_output_columns_map_,
                const std::unordered_set<String> & output_columns_,
                bool need_rollup_,
                bool & enforce_agg_node_)
                : view_output_columns_map(view_output_columns_map_)
                , output_columns(output_columns_)
                , need_rollup(need_rollup_)
                , enforce_agg_node(enforce_agg_node_)
            {
            }

            std::optional<ASTPtr> visitNode(ASTPtr & node, Void & c) override
            {
                if (view_output_columns_map.contains(node))
                    return view_output_columns_map.at(node)->clone();

                for (auto & child : node->children)
                {
                    auto result = ASTVisitorUtil::accept(child, *this, c);
                    if (!result)
                        return {};
                    if (*result != child)
                        child = std::move(*result);
                }
                return node;
            }

            std::optional<ASTPtr> visitASTFunction(ASTPtr & node, Void & c) override
            {
                auto * function = node->as<ASTFunction>();
                if (!AggregateFunctionFactory::instance().isAggregateFunctionName(function->name))
                    return visitNode(node, c);

                // try to rewrite the expression with the following rules:
                // 1. state aggregate support rollup directly.
                if (!function->name.ends_with("State"))
                {
                    auto state_function = function->clone();
                    state_function->as<ASTFunction &>().name = function->name + "State";
                    auto rewrite_expression = rewriteExpression(state_function, view_output_columns_map, output_columns);
                    if (rewrite_expression)
                    {
                        auto rewritten_function = std::make_shared<ASTFunction>();
                        rewritten_function->name = need_rollup ? (function->name + "Merge") : ("finalizeAggregation");
                        rewritten_function->arguments = std::make_shared<ASTExpressionList>();
                        rewritten_function->children.emplace_back(rewritten_function->arguments);
                        rewritten_function->arguments->children.push_back(*rewrite_expression);
                        rewritten_function->parameters = function->parameters;
                        return rewritten_function;
                    }
                }

                // 2. rewrite with direct match
                auto rewrite = rewriteExpression(node, view_output_columns_map, output_columns, true);
                if (!rewrite)
                    return {};
                // if rewrite expression is aggregate function, we should enforce an aggregate node.
                if (isAggregateFunction(*rewrite))
                {
                    enforce_agg_node = true;
                    return rewrite;
                }
                if (!need_rollup)
                    return rewrite;

                // 3. if rollup is needed, try to add rollup aggregates.
                String rollup_aggregate_name = getRollupAggregateName(function->name);
                if (rollup_aggregate_name.empty())
                    return {};
                auto rollup_function = std::make_shared<ASTFunction>();
                rollup_function->name = std::move(rollup_aggregate_name);
                rollup_function->arguments = std::make_shared<ASTExpressionList>();
                rollup_function->arguments->children.emplace_back(*rewrite);
                rollup_function->parameters = function->parameters;
                rollup_function->children.emplace_back(rollup_function->arguments);
                return rollup_function;
            }

            static bool isAggregateFunction(const ASTPtr & ast)
            {
                if (const auto * function = ast->as<const ASTFunction>())
                    if (AggregateFunctionFactory::instance().isAggregateFunctionName(function->name))
                        return true;
                return false;
            }

            static String getRollupAggregateName(const String & name)
            {
                if (name == "count")
                    return "sum";
                else if (name == "min" || name == "max" || name == "sum")
                    return name;
                return {};
            }

            const std::unordered_map<ConstASTPtr, ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> & view_output_columns_map;
            const std::unordered_set<String> & output_columns;
            bool need_rollup;
            bool & enforce_agg_node;
        };
        ExpressionWithAggregateRewriter rewriter{view_output_columns_map, output_columns, need_rollup, enforce_agg_node};
        Void c;
        auto rewrite = ASTVisitorUtil::accept(expression, rewriter, c);
        if (rewrite && isValidExpression(*rewrite, output_columns, true))
            return rewrite;
        return {};
    }

    /**
     * It rewrite input expression using output column.
     * If any of the expressions in the input expression cannot be mapped, it will return null.
     */
    static std::optional<ASTPtr> rewriteExpression(
        ASTPtr expression,
        const std::unordered_map<ConstASTPtr, ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals> & view_output_columns_map,
        const std::unordered_set<String> & output_columns,
        bool allow_aggregate = false)
    {
        EquivalencesRewriter rewriter;
        auto rewrite_expression = ASTVisitorUtil::accept(expression, rewriter, view_output_columns_map);
        if (isValidExpression(rewrite_expression, output_columns, allow_aggregate))
            return rewrite_expression;
        return {}; // rewrite fail, bail out
    }

    /**
     * Checks whether there are identifiers in input expression are mapped into output columns,
     * or there are unmapped table references,
     * or there are nested aggregate functions.
     */
    static bool
    isValidExpression(const ConstASTPtr & expression, const std::unordered_set<String> & output_columns, bool allow_aggregate = false)
    {
        class ExpressionChecker : public ConstASTVisitor<bool, bool>
        {
        public:
            explicit ExpressionChecker(const std::unordered_set<String> & output_columns_) : output_columns(output_columns_) { }

            bool visitNode(const ConstASTPtr & ast, bool & c) override
            {
                return std::all_of(ast->children.begin(), ast->children.end(), [&](const auto & child) {
                    return ASTVisitorUtil::accept(child, *this, c);
                });
            }

            bool visitASTTableColumnReference(const ConstASTPtr &, bool &) override { return false; }

            bool visitASTIdentifier(const ConstASTPtr & node, bool &) override
            {
                return output_columns.count(node->as<const ASTIdentifier &>().name());
            }

            bool visitASTFunction(const ConstASTPtr & node, bool & allow_aggregate) override
            {
                if (!AggregateFunctionFactory::instance().isAggregateFunctionName(node->as<ASTFunction &>().name))
                    return visitNode(node, allow_aggregate);
                if (!allow_aggregate)
                    return false;
                bool allow_nested_aggregate = false;
                return visitNode(node, allow_nested_aggregate);
            }

        private:
            const std::unordered_set<String> & output_columns;
        };
        ExpressionChecker checker{output_columns};
        return ASTVisitorUtil::accept(expression, checker, allow_aggregate);
    }

public:
    ContextMutablePtr context;
    const std::map<String, std::vector<MaterializedViewStructurePtr>> & table_based_materialized_views;
    std::unordered_map<PlanNodePtr, RewriterCandidates> candidates;
    std::unordered_map<PlanNodePtr, RewriterFailureMessages> failure_messages;
    std::map<String, std::optional<PlanNodeStatisticsPtr>> materialized_views_stats;
    const bool verbose;
};

using ASTToStringMap = std::unordered_map<ConstASTPtr, String, ASTEquality::ASTHash, ASTEquality::ASTEquals>;

class CostBasedMaterializedViewRewriter : public SimplePlanRewriter<Void>
{
public:
    static void rewrite(QueryPlan & plan, ContextMutablePtr context_, std::unordered_map<PlanNodePtr, RewriterCandidates> & match_results)
    {
        Void c;
        CostBasedMaterializedViewRewriter rewriter(context_, plan.getCTEInfo(), match_results);
        auto rewrite = VisitorUtil::accept(plan.getPlanNode(), rewriter, c);
        plan.update(rewrite);
    }

protected:
    CostBasedMaterializedViewRewriter(ContextMutablePtr context_, CTEInfo & cte_info, std::unordered_map<PlanNodePtr, RewriterCandidates> & match_results_)
        : SimplePlanRewriter(context_, cte_info), match_results(match_results_)
    {
    }

    PlanNodePtr visitPlanNode(PlanNodeBase & node, Void & c) override
    {
        if (auto it = match_results.find(node.shared_from_this()); it != match_results.end())
        {
            auto & candidates = it->second;
            auto candidate_it = std::min_element(candidates.begin(), candidates.end(), RewriterCandidateSort());
            context->getOptimizerMetrics()->addMaterializedView(candidate_it->view_database_and_table_name);
            return constructEquivalentPlan(*candidate_it);
        }
        return SimplePlanRewriter::visitPlanNode(node, c);
    }

    PlanNodePtr constructEquivalentPlan(const RewriterCandidate & candidate)
    {
        // table scan
        auto plan = planTableScan(candidate.target_database_and_table_name, candidate.table_output_columns, candidate.query_prewhere_expr);

        // where
        if (candidate.compensation_predicate != PredicateConst::TRUE_VALUE)
            plan = PlanNodeBase::createPlanNode(
                context->nextNodeId(), std::make_shared<FilterStep>(plan->getCurrentDataStream(), candidate.compensation_predicate), {plan});

        // aggregation
        Assignments rewrite_assignments;
        std::tie(plan, rewrite_assignments) = planAggregate(plan, candidate.need_rollup, candidate.rollup_keys, candidate.assignments);

        // output projection
        plan = PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            std::make_shared<ProjectionStep>(plan->getCurrentDataStream(), rewrite_assignments, candidate.name_to_type),
            {plan});
        return plan;
    }

    PlanNodePtr planTableScan(
        const StorageID & target_database_and_table_name,
        const NamesWithAliases & columns_with_aliases,
        ASTPtr query_prewhere_expr)
    {
        const auto select_expression_list = std::make_shared<ASTExpressionList>();
        select_expression_list->children.reserve(columns_with_aliases.size());
        /// manually substitute column names in place of asterisk
        for (const auto & column : columns_with_aliases)
            select_expression_list->children.emplace_back(std::make_shared<ASTIdentifier>(column.first));

        SelectQueryInfo query_info;
        const auto generated_query = std::make_shared<ASTSelectQuery>();
        generated_query->setExpression(ASTSelectQuery::Expression::SELECT, select_expression_list);
        query_info.query = generated_query;

        if (query_prewhere_expr)
            generated_query->setExpression(ASTSelectQuery::Expression::PREWHERE, std::move(query_prewhere_expr));

        UInt64 max_block_size = context->getSettingsRef().max_block_size;
        if (!max_block_size)
            throw Exception("Setting 'max_block_size' cannot be zero", ErrorCodes::PARAMETER_OUT_OF_BOUND);

        QueryProcessingStage::Enum processing_stage = QueryProcessingStage::Enum::FetchColumns;
        return PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            std::make_shared<TableScanStep>(
                context,
                target_database_and_table_name,
                columns_with_aliases,
                query_info,
                processing_stage,
                max_block_size));
    }

    /**
     * Extracts aggregates from assignments, plan (rollup) aggregate nodes, return final projection assignments.
     */
    std::pair<PlanNodePtr, Assignments> planAggregate(
        PlanNodePtr plan,
        bool need_rollup,
        const std::vector<ASTPtr> & rollup_keys,
        const Assignments & assignments)
    {
        if (!need_rollup)
            return {plan->shared_from_this(), assignments};

        const auto & input_types = plan->getCurrentDataStream().header.getNamesAndTypes();

        Names keys;
        Assignments arguments;
        NameToType arguments_types;
        ASTToStringMap group_keys;
        for (const auto & key : rollup_keys)
        {
            auto new_symbol = context->getSymbolAllocator()->newSymbol(key);
            auto type = TypeAnalyzer::getType(key, context, input_types);
            arguments.emplace_back(new_symbol, key);
            group_keys.emplace(key, new_symbol);
            arguments_types.emplace(new_symbol, type);
            keys.emplace_back(new_symbol);
        }

        AggregateRewriter agg_rewriter{context, input_types, arguments, arguments_types};
        GroupByKeyRewrite key_rewriter{};
        Void c;
        Assignments rewrite_arguments;
        for (const auto & assignment : assignments)
        {
            // extract aggregates into aggregating node
            auto rewritten_expression = ASTVisitorUtil::accept(assignment.second->clone(), agg_rewriter, c);
            // rewrite remaining expression using group by keys
            rewritten_expression = ASTVisitorUtil::accept(rewritten_expression, key_rewriter, group_keys);
            rewrite_arguments.emplace_back(assignment.first, rewritten_expression);
        }

        if (!agg_rewriter.aggregates.empty() || !keys.empty())
        {
            if (!Utils::isIdentity(arguments))
                plan = PlanNodeBase::createPlanNode(
                    context->nextNodeId(),
                    std::make_shared<ProjectionStep>(plan->getCurrentDataStream(), arguments, arguments_types),
                    {plan});

            plan = PlanNodeBase::createPlanNode(
                context->nextNodeId(),
                std::make_shared<AggregatingStep>(plan->getCurrentDataStream(), keys, agg_rewriter.aggregates, GroupingSetsParamsList{}, true),
                {plan});
        }
        return std::make_pair(plan, rewrite_arguments);
    }

private:
    std::unordered_map<PlanNodePtr, RewriterCandidates> & match_results;

    class AggregateRewriter : public SimpleExpressionRewriter<Void>
    {
    public:
        AggregateRewriter(
            ContextMutablePtr context_, const NamesAndTypes & input_types_, Assignments & arguments_, NameToType & arguments_types_)
            : context(context_), input_types(input_types_), arguments(arguments_), arguments_types(arguments_types_)
        {
        }

        ASTPtr visitASTFunction(ASTPtr & node, Void & c) override
        {
            auto function = node->as<ASTFunction &>();
            if (!AggregateFunctionFactory::instance().isAggregateFunctionName(function.name))
                return visitNode(node, c);

            std::vector<String> agg_arguments;
            DataTypes agg_argument_types;
            for (auto & argument : function.arguments->children)
            {
                auto it = arguments_map.find(argument);
                if (it == arguments_map.end())
                {
                    auto new_symbol = context->getSymbolAllocator()->newSymbol(argument);
                    auto type = TypeAnalyzer::getType(argument, context, input_types);
                    it = arguments_map.emplace(argument, new_symbol).first;
                    arguments_types.emplace(new_symbol, type);
                    arguments.emplace_back(new_symbol, argument);
                }
                auto & name = it->second;
                agg_arguments.emplace_back(name);
                agg_argument_types.emplace_back(arguments_types.at(name));
            }
            Array parameters;
            for (auto & argument : function.parameters->children)
                parameters.emplace_back(argument->as<ASTLiteral &>().value);
            auto output_column = context->getSymbolAllocator()->newSymbol(node);
            AggregateDescription aggregate_description;
            AggregateFunctionProperties properties;
            aggregate_description.function = AggregateFunctionFactory::instance().get(
                function.name, agg_argument_types, parameters, properties);
            aggregate_description.argument_names = agg_arguments;
            aggregate_description.parameters = parameters;
            aggregate_description.column_name = output_column;
            aggregates.emplace_back(aggregate_description);
            return std::make_shared<ASTIdentifier>(output_column);
        }

        ContextMutablePtr context;
        const NamesAndTypes & input_types;
        Assignments & arguments;
        NameToType & arguments_types;

        std::unordered_map<ConstASTPtr, String, ASTEquality::ASTHash, ASTEquality::ASTEquals> arguments_map;
        AggregateDescriptions aggregates;
    };

    class GroupByKeyRewrite : public SimpleExpressionRewriter<ASTToStringMap>
    {
    public:
        ASTPtr visitNode(ASTPtr & node, ASTToStringMap & map) override
        {
            auto it = map.find(node);
            if (it != map.end())
                return std::make_shared<ASTIdentifier>(it->second);
            return SimpleExpressionRewriter::visitNode(node, map);
        }
    };
};
}

void MaterializedViewRewriter::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    if (!context->getSettingsRef().enable_materialized_view_rewrite)
        return;

    bool verbose = context->getSettingsRef().enable_materialized_view_rewrite_verbose_log;

    auto materialized_views = getRelatedMaterializedViews(plan, context);
    if (materialized_views.empty())
        return;

    auto candidates = CandidatesExplorer::explore(plan, context, materialized_views, verbose);
    if (candidates.empty())
        return;

    CostBasedMaterializedViewRewriter::rewrite(plan, context, candidates);
    // todo: add monitoring metrics
}

std::map<String, std::vector<MaterializedViewStructurePtr>>
MaterializedViewRewriter::getRelatedMaterializedViews(QueryPlan & plan, ContextMutablePtr context)
{
    std::map<String, std::vector<MaterializedViewStructurePtr>> table_based_mview_structures;
    auto & cache = MaterializedViewMemoryCache::instance();

    auto table_based_materialized_views = RelatedMaterializedViewExtractor::extract(plan, context);
    for (const auto & views : table_based_materialized_views)
    {
        std::vector<MaterializedViewStructurePtr> structures;
        for (const auto & view : views.second)
            if (auto structure = cache.getMaterializedViewStructure(view, context))
                structures.push_back(*structure);
        if (structures.empty())
            continue;
        table_based_mview_structures.emplace(views.first, std::move(structures));
    }
    return table_based_mview_structures;
}

}
