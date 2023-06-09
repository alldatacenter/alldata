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

#include <iterator>
#include <Catalog/Catalog.h>
#include <Catalog/CatalogFactory.h>
#include <Catalog/DataModelPartWrapper.h>
#include <Catalog/StringHelper.h>
#include <Parsers/parseQuery.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/queryToString.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Parsers/ASTCreateQuery.h>
#include <Databases/DatabaseCnch.h>
#include <common/logger_useful.h>
// #include <ext/range.h>
#include <Common/Exception.h>
#include <Common/ProfileEvents.h>
#include <Common/Status.h>
#include <Common/RpcClientPool.h>
#include <Common/serverLocality.h>
#include <Core/Types.h>
// #include <Access/MaskingPolicyDataModel.h>
// #include <Access/MaskingPolicyCommon.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/getCommitted.h>
#include <CloudServices/CnchServerClient.h>
#include <CloudServices/CnchPartsHelper.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Protos/RPCHelpers.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/PartCacheManager.h>
#include <Storages/CnchStorageCache.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <brpc/server.h>
#include <Catalog/CatalogMetricHelper.h>
#include <Statistics/ExportSymbols.h>
#include <Statistics/StatisticsBase.h>
#include <Storages/StorageDictionary.h>
#include <Dictionaries/getDictionaryConfigurationFromAST.h>
#include <Parsers/formatAST.h>

/// TODO: put all global gflags together in somewhere.
namespace brpc::policy { DECLARE_string(consul_agent_addr); }

namespace brpc { DECLARE_int32(defer_close_second);}

namespace ProfileEvents
{
    extern const int CnchTxnAllTransactionRecord;
    extern const Event CatalogConstructorSuccess;
    extern const Event CatalogConstructorFailed;
    extern const Event UpdateTableStatisticsSuccess;
    extern const Event UpdateTableStatisticsFailed;
    extern const Event GetTableStatisticsSuccess;
    extern const Event GetTableStatisticsFailed;
    extern const Event GetAvailableTableStatisticsTagsSuccess;
    extern const Event GetAvailableTableStatisticsTagsFailed;
    extern const Event RemoveTableStatisticsSuccess;
    extern const Event RemoveTableStatisticsFailed;
    extern const Event UpdateColumnStatisticsSuccess;
    extern const Event UpdateColumnStatisticsFailed;
    extern const Event GetColumnStatisticsSuccess;
    extern const Event GetColumnStatisticsFailed;
    extern const Event GetAvailableColumnStatisticsTagsSuccess;
    extern const Event GetAvailableColumnStatisticsTagsFailed;
    extern const Event RemoveColumnStatisticsSuccess;
    extern const Event RemoveColumnStatisticsFailed;
    extern const Event CreateDatabaseSuccess;
    extern const Event CreateDatabaseFailed;
    extern const Event GetDatabaseSuccess;
    extern const Event GetDatabaseFailed;
    extern const Event IsDatabaseExistsSuccess;
    extern const Event IsDatabaseExistsFailed;
    extern const Event DropDatabaseSuccess;
    extern const Event DropDatabaseFailed;
    extern const Event RenameDatabaseSuccess;
    extern const Event RenameDatabaseFailed;
    extern const Event CreateTableSuccess;
    extern const Event CreateTableFailed;
    extern const Event DropTableSuccess;
    extern const Event DropTableFailed;
    extern const Event CreateUDFSuccess;
    extern const Event CreateUDFFailed;
    extern const Event DropUDFSuccess;
    extern const Event DropUDFFailed;
    extern const Event DetachTableSuccess;
    extern const Event DetachTableFailed;
    extern const Event AttachTableSuccess;
    extern const Event AttachTableFailed;
    extern const Event IsTableExistsSuccess;
    extern const Event IsTableExistsFailed;
    extern const Event AlterTableSuccess;
    extern const Event AlterTableFailed;
    extern const Event RenameTableSuccess;
    extern const Event RenameTableFailed;
    extern const Event SetWorkerGroupForTableSuccess;
    extern const Event SetWorkerGroupForTableFailed;
    extern const Event GetTableSuccess;
    extern const Event GetTableFailed;
    extern const Event TryGetTableSuccess;
    extern const Event TryGetTableFailed;
    extern const Event TryGetTableByUUIDSuccess;
    extern const Event TryGetTableByUUIDFailed;
    extern const Event GetTableByUUIDSuccess;
    extern const Event GetTableByUUIDFailed;
    extern const Event GetTablesInDBSuccess;
    extern const Event GetTablesInDBFailed;
    extern const Event GetAllViewsOnSuccess;
    extern const Event GetAllViewsOnFailed;
    extern const Event SetTableActivenessSuccess;
    extern const Event SetTableActivenessFailed;
    extern const Event GetTableActivenessSuccess;
    extern const Event GetTableActivenessFailed;
    extern const Event GetServerDataPartsInPartitionsSuccess;
    extern const Event GetServerDataPartsInPartitionsFailed;
    extern const Event GetAllServerDataPartsSuccess;
    extern const Event GetAllServerDataPartsFailed;
    extern const Event GetDataPartsByNamesSuccess;
    extern const Event GetDataPartsByNamesFailed;
    extern const Event GetStagedDataPartsByNamesSuccess;
    extern const Event GetStagedDataPartsByNamesFailed;
    extern const Event GetAllDeleteBitmapsSuccess;
    extern const Event GetAllDeleteBitmapsFailed;
    extern const Event GetStagedPartsSuccess;
    extern const Event GetStagedPartsFailed;
    extern const Event GetDeleteBitmapsInPartitionsSuccess;
    extern const Event GetDeleteBitmapsInPartitionsFailed;
    extern const Event GetDeleteBitmapByKeysSuccess;
    extern const Event GetDeleteBitmapByKeysFailed;
    extern const Event AddDeleteBitmapsSuccess;
    extern const Event AddDeleteBitmapsFailed;
    extern const Event RemoveDeleteBitmapsSuccess;
    extern const Event RemoveDeleteBitmapsFailed;
    extern const Event FinishCommitSuccess;
    extern const Event FinishCommitFailed;
    extern const Event GetKafkaOffsetsVoidSuccess;
    extern const Event GetKafkaOffsetsVoidFailed;
    extern const Event GetKafkaOffsetsTopicPartitionListSuccess;
    extern const Event GetKafkaOffsetsTopicPartitionListFailed;
    extern const Event ClearOffsetsForWholeTopicSuccess;
    extern const Event ClearOffsetsForWholeTopicFailed;
    extern const Event DropAllPartSuccess;
    extern const Event DropAllPartFailed;
    extern const Event GetPartitionListSuccess;
    extern const Event GetPartitionListFailed;
    extern const Event GetPartitionsFromMetastoreSuccess;
    extern const Event GetPartitionsFromMetastoreFailed;
    extern const Event GetPartitionIDsSuccess;
    extern const Event GetPartitionIDsFailed;
    extern const Event CreateDictionarySuccess;
    extern const Event CreateDictionaryFailed;
    extern const Event GetCreateDictionarySuccess;
    extern const Event GetCreateDictionaryFailed;
    extern const Event DropDictionarySuccess;
    extern const Event DropDictionaryFailed;
    extern const Event DetachDictionarySuccess;
    extern const Event DetachDictionaryFailed;
    extern const Event AttachDictionarySuccess;
    extern const Event AttachDictionaryFailed;
    extern const Event GetDictionariesInDBSuccess;
    extern const Event GetDictionariesInDBFailed;
    extern const Event GetDictionarySuccess;
    extern const Event GetDictionaryFailed;
    extern const Event IsDictionaryExistsSuccess;
    extern const Event IsDictionaryExistsFailed;
    extern const Event CreateTransactionRecordSuccess;
    extern const Event CreateTransactionRecordFailed;
    extern const Event RemoveTransactionRecordSuccess;
    extern const Event RemoveTransactionRecordFailed;
    extern const Event RemoveTransactionRecordsSuccess;
    extern const Event RemoveTransactionRecordsFailed;
    extern const Event GetTransactionRecordSuccess;
    extern const Event GetTransactionRecordFailed;
    extern const Event TryGetTransactionRecordSuccess;
    extern const Event TryGetTransactionRecordFailed;
    extern const Event SetTransactionRecordSuccess;
    extern const Event SetTransactionRecordFailed;
    extern const Event SetTransactionRecordWithRequestsSuccess;
    extern const Event SetTransactionRecordWithRequestsFailed;
    extern const Event SetTransactionRecordCleanTimeSuccess;
    extern const Event SetTransactionRecordCleanTimeFailed;
    extern const Event SetTransactionRecordStatusWithOffsetsSuccess;
    extern const Event SetTransactionRecordStatusWithOffsetsFailed;
    extern const Event RollbackTransactionSuccess;
    extern const Event RollbackTransactionFailed;
    extern const Event WriteIntentsSuccess;
    extern const Event WriteIntentsFailed;
    extern const Event TryResetIntentsIntentsToResetSuccess;
    extern const Event TryResetIntentsIntentsToResetFailed;
    extern const Event TryResetIntentsOldIntentsSuccess;
    extern const Event TryResetIntentsOldIntentsFailed;
    extern const Event ClearIntentsSuccess;
    extern const Event ClearIntentsFailed;
    extern const Event WritePartsSuccess;
    extern const Event WritePartsFailed;
    extern const Event SetCommitTimeSuccess;
    extern const Event SetCommitTimeFailed;
    extern const Event ClearPartsSuccess;
    extern const Event ClearPartsFailed;
    extern const Event WriteUndoBufferConstResourceSuccess;
    extern const Event WriteUndoBufferConstResourceFailed;
    extern const Event WriteUndoBufferNoConstResourceSuccess;
    extern const Event WriteUndoBufferNoConstResourceFailed;
    extern const Event ClearUndoBufferSuccess;
    extern const Event ClearUndoBufferFailed;
    extern const Event GetUndoBufferSuccess;
    extern const Event GetUndoBufferFailed;
    extern const Event GetAllUndoBufferSuccess;
    extern const Event GetAllUndoBufferFailed;
    extern const Event GetTransactionRecordsSuccess;
    extern const Event GetTransactionRecordsFailed;
    extern const Event GetTransactionRecordsTxnIdsSuccess;
    extern const Event GetTransactionRecordsTxnIdsFailed;
    extern const Event GetTransactionRecordsForGCSuccess;
    extern const Event GetTransactionRecordsForGCFailed;
    extern const Event ClearZombieIntentSuccess;
    extern const Event ClearZombieIntentFailed;
    extern const Event WriteFilesysLockSuccess;
    extern const Event WriteFilesysLockFailed;
    extern const Event GetFilesysLockSuccess;
    extern const Event GetFilesysLockFailed;
    extern const Event ClearFilesysLockDirSuccess;
    extern const Event ClearFilesysLockDirFailed;
    extern const Event ClearFilesysLockTxnIdSuccess;
    extern const Event ClearFilesysLockTxnIdFailed;
    extern const Event GetAllFilesysLockSuccess;
    extern const Event GetAllFilesysLockFailed;
    extern const Event InsertTransactionSuccess;
    extern const Event InsertTransactionFailed;
    extern const Event RemoveTransactionSuccess;
    extern const Event RemoveTransactionFailed;
    extern const Event GetActiveTransactionsSuccess;
    extern const Event GetActiveTransactionsFailed;
    extern const Event UpdateServerWorkerGroupSuccess;
    extern const Event UpdateServerWorkerGroupFailed;
    extern const Event GetWorkersInWorkerGroupSuccess;
    extern const Event GetWorkersInWorkerGroupFailed;
    extern const Event GetTableByIDSuccess;
    extern const Event GetTableByIDFailed;
    extern const Event GetTablesByIDSuccess;
    extern const Event GetTablesByIDFailed;
    extern const Event GetAllDataBasesSuccess;
    extern const Event GetAllDataBasesFailed;
    extern const Event GetAllTablesSuccess;
    extern const Event GetAllTablesFailed;
    extern const Event GetTrashTableIDIteratorSuccess;
    extern const Event GetTrashTableIDIteratorFailed;
    extern const Event GetAllUDFsSuccess;
    extern const Event GetAllUDFsFailed;
    extern const Event GetUDFByNameSuccess;
    extern const Event GetUDFByNameFailed;
    extern const Event GetTrashTableIDSuccess;
    extern const Event GetTrashTableIDFailed;
    extern const Event GetTablesInTrashSuccess;
    extern const Event GetTablesInTrashFailed;
    extern const Event GetDatabaseInTrashSuccess;
    extern const Event GetDatabaseInTrashFailed;
    extern const Event GetAllTablesIDSuccess;
    extern const Event GetAllTablesIDFailed;
    extern const Event GetTableIDByNameSuccess;
    extern const Event GetTableIDByNameFailed;
    extern const Event GetAllWorkerGroupsSuccess;
    extern const Event GetAllWorkerGroupsFailed;
    extern const Event GetAllDictionariesSuccess;
    extern const Event GetAllDictionariesFailed;
    extern const Event ClearDatabaseMetaSuccess;
    extern const Event ClearDatabaseMetaFailed;
    extern const Event ClearTableMetaForGCSuccess;
    extern const Event ClearTableMetaForGCFailed;
    extern const Event ClearDataPartsMetaSuccess;
    extern const Event ClearDataPartsMetaFailed;
    extern const Event ClearStagePartsMetaSuccess;
    extern const Event ClearStagePartsMetaFailed;
    extern const Event ClearDataPartsMetaForTableSuccess;
    extern const Event ClearDataPartsMetaForTableFailed;
    extern const Event GetSyncListSuccess;
    extern const Event GetSyncListFailed;
    extern const Event ClearSyncListSuccess;
    extern const Event ClearSyncListFailed;
    extern const Event GetServerPartsByCommitTimeSuccess;
    extern const Event GetServerPartsByCommitTimeFailed;
    extern const Event CreateRootPathSuccess;
    extern const Event CreateRootPathFailed;
    extern const Event DeleteRootPathSuccess;
    extern const Event DeleteRootPathFailed;
    extern const Event GetAllRootPathSuccess;
    extern const Event GetAllRootPathFailed;
    extern const Event CreateMutationSuccess;
    extern const Event CreateMutationFailed;
    extern const Event RemoveMutationSuccess;
    extern const Event RemoveMutationFailed;
    extern const Event GetAllMutationsStorageIdSuccess;
    extern const Event GetAllMutationsStorageIdFailed;
    extern const Event GetAllMutationsSuccess;
    extern const Event GetAllMutationsFailed;
    extern const Event SetTableClusterStatusSuccess;
    extern const Event SetTableClusterStatusFailed;
    extern const Event GetTableClusterStatusSuccess;
    extern const Event GetTableClusterStatusFailed;
    extern const Event IsTableClusteredSuccess;
    extern const Event IsTableClusteredFailed;
    extern const Event SetTablePreallocateVWSuccess;
    extern const Event SetTablePreallocateVWFailed;
    extern const Event GetTablePreallocateVWSuccess;
    extern const Event GetTablePreallocateVWFailed;
    extern const Event GetTablePartitionMetricsSuccess;
    extern const Event GetTablePartitionMetricsFailed;
    extern const Event GetTablePartitionMetricsFromMetastoreSuccess;
    extern const Event GetTablePartitionMetricsFromMetastoreFailed;
    extern const Event UpdateTopologiesSuccess;
    extern const Event UpdateTopologiesFailed;
    extern const Event GetTopologiesSuccess;
    extern const Event GetTopologiesFailed;
    extern const Event GetTrashDBVersionsSuccess;
    extern const Event GetTrashDBVersionsFailed;
    extern const Event UndropDatabaseSuccess;
    extern const Event UndropDatabaseFailed;
    extern const Event GetTrashTableVersionsSuccess;
    extern const Event GetTrashTableVersionsFailed;
    extern const Event UndropTableSuccess;
    extern const Event UndropTableFailed;
    extern const Event GetInsertionLabelKeySuccess;
    extern const Event GetInsertionLabelKeyFailed;
    extern const Event PrecommitInsertionLabelSuccess;
    extern const Event PrecommitInsertionLabelFailed;
    extern const Event CommitInsertionLabelSuccess;
    extern const Event CommitInsertionLabelFailed;
    extern const Event TryCommitInsertionLabelSuccess;
    extern const Event TryCommitInsertionLabelFailed;
    extern const Event AbortInsertionLabelSuccess;
    extern const Event AbortInsertionLabelFailed;
    extern const Event GetInsertionLabelSuccess;
    extern const Event GetInsertionLabelFailed;
    extern const Event RemoveInsertionLabelSuccess;
    extern const Event RemoveInsertionLabelFailed;
    extern const Event RemoveInsertionLabelsSuccess;
    extern const Event RemoveInsertionLabelsFailed;
    extern const Event ScanInsertionLabelsSuccess;
    extern const Event ScanInsertionLabelsFailed;
    extern const Event ClearInsertionLabelsSuccess;
    extern const Event ClearInsertionLabelsFailed;
    extern const Event CreateVirtualWarehouseSuccess;
    extern const Event CreateVirtualWarehouseFailed;
    extern const Event AlterVirtualWarehouseSuccess;
    extern const Event AlterVirtualWarehouseFailed;
    extern const Event TryGetVirtualWarehouseSuccess;
    extern const Event TryGetVirtualWarehouseFailed;
    extern const Event ScanVirtualWarehousesSuccess;
    extern const Event ScanVirtualWarehousesFailed;
    extern const Event DropVirtualWarehouseSuccess;
    extern const Event DropVirtualWarehouseFailed;
    extern const Event CreateWorkerGroupSuccess;
    extern const Event CreateWorkerGroupFailed;
    extern const Event UpdateWorkerGroupSuccess;
    extern const Event UpdateWorkerGroupFailed;
    extern const Event TryGetWorkerGroupSuccess;
    extern const Event TryGetWorkerGroupFailed;
    extern const Event ScanWorkerGroupsSuccess;
    extern const Event ScanWorkerGroupsFailed;
    extern const Event DropWorkerGroupSuccess;
    extern const Event DropWorkerGroupFailed;
    extern const Event GetNonHostUpdateTimestampFromByteKVSuccess;
    extern const Event GetNonHostUpdateTimestampFromByteKVFailed;
    extern const Event MaskingPolicyExistsSuccess;
    extern const Event MaskingPolicyExistsFailed;
    extern const Event GetMaskingPoliciesSuccess;
    extern const Event GetMaskingPoliciesFailed;
    extern const Event PutMaskingPolicySuccess;
    extern const Event PutMaskingPolicyFailed;
    extern const Event TryGetMaskingPolicySuccess;
    extern const Event TryGetMaskingPolicyFailed;
    extern const Event GetMaskingPolicySuccess;
    extern const Event GetMaskingPolicyFailed;
    extern const Event GetAllMaskingPolicySuccess;
    extern const Event GetAllMaskingPolicyFailed;
    extern const Event GetMaskingPolicyAppliedTablesSuccess;
    extern const Event GetMaskingPolicyAppliedTablesFailed;
    extern const Event GetAllMaskingPolicyAppliedTablesSuccess;
    extern const Event GetAllMaskingPolicyAppliedTablesFailed;
    extern const Event DropMaskingPoliciesSuccess;
    extern const Event DropMaskingPoliciesFailed;
    extern const Event IsHostServerSuccess;
    extern const Event IsHostServerFailed;
    extern const Event SetBGJobStatusSuccess;
    extern const Event SetBGJobStatusFailed;
    extern const Event GetBGJobStatusSuccess;
    extern const Event GetBGJobStatusFailed;
    extern const Event GetBGJobStatusesSuccess;
    extern const Event GetBGJobStatusesFailed;
    extern const Event DropBGJobStatusSuccess;
    extern const Event DropBGJobStatusFailed;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int LOGICAL_ERROR;
    extern const int VIRTUAL_WAREHOUSE_NOT_FOUND;
    extern const int DATABASE_ALREADY_EXISTS;
    extern const int UNKNOWN_DATABASE;
    extern const int TABLE_ALREADY_EXISTS;
    extern const int UNKNOWN_TABLE;
    extern const int CATALOG_COMMIT_PART_ERROR;
    extern const int CATALOG_COMMIT_NHUT_ERROR;
    extern const int CATALOG_LOCK_PARTS_FAILURE;
    extern const int CATALOG_ALTER_TABLE_FAILURE;
    extern const int CATALOG_SERVICE_INTERNAL_ERROR;
    extern const int CATALOG_TRANSACTION_RECORD_NOT_FOUND;
    extern const int CNCH_TOPOLOGY_NOT_MATCH_ERROR;
    extern const int DICTIONARY_NOT_EXIST;
    extern const int UNKNOWN_MASKING_POLICY_NAME;
    extern const int BUCKET_TABLE_ENGINE_MISMATCH;
}

namespace Catalog
{
    static ASTPtr parseCreateQuery(const String & create_query)
    {
        Strings res;
        const char *begin = create_query.data();
        const char *end = begin + create_query.size();
        ParserQuery parser(end);
        return parseQuery(parser, begin, end, "", 0, 0);
    }

    Catalog::Catalog(Context & _context, CatalogConfig & config, String _name_space) : context(_context), name_space(_name_space)
    {
        runWithMetricSupport(
            [&] {
                /// Set defer_close_second to 30s to reuse connections. Normally, the connection is closed when a channle is destroyed after completion of the job.
                /// In the scan operation of bytekv, a new channel is created each time and destroyed when scan finished. In this case, connections are close
                /// and re-open again frequently. This will cause a lot of TIME-WAIT connections and may lead to unexpected exceptions.
                brpc::FLAGS_defer_close_second = 30;

                const char * consul_http_host = getenv("CONSUL_HTTP_HOST");
                const char * consul_http_port = getenv("CONSUL_HTTP_PORT");
                if (consul_http_host != nullptr && consul_http_port != nullptr)
                {
                    brpc::policy::FLAGS_consul_agent_addr = "http://" + std::string(consul_http_host) + ":" + std::string(consul_http_port);
                    LOG_DEBUG(log, "Using consul agent: {}", brpc::policy::FLAGS_consul_agent_addr);
                }

                meta_proxy = std::make_shared<MetastoreProxy>(config);
                max_commit_size_one_batch = context.getSettingsRef().catalog_max_commit_size;
            },
            ProfileEvents::CatalogConstructorSuccess,
            ProfileEvents::CatalogConstructorFailed);
    }

