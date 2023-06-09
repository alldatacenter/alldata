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

#include <Common/config.h>
#if USE_RDKAFKA

#include <Storages/Kafka/StorageCloudKafka.h>

#include <Common/Exception.h>
#include <Common/Configurations.h>
#include <Common/Macros.h>
#include <Common/escapeForFileName.h>
#include <Common/SettingsChanges.h>
#include <Core/Settings.h>
#include <DataStreams/CountingBlockOutputStream.h>
#include <Interpreters/Context.h>
#include <Interpreters/DatabaseCatalog.h>
#include <Interpreters/loadMetadata.h>
#include <Interpreters/CnchSystemLog.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <Interpreters/InterpreterDropQuery.h>
#include <Parsers/ParserDropQuery.h>
#include <Parsers/parseQuery.h>
#include <Storages/Kafka/CnchKafkaBlockInputStream.h>
#include <Storages/Kafka/KafkaCommon.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/StorageMaterializedView.h>
#include <Transaction/CnchWorkerTransaction.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int CNCH_KAFKA_TASK_NEED_STOP;
    extern const int RDKAFKA_EXCEPTION;
}

namespace
{
    const auto STREAM_RESCHEDULE_MS = 100;

    const size_t CHECK_STAGED_AREA_RESCHEDULE_MIN_MS = 100;
    const size_t CHECK_STAGED_AREA_RESCHEDULE_STEP_MS = 100;
    const size_t CHECK_STAGED_AREA_RESCHEDULE_MAX_MS = 2000;
}

StorageCloudKafka::StorageCloudKafka
    (const StorageID &table_id_,
     ContextMutablePtr context_,
     const ColumnsDescription &columns_,
     const ConstraintsDescription &constraints_,
     const ASTPtr setting_changes_,
     const KafkaSettings &settings_,
     const String &server_client_host_,
     UInt16 server_client_rpc_port_)
     : IStorageCnchKafka(table_id_, context_, setting_changes_, settings_, columns_, constraints_),
     settings_adjustments(createSettingsAdjustments()),
     server_client_address(HostWithPorts::fromRPCAddress(addBracketsIfIpv6(server_client_host_) + ':' + toString(server_client_rpc_port_))),
       log(&Poco::Logger::get(table_id_.getNameForLogs()  + " (StorageCloudKafka)")),
       ////check_staged_area_task(context_->getCheckStagedAreaSchedulePool().createTask(log->name(), [this] { checkStagedArea(); })),
       check_staged_area_reschedule_ms(CHECK_STAGED_AREA_RESCHEDULE_MIN_MS)
{
    if (server_client_address.getHost().empty() || server_client_address.rpc_port == 0)
        throw Exception("Invalid server client " + server_client_address.getRPCAddress()
                        + " for kafka consumer " + getStorageID().getNameForLogs(), ErrorCodes::BAD_ARGUMENTS);
}

StorageCloudKafka::~StorageCloudKafka()
{
    try
    {
        auto & schema_path = settings.format_schema_path.value;
        if (!schema_path.empty() && !Kafka::startsWithHDFSOrCFS(schema_path) && Poco::File(schema_path).exists())
        {
            LOG_DEBUG(log, "Remove local schema file path: {}", schema_path);
            Poco::File(schema_path).remove(true);
        }

        if (stream_run)
            shutdown();
    }
    catch (...)
    {
        tryLogCurrentException(log, "~StorageCloudKafka");
    }
}

void StorageCloudKafka::startup()
{
    tryLoadFormatSchemaFileFromHDFS();
}

void StorageCloudKafka::shutdown()
{
    stopConsume();
}

