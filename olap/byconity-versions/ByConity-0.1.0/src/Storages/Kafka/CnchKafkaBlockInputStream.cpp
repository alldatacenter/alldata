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

#include <Storages/Kafka/CnchKafkaBlockInputStream.h>

#include <Formats/FormatFactory.h>
#include <Storages/Kafka/CnchReadBufferFromKafkaConsumer.h>
#include <DataStreams/OneBlockInputStream.h>
#include <DataStreams/ConvertingBlockInputStream.h>
#include <Interpreters/addMissingDefaults.h>
#include <Interpreters/CnchSystemLog.h>
#include <Storages/Kafka/KafkaConsumeInfo.h>
#include <DataTypes/DataTypeString.h>
#include <Processors/Formats/IRowInputFormat.h>
#include <Processors/Formats/InputStreamFromInputFormat.h>

namespace DB
{

namespace ErrorCodes
{
extern const int NO_AVAILABLE_CONSUMER;
}

/// sorted
Names CnchKafkaBlockInputStream::default_virtual_column_names = {"_content", "_info", "_key", "_offset", "_partition", "_topic"};

CnchKafkaBlockInputStream::CnchKafkaBlockInputStream(
    StorageCloudKafka & storage_,
    const StorageMetadataPtr & metadata_snapshot_,
    const std::shared_ptr<Context> & context_,
    const Names & column_names_,
    size_t max_block_size_,
    size_t consumer_index_,
    bool need_add_defaults_)
    : storage(storage_)
    , metadata_snapshot(metadata_snapshot_)
    , context(context_)
    , column_names(column_names_)
    , max_block_size(max_block_size_)
    , consumer_index(consumer_index_)
    , need_add_defaults(need_add_defaults_)
    , virtual_column_names(storage.filterVirtualNames(column_names))
    , used_column(default_virtual_column_names.size(), -1)
    , non_virtual_header(metadata_snapshot->getSampleBlockNonMaterialized())
    , virtual_header(metadata_snapshot->getSampleBlockForColumns(virtual_column_names, storage.getVirtuals(), storage.getStorageID()))

{
    for (size_t i = 0, index = 0; i < default_virtual_column_names.size() && index < virtual_column_names.size(); ++i)
    {
        if (default_virtual_column_names[i] == virtual_column_names[index])
            used_column[i] = index++;
    }
    virtual_columns = virtual_header.cloneEmptyColumns();
}

CnchKafkaBlockInputStream::~CnchKafkaBlockInputStream()
{
    if (!claimed)
        return;

    /// Some metrics to system.kafka_log
    if (auto kafka_log = context->getKafkaLog())
    {
        auto cloud_kafka_log = context->getCloudKafkaLog();
        try
        {
            auto *read_buf = delimited_buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>();
            auto create_time = read_buf->getCreateTime();
            auto duration_ms = (read_buf->getAliveTime() - create_time) * 1000;

            /// Always write POLL log event if polled nothing
            auto kafka_poll_log = storage.createKafkaLog(KafkaLogElement::POLL, consumer_index);
            kafka_poll_log.event_time = create_time;
            kafka_poll_log.duration_ms = duration_ms;
            kafka_poll_log.metric = read_buf->getReadMessages();
            kafka_poll_log.bytes = read_buf->getReadBytes();
            kafka_log->add(kafka_poll_log);
            if (cloud_kafka_log)
                cloud_kafka_log->add(kafka_poll_log);

            if (read_buf->getEmptyMessages() > 0)
            {
                auto kafka_empty_log = storage.createKafkaLog(KafkaLogElement::EMPTY_MESSAGE, consumer_index);
                kafka_empty_log.event_time = create_time;
                kafka_empty_log.duration_ms = duration_ms;
                kafka_empty_log.metric = read_buf->getEmptyMessages();
                kafka_log->add(kafka_empty_log);
                if (cloud_kafka_log)
                    cloud_kafka_log->add(kafka_empty_log);
            }

            IRowInputFormat * row_input = nullptr;
             if (!children.empty())
             {
                 if (auto *input_stream = dynamic_cast<InputStreamFromInputFormat *>(children.back().get()))
                     row_input = dynamic_cast<IRowInputFormat *>(input_stream->getInputFormatPtr().get());
             }

            if (row_input && row_input->getNumErrors() > 0)
            {
                auto kafka_parse_log = storage.createKafkaLog(KafkaLogElement::PARSE_ERROR, consumer_index);
                kafka_parse_log.event_time = create_time;
                kafka_parse_log.duration_ms = duration_ms;
                kafka_parse_log.last_exception = row_input->getAndParseException().displayText();
                kafka_parse_log.metric = row_input->getNumErrors();
                kafka_parse_log.bytes = row_input->getErrorBytes();
                kafka_parse_log.has_error = true;
                kafka_log->add(kafka_parse_log);
                if (cloud_kafka_log)
                    cloud_kafka_log->add(kafka_parse_log);
            }
        }
        catch (...)
        {
            LOG_ERROR(storage.log, "{}(): {}", __func__, getCurrentExceptionMessage(false));
        }
    }

    if (broken)
    {
        try
        {
            storage.unsubscribeBuffer(delimited_buffer);
        }
        catch (...)
        {
            LOG_ERROR(storage.log, "{}(): {}", __func__, getCurrentExceptionMessage(false));
        }
    }

    /// For releasing lock
    storage.pushBuffer();
}

Block CnchKafkaBlockInputStream::getHeader() const
{
    /// return storage.getSampleBlockNonMaterializedWithVirtualsForColumns(column_names);
    return metadata_snapshot->getSampleBlockForColumns(column_names, storage.getVirtuals(), storage.getStorageID());
}

void CnchKafkaBlockInputStream::readPrefixImpl()
{
    delimited_buffer = storage.tryClaimBuffer(context->getSettingsRef().queue_max_wait_ms.totalMilliseconds());
    claimed = !!delimited_buffer;

    /// 1. The streamThread catch this exception, it will retry at next time;
    /// 2. `SELECT FROM` CnchKafka table, the user will receive this exception.
    if (!delimited_buffer)
        throw Exception("Failed to claim consumer!", ErrorCodes::NO_AVAILABLE_CONSUMER);

    storage.subscribeBuffer(delimited_buffer);
    const auto * sub_buffer = delimited_buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>();

    FormatFactory::ReadCallback read_callback;
    if (!virtual_column_names.empty())
    {
        read_callback = [this, sub_buffer]
        {
            if (used_column[0] >= 0) virtual_columns[used_column[0]]->insert(sub_buffer->currentContent());
            if (used_column[1] >= 0) virtual_columns[used_column[1]]->insertDefault();
            if (used_column[2] >= 0) virtual_columns[used_column[2]]->insert(sub_buffer->currentKey());
            if (used_column[3] >= 0) virtual_columns[used_column[3]]->insert(sub_buffer->currentOffset());
            if (used_column[4] >= 0) virtual_columns[used_column[4]]->insert(sub_buffer->currentPartition());
            if (used_column[5] >= 0) virtual_columns[used_column[5]]->insert(sub_buffer->currentTopic());
        };
    }

    auto child = FormatFactory::instance().getInput(storage.settings.format.value,
                                                 *delimited_buffer, non_virtual_header, context, max_block_size);

    ///FIXME: Add default value if column has `DEFAULT` expression and no data read from topic
    //if (need_add_defaults)
    //    addChild(std::make_shared<AddingDefaultsBlockInputStream>(child, storage.getColumns().getDefaults(), context));
    //else
    //    addChild(child);

    if (auto *row_input = dynamic_cast<IRowInputFormat*>(child.get()))
    {
        row_input->setReadCallBack(read_callback);
        row_input->setCallbackOnError( [sub_buffer] (auto & e)
                                       {
                                          if (sub_buffer->currentMessage())
                                          {
                                              e.addMessage(
                                                  "\ntopic:     " + sub_buffer->currentTopic() +
                                                  "\npartition: " + toString(sub_buffer->currentPartition()) +
                                                  "\noffset:    " + toString(sub_buffer->currentOffset()) +
                                                  "\ncontent:   " + sub_buffer->currentContent()
                                              );
                                          }
                                       });
    }
    else
        throw Exception("An input format based on IRowInputFormat is expected, but provided: " + child->getName(), ErrorCodes::LOGICAL_ERROR);

    BlockInputStreamPtr stream = std::make_shared<InputStreamFromInputFormat>(std::move(child));
    addChild(stream);

    broken = true;
}

Block CnchKafkaBlockInputStream::readImpl()
{
    Block block = children.back()->read();
    if (!block)
        return block;

    Block virtual_block = virtual_header.cloneWithColumns(std::move(virtual_columns));
    virtual_columns = virtual_header.cloneEmptyColumns();

    for (const auto & column :  virtual_block.getColumnsWithTypeAndName())
        block.insert(column);

    /// No materialized column supported in Kafka directly, which can be implemented in MaterializedView table

    return ConvertingBlockInputStream(
        std::make_shared<OneBlockInputStream>(block), getHeader(),
        ConvertingBlockInputStream::MatchColumnsMode::Name).read();
}

void CnchKafkaBlockInputStream::readSuffixImpl()
{
    broken = false;
}

void CnchKafkaBlockInputStream::forceCommit()
{
    delimited_buffer->subBufferAs<CnchReadBufferFromKafkaConsumer>()->commit();
}

} // namespace DB

#endif
