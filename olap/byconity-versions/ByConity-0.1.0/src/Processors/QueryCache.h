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

#include <Core/Block.h>
#include <Processors/Chunk.h>
#include <Common/LRUCache.h>
#include <Common/ProfileEvents.h>
#include <Common/SipHash.h>
#include <Interpreters/AggregationCommon.h>
#include <Core/Settings.h>
#include <Core/QueryProcessingStage.h>
#include <IO/WriteBufferFromString.h>
#include <queue>


namespace ProfileEvents
{
    extern const Event QueryCacheHits;
    extern const Event QueryCacheMisses;
}

namespace DB
{

struct QueryKey
{
    const String query;
    String settings_string;
    String stage;

    QueryKey(const String & query_, const Settings & settings_, const QueryProcessingStage::Enum & stage_)
        : query(query_)
    {
        WriteBufferFromString buffer(settings_string);
        settings_.write(buffer);
        stage = QueryProcessingStage::toString(stage_);
    }

    String toString()
    {
        return query+" : "+settings_string+ " : "+stage;
    }
};

struct QueryResult;
using QueryResultPtr = std::shared_ptr<QueryResult>;

struct QueryResult
{
    UInt64 update_time;
    std::queue<ChunkPtr> result;
    size_t bytes = 0;

    QueryResult()= default;

    inline void setUpdateTime(UInt64 time)
    {
        update_time = time;
    }

    void addResult(Chunk & chunk)
    {
        bytes += chunk.bytes();
        auto chunk_ptr = std::make_shared<Chunk>(chunk.clone());
        result.push(chunk_ptr);
    }

    Chunk getChunk()
    {
        if (result.empty())
            return Chunk();

        auto res = result.front();
        result.pop();
        bytes -= res->bytes();

        return res->clone();
    }

    QueryResultPtr clone() const
    {
        QueryResultPtr res = std::make_shared<QueryResult>();
        res->result = result;
        res->bytes = bytes;
        return res;
    }
};

using QueryKeyPtr = std::shared_ptr<QueryKey>;

struct QueryWeightFunction
{
    size_t operator()(const QueryResult & query_result) const
    {
        return query_result.bytes;
    }
};

/*
*   QueryCache consists of two level structures.
*   The first level structure is QueryContainer, which is a multi_index structure for storing relationship between
*   database:table and queries.
*/

class QueryCache : public LRUCache<UInt128, QueryResult, UInt128TrivialHash, QueryWeightFunction>
{
private:

    using Base = LRUCache<UInt128, QueryResult, UInt128TrivialHash, QueryWeightFunction>;

public:

    explicit QueryCache(size_t max_size_in_bytes): Base(max_size_in_bytes)
    {
        inner_container = std::make_unique<CacheContainer<Key>>();
    }

    static UInt128 hash(const QueryKey & query_key)
    {
        UInt128 key;

        SipHash hash;
        hash.update(query_key.query.data(), query_key.query.size());
        hash.update(query_key.settings_string.data(), query_key.settings_string.size());
        hash.update(query_key.stage.data(), query_key.stage.size());
        hash.get128(key);

        return key;
    }

    template <typename LoadFunc>
    MappedPtr getOrSet(const Key & key, LoadFunc && load) const
    {
        auto result = Base::getOrSet(key, load);
        if (result.second)
            ProfileEvents::increment(ProfileEvents::QueryCacheMisses);
        else
            ProfileEvents::increment(ProfileEvents::QueryCacheHits);

        return result.first;
    }

    void insert(const String & name, const Key & key, const MappedPtr & mapped)
    {
        Base::set(key, mapped);
        if (inner_container)
            inner_container->insert(name, key);
    }

    void insert(const String & name, const Key & key)
    {
        if (inner_container)
            inner_container->insert(name, key);
    }

    void dropQueryCache(const String & name)
    {
        if (!inner_container)
            return;

        const auto & keys = inner_container->getKeys(name);
        for (const auto & key : keys)
            remove(key);
    }
};

using QueryCachePtr = std::shared_ptr<QueryCache>;

}

