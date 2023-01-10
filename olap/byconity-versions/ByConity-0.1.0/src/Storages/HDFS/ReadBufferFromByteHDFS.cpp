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

#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include "Common/ProfileEvents.h"
#include <common/logger_useful.h>
namespace DB
{
//static thread_local int64_t maxDnConnection = 0;
//static thread_local int64_t maxReadPacket = 0;

static void ReadBufferFromHdfsCallBack(const hdfsEvent & event)
{
    LOG_TRACE(&Poco::Logger::get("ReadBufferFromHDFS"), "get event {} & {}", event.eventType, event.value);
    switch (event.eventType)
    {
        case Hdfs::Event::HDFS_EVENT_SLOWNODE:
            ProfileEvents::increment(ProfileEvents::HdfsSlowNodeCount, 1);
            break;
        case Hdfs::Event::HDFS_EVENT_FAILEDNODE:
            ProfileEvents::increment(ProfileEvents::HdfsFailedNodeCount, 1);
            break;
        case Hdfs::Event::HDFS_EVENT_GET_BLOCK_LOCATION:
            ProfileEvents::increment(ProfileEvents::HdfsGetBlkLocMicroseconds, event.value / 1000);
            break;
//        case Hdfs::Event::HDFS_EVENT_CREATE_BLOCK_READER:
//            ProfileEvents::increment(ProfileEvents::HdfsCreateBlkReaderMicroseconds, event.value/1000);
//            break;
//        case Hdfs::Event::HDFS_EVENT_DN_CONNECTION: {
////            LOG_INFO(&Logger::get("ReadBufferFromHDFS"), "received event " << event.eventType);
//            ProfileEvents::increment(ProfileEvents::HdfsDnConnectionCount, 1);
//            ProfileEvents::increment(ProfileEvents::HdfsDnConnectionMicroseconds, event.value / 1000);
//            int64_t val = std::max(maxDnConnection, event.value / 1000);
//            ProfileEvents::increment(ProfileEvents::HdfsDnConnectionMax, val - maxDnConnection);
//            maxDnConnection = val;
//            break;
//        }
//        case Hdfs::Event::HDFS_EVENT_READ_PACKET: {
//            ProfileEvents::increment(ProfileEvents::HdfsReadPacketCount, 1);
//            ProfileEvents::increment(ProfileEvents::HdfsReadPacketMicroseconds, event.value / 1000);
//            int64_t val = std::max(maxReadPacket, event.value / 1000);
//            ProfileEvents::increment(ProfileEvents::HdfsReadPacketMax, val - maxReadPacket);
//            maxReadPacket = val;
//            break;
//        }
        default:
            break;
//                LOG_TRACE(&Logger::get("ReadBufferFromHDFS"), "unused hdfs event type "<< event.eventType);
    }
}

ReadBufferFromByteHDFS::ReadBufferFromByteHDFS(
    const String & hdfs_name_,
    bool pread_,
    const HDFSConnectionParams & hdfs_params_,
    size_t buf_size,
    char * existing_memory,
    size_t alignment,
    bool read_all_once_,
    ThrottlerPtr total_network_throttler_)
    : ReadBufferFromFileBase(buf_size, existing_memory, alignment)
    , hdfs_name(hdfs_name_)
    , pread(pread_)
    , fuzzy_hdfs_uri(hdfs_name_)
    , hdfs_files{}
    , read_all_once(read_all_once_)
    , hdfs_params(hdfs_params_)
    , current_pos(0)
    , offset_in_current_file(0)
    , builder(nullptr)
    // for nnproxy, we create a filesystem for each readbuffer. Otherwise, we use the global filesystem.
    , fs(hdfs_params_.conn_type == HDFSConnectionParams::CONN_NNPROXY ? nullptr : DB::getDefaultHdfsFileSystem()->getFS())
    , fin{nullptr}
    , total_network_throttler(total_network_throttler_)
{
    String fuzzyFileNames;
    uriPrefix = hdfs_name_.substr(0, fuzzy_hdfs_uri.find_last_of('/'));
    if (uriPrefix.length() == fuzzy_hdfs_uri.length())
    {
        fuzzyFileNames = fuzzy_hdfs_uri;
        uriPrefix.clear();
    }
    else
    {
        uriPrefix += "/";
        fuzzyFileNames = fuzzy_hdfs_uri.substr(uriPrefix.length());
    }

    Poco::URI uri(uriPrefix); // not include file name

    Strings fuzzyNameList = parseDescription(fuzzyFileNames, 0, fuzzyFileNames.length(), ',', 100 /* hard coded max files */);
    std::vector<Strings> fileNames;
    for (auto fuzzyName : fuzzyNameList)
        fileNames.push_back(parseDescription(fuzzyName, 0, fuzzyName.length(), '|', 100));
    for (auto & vecNames : fileNames)
    {
        for (auto & name : vecNames)
        {
            hdfs_files.push_back(std::move(name));
        }
    }

    builder = hdfs_params.createBuilder(uri);
}

ReadBufferFromByteHDFS::~ReadBufferFromByteHDFS()
{
    close();
}

void ReadBufferFromByteHDFS::close()
{
    hdfsCloseFile(fs.get(), fin);
    fin = nullptr;
}

bool ReadBufferFromByteHDFS::nextImpl()
{
    int offset = 0;
    int size = internal_buffer.size();
    Stopwatch watch(profile_callback ? clock_type : CLOCK_MONOTONIC);
    // If hdfs connection is not trigger, do it now
    tryConnect();

    do
    {
        ProfileEvents::increment(ProfileEvents::ReadBufferFromHdfsRead);
        assertValidBuffer(offset, size);
        int done = -1;
        if (pread)
            done = hdfsPRead(fs.get(), fin, internal_buffer.begin() + offset, size);
        else
            done = hdfsRead(fs.get(), fin, internal_buffer.begin() + offset, size);
        if (done < 0)
        {
            ProfileEvents::increment(ProfileEvents::ReadBufferFromHdfsReadFailed);
            throw Exception(
                "Fail to read HDFS file: " + hdfs_files[current_pos] + " " + String(hdfsGetLastError()), ErrorCodes::CANNOT_OPEN_FILE);
        }

        // Update position in current file
        offset_in_current_file += done;

        if (done)
        {
            ///ProfileEvents::increment(ProfileEvents::NetworkReadBytes, done);
            ProfileEvents::increment(ProfileEvents::ReadBufferFromHdfsReadBytes, done);
            if (total_network_throttler)
            {
                total_network_throttler->add(done);
            }
            if (!read_all_once)
            {
                working_buffer.resize(done);
                break;
            }
            else
            {
                offset += done;
                size -= done;
                if (size > 0)
                {
                    continue;
                }
                else
                {
                    working_buffer.resize(offset);
                    break;
                }
            }
        }
        else
        {
            // reach eof of one file, go to next file if there is
            if (current_pos < hdfs_files.size() - 1)
            {
                ++current_pos;
                offset_in_current_file = 0;
                close(); // close current hdfs file
                Poco::URI uri(uriPrefix + hdfs_files[current_pos]);
                auto & path = uri.getPath();
                int retry = 0;
                do
                {
                    ProfileEvents::increment(ProfileEvents::HdfsFileOpen);
                    fin = hdfsOpenFile(fs.get(), path.c_str(), O_RDONLY, 0, 0, 0, ReadBufferFromHdfsCallBack);
                    if (retry++ > 1)
                        break; // retry one more time in case namenode instable
                } while (fin == nullptr);
            }
            else
            {
                return false;
            }
        }

    } while (true);

    watch.stop();
    ProfileEvents::increment(ProfileEvents::HDFSReadElapsedMilliseconds, watch.elapsedMilliseconds());
    return true;
}

void ReadBufferFromByteHDFS::tryConnect()
{

    if(fs == nullptr) {
        int retry = 0 ;
        Poco::URI uri(uriPrefix);
        do{
            // add retry on failure.
            fs = createHDFSFS(builder.get());
            if (fs == nullptr)
            {
                hdfs_params.setNNProxyBroken();
                hdfs_params.lookupOnNeed();
                builder = hdfs_params.createBuilder(uri);
                if(retry++ >2) {
                    throw Exception(
                        "Unable to connect to HDFS with " + hdfs_params.toString() + " error: " + String(hdfsGetLastError()),
                        ErrorCodes::ALL_CONNECTION_TRIES_FAILED);
                }
            }
        } while(fs ==nullptr);
    }

    if ( fin == nullptr)
    {
        int retry = 0;
        Poco::URI uri(uriPrefix + hdfs_files[current_pos]);
        auto & path = uri.getPath();
        do
        {
            ProfileEvents::increment(ProfileEvents::HdfsFileOpen);
            fin = hdfsOpenFile(fs.get(), path.c_str(), O_RDONLY, 0, 0, 0, ReadBufferFromHdfsCallBack);
            if (retry++ > 1)
                break; // retry one more time in case namenode instable
        } while (fin == nullptr);

        if (fin == nullptr)
        {
            const char * last_err = hdfsGetLastError();
            String err_msg = last_err == nullptr ? "" : String(last_err);
            throw Exception("ReadBufferFromByteHDFS::cannot open file, " + err_msg, ErrorCodes::CANNOT_OPEN_FILE);
        }
    }
}

off_t ReadBufferFromByteHDFS::doSeek(off_t offset, int whence)
{
    size_t new_pos;
    if (whence == SEEK_SET)
    {
        assert(offset >= 0);
        new_pos = offset;
    }
    else if (whence == SEEK_CUR)
    {
        /// calculate seek position in file based on current position in file
        /// offset_in_current_file: pos in file of 'working buffer end'
        size_t pos_in_file = offset_in_current_file - (working_buffer.end() - pos);
        new_pos = pos_in_file + offset;
    }
    else
    {
        throw Exception("ReadBufferFromByteHDFS::seek expects SEEK_SET or SEEK_CUR as whence", ErrorCodes::ARGUMENT_OUT_OF_BOUND);
    }

    tryConnect();

    size_t pos_in_file = offset_in_current_file;
    // Seek to current position
    if (new_pos + (working_buffer.end() - pos) == pos_in_file)
        return new_pos;

    if (hasPendingData() && new_pos <= pos_in_file && new_pos >= pos_in_file - working_buffer.size())
    {
        /// Position is still inside buffer.
        pos = working_buffer.begin() + (new_pos - (pos_in_file - working_buffer.size()));
        return new_pos;
    }
    else
    {
        ProfileEvents::increment(ProfileEvents::HDFSSeek);
        Stopwatch watch;

        pos = working_buffer.end();
        if (!hdfsSeek(fs.get(), fin, new_pos))
        {
            offset_in_current_file = new_pos;
            ProfileEvents::increment(ProfileEvents::HDFSSeekElapsedMicroseconds, watch.elapsedMicroseconds());
            return new_pos;
        }
        else
        {
            const char * last_err = hdfsGetLastError();
            String err_msg = last_err == nullptr ? "" : String(last_err);
            throw Exception("ReadBufferFromByteHDFS::cannot seek to the offset, " + err_msg, ErrorCodes::CANNOT_SEEK_THROUGH_FILE);
        }
    }
    // return -1;
}


}
