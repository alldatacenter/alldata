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

#include <FormaterTool/HDFSDumper.h>
#include <FormaterTool/ZipHelper.h>
#include <Common/PODArray.h>
#include <Common/ThreadPool.h>
#include <Common/StringUtils/StringUtils.h>
#include <Storages/HDFS/WriteBufferFromHDFS.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Poco/URI.h>
#include <Poco/Random.h>
#include <Poco/File.h>
#include <fstream>

namespace DB
{

#define COMPRESSION_SUFFIX ".zip"

namespace ErrorCodes
{
    extern const int CANNOT_OPEN_FILE;
    extern const int CANNOT_READ_FROM_FILE_DESCRIPTOR;
    extern const int LOGICAL_ERROR;
    extern const int NOT_ENOUGH_SPACE;
    extern const int UNKNOWN_FORMAT;
}

HDFSDumper::HDFSDumper(const String & hdfs_user_, const String & hdfs_nnproxy_, size_t buffer_size_)
    : buffer_size(buffer_size_)
{
    hdfs_params = HDFSConnectionParams(HDFSConnectionParams::CONN_NNPROXY,
        hdfs_user_, hdfs_nnproxy_);
    hdfs_filesystem = std::make_unique<HDFSFileSystem>(hdfs_params, 100000, 100, 0);
}

void HDFSDumper::uploadPartsToRemote(const String & local_path, const String & remote_path, std::vector<String> & parts_to_upload)
{
    auto upload_one_part = [&] (const String & part_name)
    {
        String zip_file = local_path + part_name + COMPRESSION_SUFFIX;
        String part_path = local_path + part_name + "/";
        String remote_file = Poco::URI(remote_path).getPath() + part_name + COMPRESSION_SUFFIX;

        ZipHelper::zipFile(part_path, zip_file);

        try
        {
            uploadFileToRemote(zip_file, remote_file);
        }
        catch(Exception & e)
        {
            // clean incomplete file.
            if (hdfs_filesystem->exists(remote_file))
                hdfs_filesystem->remove(remote_file);

            throw e;
        }
    };

    ThreadPool pool;
    for (auto it = parts_to_upload.cbegin(); it != parts_to_upload.cend(); ++it)
    {
        pool.scheduleOrThrowOnError(
            [&](){upload_one_part(*it);}
        );
    }

    pool.wait();
}

std::vector<std::pair<String, DiskPtr>> HDFSDumper::fetchPartsFromRemote(const Disks & disks,
        const String & remote_path, const String & relative_local_path)
{
    if (disks.size() == 0)
        throw Exception("Disks are empty. ", ErrorCodes::LOGICAL_ERROR);

    // start fetch with a disk randomly.
    Poco::Random rand;
	rand.seed();
    size_t offset = rand.next(disks.size());

    /// choose a disk with enough space.
    auto get_disk_and_local_path = [&](size_t reserve_bytes) -> DiskPtr
    {
        size_t origin_offset = offset;

        do
        {
            if (offset >= disks.size())
                offset -= disks.size();

            auto reservation = disks.at(offset)->reserve(reserve_bytes);

            if (reservation)
                return reservation->getDisk();

            ++offset;
        } while (offset != origin_offset);

        throw Exception("No enough space in local disks", ErrorCodes::NOT_ENOUGH_SPACE);
    };

    std::vector<std::pair<String, DiskPtr>> fetched;

    String source_dir = Poco::URI(remote_path).getPath();
    std::vector<String> remote_files;
    hdfs_filesystem->list(source_dir, remote_files);
    ThreadPool pool;
    for (auto & file_name : remote_files)
    {
        String full_source_file_path = fs::path(source_dir) / file_name;
        if (hdfs_filesystem->isDirectory(full_source_file_path))
            continue;

        size_t filename_len = file_name.length();
        /// remote part files should always be .zip files.
        if (!(filename_len > 4 && endsWith(file_name, COMPRESSION_SUFFIX) ))
            throw Exception("File has wrong part file format. " + String(full_source_file_path), ErrorCodes::UNKNOWN_FORMAT);

        auto disk_ptr = get_disk_and_local_path(hdfs_filesystem->getFileSize(full_source_file_path));
        // create local directory if not exists.
        if (!disk_ptr->exists(relative_local_path))
            disk_ptr->createDirectory(relative_local_path);

        String full_local_path = fs::path(disk_ptr->getPath()) / relative_local_path ;
        String part_name = std::string(file_name, 0, filename_len - 4);
        fetched.emplace_back(part_name, disk_ptr);

        pool.scheduleOrThrowOnError([&, full_source_file_path, full_local_path, file_name, part_name]()
            {
                try
                {
                    // download zip from from remote.
                    String target_local_file = fs::path(full_local_path) / file_name;
                    getFileFromRemote(full_source_file_path, target_local_file);

                    // decompress zip file.
                    ZipHelper zip;
                    zip.unzipFile(target_local_file, fs::path(full_local_path) / part_name);
                }
                catch (Exception & e)
                {
                    /// remove dirty download file
                    Poco::File dirty_file(fs::path(full_local_path) / file_name);
                    if (dirty_file.exists())
                        dirty_file.remove(true);

                    /// remove dirty part directory
                    Poco::File dirty_part(fs::path(full_local_path) / part_name);
                    if (dirty_part.exists())
                        dirty_part.remove(true);

                    throw e;
                }

            }
        );
    }
    pool.wait();

    return fetched;
}

void HDFSDumper::uploadFileToRemote(const String & local_path, const String & remote_path)
{
    LOG_DEBUG(log, "Uploading local file {} to remote {}", local_path, remote_path);
    // open remote file for write
    WriteBufferFromHDFS write_buffer(remote_path, hdfs_params);

    // open local file for read
    FILE * fin = std::fopen(local_path.data(), "r+");

    if (!fin)
        throw Exception("Cannot open local file " + local_path, ErrorCodes::CANNOT_OPEN_FILE);

    size_t bytes_read = 0;
    PODArray<char> buffer(buffer_size);
    while (true)
    {
        bytes_read = std::fread(buffer.data(), 1, buffer_size, fin);
        if (bytes_read > 0)
            write_buffer.write(buffer.data(), bytes_read);
        else
            break;
    }
    std::fclose(fin);
}

void HDFSDumper::getFileFromRemote(const String & remote_path, const String & local_path)
{
    LOG_DEBUG(log, "Downloading remote file {} to local {}", remote_path, local_path);
    // open remote file for read
    ReadBufferFromByteHDFS read_buffer(remote_path, false, hdfs_params);

    // open local file for write
    FILE * fout = std::fopen(local_path.data(), "w+");
    if (!fout)
        throw Exception("Cannot open local file " + local_path, ErrorCodes::CANNOT_OPEN_FILE);

    ssize_t bytes_read = 0;
    PODArray<char> buffer(buffer_size);

    while (true)
    {
        bytes_read = read_buffer.read(buffer.data(), buffer_size);
        if (bytes_read < 0)
            throw Exception("Fail to read HDFS file: " + remote_path, ErrorCodes::CANNOT_READ_FROM_FILE_DESCRIPTOR);

        if (bytes_read == 0)
            break;

        std::fwrite(buffer.data(), 1, bytes_read, fout);
    }
    std::fclose(fout);
}


}
