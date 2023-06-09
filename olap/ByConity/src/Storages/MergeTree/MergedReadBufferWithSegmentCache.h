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

#include <ctime>
#include <memory>
#include <Compression/CachedCompressedReadBuffer.h>
#include <Compression/CompressedReadBufferFromFile.h>
#include <Interpreters/StorageID.h>
#include <Storages/MarkCache.h>
#include <Storages/DiskCache/IDiskCache.h>
#include <Storages/MergeTree/MergeTreeIOSettings.h>
#include <Storages/MergeTree/MergeTreeMarksLoader.h>

namespace DB
{

class MergedReadBufferWithSegmentCache: public ReadBuffer
{
public:
    MergedReadBufferWithSegmentCache(const StorageID& storage_id_,
        const String& part_name_, const String& stream_name_, const DiskPtr& source_disk_,
        const String& source_file_path_, size_t source_data_offset_,
        size_t source_data_size_, size_t cache_segment_size_,
        IDiskCache* segment_cache_, size_t estimated_range_bytes_,
        size_t buffer_size_, const MergeTreeReaderSettings& settings_,
        size_t total_segment_count_, MergeTreeMarksLoader& marks_loader_,
        UncompressedCache* uncompressed_cache_ = nullptr,
        const ReadBufferFromFileBase::ProfileCallback& profile_callback_ = {},
        clockid_t clock_type_ = CLOCK_MONOTONIC_COARSE);

    virtual size_t readBig(char* to, size_t n) override;
    virtual bool nextImpl() override;

    void seekToStart();
    void seekToMark(size_t mark);

private:
    class DualCompressedReadBuffer
    {
    public:
        DualCompressedReadBuffer(): cached_buffer(nullptr), non_cached_buffer(nullptr) {}

        inline bool initialized() const;
        inline void initialize(std::unique_ptr<CachedCompressedReadBuffer> cb,
            std::unique_ptr<CompressedReadBufferFromFile> ncb);
        inline void reset();
        inline String path() const;
        inline void setProfileCallback(const ReadBufferFromFileBase::ProfileCallback& callback,
            clockid_t clock_typ);

        inline void seek(size_t offset_in_compressed_file, size_t offset_in_decompressed_block);
        inline size_t compressedOffset();
        inline void disableChecksumming();

        inline ReadBuffer& activeBuffer();

    private:
        inline void assertInitialized() const;

        std::unique_ptr<CachedCompressedReadBuffer> cached_buffer;
        std::unique_ptr<CompressedReadBufferFromFile> non_cached_buffer;
    };

    void seekToPosition(size_t segment_idx, const MarkInCompressedFile& mark_pos);
    bool seekToMarkInSegmentCache(size_t segment_idx, const MarkInCompressedFile& mark_pos);
    void initSourceBufferIfNeeded();

    inline size_t toSourceDataOffset(size_t logical_offset) const;
    inline size_t fromSourceDataOffset(size_t physical_offset) const;

    // Reader stream info
    StorageID storage_id;
    String part_name;
    String stream_name;

    // Source file info
    DiskPtr source_disk;
    const String source_file_path;
    size_t source_data_offset;
    size_t source_data_size;

    // Segment cache info
    const size_t cache_segment_size;
    IDiskCache* segment_cache;

    // Readbuffer's settings
    size_t estimated_range_bytes;
    size_t buffer_size;
    MergeTreeReaderSettings settings;
    UncompressedCache* uncompressed_cache;
    ReadBufferFromFileBase::ProfileCallback profile_callback;
    clockid_t clock_type;

    size_t total_segment_count;

    MergeTreeMarksLoader& marks_loader;

    // current segment index is guarantee to be consistent with cache_buffer
    size_t current_segment_idx;
    // Current compressed offset of underlying data, if this object has_value,
    // then there must encounter end of a segment
    std::optional<size_t> current_compressed_offset;
    DualCompressedReadBuffer cache_buffer;
    DualCompressedReadBuffer source_buffer;

    Poco::Logger* logger;
};

}
