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

#include <memory>

#include <Core/QueryProcessingStage.h>
#include <DataStreams/IBlockStream_fwd.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/StorageID.h>
#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Parsers/ASTSelectQuery.h>
#include <Storages/SelectQueryInfo.h>

namespace DB
{

class IDataType;
using DataTypePtr = std::shared_ptr<const IDataType>;

/**
 * Perfect-Shard, is a execution mode if data is sharded in advance.
 * We will rewrite a distributed query into local query and send them to all workers to compute final results.
 * After then, the server will merge all these results by
 * 1. determine its return columns, if there is aggregation column
 *    1.1 merge the aggregation column by add a new aggregation-step accroding to its return type
 *        for example, select a, count() from test_table group by a
 *        server will get two columns (a, count()), we will add a new aggregation-step sum(count()) to merge aggregation with the same key.
 * 2. do the final projection, limits, and order by if possible.
 */
class InterpreterPerfectShard
{
public:
    InterpreterPerfectShard(InterpreterSelectQuery & interpreter_)
    : interpreter(interpreter_)
    , query(interpreter.query_ptr->clone())
    , context(interpreter.context)
    , log(&Poco::Logger::get("InterpreterPerfectShard"))
    {
        query_info.query = query;
        processed_stage = determineProcessingStage();
        collectTables();
        getOriginalProject();
    }

    void buildQueryPlan(QueryPlan & query_plan);

    bool checkPerfectShardable();

private:

    void collectTables();
    void rewriteDistributedTables();
    QueryProcessingStage::Enum determineProcessingStage();
    void sendQuery(QueryPlan & query_plan);
    void buildFinalPlan(QueryPlan & query_plan);
    void addAggregation(QueryPlan & query_plan);

    String getAggregationName(const String & function_name, const DataTypePtr & type) const;
    void getOriginalProject();

    bool checkAggregationReturnType() const;

    InterpreterSelectQuery & interpreter;
    ASTPtr query;
    std::shared_ptr<Context> context;
    Poco::Logger * log;

    SelectQueryInfo query_info;
    bool perfect_shardable = true;

    std::unordered_map<IAST*, std::pair<String, String>> table_rewrite_info;
    std::vector<std::pair<DatabaseAndTableWithAlias, String>> identifier_rewrite_info;

    String main_table;
    String main_database;

    QueryProcessingStage::Enum processed_stage;
    std::unordered_map<String, String> original_project;
};

}
