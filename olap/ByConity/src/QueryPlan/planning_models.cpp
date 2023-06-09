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

#include <QueryPlan/planning_models.h>

namespace DB
{

std::optional<String> FieldSymbolInfo::tryGetSubColumnSymbol(const SubColumnID & sub_column_id) const
{
    if (auto it = sub_column_symbols.find(sub_column_id); it != sub_column_symbols.end())
        return it->second;

    return std::nullopt;
}

RelationPlan RelationPlan::withNewRoot(PlanNodePtr new_root) const
{
    return {std::move(new_root), field_symbol_infos};
}

const String & RelationPlan::getFirstPrimarySymbol() const
{
    return field_symbol_infos.front().getPrimarySymbol();
}

}
