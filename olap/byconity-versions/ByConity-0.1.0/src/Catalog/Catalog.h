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

#include <map>
#include <set>
#include <Catalog/CatalogConfig.h>
#include <Catalog/CatalogUtils.h>
#include <Catalog/MetastoreProxy.h>
#include <Catalog/DataModelPartWrapper.h>
#include <Core/Types.h>
#include <CloudServices/Checkpoint.h>
#include <Protos/DataModelHelpers.h>
#include <Protos/cnch_server_rpc.pb.h>
#include <Statistics/StatisticsBase.h>
#include <Statistics/ExportSymbols.h>
// #include <Transaction/ICnchTransaction.h>
#include <Transaction/TxnTimestamp.h>
#include <cppkafka/cppkafka.h>
#include <Common/DNSResolver.h>
#include <Common/HostWithPorts.h>
#include <common/getFQDNOrHostName.h>
#include <Transaction/TransactionCommon.h>
#include <ResourceManagement/CommonData.h>
// #include <Access/MaskingPolicyDataModel.h>

namespace DB::ErrorCodes
{
    extern const int TIMEOUT_EXCEEDED;
} // namespace DB::ErrorCodes

namespace DB::Catalog
{
class Catalog
{
public:
    using DatabasePtr = std::shared_ptr<DB::IDatabase>;
    using DataModelTables = std::vector<Protos::DataModelTable>;
    using DataModelDBs = std::vector<Protos::DataModelDB>;
    using DataModelWorkerGroups = std::vector<Protos::DataModelWorkerGroup>;
    using DataModelDictionaries = std::vector<Protos::DataModelDictionary>;
    // using DataModelMaskingPolicies = std::vector<Protos::DataModelMaskingPolicy>;
    using DataModelUDFs = std::vector<Protos::DataModelUDF>;

    using MetastoreProxyPtr = std::shared_ptr<MetastoreProxy>;

    Catalog(Context & _context, CatalogConfig & config, String _name_space = "default");

    ~Catalog() = default;

    /// update optimizer stats
    void updateTableStatistics(const String & uuid, const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data);

    /// get the latest stats
    /// if tag not exists, don't put it into the map
    std::unordered_map<StatisticsTag, StatisticsBasePtr>
    getTableStatistics(const String & uuid, const std::unordered_set<StatisticsTag> & tags);
    /// get all tags
    std::unordered_set<StatisticsTag> getAvailableTableStatisticsTags(const String & uuid);

    /// remove tags
    void removeTableStatistics(const String & uuid, const std::unordered_set<StatisticsTag> & tags);

    /// stats for Column

    /// update optimizer stats
    void updateColumnStatistics(
        const String & uuid, const String & column, const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data);

    /// get the latest stats
    /// if tag not exists, don't put it into the map
    std::unordered_map<StatisticsTag, StatisticsBasePtr>
    getColumnStatistics(const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags);
    /// get all tags
    std::unordered_set<StatisticsTag> getAvailableColumnStatisticsTags(const String & uuid, const String & column);
    /// remove tags
    void removeColumnStatistics(const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags);
    //////////////

    ///database related interface
    void createDatabase(const String & database, const UUID & uuid, const TxnTimestamp & txnID, const TxnTimestamp & ts);

    DatabasePtr getDatabase(const String & database, const ContextPtr & context, const TxnTimestamp & ts = 0);

    bool isDatabaseExists(const String & database, const TxnTimestamp & ts = 0);

    void dropDatabase(const String & database, const TxnTimestamp & previous_version, const TxnTimestamp & txnID, const TxnTimestamp & ts);

    void renameDatabase(const String & from_database, const String & to_database, const TxnTimestamp & txnID, const TxnTimestamp & ts);

    ///table related interface
    void createTable(
        const StorageID & storage_id,
        const String & create_query,
        const String & virtual_warehouse,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts);

    void dropTable(
        const StoragePtr & storage, const TxnTimestamp & previous_version, const TxnTimestamp & txnID, const TxnTimestamp & ts);

    void createUDF(const String & db, const String & name, const String & create_query);

    void dropUDF(const String & db, const String & name);

    void detachTable(const String & db, const String & name, const TxnTimestamp & ts);

    void attachTable(const String & db, const String & name, const TxnTimestamp & ts);

