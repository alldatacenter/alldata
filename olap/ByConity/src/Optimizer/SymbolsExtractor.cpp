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

#include <Interpreters/RequiredSourceColumnsVisitor.h>
#include <Optimizer/ExpressionExtractor.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Parsers/ASTIdentifier.h>

namespace DB
{
std::set<std::string> SymbolsExtractor::extract(ConstASTPtr node)
{
    static SymbolVisitor visitor;
    SymbolVisitorContext context;
    ASTVisitorUtil::accept(node, visitor, context);
    if (!context.exclude_symbols.empty())
    {
        throw Exception("exclude_symbols should be null", ErrorCodes::LOGICAL_ERROR);
    }

    return std::move(context.result);
}

std::set<std::string> SymbolsExtractor::extract(PlanNodePtr & node)
{
    std::vector<ConstASTPtr> expressions;
    for (ConstASTPtr expr : ExpressionExtractor::extract(node))
    {
        expressions.emplace_back(expr);
    }
    return extract(expressions);
}

std::set<std::string> SymbolsExtractor::extract(std::vector<ConstASTPtr> & nodes)
{
    static SymbolVisitor visitor;
    SymbolVisitorContext context;
    for (auto & node : nodes)
    {
        ASTVisitorUtil::accept(node, visitor, context);
    }
    return std::move(context.result);
}

Void SymbolVisitor::visitNode(const ConstASTPtr & node, SymbolVisitorContext & context)
{
    for (ConstASTPtr child : node->children)
    {
        ASTVisitorUtil::accept(child, *this, context);
    }
    return Void{};
}

Void SymbolVisitor::visitASTIdentifier(const ConstASTPtr & node, SymbolVisitorContext & context)
{
    const auto & identifier = node->as<ASTIdentifier &>();
    if (!context.exclude_symbols.count(identifier.name()))
    {
        context.result.insert(identifier.name());
    }
    return Void{};
}

Void SymbolVisitor::visitASTFunction(const ConstASTPtr & node, SymbolVisitorContext & context)
{
    const auto & ast_func = node->as<const ASTFunction &>();
    if (ast_func.name == "lambda")
    {
        auto exclude_symbols = RequiredSourceColumnsMatcher::extractNamesFromLambda(ast_func);
        for (auto & es : exclude_symbols)
        {
            ++context.exclude_symbols[es];
        }

        visitNode(ast_func.arguments->children[1], context);

        for (auto & es : exclude_symbols)
        {
            auto reduced_value = --context.exclude_symbols[es];
            if (reduced_value == 0)
            {
                context.exclude_symbols.erase(es);
            }
        }

        return Void{};
    }
    else
    {
        return visitNode(node, context);
    }
}

}
