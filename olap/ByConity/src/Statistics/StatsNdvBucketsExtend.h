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
#include <DataTypes/IDataType.h>
#include <Statistics/StatisticsBaseImpl.h>
#include <Common/Exception.h>

#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/Base64.h>
#include <Statistics/StatsKllSketch.h>
#include <Statistics/StatsNdvBucketsResult.h>


namespace DB::Statistics
{
template <typename T>
class StatsNdvBucketsExtendImpl;
template <typename T>
class StatsNdvBucketsResultImpl;
class BucketBounds;

class StatsNdvBucketsExtend : public StatisticsBase
{
public:
    static constexpr auto tag = StatisticsTag::NdvBucketsExtend;

    StatisticsTag getTag() const override { return tag; }

    virtual SerdeDataType getSerdeDataType() const = 0;

    template <typename T>
    using Impl = StatsNdvBucketsExtendImpl<T>;

    virtual const BucketBounds & getBucketBounds() const = 0;

    virtual std::vector<UInt64> getCounts() const = 0;
    virtual std::vector<double> getNdvs() const = 0;
    virtual std::vector<double> getBlockNdvs() const = 0;

private:
};


}
