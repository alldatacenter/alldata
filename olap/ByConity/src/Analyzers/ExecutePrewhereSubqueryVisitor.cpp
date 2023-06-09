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

#include <Analyzers/ExecutePrewhereSubqueryVisitor.h>
#include <DataStreams/BlockIO.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTSubquery.h>
#include <Interpreters/InterpreterFactory.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INCORRECT_RESULT_OF_SCALAR_SUBQUERY;
    extern const int TOO_MANY_ROWS;
}

static ASTPtr addTypeConversion(std::unique_ptr<ASTLiteral> && ast, const String & type_name)
{
    auto func = std::make_shared<ASTFunction>();
    ASTPtr res = func;
    func->alias = ast->alias;
    func->prefer_alias_to_column_name = ast->prefer_alias_to_column_name;
    ast->alias.clear();
    func->name = "CAST";
    auto exp_list = std::make_shared<ASTExpressionList>();
    func->arguments = exp_list;
    func->children.push_back(func->arguments);
    exp_list->children.emplace_back(ast.release());
    exp_list->children.emplace_back(std::make_shared<ASTLiteral>(type_name));
    return res;
}

void ExecutePrewhereSubquery::visit(ASTSubquery & subquery, ASTPtr & ast) const
{
    ContextMutablePtr subquery_context = Context::createCopy(context);
    Settings subquery_settings = context->getSettings();
    subquery_settings.max_result_rows = 1;
    subquery_settings.extremes = false;
    // internal SQL doesn't work well in optimizer mode, mainly due to PlanSegmentExecutor
    subquery_settings.enable_optimizer = false;
    subquery_context->setSettings(subquery_settings);

    ASTPtr subquery_select = subquery.children.at(0);
    auto interpreter = InterpreterFactory::get(subquery_select, subquery_context,
                                               SelectQueryOptions(QueryProcessingStage::Complete).setInternal(true));
    auto stream = interpreter->execute().getInputStream();

    Block block;
    try
    {
        do
        {
            block = stream->read();
        } while (block && block.rows() == 0);

        if (!block)
        {
            /// Interpret subquery with empty result as Null literal
            auto ast_new = std::make_unique<ASTLiteral>(Null());
            ast_new->setAlias(ast->tryGetAlias());
            ast = std::move(ast_new);
            return;
        }

        if (block.rows() > 1)
            throw Exception(
                "Scalar subquery expected 1 row, got " + std::to_string(block.rows()) + " rows",
                ErrorCodes::INCORRECT_RESULT_OF_SCALAR_SUBQUERY);
        while (Block rest = stream->read())
            if (rest.rows() > 0)
                throw Exception(
                    "Scalar subquery returned more than one non-empty block", ErrorCodes::INCORRECT_RESULT_OF_SCALAR_SUBQUERY);
    }
    catch (const Exception & e)
    {
        if (e.code() == ErrorCodes::TOO_MANY_ROWS)
            throw Exception("Scalar subquery returned too many rows", ErrorCodes::INCORRECT_RESULT_OF_SCALAR_SUBQUERY);
        else
            throw;
    }

    size_t columns = block.columns();
    if (columns == 1)
    {
        auto lit = std::make_unique<ASTLiteral>((*block.safeGetByPosition(0).column)[0]);
        lit->alias = subquery.alias;
        lit->prefer_alias_to_column_name = subquery.prefer_alias_to_column_name;
        ast = addTypeConversion(std::move(lit), block.safeGetByPosition(0).type->getName());
    }
    else
    {
        auto tuple = std::make_shared<ASTFunction>();
        tuple->alias = subquery.alias;
        ast = tuple;
        tuple->name = "tuple";
        auto exp_list = std::make_shared<ASTExpressionList>();
        tuple->arguments = exp_list;
        tuple->children.push_back(tuple->arguments);

        exp_list->children.resize(columns);
        for (size_t i = 0; i < columns; ++i)
        {
            exp_list->children[i] = addTypeConversion(
                std::make_unique<ASTLiteral>((*block.safeGetByPosition(i).column)[0]), block.safeGetByPosition(i).type->getName());
        }
    }
}

}
