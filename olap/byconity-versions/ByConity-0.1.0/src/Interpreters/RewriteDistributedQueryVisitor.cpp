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

#include <Interpreters/RewriteDistributedQueryVisitor.h>
#include <Interpreters/IdentifierSemantic.h>
#include <Interpreters/StorageID.h>
#include <Interpreters/Context.h>
#include <Interpreters/Cluster.h>

#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTQualifiedAsterisk.h>
#include <Parsers/queryToString.h>

#include <Storages/StorageDistributed.h>

#include <queue>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

StoragePtr RewriteDistributedQueryMatcher::tryGetTable(const ASTPtr & database_and_table, const ContextPtr & context)
{
    auto table_id = context->tryResolveStorageID(database_and_table);
    if (!table_id)
        return {};
    return DatabaseCatalog::instance().tryGetTable(table_id, context);
}

RewriteDistributedQueryMatcher::Data RewriteDistributedQueryMatcher::collectTableInfos(const ASTPtr & query, const ContextPtr & context)
{
    RewriteDistributedQueryMatcher::Data result_data;

    ASTs tables;
    std::queue<ASTPtr> q;
    q.push(query);
    while (!q.empty())
    {
        auto node = q.front();
        for (auto & child : node->children)
            q.push(child);

        if (const ASTTableExpression * table = node->as<ASTTableExpression>())
        {
            if (table->database_and_table_name)
                tables.push_back(table->database_and_table_name);
        }

        q.pop();
    }


    for (auto & table : tables)
    {
        auto storage = tryGetTable(table, context);
        auto distributed_table = dynamic_cast<StorageDistributed *>(storage.get());
        if (distributed_table)
        {
            result_data.table_rewrite_info[table.get()] = {distributed_table->getRemoteDatabaseName(), distributed_table->getRemoteTableName()};

            auto * identifier = table->as<ASTTableIdentifier>();
            DatabaseAndTableWithAlias database_table(*identifier, context->getCurrentDatabase());
            result_data.identifier_rewrite_info.emplace_back(database_table, distributed_table->getRemoteTableName());

            if (!result_data.cluster)
            {
                result_data.cluster = distributed_table->getCluster();
                result_data.cluster_name = distributed_table->getClusterName();
            }
            else
            {
                if (result_data.cluster != distributed_table->getCluster())
                    throw Exception("Cannot rewrite distributed query with multiple different cluster", ErrorCodes::LOGICAL_ERROR);
            }
        }
        else
        {
            result_data.all_distributed = false;
        }
    }

    return result_data;
}

bool RewriteDistributedQueryMatcher::needChildVisit(ASTPtr & node, const ASTPtr &)
{
    if (auto * table_expression = node->as<ASTTableExpression>())
    {
        if (table_expression->database_and_table_name)
            return false;
    }
    return true;
}

void RewriteDistributedQueryMatcher::visit(ASTPtr & ast, Data & data)
{
    if (auto * node = ast->as<ASTTableExpression>())
        visit(*node, ast, data);
    if (auto * node = ast->as<ASTIdentifier>())
        visit(*node, ast, data);
    if (auto * node = ast->as<ASTQualifiedAsterisk>())
        visit(*node, ast, data);
}

void RewriteDistributedQueryMatcher::visit(ASTTableExpression & table_expression, ASTPtr &, Data & data)
{
    if (table_expression.database_and_table_name)
    {
        auto it = data.table_rewrite_info.find(table_expression.database_and_table_name.get());
        if (it != data.table_rewrite_info.end())
        {
            auto * table_identifier = table_expression.database_and_table_name->as<ASTTableIdentifier>();
            table_identifier->resetTable(it->second.first, it->second.second);
        }
    }
}

/**
 * Rewrite column if we find it has a table reference.
 * for example:
 * select a.id from a,
 * after we rewrite sql (perfectshardable), it will be
 * select a_local.id from a_local.
 */

void RewriteDistributedQueryMatcher::visit(ASTIdentifier & identifier, ASTPtr &, Data & data)
{
    for (auto & rewrite_info : data.identifier_rewrite_info)
    {
        auto match = IdentifierSemantic::canReferColumnToTable(identifier, rewrite_info.first);
        if (match == IdentifierSemantic::ColumnMatch::DbAndTable
            || match == IdentifierSemantic::ColumnMatch::AliasedTableName
            || match == IdentifierSemantic::ColumnMatch::TableName)
        {
            IdentifierSemantic::setColumnTableName(identifier, rewrite_info.second);
            break;
        }
    }
}

void RewriteDistributedQueryMatcher::visit(ASTQualifiedAsterisk & qualified_asterisk, ASTPtr &, Data & data)
{
    ASTTableIdentifier & identifier = *qualified_asterisk.children[0]->as<ASTTableIdentifier>();
    for (auto & rewrite_info : data.identifier_rewrite_info)
    {
        auto match = IdentifierSemantic::canReferColumnToTable(identifier, rewrite_info.first);
        if (match == IdentifierSemantic::ColumnMatch::DbAndTable
            || match == IdentifierSemantic::ColumnMatch::AliasedTableName
            || match == IdentifierSemantic::ColumnMatch::TableName)
        {
            IdentifierSemantic::setColumnTableName(identifier, rewrite_info.second);
            break;
        }
    }
}
}
