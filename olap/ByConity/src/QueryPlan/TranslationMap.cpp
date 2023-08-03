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

#include <Analyzers/function_utils.h>
#include <QueryPlan/TranslationMap.h>
#include <QueryPlan/planning_common.h>
#include <QueryPlan/Void.h>
#include <Parsers/ASTVisitor.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTFunction.h>

namespace DB
{

TranslationMap::TranslationMap(TranslationMapPtr outer_context_,
                               ScopePtr scope_,
                               FieldSymbolInfos field_symbol_infos_,
                               Analysis & analysis_,
                               ContextPtr context_) :
    analysis(analysis_),
    context(std::move(context_)),
    outer_context(std::move(outer_context_)),
    scope(scope_),
    field_symbol_infos(std::move(field_symbol_infos_)),
    expression_symbols(createScopeAwaredASTMap<String>(analysis))
{
    checkSymbols();
}

TranslationMap & TranslationMap::withScope(ScopePtr scope_, const FieldSymbolInfos & field_symbol_infos_, bool remove_mappings)
{
    scope = scope_;
    field_symbol_infos = field_symbol_infos_;

    checkSymbols();

    if (remove_mappings)
        expression_symbols.clear();

    return *this;
}

TranslationMap & TranslationMap::withNewMappings(const FieldSymbolInfos & field_symbol_infos_, const AstToSymbol & expression_symbols_)
{
    field_symbol_infos = field_symbol_infos_;
    expression_symbols = expression_symbols_;

    checkSymbols();

    return *this;
}

void TranslationMap::checkSymbols() const
{
    if (scope->getHierarchySize() != field_symbol_infos.size())
        throw Exception("TranslationMap Error: incorrect symbol size.", ErrorCodes::LOGICAL_ERROR);
}

void TranslationMap::checkFieldIndex(size_t field_index) const
{
    if (field_index >= field_symbol_infos.size())
        throw Exception("Field index is out of range.", ErrorCodes::LOGICAL_ERROR);
}

const FieldSymbolInfo & TranslationMap::getGlobalFieldSymbolInfo(const ResolvedField & resolved_field) const
{
    if (scope->isLocalScope(resolved_field.scope))
        return getFieldSymbolInfo(resolved_field.hierarchy_index);
    else if (outer_context)
        return outer_context->getGlobalFieldSymbolInfo(resolved_field);
    else
        throw Exception("Can not get global field symbol info", ErrorCodes::LOGICAL_ERROR);
}

const FieldSymbolInfo & TranslationMap::getFieldSymbolInfo(size_t field_index) const
{
    checkFieldIndex(field_index);
    return field_symbol_infos.at(field_index);
}

String TranslationMap::getFieldSymbol(size_t field_index) const
{
    checkFieldIndex(field_index);
    return field_symbol_infos.at(field_index).getPrimarySymbol();
}

// TODO: support type coercion
class TranslationMapVisitor : public ASTVisitor<ASTPtr, const Void>
{
public:
    ASTPtr visitASTLiteral(ASTPtr & node, const Void &) override;
    ASTPtr visitASTIdentifier(ASTPtr & node, const Void &) override;
    ASTPtr visitASTFieldReference(ASTPtr & node, const Void &) override;
    ASTPtr visitASTFunction(ASTPtr & node, const Void &) override;
    ASTPtr visitASTSubquery(ASTPtr & node, const Void &) override;
    ASTPtr visitASTQuantifiedComparison(ASTPtr & node, const Void &) override;

    TranslationMapVisitor(Analysis & analysis_, const TranslationMap & translation_map_)
        : analysis(analysis_),
        translation_map(translation_map_),
        use_legacy_column_name_of_tuple(translation_map.context->getSettingsRef().legacy_column_name_of_tuple_literal)
    {}

    ASTPtr process(ASTPtr & node) { return ASTVisitorUtil::accept(node, *this, {}); }

    ASTs process(ASTs & nodes)
    {
        ASTs result;

        for (auto & node: nodes)
        {
            result.push_back(process(node));
        }

        return result;
    }

private:
    Analysis & analysis;
    const TranslationMap & translation_map;
    const bool use_legacy_column_name_of_tuple;

    template<typename F>
    ASTPtr preferToUseMapped(ASTPtr & node, F && translate_func)
    {
        const auto & expression_symbols = translation_map.expression_symbols;

        if (expression_symbols.find(node) != expression_symbols.end())
            return toSymbolRef(expression_symbols.at(node));

        return translate_func(node);
    }

