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

#include <Optimizer/MaterializedView/MaterializedViewStructure.h>

#include <Optimizer/MaterializedView/MaterializeViewChecker.h>
#include <Optimizer/PredicateUtils.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR; // NOLINT
}

std::optional<MaterializedViewStructurePtr>
MaterializedViewStructure::buildFrom(StorageMaterializedView & view, PlanNodePtr & query, ContextMutablePtr context)
{
    static Poco::Logger * log = &Poco::Logger::get("MaterializedViewStructure");
    MaterializedViewPlanChecker checker;
    bool is_valid = checker.check(*query, context);
    if (!is_valid)
        return {};

    auto target_table = DatabaseCatalog::instance().tryGetTable(view.getTargetTableId(), context);
    if (!target_table)
    {
        LOG_WARNING(log, "materialized view target table not found.");
        return {};
    }

    auto top_aggregate_node = checker.getTopAggregateNode();

    JoinGraph join_graph = JoinGraph::build(top_aggregate_node ? top_aggregate_node->getChildren()[0] : query, context, true, false, true);

    // enable enable_materialized_view_join_rewriting, optimizer rewrite query use mview contains join.
    if (join_graph.getNodes().size() > 1 && !context->getSettingsRef().enable_materialized_view_join_rewriting)
    {
        LOG_DEBUG(log, "join is not supported for materialized view yet.");
        return {};
    }

    for (auto & node : join_graph.getNodes())
        if (node->getStep()->getType() != IQueryPlanStep::Type::TableScan)
            return {};

    std::vector<ConstASTPtr> other_predicates;
    if (top_aggregate_node)
    {
        auto having_predicates = JoinGraph::build(query, context, true, false, true).getFilters();
        other_predicates.insert(other_predicates.end(), having_predicates.begin(), having_predicates.end());
    }

    auto symbol_map = SymbolTransformMap::buildFrom(*query);
    if (!symbol_map)
        return {};

    ExpressionEquivalences expression_equivalences;
    auto predicates = PredicateUtils::extractEqualPredicates(join_graph.getFilters());
    for (const auto & predicate : predicates.first)
    {
        auto left_symbol_lineage = symbol_map->inlineReferences(predicate.first);
        auto right_symbol_lineage = symbol_map->inlineReferences(predicate.second);
        expression_equivalences.add(left_symbol_lineage, right_symbol_lineage);
    }

    // materialized view output may not be inconsistent with table (if create materialized view with optimizer disabled)
    std::unordered_map<String, String> output_columns_to_table_columns_map;
    std::unordered_set<String> output_columns;

    auto table_columns = target_table->getInMemoryMetadataPtr()->getColumns().getAllPhysical();
    if (table_columns.size() != query->getCurrentDataStream().header.columns())
    {
        LOG_WARNING(
            log,
            "size of materialized view physical columns is inconsistent with select outputs for " +
                view.getTargetTableId().getFullTableName());
        return {};
    }

    size_t index = 0;
    for (auto & table_column : table_columns)
    {
        auto & query_column = query->getCurrentDataStream().header.getByPosition(index);
        if (!removeLowCardinality(removeNullable(query_column.type))->equals(*removeLowCardinality(removeNullable(table_column.type))))
        {
            LOG_WARNING(
                log,
                "materialized view physical columns type is inconsistent with select outputs for column " + table_column.name + " in "
                    + view.getTargetTableId().getFullTableName());
            return {};
        }

        auto query_column_name = query->getCurrentDataStream().header.getByPosition(index++).name;
        output_columns.emplace(query_column_name);
        output_columns_to_table_columns_map.emplace(query_column_name, table_column.name);
    }

    std::shared_ptr<const AggregatingStep> aggregating_step = top_aggregate_node
        ? dynamic_pointer_cast<const AggregatingStep>(top_aggregate_node->getStep())
        : std::shared_ptr<const AggregatingStep>{};
    return std::make_shared<MaterializedViewStructure>(
        view.getStorageID(),
        view.getTargetTableId(),
        std::move(join_graph),
        std::move(other_predicates),
        std::move(*symbol_map),
        std::move(output_columns),
        std::move(output_columns_to_table_columns_map),
        std::move(expression_equivalences),
        std::move(aggregating_step));
}
}
