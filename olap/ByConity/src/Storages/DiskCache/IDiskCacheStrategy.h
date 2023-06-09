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

#include <Storages/DiskCache/DiskCacheSettings.h>
#include <Storages/DiskCache/IDiskCacheSegment.h>
#include <Storages/MergeTree/MarkRange.h>

#include <set>
#include <type_traits>

namespace DB
{

class IDiskCacheStrategy : public std::enable_shared_from_this<IDiskCacheStrategy>
{
public:
    explicit IDiskCacheStrategy(const DiskCacheStrategySettings & settings_): segment_size(settings_.segment_size) {}
    virtual ~IDiskCacheStrategy() = default;

    IDiskCacheStrategy(const IDiskCacheStrategy &) = delete;
    IDiskCacheStrategy & operator=(const IDiskCacheStrategy &) = delete;

    /// get segments need to be cached
    virtual IDiskCacheSegmentsVector getCacheSegments(const IDiskCacheSegmentsVector & segments) = 0;

    size_t getSegmentSize() const { return segment_size; }

    template<typename T, typename... Args, typename = std::enable_if<std::is_base_of_v<IDiskCacheSegment, T>>>
    IDiskCacheSegmentsVector transferRangesToSegments(const MarkRanges & ranges, Args &&... args) const
    {
        auto segment_nums = transferRangesToSegmentNumbers(ranges);
        IDiskCacheSegmentsVector segments;
        segments.reserve(segment_nums.size());

        for (auto segment_num : segment_nums)
        {
            auto segment = std::make_shared<T>(segment_num, segment_size, std::forward<Args>(args)...);
            segments.push_back(std::move(segment));
        }

        return segments;
    }

protected:
    std::set<size_t> transferRangesToSegmentNumbers(const MarkRanges & ranges) const;

    /// It indicates how many index granules a segment contains
    size_t segment_size;
};

using IDiskCacheStrategyPtr = std::shared_ptr<IDiskCacheStrategy>;

}

