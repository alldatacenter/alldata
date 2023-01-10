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

#include <memory>
#include <utility>
#include <Core/Types.h>

namespace DB::Statistics
{
struct OverlappedRange;

class Bucket;
using BucketPtr = std::shared_ptr<Bucket>;
using Buckets = std::vector<BucketPtr>;
struct OverlappedRange;
using OverlappedRanges = std::vector<OverlappedRange>;

/**
 * A join between two histograms may produce multiple overlapped ranges.
 * Each overlapped range is produced by a part of one bin in the left histogram and a part of
 * one bin in the right histogram.
 * - lower_bound lower bound of this overlapped range.
 * - upper_bound higher bound of this overlapped range.
 * - left_ndv ndv in the left part.
 * - right_ndv ndv in the right part.
 * - left_rows number of rows in the left part.
 * - right_rows number of rows in the right part.
 */
struct OverlappedRange
{
    Float64 lower_bound;
    Float64 upper_bound;
    double left_ndv;
    double right_ndv;
    double left_rows;
    double right_rows;
};

/**
 * A bucket in an histogram. We use double type for lower/higher bound for simplicity.
 */
class Bucket : public std::enable_shared_from_this<Bucket>
{
private:
    // lower bound of bucket
    double lower_bound;

    // upper bound of bucket
    double upper_bound;

    // number of distinct elements in bucket
    double ndv;

    // number of elements in bucket
    double count;

    // is lower bound closed (does bucket include boundary value)
    bool lower_bound_inclusive;

    // is upper bound closed (does bucket includes boundary value)
    bool upper_bound_inclusive;

    // private copy constructor
    Bucket(const Bucket &);

    // private assignment operator
    Bucket & operator=(const Bucket &);

public:
    Bucket(double lower_bound, double upper_bound, double ndv, double count, bool lower_bound_inclusive, bool upper_bound_inclusive);

    // is bucket singleton?
    bool isSingleton() const;

    // does bucket contain point
    bool contains(const double point) const;

    // is the point before the lower bound of the bucket
    bool isBefore(const double point) const;

    // is the point after the upper bound of the bucket
    bool isAfter(const double point) const;

    // what percentage of bucket is covered by [lb,pp]
    double getOverlapPercentage(const double point) const;

    // Distance between two points
    double distance(double point1, double point2) const;

    // row count associated with bucket
    double getCount() const { return count; }

    // width of bucket
    double width() const;

    // set count
    void setHeight(double count_) { count = count_; }

    // set number of distinct values
    void setNDV(double distinct) { ndv = distinct; }

    // number of distinct values in bucket
    double getNumDistinct() const { return ndv; }

    // lower point
    double getLowerBound() const { return lower_bound; }

    // upper point
    double getUpperBound() const { return upper_bound; }

    double range() const
    {
        if (isSingleton())
            return 1;

        double result = upper_bound - lower_bound;
        if (lower_bound_inclusive)
        {
            result++;
        }
        if (upper_bound_inclusive)
        {
            result++;
        }
        return result;
    }

    // is lower bound closed (does bucket includes boundary value)
    bool isLowerClosed() const { return lower_bound_inclusive; }

    // is upper bound closed (does bucket includes boundary value)
    bool isUpperClosed() const { return upper_bound_inclusive; }

    BucketPtr trim(double lower_bound_, double upper_bound_);

    // does bucket's range intersect another's
    static bool intersects(const BucketPtr & bucket1, const BucketPtr & bucket2);

    // does bucket's range subsume another's
    static bool subsumes(const BucketPtr & bucket1, const BucketPtr & bucket2);

    // does bucket occur before another
    static bool isBefore(const BucketPtr & bucket1, const BucketPtr & bucket2);

    // does bucket occur after another
    static bool isAfter(const BucketPtr & bucket1, const BucketPtr & bucket2);

    // construct new bucket with lower bound greater than given point
    BucketPtr makeBucketGreaterThan(double point) const;

