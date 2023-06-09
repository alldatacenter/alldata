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

#include <QueryPlan/QueryCacheStep.h>
#include <Processors/QueryPipeline.h>
#include <QueryPlan/ITransformingStep.h>
#include <Processors/Sources//SourceFromQueryCache.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTAlterQuery.h>
#include <Functions/IFunction.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Functions/FunctionFactory.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>

namespace DB
{

static ITransformingStep::Traits getTraits()
{
    return ITransformingStep::Traits
        {
            {
                .preserves_distinct_columns = false,
                .returns_single_stream = true,
                .preserves_number_of_streams = true,
                .preserves_sorting = true,
            },
            {
                .preserves_number_of_rows = false,
            }
        };
}

DataStream createOutputStream(
    const DataStream & input_stream,
    Block output_header,
    const ITransformingStep::DataStreamTraits & stream_traits)
{
    DataStream output_stream{.header = std::move(output_header)};

    if (stream_traits.preserves_distinct_columns)
        output_stream.distinct_columns = input_stream.distinct_columns;

    output_stream.has_single_port = stream_traits.returns_single_stream
        || (input_stream.has_single_port && stream_traits.preserves_number_of_streams);

    if (stream_traits.preserves_sorting)
    {
        output_stream.sort_description = input_stream.sort_description;
        output_stream.sort_mode = input_stream.sort_mode;
    }

    return output_stream;
}

QueryCacheStep::QueryCacheStep(const DataStream & input_stream_,
                               const ASTPtr & query_ptr_,
                               const ContextPtr & context_,
                               QueryProcessingStage::Enum stage_)
    : query_ptr(query_ptr_), context(context_), stage(stage_)
{
    init();

    if (hitCache())
    {
        output_stream = std::move(input_stream_);
    }
    else
    {
        auto traits = getTraits();
        auto header = input_stream_.header;
        input_streams.emplace_back(std::move(input_stream_));
        output_stream = createOutputStream(input_streams.front(), std::move(header), traits.data_stream_traits);
    }
}

void QueryCacheStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    if (hitCache())
    {
        output_stream = std::move(input_streams_[0]);
    }
    else
    {
        auto traits = getTraits();
        auto header = input_streams_[0].header;
        input_streams.emplace_back(std::move(input_streams_[0]));
        output_stream = createOutputStream(input_streams.front(), std::move(header), traits.data_stream_traits);
    }
}

void QueryCacheStep::init()
{
    is_valid_query = checkViableQuery();
    if (!context->getSettings().enable_query_cache || !is_valid_query)
        return;

    query_cache = context->getQueryCache();
    auto key = std::make_shared<QueryKey>(queryToString(query_ptr), context->getSettings(), stage);
    if (query_cache)
    {
        query_key = QueryCache::hash(*key);
        auto result = query_cache->get(query_key);

        if (result && checkCacheTime(result->update_time))
        {
            query_result = result->clone();
            hit_query_cache = true;
            ProfileEvents::increment(ProfileEvents::QueryCacheHits);
        }
        else
        {
            query_result = std::make_shared<QueryResult>();
            ProfileEvents::increment(ProfileEvents::QueryCacheMisses);
        }
    }
}

QueryPipelinePtr QueryCacheStep::updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & settings)
{
    // if hit query cache, generate pipeline
    if (hitCache())
    {
        auto pipeline = std::make_unique<QueryPipeline>();
        QueryPipelineProcessorsCollector collector(*pipeline, this);

        initializePipeline(*pipeline, settings);

        auto added_processors = collector.detachProcessors();
        processors.insert(processors.end(), added_processors.begin(), added_processors.end());
        return pipeline;
    }

    // else, update pipeline
    QueryPipelineProcessorsCollector collector(*pipelines.front(), this);
    transformPipeline(*pipelines.front(), settings);
    processors = collector.detachProcessors();

    return std::move(pipelines.front());
}

void QueryCacheStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
                                {
                                    if (stream_type != QueryPipeline::StreamType::Main)
                                        return nullptr;

                                    return std::make_shared<QueryCacheTransform>(header, query_cache, query_key, query_result, ref_db_and_table, latest_time);
                                });
}

void QueryCacheStep::initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    pipeline.init(Pipe(std::make_shared<SourceFromQueryCache>(getOutputStream().header, query_result)));
}

void QueryCacheStep::checkDeterministic(const ASTPtr & node)
{
    if (!is_deterministic || !node)
        return;

    if (auto * function = node->as<ASTFunction>())
    {
        if (AggregateFunctionFactory::instance().hasNameOrAlias(function->name))
            return;

        const auto func = FunctionFactory::instance().get(function->name, context);
        if (!func->isDeterministic())
        {
            is_deterministic = false;
            return;
        }
    }

    for (const auto & child : node->children)
        checkDeterministic(child);
}

bool QueryCacheStep::checkCacheTime(UInt64 cache_time) const
{
    return latest_time <= cache_time;
}

bool QueryCacheStep::checkViableQuery()
{
    if (!query_ptr)
        return false;

    const ASTSelectWithUnionQuery * query = typeid_cast<ASTSelectWithUnionQuery *>(query_ptr.get());

    if (!query)
        return false;

    std::vector<ASTPtr> tables;
    bool dummy = false;
    query->collectAllTables(tables, dummy);

    // If there is no target table, do not use query cache
    if (tables.empty())
        return false;

    for (auto & table : tables)
    {
        auto target_table_id = context->resolveStorageID(table);
        auto storage_table = DatabaseCatalog::instance().tryGetTable(target_table_id, context);

        if (!storage_table)
            continue;

        String database = target_table_id.database_name;
        if (database.empty())
            database = context->getCurrentDatabase();

        if (database == "system")
            return false;

        auto * merge_tree_data = dynamic_cast<MergeTreeData *>(storage_table.get());
        if (!merge_tree_data)
            return false;

        // Add db and table into ref_db_and_table from which we can drop specific queries
        updateRefDatabaseAndTable(database, target_table_id.table_name);
        updateLastedTime(storage_table->getTableUpdateTime());
    }

    checkDeterministic(query_ptr);
    return is_deterministic;
}

void QueryCacheStep::serialize(WriteBuffer &) const
{
    throw Exception("QueryCacheStep can not be serialized", ErrorCodes::LOGICAL_ERROR);
}

QueryPlanStepPtr QueryCacheStep::deserialize(ReadBuffer &, ContextPtr )
{
    return nullptr;
}

std::shared_ptr<IQueryPlanStep> QueryCacheStep::copy(ContextPtr) const
{
    throw Exception("QueryCacheStep can not copy", ErrorCodes::NOT_IMPLEMENTED);
}

}
