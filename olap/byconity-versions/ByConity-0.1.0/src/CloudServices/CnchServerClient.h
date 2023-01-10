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

#include <CloudServices/RpcClientBase.h>
#include <Protos/cnch_server_rpc.pb.h>
#include <Storages/IStorage_fwd.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/ICnchTransaction.h>
#include <Transaction/CnchLock.h>
#include <WorkerTasks/ManipulationType.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Catalog/CatalogUtils.h>

namespace DB
{
namespace Protos
{
    class CnchServerService_Stub;
}

class ICnchTransaction;

class CnchServerClient : public RpcClientBase
{
public:
    static String getName() { return "CnchServerClient"; }

    explicit CnchServerClient(String host_port_);
    explicit CnchServerClient(HostWithPorts host_ports_);

    ~CnchServerClient() override;

    /// Transaction RPCs related. TODO @canh: add implement when baseline rpc implementation is merged
    std::pair<TxnTimestamp, TxnTimestamp> createTransaction(const TxnTimestamp & primary_txn_id = {0});
    std::pair<TxnTimestamp, TxnTimestamp> createTransactionForKafka(const StorageID & storage_id, const size_t consumer_index);
    TxnTimestamp commitTransaction(
        const ICnchTransaction & txn, const StorageID & kafka_storage_id = StorageID::createEmpty(), const size_t consumer_index = 0);
    void precommitTransaction(const TxnTimestamp & txn_id, const UUID & uuid = UUIDHelpers::Nil);
    TxnTimestamp rollbackTransaction(const TxnTimestamp & txn_id);
    void finishTransaction(const TxnTimestamp & txn_id);

    CnchTransactionStatus getTransactionStatus(const TxnTimestamp & txn_id, bool need_search_catalog = false);

    void removeIntermediateData(const TxnTimestamp & txn_id);

    ServerDataPartsVector fetchDataParts(const String & remote_host, const StoragePtr & table, const Strings & partition_list, const TxnTimestamp & ts);

    void redirectCommitParts(
        const StoragePtr & table,
        const Catalog::CommitItems & commit_data,
        const TxnTimestamp & txnID,
        const bool is_merged_parts,
        const bool preallocate_mode);

    void redirectSetCommitTime(
        const StoragePtr & table,
        const Catalog::CommitItems & commit_data,
        const TxnTimestamp & commitTs,
        const UInt64 txn_id);

    TxnTimestamp commitParts(
        const TxnTimestamp & txn_id,
        ManipulationType type,
        MergeTreeMetaBase & storage,
        const MutableMergeTreeDataPartsCNCHVector & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const MutableMergeTreeDataPartsCNCHVector & staged_parts,
        const String & task_id = {},
        const bool from_server = false,
        const String & consumer_group = {},
        const cppkafka::TopicPartitionList & tpl = {});

    TxnTimestamp precommitParts(
        ContextPtr context,
        const TxnTimestamp & txn_id,
        ManipulationType type,
        MergeTreeMetaBase & storage,
        const MutableMergeTreeDataPartsCNCHVector & parts,
        const DeleteBitmapMetaPtrVector & delete_bitmaps,
        const MutableMergeTreeDataPartsCNCHVector & staged_parts,
        const String & task_id = {},
        const bool from_server = false,
        const String & consumer_group = {},
        const cppkafka::TopicPartitionList & tpl = {});

    google::protobuf::RepeatedPtrField<DB::Protos::DataModelTableInfo>
    getTableInfo(const std::vector<std::shared_ptr<Protos::TableIdentifier>> & tables);
    void controlCnchBGThread(const StorageID & storage_id, CnchBGThreadType type, CnchBGThreadAction action);
    void cleanTransaction(const TransactionRecord & txn_record);
    std::set<UUID> getDeletingTablesInGlobalGC();
    bool removeMergeMutateTasksOnPartition(const StorageID &, const String &);

    void acquireLock(const LockInfoPtr & info);
    void releaseLock(const LockInfoPtr & info);
    void reportCnchLockHeartBeat(const TxnTimestamp & txn_id, UInt64 expire_time = 0);

    std::optional<TxnTimestamp> getMinActiveTimestamp(const StorageID & storage_id);

    UInt64 getServerStartTime();
    bool scheduleGlobalGC(const std::vector<Protos::DataModelTable> & tables);
    size_t getNumOfTablesCanSendForGlobalGC();
    google::protobuf::RepeatedPtrField<DB::Protos::BackgroundThreadStatus>
    getBackGroundStatus(const CnchBGThreadType & type);

    void submitQueryWorkerMetrics(const QueryWorkerMetricElementPtr & query_worker_metric_element);

    UInt32 reportDeduperHeartbeat(const StorageID & cnch_storage_id, const String & worker_table_name);

    void executeOptimize(const StorageID & storage_id, const String & partition_id, bool enable_try, bool mutations_sync, UInt64 timeout_ms);
private:
    std::unique_ptr<Protos::CnchServerService_Stub> stub;
};

using CnchServerClientPtr = std::shared_ptr<CnchServerClient>;

}
