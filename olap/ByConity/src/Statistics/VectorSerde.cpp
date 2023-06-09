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

#include <DataTypes/IDataType.h>
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/SerdeUtils.h>
#include <Statistics/TypeMacros.h>
#include <Common/Exception.h>

namespace DB::Statistics
{
template <typename T>
std::string vectorSerialize(const std::vector<T> & data)
{
    // TODO: optimize it to use better encoding
    static_assert(std::is_integral_v<T> || std::is_floating_point_v<T> || IsWideInteger<T>);
    static_assert(std::is_trivial_v<T>);
    // bool not supported due to stupid vector<bool>
    static_assert(!std::is_same_v<bool, T>);
    const char * ptr = reinterpret_cast<const char *>(data.data());
    auto bytes = sizeof(T) * data.size();
    return std::string(ptr, bytes);
}

template <typename T>
std::vector<T> vectorDeserialize(std::string_view blob)
{
    // TODO: optimize it to use better encoding
    // TODO: support string
    if (blob.size() % sizeof(T) != 0)
    {
        throw Exception("Corrupted Blob", ErrorCodes::LOGICAL_ERROR);
    }
    auto size = blob.size() / sizeof(T);
    std::vector<T> vec(size);
    memcpy(vec.data(), blob.data(), blob.size());
    return vec;
}

#define INITIALIZE(TYPE) template std::string vectorSerialize<TYPE>(const std::vector<TYPE> & data);
FIXED_TYPE_ITERATE(INITIALIZE)
#undef INITIALIZE

#define INITIALIZE(TYPE) template std::vector<TYPE> vectorDeserialize<TYPE>(std::string_view blob);
FIXED_TYPE_ITERATE(INITIALIZE)
#undef INITIALIZE
}
