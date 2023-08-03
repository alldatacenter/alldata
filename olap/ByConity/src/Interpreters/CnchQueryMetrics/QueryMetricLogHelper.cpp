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

#include <Interpreters/CnchQueryMetrics/QueryMetricLogHelper.h>
#include <Parsers/ASTAlterQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTDropQuery.h>
#include <Parsers/ASTRenameQuery.h>
// #include <Parsers/ASTDeleteQuery.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/ProcessList.h>
#include <Interpreters/CnchSystemLog.h>
#include <Interpreters/VirtualWarehouseHandle.h>
#include <Interpreters/WorkerGroupHandle.h>
#include <Interpreters/CnchQueryMetrics/QueryMetricLog.h>
#include <Interpreters/CnchQueryMetrics/QueryWorkerMetricLog.h>
#include <DataStreams/BlockStreamProfileInfo.h>
#include <Common/RpcClientPool.h>
#include <Processors/Formats/IOutputFormat.h>
#include <Common/HostWithPorts.h>


namespace ProfileEvents
{
    extern const Event CatalogTime;
    extern const Event TotalPartitions;
    extern const Event PrunedPartitions;
    extern const Event SelectedParts;
    extern const Event SelectedRanges;
    extern const Event SelectedMarks;
    extern const Event HDFSReadElapsedMilliseconds;
    extern const Event HDFSWriteElapsedMilliseconds;
}

