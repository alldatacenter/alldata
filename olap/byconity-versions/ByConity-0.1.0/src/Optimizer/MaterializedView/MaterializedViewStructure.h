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

#include <Analyzers/ASTEquals.h>
#include <Optimizer/JoinGraph.h>
#include <Optimizer/SymbolTransformMap.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/QueryPlan.h>
#include <Storages/StorageMaterializedView.h>

namespace DB
{
struct MaterializedViewStructure;
using MaterializedViewStructurePtr = std::shared_ptr<MaterializedViewStructure>;

using ExpressionEquivalences = Equivalences<ConstASTPtr, ASTEquality::ASTHash, ASTEquality::ASTEquals>;

/**
 * Structural information extracted from materialized view, used for materialized view rewriting.
 */
struct MaterializedViewStructure
{
    /**
     * @param view storage of materialized view
     * @param query create table query statement after optimizer RBO ruls of materialized view
     * @return structure info if it support materialized view rewriting, empty otherwise.
     */
    static std::optional<MaterializedViewStructurePtr> buildFrom(
        StorageMaterializedView & view, PlanNodePtr & query, ContextMutablePtr context);

    const StorageID view_storage_id;
    const StorageID target_storage_id;

    const JoinGraph join_graph;
    const std::vector<ConstASTPtr> other_predicates;
    const SymbolTransformMap symbol_map;
    const std::unordered_set<String> output_columns;
    const std::unordered_map<String, String> output_columns_to_table_columns_map;
    const ExpressionEquivalences expression_equivalences;
    const std::shared_ptr<const AggregatingStep> top_aggregating_step;

    MaterializedViewStructure(
        StorageID view_storage_id_,
        StorageID target_storage_id_,
        JoinGraph join_graph_,
        std::vector<ConstASTPtr> other_predicates_,
        SymbolTransformMap symbol_map_,
        std::unordered_set<String> output_columns_,
        std::unordered_map<String, String> output_columns_to_table_columns_map_,
        ExpressionEquivalences expression_equivalences_,
        std::shared_ptr<const AggregatingStep> top_aggregating_step_)
        : view_storage_id(std::move(view_storage_id_))
        , target_storage_id(std::move(target_storage_id_))
        , join_graph(std::move(join_graph_))
        , other_predicates(std::move(other_predicates_))
        , symbol_map(std::move(symbol_map_))
        , output_columns(std::move(output_columns_))
        , output_columns_to_table_columns_map(std::move(output_columns_to_table_columns_map_))
        , expression_equivalences(std::move(expression_equivalences_))
        , top_aggregating_step(std::move(top_aggregating_step_))
    {
    }
};
}
