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

#include <Optimizer/ExpressionExtractor.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Parsers/ASTIdentifier.h>

namespace DB
{
std::set<std::string> SymbolsExtractor::extract(ConstASTPtr node)
{
    static SymbolVisitor visitor;
    std::set<std::string> context;
    ASTVisitorUtil::accept(node, visitor, context);
    return context;
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
    std::set<std::string> context;
    for (auto & node : nodes)
    {
        ASTVisitorUtil::accept(node, visitor, context);
    }
    return context;
}

Void SymbolVisitor::visitNode(const ConstASTPtr & node, std::set<std::string> & context)
{
    for (ConstASTPtr child : node->children)
    {
        ASTVisitorUtil::accept(child, *this, context);
    }
    return Void{};
}

Void SymbolVisitor::visitASTIdentifier(const ConstASTPtr & node, std::set<std::string> & context)
{
    auto & identifier = node->as<ASTIdentifier &>();
    context.emplace(identifier.name());
    return Void{};
}

}