    ASTPtr handleColumnReference(const ResolvedField & column_reference, const ASTPtr & node);
    ASTPtr tryHandleSubColumnReference(const SubColumnReference & sub_column_reference, const ASTPtr & node);
};

ASTPtr TranslationMap::translate(ASTPtr expression) const
{
    TranslationMapVisitor visitor {analysis, *this};
    return visitor.process(expression);
}

String TranslationMap::translateToSymbol(const ASTPtr & expression) const
{
    if (canTranslateToSymbol(expression))
    {
        auto translated = translate(expression);

        if (auto * iden = translated->as<ASTIdentifier>())
            return iden->name();
    }

    throw Exception("Expression " + expression->getColumnName() + " can not be translated to symbol" , ErrorCodes::LOGICAL_ERROR);
}

bool TranslationMap::canTranslateToSymbol(const ASTPtr & expression) const
{
    if (expression_symbols.find(expression) != expression_symbols.end())
        return true;

    if (auto column_ref = analysis.tryGetColumnReference(expression))
        return scope->isLocalScope(column_ref->scope) || (outer_context && outer_context->canTranslateToSymbol(expression));

    if (auto sub_column_ref = analysis.tryGetSubColumnReference(expression))
    {
        if (scope->isLocalScope(sub_column_ref->getScope()))
        {
            auto field_index = sub_column_ref->getFieldHierarchyIndex();
            auto sub_column_id = sub_column_ref->getColumnID();
            return getFieldSymbolInfo(field_index).tryGetSubColumnSymbol(sub_column_id).has_value();
        }
        else
        {
            return outer_context && outer_context->canTranslateToSymbol(expression);
        }
    }

    return false;
}

ASTPtr TranslationMapVisitor::visitASTLiteral(ASTPtr & node, const Void &)
{
    return preferToUseMapped(node, [&](ASTPtr & lit) -> ASTPtr {
        auto & field = lit->as<ASTLiteral &>().value;
        auto rewritten_lit = std::make_shared<ASTLiteral>(field);

        if (use_legacy_column_name_of_tuple && field.getType() == Field::Types::Tuple)
            rewritten_lit->use_legacy_column_name_of_tuple = true;

        return rewritten_lit;
    });
}

ASTPtr TranslationMapVisitor::visitASTIdentifier(ASTPtr & node, const Void &)
{
    return preferToUseMapped(node, [&](ASTPtr & iden) -> ASTPtr {
        if (auto column_refer = analysis.tryGetColumnReference(iden))
            return handleColumnReference(*column_refer, iden);

        // lambda argument reference
        return std::make_shared<ASTIdentifier>(iden->as<ASTIdentifier &>().name());
    });
}

ASTPtr TranslationMapVisitor::visitASTFieldReference(ASTPtr & node, const Void &)
{
    return preferToUseMapped(node, [&](auto & field_ref) {
        auto column_refer = analysis.tryGetColumnReference(field_ref);

        if (!column_refer)
            throw Exception("Expression is not a column reference.", ErrorCodes::LOGICAL_ERROR);

        return handleColumnReference(*column_refer, field_ref);
    });
}

ASTPtr TranslationMapVisitor::visitASTSubquery(ASTPtr & node, const Void &)
{
    return preferToUseMapped(node, [](auto &) -> ASTPtr {
        throw Exception("Subqueries should be planned to symbols before translating", ErrorCodes::LOGICAL_ERROR);
    });
}

ASTPtr TranslationMapVisitor::visitASTFunction(ASTPtr & node, const Void &)
{
    return preferToUseMapped(node, [&](ASTPtr & func) -> ASTPtr {
        if (auto sub_col_ref = analysis.tryGetSubColumnReference(node))
        {
            if (auto rewritten_sub_col = tryHandleSubColumnReference(*sub_col_ref, node))
            {
                return rewritten_sub_col;
            }
        }

        auto & function = func->as<ASTFunction &>();
        auto & function_args = function.arguments->children;

        auto function_type = getFunctionType(function, translation_map.context);

        if (function_type == FunctionType::FUNCTION)
        {
            return makeASTFunction(function.name, process(function_args));
        }
        else if (function_type == FunctionType::LAMBDA_EXPRESSION)
        {
            return makeASTFunction("lambda", function_args.at(0), process(function_args.at(1)));
        }

        throw Exception("Ast should be planned to symbols before translating", ErrorCodes::LOGICAL_ERROR);
    });
}

ASTPtr TranslationMapVisitor::visitASTQuantifiedComparison(ASTPtr & node, const Void &)
{
    return preferToUseMapped(node, [&](ASTPtr & ) -> ASTPtr {
        throw Exception("Ast should be planned to symbols before translating", ErrorCodes::LOGICAL_ERROR);
    });
}

ASTPtr TranslationMapVisitor::handleColumnReference(const ResolvedField & column_reference, const ASTPtr & node)
{
    if (translation_map.scope->isLocalScope(column_reference.scope))
        return toSymbolRef(translation_map.getFieldSymbol(column_reference.hierarchy_index));
    else if (translation_map.outer_context)
        return translation_map.outer_context->translate(node);
    else
        throw Exception("Can not translate a column reference.", ErrorCodes::LOGICAL_ERROR);
}

ASTPtr TranslationMapVisitor::tryHandleSubColumnReference(const SubColumnReference & sub_column_reference, const ASTPtr & node)
{
    if (translation_map.scope->isLocalScope(sub_column_reference.getScope()))
    {
        const auto & field_symbol_info = translation_map.getFieldSymbolInfo(sub_column_reference.getFieldHierarchyIndex());
        auto sub_column_symbol = field_symbol_info.tryGetSubColumnSymbol(sub_column_reference.getColumnID());

        if (sub_column_symbol)
            return toSymbolRef(*sub_column_symbol);
        else
            return nullptr; // Note: sub column symbol may be invalidated by aggregate/join using/set operations.
    }
    else if (translation_map.outer_context)
        return translation_map.outer_context->translate(node);
    else
        throw Exception("Can not translate a sub column reference.", ErrorCodes::LOGICAL_ERROR);
}

}
