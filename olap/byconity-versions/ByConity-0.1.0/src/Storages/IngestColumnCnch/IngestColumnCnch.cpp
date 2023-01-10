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

#include <Storages/IngestColumnCnch/IngestColumnCnch.h>
#include <Storages/IngestColumnCnch/IngestColumnCnchHelper.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/PartitionCommands.h>
#include <DaemonManager/DaemonManagerClient.h>
#include <DaemonManager/BackgroundJob.h>
#include <CloudServices/CnchWorkerClientPools.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/CnchCreateQueryHelper.h>
#include <Catalog/Catalog.h>
#include <DataTypes/MapHelpers.h>
#include <CloudServices/CnchServerClient.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <DataStreams/SquashingBlockOutputStream.h>
#include <DataStreams/AddingDefaultBlockOutputStream.h>
#include <CloudServices/CnchBGThreadCommon.h>
#include <ResourceManagement/VWScheduleAlgo.h>
#include <Interpreters/VirtualWarehousePool.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Parsers/queryToString.h>
#include <CloudServices/CnchWorkerResource.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int SYSTEM_ERROR;
    extern const int LOGICAL_ERROR;
    extern const int ABORTED;
    extern const int BAD_ARGUMENTS;
    extern const int DUPLICATE_COLUMN;
    extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
}

namespace
{

String createCloudMergeTreeCreateQuery(
    const StorageCnchMergeTree & table,
    const String & suffix
)
{
    String table_name = table.getTableName() + '_' + suffix;
    String database_name = table.getDatabaseName() + '_' + suffix;
    return table.getCreateQueryForCloudTable(
        table.getCreateTableSql(),
        table_name, database_name);
}

WorkerGroupHandle getWorkerGroup(const StorageCnchMergeTree & cnch_table, ContextPtr context)
{
    String vw_name = cnch_table.getSettings()->cnch_vw_write;
    auto vw_handle = context->getVirtualWarehousePool().get(vw_name);
    auto value = context->getSettingsRef().vw_schedule_algo.value;
    auto algo = ResourceManagement::toVWScheduleAlgo(&value[0]);
    return vw_handle->pickWorkerGroup(algo);
}

String reconstructAlterQuery(
    const StorageID & target_storage_id,
    const StorageID & source_storage_id,
    const struct PartitionCommand & command,
    const String & suffix)
{
    auto get_column_name = [] (const String & input)
    {
        if (!isMapImplicitKey(input))
            return backQuoteIfNeed(input);
        String map_name = parseMapNameFromImplicitColName(input);
        String key_name = parseKeyNameFromImplicitColName(input, map_name);
        return map_name + '{' + key_name + '}';
    };

    auto get_comma_separated = [& get_column_name] (const Strings & input)
    {
        String res;
        String separator{" "};

        for (const String & s : input)
        {
            res += separator + get_column_name(s);
            separator = " ,";
        }
        return res;
    };

    const String prepared_suffix = "_" + suffix;
    String query_first_part = fmt::format("ALTER TABLE {}.{} INGEST PARTITION {} COLUMNS {}",
        backQuoteIfNeed(target_storage_id.getDatabaseName() + prepared_suffix),
        backQuoteIfNeed(target_storage_id.getTableName() + prepared_suffix),
        queryToString(command.partition),
        get_comma_separated(command.column_names));


    String query_key_part = (command.key_names.empty()) ? "" :
        fmt::format("KEY {}", get_comma_separated(command.key_names));

    String query_end_part = fmt::format(" FROM {}.{}",
        backQuoteIfNeed(source_storage_id.getDatabaseName() + prepared_suffix),
        backQuoteIfNeed(source_storage_id.getTableName() + prepared_suffix));

    return fmt::format("{} {} {}", query_first_part, query_key_part, query_end_part);
}

Names getOrderedKeys(const Names & names_to_order, const StorageInMemoryMetadata & meta_data)
{
    auto ordered_keys = meta_data.getColumnsRequiredForPrimaryKey();

    if (names_to_order.empty())
    {
        return ordered_keys;
    }
    else
    {
        for (auto & key : names_to_order)
        {
            bool found = false;
            for (auto & table_key : ordered_keys)
            {
                if (table_key == key)
                    found = true;
            }

            if (!found)
                throw Exception("Some given keys are not part of the table's primary key, please check!", ErrorCodes::BAD_ARGUMENTS);
        }

        // get reorderd ingest key
        Names res;
        for (size_t i = 0; i < ordered_keys.size(); ++i)
        {
            for (auto & key : names_to_order)
            {
                if (key == ordered_keys[i])
                    res.push_back(key);
            }
        }

        for (size_t i = 0; i < ordered_keys.size(); ++i)
        {
            if (i < res.size() && res[i] != ordered_keys[i])
                throw Exception("Reordered ingest key must be a prefix of the primary key.", ErrorCodes::BAD_ARGUMENTS);
        }

        return res;
    }
}

void checkColumnStructure(const StorageInMemoryMetadata & target_data, const StorageInMemoryMetadata & src_data, const Names & names)
{
    for (const auto & col_name : names)
    {
        const auto & target = target_data.getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, col_name);
        const auto & src = src_data.getColumns().getColumnOrSubcolumn(ColumnsDescription::GetFlags::AllPhysical, col_name);

        if (target.name != src.name)
            throw Exception("Column structure mismatch, found different names of column " + backQuoteIfNeed(col_name),
                            ErrorCodes::BAD_ARGUMENTS);

        if (!target.type->equals(*src.type))
            throw Exception("Column structure mismatch, found different types of column " + backQuoteIfNeed(col_name),
                            ErrorCodes::BAD_ARGUMENTS);
    }
}

