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

#include <CloudServices/CnchServerServiceImpl.h>

#include <Catalog/Catalog.h>
#include <Protos/RPCHelpers.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Interpreters/Context.h>
#include <Interpreters/CnchQueryMetrics/QueryWorkerMetricLog.h>
#include <Protos/RPCHelpers.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/LockManager.h>
#include <Common/Exception.h>
#include <CloudServices/CnchMergeMutateThread.h>
#include <CloudServices/commitCnchParts.h>
#include <CloudServices/DedupWorkerManager.h>
#include <CloudServices/DedupWorkerStatus.h>
#include <WorkerTasks/ManipulationType.h>
#include <Storages/Kafka/CnchKafkaConsumeManager.h>
#include <Storages/PartCacheManager.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BAD_ARGUMENTS;
    extern const int NOT_IMPLEMENTED;
    extern const int CNCH_KAFKA_TASK_NEED_STOP;
    extern const int UNKNOWN_TABLE;
}

CnchServerServiceImpl::CnchServerServiceImpl(ContextMutablePtr global_context)
    : WithMutableContext(global_context),
      server_start_time(global_context->getTimestamp()),
      global_gc_manager(global_context),

      log(&Poco::Logger::get("CnchServerService"))
{
}


void CnchServerServiceImpl::commitParts(
    [[maybe_unused]] google::protobuf::RpcController * cntl,
    [[maybe_unused]] const Protos::CommitPartsReq * request,
    [[maybe_unused]] Protos::CommitPartsResp * response,
    [[maybe_unused]] google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [c = cntl, req = request, rsp = response, done = done, gc = getContext(), log = log] {
            brpc::ClosureGuard done_guard(done);

            try
            {
                // we need to set the response before any exception is thrown.
                rsp->set_commit_timestamp(0);
                /// Resolve request parameters
                if (req->parts_size() != req->paths_size())
                    throw Exception("Incorrect arguments", ErrorCodes::BAD_ARGUMENTS);

                auto cnch_txn = gc->getCnchTransactionCoordinator().getTransaction(req->txn_id());
                /// Create new rpc context and bind the previous created txn to this rpc context.
                auto rpc_context = RPCHelpers::createSessionContextForRPC(gc, *c);
                rpc_context->setCurrentTransaction(cnch_txn, false);

                auto & database_catalog = DatabaseCatalog::instance();
                StorageID table_id(req->database(), req->table());
                auto storage = database_catalog.tryGetTable(table_id, rpc_context);
                if (!storage)
                    throw Exception("Table " + table_id.getFullTableName() + " not found while committing parts", ErrorCodes::LOGICAL_ERROR);

                auto * cnch = dynamic_cast<MergeTreeMetaBase *>(storage.get());
                if (!cnch)
                    throw Exception("MergeTree is expected, but got " + storage->getName() + " for " + table_id.getFullTableName(), ErrorCodes::BAD_ARGUMENTS);

                auto parts = createPartVectorFromModels<MutableMergeTreeDataPartCNCHPtr>(*cnch, req->parts(), &req->paths());
                auto staged_parts = createPartVectorFromModels<MutableMergeTreeDataPartCNCHPtr>(*cnch, req->staged_parts(), &req->staged_parts_paths());

                DeleteBitmapMetaPtrVector delete_bitmaps;
                delete_bitmaps.reserve(req->delete_bitmaps_size());
                for (const auto & bitmap_model : req->delete_bitmaps())
                    delete_bitmaps.emplace_back(createFromModel(*cnch, bitmap_model));

                /// check and parse offsets
                String consumer_group;
                cppkafka::TopicPartitionList tpl;
                if (req->has_consumer_group())
                {
                    consumer_group = req->consumer_group();
                    tpl.reserve(req->tpl_size());
                    for (const auto & tp : req->tpl())
                        tpl.emplace_back(cppkafka::TopicPartition(tp.topic(), tp.partition(), tp.offset()));

                    LOG_TRACE(&Poco::Logger::get("CnchServerService"), "parsed tpl to commit with size: {}\n", tpl.size());
                }
                CnchDataWriter cnch_writer(
                    *cnch,
                    rpc_context,
                    ManipulationType(req->type()),
                    req->task_id(),
                    std::move(consumer_group),
                    tpl);

                TxnTimestamp commit_time
                    = cnch_writer.commitPreparedCnchParts(DumpedData{std::move(parts), std::move(delete_bitmaps), std::move(staged_parts)});

                rsp->set_commit_timestamp(commit_time);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(rsp->mutable_exception());
            }
        }
    );
}

