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

#include "config_formats.h"
#if USE_PARQUET

#include <Processors/Formats/IInputFormat.h>
#include <Formats/FormatSettings.h>

namespace parquet::arrow { class FileReader; }

namespace arrow { class Buffer; }

namespace DB
{

class ArrowColumnToCHColumn;

class ParquetBlockInputFormat : public IInputFormat
{
public:
    ParquetBlockInputFormat(
        ReadBuffer & in_,
        Block header_,
        const FormatSettings & format_settings_,
        const std::map<String, String> & partition_kv_ = {},
        const std::unordered_set<Int64> & skip_row_groups_ = {},
        const size_t row_group_index_ = 0,
        bool read_one_group_ = false);

    void resetParser() override;

    String getName() const override { return "ParquetBlockInputFormat"; }

protected:
    Chunk generate() override;

private:
    void prepareReader();

private:
    const FormatSettings format_settings;
    std::unique_ptr<parquet::arrow::FileReader> file_reader;
    int row_group_total = 0;
    // indices of columns to read from Parquet file
    std::vector<int> column_indices;
    std::unique_ptr<ArrowColumnToCHColumn> arrow_column_to_ch_column;
    int row_group_current = 0;

    std::map<String, String> partition_kv;

    const std::unordered_set<Int64> skip_row_groups;

    bool read_one_group;
};

}

#endif