    bool isTableExists(const String & db, const String & name, const TxnTimestamp & ts = 0);

    void alterTable(
        const StoragePtr & storage,
        const String & new_create,
        const TxnTimestamp & previous_version,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts);


    void renameTable(
        const String & from_database,
        const String & from_table,
        const String & to_database,
        const String & to_table,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts);

    void setWorkerGroupForTable(const String & db, const String & name, const String & worker_group, UInt64 worker_topology_hash);

    StoragePtr getTable(const Context & query_context, const String & database, const String & name, const TxnTimestamp & ts = TxnTimestamp::maxTS());

    StoragePtr tryGetTable(const Context & query_context, const String & database, const String & name, const TxnTimestamp & ts = TxnTimestamp::maxTS());

    StoragePtr tryGetTableByUUID(const Context & query_context, const String & uuid, const TxnTimestamp & ts, bool with_delete = false);

    StoragePtr getTableByUUID(const Context & query_context, const String & uuid, const TxnTimestamp & ts, bool with_delete = false);

    Strings getTablesInDB(const String & database);

    std::vector<StoragePtr> getAllViewsOn(const Context & session_context, const StoragePtr & storage, const TxnTimestamp & ts);

    void setTableActiveness(const StoragePtr & storage, const bool is_active, const TxnTimestamp & ts);
    /// return true if table is active, false otherwise
    bool getTableActiveness(const StoragePtr & storage, const TxnTimestamp & ts);

    ///data parts related interface
    ServerDataPartsVector getServerDataPartsInPartitions(const StoragePtr & storage, const Strings & partitions, const TxnTimestamp & ts, const Context * session_context);

    ServerDataPartsVector getAllServerDataParts(const StoragePtr & storage, const TxnTimestamp & ts, const Context * session_context);
    DataPartsVector getDataPartsByNames(const NameSet & names, const StoragePtr & table, const TxnTimestamp & ts);
    DataPartsVector getStagedDataPartsByNames(const NameSet & names, const StoragePtr & table, const TxnTimestamp & ts);
    DeleteBitmapMetaPtrVector getAllDeleteBitmaps(const StoragePtr & table, const TxnTimestamp & ts);

    // return table's committed staged parts. if partitions != null, ignore staged parts not belong to `partitions`.
    DataPartsVector getStagedParts(const StoragePtr & table, const TxnTimestamp & ts, const NameSet * partitions = nullptr);

    /// (UNIQUE KEY) fetch all delete bitmaps <= ts in the given partitions
    DeleteBitmapMetaPtrVector
    getDeleteBitmapsInPartitions(const StoragePtr & storage, const Strings & partitions, const TxnTimestamp & ts = 0);
    /// (UNIQUE KEY) get bitmaps by keys
    DeleteBitmapMetaPtrVector getDeleteBitmapByKeys(const StoragePtr & storage, const NameSet & keys);
    /// (UNIQUE KEY) remove bitmaps meta from KV, used by GC
    void removeDeleteBitmaps(const StoragePtr & storage, const DeleteBitmapMetaPtrVector & bitmaps);

    // V1 part commit API
    void finishCommit(
        const StoragePtr & table,
        const TxnTimestamp & txnID,
        const TxnTimestamp & commit_ts,
        const DataPartsVector & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps = {},
        const bool is_merged_parts = false,
        const bool preallocate_mode = false);

    void getKafkaOffsets(const String & consumer_group, cppkafka::TopicPartitionList & tpl);
    cppkafka::TopicPartitionList getKafkaOffsets(const String & consumer_group, const String & kafka_topic);
    void clearOffsetsForWholeTopic(const String & topic, const String & consumer_group);

    void dropAllPart(const StoragePtr & storage, const TxnTimestamp & txnID, const TxnTimestamp & ts);

    std::vector<std::shared_ptr<MergeTreePartition>> getPartitionList(const StoragePtr & table, const Context * session_context);

    void getPartitionsFromMetastore(const MergeTreeMetaBase & table, PartitionMap & partition_list);

    Strings getPartitionIDs(const StoragePtr & storage, const Context * session_context);

    /// dictionary related APIs

    void createDictionary(const StorageID & storage_id, const String & create_query);

    ASTPtr getCreateDictionary(const String & database, const String & name);

    void dropDictionary(const String & database, const String & name);

    void detachDictionary(const String & database, const String & name);

