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

#include <IO/WriteBuffer.h>
#include <butil/iobuf.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int DISTRIBUTE_STAGE_QUERY_EXCEPTION;
}

/// Zero-copy write buffer from butil::IOBuf of brpc library.
/// Add a member IOBuf::epxand(size_t hint) for simplifying code, and very few performance gain
class WriteBufferFromBrpcBuf : public WriteBuffer
{
public:
    WriteBufferFromBrpcBuf() : WriteBuffer(nullptr, 0)
    {
        auto block_view = buf.expand(initial_size);
        if (block_view.empty())
            throw Exception("Cannot resize butil::IOBuf to " + std::to_string(initial_size), ErrorCodes::DISTRIBUTE_STAGE_QUERY_EXCEPTION);
        set(const_cast<Position>(block_view.data()), block_view.size());
    }

    ~WriteBufferFromBrpcBuf() override { finish(); }

    void nextImpl() override
    {
        if (is_finished)
            throw Exception("WriteBufferFromBrpcBuf is finished", ErrorCodes::CANNOT_WRITE_AFTER_END_OF_BUFFER);

        auto block_view = buf.expand(buf.size());
        if (block_view.empty())
            throw Exception("Cannot resize butil::IOBuf to " + std::to_string(initial_size), ErrorCodes::DISTRIBUTE_STAGE_QUERY_EXCEPTION);
        set(const_cast<Position>(block_view.data()), block_view.size());
    }

    void finish()
    {
        if (is_finished)
            return;
        is_finished = true;

        buf.resize(buf.size() - available());
        /// Prevent further writes.
        set(nullptr, 0);
    }

    const auto & getIntermediateBuf() const { return buf; }

    const auto & getFinishedBuf()
    {
        finish();
        return buf;
    }

private:
    static constexpr size_t initial_size = 32;

    butil::IOBuf buf;
    bool is_finished = false;
};
}
