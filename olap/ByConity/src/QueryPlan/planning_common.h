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
#include <Core/NamesAndTypes.h>
#include <Core/Block.h>
#include <Parsers/IAST_fwd.h>
#include <Analyzers/ScopeAwareEquals.h>
#include <QueryPlan/Assignment.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/planning_models.h>
#include <Optimizer/Utils.h>

namespace DB
{

template <typename Container1, typename Container2>
void append(Container1 & v1, Container2 && v2)
{
    v1.reserve(v1.size() + v2.size());
    v1.insert(v1.end(), v2.begin(), v2.end());
}

template<typename T>
struct is_vector : public std::false_type {}; // NOLINT(readability-identifier-naming)

template<typename T, typename A>
struct is_vector<std::vector<T, A>> : public std::true_type {};

template <typename Container1, typename Container2, typename F>
void append(Container1 & v1, Container2 && v2, F && transformer)
{
    v1.reserve(v1.size() + v2.size());

    if constexpr(is_vector<Container1>::value){
        std::transform(v2.begin(), v2.end(), std::back_inserter(v1), transformer);
    }
    else {
        std::transform(v2.begin(), v2.end(), std::inserter(v1, v1.end()), transformer);
    }
}

template <typename Container, typename F>
Container deduplicateByAst(Container container, Analysis & analysis, F && extractor)
{
    auto existing = createScopeAwaredASTSet(analysis);
    container.erase(
        std::remove_if(container.begin(), container.end(), [&] (auto & elem) -> bool
                       {
                           return !existing.insert(extractor(elem)).second;
                       }),
        container.end());
    return container;
}

inline ASTPtr toSymbolRef(const String & symbol_name)
{
    return std::make_shared<ASTIdentifier>(symbol_name);
}

void putIdentities(const NamesAndTypes & columns, Assignments & assignments, NameToType & types);

inline void putIdentities(const Block & block, Assignments & assignments, NameToType & types)
{
    putIdentities(block.getNamesAndTypes(), assignments, types);
}

FieldSymbolInfos mapSymbols(const FieldSymbolInfos & field_symbol_infos, const std::unordered_map<String, String> & old_to_new);

}
