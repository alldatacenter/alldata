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
#include <Processors/ISource.h>
#include <Processors/QueryCache.h>

namespace DB
{

class SourceFromQueryCache : public ISource
{
public:
    SourceFromQueryCache(const Block & header,
                         const QueryResultPtr & query_result_)
        : ISource(std::move(header)), query_result(query_result_) {}

    String getName() const override { return "SourceFromQueryCache"; }

protected:
    Chunk generate() override
    {
        if (query_result)
            return query_result->getChunk();

        return Chunk();
    }

private:
    QueryResultPtr query_result = nullptr;
};

}