void StorageCloudKafka::tryLoadFormatSchemaFileFromHDFS()
{
    if (settings.format_schema_path.value.empty())
        return;

    auto path = settings.format_schema_path.value;
    if (Kafka::startsWithHDFSOrCFS(path))
    {
        auto local_format_path = getContext()->getFormatSchemaPath(false);
        if (local_format_path.empty())
            throw Exception("Default local path for format schema is not set", ErrorCodes::LOGICAL_ERROR);

        /// XXX: maybe we can optimize to dump once for multi-consumers of the same kafka-table on the same worker
        const String path_suffix = escapeForFileName(getTableName());
        local_format_path = local_format_path + "/" + path_suffix;
        Poco::File(local_format_path).createDirectories();

        try
        {
            /// XXX: As the schema file may import other pb files, we will load the whole directory now
            /// We recommend that each kafka table has their own hdfs path to store PB files they need
            size_t pos = settings.schema.value.find(':');
            if (pos == String::npos || pos == 0 || pos == settings.schema.value.length() - 1)
                throw Exception("Format schema should have the 'schema_file:message_name' format, got "
                                + settings.schema.value, ErrorCodes::BAD_ARGUMENTS);

            Poco::Path target_path;
            target_path.assign(settings.schema.value.substr(0, pos)).makeFile();
            if (target_path.getExtension().empty()) {
                if (settings.format.value == "Protobuf")
                    target_path.setExtension("proto");
                else
                    throw Exception(settings.format.value + " is not supported to dump schema file from hdfs now",
                                    ErrorCodes::LOGICAL_ERROR);
            }

            LOG_DEBUG(log, "Loading format schema files from remote path: {} to local path: {}", path, local_format_path);
            reloadFormatSchema(path, local_format_path, log);

            /// Check the existence of the file to ensure loading successfully
            if (!Poco::File(local_format_path + "/" + target_path.getFileName()).exists())
                throw Exception("Cannot get target file " + target_path.getFileName() + " from remote path: " + path,
                                ErrorCodes::LOGICAL_ERROR);

            settings.format_schema_path.value = local_format_path;
        }
        catch (...)
        {
            Poco::File(local_format_path).remove(true);
            throw;
        }
    }
}

BufferPtr StorageCloudKafka::tryClaimBuffer(long wait_ms)
{
    auto lock_result = consumer_context.mutex->try_lock_for(std::chrono::milliseconds(wait_ms));
    if (!lock_result)
        throw Exception("Failed to claim consumer buffer #" + toString(assigned_consumer_index), ErrorCodes::LOGICAL_ERROR);

    /// reset buffer if dc changes
    if (consumer_context.error_event
        || (consumer_context.buffer &&
            consumer_context.buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>()->getConsumer()->check_destroyed()))
    {
        LOG_INFO(log, "Handle of Consumer #{} has been destroyed, reset it.", assigned_consumer_index);
        consumer_context.buffer.reset();
        consumer_context.error_event = false;
    }
    /// Create consumer buffer if necessary
    if (!consumer_context.buffer)
    {
        try
        {
            consumer_context.buffer = createBuffer();
        }
        catch (...)
        {
            consumer_context.mutex->unlock();
            throw;
        }
    }
    return consumer_context.buffer;
}

void StorageCloudKafka::pushBuffer()
{
    consumer_context.mutex->unlock();
}

void StorageCloudKafka::subscribeBuffer(BufferPtr &buffer)
{
    auto *consumer_buf = buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>();
    consumer_buf->assign(consumer_context.assignment);

    consumer_buf->reset();
}

void StorageCloudKafka::unsubscribeBuffer(BufferPtr &buffer)
{
    if (buffer)
        buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>()->unassign();
}

cppkafka::Configuration StorageCloudKafka::createConsumerConfiguration()
{
    /// Create consumer conf
    cppkafka::Configuration conf = Kafka::createConsumerConfiguration(getContext(), getStorageID(), topics, settings);
    return conf;
}

BufferPtr StorageCloudKafka::createBuffer()
{
    auto consumer = std::make_shared<KafkaConsumer>(createConsumerConfiguration());

    using namespace std::chrono_literals;
    consumer->set_timeout(5s);

    std::ostringstream logger_name;
    logger_name << getDatabaseName() << '.' << getTableName() + " (Consumer #" << assigned_consumer_index << ')';

    size_t batch_size = settings.max_block_size.value;
    size_t poll_timeout = getContext()->getRootConfig().stream_poll_timeout_ms.safeGet();
    size_t expire_timeout = settings.max_poll_interval_ms.totalMilliseconds();

    return std::make_shared<DelimitedReadBuffer>(
            std::make_unique<CnchReadBufferFromKafkaConsumer>(
                    consumer, logger_name.str(), batch_size, poll_timeout, expire_timeout, &stream_run),
            settings.row_delimiter);
}