void checkIngestColumns(const Strings & column_names, const StorageInMemoryMetadata & meta_data, bool & has_map_implicite_key)
{
    if (!meta_data.getColumns().getMaterialized().empty())
        throw Exception("There is materialized column in table which is not allowed!", ErrorCodes::BAD_ARGUMENTS);

    for (auto & primary_key : meta_data.getColumnsRequiredForPrimaryKey())
    {
        for (auto & col_name : column_names)
        {
            if (col_name == primary_key)
                throw Exception("Column " + backQuoteIfNeed(col_name) + " is part of the table's primary key which is not allowed!", ErrorCodes::BAD_ARGUMENTS);
        }
    }

    for (auto & partition_key : meta_data.getColumnsRequiredForPartitionKey())
    {
        for (auto & col_name : column_names)
        {
            if (col_name == partition_key)
                throw Exception("Column " + backQuoteIfNeed(col_name) + " is part of the table's partition key which is not allowed!", ErrorCodes::BAD_ARGUMENTS);
        }
    }

    std::unordered_set<String> all_columns;
    for (const auto & col_name : column_names)
    {
        /// Check for duplicates
        if (!all_columns.emplace(col_name).second)
            throw Exception("Ingest duplicate column " + backQuoteIfNeed(col_name), ErrorCodes::DUPLICATE_COLUMN);

        if (isMapImplicitKeyNotKV(col_name))
        {
            has_map_implicite_key = true;
            continue;
        }

        if (meta_data.getColumns().get(col_name).type->isMap())
            throw Exception("Ingest whole map column " + backQuoteIfNeed(col_name) +
                            " is not supported, you can specify a map key.", ErrorCodes::BAD_ARGUMENTS);
    }
}

} /// end namespace anonymous

