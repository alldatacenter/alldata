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

#include "IDiskCacheSegment.h"

#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>

namespace DB
{
String IDiskCacheSegment::formatSegmentName(
    const String & uuid, const String & part_name, const String & column_name, UInt32 segment_number, const String & extension)
{
    WriteBufferFromOwnString wb;

    writeString(uuid, wb);
    writeChar('/', wb);
    writeString(part_name, wb);
    writeChar('/', wb);
    writeString(column_name, wb);
    writeChar('#', wb);
    writeIntText(segment_number, wb);
    writeString(extension, wb);

    return wb.str();
}
}
