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

#include <Interpreters/InterpreterPerfectShard.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/ClusterProxy/SelectStreamFactory.h>
#include <Interpreters/ClusterProxy/executeQuery.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/IdentifierSemantic.h>
#include <Interpreters/RewriteDistributedQueryVisitor.h>

#include <DataTypes/DataTypeFactory.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeAggregateFunction.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>

#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTQualifiedAsterisk.h>
#include <Parsers/queryToString.h>

#include <Storages/StorageDistributed.h>
#include <QueryPlan/ReadFromPreparedSource.h>
#include <Processors/Sources/NullSource.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/AggregatingStep.h>

#include <IO/WriteBufferFromString.h>

#include <queue>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

bool checkIfSelectListExistConstant(const ASTPtr & node)
{
    if (!node)
        return false;

    for (auto & child : node->children)
    {
        if (child->as<ASTLiteral>())
            return true;
    }

    return false;
}

/**
 * We will add all rules for determining whether a sql is perfect-shardable in this function.
 * 1. disable perfect-shard if select list has a constant ( if constant can be removed, it must removed before Perfect-Shard is checked.)
 */
bool InterpreterPerfectShard::checkPerfectShardable()
{
    auto * select = query->as<ASTSelectQuery>();
    if (!select || !perfect_shardable)
        return false;

    if (checkIfSelectListExistConstant(select->select()))
        return false;

    if (!checkAggregationReturnType())
        return false;

    return perfect_shardable;
}

void InterpreterPerfectShard::collectTables()
{
    ASTs tables;
    std::queue<ASTPtr> q;
    q.push(query);
    while (!q.empty())
    {
        auto node = q.front();
        for (auto & child : node->children)
            q.push(child);

        if (const ASTTableExpression * table = node->as<ASTTableExpression>())
        {
            if (table->database_and_table_name)
                tables.push_back(table->database_and_table_name);
        }

        q.pop();
    }

    ClusterPtr cluster = nullptr;
    for (auto & table : tables)
    {
        auto storage = RewriteDistributedQueryMatcher::tryGetTable(table, context);
        auto distributed_table = dynamic_cast<StorageDistributed *>(storage.get());
        if (distributed_table)
        {
            table_rewrite_info[table.get()] = {distributed_table->getRemoteDatabaseName(), distributed_table->getRemoteTableName()};

            auto * identifier = table->as<ASTTableIdentifier>();
            DatabaseAndTableWithAlias database_table(*identifier, context->getCurrentDatabase());
            identifier_rewrite_info.emplace_back(database_table, distributed_table->getRemoteTableName());

            if (main_table.empty())
            {
                main_database = distributed_table->getRemoteDatabaseName();
                main_table = distributed_table->getRemoteTableName();
            }

            // check if all tables locate on the same cluster
            if (!cluster)
            {
                cluster = distributed_table->getCluster();
                query_info.cluster = cluster;
            }
            else if (cluster != distributed_table->getCluster())
            {
                // tables in different cluster, perfect shard doesn't know
                // how to rewrite the query, fall back to common case
                perfect_shardable = false;
                break;
            }
        }
        else
        {
            perfect_shardable = false;
            break;
        }
    }

    if (!query_info.cluster)
        perfect_shardable = false;
}

void InterpreterPerfectShard::rewriteDistributedTables()
{
    RewriteDistributedQueryMatcher::Data rewrite_data{.table_rewrite_info = table_rewrite_info, .identifier_rewrite_info = identifier_rewrite_info};
    RewriteDistributedQueryVisitor(rewrite_data).visit(query);
}

void InterpreterPerfectShard::buildQueryPlan(QueryPlan & query_plan)
{
    if (!checkPerfectShardable())
        return;

    rewriteDistributedTables();

    sendQuery(query_plan);

    try
    {
        buildFinalPlan(query_plan);

        LOG_DEBUG(log, "Perfect-Shard applied");

    }catch(...){
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        LOG_DEBUG(log, "Build query plan for Perfect-Shard failed, fallback to original plan");
        perfect_shardable = false;
    }
}

