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
#include <QueryPlan/IQueryPlanStep.h>
#include <Processors/Transforms/QueryCacheTransform.h>
#include <Processors/QueryCache.h>
#include <Interpreters/Context.h>
#include <Storages/IStorage.h>

namespace DB
{

class QueryCacheStep : public IQueryPlanStep
{
public:
    QueryCacheStep(const DataStream & input_stream_,
                            const ASTPtr & query_ptr_,
                            const ContextPtr & context_,
                            QueryProcessingStage::Enum stage = QueryProcessingStage::Complete);

    String getName() const override { return "QueryCache"; }

    Type getType() const override { return Type::QueryCache; }

    QueryPipelinePtr updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & settings) override;
    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &);
    void initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &);

    void checkDeterministic(const ASTPtr & node);
    bool checkViableQuery();
    bool isValidQuery() const { return is_valid_query; }

    inline bool checkCacheTime(UInt64 cache_time) const;
    bool hitCache() const { return hit_query_cache; }

    void serialize(WriteBuffer &) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer &, ContextPtr context_ = nullptr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    Processors processors;

    const ASTPtr & query_ptr;
    const ContextPtr context;

    QueryCachePtr query_cache = nullptr;
    UInt128 query_key;
    QueryResultPtr query_result = nullptr;

    std::set<String> ref_db_and_table;
    UInt64 latest_time;

    bool is_deterministic = true;
    bool is_valid_query = true;
    bool hit_query_cache = false;
    QueryProcessingStage::Enum stage;

    void init();

    void updateRefDatabaseAndTable(const String & database, const String & table)
    {
        ref_db_and_table.insert(database + "." + table);
    }

    inline void updateLastedTime(UInt64 time)
    {
        latest_time = std::max(latest_time, time);
    }
};

}
