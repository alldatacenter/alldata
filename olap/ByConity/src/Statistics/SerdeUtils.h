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
#include <string_view>
#include <tuple>
#include <DataTypes/IDataType.h>
#include <Statistics/SerdeDataType.h>
#include <fmt/format.h>

namespace DB::Statistics
{
template <typename HeaderType = SerdeDataType>
inline std::tuple<HeaderType, std::string_view> parseBlobWithHeader(std::string_view raw_blob)
{
    static_assert(std::is_trivial_v<HeaderType>);
    if (raw_blob.size() <= sizeof(HeaderType))
    {
        throw Exception("corrupted blob", ErrorCodes::LOGICAL_ERROR);
    }

    HeaderType header;
    memcpy(&header, raw_blob.data(), sizeof(header));
    auto blob = raw_blob.substr(sizeof(header), raw_blob.size() - sizeof(header));
    return {header, blob};
}

template <typename T>
constexpr bool IsWideInteger = wide::IsWideInteger<T>::value;
template <typename T>
void checkSerdeDataType(SerdeDataType serde_data_type)
{
    if (std::is_same_v<T, String> && serde_data_type == SerdeDataType::StringOldVersion)
        return;

    if (serde_data_type != SerdeDataTypeFrom<T>)
    {
        throw Exception("mismatched type", ErrorCodes::LOGICAL_ERROR);
    }
}


}
