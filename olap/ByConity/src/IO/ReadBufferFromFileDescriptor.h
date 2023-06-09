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

#include <memory>

#include <IO/ReadBufferFromFileBase.h>

#include <unistd.h>


namespace DB
{

class Context;

/// Most used types have shorter names
using ContextPtr = std::shared_ptr<const Context>;
/** Use ready file descriptor. Does not open or close a file.
  */
class ReadBufferFromFileDescriptor : public ReadBufferFromFileBase
{
protected:
    int fd;
    size_t file_offset_of_buffer_end; /// What offset in file corresponds to working_buffer.end().

    bool nextImpl() override;

    /// Name or some description of file.
    std::string getFileName() const override;

public:
    ReadBufferFromFileDescriptor(int fd_, size_t buf_size = DBMS_DEFAULT_BUFFER_SIZE, char * existing_memory = nullptr, size_t alignment = 0)
        : ReadBufferFromFileBase(buf_size, existing_memory, alignment), fd(fd_), file_offset_of_buffer_end(0) {}

    int getFD() const
    {
        return fd;
    }

    off_t getPosition() override
    {
        return file_offset_of_buffer_end - (working_buffer.end() - pos);
    }

    /// If 'offset' is small enough to stay in buffer after seek, then true seek in file does not happen.
    off_t seek(off_t off, int whence) override;

    /// Seek to the beginning, discarding already read data if any. Useful to reread file that changes on every read.
    void rewind();

    off_t size();

    void setProgressCallback(ContextPtr context);
private:
    /// Assuming file descriptor supports 'select', check that we have data to read or wait until timeout.
    bool poll(size_t timeout_microseconds);
};

}