    void attachDictionary(const String & database, const String & name);

    /// for backward compatible, the old dictionary doesn't have UUID
    void fixDictionary(const String & database, const String & name);

    Strings getDictionariesInDB(const String & database);

    Protos::DataModelDictionary getDictionary(const String & database, const String & name);

    StoragePtr tryGetDictionary(const String & database, const String & name, ContextPtr context);

    bool isDictionaryExists(const String & db, const String & name);

    /// API for transaction model
    void createTransactionRecord(const TransactionRecord & record);

    void removeTransactionRecord(const TransactionRecord & record);
    void removeTransactionRecords(const std::vector<TxnTimestamp> & txn_ids);

    TransactionRecord getTransactionRecord(const TxnTimestamp & txnID);
    std::optional<TransactionRecord> tryGetTransactionRecord(const TxnTimestamp & txnID);

    /// CAS operation
    /// If success return true.
    /// If fail return false and put current kv value in `target_record`
    bool setTransactionRecord(const TransactionRecord & expected_record, TransactionRecord & record);

    /// CAS operation
    /// Similiar to setTransactionRecord, but we want to pack addition requests (e.g. create a insertion label)
    bool setTransactionRecordWithRequests(
        const TransactionRecord & expected_record, TransactionRecord & record, BatchCommitRequest & request, BatchCommitResponse & response);

    void setTransactionRecordCleanTime(TransactionRecord record, const TxnTimestamp & ts, UInt64 ttl);

    bool setTransactionRecordStatusWithOffsets(
        const TransactionRecord & expected_record,
        TransactionRecord & record,
        const String & consumer_group,
        const cppkafka::TopicPartitionList & tpl);

    /// just set transaction status to aborted
    void rollbackTransaction(TransactionRecord record);

    /// add lock implementation for Pessimistic mode.
    bool writeIntents(
        const String & intent_prefix,
        const std::vector<WriteIntent> & intents,
        std::map<std::pair<TxnTimestamp, String>, std::vector<String>> & conflictIntents);

    /// used in the following 2 cases.
    /// 1. when meeting with some intents belongs to some zombie transaction.
    /// 2. try to preempt some intents belongs to low-priority running transaction.
    bool tryResetIntents(
        const String & intent_prefix,
        const std::map<std::pair<TxnTimestamp, String>, std::vector<String>> & intentsToReset,
        const TxnTimestamp & newTxnID,
        const String & newLocation);

    bool tryResetIntents(
        const String & intent_prefix,
        const std::vector<WriteIntent> & oldIntents,
        const TxnTimestamp & newTxnID,
        const String & newLocation);

    /// used when current transaction is committed or aborted,
    /// to clear intents in batch.
    void clearIntents(const String & intent_prefix, const std::vector<WriteIntent> & intents);

    /// V2 part commit API.
    /// If the commit time of parts and delete_bitmaps not set, they are invisible unless txn_reocrd is committed.
    void writeParts(
        const StoragePtr & table,
        const TxnTimestamp & txnID,
        const CommitItems & commit_data,
        const bool is_merged_parts = false,
        const bool preallocate_mode = false);

    /// set commit time for parts and delete bitmaps
    void setCommitTime(
        const StoragePtr & table,
        const CommitItems & commit_data,
        const TxnTimestamp & commitTs,
        const UInt64 txn_id = 0);

    void clearParts(
        const StoragePtr & table,
        const CommitItems & commit_data,
        const bool skip_part_cache = false);

    /// write undo buffer before write vfs
    void writeUndoBuffer(const String & uuid, const TxnTimestamp & txnID, const UndoResources & resources);
    void writeUndoBuffer(const String & uuid, const TxnTimestamp & txnID, UndoResources && resources);

    /// clear undo buffer
    void clearUndoBuffer(const TxnTimestamp & txnID);

    /// return storage uuid -> undo resources
    std::unordered_map<String, UndoResources> getUndoBuffer(const TxnTimestamp & txnID);

    /// return txn_id -> undo resources
    std::unordered_map<UInt64, UndoResources> getAllUndoBuffer();

