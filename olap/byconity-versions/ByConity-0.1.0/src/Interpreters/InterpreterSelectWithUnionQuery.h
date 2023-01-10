/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Core/QueryProcessingStage.h>
#include <Interpreters/IInterpreterUnionOrSelectQuery.h>
#include <QueryPlan/QueryCacheStep.h>

namespace DB
{

class InterpreterSelectQuery;
class QueryPlan;

/** Interprets one or multiple SELECT queries inside UNION/UNION ALL/UNION DISTINCT chain.
  */
class InterpreterSelectWithUnionQuery : public IInterpreterUnionOrSelectQuery
{
public:
    using IInterpreterUnionOrSelectQuery::getSampleBlock;

    InterpreterSelectWithUnionQuery(
        const ASTPtr & query_ptr_,
        ContextPtr context_,
        const SelectQueryOptions &,
        const Names & required_result_column_names = {});

    ~InterpreterSelectWithUnionQuery() override;

    /// Builds QueryPlan for current query.
    virtual void buildQueryPlan(QueryPlan & query_plan) override;

    BlockIO execute() override;

    QueryPipeline executeTEALimit(QueryPipelinePtr& );

    bool ignoreLimits() const override { return options.ignore_limits; }
    bool ignoreQuota() const override { return options.ignore_quota; }

    static Block getSampleBlock(
        const ASTPtr & query_ptr_,
        ContextPtr context_,
        bool is_subquery = false);

    virtual void ignoreWithTotals() override;

private:

    void checkQueryCache(QueryPlan & query_plan);

    std::vector<std::unique_ptr<IInterpreterUnionOrSelectQuery>> nested_interpreters;

    static Block getCommonHeaderForUnion(const Blocks & headers, bool allow_extended_conversion);

    Block getCurrentChildResultHeader(const ASTPtr & ast_ptr_, const Names & required_result_column_names);

    std::unique_ptr<IInterpreterUnionOrSelectQuery>
    buildCurrentChildInterpreter(const ASTPtr & ast_ptr_, const Names & current_required_result_column_names);
};

}
