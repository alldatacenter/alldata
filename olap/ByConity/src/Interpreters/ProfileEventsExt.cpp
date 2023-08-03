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

#include "ProfileEventsExt.h"
#include <Common/typeid_cast.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnMap.h>
#include <Columns/ColumnByteMap.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeArray.h>

namespace ProfileEvents
{

/// Put implementation here to avoid extra linking dependencies for clickhouse_common_io
void dumpToMapColumn(const Counters & counters, DB::IColumn * column, bool nonzero_only)
{
#ifdef USE_COMMUNITY_MAP
    auto * column_map = column ? &typeid_cast<DB::ColumnMap &>(*column) : nullptr;
    if (!column_map)
        return;

    auto & offsets = column_map->getNestedColumn().getOffsets();
    auto & tuple_column = column_map->getNestedData();
    auto & key_column = tuple_column.getColumn(0);
    auto & value_column = tuple_column.getColumn(1);

    size_t size = 0;
    for (Event event = 0; event < Counters::num_counters; ++event)
    {
        UInt64 value = counters[event].load(std::memory_order_relaxed);

        if (nonzero_only && 0 == value)
            continue;

        const char * desc = ProfileEvents::getName(event);
        key_column.insertData(desc, strlen(desc));
        value_column.insert(value);
        size++;
    }

    offsets.push_back(offsets.back() + size);
#else
    auto * column_map = column ? &typeid_cast<DB::ColumnByteMap &>(*column) : nullptr;
    if (!column_map)
        return;

    auto & offsets = column_map->getOffsets();
    auto & key_column = column_map->getKey();
    auto & value_column = column_map->getValue();

    size_t size = 0;
    for (Event event = 0; event < Counters::num_counters; ++event)
    {
        UInt64 value = counters[event].load(std::memory_order_relaxed);

        if (nonzero_only && 0 == value)
            continue;

        const char * desc = ProfileEvents::getName(event);
        key_column.insertData(desc, strlen(desc));
        value_column.insert(value);
        size++;
    }

    offsets.push_back((offsets.size() == 0 ? 0 : offsets.back()) + size);
#endif
}

}
