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

#include <Parsers/ASTVisitor.h>
#include <Parsers/IAST_fwd.h>
#include <Optimizer/SimpleExpressionRewriter.h>
#include <QueryPlan/Assignment.h>
#include <unordered_map>

namespace DB
{
class ExpressionInliner
{
public:
    static ASTPtr inlineSymbols(const ConstASTPtr & predicate, const Assignments & mapping);
};

class InlinerVisitor : public SimpleExpressionRewriter<const Assignments>
{
public:
    ASTPtr visitASTIdentifier(ASTPtr & node, const Assignments & context) override;
};

}
