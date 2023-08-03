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

#pragma once

#include <Common/config.h>

#if USE_HDFS
#include <IO/WriteBufferFromFileBase.h>
#include <Storages/HDFS/HDFSFileSystem.h>
#include <string>
#include <memory>


namespace DB
{
/** Accepts HDFS path to file and opens it.
 * Closes file by himself (thus "owns" a file descriptor).
 */
class WriteBufferFromHDFS final : public WriteBufferFromFileBase
{

public:
    WriteBufferFromHDFS(
        const std::string & hdfs_name_,
        const Poco::Util::AbstractConfiguration & config_,
        size_t buf_size_ = DBMS_DEFAULT_BUFFER_SIZE,
        int flags = O_WRONLY);


    WriteBufferFromHDFS(
        const std::string & hdfs_name_,
        const HDFSConnectionParams & hdfs_params = HDFSConnectionParams::defaultNNProxy(),
        const size_t buf_size = DBMS_DEFAULT_BUFFER_SIZE,
        int flag = O_WRONLY);

    ~WriteBufferFromHDFS() override;

    void nextImpl() override;

    void sync() override;
    off_t getPositionInFile();
    std::string getFileName() const override;
    void finalize() override;

private:
    struct WriteBufferFromHDFSImpl;
    std::unique_ptr<WriteBufferFromHDFSImpl> impl;
    std::string hdfs_name;
};

}
#endif
