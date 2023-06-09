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
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/PlanVisitor.h>


namespace DB
{
class PlanCheck
{
public:
    static void checkInitPlan(QueryPlan & plan, ContextMutablePtr context);
    static void checkFinalPlan(QueryPlan & plan, ContextMutablePtr context);
};

class ReadNothingChecker : public PlanNodeVisitor<Void, Void>
{
public:
    static void check(PlanNodePtr plan);

    Void visitPlanNode(PlanNodeBase &, Void &) override;
    Void visitReadNothingNode(ReadNothingNode &, Void &) override;
};

class SymbolChecker : public PlanNodeVisitor<Void, ContextMutablePtr>
{
public:
    static void check(QueryPlan & plan, ContextMutablePtr & context, bool check_filter);

    SymbolChecker(bool checkFilter) : check_filter(checkFilter) { }

    Void visitPlanNode(PlanNodeBase &, ContextMutablePtr &) override;
    Void visitProjectionNode(ProjectionNode &, ContextMutablePtr &) override;
    Void visitFilterNode(FilterNode &, ContextMutablePtr &) override;

private:
    bool check_filter;
};

}
