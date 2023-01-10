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

#include <CloudServices/CnchServerResource.h>
#include <Core/NamesAndTypes.h>
#include <Core/QueryProcessingStage.h>
#include <Interpreters/ClusterProxy/SelectStreamFactory.h>
#include <Interpreters/ClusterProxy/executeQuery.h>
#include <Interpreters/Context.h>
#include <Interpreters/ExpressionAnalyzer.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/SelectQueryOptions.h>
#include <Interpreters/TranslateQualifiedNamesVisitor.h>
#include <Interpreters/TreeRewriter.h>
#include <Interpreters/VirtualWarehousePool.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/trySetVirtualWarehouse.h>
#include <MergeTreeCommon/CnchStorageCommon.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTPartition.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/queryToString.h>
#include <Processors/Sources/NullSource.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/ReadFromPreparedSource.h>
#include <Storages/Hive/HiveBucketFilter.h>
#include <Storages/Hive/HiveWhereOptimizer.h>
#include <Storages/IStorage.h>
#include <Storages/StorageCnchHive.h>
#include <Storages/StorageInMemoryMetadata.h>
namespace DB
{
namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
    extern const int NOT_FOUND_COLUMN_IN_BLOCK;
    extern const int SUPPORT_IS_DISABLED;

}

StorageCnchHive::~StorageCnchHive()
{
    try
    {
        shutdown();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

void StorageCnchHive::shutdown()
{
}

StorageCnchHive::StorageCnchHive(
    const StorageID & table_id_,
    const String & remote_psm_,
    const String & remote_database_name_,
    const String & remote_table_name_,
    ASTPtr partition_by_ast_,
    ASTPtr cluster_by_ast_,
    ASTPtr order_by_ast_,
    bool is_create_,
    const ColumnsDescription & columns_,
    const ConstraintsDescription & constraints_,
    ContextMutablePtr context_,
    const CnchHiveSettings & settings_)
    : IStorage(table_id_)
    , WithMutableContext(context_->getGlobalContext())
    , CnchStorageCommonHelper(table_id_, remote_database_name_, remote_table_name_)
    , remote_psm(remote_psm_)
    , partition_by_ast(std::move(partition_by_ast_))
    , cluster_by_ast(std::move(cluster_by_ast_))
    , order_by_ast(std::move(order_by_ast_))
    , is_create(is_create_)
    , log(&Poco::Logger::get("StorageCnchHive"))
    , settings(settings_)
{
    StorageInMemoryMetadata metadata;
    metadata.setColumns(columns_);
    metadata.setConstraints(constraints_);

    for(const auto & col : columns_)
    {
        LOG_TRACE(log, " StorageCnchHive : col name {}  table name {}, database name{} ", col.name, table_id_.table_name, table_id_.database_name);

    }

    setInMemoryMetadata(metadata);

    //only when create table, need to check schema and storage format.
    if (is_create)
    {
        auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
        hms_client->check(columns_, remote_database, remote_table);
        checkStorageFormat();
        checkPartitionByKey();
        checkClusterByKey();
        checkSortByKey();
    }

    //
    setProperties();
}

/// Get basic select query to read from prepared pipe: remove prewhere, sampling, offset, final
static ASTPtr getBasicSelectQuery(const ASTPtr & original_query)
{
    auto query = original_query->clone();
    auto & select = query->as<ASTSelectQuery &>();
    auto & tables_in_select_query = select.refTables()->as<ASTTablesInSelectQuery &>();
    if (tables_in_select_query.children.empty())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Tables list is empty, it's a bug");
    auto & tables_element = tables_in_select_query.children[0]->as<ASTTablesInSelectQueryElement &>();
    if (!tables_element.table_expression)
        throw Exception(ErrorCodes::LOGICAL_ERROR, "There is no table expression, it's a bug");
    tables_element.table_expression->as<ASTTableExpression &>().final = false;
    tables_element.table_expression->as<ASTTableExpression &>().sample_size = nullptr;
    tables_element.table_expression->as<ASTTableExpression &>().sample_offset = nullptr;

    /// TODO @canh: can we just throw prewhere away?
    if (select.prewhere() && select.where())
        select.setExpression(ASTSelectQuery::Expression::WHERE, makeASTFunction("and", select.where(), select.prewhere()));
    else if (select.prewhere())
        select.setExpression(ASTSelectQuery::Expression::WHERE, select.prewhere()->clone());
    select.setExpression(ASTSelectQuery::Expression::PREWHERE, nullptr);
    return query;
}

ASTPtr StorageCnchHive::extractKeyExpressionList(const ASTPtr & node)
{
    if (!node)
        return std::make_shared<ASTExpressionList>();

    const auto * expr_func = node->as<ASTFunction>();

    if (expr_func && expr_func->name == "tuple")
    {
        return expr_func->arguments->clone();
    }
    else
    {
        auto res = std::make_shared<ASTExpressionList>();
        res->children.push_back(node);
        return res;
    }
}

void StorageCnchHive::setProperties()
{
    if (!partition_by_ast)
        throw Exception("PARTITION BY cannot be empty", ErrorCodes::BAD_ARGUMENTS);

    size_t col_size = getColumns().size();

    LOG_TRACE(log, " setProperties col_size : {}  remote_database_name {} remote_table_name {}", col_size, remote_database, remote_table);

    auto all_columns = getInMemoryMetadataPtr()->getColumns().getAllPhysical();
    partition_key_expr_list = extractKeyExpressionList(partition_by_ast);
    if (!partition_key_expr_list->children.empty())
    {
        auto partition_key_syntax = TreeRewriter(getContext()).analyze(partition_key_expr_list, all_columns);
        partition_key_expr = ExpressionAnalyzer(partition_key_expr_list, partition_key_syntax, getContext()).getActions(false);

        partition_name_and_types = partition_key_expr->getSampleBlock().getNamesAndTypesList();
    }

    if (cluster_by_ast)
    {
        cluster_by_expr_list = extractKeyExpressionList(cluster_by_ast->children.front());

        auto cluster_by_key_syntax = TreeRewriter(getContext()).analyze(cluster_by_expr_list, all_columns);
        cluster_by_key_expr = ExpressionAnalyzer(cluster_by_expr_list, cluster_by_key_syntax, getContext()).getActions(false);

        ASTLiteral * literal = cluster_by_ast->children.back()->as<ASTLiteral>();
        cluster_by_total_bucket_number = literal->value.get<Int64>();
        cluster_by_columns = cluster_by_key_expr->getSampleBlock().getNames();
    }

    if (order_by_ast)
    {
        sorting_key_expr_list = extractKeyExpressionList(order_by_ast);
        if (!sorting_key_expr_list->children.empty())
        {
            auto sorting_key_syntax = TreeRewriter(getContext()).analyze(sorting_key_expr_list, all_columns);
            sorting_key_expr = ExpressionAnalyzer(sorting_key_expr_list, sorting_key_syntax, getContext()).getActions(false);

            sorting_name_and_types = sorting_key_expr->getSampleBlock().getNamesAndTypesList();
            sorting_key_columns = sorting_key_expr->getSampleBlock().getNames();
        }

        const NamesAndTypesList & minmax_idx_columns_with_types = sorting_key_expr->getRequiredColumnsWithTypes();
        minmax_idx_expr = std::make_shared<ExpressionActions>(std::make_shared<ActionsDAG>(minmax_idx_columns_with_types));
        for (const NameAndTypePair & column : minmax_idx_columns_with_types)
        {
            if (minmax_idx_columns.end() == std::find(minmax_idx_columns.begin(), minmax_idx_columns.end(), column.name))
            {
                minmax_idx_columns.emplace_back(column.name);
                minmax_idx_column_types.emplace_back(column.type);
            }
        }
    }
}

void StorageCnchHive::checkStorageFormat()
{
    if (table == nullptr)
    {
        auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
        table = std::make_shared<Apache::Hadoop::Hive::Table>();
        hms_client->getTable(*table, remote_database, remote_table);
    }

    const String format = (*table).sd.outputFormat;
    if (format.find("parquet") == String::npos)
        throw Exception("CnchHive only support parquet format. Current format is " + format + " .", ErrorCodes::BAD_ARGUMENTS);
}

bool StorageCnchHive::isBucketTable() const
{
    return cluster_by_ast != nullptr;
}

void StorageCnchHive::checkSortByKey()
{
    if (table == nullptr)
    {
        auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
        table = std::make_shared<Apache::Hadoop::Hive::Table>();
        hms_client->getTable(*table, remote_database, remote_table);
    }

    auto sortcols = (*table).sd.sortCols;
    auto sorting_key_expr_list_local = extractKeyExpressionList(order_by_ast);
    if (sorting_key_expr_list_local->children.size() != sortcols.size())
        throw Exception("CnchHive sort cols doesn't match .", ErrorCodes::BAD_ARGUMENTS);

    if (!sorting_key_expr_list_local->children.empty())
    {
        auto all_columns = getInMemoryMetadataPtr()->getColumns().getAllPhysical();
        auto sorting_key_syntax = TreeRewriter(getContext()).analyze(sorting_key_expr_list_local, all_columns);
        auto sorting_key_expr_local = ExpressionAnalyzer(sorting_key_expr_list_local, sorting_key_syntax, getContext()).getActions(false);

        auto sorting_name_and_types_local = sorting_key_expr_local->getSampleBlock().getNamesAndTypesList();
        for (const ASTPtr & ast : sorting_key_expr_list_local->children)
        {
            String col_name = ast->getColumnName();
            size_t i = 0;
            for (; i < sortcols.size(); ++i)
            {
                if (sortcols[i].col == col_name)
                {
                    sorting_key_columns.push_back(col_name);
                    break;
                }
            }

            if (i >= sortcols.size())
                throw Exception("CnchHive sorting col doesn't match .", ErrorCodes::BAD_ARGUMENTS);
        }
    }
}

void StorageCnchHive::checkPartitionByKey()
{
    if (table == nullptr)
    {
        auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
        table = std::make_shared<Apache::Hadoop::Hive::Table>();
        hms_client->getTable(*table, remote_database, remote_table);
    }

    std::vector<FieldSchema> partitionkeys = (*table).partitionKeys;
    auto partition_key_expr_list_local = extractKeyExpressionList(partition_by_ast);
    if (partition_key_expr_list_local->children.size() != partitionkeys.size())
        throw Exception("CnchHive partition Key doesn't match .", ErrorCodes::BAD_ARGUMENTS);

    if (!partition_key_expr_list_local->children.empty())
    {
        auto all_columns = getInMemoryMetadataPtr()->getColumns().getAllPhysical();
        auto partition_key_syntax = TreeRewriter(getContext()).analyze(partition_key_expr_list_local, all_columns);
        auto partition_key_expr_local
            = ExpressionAnalyzer(partition_key_expr_list_local, partition_key_syntax, getContext()).getActions(false);

        auto partition_name_and_types_local = partition_key_expr_local->getSampleBlock().getNamesAndTypesList();
        for (const NameAndTypePair & column_data : partition_name_and_types_local)
        {
            const String col_name = column_data.name;
            size_t i = 0;
            for (; i < partitionkeys.size(); i++)
            {
                if (partitionkeys[i].name == col_name)
                    break;
            }
            if (i >= partitionkeys.size())
                throw Exception("CnchHive partition Key doesn't match .", ErrorCodes::BAD_ARGUMENTS);
        }
    }
}

void StorageCnchHive::checkClusterByKey()
{
    if (table == nullptr)
    {
        auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
        table = std::make_shared<Apache::Hadoop::Hive::Table>();
        hms_client->getTable(*table, remote_database, remote_table);
    }

    const auto & hivebucket_cols = (*table).sd.bucketCols;
    LOG_TRACE(log, " hivebucket_cols size = {}", hivebucket_cols.size());

    for (const auto & col : hivebucket_cols)
        LOG_TRACE(log, " hivebucket_cols col = {}", col);


    if (cluster_by_ast)
    {
        auto cluster_by_expr_list_local = extractKeyExpressionList(cluster_by_ast->children.front());
        if (cluster_by_expr_list_local->children.size() != hivebucket_cols.size())
            throw Exception("CnchHive hiveBucket doesn't match.", ErrorCodes::BAD_ARGUMENTS);

        auto all_columns = getInMemoryMetadataPtr()->getColumns().getAllPhysical();
        auto cluster_by_key_syntax = TreeRewriter(getContext()).analyze(cluster_by_expr_list_local, all_columns);
        auto cluster_by_key_expr_local
            = ExpressionAnalyzer(cluster_by_expr_list_local, cluster_by_key_syntax, getContext()).getActions(false);

        ASTLiteral * literal = cluster_by_ast->children.back()->as<ASTLiteral>();
        auto cluster_by_total_bucket_number_local = literal->value.get<Int64>();
        if ((*table).sd.numBuckets != cluster_by_total_bucket_number_local)
            throw Exception("CnchHive hiveBucket number doesn't match .", ErrorCodes::BAD_ARGUMENTS);

        auto cluster_by_columns_local = cluster_by_key_expr_local->getSampleBlock().getNames();

        size_t cluster_by_expr_size = cluster_by_expr_list_local->children.size();
        for (size_t i = 0; i < cluster_by_expr_size; ++i)
        {
            const String clustering_key_column = cluster_by_expr_list_local->children[i]->getColumnName();
            LOG_TRACE(log, " clustering_key_column = {}", clustering_key_column);
            auto it = std::find(hivebucket_cols.begin(), hivebucket_cols.end(), clustering_key_column);
            if (it == hivebucket_cols.end())
                throw Exception(
                    "CnchHive hiveBucket col doesn't match . clustering_key_column is not hiveBucket col" + clustering_key_column + " .",
                    ErrorCodes::BAD_ARGUMENTS);
        }
    }
    else if (!hivebucket_cols.empty())
        throw Exception("CnchHive hiveBucket doesn't match .", ErrorCodes::BAD_ARGUMENTS);
}

QueryProcessingStage::Enum StorageCnchHive::getQueryProcessingStage(
    ContextPtr local_context, QueryProcessingStage::Enum, const StorageMetadataPtr &, SelectQueryInfo &) const
{
    const auto & local_settings = local_context->getSettingsRef();

    if (local_settings.distributed_perfect_shard || local_settings.distributed_group_by_no_merge)
    {
        return QueryProcessingStage::Complete;
    }
    else if (auto worker_group = local_context->tryGetCurrentWorkerGroup())
    {
        size_t num_workers = worker_group->getShardsInfo().size();
        size_t result_size = (num_workers * local_settings.max_parallel_replicas);
        return result_size == 1 ? QueryProcessingStage::Complete : QueryProcessingStage::WithMergeableState;
    }
    else
    {
        return QueryProcessingStage::WithMergeableState;
    }
}

HiveDataPartsCNCHVector StorageCnchHive::prepareReadContext(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    unsigned num_streams)
{
    auto txn = local_context->getCurrentTransaction();
    if (local_context->getServerType() == ServerType::cnch_server && txn && txn->isReadOnly())
        local_context->getCnchTransactionCoordinator().touchActiveTimestampByTable(getStorageID(), txn);

    metadata_snapshot->check(column_names, getVirtuals(), getStorageID());

    auto worker_group = local_context->getCurrentWorkerGroup();
    healthCheckForWorkerGroup(local_context, worker_group);

    auto parts = selectPartsToRead(column_names, local_context, query_info, num_streams);
    LOG_INFO(log, "Number of parts to read: {}", parts.size());

    String local_table_name = getCloudTableName(local_context);

    LOG_TRACE(log, " local table name = {}", local_table_name);
    collectResource(local_context, parts, local_table_name);

    return parts;
}

HiveDataPartsCNCHVector StorageCnchHive::selectPartsToRead(
    const Names & /*column_names_to_return*/, ContextPtr local_context, const SelectQueryInfo & query_info, unsigned num_streams)
{
    HiveDataPartsCNCHVector data_parts;

    // TEST_START(testlog);
    auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
    if (hms_client)
    {
        Stopwatch watch;
        HivePartitionVector pruned_partitions = selectPartitionsByPredicate(local_context, query_info, hms_client);
        // LOG_INFO(log, std::fixed << std::setprecision(3) << "select partitions by predicate in " << watch.elapsedSeconds() << " sec.");
        // TEST_LOG(testlog, "select partitions by predicate.");
        auto bucketnums = getSelectedBucketNumbers(query_info, local_context);
        LOG_TRACE(log, " partition size = {}", pruned_partitions.size());
        data_parts = getDataPartsInPartitions(hms_client, pruned_partitions, local_context, query_info, num_streams, bucketnums);
        // LOG_INFO(log, std::fixed << std::setprecision(3) << "get dataparts in partitions in " << watch.elapsedSeconds() << " sec.");
        // TEST_LOG(testlog, "get dataparts in partitions.");
        LOG_DEBUG(log, "Total number of parts get from hive: {}", data_parts.size());
        // ProfileEvents::increment(ProfileEvents::CatalogTime, watch.elapsedMilliseconds());
    }

    // TEST_END(testlog, "Get pruned parts from HiveMetastore Service");

    LOG_INFO(log, "Number of parts get from HiveMetaStore: {}", data_parts.size());

    return data_parts;
}

HivePartitionVector StorageCnchHive::selectPartitionsByPredicate(
    ContextPtr local_context, const SelectQueryInfo & query_info, std::shared_ptr<HiveMetastoreClient> & hms_client)
{
    HivePartitionVector result;
    HivePartitionVector all;
    ASTSelectQuery & select_ast = typeid_cast<ASTSelectQuery &>(*query_info.query);
    HiveWhereOptimizer optimizer(query_info, local_context, shared_from_this());
    optimizer.implicitwhereOptimize();
    String all_usefull_filter;
    String can_covert_to_filter;
    bool is_can_convert = false;
    bool is_all_usefull = optimizer.getUsefullFilter(all_usefull_filter);
    const String table_path = getFullTablePath();

    ASTs where_conditions = optimizer.getWhereOptimizerConditions(select_ast.where());

    /// ThriftHiveMetatsore get_partition_by_filter have some limits:
    /// (1)filter parameter must be String.
    /// (2)only Comparison Function can be effective,
    /// if where condition is "app_name IN ('aweme', 'test1', 'test2') AND (date = '20220105') AND (live_id = 1)"
    /// it will throw exception, so we need to get useful filter, avoid throw exception.
    if (!where_conditions.empty())
    {
        is_can_convert = optimizer.convertWhereToUsefullFilter(where_conditions, can_covert_to_filter);
    }

    if (select_ast.implicitWhere())
    {
        if (is_all_usefull)
        {
            /// case: app_name IN ('test', 'test1')
            /// this condition will push to where, need convert it to "(app_name = 'test') or (app_name = 'test1')"
            if (is_can_convert)
                all_usefull_filter = all_usefull_filter + " AND " + can_covert_to_filter;

            LOG_TRACE(log, "Cnchhive all_usefull_filter:  {} ", all_usefull_filter);

            all = hms_client->getPartitionsByFilter(shared_from_this(), remote_database, remote_table, table_path, all_usefull_filter);

            LOG_TRACE(log, "CnchHive getPartitionsByFilter size is {}", all.size());

            return all;
        }
        else
        {
            /// case: app_name IN ('test')
            /// this condition will push to implicitwhere(app_name is partition key), need to convert it to "app_name = ('test')"
            String implicit_where_filter;
            bool implicit_can_convert = optimizer.convertImplicitWhereToUsefullFilter(implicit_where_filter);
            if (implicit_can_convert)
            {
                if (is_can_convert)
                    implicit_where_filter = implicit_where_filter + " AND " + can_covert_to_filter;

                LOG_TRACE(log, "Cnchhive can_covert_to_filter:  {}", implicit_where_filter);

                all = hms_client->getPartitionsByFilter(
                    shared_from_this(), remote_database, remote_table, table_path, implicit_where_filter);

                LOG_TRACE(log, "CnchHive getPartitionsByFilter size is {}", all.size());

                return all;
            }
        }
    }

    all = hms_client->getPartitionList(shared_from_this(), remote_database, remote_table, table_path);
    LOG_TRACE(log, "CnchHive getPartitionList size is {}", all.size());

    // result = eliminatePartitions(all, context, query_info, optimizer);

    LOG_TRACE(log, "CnchHive after purn get PartitionList size is {}", all.size());

    return all;
}

/// IF hive table is bucket table, according to cluster by key column,
/// bucket_id = column.hashcode % bucket.num
/// so, we need to have mirror implement of hive hash algorithms to get the bucket_id.
/// meamwhile, bucket_id will be the partial of part name.
/// this function will have two steps:
///  (1) get where condition related to cluster by key column
///  (2) according to column, get the bucket index
/// TODO: handle more than one bucket column expression
std::set<Int64> StorageCnchHive::getSelectedBucketNumbers(const SelectQueryInfo & query_info, ContextPtr & local_context)
{
    if (!isBucketTable() || cluster_by_columns.size() != 1)
        return {};

    std::set<Int64> bucket_numbers;
    ASTPtr where_expression = query_info.query->as<ASTSelectQuery>()->where();
    HiveWhereOptimizer optimizer(query_info, local_context, shared_from_this());
    ASTs bucket_column_expressions
        = extractBucketColumnExpression(optimizer.getWhereOptimizerConditions(where_expression), cluster_by_columns);

    LOG_TRACE(log, "bucket column expression size: {}", bucket_column_expressions.size());
    if (bucket_column_expressions.empty() || bucket_column_expressions.size() > 1)
        return {};

    Block sample_block;
    NamesAndTypesList source_columns = cluster_by_key_expr->getSampleBlock().getNamesAndTypesList();
    auto bucket_column_expression = bucket_column_expressions[0];
    auto syntax_result = TreeRewriter(local_context).analyze(bucket_column_expression, source_columns);
    ExpressionActionsPtr const_actions = ExpressionAnalyzer{bucket_column_expression, syntax_result, local_context}.getConstActions();
    const_actions->execute(sample_block);

    LOG_TRACE(log, "select total_bucket_number: {} sample block size: {}", cluster_by_total_bucket_number, sample_block.columns());

    createHiveBucketColumn(sample_block, cluster_by_key_expr->getSampleBlock(), cluster_by_total_bucket_number, local_context);

    auto bucket_number = sample_block.getByPosition(sample_block.columns() - 1).column->getInt(0); // this block only contains one row
    bucket_numbers.insert(bucket_number);

    for (auto bucket_number_elem : bucket_numbers)
        LOG_TRACE(log, "bucket_number: {}", bucket_number_elem);

    return bucket_numbers;
}

String StorageCnchHive::getFullTablePath()
{
    std::shared_ptr<Apache::Hadoop::Hive::Table> temp_table = nullptr;
    {
        std::lock_guard lock(mutex);
        temp_table = table;
    }

    if (temp_table == nullptr)
    {
        LOG_TRACE(log, " get table path, remote_psm: {}", remote_psm);
        auto hms_client = HiveMetastoreClientFactory::instance().getOrCreate(remote_psm, settings);
        auto local_temp_table = std::make_shared<Apache::Hadoop::Hive::Table>();
        hms_client->getTable(*local_temp_table, remote_database, remote_table);

        std::lock_guard lock(mutex);
        if (table == nullptr)
            table = local_temp_table;
    }

    Poco::URI uri((*table).sd.location);
    LOG_TRACE(log, "table location: {}", (*table).sd.location);

    if (uri.getScheme() == "hdfs")
    {
        return uri.getPath();
    }
    else
    {
        LOG_ERROR(log, "remote hive location path only support hdfs. Currently hdfs schema is :{}", uri.getScheme());
        throw Exception("remote hive location path only support hdfs now", ErrorCodes::LOGICAL_ERROR);
    }
}

HiveDataPartsCNCHVector StorageCnchHive::getDataPartsInPartitions(
    std::shared_ptr<HiveMetastoreClient> & hms_client,
    HivePartitionVector & partitions,
    ContextPtr local_context,
    const SelectQueryInfo & query_info,
    unsigned num_streams,
    const std::set<Int64> & required_bucket_numbers)
{
    if (partitions.empty())
        return {};

    HiveDataPartsCNCHVector hive_files;
    std::mutex hive_files_mutex;

    if (num_streams == 1 &&  partitions.size() > num_streams)
        num_streams = partitions.size();

    LOG_TRACE(log, " num_streams size = {} partitions size = {}", num_streams, partitions.size());


    ThreadPool thread_pool(num_streams);
    // ExceptionHandler exception_handler;

    for (auto & partition : partitions)
    {
        thread_pool.scheduleOrThrow([&] {
            auto hive_files_in_partition
                = collectHiveFilesFromPartition(hms_client, partition, local_context, query_info, required_bucket_numbers);
            if (!hive_files_in_partition.empty())
            {
                std::lock_guard<std::mutex> lock(hive_files_mutex);
                hive_files.insert(std::end(hive_files), std::begin(hive_files_in_partition), std::end(hive_files_in_partition));
            }
        });
    }
    thread_pool.wait();
    // exception_handler.throwIfException();

    LOG_TRACE(log, " hive parts size = {}", hive_files.size());

    return hive_files;
}

HiveDataPartsCNCHVector StorageCnchHive::collectHiveFilesFromPartition(
    std::shared_ptr<HiveMetastoreClient> & hms_client,
    HivePartitionPtr & partition,
    ContextPtr local_context,
    const SelectQueryInfo & /*query_info*/,
    const std::set<Int64> & required_bucket_numbers)
{
    return hms_client->getDataPartsInPartition(
        shared_from_this(), partition, local_context->getHdfsConnectionParams(), required_bucket_numbers);
}

void StorageCnchHive::collectResource(ContextPtr local_context, const HiveDataPartsCNCHVector & parts, const String & local_table_name)
{
    auto cnch_resource = local_context->getCnchServerResource();
    auto create_table_query = getCreateQueryForCloudTable(getCreateTableSql(), local_table_name, local_context);

    LOG_TRACE(log, " create table query {}", create_table_query);

    cnch_resource->setWorkerGroup(local_context->getCurrentWorkerGroup());
    cnch_resource->addCreateQuery(local_context, shared_from_this(), create_table_query, local_table_name);
    cnch_resource->addDataParts(getStorageUUID(), parts);
}

Pipe StorageCnchHive::read(
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    size_t max_block_size,
    unsigned num_streams)
{
    QueryPlan plan;
    read(plan, column_names, metadata_snapshot, query_info, local_context, processed_stage, max_block_size, num_streams);
    return plan.convertToPipe(
        QueryPlanOptimizationSettings::fromContext(local_context), BuildQueryPipelineSettings::fromContext(local_context));
}

void StorageCnchHive::read(
    QueryPlan & query_plan,
    const Names & column_names,
    const StorageMetadataPtr & metadata_snapshot,
    SelectQueryInfo & query_info,
    ContextPtr local_context,
    QueryProcessingStage::Enum processed_stage,
    const size_t /*max_block_size*/,
    unsigned num_streams)
{
    LOG_TRACE(log, " read  num_streams = {}", num_streams);

    auto data_parts = prepareReadContext(column_names, metadata_snapshot, query_info, local_context, num_streams);
    Block header = InterpreterSelectQuery(query_info.query, local_context, SelectQueryOptions(processed_stage)).getSampleBlock();

    auto worker_group = local_context->getCurrentWorkerGroup();
    /// Return directly (with correct header) if no shard read from
    if (!worker_group || worker_group->getShardsInfo().empty())
    {
        LOG_TRACE(log, " worker group empty ");
        Pipe pipe(std::make_shared<NullSource>(header));
        auto read_from_pipe = std::make_unique<ReadFromPreparedSource>(std::move(pipe));
        read_from_pipe->setStepDescription("Read from NullSource (CnchMergeTree)");
        query_plan.addStep(std::move(read_from_pipe));
        return;
    }

    LOG_TRACE(log, " data parts size = {}", data_parts.size());

    /// If no parts to read from - execute locally, must make sure that all stages are executed
    /// because CnchMergeTree is a high order storage
    if (data_parts.empty())
    {
        /// Stage 1: read from source table, just assume we read everything
        const auto & source_columns = query_info.syntax_analyzer_result->required_source_columns;
        auto fetch_column_header = Block(NamesAndTypes{source_columns.begin(), source_columns.end()});
        Pipe pipe(std::make_shared<NullSource>(std::move(fetch_column_header)));
        /// Stage 2: (partial) aggregation and projection if any
        auto query = getBasicSelectQuery(query_info.query);
        InterpreterSelectQuery(query, local_context, std::move(pipe), SelectQueryOptions(processed_stage)).buildQueryPlan(query_plan);
        return;
    }

    LOG_TRACE(log, "Original query before rewrite: {}", queryToString(query_info.query));
    auto modified_query_ast = rewriteSelectQuery(query_info.query, getDatabaseName(), getCloudTableName(local_context));

    const Scalars & scalars = local_context->hasQueryContext() ? local_context->getQueryContext()->getScalars() : Scalars{};

    ClusterProxy::SelectStreamFactory select_stream_factory = ClusterProxy::SelectStreamFactory(
        header,
        processed_stage,
        StorageID::createEmpty(), /// Don't check whether table exists in cnch-worker
        scalars,
        false,
        local_context->getExternalTables());

    ClusterProxy::executeQuery(query_plan, select_stream_factory, log, modified_query_ast, local_context, worker_group);

    if (!query_plan.isInitialized())
        throw Exception("Pipeline is not initialized", ErrorCodes::LOGICAL_ERROR);
}

void registerStorageCnchHive(StorageFactory & factory)
{
    StorageFactory::StorageFeatures features{
        .supports_settings = true,
        .supports_projections = true,
        .supports_sort_order = true,
    };

    LOG_DEBUG(&Poco::Logger::get("registerStorageCnchHive"), "registerStorageCnchHive ");
    factory.registerStorage(
        "CnchHive",
        [](const StorageFactory::Arguments & args) {
            ASTs & engine_args = args.engine_args;
            if (engine_args.size() != 3)
                throw Exception(
                    "Storage CnchHive require 3 parameaters: "
                    " remote_psm, remote_database, remote_table. ",
                    ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

            ASTPtr partition_by_ast = nullptr;
            if (args.storage_def->partition_by)
                partition_by_ast = args.storage_def->partition_by->ptr();

            ASTPtr cluster_by_ast = nullptr;
            if (args.storage_def->cluster_by)
                cluster_by_ast = args.storage_def->cluster_by->ptr();

            ASTPtr order_by_ast = nullptr;
            if (args.storage_def->order_by)
                order_by_ast = args.storage_def->order_by->ptr();

            engine_args[0] = evaluateConstantExpressionOrIdentifierAsLiteral(engine_args[0], args.getLocalContext());
            engine_args[1] = evaluateConstantExpressionOrIdentifierAsLiteral(engine_args[1], args.getLocalContext());
            engine_args[2] = evaluateConstantExpressionOrIdentifierAsLiteral(engine_args[2], args.getLocalContext());

            const String & remote_psm = engine_args[0]->as<ASTLiteral &>().value.safeGet<String>();
            const String & remote_database = engine_args[1]->as<ASTLiteral &>().value.safeGet<String>();
            const String & remote_table = engine_args[2]->as<ASTLiteral &>().value.safeGet<String>();
            CnchHiveSettings storage_settings = args.getContext()->getCnchHiveSettings();
            storage_settings.loadFromQuery(*args.storage_def);

            return StorageCnchHive::create(
                args.table_id,
                remote_psm,
                remote_database,
                remote_table,
                partition_by_ast,
                cluster_by_ast,
                order_by_ast,
                args.create,
                args.columns,
                args.constraints,
                args.getContext(),
                storage_settings);
        },
        features);
}

}