    /// get transaction records, if the records exists, we can check with the transaction coordinator to detect zombie record.
    /// the transaction record will be cleared only after all intents have been cleared and set commit time for all parts.
    /// For zombie record, the intents to be clear can be scanned from intents space with txnid. The parts can be get from undo buffer.
    std::vector<TransactionRecord> getTransactionRecords();
    std::vector<TransactionRecord> getTransactionRecords(const std::vector<TxnTimestamp> & txn_ids, size_t batch_size = 0);
    /// clean zombie records. If the total transaction record number is too large, it may be impossible to get all of them. We can
    /// pass a max_result_number to only get part of them and clean zombie records repeatedlly
    std::vector<TransactionRecord> getTransactionRecordsForGC(size_t max_result_number);

    /// Clear intents written by zombie transaction.
    void clearZombieIntent(const TxnTimestamp & txnID);

    /// Below method provides method to locks a directory during `ALTER ATTACH PARTS FROM ... ` query. If there's a lock record
    /// that mean there's some manipulation are being done on that directory by cnch, so user should not use it in other query
    /// (that will eventualy fail). **NOTES: it DOES NOT belong to general cnch lock system, don't confuse.

    /// write a directory lock
    bool writeFilesysLock(TxnTimestamp txn_id, const String & dir, const String & db, const String & table);
    /// check if a directory is lock
    std::optional<FilesysLock> getFilesysLock(const String & dir);
    /// clean a directory lock
    void clearFilesysLock(const String & dir);
    /// clean a directory lock by transaction id
    void clearFilesysLock(TxnTimestamp txn_id);
    /// get all filesys lock record
    std::vector<FilesysLock> getAllFilesysLock();

    /// For discussion: as parts can be intents, need some extra transaction record check logic. Return all parts and do in server side?
    /// DataPartsVector getDataPartsInPartitions(const StoragePtr & storage, const Strings & partitions, const TxnTimestamp & ts = 0);
    ///DataPartsVector getAllDataParts(const StoragePtr & table, const TxnTimestamp & ts = 0);

    /// End of API for new transaction model (sync with guanzhe)

    void insertTransaction(TxnTimestamp & txnID);

    void removeTransaction(const TxnTimestamp & txnID);

    std::vector<TxnTimestamp> getActiveTransactions();

    /// virtual warehouse related interface
    void updateServerWorkerGroup(const String & vw_name, const String & worker_group_name, const HostWithPortsVec & workers);

    HostWithPortsVec getWorkersInWorkerGroup(const String & worker_group_name);

    /// system tables related interface

    std::optional<DB::Protos::DataModelTable> getTableByID(const Protos::TableIdentifier & identifier);
    DataModelTables getTablesByID(std::vector<std::shared_ptr<Protos::TableIdentifier>> & identifiers);

    DataModelDBs getAllDataBases();

    DataModelTables getAllTables();

    IMetaStore::IteratorPtr getTrashTableIDIterator(uint32_t iterator_internal_batch_size);

    DataModelUDFs getAllUDFs(const String &database_name, const String &function_name);

    DataModelUDFs getUDFByName(const std::unordered_set<String> &function_names);

    std::vector<std::shared_ptr<Protos::TableIdentifier>> getTrashTableID();

    DataModelTables getTablesInTrash();

    DataModelDBs getDatabaseInTrash();

    std::vector<std::shared_ptr<Protos::TableIdentifier>> getAllTablesID(const String & db = "");

    std::shared_ptr<Protos::TableIdentifier> getTableIDByName(const String & db, const String & table);

    DataModelWorkerGroups getAllWorkerGroups();

    DataModelDictionaries getAllDictionaries();

    /// APIs to clear metadata from ByteKV
    void clearDatabaseMeta(const String & database, const UInt64 & ts);

    void clearTableMetaForGC(const String & database, const String & name, const UInt64 & ts);
    void clearDataPartsMeta(const StoragePtr & table, const DataPartsVector & parts, const bool skip_part_cache = false);
    void clearStagePartsMeta(const StoragePtr & table, const DataPartsVector & parts);
    void clearDataPartsMetaForTable(const StoragePtr & table);

    /// APIs to sync data parts for preallocate mode
    std::vector<TxnTimestamp> getSyncList(const StoragePtr & table);
    void clearSyncList(const StoragePtr & table, std::vector<TxnTimestamp> & sync_list);
    ServerDataPartsVector getServerPartsByCommitTime(const StoragePtr & table, std::vector<TxnTimestamp> & sync_list);

