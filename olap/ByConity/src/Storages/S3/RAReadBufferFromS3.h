/*
 * Copyright 2023 Bytedance Ltd. and/or its affiliates.
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

#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>
#include <IO/HTTPCommon.h>
#include <IO/ReadBuffer.h>
#include <IO/S3Common.h>
#include <IO/ReadBufferFromFileBase.h>
#include <Common/Throttler.h>
#include <aws/s3/model/GetObjectResult.h>
#include <sys/types.h>

namespace Aws::S3
{
class S3Client;
}

namespace DB
{

class S3ReadAheadReader
{
public:
    S3ReadAheadReader(const std::shared_ptr<Aws::S3::S3Client>& client_,
        const String& bucket_, const String& key_, Poco::Logger* logger_,
        int max_buffer_expand_times_ = 8);

    bool read(BufferBase::Buffer& buffer_, size_t size_);
    size_t seek(size_t offset_);

    off_t offset() const { return static_cast<off_t>(current_offset); }

    String bucket() const { return obj_bucket; }
    String key() const { return obj_key; }

private:
    bool refillBuffer();
    void updateBufferSize(size_t size_);
    bool readFromBuffer(BufferBase::Buffer& buffer_, size_t size_);

    std::shared_ptr<Aws::S3::S3Client> client;
    String obj_bucket;
    String obj_key;

    size_t buffer_size;
    int max_buffer_expand_times;
    int buffer_expand_times;

    // Offset of first byte in reader
    size_t current_offset;

    // End offset of reader result
    size_t reader_end_offset;
    std::unique_ptr<Aws::S3::Model::GetObjectResult> reader;

    Poco::Logger* logger;
};

class RAReadBufferFromS3: public ReadBufferFromFileBase
{
public:
    RAReadBufferFromS3(const std::shared_ptr<Aws::S3::S3Client>& client_,
        const String& bucket_, const String& key_, size_t read_retry_ = 3,
        size_t buffer_size_ = DBMS_DEFAULT_BUFFER_SIZE, char* existing_memory_ = nullptr,
        size_t alignment_ = 0, const ThrottlerPtr& throttler_ = nullptr);

    virtual bool nextImpl() override;

    virtual off_t seek(off_t off_, int whence_) override;

    virtual off_t getPosition() override
    {
        return reader.offset() - (working_buffer.end() - pos);
    }

    virtual String getFileName() const override
    {
        return reader.bucket() + "/" + reader.key();
    }

private:
    size_t read_retry;
    ThrottlerPtr throttler;

    Poco::Logger* log;

    S3ReadAheadReader reader;
};

}
