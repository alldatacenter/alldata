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
#include <Common/Exception.h>

namespace DB::Statistics
{

inline double scaleCount(double full_count, double sample_count, double target)
{
    return target * full_count / sample_count;
}

// paramenters
// - full_count: count of the entire table.
// - sample_count: count of sampled data, including null values. sample_count/full_count equals sample_factor
// - sample_ndv: ndv of sampled data excluding nulls.
// - sample_nonnull: count of sampled data excluding nulls. In block sampling, it may be a special value.
inline double scaleNdv(double full_count, double sample_count, double sample_ndv, double sample_nonnull)
{
    // NDVsample = NDV * (1 - (1 - COUNTsample/ COUNT) ^ (COUNT/NDV) )
    // NOTE: total_count, sample_count should be nonnull

    if (sample_count == 0)
        return 0;

    auto ratio = sample_count / full_count;
    auto estimate_nonnull = sample_nonnull / ratio;

    if (sample_count > full_count)
    {
        throw Exception("ill-formed ndv", ErrorCodes::LOGICAL_ERROR);
    }

    // Oracle Formula: NDVsample = NDV * (1 - (1 - ratio) ^ (COUNT/NDV))
    auto calc_sample_ndv = [=](double ndv) { return ndv * (1 - std::pow(1 - ratio, estimate_nonnull / ndv)); };

    if (sample_ndv >= estimate_nonnull)
    {
        return estimate_nonnull;
    }

    auto estimate_begin = std::max(1.0, sample_ndv);
    auto estimate_end = estimate_nonnull;

    // Binary Search ndv
    while ((estimate_end - estimate_begin) / estimate_end > 0.0001)
    {
        auto mid = (estimate_begin + estimate_end) / 2;
        auto calc_result = calc_sample_ndv(mid);
        if (calc_result <= sample_ndv)
        {
            estimate_begin = mid;
        }
        else
        {
            estimate_end = mid;
        }
    }

    return (estimate_end + estimate_begin) / 2;
}
}
