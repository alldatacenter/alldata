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

#include <string>
#include <Processors/Exchange/DataTrans/DataTransKey.h>
#include <fmt/core.h>

namespace DB
{
class ExchangeDataKey : public DataTransKey
{
public:
    ExchangeDataKey(
        String query_id_, UInt64 write_segment_id_, UInt64 read_segment_id_, UInt64 parallel_index_, String coordinator_address_)
        : query_id(std::move(query_id_))
        , write_segment_id(write_segment_id_)
        , read_segment_id(read_segment_id_)
        , parallel_index(parallel_index_)
        , coordinator_address(std::move(coordinator_address_))
    {
    }

    ~ExchangeDataKey() override = default;

    String getKey() const override
    {
        return query_id + "_" + std::to_string(write_segment_id) + "_" + std::to_string(read_segment_id) + "_"
            + std::to_string(parallel_index) + "_" + coordinator_address;
    }

    String dump() const override
    {
        return fmt::format(
            "ExchangeDataKey: [query_id: {}, write_segment_id: {}, read_segment_id: {}, parallel_index: {}, coordinator_address: {}]",
            query_id,
            write_segment_id,
            read_segment_id,
            parallel_index,
            coordinator_address);
    }

    inline const String & getQueryId() const { return query_id; }

    inline const String & getCoordinatorAddress() const { return coordinator_address; }

    inline UInt64 getWriteSegmentId() const {return write_segment_id;}

    inline UInt64 getReadSegmentId() const {return read_segment_id;}

    inline UInt64 getParallelIndex() const {return parallel_index;}

private:
    String query_id;
    UInt64 write_segment_id;
    UInt64 read_segment_id;
    UInt64 parallel_index;
    String coordinator_address;
};
}
