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
#include <Parsers/ASTVisitor.h>
#include <QueryPlan/PlanVisitor.h>
#include <Optimizer/Utils.h>

namespace DB
{
using ConstASTSet = ASTSet<ConstASTPtr>;

class ExpressionExtractor
{
public:
    static std::vector<ConstASTPtr> extract(PlanNodePtr & node);
};

class ExpressionVisitor : public PlanNodeVisitor<Void, std::vector<ConstASTPtr>>
{
public:
    Void visitPlanNode(PlanNodeBase &, std::vector<ConstASTPtr> & expressions) override;
    Void visitProjectionNode(ProjectionNode &, std::vector<ConstASTPtr> & expressions) override;
    Void visitFilterNode(FilterNode &, std::vector<ConstASTPtr> & expressions) override;
    Void visitAggregatingNode(AggregatingNode &, std::vector<ConstASTPtr> & expressions) override;
    Void visitApplyNode(ApplyNode &, std::vector<ConstASTPtr> & expressions) override;
    Void visitJoinNode(JoinNode &, std::vector<ConstASTPtr> & expressions) override;
};

class SubExpressionExtractor
{
public:
    static ConstASTSet extract(ConstASTPtr node);
};

class SubExpressionVisitor : public ConstASTVisitor<Void, ConstASTSet>
{
public:
    Void visitNode(const ConstASTPtr &, ConstASTSet & context) override;
    Void visitASTFunction(const ConstASTPtr &, ConstASTSet & context) override;
    Void visitASTIdentifier(const ConstASTPtr &, ConstASTSet & context) override;
};

}
