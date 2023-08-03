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

#include <Core/Names.h>
#include <Parsers/ASTFunction.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/Aliases.h>


namespace DB
{

class ASTIdentifier;
class TableJoin;

namespace ASOF
{
    enum class Inequality;
}

class CollectJoinOnKeysMatcher
{
public:
    using Visitor = ConstInDepthNodeVisitor<CollectJoinOnKeysMatcher, true>;

    struct Data
    {
        TableJoin & analyzed_join;
        const TableWithColumnNamesAndTypes & left_table;
        const TableWithColumnNamesAndTypes & right_table;
        const Aliases & aliases;
        const bool is_asof{false};
        bool is_nest_loop_join{false};
        ASTPtr asof_left_key{};
        ASTPtr asof_right_key{};
        bool has_some{false};

        void addJoinKeys(const ASTPtr & left_ast, const ASTPtr & right_ast, const std::pair<size_t, size_t> & table_no);
        void addAsofJoinKeys(const ASTPtr & left_ast, const ASTPtr & right_ast, const std::pair<size_t, size_t> & table_no,
                             const ASOF::Inequality & asof_inequality);
        void asofToJoinKeys();
    };

    static void visit(const ASTPtr & ast, Data & data)
    {
        if (auto * func = ast->as<ASTFunction>())
            visit(*func, ast, data);
    }

    static bool needChildVisit(const ASTPtr & node, const ASTPtr &)
    {
        if (auto * func = node->as<ASTFunction>())
            return func->name == "and";
        return true;
    }

private:
    static void visit(const ASTFunction & func, const ASTPtr & ast, Data & data);

    static void getIdentifiers(const ASTPtr & ast, std::vector<const ASTIdentifier *> & out);
    static std::pair<size_t, size_t> getTableNumbers(const ASTPtr & expr, const ASTPtr & left_ast, const ASTPtr & right_ast, Data & data);
    static const ASTIdentifier * unrollAliases(const ASTIdentifier * identifier, const Aliases & aliases);
    static size_t getTableForIdentifiers(std::vector<const ASTIdentifier *> & identifiers, const Data & data);
};

/// Parse JOIN ON expression and collect ASTs for joined columns.
using CollectJoinOnKeysVisitor = CollectJoinOnKeysMatcher::Visitor;

}
