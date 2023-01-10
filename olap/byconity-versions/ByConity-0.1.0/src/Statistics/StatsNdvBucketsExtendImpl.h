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
#include <Statistics/StatsNdvBucketsExtend.h>
#include <Statistics/StatsNdvBucketsResultImpl.h>
#include <Common/Exception.h>

#include <algorithm>
#include <IO/WriteHelpers.h>
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/Base64.h>
#include <Statistics/BucketBoundsImpl.h>
#include <Statistics/DataSketchesHelper.h>
#include <Statistics/StatsCpcSketch.h>
#include <Statistics/StatsKllSketchImpl.h>
#include <boost/algorithm/string/join.hpp>

namespace DB::Statistics
{
template <typename T>
class StatsNdvBucketsExtendImpl : public StatsNdvBucketsExtend
{
public:
    StatsNdvBucketsExtendImpl() = default;

    String serialize() const override;
    void deserialize(std::string_view blob) override;

    void update(const T & value, UInt64 block_value)
    {
        auto bucket_id = bounds_.binarySearchBucket(value);
        counts_[bucket_id] += 1;
        cpc_sketches_[bucket_id].update(value);
        block_cpc_sketches_[bucket_id].update(block_value);
    }

    void merge(const StatsNdvBucketsExtendImpl & rhs)
    {
        if (!bounds_.equals(rhs.bounds_))
        {
            throw Exception("Mismatch Bounds", ErrorCodes::LOGICAL_ERROR);
        }

        for (size_t i = 0; i < numBuckets(); ++i)
        {
            counts_[i] += rhs.counts_[i];
            cpc_sketches_[i].merge(rhs.cpc_sketches_[i]);
            block_cpc_sketches_[i].merge(rhs.block_cpc_sketches_[i]);
        }
    }

    void initialize(BucketBoundsImpl<T> bounds)
    {
        counts_.clear();
        cpc_sketches_.clear();
        auto num_buckets = bounds.numBuckets();
        counts_.resize(num_buckets);
        cpc_sketches_.resize(num_buckets);
        block_cpc_sketches_.resize(num_buckets);
        bounds_ = std::move(bounds);
    }

    SerdeDataType getSerdeDataType() const override { return SerdeDataTypeFrom<T>; }

    auto numBuckets() const { return bounds_.numBuckets(); }

    void checkValid() const;

    const BucketBounds & getBucketBounds() const override { return bounds_; }

    std::vector<UInt64> getCounts() const override { return counts_; }
    std::vector<double> getNdvs() const override { return cpcToNdv(cpc_sketches_); }
    std::vector<double> getBlockNdvs() const override { return cpcToNdv(block_cpc_sketches_); }

private:
    static std::vector<double> cpcToNdv(const std::vector<StatsCpcSketch> & cpcs)
    {
        std::vector<double> result;
        for (auto & cpc : cpcs)
        {
            result.emplace_back(cpc.get_estimate());
        }
        return result;
    }

private:
    BucketBoundsImpl<T> bounds_;
    std::vector<UInt64> counts_; // of size buckets
    std::vector<StatsCpcSketch> cpc_sketches_; // of size buckets
    std::vector<StatsCpcSketch> block_cpc_sketches_; // of size buckets
};

template <typename T>
void StatsNdvBucketsExtendImpl<T>::checkValid() const
{
    bounds_.checkValid();

    if (counts_.size() != numBuckets() || cpc_sketches_.size() != numBuckets())
    {
        throw Exception("counts/cpc size mismatch", ErrorCodes::LOGICAL_ERROR);
    }
}


template <typename T>
String StatsNdvBucketsExtendImpl<T>::serialize() const
{
    checkValid();
    std::ostringstream ss;
    auto serde_data_type = getSerdeDataType();
    ss.write(reinterpret_cast<const char *>(&serde_data_type), sizeof(serde_data_type));
    Protos::StatsNdvBucketsExtend pb;
    pb.set_bounds_blob(bounds_.serialize());

    for (auto & count : counts_)
    {
        pb.add_counts(count);
    }
    for (auto & cpc : cpc_sketches_)
    {
        pb.add_cpc_sketch_blobs(cpc.serialize());
    }
    for (auto & block_cpc : block_cpc_sketches_)
    {
        pb.add_block_cpc_sketch_blobs(block_cpc.serialize());
    }
    pb.SerializeToOstream(&ss);
    return ss.str();
}

template <typename T>
void StatsNdvBucketsExtendImpl<T>::deserialize(std::string_view raw_blob)
{
    std::tie(bounds_, counts_, cpc_sketches_, block_cpc_sketches_) = [raw_blob] {
        if (raw_blob.size() <= sizeof(SerdeDataType))
        {
            throw Exception("corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        SerdeDataType serde_data_type;
        memcpy(&serde_data_type, raw_blob.data(), sizeof(serde_data_type));

        checkSerdeDataType<T>(serde_data_type);

        auto blob = raw_blob.substr(sizeof(serde_data_type), raw_blob.size() - sizeof(serde_data_type));
        Protos::StatsNdvBucketsExtend pb;
        pb.ParseFromArray(blob.data(), blob.size());
        BucketBoundsImpl<T> bounds;
        bounds.deserialize(pb.bounds_blob());
        int64_t num_buckets = bounds.numBuckets();
        if (pb.counts_size() != num_buckets || pb.cpc_sketch_blobs_size() != num_buckets)
        {
            throw Exception("Corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        decltype(counts_) counts(num_buckets);
        decltype(cpc_sketches_) cpc_sketches(num_buckets);
        decltype(block_cpc_sketches_) block_cpc_sketches(num_buckets);
        for (int64_t i = 0; i < num_buckets; ++i)
        {
            counts[i] = pb.counts(i);
            cpc_sketches[i].deserialize(pb.cpc_sketch_blobs(i));
            block_cpc_sketches[i].deserialize(pb.block_cpc_sketch_blobs(i));
        }
        return std::tuple{std::move(bounds), std::move(counts), std::move(cpc_sketches), std::move(block_cpc_sketches)};
    }();
    checkValid();
}


}