    /// APIs for multiple namenode
    void createRootPath(const String & path);
    void deleteRootPath(const String & path);
    std::vector<std::pair<String, UInt32>> getAllRootPath();

    ///APIs for data mutation
    void createMutation(const StorageID & storage_id, const String & mutation_name, const String & mutate_text);
    void removeMutation(const StorageID & storage_id, const String & mutation_name);
    Strings getAllMutations(const StorageID & storage_id);
    std::multimap<String, String> getAllMutations();


    /// TODO:
    // void addCheckpoint(const StoragePtr &, Checkpoint) { }
    // void markCheckpoint(const StoragePtr &, Checkpoint) { }
    // void removeCheckpoint(const StoragePtr &, Checkpoint) { }
    Checkpoints getCheckpoints() { return {}; }

    /// TODO:
    DataPartsVector getAllDataPartsBetween(const StoragePtr &, const TxnTimestamp &, const TxnTimestamp &) { return {}; }

    void setTableClusterStatus(const UUID & table_uuid, const bool clustered);
    void getTableClusterStatus(const UUID & table_uuid, bool & clustered);
    bool isTableClustered(const UUID & table_uuid);

    /// BackgroundJob related API
    void setBGJobStatus(const UUID & table_uuid, CnchBGThreadType type, CnchBGThreadStatus status);
    std::optional<CnchBGThreadStatus> getBGJobStatus(const UUID & table_uuid, CnchBGThreadType type);
    std::unordered_map<UUID, CnchBGThreadStatus> getBGJobStatuses(CnchBGThreadType type);
    void dropBGJobStatus(const UUID & table_uuid, CnchBGThreadType type);

    void setTablePreallocateVW(const UUID & table_uuid, const String vw);
    void getTablePreallocateVW(const UUID & table_uuid, String & vw);

    /***
     * API to collect all metrics about a table
     */
    std::unordered_map<String, PartitionFullPtr> getTablePartitionMetrics(const DB::Protos::DataModelTable & table, bool & is_ready);

    std::unordered_map<String, PartitionMetricsPtr> getTablePartitionMetricsFromMetastore(const String & table_uuid);

    /// this is periodically called by leader server only
    void updateTopologies(const std::list<CnchServerTopology> & topologies);

    std::list<CnchServerTopology> getTopologies();

    /// Time Travel relate interfaces
    std::vector<UInt64> getTrashDBVersions(const String & database);
    void undropDatabase(const String & database, const UInt64 & ts);

    std::unordered_map<String, UInt64> getTrashTableVersions(const String & database, const String & table);
    void undropTable(const String & database, const String & table, const UInt64 & ts);

    /// Get the key of a insertion label with the KV namespace
    String getInsertionLabelKey(const InsertionLabelPtr & label);
    /// CAS: insert a label with precommitted state
    void precommitInsertionLabel(const InsertionLabelPtr & label);
    /// CAS: change from precommitted state to committed state
    void commitInsertionLabel(InsertionLabelPtr & label);
    void tryCommitInsertionLabel(InsertionLabelPtr & label);
    /// Check the label and delete it
    bool abortInsertionLabel(const InsertionLabelPtr & label, String & message);

    InsertionLabelPtr getInsertionLabel(UUID uuid, const String & name);
    void removeInsertionLabel(UUID uuid, const String & name);
    void removeInsertionLabels(const std::vector<InsertionLabel> & labels);
    /// Scan all the labels under a table
    std::vector<InsertionLabel> scanInsertionLabels(UUID uuid);
    /// Clear all the labels under a table
    void clearInsertionLabels(UUID uuid);

    void createVirtualWarehouse(const String & vw_name, const VirtualWarehouseData & data);
    void alterVirtualWarehouse(const String & vw_name, const VirtualWarehouseData & data);
    bool tryGetVirtualWarehouse(const String & vw_name, VirtualWarehouseData & data);
    std::vector<VirtualWarehouseData> scanVirtualWarehouses();
    void dropVirtualWarehouse(const String & vw_name);

    void createWorkerGroup(const String & worker_group_id, const WorkerGroupData & data);
    void updateWorkerGroup(const String & worker_group_id, const WorkerGroupData & data);
    bool tryGetWorkerGroup(const String & worker_group_id, WorkerGroupData & data);
    std::vector<WorkerGroupData> scanWorkerGroups();
    void dropWorkerGroup(const String & worker_group_id);

