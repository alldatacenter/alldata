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

#include <Analyzers/Analysis.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/SymbolAllocator.h>
#include <QueryPlan/TranslationMap.h>

namespace DB
{

struct PlanBuilder
{
    Analysis & analysis;
    PlanNodeIdAllocatorPtr id_allocator;
    SymbolAllocatorPtr symbol_allocator;
    PlanNodePtr plan;
    TranslationMapPtr translation;

    PlanBuilder(
        Analysis & analysis_,
        PlanNodeIdAllocatorPtr id_allocator_,
        SymbolAllocatorPtr symbol_allocator_,
        PlanNodePtr plan_,
        TranslationMapPtr translation_map_)
        : analysis(analysis_)
        , id_allocator(std::move(id_allocator_))
        , symbol_allocator(std::move(symbol_allocator_))
        , plan(std::move(plan_))
        , translation(std::move(translation_map_))
    {
    }

    PlanNodePtr getRoot() { return plan; }
    TranslationMapPtr getTranslation() { return translation; }

    // delegation methods
    void addStep(QueryPlanStepPtr step, PlanNodes children = {});
    void withNewRoot(const PlanNodePtr & new_root) { plan = new_root; }
    const DataStream & getCurrentDataStream() const { return plan->getCurrentDataStream(); }
    Names getOutputNames() const { return plan->getOutputNames(); }
    NamesAndTypes getOutputNamesAndTypes() const { return plan->getOutputNamesAndTypes(); }
    NameToType getOutputNamesToTypes() const { return plan->getOutputNamesToTypes(); }
    String getFieldSymbol(size_t index) const { return translation->getFieldSymbol(index); }
    const FieldSymbolInfo & getFieldSymbolInfo(size_t index) const { return translation->getFieldSymbolInfo(index); }
    const FieldSymbolInfo & getGlobalFieldSymbolInfo(const ResolvedField & resolved_field) const
    {
        return translation->getGlobalFieldSymbolInfo(resolved_field);
    }
    const FieldSymbolInfos & getFieldSymbolInfos() const { return translation->field_symbol_infos; }
    ASTPtr translate(const ASTPtr & expr) const { return translation->translate(expr); }
    bool canTranslateToSymbol(const ASTPtr & expr) const { return translation->canTranslateToSymbol(expr); }
    String translateToSymbol(const ASTPtr & expr) const { return translation->translateToSymbol(expr); }
    ScopePtr getScope() const { return translation->scope; }
    bool isLocalScope(ScopePtr other) const { return translation->isLocalScope(other); }
    bool isCalculatedExpression(const ASTPtr & expression) const { return translation->isCalculatedExpression(expression); }
    TranslationMap & withScope(ScopePtr scope, const FieldSymbolInfos & field_symbol_infos, bool remove_mappings = true) // NOLINT(readability-make-member-function-const)
    {
        return translation->withScope(scope, field_symbol_infos, remove_mappings);
    }
    TranslationMap & withNewMappings(const FieldSymbolInfos & field_symbol_infos, const AstToSymbol & expression_symbols)
    {
        return translation->withNewMappings(field_symbol_infos, expression_symbols);
    }
    TranslationMap & withNewMappings(const FieldSymbolInfos & field_symbol_infos)
    {
        return translation->withNewMappings(field_symbol_infos, createScopeAwaredASTMap<String>(analysis));
    }
    TranslationMap & removeMappings() { return translation->removeMappings(); } // NOLINT(readability-make-member-function-const)
    TranslationMap & withAdditionalMapping(const ASTPtr & expression, const String & symbol) // NOLINT(readability-make-member-function-const)
    {
        return translation->withAdditionalMapping(expression, symbol);
    }
    TranslationMap & withAdditionalMappings(const AstToSymbol & expression_symbols) // NOLINT(readability-make-member-function-const)
    {
        return translation->withAdditionalMappings(expression_symbols);
    }

    // utils
    Names translateToSymbols(ASTs & expressions) const;
    Names translateToUniqueSymbols(ASTs & expressions) const;
    void appendProjection(ASTs & expressions);
    void appendProjection(const ASTPtr & expression)
    {
        ASTs list{expression};
        appendProjection(list);
    }
};

}