void forwardIngestPartitionToWorker(
    StorageCnchMergeTree & target_table,
    StorageCnchMergeTree & source_table,
    const struct PartitionCommand & command,
    ContextPtr context
    )
{
    Poco::Logger * log = target_table.getLogger();
    TxnTimestamp txn_id = context->getCurrentTransactionID();
    std::hash<String> hasher;
    const String transaction_string = toString(txn_id.toUInt64());
    String source_database = source_table.getDatabaseName();
    const String task_id = transaction_string + "_" +
        std::to_string(hasher(queryToString(command.partition) + source_database + source_table.getTableName()));
    const String & create_target_cloud_merge_tree_query = createCloudMergeTreeCreateQuery(target_table, task_id);
    LOG_TRACE(log, "create target cloud merge tree query: {}", create_target_cloud_merge_tree_query);

    const String & create_source_cloud_merge_tree_query = createCloudMergeTreeCreateQuery(source_table, task_id);
    LOG_TRACE(log, "create source cloud merge tree query: {}", create_source_cloud_merge_tree_query);

    const String query_for_send_to_worker = reconstructAlterQuery(target_table.getStorageID(), source_table.getStorageID(), command, task_id);
    LOG_TRACE(log, "reconstruct query to send to worker {}", query_for_send_to_worker);
    WorkerGroupHandle worker_group = getWorkerGroup(target_table, context);
    auto num_of_workers = worker_group->getShardsInfo().size();
    if (!num_of_workers)
        throw Exception("No heathy worker available", ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);
    std::size_t index = std::hash<String>{}(task_id) % num_of_workers;
    auto * write_shard_ptr = &(worker_group->getShardsInfo().at(index));
    auto worker_client = worker_group->getWorkerClients().at(index);
    worker_client->sendCreateQueries(context,
        {create_target_cloud_merge_tree_query, create_source_cloud_merge_tree_query});

    CnchStorageCommonHelper::sendQueryPerShard(context, query_for_send_to_worker, *write_shard_ptr, true);
}

void StorageCloudMergeTree::ingestPartition(const StorageMetadataPtr & metadata_snapshot, const PartitionCommand & command, ContextPtr local_context)
{
    LOG_TRACE(log, "execute ingest partition in worker");

    const Names ordered_key_names = getOrderedKeys(command.key_names, *metadata_snapshot);
    String partition_id = getPartitionIDFromQuery(command.partition, local_context);

    StoragePtr source_storage = local_context->tryGetCnchWorkerResource()->getTable(StorageID{command.from_database, command.from_table});
    auto ingest_partition = std::make_shared<IngestColumnCnch::MemoryInefficientIngest>(
        this->shared_from_this(), std::move(source_storage), std::move(partition_id), command.column_names,
        ordered_key_names, local_context);

    ingest_partition->ingestPartition();
}

