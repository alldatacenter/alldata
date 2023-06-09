/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/Aliases.h>

namespace DB
{

class ASTSelectQuery;
class Context;

/// AST transformer. It replaces multiple joins to (subselect + join) track.
/// 'select * from t1 join t2 on ... join t3 on ... join t4 on ...' would be rewritten with
/// 'select * from (select * from t1 join t2 on ...) join t3 on ...) join t4 on ...'
class JoinToSubqueryTransformMatcher
{
public:
    struct Data
    {
        const std::vector<TableWithColumnNamesAndTypes> & tables;
        enum DialectType dialect_type;
        const Aliases & aliases;
        bool done = false;
    };

    static bool needChildVisit(ASTPtr &, const ASTPtr &);
    static void visit(ASTPtr & ast, Data & data);

private:
    /// - combines two source TablesInSelectQueryElement into resulting one (Subquery)
    /// - adds table hidings to ASTSelectQuery.with_expression_list
    ///
    /// TablesInSelectQueryElement [result]
    ///  TableExpression
    ///   Subquery (alias __join1)
    ///    SelectWithUnionQuery
    ///      ExpressionList
    ///       SelectQuery
    ///        ExpressionList
    ///         Asterisk
    ///        TablesInSelectQuery
    ///         TablesInSelectQueryElement [source1]
    ///         TablesInSelectQueryElement [source2]
    ///
    static void visit(ASTSelectQuery & select, ASTPtr & ast, Data & data);

    /// @return combined TablesInSelectQueryElement or nullptr if cannot rewrite
    static ASTPtr replaceJoin(ASTPtr left, ASTPtr right, ASTPtr subquery_template);
};

using JoinToSubqueryTransformVisitor = InDepthNodeVisitor<JoinToSubqueryTransformMatcher, true>;

}
