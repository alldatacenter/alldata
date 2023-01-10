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
#include <algorithm>
#include <IO/WriteHelpers.h>
#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/Base64.h>
#include <Statistics/BucketBounds.h>
#include <Statistics/SerdeUtils.h>

namespace DB::Statistics
{
template <typename T>
class StatsNdvBucketsResultImpl;

class StatsNdvBucketsResult : public StatisticsBase
{
public:
    static constexpr auto tag = StatisticsTag::NdvBucketsResult;

    StatisticsTag getTag() const override { return tag; }

    virtual SerdeDataType getSerdeDataType() const = 0;

    template <typename T>
    using Impl = StatsNdvBucketsResultImpl<T>;

    // TODO: use visitor pattern to hide implementations
    virtual void writeSymbolStatistics(SymbolStatistics & symbol) = 0;
    virtual const BucketBounds & getBucketBounds() const = 0;

    virtual UInt64 getCount(size_t bucket_id) const = 0;
    virtual double getNdv(size_t bucket_id) const = 0;

    virtual void setCount(size_t bucket_id, UInt64 count) = 0;
    virtual void setNdv(size_t bucket_id, double ndv) = 0;
    virtual size_t numBuckets() const = 0;


    static std::shared_ptr<StatsNdvBucketsResult> create(const BucketBounds & bounds, std::vector<UInt64> counts, std::vector<double> ndvs);

private:
};


} // namespace DB
