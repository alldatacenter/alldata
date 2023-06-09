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

#include <Storages/MergeTree/MergeSelector.h>


namespace DB
{

/** Select parts to merge based on its level.
  * Select first range of parts of parts_to_merge length with minimum level.
  */
class LevelMergeSelector : public IMergeSelector
{
public:
    struct Settings
    {
        size_t parts_to_merge = 10;
    };

    explicit LevelMergeSelector(const Settings & settings_) : settings(settings_) {}

    PartsRange select(
        const PartsRanges & parts_ranges,
        const size_t max_total_size_to_merge,
        [[maybe_unused]] MergeScheduler * merge_scheduler = nullptr) override;

private:
    const Settings settings;
};

}
