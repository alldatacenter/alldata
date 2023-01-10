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

#include <cstddef>

namespace DB::TSO
{

constexpr static size_t TSO_BITS = 64;
constexpr static size_t LOGICAL_BITS = 18;
constexpr static size_t MAX_LOGICAL = 1 << LOGICAL_BITS;
constexpr static size_t LOGICAL_BITMASK = 0x3FFFF;
constexpr static size_t TSO_UPDATE_INTERVAL = 50;  /// 50 milliseconds

#define ts_to_physical(ts) ((ts) >> LOGICAL_BITS)
#define ts_to_logical(ts) ((ts) & LOGICAL_BITMASK)
#define physical_logical_to_ts(physical, logical) (((physical) << LOGICAL_BITS) | ((logical) & LOGICAL_BITMASK))

}
