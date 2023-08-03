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

#include <time.h>
#include <Core/DecimalFunctions.h>

namespace DB {

namespace ErrorCodes
{
    extern const int CANNOT_CLOCK_GETTIME;
}

static DateTime64 nowSubsecondDt64(UInt32 scale)
{
    static constexpr Int32 fractional_scale = 9;

    timespec spec{};
    if (clock_gettime(CLOCK_REALTIME, &spec))
        throwFromErrno("Cannot clock_gettime.", ErrorCodes::CANNOT_CLOCK_GETTIME);

    DecimalUtils::DecimalComponents<DateTime64> components{spec.tv_sec, spec.tv_nsec};

    // clock_gettime produces subsecond part in nanoseconds, but decimalFromComponents fractional is scale-dependent.
    // Andjust fractional to scale, e.g. for 123456789 nanoseconds:
    //   if scale is  6 (miscoseconds) => divide by 9 - 6 = 3 to get 123456 microseconds
    //   if scale is 12 (picoseconds)  => multiply by abs(9 - 12) = 3 to get 123456789000 picoseconds
    const auto adjust_scale = fractional_scale - static_cast<Int32>(scale);
    if (adjust_scale < 0)
        components.fractional *= intExp10(std::abs(adjust_scale));
    else if (adjust_scale > 0)
        components.fractional /= intExp10(adjust_scale);
    return DecimalUtils::decimalFromComponents<DateTime64>(components, scale);
}

}