void StorageCloudKafka::createStreamThread(const cppkafka::TopicPartitionList & assignment)
{
    if (consumer_context.initialized)
    {
        LOG_WARNING(log, "Stream thread has been created");
        return;
    }

    LOG_TRACE(log, "Creating stream thread and buffer");
    consumer_context.mutex = std::make_unique<std::timed_mutex>();
    consumer_context.assignment = assignment;
    auto task = getContext()->getConsumeSchedulePool().createTask(
            log->name(),[this] { streamThread(); });
    task->deactivate();
    consumer_context.task = std::move(task);

    consumer_context.buffer = createBuffer();
    consumer_context.initialized = true;
}

void StorageCloudKafka::checkStagedArea()
{
    try
    {
        if (!checkDependencies(getDatabaseName(), getTableName(), /**check staged area**/true))
            LOG_ERROR(log, "Check dependencies failed when checking for staged area.");
    }
    catch(...)
    {
        LOG_ERROR(log, "Check dependencies failed when checking for staged area.");
    }

    if (stream_run && cloud_table_has_unique_key)
    {
        if (!wait_for_staged_parts_to_publish)
            check_staged_area_reschedule_ms = std::min(CHECK_STAGED_AREA_RESCHEDULE_MAX_MS, check_staged_area_reschedule_ms + CHECK_STAGED_AREA_RESCHEDULE_STEP_MS);
        else
            check_staged_area_reschedule_ms = CHECK_STAGED_AREA_RESCHEDULE_MIN_MS;
        ///check_staged_area_task->scheduleAfter(check_staged_area_reschedule_ms);
    }
}

void StorageCloudKafka::startConsume(size_t consumer_index, const cppkafka::TopicPartitionList & tpl)
{
    std::lock_guard lock_thread(table_status_mutex);

    assigned_consumer_index = consumer_index;
    createStreamThread(tpl);

    LOG_TRACE(log, "Activating stream thread");
    stream_run = true;

    ///check_staged_area_task->activateAndSchedule();
    consumer_context.task->activateAndSchedule();
}

void StorageCloudKafka::ConsumerContext::reset()
{
    buffer.reset();
    assignment.clear();
    latest_offsets.clear();
    error_event = false;
    initialized = false;
}

void StorageCloudKafka::stopStreamThread()
{
    SCOPE_EXIT({
                   /// Notify other possible stopping threads
                   std::lock_guard lock(table_status_mutex);
                   is_stopping_consume = false;
                   stop_cv.notify_all();
               });

    /// First, check empty with lock to ensure only execute once
    {
        std::unique_lock lock_thread(table_status_mutex);
        stop_cv.wait(lock_thread, [this] { return !is_stopping_consume; });

        is_stopping_consume = true;
        if (!consumer_context.initialized)
            return;
    }

    LOG_TRACE(log, "Deactivating stream thread and destroying buffer");

    /// Second, stop task: do not add lock here
    /// If exception occurs when stopping task, it needs to add lock to update some info,
    /// so if locked here, the dead lock would happen
    unsubscribeBuffer(consumer_context.buffer);
    consumer_context.task->deactivate();

    /// Then reset `consumer_context`
    {
        std::lock_guard lock_thread(table_status_mutex);
        consumer_context.reset();
    }

    ///if (check_staged_area_task.hasTask())
    ///   check_staged_area_task->deactivate();
}

void StorageCloudKafka::stopConsume()
{
    stream_run = false;
    stopStreamThread();
}

