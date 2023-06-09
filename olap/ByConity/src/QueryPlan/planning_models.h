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

#include <Core/Types.h>
#include <Analyzers/Analysis.h>
#include <Analyzers/SubColumnID.h>
#include <QueryPlan/PlanNode.h>

namespace DB
{

struct FieldSymbolInfo
{
    using SubColumnToSymbol = std::unordered_map<SubColumnID, String, SubColumnID::Hash>;

    String primary_symbol;
    SubColumnToSymbol sub_column_symbols;

    FieldSymbolInfo(String primary_symbol_ = ""): primary_symbol(std::move(primary_symbol_)) // NOLINT(google-explicit-constructor)
    {}

    FieldSymbolInfo(String primary_symbol_, SubColumnToSymbol sub_column_symbols_)
        : primary_symbol(std::move(primary_symbol_)), sub_column_symbols(std::move(sub_column_symbols_))
    {}

    const String & getPrimarySymbol() const
    {
        return primary_symbol;
    }

    std::optional<String> tryGetSubColumnSymbol(const SubColumnID & sub_column_id) const;
};

using FieldSymbolInfos = std::vector<FieldSymbolInfo>;

struct RelationPlan
{
    PlanNodePtr root;
    FieldSymbolInfos field_symbol_infos;

    RelationPlan() = default;
    RelationPlan(PlanNodePtr root_, FieldSymbolInfos field_symbol_infos_)
        : root(std::move(root_)), field_symbol_infos(std::move(field_symbol_infos_))
    {
    }

    RelationPlan withNewRoot(PlanNodePtr new_root) const;
    PlanNodePtr getRoot() const { return root; }
    const FieldSymbolInfos & getFieldSymbolInfos() const { return field_symbol_infos; }
    const String & getFirstPrimarySymbol() const;
};

using RelationPlans = std::vector<RelationPlan>;

using CTERelationPlans = std::unordered_map<CTEId, RelationPlan>;
}
