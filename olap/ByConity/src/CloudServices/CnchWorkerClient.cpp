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

#include <CloudServices/CnchWorkerClient.h>

#include <Interpreters/Context.h>
#include <Protos/DataModelHelpers.h>
#include <Protos/RPCHelpers.h>
#include <Protos/cnch_worker_rpc.pb.h>
#include <Storages/IStorage.h>
#include <Transaction/ICnchTransaction.h>
#include <CloudServices/DedupWorkerStatus.h>
#include <WorkerTasks/ManipulationList.h>
#include <WorkerTasks/ManipulationTaskParams.h>

#include <brpc/channel.h>
#include <brpc/controller.h>

namespace DB
{
CnchWorkerClient::CnchWorkerClient(String host_port_)
    : RpcClientBase(getName(), std::move(host_port_)), stub(std::make_unique<Protos::CnchWorkerService_Stub>(&getChannel()))
{
}

CnchWorkerClient::CnchWorkerClient(HostWithPorts host_ports_)
    : RpcClientBase(getName(), std::move(host_ports_)), stub(std::make_unique<Protos::CnchWorkerService_Stub>(&getChannel()))
{
}

CnchWorkerClient::~CnchWorkerClient() = default;

void CnchWorkerClient::submitManipulationTask(
    const MergeTreeMetaBase & storage, const ManipulationTaskParams & params, TxnTimestamp txn_id, TxnTimestamp begin_ts)
{
    if (!params.rpc_port)
        throw Exception("Rpc port is not set in ManipulationTaskParams", ErrorCodes::LOGICAL_ERROR);

    brpc::Controller cntl;
    Protos::SubmitManipulationTaskReq request;
    Protos::SubmitManipulationTaskResp response;

    request.set_txn_id(txn_id);
    request.set_timestamp(begin_ts);
    request.set_type(static_cast<UInt32>(params.type));
    request.set_task_id(params.task_id);
    request.set_rpc_port(params.rpc_port);
    request.set_columns_commit_time(params.columns_commit_time);
    request.set_is_bucket_table(params.is_bucket_table);
    if (!params.create_table_query.empty())
        request.set_create_table_query(params.create_table_query);
    fillPartsModelForSend(storage, params.source_parts, *request.mutable_source_parts());

    if (params.type == ManipulationType::Mutate || params.type == ManipulationType::Clustering)
    {
        request.set_mutation_commit_time(params.mutation_commit_time);
        WriteBufferFromString write_buf(*request.mutable_mutate_commands());
        params.mutation_commands->writeText(write_buf);
    }

    stub->submitManipulationTask(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchWorkerClient::shutdownManipulationTasks(const UUID & table_uuid)
{
    brpc::Controller cntl;
    Protos::ShutdownManipulationTasksReq request;
    Protos::ShutdownManipulationTasksResp response;

    RPCHelpers::fillUUID(table_uuid, *request.mutable_table_uuid());
    stub->shutdownManipulationTasks(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

std::unordered_set<std::string> CnchWorkerClient::touchManipulationTasks(const UUID & table_uuid, const Strings & tasks_id)
{
    brpc::Controller cntl;
    Protos::TouchManipulationTasksReq request;
    Protos::TouchManipulationTasksResp response;

    RPCHelpers::fillUUID(table_uuid, *request.mutable_table_uuid());

    for (const auto & t : tasks_id)
        request.add_tasks_id(t);

    stub->touchManipulationTasks(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    return {response.tasks_id().begin(), response.tasks_id().end()};
}

std::vector<ManipulationInfo> CnchWorkerClient::getManipulationTasksStatus()
{
    brpc::Controller cntl;
    Protos::GetManipulationTasksStatusReq request;
    Protos::GetManipulationTasksStatusResp response;

    stub->getManipulationTasksStatus(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);

    std::vector<ManipulationInfo> res;
    for (const auto & task : response.tasks())
    {
        ManipulationInfo info(RPCHelpers::createStorageID(task.storage_id()));
        info.type = ManipulationType(task.type());
        info.related_node = this->getRPCAddress();
        info.elapsed = task.elapsed();
        info.num_parts = task.num_parts();
        for (const auto & source_part_name : task.source_part_names())
            info.source_part_names.emplace_back(source_part_name);
        for (const auto & result_part_name : task.result_part_names())
            info.result_part_names.emplace_back(result_part_name);
        info.partition_id = task.partition_id();
        info.total_size_bytes_compressed = task.total_size_bytes_compressed();
        info.total_size_marks = task.total_size_marks();
        info.progress = task.progress();
        info.bytes_read_uncompressed = task.bytes_read_uncompressed();
        info.bytes_written_uncompressed = task.bytes_written_uncompressed();
        info.rows_read = task.rows_read();
        info.rows_written = task.rows_written();
        info.columns_written = task.columns_written();
        info.memory_usage = task.memory_usage();
        info.thread_id = task.thread_id();
        res.emplace_back(info);
    }

    return res;
}

void CnchWorkerClient::sendCreateQueries(const ContextPtr & context, const std::vector<String> & create_queries)
{
    brpc::Controller cntl;
    Protos::SendCreateQueryReq request;
    Protos::SendCreateQueryResp response;

    const auto & settings = context->getSettingsRef();
    auto timeout = settings.max_execution_time.value.totalSeconds();

    request.set_txn_id(context->getCurrentTransactionID());
    request.set_primary_txn_id(context->getCurrentTransaction()->getPrimaryTransactionID()); /// Why?
    request.set_timeout(timeout ? timeout : 3600); // clean session resource if there exists Exception after 3600s

    for (const auto & create_query : create_queries)
        *request.mutable_create_queries()->Add() = create_query;

    stub->sendCreateQuery(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

brpc::CallId CnchWorkerClient::sendCnchHiveDataParts(
    const ContextPtr & context,
    const StoragePtr & storage,
    const String & local_table_name,
    const HiveDataPartsCNCHVector & parts,
    ExceptionHandler & handler)
{
    Protos::SendCnchHiveDataPartsReq request;

    request.set_txn_id(context->getCurrentTransactionID());
    request.set_database_name(storage->getDatabaseName());
    request.set_table_name(local_table_name);
    fillCnchHivePartsModel(parts, *request.mutable_parts());

    auto * cntl = new brpc::Controller();
    Protos::SendCnchHiveDataPartsResp * response = new Protos::SendCnchHiveDataPartsResp();
    const auto & settings = context->getSettingsRef();
    auto send_timeout = std::max(settings.max_execution_time.value.totalMilliseconds() >> 1, 30 * 1000L);
    cntl->set_timeout_ms(send_timeout);

    auto call_id = cntl->call_id();
    stub->sendCnchHiveDataParts(cntl, &request, response, brpc::NewCallback(RPCHelpers::onAsyncCallDone, response, cntl, &handler));

    return call_id;
}

brpc::CallId CnchWorkerClient::preloadDataParts(
    const ContextPtr & context,
    const TxnTimestamp & txn_id,
    const IStorage & storage,
    const String & create_local_table_query,
    const ServerDataPartsVector & parts,
    bool sync,
    ExceptionHandler & handler)
{
    Protos::PreloadDataPartsReq request;
    request.set_txn_id(txn_id);
    request.set_create_table_query(create_local_table_query);
    request.set_sync(sync);
    fillPartsModelForSend(storage, parts, *request.mutable_parts());

    auto * cntl = new brpc::Controller();
    auto * response = new Protos::PreloadDataPartsResp();
    /// adjust the timeout to prevent timeout if there are too many parts to send,
    const auto & settings = context->getSettingsRef();
    auto send_timeout = std::max(settings.max_execution_time.value.totalMilliseconds() >> 1, 30 * 1000L);
    cntl->set_timeout_ms(send_timeout);

    auto call_id = cntl->call_id();
    stub->preloadDataParts(cntl, &request, response, brpc::NewCallback(RPCHelpers::onAsyncCallDone, response, cntl, &handler));
    return call_id;
}

brpc::CallId CnchWorkerClient::sendQueryDataParts(
    const ContextPtr & context,
    const StoragePtr & storage,
    const String & local_table_name,
    const ServerDataPartsVector & data_parts,
    const std::set<Int64> & required_bucket_numbers,
    ExceptionHandler & handler)
{
    Protos::SendDataPartsReq request;
    request.set_txn_id(context->getCurrentTransactionID());
    request.set_database_name(storage->getDatabaseName());
    request.set_table_name(local_table_name);
    request.set_disk_cache_mode(context->getSettingsRef().disk_cache_mode.toString());

    fillBasePartAndDeleteBitmapModels(*storage, data_parts, *request.mutable_parts(), *request.mutable_bitmaps());
    for (const auto & bucket_num : required_bucket_numbers)
        *request.mutable_bucket_numbers()->Add() = bucket_num;

    // TODO:
    // auto udf_info = context.getNonSqlUdfVersionMap();
    // for (const auto & [name, version]: udf_info)
    // {
    //     auto & new_info = *request.mutable_udf_infos()->Add();
    //     new_info.set_function_name(name);
    //     new_info.set_version(version);
    // }


    auto * cntl = new brpc::Controller();
    auto * response = new Protos::SendDataPartsResp();
    /// adjust the timeout to prevent timeout if there are too many parts to send,
    const auto & settings = context->getSettingsRef();
    auto send_timeout = std::max(settings.max_execution_time.value.totalMilliseconds() >> 1, 30 * 1000L);
    cntl->set_timeout_ms(send_timeout);

    auto call_id = cntl->call_id();
    stub->sendQueryDataParts(cntl, &request, response, brpc::NewCallback(RPCHelpers::onAsyncCallDone, response, cntl, &handler));

    return call_id;
}

brpc::CallId CnchWorkerClient::sendOffloadingInfo(
    [[maybe_unused]] const ContextPtr & context,
    [[maybe_unused]] const HostWithPortsVec & read_workers,
    [[maybe_unused]] const std::vector<std::pair<StorageID, String>> & worker_table_names,
    [[maybe_unused]] const std::vector<HostWithPortsVec> & buffer_workers_vec,
    [[maybe_unused]] ExceptionHandler & handler)
{
    /// TODO:
    return {};
}

void CnchWorkerClient::removeWorkerResource(TxnTimestamp txn_id)
{
    brpc::Controller cntl;
    Protos::RemoveWorkerResourceReq request;
    Protos::RemoveWorkerResourceResp response;

    request.set_txn_id(txn_id);
    stub->removeWorkerResource(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchWorkerClient::createDedupWorker(const StorageID & storage_id, const String & create_table_query, const HostWithPorts & host_ports_)
{
    brpc::Controller cntl;
    Protos::CreateDedupWorkerReq request;
    Protos::CreateDedupWorkerResp response;

    RPCHelpers::fillStorageID(storage_id, *request.mutable_table());
    request.set_create_table_query(create_table_query);
    RPCHelpers::fillHostWithPorts(host_ports_, *request.mutable_host_ports());

    stub->createDedupWorker(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);
}

void CnchWorkerClient::dropDedupWorker(const StorageID & storage_id)
{
    brpc::Controller cntl;
    Protos::DropDedupWorkerReq request;
    Protos::DropDedupWorkerResp response;

    RPCHelpers::fillStorageID(storage_id, *request.mutable_table());

    stub->dropDedupWorker(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);

}

DedupWorkerStatus CnchWorkerClient::getDedupWorkerStatus(const StorageID & storage_id)
{
    brpc::Controller cntl;
    Protos::GetDedupWorkerStatusReq request;
    Protos::GetDedupWorkerStatusResp response;
    RPCHelpers::fillStorageID(storage_id, *request.mutable_table());

    stub->getDedupWorkerStatus(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);

    DedupWorkerStatus status;
    status.is_active = response.is_active();
    if (status.is_active)
    {
        status.create_time = response.create_time();
        status.total_schedule_cnt = response.total_schedule_cnt();
        status.total_dedup_cnt = response.total_dedup_cnt();
        status.last_schedule_wait_ms = response.last_schedule_wait_ms();
        status.last_task_total_cost_ms = response.last_task_total_cost_ms();
        status.last_task_dedup_cost_ms = response.last_task_dedup_cost_ms();
        status.last_task_publish_cost_ms = response.last_task_publish_cost_ms();
        status.last_task_staged_part_cnt = response.last_task_staged_part_cnt();
        status.last_task_visible_part_cnt = response.last_task_visible_part_cnt();
        status.last_task_staged_part_total_rows = response.last_task_staged_part_total_rows();
        status.last_task_visible_part_total_rows = response.last_task_visible_part_total_rows();
        status.last_exception = response.last_exception();
        status.last_exception_time = response.last_exception_time();
    }
    return status;
}

#if USE_RDKAFKA
CnchConsumerStatus CnchWorkerClient::getConsumerStatus(const StorageID & storage_id)
{
    brpc::Controller cntl;
    Protos::GetConsumerStatusReq request;
    Protos::GetConsumerStatusResp response;
    RPCHelpers::fillStorageID(storage_id, *request.mutable_table());

    stub->getConsumerStatus(&cntl, &request, &response, nullptr);
    assertController(cntl);
    RPCHelpers::checkResponse(response);

    CnchConsumerStatus status;
    status.cluster = response.cluster();
    for (const auto & topic : response.topics())
        status.topics.emplace_back(topic);
    for (const auto & tpl : response.assignments())
        status.assignment.emplace_back(tpl);
    status.assigned_consumers = response.consumer_num();
    status.last_exception = response.last_exception();

    return status;
}

void CnchWorkerClient::submitKafkaConsumeTask(const KafkaTaskCommand & command)
{
    if (!command.rpc_port)
        throw Exception("Rpc port is not set in KafkaTaskCommand", ErrorCodes::LOGICAL_ERROR);

    brpc::Controller cntl;
    Protos::SubmitKafkaConsumeTaskReq request;
    Protos::SubmitKafkaConsumeTaskResp response;

    request.set_type(command.type);
    request.set_task_id(command.task_id);
    request.set_rpc_port(command.rpc_port);
    request.set_database(command.local_database_name);
    request.set_table(command.local_table_name);
    request.set_assigned_consumer(command.assigned_consumer);
    for (const auto & cmd : command.create_table_commands)
    {
        request.add_create_table_command(cmd);
    }
    for (const auto & tpl : command.tpl)
    {
        auto * cur_tpl = request.add_tpl();
        cur_tpl->set_topic(toString(tpl.get_topic()));
        cur_tpl->set_partition(tpl.get_partition());
        cur_tpl->set_offset(tpl.get_offset());
    }
    if (command.type == KafkaTaskCommand::START_CONSUME)
    {
        RPCHelpers::fillStorageID(command.cnch_storage_id, *request.mutable_cnch_storage_id());
    }

    stub->submitKafkaConsumeTask(&cntl, &request, &response, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(response);
}
#endif

}
