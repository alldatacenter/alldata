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
#include <chrono>
#include <memory>
#include <string_view>
#include <unordered_map>
#include <Core/Block.h>
#include <Core/Types.h>
#include <Statistics/CommonErrorCodes.h>
#include <Statistics/StatisticsCommon.h>

namespace DB::Statistics
{
enum class StatisticsTag : UInt64
{
    Invalid = 0,

    TableBasic = 1,
    CpcSketch = 2,
    KllSketch = 3,
    NdvBuckets = 4, // including bounds, min/max and ndv(cpc object) for each buckets
    NdvBucketsResult = 5, // including bounds, min/max and ndv(double value) for each buckets
    ColumnBasic = 6, // now put min/max here
    NdvBucketsExtend = 7, // including bounds, min/max and ndv(cpc object), block_ndv for each buckets

    // for test only
    DummyAlpha = 2000,
    DummyBeta = 2001,
};

class StatisticsBase
{
public:
    // get type of statistics
    virtual StatisticsTag getTag() const = 0;

    // serialize as binary blob
    // note: timestamp is not included
    virtual String serialize() const = 0;

    // deserialize from binary blob
    // note: timestamp is not included
    virtual void deserialize(std::string_view blob) = 0;

    //serialize as json
    virtual String serializeToJson() const { return String{}; }

    //deserialize from json
    virtual void deserializeFromJson(std::string_view) { }

    StatisticsBase() = default;
    StatisticsBase(const StatisticsBase &) = default;
    StatisticsBase(StatisticsBase &&) = default;
    StatisticsBase & operator=(const StatisticsBase &) = default;
    StatisticsBase & operator=(StatisticsBase &&) = default;

    virtual ~StatisticsBase() = default;
};

using StatisticsBasePtr = std::shared_ptr<StatisticsBase>;
using StatsCollection = std::unordered_map<StatisticsTag, StatisticsBasePtr>;

struct StatsData
{
    StatsCollection table_stats;
    std::unordered_map<String, StatsCollection> column_stats;
};

// helper function to create statistics object from binary blob
StatisticsBasePtr createStatisticsBase(StatisticsTag tag, std::string_view blob);
//helper function to create statistics object from json string
StatisticsBasePtr createStatisticsBaseFromJson(StatisticsTag tag, std::string_view blob);
}