namespace DB
{

QueryMetricLogType::QueryMetricLogType(const ASTPtr & ast)
{
    if (ast == nullptr)
    {
        type = UNKNOWN;
        return;
    }

    if (ast->as<ASTCreateQuery>())
        type = CREATE;
    else if (ast->as<ASTDropQuery>())
        type = DROP;
    else if (ast->as<ASTRenameQuery>())
        type = RENAME;
    else if (ast->as<ASTSelectQuery>() || ast->as<ASTSelectWithUnionQuery>())
        type = SELECT;
    else if (ast->as<ASTInsertQuery>())
        type = INSERT;
    // else if (ast->as<ASTDeleteQuery>())
    //     type = DELETE;
    else if (ast->as<ASTAlterQuery>())
        type = ALTER;
    else
        type = OTHER;
}

void extractDatabaseAndTableNames(const ContextPtr & context, const ASTPtr & ast, String & database, String & table)
{
    database.clear();
    table.clear();
    if (ast == nullptr)
        return;
    if (const auto * ast_with_table = dynamic_cast<const ASTQueryWithTableAndOutput *>(ast.get()))  /// CREATE, DROP, ALTER
    {
        database = ast_with_table->database;
        table = ast_with_table->table;
        if (database.empty())
            database = context->getCurrentDatabase();
    }
    else if (auto * rename = ast->as<ASTRenameQuery>())  /// Several entities can be renamed in a single query
    {
        bool first = true;
        for (const auto & db_and_table : rename->elements)
        {
            String rename_db = db_and_table.from.database;
            String rename_table = db_and_table.from.table;
            if (!rename_table.empty() && rename_db.empty())
                rename_db = context->getCurrentDatabase();

            if (first)
            {
                database = rename_db;
                table = rename_table;
                first = false;
            }
            else
            {
                database = database + ", " + rename_db;
                table = table + ", " + rename_table;
            }
        }
    }
    else if (auto * select = ast->as<ASTSelectQuery>())
    {
        auto db_and_table = getDatabaseAndTable(*select, 0);
        if (!db_and_table)
            return;

        database = db_and_table->database;
        table = db_and_table->table;
        if (database.empty())
            database = context->getCurrentDatabase();
    }
    else if (auto * select_union = ast->as<ASTSelectWithUnionQuery>())
    {
        ASTs all_tables;
        bool dummy = false;
        select_union->collectAllTables(all_tables, dummy);
        bool first = true;
        for (const auto & db_table : all_tables)
        {
            auto table_id = context->resolveStorageID(db_table);
            String database_name = table_id.database_name;
            String table_name = table_id.table_name;
            if (database_name.empty())
                database_name = context->getCurrentDatabase();

            if (first)
            {
                database = database_name;
                table = table_name;
                first = false;
            }
            else
            {
                database = database + ", " + database_name;
                table = table + ", " + table_name;
            }
        }
    }
    else if (auto * insert = ast->as<ASTInsertQuery>())
    {
        database = insert->table_id.database_name;
        table = insert->table_id.table_name;
        if (database.empty())
            database = context->getCurrentDatabase();

        if (insert->select)
        {
            const auto & select_ast = insert->select->as<ASTSelectWithUnionQuery &>();
            ASTs all_tables;
            bool dummy = false;
            select_ast.collectAllTables(all_tables, dummy);
            for (const auto & db_table : all_tables)
            {
                auto table_id = context->resolveStorageID(db_table);
                String database_name = table_id.database_name;
                String table_name = table_id.table_name;
                if (database_name.empty())
                    database_name = context->getCurrentDatabase();

                database = database + ", " + database_name;
                table = table + ", " + table_name;
            }
        }
    }
    // else if (auto * delete_ast = ast->as<ASTDeleteQuery>())
    // {
    //     database = delete_ast->database;
    //     table = delete_ast->table;
    //     if (database.empty())
    //         database = context->getCurrentDatabase();
    // }
}

static bool checkSelectForFilteredTables(const ASTPtr & ast, const ContextPtr & context)
{
    if (auto * select_ast = ast->as<ASTSelectWithUnionQuery>())
    {
        std::vector<ASTPtr> all_base_tables;
        bool dummy = false;
        select_ast->collectAllTables(all_base_tables, dummy);
        for (auto & table_ast : all_base_tables)
        {
            DatabaseAndTableWithAlias database_table(table_ast);
            String database = database_table.database;
            String table = database_table.table;
            if ((database == CNCH_SYSTEM_LOG_DB_NAME || database == "system")
                && ((!context->getSettingsRef().enable_query_metrics_tables_profiling
                        && (table == CNCH_SYSTEM_LOG_QUERY_METRICS_TABLE_NAME || table == CNCH_SYSTEM_LOG_QUERY_WORKER_METRICS_TABLE_NAME))
                    || (!context->getSettingsRef().enable_kafka_log_profiling && table == CNCH_SYSTEM_LOG_KAFKA_LOG_TABLE_NAME)))
            {
                return true;
            }
        }
    }
    return false;
}

void insertCnchQueryMetric(
    ContextMutablePtr context,
    const String & query,
    const QueryMetricLogState state,
    const time_t current_time,
    const ASTPtr & ast,
    const QueryStatusInfo * info,
    const BlockStreamProfileInfo * stream_in_info,
    const QueryPipeline * query_pipeline,
    bool empty_stream,
    UInt8 complex_query,
    UInt32 init_time,
    UInt32 runtime_latency,
    const String & exception,
    const String & stack_trace)
{
    if (ast && checkSelectForFilteredTables(ast, context))
    {
        LOG_DEBUG(&Poco::Logger::get("QueryMetricLogHelper"),
            "Not inserting query metric for SELECT query from query metrics or kafka log tables");
        return;
    }
    else
    {
        LOG_DEBUG(&Poco::Logger::get("QueryMetricLogHelper"),
            "Inserting query metric for query: {}", query);
    }

    QueryMetricLogType type(ast);
    String database;
    String table;
    /// Extract the database names and table names from the query
    extractDatabaseAndTableNames(context, ast, database, table);

    if (context->getServerType() == ServerType::cnch_server && context->getQueryMetricsLog())
    {
        String query_id = context->getClientInfo().initial_query_id;
        String server_id = getenv("SERVER_ID") ? getenv("SERVER_ID") : context->getHostWithPorts().getTCPAddress();
        time_t event_time = current_time;
        UInt32 latency = (info) ? info->elapsed_seconds * 1000 : 0;
        UInt32 catalog_time = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::CatalogTime]).load() : 0;
        UInt32 total_partitions = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::TotalPartitions]).load() : 0;
        UInt32 pruned_partitions = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::PrunedPartitions]).load() : 0;
        UInt32 selected_parts = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::SelectedParts]).load() : 0;
        UInt64 peak_memory = (info) ? info->peak_memory_usage : 0;
        UInt32 read_rows = 0;
        UInt64 read_bytes = 0;
        UInt64 read_cached_bytes = 0;
        UInt32 write_rows = 0;
        UInt64 write_bytes = 0;
        UInt64 write_duration = 0;
        UInt32 result_rows = 0;

        if (empty_stream)
        {
            const auto & insert_profile_info = context->getExtendedProfileInfo();
            read_rows = insert_profile_info.read_rows;
            read_bytes = insert_profile_info.read_bytes;
            read_cached_bytes = insert_profile_info.read_cached_bytes;
            runtime_latency = insert_profile_info.runtime_latency;

            write_rows = insert_profile_info.written_rows;
            write_bytes = insert_profile_info.written_bytes;
            write_duration = insert_profile_info.written_duration;
        }
        else if (info)
        {
            read_rows = info->read_rows;
            read_bytes = info->read_bytes;
            read_cached_bytes = info->disk_cache_bytes;

            write_rows = info->written_rows;
            write_bytes = info->written_bytes;
            write_duration = info->written_duration;
        }

        if (stream_in_info)
        {
            result_rows = stream_in_info->rows;
        }
        else if (query_pipeline)
        {
            if (const auto * output_format = query_pipeline->getOutputFormat())
                result_rows = output_format->getResultRows();
        }

        String operator_level = (info) ? info->operator_level : "";
        auto vw = context->tryGetCurrentVW();
        String virtual_warehouse = vw ? vw->getName() :  "";
        auto wg = context->tryGetCurrentWorkerGroup();
        String worker_group = wg ? wg->getID() :  "";
        const ClientInfo & client_info = context->getClientInfo();
        QueryMetricElement query_metric_elem{
            query_id,
            query,
            state,
            type,
            database,
            table,
            complex_query,
            server_id,
            event_time,
            latency,
            runtime_latency,
            init_time,
            catalog_time,
            total_partitions,
            pruned_partitions,
            selected_parts,
            peak_memory,
            read_rows,
            read_bytes,
            read_cached_bytes,
            write_rows,
            write_bytes,
            write_duration,
            result_rows,
            operator_level,
            virtual_warehouse,
            worker_group,
            exception,
            stack_trace,
            client_info};
        context->insertQueryMetricsElement(query_metric_elem);
    }
    else if (context->getServerType() == ServerType::cnch_worker)
    {
        String initial_query_id = context->getClientInfo().initial_query_id;
        String current_query_id = context->getCurrentQueryId();
        String worker_id = getWorkerID(context);
        time_t event_time = current_time;
        UInt32 latency = (info) ? info->elapsed_seconds * 1000 : 0;
        UInt32 selected_parts = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::SelectedParts]).load() : 0;
        UInt32 selected_ranges = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::SelectedRanges]).load() : 0;
        UInt32 selected_marks = (info && info->profile_counters) ? ((*info->profile_counters)[ProfileEvents::SelectedMarks]).load() : 0;
        UInt64 peak_memory = (info) ? info->peak_memory_usage : 0;
        UInt32 read_rows = (info) ? info->read_rows : 0;
        UInt64 read_bytes = (info) ? info->read_bytes : 0;
        UInt64 read_cached_bytes = (info) ? info->disk_cache_bytes : 0;
        UInt32 write_rows = (info) ? info->written_rows : 0;
        UInt64 write_bytes = (info) ? info->written_bytes : 0;
        UInt64 write_duration = (info) ? info->written_duration : 0;
        UInt32 vfs_time = (info && info->profile_counters)
            ? ((*info->profile_counters)[ProfileEvents::HDFSReadElapsedMilliseconds]
                   ? ((*info->profile_counters)[ProfileEvents::HDFSReadElapsedMilliseconds]).load()
                   : ((*info->profile_counters)[ProfileEvents::HDFSWriteElapsedMilliseconds]).load())
            : 0;

        String operator_level = (info) ? info->operator_level : "";
        auto query_worker_metric_elem = std::make_shared<QueryWorkerMetricElement>(
            initial_query_id,
            current_query_id,
            query,
            state,
            type,
            database,
            table,
            worker_id,
            event_time,
            latency,
            runtime_latency,
            selected_parts,
            selected_ranges,
            selected_marks,
            vfs_time,
            peak_memory,
            read_rows,
            read_bytes,
            read_cached_bytes,
            write_rows,
            write_bytes,
            write_duration,
            operator_level,
            exception,
            stack_trace);

        if (context->getClientInfo().client_type == ClientInfo::ClientType::UNKNOWN)  /// query is directly sent from client, the metrics will be sent to a random cnch server by RPC
        {
            auto server_client = context->getCnchServerClientPool().get();
            server_client->submitQueryWorkerMetrics(query_worker_metric_elem);
        }
        else  /// query from cnch server or cnch aggre worker, store the metric elements here and send out in TCPHandler.
        {
            context->addQueryWorkerMetricElements(query_worker_metric_elem);
        }
    }
}

}