void StorageCloudKafka::streamThread()
{
    auto process_exception = [this] () {
        LOG_ERROR(this->log, "Stream thread failed: {}", getCurrentExceptionMessage(true));

        if (auto kafka_log = getContext()->getKafkaLog())
        {
            auto kafka_error_log = createKafkaLog(KafkaLogElement::EXCEPTION, assigned_consumer_index);
            kafka_error_log.has_error = true;
            kafka_error_log.last_exception = getCurrentExceptionMessage(false);
            kafka_log->add(kafka_error_log);
            if (auto cloud_kafka_log = getContext()->getCloudKafkaLog())
                cloud_kafka_log->add(kafka_error_log);

            std::lock_guard lock(table_status_mutex);
            last_exception = kafka_error_log.last_exception;
        }
    };

    try
    {
        /// auto dependencies = getContext()->getDependencies(getDatabaseName(), getTableName());
        auto dependencies = DatabaseCatalog::instance().getDependencies({getDatabaseName(), getTableName()});

        while (!dependencies.empty())
        {
            if (!checkDependencies(getDatabaseName(), getTableName(), false))
                throw Exception("Check dependencies failed, just restart consumer task", ErrorCodes::CNCH_KAFKA_TASK_NEED_STOP);

            if (wait_for_staged_parts_to_publish)
            {
                LOG_DEBUG(log, "Target table has some parts too old, skip this consume action. ");
                break;
            }

            LOG_DEBUG(log, "Started streaming to {} attached views", dependencies.size());

            if (!streamToViews())
                break;
        }
        rdkafka_exception_times = 0;
    }
    catch (const Exception & e)
    {
        if (e.code() == ErrorCodes::CNCH_KAFKA_TASK_NEED_STOP)
        {
            String database = getDatabaseName();
            String table = getTableName();

            LOG_WARNING(log, "Self-stop consumer {} as exception occurs with {}", table, e.displayText());

            ThreadFromGlobalPool([c = getContext(), db = database, tb = table] {
                try
                {
                    LOG_DEBUG(&Poco::Logger::get("SelfDropKafkaTable"), "Self-drop table: {}.{}", db, tb);
                    /// Copy context in case the global_context would be invalid if the consumer is dropped
                    auto drop_context = Context::createCopy(c);
                    dropConsumerTables(drop_context, db, tb);
                }
                catch (...)
                {
                    tryLogCurrentException(__PRETTY_FUNCTION__);
                }
            }).detach();
            return;
        }
        else if (e.code() == ErrorCodes::RDKAFKA_EXCEPTION)
        {
            rdkafka_exception_times = std::min(10ul, rdkafka_exception_times + 1);
        }

        process_exception();
    }
    catch(...)
    {
        process_exception();
    }

    if (stream_run)
        consumer_context.task->scheduleAfter(STREAM_RESCHEDULE_MS * (1 << rdkafka_exception_times));
}

