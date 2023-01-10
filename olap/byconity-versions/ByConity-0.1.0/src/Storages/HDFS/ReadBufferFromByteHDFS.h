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

#pragma once

#include <assert.h>

#include <Common/config.h>
#include <Common/formatIPv6.h>
#include <Common/ProfileEvents.h>
#include <Common/Stopwatch.h>
#include <Common/Throttler.h>
#include <IO/ReadBuffer.h>
#include <Poco/URI.h>
#include <Poco/String.h>
#include <Poco/Logger.h>
#include <IO/ReadBufferFromFileBase.h>
#include <TableFunctions/ITableFunction.h>
#include <ServiceDiscovery/ServiceDiscoveryFactory.h>
#include <ServiceDiscovery/ServiceDiscoveryConsul.h>
#include <common/logger_useful.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/HDFS/HDFSFileSystem.h>

#ifndef O_DIRECT
#define O_DIRECT 00040000
#endif

#if USE_HDFS
#include <hdfs/hdfs.h>
#include <hdfs/HdfsEvent.h>
#include <random>
#include <shared_mutex>

namespace ProfileEvents
{
    extern const Event NetworkReadBytes;
    extern const Event ReadBufferFromHdfsRead;
    extern const Event ReadBufferFromHdfsReadBytes;
    extern const Event ReadBufferFromHdfsReadFailed;
    extern const Event HdfsFileOpen;
    extern const Event HDFSSeek;
    extern const Event HDFSSeekElapsedMicroseconds;
    extern const Event HDFSReadElapsedMilliseconds;
    extern const Event HdfsGetBlkLocMicroseconds;
    extern const Event HdfsCreateBlkReaderMicroseconds;
    extern const Event HdfsSlowNodeCount;
    extern const Event HdfsFailedNodeCount;
    extern const Event HdfsDnConnectionCount;
    extern const Event HdfsDnConnectionMicroseconds;
    extern const Event HdfsDnConnectionMax;
    extern const Event HdfsReadPacketCount;
    extern const Event HdfsReadPacketMicroseconds;
    extern const Event HdfsReadPacketMax;

}

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int LOGICAL_ERROR;
    extern const int NETWORK_ERROR;
    extern const int CANNOT_OPEN_FILE;
    extern const int CANNOT_SEEK_THROUGH_FILE;
    extern const int POSITION_OUT_OF_BOUND;
    extern const int ALL_CONNECTION_TRIES_FAILED;
}


/** Accepts path to file and opens it, or pre-opened file descriptor.
     * Closes file by himself (thus "owns" a file descriptor).
     */
    class ReadBufferFromByteHDFS : public ReadBufferFromFileBase
    {
        protected:
            String hdfs_name;
            bool pread;
            String fuzzy_hdfs_uri;
            String uriPrefix;
            Strings hdfs_files;
            bool read_all_once;

//            String hdfs_user;
//            String hdfs_nnproxy;
            HDFSConnectionParams hdfs_params;

            std::pair<String, size_t> host_port;

            size_t current_pos;
            UInt64 offset_in_current_file;

            HDFSBuilderPtr builder;
            //            hdfsFS fs;
            //            std::shared_ptr<DB::HDFSFileSystem> fs;
            HDFSFSPtr fs;
            hdfsFile fin;
            ThrottlerPtr total_network_throttler;

        public:
            explicit ReadBufferFromByteHDFS(
                const String & hdfs_name_,
                bool pread_ = false,
                const HDFSConnectionParams & hdfs_params_ = HDFSConnectionParams::defaultNNProxy(),
                size_t buf_size = DBMS_DEFAULT_BUFFER_SIZE,
                char * existing_memory = nullptr,
                size_t alignment = 0,
                bool read_all_once_ = false,
                ThrottlerPtr total_network_throttler_ = nullptr);


            // ReadBufferFromByteHDFS(ReadBufferFromByteHDFS &&) = default;

            ~ReadBufferFromByteHDFS() override;

            /// Close HDFS connection before destruction of object.
            void close();

            void assertValidBuffer(int offset, int size)
            {
                if (internal_buffer.begin() + offset >= internal_buffer.begin() &&
                    internal_buffer.begin() + offset + size <= internal_buffer.end())
                    return;
                throw Exception("Invalid ReadBufferFromByteHDFS offset: " + std::to_string(offset) + " size:" + std::to_string(size), ErrorCodes::POSITION_OUT_OF_BOUND);
            }

            bool nextImpl() override;

            const std::string& getHDFSUri() const
            {
                return fuzzy_hdfs_uri;
            }


            int getFD() const
            {
                return -1;
            }

            std::string getFileName() const override
            {
                return hdfs_name;
            }

            off_t seek(off_t offset, int whence = SEEK_SET) override
            {
               return doSeek(offset, whence);
            }

            off_t getPosition() override
            {
                return getPositionInFile();
            }



        private:
            void tryConnect();
            off_t doSeek(off_t offset, int whence) ;

            /// Get the position of the file corresponding to the data in buffer.
            off_t getPositionInFile()
            {
                return offset_in_current_file - (working_buffer.end() - pos);
            }
    };
}

#endif
