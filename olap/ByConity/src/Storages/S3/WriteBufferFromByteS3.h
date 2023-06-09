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

#include <memory>
#include <vector>
#include <common/logger_useful.h>

#include <IO/BufferWithOwnMemory.h>
#include <IO/WriteBuffer.h>
#include <IO/WriteBufferFromFileBase.h>
#include <IO/S3Common.h>

#include <aws/core/utils/memory/stl/AWSStringStream.h> // Y_IGNORE

namespace Aws::S3
{
class S3Client;
}

namespace DB
{

/**
 * Buffer to write a data to a S3 object with specified bucket and key.
 * If data size written to the buffer is less than 'max_single_part_upload_size' write is performed using singlepart upload.
 * In another case multipart upload is used:
 * Data is divided on chunks with size greater than 'minimum_upload_part_size'. Last chunk can be less than this threshold.
 * Each chunk is written as a part to S3.
 */
class WriteBufferFromByteS3 : public WriteBufferFromFileBase
{
private:
    String key;
    std::optional<std::map<String, String>> object_metadata;
    S3::S3Util s3_util;

    UInt64 max_single_put_threshold;
    UInt64 min_segment_size;

    /// Buffer to accumulate data.
    std::shared_ptr<Aws::StringStream> temporary_buffer;
    UInt64 last_part_size;
    UInt64 total_write_size;

    /// Upload in S3 is made in parts.
    /// We initiate upload, then upload each part and get ETag as a response, and then finish upload with listing all our parts.
    String multipart_upload_id;
    std::vector<String> part_tags;

    Poco::Logger * log;

public:
    // For default settings in 16M segment, max part is about 156G
    explicit WriteBufferFromByteS3(
        const std::shared_ptr<Aws::S3::S3Client>& client_,
        const String& bucket_,
        const String& key_,
        UInt64 max_single_put_threshold_ = 16 * 1024 * 1024,
        UInt64 min_segment_size_ = 16 * 1024 * 1024,
        std::optional<std::map<String, String>> object_metadata_ = std::nullopt,
        size_t buf_size_ = DBMS_DEFAULT_BUFFER_SIZE,
        char* mem_ = nullptr,
        size_t alignment_ = 0,
        bool allow_overwrite_ = false);

    ~WriteBufferFromByteS3() override;

    /// Receives response from the server after sending all data.
    void finalize() override;

    void nextImpl() override;

    off_t getPositionInFile() ;

    void sync() override;

    String getFileName() const override
    {
        return s3_util.getBucket() + "/" + key;
    }

    static int getFD()  
    {
        return -1;
    }
private:
    bool finalized = false;

    void allocateBuffer();
    void clearBuffer();

    void createMultipartUpload();
    void writePart();
    void completeMultipartUpload();
    void abortMultipartUpload();

    void makeSinglepartUpload();
};

}
