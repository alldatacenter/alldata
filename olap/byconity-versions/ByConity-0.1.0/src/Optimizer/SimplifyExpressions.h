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

#include <Interpreters/Context.h>
#include <Parsers/ASTVisitor.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/SimplePlanRewriter.h>

namespace DB
{
struct NodeContext
{
    enum Root
    {
        ROOT_NODE,
        NOT_ROOT_NODE
    };
    Root root;
    ContextMutablePtr & context;
};

/**
 * CommonPredicatesRewriter rewrite boolean function by following rules:
 *
 *   1. (A AND B) OR (A AND C) => A AND (B OR C);
 *   2. (A OR B) AND (A OR C) => A OR (B AND C);
 *   3. (A AND B) OR (C AND D) => (A OR C) AND (A OR D) AND (B OR C) AND (B OR D);
 *
 * Note that this rule is only used to convert the root predicate to a CNF.
 */
class CommonPredicatesRewriter : public ConstASTVisitor<ConstASTPtr, NodeContext>
{
public:
    static ConstASTPtr rewrite(const ConstASTPtr & predicate, ContextMutablePtr & context);
    ConstASTPtr visitNode(const ConstASTPtr &, NodeContext &) override;
    ConstASTPtr visitASTFunction(const ConstASTPtr &, NodeContext &) override;

private:
    ConstASTPtr process(const ConstASTPtr &, NodeContext &);
};

/**
 * SwapPredicateRewriter rewrite comparison expr like
 * `Literal ComparsionOp Identifier` to `Identifier FlipComparisonOp Literal`.
 */
class SwapPredicateRewriter : public ConstASTVisitor<ConstASTPtr, Void>
{
public:
    static ConstASTPtr rewrite(const ConstASTPtr & predicate, ContextMutablePtr & context);
    ConstASTPtr visitNode(const ConstASTPtr & node, Void & context) override;
    ConstASTPtr visitASTFunction(const ConstASTPtr & node, Void & context) override;
};

}
