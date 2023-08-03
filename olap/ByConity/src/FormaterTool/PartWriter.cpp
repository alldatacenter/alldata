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

#include <FormaterTool/PartWriter.h>
#include <FormaterTool/HDFSDumper.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ASTPartToolKit.h>
#include <Interpreters/InterpreterInsertQuery.h>
#include <DataStreams/UnionBlockInputStream.h>
#include <DataStreams/OwningBlockInputStream.h>
#include <DataStreams/NullAndDoCopyBlockInputStream.h>
#include <DataStreams/CheckConstraintsBlockOutputStream.h>
#include <DataStreams/AddingDefaultBlockOutputStream.h>
#include <DataStreams/CountingBlockOutputStream.h>
#include <DataStreams/SquashingBlockOutputStream.h>
#include <DataStreams/SquashingBlockInputStream.h>
#include <Storages/MergeTree/CloudMergeTreeBlockOutputStream.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/ColumnsDescription.h>
#include <Storages/StorageMergeTree.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/MergeTree/MergeTreeCNCHDataDumper.h>
#include <Disks/HDFS/DiskByteHDFS.h>
#include <common/logger_useful.h>
#include <Transaction/TransactionCommon.h>
#include <IO/SnappyReadBuffer.h>
#include <Poco/UUIDGenerator.h>
#include <Poco/URI.h>
#include <Poco/Path.h>
#include <sstream>

namespace DB
{

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
    extern const int LOGICAL_ERROR;
    extern const int DIRECTORY_DOESNT_EXIST;
    extern const int DIRECTORY_ALREADY_EXISTS;
}

PartWriter::PartWriter(const ASTPtr & query_ptr_, ContextMutablePtr context_)
    : PartToolkitBase(query_ptr_, context_)
{
    const ASTPartToolKit & pw_query = query_ptr->as<ASTPartToolKit &>();
    if (pw_query.type != PartToolType::WRITER)
        throw Exception("Wrong input query.", ErrorCodes::INCORRECT_QUERY);

    source_path = pw_query.source_path->as<ASTLiteral &>().value.safeGet<String>();
    dest_path = pw_query.target_path->as<ASTLiteral &>().value.safeGet<String>();
    data_format = getIdentifierName(pw_query.data_format);

    if (source_path.empty() || dest_path.empty())
        throw Exception("Source path and target path cannot be empty.", ErrorCodes::LOGICAL_ERROR);

    Poco::URI uri(dest_path);

    if (!(uri.getScheme() == "hdfs"))
        throw Exception("Target path must be hdfs directory.", ErrorCodes::LOGICAL_ERROR);

    dest_path = uri.getPath();
    if (!endsWith(dest_path, "/"))
        dest_path.append("/");

    Poco::UUIDGenerator & generator = Poco::UUIDGenerator::defaultGenerator();
    working_path = Poco::Path::current() + generator.createRandom().toString() + "/";

    if (fs::exists(working_path))
    {
        LOG_INFO(log, "Working path {} already exists. Will remove it.", working_path);
        fs::remove_all(working_path);
    }

    LOG_INFO(log, "Creating new working directory {}.", working_path);
    fs::create_directory(working_path);
    fs::create_directory(working_path + "disks/");
    fs::create_directory(working_path + PT_RELATIVE_LOCAL_PATH);

    getContext()->setPath(working_path);
    getContext()->setHdfsUser("clickhouse");
    getContext()->setHdfsNNProxy("nnproxy");

    applySettings();
}

PartWriter::~PartWriter()
{
    if (fs::exists(working_path))
        fs::remove_all(working_path);
}

void PartWriter::execute()
{
    LOG_INFO(log, "PartWriter start to dump part.");
    const Settings & settings = getContext()->getSettingsRef();
    StoragePtr table = getTable();
    StorageCloudMergeTree * storage = static_cast<StorageCloudMergeTree *>(table.get());
    String uuid = UUIDHelpers::UUIDToString(storage->getStorageUUID());

    auto metadata_snapshot = table->getInMemoryMetadataPtr();

    Block sample_block = metadata_snapshot->getSampleBlockNonMaterialized();

    /// set current transaction
    TransactionRecord record;
    record.read_only = true;
    record.setID(TxnTimestamp{1});
    getContext()->setCurrentTransaction(std::make_shared<CnchServerTransaction>(getContext(), record));

    /// prepare remote disk
    HDFSConnectionParams params{HDFSConnectionParams::CONN_NNPROXY, getContext()->getHdfsUser(), getContext()->getHdfsNNProxy()};
    std::shared_ptr<DiskByteHDFS> remote_disk = std::make_shared<DiskByteHDFS>("hdfs", dest_path, params);

    if (remote_disk->exists(uuid))
    {
        LOG_WARNING(log, "Remote path {} already exists. Try to remove it.", dest_path + uuid);
        remote_disk->removeDirectory(uuid);
    }
    LOG_DEBUG(log, "Creating remote storage path {} for table.", dest_path + uuid);
    remote_disk->createDirectory(uuid);

    S3ObjectMetadata::PartGeneratorID part_generator_id(S3ObjectMetadata::PartGeneratorID::DUMPER,
        UUIDHelpers::UUIDToString(UUIDHelpers::generateV4()));
    MergeTreeCNCHDataDumper cnch_dumper(*storage, part_generator_id);
    /// prepare outputstream
    BlockOutputStreamPtr out = table->write(query_ptr, metadata_snapshot, getContext());
    CloudMergeTreeBlockOutputStream * cloud_stream = static_cast<CloudMergeTreeBlockOutputStream *>(out.get());

    auto input_stream = InterpreterInsertQuery::buildInputStreamFromSource(getContext(), sample_block, settings, source_path, data_format);

    input_stream->readPrefix();
    while (true)
    {
        const auto block = input_stream->read();
        if (!block)
            break;
        auto parts = cloud_stream->convertBlockIntoDataParts(block, true);
        LOG_DEBUG(log, "Dumping {} parts to remote storage.", parts.size());
        for (const auto & temp_part : parts)
        {
            cnch_dumper.dumpTempPart(temp_part, false, remote_disk);
            LOG_DEBUG(log, "Dumped part {}", temp_part->name);
        }
    }

    input_stream->readSuffix();

    LOG_INFO(log, "Finish write data parts into local file system.");
}

}
