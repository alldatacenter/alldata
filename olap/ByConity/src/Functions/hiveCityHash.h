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
#include <cstdlib>
#include <cstdint>

/// A C++ implementation for the cityHash64 used by the Hive.

namespace DB::HiveCityHash
{

typedef int32_t Int32;
typedef int64_t Int64;
typedef uint64_t UInt64;

// Hash function for a byte array.
UInt64 cityHash64(const char* s, int pos, int len);

}