bool StorageCloudKafka::streamToViews(/* required_column_names */)
{
    /// XXX: multiple tables of CnchKafka may have the same UUID, so just create StorageID with database and table name
    auto table = DatabaseCatalog::instance().getTable({getDatabaseName(), getTableName()}, getContext());
    if (!table) {
        throw Exception("Engine table " + getDatabaseName() + "." + getTableName() + " doesn't exist.", ErrorCodes::LOGICAL_ERROR);
    }

    /// create INSERT query
    auto insert = std::make_shared<ASTInsertQuery>();
    insert->table_id = {getDatabaseName(), getTableName()};
    /// insert->no_destination = true; // Only insert into dependent views
    ///insert->columns = std::make_shared<ASTExpressionList>();
    ///for (const auto & name : required_column_names)
    ///{
    ///    insert->columns->children.emplace_back(std::make_shared<ASTIdentifier>(name));
    ///}

    const Settings & global_settings = getContext()->getSettingsRef();
    size_t block_size = settings.max_block_size.value;
    if (block_size == 0)
        block_size = global_settings.max_block_size.value;

    auto consume_context = Context::createCopy(getContext());
    consume_context->makeQueryContext();
    //consume_context.setSetting("min_insert_block_size_rows", UInt64(1));
    consume_context->applySettingsChanges(settings_adjustments);

    consume_context->setSessionContext(consume_context);

    auto server_client = consume_context->getCnchServerClient(server_client_address);
    if (!server_client)
        throw Exception("Cannot get server client while copy data to server", ErrorCodes::CNCH_KAFKA_TASK_NEED_STOP);

    try
    {
        auto table_id = getStorageID();
        table_id.uuid = cnch_storage_id.uuid;
        auto txn = std::make_shared<CnchWorkerTransaction>(consume_context, server_client, table_id, assigned_consumer_index);
        consume_context->setCurrentTransaction(txn);
    }
    catch (...)
    {
        tryLogCurrentException(log, "create transaction failed for consuming kafka");
        throw Exception("create transaction failed to " + server_client->getRPCAddress(), ErrorCodes::CNCH_KAFKA_TASK_NEED_STOP);
    }

    /// set rpc info for committing later
    auto & client_info = consume_context->getClientInfo();

    // Poco::Net::SocketAddress can't parse ipv6 host with [] for example [::1], so always pass by host_port string created by createHostPortString
    client_info.current_address =
        Poco::Net::SocketAddress(
            server_client_address.getRPCAddress()
        );

    client_info.rpc_port = server_client_address.rpc_port;

    /// execute insert query
    InterpreterInsertQuery interpreter{insert, consume_context, false, true, true};
    auto block_io = interpreter.execute();

    BlockInputStreamPtr in = std::make_shared<CnchKafkaBlockInputStream>(*this, getInMemoryMetadataPtr(),
                             consume_context, block_io.out->getHeader().getNames(), block_size, assigned_consumer_index);


    streamCopyData(*in, *block_io.out, consume_context);

    if (global_settings.constraint_skip_violate)
    {
        if (const auto *counting_stream = dynamic_cast<const CountingBlockOutputStream *>(block_io.out.get()))
        {
            size_t origin_bytes = in->getProfileInfo().bytes;
            size_t written_bytes = counting_stream->getProgress().written_bytes;

            size_t origin_rows = in->getProfileInfo().rows;
            size_t written_rows = counting_stream->getProgress().written_rows;
            if (origin_rows != written_rows)
            {
                KafkaLogElement kafka_filter_log = createKafkaLog(KafkaLogElement::FILTER, assigned_consumer_index);
                kafka_filter_log.bytes = origin_bytes - written_bytes;
                kafka_filter_log.metric = origin_rows - written_rows;
                if (auto kafka_log = getContext()->getKafkaLog())
                    kafka_log->add(kafka_filter_log);
                if (auto cloud_kafka_log = getContext()->getCloudKafkaLog())
                    cloud_kafka_log->add(kafka_filter_log);
            }
        }
    }

    /// update offsets in memory in case of resetting buffer instead of restarting
    if (!consumer_context.latest_offsets.empty())
    {
        consumer_context.assignment = consumer_context.latest_offsets;
        consumer_context.latest_offsets.clear();

        try
        {
            auto *consumer_buf = consumer_context.buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>();
            consumer_buf->commit();
        }
        catch (...)
        {
            /// as we have stored offsets in catalog here we can just ignore exceptions
            tryLogCurrentException(__PRETTY_FUNCTION__, "Commit offsets to kafka broker failed");
        }
    }

    return false;
}

void StorageCloudKafka::streamCopyData(IBlockInputStream &from, IBlockOutputStream &to,
                                              ContextMutablePtr consume_context)
{
    TransactionCnchPtr current_txn = consume_context->getCurrentTransaction();
    if (!current_txn)
        throw Exception("No transaction set while consuming Kafka", ErrorCodes::LOGICAL_ERROR);

    from.readPrefix();
    to.writePrefix();

    UInt64 commit_rows{0}, commit_bytes{0};
    while (Block block = from.read())
    {
        /// Add group and tpl in transaction for committing
        current_txn->setKafkaTpl(getGroupForBytekv(), getCurrentConsumptionOffsets());

        KafkaLogElement kafka_write_log = createKafkaLog(KafkaLogElement::WRITE, assigned_consumer_index);
        kafka_write_log.metric = block.rows();
        kafka_write_log.bytes = block.bytes();
        Stopwatch watch;

        to.write(block);

        kafka_write_log.duration_ms = watch.elapsedMilliseconds();
        if (auto kafka_log = getContext()->getKafkaLog())
            kafka_log->add(kafka_write_log);
        if (auto cloud_kafka_log = getContext()->getCloudKafkaLog())
            cloud_kafka_log->add(kafka_write_log);

        commit_rows += kafka_write_log.metric;
        commit_bytes += kafka_write_log.bytes;
    }

    if (from.getProfileInfo().hasAppliedLimit())
        to.setRowsBeforeLimit(from.getProfileInfo().getRowsBeforeLimit());
    to.setTotals(from.getTotals());
    to.setExtremes(from.getExtremes());

    /// Write Commit kafka_log which should be the exactly data written to VFS
    auto kafka_commit_log = createKafkaLog(KafkaLogElement::COMMIT, assigned_consumer_index);
    kafka_commit_log.metric = commit_rows;
    kafka_commit_log.bytes = commit_bytes;
    Stopwatch watch;

    to.writeSuffix();

    kafka_commit_log.duration_ms = watch.elapsedMilliseconds();
    if (auto kafka_log = getContext()->getKafkaLog())
        kafka_log->add(kafka_commit_log);
    if (auto cloud_kafka_log = getContext()->getCloudKafkaLog())
        cloud_kafka_log->add(kafka_commit_log);

    from.readSuffix();
}

