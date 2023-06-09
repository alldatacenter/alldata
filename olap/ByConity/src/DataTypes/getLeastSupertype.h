/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <DataTypes/IDataType.h>


namespace DB
{

template <typename T,
typename = std::enable_if_t<std::is_same_v<T, DataTypeDateTime64> || std::is_same_v<T, DataTypeTime>, T>>
size_t getMaxScaleIndex(const DataTypes &types)
{
    UInt8 max_scale = 0;
    size_t max_scale_date_time_index = 0;

    for (size_t i = 0; i < types.size(); ++i)
    {
        const auto & type = types[i];

        if (const auto * datetime64_or_time_type = typeid_cast<const T *>(type.get()))
        {
            const auto scale = datetime64_or_time_type->getScale();
            if (scale >= max_scale)
            {
                max_scale_date_time_index = i;
                max_scale = scale;
            }
        }
    }
    return max_scale_date_time_index;
}

/** Get data type that covers all possible values of passed data types.
  * If there is no such data type, throws an exception.
  *
  * Examples: least common supertype for UInt8, Int8 - Int16.
  * Examples: there is no least common supertype for Array(UInt8), Int8.
  */
DataTypePtr getLeastSupertype(const DataTypes & types, bool allow_extended_conversion = false);

}
