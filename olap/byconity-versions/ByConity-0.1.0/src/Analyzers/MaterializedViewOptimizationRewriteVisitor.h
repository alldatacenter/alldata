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

#include <Analyzers/ASTEquals.h>
#include <Analyzers/QueryAnalyzer.h>
#include <Analyzers/QueryRewriter.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeNullable.h>
#include <Interpreters/Context.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Optimizer/OptimizerMetrics.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Storages/StorageMaterializedView.h>

namespace DB
{
struct RewriteMaterializedViewOptimization
{
    using TypeToVisit = ASTSelectQuery;

    ContextMutablePtr context;
    explicit RewriteMaterializedViewOptimization(ContextMutablePtr & context_) : context(context_) { }

    void visit(ASTSelectQuery & query, ASTPtr & ast)
    {
        if (!context->getSettingsRef().enable_materialized_view_ast_rewrite)
            return;

        if (!valid(query))
            return;
        auto related_materialized_views = getRelatedMaterializedViews(query);
        for (const auto & materialized_view : related_materialized_views)
        {
            auto rewritten_query = rewrite(query, materialized_view);
            if (rewritten_query)
            {
                ast = std::move(rewritten_query);
                break;
            }
        }
    }

private:
    static bool valid(ASTSelectQuery & /* query */) { return true; }

    std::vector<StorageID> getRelatedMaterializedViews(ASTSelectQuery & query) const
    {
        std::vector<StorageID> materialized_views;

        auto table_expressions = extractTableExpressions(query);
        for (auto & table_expression : table_expressions)
        {
            if (table_expression->database_and_table_name)
            {
                auto storage_id = context->tryResolveStorageID(table_expression->database_and_table_name);
                auto dependencies = DatabaseCatalog::instance().getDependencies(storage_id);
                materialized_views.insert(materialized_views.end(), dependencies.begin(), dependencies.end());
            }
        }
        return materialized_views;
    }

    static std::vector<const ASTTableExpression *> extractTableExpressions(ASTSelectQuery & query)
    {
        std::vector<const ASTTableExpression *> tables_expression;

        std::stack<ASTPtr> stack;
        stack.push(query.shared_from_this());
        while (!stack.empty())
        {
            auto frame = stack.top();
            stack.pop();

            if (const auto * tables_element = frame->as<ASTTablesInSelectQueryElement>())
                if (tables_element->table_expression)
                    tables_expression.emplace_back(tables_element->table_expression->as<ASTTableExpression>());

            for (auto & child : frame->children)
                stack.push(child);
        }
        return tables_expression;
    }

    ASTPtr rewrite(ASTSelectQuery & query, const StorageID & materialized_view_name)
    {
        auto table = DatabaseCatalog::instance().tryGetTable(materialized_view_name, context);
        if (!table)
            return {};
        auto materialized_view = dynamic_pointer_cast<StorageMaterializedView>(table);
        if (!materialized_view)
            return {};
        auto target_table = DatabaseCatalog::instance().tryGetTable(materialized_view->getTargetTableId(), context);
        if (!target_table)
            return {};

        auto materialized_view_ast = QueryRewriter::rewrite(materialized_view->getInnerQuery(), context, false);
        AnalysisPtr analysis = QueryAnalyzer::analyze(materialized_view_ast, context);
        if (!analysis->non_deterministic_functions.empty())
            return {};

        if (!ASTEquality::ASTEquals()(query.shared_from_this(), materialized_view_ast))
            return {};

        auto select = std::make_shared<ASTSelectQuery>();

        auto tables_list = std::make_shared<ASTTablesInSelectQuery>();
        auto table_expr = std::make_shared<ASTTableExpression>();
        auto element = std::make_shared<ASTTablesInSelectQueryElement>();
        auto table_ast = std::make_shared<ASTTableIdentifier>(materialized_view->getTargetTableId());
        table_expr->database_and_table_name = table_ast;
        table_expr->children.push_back(table_ast);
        element->table_expression = table_expr;
        element->children.push_back(table_expr);
        tables_list->children.push_back(element);
        select->setExpression(ASTSelectQuery::Expression::TABLES, tables_list);

        auto select_expression_list = std::make_shared<ASTExpressionList>();
        auto table_columns = target_table->getInMemoryMetadataPtr()->getColumns().getAllPhysical();
        auto query_columns = query.getSelect()->as<ASTExpressionList &>().children;
        if (query_columns.size() != table_columns.size())
            return {};
        size_t index = 0;
        for (auto & table_column : table_columns)
        {
            auto identifier = std::make_shared<ASTIdentifier>(table_column.name);
            identifier->setAlias(query_columns[index++]->getAliasOrColumnName());
            select_expression_list->children.emplace_back(identifier);
        }
        select->setExpression(ASTSelectQuery::Expression::SELECT, select_expression_list);

        context->getOptimizerMetrics()->addMaterializedView(materialized_view_name);

        return select;
    }
};

using MaterializedViewOptimizationRewriteMatcher = OneTypeMatcher<RewriteMaterializedViewOptimization>;
using MaterializedViewOptimizationRewriteVisitor = InDepthNodeVisitor<MaterializedViewOptimizationRewriteMatcher, true>;
}