bool StorageCloudKafka::checkDependencies(const String &database_name, const String &table_name, bool check_staged_area)
{
    /// check if all dependencies are attached
    auto dependencies = DatabaseCatalog::instance().getDependencies({database_name, table_name});
    if (dependencies.empty())
    {
        return !(table_name == getTableName() && database_name == getDatabaseName());
    }

    /// otherwise check if each dependency is ready one by one
    for (const auto & db_tab : dependencies)
    {
        auto table = DatabaseCatalog::instance().tryGetTable({db_tab.database_name, db_tab.table_name}, getContext());
        if (!table)
            return false;

        /// check the target table for materialized view
        auto * materialized_view = dynamic_cast<StorageMaterializedView *>(table.get());
        if (materialized_view)
        {
            auto target_table = materialized_view->tryGetTargetTable();
            if (!target_table)
                return false;

            auto * cloud = dynamic_cast<StorageCloudMergeTree *> (target_table.get());
            if (!cloud)
                return false;
            if (table_name == getTableName() && database_name == getDatabaseName())
            {
                /* FIXME: unique table
                cloud_table_has_unique_key = cloud->hasUniqueKey();
                if (cloud_table_has_unique_key && check_staged_area)
                {
                    wait_for_staged_parts_to_publish = !cloud->checkStagedParts();
                } */
            }
        }

        /// check its dependencies
        if (!checkDependencies(db_tab.database_name, db_tab.table_name, check_staged_area))
            return false;
    }

    return true;
}

SettingsChanges StorageCloudKafka::createSettingsAdjustments()
{
    SettingsChanges result;
    // Needed for backward compatibility
    if (settings.input_format_skip_unknown_fields.changed)
        // Always skip unknown fields regardless of the context (JSON or TSKV)
        result.emplace_back("input_format_skip_unknown_fields", settings.input_format_skip_unknown_fields.value);
    else
        result.emplace_back("input_format_skip_unknown_fields", 1u);

    if (settings.input_format_allow_errors_ratio.changed)
        result.emplace_back("input_format_allow_errors_ratio", settings.input_format_allow_errors_ratio.value);
    else
        result.emplace_back("input_format_allow_errors_ratio", 1.0);

    result.emplace_back("input_format_allow_errors_num", settings.skip_broken_messages.value);

    if (!settings.schema.value.empty())
        result.emplace_back("format_schema", settings.schema.value);

    if (!settings.format_schema_path.value.empty())
        result.emplace_back("format_schema_path", settings.format_schema_path.value);

    /// Forbidden parallel parsing for Kafka in case of global setting.
    /// Kafka cannot support parallel parsing due to virtual column
    result.emplace_back("input_format_parallel_parsing", false);

    result.emplace_back("input_format_json_aggregate_function_type_base64_encode", settings.json_aggregate_function_type_base64_encode.value);
    result.emplace_back("format_protobuf_enable_multiple_message", settings.protobuf_enable_multiple_message.value);
    result.emplace_back("format_protobuf_default_length_parser", settings.protobuf_default_length_parser.value);

    return result;
}

