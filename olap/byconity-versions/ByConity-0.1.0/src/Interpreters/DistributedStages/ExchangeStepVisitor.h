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
#include <Optimizer/Property/Property.h>
#include <Interpreters/Context_fwd.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/PlanVisitor.h>


namespace DB
{

using ExchangeStepResult = QueryPlan::Node *;



struct ExchangeStepContext
{
    ContextPtr context;
    QueryPlan & query_plan;
    bool has_gathered = false;
};

class ExchangeStepVisitor : public NodeVisitor<ExchangeStepResult, ExchangeStepContext>
{
public:

    ExchangeStepResult visitNode(QueryPlan::Node *, ExchangeStepContext & exchange_context) override;

    ExchangeStepResult visitMergingAggregatedNode(QueryPlan::Node * node, ExchangeStepContext &) override;

    ExchangeStepResult visitJoinNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context) override;

    ExchangeStepResult visitLimitNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context) override;

    ExchangeStepResult visitMergingSortedNode(QueryPlan::Node * node, ExchangeStepContext & exchange_context) override;

    void addGather(QueryPlan & query_plan, ExchangeStepContext & exchange_context);

private:

    ExchangeStepResult visitChild(QueryPlan::Node * node, ExchangeStepContext & exchange_context);

    void addExchange(QueryPlan::Node * node, ExchangeMode mode, const Partitioning & partition, ExchangeStepContext & exchange_context);

};

class AddExchangeRewriter
{
public:

    static void rewrite(QueryPlan & query_plan, ExchangeStepContext & exchange_context);

};

}
