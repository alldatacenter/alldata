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

#include <Catalog/CatalogConfig.h>
//#include <Catalog/MetastoreByteKVImpl.h>
#include <Catalog/MetastoreFDBImpl.h>
#include <Catalog/StringHelper.h>
#include <Protos/data_models.pb.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
// #include <Transaction/ICnchTransaction.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/TransactionCommon.h>
#include <google/protobuf/repeated_field.h>
#include <unordered_set>
#include <sstream>
#include <Core/UUID.h>
#include <Protos/RPCHelpers.h>
#include <cppkafka/cppkafka.h>
#include <MergeTreeCommon/InsertionLabel.h>
#include <Statistics/ExportSymbols.h>
#include <Statistics/StatisticsBase.h>
#include <ResourceManagement/CommonData.h>
#include <Catalog/IMetastore.h>
#include <CloudServices/CnchBGThreadCommon.h>

namespace DB::Catalog
{

#define DB_STORE_PREFIX "DB_"
#define DB_UUID_UNIQUE_PREFIX "DU_"
#define DELETE_BITMAP_PREFIX "DLB_"
#define TABLE_STORE_PREFIX "TB_"
#define MASKING_POLICY_PREFIX "MP_"
#define MASKING_POLICY_TABLE_MAPPING "MPT_"
#define TABLE_UUID_MAPPING "TM_"
#define TABLE_UUID_UNIQUE_PREFIX "TU_"
#define PART_STORE_PREFIX "PT_"
#define STAGED_PART_STORE_PREFIX "STG_PT_"
#define TABLE_PARTITION_INFO_PREFIX "TP_"
#define WORKER_GROUP_STORE_PREFIX "WC_"
#define RM_VW_PREFIX "RMVW_" /// Resource Management Virtual Warehouse
#define RM_WG_PREFIX "RMWG_" /// Resource Management Worker Group
#define TRANSACTION_STORE_PREFIX "TXN_"
#define TRANSACTION_RECORD_PREFIX "TR_"
#define UNDO_BUFFER_PREFIX "UB_"
#define KV_LOCK_PREFIX "LK_"
#define VIEW_DEPENDENCY_PREFIX "VD_"
#define SYNC_LIST_PREFIX "SL_"
#define KAFKA_OFFSETS_PREFIX "KO_"
#define ROOT_PATH_PREFIX "RP_"
#define ROOT_PATH_ID_UNIQUE_PREFIX "RU_"
#define TABLE_MUTATION_PREFIX "MT_"
#define WRITE_INTENT_PREFIX "WI_"
#define TABLE_TRASH_PREFIX "TRASH_"
#define DICTIONARY_TRASH_PREFIX "DICTRASH_"
#define CNCH_LOG_PREFIX "LG_"
#define DATABASE_TRASH_PREFIX "DTRASH_"
#define SERVERS_TOPOLOGY_KEY "SERVERS_TOPOLOGY"
#define TABLE_CLUSTER_STATUS "TCS_"
#define CLUSTER_BG_JOB_STATUS "CLUSTER_BGJS_"
#define MERGE_BG_JOB_STATUS "MERGE_BGJS_"
#define PARTGC_BG_JOB_STATUS "PARTGC_BGJS_"
#define CONSUMER_BG_JOB_STATUS "CONSUMER_BGJS_"
#define DEDUPWORKER_BG_JOB_STATUS "DEDUPWORKER_BGJS_"
#define PREALLOCATE_VW "PVW_"
#define DICTIONARY_STORE_PREFIX "DIC_"
#define RESOURCE_GROUP_PREFIX "RG_"
#define NONHOST_UPDATE_TIMESTAMP_PREFIX "NHUT_"
#define INSERTION_LABEL_PREFIX "ILB_"
#define TABLE_STATISTICS_PREFIX "TS_"
#define TABLE_STATISTICS_TAG_PREFIX "TST_"
#define COLUMN_STATISTICS_PREFIX "CS_"
#define COLUMN_STATISTICS_TAG_PREFIX "CST_"
#define FILESYS_LOCK_PREFIX "FSLK_"
#define UDF_STORE_PREFIX "UDF_"
#define MERGEMUTATE_THREAD_START_TIME "MTST_"

class MetastoreProxy
{
public:
    using MetastorePtr = std::shared_ptr<IMetaStore>;
    using RepeatedFields = google::protobuf::RepeatedPtrField<std::string>;

