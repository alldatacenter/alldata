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

#include <common/types.h>

namespace DB
{

/// Types of data part format.
class MergeTreeDataPartType
{
public:
    enum Value
    {
        /// Data of each column is stored in one or several (for complex types) files.
        /// Every data file is followed by marks file.
        WIDE,

        /// Data of all columns is stored in one file. Marks are also stored in single file.
        COMPACT,

        /// Format with buffering data in RAM.
        IN_MEMORY,

        /// CNCH cloud storage.
        CNCH,

        UNKNOWN,
    };

    MergeTreeDataPartType() : value(UNKNOWN) {}
    MergeTreeDataPartType(Value value_) : value(value_) {}

    bool operator==(const MergeTreeDataPartType & other) const
    {
        return value == other.value;
    }

    bool operator!=(const MergeTreeDataPartType & other) const
    {
        return !(*this == other);
    }

    bool operator<(const MergeTreeDataPartType & other) const
    {
        return value < other.value;
    }

    bool operator>(const MergeTreeDataPartType & other) const
    {
        return value > other.value;
    }

    void fromString(const String & str);
    String toString() const;

    Value getValue() const { return value; }

private:
    Value value;
};

}
