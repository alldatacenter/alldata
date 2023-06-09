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

#include <Interpreters/Aliases.h>
#include <Interpreters/InDepthNodeVisitor.h>

namespace DB
{

class ASTSelectWithUnionQuery;
class ASTSubquery;
class ASTSelectQuery;
struct ASTTableExpression;
struct ASTArrayJoin;

/// Visits AST node to collect alias --> vector of original column names.
class MultipleAliasMatcher
{
public:
    using Visitor = InDepthNodeVisitor<MultipleAliasMatcher, false>;

    struct Data
    {
        MultipleAliases & multiple_aliases;
    };

    static void visit(ASTPtr & ast, Data & data);
    static bool needChildVisit(ASTPtr & node, const ASTPtr & child);

private:
    static void visit(ASTSubquery & subquery, const ASTPtr & ast, Data & data);
    static void visit(const ASTArrayJoin &, const ASTPtr & ast, Data & data);
    static void visitOther(const ASTPtr & ast, Data & data);
    static void visitFilter(ASTPtr & filter);
    static void visit(ASTSelectQuery & select, const ASTPtr & ast, Data & data);
};

/// Visits AST nodes and collect their aliases in one map (with links to source nodes).
using MultipleAliasVisitor = MultipleAliasMatcher::Visitor;

}