QueryProcessingStage::Enum InterpreterPerfectShard::determineProcessingStage()
{
    ASTSelectQuery * select = query->as<ASTSelectQuery>();
    if (select->orderBy())
        return QueryProcessingStage::WithMergeableStateAfterAggregation;
    if (select->limitBy() || select->limitLength() || select->limitOffset())
        return QueryProcessingStage::WithMergeableStateAfterAggregation;

    return QueryProcessingStage::Complete;
}

void InterpreterPerfectShard::sendQuery(QueryPlan & query_plan)
{
    const Scalars & scalars = context->hasQueryContext() ? context->getQueryContext()->getScalars() : Scalars{};

    Block header = InterpreterSelectQuery(query, context, SelectQueryOptions(processed_stage).analyze()).getSampleBlock();

    if (!query_info.getCluster())
        throw Exception("Cluster should not be nullptr when sendQuery in PerfectShard: ", ErrorCodes::LOGICAL_ERROR);

    /// Return directly (with correct header) if no shard to query.
    if (query_info.getCluster()->getShardsInfo().empty())
    {
        Pipe pipe(std::make_shared<NullSource>(header));
        auto read_from_pipe = std::make_unique<ReadFromPreparedSource>(std::move(pipe));
        read_from_pipe->setStepDescription("Read from NullSource (Distributed)");
        query_plan.addStep(std::move(read_from_pipe));

        return;
    }

    ClusterProxy::SelectStreamFactory select_stream_factory = ClusterProxy::SelectStreamFactory(
        header, processed_stage, StorageID(main_database, main_table), scalars, false, context->getExternalTables()
    );

    LOG_TRACE(log, "Perfect-Shard will send query {} ", queryToString(query));

    ClusterProxy::executeQuery(query_plan, select_stream_factory, log,
        query, context, query_info, nullptr, {}, nullptr);

    if (!query_plan.isInitialized())
        throw Exception("Pipeline is not initialized", ErrorCodes::LOGICAL_ERROR);
}

void InterpreterPerfectShard::buildFinalPlan(QueryPlan & query_plan)
{
    if (interpreter.analysis_result.need_aggregate)
        addAggregation(query_plan);

    if (interpreter.analysis_result.has_order_by)
    {
        // if (processed_stage == QueryProcessingStage::WithMergeableStateAfterAggregation)
        //     interpreter.executeMergeSorted(query_plan, "for PERFECT-SHARD ORDER BY");
        // else
            interpreter.executeOrder(query_plan, nullptr);
    }

    if (interpreter.analysis_result.hasLimitBy())
    {
        interpreter.executeExpression(query_plan, interpreter.analysis_result.before_limit_by, "Before LIMIT BY");
        interpreter.executeLimitBy(query_plan);
    }

    interpreter.executeProjection(query_plan, interpreter.analysis_result.final_projection);

    interpreter.executeLimit(query_plan);
    interpreter.executeOffset(query_plan);
}

String InterpreterPerfectShard::getAggregationName(const String & function_name, const DataTypePtr & type) const
{
    if (Poco::toLower(function_name) == "max")
            return "max";
    else if (Poco::toLower(function_name) == "min")
        return "min";
    else if (Poco::toLower(function_name) == "any")
        return "any";
    else if (isNumber(*type))
        return "sum";
    else if (const auto * array = typeid_cast<const DataTypeArray *>(type.get()))
        return getAggregationName(function_name, array->getNestedType()) + "ForEach";
    else if (const auto * nullable = typeid_cast<const DataTypeNullable *>(type.get()))
        return getAggregationName(function_name, nullable->getNestedType());
    // TODO: bitmap64, pathfind, stack is not implement right now
    else
        return "";
}

