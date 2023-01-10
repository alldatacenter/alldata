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

#include <Parsers/IAST_fwd.h>
#include <QueryPlan/PlanNode.h>

#include <unordered_map>
#include <optional>

namespace DB
{
/**
 * Used to determines the origin of identifier in expression.
 */
class SymbolTransformMap
{
public:
    static std::optional<SymbolTransformMap> buildFrom(PlanNodeBase & plan);
//    SymbolTransformMap(const SymbolTransformMap &) = default;
//    SymbolTransformMap & operator=(const SymbolTransformMap &) = default;
//    SymbolTransformMap(SymbolTransformMap &&) = default;
//    SymbolTransformMap & operator=(SymbolTransformMap &&) = default;

    ASTPtr inlineReferences(const ConstASTPtr & expression) const;
private:
    SymbolTransformMap(
        std::unordered_map<String, ConstASTPtr> symbol_to_expressions_,
        std::unordered_map<String, ConstASTPtr> symbol_to_cast_lossless_expressions_)
        : symbol_to_expressions(std::move(symbol_to_expressions_))
        , symbol_to_cast_lossless_expressions(std::move(symbol_to_cast_lossless_expressions_))
    {
    }

    std::unordered_map<String, ConstASTPtr> symbol_to_expressions;
    std::unordered_map<String, ConstASTPtr> symbol_to_cast_lossless_expressions;

    mutable std::unordered_map<String, ConstASTPtr> expression_lineage;

    class Visitor;
    class Rewriter;
};
}
