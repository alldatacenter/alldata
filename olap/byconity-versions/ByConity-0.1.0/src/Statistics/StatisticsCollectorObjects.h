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
#include <Statistics/StatsColumnBasic.h>
#include <Statistics/StatsCpcSketch.h>
#include <Statistics/StatsKllSketch.h>
#include <Statistics/StatsNdvBuckets.h>
#include <Statistics/StatsTableBasic.h>

namespace DB::Statistics
{
namespace StatisticsImpl
{
    struct TableStats
    {
    public:
        std::shared_ptr<StatsTableBasic> basic;

    public:
        // TODO: use reflection to eliminate this manual code
        StatsCollection writeToCollection() const
        {
            std::unordered_map<StatisticsTag, StatisticsBasePtr> collection;
            if (basic)
                collection.emplace(basic->getTag(), basic);
            return collection;
        }

        // TODO: use reflection to eliminate this manual code
        void readFromCollection(const StatsCollection & collection)
        {
            auto tag = StatisticsTag::TableBasic;
            if (collection.count(tag))
                basic = std::static_pointer_cast<StatsTableBasic>(collection.at(tag));
        }
    };

    struct ColumnStats
    {
    public:
        std::shared_ptr<StatsNdvBucketsResult> ndv_buckets_result;
        // basic contains ndv and histogram bounds, a.k.a. cpc/kll sketch
        std::shared_ptr<StatsColumnBasic> basic;

    public:
        // TODO: use reflection to eliminate this manual code
        StatsCollection writeToCollection() const
        {
            std::unordered_map<StatisticsTag, StatisticsBasePtr> collection;

            auto list = std::vector<StatisticsBasePtr>{ndv_buckets_result, basic};

            for (auto & ptr : list)
            {
                if (ptr)
                    collection.emplace(ptr->getTag(), ptr);
            }
            return collection;
        }

        // TODO: use reflection to eliminate this manual code
        void readFromCollection(const StatsCollection & collection)
        {
            auto handle = [&]<typename T>(std::shared_ptr<T> & field) {
                using StatsType = T;
                constexpr auto tag = StatsType::tag;
                if (collection.count(tag))
                {
                    field = std::static_pointer_cast<StatsType>(collection.at(tag));
                }
            };

            handle(ndv_buckets_result);
            handle(basic);
        }
    };

    using ColumnStatsMap = std::map<String, ColumnStats>;
} // namespace StatisticsImpl
}
