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

#include <Storages/S3/RAReadBufferFromS3.h>

#include <cassert>
#include <chrono>
#include <memory>
#include <thread>
#include <aws/s3/model/GetObjectRequest.h>
#include <aws/s3/S3Client.h>
#include <Core/Types.h>
#include <IO/S3Common.h>
#include <common/logger_useful.h>
#include <common/scope_guard.h>

namespace ProfileEvents
{
    extern const Event ReadBufferFromS3Read;
    extern const Event ReadBufferFromS3ReadFailed;
    extern const Event ReadBufferFromS3ReadBytes;
    extern const Event ReadBufferFromS3ReadMicro;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int S3_ERROR;
    extern const int BAD_ARGUMENTS;
}

S3ReadAheadReader::S3ReadAheadReader(const std::shared_ptr<Aws::S3::S3Client>& client_,
    const String& bucket_, const String& key_, Poco::Logger* logger_,
    int max_buffer_expand_times_):
        client(client_), obj_bucket(bucket_), obj_key(key_), buffer_size(0),
        max_buffer_expand_times(max_buffer_expand_times_), buffer_expand_times(0),
        current_offset(0), reader_end_offset(0), reader(std::make_unique<Aws::S3::Model::GetObjectResult>()),
        logger(logger_)
{}

bool S3ReadAheadReader::read(BufferBase::Buffer& buffer_, size_t size_)
{
    // Reader already has no data, update buffer size and refill buffer
    if (current_offset == reader_end_offset)
    {
        updateBufferSize(size_);

        if (!refillBuffer())
        {
            return false;
        }
    }

    return readFromBuffer(buffer_, size_);
}

void S3ReadAheadReader::updateBufferSize(size_t size_)
{
    if (buffer_size == 0)
    {
        buffer_size = size_;
    }
    else
    {
        if (max_buffer_expand_times > buffer_expand_times)
        {
            buffer_size = (buffer_size * 3) / 2;
            ++buffer_expand_times;
        }
    }

    LOG_TRACE(logger, "Update buffer size to {}, Buffer expand times {}/{}",
        buffer_size, buffer_expand_times, max_buffer_expand_times);
}

bool S3ReadAheadReader::refillBuffer()
{
    SCOPE_EXIT({
        LOG_TRACE(logger, "Refill buffer for {} from {} to {}", obj_key,
            current_offset, reader_end_offset);
    });

    ProfileEvents::increment(ProfileEvents::ReadBufferFromS3Read, 1);
    ProfileEvents::increment(ProfileEvents::ReadBufferFromS3ReadBytes, buffer_size);

    String range = "bytes=" + std::to_string(current_offset) + "-" \
        + std::to_string(current_offset + buffer_size - 1);

    Aws::S3::Model::GetObjectRequest req;
    req.SetBucket(obj_bucket);
    req.SetKey(obj_key);
    req.SetRange(range);

    Aws::S3::Model::GetObjectOutcome outcome = client->GetObject(req);

    if (outcome.IsSuccess())
    {
        *reader = outcome.GetResultWithOwnership();
        reader_end_offset += reader->GetContentLength();

        return true;
    }
    else
    {
        if (outcome.GetError().GetResponseCode() == Aws::Http::HttpResponseCode::REQUESTED_RANGE_NOT_SATISFIABLE)
        {
            return false;
        }
        throw S3::S3Exception(outcome.GetError());
    }
}

// When encounter exception, reset reader's stream
bool S3ReadAheadReader::readFromBuffer(BufferBase::Buffer& buffer, size_t size)
{
    try
    {
        Aws::IOStream& stream = reader->GetBody();
        stream.read(buffer.begin(), size);
        size_t last_read_count = stream.gcount();
        if (!last_read_count)
        {
            if (stream.eof())
                return false;

            if (stream.fail())
                throw Exception(fmt::format("Cannot read from input stream while reading {}",
                    obj_key), ErrorCodes::S3_ERROR);

            throw Exception(fmt::format("Unexpected state of input stream while reading {}",
                obj_key), ErrorCodes::S3_ERROR);
        }

        LOG_TRACE(logger, "Read {} bytes from reader, offset: {}, reader end offset: {}",
            last_read_count, current_offset, reader_end_offset);

        buffer.resize(last_read_count);
        current_offset += last_read_count;
        return true;
    }
    catch (...)
    {
        // Read from stream failed, reset reader
        buffer_size = 0;
        buffer_expand_times = 0;
        reader_end_offset = current_offset;

        LOG_TRACE(logger, "Read from input stream failed, reset read ahead state, offset: {}",
            current_offset);

        throw;
    }
}

size_t S3ReadAheadReader::seek(size_t offset)
{
    if (offset >= current_offset)
    {
        if (offset == reader_end_offset)
        {
            return offset;
        }
        else if (offset < reader_end_offset)
        {
            reader->GetBody().ignore(offset - current_offset);
            current_offset = offset;
            return offset;
        }
    }

    // Cache miss, reset read ahead state
    LOG_TRACE(logger, "Seek to outside input stream, reset read ahead state, offset: {}",
        offset);

    buffer_size = 0;
    buffer_expand_times = 0;
    current_offset = offset;
    reader_end_offset = current_offset;
    return offset;
}

RAReadBufferFromS3::RAReadBufferFromS3(const std::shared_ptr<Aws::S3::S3Client>& client_,
    const String& bucket_, const String& key_, size_t read_retry_,
    size_t buffer_size_, char* existing_memory_, size_t alignment_, const ThrottlerPtr& throttler_):
        ReadBufferFromFileBase(buffer_size_, existing_memory_, alignment_),
        read_retry(read_retry_), throttler(throttler_), log(&Poco::Logger::get("RAReadBufferFromS3")),
        reader(client_, bucket_, key_, log) {}

bool RAReadBufferFromS3::nextImpl()
{
    auto sleep_time_with_backoff_milliseconds = std::chrono::milliseconds(100);
    for (size_t attempt = 0; true; ++attempt)
    {
        try
        {
            bool more = false;
            {
                Stopwatch watch;
                SCOPE_EXIT({
                    auto time = watch.elapsedMicroseconds();
                    ProfileEvents::increment(ProfileEvents::ReadBufferFromS3ReadMicro, time);
                });

                more = reader.read(buffer(), internalBuffer().size());
            }

            if (throttler)
            {
                throttler->add(buffer().size());
            }

            return more;
        }
        catch (const Exception&)
        {
            ProfileEvents::increment(ProfileEvents::ReadBufferFromS3ReadFailed, 1);

            tryLogCurrentException(log);

            if (attempt >= read_retry)
                throw;

            std::this_thread::sleep_for(sleep_time_with_backoff_milliseconds);
            sleep_time_with_backoff_milliseconds *= 2;
        }
    }
}

off_t RAReadBufferFromS3::seek(off_t off, int whence)
{
    if (whence != SEEK_SET)
    {
        throw Exception(fmt::format("ReadBufferFromS3::seek expects SEEK_SET as whence while reading {}",
            reader.key()), ErrorCodes::BAD_ARGUMENTS);
    }

    off_t buffer_start_offset = reader.offset() - static_cast<off_t>(working_buffer.size());
    if (hasPendingData() && off <= reader.offset() && off >= buffer_start_offset)
    {
        pos = working_buffer.begin() + off - buffer_start_offset;
        return off;
    }
    else
    {
        pos = working_buffer.end();
        return reader.seek(off);
    }
}

}
