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

#include <Analyzers/ExprAnalyzer.h>
#include <Analyzers/TypeAnalyzer.h>
#include <Core/NamesAndTypes.h>
#include <Interpreters/Context_fwd.h>
#include <Parsers/IAST_fwd.h>
#include <boost/core/noncopyable.hpp>

namespace DB
{

using ExpressionTypes = std::unordered_map<ASTPtr, DataTypePtr>;

/**
 * AST type analyzer.
 *
 * Analyze and return the type of given expression.
 */
class TypeAnalyzer : boost::noncopyable
{
public:
    // WARNING: this can be slow
    // if you will use the same `input_types` to getType for many times
    // use the following instead
    // ```
    // auto analyzer = TypeAnalyzer::create(context, input_types);
    // for (...) {...; analyzer.getType(expr); ...;}
    // ```
    static DataTypePtr getType(const ConstASTPtr & expr, ContextMutablePtr context, const NamesAndTypes & input_types);

    static TypeAnalyzer create(ContextMutablePtr context, const NameToType & input_types);
    static TypeAnalyzer create(ContextMutablePtr context, const NamesAndTypes & input_types);
    DataTypePtr getType(const ConstASTPtr & expr) const;
    ExpressionTypes getExpressionTypes(const ConstASTPtr & expr) const;

private:
    TypeAnalyzer(ContextMutablePtr context_, Scope && scope_) : context(std::move(context_)), scope(std::move(scope_)) { }

    ContextMutablePtr context;
    Scope scope;
};


}
