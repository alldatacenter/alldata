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

#include <Storages/System/CollectWhereClausePredicate.h>
#include <Columns/ColumnConst.h>
#include <Interpreters/Context.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTWithAlias.h>

namespace DB
{
    std::pair<String,String> collectWhereEqualClausePredicate(const ASTPtr & ast, const ContextPtr & context)
    {
        std::pair<String,String> res;
        if (!ast)
            return res;

        ASTFunction * func = ast->as<ASTFunction>();
        if (!func)
            return res;

        if (func->name != "equals")
            return res;

        const auto * column_name = func->arguments->children[0]->as<ASTIdentifier>();
        if (!column_name)
            return res;

        const ASTPtr ast_value = func->arguments->children[1];
        const ASTFunction * function_value = ast_value->as<ASTFunction>();
        if (function_value && function_value->name == "currentDatabase")
        {
            ASTPtr db_ast = evaluateConstantExpressionForDatabaseName(ast_value , context);
            return std::make_pair(column_name->name(), db_ast->as<const ASTLiteral &>().value.get<String>());
        }

        const auto * column_value = ast_value->as<ASTLiteral>();
        if (!column_value)
            return res;

        if (column_value->value.getType() != Field::Types::String)
            return std::make_pair(column_name->name(), "");

        return std::make_pair(column_name->name(), column_value->value.get<String>());
    }

    std::map<String,String> collectWhereANDClausePredicate(const ASTPtr & ast, const ContextPtr & context)
    {
        std::map<String,String> res;
        if (!ast)
            return res;

        ASTFunction * func = ast->as<ASTFunction>();
        if (!func)
            return res;

        else if ((func->name == "and") && func->arguments)
        {
            const auto children = func->arguments->children;
            std::for_each(children.begin(), children.end(), [& res, & context] (const auto & child) {
                std::pair<String, String> p = collectWhereEqualClausePredicate(child, context);
                if (!p.first.empty())
                    res.insert(p);
            });
        }
        else if (func->name == "equals")
        {
            auto p = collectWhereEqualClausePredicate(ast, context);
            if (!p.first.empty())
                res.insert(p);
        }

        return res;
    }

    // collects columns and values for WHERE condition with OR, AND and EQUALS (case insesitive)
    // e.g for query "select ... where ((db = 'db') AND (name = 'name1')) OR ((db = 'db') AND (name = 'name2')) ", method will return a vector {'name':'name1', 'db':'db'}, {'db' : 'db', 'name':'name2'}
    // if a value of column is not a string, it will has value as an empty string
    std::vector<std::map<String,String>> collectWhereORClausePredicate(const ASTPtr & ast, const ContextPtr & context)
    {
        std::vector<std::map<String,String>> res;
        if (!ast)
            return res;

        ASTFunction * func = ast->as<ASTFunction>();
        if (!func)
            return res;

        if ((func->name == "or") && func->arguments)
        {
            const auto children = func->arguments->children;
            std::for_each(children.begin(), children.end(), [& res, & context] (const auto & child) {
                std::map<String,String> m = collectWhereANDClausePredicate(child, context);
                if (!m.empty())
                    res.push_back(m);
            });
        }
        else if (func->name == "and")
        {
            std::map<String,String> m = collectWhereANDClausePredicate(ast, context);
            if (!m.empty())
                res.push_back(m);
        }
        else if (func->name == "equals")
        {
            auto p = collectWhereEqualClausePredicate(ast, context);
            if (!p.first.empty())
                res.push_back(std::map<String, String>{p});
        }

        return res;
    }
}
