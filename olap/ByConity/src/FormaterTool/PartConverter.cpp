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

#include <FormaterTool/PartConverter.h>
#include <FormaterTool/ZipHelper.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTPartToolKit.h>
#include <Interpreters/InterpreterCreateQuery.h>
#include <Interpreters/Context.h>
#include <QueryPlan/QueryPlan.h>
#include <CloudServices/CnchPartsHelper.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Processors/Executors/PipelineExecutingBlockInputStream.h>
#include <Formats/FormatFactory.h>
#include <Disks/HDFS/DiskByteHDFS.h>
#include <Disks/SingleDiskVolume.h>
#include <Poco/Path.h>
#include <Poco/DirectoryIterator.h>
#include <Poco/UUIDGenerator.h>
#include <IO/ZlibDeflatingWriteBuffer.h>
#include <Common/StringUtils/StringUtils.h>
#include <common/logger_useful.h>

namespace DB
{

#define DEFAULT_MAX_CONVERT_THREADS 1
#define DEFAULT_MAX_SPLIT_FILE_SIZE 1024*1024*1024

namespace ErrorCodes
{
    extern const int INCORRECT_QUERY;
    extern const int LOGICAL_ERROR;
    extern const int DIRECTORY_DOESNT_EXIST;
    extern const int DIRECTORY_ALREADY_EXISTS;
}

using BlockPtr = std::shared_ptr<Block>;
using BlockVector = std::vector<BlockPtr>;

PartConverter::PartConverter(const ASTPtr & query_ptr_, ContextMutablePtr context_)
    : PartToolkitBase(query_ptr_, context_)
{
    const ASTPartToolKit & pc_query = query_ptr->as<ASTPartToolKit &>();
    if (pc_query.type != PartToolType::CONVERTER)
        throw Exception("Wrong input query.", ErrorCodes::INCORRECT_QUERY);

    source_path = pc_query.source_path->as<ASTLiteral &>().value.safeGet<String>();
    target_path = pc_query.target_path->as<ASTLiteral &>().value.safeGet<String>();
    data_format = getIdentifierName(pc_query.data_format);

    if (source_path.empty() || target_path.empty())
        throw Exception("Source path and target path cannot be empty.", ErrorCodes::LOGICAL_ERROR);

    if (!endsWith(source_path, "/"))
        source_path.append("/");

    if (!endsWith(target_path, "/"))
        target_path.append("/");

    Poco::URI uri(source_path);
    if (!(uri.getScheme() == "hdfs"))
        throw Exception("Source path must be hdfs directory.", ErrorCodes::LOGICAL_ERROR);

    source_path = uri.getPath();
    if (!endsWith(source_path, "/"))
        source_path.append("/");

    if (fs::exists(target_path))
        throw Exception("Target location already exists. Please use a non-exists directory instead.", ErrorCodes::DIRECTORY_ALREADY_EXISTS);

    // create target directory to get generated files.
    fs::create_directories(target_path);

    Poco::UUIDGenerator & generator = Poco::UUIDGenerator::defaultGenerator();
    working_path = Poco::Path::current() + generator.createRandom().toString() + "/";
    if (fs::exists(working_path))
    {
        LOG_INFO(log, "Local path {} already exists. Will remove it.", working_path);
        fs::remove_all(working_path);
    }
    getContext()->setPath(working_path);
    fs::create_directory(working_path);
    fs::create_directory(working_path + "disks/");
    fs::create_directory(working_path + PT_RELATIVE_LOCAL_PATH);

    getContext()->setHdfsUser("clickhouse");
    getContext()->setHdfsNNProxy("nnproxy");

    applySettings();
}

PartConverter::~PartConverter()
{
    if (fs::exists(working_path))
        fs::remove_all(working_path);
}

void PartConverter::execute()
{
    StoragePtr table = getTable();
    StorageCloudMergeTree * storage = static_cast<StorageCloudMergeTree *>(table.get());
    if (storage->getStorageUUID() == UUIDHelpers::Nil)
        throw Exception("UUID should be specified in table definition.", ErrorCodes::LOGICAL_ERROR);
    String uuid = UUIDHelpers::UUIDToString(storage->getStorageUUID());

    HDFSConnectionParams params{HDFSConnectionParams::CONN_NNPROXY, getContext()->getHdfsUser(), getContext()->getHdfsNNProxy()};
    std::shared_ptr<DiskByteHDFS> remote_disk = std::make_shared<DiskByteHDFS>("hdfs", source_path, params);
    auto single_volume = std::make_shared<SingleDiskVolume>("volume_single", remote_disk, 0);

    auto create_part = [&](const String & name)
    {
        auto part = std::make_shared<MergeTreeDataPartCNCH>(*storage, name, single_volume, name + '/');
        part->loadFromFileSystem();
        return part;
    };

    MergeTreeDataPartsVector data_parts;
    if (user_settings.count("part_names"))
    {
        String parts_str = user_settings["part_names"].safeGet<String>();
        std::stringstream ss(parts_str);
        std::string part;
        while(std::getline(ss, part, ','))
        {
            data_parts.push_back(create_part(part));
        }
    }
    else if (user_settings.count("part_name_prefix"))
    {
        String prefix = user_settings["part_name_prefix"].safeGet<String>();
        std::vector<String> file_names;
        remote_disk->listFiles(uuid, file_names);
        for (auto & file : file_names)
        {
            if (startsWith(file, prefix))
                data_parts.push_back(create_part(file));
        }
    }
    else
        throw Exception("User should specify which part to be converted. Either by part name or part name prefix.", ErrorCodes::LOGICAL_ERROR);

    if (data_parts.empty())
    {
        LOG_INFO(log, "No available parts to convert.");
        return;
    }

    data_parts = CnchPartsHelper::calcVisibleParts(data_parts, false);

    LOG_INFO(log, "Start to convert {} data parts.", data_parts.size());

    size_t max_threads = user_settings.count("max_convert_threads") ? user_settings["max_convert_threads"].safeGet<UInt64>() : DEFAULT_MAX_CONVERT_THREADS;
    size_t max_split_file_size = user_settings.count("max_split_file_size") ? user_settings["max_split_file_size"].safeGet<UInt64>() : DEFAULT_MAX_SPLIT_FILE_SIZE;

    ExceptionHandler exception_handler;
    ThreadPool pool(max_threads);

    auto metadata_snapshot = storage->getInMemoryMetadataPtr();
    Names column_names = metadata_snapshot->getColumns().getNamesOfPhysical();

    auto convert = [&](const BlockVector & blocks, const String & outfile, const Block & header)
    {
        if (fs::exists(outfile))
            throw Exception("File {} already exists.", ErrorCodes::LOGICAL_ERROR);

        LOG_DEBUG(log, "Dump blocks to new file {}", outfile);
        auto file_buffer = std::make_unique<WriteBufferFromFile>(outfile);
        auto zip_buffer = std::make_unique<ZlibDeflatingWriteBuffer>(std::move(file_buffer), DB::CompressionMethod::Gzip, 3);
        auto out = FormatFactory::instance().getOutputStream(data_format, *zip_buffer, header, getContext(), {}, {});
        out->writePrefix();
        for (const BlockPtr & block : blocks)
            out->write(*block);
        out->writeSuffix();
    };

    for (auto & part : data_parts)
    {
        auto input = std::make_shared<MergeTreeSequentialSource>(*storage, storage->getInMemoryMetadataPtr(), part, column_names, false, true);
        QueryPipeline pipeline;
        pipeline.init(Pipe(std::move(input)));
        pipeline.setMaxThreads(1);
        BlockInputStreamPtr input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(pipeline));
        Block header = input_stream->getHeader();
        size_t serial = 1;
        size_t total_bytes = 0;
        input_stream->readPrefix();
        BlockVector blocks;
        while(Block block = input_stream->read())
        {
            blocks.push_back(std::make_shared<Block>(block));
            total_bytes += block.bytes();

            if (total_bytes > max_split_file_size)
            {
                String new_file_name = target_path + part->name + "_" + toString(serial++) + ".gz";
                BlockVector batch {};
                batch.swap(blocks);
                pool.scheduleOrThrowOnError(
                    createExceptionHandledJob([&, batch, header, new_file_name]()
                    {
                        convert(batch, new_file_name, header);
                    }, exception_handler)
                );

                total_bytes = 0;
            }
        }

        if (!blocks.empty())
        {
            String new_file_name = target_path + part->name + "_" + toString(serial++) + ".gz";
            pool.scheduleOrThrowOnError(
                createExceptionHandledJob([&, blocks, header, new_file_name]()
                {
                    convert(blocks, new_file_name, header);
                }, exception_handler)
            );
        }

        input_stream->readSuffix();
    }

    pool.wait();

    /// throw if exception during converting parts.
    exception_handler.throwIfException();

    LOG_INFO(log, "Finish to convert all data parts.");
}

}
