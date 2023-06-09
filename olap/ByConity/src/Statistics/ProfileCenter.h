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

#if false
#    include <atomic>
#    include <unordered_map>
#    include <Core/Types.h>
#    include <boost/core/noncopyable.hpp>
#    include <fmt/format.h>
#    include <Common/Exception.h>
#    include <common/logger_useful.h>

namespace DB::ErrorCodes
{
extern const int LOGICAL_ERROR;
}

namespace DB::Statistics
{

enum class RecordKey
{
    CatalogTime,
    DeserializeTime,
    TagFetchingTime,
    LRUCachedTime,
    FullTime,
    EndOfList
};

inline std::string getRecordKeyName(RecordKey key)
{
    switch (key)
    {
        case RecordKey::CatalogTime:
            return "CatalogTime";
        case RecordKey::DeserializeTime:
            return "DeserializeTime";
        case RecordKey::TagFetchingTime:
            return "TagFetchingTime";
        case RecordKey::LRUCachedTime:
            return "LRUCachedTime";
        case RecordKey::FullTime:
            return "FullTime";
        default:
            throw Exception("Unknown record key", ErrorCodes::LOGICAL_ERROR);
    }
}

class ProfileCenter : boost::noncopyable
{
public:
    ProfileCenter()
    {
        for (int i = 0; i < static_cast<int>(RecordKey::EndOfList); ++i)
        {
            records.emplace(static_cast<RecordKey>(i), 0);
        }
    }
    ~ProfileCenter() = default;

    // this is thread safe
    void append(RecordKey key, double time) { records.at(key) += time; }

    void reset()
    {
        for (auto & [k, v] : records)
        {
            v = 0;
        }
    }

    String get_summary()
    {
        String result;
        for (auto & [key, time] : records)
        {
            result += fmt::format("{}->{} \n", getRecordKeyName(key), time);
        }
        return result;
    }

    static ProfileCenter & globalInstance();

private:
    std::unordered_map<RecordKey, std::atomic<double>> records;
};


}
#endif
