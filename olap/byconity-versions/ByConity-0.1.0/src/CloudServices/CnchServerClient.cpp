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

#include <CloudServices/CnchServerClient.h>
#include <Protos/DataModelHelpers.h>
#include <Protos/cnch_server_rpc.pb.h>

#include <brpc/channel.h>
#include <brpc/controller.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Interpreters/CnchQueryMetrics/QueryWorkerMetricLog.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BRPC_CANNOT_INIT_CHANNEL;
    extern const int NOT_IMPLEMENTED;
}

CnchServerClient::CnchServerClient(String host_port_)
    : RpcClientBase(getName(), std::move(host_port_)), stub(std::make_unique<Protos::CnchServerService_Stub>(&getChannel()))
{
}

CnchServerClient::CnchServerClient(HostWithPorts host_ports_)
    : RpcClientBase(getName(), std::move(host_ports_)), stub(std::make_unique<Protos::CnchServerService_Stub>(&getChannel()))
{
}

CnchServerClient::~CnchServerClient() = default;

std::pair<TxnTimestamp, TxnTimestamp> CnchServerClient::createTransaction(const TxnTimestamp & primary_txn_id)
{
    brpc::Controller cntl;
    Protos::CreateTransactionReq request;
    Protos::CreateTransactionResp response;
    if (primary_txn_id)
        request.set_primary_txn_id(primary_txn_id.toUInt64());
    stub->createTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return {response.txn_id(), response.start_time()};
}

TxnTimestamp
CnchServerClient::commitTransaction(const ICnchTransaction & txn, const StorageID & kafka_storage_id, const size_t consumer_index)
{
    brpc::Controller cntl;
    cntl.set_timeout_ms(10 * 1000);

    Protos::CommitTransactionReq request;
    Protos::CommitTransactionResp response;

    request.set_txn_id(txn.getTransactionID());
    if (const auto & label = txn.getInsertionLabel())
        request.set_insertion_label(label->name);

    if (!kafka_storage_id.empty())
    {
        RPCHelpers::fillStorageID(kafka_storage_id, *request.mutable_kafka_storage_id());
        request.set_kafka_consumer_index(consumer_index);
    }
    stub->commitTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
    return response.commit_ts();
}