    MetastoreProxy(CatalogConfig & config)
    {
        if (config.type == StoreType::FDB)
        {
            metastore_ptr = std::make_shared<MetastoreFDBImpl>(config.fdb_conf.cluster_conf_path);
        }
        else
        {
            throw Exception("Catalog must be correctly configured. Only support foundationdb and bytekv now.", ErrorCodes::METASTORE_EXCEPTION);
        }
    }

    ~MetastoreProxy() {}

    /**
     * Metastore Proxy keying schema
     *
     */
    static std::string tableUUIDMappingPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + TABLE_UUID_MAPPING;
    }

    static std::string tableUUIDMappingPrefix(const std::string & name_space, const std::string & db)
    {
        return tableUUIDMappingPrefix(name_space) + escapeString(db) + (db.empty() ? "" : "_");
    }

    static std::string tableUUIDMappingKey(const std::string & name_space, const std::string & db, const std::string & table_name)
    {
        return tableUUIDMappingPrefix(name_space, db) + escapeString(table_name);
    }

    static std::string allDbPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + DB_STORE_PREFIX;
    }

    static std::string dbKeyPrefix(const std::string & name_space, const std::string & db)
    {
        return allDbPrefix(name_space) + escapeString(db) + '_';
    }

    static std::string dbUUIDUniqueKey(const std::string & name_space, const std::string & uuid)
    {
        return escapeString(name_space) + '_' + DB_UUID_UNIQUE_PREFIX + uuid;
    }

    static std::string dbKey(const std::string & name_space, const std::string & db, UInt64 ts)
    {
        return allDbPrefix(name_space) + escapeString(db) + '_' + toString(ts);
    }

    static std::string deleteBitmapPrefix(const std::string & name_space, const std::string & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << "_" << DELETE_BITMAP_PREFIX << uuid << "_";
        return ss.str();
    }

    /// Prefix_PartitionID_PartMinBlock_PartMaxBlock_Reserved_Type_TxnID
    static std::string deleteBitmapKey(const std::string & name_space, const std::string & uuid, const Protos::DataModelDeleteBitmap & bitmap)
    {
        std::stringstream ss;
        ss << deleteBitmapPrefix(name_space, uuid)
           << bitmap.partition_id() << "_" << bitmap.part_min_block() << "_" << bitmap.part_max_block() << "_"
           << bitmap.reserved() << "_" << bitmap.type() << "_" << bitmap.txn_id();
        return ss.str();
    }

    static std::string tableMetaPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + TABLE_STORE_PREFIX;
    }

    static std::string udfMetaPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + UDF_STORE_PREFIX;
    }

    static std::string dbTrashPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + DATABASE_TRASH_PREFIX;
    }

    static std::string dbTrashKey(const std::string & name_space, const std::string & db, const UInt64 & ts)
    {
        return dbTrashPrefix(name_space) + escapeString(db) + "_" + toString(ts);
    }

    static std::string tableTrashPrefix(const std::string & name_space, const std::string & db = "")
    {
        return escapeString(name_space) + '_' + TABLE_TRASH_PREFIX + escapeString(db) + (db.empty() ? "" : "_");
    }

    static std::string tableTrashKey(const std::string & name_space, const std::string & db, const std::string & table, const UInt64 & ts)
    {
        return tableTrashPrefix(name_space, db) + escapeString(table) + "_" + toString(ts);
    }

    static std::string dictionaryTrashPrefix(const std::string & name_space, const std::string & db)
    {
        return escapeString(name_space) + '_' + DICTIONARY_TRASH_PREFIX + escapeString(db);
    }

    static std::string dictionaryTrashKey(const std::string & name_space, const std::string & db, const std::string & name)
    {
        return dictionaryTrashPrefix(name_space, db) + "_" + escapeString(name);
    }

    static std::string tableStorePrefix(const std::string & name_space, const std::string & uuid)
    {
        return tableMetaPrefix(name_space) + uuid + '_';
    }

    static std::string udfStoreKey(const std::string & name_space, const std::string & db, const std::string & function_name)
    {
        return udfMetaPrefix(name_space) + db + '.' + function_name;
    }

    static std::string udfStoreKey(const std::string & name_space, const std::string & name)
    {
        return udfMetaPrefix(name_space) + name;
    }

    static std::string tableStoreKey(const std::string & name_space, const std::string & uuid, UInt64 commit_time)
    {
        return tableStorePrefix(name_space, uuid) + toString(commit_time);
    }

    static std::string maskingPolicyMetaPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + MASKING_POLICY_PREFIX;
    }

    static std::string maskingPolicyKey(const std::string & name_space, const std::string & masking_policy_name)
    {
        return fmt::format("{}_{}{}", escapeString(name_space), MASKING_POLICY_PREFIX, masking_policy_name);
    }

    static std::string maskingPolicyTableMappingPrefix(const std::string & name_space, const std::string & masking_policy_name)
    {
        return fmt::format("{}_{}{}", escapeString(name_space), MASKING_POLICY_TABLE_MAPPING, masking_policy_name);
    }

    static std::string maskingPolicyTableMappingKey(const std::string & name_space, const std::string & masking_policy_name, const std::string & uuid)
    {
        return fmt::format("{}_{}", maskingPolicyTableMappingPrefix(name_space, masking_policy_name), uuid);
    }

    static std::string nonHostUpdateKey(const std::string & name_space, const String & table_uuid)
    {
        return escapeString(name_space) + "_" + NONHOST_UPDATE_TIMESTAMP_PREFIX + table_uuid;
    }

    static std::string dictionaryPrefix(const std::string & name_space, const std::string & db)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << DICTIONARY_STORE_PREFIX << escapeString(db) << "_";
        return ss.str();
    }

    static std::string dictionaryStoreKey(const std::string & name_space, const std::string & db, const std::string & name)
    {
        std::stringstream ss;
        ss << dictionaryPrefix(name_space, db) << escapeString(name);
        return ss.str();
    }

    static std::string allDictionaryPrefix(const std::string & name_space)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << DICTIONARY_STORE_PREFIX;
        return ss.str();
    }

    static std::string viewDependencyPrefix(const std::string & name_space, const std::string & dependency)
    {
        return escapeString(name_space) + '_' + VIEW_DEPENDENCY_PREFIX + dependency + '_';
    }

    static std::string viewDependencyKey(const std::string & name_space, const std::string & dependency, const std::string & uuid)
    {
        return viewDependencyPrefix(name_space, dependency) + uuid;
    }

    static std::string tableUUIDUniqueKey(const std::string & name_space, const std::string & uuid)
    {
        return escapeString(name_space) + '_' + TABLE_UUID_UNIQUE_PREFIX + uuid;
    }

    static std::string tablePartitionInfoPrefix(const std::string & name_space, const std::string & uuid)
    {
        return escapeString(name_space) + '_' + TABLE_PARTITION_INFO_PREFIX + uuid + '_';
    }

    static std::string tableMutationPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + "_" + TABLE_MUTATION_PREFIX;
    }

    static std::string tableMutationPrefix(const std::string & name_space, const std::string & uuid)
    {
        return escapeString(name_space) + "_" + TABLE_MUTATION_PREFIX + uuid + "_";
    }

    static std::string tableMutationKey(const std::string & name_space, const std::string & uuid, const std::string & mutation)
    {
        return escapeString(name_space) + "_" + TABLE_MUTATION_PREFIX + uuid + "_" + mutation;
    }

    static std::string dataPartPrefix(const std::string & name_space, const std::string & uuid)
    {
        return  escapeString(name_space) + '_' + PART_STORE_PREFIX + uuid + '_';
    }

    static std::string stagedDataPartPrefix(const std::string & name_space, const std::string & uuid)
    {
        return  escapeString(name_space) + '_' + STAGED_PART_STORE_PREFIX + uuid + '_';
    }

    static std::string dataPartKey(const std::string & name_space, const std::string & uuid, const String & part_name)
    {
        return dataPartPrefix(name_space, uuid) + part_name;
    }

    static std::string stagedDataPartKey(const std::string & name_space, const std::string & uuid, const String & part_name)
    {
        return stagedDataPartPrefix(name_space, uuid) + part_name;
    }

    static std::string syncListPrefix(const std::string & name_space, const std::string & uuid)
    {
        return escapeString(name_space) + '_' + SYNC_LIST_PREFIX + uuid + '_';
    }

    static std::string syncListKey(const std::string & name_space, const std::string & uuid, UInt64 ts)
    {
        return syncListPrefix(name_space, uuid) + toString(ts);
    }

    static std::string syncListKey(const std::string & name_space, const std::string & uuid, TxnTimestamp & ts)
    {
        return syncListPrefix(name_space, uuid) + ts.toString();
    }

    static std::string kafkaOffsetsKey(const std::string & name_space, const std::string & group_name,
                                              const std::string & topic, const int partition_num)
    {
        return escapeString(name_space) + "_" + KAFKA_OFFSETS_PREFIX + escapeString(group_name) + "_"
               + escapeString(topic) + "_" + std::to_string(partition_num);
    }

    static std::string transactionRecordKey(const std::string & name_space, const UInt64 & txn_id)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << "_" << TRANSACTION_RECORD_PREFIX << txn_id;
        return ss.str();
    }

    static std::string transactionKey(UInt64 txn)
    {
        return TRANSACTION_STORE_PREFIX + toString(txn);
    }

    static std::string writeIntentKey(const std::string & name_space, const String & intent_prefix, const std::string & intent_name)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << "_" << WRITE_INTENT_PREFIX << intent_prefix << "_" << intent_name;
        return ss.str();
    }

    static std::string undoBufferKey(const std::string & name_space, const UInt64 & txn)
    {
        return escapeString(name_space) + '_' + UNDO_BUFFER_PREFIX + toString(txn);
    }

    static std::string undoBufferStoreKey(const std::string & name_space, const UInt64 & txn, const UndoResource & resource)
    {
        return undoBufferKey(name_space, txn) + '_' + escapeString(toString(resource.id));
    }

    static std::string kvLockKey(const std::string & name_space, const std::string & uuid, const std::string & part_name)
    {
        return escapeString(name_space) + '_' + KV_LOCK_PREFIX + uuid + '_' + part_name;
    }

    static std::string clusterStatusKey(const std::string & name_space, const std::string & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << TABLE_CLUSTER_STATUS << uuid;
        return ss.str();
    }

    static std::string allClusterBGJobStatusKeyPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + CLUSTER_BG_JOB_STATUS;
    }

    static std::string clusterBGJobStatusKey(const std::string & name_space, const std::string & uuid)
    {
        return allClusterBGJobStatusKeyPrefix(name_space) + uuid;
    }

    static std::string allMergeBGJobStatusKeyPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + MERGE_BG_JOB_STATUS;
    }

    static std::string mergeBGJobStatusKey(const std::string & name_space, const std::string & uuid)
    {
        return allMergeBGJobStatusKeyPrefix(name_space) + uuid;
    }

    static std::string allPartGCBGJobStatusKeyPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + PARTGC_BG_JOB_STATUS;
    }

    static std::string partGCBGJobStatusKey(const std::string & name_space, const std::string & uuid)
    {
        return allPartGCBGJobStatusKeyPrefix(name_space) + uuid;
    }

    static std::string allConsumerBGJobStatusKeyPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + CONSUMER_BG_JOB_STATUS;
    }

    static std::string consumerBGJobStatusKey(const std::string & name_space, const std::string & uuid)
    {
        return allConsumerBGJobStatusKeyPrefix(name_space) + uuid;
    }

    static std::string allDedupWorkerBGJobStatusKeyPrefix(const std::string & name_space)
    {
        return escapeString(name_space) + '_' + DEDUPWORKER_BG_JOB_STATUS;
    }

    static std::string dedupWorkerBGJobStatusKey(const std::string & name_space, const std::string & uuid)
    {
        return allDedupWorkerBGJobStatusKeyPrefix(name_space) + uuid;
    }

    static UUID parseUUIDFromBGJobStatusKey(const std::string & key);

    static std::string preallocateVW(const std::string & name_space, const std::string & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << PREALLOCATE_VW << uuid;
        return ss.str();
    }

    static String insertionLabelKey(const String & name_space, const std::string & uuid, const std::string & label)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << INSERTION_LABEL_PREFIX << uuid << '_' << label;
        return ss.str();
    }

    static String tableStatisticKey(const String name_space, const String & uuid, const StatisticsTag & tag)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << TABLE_STATISTICS_PREFIX << uuid << '_' << static_cast<UInt64>(tag);
        return ss.str();
    }

    static String tableStatisticPrefix(const String name_space, const String & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << TABLE_STATISTICS_PREFIX << uuid << '_';
        return ss.str();
    }

    static String tableStatisticTagKey(const String name_space, const String & uuid, const StatisticsTag & tag)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << TABLE_STATISTICS_TAG_PREFIX << uuid << '_' << static_cast<UInt64>(tag);
        return ss.str();
    }

    static String tableStatisticTagPrefix(const String name_space, const String & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << TABLE_STATISTICS_TAG_PREFIX << uuid << '_';
        return ss.str();
    }

     static String columnStatisticKey(const String name_space, const String & uuid, const String & column, const StatisticsTag & tag)
     {
         std::stringstream ss;
         ss << escapeString(name_space) << '_' << COLUMN_STATISTICS_PREFIX << uuid << '_' << escapeString(column) << '_' << static_cast<UInt64>(tag);
         return ss.str();
     }

    static String columnStatisticPrefix(const String name_space, const String & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << COLUMN_STATISTICS_PREFIX << uuid << '_';
        return ss.str();
    }

     static String columnStatisticTagKey(const String name_space, const String & uuid, const String & column, const StatisticsTag & tag)
     {
         std::stringstream ss;
         ss << escapeString(name_space) << '_' << COLUMN_STATISTICS_TAG_PREFIX << uuid << '_' << escapeString(column) << '_' << static_cast<UInt64>(tag);
         return ss.str();
     }

    static String columnStatisticTagPrefix(const String name_space, const String & uuid, const String & column)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << COLUMN_STATISTICS_TAG_PREFIX << uuid << '_' << escapeString(column) << '_';
        return ss.str();
    }

    static String columnStatisticTagPrefixWithoutColumn(const String name_space, const String & uuid)
    {
        std::stringstream ss;
        ss << escapeString(name_space) << '_' << COLUMN_STATISTICS_TAG_PREFIX << uuid << '_';
        return ss.str();
    }

    static String VWKey(const String & name_space, const String & vw_name)
    {
        return escapeString(name_space) + "_" + RM_VW_PREFIX + vw_name;
    }

    static String WorkerGroupKey(const String & name_space, const String & worker_group_id)
    {
        return escapeString(name_space) + "_" + RM_WG_PREFIX + worker_group_id;
    }

    static String filesysLockKey(const String & name_sapce, const String hdfs_path)
    {
        return escapeString(name_sapce) + '_' + FILESYS_LOCK_PREFIX + escapeString(hdfs_path);
    }

    static String mergeMutateThreadStartTimeKey(const String & name_sapce, const String & uuid)
    {
        return escapeString(name_sapce) + '_' + MERGEMUTATE_THREAD_START_TIME + uuid;
    }

    /// end of Metastore Proxy keying schema

    void createTransactionRecord(const String & name_space, const UInt64 & txn_id, const String & txn_data);
    void removeTransactionRecord(const String & name_space, const UInt64 & txn_id);
    void removeTransactionRecords(const String & name_space, const std::vector<TxnTimestamp> & txn_ids);
    String getTransactionRecord(const String & name_space, const UInt64 & txn_id);
    IMetaStore::IteratorPtr getAllTransactionRecord(const String & name_space, const size_t & max_result_number = 0);
    std::pair<bool, String> updateTransactionRecord(const String & name_space, const UInt64 & txn_id, const String & txn_data_old, const String & txn_data_new);
    std::vector<std::pair<String, UInt64>> getTransactionRecords(const String & name_space, const std::vector<TxnTimestamp> & txn_ids);

    bool updateTransactionRecordWithOffsets(const String & name_space, const UInt64 & txn_id, const String & txn_data_old, const String & txn_data_new, const String & consumer_group, const cppkafka::TopicPartitionList &);
    void setTransactionRecord(const String & name_space, const UInt64 & txn_id, const String & txn_data, UInt64 ttl = 0);

    std::pair<bool, String> updateTransactionRecordWithRequests(
        SinglePutRequest & txn_request, BatchCommitRequest & requests, BatchCommitResponse & response);

    bool writeIntent(const String & name_space, const String & uuid, const std::vector<WriteIntent> & intents, std::vector<String> & cas_failed_list);
    bool resetIntent(const String & name_space, const String & uuid, const std::vector<WriteIntent> & intents, const UInt64 & new_txn_id, const String & new_location);
    void clearIntents(const String & name_space, const String & uuid, const std::vector<WriteIntent> & intents);
    void clearZombieIntent(const String & name_space, const UInt64 & txn_id);

    void writeFilesysLock(const String & name_space, UInt64 txn_id, const String & dir, const String & db, const String & table);

    String getFilesysLock(const String & name_space, const String & dir);

    void clearFilesysLock(const String & name_space, const String & dir);

    IMetaStore::IteratorPtr getAllFilesysLock(const String & name_space);

    /// TODO: remove old transaction api
    void insertTransaction(UInt64 txn);
    void removeTransaction(UInt64 txn);
    IMetaStore::IteratorPtr getActiveTransactions();
    std::unordered_set<UInt64> getActiveTransactionsSet();

    void updateServerWorkerGroup(const String & worker_group_name, const String & worker_group_info);
    void getServerWorkerGroup(const String & worker_group_name, String & worker_group_info);
    void dropServerWorkerGroup(const String & worker_group_name);
    IMetaStore::IteratorPtr getAllWorkerGroupMeta();

    void addDatabase(const String & name_space, const Protos::DataModelDB & db_model);
    void getDatabase(const String & name_space, const String & name, Strings & db_info);
    void dropDatabase(const String & name_space, const Protos::DataModelDB & db_model);
    IMetaStore::IteratorPtr getAllDatabaseMeta(const String & name_space);
    std::vector<Protos::DataModelDB> getTrashDBs(const String & name_space);
    std::vector<UInt64> getTrashDBVersions(const String & name_space, const String & database);

    String getMaskingPolicy(const String & name_space, const String & masking_policy_name) const;
    Strings getMaskingPolicies(const String & name_space, const Strings & masking_policy_names) const;
    Strings getAllMaskingPolicy(const String & name_space) const;
    void putMaskingPolicy(const String & name_space, const Protos::DataModelMaskingPolicy & new_masking_policy) const;
    IMetaStore::IteratorPtr getMaskingPolicyAppliedTables(const String & name_space, const String & masking_policy_name) const;
    void dropMaskingPolicies(const String & name_space, const Strings & masking_policy_name);

    String getTableUUID(const String & name_space, const String & database, const String & name);
    std::shared_ptr<Protos::TableIdentifier> getTableID(const String & name_space, const String & database, const String & name);
    String getTrashTableUUID(const String & name_space, const String & database, const String & name, const UInt64 & ts);
    void createTable(const String & name_space, const DB::Protos::DataModelTable & table_data, const Strings & dependencies, const Strings & masking_policy_mapping);
    void createUDF(const String & name_space, const DB::Protos::DataModelUDF & udf_data);
    void dropUDF(const String & name_space, const String &db_name, const String &function_name);
    void updateTable(const String & name_space, const String & table_uuid, const String & table_info_new, const UInt64 & ts);
    void getTableByUUID(const String & name_space, const String & table_uuid, Strings & tables_info);
    void clearTableMeta(const String & name_space, const String & database, const String & table, const String & uuid, const Strings & dependencies, const UInt64 & ts = 0);
    void renameTable(const String & name_space, Protos::DataModelTable & table, const String & old_db_name, const String & old_table_name, const String & uuid, BatchCommitRequest & batch_write);
    bool alterTable(const String & name_space, const Protos::DataModelTable & table, const Strings & masks_to_remove, const Strings & masks_to_add);
    Strings getAllTablesInDB(const String & name_space, const String & database);
    IMetaStore::IteratorPtr getAllTablesMeta(const String & name_space);
    IMetaStore::IteratorPtr getAllUDFsMeta(const String & name_space, const String & database_name = "");
    Strings getUDFsMetaByName(const String & name_space, const std::unordered_set<String> &function_names);
    std::vector<std::shared_ptr<Protos::TableIdentifier>> getAllTablesId(const String & name_space, const String & db = "");
    Strings getAllDependence(const String & name_space, const String & uuid);
    IMetaStore::IteratorPtr getTrashTableIDIterator(const String & name_space, uint32_t iterator_internal_batch_size);
    std::vector<std::shared_ptr<Protos::TableIdentifier>> getTrashTableID(const String & name_space);
    std::shared_ptr<Protos::TableIdentifier> getTrashTableID(const String & name_space, const String & database, const String & table, const UInt64 & ts);
    std::vector<std::shared_ptr<Protos::TableIdentifier>> getTablesFromTrash(const String & name_space, const String & database);
    std::unordered_map<String, UInt64> getTrashTableVersions(const String & name_space, const String & database, const String & table);

    void createDictionary(const String & name_space, const String & db, const String & name, const String & dic_meta);
    void getDictionary(const String & name_space, const String & db, const String & name, String & dic_meta);
    void dropDictionary(const String & name_space, const String & db, const String & name);
    std::vector<std::shared_ptr<DB::Protos::DataModelDictionary>> getDictionariesInDB(const String & name_space, const String & database);
    IMetaStore::IteratorPtr getAllDictionaryMeta(const String & name_space);
    std::vector<std::shared_ptr<DB::Protos::DataModelDictionary>> getDictionariesFromTrash(const String & name_space, const String & database);

    void prepareAddDataParts(const String & name_space, const String & table_uuid, const Strings & current_partitions,
                             const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & parts, BatchCommitRequest & batch_write,
                             const std::vector<String> & expected_parts, bool update_sync_list = false);
    void prepareAddStagedParts(const String & name_space, const String & table_uuid, const google::protobuf::RepeatedPtrField<Protos::DataModelPart> & parts,
                               BatchCommitRequest & batch_write, const std::vector<String> & expected_staged_parts);

    /// mvcc version drop part
    void dropDataPart(const String & name_space, const String & table_uuid, const String & part_name, const String & part_info);
    Strings getPartsByName(const String & name_space, const String & uuid, RepeatedFields & parts_name);
    IMetaStore::IteratorPtr getPartsInRange(const String & name_space, const String & uuid, const String & partition_id);
    IMetaStore::IteratorPtr getPartsInRange(const String & name_space, const String & table_uuid, const String & range_start, const String & range_end, bool include_start, bool include_end);
    void dropDataPart(const String & name_space, const String & uuid, const String & part_name);
    void dropAllPartInTable(const String & name_space, const String & uuid);

    /// scan staged parts
    IMetaStore::IteratorPtr getStagedParts(const String & name_space, const String & uuid);
    IMetaStore::IteratorPtr getStagedPartsInPartition(const String & name_space, const String & uuid, const String & partition);

    /// check part/delete-bitmap exist
    template <typename T, typename GenerateKeyFunc>
    std::vector<std::pair<String, UInt64>> getItemStatus(const std::vector<T> & items, GenerateKeyFunc&& generateKey)
    {
        if (items.size() == 0)
            return {};

        Strings keys;
        for (const T item: items)
            keys.emplace_back(generateKey(item));

        return metastore_ptr->multiGet(keys);
    }

    void createRootPath(const String & root_path);
    void deleteRootPath(const String & root_path);
    std::vector<std::pair<String, UInt32>> getAllRootPath();

    void createMutation(const String & name_space, const String & uuid, const String & mutation_name, const String & mutation_text);
    void removeMutation(const String & name_space, const String & uuid, const String & mutation_name);
    Strings getAllMutations(const String & name_space, const String & uuid);
    std::multimap<String, String> getAllMutations(const String & name_space);

    void writeUndoBuffer(const String & name_space, const UInt64 & txnID, const String & uuid, UndoResources & resources);

    void clearUndoBuffer(const String & name_space, const UInt64 & txnID);
    IMetaStore::IteratorPtr getUndoBuffer(const String & name_space, UInt64 txnID);
    IMetaStore::IteratorPtr getAllUndoBuffer(const String & name_space);

    void multiDrop(const Strings & keys);

    bool batchWrite(const BatchCommitRequest & request, BatchCommitResponse response);
    /// tmp api to help debug drop keys failed issue. remove this later.
    std::vector<String> multiDropAndCheck(const Strings & keys);

    IMetaStore::IteratorPtr getPartitionList(const String & name_space, const String & uuid);

    void updateTopologyMeta(const String & name_space, const String & topology);
    String getTopologyMeta(const String & name_space);

    IMetaStore::IteratorPtr getSyncList(const String & name_space, const String & uuid);

    void clearSyncList(const String & name_space, const String & uuid, const std::vector<TxnTimestamp> & sync_list);

    /// operations on kafka offsets
    void getKafkaTpl(const String & name_space, const String & consumer_group, cppkafka::TopicPartitionList & tpl);
    cppkafka::TopicPartitionList getKafkaTpl(const String & name_space, const String & consumer_group, const String & topic_name);
    void clearOffsetsForWholeTopic(const String & name_space, const String & topic, const String & consumer_group);

    void setTableClusterStatus(const String & name_space, const String & uuid, const bool & already_clustered);
    void getTableClusterStatus(const String & name_space, const String & uuid, bool & is_clustered);

    /// BackgroundJob related API
    void setBGJobStatus(const String & name_space, const String & uuid, CnchBGThreadType type, CnchBGThreadStatus status);
    std::optional<CnchBGThreadStatus> getBGJobStatus(const String & name_space, const String & uuid, CnchBGThreadType type);

    std::unordered_map<UUID, CnchBGThreadStatus> getBGJobStatuses(const String & name_space, CnchBGThreadType type);

    void dropBGJobStatus(const String & name_space, const String & uuid, CnchBGThreadType type);

    void setTablePreallocateVW(const String & name_space, const String & uuid, const String & vw);
    void getTablePreallocateVW(const String & name_space, const String & uuid, String & vw);

    /// delete bitmap/keys related api
    void prepareAddDeleteBitmaps(const String & name_space, const String & table_uuid, const DeleteBitmapMetaPtrVector & bitmaps,
                                 BatchCommitRequest & batch_write, const std::vector<String> & expected_bitmaps = {});
    Strings getDeleteBitmapByKeys(const Strings & key);

    IMetaStore::IteratorPtr getMetaInRange(const String & prefix, const String & range_start, const String & range_end, bool include_start, bool include_end);

    void precommitInsertionLabel(const String & name_space, const InsertionLabelPtr & label);
    void commitInsertionLabel(const String & name_space, InsertionLabelPtr & label);

    std::pair<uint64_t, String> getInsertionLabel(const String & name_space, const String & uuid, const String & name);
    void removeInsertionLabel(const String & name_space, const String & uuid, const String & name, uint64_t expected_version = 0);
    void removeInsertionLabels(const String & name_space, const std::vector<InsertionLabel> & labels);
    IMetaStore::IteratorPtr scanInsertionLabels(const String & name_space, const String & uuid);
    void clearInsertionLabels(const String & name_space, const String & uuid);

    void createVirtualWarehouse(const String & name_space, const String & vw_name, const VirtualWarehouseData & data);
    void alterVirtualWarehouse(const String & name_space, const String & vw_name, const VirtualWarehouseData & data);
    bool tryGetVirtualWarehouse(const String & name_space, const String & vw_name, VirtualWarehouseData & data);
    std::vector<VirtualWarehouseData> scanVirtualWarehouses(const String & name_space);
    void dropVirtualWarehouse(const String & name_space, const String & vw_name);

    void createWorkerGroup(const String & name_space, const String & worker_group_id, const WorkerGroupData & data);
    void updateWorkerGroup(const String & name_space, const String & worker_group_id, const WorkerGroupData & data);
    bool tryGetWorkerGroup(const String & name_space, const String & worker_group_id, WorkerGroupData & data);
    std::vector<WorkerGroupData> scanWorkerGroups(const String & name_space);
    void dropWorkerGroup(const String & name_space, const String & worker_group_id);

    UInt64 getNonHostUpdateTimeStamp(const String & name_space, const String & table_uuid);
    void setNonHostUpdateTimeStamp(const String & name_space, const String & table_uuid, const UInt64 pts);

    void updateTableStatistics(
        const String & name_space, const String & uuid, const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data);
    std::unordered_map<StatisticsTag, StatisticsBasePtr>
    getTableStatistics(const String & name_space, const String & uuid, const std::unordered_set<StatisticsTag> & tags);
    std::unordered_set<StatisticsTag> getAvailableTableStatisticsTags(const String & name_space, const String & uuid);
    void removeTableStatistics(const String & name_space, const String & uuid, const std::unordered_set<StatisticsTag> & tags);

    void updateColumnStatistics(
        const String & name_space,
        const String & uuid,
        const String & column,
        const std::unordered_map<StatisticsTag, StatisticsBasePtr> & data);
    std::unordered_map<StatisticsTag, StatisticsBasePtr> getColumnStatistics(
        const String & name_space, const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags);
    std::unordered_set<StatisticsTag>
    getAvailableColumnStatisticsTags(const String & name_space, const String & uuid, const String & column);
    void removeColumnStatistics(
        const String & name_space, const String & uuid, const String & column, const std::unordered_set<StatisticsTag> & tags);
    void setMergeMutateThreadStartTime(const String & name_space, const String & uuid, const UInt64 & start_time);
    UInt64 getMergeMutateThreadStartTime(const String & name_space, const String & uuid);

private:

    MetastorePtr metastore_ptr;
};

} // namespace DB::Catalog
