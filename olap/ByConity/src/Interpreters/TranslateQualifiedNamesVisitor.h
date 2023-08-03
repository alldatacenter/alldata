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

#include <vector>

#include <Core/Names.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/InDepthNodeVisitor.h>

namespace DB
{

class ASTIdentifier;
class ASTQualifiedAsterisk;
struct ASTTableJoin;
class ASTSelectQuery;
class ASTExpressionList;
class ASTFunction;

/// Visit one node for names qualification. @sa InDepthNodeVisitor.
class TranslateQualifiedNamesMatcher
{
public:
    using Visitor = InDepthNodeVisitor<TranslateQualifiedNamesMatcher, true>;

    struct Data
    {
        const NameSet source_columns;
        const TablesWithColumns & tables;
        std::unordered_set<String> join_using_columns;
        bool has_columns;

        Data(const NameSet & source_columns_, const TablesWithColumns & tables_, bool has_columns_ = true)
            : source_columns(source_columns_)
            , tables(tables_)
            , has_columns(has_columns_)
        {}

        bool hasColumn(const String & name) const { return source_columns.count(name); }
        bool hasTable() const { return !tables.empty(); }
        bool processAsterisks() const { return hasTable() && has_columns; }
        bool unknownColumn(size_t table_pos, const ASTIdentifier & identifier) const;
    };

    static void visit(ASTPtr & ast, Data & data);
    static bool needChildVisit(ASTPtr & node, const ASTPtr & child);

private:
    static void visit(ASTIdentifier & identifier, ASTPtr & ast, Data &);
    static void visit(const ASTQualifiedAsterisk & node, const ASTPtr & ast, Data &);
    static void visit(ASTTableJoin & join, const ASTPtr & ast, Data &);
    static void visit(ASTSelectQuery & select, const ASTPtr & ast, Data &);
    static void visit(ASTExpressionList &, const ASTPtr &, Data &);
    static void visit(ASTFunction &, const ASTPtr &, Data &);

    static void extractJoinUsingColumns(const ASTPtr ast, Data & data);
};

/// Visits AST for names qualification.
/// It finds columns and translate their names to the normal form. Expand asterisks and qualified asterisks with column names.
using TranslateQualifiedNamesVisitor = TranslateQualifiedNamesMatcher::Visitor;

/// Restore ASTTableExpressions to long form, change subquery table_name in case of distributed.
struct RestoreTableExpressionsMatcher
{
    struct Data
    {
        String database;
    };

    static bool needChildVisit(ASTPtr & node, const ASTPtr & child);
    static void visit(ASTPtr & ast, Data & data);
    static void visit(ASTTableExpression & expression, ASTPtr & ast, Data & data);
};

using RestoreTableExpressionsVisitor = InDepthNodeVisitor<RestoreTableExpressionsMatcher, true>;

/// Restore ASTIdentifiers to long form, change table name in case of distributed.
struct RestoreQualifiedNamesMatcher
{
    struct Data
    {
        DatabaseAndTableWithAlias distributed_table;
        DatabaseAndTableWithAlias remote_table;

        void changeTable(ASTIdentifier & identifier) const;
    };

    static bool needChildVisit(ASTPtr & node, const ASTPtr & child);
    static void visit(ASTPtr & ast, Data & data);
    static void visit(ASTIdentifier & identifier, ASTPtr & ast, Data & data);
};

using RestoreQualifiedNamesVisitor = InDepthNodeVisitor<RestoreQualifiedNamesMatcher, true>;

}