void CnchServerClient::precommitTransaction(const TxnTimestamp & txn_id, const UUID & uuid)
{
    brpc::Controller cntl;
    cntl.set_timeout_ms(10 * 1000);

    Protos::PrecommitTransactionReq request;
    Protos::PrecommitTransactionResp response;

    request.set_txn_id(txn_id);
    RPCHelpers::fillUUID(uuid, *request.mutable_main_table_uuid());
    stub->precommitTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

TxnTimestamp CnchServerClient::rollbackTransaction(const TxnTimestamp & txn_id)
{
    brpc::Controller cntl;
    cntl.set_timeout_ms(10 * 1000);

    Protos::RollbackTransactionReq request;
    Protos::RollbackTransactionResp response;

    request.set_txn_id(txn_id);
    stub->rollbackTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
    return response.commit_ts();
}

void CnchServerClient::finishTransaction(const TxnTimestamp & txn_id)
{
    brpc::Controller cntl;
    cntl.set_timeout_ms(20 * 1000); /// TODO: from config

    Protos::FinishTransactionReq request;
    Protos::FinishTransactionResp response;

    request.set_txn_id(txn_id);

    stub->finishTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

std::pair<TxnTimestamp, TxnTimestamp> CnchServerClient::createTransactionForKafka(const StorageID & storage_id, const size_t consumer_index)
{
    brpc::Controller cntl;
    Protos::CreateKafkaTransactionReq request;
    Protos::CreateKafkaTransactionResp response;

    RPCHelpers::fillUUID(storage_id.uuid, *request.mutable_uuid());
    request.set_consumer_index(consumer_index);
    request.set_table_name(storage_id.table_name);

    stub->createTransactionForKafka(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return {response.txn_id(), response.start_time()};
}

ServerDataPartsVector CnchServerClient::fetchDataParts(const String & remote_host, const StoragePtr & table, const Strings & partition_list, const TxnTimestamp & ts)
{
    brpc::Controller cntl;
    Protos::FetchDataPartsReq request;
    Protos::FetchDataPartsResp response;

    request.set_remote_host(remote_host);
    request.set_database(table->getDatabaseName());
    request.set_table(table->getTableName());
    request.set_table_commit_time(table->commit_time);
    request.set_timestamp(ts.toUInt64());

    for (auto & partition_id : partition_list)
        request.add_partitions(partition_id);

    stub->fetchDataParts(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    auto & storage = dynamic_cast<MergeTreeMetaBase &>(*table);
    return createServerPartsFromModels(storage, response.parts());
}

void buildRedirectCommitRequestBase(
    const StoragePtr & table,
    const Catalog::CommitItems & commit_data,
    Protos::RedirectCommitPartsReq & request)
{
    request.set_database(table->getDatabaseName());
    request.set_table(table->getTableName());
    RPCHelpers::fillUUID(table->getStorageUUID(), *request.mutable_uuid());

    for (auto & part : commit_data.data_parts)
    {
        fillPartModel(*table, *part, *request.add_parts());
    }

    for (auto & staged_part : commit_data.staged_parts)
    {
        fillPartModel(*table, *staged_part, *request.add_staged_parts());
    }

    for (auto & delete_bitmap : commit_data.delete_bitmaps)
    {
        auto * new_bitmap = request.add_delete_bitmaps();
        new_bitmap->CopyFrom(*(delete_bitmap->getModel()));
    }
}

void CnchServerClient::redirectCommitParts(
    const StoragePtr & table,
    const Catalog::CommitItems & commit_data,
    const TxnTimestamp & txnID,
    const bool is_merged_parts,
    const bool preallocate_mode)
{
    brpc::Controller cntl;
    Protos::RedirectCommitPartsReq request;
    Protos::RedirectCommitPartsResp response;

    buildRedirectCommitRequestBase(table, commit_data, request);

    request.set_txn_id(txnID.toUInt64());
    request.set_from_merge_task(is_merged_parts);
    request.set_preallocate_mode(preallocate_mode);

    stub->redirectCommitParts(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchServerClient::redirectSetCommitTime(
    const StoragePtr & table,
    const Catalog::CommitItems & commit_data,
    const TxnTimestamp & commitTs,
    const UInt64 txn_id)
{
    brpc::Controller cntl;
    Protos::RedirectCommitPartsReq request;
    Protos::RedirectCommitPartsResp response;

    buildRedirectCommitRequestBase(table, commit_data, request);

    request.set_txn_id(txn_id);
    request.set_commit_ts(commitTs.toUInt64());

    stub->redirectSetCommitTime(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

TxnTimestamp CnchServerClient::commitParts(
    const TxnTimestamp & txn_id,
    ManipulationType type,
    MergeTreeMetaBase & storage,
    const MutableMergeTreeDataPartsCNCHVector & parts,
    const DeleteBitmapMetaPtrVector & delete_bitmaps,
    const MutableMergeTreeDataPartsCNCHVector & staged_parts,
    const String & task_id,
    const bool from_server,
    const String & consumer_group,
    const cppkafka::TopicPartitionList & tpl)
{
    /// TODO: check txn_id & start_ts

    brpc::Controller cntl;
    cntl.set_timeout_ms(10 * 1000); /// TODO: from config
    Protos::CommitPartsReq request;
    Protos::CommitPartsResp response;

    if (from_server)
    {
        StorageCnchMergeTree & cnch_storage = dynamic_cast<StorageCnchMergeTree &>(storage);
        request.set_database(cnch_storage.getDatabaseName());
        request.set_table(cnch_storage.getTableName());
        RPCHelpers::fillUUID(cnch_storage.getStorageUUID(), *request.mutable_uuid());
    }
    else
    {
        StorageCloudMergeTree & cloud_storage = dynamic_cast<StorageCloudMergeTree &>(storage);
        request.set_database(cloud_storage.getCnchDatabase());
        request.set_table(cloud_storage.getCnchTable());
        RPCHelpers::fillUUID(cloud_storage.getStorageUUID(), *request.mutable_uuid());
    }

    request.set_type(UInt32(type));
    for (const auto & part : parts)
    {
        auto * new_part = request.add_parts();
        fillPartModel(storage, *part, *new_part);
        request.add_paths()->assign(part->relative_path);
    }
    for (const auto & staged_part : staged_parts)
    {
        fillPartModel(storage, *staged_part, *request.add_staged_parts());
        request.add_staged_parts_paths()->assign(staged_part->relative_path);
    }

    request.set_txn_id(UInt64(txn_id));
    if (!task_id.empty())
        request.set_task_id(task_id);

    /// add tpl for kafka commit
    if (!consumer_group.empty())
    {
        if (tpl.empty())
            throw Exception("No tpl get while committing kafka data", ErrorCodes::LOGICAL_ERROR);
        request.set_consumer_group(consumer_group);
        for (auto & tp : tpl)
        {
            auto * cur_tp = request.add_tpl();
            cur_tp->set_topic(tp.get_topic());
            cur_tp->set_partition(tp.get_partition());
            cur_tp->set_offset(tp.get_offset());
        }
    }

    /// add delete bitmaps for table with unique key
    for (const auto & delete_bitmap : delete_bitmaps)
    {
        auto * new_bitmap = request.add_delete_bitmaps();
        new_bitmap->CopyFrom(*(delete_bitmap->getModel()));
    }

    stub->commitParts(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return response.commit_timestamp();
}

TxnTimestamp CnchServerClient::precommitParts(
    ContextPtr context,
    const TxnTimestamp & txn_id,
    ManipulationType type,
    MergeTreeMetaBase & storage,
    const MutableMergeTreeDataPartsCNCHVector & parts,
    const DeleteBitmapMetaPtrVector & delete_bitmaps,
    const MutableMergeTreeDataPartsCNCHVector & staged_parts,
    const String & task_id,
    const bool from_server,
    const String & consumer_group,
    const cppkafka::TopicPartitionList & tpl)
{
    // TODO: this method only apply to ManipulationType which supports 2pc
    if (type != ManipulationType::Insert)
    {
        // fallback to 1pc
        return commitParts(txn_id, type, storage, parts, delete_bitmaps, staged_parts, task_id, from_server, consumer_group, tpl);
    }

    const UInt64 batch_size = context->getSettingsRef().catalog_max_commit_size;

    // Precommit parts in batches {batch_begin, batch_end}
    const size_t max_size = std::max({parts.size(), delete_bitmaps.size(), staged_parts.size()});
    for (size_t batch_begin = 0; batch_begin < max_size; batch_begin += batch_size)
    {
        size_t batch_end = batch_begin + batch_size;

        size_t part_batch_begin = std::min(batch_begin, parts.size());
        size_t part_batch_end = std::min(batch_end, parts.size());
        size_t bitmap_batch_begin = std::min(batch_begin, delete_bitmaps.size());
        size_t bitmap_batch_end = std::min(batch_end, delete_bitmaps.size());
        size_t staged_part_batch_begin = std::min(batch_begin, staged_parts.size());
        size_t staged_part_batch_end = std::min(batch_end, staged_parts.size());

        Poco::Logger * log = &Poco::Logger::get(__func__);
        LOG_DEBUG(
            log,
            "Precommit: parts in batch: [{} ~  {}] of total:  {}; delete_bitmaps in batch [{} ~ {}] of total {}; staged parts in batch [{} "
            "~ {}] of total {}.",
            part_batch_begin,
            part_batch_end,
            parts.size(),
            bitmap_batch_begin,
            bitmap_batch_end,
            delete_bitmaps.size(),
            staged_part_batch_begin,
            staged_part_batch_end,
            staged_parts.size());

        commitParts(
            txn_id,
            type,
            storage,
            {parts.begin() + part_batch_begin, parts.begin() + part_batch_end},
            {delete_bitmaps.begin() + bitmap_batch_begin, delete_bitmaps.begin() + bitmap_batch_end},
            {staged_parts.begin() + staged_part_batch_begin, staged_parts.begin() + staged_part_batch_end},
            task_id,
            from_server,
            consumer_group,
            tpl);
    }

    // no commit_time for precommit
    return {};
}

google::protobuf::RepeatedPtrField<DB::Protos::DataModelTableInfo>
CnchServerClient::getTableInfo(const std::vector<std::shared_ptr<Protos::TableIdentifier>> & tables)
{
    brpc::Controller cntl;
    Protos::GetTableInfoReq request;
    Protos::GetTableInfoResp response;

    for (const auto & table : tables)
    {
        DB::Protos::TableIdentifier * table_id = request.add_table_ids();
        table_id->CopyFrom(*table);
    }

    stub->getTableInfo(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return response.table_infos();
}

CnchTransactionStatus CnchServerClient::getTransactionStatus(const TxnTimestamp & txn_id, const bool need_search_catalog)
{
    brpc::Controller cntl;
    Protos::GetTransactionStatusReq request;
    Protos::GetTransactionStatusResp response;

    request.set_txn_id(txn_id.toUInt64());
    request.set_need_search_catalog(need_search_catalog);

    stub->getTransactionStatus(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return response.status();
}
void CnchServerClient::removeIntermediateData(const TxnTimestamp & txn_id)
{
    brpc::Controller cntl;
    Protos::RollbackTransactionReq request;
    Protos::RollbackTransactionResp response;

    request.set_txn_id(txn_id);
    request.set_only_clean_data(true);
    stub->rollbackTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchServerClient::controlCnchBGThread(const StorageID & storage_id, CnchBGThreadType type, CnchBGThreadAction action)
{
    brpc::Controller cntl;
    Protos::ControlCnchBGThreadReq request;
    Protos::ControlCnchBGThreadResp response;

    RPCHelpers::fillStorageID(storage_id, *request.mutable_storage_id());
    request.set_type(uint32_t(type));
    request.set_action(uint32_t(action));

    stub->controlCnchBGThread(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchServerClient::cleanTransaction(const TransactionRecord & txn_record)
{
    brpc::Controller cntl;
    Protos::CleanTransactionReq request;
    Protos::CleanTransactionResp response;

    LOG_DEBUG(&Poco::Logger::get(__func__), "clean txn: [{}] on server: {}", txn_record.toString(), getRPCAddress());

    request.mutable_txn_record()->CopyFrom(txn_record.pb_model);
    stub->cleanTransaction(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchServerClient::acquireLock(const LockInfoPtr & lock)
{
    brpc::Controller cntl;
    Protos::AcquireLockReq request;
    Protos::AcquireLockResp response;
    // TODO: set a big enough waiting time
    cntl.set_timeout_ms(10 * lock->timeout);
    cntl.set_max_retry(0);

    fillLockInfoModel(*lock, *request.mutable_lock());
    stub->acquireLock(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);
    lock->status = static_cast<LockStatus>(response.lock_status());
}

void CnchServerClient::releaseLock(const LockInfoPtr & lock)
{
    brpc::Controller cntl;
    Protos::ReleaseLockReq request;
    Protos::ReleaseLockResp response;

    fillLockInfoModel(*lock, *request.mutable_lock());
    stub->releaseLock(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchServerClient::reportCnchLockHeartBeat(const TxnTimestamp & txn_id, UInt64 expire_time)
{
    brpc::Controller cntl;
    Protos::ReportCnchLockHeartBeatReq request;
    Protos::ReportCnchLockHeartBeatResp response;

    request.set_txn_id(txn_id);
    request.set_expire_time(expire_time);

    stub->reportCnchLockHeartBeat(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

std::set<UUID> CnchServerClient::getDeletingTablesInGlobalGC()
{
    brpc::Controller cntl;
    Protos::GetDeletingTablesInGlobalGCReq request;
    Protos::GetDeletingTablesInGlobalGCResp response;

    stub->getDeletingTablesInGlobalGC(&cntl, &request, &response, nullptr);

    assertController(cntl);

    std::set<UUID> res;
    for (auto & uuid : response.uuids())
        res.insert(RPCHelpers::createUUID(uuid));
    return res;
}

bool CnchServerClient::removeMergeMutateTasksOnPartition(const StorageID & storage_id, const String & partition_id)
{
    brpc::Controller cntl;
    Protos::RemoveMergeMutateTasksOnPartitionReq request;
    RPCHelpers::fillStorageID(storage_id, *request.mutable_storage_id());
    request.set_partition_id(partition_id);
    Protos::RemoveMergeMutateTasksOnPartitionResp response;

    stub->removeMergeMutateTasksOnPartition(&cntl, &request, &response, nullptr);

    assertController(cntl);
    return response.ret();
}

std::optional<TxnTimestamp> CnchServerClient::getMinActiveTimestamp(const StorageID & storage_id)
{
    brpc::Controller cntl;
    Protos::GetMinActiveTimestampReq request;
    Protos::GetMinActiveTimestampResp response;

    RPCHelpers::fillStorageID(storage_id, *request.mutable_storage_id());

    stub->getMinActiveTimestamp(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return response.has_timestamp() ? std::optional(response.timestamp()) : std::nullopt;
}

UInt64 CnchServerClient::getServerStartTime()
{
    brpc::Controller cntl;
    Protos::GetServerStartTimeReq request;
    Protos::GetServerStartTimeResp response;

    stub->getServerStartTime(&cntl, &request, &response, nullptr);

    assertController(cntl);
    return response.server_start_time();
}

bool CnchServerClient::scheduleGlobalGC(const std::vector<Protos::DataModelTable> & tables)
{
    brpc::Controller cntl;
    Protos::ScheduleGlobalGCReq request;

    for (auto & table : tables)
    {
        DB::Protos::DataModelTable * temp_table = request.add_tables();
        temp_table->CopyFrom(table);
    }
    Protos::ScheduleGlobalGCResp response;

    stub->scheduleGlobalGC(&cntl, &request, &response, nullptr);

    assertController(cntl);
    return response.ret();
}

UInt64 CnchServerClient::getNumOfTablesCanSendForGlobalGC()
{
    brpc::Controller cntl;
    Protos::GetNumOfTablesCanSendForGlobalGCReq request;
    Protos::GetNumOfTablesCanSendForGlobalGCResp response;

    stub->getNumOfTablesCanSendForGlobalGC(&cntl, &request, &response, nullptr);

    assertController(cntl);
    return response.num_of_tables_can_send();
}

google::protobuf::RepeatedPtrField<DB::Protos::BackgroundThreadStatus>
CnchServerClient::getBackGroundStatus(const CnchBGThreadType & type)
{
    brpc::Controller cntl;
    Protos::BackgroundThreadStatusReq request;
    Protos::BackgroundThreadStatusResp response;
    request.set_type(type);

    stub->getBackgroundThreadStatus(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return response.status();
}

void CnchServerClient::submitQueryWorkerMetrics(const QueryWorkerMetricElementPtr & query_worker_metric_element)
{
    brpc::Controller cntl;
    Protos::SubmitQueryWorkerMetricsReq request;
    Protos::SubmitQueryWorkerMetricsResp response;

    fillQueryWorkerMetricElement(query_worker_metric_element, *request.mutable_element());

    stub->submitQueryWorkerMetrics(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

UInt32 CnchServerClient::reportDeduperHeartbeat(const StorageID & cnch_storage_id, const String & worker_table_name)
{
    brpc::Controller cntl;
    Protos::ReportDeduperHeartbeatReq request;
    Protos::ReportDeduperHeartbeatResp response;

    RPCHelpers::fillStorageID(cnch_storage_id, *request.mutable_cnch_storage_id());
    request.set_worker_table_name(worker_table_name);

    stub->reportDeduperHeartbeat(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return response.code();
}

void CnchServerClient::executeOptimize(const StorageID & storage_id, const String & partition_id, bool enable_try, bool mutations_sync, UInt64 timeout_ms)
{
    brpc::Controller cntl;
    Protos::ExecuteOptimizeQueryReq request;
    Protos::ExecuteOptimizeQueryResp response;

    RPCHelpers::fillStorageID(storage_id, *request.mutable_storage_id());
    request.set_partition_id(partition_id);
    request.set_enable_try(enable_try);
    request.set_mutations_sync(mutations_sync);

    if (mutations_sync && timeout_ms)
    {
        cntl.set_timeout_ms(timeout_ms);
        request.set_timeout_ms(timeout_ms);
    }

    stub->executeOptimize(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

}