void ingestPartitionInServer(
    StorageCnchMergeTree & storage,
    const struct PartitionCommand & command,
    const ContextPtr local_context)
{
    Poco::Logger * log = storage.getLogger();
    LOG_DEBUG(log, "execute ingest partition in server");
    StorageMetadataPtr target_meta_data_ptr = storage.getInMemoryMetadataPtr();
    const Names & column_names = command.column_names;
    /// Order is important when execute the outer join.
    const Names ordered_key_names = getOrderedKeys(command.key_names, *target_meta_data_ptr);

    /// Perform checking in server side
    bool has_map_implicite_key = false;
    checkIngestColumns(column_names, *target_meta_data_ptr, has_map_implicite_key);
    /// Now not support ingesting to storage with compact map type
    if (has_map_implicite_key && storage.getSettings()->enable_compact_map_data)
        throw Exception("INGEST PARTITON not support compact map type now", ErrorCodes::NOT_IMPLEMENTED);


    String from_database = command.from_database.empty() ? local_context->getCurrentDatabase() : command.from_database;
    StoragePtr source_storage = local_context->getCnchCatalog()->tryGetTable(*local_context, from_database, command.from_table, local_context->getCurrentCnchStartTime());
    if (!source_storage)
        throw Exception("Failed to get StoragePtr for source table", ErrorCodes::SYSTEM_ERROR);

    auto * source_merge_tree = dynamic_cast<StorageCnchMergeTree *>(source_storage.get());
    if (!source_merge_tree)
        throw Exception("INGEST PARTITON source table only support cnch merge tree table", ErrorCodes::NOT_IMPLEMENTED);

    /// check whether the columns have equal structure between target table and source table
    StorageMetadataPtr source_meta_data_ptr = source_merge_tree->getInMemoryMetadataPtr();
    checkColumnStructure(*target_meta_data_ptr, *source_meta_data_ptr, column_names);
    checkColumnStructure(*target_meta_data_ptr, *source_meta_data_ptr, ordered_key_names);
    checkColumnStructure(*target_meta_data_ptr, *source_meta_data_ptr,
        target_meta_data_ptr->getColumnsRequiredForPartitionKey());

    TransactionCnchPtr cur_txn = local_context->getCurrentTransaction();
    String partition_id = storage.getPartitionIDFromQuery(command.partition, local_context);
    std::shared_ptr<Catalog::Catalog> catalog = local_context->getCnchCatalog();

    ServerDataPartsVector source_parts = catalog->getServerDataPartsInPartitions(source_storage, {partition_id}, local_context->getCurrentCnchStartTime(), local_context.get());
    LOG_DEBUG(log, "number of server source parts: {}", source_parts.size());
    ServerDataPartsVector visible_source_parts =
        CnchPartsHelper::calcVisibleParts(source_parts, false, CnchPartsHelper::LoggingOption::EnableLogging);
    LOG_DEBUG(log, "number of visible_server source parts: {}", source_parts.size());

#if 0
    /// lock must be acquire before fetching the source parts
    TxnTimestamp txn_id = cur_txn->getTransactionID();
    LockInfoPtr partition_lock = std::make_shared<LockInfo>(txn_id);
    partition_lock->setMode(LockMode::X);
    partition_lock->setTimeout(5000); // 5s
    partition_lock->setUUID(storage.getStorageUUID());
    partition_lock->setPartition(partition_id);

    Stopwatch lock_watch;
    if (cur_txn->tryLock(partition_lock))
        LOG_DEBUG(log, "Acquired lock in {} ms", lock_watch.elapsedMilliseconds());
    else
        throw Exception("Failed to acquire lock for ingest, please retry after a while", ErrorCodes::ABORTED);
#endif

    /// The server part must be fetch using the new timestamp, not the ts of transaction
    ServerDataPartsVector target_parts = catalog->getServerDataPartsInPartitions(storage.shared_from_this(), {partition_id}, local_context->getTimestamp(), local_context.get());
    LOG_DEBUG(log, "number of server target parts: {}", target_parts.size());
    ServerDataPartsVector visible_target_parts = CnchPartsHelper::calcVisibleParts(target_parts, false, CnchPartsHelper::LoggingOption::EnableLogging);
    LOG_DEBUG(log, "number of visible server target parts: {}", target_parts.size());

    if (visible_source_parts.empty())
    {
        LOG_INFO(log, "There is no part to ingest, do nothing");
        return;
    }

/// TODO uncomment when merge ready
#if 0
    /// remove the merge mutate tasks that could cause WW conflict
    auto daemon_manager_client_ptr = local_context->getDaemonManagerClient();
    if (!daemon_manager_client_ptr)
        throw Exception("Failed to get daemon manager client", ErrorCodes::SYSTEM_ERROR);

    const StorageID target_storage_id = getStorageID();
    std::optional<DaemonManager::BGJobInfo> merge_job_info = daemon_manager_client_ptr->getDMBGJobInfo(target_storage_id.uuid, CnchBGThreadType::MergeMutate);
    if (!merge_job_info)
    {
        throw Exception("Failed to get merge job info for " + target_storage_id.getNameForLogs() , ErrorCodes::SYSTEM_ERROR);
    }

    if (merge_job_info->host_port.empty())
        LOG_DEBUG(log, "Host port of merge job is empty, the merge thread is stopped");
    else
    {
        auto server_client_ptr = local_context->getCnchServerClient(merge_job_info->host_port);
        if (!server_client_ptr)
            throw Exception("Failed to get server client with host port " + merge_job_info->host_port, ErrorCodes::SYSTEM_ERROR);
        if (!server_client_ptr->removeMergeMutateTasksOnPartition(target_storage_id, partition_id))
            throw Exception("Failed to get remove MergeMutateTasks on partition_id " + partition_id + " for table " + target_storage_id.getNameForLogs(), ErrorCodes::SYSTEM_ERROR);
    }
#endif

    forwardIngestPartitionToWorker(storage, *source_merge_tree, command, local_context);
    cur_txn->setMainTableUUID(storage.getStorageUUID());
    cur_txn->commitV2();
    LOG_TRACE(log, "Finish ingestion partition, commit in cnch server.");
    return;
}

void StorageCnchMergeTree::ingestPartition(const struct PartitionCommand & command, const ContextPtr local_context)
{
    ingestPartitionInServer(*this, command, local_context);
}

} /// end namespace

