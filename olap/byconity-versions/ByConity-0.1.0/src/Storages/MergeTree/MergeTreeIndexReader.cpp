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

#include <Storages/MergeTree/MergeTreeIndexReader.h>


namespace DB
{

MergeTreeIndexReader::MergeTreeIndexReader(
    MergeTreeIndexPtr index_,
    MergeTreeData::DataPartPtr part_,
    size_t marks_count_,
    const MarkRanges & all_mark_ranges_,
    MergeTreeReaderSettings settings)
    : index(index_)
    , stream(
          part_->volume->getDisk(),
          part_->getFullRelativePath() + index->getFileName(),
          index->getFileName(),
          ".idx",
          marks_count_,
          all_mark_ranges_,
          std::move(settings),
          nullptr,
          nullptr,
          &part_->index_granularity_info,
          ReadBufferFromFileBase::ProfileCallback{},
          CLOCK_MONOTONIC_COARSE,
          part_->getFileOffsetOrZero(index->getFileName() + ".idx"),
          part_->getFileSizeOrZero(index->getFileName() + ".idx"),
          part_->getFileOffsetOrZero(index->getFileName() + part_->getMarksFileExtension()),
          part_->getFileSizeOrZero(index->getFileName() + part_->getMarksFileExtension()))
{
    stream.seekToStart();
}

void MergeTreeIndexReader::seek(size_t mark)
{
    stream.seekToMark(mark);
}

MergeTreeIndexGranulePtr MergeTreeIndexReader::read()
{
    auto granule = index->createIndexGranule();
    granule->deserializeBinary(*stream.data_buffer);
    return granule;
}

}