Names StorageCloudKafka::filterVirtualNames(const Names &names) const
{
    Names virtual_names {};
    for (const auto & name : names)
    {
        if (name == "_topic"
            || name == "_key"
            || name == "_offset"
            || name == "_partition"
            || name == "_content"
            || name == "_info")
        {
            virtual_names.push_back(name);
        }
    }
    std::sort(virtual_names.begin(), virtual_names.end());
    return virtual_names;
}

KafkaLogElement StorageCloudKafka::createKafkaLog(KafkaLogElement::Type type, size_t consumer_index)
{
    KafkaLogElement elem;
    elem.event_type = type;
    elem.event_time = time(nullptr);
    elem.cnch_database = cnch_storage_id.database_name;
    elem.cnch_table = cnch_storage_id.table_name;
    elem.database = getDatabaseName();
    elem.table = getTableName();
    elem.consumer = toString(consumer_index);
    return elem;
}

void StorageCloudKafka::getConsumersStatus(CnchConsumerStatus &status) const
{
    std::lock_guard lock(table_status_mutex);
    if (!consumer_context.initialized)
    {
        /// Don't throw exception directly here to avoid re-scheduling new consumer
        /// as it may be starting for some pb tables who need time to load schema file from hdfs
        status.last_exception = "No consumer " + getTableName() + " found as it may be starting or stopping. This should be a short-time status";
        status.assignment = {};
    }
    else
    {
        status.last_exception = last_exception;

        std::ostringstream oss;
        for (const auto &tpl : consumer_context.assignment)
            oss << tpl << ";";
        status.assignment.emplace_back(oss.str());
    }

    status.cluster = getCluster();
    status.topics = topics;
    status.assigned_consumers = assigned_consumer_index;
}

static cppkafka::TopicPartitionList mergeOffsets(const cppkafka::TopicPartitionList & old_offsets,
                                                 const cppkafka::TopicPartitionList & new_offsets)
{
    OffsetsMap offsets;
    for (const auto & tpl : old_offsets)
        offsets[{tpl.get_topic(), tpl.get_partition()}] = tpl.get_offset();

    for (const auto & tpl : new_offsets)
        offsets[{tpl.get_topic(), tpl.get_partition()}] = tpl.get_offset();

    cppkafka::TopicPartitionList res;
    for (auto & tpl : offsets)
        res.emplace_back(tpl.first.first, tpl.first.second, tpl.second);
    return res;
}

cppkafka::TopicPartitionList StorageCloudKafka::getCurrentConsumptionOffsets()
{
    std::lock_guard lock_thread(table_status_mutex);
    if (!consumer_context.initialized)
        return {};
    if (!consumer_context.buffer)
        throw Exception("No buffer found while get commit offsets", ErrorCodes::LOGICAL_ERROR);

    auto *buffer = consumer_context.buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>();
    auto offsets = buffer->getOffsets();
    if (offsets.empty())
        return {};

    consumer_context.latest_offsets = mergeOffsets(consumer_context.assignment, offsets);

    return consumer_context.latest_offsets;
}

cppkafka::TopicPartitionList StorageCloudKafka::getConsumerAssignment() const
{
    std::lock_guard lock_thread(table_status_mutex);
    return consumer_context.assignment;
}

