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
#include <Statistics/StatsNdvBuckets.h>
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
class StatsNdvBucketsImpl : public StatsNdvBuckets
{
public:
    StatsNdvBucketsImpl() = default;

    String serialize() const override;
    void deserialize(std::string_view blob) override;

    void update(const T & value)
    {
        auto bucket_id = bounds.binarySearchBucket(value);
        counts[bucket_id] += 1;
        cpc_sketches[bucket_id].update(value);
    }

    void merge(const StatsNdvBucketsImpl & rhs)
    {
        if (!bounds.equals(rhs.bounds))
        {
            throw Exception("Mismatch Kll Sketch", ErrorCodes::LOGICAL_ERROR);
        }

        for (size_t i = 0; i < numBuckets(); ++i)
        {
            counts[i] += rhs.counts[i];
            cpc_sketches[i].merge(rhs.cpc_sketches[i]);
        }
    }

    void setCount(size_t bucket_id, uint64_t count) { counts[bucket_id] = count; }

    void set_cpc(size_t bucket_id, StatsCpcSketch cpc) { cpc_sketches[bucket_id] = std::move(cpc); }

    void initialize(BucketBoundsImpl<T> bounds_)
    {
        counts.clear();
        cpc_sketches.clear();
        auto num_buckets = bounds_.numBuckets();
        counts.resize(num_buckets);
        cpc_sketches.resize(num_buckets);
        bounds = std::move(bounds_);
    }

    SerdeDataType getSerdeDataType() const override { return SerdeDataTypeFrom<T>; }

    auto numBuckets() const { return bounds.numBuckets(); }

    void checkValid() const;

    const BucketBounds & getBucketBounds() const override { return bounds; }

    uint64_t getCount(size_t bucket_id) const { return counts[bucket_id]; }
    double getNdv(size_t bucket_id) const { return cpc_sketches[bucket_id].get_estimate(); }
    std::shared_ptr<StatsNdvBucketsResultImpl<T>> asResultImpl() const;

    std::shared_ptr<StatsNdvBucketsResult> asResult() const override { return asResultImpl(); }

private:
    BucketBoundsImpl<T> bounds;
    std::vector<uint64_t> counts; // of size buckets
    std::vector<StatsCpcSketch> cpc_sketches; // of size buckets
};

template <typename T>
void StatsNdvBucketsImpl<T>::checkValid() const
{
    bounds.checkValid();

    if (counts.size() != numBuckets() || cpc_sketches.size() != numBuckets())
    {
        throw Exception("counts/cpc size mismatch", ErrorCodes::LOGICAL_ERROR);
    }
}


template <typename T>
String StatsNdvBucketsImpl<T>::serialize() const
{
    checkValid();
    std::ostringstream ss;
    auto serde_data_type = getSerdeDataType();
    ss.write(reinterpret_cast<const char *>(&serde_data_type), sizeof(serde_data_type));
    Protos::StatsNdvBuckets pb;
    pb.set_bounds_blob(bounds.serialize());

    for (auto & count : counts)
    {
        pb.add_counts(count);
    }
    for (auto & cpc : cpc_sketches)
    {
        pb.add_cpc_sketch_blobs(cpc.serialize());
    }
    pb.SerializeToOstream(&ss);
    return ss.str();
}

template <typename T>
void StatsNdvBucketsImpl<T>::deserialize(std::string_view raw_blob)
{
    std::tie(bounds, counts, cpc_sketches) = [raw_blob] {
        if (raw_blob.size() <= sizeof(SerdeDataType))
        {
            throw Exception("corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        SerdeDataType serde_data_type;
        memcpy(&serde_data_type, raw_blob.data(), sizeof(serde_data_type));

        checkSerdeDataType<T>(serde_data_type);

        auto blob = raw_blob.substr(sizeof(serde_data_type), raw_blob.size() - sizeof(serde_data_type));
        Protos::StatsNdvBuckets pb;
        pb.ParseFromArray(blob.data(), blob.size());
        BucketBoundsImpl<T> bounds_;
        bounds_.deserialize(pb.bounds_blob());
        int64_t num_buckets = bounds_.numBuckets();
        if (pb.counts_size() != num_buckets || pb.cpc_sketch_blobs_size() != num_buckets)
        {
            throw Exception("Corrupted blob", ErrorCodes::LOGICAL_ERROR);
        }
        decltype(counts) counts_(num_buckets);
        decltype(cpc_sketches) cpc_sketches_(num_buckets);
        for (int64_t i = 0; i < num_buckets; ++i)
        {
            counts_[i] = pb.counts(i);
            auto & cpc_blob = pb.cpc_sketch_blobs(i);
            cpc_sketches_[i].deserialize(cpc_blob);
        }
        return std::tuple{std::move(bounds_), std::move(counts_), std::move(cpc_sketches_)};
    }();
    checkValid();
}

template <typename T>
std::shared_ptr<StatsNdvBucketsResultImpl<T>> StatsNdvBucketsImpl<T>::asResultImpl() const
{
    std::vector<double> ndvs;
    for (auto & cpc : this->cpc_sketches)
    {
        auto ndv = cpc.get_estimate();
        ndvs.emplace_back(ndv);
    }
    return StatsNdvBucketsResultImpl<T>::createImpl(this->bounds, this->counts, std::move(ndvs));
}

}
