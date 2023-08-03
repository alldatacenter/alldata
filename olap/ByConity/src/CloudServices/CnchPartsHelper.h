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

#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>

namespace DB
{
class Context;
}

namespace DB::CnchPartsHelper
{

enum LoggingOption
{
    DisableLogging = 0,
    EnableLogging = 1,
};

LoggingOption getLoggingOption(const Context & c);

IMergeTreeDataPartsVector toIMergeTreeDataPartsVector(const MergeTreeDataPartsCNCHVector & vec);
MergeTreeDataPartsCNCHVector toMergeTreeDataPartsCNCHVector(const IMergeTreeDataPartsVector & vec);

MergeTreeDataPartsVector calcVisibleParts(MergeTreeDataPartsVector & all_parts, bool flatten, LoggingOption logging = DisableLogging);
ServerDataPartsVector calcVisibleParts(ServerDataPartsVector & all_parts, bool flatten, LoggingOption logging = DisableLogging);
MergeTreeDataPartsCNCHVector calcVisibleParts(MergeTreeDataPartsCNCHVector & all_parts, bool flatten, LoggingOption logging = DisableLogging);

ServerDataPartsVector calcVisiblePartsForGC(
    ServerDataPartsVector & all_parts,
    ServerDataPartsVector * visible_alone_drop_ranges,
    ServerDataPartsVector * invisible_dropped_parts,
    LoggingOption logging = DisableLogging);

void calcVisibleDeleteBitmaps(
    DeleteBitmapMetaPtrVector & all_bitmaps,
    DeleteBitmapMetaPtrVector & visible_bitmaps,
    bool include_tombstone = false,
    DeleteBitmapMetaPtrVector * visible_alone_tombstones = nullptr,
    DeleteBitmapMetaPtrVector * bitmaps_covered_by_range_tombstones = nullptr);

template <typename T>
void flattenPartsVector(std::vector<T> & visible_parts)
{
    size_t size = visible_parts.size();
    for (size_t i = 0; i < size; ++i)
    {
        auto prev_part = visible_parts[i]->tryGetPreviousPart();
        while (prev_part)
        {
            if constexpr (std::is_same_v<T, decltype(prev_part)>)
                visible_parts.push_back(prev_part);
            else
                visible_parts.push_back(std::dynamic_pointer_cast<typename T::element_type>(prev_part));

            prev_part = prev_part->tryGetPreviousPart();
        }
    }
}
}