void dropConsumerTables(ContextMutablePtr context, const String & db_name, const String & tb_name)
{
    std::unordered_set<String> tables_to_drop;

    /// TODO: get dependencies failed sometimes
    auto dependencies = DatabaseCatalog::instance().getDependencies({db_name, tb_name});
    if (dependencies.empty())
    {
        LOG_DEBUG(&Poco::Logger::get("CnchKafkaWorker"), "No dependencies found for " + db_name + "." + tb_name);
        tables_to_drop.emplace(backQuoteIfNeed(db_name)  + "." + backQuoteIfNeed(tb_name));
    }
    else
    {
        for (auto & db_tb : dependencies)
        {
            auto table = DatabaseCatalog::instance().getTable({db_tb.database_name, db_tb.table_name}, context);
            if (auto *mv = dynamic_cast<StorageMaterializedView*>(table.get()))
            {
                tables_to_drop.emplace(backQuoteIfNeed(mv->getDatabaseName()) + "." + backQuoteIfNeed(mv->getTableName()));
                ///tables_to_drop.emplace(backQuoteIfNeed(mv->getSelectDatabaseName()) + "." + backQuoteIfNeed(mv->getSelectTableName()));

                try
                {
                    auto target_table = mv->getTargetTable();
                    if (auto * cloud_table = dynamic_cast<StorageCloudMergeTree *>(target_table.get()))
                    {
                        tables_to_drop.emplace(backQuoteIfNeed(mv->getTargetDatabaseName()) + "." + backQuoteIfNeed(mv->getTargetTableName()));
                    }
                }
                catch (...)
                {
                    LOG_WARNING(&Poco::Logger::get("CnchKafkaWorker"), "Get local target table failed");
                }

            }
        }
        tables_to_drop.emplace(backQuoteIfNeed(db_name)  + "." + backQuoteIfNeed(tb_name));
    }

    ParserDropQuery parser;
    for (const auto & table_to_drop : tables_to_drop)
    {
        String drop_table_command = "DROP TABLE IF EXISTS " + table_to_drop;
        LOG_DEBUG(&Poco::Logger::get("CnchKafkaWorker"), "DROP table : {}", drop_table_command);

        try
        {
            InterpreterDropQuery interpreter(
                parseQuery(parser, drop_table_command, context->getSettings().max_query_size, context->getSettings().max_parser_depth),
                context);
            interpreter.execute();
        }
        catch (...)
        {
            /// just print log to ensure all tables to be dropped
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }
    }
}

void createConsumerTables(const std::vector<String> & create_table_commands, ContextMutablePtr global_context)
{
    auto create_context = Context::createCopy(global_context);
    create_context->setSetting("default_database_engine", String("Memory"));

    ParserCreateQuery parser;
    for (const auto & cmd : create_table_commands)
    {
        LOG_DEBUG(&Poco::Logger::get("CnchKafkaWorker"), "CREATE local table: {}", cmd);
        ASTPtr ast = parseQuery(parser, cmd, global_context->getSettings().max_query_size,
                                global_context->getSettings().max_parser_depth);

        InterpreterCreateQuery interpreter_tb(ast, create_context);
        interpreter_tb.execute();
    }
    LOG_DEBUG(&Poco::Logger::get("CnchKafkaWorker"), "CREATE local tables on worker successfully");
}

void executeKafkaConsumeTaskImpl(const KafkaTaskCommand & command, ContextMutablePtr context)
{
    /// create tables first if starting consume
    if (command.type == KafkaTaskCommand::Type::START_CONSUME)
        createConsumerTables(command.create_table_commands, context);

    auto table = DatabaseCatalog::instance().getTable({command.local_database_name, command.local_table_name}, context);
    if (!table)
        throw Exception("get table from Kafka command failed", ErrorCodes::LOGICAL_ERROR);

    auto *storage = dynamic_cast<StorageCloudKafka *>(table.get());
    if (!storage)
        throw Exception("convert to StorageCloudKafka from table failed", ErrorCodes::LOGICAL_ERROR);

    /// START/STOP CONSUME
    switch (command.type)
    {
        case KafkaTaskCommand::Type::START_CONSUME:
            storage->setCnchStorageID(command.cnch_storage_id);
            storage->startConsume(command.assigned_consumer, command.tpl);
            break;
        case KafkaTaskCommand::Type::STOP_CONSUME:
            storage->stopConsume();
            dropConsumerTables(context, command.local_database_name, command.local_table_name);
            break;
        default:
            throw Exception("Unknown Kafka command", ErrorCodes::BAD_ARGUMENTS);
    }
}

void executeKafkaConsumeTask(const KafkaTaskCommand & command, ContextMutablePtr context)
{
    try
    {
        executeKafkaConsumeTaskImpl(command, context);
    }
    catch (...)
    {
        tryLogCurrentException(__func__, "Failed to execute Kafka consume task");
    }
}

} /// namespace DB

#endif
