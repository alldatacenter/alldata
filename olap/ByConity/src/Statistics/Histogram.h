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
#include <optional>
#include <set>
#include <Core/Types.h>
#include <Statistics/Bucket.h>
#include <Statistics/CommonErrorCodes.h>
#include <Common/Exception.h>

namespace DB::Statistics
{
/**
 * This class is an implementation of histogram.
 *
 * histogram represents the distribution of a column's values by a sequence of buckets.
 * Each bucket has a value range and contains approximately the same number of rows.
 *
 * - buckets histogram bucket
 */
class Histogram
{
public:
    Histogram(Buckets buckets = {});

    void emplaceBackBucket(Bucket ptr) { buckets.emplace_back(std::move(ptr)); }

    const Buckets & getBuckets() const { return buckets; }

    BucketOpt getBucket(size_t index) const
    {
        if (index < buckets.size())
        {
            return buckets[index];
        }
        return std::nullopt;
    }

    size_t getBucketSize() const { return buckets.size(); }
    bool empty() const { return buckets.empty(); }
    double getTotalCount() const;
    double getTotalNdv() const;
    double getMin() const;
    double getMax() const;

    double estimateEqual(double value) const;
    double estimateLessThanOrLessThanEqualFilter(double value, bool equal) const;
    double estimateGreaterThanOrGreaterThanEqualFilter(double value, bool equal) const;
    Buckets estimateJoin(const Histogram & right, double lower_bound, double upper_bound) const;

    Histogram createEqualFilter(double value) const;
    Histogram createNotEqualFilter(double value) const;
    Histogram createLessThanOrLessThanEqualFilter(double value, bool equal) const;
    Histogram createGreaterThanOrGreaterThanEqualFilter(double value, bool equal) const;
    Histogram createInFilter(std::set<double> & values) const;
    Histogram createNotInFilter(std::set<double> & values) const;
    Histogram createUnion(const Histogram & other) const;
    Histogram createNot(const Histogram & origin) const;

    Histogram applySelectivity(double rowcount_selectivity, double ndv_selectivity) const;

    Histogram copy() const;
    void clear() { buckets.clear(); }

private:
    Buckets buckets;
    void cleanupResidualBucket(BucketOpt & bucket, bool bucket_is_residual) const;
    BucketOpt getNextBucket(const BucketOpt & new_bucket, bool & result_bucket_is_residual, size_t & current_bucket_index) const;
    size_t addResidualUnionAllBucket(Buckets & histogram_buckets, const BucketOpt & bucket, bool bucket_is_residual, size_t index) const;
    static void addBuckets(const Buckets & src_buckets, Buckets & dest_buckets, size_t begin, size_t end);
    bool subsumes(const Bucket & bucket) const;
};

}
