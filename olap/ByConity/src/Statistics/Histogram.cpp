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

#include <Statistics/Histogram.h>

#include <optional>
#include <utility>
#include <vector>
#include <Optimizer/Utils.h>

namespace DB::Statistics
{
Histogram::Histogram(Buckets buckets_) : buckets(std::move(buckets_))
{
}

double Histogram::getTotalNdv() const
{
    double count = 0;
    for (const auto & bucket : buckets)
    {
        count += bucket.getNumDistinct();
    }
    return count;
}

double Histogram::getTotalCount() const
{
    double count = 0;
    for (const auto & bucket : buckets)
    {
        count += bucket.getCount();
    }
    return count;
}

double Histogram::getMin() const
{
    if (buckets.empty())
    {
        return 0;
    }
    double min = buckets[0].getLowerBound();
    for (size_t i = 1; i < buckets.size(); i++)
    {
        if (min > buckets[i].getLowerBound())
        {
            min = buckets[i].getLowerBound();
        }
    }
    return min;
}

double Histogram::getMax() const
{
    if (buckets.empty())
    {
        return 0;
    }
    double max = buckets[0].getUpperBound();
    for (size_t i = 1; i < buckets.size(); i++)
    {
        if (max < buckets[i].getUpperBound())
        {
            max = buckets[i].getUpperBound();
        }
    }
    return max;
}

double Histogram::estimateEqual(double value) const
{
    if (getTotalCount() == 0)
        return 0;

    for (const auto & bucket : buckets)
    {
        if (bucket.contains(value) && bucket.getNumDistinct() > 0)
        {
            return ((1.0 / bucket.getNumDistinct()) * bucket.getCount()) / getTotalCount();
        }
    }
    return 0.0;
}

double Histogram::estimateLessThanOrLessThanEqualFilter(double value, bool equal) const
{
    if (getTotalCount() == 0)
        return 0;
    UInt64 hit_count = 0;
    for (const auto & bucket : buckets)
    {
        // ----- point ----- lower_bound -----
        if (bucket.isBefore(value))
        {
            break;
        }
        // ----- upper_bound ----- point -----
        else if (bucket.isAfter(value) || (equal && bucket.isUpperClosed() && bucket.getUpperBound() == value))
        {
            hit_count += bucket.getCount();
        }
        else
        {
            // ----- lower_bound ----- point ----- upper_bound  ----
            double overlap = bucket.getOverlapPercentage(value);
            hit_count += overlap * bucket.getCount();
        }
    }
    return static_cast<double>(hit_count) / getTotalCount();
}

double Histogram::estimateGreaterThanOrGreaterThanEqualFilter(double value, bool equal) const
{
    UInt64 hit_count = 0;
    for (const auto & bucket : buckets)
    {
        // ----- point ----- lower_bound -----
        if (bucket.isBefore(value) || (equal && bucket.isLowerClosed() && bucket.getLowerBound() == value))
        {
            hit_count += bucket.getCount();
        }
        // ----- upper_bound ----- point -----
        else if (bucket.isAfter(value))
        {
            continue;
        }
        else
        {
            // ----- lower_bound ----- point ----- upper_bound  ----
            double overlap = 1.0 - bucket.getOverlapPercentage(value);
            hit_count += bucket.getCount() * overlap;
        }
    }
    return static_cast<double>(hit_count) / getTotalCount();
}

Buckets Histogram::estimateJoin(const Histogram & right_histogram, double lower_bound, double upper_bound) const
{
    // Only bins whose range intersect [lower_bound, upper_bound] have join possibility.
    std::vector<Bucket> left_buckets;
    for (const auto & bucket : this->getBuckets())
    {
        if (bucket.getLowerBound() <= upper_bound && bucket.getUpperBound() >= lower_bound)
        {
            left_buckets.emplace_back(bucket);
        }
    }
    std::vector<Bucket> right_buckets;
    for (const auto & bucket : right_histogram.getBuckets())
    {
        if (bucket.getLowerBound() <= upper_bound && bucket.getUpperBound() >= lower_bound)
        {
            right_buckets.emplace_back(bucket);
        }
    }

    OverlappedRanges overlaps;
    size_t left_index = 0;
    size_t right_index = 0;
    while (left_index < left_buckets.size() && right_index < right_buckets.size())
    {
        auto & left_bucket = left_buckets[left_index];
        Bucket left = left_bucket.trim(lower_bound, upper_bound);

        auto & right_bucket = right_buckets[right_index];
        Bucket right = right_bucket.trim(lower_bound, upper_bound);

        // Only collect overlapped ranges.
        bool intersects = Bucket::intersects(left, right);

        if (intersects)
        {
            auto res = Bucket::compareUpperBounds(left, right);
            if (0 == res)
            {
                // both ubs are equal
                left_index++;
                right_index++;
            }
            else if (1 > res)
            {
                // bucket1's ub is smaller than that of the ub of bucket2
                left_index++;
            }
            else
            {
                right_index++;
            }
        }
        else
        {
            if (Bucket::isBefore(left, right))
            {
                // buckets do not intersect there one bucket is before the other
                left_index++;
            }
            else
            {
                right_index++;
            }
            continue;
        }

        OverlappedRange overlap = left.makeBucketIntersect(right);
        overlaps.emplace_back(overlap);
    }

    Buckets join_buckets;
    for (auto range : overlaps)
    {
        // Apply the formula in this overlapped range.
        double range_ndv = std::max(range.left_ndv, range.right_ndv);
        double range_count = range.left_rows * range.right_rows;

        if (range_ndv > 0)
        {
            auto bucket = Bucket(range.lower_bound, range.upper_bound, range_ndv, range_count / range_ndv, true, true);
            join_buckets.emplace_back(bucket);
        }
    }

    return join_buckets;
}

Histogram Histogram::createEqualFilter(double value) const
{
    Buckets new_buckets;
    for (const auto & bucket : buckets)
    {
        if (bucket.contains(value))
        {
            if (bucket.isSingleton())
            {
                new_buckets.emplace_back(bucket);
            }
            else
            {
                Bucket new_bucket = bucket.makeBucketSingleton(value);
                new_buckets.emplace_back(new_bucket);
            }
        }
    }
    return Histogram{std::move(new_buckets)};
}

Histogram Histogram::createNotEqualFilter(double value) const
{
    std::vector<Bucket> new_buckets;
    for (const auto & bucket : buckets)
    {
        if (bucket.contains(value))
        {
            // bucket is singleton, and contains, pass
            if (bucket.isSingleton())
            {
                continue;
            }
            else
            {
                if (auto new_bucket_1 = bucket.makeBucketScaleUpper(value, false))
                {
                    new_buckets.emplace_back(std::move(new_bucket_1.value()));
                }
                if (auto new_bucket_2 = bucket.makeBucketScaleLower(value, false))
                {
                    new_buckets.emplace_back(std::move(new_bucket_2.value()));
                }
            }
        }
        else
        {
            new_buckets.emplace_back(bucket);
        }
    }
    return Histogram{new_buckets};
}

Histogram Histogram::createLessThanOrLessThanEqualFilter(double value, bool equal) const
{
    std::vector<Bucket> new_buckets;
    for (const auto & bucket : buckets)
    {
        // ----- point ----- lower_bound -----
        if (bucket.isBefore(value))
        {
            break;
        }
        // ----- upper_bound ----- point -----
        else if (bucket.isAfter(value) || (equal && bucket.isUpperClosed() && bucket.getUpperBound() == value))
        {
            new_buckets.emplace_back(bucket);
        }
        else
        {
            // ----- lower_bound ----- point ----- upper_bound  ----
            BucketOpt new_bucket = bucket.makeBucketScaleUpper(value, equal);
            if (new_bucket)
            {
                new_buckets.emplace_back(std::move(new_bucket.value()));
            }
        }
    }
    return Histogram{new_buckets};
}

Histogram Histogram::createGreaterThanOrGreaterThanEqualFilter(double value, bool equal) const
{
    std::vector<Bucket> new_buckets;
    for (const auto & bucket : buckets)
    {
        // ----- point ----- lower_bound -----
        if (bucket.isBefore(value) || (equal && bucket.isLowerClosed() && bucket.getLowerBound() == value))
        {
            new_buckets.emplace_back(bucket);
        }
        // ----- upper_bound ----- point -----
        else if (bucket.isAfter(value))
        {
            continue;
        }
        else
        {
            // ----- lower_bound ----- point ----- upper_bound  ----
            if (auto new_bucket = bucket.makeBucketScaleLower(value, equal))
            {
                new_buckets.emplace_back(std::move(new_bucket.value()));
            }
        }
    }
    return Histogram{new_buckets};
}

Histogram Histogram::createInFilter(std::set<double> & values) const
{
    Buckets new_buckets;
    for (const auto & value : values)
    {
        for (const auto & bucket : buckets)
        {
            if (bucket.contains(value))
            {
                if (bucket.isSingleton())
                {
                    new_buckets.emplace_back(bucket);
                }
                else
                {
                    Bucket new_bucket = bucket.makeBucketSingleton(value);
                    new_buckets.emplace_back(new_bucket);
                }
            }
        }
    }
    return Histogram{new_buckets};
}

Histogram Histogram::createNotInFilter(std::set<double> & values) const
{
    Buckets new_buckets;
    for (const auto & value : values)
    {
        for (const auto & bucket : buckets)
        {
            if (bucket.contains(value))
            {
                // bucket is singleton, and contains, pass
                if (bucket.isSingleton())
                {
                    continue;
                }
                else
                {
                    if (auto new_bucket_1 = bucket.makeBucketScaleUpper(value, false))
                    {
                        new_buckets.emplace_back(std::move(new_bucket_1.value()));
                    }
                    if (auto new_bucket_2 = bucket.makeBucketScaleLower(value, false))
                    {
                        new_buckets.emplace_back(std::move(new_bucket_2.value()));
                    }
                }
            }
            else
            {
                new_buckets.emplace_back(bucket);
            }
        }
    }
    return Histogram{new_buckets};
}

Histogram Histogram::createUnion(const Histogram & other) const
{
    Buckets new_buckets;

    size_t idx1 = 0; // index on buckets from this histogram
    size_t idx2 = 0; // index on buckets from other histogram

    if (buckets.empty())
    {
        return other;
    }

    if (other.getBucketSize() == 0)
    {
        return Histogram{};
    }

    auto bucket1_first = this->getBucket(idx1);
    auto bucket2_first = other.getBucket(idx2);

    BucketOpt bucket1 = std::move(bucket1_first);
    BucketOpt bucket2 = std::move(bucket2_first);

    // flags to determine if the buckets where residue of the bucket-merge operation
    bool bucket1_is_residual = false;
    bool bucket2_is_residual = false;

    while (bucket1 && bucket2)
    {
        if (Bucket::isBefore(bucket1.value(), bucket2.value()))
        {
            new_buckets.emplace_back(bucket1.value());
            cleanupResidualBucket(bucket1, bucket1_is_residual);
            idx1++;
            bucket1 = this->getBucket(idx1);
            bucket1_is_residual = false;
        }
        else if (Bucket::isBefore(bucket2.value(), bucket1.value()))
        {
            new_buckets.emplace_back(bucket2.value());
            cleanupResidualBucket(bucket2, bucket2_is_residual);
            idx2++;
            bucket2 = other.getBucket(idx2);
            bucket2_is_residual = false;
        }
        else
        {
            Utils::checkState(Bucket::intersects(bucket1.value(), bucket2.value()));
            auto [merge_bucket, bucket1_new, bucket2_new] = bucket1.value().makeBucketMerged(bucket2.value());
            new_buckets.emplace_back(merge_bucket);

            Utils::checkState(!bucket1_new || !bucket2_new);

            cleanupResidualBucket(bucket1, bucket1_is_residual);
            cleanupResidualBucket(bucket2, bucket2_is_residual);

            bucket1 = getNextBucket(bucket1_new, bucket1_is_residual, idx1);
            bucket2 = other.getNextBucket(bucket2_new, bucket2_is_residual, idx2);
        }
    }

    size_t buckets1 = getBucketSize();
    size_t buckets2 = other.getBucketSize();

    Utils::assertIff(!bucket1, idx1 == buckets1);
    Utils::assertIff(!bucket2, idx2 == buckets2);

    idx1 = addResidualUnionAllBucket(new_buckets, bucket1, bucket1_is_residual, idx1);
    idx2 = addResidualUnionAllBucket(new_buckets, bucket2, bucket2_is_residual, idx2);

    cleanupResidualBucket(bucket1, bucket1_is_residual);
    cleanupResidualBucket(bucket2, bucket2_is_residual);

    // add any leftover buckets from this histogram
    addBuckets(getBuckets(), new_buckets, idx1, buckets1);

    // add any leftover buckets from other histogram
    addBuckets(other.getBuckets(), new_buckets, idx2, buckets2);
    return Histogram{new_buckets};
}

Histogram Histogram::createNot(const Histogram & origin) const
{
    Buckets not_buckets;
    for (auto & origin_bucket : origin.buckets)
    {
        if (!subsumes(origin_bucket))
        {
            not_buckets.emplace_back(origin_bucket);
        }
    }
    return Histogram{not_buckets};
}

// cleanup residual buckets
void Histogram::cleanupResidualBucket(BucketOpt & bucket, bool bucket_is_residual) const
{
    if (bucket && bucket_is_residual)
    {
        bucket = {};
    }
}

// get the next bucket for union / union all
BucketOpt Histogram::getNextBucket(const BucketOpt & new_bucket, bool & result_bucket_is_residual, size_t & current_bucket_index) const
{
    if (new_bucket)
    {
        result_bucket_is_residual = true;
        return new_bucket;
    }

    current_bucket_index = current_bucket_index + 1;
    result_bucket_is_residual = false;

    return getBucket(current_bucket_index);
}

// add residual bucket in the union all operation to the array of buckets in the histogram
size_t
Histogram::addResidualUnionAllBucket(Buckets & histogram_buckets, const BucketOpt & bucket, bool bucket_is_residual, size_t index) const
{
    if (bucket_is_residual)
    {
        Utils::checkState(bucket.has_value());
        histogram_buckets.emplace_back(bucket.value());
        return index + 1;
    }

    return index;
}

// add buckets from one array to another
void Histogram::addBuckets(const Buckets & src_buckets, Buckets & dest_buckets, size_t begin, size_t end)
{
    Utils::checkState(begin <= end);
    Utils::checkState(end <= src_buckets.size());

    for (size_t ul = begin; ul < end; ul++)
    {
        Bucket bucket = src_buckets[ul];
        dest_buckets.emplace_back(bucket);
    }
}

Histogram Histogram::applySelectivity(double rowcount_selectivity, double ndv_selectivity) const
{
    Buckets new_buckets;
    for (const auto & bucket : buckets)
    {
        new_buckets.emplace_back(bucket.applySelectivity(rowcount_selectivity, ndv_selectivity));
    }
    return Histogram{new_buckets};
}

Histogram Histogram::copy() const
{
    Buckets new_buckets;
    for (const auto & bucket : buckets)
    {
        new_buckets.emplace_back(bucket);
    }
    return Histogram{new_buckets};
}

bool Histogram::subsumes(const Bucket & origin_bucket) const
{
    for (auto & bucket : buckets)
    {
        if (Bucket::subsumes(bucket, origin_bucket))
        {
            return true;
        }
    }
    return false;
}

}