    void Catalog::createDatabase(const String & database, const UUID & uuid, const TxnTimestamp & txnID, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                if (tryGetDatabaseFromMetastore(database, ts.toUInt64()))
                {
                    throw Exception("Database already exits.", ErrorCodes::DATABASE_ALREADY_EXISTS);
                }
                else
                {
                    DB::Protos::DataModelDB db_data;

                    db_data.set_name(database);
                    db_data.set_previous_version(0);
                    db_data.set_txnid(txnID.toUInt64());
                    db_data.set_commit_time(ts.toUInt64());
                    db_data.set_status(0);
                    if (uuid != UUIDHelpers::Nil)
                        RPCHelpers::fillUUID(uuid, *(db_data.mutable_uuid()));

                    meta_proxy->addDatabase(name_space, db_data);
                }
            },
            ProfileEvents::CreateDatabaseSuccess,
            ProfileEvents::CreateDatabaseFailed);
    }

    DatabasePtr Catalog::getDatabase(const String & database, const ContextPtr & context, const TxnTimestamp & ts)
    {
        DatabasePtr res = nullptr;
        runWithMetricSupport(
            [&] {
                auto database_model = tryGetDatabaseFromMetastore(database, ts.toUInt64());
                if (database_model)
                {
                    DatabasePtr db = CatalogFactory::getDatabaseByDataModel(*database_model, context);

                    if (database_model->has_commit_time())
                        dynamic_cast<DatabaseCnch &>(*db).commit_time = TxnTimestamp{database_model->commit_time()};
                    res = db;
                }
                else
                {
                    res = nullptr;
                }
            },
            ProfileEvents::GetDatabaseSuccess,
            ProfileEvents::GetDatabaseFailed);
        return res;
    }

    bool Catalog::isDatabaseExists(const String & database, const TxnTimestamp & ts)
    {
        bool res = false;
        runWithMetricSupport(
            [&] {
                if (tryGetDatabaseFromMetastore(database, ts.toUInt64()))
                    res = true;
            },
            ProfileEvents::IsDatabaseExistsSuccess,
            ProfileEvents::IsDatabaseExistsFailed);
        return res;
    }

    void Catalog::dropDatabase(
        const String & database, const TxnTimestamp & previous_version, const TxnTimestamp & txnID, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                auto db = tryGetDatabaseFromMetastore(database, ts.toUInt64());
                if (db)
                {
                    BatchCommitRequest batch_writes;

                    DB::Protos::DataModelDB db_data;
                    db_data.CopyFrom(*db);
                    db_data.set_previous_version(previous_version.toUInt64());
                    db_data.set_txnid(txnID.toUInt64());
                    db_data.set_commit_time(ts.toUInt64());
                    db_data.set_status(Status::setDelete(db->status()));

                    String db_meta;
                    db_data.SerializeToString(&db_meta);

                    // add a record into trash as well as a new version(mark as deleted) of db meta
                    batch_writes.AddPut(SinglePutRequest(MetastoreProxy::dbTrashKey(name_space, database, ts.toUInt64()), db_meta));
                    batch_writes.AddPut(SinglePutRequest(MetastoreProxy::dbKey(name_space, database, ts.toUInt64()), db_meta));

                    /// move all tables and dictionaries under this database into trash
                    auto table_id_ptrs = meta_proxy->getAllTablesId(name_space, database);
                    auto dic_ptrs = meta_proxy->getDictionariesInDB(name_space, database);

                    String trashBD_name = database + "_" + std::to_string(ts.toUInt64());
                    LOG_DEBUG(log, "Drop database {} with {} tables and {} dictionaries in it.", database, table_id_ptrs.size(), dic_ptrs.size());
                    for (auto & table_id_ptr : table_id_ptrs)
                    {
                        checkCanbeDropped(*table_id_ptr, true);
                        /// reset database name of table_id in trash because we need to differentiate multiple versions of dropped table which may have same name.
                        table_id_ptr->set_database(trashBD_name);
                        auto table = tryGetTableFromMetastore(table_id_ptr->uuid(), UINT64_MAX);
                        if (table)
                            moveTableIntoTrash(*table, *table_id_ptr, txnID, ts, batch_writes);
                    }

                    for (auto & dic_ptr : dic_ptrs)
                    {
                        batch_writes.AddPut(
                            SinglePutRequest(MetastoreProxy::dictionaryTrashKey(name_space, trashBD_name, dic_ptr->name()), dic_ptr->SerializeAsString()));
                        batch_writes.AddDelete(SingleDeleteRequest(MetastoreProxy::dictionaryStoreKey(name_space, database, dic_ptr->name())));
                    }

                    BatchCommitResponse resp;
                    meta_proxy->batchWrite(batch_writes, resp);
                }
                else
                {
                    throw Exception("Database not found.", ErrorCodes::UNKNOWN_DATABASE);
                }
            },
            ProfileEvents::DropDatabaseSuccess,
            ProfileEvents::DropDatabaseFailed);
    }

    void Catalog::renameDatabase(
        const String & from_database, const String & to_database, [[maybe_unused]] const TxnTimestamp & txnID, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                auto database = tryGetDatabaseFromMetastore(from_database, ts.toUInt64());
                if (database)
                {
                    /// TODO: if there are too many tables in the database, the batch commit may fail because it will exceed the max batch size that bytekv allowed.
                    /// Then we should split rename tables task in sub transactions.

                    BatchCommitRequest batch_writes;
                    /// rename all tables in current database;
                    auto table_id_ptrs = meta_proxy->getAllTablesId(name_space, from_database);
                    for (auto & table_id_ptr : table_id_ptrs)
                    {
                        auto table = tryGetTableFromMetastore(table_id_ptr->uuid(), UINT64_MAX);
                        if (!table)
                            throw Exception(
                                "Cannot get metadata of table " + table_id_ptr->database() + "." + table_id_ptr->name()
                                    + " by UUID : " + table_id_ptr->uuid(),
                                ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                        if (table->commit_time() >= ts.toUInt64())
                            throw Exception("Cannot rename table with an earlier timestamp", ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                        replace_definition(*table, to_database, table_id_ptr->name());
                        table->set_txnid(txnID.toUInt64());
                        table->set_commit_time(ts.toUInt64());
                        meta_proxy->renameTable(name_space, *table, from_database, table_id_ptr->name(), table_id_ptr->uuid(), batch_writes);
                    }

                    /// remove old database record;
                    batch_writes.AddDelete(SingleDeleteRequest(MetastoreProxy::dbKey(name_space, from_database, database->commit_time())));
                    /// create new database record;
                    database->set_name(to_database);
                    database->set_previous_version(0);
                    database->set_txnid(txnID.toUInt64());
                    database->set_commit_time(ts.toUInt64());
                    batch_writes.AddPut(SinglePutRequest(MetastoreProxy::dbKey(name_space, to_database, ts.toUInt64()), database->SerializeAsString()));

                    BatchCommitResponse resp;
                    meta_proxy->batchWrite(batch_writes, resp);
                }
                else
                {
                    throw Exception("Database not found.", ErrorCodes::UNKNOWN_DATABASE);
                }
            },
            ProfileEvents::RenameDatabaseSuccess,
            ProfileEvents::RenameDatabaseFailed);
    }

    void Catalog::createTable(
        const StorageID & storage_id,
        const String & create_query,
        const String & virtual_warehouse,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                String uuid_str = UUIDHelpers::UUIDToString(storage_id.uuid);
                LOG_INFO(log, "start createTable namespace {} table_name {}, uuid {}", name_space, storage_id.getFullTableName(), uuid_str);
                Protos::DataModelTable tb_data;

                tb_data.set_database(storage_id.getDatabaseName());
                tb_data.set_name(storage_id.getTableName());
                tb_data.set_definition(create_query);
                tb_data.set_txnid(txnID.toUInt64());
                tb_data.set_commit_time(ts.toUInt64());
                RPCHelpers::fillUUID(storage_id.uuid, *(tb_data.mutable_uuid()));

                if (!virtual_warehouse.empty())
                    tb_data.set_vw_name(virtual_warehouse);

                ASTPtr ast = parseCreateQuery(create_query);
                Strings dependencies = tryGetDependency(ast);
                ///FIXME: if masking policy is ready.
                // Strings masking_policy_names = getMaskingPolicyNames(ast);
                Strings masking_policy_names = {};
                meta_proxy->createTable(name_space, tb_data, dependencies, masking_policy_names);
                meta_proxy->setTableClusterStatus(name_space, uuid_str, true);

                LOG_INFO(log, "finish createTable namespace {} table_name {}, uuid {}", name_space, storage_id.getFullTableName(), uuid_str);
            },
            ProfileEvents::CreateTableSuccess,
            ProfileEvents::CreateTableFailed);
    }

    void Catalog::createUDF(const String & db, const String & name, const String & create_query)
    {
        runWithMetricSupport(
            [&] {
                Protos::DataModelUDF udf_data;

                LOG_DEBUG(log, "start createUDF {}: {}.{}", name_space, db, name);
                //:ToDo add version and other info separately.
                udf_data.set_database(db);
                udf_data.set_function_name(name);
                udf_data.set_function_definition(create_query);
                meta_proxy->createUDF(name_space, udf_data);
            },
            ProfileEvents::CreateUDFSuccess,
            ProfileEvents::CreateUDFFailed);
    }

    void Catalog::dropUDF(const String & db, const String & name)
    {
        runWithMetricSupport(
            [&] { meta_proxy->dropUDF(name_space, db, name); }, ProfileEvents::DropUDFSuccess, ProfileEvents::DropUDFFailed);
    }

    void Catalog::dropTable(
        const StoragePtr & storage,
        [[maybe_unused]] const TxnTimestamp & previous_version,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                String table_uuid = UUIDHelpers::UUIDToString(storage->getStorageID().uuid);
                if (table_uuid.empty())
                    throw Exception("Table not found.", ErrorCodes::UNKNOWN_TABLE);

                /// get latest table version.
                auto table = tryGetTableFromMetastore(table_uuid, UINT64_MAX);

                if (table)
                {
                    const auto & db = table->database();
                    const auto & name = table->name();
                    BatchCommitRequest batch_writes;

                    Protos::TableIdentifier identifier;
                    identifier.set_database(db);
                    identifier.set_name(name);
                    identifier.set_uuid(table_uuid);

                    checkCanbeDropped(identifier, false);
                    moveTableIntoTrash(*table, identifier, txnID, ts, batch_writes);
                    ///FIXME: if masking policy is ready
                    // for (const auto & mask : storage->getColumns().getAllMaskingPolicy())
                    //     batch_writes.AddDelete(MetastoreProxy::maskingPolicyTableMappingKey(name_space, mask, table_uuid));

                    BatchCommitResponse resp;
                    meta_proxy->batchWrite(batch_writes, resp);

                    if (context.getCnchStorageCache())
                        context.getCnchStorageCache()->remove(db, name);

                    if (context.getPartCacheManager())
                        context.getPartCacheManager()->invalidPartCache(UUID(stringToUUID(table_uuid)));
                }
            },
            ProfileEvents::DropTableSuccess,
            ProfileEvents::DropTableFailed);
    }

    void Catalog::detachTable(const String & db, const String & name, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                detachOrAttachTable(db, name, ts, true);
                if (context.getCnchStorageCache())
                    context.getCnchStorageCache()->remove(db, name);
            },
            ProfileEvents::DetachTableSuccess,
            ProfileEvents::DetachTableFailed);
    }

    void Catalog::attachTable(const String & db, const String & name, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] { detachOrAttachTable(db, name, ts, false); }, ProfileEvents::AttachTableSuccess, ProfileEvents::AttachTableFailed);
    }

    bool Catalog::isTableExists(const String & db, const String & name, const TxnTimestamp & ts)
    {
        bool res = false;
        std::shared_ptr<Protos::DataModelTable> table;
        runWithMetricSupport(
            [&] {
                String table_uuid = meta_proxy->getTableUUID(name_space, db, name);

                if (!table_uuid.empty() && (table = tryGetTableFromMetastore(table_uuid, ts.toUInt64())) && !Status::isDetached(table->status()) && !Status::isDeleted(table->status()))
                    res = true;
            },
            ProfileEvents::IsTableExistsSuccess,
            ProfileEvents::IsTableExistsFailed);
        return res;
    }

    void Catalog::alterTable(
        const StoragePtr & storage,
        const String & new_create,
        const TxnTimestamp & previous_version,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                String table_uuid = meta_proxy->getTableUUID(name_space, storage->getDatabaseName(), storage->getTableName());

                if (table_uuid.empty())
                    throw Exception("Table not found.", ErrorCodes::UNKNOWN_TABLE);

                /// get latest version of the table.
                auto table = tryGetTableFromMetastore(table_uuid, UINT64_MAX);

                if (!table)
                    throw Exception(
                        "Cannot get metadata of table " + storage->getDatabaseName() + "." + storage->getTableName()
                            + " by UUID : " + table_uuid,
                        ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                if (table->commit_time() >= ts.toUInt64())
                    throw Exception("Cannot alter table with an earlier timestamp", ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                auto host_port = context.getCnchTopologyMaster()
                                     ->getTargetServer(UUIDHelpers::UUIDToString(storage->getStorageID().uuid), false)
                                     .getRPCAddress();
                if (!isLocalServer(host_port, std::to_string(context.getRPCPort())))
                    throw Exception(
                        "Cannot alter table because of choosing wrong server according to current topology, chosen server: " + host_port,
                        ErrorCodes::CNCH_TOPOLOGY_NOT_MATCH_ERROR);

                table->set_definition(new_create);
                table->set_txnid(txnID.toUInt64());
                table->set_commit_time(ts.toUInt64());
                table->set_previous_version(previous_version.toUInt64());

                // auto res = meta_proxy->alterTable(
                //     name_space, *table, storage->getOutDatedMaskingPolicy(), storage->getColumns().getAllMaskingPolicy());
                ///FIXME: if masking policy is ready @guanzhe.andy
                auto res = meta_proxy->alterTable(
                    name_space, *table, {}, {});
                if (!res)
                    throw Exception("Alter table failed.", ErrorCodes::CATALOG_ALTER_TABLE_FAILURE);

                if (context.getCnchStorageCache())
                {
                    /// update cache with nullptr and latest table commit_time to prevent an old version be inserted into cache. the cache will be reloaded in following getTable
                    context.getCnchStorageCache()->insert(
                        storage->getDatabaseName(), storage->getTableName(), table->commit_time(), nullptr);
                }
            },
            ProfileEvents::AlterTableSuccess,
            ProfileEvents::AlterTableFailed);
    }

    void Catalog::checkCanbeDropped(Protos::TableIdentifier & table_id, bool is_dropping_db)
    {
        // Get uuids of all tables which rely on current table.
        auto tables_rely_on = meta_proxy->getAllDependence(name_space, table_id.uuid());

        if (!tables_rely_on.empty())
        {
            Strings table_names;
            for (auto & uuid : tables_rely_on)
            {
                StoragePtr storage = tryGetTableByUUID(context, uuid, TxnTimestamp::maxTS(), false);
                if (storage)
                {
                    if (!is_dropping_db || storage->getDatabaseName() != table_id.database())
                    {
                        table_names.emplace_back(storage->getDatabaseName() + "." + storage->getTableName());
                    }
                }
            }
            if (!table_names.empty())
            {
                auto joiner = [](const Strings & vec) -> String {
                    String res;
                    for (auto & e : vec)
                        res += "'" + e + "'" + ", ";
                    return res;
                };
                throw Exception(
                    "Cannot drop table " + table_id.database() + "." + table_id.name() + " because table [" + joiner(table_names)
                        + "] may rely on it. Please drop these tables firstly and try again",
                    ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);
            }
        }
    }

    void Catalog::renameTable(
        const String & from_database,
        const String & from_table,
        const String & to_database,
        const String & to_table,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                String table_uuid = meta_proxy->getTableUUID(name_space, from_database, from_table);

                if (table_uuid.empty())
                    throw Exception("Table not found.", ErrorCodes::UNKNOWN_TABLE);

                /// get latest version of the table
                auto table = tryGetTableFromMetastore(table_uuid, UINT64_MAX);
                if (table)
                {
                    if (table->commit_time() >= ts.toUInt64())
                        throw Exception("Cannot rename table with an earlier timestamp", ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                    BatchCommitRequest batch_writes;
                    replace_definition(*table, to_database, to_table);
                    table->set_txnid(txnID.toUInt64());
                    table->set_commit_time(ts.toUInt64());
                    meta_proxy->renameTable(name_space, *table, from_database, from_table, table_uuid, batch_writes);
                    BatchCommitResponse resp;
                    meta_proxy->batchWrite(batch_writes, resp);

                    /// update table name in table meta entry so that we can get table part metrics correctly.
                    if (context.getPartCacheManager())
                    {
                        context.getPartCacheManager()->updateTableNameInMetaEntry(table_uuid, to_database, to_table);
                    }

                    if (context.getCnchStorageCache())
                        context.getCnchStorageCache()->remove(from_database, from_table);
                }
                else
                {
                    throw Exception(
                        "Cannot get metadata of table " + from_database + "." + from_table + " by UUID : " + table_uuid,
                        ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);
                }
            },
            ProfileEvents::RenameTableSuccess,
            ProfileEvents::RenameTableFailed);
    }

    void Catalog::setWorkerGroupForTable(const String & db, const String & name, const String & worker_group, UInt64 worker_topology_hash)
    {
        runWithMetricSupport(
            [&] {
                String table_uuid = meta_proxy->getTableUUID(name_space, db, name);
                if (table_uuid.empty())
                    throw Exception("Table not found.", ErrorCodes::UNKNOWN_TABLE);

                auto table = tryGetTableFromMetastore(table_uuid, UINT64_MAX);

                if (table)
                {
                    if (worker_group.empty())
                        table->clear_vw_name();
                    else
                        table->set_vw_name(worker_group);

                    if (worker_topology_hash == 0)
                        table->clear_worker_topology_hash();
                    else
                        table->set_worker_topology_hash(worker_topology_hash);

                    meta_proxy->updateTable(name_space, table_uuid, table->SerializeAsString(), table->commit_time());
                }
                else
                {
                    throw Exception(
                        "Cannot get metadata of table " + db + "." + name + " by UUID : " + table_uuid,
                        ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);
                }
            },
            ProfileEvents::SetWorkerGroupForTableSuccess,
            ProfileEvents::SetWorkerGroupForTableFailed);
    }

    StoragePtr Catalog::getTable(const Context & query_context, const String & db, const String & name, const TxnTimestamp & ts)
    {
        StoragePtr outRes = nullptr;
        runWithMetricSupport(
            [&] {
                String table_uuid = meta_proxy->getTableUUID(name_space, db, name);

                if (table_uuid.empty())
                    throw Exception("Table not found: " + db + "." + name, ErrorCodes::UNKNOWN_TABLE);


                auto storage_cache = context.getCnchStorageCache();
                if (storage_cache)
                {
                    if (auto storage = storage_cache->get(db, name))
                    {
                        /// Compare the table uuid to make sure we get the correct storage cache. Remove outdated cache if necessary.
                        if (UUIDHelpers::UUIDToString(storage->getStorageID().uuid) == table_uuid)
                        {
                            outRes = storage;
                            return;
                        }
                        else
                            storage_cache->remove(db, name);
                    }
                }

                auto table = tryGetTableFromMetastore(table_uuid, ts.toUInt64(), true);

                if (!table)
                    throw Exception(
                        "Cannot get metadata of table " + db + "." + name + " by UUID : " + table_uuid,
                        ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                auto res = createTableFromDataModel(query_context, *table);

                /// Try insert the storage into cache.
                if (res && storage_cache)
                {
                    auto server = context.getCnchTopologyMaster()->getTargetServer(table_uuid, true);
                    if (!server.empty() && isLocalServer(server.getRPCAddress(), std::to_string(context.getRPCPort())))
                        storage_cache->insert(db, name, table->commit_time(), res);
                }
                outRes = res;
            },
            ProfileEvents::GetTableSuccess,
            ProfileEvents::GetTableFailed);
        return outRes;
    }

    StoragePtr Catalog::tryGetTable(const Context & query_context, const String & database, const String & name, const TxnTimestamp & ts)
    {
        StoragePtr res = nullptr;
        runWithMetricSupport(
            [&] {
                try
                {
                    res = getTable(query_context, database, name, ts);
                }
                catch (Exception & e)
                {
                    /// do not need to log exception if table not found.
                    if (e.code() != ErrorCodes::UNKNOWN_TABLE)
                    {
                        tryLogDebugCurrentException(__PRETTY_FUNCTION__);
                    }
                }
            },
            ProfileEvents::TryGetTableSuccess,
            ProfileEvents::TryGetTableFailed);
        return res;
    }

    StoragePtr Catalog::tryGetTableByUUID(const Context & query_context, const String & uuid, const TxnTimestamp & ts, bool with_delete)
    {
        StoragePtr outRes = nullptr;
        runWithMetricSupport(
            [&] {
                auto table = tryGetTableFromMetastore(uuid, ts.toUInt64(), true, with_delete);
                if (!table)
                {
                    outRes = {};
                    return;
                }
                auto res = createTableFromDataModel(query_context, *table);
                outRes = res;
            },
            ProfileEvents::TryGetTableByUUIDSuccess,
            ProfileEvents::TryGetTableByUUIDFailed);
        return outRes;
    }

    StoragePtr Catalog::getTableByUUID(const Context & query_context, const String & uuid, const TxnTimestamp & ts, bool with_delete)
    {
        StoragePtr res = nullptr;
        runWithMetricSupport(
            [&] {
                auto table = tryGetTableByUUID(query_context, uuid, ts, with_delete);
                if (!table)
                    throw Exception("Cannot get table by UUID : " + uuid, ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);
                res = table;
            },
            ProfileEvents::GetTableByUUIDSuccess,
            ProfileEvents::GetTableByUUIDFailed);
        return res;
    }

    Strings Catalog::getTablesInDB(const String & database)
    {
        Strings res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAllTablesInDB(name_space, database); },
            ProfileEvents::GetTablesInDBSuccess,
            ProfileEvents::GetTablesInDBFailed);
        return res;
    }

    std::vector<StoragePtr> Catalog::getAllViewsOn(const Context & session_context, const StoragePtr & storage, const TxnTimestamp & ts)
    {
        std::vector<StoragePtr> res;
        runWithMetricSupport(
            [&] {
                Strings dependencies = meta_proxy->getAllDependence(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid));
                for (auto & dependence : dependencies)
                {
                    auto table = tryGetTableFromMetastore(dependence, ts, true);
                    if (table)
                    {
                        StoragePtr view = createTableFromDataModel(session_context, *table);
                        res.push_back(view);
                    }
                }
            },
            ProfileEvents::GetAllViewsOnSuccess,
            ProfileEvents::GetAllViewsOnFailed);
        return res;
    }

    void Catalog::setTableActiveness(const StoragePtr & storage, const bool is_active, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                /// get latest table version.
                String uuid = UUIDHelpers::UUIDToString(storage->getStorageID().uuid);
                auto table = tryGetTableFromMetastore(uuid, ts.toUInt64());

                if (!table)
                    throw Exception("Cannot get table by UUID : " + uuid, ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

                LOG_DEBUG(log, "Modify table activeness to {} ", (is_active ? "active" : "inactive"));
                table->set_status(Status::setInActive(table->status(), is_active));
                /// directly rewrite the old table metadata rather than adding a new version
                meta_proxy->updateTable(name_space, uuid, table->SerializeAsString(), table->commit_time());
            },
            ProfileEvents::SetTableActivenessSuccess,
            ProfileEvents::SetTableActivenessFailed);
    }

    bool Catalog::getTableActiveness(const StoragePtr & storage, const TxnTimestamp & ts)
    {
        bool res;
        runWithMetricSupport(
            [&] {
                String uuid = UUIDHelpers::UUIDToString(storage->getStorageID().uuid);
                /// get latest table version.
                auto table = tryGetTableFromMetastore(uuid, ts.toUInt64());
                if (table)
                {
                    res = !Status::isInActive(table->status());
                }
                else
                {
                    throw Exception("Cannot get table metadata by UUID : " + uuid, ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);
                }
            },
            ProfileEvents::GetTableActivenessSuccess,
            ProfileEvents::GetTableActivenessFailed);
        return res;
    }

    DataPartsVector Catalog::getStagedParts(const StoragePtr & table, const TxnTimestamp & ts, const NameSet * partitions)
    {
        DataPartsVector outRes;
        runWithMetricSupport(
            [&] {
                auto * storage = dynamic_cast<MergeTreeMetaBase *>(table.get());
                if (!storage)
                    throw Exception("Table is not a merge tree", ErrorCodes::BAD_ARGUMENTS);
                String table_uuid = UUIDHelpers::UUIDToString(table->getStorageUUID());

                IMetaStore::IteratorPtr mIt;
                bool need_partitions_check = false;
                if (partitions && partitions->size() == 1)
                {
                    mIt = meta_proxy->getStagedPartsInPartition(name_space, table_uuid, *(partitions->begin()));
                }
                else
                {
                    /// TODO: find out more efficient way to scan staged parts only in the requested partitions.
                    /// currently seems not a bottleneck because the total number of staged parts won't be very large
                    mIt = meta_proxy->getStagedParts(name_space, table_uuid);
                    need_partitions_check = (partitions != nullptr);
                }

                DataModelPartPtrVector models;
                while (mIt->next())
                {
                    /// if ts is set, exclude model whose txn id > ts
                    if (ts.toUInt64())
                    {
                        const auto & key = mIt->key();
                        auto pos = key.find_last_of('_');
                        if (pos != String::npos)
                        {
                            UInt64 txn_id = std::stoull(key.substr(pos + 1, String::npos), nullptr);
                            if (txn_id > ts)
                                continue;
                        }
                    }

                    DataModelPartPtr model = std::make_shared<Protos::DataModelPart>();
                    model->ParseFromString(mIt->value());
                    /// exclude model whose commit ts > ts
                    if (ts.toUInt64() && model->has_commit_time() && model->commit_time() > ts.toUInt64())
                        continue;
                    /// exclude model not belong to the given partitions
                    if (need_partitions_check && !partitions->count(model->part_info().partition_id()))
                        continue;
                    models.push_back(std::move(model));
                }

                DataPartsVector res;
                res.reserve(models.size());
                for (auto & model : models)
                {
                    res.push_back(createPartFromModel(*storage, *model));
                }

                /// remove uncommitted parts
                if (ts)
                {
                    getCommittedDataParts(res, ts, this);
                }
                outRes = res;
            },
            ProfileEvents::GetStagedPartsSuccess,
            ProfileEvents::GetStagedPartsFailed);
        return outRes;
    }

    DB::ServerDataPartsVector Catalog::getServerDataPartsInPartitions(
        const ConstStoragePtr & storage, const Strings & partitions, const TxnTimestamp & ts, const Context * session_context)
    {
        ServerDataPartsVector outRes;
        runWithMetricSupport(
            [&] {
                Stopwatch watch;
                auto fall_back = [&]() {
                    ServerDataPartsVector res;
                    auto & merge_tree_storage = dynamic_cast<const MergeTreeMetaBase &>(*storage);
                    Strings all_partitions = getPartitionIDsFromMetastore(storage);
                    auto parts_model = getDataPartsMetaFromMetastore(storage, partitions, all_partitions, ts);
                    for (auto & part_model_ptr : parts_model)
                    {
                        auto part_model_wrapper = createPartWrapperFromModel(merge_tree_storage, *part_model_ptr);
                        res.push_back(std::make_shared<ServerDataPart>(std::move(part_model_wrapper)));
                    }
                    return res;
                };

                ServerDataPartsVector res;
                if (!dynamic_cast<const MergeTreeMetaBase *>(storage.get()))
                {
                    outRes = res;
                    return;
                }
                auto host_port
                    = context.getCnchTopologyMaster()->getTargetServer(UUIDHelpers::UUIDToString(storage->getStorageID().uuid), true);
                auto host_with_rpc = host_port.getRPCAddress();

                String source;

                if (host_port.empty())
                {
                    /// if host not found, fall back to fetch part from metastore.
                    LOG_DEBUG(log, "Fall back to get from metastore because no available server found in current topology.");
                    res = fall_back();
                    source = "KV(target server not found)";
                }
                else if (
                    context.getServerType() == ServerType::cnch_server
                    && isLocalServer(host_with_rpc, std::to_string(context.getRPCPort())))
                {
                    bool can_use_cache = true;
                    if (context.getSettingsRef().server_write_ha)
                        can_use_cache = canUseCache(storage, session_context);

                    if (!can_use_cache)
                    {
                        res = fall_back();
                        source = "KV(can not laverage cache)";
                    }
                    else
                    {
                        source = "PartCache";
                        res = context.getPartCacheManager()->getOrSetServerDataPartsInPartitions(
                            *storage,
                            partitions,
                            [&](const Strings & required_partitions, const Strings & full_partitions) {
                                source = "KV(miss cache)";
                                return getDataPartsMetaFromMetastore(storage, required_partitions, full_partitions, ts);
                            },
                            ts.toUInt64());
                    }
                }
                else
                {
                    try
                    {
                        res = context.getCnchServerClientPool().get(host_with_rpc)->fetchDataParts(host_with_rpc, storage, partitions, ts);
                        source = "TargetServer(" + host_with_rpc + ")";
                    }
                    catch (...)
                    {
                        LOG_DEBUG(log, "Fall back to get from metastore because fail to fetch part from remote server.");
                        res = fall_back();
                        source = "KV(cannot reach target server)";
                    }
                }

                if (ts)
                {
                    LOG_TRACE(
                        log,
                        "{} Start handle intermediate parts. Total number of parts is {}, timestamp: {}"
                        ,storage->getStorageID().getNameForLogs()
                        ,res.size()
                        ,ts.toString());

                    getCommittedServerDataParts(res, ts, this);

                    LOG_TRACE(
                        log,
                        "{} Finish handle intermediate parts. Total number of parts is {}, timestamp: {}"
                        ,storage->getStorageID().getNameForLogs()
                        ,res.size()
                        ,ts.toString());
                }

                LOG_DEBUG(
                    log,
                    "Elapsed {}ms to get {} parts in {} partitions for table : {} , source : {}, ts : {}"
                    ,watch.elapsedMilliseconds()
                    ,res.size()
                    ,partitions.size()
                    ,storage->getStorageID().getNameForLogs()
                    ,source
                    ,ts.toString());
                outRes = res;
            },
            ProfileEvents::GetServerDataPartsInPartitionsSuccess,
            ProfileEvents::GetServerDataPartsInPartitionsFailed);

        return outRes;
    }

    ServerDataPartsVector Catalog::getAllServerDataParts(const ConstStoragePtr & table, const TxnTimestamp & ts, const Context * session_context)
    {
        ServerDataPartsVector outRes;
        runWithMetricSupport(
            [&] {
                if (!dynamic_cast<const MergeTreeMetaBase *>(table.get()))
                {
                    outRes = {};
                    return;
                }
                auto res = getServerDataPartsInPartitions(table, getPartitionIDs(table, session_context), ts, session_context);
                outRes = res;
                return;
            },
            ProfileEvents::GetAllServerDataPartsSuccess,
            ProfileEvents::GetAllServerDataPartsFailed);
        return outRes;
    }

    DataPartsVector Catalog::getDataPartsByNames(const NameSet & names, const StoragePtr & table, const TxnTimestamp & ts)
    {
        DataPartsVector outRes;
        runWithMetricSupport(
            [&] {
                auto * storage = dynamic_cast<MergeTreeMetaBase *>(table.get());
                if (!storage)
                {
                    outRes = {};
                    return;
                }
                Strings partitions;
                std::unordered_set<String> s;
                for (auto & name : names)
                {
                    String partition = MergeTreePartInfo::fromPartName(name, storage->format_version).partition_id;
                    if (s.insert(partition).second)
                        partitions.emplace_back(std::move(partition));
                }

                // get parts in related partitions
                auto parts_from_partitions = getServerDataPartsInPartitions(table, partitions, ts, nullptr);
                DataPartsVector res;
                for (const auto & part : parts_from_partitions)
                {
                    if (names.find(part->info().getPartNameWithHintMutation()) != names.end())
                        res.push_back(part->toCNCHDataPart(*storage));
                }
                outRes = res;
            },
            ProfileEvents::GetDataPartsByNamesSuccess,
            ProfileEvents::GetDataPartsByNamesFailed);
        return outRes;
    }

    DataPartsVector
    Catalog::getStagedDataPartsByNames(const NameSet & names, const StoragePtr & table, const TxnTimestamp & ts)
    {
        DataPartsVector outRes;
        runWithMetricSupport(
            [&] {
                auto * storage = dynamic_cast<MergeTreeMetaBase *>(table.get());
                if (!storage)
                {
                    outRes = {};
                    return;
                }
                NameSet partitions;
                for (auto & name : names)
                {
                    auto info = MergeTreePartInfo::fromPartName(name, storage->format_version);
                    partitions.insert(info.partition_id);
                }

                DataPartsVector parts = getStagedParts(table, ts, &partitions);
                DataPartsVector res;
                for (auto & part : parts)
                {
                    auto name = part->info.getPartNameWithHintMutation();
                    if (names.count(name))
                        res.push_back(part);
                }
                outRes = res;
            },
            ProfileEvents::GetStagedDataPartsByNamesSuccess,
            ProfileEvents::GetStagedDataPartsByNamesFailed);
        return outRes;
    }

    DeleteBitmapMetaPtrVector Catalog::getAllDeleteBitmaps(const StoragePtr & table, const TxnTimestamp & ts)
    {
        DeleteBitmapMetaPtrVector outRes;
        runWithMetricSupport(
            [&] {
                if (!dynamic_cast<MergeTreeMetaBase *>(table.get()))
                {
                    outRes = {};
                    return;
                }
                auto res = getDeleteBitmapsInPartitions(table, getPartitionIDs(table, nullptr), ts);
                outRes = res;
            },
            ProfileEvents::GetAllDeleteBitmapsSuccess,
            ProfileEvents::GetAllDeleteBitmapsFailed);
        return outRes;
    }


    void Catalog::assertLocalServerThrowIfNot(const StoragePtr & storage) const
    {
        auto host_port = context.getCnchTopologyMaster()->getTargetServer(UUIDHelpers::UUIDToString(storage->getStorageID().uuid), false);

        if (!isLocalServer(host_port.getRPCAddress(), std::to_string(context.getRPCPort())))
            throw Exception(
                "Cannot commit parts because of choosing wrong server according to current topology, chosen server: "
                    + host_port.getTCPAddress(),
                ErrorCodes::CNCH_TOPOLOGY_NOT_MATCH_ERROR); /// Gateway client will use this target server with tcp port to do retry
    }

    UInt64 Catalog::getNonHostUpdateTimestampFromByteKV(const UUID & uuid)
    {
        return meta_proxy->getNonHostUpdateTimeStamp(name_space, UUIDHelpers::UUIDToString(uuid));
    }

    bool Catalog::isHostServer(const StoragePtr & storage) const
    {
        bool outRes;
        runWithMetricSupport(
            [&] {
                const auto host_port
                    = context.getCnchTopologyMaster()->getTargetServer(UUIDHelpers::UUIDToString(storage->getStorageID().uuid), true);
                outRes = isLocalServer(host_port.getRPCAddress(), std::to_string(context.getRPCPort()));
            },
            ProfileEvents::IsHostServerSuccess,
            ProfileEvents::IsHostServerFailed);
        return outRes;
    }

    void Catalog::finishCommit(
        const StoragePtr & storage,
        const TxnTimestamp & txnID,
        [[maybe_unused]] const TxnTimestamp & commit_ts,
        const DataPartsVector & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const bool is_merged_parts,
        const bool preallocate_mode)
    {
        runWithMetricSupport(
            [&] {
                // In some cases the parts and delete_bitmap are empty, eg: truncate an empty table. We just directly return from here without throwing any exception.
                if (parts.empty() && delete_bitmaps.empty())
                {
                    return;
                }
                // If target table is a bucket table, ensure that the source part is not a bucket part or if the source part is a bucket part
                // ensure that the table_definition_hash is the same before committing
                // TODO: Implement rollback for this to work properly
                if (storage->isBucketTable())
                {
                    for (auto & part : parts)
                    {
                        if (!part->deleted
                            && (part->bucket_number < 0 || storage->getTableHashForClusterBy() != part->table_definition_hash))
                        {
                            LOG_DEBUG(
                                log,
                                "Part's table_definition_hash {} is different from target table's table_definition_hash {}"
                                ,part->table_definition_hash
                                ,storage->getTableHashForClusterBy());

                            throw Exception(
                                "Source table is not a bucket table or has a different CLUSTER BY definition from the target table. ",
                                ErrorCodes::BUCKET_TABLE_ENGINE_MISMATCH);
                        }
                    }
                }

                if (!context.getSettingsRef().server_write_ha)
                    assertLocalServerThrowIfNot(storage);

                /// add data parts
                Protos::DataModelPartVector commit_parts;
                fillPartsModel(*storage, parts, *commit_parts.mutable_parts());

                finishCommitInternal(
                    storage,
                    commit_parts.parts(),
                    delete_bitmaps,
                    /*staged_parts*/ {},
                    txnID.toUInt64(),
                    preallocate_mode,
                    std::vector<String>(commit_parts.parts().size()),
                    std::vector<String>(delete_bitmaps.size()),
                    /*expected_staged_parts*/ {});

                /// insert new added parts into cache manager
                if (context.getPartCacheManager())
                    context.getPartCacheManager()->insertDataPartsIntoCache(*storage, commit_parts.parts(), is_merged_parts, true);
            },
            ProfileEvents::FinishCommitSuccess,
            ProfileEvents::FinishCommitFailed);
    }

    void Catalog::finishCommitInBatch(
        const StoragePtr & storage,
        const TxnTimestamp & txnID,
        const Protos::DataModelPartVector & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const Protos::DataModelPartVector & staged_parts,
        const bool preallocate_mode,
        const std::vector<String> & expected_parts,
        const std::vector<String> & expected_bitmaps,
        const std::vector<String> & expected_staged_parts)
    {
        size_t total_parts_number = parts.parts().size();
        size_t total_deleted_bitmaps_number = delete_bitmaps.size();
        size_t total_staged_parts_num = staged_parts.parts().size();
        size_t total_expected_parts_num = expected_parts.size();
        size_t total_expected_bitmaps_num = expected_bitmaps.size();
        size_t total_expected_staged_parts_num = expected_staged_parts.size();

        if (total_expected_parts_num != total_parts_number || total_expected_bitmaps_num != total_deleted_bitmaps_number
            || total_staged_parts_num != total_expected_staged_parts_num)
            throw Exception("Expected parts or bitmaps number does not match the actual number", ErrorCodes::LOGICAL_ERROR);

        auto commit_in_batch = [&](BatchedCommitIndex commit_index) {
            // reuse finishCommitInternal for parts write.
            finishCommitInternal(
                storage,
                google::protobuf::RepeatedPtrField<Protos::DataModelPart>{
                    parts.parts().begin() + commit_index.parts_begin, parts.parts().begin() + commit_index.parts_end},
                DeleteBitmapMetaPtrVector{
                    delete_bitmaps.begin() + commit_index.bitmap_begin, delete_bitmaps.begin() + commit_index.bitmap_end},
                google::protobuf::RepeatedPtrField<Protos::DataModelPart>{
                    staged_parts.parts().begin() + commit_index.staged_begin, staged_parts.parts().begin() + commit_index.staged_end},
                txnID.toUInt64(),
                preallocate_mode,
                std::vector<String>{
                    expected_parts.begin() + commit_index.expected_parts_begin, expected_parts.begin() + commit_index.expected_parts_end},
                std::vector<String>{
                    expected_bitmaps.begin() + commit_index.expected_bitmap_begin,
                    expected_bitmaps.begin() + commit_index.expected_bitmap_end},
                std::vector<String>{
                    expected_staged_parts.begin() + commit_index.expected_staged_begin,
                    expected_staged_parts.begin() + commit_index.expected_staged_end});
        };

        if (!context.getSettingsRef().server_write_ha)
            assertLocalServerThrowIfNot(storage);

        // commit parts and delete bitmaps in one batch if the size is small.
        if (total_parts_number + delete_bitmaps.size() + total_staged_parts_num < max_commit_size_one_batch)
        {
            commit_in_batch(
                {0,
                 total_parts_number,
                 0,
                 total_deleted_bitmaps_number,
                 0,
                 total_staged_parts_num,
                 0,
                 total_parts_number,
                 0,
                 total_deleted_bitmaps_number,
                 0,
                 total_expected_staged_parts_num});
        }
        else
        {
            // commit data parts first
            size_t batch_count{0};
            while (batch_count + max_commit_size_one_batch < total_parts_number)
            {
                commit_in_batch(
                    {batch_count,
                     batch_count + max_commit_size_one_batch,
                     0,
                     0,
                     0,
                     0,
                     batch_count,
                     batch_count + max_commit_size_one_batch,
                     0,
                     0,
                     0,
                     0});
                batch_count += max_commit_size_one_batch;
            }
            commit_in_batch({batch_count, total_parts_number, 0, 0, 0, 0, batch_count, total_parts_number, 0, 0, 0, 0});

            // then commit delete bitmap
            batch_count = 0;
            while (batch_count + max_commit_size_one_batch < delete_bitmaps.size())
            {
                commit_in_batch(
                    {0,
                     0,
                     batch_count,
                     batch_count + max_commit_size_one_batch,
                     0,
                     0,
                     0,
                     0,
                     batch_count,
                     batch_count + max_commit_size_one_batch,
                     0,
                     0});
                batch_count += max_commit_size_one_batch;
            }
            commit_in_batch({0, 0, batch_count, total_deleted_bitmaps_number, 0, 0, 0, 0, batch_count, total_deleted_bitmaps_number, 0, 0});

            // then commit staged parts
            batch_count = 0;
            while (batch_count + max_commit_size_one_batch < total_staged_parts_num)
            {
                commit_in_batch(
                    {0,
                     0,
                     0,
                     0,
                     batch_count,
                     batch_count + max_commit_size_one_batch,
                     0,
                     0,
                     0,
                     0,
                     batch_count,
                     batch_count + max_commit_size_one_batch});
                batch_count += max_commit_size_one_batch;
            }
            commit_in_batch({0, 0, 0, 0, batch_count, total_staged_parts_num, 0, 0, 0, 0, batch_count, total_staged_parts_num});
        }
    }

    void Catalog::mayUpdateUHUT(const StoragePtr & storage)
    {
        UInt64 current_pts = context.getTimestamp() >> 18;
        auto cache_manager = context.getPartCacheManager();

        if (cache_manager && cache_manager->trySetCachedNHUTForUpdate(storage->getStorageID().uuid, current_pts))
            meta_proxy->setNonHostUpdateTimeStamp(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid), current_pts);
    }

    bool Catalog::canUseCache(const ConstStoragePtr & storage, const Context * session_context)
    {
        UInt64 latest_nhut;
        if (!context.getPartCacheManager())
            return false;
        if (session_context)
            latest_nhut = const_cast<Context *>(session_context )->getNonHostUpdateTime(storage->getStorageID().uuid);
        else
            latest_nhut = getNonHostUpdateTimestampFromByteKV(storage->getStorageID().uuid);
        return context.getPartCacheManager()->checkIfCacheValidWithNHUT(storage->getStorageID().uuid, latest_nhut);
    }

    void Catalog::finishCommitInternal(
        const StoragePtr & storage,
        const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & staged_parts,
        const UInt64 & txnid,
        const bool preallocate_mode,
        const std::vector<String> & expected_parts,
        const std::vector<String> & expected_bitmaps,
        const std::vector<String> & expected_staged_parts)
    {
        if (!isHostServer(storage))
        {
            mayUpdateUHUT(storage);
        }

        BatchCommitRequest batch_writes(false);
        batch_writes.SetTimeout(3000);

        if (!parts.empty())
        {
            Strings current_partitions = getPartitionIDs(storage, nullptr);
            meta_proxy->prepareAddDataParts(
                name_space,
                UUIDHelpers::UUIDToString(storage->getStorageID().uuid),
                current_partitions,
                parts,
                batch_writes,
                expected_parts,
                preallocate_mode);
        }
        meta_proxy->prepareAddDeleteBitmaps(
            name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid), delete_bitmaps, batch_writes, expected_bitmaps);
        meta_proxy->prepareAddStagedParts(
            name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid), staged_parts, batch_writes, expected_staged_parts);

        if (batch_writes.isEmpty())
        {
            LOG_DEBUG(
                log,
                "Nothing to do while committing: {} with {} parts, {} delete bitmaps, {} staged parts."
                ,txnid
                ,parts.size()
                ,delete_bitmaps.size()
                ,staged_parts.size());
            return;
        }

        BatchCommitResponse resp;
        meta_proxy->batchWrite(batch_writes, resp);
        if (resp.puts.size())
        {
            throw Exception("Commit parts fail with conflicts. First conflict key is : " + batch_writes.puts[resp.puts.begin()->first].key +
                ", total: " + toString(resp.puts.size()), ErrorCodes::CATALOG_COMMIT_PART_ERROR);
        }
    }


    void Catalog::dropAllPart(const StoragePtr & storage, const TxnTimestamp & txnID, const TxnTimestamp & ts)
    {
        runWithMetricSupport(
            [&] {
                String table_uuid = UUIDHelpers::UUIDToString(storage->getStorageID().uuid);
                BatchCommitRequest batch_writes;

                std::unordered_set<String> parts_set;
                String part_prefix = MetastoreProxy::dataPartPrefix(name_space, table_uuid);
                IMetaStore::IteratorPtr it = meta_proxy->getPartsInRange(name_space, table_uuid, "");

                DB::Protos::DataModelPart part_data;
                while (it->next())
                {
                    part_data.ParseFromString(it->value());
                    auto part_info = part_data.mutable_part_info();

                    String part_name = createPartInfoFromModel(*part_info)->getPartName();
                    auto res_p = parts_set.insert(part_name);
                    if (res_p.second)
                    {
                        /// TODO: replace commit time to txnid for mutation?
                        part_info->set_hint_mutation(part_info->mutation());
                        part_info->set_mutation(ts.toUInt64());
                        part_data.set_deleted(true);
                        part_data.set_txnid(txnID.toUInt64());
                        batch_writes.AddPut(SinglePutRequest(part_prefix + part_name, part_data.SerializeAsString()));
                    }
                }
                BatchCommitResponse resp;
                meta_proxy->batchWrite(batch_writes, resp);
            },
            ProfileEvents::DropAllPartSuccess,
            ProfileEvents::DropAllPartFailed);
    }

    std::vector<std::shared_ptr<MergeTreePartition>> Catalog::getPartitionList(const ConstStoragePtr & table, const Context * session_context)
    {
        std::vector<std::shared_ptr<MergeTreePartition>> partition_list;
        runWithMetricSupport(
            [&] {
                PartitionMap partitions;
                if (auto * cnch_table = dynamic_cast<const MergeTreeMetaBase *>(table.get()))
                {
                    bool can_use_cache = true;
                    if (context.getSettingsRef().server_write_ha)
                        can_use_cache = canUseCache(table, session_context);

                    auto cache_manager = context.getPartCacheManager();

                    if (!cache_manager || !can_use_cache || !cache_manager->getTablePartitions(*cnch_table, partitions))
                        getPartitionsFromMetastore(*cnch_table, partitions);
                }

                for (auto it = partitions.begin(); it != partitions.end(); it++)
                    partition_list.push_back(it->second->partition_ptr);
            },
            ProfileEvents::GetPartitionListSuccess,
            ProfileEvents::GetPartitionListFailed);
        return partition_list;
    }

    Strings Catalog::getPartitionIDs(const ConstStoragePtr & storage, const Context * session_context)
    {
        Strings partition_ids;
        runWithMetricSupport(
            [&] {
                PartitionMap partitions;

                bool can_use_cache = true;
                if (context.getSettingsRef().server_write_ha)
                    can_use_cache = canUseCache(storage, session_context);

                auto cache_manager = context.getPartCacheManager();
                if (cache_manager && can_use_cache && cache_manager->getTablePartitions(*storage, partitions))
                {
                    for (auto it = partitions.begin(); it != partitions.end(); it++)
                        partition_ids.push_back(it->first);
                }
                else
                    partition_ids = getPartitionIDsFromMetastore(storage);
            },
            ProfileEvents::GetPartitionIDsSuccess,
            ProfileEvents::GetPartitionIDsFailed);
        return partition_ids;
    }

    void Catalog::getPartitionsFromMetastore(const MergeTreeMetaBase & table, PartitionMap & partition_list)
    {
        runWithMetricSupport(
            [&] {
                partition_list.clear();

                IMetaStore::IteratorPtr it = meta_proxy->getPartitionList(name_space, UUIDHelpers::UUIDToString(table.getStorageUUID()));
                while (it->next())
                {
                    Protos::PartitionMeta partition_meta;
                    partition_meta.ParseFromString(it->value());
                    auto partition_ptr = createPartitionFromMetaModel(table, partition_meta);
                    partition_list.emplace(partition_meta.id(), std::make_shared<CnchPartitionInfo>(partition_ptr));
                }
            },
            ProfileEvents::GetPartitionsFromMetastoreSuccess,
            ProfileEvents::GetPartitionsFromMetastoreFailed);
    }

    Strings Catalog::getPartitionIDsFromMetastore(const ConstStoragePtr & storage)
    {
        Strings partitions_id;
        IMetaStore::IteratorPtr it = meta_proxy->getPartitionList(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid));
        while (it->next())
        {
            Protos::PartitionMeta partition_meta;
            partition_meta.ParseFromString(it->value());
            partitions_id.emplace_back(partition_meta.id());
        }
        return partitions_id;
    }

    void Catalog::createDictionary(const StorageID & storage_id, const String & create_query)
    {
        runWithMetricSupport(
            [&] {
                Protos::DataModelDictionary dic_model;

                dic_model.set_database(storage_id.getDatabaseName());
                dic_model.set_name(storage_id.getTableName());

                RPCHelpers::fillUUID(storage_id.uuid, *(dic_model.mutable_uuid()));
                dic_model.set_definition(create_query);
                dic_model.set_last_modification_time(Poco::Timestamp().raw());

                meta_proxy->createDictionary(name_space, storage_id.getDatabaseName(), storage_id.getTableName(), dic_model.SerializeAsString());
            },
            ProfileEvents::CreateDictionarySuccess,
            ProfileEvents::CreateDictionaryFailed);
    }

    ASTPtr Catalog::getCreateDictionary(const String & database, const String & name)
    {
        ASTPtr outRes;
        runWithMetricSupport(
            [&] {
                String dic_meta;
                meta_proxy->getDictionary(name_space, database, name, dic_meta);

                if (dic_meta.empty())
                    throw Exception("Dictionary " + database + "." + name + " doesn't exists.", ErrorCodes::DICTIONARY_NOT_EXIST);

                Protos::DataModelDictionary dic_model;
                dic_model.ParseFromString(dic_meta);
                auto res = CatalogFactory::getCreateDictionaryByDataModel(dic_model);
                outRes = res;
            },
            ProfileEvents::GetCreateDictionarySuccess,
            ProfileEvents::GetCreateDictionaryFailed);

        return outRes;
    }

    void Catalog::dropDictionary(const String & database, const String & name)
    {
        runWithMetricSupport(
            [&] {
                String dic_meta;
                meta_proxy->getDictionary(name_space, database, name, dic_meta);
                if (dic_meta.empty())
                    throw Exception("Dictionary " + database + "." + name + " doesn't  exists.", ErrorCodes::DICTIONARY_NOT_EXIST);

                meta_proxy->dropDictionary(name_space, database, name);
            },
            ProfileEvents::DropDictionarySuccess,
            ProfileEvents::DropDictionaryFailed);
    }

    void Catalog::attachDictionary(const String & database, const String & name)
    {
        runWithMetricSupport(
            [&] { detachOrAttachDictionary(database, name, false); },
            ProfileEvents::AttachDictionarySuccess,
            ProfileEvents::AttachDictionaryFailed);
    }

    void Catalog::detachDictionary(const String & database, const String & name)
    {
        runWithMetricSupport(
            [&] { detachOrAttachDictionary(database, name, true); },
            ProfileEvents::DetachDictionarySuccess,
            ProfileEvents::DetachDictionaryFailed);
    }

    void Catalog::fixDictionary(const String & database, const String & name)
    {
        String dic_meta;
        meta_proxy->getDictionary(name_space, database, name, dic_meta);
        DB::Protos::DataModelDictionary dic_model;
        dic_model.ParseFromString(dic_meta);
        fillUUIDForDictionary(dic_model);
        meta_proxy->createDictionary(name_space, database, name, dic_model.SerializeAsString());
    }

    Strings Catalog::getDictionariesInDB(const String & database)
    {
        Strings res;
        runWithMetricSupport(
            [&] {
                auto dic_ptrs = meta_proxy->getDictionariesInDB(name_space, database);
                res.reserve(dic_ptrs.size());
                for (auto & dic_ptr : dic_ptrs)
                    res.push_back(dic_ptr->name());
            },
            ProfileEvents::GetDictionariesInDBSuccess,
            ProfileEvents::GetDictionariesInDBFailed);
        return res;
    }

    Protos::DataModelDictionary Catalog::getDictionary(const String & database, const String & name)
    {
        Protos::DataModelDictionary res;
        runWithMetricSupport(
            [&] {
                String dic_meta;
                meta_proxy->getDictionary(name_space, database, name, dic_meta);

                DB::Protos::DataModelDictionary dict_data;
                dict_data.ParseFromString(dic_meta);
                res = dict_data;
            },
            ProfileEvents::GetDictionarySuccess,
            ProfileEvents::GetDictionaryFailed);
        return res;
    }

    StoragePtr Catalog::tryGetDictionary(const String & database, const String & name, ContextPtr local_context)
    {
        StoragePtr res;
        runWithMetricSupport(
            [&] {
                String dic_meta;
                meta_proxy->getDictionary(name_space, database, name, dic_meta);
                if (dic_meta.empty())
                    return;
                DB::Protos::DataModelDictionary dict_data;
                dict_data.ParseFromString(dic_meta);

                const UInt64 & status = dict_data.status();
                if (Status::isDeleted(status) || Status::isDetached(status))
                    return;
                ASTPtr ast = CatalogFactory::getCreateDictionaryByDataModel(dict_data);
                const ASTCreateQuery & create_query = ast->as<ASTCreateQuery &>();
                DictionaryConfigurationPtr abstract_dictionary_configuration =
                    getDictionaryConfigurationFromAST(create_query, local_context, dict_data.database());
                abstract_dictionary_configuration->setBool("is_cnch_dictionary", true);
                StorageID storage_id{database, name, RPCHelpers::createUUID(dict_data.uuid())};
                res = StorageDictionary::create(
                    storage_id,
                    std::move(abstract_dictionary_configuration),
                    local_context,
                    true);
            },
            ProfileEvents::GetDictionarySuccess,
            ProfileEvents::GetDictionaryFailed);
        return res;
    }

    bool Catalog::isDictionaryExists(const String & database, const String & name)
    {
        bool res = false;
        runWithMetricSupport(
            [&] {
                String dic_meta;
                meta_proxy->getDictionary(name_space, database, name, dic_meta);

                if (dic_meta.empty())
                {
                    res = false;
                    return;
                }

                DB::Protos::DataModelDictionary dict_data;
                dict_data.ParseFromString(dic_meta);
                const UInt64 & status = dict_data.status();
                if (Status::isDeleted(status) || Status::isDetached(status))
                    res = false;
                else
                    res = true;
            },
            ProfileEvents::IsDictionaryExistsSuccess,
            ProfileEvents::IsDictionaryExistsFailed);
        return res;
    }

    void Catalog::createTransactionRecord(const TransactionRecord & record)
    {
        runWithMetricSupport(
            [&] {
                meta_proxy->createTransactionRecord(name_space, record.txnID().toUInt64(), record.serialize());
                ProfileEvents::increment(ProfileEvents::CnchTxnAllTransactionRecord);
            },
            ProfileEvents::CreateTransactionRecordSuccess,
            ProfileEvents::CreateTransactionRecordFailed);
    }

    void Catalog::removeTransactionRecord(const TransactionRecord & record)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeTransactionRecord(name_space, record.txnID()); },
            ProfileEvents::RemoveTransactionRecordSuccess,
            ProfileEvents::RemoveTransactionRecordFailed);
    }

    void Catalog::removeTransactionRecords(const std::vector<TxnTimestamp> & txn_ids)
    {
        runWithMetricSupport(
            [&] {
                if (txn_ids.empty())
                {
                    return;
                }
                meta_proxy->removeTransactionRecords(name_space, txn_ids);
            },
            ProfileEvents::RemoveTransactionRecordsSuccess,
            ProfileEvents::RemoveTransactionRecordsFailed);
    }

    TransactionRecord Catalog::getTransactionRecord(const TxnTimestamp & txnID)
    {
        TransactionRecord outRes;
        runWithMetricSupport(
            [&] {
                String txn_data = meta_proxy->getTransactionRecord(name_space, txnID);
                if (txn_data.empty())
                    throw Exception(
                        "No transaction record found with txn_id : " + std::to_string(txnID),
                        ErrorCodes::CATALOG_TRANSACTION_RECORD_NOT_FOUND);
                auto res = TransactionRecord::deserialize(txn_data);
                outRes = res;
            },
            ProfileEvents::GetTransactionRecordSuccess,
            ProfileEvents::GetTransactionRecordFailed);
        return outRes;
    }

    std::optional<TransactionRecord> Catalog::tryGetTransactionRecord(const TxnTimestamp & txnID)
    {
        std::optional<TransactionRecord> outRes;
        runWithMetricSupport(
            [&] {
                String txn_data = meta_proxy->getTransactionRecord(name_space, txnID);
                if (txn_data.empty())
                {
                    outRes = {};
                    return;
                }
                auto res = TransactionRecord::deserialize(txn_data);
                outRes = res;
            },
            ProfileEvents::TryGetTransactionRecordSuccess,
            ProfileEvents::TryGetTransactionRecordFailed);
        return outRes;
    }

    bool Catalog::setTransactionRecordStatusWithOffsets(
        const TransactionRecord & expected_record,
        TransactionRecord & target_record,
        const String & consumer_group,
        const cppkafka::TopicPartitionList & tpl)
    {
        bool outRes;
        runWithMetricSupport(
            [&] {
                auto res = meta_proxy->updateTransactionRecordWithOffsets(
                    name_space,
                    expected_record.txnID().toUInt64(),
                    expected_record.serialize(),
                    target_record.serialize(),
                    consumer_group,
                    tpl);
                outRes = res;
            },
            ProfileEvents::SetTransactionRecordStatusWithOffsetsSuccess,
            ProfileEvents::SetTransactionRecordStatusWithOffsetsFailed);
        return outRes;
    }

    /// commit and abort can reuse this API. set record status to targetStatus if current record.status is Running
    bool Catalog::setTransactionRecord(const TransactionRecord & expected_record, TransactionRecord & target_record)
    {
        bool res;
        runWithMetricSupport(
            [&] {
                // TODO: topology check and solving the gap between checking and write
                auto [success, txn_data] = meta_proxy->updateTransactionRecord(
                    name_space, expected_record.txnID().toUInt64(), expected_record.serialize(), target_record.serialize());
                if (success)
                {
                    res = true;
                }
                else
                {
                    if (txn_data.empty())
                    {
                        LOG_DEBUG(log, "UpdateTransactionRecord fails. Expected record {} not exist.", expected_record.toString());
                        target_record = expected_record;
                        target_record.setStatus(CnchTransactionStatus::Unknown);
                    }
                    else
                    {
                        target_record = TransactionRecord::deserialize(txn_data);
                    }
                    res = false;
                }
            },
            ProfileEvents::SetTransactionRecordSuccess,
            ProfileEvents::SetTransactionRecordFailed);
        return res;
    }

    bool Catalog::setTransactionRecordWithRequests(
        const TransactionRecord & expected_record, TransactionRecord & target_record, BatchCommitRequest & additional_requests, BatchCommitResponse & response)
    {
        bool res;
        runWithMetricSupport(
            [&] {
                SinglePutRequest txn_request(MetastoreProxy::transactionRecordKey(name_space, expected_record.txnID().toUInt64()), target_record.serialize(), expected_record.serialize());

                auto [success, txn_data] = meta_proxy->updateTransactionRecordWithRequests(txn_request, additional_requests, response);
                if (success)
                {
                    res = true;
                    return;
                }
                
                if (txn_data.empty())
                {
                    LOG_DEBUG(log, "UpdateTransactionRecord fails. Expected record {} not exist.", expected_record.toString());
                    target_record = expected_record;
                    target_record.setStatus(CnchTransactionStatus::Unknown);
                }
                else
                {
                    for (auto it=response.puts.begin(); it!=response.puts.end(); it++)
                    {
                        if (additional_requests.puts[it->first].callback)
                            additional_requests.puts[it->first].callback(CAS_FAILED, "");
                    }
                    target_record = TransactionRecord::deserialize(txn_data);
                }
                res = false;
            },
            ProfileEvents::SetTransactionRecordWithRequestsSuccess,
            ProfileEvents::SetTransactionRecordWithRequestsFailed);
        return res;
    }

    void Catalog::setTransactionRecordCleanTime(TransactionRecord record, const TxnTimestamp & clean_ts, UInt64 ttl)
    {
        runWithMetricSupport(
            [&] {
                if (record.status() != CnchTransactionStatus::Finished)
                    throw Exception("Unable to set clean time for uncommitted transaction", ErrorCodes::LOGICAL_ERROR);

                record.setCleanTs(clean_ts);
                meta_proxy->setTransactionRecord(name_space, record.txnID().toUInt64(), record.serialize(), ttl);
            },
            ProfileEvents::SetTransactionRecordCleanTimeSuccess,
            ProfileEvents::SetTransactionRecordCleanTimeFailed);
    }

    void Catalog::rollbackTransaction(TransactionRecord record)
    {
        runWithMetricSupport(
            [&] {
                record.setStatus(CnchTransactionStatus::Aborted);
                meta_proxy->setTransactionRecord(name_space, record.txnID().toUInt64(), record.serialize());
            },
            ProfileEvents::RollbackTransactionSuccess,
            ProfileEvents::RollbackTransactionFailed);
    }

    /// add lock implementation for Pessimistic mode.
    /// replace previous tryLockPartInKV implementation.
    /// key is WriteIntent.intent, value is WriteIntent.txn_id and location.
    /// The key format is use `intent` as prefix like intent_partname.
    /// ConflictIntents map: key is <txnid, location> pair, value is intent names.
    /// (*CAS* operation) put if not exist .
    bool Catalog::writeIntents(
        const String & intent_prefix,
        const std::vector<WriteIntent> & intents,
        std::map<std::pair<TxnTimestamp, String>, std::vector<String>> & conflictIntents)
    {
        bool outRes;
        runWithMetricSupport(
            [&] {
                std::vector<String> cas_failed_list;

                bool res = meta_proxy->writeIntent(name_space, intent_prefix, intents, cas_failed_list);

                if (!res)
                {
                    for (auto & intent_data : cas_failed_list)
                    {
                        WriteIntent write_intent = WriteIntent::deserialize(intent_data);
                        std::pair<TxnTimestamp, String> key{write_intent.txnId(), write_intent.location()};
                        auto it = conflictIntents.find(key);
                        if (it == conflictIntents.end())
                        {
                            std::vector<String> intent_names{write_intent.intent()};
                            conflictIntents.emplace(key, intent_names);
                        }
                        else
                        {
                            it->second.push_back(write_intent.intent());
                        }
                    }
                }
                outRes = res;
            },
            ProfileEvents::WriteIntentsSuccess,
            ProfileEvents::WriteIntentsFailed);
        return outRes;
    }

    /// used in the following 2 cases.
    /// 1. when meeting with some intents belongs to some zombie transaction.
    /// 2. try to preempt some intents belongs to low-priority running transaction.
    // replace previous tryResetAndLockConflictPartsInKV implementation.
    // intentsToReset map: key is <txnID, location> pair, value is the intent names.
    // (*CAS* operation): set to <newTxnID, newLocation> if value equals <txnID, location>.
    bool Catalog::tryResetIntents(
        const String & intent_prefix,
        const std::map<std::pair<TxnTimestamp, String>, std::vector<String>> & intentsToReset,
        const TxnTimestamp & newTxnID,
        const String & newLocation)
    {
        bool outRes;
        runWithMetricSupport(
            [&] {
                std::vector<WriteIntent> intent_vector;
                for (auto it = intentsToReset.begin(); it != intentsToReset.end(); it++)
                {
                    const TxnTimestamp & txn_id = it->first.first;
                    const String & location = it->first.second;
                    for (auto & intent_name : it->second)
                        intent_vector.emplace_back(txn_id, location, intent_name);
                }

                bool res = meta_proxy->resetIntent(name_space, intent_prefix, intent_vector, newTxnID.toUInt64(), newLocation);
                outRes = res;
            },
            ProfileEvents::TryResetIntentsIntentsToResetSuccess,
            ProfileEvents::TryResetIntentsIntentsToResetFailed);
        return outRes;
    }

    bool Catalog::tryResetIntents(
        const String & intent_prefix,
        const std::vector<WriteIntent> & oldIntents,
        const TxnTimestamp & newTxnID,
        const String & newLocation)
    {
        bool outRes;
        runWithMetricSupport(
            [&] {
                auto res = meta_proxy->resetIntent(name_space, intent_prefix, oldIntents, newTxnID.toUInt64(), newLocation);
                outRes = res;
            },
            ProfileEvents::TryResetIntentsOldIntentsSuccess,
            ProfileEvents::TryResetIntentsOldIntentsFailed);
        return outRes;
    }

    /// used when current transaction is committed or aborted,
    /// to clear intents in batch.
    // replace previous unLockPartInKV implementation.
    // after txn record is committed, clear the intent asynchronously.
    // (*CAS* operation): delete intents by keys if value equals <txnID, location>
    // clear may fail due to preemption by others. so we need another clearIntent part to clear part by part.
    void Catalog::clearIntents(const String & intent_prefix, const std::vector<WriteIntent> & intents)
    {
        runWithMetricSupport(
            [&] { meta_proxy->clearIntents(name_space, intent_prefix, intents); },
            ProfileEvents::ClearIntentsSuccess,
            ProfileEvents::ClearIntentsFailed);
    }

    /// write part into bytekv, replace commitParts
    void Catalog::writeParts(
        const StoragePtr & table,
        const TxnTimestamp & txnID,
        const CommitItems & commit_data,
        const bool is_merged_parts,
        const bool preallocate_mode)
    {
        runWithMetricSupport(
            [&] {
                Stopwatch watch;

                if (commit_data.empty())
                {
                    return;
                }
                LOG_DEBUG(
                    log,
                    "Start write {} parts and {} delete_bitmaps and {} staged parts to bytekv for txn {}"
                    ,commit_data.data_parts.size()
                    ,commit_data.delete_bitmaps.size()
                    ,commit_data.staged_parts.size()
                    ,txnID);

                const auto host_port
                    = context.getCnchTopologyMaster()->getTargetServer(UUIDHelpers::UUIDToString(table->getStorageUUID()), true);

                if (!host_port.empty() && !isLocalServer(host_port.getRPCAddress(), std::to_string(context.getRPCPort()))
                    && (context.getSettingsRef().enable_write_non_host_server || context.getSettingsRef().server_write_ha))
                {
                    try
                    {
                        LOG_DEBUG(
                            log,
                            "Redirect writeParts request to remote host : {} for table {} , txn id : {}"
                            ,host_port.toDebugString()
                            ,table->getStorageID().getNameForLogs()
                            ,txnID);

                        context.getCnchServerClientPool().get(host_port)->redirectCommitParts(
                            table, commit_data, txnID, is_merged_parts, preallocate_mode);
                        return;
                    }
                    catch (Exception & e)
                    {
                        /// if remote quest got exception and cannot fallback to commit to current node, throw exception directly
                        if (!context.getSettingsRef().server_write_ha)
                            throw Exception(
                                "Fail to redirect writeParts request to remote host : " + host_port.toDebugString()
                                    + ". Error message : " + e.what(),
                                ErrorCodes::CATALOG_COMMIT_PART_ERROR);
                    }
                }

                // If target table is a bucket table, ensure that the source part is not a bucket part or if the source part is a bucket part
                // ensure that the table_definition_hash is the same before committing
                // TODO: Implement rollback for this to work properly
                if (table->isBucketTable())
                {
                    for (auto & part : commit_data.data_parts)
                    {
                        if (!part->deleted && (part->bucket_number < 0 || table->getTableHashForClusterBy() != part->table_definition_hash))
                            throw Exception(
                                "Part's table_definition_hash [" + std::to_string(part->table_definition_hash)
                                    + "] is different from target table's table_definition_hash  ["
                                    + std::to_string(table->getTableHashForClusterBy()) + "]",
                                ErrorCodes::BUCKET_TABLE_ENGINE_MISMATCH);
                    }
                }

                Protos::DataModelPartVector part_models;
                fillPartsModel(*table, commit_data.data_parts, *part_models.mutable_parts());

                Protos::DataModelPartVector staged_part_models;
                fillPartsModel(*table, commit_data.staged_parts, *staged_part_models.mutable_parts());

                finishCommitInBatch(
                    table,
                    txnID,
                    part_models,
                    commit_data.delete_bitmaps,
                    staged_part_models,
                    preallocate_mode,
                    std::vector<String>(part_models.parts().size()),
                    std::vector<String>(commit_data.delete_bitmaps.size()),
                    std::vector<String>(staged_part_models.parts().size()));
                LOG_DEBUG(
                    log,
                    "Finish write parts to bytekv for txn {}, elapsed {}ms, start write part cache."
                    ,txnID
                    ,watch.elapsedMilliseconds());

                /// insert new added parts into cache manager
                if (context.getPartCacheManager() && !part_models.parts().empty())
                    context.getPartCacheManager()->insertDataPartsIntoCache(*table, part_models.parts(), is_merged_parts, true);

                LOG_DEBUG(log, "Finish write part for txn {}, elapsed {} ms.", txnID, watch.elapsedMilliseconds());
            },
            ProfileEvents::WritePartsSuccess,
            ProfileEvents::WritePartsFailed);
    }

    /// set commit time for parts
    void Catalog::setCommitTime(const StoragePtr & table, const CommitItems & commit_data, const TxnTimestamp & ts, const UInt64 txn_id)
    {
        runWithMetricSupport(
            [&] {
                Stopwatch watch;

                if (commit_data.empty())
                {
                    return;
                }
                LOG_DEBUG(
                    log,
                    "Start set commit time of {} parts and {} delete_bitmaps and {} staged parts for txn {}."
                    ,commit_data.data_parts.size()
                    ,commit_data.delete_bitmaps.size()
                    ,commit_data.staged_parts.size()
                    ,txn_id);

                const auto host_port
                    = context.getCnchTopologyMaster()->getTargetServer(UUIDHelpers::UUIDToString(table->getStorageUUID()), true);

                if (!host_port.empty() && !isLocalServer(host_port.getRPCAddress(), std::to_string(context.getRPCPort()))
                    && (context.getSettingsRef().enable_write_non_host_server || context.getSettingsRef().server_write_ha))
                {
                    try
                    {
                        LOG_DEBUG(
                            log,
                            "Redirect setCommitTime request to remote host : {} for table {}, txn id : {}"
                            ,host_port.toDebugString()
                            ,table->getStorageID().getNameForLogs()
                            ,txn_id);

                        context.getCnchServerClientPool().get(host_port)->redirectSetCommitTime(table, commit_data, ts, txn_id);
                        return;
                    }
                    catch (Exception & e)
                    {
                        /// if remote quest got exception and cannot fallback to commit to current node, throw exception directly
                        if (!context.getSettingsRef().server_write_ha)
                            throw Exception(
                                "Fail to redirect setCommitTime request to remote host : " + host_port.toDebugString()
                                    + ". Error message : " + e.what(),
                                ErrorCodes::CATALOG_COMMIT_PART_ERROR);
                    }
                }

                // set part commit time
                for (auto & part : commit_data.data_parts)
                    part->commit_time = ts;

                for (auto & bitmap : commit_data.delete_bitmaps)
                    bitmap->updateCommitTime(ts);

                for (auto & part : commit_data.staged_parts)
                    part->commit_time = ts;

                // the same logic as write parts, just re-write data parts and update part cache.
                Protos::DataModelPartVector part_models;
                Protos::DataModelPartVector staged_part_models;

                /// check parts status
                std::vector<size_t> parts_to_remove;
                std::vector<String> expected_parts;
                checkItemsStatus<DataPartPtr>(
                    commit_data.data_parts,
                    [&](const DataPartPtr part) -> String {
                        return MetastoreProxy::dataPartKey(
                            name_space, UUIDHelpers::UUIDToString(table->getStorageUUID()), part->info.getPartName());
                    },
                    parts_to_remove,
                    expected_parts);

                /// check delete bitmaps status
                std::vector<size_t> bitmaps_to_remove;
                std::vector<String> expected_bitmap;
                checkItemsStatus<DeleteBitmapMetaPtr>(
                    commit_data.delete_bitmaps,
                    [&](const DeleteBitmapMetaPtr delete_bitmap) -> String {
                        return MetastoreProxy::deleteBitmapKey(
                            name_space, UUIDHelpers::UUIDToString(table->getStorageUUID()), *(delete_bitmap->getModel()));
                    },
                    bitmaps_to_remove,
                    expected_bitmap);

                /// check staged parts status
                std::vector<size_t> staged_parts_to_remove;
                std::vector<String> expected_staged_parts;
                checkItemsStatus<DataPartPtr>(
                    commit_data.staged_parts,
                    [&](const DataPartPtr part) -> String {
                        return MetastoreProxy::stagedDataPartKey(
                            name_space, UUIDHelpers::UUIDToString(table->getStorageUUID()), part->info.getPartName());
                    },
                    staged_parts_to_remove,
                    expected_staged_parts);

                if (parts_to_remove.size() == 0 && bitmaps_to_remove.size() == 0 && staged_parts_to_remove.size() == 0)
                {
                    fillPartsModel(*table, commit_data.data_parts, *part_models.mutable_parts());
                    fillPartsModel(*table, commit_data.staged_parts, *staged_part_models.mutable_parts());

                    finishCommitInBatch(
                        table,
                        txn_id,
                        part_models,
                        commit_data.delete_bitmaps,
                        staged_part_models,
                        false,
                        expected_parts,
                        expected_bitmap,
                        expected_staged_parts);
                }
                else
                {
                    LOG_INFO(
                        log,
                        "Some parts, bitmaps, staged_parts not found when set commit time, # of parts to remove: {}, # of bitmaps to remove: {}"
                        ,parts_to_remove.size(), bitmaps_to_remove.size());
                    DataPartsVector parts_to_write = commit_data.data_parts;
                    DeleteBitmapMetaPtrVector bitmap_to_write = commit_data.delete_bitmaps;
                    DataPartsVector staged_parts_to_write = commit_data.staged_parts;

                    // remove non existed parts
                    remove_not_exist_items<DataPartPtr>(parts_to_write, parts_to_remove);

                    // remove non existed parts
                    remove_not_exist_items<DeleteBitmapMetaPtr>(bitmap_to_write, bitmaps_to_remove);

                    // remove non existed parts
                    remove_not_exist_items<DataPartPtr>(staged_parts_to_write, staged_parts_to_remove);

                    // Perform logic checking
                    if (parts_to_write.size() != expected_parts.size() || bitmap_to_write.size() != expected_bitmap.size()
                        || staged_parts_to_write.size() != expected_staged_parts.size())
                        throw Exception(
                            "The part size or bitmap size want to insert does not match with the actual existed size in catalog.",
                            ErrorCodes::LOGICAL_ERROR);

                    fillPartsModel(*table, parts_to_write, *part_models.mutable_parts());
                    fillPartsModel(*table, staged_parts_to_write, *staged_part_models.mutable_parts());

                    finishCommitInBatch(
                        table,
                        txn_id,
                        part_models,
                        bitmap_to_write,
                        staged_part_models,
                        false,
                        expected_parts,
                        expected_bitmap,
                        expected_staged_parts);
                }
                LOG_DEBUG(
                    log,
                    "Finish set commit time in bytekv for txn {}, elapsed {} ms, start set commit time in part cache."
                    ,txn_id
                    ,watch.elapsedMilliseconds());

                if (context.getPartCacheManager() && !part_models.parts().empty())
                    context.getPartCacheManager()->insertDataPartsIntoCache(*table, part_models.parts(), false, false);
                LOG_DEBUG(log, "Finish set commit time for txn {}, elapsed {} ms.", txn_id, watch.elapsedMilliseconds());
            },
            ProfileEvents::SetCommitTimeSuccess,
            ProfileEvents::SetCommitTimeFailed);
    }

    /// clear garbage parts generated by aborted or failed transaction.
    void Catalog::clearParts(
        const StoragePtr & storage,
        const CommitItems & commit_data,
        const bool skip_part_cache)
    {
        runWithMetricSupport(
            [&] {
                if (commit_data.empty())
                {
                    return;
                }
                LOG_INFO(
                    log,
                    "Start clear metadata of {} parts, {} delete bitmaps, {} staged parts of table {}."
                    ,commit_data.data_parts.size()
                    ,commit_data.delete_bitmaps.size()
                    ,commit_data.staged_parts.size()
                    ,storage->getStorageID().getNameForLogs());

                Strings drop_keys;
                drop_keys.reserve(commit_data.data_parts.size() + commit_data.delete_bitmaps.size() + commit_data.staged_parts.size());
                String table_uuid = UUIDHelpers::UUIDToString(storage->getStorageID().uuid);
                String part_meta_prefix = MetastoreProxy::dataPartPrefix(name_space, table_uuid);

                /// Add parts, delete_bitmaps and staged_parts into drop list. The order of adding into drop list matters. Should always add parts firstly.
                // Clear part meta
                for (auto & part : commit_data.data_parts)
                    drop_keys.emplace_back(part_meta_prefix + part->info.getPartName());

                // Clear bitmap meta
                for (const auto & bitmap : commit_data.delete_bitmaps)
                {
                    const auto & model = *(bitmap->getModel());
                    drop_keys.emplace_back(MetastoreProxy::deleteBitmapKey(name_space, table_uuid, model));
                }

                // Clear staged part meta
                const auto staged_part_prefix = MetastoreProxy::stagedDataPartPrefix(name_space, table_uuid);
                for (auto & part : commit_data.staged_parts)
                    drop_keys.emplace_back(staged_part_prefix + part->info.getPartName());

                bool need_invalid_cache = context.getPartCacheManager() && !commit_data.data_parts.empty() && !skip_part_cache;
                /// drop in batch if the number of drop keys greater than max_drop_size_one_batch
                if (drop_keys.size() > max_drop_size_one_batch)
                {
                    size_t batch_count{0};
                    while (batch_count + max_drop_size_one_batch < drop_keys.size())
                    {
                        meta_proxy->multiDrop(
                            Strings{drop_keys.begin() + batch_count, drop_keys.begin() + batch_count + max_drop_size_one_batch});
                        /// clear part cache immediately after drop from metastore
                        if (need_invalid_cache)
                        {
                            if (batch_count + max_drop_size_one_batch < commit_data.data_parts.size())
                            {
                                context.getPartCacheManager()->invalidPartCache(
                                    storage->getStorageID().uuid,
                                    DataPartsVector{
                                        commit_data.data_parts.begin() + batch_count,
                                        commit_data.data_parts.begin() + batch_count + max_drop_size_one_batch});
                            }
                            else if (batch_count < commit_data.data_parts.size())
                            {
                                context.getPartCacheManager()->invalidPartCache(
                                    storage->getStorageID().uuid,
                                    DataPartsVector{commit_data.data_parts.begin() + batch_count, commit_data.data_parts.end()});
                            }
                        }
                        batch_count += max_drop_size_one_batch;
                    }
                    meta_proxy->multiDrop(Strings{drop_keys.begin() + batch_count, drop_keys.end()});
                    if (need_invalid_cache && batch_count < commit_data.data_parts.size())
                    {
                        context.getPartCacheManager()->invalidPartCache(
                            storage->getStorageID().uuid,
                            DataPartsVector{commit_data.data_parts.begin() + batch_count, commit_data.data_parts.end()});
                    }
                }
                else
                {
                    meta_proxy->multiDrop(drop_keys);
                    if (need_invalid_cache)
                        context.getPartCacheManager()->invalidPartCache(storage->getStorageID().uuid, commit_data.data_parts);
                }

                LOG_INFO(
                    log,
                    "Finish clear metadata of {} parts, {} delete bitmaps, {} staged parts of table {}."
                    ,commit_data.data_parts.size()
                    ,commit_data.delete_bitmaps.size()
                    ,commit_data.staged_parts.size()
                    ,storage->getStorageID().getNameForLogs());
            },
            ProfileEvents::ClearPartsSuccess,
            ProfileEvents::ClearPartsFailed);
    }

    // write undo buffer before write vfs
    void Catalog::writeUndoBuffer(const String & uuid, const TxnTimestamp & txnID, const UndoResources & resources)
    {
        runWithMetricSupport(
            [&] {
                /// write resources in batch, max batch size is max_commit_size_one_batch
                auto begin = resources.begin(), end = std::min(resources.end(), begin + max_commit_size_one_batch);
                while (begin < end)
                {
                    UndoResources tmp{begin, end};
                    meta_proxy->writeUndoBuffer(name_space, txnID.toUInt64(), uuid, tmp);
                    begin = end;
                    end = std::min(resources.end(), begin + max_commit_size_one_batch);
                }
            },
            ProfileEvents::WriteUndoBufferConstResourceSuccess,
            ProfileEvents::WriteUndoBufferConstResourceFailed);
    }

    void Catalog::writeUndoBuffer(const String & uuid, const TxnTimestamp & txnID, UndoResources && resources)
    {
        runWithMetricSupport(
            [&] {
                auto begin = resources.begin(), end = std::min(resources.end(), begin + max_commit_size_one_batch);
                while (begin < end)
                {
                    UndoResources tmp{std::make_move_iterator(begin), std::make_move_iterator(end)};
                    meta_proxy->writeUndoBuffer(name_space, txnID.toUInt64(), uuid, tmp);
                    begin = end;
                    end = std::min(resources.end(), begin + max_commit_size_one_batch);
                }
            },
            ProfileEvents::WriteUndoBufferNoConstResourceSuccess,
            ProfileEvents::WriteUndoBufferNoConstResourceFailed);
    }

    // clear undo buffer
    void Catalog::clearUndoBuffer(const TxnTimestamp & txnID)
    {
        runWithMetricSupport(
            [&] {
                /// clear by prefix (txnID) for undo buffer.
                meta_proxy->clearUndoBuffer(name_space, txnID.toUInt64());
            },
            ProfileEvents::ClearUndoBufferSuccess,
            ProfileEvents::ClearUndoBufferFailed);
    }

    std::unordered_map<String, UndoResources> Catalog::getUndoBuffer(const TxnTimestamp & txnID)
    {
        std::unordered_map<String, UndoResources> res;
        runWithMetricSupport(
            [&] {
                auto it = meta_proxy->getUndoBuffer(name_space, txnID.toUInt64());
                while (it->next())
                {
                    UndoResource resource = UndoResource::deserialize(it->value());
                    resource.txn_id = txnID;
                    res[resource.uuid()].emplace_back(std::move(resource));
                }
            },
            ProfileEvents::GetUndoBufferSuccess,
            ProfileEvents::GetUndoBufferFailed);
        return res;
    }

    std::unordered_map<UInt64, UndoResources> Catalog::getAllUndoBuffer()
    {
        std::unordered_map<UInt64, UndoResources> txn_undobuffers;
        runWithMetricSupport(
            [&] {
                auto it = meta_proxy->getAllUndoBuffer(name_space);
                size_t size = 0;
                const String ub_prefix{UNDO_BUFFER_PREFIX};
                while (it->next())
                {
                    /// pb_model
                    UndoResource resource = UndoResource::deserialize(it->value());

                    // txn_id
                    auto pos = it->key().find(ub_prefix);
                    if (pos == std::string::npos || pos + ub_prefix.size() > it->key().size())
                    {
                        LOG_ERROR(log, "Invalid undobuffer key: ", it->key());
                        continue;
                    }
                    UInt64 txn_id = std::stoull(it->key().substr(pos + ub_prefix.size()));
                    resource.txn_id = txn_id;
                    txn_undobuffers[txn_id].emplace_back(std::move(resource));
                    size++;
                }

                LOG_DEBUG(log, "Fetched {} undo buffers", size);
            },
            ProfileEvents::GetAllUndoBufferSuccess,
            ProfileEvents::GetAllUndoBufferFailed);
        return txn_undobuffers;
    }

    /// get transaction records, if the records exists, we can check with the transaction coordinator to detect zombie record.
    /// the transaction record will be cleared only after all intents have been cleared and set commit time for all parts.
    /// For zombie record, the intents to be clear can be scanned from intents space with txnid. The parts can be get from undo buffer.
    std::vector<TransactionRecord> Catalog::getTransactionRecords()
    {
        std::vector<TransactionRecord> res;
        runWithMetricSupport(
            [&] {
                /// if exception occurs during get txn record, just return the partial result;
                try
                {
                    auto it = meta_proxy->getAllTransactionRecord(name_space);

                    while (it->next())
                    {
                        res.push_back(TransactionRecord::deserialize(it->value()));
                    }
                }
                catch (...)
                {
                    tryLogDebugCurrentException(__PRETTY_FUNCTION__);
                }
            },
            ProfileEvents::GetTransactionRecordsSuccess,
            ProfileEvents::GetTransactionRecordsFailed);
        return res;
    }

    std::vector<TransactionRecord> Catalog::getTransactionRecords(const std::vector<TxnTimestamp> & txn_ids, size_t batch_size)
    {
        std::vector<TransactionRecord> records;
        runWithMetricSupport(
            [&] {
                size_t total_txn_size = txn_ids.size();

                records.reserve(total_txn_size);

                auto fetch_records_in_batch = [&](size_t begin, size_t end) {
                    auto txn_values = meta_proxy->getTransactionRecords(
                        name_space, std::vector<TxnTimestamp>(txn_ids.begin() + begin, txn_ids.begin() + end));
                    for (size_t i = 0; i < txn_values.size(); ++i)
                    {
                        const auto & data = txn_values[i].first;
                        if (data.empty())
                        {
                            TransactionRecord record;
                            record.setID(txn_ids[i]);
                            records.emplace_back(std::move(record)); // txn_id does not exist
                        }
                        else
                        {
                            records.emplace_back(TransactionRecord::deserialize(data));
                        }
                    }
                };

                if (batch_size > 0)
                {
                    size_t batch_count{0};
                    while (batch_count + batch_size < total_txn_size)
                    {
                        fetch_records_in_batch(batch_count, batch_count + batch_size);
                        batch_count += batch_size;
                    }
                    fetch_records_in_batch(batch_count, total_txn_size);
                }
                else
                    fetch_records_in_batch(0, total_txn_size);
            },
            ProfileEvents::GetTransactionRecordsTxnIdsSuccess,
            ProfileEvents::GetTransactionRecordsTxnIdsFailed);
        return records;
    }

    std::vector<TransactionRecord> Catalog::getTransactionRecordsForGC(size_t max_result_number)
    {
        std::vector<TransactionRecord> res;
        /// if exception occurs during get txn record, just return the partial result;
        std::set<UInt64> primary_txn_ids;
        runWithMetricSupport(
            [&] {
                try
                {
                    auto it = meta_proxy->getAllTransactionRecord(name_space, max_result_number);

                    while (it->next())
                    {
                        auto record = TransactionRecord::deserialize(it->value());
                        if (record.isSecondary())
                        {
                            /// Only clear if the primary transaction is lost.
                            if (primary_txn_ids.find(record.primaryTxnID()) == primary_txn_ids.end())
                            {
                                /// Get the transaction record of the primary transaction -- this is very rare operator
                                if (tryGetTransactionRecord(record.primaryTxnID()))
                                {
                                    primary_txn_ids.insert(record.primaryTxnID());
                                }
                                else
                                {
                                    res.emplace_back(std::move(record));
                                }
                            }
                        }
                        else
                        {
                            if (record.type() == CnchTransactionType::Explicit)
                            {
                                primary_txn_ids.insert(record.primaryTxnID());
                            }
                            res.push_back(std::move(record));
                        }
                    }
                }
                catch (...)
                {
                    tryLogDebugCurrentException(__PRETTY_FUNCTION__);
                }
                LOG_DEBUG(log, "Get {} transaction records for GC.", res.size());
            },
            ProfileEvents::GetTransactionRecordsForGCSuccess,
            ProfileEvents::GetTransactionRecordsForGCFailed);
        return res;
    }


    /// Clear intents written by zombie transaction.
    void Catalog::clearZombieIntent(const TxnTimestamp & txnID)
    {
        runWithMetricSupport(
            [&] {
                /// scan intents by txnID
                /// clear with clearIntents and clearIntent APIs
                meta_proxy->clearZombieIntent(name_space, txnID.toUInt64());
            },
            ProfileEvents::ClearZombieIntentSuccess,
            ProfileEvents::ClearZombieIntentFailed);
    }


    bool Catalog::writeFilesysLock(TxnTimestamp txn_id, const String & dir, const String & db, const String & table)
    {
        bool res;
        runWithMetricSupport(
            [&] {
                if (dir.empty() || db.empty() || table.empty())
                {
                    throw Exception(
                        "Invalid parameter for writeFilesysLock: dir = " + dir + ", db = " + db + ", tb = " + table,
                        ErrorCodes::BAD_ARGUMENTS);
                }
                String normalized_dir = normalizePath(dir);
                if (normalized_dir == "/")
                {
                    throw Exception("Trying to lock the root directory, it's probably a bug", ErrorCodes::LOGICAL_ERROR);
                }
                /// if outer dir is locked, then we cannot lock the inner dir

                String cur_dir = normalized_dir;
                auto get_parent_dir = [](String & dir) {
                    auto pos = dir.rfind('/');
                    if (pos == std::string::npos)
                        dir = "";
                    else
                        dir.erase(dir.begin() + pos, dir.end());
                };

                while (!cur_dir.empty())
                {
                    if (getFilesysLock(cur_dir))
                    {
                        LOG_DEBUG(log, "{} is locked, cannot lock {}", cur_dir, normalized_dir);
                        res = false;
                        return;
                    }
                    get_parent_dir(cur_dir);
                }
                meta_proxy->writeFilesysLock(name_space, txn_id, normalized_dir, db, table);
                res = true;
            },
            ProfileEvents::WriteFilesysLockSuccess,
            ProfileEvents::WriteFilesysLockFailed);
        return res;
    }

    std::optional<FilesysLock> Catalog::getFilesysLock(const String & dir)
    {
        std::optional<FilesysLock> outRes;
        runWithMetricSupport(
            [&] {
                String normalized_dir = normalizePath(dir);
                auto data = meta_proxy->getFilesysLock(name_space, dir);
                if (!data.empty())
                {
                    std::optional<FilesysLock> res{FilesysLock()};
                    res->pb_model.ParseFromString(data);
                    outRes = res;
                }
                else
                {
                    outRes = {};
                }
            },
            ProfileEvents::GetFilesysLockSuccess,
            ProfileEvents::GetFilesysLockFailed);
        return outRes;
    }

    void Catalog::clearFilesysLock(const String & dir)
    {
        runWithMetricSupport(
            [&] {
                String normalized_dir = normalizePath(dir);
                if (normalized_dir == "/")
                {
                    throw Exception("Trying to unlock the root directory, it's probably a bug", ErrorCodes::LOGICAL_ERROR);
                }
                meta_proxy->clearFilesysLock(name_space, dir);
            },
            ProfileEvents::ClearFilesysLockDirSuccess,
            ProfileEvents::ClearFilesysLockDirFailed);
    }

    void Catalog::clearFilesysLock(TxnTimestamp txn_id)
    {
        runWithMetricSupport(
            [&] {
                /// get all lock and filter by id, this is inefficient, but it is ok for now.
                auto it = meta_proxy->getAllFilesysLock(name_space);
                std::vector<String> to_delete;
                FilesysLock record;
                while (it->next())
                {
                    record.pb_model.ParseFromString(it->value());
                    if (record.pb_model.txn_id() == txn_id.toUInt64())
                        to_delete.push_back(std::move(record.pb_model.directory()));
                }
                /// may need multi-write later, as now there's will be only 1 dir at most
                std::for_each(to_delete.begin(), to_delete.end(), [this](const String & dir) { clearFilesysLock(dir); });
            },
            ProfileEvents::ClearFilesysLockTxnIdSuccess,
            ProfileEvents::ClearFilesysLockTxnIdFailed);
    }

    std::vector<FilesysLock> Catalog::getAllFilesysLock()
    {
        std::vector<FilesysLock> res;
        runWithMetricSupport(
            [&] {
                auto it = meta_proxy->getAllFilesysLock(name_space);

                FilesysLock record;
                while (it->next())
                {
                    record.pb_model.ParseFromString(it->value());
                    res.push_back(std::move(record));
                }
            },
            ProfileEvents::GetAllFilesysLockSuccess,
            ProfileEvents::GetAllFilesysLockFailed);
        return res;
    }

    void Catalog::insertTransaction(TxnTimestamp & txnID)
    {
        runWithMetricSupport(
            [&] { meta_proxy->insertTransaction(txnID.toUInt64()); },
            ProfileEvents::InsertTransactionSuccess,
            ProfileEvents::InsertTransactionFailed);
    }

    void Catalog::removeTransaction(const TxnTimestamp & txnID)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeTransaction(txnID.toUInt64()); },
            ProfileEvents::RemoveTransactionSuccess,
            ProfileEvents::RemoveTransactionFailed);
    }

    std::vector<TxnTimestamp> Catalog::getActiveTransactions()
    {
        std::vector<TxnTimestamp> res;
        runWithMetricSupport(
            [&] {
                IMetaStore::IteratorPtr it = meta_proxy->getActiveTransactions();

                while (it->next())
                {
                    UInt64 txnID = std::stoull(it->key().substr(String(TRANSACTION_STORE_PREFIX).size(), std::string::npos), nullptr);
                    res.emplace_back(txnID);
                }
            },
            ProfileEvents::GetActiveTransactionsSuccess,
            ProfileEvents::GetActiveTransactionsFailed);
        return res;
    }

    void Catalog::updateServerWorkerGroup(const String & vw_name, const String & worker_group_name, const HostWithPortsVec & workers)
    {
        runWithMetricSupport(
            [&] {
                Protos::DataModelWorkerGroup worker_group_model;
                worker_group_model.set_vw_name(vw_name);
                worker_group_model.set_worker_group_name(worker_group_name);

                for (auto & worker : workers)
                    RPCHelpers::fillHostWithPorts(worker, *worker_group_model.add_host_ports_vec());

                String worker_group_meta;
                worker_group_model.SerializeToString(&worker_group_meta);

                meta_proxy->updateServerWorkerGroup(worker_group_name, worker_group_meta);
            },
            ProfileEvents::UpdateServerWorkerGroupSuccess,
            ProfileEvents::UpdateServerWorkerGroupFailed);
    }

    HostWithPortsVec Catalog::getWorkersInWorkerGroup(const String & worker_group_name)
    {
        HostWithPortsVec res;
        runWithMetricSupport(
            [&] {
                String worker_group_meta;
                meta_proxy->getServerWorkerGroup(worker_group_name, worker_group_meta);

                if (worker_group_meta.empty())
                    throw Exception("Cannot find worker_group: " + worker_group_name, ErrorCodes::VIRTUAL_WAREHOUSE_NOT_FOUND);

                Protos::DataModelWorkerGroup worker_group_model;
                worker_group_model.ParseFromString(worker_group_meta);


                for (const auto & host_ports : worker_group_model.host_ports_vec())
                    res.push_back(RPCHelpers::createHostWithPorts(host_ports));
            },
            ProfileEvents::GetWorkersInWorkerGroupSuccess,
            ProfileEvents::GetWorkersInWorkerGroupFailed);
        return res;
    }

    Catalog::DataModelDBs Catalog::getAllDataBases()
    {
        DataModelDBs res;
        runWithMetricSupport(
            [&] {
                IMetaStore::IteratorPtr it = meta_proxy->getAllDatabaseMeta(name_space);

                std::unordered_map<String, DB::Protos::DataModelDB> db_map{};
                while (it->next())
                {
                    DB::Protos::DataModelDB db_data;
                    db_data.ParseFromString(it->value());
                    if (db_map.count(db_data.name()))
                    {
                        // if multiple versions exists, only return the version with larger commit_time
                        if (db_map[db_data.name()].commit_time() < db_data.commit_time())
                            db_map[db_data.name()] = db_data;
                    }
                    else
                        db_map.emplace(db_data.name(), db_data);
                }

                for (auto m_it = db_map.begin(); m_it != db_map.end(); m_it++)
                    res.push_back(m_it->second);
            },
            ProfileEvents::GetAllDataBasesSuccess,
            ProfileEvents::GetAllDataBasesFailed);
        return res;
    }

    Catalog::DataModelTables Catalog::getAllTables()
    {
        DataModelTables res;
        runWithMetricSupport(
            [&] {
                /// there may be diferrent version of the same table, we only need the latest version of it. And meta data of a table is sorted by commit time as ascending order in bytekv.
                Protos::DataModelTable latest_version, table_data;
                auto it = meta_proxy->getAllTablesMeta(name_space);
                while (it->next())
                {
                    table_data.ParseFromString(it->value());

                    if (latest_version.name().empty())
                    {
                        // initialize latest_version;
                        latest_version = table_data;
                    }
                    else
                    {
                        // will return the last version of table meta
                        if (latest_version.uuid().low() != table_data.uuid().low()
                            || latest_version.uuid().high() != table_data.uuid().high())
                            res.push_back(latest_version);

                        latest_version = table_data;
                    }
                }

                if (!latest_version.name().empty())
                    res.push_back(latest_version);
            },
            ProfileEvents::GetAllTablesSuccess,
            ProfileEvents::GetAllTablesFailed);
        // the result will be ordered by table uuid
        return res;
    }

    IMetaStore::IteratorPtr Catalog::getTrashTableIDIterator(uint32_t iterator_internal_batch_size)
    {
        IMetaStore::IteratorPtr res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTrashTableIDIterator(name_space, iterator_internal_batch_size); },
            ProfileEvents::GetTrashTableIDIteratorSuccess,
            ProfileEvents::GetTrashTableIDIteratorFailed);
        return res;
    }

    Catalog::DataModelUDFs Catalog::getAllUDFs(const String & database_name, const String & function_name)
    {
        DataModelUDFs res;
        runWithMetricSupport(
            [&] {
                if (!database_name.empty() && !function_name.empty())
                {
                    res = getUDFByName({database_name + "." + function_name});
                    return;
                }
                const auto & it = meta_proxy->getAllUDFsMeta(name_space, database_name);
                while (it->next())
                {
                    DB::Protos::DataModelUDF model;

                    model.ParseFromString(it->value());
                    res.emplace_back(std::move(model));
                }
            },
            ProfileEvents::GetAllUDFsSuccess,
            ProfileEvents::GetAllUDFsFailed);
        return res;
    }

    Catalog::DataModelUDFs Catalog::getUDFByName(const std::unordered_set<String> & function_names)
    {
        DataModelUDFs res;
        runWithMetricSupport(
            [&] {
                auto udfs_info = meta_proxy->getUDFsMetaByName(name_space, function_names);


                for (const auto & it : udfs_info)
                {
                    res.emplace_back();
                    if (!it.empty())
                        res.back().ParseFromString(it);
                }
            },
            ProfileEvents::GetUDFByNameSuccess,
            ProfileEvents::GetUDFByNameFailed);
        return res;
    }

    std::vector<std::shared_ptr<Protos::TableIdentifier>> Catalog::getTrashTableID()
    {
        std::vector<std::shared_ptr<Protos::TableIdentifier>> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTrashTableID(name_space); },
            ProfileEvents::GetTrashTableIDSuccess,
            ProfileEvents::GetTrashTableIDFailed);
        return res;
    }

    Catalog::DataModelTables Catalog::getTablesInTrash()
    {
        DataModelTables res;
        runWithMetricSupport(
            [&] {
                auto identifiers = meta_proxy->getTrashTableID(name_space);
                res = getTablesByID(identifiers);
            },
            ProfileEvents::GetTablesInTrashSuccess,
            ProfileEvents::GetTablesInTrashFailed);
        return res;
    }

    Catalog::DataModelDBs Catalog::getDatabaseInTrash()
    {
        DataModelDBs res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTrashDBs(name_space); },
            ProfileEvents::GetDatabaseInTrashSuccess,
            ProfileEvents::GetDatabaseInTrashFailed);
        return res;
    }

    std::vector<std::shared_ptr<Protos::TableIdentifier>> Catalog::getAllTablesID(const String & db)
    {
        std::vector<std::shared_ptr<Protos::TableIdentifier>> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAllTablesId(name_space, db); },
            ProfileEvents::GetAllTablesIDSuccess,
            ProfileEvents::GetAllTablesIDFailed);
        return res;
    }

    std::shared_ptr<Protos::TableIdentifier> Catalog::getTableIDByName(const String & db, const String & table)
    {
        std::shared_ptr<Protos::TableIdentifier> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTableID(name_space, db, table); },
            ProfileEvents::GetTableIDByNameSuccess,
            ProfileEvents::GetTableIDByNameFailed);
        return res;
    }

    Catalog::DataModelWorkerGroups Catalog::getAllWorkerGroups()
    {
        DataModelWorkerGroups res;
        runWithMetricSupport(
            [&] {
                IMetaStore::IteratorPtr it = meta_proxy->getAllWorkerGroupMeta();

                while (it->next())
                {
                    DB::Protos::DataModelWorkerGroup worker_group_data;
                    worker_group_data.ParseFromString(it->value());
                    res.push_back(worker_group_data);
                }
            },
            ProfileEvents::GetAllWorkerGroupsSuccess,
            ProfileEvents::GetAllWorkerGroupsFailed);
        return res;
    }

    Catalog::DataModelDictionaries Catalog::getAllDictionaries()
    {
        DataModelDictionaries res;
        runWithMetricSupport(
            [&] {
                IMetaStore::IteratorPtr it = meta_proxy->getAllDictionaryMeta(name_space);

                while (it->next())
                {
                    DB::Protos::DataModelDictionary dict_data;
                    dict_data.ParseFromString(it->value());
                    res.push_back(dict_data);
                }
            },
            ProfileEvents::GetAllDictionariesSuccess,
            ProfileEvents::GetAllDictionariesFailed);
        return res;
    }

    void Catalog::clearDatabaseMeta(const String & database, const UInt64 & ts)
    {
        runWithMetricSupport(
            [&] {
                LOG_INFO(log, "Start clear metadata for database : {}", database);

                Strings databases_meta;
                meta_proxy->getDatabase(name_space, database, databases_meta);

                Protos::DataModelDB db_model;
                for (auto & meta : databases_meta)
                {
                    db_model.ParseFromString(meta);
                    if (db_model.commit_time() <= ts)
                        meta_proxy->dropDatabase(name_space, db_model);
                }

                LOG_INFO(log, "Finish clear metadata for database : {}", database);
            },
            ProfileEvents::ClearDatabaseMetaSuccess,
            ProfileEvents::ClearDatabaseMetaFailed);
    }

    void Catalog::clearTableMetaForGC(const String & database, const String & name, const UInt64 & ts)
    {
        runWithMetricSupport(
            [&] {
                String table_uuid = meta_proxy->getTrashTableUUID(name_space, database, name, ts);
                auto table = tryGetTableFromMetastore(table_uuid, ts, false, true);

                Strings dependencies;
                if (table)
                    dependencies = tryGetDependency(parseCreateQuery(table->definition()));

                meta_proxy->clearTableMeta(name_space, database, name, table_uuid, dependencies, ts);

                if (!table_uuid.empty() && context.getPartCacheManager())
                {
                    UUID uuid = UUID(stringToUUID(table_uuid));
                    context.getPartCacheManager()->invalidPartCache(uuid);
                }
            },
            ProfileEvents::ClearTableMetaForGCSuccess,
            ProfileEvents::ClearTableMetaForGCFailed);
    }

    void Catalog::clearDataPartsMeta(const StoragePtr & storage, const DataPartsVector & parts, const bool skip_part_cache)
    {
        runWithMetricSupport(
            [&] {
                clearParts(storage, CommitItems{parts, {}, {}}, skip_part_cache);
            },
            ProfileEvents::ClearDataPartsMetaSuccess,
            ProfileEvents::ClearDataPartsMetaFailed);
    }

    void Catalog::clearStagePartsMeta(const StoragePtr & storage, const DataPartsVector & parts)
    {
        runWithMetricSupport(
            [&] {
                clearParts(storage, CommitItems{{}, {}, parts});
            },
            ProfileEvents::ClearStagePartsMetaSuccess,
            ProfileEvents::ClearStagePartsMetaFailed);
    }

    void Catalog::clearDataPartsMetaForTable(const StoragePtr & storage)
    {
        runWithMetricSupport(
            [&] {
                LOG_INFO(log, "Start clear all data parts for table : {}.{}", storage->getDatabaseName(), storage->getTableName());

                meta_proxy->dropAllPartInTable(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid));

                if (context.getPartCacheManager())
                {
                    auto cache_manager = context.getPartCacheManager();
                    cache_manager->invalidPartCache(storage->getStorageID().uuid);
                }

                LOG_INFO(log, "Finish clear all data parts for table : {}.{}", storage->getDatabaseName(), storage->getTableName());
            },
            ProfileEvents::ClearDataPartsMetaForTableSuccess,
            ProfileEvents::ClearDataPartsMetaForTableFailed);
    }

    std::vector<TxnTimestamp> Catalog::getSyncList(const StoragePtr & storage)
    {
        std::vector<TxnTimestamp> res;
        runWithMetricSupport(
            [&] {
                IMetaStore::IteratorPtr it = meta_proxy->getSyncList(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid));

                while (it->next())
                {
                    UInt64 commit_time = std::stoull(it->value(), nullptr);
                    res.emplace_back(commit_time);
                }
            },
            ProfileEvents::GetSyncListSuccess,
            ProfileEvents::GetSyncListFailed);
        return res;
    }

    void Catalog::clearSyncList(const StoragePtr & storage, std::vector<TxnTimestamp> & sync_list)
    {
        runWithMetricSupport(
            [&] { meta_proxy->clearSyncList(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid), sync_list); },
            ProfileEvents::ClearSyncListSuccess,
            ProfileEvents::ClearSyncListFailed);
    }

    DB::ServerDataPartsVector Catalog::getServerPartsByCommitTime(const StoragePtr & table, std::vector<TxnTimestamp> & sync_list)
    {
        ServerDataPartsVector res;
        runWithMetricSupport(
            [&] {
                std::unordered_set<UInt64> sync_set;
                for (auto & ele : sync_list)
                    sync_set.emplace(ele.toUInt64());

                IMetaStore::IteratorPtr it
                    = meta_proxy->getPartsInRange(name_space, UUIDHelpers::UUIDToString(table->getStorageUUID()), "");


                auto & storage = dynamic_cast<MergeTreeMetaBase &>(*table);

                while (it->next())
                {
                    const auto & key = it->key();
                    auto pos = key.find_last_of('_');
                    if (pos != String::npos)
                    {
                        UInt64 commit_time = std::stoull(key.substr(pos + 1, String::npos), nullptr);
                        if (sync_set.count(commit_time))
                        {
                            Protos::DataModelPart part_model;
                            part_model.ParseFromString(it->value());
                            res.push_back(std::make_shared<ServerDataPart>(createPartWrapperFromModel(storage, part_model)));
                        }
                    }
                }
            },
            ProfileEvents::GetServerPartsByCommitTimeSuccess,
            ProfileEvents::GetServerPartsByCommitTimeFailed);
        return res;
    }

    void Catalog::createRootPath(const String & path)
    {
        runWithMetricSupport(
            [&] { meta_proxy->createRootPath(path); }, ProfileEvents::CreateRootPathSuccess, ProfileEvents::CreateRootPathFailed);
    }

    void Catalog::deleteRootPath(const String & path)
    {
        runWithMetricSupport(
            [&] { meta_proxy->deleteRootPath(path); }, ProfileEvents::DeleteRootPathSuccess, ProfileEvents::DeleteRootPathFailed);
    }

    std::vector<std::pair<String, UInt32>> Catalog::getAllRootPath()
    {
        std::vector<std::pair<String, UInt32>> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAllRootPath(); }, ProfileEvents::GetAllRootPathSuccess, ProfileEvents::GetAllRootPathFailed);

        return res;
    }

    void Catalog::createMutation(const StorageID & storage_id, const String & mutation_name, const String & mutate_text)
    {
        runWithMetricSupport(
            [&] { meta_proxy->createMutation(name_space, UUIDHelpers::UUIDToString(storage_id.uuid), mutation_name, mutate_text); },
            ProfileEvents::CreateMutationSuccess,
            ProfileEvents::CreateMutationFailed);
    }

    void Catalog::removeMutation(const StorageID & storage_id, const String & mutation_name)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeMutation(name_space, UUIDHelpers::UUIDToString(storage_id.uuid), mutation_name); },
            ProfileEvents::RemoveMutationSuccess,
            ProfileEvents::RemoveMutationFailed);
    }

    Strings Catalog::getAllMutations(const StorageID & storage_id)
    {
        Strings res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAllMutations(name_space, UUIDHelpers::UUIDToString(storage_id.uuid)); },
            ProfileEvents::GetAllMutationsStorageIdSuccess,
            ProfileEvents::GetAllMutationsStorageIdFailed);
        return res;
    }

    // uuid, mutation
    std::multimap<String, String> Catalog::getAllMutations()
    {
        std::multimap<String, String> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAllMutations(name_space); },
            ProfileEvents::GetAllMutationsSuccess,
            ProfileEvents::GetAllMutationsFailed);
        return res;
    }

    void Catalog::setTableClusterStatus(const UUID & table_uuid, const bool clustered)
    {
        runWithMetricSupport(
            [&] {
                meta_proxy->setTableClusterStatus(name_space, UUIDHelpers::UUIDToString(table_uuid), clustered);
                /// keep the cache status up to date.
                if (context.getPartCacheManager())
                    context.getPartCacheManager()->setTableClusterStatus(table_uuid, clustered);
            },
            ProfileEvents::SetTableClusterStatusSuccess,
            ProfileEvents::SetTableClusterStatusFailed);
    }

    void Catalog::getTableClusterStatus(const UUID & table_uuid, bool & clustered)
    {
        runWithMetricSupport(
            [&] { meta_proxy->getTableClusterStatus(name_space, UUIDHelpers::UUIDToString(table_uuid), clustered); },
            ProfileEvents::GetTableClusterStatusSuccess,
            ProfileEvents::GetTableClusterStatusFailed);
    }

    bool Catalog::isTableClustered(const UUID & table_uuid)
    {
        bool outRes;
        runWithMetricSupport(
            [&] {
                if (context.getPartCacheManager())
                {
                    auto res = context.getPartCacheManager()->getTableClusterStatus(table_uuid);
                    outRes = res;
                }
                else
                {
                    bool clustered;
                    getTableClusterStatus(table_uuid, clustered);
                    outRes = clustered;
                }
            },
            ProfileEvents::IsTableClusteredSuccess,
            ProfileEvents::IsTableClusteredFailed);
        return outRes;
    }

    void Catalog::setBGJobStatus(const UUID & table_uuid, CnchBGThreadType type, CnchBGThreadStatus status)
    {
        runWithMetricSupport(
            [&] {
                    meta_proxy->setBGJobStatus(
                        name_space, UUIDHelpers::UUIDToString(table_uuid), type, status);
                },
            ProfileEvents::SetBGJobStatusSuccess,
            ProfileEvents::SetBGJobStatusFailed);
    }

    std::optional<CnchBGThreadStatus> Catalog::getBGJobStatus(const UUID & table_uuid, CnchBGThreadType type)
    {
        std::optional<CnchBGThreadStatus> res;
        runWithMetricSupport(
            [&] {
                    res = meta_proxy->getBGJobStatus(
                        name_space, UUIDHelpers::UUIDToString(table_uuid), type);
                },
            ProfileEvents::GetBGJobStatusSuccess,
            ProfileEvents::GetBGJobStatusFailed);

        return res;
    }

    std::unordered_map<UUID, CnchBGThreadStatus> Catalog::getBGJobStatuses(CnchBGThreadType type)
    {
        std::unordered_map<UUID, CnchBGThreadStatus> res;
        runWithMetricSupport(
            [&] {
                    res = meta_proxy->getBGJobStatuses(
                        name_space, type);
                },
            ProfileEvents::GetBGJobStatusesSuccess,
            ProfileEvents::GetBGJobStatusesFailed);

        return res;
    }

    void Catalog::dropBGJobStatus(const UUID & table_uuid, CnchBGThreadType type)
    {
        runWithMetricSupport(
            [&] {
                    meta_proxy->dropBGJobStatus(
                        name_space, UUIDHelpers::UUIDToString(table_uuid), type);
                },
            ProfileEvents::DropBGJobStatusSuccess,
            ProfileEvents::DropBGJobStatusFailed);
    }

    void Catalog::setTablePreallocateVW(const UUID & table_uuid, const String vw)
    {
        runWithMetricSupport(
            [&] {
                meta_proxy->setTablePreallocateVW(name_space, UUIDHelpers::UUIDToString(table_uuid), vw);
                /// keep the cache status up to date.
                if (context.getPartCacheManager())
                    context.getPartCacheManager()->setTablePreallocateVW(table_uuid, vw);
            },
            ProfileEvents::SetTablePreallocateVWSuccess,
            ProfileEvents::SetTablePreallocateVWFailed);
    }

    void Catalog::getTablePreallocateVW(const UUID & table_uuid, String & vw)
    {
        runWithMetricSupport(
            [&] { meta_proxy->getTablePreallocateVW(name_space, UUIDHelpers::UUIDToString(table_uuid), vw); },
            ProfileEvents::GetTablePreallocateVWSuccess,
            ProfileEvents::GetTablePreallocateVWFailed);
    }


    std::unordered_map<String, PartitionFullPtr>
    Catalog::getTablePartitionMetrics(const DB::Protos::DataModelTable & table, bool & is_ready)
    {
        std::unordered_map<String, PartitionFullPtr> res;
        runWithMetricSupport(
            [&] {
                auto storage = createTableFromDataModel(context, table);

                /// getTablePartitionMetrics can only be called on server, so the second condition should always be true
                if (storage && context.getPartCacheManager())
                {
                    is_ready = context.getPartCacheManager()->getTablePartitionMetrics(*storage, res);
                }
                else
                {
                    /// Table may already be marked as deleted, just return empty partition metrics and set is_ready `true`
                    is_ready = true;
                }
            },
            ProfileEvents::GetTablePartitionMetricsSuccess,
            ProfileEvents::GetTablePartitionMetricsFailed);
        return res;
    }

    std::unordered_map<String, PartitionMetricsPtr> Catalog::getTablePartitionMetricsFromMetastore(const String & table_uuid)
    {
        std::unordered_map<String, PartitionMetricsPtr> partition_map;
        /// calculate metrics partition by partition.
        auto calculate_metrics_by_partion = [&](std::unordered_map<String, Protos::DataModelPart> & parts, bool calc_visibility) {
            PartitionMetricsPtr res = std::make_shared<PartitionMetrics>();
            /// when there is drop range in this partition, we need to calculate visibility.
            if (calc_visibility)
            {
                ServerDataPartsVector server_parts;
                server_parts.reserve(parts.size());
                for (auto it = parts.begin(); it != parts.end(); it++)
                {
                    DataModelPartWrapperPtr part_model_wrapper = createPartWrapperFromModelBasic(it->second);
                    server_parts.push_back(std::make_shared<ServerDataPart>(std::move(part_model_wrapper)));
                }
                auto visible_parts = CnchPartsHelper::calcVisibleParts(server_parts, false);
                for (auto s_part : visible_parts)
                    res->update(*(s_part->part_model_wrapper->part_model));
            }
            else
            {
                for (auto it = parts.begin(); it != parts.end(); it++)
                {
                    /// for those block only has deleted part, just ignore them because the covered part may be already removed by GC
                    if (it->second.deleted())
                        continue;

                    res->update(it->second);
                }
            }

            return res;
        };

        runWithMetricSupport(
            [&] {
                Stopwatch watch;
                SCOPE_EXIT({
                    LOG_DEBUG(
                        log, "getTablePartitionMetrics for table {}  elapsed: {}ms", table_uuid, watch.elapsedMilliseconds());
                });

                IMetaStore::IteratorPtr it = meta_proxy->getPartsInRange(name_space, table_uuid, "");
                std::unordered_map<String, Protos::DataModelPart> block_name_to_part;
                String current_partition = "";
                bool need_calculate_visibility = false;
                PartitionMetricsPtr current_metrics = std::make_shared<PartitionMetrics>();
                while (it->next())
                {
                    Protos::DataModelPart part_model;
                    part_model.ParseFromString(it->value());
                    const String & partition_id = part_model.part_info().partition_id();

                    if (current_partition != partition_id)
                    {
                        if (!block_name_to_part.empty())
                        {
                            PartitionMetricsPtr metrics = calculate_metrics_by_partion(block_name_to_part, need_calculate_visibility);
                            partition_map.emplace(current_partition, metrics);
                        }

                        block_name_to_part.clear();
                        need_calculate_visibility = false;
                        current_partition = partition_id;
                    }

                    auto part_info = createPartInfoFromModel(part_model.part_info());
                    /// skip partial part.
                    if (part_info->hint_mutation)
                        continue;

                    /// if there is any drop range in current partition , we need calculate visibility when computing metrics later
                    if (part_model.deleted() && part_info->level == MergeTreePartInfo::MAX_LEVEL)
                        need_calculate_visibility = true;

                    String block_name = part_info->getBlockName();
                    auto it = block_name_to_part.find(block_name);
                    /// When there are parts with same block ID, it means the part has been marked as deleted (Remember, we have filter out partial part).
                    /// So we can safely remove them from container since we don't need count in those parts.
                    if (it != block_name_to_part.end())
                        block_name_to_part.erase(it);
                    else
                        block_name_to_part.emplace(block_name, part_model);
                }

                if (!block_name_to_part.empty())
                {
                    PartitionMetricsPtr metrics = calculate_metrics_by_partion(block_name_to_part, need_calculate_visibility);
                    partition_map.emplace(current_partition, metrics);
                }
            },
            ProfileEvents::GetTablePartitionMetricsFromMetastoreSuccess,
            ProfileEvents::GetTablePartitionMetricsFromMetastoreFailed);
        return partition_map;
    }

    void Catalog::updateTopologies(const std::list<CnchServerTopology> & topologies)
    {
        runWithMetricSupport(
            [&] {
                LOG_TRACE(log, "Updating topologies : {}", DB::dumpTopologies(topologies));

                if (topologies.empty())
                {
                    return;
                }
                Protos::DataModelTopologyVersions topology_versions;
                fillTopologyVersions(topologies, *topology_versions.mutable_topologies());
                meta_proxy->updateTopologyMeta(name_space, topology_versions.SerializeAsString());
            },
            ProfileEvents::UpdateTopologiesSuccess,
            ProfileEvents::UpdateTopologiesFailed);
    }

    std::list<CnchServerTopology> Catalog::getTopologies()
    {
        std::list<CnchServerTopology> res;
        runWithMetricSupport(
            [&] {
                String topology_meta = meta_proxy->getTopologyMeta(name_space);
                if (topology_meta.empty())
                {
                    res = {};
                    return;
                }
                Protos::DataModelTopologyVersions topology_versions;
                topology_versions.ParseFromString(topology_meta);
                res = createTopologyVersionsFromModel(topology_versions.topologies());
            },
            ProfileEvents::GetTopologiesSuccess,
            ProfileEvents::GetTopologiesFailed);
        return res;
    }

    std::vector<UInt64> Catalog::getTrashDBVersions(const String & database)
    {
        std::vector<UInt64> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTrashDBVersions(name_space, database); },
            ProfileEvents::GetTrashDBVersionsSuccess,
            ProfileEvents::GetTrashDBVersionsFailed);
        return res;
    }

    void Catalog::undropDatabase(const String & database, const UInt64 & ts)
    {
        runWithMetricSupport(
            [&] {
                auto db_model_ptr = tryGetDatabaseFromMetastore(database, ts);
                if (!db_model_ptr)
                    throw Exception("Cannot find database in trash to restore.", ErrorCodes::UNKNOWN_DATABASE);

                // delete the record from db trash as well as the corresponding version of db meta.
                BatchCommitRequest batch_writes;
                batch_writes.AddDelete(SingleDeleteRequest(MetastoreProxy::dbTrashKey(name_space, database, ts)));
                batch_writes.AddDelete(SingleDeleteRequest(MetastoreProxy::dbKey(name_space, database, ts)));

                // restore table and dictionary
                String trashDBName = database + "_" + toString(ts);
                auto table_id_ptrs = meta_proxy->getTablesFromTrash(name_space, trashDBName);
                auto dic_ptrs = meta_proxy->getDictionariesFromTrash(name_space, trashDBName);

                for (auto & table_id_ptr : table_id_ptrs)
                {
                    table_id_ptr->set_database(database);
                    batch_writes.AddDelete(SingleDeleteRequest(MetastoreProxy::tableTrashKey(name_space, trashDBName, table_id_ptr->name(), ts)));
                    restoreTableFromTrash(table_id_ptr, ts, batch_writes);
                }

                for (auto & dic_ptr : dic_ptrs)
                {
                    batch_writes.AddDelete(SingleDeleteRequest(MetastoreProxy::dictionaryTrashKey(name_space, trashDBName, dic_ptr->name())));
                    batch_writes.AddPut(SinglePutRequest(
                        MetastoreProxy::dictionaryStoreKey(name_space, database, dic_ptr->name()), dic_ptr->SerializeAsString()));
                }

                BatchCommitResponse resp;
                meta_proxy->batchWrite(batch_writes, resp);
            },
            ProfileEvents::UndropDatabaseSuccess,
            ProfileEvents::UndropDatabaseFailed);
    }

    std::unordered_map<String, UInt64> Catalog::getTrashTableVersions(const String & database, const String & table)
    {
        std::unordered_map<String, UInt64> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTrashTableVersions(name_space, database, table); },
            ProfileEvents::GetTrashTableVersionsSuccess,
            ProfileEvents::GetTrashTableVersionsFailed);
        return res;
    }

    void Catalog::undropTable(const String & database, const String & table, const UInt64 & ts)
    {
        runWithMetricSupport(
            [&] {
                auto trash_id_ptr = meta_proxy->getTrashTableID(name_space, database, table, ts);
                if (!trash_id_ptr)
                    throw Exception("Cannot find table in trash to restore", ErrorCodes::UNKNOWN_TABLE);

                BatchCommitRequest batch_writes;
                restoreTableFromTrash(trash_id_ptr, ts, batch_writes);
                BatchCommitResponse resp;
                meta_proxy->batchWrite(batch_writes, resp);
            },
            ProfileEvents::UndropTableSuccess,
            ProfileEvents::UndropTableFailed);
    }

    std::shared_ptr<Protos::DataModelDB> Catalog::tryGetDatabaseFromMetastore(const String & database, const UInt64 & ts)
    {
        std::shared_ptr<DB::Protos::DataModelDB> res;

        Strings databases_meta;
        meta_proxy->getDatabase(name_space, database, databases_meta);

        std::map<UInt64, std::shared_ptr<DB::Protos::DataModelDB>, std::greater<>> db_versions;

        for (auto & meta : databases_meta)
        {
            std::shared_ptr<DB::Protos::DataModelDB> model(new DB::Protos::DataModelDB);
            model->ParseFromString(meta);
            if (model->commit_time() < ts)
                db_versions.emplace(model->commit_time(), model);
        }

        if (!db_versions.empty() && !Status::isDeleted(db_versions.begin()->second->status()))
            res = db_versions.begin()->second;

        return res;
    }

    std::shared_ptr<Protos::DataModelTable> Catalog::tryGetTableFromMetastore(const String & table_uuid, const UInt64 & ts, bool with_prev_versions, bool with_deleted)
    {
        Strings tables_meta;
        meta_proxy->getTableByUUID(name_space, table_uuid, tables_meta);
        if (tables_meta.empty())
            return {};

        /// get latest table name. we may replace previous version's table name with the latest ones.
        DB::Protos::DataModelTable latest_version;
        latest_version.ParseFromString(tables_meta.back());
        const String & latest_db_name = latest_version.database();
        const String & latest_table_name = latest_version.name();

        std::vector<std::shared_ptr<DB::Protos::DataModelTable>> table_versions;
        for (auto & table_meta : tables_meta)
        {
            std::shared_ptr<DB::Protos::DataModelTable> model(new DB::Protos::DataModelTable);
            model->ParseFromString(table_meta);
            if (model->commit_time() <= ts)
            {
                if (model->database() != latest_db_name || model->name() != latest_table_name)
                    replace_definition(*model, latest_db_name, latest_table_name);
                table_versions.push_back(model);
            }
            else
                break;
        }

        if (table_versions.empty() || (Status::isDeleted(table_versions.back()->status()) && !with_deleted))
            return {};

        auto & table = table_versions.back();

        /// collect all previous version's definition
        if (with_prev_versions)
        {
            table->clear_definitions();
            for (size_t i = 0; i < table_versions.size() - 1; i++)
            {
                auto added_definition = table->add_definitions();
                added_definition->set_commit_time(table_versions[i]->commit_time());
                added_definition->set_definition(table_versions[i]->definition());
            }
        }

        return table;
    }

    // MaskingPolicyExists Catalog::maskingPolicyExists(const Strings & masking_policy_names)
    // {
    //     MaskingPolicyExists outRes;
    //     runWithMetricSupport(
    //         [&] {
    //             MaskingPolicyExists res(masking_policy_names.size());
    //             if (masking_policy_names.empty())
    //             {
    //                 outRes = res;
    //             }
    //             auto policies = meta_proxy->getMaskingPolicies(name_space, masking_policy_names);
    //             for (const auto i : ext::range(0, res.size()))
    //                 res.set(i, !policies[i].empty());
    //             outRes = res;
    //         },
    //         ProfileEvents::MaskingPolicyExistsSuccess,
    //         ProfileEvents::MaskingPolicyExistsFailed);
    //     return outRes;
    // }

    // std::vector<std::optional<MaskingPolicyModel>> Catalog::getMaskingPolicies(const Strings & masking_policy_names)
    // {
    //     std::vector<std::optional<MaskingPolicyModel>> outRes;
    //     runWithMetricSupport(
    //         [&] {
    //             if (masking_policy_names.empty())
    //             {
    //                 outRes = {};
    //                 return;
    //             }
    //             std::vector<std::optional<MaskingPolicyModel>> res(masking_policy_names.size());
    //             auto policies = meta_proxy->getMaskingPolicies(name_space, masking_policy_names);
    //             for (const auto i : ext::range(0, res.size()))
    //             {
    //                 if (!policies[i].empty())
    //                 {
    //                     MaskingPolicyModel model;
    //                     model.ParseFromString(policies[i]);
    //                     res[i] = std::move(model);
    //                 }
    //             }
    //             outRes = res;
    //         },
    //         ProfileEvents::GetMaskingPoliciesSuccess,
    //         ProfileEvents::GetMaskingPoliciesFailed);
    //     return outRes;
    // }

    // void Catalog::putMaskingPolicy(MaskingPolicyModel & masking_policy)
    // {
    //     runWithMetricSupport(
    //         [&] { meta_proxy->putMaskingPolicy(name_space, masking_policy); },
    //         ProfileEvents::PutMaskingPolicySuccess,
    //         ProfileEvents::PutMaskingPolicyFailed);
    // }

    // std::optional<MaskingPolicyModel> Catalog::tryGetMaskingPolicy(const String & masking_policy_name)
    // {
    //     std::optional<MaskingPolicyModel> outRes;
    //     runWithMetricSupport(
    //         [&] {
    //             String data = meta_proxy->getMaskingPolicy(name_space, masking_policy_name);
    //             if (data.empty())
    //             {
    //                 outRes = {};
    //                 return;
    //             }
    //             MaskingPolicyModel mask;
    //             mask.ParseFromString(std::move(data));
    //             outRes = mask;
    //         },
    //         ProfileEvents::TryGetMaskingPolicySuccess,
    //         ProfileEvents::TryGetMaskingPolicyFailed);
    //     return outRes;
    // }

    // MaskingPolicyModel Catalog::getMaskingPolicy(const String & masking_policy_name)
    // {
    //     MaskingPolicyModel res;
    //     runWithMetricSupport(
    //         [&] {
    //             auto mask = tryGetMaskingPolicy(masking_policy_name);
    //             if (mask)
    //             {
    //                 res = *mask;
    //                 return;
    //             }
    //             throw Exception("Masking policy not found: " + masking_policy_name, ErrorCodes::UNKNOWN_MASKING_POLICY_NAME);
    //         },
    //         ProfileEvents::GetMaskingPolicySuccess,
    //         ProfileEvents::GetMaskingPolicyFailed);
    //     return res;
    // }

    // std::vector<MaskingPolicyModel> Catalog::getAllMaskingPolicy()
    // {
    //     std::vector<MaskingPolicyModel> masks;
    //     runWithMetricSupport(
    //         [&] {
    //             Strings data = meta_proxy->getAllMaskingPolicy(name_space);

    //             masks.reserve(data.size());

    //             for (const auto & s : data)
    //             {
    //                 MaskingPolicyModel model;
    //                 model.ParseFromString(std::move(s));
    //                 masks.push_back(std::move(model));
    //             }
    //         },
    //         ProfileEvents::GetAllMaskingPolicySuccess,
    //         ProfileEvents::GetAllMaskingPolicyFailed);
    //     return masks;
    // }

    // Strings Catalog::getMaskingPolicyAppliedTables(const String & masking_policy_name)
    // {
    //     Strings res;
    //     runWithMetricSupport(
    //         [&] {
    //             auto it = meta_proxy->getMaskingPolicyAppliedTables(name_space, masking_policy_name);
    //             while (it->next())
    //                 res.push_back(std::move(it->value()));
    //         },
    //         ProfileEvents::GetMaskingPolicyAppliedTablesSuccess,
    //         ProfileEvents::GetMaskingPolicyAppliedTablesFailed);
    //     return res;
    // }

    // Strings Catalog::getAllMaskingPolicyAppliedTables()
    // {
    //     Strings res;
    //     runWithMetricSupport(
    //         [&] { res = getMaskingPolicyAppliedTables(""); },
    //         ProfileEvents::GetAllMaskingPolicyAppliedTablesSuccess,
    //         ProfileEvents::GetAllMaskingPolicyAppliedTablesFailed);
    //     return res;
    // }

    // void Catalog::dropMaskingPolicies(const Strings & masking_policy_names)
    // {
    //     runWithMetricSupport(
    //         [&] { meta_proxy->dropMaskingPolicies(name_space, masking_policy_names); },
    //         ProfileEvents::DropMaskingPoliciesSuccess,
    //         ProfileEvents::DropMaskingPoliciesFailed);
    // }

    Strings Catalog::tryGetDependency(const ASTPtr & ast)
    {
        ASTCreateQuery * create_ast = ast->as<ASTCreateQuery>();

        Strings res;
        /// get the uuid of the table on which this view depends.
        if (create_ast && create_ast->isView())
        {
            ASTs tables;
            bool has_table_func = false;
            create_ast->select->collectAllTables(tables, has_table_func);
            if (!tables.empty())
            {
                std::unordered_set<String> table_set{};
                for (size_t i = 0; i < tables.size(); i++)
                {
                    DatabaseAndTableWithAlias table(tables[i]);
                    String qualified_name = table.database + "." + table.table;
                    /// do not add a dependency twice
                    if (table_set.count(qualified_name))
                        continue;
                    table_set.insert(qualified_name);
                    String dependency_uuid = meta_proxy->getTableUUID(name_space, table.database, table.table);
                    if (!dependency_uuid.empty())
                        res.emplace_back(std::move(dependency_uuid));
                }
            }
        }
        return res;
    }

    void Catalog::replace_definition(Protos::DataModelTable & table, const String & db_name, const String & table_name)
    {
        const String & definition = table.definition();
        const char * begin = definition.data();
        const char * end = begin + definition.size();
        ParserQuery parser(end);
        ASTPtr ast = parseQuery(parser, begin, end, "", 0, 0);
        ASTCreateQuery * create_ast = ast->as<ASTCreateQuery>();
        create_ast->database = db_name;
        create_ast->table = table_name;

        table.set_definition(queryToString(ast));
        table.set_database(db_name);
        table.set_name(table_name);
    }

    StoragePtr Catalog::createTableFromDataModel(const Context & context, const Protos::DataModelTable & data_model)
    {
        StoragePtr res = CatalogFactory::getTableByDataModel(Context::createCopy(context.shared_from_this()), &data_model);

        /// set worker group for StorageCnchMergeTree if virtual warehouse is exists.
        ///FIXME: if StorageCnchMergeTree is ready
        // if (data_model.has_vw_name())
        // {
        //     if (auto * table = dynamic_cast<StorageCnchMergeTree *>(res.get()))
        //     {
        //         String vw_name = data_model.vw_name();
        //         String worker_group_name = data_model.worker_group_name();
        //         UInt64 worker_topology_hash = data_model.worker_topology_hash();

        //         HostWithPortsVec workers;

        //         try
        //         {
        //             workers = getWorkersInWorkerGroup(worker_group_name);
        //         }
        //         catch (...)
        //         {
        //             tryLogDebugCurrentException(__PRETTY_FUNCTION__);
        //         }

        //         table->setWorkerGroupInfo(vw_name, worker_group_name, worker_topology_hash);
        //     }
        // }

        return res;
    }

    void Catalog::detachOrAttachTable(const String & db, const String & name, const TxnTimestamp & ts, bool is_detach)
    {
        String table_uuid = meta_proxy->getTableUUID(name_space, db, name);
        if (table_uuid.empty())
            throw Exception("Table not found.", ErrorCodes::UNKNOWN_TABLE);

        /// get latest table version.
        auto table = tryGetTableFromMetastore(table_uuid, ts.toUInt64());

        if (table)
        {
            /// detach or attach table
            if (is_detach)
                table->set_status(Status::setDetached(table->status()));
            else
                table->set_status(Status::setAttached(table->status()));
            /// directly rewrite the old table metadata rather than adding a new version
            meta_proxy->updateTable(name_space, table_uuid, table->SerializeAsString(), table->commit_time());
            if (auto storage_cache = context.getCnchStorageCache())
            {
                storage_cache->remove(table->database(), table->name());
            }
        }
        else
        {
            throw Exception("Cannot get table metadata for UUID : " + table_uuid, ErrorCodes::UNKNOWN_TABLE);
        }
    }

    void Catalog::detachOrAttachDictionary(const String & database, const String & name, bool is_detach)
    {
        String dic_meta;
        meta_proxy->getDictionary(name_space, database, name, dic_meta);

        if (dic_meta.empty())
            throw Exception("Dictionary " + database + "." + name + " doesn't  exists.", ErrorCodes::DICTIONARY_NOT_EXIST);

        Protos::DataModelDictionary dic_model;
        dic_model.ParseFromString(dic_meta);

        if (is_detach)
            dic_model.set_status(Status::setDetached(dic_model.status()));
        else
            dic_model.set_status(Status::setAttached(dic_model.status()));

        dic_model.set_last_modification_time(Poco::Timestamp().raw());

        meta_proxy->createDictionary(name_space, database, name, dic_model.SerializeAsString());
    }

    DataModelPartPtrVector Catalog::getDataPartsMetaFromMetastore(
        const ConstStoragePtr & storage, const Strings & required_partitions, const Strings & full_partitions, const TxnTimestamp & ts)
    {
        auto createDataModelPartPtr = [&](const String & meta) {
            Protos::DataModelPart part_model;
            part_model.ParseFromString(meta);
            std::shared_ptr<Protos::DataModelPart> res_ptr;
            if (ts.toUInt64() && part_model.has_commit_time() && TxnTimestamp{part_model.commit_time()} > ts)
                return res_ptr;
            // compatible with old parts from alpha, old part doesn't have commit time field, the mutation is its commit time
            else if (ts.toUInt64() && !part_model.has_commit_time() && UInt64(part_model.part_info().mutation()) > ts)
                return res_ptr;
            return createPtrFromModel(std::move(part_model));
        };

        UInt32 time_out_ms = 1000 * (context.getSettingsRef().cnch_fetch_parts_timeout.totalSeconds());

        return getDataModelsByPartitions<DataModelPartPtr>(
            storage,
            MetastoreProxy::dataPartPrefix(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid)),
            required_partitions,
            full_partitions,
            createDataModelPartPtr,
            ts,
            time_out_ms);
    }

    DeleteBitmapMetaPtrVector Catalog::getDeleteBitmapByKeys(const StoragePtr & storage, const NameSet & keys)
    {
        DeleteBitmapMetaPtrVector outRes;
        runWithMetricSupport(
            [&] {
                if (keys.empty())
                {
                    outRes = {};
                    return;
                }
                Strings full_keys;
                for (const auto & key : keys)
                {
                    String full_key
                        = MetastoreProxy::deleteBitmapPrefix(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid)) + key;
                    full_keys.emplace_back(full_key);
                }

                DeleteBitmapMetaPtrVector res;
                auto metas = meta_proxy->getDeleteBitmapByKeys(full_keys);
                for (const auto & meta : metas)
                {
                    // NOTE: If there are no data for a key, we simply ignores it here.
                    if (!meta.empty())
                    {
                        DataModelDeleteBitmapPtr model_ptr = std::make_shared<Protos::DataModelDeleteBitmap>();
                        model_ptr->ParseFromString(meta);
                        auto & merge_tree_storage = dynamic_cast<MergeTreeMetaBase &>(*storage);
                        res.push_back(std::make_shared<DeleteBitmapMeta>(merge_tree_storage, model_ptr));
                    }
                }
                outRes = res;
            },
            ProfileEvents::GetDeleteBitmapByKeysSuccess,
            ProfileEvents::GetDeleteBitmapByKeysFailed);
        return outRes;
    }

    DeleteBitmapMetaPtrVector
    Catalog::getDeleteBitmapsInPartitions(const ConstStoragePtr & storage, const Strings & partitions, const TxnTimestamp & ts)
    {
        DeleteBitmapMetaPtrVector outRes;
        runWithMetricSupport(
            [&] {
                const auto & merge_tree_storage = dynamic_cast<const MergeTreeMetaBase &>(*storage);

                auto createDeleteBitmapMetaPtr = [&](const String & meta) {
                    DataModelDeleteBitmapPtr model_ptr = std::make_shared<Protos::DataModelDeleteBitmap>();
                    model_ptr->ParseFromString(meta);
                    std::shared_ptr<DeleteBitmapMeta> res_ptr;
                    if (ts.toUInt64() && model_ptr->has_commit_time() && TxnTimestamp{model_ptr->commit_time()} > ts)
                    {
                        return res_ptr;
                    }
                    return std::make_shared<DeleteBitmapMeta>(merge_tree_storage, model_ptr);
                };

                Strings all_partitions = getPartitionIDsFromMetastore(storage);
                auto all_bitmaps = getDataModelsByPartitions<DeleteBitmapMetaPtr>(
                    storage,
                    MetastoreProxy::deleteBitmapPrefix(name_space, UUIDHelpers::UUIDToString(storage->getStorageID().uuid)),
                    partitions,
                    all_partitions,
                    createDeleteBitmapMetaPtr,
                    ts);
                // NOTE: the below logic does is to filter out uncommited bitmaps at this moment.
                getCommittedBitmaps(all_bitmaps, ts, this);
                outRes = all_bitmaps;
            },
            ProfileEvents::GetDeleteBitmapsInPartitionsSuccess,
            ProfileEvents::GetDeleteBitmapsInPartitionsFailed);
        return outRes;
    }

    void Catalog::removeDeleteBitmaps(const StoragePtr & storage, const DeleteBitmapMetaPtrVector & bitmaps)
    {
        runWithMetricSupport(
            [&] {
                clearParts(storage, CommitItems{{}, bitmaps, {}}, /*skip_part_cache*/ true);
            },
            ProfileEvents::RemoveDeleteBitmapsSuccess,
            ProfileEvents::RemoveDeleteBitmapsFailed);
    }

    void Catalog::getKafkaOffsets(const String & consumer_group, cppkafka::TopicPartitionList & tpl)
    {
        runWithMetricSupport(
            [&] { meta_proxy->getKafkaTpl(name_space, consumer_group, tpl); },
            ProfileEvents::GetKafkaOffsetsVoidSuccess,
            ProfileEvents::GetKafkaOffsetsVoidFailed);
    }

    cppkafka::TopicPartitionList Catalog::getKafkaOffsets(const String & consumer_group, const String & kafka_topic)
    {
        cppkafka::TopicPartitionList res;

        runWithMetricSupport(
            [&] { res = meta_proxy->getKafkaTpl(name_space, consumer_group, kafka_topic); },
            ProfileEvents::GetKafkaOffsetsTopicPartitionListSuccess,
            ProfileEvents::GetKafkaOffsetsTopicPartitionListFailed);
        return res;
    }

    void Catalog::clearOffsetsForWholeTopic(const String & topic, const String & consumer_group)
    {
        runWithMetricSupport(
            [&] { meta_proxy->clearOffsetsForWholeTopic(name_space, topic, consumer_group); },
            ProfileEvents::ClearOffsetsForWholeTopicSuccess,
            ProfileEvents::ClearOffsetsForWholeTopicFailed);
    }

    std::optional<DB::Protos::DataModelTable> Catalog::getTableByID(const Protos::TableIdentifier & identifier)
    {
        std::optional<DB::Protos::DataModelTable> outRes;
        runWithMetricSupport(
            [&] {
                const String & database_name = identifier.database();
                const String & table_name = identifier.name();
                const String & uuid = identifier.uuid();

                Strings tables_meta;
                meta_proxy->getTableByUUID(name_space, uuid, tables_meta);

                if (!tables_meta.empty())
                {
                    DB::Protos::DataModelTable table_data;
                    table_data.ParseFromString(tables_meta.back());
                    replace_definition(table_data, database_name, table_name);
                    auto res = std::make_optional<DB::Protos::DataModelTable>(std::move(table_data));
                    outRes = res;
                    return;
                }
                outRes = {};
            },
            ProfileEvents::GetTableByIDSuccess,
            ProfileEvents::GetTableByIDFailed);
        return outRes;
    }

    Catalog::DataModelTables Catalog::getTablesByID(std::vector<std::shared_ptr<Protos::TableIdentifier>> & identifiers)
    {
        DataModelTables res;
        runWithMetricSupport(
            [&] {
                for (auto & identifier : identifiers)
                {
                    const String & database_name = identifier->database();
                    const String & table_name = identifier->name();
                    const String & uuid = identifier->uuid();

                    Strings tables_meta;
                    meta_proxy->getTableByUUID(name_space, uuid, tables_meta);

                    if (!tables_meta.empty())
                    {
                        DB::Protos::DataModelTable table_data;
                        table_data.ParseFromString(tables_meta.back());
                        replace_definition(table_data, database_name, table_name);
                        res.push_back(table_data);
                    }
                }
            },
            ProfileEvents::GetTablesByIDSuccess,
            ProfileEvents::GetTablesByIDFailed);
        return res;
    }

    void Catalog::moveTableIntoTrash(
        Protos::DataModelTable & table,
        Protos::TableIdentifier & table_id,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts,
        BatchCommitRequest & batch_write)
    {
        if (table.commit_time() >= ts.toUInt64())
            throw Exception("Cannot drop table with an old timestamp.", ErrorCodes::CATALOG_SERVICE_INTERNAL_ERROR);

        table.clear_definitions();
        table.set_txnid(txnID.toUInt64());
        table.set_commit_time(ts.toUInt64());
        /// mark table as deleted
        table.set_status(Status::setDelete(table.status()));

        Strings dependencies = tryGetDependency(parseCreateQuery(table.definition()));

        /// remove dependency
        for (const String & dependency : dependencies)
            batch_write.AddDelete(SingleDeleteRequest(MetastoreProxy::viewDependencyKey(name_space, dependency, table_id.uuid())));

        batch_write.AddPut(SinglePutRequest(MetastoreProxy::tableStoreKey(name_space, table_id.uuid(), ts.toUInt64()), table.SerializeAsString()));
        // use database name and table name in table_id is required because it may different with that in table data model.
        batch_write.AddPut(SinglePutRequest(
            MetastoreProxy::tableTrashKey(name_space, table_id.database(), table_id.name(), ts.toUInt64()), table_id.SerializeAsString()));
        batch_write.AddDelete(SingleDeleteRequest(MetastoreProxy::tableUUIDMappingKey(name_space, table.database(), table.name())));
    }

    void Catalog::restoreTableFromTrash(
        std::shared_ptr<Protos::TableIdentifier> table_id, const UInt64 & ts, BatchCommitRequest & batch_write)
    {
        auto table_model = tryGetTableFromMetastore(table_id->uuid(), UINT64_MAX, false, true);

        if (!table_model)
            throw Exception("Cannot found table meta with uuid " + table_id->uuid() + ".", ErrorCodes::UNKNOWN_TABLE);

        /// 1. remove trash record;
        /// 2.add table->uuid mapping;
        /// 3. remove last version of table meta(which is marked as delete);
        /// 4. try rebuild dependencies if any
        batch_write.AddDelete(SingleDeleteRequest(MetastoreProxy::tableTrashKey(name_space, table_id->database(), table_id->name(), ts)));
        batch_write.AddDelete(SingleDeleteRequest(MetastoreProxy::tableStoreKey(name_space, table_id->uuid(), table_model->commit_time())));
        batch_write.AddPut(SinglePutRequest(
            MetastoreProxy::tableUUIDMappingKey(name_space, table_id->database(), table_id->name()),
            table_id->SerializeAsString(), true));
        Strings dependencies = tryGetDependency(parseCreateQuery(table_model->definition()));
        if (!dependencies.empty())
        {
            for (const String & dependency : dependencies)
                batch_write.AddPut(SinglePutRequest(MetastoreProxy::viewDependencyKey(name_space, dependency, table_id->uuid()), table_id->uuid()));
        }
    }

    void Catalog::createVirtualWarehouse(const String & vw_name, const VirtualWarehouseData & data)
    {
        runWithMetricSupport(
            [&] { meta_proxy->createVirtualWarehouse(name_space, vw_name, data); },
            ProfileEvents::CreateVirtualWarehouseSuccess,
            ProfileEvents::CreateVirtualWarehouseFailed);
    }

    void Catalog::alterVirtualWarehouse(const String & vw_name, const VirtualWarehouseData & data)
    {
        runWithMetricSupport(
            [&] { meta_proxy->alterVirtualWarehouse(name_space, vw_name, data); },
            ProfileEvents::AlterVirtualWarehouseSuccess,
            ProfileEvents::AlterVirtualWarehouseFailed);
    }

    bool Catalog::tryGetVirtualWarehouse(const String & vw_name, VirtualWarehouseData & data)
    {
        bool res;
        runWithMetricSupport(
            [&] { res = meta_proxy->tryGetVirtualWarehouse(name_space, vw_name, data); },
            ProfileEvents::TryGetVirtualWarehouseSuccess,
            ProfileEvents::TryGetVirtualWarehouseFailed);
        return res;
    }

    std::vector<VirtualWarehouseData> Catalog::scanVirtualWarehouses()
    {
        std::vector<VirtualWarehouseData> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->scanVirtualWarehouses(name_space); },
            ProfileEvents::TryGetVirtualWarehouseSuccess,
            ProfileEvents::ScanVirtualWarehousesFailed);
        return res;
    }

    void Catalog::dropVirtualWarehouse(const String & vm_name)
    {
        runWithMetricSupport(
            [&] { meta_proxy->dropVirtualWarehouse(name_space, vm_name); },
            ProfileEvents::DropVirtualWarehouseSuccess,
            ProfileEvents::DropVirtualWarehouseFailed);
    }

    void Catalog::createWorkerGroup(const String & worker_group_id, const WorkerGroupData & data)
    {
        runWithMetricSupport(
            [&] { meta_proxy->createWorkerGroup(name_space, worker_group_id, data); },
            ProfileEvents::CreateWorkerGroupSuccess,
            ProfileEvents::CreateWorkerGroupFailed);
    }

    void Catalog::updateWorkerGroup(const String & worker_group_id, const WorkerGroupData & data)
    {
        runWithMetricSupport(
            [&] { meta_proxy->updateWorkerGroup(name_space, worker_group_id, data); },
            ProfileEvents::UpdateWorkerGroupSuccess,
            ProfileEvents::UpdateWorkerGroupFailed);
    }

    bool Catalog::tryGetWorkerGroup(const String & worker_group_id, WorkerGroupData & data)
    {
        bool res;
        runWithMetricSupport(
            [&] { res = meta_proxy->tryGetWorkerGroup(name_space, worker_group_id, data); },
            ProfileEvents::TryGetWorkerGroupSuccess,
            ProfileEvents::TryGetWorkerGroupFailed);
        return res;
    }

    std::vector<WorkerGroupData> Catalog::scanWorkerGroups()
    {
        std::vector<WorkerGroupData> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->scanWorkerGroups(name_space); },
            ProfileEvents::ScanWorkerGroupsSuccess,
            ProfileEvents::ScanWorkerGroupsFailed);
        return res;
    }

    void Catalog::dropWorkerGroup(const String & worker_group_id)
    {
        runWithMetricSupport(
            [&] { meta_proxy->dropWorkerGroup(name_space, worker_group_id); },
            ProfileEvents::DropWorkerGroupSuccess,
            ProfileEvents::DropWorkerGroupFailed);
    }

    String Catalog::getInsertionLabelKey(const InsertionLabelPtr & label)
    {
        String res;
        runWithMetricSupport(
            [&] { res = MetastoreProxy::insertionLabelKey(name_space, toString(label->table_uuid), label->name); },
            ProfileEvents::GetInsertionLabelKeySuccess,
            ProfileEvents::GetInsertionLabelKeyFailed);
        return res;
    }

    void Catalog::precommitInsertionLabel(const InsertionLabelPtr & label)
    {
        runWithMetricSupport(
            [&] { meta_proxy->precommitInsertionLabel(name_space, label); },
            ProfileEvents::PrecommitInsertionLabelSuccess,
            ProfileEvents::PrecommitInsertionLabelFailed);
    }

    void Catalog::commitInsertionLabel(InsertionLabelPtr & label)
    {
        runWithMetricSupport(
            [&] { meta_proxy->commitInsertionLabel(name_space, label); },
            ProfileEvents::CommitInsertionLabelSuccess,
            ProfileEvents::CommitInsertionLabelFailed);
    }

    void Catalog::tryCommitInsertionLabel(InsertionLabelPtr & label)
    {
        runWithMetricSupport(
            [&] {
                auto label_in_kv = getInsertionLabel(label->table_uuid, label->name);
                if (!label_in_kv)
                    throw Exception("Label " + label->name + " not found", ErrorCodes::LOGICAL_ERROR);

                if (label_in_kv->status == InsertionLabel::Committed)
                {
                    return;
                }
                if (label->serializeValue() != label->serializeValue())
                    throw Exception("Label " + label->name + " has been changed", ErrorCodes::LOGICAL_ERROR);

                commitInsertionLabel(label);
            },
            ProfileEvents::TryCommitInsertionLabelSuccess,
            ProfileEvents::TryCommitInsertionLabelFailed);
    }

    bool Catalog::abortInsertionLabel(const InsertionLabelPtr & label, String & message)
    {
        bool res;
        runWithMetricSupport(
            [&] {
                auto [version, label_in_kv] = meta_proxy->getInsertionLabel(name_space, toString(label->table_uuid), label->name);
                if (label_in_kv.empty())
                {
                    message = "Label " + label->name + " not found.";
                    res = false;
                    return;
                }

                if (label_in_kv != label->serializeValue())
                {
                    message = "Label " + label->name + " has been changed.";
                    res = false;
                    return;
                }

                meta_proxy->removeInsertionLabel(name_space, toString(label->table_uuid), label->name, version);
                res = false;
            },
            ProfileEvents::AbortInsertionLabelSuccess,
            ProfileEvents::AbortInsertionLabelFailed);
        return res;
    }

    InsertionLabelPtr Catalog::getInsertionLabel(UUID uuid, const String & name)
    {
        InsertionLabelPtr res;
        runWithMetricSupport(
            [&] {
                auto [_, value] = meta_proxy->getInsertionLabel(name_space, toString(uuid), name);
                if (value.empty())
                {
                    res = nullptr;
                    return;
                }


                auto label = std::make_shared<InsertionLabel>(uuid, name);
                label->parseValue(value);
                res = label;
            },
            ProfileEvents::GetInsertionLabelSuccess,
            ProfileEvents::GetInsertionLabelFailed);
        return res;
    }

    void Catalog::removeInsertionLabel(UUID uuid, const String & name)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeInsertionLabel(name_space, toString(uuid), name); },
            ProfileEvents::RemoveInsertionLabelSuccess,
            ProfileEvents::RemoveInsertionLabelFailed);
    }

    void Catalog::removeInsertionLabels(const std::vector<InsertionLabel> & labels)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeInsertionLabels(name_space, labels); },
            ProfileEvents::RemoveInsertionLabelsSuccess,
            ProfileEvents::RemoveInsertionLabelsFailed);
    }

    std::vector<InsertionLabel> Catalog::scanInsertionLabels(UUID uuid)
    {
        std::vector<InsertionLabel> res;
        runWithMetricSupport(
            [&] {
                auto it = meta_proxy->scanInsertionLabels(name_space, toString(uuid));
                while (it->next())
                {
                    auto && key = it->key();
                    auto pos = key.rfind('_');
                    if (pos == std::string::npos)
                        continue;

                    res.emplace_back();
                    res.back().name = key.substr(pos + 1);
                    res.back().table_uuid = uuid;
                    res.back().parseValue(it->value());
                }
            },
            ProfileEvents::ScanInsertionLabelsFailed,
            ProfileEvents::ScanInsertionLabelsSuccess);
        return res;
    }

    void Catalog::clearInsertionLabels(UUID uuid)
    {
        runWithMetricSupport(
            [&] { meta_proxy->clearInsertionLabels(name_space, toString(uuid)); },
            ProfileEvents::ClearInsertionLabelsSuccess,
            ProfileEvents::ClearInsertionLabelsFailed);
    }

    void Catalog::updateTableStatistics(const String & uuid, const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data)
    {
        runWithMetricSupport(
            [&] { meta_proxy->updateTableStatistics(name_space, uuid, data); },
            ProfileEvents::UpdateTableStatisticsSuccess,
            ProfileEvents::UpdateTableStatisticsFailed);
    }

    std::unordered_map<StatisticsTag, StatisticsBasePtr>
    Catalog::getTableStatistics(const String & uuid, const std::unordered_set<StatisticsTag> & tags)
    {
        std::unordered_map<StatisticsTag, StatisticsBasePtr> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getTableStatistics(name_space, uuid, tags); },
            ProfileEvents::GetTableStatisticsSuccess,
            ProfileEvents::GetTableStatisticsFailed);
        return res;
    }

    std::unordered_set<StatisticsTag> Catalog::getAvailableTableStatisticsTags(const String & uuid)
    {
        std::unordered_set<StatisticsTag> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAvailableTableStatisticsTags(name_space, uuid); },
            ProfileEvents::GetAvailableTableStatisticsTagsSuccess,
            ProfileEvents::GetAvailableTableStatisticsTagsFailed);
        return res;
    }

    void Catalog::removeTableStatistics(const String & uuid, const std::unordered_set<StatisticsTag> & tags)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeTableStatistics(name_space, uuid, tags); },
            ProfileEvents::RemoveTableStatisticsSuccess,
            ProfileEvents::RemoveTableStatisticsFailed);
    }

    void Catalog::updateColumnStatistics(
        const String & uuid, const String & column, const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data)
    {
        runWithMetricSupport(
            [&] { meta_proxy->updateColumnStatistics(name_space, uuid, column, data); },
            ProfileEvents::UpdateColumnStatisticsSuccess,
            ProfileEvents::UpdateColumnStatisticsFailed);
    }

    std::unordered_map<StatisticsTag, StatisticsBasePtr>
    Catalog::getColumnStatistics(const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags)
    {
        std::unordered_map<StatisticsTag, StatisticsBasePtr> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getColumnStatistics(name_space, uuid, column, tags); },
            ProfileEvents::GetColumnStatisticsSuccess,
            ProfileEvents::GetColumnStatisticsFailed);
        return res;
    }

    std::unordered_set<StatisticsTag> Catalog::getAvailableColumnStatisticsTags(const String & uuid, const String & column)
    {
        std::unordered_set<StatisticsTag> res;
        runWithMetricSupport(
            [&] { res = meta_proxy->getAvailableColumnStatisticsTags(name_space, uuid, column); },
            ProfileEvents::GetAvailableColumnStatisticsTagsSuccess,
            ProfileEvents::GetAvailableColumnStatisticsTagsFailed);
        return res;
    }

    void Catalog::removeColumnStatistics(const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags)
    {
        runWithMetricSupport(
            [&] { meta_proxy->removeColumnStatistics(name_space, uuid, column, tags); },
            ProfileEvents::RemoveColumnStatisticsSuccess,
            ProfileEvents::RemoveColumnStatisticsFailed);
    }

    void Catalog::setMergeMutateThreadStartTime(const StorageID & storage_id, const UInt64 & startup_time) const
    {
        meta_proxy->setMergeMutateThreadStartTime(name_space, UUIDHelpers::UUIDToString(storage_id.uuid), startup_time);
    }

    UInt64 Catalog::getMergeMutateThreadStartTime(const StorageID & storage_id) const
    {
        return meta_proxy->getMergeMutateThreadStartTime(name_space, UUIDHelpers::UUIDToString(storage_id.uuid));
    }

    void fillUUIDForDictionary(DB::Protos::DataModelDictionary & d)
    {
        UUID final_uuid = UUIDHelpers::Nil;
        UUID uuid = RPCHelpers::createUUID(d.uuid());
        ASTPtr ast = CatalogFactory::getCreateDictionaryByDataModel(d);
        ASTCreateQuery * create_ast = ast->as<ASTCreateQuery>();
        UUID uuid_in_create_query = create_ast->uuid;
        if (uuid != UUIDHelpers::Nil)
            final_uuid = uuid;

        if (final_uuid == UUIDHelpers::Nil)
            final_uuid = uuid_in_create_query;

        if (final_uuid == UUIDHelpers::Nil)
            final_uuid = UUIDHelpers::generateV4();

        RPCHelpers::fillUUID(final_uuid, *(d.mutable_uuid()));
        create_ast->uuid = final_uuid;
        String create_query = serializeAST(*ast);
        d.set_definition(create_query);
    }
}
}