    UInt64 getNonHostUpdateTimestampFromByteKV(const UUID & uuid);

    /** Masking policy NOTEs
     * 1. Column masking policy refers to the masking policy data model, it will be applied to multiple columns with 1-n association.
     * 2. Column masking info refers to the relationship between a table's column and the uuid of masking policy applied to it.
     * 3. Database and column in future may have UUID by design instead of name
     */
    // MaskingPolicyExists maskingPolicyExists(const Strings & masking_policy_names);
    // std::vector<std::optional<MaskingPolicyModel>> getMaskingPolicies(const Strings & masking_policy_names);
    // void putMaskingPolicy(MaskingPolicyModel & masking_policy);
    // std::optional<MaskingPolicyModel> tryGetMaskingPolicy(const String & masking_policy_name);
    // MaskingPolicyModel getMaskingPolicy(const String & masking_policy_name);
    // std::vector<MaskingPolicyModel> getAllMaskingPolicy();
    // Strings getMaskingPolicyAppliedTables(const String & masking_policy_name);
    // Strings getAllMaskingPolicyAppliedTables();
    // void dropMaskingPolicies(const Strings & masking_policy_names);

    bool isHostServer(const StoragePtr & storage) const;

    void setMergeMutateThreadStartTime(const StorageID & storage_id, const UInt64 & startup_time) const;

    UInt64 getMergeMutateThreadStartTime(const StorageID & storage_id) const;

private:
    Poco::Logger * log = &Poco::Logger::get("Catalog");
    Context & context;
    MetastoreProxyPtr meta_proxy;
    const String name_space;

    UInt32 max_commit_size_one_batch {2000};
    std::unordered_map<UUID, std::shared_ptr<std::mutex>> nhut_mutex;
    std::mutex all_storage_nhut_mutex;
    UInt32 max_drop_size_one_batch {10000};

    std::shared_ptr<Protos::DataModelDB> tryGetDatabaseFromMetastore(const String & database, const UInt64 & ts);
    std::shared_ptr<Protos::DataModelTable>
    tryGetTableFromMetastore(const String & table_uuid, const UInt64 & ts, bool with_prev_versions = false, bool with_deleted = false);
    Strings tryGetDependency(const ASTPtr & create_query);
    static void replace_definition(Protos::DataModelTable & table, const String & db_name, const String & table_name);
    StoragePtr createTableFromDataModel(const Context & session_context, const Protos::DataModelTable & data_model);
    void detachOrAttachTable(const String & db, const String & name, const TxnTimestamp & ts, bool is_detach);
    DataModelPartPtrVector getDataPartsMetaFromMetastore(
        const StoragePtr & storage, const Strings & required_partitions, const Strings & full_partitions, const TxnTimestamp & ts);
    void detachOrAttachDictionary(const String & db, const String & name, bool is_detach);
    void moveTableIntoTrash(
        Protos::DataModelTable & table,
        Protos::TableIdentifier & table_id,
        const TxnTimestamp & txnID,
        const TxnTimestamp & ts,
        BatchCommitRequest & batchWrite);
    void restoreTableFromTrash(
        std::shared_ptr<Protos::TableIdentifier> table_id, const UInt64 & ts, BatchCommitRequest & batch_write);

    void clearDataPartsMetaInternal(
        const StoragePtr & table, const DataPartsVector & parts, const DeleteBitmapMetaPtrVector & delete_bitmaps = {});

    Strings getPartitionIDsFromMetastore(const StoragePtr & storage);

    void mayUpdateUHUT(const StoragePtr & storage);

    bool canUseCache(const StoragePtr & storage, const Context * session_context);

    void finishCommitInBatch(
        const StoragePtr & storage,
        const TxnTimestamp & txnID,
        const Protos::DataModelPartVector & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const Protos::DataModelPartVector & staged_parts,
        const bool preallocate_mode,
        const std::vector<String> & expected_parts,
        const std::vector<String> & expected_bitmaps,
        const std::vector<String> & expected_staged_parts);

    void finishCommitInternal(
        const StoragePtr & storage,
        const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & parts_to_commit,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & staged_parts,
        const UInt64 & txnid,
        const bool preallocate_mode,
        const std::vector<String> & expected_parts,
        const std::vector<String> & expected_bitmaps,
        const std::vector<String> & expected_staged_parts);

