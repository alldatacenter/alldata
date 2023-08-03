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

#include <Storages/MergeTree/MergeTreeDataPartType.h>
#include <Common/Exception.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int UNKNOWN_PART_TYPE;
}

void MergeTreeDataPartType::fromString(const String & str)
{
    if (str == "Wide")
        value = WIDE;
    else if (str == "Compact")
        value = COMPACT;
    else if (str == "InMemory")
        value = IN_MEMORY;
    else if (str == "CNCH")
        value = CNCH;
    else
        throw DB::Exception("Unexpected string for part type: " + str, ErrorCodes::UNKNOWN_PART_TYPE);
}

String MergeTreeDataPartType::toString() const
{
    switch (value)
    {
        case WIDE:
            return "Wide";
        case COMPACT:
            return "Compact";
        case IN_MEMORY:
            return "InMemory";
        case CNCH:
            return "CNCH";
        default:
            return "Unknown";
    }
}

}
