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
#include <Storages/Hive/HiveDataPart_fwd.h>

namespace DB
{

struct RowGroupsInDataPart
{
    HiveDataPartCNCHPtr data_part;
    size_t total_row_groups;

    RowGroupsInDataPart() = default;

    RowGroupsInDataPart(const HiveDataPartCNCHPtr & data_part_, size_t total_row_groups_ = 0)
        : data_part(data_part_)
        , total_row_groups(total_row_groups_)
    {}

    size_t getRowGroups() const
    {
        return total_row_groups;
    }

};

using RowGroupsInDataParts = std::vector<RowGroupsInDataPart>;
}
