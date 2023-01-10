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

#include <Processors/Transforms/QueryCacheTransform.h>
#include <Common/PODArray.h>

namespace DB
{

QueryCacheTransform::QueryCacheTransform(const Block & header,
                                         const QueryCachePtr & query_cache_,
                                         const UInt128 & query_key_,
                                         const QueryResultPtr & query_result_,
                                         const std::set<String> & ref_db_and_table_,
                                         UInt64 update_time_)
    : ISimpleTransform(header, header, false),
    query_cache(query_cache_), query_key(query_key_), query_result(query_result_),
    ref_db_and_table(ref_db_and_table_), update_time(update_time_)
{

}

QueryCacheTransform::~QueryCacheTransform()
{
    setQueryCache();
}

void QueryCacheTransform::setQueryCache()
{
    // How to update cache:
    // 1. Each database:table pair can have multiple queries
    // 2. Each query can be referenced by multiple database:table pair
    // Thus, we insert database:table and key for multiple times

    if (!isCancelled() && query_cache && query_key && query_result)
    {
        query_result->setUpdateTime(update_time);

        // cache query only when it has reference tables, otherwise we cannot drop this query
        if (!ref_db_and_table.empty())
            query_cache->set(query_key, query_result);

        for (const auto & name : ref_db_and_table)
            query_cache->insert(name, query_key);
    }
}

void QueryCacheTransform::transform(Chunk & chunk)
{
    if (!query_result)
        return;

    query_result->addResult(chunk);
}

}
