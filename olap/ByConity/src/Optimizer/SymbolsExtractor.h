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
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTVisitor.h>
#include <QueryPlan/PlanVisitor.h>

namespace DB
{
class SymbolsExtractor
{
public:
    static std::set<std::string> extract(ConstASTPtr node);
    static std::set<std::string> extract(PlanNodePtr & node);
    static std::set<std::string> extract(std::vector<ConstASTPtr> & nodes);
};


struct SymbolVisitorContext
{
    std::set<std::string> result;
    // count of forbidden list
    std::unordered_map<std::string, UInt64> exclude_symbols;
};

class SymbolVisitor : public ConstASTVisitor<Void, SymbolVisitorContext>
{
public:
    Void visitNode(const ConstASTPtr &, SymbolVisitorContext & context) override;
    Void visitASTIdentifier(const ConstASTPtr &, SymbolVisitorContext & context) override;
    Void visitASTFunction(const ConstASTPtr & node, SymbolVisitorContext & context) override;
};

}
