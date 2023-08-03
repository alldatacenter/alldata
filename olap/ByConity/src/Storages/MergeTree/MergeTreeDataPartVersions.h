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
#include <Core/Types.h>
#include <IO/ReadBuffer.h>
#include <IO/WriteBuffer.h>
#include <Storages/MergeTree/MergeTreeSettings.h>
#include <map>

namespace DB
{
/**
 * In order to consider compatibility, data part needs to adopt different strategies for the new
 * and old data after making some implementation improvements. Therefore, this class can be used
 * to record different versions of the implementation, which is convenient for judging in the code.
 */
struct MergeTreeDataPartVersions
{
    bool enable_compact_map_data = false;

    MergeTreeDataPartVersions(bool enable_compact_map_data_): enable_compact_map_data(enable_compact_map_data_) {}

    MergeTreeDataPartVersions(const MergeTreeSettingsPtr & settings) : enable_compact_map_data(settings->enable_compact_map_data) { }

    void write(WriteBuffer & to);

    bool read(ReadBuffer & from, bool needCheckHeader = true);
};

}