void InterpreterPerfectShard::addAggregation(QueryPlan & query_plan)
{
    const auto & header_before_aggregation = query_plan.getCurrentDataStream().header;
    ColumnNumbers keys;
    for (const auto & key : interpreter.query_analyzer->aggregationKeys())
        keys.push_back(header_before_aggregation.getPositionByName(key.name));

    AggregateDescriptions pre_aggregates = interpreter.query_analyzer->aggregates();

    AggregateDescriptions aggregates;

    auto build_aggregation = [&](AggregateDescription & descr)
    {
        AggregateDescription aggregate;
        String argument_column_name = descr.column_name;
        /**
         * For Complete stage, each worker will project the select list to final column ( project alias ).
         * For WithMergeableStateAfterAggregation stage, final projection will not be done.
         */
        if (!original_project.empty() && processed_stage == QueryProcessingStage::Complete)
        {
            auto it = original_project.find(descr.column_name);
            if (it != original_project.end())
                argument_column_name = it->second;
        }
        aggregate.column_name = descr.column_name;
        aggregate.argument_names.push_back(argument_column_name);
        aggregate.arguments.push_back(header_before_aggregation.getPositionByName(argument_column_name));
        DataTypePtr type = header_before_aggregation.getByName(argument_column_name).type;

        AggregateFunctionProperties properties;
        aggregate.parameters = Array();
        aggregate.function = AggregateFunctionFactory::instance().get(getAggregationName(descr.function->getName(), type), {type}, aggregate.parameters, properties);
        aggregates.push_back(aggregate);
    };

    for (auto & descr : pre_aggregates)
        build_aggregation(descr);

    const Settings & settings = context->getSettingsRef();

    SortDescription group_by_sort_description;
    InputOrderInfoPtr group_by_info = nullptr;
    // TODO: determine overflow
    bool overflow_row = false;

    Aggregator::Params params(header_before_aggregation, keys, aggregates,
                              overflow_row,
                              settings.max_threads);

    auto merge_threads = interpreter.getMaxStreams();
    auto temporary_data_merge_threads = settings.aggregation_memory_efficient_merge_threads
                                        ? static_cast<size_t>(settings.aggregation_memory_efficient_merge_threads)
                                        : static_cast<size_t>(settings.max_threads);

    bool storage_has_evenly_distributed_read = false;

    auto aggregating_step = std::make_unique<AggregatingStep>(
            query_plan.getCurrentDataStream(),
            params, GroupingSetsParamsList{}, true,
            settings.max_block_size,
            merge_threads,
            temporary_data_merge_threads,
            storage_has_evenly_distributed_read,
            std::move(group_by_info),
            std::move(group_by_sort_description),
            // because final_=true => no further merging/aggregating step =>
            // should_produce_results_in_order_of_bucket_number_ = false
            false);

    query_plan.addStep(std::move(aggregating_step));
}

void InterpreterPerfectShard::getOriginalProject()
{
    Aliases aliases = interpreter.syntax_analyzer_result->aliases;
    for (auto it = aliases.begin(); it != aliases.end(); ++it)
    {
        if (original_project.count(it->second->getColumnName()))
        {
            LOG_DEBUG(log, "Found duplicated alias " + it->second->getColumnName() +
                " : " + it->first + " when apply Perfect-shard", ErrorCodes::LOGICAL_ERROR);
            perfect_shardable = false;
            return;
        }
        original_project[it->second->getColumnName()] = it->first;
    }
}

/**
 * Perfect-Shard uses return type to determine the final merge aggregate functions.
 * If the return type we checked is not a valid type for Perfect-Shard, perfect-shard will not be performed.
 * Be careful for alias of aggregate function, worker side will return a column without alias, hence we use original_project
 * to determine the arguments of final merge aggregate functions.
 **/
bool InterpreterPerfectShard::checkAggregationReturnType() const
{
    if (interpreter.analysis_result.need_aggregate)
    {
        AggregateDescriptions pre_aggregates = interpreter.query_analyzer->aggregates();
        auto result_header = interpreter.getSampleBlock();

        auto check_aggregation = [&](AggregateDescription & descr) -> bool
        {
            String argument_column_name = descr.column_name;
            if (!original_project.empty())
            {
                auto it = original_project.find(descr.column_name);
                if (it != original_project.end())
                    argument_column_name = it->second;
            }
            DataTypePtr type = result_header.getByName(argument_column_name).type;
            return !getAggregationName(descr.function->getName(), type).empty();
        };

        for (auto & descr : pre_aggregates)
        {
            if (!check_aggregation(descr))
                return false;
        }
    }

    return true;
}

}
