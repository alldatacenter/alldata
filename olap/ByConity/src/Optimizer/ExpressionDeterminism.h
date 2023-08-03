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

#include <Interpreters/Context.h>
#include <Parsers/ASTVisitor.h>
#include <Parsers/IAST.h>
#include <QueryPlan/Assignment.h>

namespace DB
{
class ExpressionDeterminism
{
public:
    static std::set<String> getDeterministicSymbols(Assignments & assignments, ContextPtr context);
    static ConstASTPtr filterDeterministicConjuncts(ConstASTPtr predicate, ContextPtr context);
    static ConstASTPtr filterNonDeterministicConjuncts(ConstASTPtr predicate, ContextPtr context);
    static std::set<ConstASTPtr> filterDeterministicPredicates(std::vector<ConstASTPtr> & predicates, ContextPtr context);
    static bool isDeterministic(ConstASTPtr expression, ContextPtr context);
    static bool canChangeOutputRows(ConstASTPtr expression, ContextPtr context);

    struct ExpressionProperty
    {
        bool is_deterministic;
        bool can_change_output_rows;
    };

private:
    static ExpressionProperty getExpressionProperty(ConstASTPtr expression, ContextPtr context);
};

class DeterminismVisitor : public ConstASTVisitor<Void, ContextPtr>
{
public:
    explicit DeterminismVisitor(bool isDeterministic);
    Void visitNode(const ConstASTPtr & node, ContextPtr & context) override;
    Void visitASTFunction(const ConstASTPtr & node, ContextPtr & context) override;
    bool isDeterministic() const { return is_deterministic; }
    bool canChangeOutputRows() const { return can_change_output_rows; }

private:
    bool is_deterministic;
    bool can_change_output_rows = false;
};

}