    /// check if the table can be dropped safely. It's not allowed to drop a table if other tables rely on it.
    /// If is_dropping_db set to true, we can ignore any dependency under the same database.
    void checkCanbeDropped(Protos::TableIdentifier & table_id, bool is_dropping_db);

    void assertLocalServerThrowIfNot(const StoragePtr & storage) const;

    /// Data models whose key begins with 'PartitionID_' and ends with '_TxnID' can share the following logic,
    /// currently used by data parts and delete bitmaps
    template <typename T>
    std::vector<T> getDataModelsByPartitions(
        const StoragePtr & storage,
        const String & meta_prefix,
        const Strings & partitions,
        const Strings & full_partitions,
        const std::function<T(const String &)> & create_func,
        const TxnTimestamp & ts,
        UInt32 time_out_ms = 0)
    {
        Stopwatch watch;
        std::vector<T> res;

        String table_uuid = UUIDHelpers::UUIDToString(storage->getStorageID().uuid);
        UInt64 timestamp = ts.toUInt64();

        std::vector<String> request_partitions;
        for (const auto & partition : partitions)
            request_partitions.emplace_back(partition);

        std::sort(request_partitions.begin(), request_partitions.end(), partition_comparator{});

        auto plist_start = full_partitions.begin();
        auto plist_end = full_partitions.end();

        for (auto partition_it = request_partitions.begin(); partition_it != request_partitions.end();)
        {
            IMetaStore::IteratorPtr mIt;
            while (plist_start != plist_end && *partition_it != *plist_start)
                plist_start++;

            if (plist_start == plist_end)
            {
                mIt = meta_proxy->getMetaInRange(meta_prefix, *partition_it + "_", *partition_it + "_", true, true);
                partition_it++;
            }
            else
            {
                auto & start_partition = *partition_it;
                String end_partition = *partition_it;
                while (plist_start != plist_end && partition_it != request_partitions.end() && *partition_it == *plist_start)
                {
                    end_partition = *partition_it;
                    partition_it++;
                    plist_start++;
                }
                mIt = meta_proxy->getMetaInRange(meta_prefix, start_partition + "_", end_partition + "_", true, true);
            }

            size_t counter = 0;
            while (mIt->next())
            {
                if (time_out_ms && !(++counter%20000) && watch.elapsedMilliseconds()>time_out_ms)
                {
                    throw Exception("Get data from metastore reached timeout " + toString(time_out_ms) + "ms.", ErrorCodes::TIMEOUT_EXCEEDED);
                }
                /// if timestamp is set, only return the meta where commit time <= timestamp
                if (timestamp)
                {
                    const auto & key = mIt->key();
                    auto pos = key.find_last_of('_');
                    if (pos != String::npos)
                    {
                        UInt64 commit_time = std::stoull(key.substr(pos + 1, String::npos), nullptr);
                        if (commit_time > timestamp)
                            continue;
                    }
                }

                T data_model = create_func(mIt->value());
                if (data_model)
                    res.emplace_back(std::move(data_model));
            }
        }

        return res;
    }

    /// Check status for part, or deleted bitmap or staged part
    template <typename T, typename GenerateKeyFunc>
    void checkItemsStatus(const std::vector<T> & items, GenerateKeyFunc&& generateKey, std::vector<size_t> & items_to_remove, std::vector<String> & expected_items)
    {
        auto item_status = meta_proxy->getItemStatus<T>(items, generateKey);
        for (size_t i = 0; i < item_status.size(); i++) {
            if (item_status[i].second == 0) // status 0 indicates not exist
                items_to_remove.push_back(i);
            else
                expected_items.emplace_back(std::move(item_status[i].first));
        }
    }
};

template<typename T>
void remove_not_exist_items(std::vector<T> & items_to_write, std::vector<size_t> & items_not_exist)
{
    size_t item_id {0};
    items_to_write.erase(std::remove_if(items_to_write.begin(), items_to_write.end(), [&](auto & elem) {
        if (item_id != items_not_exist.size() && (&elem - &items_to_write[0]) == static_cast<long>(items_not_exist[item_id])) {
            ++item_id;
            return true;
        }
        return false;
    }), items_to_write.end());
}

using CatalogPtr = std::shared_ptr<Catalog>;
void fillUUIDForDictionary(DB::Protos::DataModelDictionary &);
}
