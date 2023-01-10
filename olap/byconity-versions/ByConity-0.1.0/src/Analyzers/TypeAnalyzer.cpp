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

#include <Analyzers/TypeAnalyzer.h>
#include <Analyzers/ExprAnalyzer.h>
#include <Analyzers/Analysis.h>

namespace DB
{

DataTypePtr TypeAnalyzer::getType(const ConstASTPtr & expr, ContextMutablePtr context, const NamesAndTypes & input_types)
{
    return TypeAnalyzer::create(context, input_types).getType(expr);
}

TypeAnalyzer TypeAnalyzer::create(ContextMutablePtr context, const NameToType & input_types)
{
    NamesAndTypes names_and_types;

    for (const auto & [name, type]: input_types)
        names_and_types.emplace_back(name, type);

    return create(context, names_and_types);
}

TypeAnalyzer TypeAnalyzer::create(ContextMutablePtr context, const NamesAndTypes & input_types)
{
    FieldDescriptions fields;
    for(const auto & input : input_types) {
        FieldDescription field {input.name, input.type};
        fields.emplace_back(field);
    }
    Scope scope(Scope::ScopeType::RELATION, nullptr, true, fields);
    return TypeAnalyzer(context, std::move(scope));
}

#define REMOVE_CONST(const_ptr) (std::const_pointer_cast<IAST>(const_ptr))

DataTypePtr TypeAnalyzer::getType(const ConstASTPtr & expr) const
{
    Analysis analysis;
    return ExprAnalyzer::analyze(REMOVE_CONST(expr), &scope, context, analysis);
}

ExpressionTypes TypeAnalyzer::getExpressionTypes(const ConstASTPtr & expr) const
{
    Analysis analysis;
    ExprAnalyzer::analyze(REMOVE_CONST(expr), &scope, context, analysis);
    return std::move(analysis.expression_types);
}

}