void CnchServerServiceImpl::getMinActiveTimestamp(
    [[maybe_unused]] google::protobuf::RpcController * cntl,
    [[maybe_unused]] const Protos::GetMinActiveTimestampReq * request,
    [[maybe_unused]] Protos::GetMinActiveTimestampResp * response,
    [[maybe_unused]] google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, global_context = getContext(), log = log] {
            brpc::ClosureGuard done_guard(done);
            try
            {
                auto & txn_coordinator = global_context->getCnchTransactionCoordinator();
                auto storage_id = RPCHelpers::createStorageID(request->storage_id());
                if (auto timestamp = txn_coordinator.getMinActiveTimestamp(storage_id))
                    response->set_timestamp(*timestamp);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );
}


void CnchServerServiceImpl::createTransaction(
    google::protobuf::RpcController * cntl,
    const Protos::CreateTransactionReq * request,
    Protos::CreateTransactionResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();
    RPCHelpers::serviceHandler(
        done, response, [cntl = cntl, request = request, response = response, done = done, &global_context = *context_ptr, log = log] {
            brpc::ClosureGuard done_guard(done);
        // TODO: Use heartbeat to ensure transaction is still being used
        try
        {
            TxnTimestamp primary_txn_id = request->has_primary_txn_id() ? TxnTimestamp(request->primary_txn_id()) : TxnTimestamp(0);
            CnchTransactionInitiator initiator
                = request->has_primary_txn_id() ? CnchTransactionInitiator::Txn : CnchTransactionInitiator::Worker;
            auto transaction
                = global_context.getCnchTransactionCoordinator().createTransaction(CreateTransactionOption()
                                                                                        .setPrimaryTransactionId(primary_txn_id)
                                                                                        .setType(CnchTransactionType::Implicit)
                                                                                        .setInitiator(initiator));
            auto & controller = static_cast<brpc::Controller &>(*cntl);
            transaction->setCreator(butil::endpoint2str(controller.remote_side()).c_str());

            response->set_txn_id(transaction->getTransactionID());
            response->set_start_time(transaction->getStartTime());

            LOG_TRACE(log, "Create transaction by request: {}\n", transaction->getTransactionID());
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::finishTransaction(
    google::protobuf::RpcController *  /*cntl*/,
    const Protos::FinishTransactionReq * request,
    Protos::FinishTransactionResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();
    RPCHelpers::serviceHandler(
        done, response, [request = request, response = response, done = done, &global_context = *context_ptr, log = log] {
        brpc::ClosureGuard done_guard(done);

        try
        {
            global_context.getCnchTransactionCoordinator().finishTransaction(request->txn_id());

            LOG_TRACE(log, "Finish transaction by request: {}\n", request->txn_id());
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}


void CnchServerServiceImpl::commitTransaction(
    google::protobuf::RpcController *  /*cntl*/,
    const Protos::CommitTransactionReq * request,
    Protos::CommitTransactionResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();

    RPCHelpers::serviceHandler(
        done, response, [request = request, response = response, done = done, &global_context = *context_ptr, log = log] {
            brpc::ClosureGuard done_guard(done);
            response->set_commit_ts(0);

            try
            {
                /// Check validity of consumer before committing parts & offsets in case a new consumer has been scheduled
                if (request->has_kafka_storage_id())
                {
                    auto storage_id = RPCHelpers::createStorageID(request->kafka_storage_id());
                    auto bgthread = global_context.getCnchBGThread(CnchBGThreadType::Consumer, storage_id);
                    auto *manager = dynamic_cast<CnchKafkaConsumeManager *>(bgthread.get());

                    if (!manager->checkWorkerClient(storage_id.getTableName(), request->kafka_consumer_index()))
                        throw Exception(
                            "check validity of worker client for " + storage_id.getFullTableName() + " failed",
                            ErrorCodes::CNCH_KAFKA_TASK_NEED_STOP);
                    LOG_TRACE(
                        log,
                        "Check consumer {} OK. Now commit parts and offsets for Kafka transaction\n",storage_id.getFullTableName());
                }

                auto & txn_coordinator = global_context.getCnchTransactionCoordinator();
                auto txn_id = request->txn_id();
                auto txn = txn_coordinator.getTransaction(txn_id);

                if (request->has_insertion_label())
                {
                    if (UUIDHelpers::Nil == txn->getMainTableUUID())
                        throw Exception("Main table is not set when using insertion label", ErrorCodes::LOGICAL_ERROR);

                    txn->setInsertionLabel(std::make_shared<InsertionLabel>(
                        txn->getMainTableUUID(), request->insertion_label(), txn->getTransactionID().toUInt64()));
                }

                auto commit_ts = txn->commit();
                response->set_commit_ts(commit_ts.toUInt64());

                LOG_TRACE(log, "Committed transaction from worker side: {}\n", request->txn_id());
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        });
}
void CnchServerServiceImpl::precommitTransaction(
    google::protobuf::RpcController * /*cntl*/,
    const Protos::PrecommitTransactionReq * request,
    Protos::PrecommitTransactionResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();

    RPCHelpers::serviceHandler(
        done, response, [request = request, response = response, done = done, &global_context = *context_ptr, log = log] {
            brpc::ClosureGuard done_guard(done);
        try
        {
            auto & txn_coordinator = global_context.getCnchTransactionCoordinator();
            auto txn = txn_coordinator.getTransaction(request->txn_id());
            auto main_table_uuid = RPCHelpers::createUUID(request->main_table_uuid());
            // If main table uuid is not set, set it. Otherwise, skip it
            if (txn->getMainTableUUID() == UUIDHelpers::Nil)
                txn->setMainTableUUID(main_table_uuid);
            txn->precommit();
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::rollbackTransaction(
    google::protobuf::RpcController * /*cntl*/,
    const Protos::RollbackTransactionReq * request,
    Protos::RollbackTransactionResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();
    RPCHelpers::serviceHandler(
        done, response, [request = request, response = response, done = done, &global_context = *context_ptr, log = log] {
            brpc::ClosureGuard done_guard(done);
        try
        {
            auto & txn_coordinator = global_context.getCnchTransactionCoordinator();
            auto txn = txn_coordinator.getTransaction(request->txn_id());
            if (request->has_only_clean_data() && request->only_clean_data())
                txn->removeIntermediateData();
            else
                response->set_commit_ts(txn->rollback());
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::createTransactionForKafka(
    google::protobuf::RpcController * cntl,
    const Protos::CreateKafkaTransactionReq * request,
    Protos::CreateKafkaTransactionResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();
    RPCHelpers::serviceHandler(
        done, response, [cntl = cntl, request = request, response = response, done = done, &global_context = *context_ptr, log = log] {
            brpc::ClosureGuard done_guard(done);

        try
        {
            auto uuid = UUIDHelpers::UUIDToString(RPCHelpers::createUUID(request->uuid()));
            auto storage = global_context.getCnchCatalog()->getTableByUUID(global_context, uuid, TxnTimestamp::maxTS());
            auto bgthread = global_context.getCnchBGThread(CnchBGThreadType::Consumer, storage->getStorageID());
            auto * manager = dynamic_cast<CnchKafkaConsumeManager *>(bgthread.get());

            if (!manager->checkWorkerClient(request->table_name(), request->consumer_index()))
                throw Exception("check validity of worker client for " + request->table_name() + " failed", ErrorCodes::LOGICAL_ERROR);

            auto transaction = global_context.getCnchTransactionCoordinator().createTransaction(
                CreateTransactionOption().setInitiator(CnchTransactionInitiator::Kafka));
            auto & controller = static_cast<brpc::Controller &>(*cntl);
            transaction->setCreator(butil::endpoint2str(controller.remote_side()).c_str());

            response->set_txn_id(transaction->getTransactionID());
            response->set_start_time(transaction->getStartTime());
            LOG_TRACE(log, "Create transaction by request: {}\n", transaction->getTransactionID().toUInt64());
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }

    });
}

void CnchServerServiceImpl::getTransactionStatus(
    ::google::protobuf::RpcController * /*cntl*/,
    const ::DB::Protos::GetTransactionStatusReq * request,
    ::DB::Protos::GetTransactionStatusResp * response,
    ::google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, &global_context = *context_ptr, log = log]
        {
            brpc::ClosureGuard done_guard(done);
            try
            {
                TxnTimestamp txn_id{request->txn_id()};
                auto & txn_coordinator = global_context.getCnchTransactionCoordinator();
                CnchTransactionStatus status = txn_coordinator.getTransactionStatus(txn_id);

                if (status == CnchTransactionStatus::Inactive && request->need_search_catalog())
                {
                    auto txn_record = global_context.getCnchCatalog()->tryGetTransactionRecord(txn_id);
                    if (txn_record)
                        status = txn_record->status();
                }

                switch (status)
                {
                    case CnchTransactionStatus::Running:
                        response->set_status(Protos::TransactionStatus::Running);
                        break;
                    case CnchTransactionStatus::Finished:
                        response->set_status(Protos::TransactionStatus::Finished);
                        break;
                    case CnchTransactionStatus::Aborted:
                        response->set_status(Protos::TransactionStatus::Aborted);
                        break;
                    case CnchTransactionStatus::Inactive:
                        response->set_status(Protos::TransactionStatus::Inactive);
                        break;

                    default:
                        throw Exception("Unknown transaction status", ErrorCodes::NOT_IMPLEMENTED);
                }
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );
}

#if defined(__clang__)
    #pragma clang diagnostic push
    #pragma clang diagnostic ignored "-Wunused-parameter"
#else
    #pragma GCC diagnostic push
    #pragma GCC diagnostic ignored "-Wunused-parameter"
#endif
void CnchServerServiceImpl::checkConsumerValidity(
    google::protobuf::RpcController * cntl,
    const Protos::CheckConsumerValidityReq * request,
    Protos::CheckConsumerValidityResp * response,
    google::protobuf::Closure * done)
{

}
void CnchServerServiceImpl::reportTaskHeartbeat(
    google::protobuf::RpcController * cntl,
    const Protos::ReportTaskHeartbeatReq * request,
    Protos::ReportTaskHeartbeatResp * response,
    google::protobuf::Closure * done)
{
}

void CnchServerServiceImpl::reportDeduperHeartbeat(
    google::protobuf::RpcController * cntl,
    const Protos::ReportDeduperHeartbeatReq * request,
    Protos::ReportDeduperHeartbeatResp * response,
    google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        auto cnch_storage_id = RPCHelpers::createStorageID(request->cnch_storage_id());

        if (auto bg_thread = getContext()->tryGetDedupWorkerManager(cnch_storage_id))
        {
            auto worker_table_name = request->worker_table_name();
            auto & manager = static_cast<DedupWorkerManager &>(*bg_thread);

            auto ret = manager.reportHeartbeat(worker_table_name);

            // NOTE: here we send a response back to let the worker know the result.
            response->set_code(static_cast<UInt32>(ret));
            return;
        }
        else
        {
            LOG_WARNING(log, "Failed to get background thread");
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        RPCHelpers::handleException(response->mutable_exception());
    }
    response->set_code(static_cast<UInt32>(DedupWorkerHeartbeatResult::Kill));
}

void CnchServerServiceImpl::fetchDataParts(
    ::google::protobuf::RpcController *,
    const ::DB::Protos::FetchDataPartsReq * request,
    ::DB::Protos::FetchDataPartsResp * response,
    ::google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(done, response, [request = request, response = response, done = done, gc = getContext(), log = log] {
        brpc::ClosureGuard done_guard(done);
        try
        {
            StoragePtr storage
                = gc->getCnchCatalog()->getTable(*gc, request->database(), request->table(), TxnTimestamp{request->table_commit_time()});

            auto calculated_host
                = gc->getCnchTopologyMaster()->getTargetServer(UUIDHelpers::UUIDToString(storage->getStorageUUID()), true).getRPCAddress();
            if (request->remote_host() != calculated_host)
                throw Exception(
                    "Fetch parts failed because of inconsistent view of topology in remote server, remote_host: " + request->remote_host()
                        + ", calculated_host: " + calculated_host,
                    ErrorCodes::LOGICAL_ERROR);

            if (!isLocalServer(calculated_host, std::to_string(gc->getRPCPort())))
                throw Exception(
                    "Fetch parts failed because calculated host (" + calculated_host + ") is not remote server.",
                    ErrorCodes::LOGICAL_ERROR);

            Strings partition_list;
            for (const auto & partition : request->partitions())
                partition_list.emplace_back(partition);

            auto parts = gc->getCnchCatalog()->getServerDataPartsInPartitions(
                storage, partition_list, TxnTimestamp{request->timestamp()}, nullptr);
            auto & mutable_parts = *response->mutable_parts();
            for (const auto & part : parts)
                *mutable_parts.Add() = part->part_model();
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::fetchUniqueTableMeta(
    ::google::protobuf::RpcController * controller,
    const ::DB::Protos::FetchUniqueTableMetaReq * request,
    ::DB::Protos::FetchUniqueTableMetaResp * response,
    ::google::protobuf::Closure * done)
{
}

void CnchServerServiceImpl::getBackgroundThreadStatus(
    google::protobuf::RpcController * cntl,
    const Protos::BackgroundThreadStatusReq * request,
    Protos::BackgroundThreadStatusResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, log = log] {
            brpc::ClosureGuard done_guard(done);

            try
            {
                std::map<StorageID, CnchBGThreadStatus> res;

                auto type = CnchBGThreadType(request->type());
                if (
                    type == CnchBGThreadType::PartGC ||
                    type == CnchBGThreadType::MergeMutate ||
                    type == CnchBGThreadType::Consumer ||
                    type == CnchBGThreadType::DedupWorker ||
                    type == CnchBGThreadType::Clustering)
                {
#if 0
                    auto threads = global_context.getCnchBGThreads(type);
                    res = threads->getStatusMap();
#endif
                }
                else
                {
                    throw Exception("Not support type " + toString(int(request->type())), ErrorCodes::NOT_IMPLEMENTED);
                }

                for (const auto & [storage_id, status] : res)
                {
                    auto * item = response->mutable_status()->Add();
                    RPCHelpers::fillStorageID(storage_id, *(item->mutable_storage_id()));
                    item->set_status(status);
                }
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );
}

void CnchServerServiceImpl::getNumBackgroundThreads(
    google::protobuf::RpcController * cntl,
    const Protos::BackgroundThreadNumReq * request,
    Protos::BackgroundThreadNumResp * response,
    google::protobuf::Closure * done)
{
}
void CnchServerServiceImpl::controlCnchBGThread(
    google::protobuf::RpcController * /*cntl*/,
    const Protos::ControlCnchBGThreadReq * request,
    Protos::ControlCnchBGThreadResp * response,
    google::protobuf::Closure * done)
{
    ContextPtr context_ptr = getContext();
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, & global_context = *context_ptr, log = log] {
            brpc::ClosureGuard done_guard(done);

            try
            {
                auto storage_id = RPCHelpers::createStorageID(request->storage_id());
                auto type = CnchBGThreadType(request->type());
                auto action = CnchBGThreadAction(request->action());
                global_context.controlCnchBGThread(storage_id, type, action);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );

}
void CnchServerServiceImpl::getTablePartitionInfo(
    google::protobuf::RpcController * cntl,
    const Protos::GetTablePartitionInfoReq * request,
    Protos::GetTablePartitionInfoResp * response,
    google::protobuf::Closure * done)
{
}
void CnchServerServiceImpl::getTableInfo(
    google::protobuf::RpcController * cntl,
    const Protos::GetTableInfoReq * request,
    Protos::GetTableInfoResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, gc = getContext(), log = log] {
            brpc::ClosureGuard done_guard(done);

            try
            {
                auto part_cache_manager = gc->getPartCacheManager();
                for (auto & table_id : request->table_ids())
                {
                    UUID uuid(stringToUUID(table_id.uuid()));
                    Protos::DataModelTableInfo * table_info = response->add_table_infos();
                    table_info->set_database(table_id.database());
                    table_info->set_table(table_id.name());
                    table_info->set_last_modification_time(part_cache_manager->getTableLastUpdateTime(uuid));
                    table_info->set_cluster_status(part_cache_manager->getTableClusterStatus(uuid));
                }
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );
}

void CnchServerServiceImpl::invalidateBytepond(
    google::protobuf::RpcController * cntl,
    const Protos::InvalidateBytepondReq * request,
    Protos::InvalidateBytepondResp * response,
    google::protobuf::Closure * done)
{
}
void CnchServerServiceImpl::commitWorkerRPCByKey(
    google::protobuf::RpcController * cntl,
    const Protos::CommitWorkerRPCByKeyReq * request,
    Protos::CommitWorkerRPCByKeyResp * response,
    google::protobuf::Closure * done)
{
}
void CnchServerServiceImpl::cleanTransaction(
    google::protobuf::RpcController * cntl,
    const Protos::CleanTransactionReq * request,
    Protos::CleanTransactionResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, gc = getContext(), log = log] {
            brpc::ClosureGuard done_guard(done);

            auto & txn_cleaner = gc->getCnchTransactionCoordinator().getTxnCleaner();
            TransactionRecord txn_record{request->txn_record()};

            try
            {
                txn_cleaner.cleanTransaction(txn_record);
            }
            catch (...)
            {
                LOG_WARNING(log, "Clean txn record {} failed.", txn_record.toString());
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );
}
void CnchServerServiceImpl::acquireLock(
    google::protobuf::RpcController * cntl,
    const Protos::AcquireLockReq * request,
    Protos::AcquireLockResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(done, response, [req = request, resp = response, d = done, gc = getContext(), logger = log] {
        brpc::ClosureGuard done_guard(d);
        try
        {
            LockInfoPtr info = createLockInfoFromModel(req->lock());
            LockManager::instance().lock(info, *gc);
            resp->set_lock_status(to_underlying(info->status));
        }
        catch (...)
        {
            tryLogCurrentException(logger, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(resp->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::releaseLock(
    google::protobuf::RpcController * cntl,
    const Protos::ReleaseLockReq * request,
    Protos::ReleaseLockResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(done, response, [req = request, resp = response, d = done, logger = log] {
        brpc::ClosureGuard done_guard(d);

        try
        {
            LockInfoPtr info = createLockInfoFromModel(req->lock());
            LockManager::instance().unlock(info);
        }
        catch (...)
        {
            tryLogCurrentException(logger, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(resp->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::reportCnchLockHeartBeat(
    google::protobuf::RpcController * cntl,
    const Protos::ReportCnchLockHeartBeatReq * request,
    Protos::ReportCnchLockHeartBeatResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(done, response, [req = request, resp = response, d = done, logger = log]
    {
        brpc::ClosureGuard done_guard(d);

        try
        {
            auto tp = LockManager::Clock::now() + std::chrono::milliseconds(req->expire_time());
            LockManager::instance().updateExpireTime(req->txn_id(), tp);
        }
        catch (...)
        {
            tryLogCurrentException(logger, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(resp->mutable_exception());
        }
    });
}
void CnchServerServiceImpl::getServerStartTime(
    google::protobuf::RpcController * cntl,
    const Protos::GetServerStartTimeReq * request,
    Protos::GetServerStartTimeResp * response,
    google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);
    response->set_server_start_time(server_start_time);
}

void CnchServerServiceImpl::scheduleGlobalGC(
    google::protobuf::RpcController * cntl,
    const Protos::ScheduleGlobalGCReq * request,
    Protos::ScheduleGlobalGCResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, & global_gc_manager = this->global_gc_manager, log = log] {
            brpc::ClosureGuard done_guard(done);

            std::vector<Protos::DataModelTable> tables (request->tables().begin(), request->tables().end());
            LOG_DEBUG(log, "Receive {} tables from DM, they are", tables.size());

            for (size_t i = 0; i < tables.size(); ++i)
            {
                LOG_DEBUG(log, "table {} : {}.{}", i + 1, tables[i].database(), tables[i].name());
            }

            try
            {
                bool ret = global_gc_manager->schedule(std::move(tables));
                response->set_ret(ret);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                response->set_ret(false);
            }
        }
    );
}

void CnchServerServiceImpl::getNumOfTablesCanSendForGlobalGC(
    google::protobuf::RpcController * cntl,
    const Protos::GetNumOfTablesCanSendForGlobalGCReq * request,
    Protos::GetNumOfTablesCanSendForGlobalGCResp * response,
    google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        response->set_num_of_tables_can_send(GlobalGCHelpers::amountOfWorkCanReceive(global_gc_manager->getMaxThreads(), global_gc_manager->getNumberOfDeletingTables()));
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        response->set_num_of_tables_can_send(0);
    }
}
void CnchServerServiceImpl::getDeletingTablesInGlobalGC(
    google::protobuf::RpcController * cntl,
    const Protos::GetDeletingTablesInGlobalGCReq * request,
    Protos::GetDeletingTablesInGlobalGCResp * response,
    google::protobuf::Closure * done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        auto uuids = global_gc_manager->getDeletingUUIDs();
        for (const auto & uuid : uuids)
        {
            auto * item = response->mutable_uuids()->Add();
            RPCHelpers::fillUUID(uuid, *item);
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void CnchServerServiceImpl::handleRedirectCommitRequest(
    [[maybe_unused]] google::protobuf::RpcController* controller,
    [[maybe_unused]] const Protos::RedirectCommitPartsReq * request,
    [[maybe_unused]] Protos::RedirectCommitPartsResp * response,
    [[maybe_unused]] google::protobuf::Closure * done,
    bool final_commit)
{
    RPCHelpers::serviceHandler(done, response, [request = request, response = response, done = done, final_commit=final_commit, &global_context = *getContext(), log = log] {
        brpc::ClosureGuard done_guard(done);
        try
        {
            String table_uuid = UUIDHelpers::UUIDToString(RPCHelpers::createUUID(request->uuid()));
            StoragePtr storage = global_context.getCnchCatalog()->tryGetTableByUUID(
                global_context, table_uuid, TxnTimestamp::maxTS());

            if (!storage)
                throw Exception("Table with uuid " + table_uuid + " not found.", ErrorCodes::UNKNOWN_TABLE);

            auto * cnch = dynamic_cast<MergeTreeMetaBase *>(storage.get());
            if (!cnch)
                throw Exception("Table is not of MergeTree class", ErrorCodes::BAD_ARGUMENTS);

            auto parts = createPartVectorFromModels<MergeTreeDataPartCNCHPtr>(*cnch, request->parts(), nullptr);
            auto staged_parts = createPartVectorFromModels<MergeTreeDataPartCNCHPtr>(*cnch, request->staged_parts(), nullptr);
            DeleteBitmapMetaPtrVector delete_bitmaps;
            delete_bitmaps.reserve(request->delete_bitmaps_size());
            for (auto & bitmap_model : request->delete_bitmaps())
                delete_bitmaps.emplace_back(createFromModel(*cnch, bitmap_model));


            if (!final_commit)
            {
                TxnTimestamp txnID{request->txn_id()};
                global_context.getCnchCatalog()->writeParts(storage, txnID,
                    Catalog::CommitItems{parts, delete_bitmaps, staged_parts}, request->from_merge_task(), request->preallocate_mode());
            }
            else
            {
                TxnTimestamp commitTs {request->commit_ts()};
                global_context.getCnchCatalog()->setCommitTime(storage, Catalog::CommitItems{parts, delete_bitmaps, staged_parts},
                    commitTs, request->txn_id());
            }
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::redirectCommitParts(
    google::protobuf::RpcController * controller,
    const Protos::RedirectCommitPartsReq * request,
    Protos::RedirectCommitPartsResp * response,
    google::protobuf::Closure * done)
{
    handleRedirectCommitRequest(controller, request, response, done, false);
}
void CnchServerServiceImpl::redirectSetCommitTime(
    google::protobuf::RpcController * controller,
    const Protos::RedirectCommitPartsReq * request,
    Protos::RedirectCommitPartsResp * response,
    google::protobuf::Closure * done)
{
    handleRedirectCommitRequest(controller, request, response, done, true);
}
void CnchServerServiceImpl::removeMergeMutateTasksOnPartition(
    google::protobuf::RpcController * cntl,
    const Protos::RemoveMergeMutateTasksOnPartitionReq * request,
    Protos::RemoveMergeMutateTasksOnPartitionResp * response,
    google::protobuf::Closure * done)
{
}

void CnchServerServiceImpl::submitQueryWorkerMetrics(
    google::protobuf::RpcController * /*cntl*/,
    const Protos::SubmitQueryWorkerMetricsReq * request,
    Protos::SubmitQueryWorkerMetricsResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(
        done,
        response,
        [request = request, response = response, done = done, gc = getContext(), log = log] {
            brpc::ClosureGuard done_guard(done);

            try
            {
                auto query_worker_metric_element = createQueryWorkerMetricElement(request->element());
                gc->insertQueryWorkerMetricsElement(query_worker_metric_element);

                LOG_TRACE(log, "Submit query worker metrics [{}] from {}: {}", query_worker_metric_element.current_query_id,
                    query_worker_metric_element.worker_id, query_worker_metric_element.query);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                RPCHelpers::handleException(response->mutable_exception());
            }
        }
    );
}

void CnchServerServiceImpl::submitPreloadTask(
    google::protobuf::RpcController * cntl,
    const Protos::SubmitPreloadTaskReq * request,
    Protos::SubmitPreloadTaskResp * response,
    google::protobuf::Closure * done)
{
    RPCHelpers::serviceHandler(done, response, [c = cntl, request, response, done, gc = getContext(), log = log] {
        brpc::ClosureGuard done_guard(done);

        try
        {
            auto rpc_context = RPCHelpers::createSessionContextForRPC(gc, *c);
            const TxnTimestamp txn_id = rpc_context->getTimestamp();
            rpc_context->setTemporaryTransaction(txn_id, {}, /*check catalog*/ false);

            String table_uuid = UUIDHelpers::UUIDToString(RPCHelpers::createUUID(request->uuid()));
            auto storage = rpc_context->getCnchCatalog()->tryGetTableByUUID(*rpc_context, table_uuid, TxnTimestamp::maxTS());
            if (!storage)
                throw Exception(ErrorCodes::UNKNOWN_TABLE, "Table with uuid {} not found.", table_uuid);

            auto * cnch = dynamic_cast<StorageCnchMergeTree *>(storage.get());
            if (!cnch)
                throw Exception(ErrorCodes::BAD_ARGUMENTS, "Table is not of MergeTree class");

            ServerDataPartsVector parts = createServerPartsFromModels(*cnch, request->parts());
            if (parts.empty())
                return;

            cnch->sendPreloadTasks(rpc_context, std::move(parts), request->sync());
        }
        catch (...)
        {
            tryLogCurrentException(log);
            RPCHelpers::handleException(response->mutable_exception());
        }
    });
}

void CnchServerServiceImpl::executeOptimize(
    google::protobuf::RpcController *,
    const Protos::ExecuteOptimizeQueryReq * request,
    Protos::ExecuteOptimizeQueryResp * response,
    google::protobuf::Closure *done)
{
    brpc::ClosureGuard done_guard(done);

    try
    {
        auto enable_try = request->enable_try();
        const auto & partition_id = request->partition_id();

        auto storage_id = RPCHelpers::createStorageID(request->storage_id());
        auto bg_thread = getContext()->getCnchBGThread(CnchBGThreadType::MergeMutate, storage_id);

        auto & database_catalog = DatabaseCatalog::instance();
        auto istorage = database_catalog.getTable(storage_id, getContext());

        auto * merge_mutate_thread = dynamic_cast<CnchMergeMutateThread *>(bg_thread.get());
        auto task_id = merge_mutate_thread->triggerPartMerge(istorage, partition_id, false, enable_try, false);
        if (request->mutations_sync())
        {
            auto timeout = request->has_timeout_ms() ? request->timeout_ms() : 0;
            merge_mutate_thread->waitTasksFinish({task_id}, timeout);
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        RPCHelpers::handleException(response->mutable_exception());
    }
}

#if defined(__clang__)
    #pragma clang diagnostic pop
#else
    #pragma GCC diagnostic pop
#endif

}
