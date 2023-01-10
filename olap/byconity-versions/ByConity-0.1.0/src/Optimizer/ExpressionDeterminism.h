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
    static std::set<String> getDeterministicSymbols(Assignments & assignments, ContextMutablePtr & context);
    static ConstASTPtr filterDeterministicConjuncts(ConstASTPtr predicate, ContextMutablePtr & context);
    static ConstASTPtr filterNonDeterministicConjuncts(ConstASTPtr predicate, ContextMutablePtr & context);
    static std::set<ConstASTPtr> filterDeterministicPredicates(std::vector<ConstASTPtr> & predicates, ContextMutablePtr & context);
    static bool isDeterministic(ConstASTPtr expression, ContextMutablePtr & context);
};

class DeterminismVisitor : public ConstASTVisitor<Void, ContextMutablePtr>
{
public:
    explicit DeterminismVisitor(bool isDeterministic);
    Void visitNode(const ConstASTPtr & node, ContextMutablePtr & context) override;
    Void visitASTFunction(const ConstASTPtr & node, ContextMutablePtr & context) override;
    bool isDeterministic() const { return is_deterministic; }

private:
    bool is_deterministic;
};

}
