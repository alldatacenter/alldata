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

#include <Analyzers/Analysis.h>
#include <QueryPlan/PlanBuilder.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/planning_models.h>
#include <QueryPlan/TranslationMap.h>

namespace DB
{

class QueryPlanner
{
public:
    /**
     * Entry method of planning phase.
     *
     */
    static QueryPlanPtr plan(ASTPtr & query, Analysis & analysis, ContextMutablePtr context);

    /**
     * Entry method of planning an cross-scoped ASTSelectWithUnionQuery/ASTSelectQuery.
     *
     */
    static RelationPlan planQuery(
        ASTPtr query, TranslationMapPtr outer_query_context, Analysis & analysis, ContextMutablePtr context, CTERelationPlans & cte_info);
};

}
