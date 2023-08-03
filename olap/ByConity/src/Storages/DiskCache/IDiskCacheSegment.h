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
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>

#include <memory>
#include <vector>

namespace DB
{
class IDiskCache;

class IDiskCacheSegment
{
public:
    explicit IDiskCacheSegment(size_t start_segment_number, size_t size) : segment_number(start_segment_number), segment_size(size) { }
    virtual ~IDiskCacheSegment() = default;

    virtual String getSegmentName() const = 0;
    virtual void cacheToDisk(IDiskCache & diskcache) = 0;

    static String formatSegmentName(
        const String & uuid, const String & part_name, const String & column_name, UInt32 segment_number, const String & extension);

protected:
    size_t segment_number;
    size_t segment_size;
};

using IDiskCacheSegmentPtr = std::shared_ptr<IDiskCacheSegment>;
using IDiskCacheSegmentsVector = std::vector<IDiskCacheSegmentPtr>;

}
