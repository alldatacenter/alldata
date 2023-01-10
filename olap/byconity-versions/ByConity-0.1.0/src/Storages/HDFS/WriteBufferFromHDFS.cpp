/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Common/config.h>
#include <Common/ProfileEvents.h>
#include <common/logger_useful.h>
#if USE_HDFS

#include <Interpreters/Context.h>
#include <Storages/HDFS/WriteBufferFromHDFS.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <hdfs/hdfs.h>
#include <Common/ProfileEvents.h>

namespace ProfileEvents
{
    extern const Event NetworkWriteBytes;
    extern const Event WriteBufferFromHdfsWrite;
    extern const Event WriteBufferFromHdfsWriteFailed;
    extern const Event WriteBufferFromHdfsWriteBytes;
    extern const Event HdfsFileOpen;
    extern const Event HDFSWriteElapsedMilliseconds;
}
namespace DB
{

namespace ErrorCodes
{
extern const int NETWORK_ERROR;
extern const int CANNOT_OPEN_FILE;
extern const int CANNOT_FSYNC;
extern const int BAD_ARGUMENTS;
}


struct WriteBufferFromHDFS::WriteBufferFromHDFSImpl
{
    Poco::URI hdfs_uri;
    hdfsFile fout;
    HDFSBuilderWrapper builder;
    HDFSFSPtr fs;
    void openFile( const std::string & hdfs_name_, int flags) {
        ProfileEvents::increment(ProfileEvents::HdfsFileOpen);
        /// We use hdfs_name as path directly to avoid Poco URI escaping character(e.g. %) in hdfs_name.
        std::string path;
        if (hdfs_name_.size() > 0 && hdfs_name_.at(0) == '/')
            path = hdfs_name_;
        else
            path = hdfs_uri.getPath();

        if (path.find_first_of("*?{") != std::string::npos)
            throw Exception(ErrorCodes::CANNOT_OPEN_FILE, "URI '{}' contains globs, so the table is in readonly mode", hdfs_uri.toString());

        if (!hdfsExists(fs.get(), path.c_str()))
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "File {} already exists", path);

        fout = hdfsOpenFile(fs.get(), path.c_str(), flags, 0, 0, 0);     /// O_WRONLY meaning create or overwrite i.e., implies O_TRUNCAT here

        if (fout == nullptr)
        {
            throw Exception("Unable to open HDFS file: " + path + " error: " + std::string(hdfsGetLastError()),
                ErrorCodes::CANNOT_OPEN_FILE);
        }
    }
    explicit WriteBufferFromHDFSImpl(
            const std::string & hdfs_name_,
            const Poco::Util::AbstractConfiguration & config_,
            int flags)
        : hdfs_uri(hdfs_name_)
        , builder(createHDFSBuilder(hdfs_name_, config_))
        , fs(createHDFSFS(builder.get()))
    {
        openFile(hdfs_name_,flags);
    }

    explicit WriteBufferFromHDFSImpl(
        const std::string & hdfs_name_,
        const HDFSConnectionParams & hdfsParams,
        int flags
    ): hdfs_uri(hdfs_name_), builder(hdfsParams.createBuilder(hdfs_uri)),fs(createHDFSFS(builder.get())){
        openFile(hdfs_name_,flags);
    }

    ~WriteBufferFromHDFSImpl()
    {
        int ec = hdfsCloseFile(fs.get(), fout);
        if (ec != 0)
        {
            const char * underlying_err_msg = hdfsGetLastError();
            std::string underlying_err_str = underlying_err_msg ? std::string(underlying_err_msg) : "unknown error";
            LOG_ERROR(&Poco::Logger::get(__PRETTY_FUNCTION__), "failed to close file {}, errno: {}, reason: {} ", hdfs_uri.toString(), ec, underlying_err_str);
        }
    }


    int write(const char * start, size_t size) const
    {
        ProfileEvents::increment(ProfileEvents::WriteBufferFromHdfsWrite);
        int bytes_written = hdfsWrite(fs.get(), fout, start, size);
        if (bytes_written < 0)
        {
            ProfileEvents::increment(ProfileEvents::WriteBufferFromHdfsWriteFailed);
            throw Exception("Fail to write HDFS file: " + hdfs_uri.toString() + " " + std::string(hdfsGetLastError()),
                ErrorCodes::NETWORK_ERROR);
        }
        return bytes_written;
    }

    void sync() const
    {
        int result = hdfsSync(fs.get(), fout);
        if (result < 0)
            throwFromErrno("Cannot HDFS sync" + hdfs_uri.toString() + " " + std::string(hdfsGetLastError()),
                ErrorCodes::CANNOT_FSYNC);
    }
};

WriteBufferFromHDFS::WriteBufferFromHDFS(
    const std::string & hdfs_name_, const Poco::Util::AbstractConfiguration & config_, size_t buf_size_, int flags_)
    : WriteBufferFromFileBase(buf_size_, nullptr, 0)
    , impl(std::make_unique<WriteBufferFromHDFSImpl>(hdfs_name_, config_, flags_))
    , hdfs_name(hdfs_name_)
{
}


WriteBufferFromHDFS::WriteBufferFromHDFS(
    const std::string & hdfs_name_, const HDFSConnectionParams & hdfs_params, const size_t buf_size_, int flag)
    : WriteBufferFromFileBase(buf_size_, nullptr, 0)
    , impl(std::make_unique<WriteBufferFromHDFSImpl>(hdfs_name_, hdfs_params, flag))
    , hdfs_name(hdfs_name_)
{
}

void WriteBufferFromHDFS::nextImpl()
{
    if (!offset())
        return;
    Stopwatch watch;

    size_t bytes_written = 0;

    while (bytes_written != offset())
        bytes_written += impl->write(working_buffer.begin() + bytes_written, offset() - bytes_written);

    ProfileEvents::increment(ProfileEvents::WriteBufferFromHdfsWriteBytes, bytes_written);
    watch.stop();
    ProfileEvents::increment(ProfileEvents::HDFSWriteElapsedMilliseconds, watch.elapsedMilliseconds());
}


void WriteBufferFromHDFS::sync()
{
    impl->sync();
}

std::string WriteBufferFromHDFS::getFileName() const
{
    return hdfs_name;
}

off_t WriteBufferFromHDFS::getPositionInFile()
{
    return hdfsTell(impl->fs.get(), impl->fout);
}

void WriteBufferFromHDFS::finalize()
{
    try
    {
        next();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}


WriteBufferFromHDFS::~WriteBufferFromHDFS()
{
    finalize();
}

}
#endif
