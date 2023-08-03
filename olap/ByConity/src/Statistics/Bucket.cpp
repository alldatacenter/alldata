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

#include <Optimizer/Utils.h>
#include <Statistics/Bucket.h>

namespace DB::Statistics
{
Bucket::Bucket(
    double lower_bound_, double upper_bound_, double ndv_, double count_, bool lower_bound_inclusive_, bool upper_bound_inclusive_)
    : lower_bound(lower_bound_)
    , upper_bound(upper_bound_)
    , ndv(ndv_)
    , count(count_)
    , lower_bound_inclusive(lower_bound_inclusive_)
    , upper_bound_inclusive(upper_bound_inclusive_)
{
}

bool Bucket::isSingleton() const
{
    return lower_bound == upper_bound;
};

bool Bucket::contains(const double point) const
{
    // special case for singleton bucket
    if (isSingleton())
    {
        return lower_bound == point;
    }

    // special case if point equal to lower bound
    if (lower_bound_inclusive && lower_bound == point)
    {
        return true;
    }

    // special case if point equal to upper bound
    if (upper_bound_inclusive && upper_bound == point)
    {
        return true;
    }

    if (lower_bound_inclusive && !upper_bound_inclusive)
    {
        return lower_bound <= point && upper_bound > point;
    }

    if (!lower_bound_inclusive && upper_bound_inclusive)
    {
        return lower_bound < point && upper_bound >= point;
    }

    if (!lower_bound_inclusive && !upper_bound_inclusive)
    {
        return lower_bound < point && upper_bound > point;
    }

    return lower_bound <= point && upper_bound >= point;
}

bool Bucket::isBefore(const double point) const
{
    return (lower_bound_inclusive && lower_bound > point) || (!lower_bound_inclusive && lower_bound >= point);
}

bool Bucket::isAfter(const double point) const
{
    return (upper_bound_inclusive && upper_bound < point) || (!upper_bound_inclusive && upper_bound <= point);
}

double Bucket::getOverlapPercentage(const double point) const
{
    // special case of upper bound equal to point
    if (upper_bound <= point)
    {
        return 1.0;
    }

    // if point is not contained, then no overlap
    if (!contains(point))
    {
        return 0.0;
    }

    // special case for singleton bucket
    if (isSingleton())
    {
        if (lower_bound == point)
        {
            return 1.0;
        }
    }

    // general case, compute distance ratio
    double distance_upper = distance(upper_bound, lower_bound);
    double distance_middle = distance(point, lower_bound);

    double res = distance_upper == 0 ? 1 : 1 / distance_upper;
    if (distance_middle > 0.0)
    {
        res = res * distance_middle;
    }

    return std::min(res, 1.0);
}

double Bucket::distance(double point1, double point2) const
{
    return point1 - point2;
}

double Bucket::width() const
{
    if (isSingleton())
    {
        return 1.0;
    }
    else
    {
        return distance(upper_bound, lower_bound);
    }
}

Bucket Bucket::trim(double lower_bound_, double upper_bound_)
{
    double lower_value;
    double upper_value;

    //     lower_bound     lower_bound_  upper_bound_   upper_bound
    // --------+------------------+------------+-------------+------->
    if (lower_bound <= lower_bound_ && upper_bound >= upper_bound_)
    {
        lower_value = lower_bound_;
        upper_value = upper_bound_;
    }
    //     lower_bound       lower_bound_   upper_bound   upper_bound_
    // --------+------------------+------------+-------------+------->
    else if (lower_bound <= lower_bound_ && upper_bound >= lower_bound_)
    {
        lower_value = lower_bound_;
        upper_value = upper_bound;
    }
    //    lower_bound_        lower_bound    upper_bound_    upper_bound
    // --------+------------------+------------+-------------+------->
    else if (lower_bound <= upper_bound_ && upper_bound >= upper_bound_)
    {
        lower_value = lower_bound;
        upper_value = upper_bound_;
    }
    //    lower_bound_        lower_bound    upper_bound   upper_bound_
    // --------+------------------+------------+-------------+------->
    else
    {
        Utils::checkState(lower_bound >= lower_bound_ && upper_bound <= upper_bound_);
        lower_value = lower_bound;
        upper_value = upper_bound;
    }

    if (lower_value == upper_value)
    {
        return Bucket(lower_value, upper_value, ndv == 0 ? 0 : 1, ndv == 0 ? 0 : count / ndv, true, true);
    }
    else
    {
        Utils::checkState(lower_bound != upper_bound);
        double ratio = (upper_value - lower_value) / (upper_bound - lower_bound);
        return Bucket(lower_value, upper_value, ndv * ratio, count * ratio, true, true);
    }
}

bool Bucket::intersects(const Bucket & bucket1, const Bucket & bucket2)
{
    if (bucket1.isSingleton() && bucket2.isSingleton())
    {
        return bucket1.getLowerBound() == bucket2.getLowerBound();
    }

    if (bucket1.isSingleton())
    {
        return bucket2.contains(bucket1.getLowerBound());
    }

    if (bucket2.isSingleton())
    {
        return bucket1.contains(bucket2.getLowerBound());
    }

    if (subsumes(bucket1, bucket2) || subsumes(bucket2, bucket1))
    {
        return true;
    }

    if (0 >= compareLowerBounds(bucket1, bucket2))
    {
        // current bucket starts before the other bucket
        if (0 >= compareLowerBoundToUpperBound(bucket2, bucket1))
        {
            // other bucket starts before current bucket ends
            return true;
        }

        return false;
    }

    if (0 >= compareLowerBoundToUpperBound(bucket1, bucket2))
    {
        // current bucket starts before the other bucket ends
        return true;
    }

    return false;
}

bool Bucket::subsumes(const Bucket & bucket1, const Bucket & bucket2)
{
    // both are singletons
    if (bucket1.isSingleton() && bucket2.isSingleton())
    {
        return bucket1.getLowerBound() == bucket2.getLowerBound();
    }

    // other one is a singleton
    if (bucket2.isSingleton())
    {
        return bucket1.contains(bucket2.getLowerBound());
    }

    int lower_bounds_comparison = compareLowerBounds(bucket1, bucket2);
    int upper_bounds_comparison = compareUpperBounds(bucket1, bucket2);

    return (0 >= lower_bounds_comparison && 0 <= upper_bounds_comparison);
}

bool Bucket::isBefore(const Bucket & bucket1, const Bucket & bucket2)
{
    if (intersects(bucket1, bucket2))
    {
        return false;
    }
    return bucket1.getUpperBound() <= bucket2.getLowerBound();
}

bool Bucket::isAfter(const Bucket & bucket1, const Bucket & bucket2)
{
    if (intersects(bucket1, bucket2))
    {
        return false;
    }
    return bucket1.getLowerBound() >= bucket2.getUpperBound();
}

BucketOpt Bucket::makeBucketGreaterThan(double point) const
{
    Utils::checkState(this->contains(point));

    if (isSingleton() || this->getUpperBound() == point)
    {
        return {};
    }

    BucketOpt result_bucket;
    double point_new = point + 1;

    if (this->contains(point_new))
    {
        result_bucket = makeBucketScaleLower(point_new, true /* include_lower */);
    }

    return result_bucket;
}

BucketOpt Bucket::makeBucketScaleUpper(double point_upper_new, bool include_upper) const
{
    Utils::checkState(this->contains(point_upper_new));

    // scaling upper to be same as lower is identical to producing a singleton bucket
    if (this->getLowerBound() == point_upper_new)
    {
        // invalid bucket, e.g. if bucket is [5,10) and
        // point_upper_new is 5 open, null should be returned
        if (!include_upper)
        {
            return {};
        }
        return makeBucketSingleton(point_upper_new);
    }

    UInt64 count_new = this->getCount();
    UInt64 distinct_new = this->getNumDistinct();

    if (this->getUpperBound() != point_upper_new)
    {
        double overlap = this->getOverlapPercentage(point_upper_new);
        count_new = count_new * overlap;
        distinct_new = distinct_new * overlap;
    }

    auto bucket = Bucket(this->lower_bound, point_upper_new, distinct_new, count_new, this->isLowerClosed(), include_upper);
    return bucket;
}

BucketOpt Bucket::makeBucketScaleLower(double point_lower_new, bool include_lower) const
{
    Utils::checkState(this->contains(point_lower_new));

    // scaling lower to be same as upper is identical to producing a singleton bucket
    if (this->upper_bound == point_lower_new)
    {
        if (!include_lower)
        {
            return {};
        }
        return makeBucketSingleton(point_lower_new);
    }

    UInt64 count_new = this->getCount();
    UInt64 distinct_new = this->getNumDistinct();

    if (this->lower_bound != point_lower_new)
    {
        double overlap = 1.0 - this->getOverlapPercentage(point_lower_new);
        count_new = this->getCount() * overlap;
        distinct_new = this->getNumDistinct() * overlap;
    }

    auto bucket = Bucket(point_lower_new, this->upper_bound, distinct_new, count_new, include_lower, this->upper_bound_inclusive);
    return bucket;
}

Bucket Bucket::makeBucketSingleton(double point_singleton) const
{
    Utils::checkState(this->contains(point_singleton));

    // assume that this point is one of the ndistinct values
    // in the bucket
    double distinct_ratio = 1.0 / this->ndv;
    double count_new = this->getCount() * distinct_ratio;

    // singleton point is both lower and upper
    auto bucket = Bucket(point_singleton, point_singleton, 1, count_new, true, true);
    return bucket;
}

OverlappedRange Bucket::makeBucketIntersect(const Bucket & right) const
{
    double left_count = getCount();
    double left_ndv = getNumDistinct();

    double right_count = right.getCount();
    double right_ndv = right.getNumDistinct();

    // Case1: the left bucket is "smaller" than the right bucket
    //  left.lower_bound    right.lower_bound      left.upper_bound   right.upper_bound
    // --------+------------------+---------------------+-----------------------+------->
    if (right.getLowerBound() >= getLowerBound() && right.getUpperBound() >= getUpperBound())
    {
        if (getUpperBound() == right.getLowerBound())
        {
            // The overlapped range has only one value.

            double overlap_left_rows = left_ndv == 0 ? 0 : left_count / left_ndv;
            double overlap_right_rows = right_ndv == 0 ? 0 : right_count / right_ndv;

            OverlappedRange range{
                .lower_bound = right.getLowerBound(),
                .upper_bound = right.getLowerBound(),
                .left_ndv = 1,
                .right_ndv = 1,
                .left_rows = overlap_left_rows,
                .right_rows = overlap_right_rows};
            return range;
        }
        else
        {
            double overlap = getUpperBound() - right.getLowerBound();
            if (right.isLowerClosed())
            {
            }

            double left_ratio = overlap / range();
            double right_ratio = overlap / (right.range());

            double overlap_left_ndv = left_ndv * left_ratio;
            double overlap_right_ndv = right_ndv * right_ratio;
            double overlap_left_rows = left_count * left_ratio;
            double overlap_right_rows = right_count * right_ratio;

            OverlappedRange range{
                .lower_bound = right.getLowerBound(),
                .upper_bound = getUpperBound(),
                .left_ndv = overlap_left_ndv,
                .right_ndv = overlap_right_ndv,
                .left_rows = overlap_left_rows,
                .right_rows = overlap_right_rows};
            return range;
        }
    }

    // Case2: the left bucket is "larger" than the right bucket
    //      right.lower_bound   left.lower_bound     right.upper_bound       left.upper_bound
    // --------+------------------+-------------------------+-----------------------+------->
    else if (right.getLowerBound() <= getLowerBound() && right.getUpperBound() <= getUpperBound())
    {
        if (right.getUpperBound() == getLowerBound())
        {
            // The overlapped range has only one value.

            double overlap_left_rows = left_ndv == 0 ? 0 : left_count / left_ndv;
            double overlap_right_rows = right_ndv == 0 ? 0 : right_count / right_ndv;

            OverlappedRange range{
                .lower_bound = right.getUpperBound(),
                .upper_bound = right.getUpperBound(),
                .left_ndv = 1,
                .right_ndv = 1,
                .left_rows = overlap_left_rows,
                .right_rows = overlap_right_rows};
            return range;
        }
        else
        {
            double left_ratio = (right.getUpperBound() - getLowerBound()) / range();
            double right_ratio = (right.getUpperBound() - getLowerBound()) / (right.range());


            double overlap_left_ndv = left_ndv * left_ratio;
            double overlap_right_ndv = right_ndv * right_ratio;
            double overlap_left_rows = left_count * left_ratio;
            double overlap_right_rows = right_count * right_ratio;

            OverlappedRange range{
                .lower_bound = getLowerBound(),
                .upper_bound = right.getUpperBound(),
                .left_ndv = overlap_left_ndv,
                .right_ndv = overlap_right_ndv,
                .left_rows = overlap_left_rows,
                .right_rows = overlap_right_rows};
            return range;
        }
    }

    // Case3: the left bucket contains the right bucket
    //    left.lower_bound   right.lower_bound     right.upper_bound      left.upper_bound
    // --------+------------------+---------------------+-------------------------+---------------->
    else if (right.getLowerBound() >= getLowerBound() && right.getUpperBound() <= getUpperBound())
    {
        double left_ratio = (right.range()) / range();

        double overlap_left_ndv = left_ndv * left_ratio;
        double overlap_left_rows = left_count * left_ratio;

        OverlappedRange range{
            .lower_bound = right.getLowerBound(),
            .upper_bound = right.getUpperBound(),
            .left_ndv = overlap_left_ndv,
            .right_ndv = right_ndv,
            .left_rows = overlap_left_rows,
            .right_rows = right_count};
        return range;
    }

    // Case4: the right bucket contains the left bucket
    //   right.lower_bound   left.lower_bound   left.upper_bound     right.upper_bound
    // --------+------------------+----------------------+----------------+------->
    else
    {
        Utils::checkState(right.getLowerBound() <= getLowerBound() && right.getUpperBound() >= getUpperBound());

        double right_ratio = range() / (right.range());

        double overlap_right_ndv = right_ndv * right_ratio;
        double overlap_right_rows = right_count * right_ratio;

        OverlappedRange range{
            .lower_bound = getLowerBound(),
            .upper_bound = getUpperBound(),
            .left_ndv = getNumDistinct(),
            .right_ndv = overlap_right_ndv,
            .left_rows = left_count,
            .right_rows = overlap_right_rows};
        return range;
    }
}

Bucket Bucket::makeBucketCopy() const
{
    auto bucket
        = Bucket(this->lower_bound, this->upper_bound, this->ndv, this->count, this->lower_bound_inclusive, this->upper_bound_inclusive);
    return bucket;
}

//		Merges with another bucket. Returns merged bucket that should be part
//		of the output. It also returns what is leftover from the merge.
//		E.g.
//		merge of [1,100) and [50,150) produces [1, 100), NULL, [100, 150)
//		merge of [1,100) and [50,75) produces [1, 75), [75,100), NULL
//		merge of [1,1) and [1,1) produces [1,1), NULL, NULL
//
//---------------------------------------------------------------------------
std::tuple<Bucket, BucketOpt, BucketOpt> Bucket::makeBucketMerged(const Bucket & bucket_other, bool is_union_all) const
{
    BucketOpt result_bucket_new1;
    BucketOpt result_bucket_new2;

    double result_lower_new = std::min(this->lower_bound, bucket_other.lower_bound);
    double result_upper_new = std::min(this->upper_bound, bucket_other.upper_bound);

    double overlap = this->getOverlapPercentage(result_upper_new);
    double distinct_new = this->getNumDistinct() * overlap;
    double count_new = this->getCount() * overlap;

    if (is_union_all)
    {
        double overlap_other = bucket_other.getOverlapPercentage(result_upper_new);
        if (!this->isSingleton())
            distinct_new = distinct_new + (bucket_other.getNumDistinct() * overlap_other);
        count_new = count_new + (bucket_other.getCount() * overlap_other);
    }

    bool is_upper_closed = result_lower_new == result_upper_new;

    if (result_upper_new < upper_bound)
    {
        result_bucket_new1 = this->makeBucketScaleLower(result_upper_new, !is_upper_closed);
    }

    if (result_upper_new < bucket_other.upper_bound)
    {
        result_bucket_new2 = bucket_other.makeBucketScaleLower(result_upper_new, !is_upper_closed);
    }

    auto bucket = Bucket(result_lower_new, result_upper_new, distinct_new, count_new, true, is_upper_closed);
    return std::make_tuple(bucket, result_bucket_new1, result_bucket_new2);
}

int Bucket::compareLowerBounds(const Bucket & bucket1, const Bucket & bucket2)
{
    double point1 = bucket1.getLowerBound();
    double point2 = bucket2.getLowerBound();

    bool is_closed_point1 = bucket1.isLowerClosed();
    bool is_closed_point2 = bucket2.isLowerClosed();

    if (point1 == point2)
    {
        if (is_closed_point1 == is_closed_point2)
        {
            return 0;
        }

        if (is_closed_point1)
        {
            // bucket1 contains the lower bound (lb), while bucket2 contain all
            // values between (lb + delta) and upper bound (ub)
            return -1;
        }

        return 1;
    }

    if (point1 <= point2)
    {
        return -1;
    }

    return 1;
}

// compare upper bucket boundaries
int Bucket::compareUpperBounds(const Bucket & bucket1, const Bucket & bucket2)
{
    double point1 = bucket1.getUpperBound();
    double point2 = bucket2.getUpperBound();

    bool is_closed_point1 = bucket1.isUpperClosed();
    bool is_closed_point2 = bucket2.isUpperClosed();

    if (point1 == point2)
    {
        if (is_closed_point1 == is_closed_point2)
        {
            return 0;
        }

        if (is_closed_point1)
        {
            // bucket2 contains all values less than upper bound not including upper bound point
            // therefore bucket1 upper bound greater than bucket2 upper bound
            return 1;
        }

        return -1;
    }

    if (point1 <= point2)
    {
        return -1;
    }

    return 1;
}

// compare lower bound of first bucket to upper bound of second bucket
int Bucket::compareLowerBoundToUpperBound(const Bucket & bucket1, const Bucket & bucket2)
{
    double lower_bound_first = bucket1.getLowerBound();
    double upper_bound_second = bucket2.getUpperBound();

    if (lower_bound_first >= upper_bound_second)
    {
        return 1;
    }

    if (lower_bound_first <= upper_bound_second)
    {
        return -1;
    }

    // equal
    if (bucket1.isLowerClosed() && bucket2.isUpperClosed())
    {
        return 0;
    }

    return 1; // points not comparable
}

Bucket Bucket::applySelectivity(double rowcount_selectivity, double ndv_selectivity) const
{
    double new_count = count * rowcount_selectivity;
    double new_ndv = std::min(ndv * ndv_selectivity, new_count);
    auto bucket = Bucket(lower_bound, upper_bound, new_ndv, new_count, isLowerClosed(), isUpperClosed());
    return bucket;
}

}
