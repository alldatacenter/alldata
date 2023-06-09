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

#include <DataStreams/OwningBlockInputStream.h>
#include <Formats/FormatFactory.h>
#include <Processors/Formats/Impl/ParquetBlockInputFormat.h>
#include <Processors/Formats/Impl/ORCBlockInputFormat.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/Hive/ParquetBlockInputStream.h>
#include <Storages/Hive/ORCBlockInputStream.h>
#include <Storages/MergeTree/CnchHiveThreadSelectBlockInputProcessor.h>

namespace DB
{
class ParquetBlockInputFormat;
class ORCBlockInputFormat;

CnchHiveThreadSelectBlockInputProcessor::CnchHiveThreadSelectBlockInputProcessor(
    const size_t & thread_,
    const std::shared_ptr<CnchHiveReadPool> & pool_,
    const StorageCloudHive & /*storage_*/,
    const StorageMetadataPtr & metadata_snapshot_,
    ContextPtr & context_,
    const UInt64 & /*max_block_size_*/)
    : SourceWithProgress(pool_->getHeader()), thread(thread_), pool(pool_), metadata_snapshot(metadata_snapshot_), context(context_)
{
}

CnchHiveThreadSelectBlockInputProcessor::~CnchHiveThreadSelectBlockInputProcessor() = default;

Block CnchHiveThreadSelectBlockInputProcessor::getHeader() const
{
    return pool->getHeader();
}

Chunk CnchHiveThreadSelectBlockInputProcessor::generate()
{
    LOG_TRACE(&Poco::Logger::get("CnchHiveThreadSelectBlockInputProcessor"), " generate ");
    Block res;

    while (!res && !isCancelled())
    {
        if (!task && !getNewTask())
            break;

        res = stream->read();

        LOG_TRACE(&Poco::Logger::get("CnchHiveThreadSelectBlockInputProcessor"), " parquet read rows: {}", res.rows());

        const auto * parquet = dynamic_cast<const ParquetBlockInputStream *>(stream.get());
        const auto * orc = dynamic_cast<const ORCBlockInputStream *>(stream.get());
        if (!parquet && !orc)
            throw Exception("Unexpected Format in CnchHive ,currently only support Parquet/ORC", ErrorCodes::LOGICAL_ERROR);

        // if(parquet->isFinished())
        // {
        //     task.reset();
        // }

        task.reset();
        read_buf.reset();
        stream.reset();
    }

    return Chunk(res.getColumns(), res.rows());
}

bool CnchHiveThreadSelectBlockInputProcessor::getNewTask()
{
    task = pool->getTask(thread);

    if (!task)
    {
        read_buf.reset();
        stream.reset();
        return false;
    }

    auto & part = task->data_part;
    size_t current_row_group = task->current_row_group;
    const String part_path = part->getFullDataPartPath();
    const String part_format = part->getFormatName();

    LOG_TRACE(
        &Poco::Logger::get("CnchHiveThreadSelectBlockInputStream"),
        "getNewTask current_row_group: {} part is {} ",
        current_row_group,
        part_path);
    read_buf = std::make_unique<ReadBufferFromByteHDFS>(part_path, true, context->getHdfsConnectionParams());

    FormatSettings format_settings;
    if (part_format.find("Parquet") != String::npos)
    {
        format_settings.parquet.partition_kv = part->getInfo().getPartition();
        format_settings.parquet.skip_row_groups = part->getSkipSplits();
        format_settings.parquet.current_row_group = current_row_group;
        format_settings.parquet.read_one_group = true;

        auto parquet_format = FormatFactory::instance().getInput(
            "Parquet", *read_buf, getHeader(), context, context->getSettingsRef().max_block_size, format_settings);

        stream = std::make_shared<ParquetBlockInputStream>(parquet_format);
    }
    else if (part_format.find("Orc") != String::npos)
    {
        format_settings.orc.partition_kv = part->getInfo().getPartition();

        auto orc_format = FormatFactory::instance().getInput(
            "ORC", *read_buf, getHeader(), context, context->getSettingsRef().max_block_size, format_settings);

        stream = std::make_shared<ORCBlockInputStream>(orc_format);
    }
    else
        throw Exception("Unexpected Format in CnchHive ,currently only support Parquet/ORC", ErrorCodes::LOGICAL_ERROR);

    return true;
}

}