    // scale down version of bucket adjusting upper boundary
    BucketPtr makeBucketScaleUpper(double upper_bound, bool include_upper) const;

    // scale down version of bucket adjusting lower boundary
    BucketPtr makeBucketScaleLower(double point_lower_new, bool include_lower) const;

    // extract singleton bucket at given point
    BucketPtr makeBucketSingleton(double point_singleton) const;

    //		Create a new bucket by intersecting with another
    //		and return the percentage of each of the buckets that intersect.
    // 		Points will be shared
    //
    //		We can think of this method as looking at the cartesian product of
    //		two histograms, with "this" being a bucket from histogram 1 and
    //		and "bucket" being from histogram 2.
    //
    //		The goal is to build a histogram that reflects the diagonal of
    //		the cartesian product, where the two values are equal, which is
    //		the result of the equi-join.
    //
    //		To do this, we take the overlapping rectangles from the original
    //		buckets and form new "squares" such that their corners lie on
    //		the diagonal. This method will take two overlapping buckets and
    //		return one such result bucket.
    //
    //		Example (shown below): this = [10, 14], bucket = [8,16]
    //
    //		The result will be [10,14] in this example, since "this"
    //		is fully contained in "bucket".
    //
    //		                                       diagonal
    //		                                          V
    //		               +----------------------------------+
    //		 histogram 1   |       |              |  /        |
    //		               |                       /          |
    //		               |       |             /|           |
    //		      +-->  14 *- - - - - -+-------* - - - - - - -|
    //		      |        |       |   |     / |  |           |
    //		   "this"      |           |   /   |              |
    //		      |        |       |   | /     |  |           |
    //		      +-->  10 *- - - -+---*-------+ - - - - - - -|
    //		               |       | / |          |           |
    //		             8 |       *---+                      |
    //		               |     / |              |           |
    //		               |   /                               |
    //		               | /     |              |           |
    //		               +-------+---*-------*--+-----------+
    //		                       8  10      14  16
    //		                       +-- "bucket" --+
    //
    //		                                    histogram 2
    //
    //		The reason why we show this as a two-dimensional picture here instead
    //		of just two overlapping intervals is because of how we compute the frequency
    //		of this resulting square:
    //
    //		This is done by applying the general cardinality formula for
    //		equi-joins: | R join S on R.a = S.b | = |R| * |S| / max(NDV(R.a), NDV(S.b))
    //
    //		The join of the two tables is the union of the join of each of the
    //		squares we produce, so we apply the formula to each generated square
    //		(bucket of the join histogram).
    //		Note that there are no equi-join results outside of these squares that
    //		overlay the diagonal.
    //------------------------------------------------------------------------
    OverlappedRange makeBucketIntersect(BucketPtr & bucket) const;

    // return copy of bucket
    BucketPtr makeBucketCopy();

    //		Merges with another bucket. Returns merged bucket that should be part
    //		of the output. It also returns what is leftover from the merge.
    //		E.g.
    //		merge of [1,100) and [50,150) produces [1, 100), NULL, [100, 150)
    //		merge of [1,100) and [50,75) produces [1, 75), [75,100), NULL
    //		merge of [1,1) and [1,1) produces [1,1), NULL, NULL
    //
    //---------------------------------------------------------------------------
    BucketPtr makeBucketMerged(
        BucketPtr & bucket_other, BucketPtr & result_bucket1_new, BucketPtr & result_bucket2_new, bool is_union_all = true) const;

    // compare lower bucket boundaries
    static int compareLowerBounds(const BucketPtr & bucket1, const BucketPtr & bucket2);

    // compare upper bucket boundaries
    static int compareUpperBounds(const BucketPtr & bucket1, const BucketPtr & bucket2);

    // compare lower bound of first bucket to upper bound of second bucket
    static int compareLowerBoundToUpperBound(const BucketPtr & bucket1, const BucketPtr & bucket2);

    BucketPtr applySelectivity(double rowcount_selectivity, double ndv_selectivity);
};

}
