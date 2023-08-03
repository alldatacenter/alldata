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
#include <Processors/Chunk.h>
#include <butil/iobuf.h>
#include <Processors/ISimpleTransform.h>

namespace DB
{
class DeserializeBufTransform : public ISimpleTransform
{
public:

    struct IOBufChunkInfo : public ChunkInfo
    {
        butil::IOBuf io_buf;
    };

    explicit DeserializeBufTransform(const Block & header_, bool enable_block_compress_);

    String getName() const override { return "DeserializeBufTransform"; }

protected:
    void transform(Chunk & chunk) override;

private:
    const Block & header;
    bool enable_block_compress;
    Poco::Logger * logger;

};

}
