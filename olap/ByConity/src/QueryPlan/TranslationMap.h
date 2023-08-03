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

#include <Analyzers/Scope.h>
#include <Analyzers/Analysis.h>
#include <Analyzers/ScopeAwareEquals.h>
#include <QueryPlan/planning_models.h>

#include <unordered_map>
#include <memory>
#include <utility>
#include <vector>

namespace DB
{

using AstToSymbol = ScopeAwaredASTMap<String>;
struct TranslationMap;
using TranslationMapPtr = std::shared_ptr<TranslationMap>;

/// TranslationMap maintains the mapping of
///   1. a FieldDescription to its symbol name
///   2. a calculated expression to its symbol name
struct TranslationMap
{
    Analysis & analysis;
    ContextPtr context;
    TranslationMapPtr outer_context;
    ScopePtr scope;

    // for each field of a scope, track its symbol name
    FieldSymbolInfos field_symbol_infos;

    // for each calculated expression, track its symbol name
    // TODO: use ScopeAware
    AstToSymbol expression_symbols;

    TranslationMap(TranslationMapPtr outer_context_,
                   ScopePtr scope_,
                   FieldSymbolInfos field_symbol_infos_,
                   Analysis & analysis_,
                   ContextPtr context_);

    TranslationMap & withScope(ScopePtr scope_, const FieldSymbolInfos & field_symbol_infos_, bool remove_mappings = true);

    TranslationMap & withNewMappings(const FieldSymbolInfos & field_symbol_infos_, const AstToSymbol & expression_symbols_);

    TranslationMap & removeMappings()
    {
        expression_symbols.clear();
        return *this;
    }

    TranslationMap & withAdditionalMapping(const ASTPtr & expression, const String & symbol)
    {
        expression_symbols.emplace(expression, symbol);
        return *this;
    }

    TranslationMap & withAdditionalMappings(const AstToSymbol & expression_symbols_)
    {
        expression_symbols.insert(expression_symbols_.begin(), expression_symbols_.end());
        return *this;
    }

    void checkSymbols() const;
    void checkFieldIndex(size_t field_index) const;

    const FieldSymbolInfo & getGlobalFieldSymbolInfo(const ResolvedField & resolved_field) const;
    const FieldSymbolInfo & getFieldSymbolInfo(size_t field_index) const;
    String getFieldSymbol(size_t field_index) const;

    bool isLocalScope(ScopePtr other) const {return scope->isLocalScope(other);}
    bool isCalculatedExpression(const ASTPtr & expression) const
    {
        return expression_symbols.find(expression) != expression_symbols.end();
    }
    ASTPtr translate(ASTPtr expression) const;
    String translateToSymbol(const ASTPtr & expression) const;
    bool canTranslateToSymbol(const ASTPtr & expression) const;
};

}
