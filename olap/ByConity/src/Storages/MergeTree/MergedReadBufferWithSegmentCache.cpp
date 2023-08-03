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

#include <memory>
#include <optional>
#include <Storages/MergeTree/MergedReadBufferWithSegmentCache.h>
#include <Storages/DiskCache/DiskCacheSegment.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <IO/createReadBufferFromFileBase.h>
#include "Compression/CachedCompressedReadBuffer.h"
#include "Compression/CompressedReadBufferFromFile.h"

namespace ProfileEvents
{
    extern const Event CnchReadSizeFromDiskCache;
    extern const Event CnchReadSizeFromRemote;
}

namespace DB
{

bool MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::initialized() const
{
    return cached_buffer != nullptr || non_cached_buffer != nullptr;
}

void MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::initialize(
    std::unique_ptr<CachedCompressedReadBuffer> cb,
    std::unique_ptr<CompressedReadBufferFromFile> ncb)
{
    if (!((cb != nullptr) ^ (ncb != nullptr)))
    {
        throw Exception("Can't specific cached buffer and non cached buffer at same time",
            ErrorCodes::LOGICAL_ERROR);
    }
    cached_buffer = std::move(cb);
    non_cached_buffer = std::move(ncb);
}

void MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::reset()
{
    cached_buffer = nullptr;
    non_cached_buffer = nullptr;
}

String MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::path() const
{
    assertInitialized();

    return cached_buffer != nullptr ? cached_buffer->getPath() : non_cached_buffer->getPath();
}

void MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::setProfileCallback(const ReadBufferFromFileBase::ProfileCallback &callback,
    clockid_t clock_typ)
{
    assertInitialized();

    cached_buffer != nullptr ? cached_buffer->setProfileCallback(callback, clock_typ):
        non_cached_buffer->setProfileCallback(callback, clock_typ);
}

void MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::seek(size_t offset_in_compressed_file,
    size_t offset_in_decompressed_block)
{
    assertInitialized();

    cached_buffer != nullptr ? cached_buffer->seek(offset_in_compressed_file, offset_in_decompressed_block):
        non_cached_buffer->seek(offset_in_compressed_file, offset_in_decompressed_block);
}

size_t MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::compressedOffset()
{
    assertInitialized();

    return cached_buffer != nullptr ? cached_buffer->compressedOffset()
        : non_cached_buffer->compressedOffset();
}

void MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::disableChecksumming()
{
    assertInitialized();

    cached_buffer != nullptr ? cached_buffer->disableChecksumming()
        : non_cached_buffer->disableChecksumming();
}

ReadBuffer& MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::activeBuffer()
{
    assertInitialized();

    return cached_buffer != nullptr ? static_cast<ReadBuffer&>(*cached_buffer)
        : static_cast<ReadBuffer&>(*non_cached_buffer);
}

void MergedReadBufferWithSegmentCache::DualCompressedReadBuffer::assertInitialized() const
{
    if (unlikely(!initialized()))
    {
        throw Exception("DualCompressedReadBuffer not initialized yet",
            ErrorCodes::LOGICAL_ERROR);
    }
}

MergedReadBufferWithSegmentCache::MergedReadBufferWithSegmentCache(
    const StorageID& storage_id_, const String& part_name_, const String& stream_name_,
    const DiskPtr& source_disk_, const String& source_file_path_,
    size_t source_data_offset_, size_t source_data_size_, size_t cache_segment_size_,
    IDiskCache* segment_cache_, size_t estimated_range_bytes_,
    size_t buffer_size_, const MergeTreeReaderSettings& settings_,
    size_t total_segment_count_, MergeTreeMarksLoader& marks_loader_,
    UncompressedCache* uncompressed_cache_,
    const ReadBufferFromFileBase::ProfileCallback& profile_callback_,
    clockid_t clock_type_):
        ReadBuffer(nullptr, 0),
        storage_id(storage_id_), part_name(part_name_), stream_name(stream_name_),
        source_disk(source_disk_), source_file_path(source_file_path_),
        source_data_offset(source_data_offset_), source_data_size(source_data_size_),
        cache_segment_size(cache_segment_size_), segment_cache(segment_cache_),
        estimated_range_bytes(estimated_range_bytes_), buffer_size(buffer_size_),
        settings(settings_), uncompressed_cache(uncompressed_cache_),
        profile_callback(profile_callback_), clock_type(clock_type_),
        total_segment_count(total_segment_count_), marks_loader(marks_loader_),
        current_segment_idx(0), current_compressed_offset(std::nullopt),
        logger(&Poco::Logger::get("MergedReadBufferWithSegmentCache"))
{
    seekToStart();
}

size_t MergedReadBufferWithSegmentCache::readBig(char *to, size_t n)
{
    // TODO(wsy): Use buffer's readBig here
    // How to switch from source buffer to cache buffer seemless when call readBig
    // of source buffer?
    return read(to, n);
}

bool MergedReadBufferWithSegmentCache::nextImpl()
{
    if (segment_cache != nullptr && cache_buffer.initialized())
    {
        // There is a active cache buffer, trying to use it
        ReadBuffer& active_buffer = cache_buffer.activeBuffer();

        if (likely(!active_buffer.eof()))
        {
            // Cache buffer not eof yet, use it
            Position buf_pos = active_buffer.position();
            size_t buf_size = active_buffer.buffer().end() - buf_pos;
            BufferBase::set(buf_pos, buf_size, 0);
            // Adjust underlying buffer's cursor to working buffer's end
            active_buffer.position() += buf_size;

            ProfileEvents::increment(ProfileEvents::CnchReadSizeFromDiskCache,
                buf_size);

            return true;
        }

        current_compressed_offset = marks_loader.getMark(current_segment_idx * cache_segment_size).offset_in_compressed_file
            + cache_buffer.compressedOffset();

        cache_buffer.reset();

        LOG_TRACE(logger, fmt::format("Cache buffer of segment {} encounter "
            "eof, compressed offset {}", current_segment_idx, current_compressed_offset.value()));

        // Current cache buffer encounter eof, there maybe following conditions
        // 1. This is last segment of stream, we encounter true eof
        // 2. This is just eof of one segment, we still have data to read
    }

    // Trying to adjust current segment index
    // No need to do this if segment cache is not enabled
    if (unlikely(segment_cache != nullptr && current_compressed_offset.has_value()))
    {
        if (current_segment_idx + 1 >= total_segment_count)
        {
            return false;
        }
        size_t new_segment_idx = current_segment_idx;
        while (new_segment_idx + 1 < total_segment_count
            && (current_compressed_offset.value() >= marks_loader.getMark((new_segment_idx + 1) * cache_segment_size).offset_in_compressed_file))
        {
            ++new_segment_idx;
        }

        // Seek to corresponding position, init cache/source buffer if necessary
        // will reset current_compressed_offset
        seekToPosition(new_segment_idx, {current_compressed_offset.value(), 0});
    }

    ReadBuffer& active_buffer = cache_buffer.initialized() ?
        cache_buffer.activeBuffer() : source_buffer.activeBuffer();

    bool encounter_eof = active_buffer.eof();
    if (!encounter_eof)
    {
        Position buf_pos = active_buffer.position();
        size_t buf_size = active_buffer.buffer().end() - buf_pos;
        BufferBase::set(buf_pos, buf_size, 0);
        active_buffer.position() += buf_size;

        ProfileEvents::increment(
            cache_buffer.initialized() ?
                ProfileEvents::CnchReadSizeFromDiskCache
                : ProfileEvents::CnchReadSizeFromRemote,
            buf_size);

        if (segment_cache != nullptr && !cache_buffer.initialized())
        {
            // We are reading from source, should check if we need to seek to next
            // segment
            size_t source_compressed_offset = fromSourceDataOffset(
                source_buffer.compressedOffset());
            if (unlikely(current_segment_idx + 1 < total_segment_count
                && source_compressed_offset >= marks_loader.getMark(cache_segment_size * (current_segment_idx + 1)).offset_in_compressed_file))
            {
                LOG_TRACE(logger, fmt::format("Offset {}, need seek to next segment {}",
                    source_compressed_offset, current_segment_idx));
                current_compressed_offset = source_compressed_offset;
            }
        }
    }

    return !encounter_eof;
}

void MergedReadBufferWithSegmentCache::seekToStart()
{
    seekToPosition(0, {0, 0});
}

void MergedReadBufferWithSegmentCache::seekToMark(size_t mark)
{
    seekToPosition(mark / cache_segment_size, marks_loader.getMark(mark));
}

void MergedReadBufferWithSegmentCache::seekToPosition(size_t segment_idx,
    const MarkInCompressedFile& mark_pos)
{
    // Reset current working/internal buffer first
    reset();

    current_compressed_offset = std::nullopt;

    if (seekToMarkInSegmentCache(segment_idx, mark_pos))
    {
        return;
    }
    else
    {
        // Failed to seek to cache, reset cache buffer
        cache_buffer.reset();
    }

    // No segment cache, trying to use source reader
    initSourceBufferIfNeeded();

    LOG_TRACE(logger, fmt::format("Seek to {}, offset {}:{}, base offset {}, limit {}",
        segment_idx, mark_pos.offset_in_compressed_file, mark_pos.offset_in_decompressed_block,
        source_data_offset, source_data_size));

    // seek to mark
    source_buffer.seek(toSourceDataOffset(mark_pos.offset_in_compressed_file),
        mark_pos.offset_in_decompressed_block);
    current_segment_idx = segment_idx;
}

bool MergedReadBufferWithSegmentCache::seekToMarkInSegmentCache(size_t segment_idx,
    const MarkInCompressedFile& mark_pos)
{
    if (segment_cache == nullptr)
    {
        return false;
    }

    String segment_key = DiskCacheSegment::getSegmentKey(storage_id, part_name,
        stream_name, segment_idx, DATA_FILE_EXTENSION);
    std::pair<DiskPtr, String> cache_entry = segment_cache->get(segment_key);
    if (cache_entry.first == nullptr)
    {
        return false;
    }

    DiskPtr& cache_disk = cache_entry.first;
    const String& cache_path = cache_entry.second;

    try
    {
        size_t segment_start_compressed_offset =
            marks_loader.getMark(segment_idx * cache_segment_size).offset_in_compressed_file;

        LOG_TRACE(logger, fmt::format("Seek to diskcache {} (current buffer at {}), segment {}, offset {}:{}", cache_path, cache_buffer.initialized() ? cache_buffer.path() : "Uninitialized", segment_idx, mark_pos.offset_in_compressed_file, mark_pos.offset_in_decompressed_block));
        // There isn't any segment reading right now, or it's not the segment we
        // are looking for, initialize one
        if (!cache_buffer.initialized() || cache_buffer.path() != fullPath(cache_disk, cache_path))
        {
            cache_buffer.reset();

            // Init cache buffer
            if (uncompressed_cache)
            {
                auto cached_compressed_buffer = std::make_unique<CachedCompressedReadBuffer>(
                    fullPath(cache_disk, cache_path),
                    [this, cache_disk, cache_path]() {
                        return cache_disk->readFile(
                            cache_path, {
                                .buffer_size = buffer_size,
                                .estimated_size = estimated_range_bytes,
                                .aio_threshold = settings.min_bytes_to_use_direct_io,
                                .mmap_threshold = settings.min_bytes_to_use_mmap_io,
                                .mmap_cache = settings.mmap_cache.get()
                            }
                        );
                    },
                    uncompressed_cache
                );

                cache_buffer.initialize(std::move(cached_compressed_buffer), nullptr);
            }
            else
            {
                auto non_cached_compressed_buffer = std::make_unique<CompressedReadBufferFromFile>(
                    cache_disk->readFile(
                        cache_path, {
                            .buffer_size = buffer_size,
                            .estimated_size = estimated_range_bytes,
                            .aio_threshold = settings.min_bytes_to_use_direct_io,
                            .mmap_threshold = settings.min_bytes_to_use_mmap_io,
                            .mmap_cache = settings.mmap_cache.get()
                        }
                    )
                );

                cache_buffer.initialize(nullptr, std::move(non_cached_compressed_buffer));
            }

            if (profile_callback)
            {
                cache_buffer.setProfileCallback(profile_callback, clock_type);
            }

            if (!settings.checksum_on_read)
            {
                cache_buffer.disableChecksumming();
            }
        }

        cache_buffer.seek(mark_pos.offset_in_compressed_file - segment_start_compressed_offset,
            mark_pos.offset_in_decompressed_block);
        current_segment_idx = segment_idx;
    }
    catch(...)
    {
        tryLogCurrentException("MergedReadBufferWithSegmentCache");
        cache_buffer.reset();
        return false;
    }

    return true;
}

void MergedReadBufferWithSegmentCache::initSourceBufferIfNeeded()
{
    if (source_buffer.initialized())
    {
        return;
    }

    if (uncompressed_cache)
    {
        auto cached_compressed_buffer = std::make_unique<CachedCompressedReadBuffer>(
            fullPath(source_disk, source_file_path),
            [this]() {
                return source_disk->readFile(source_file_path, {
                    .buffer_size = buffer_size,
                    .estimated_size = estimated_range_bytes,
                    .aio_threshold = settings.min_bytes_to_use_direct_io,
                    .mmap_threshold = settings.min_bytes_to_use_mmap_io,
                    .mmap_cache = settings.mmap_cache.get()
                });
            },
            uncompressed_cache, false, source_data_offset, source_data_size, true
        );

        source_buffer.initialize(std::move(cached_compressed_buffer), nullptr);
    }
    else
    {
        auto non_cached_compressed_buffer = std::make_unique<CompressedReadBufferFromFile>(
            source_disk->readFile(source_file_path, {
                .buffer_size = buffer_size,
                .estimated_size = estimated_range_bytes,
                .aio_threshold = settings.min_bytes_to_use_direct_io,
                .mmap_threshold = settings.min_bytes_to_use_mmap_io,
                .mmap_cache = settings.mmap_cache.get()
            }),
            false, source_data_offset, source_data_size, true
        );

        source_buffer.initialize(nullptr, std::move(non_cached_compressed_buffer));
    }

    if (profile_callback)
    {
        source_buffer.setProfileCallback(profile_callback, clock_type);
    }

    if (!settings.checksum_on_read)
    {
        source_buffer.disableChecksumming();
    }
}

size_t MergedReadBufferWithSegmentCache::toSourceDataOffset(size_t logical_offset) const
{
    return logical_offset + source_data_offset;
}

size_t MergedReadBufferWithSegmentCache::fromSourceDataOffset(size_t physical_offset) const
{
    if (unlikely(physical_offset < source_data_offset))
    {
        throw Exception(fmt::format("Try to convert invalid physical offset {}"
            ", source data offset {}, source data limit {}", physical_offset,
            source_data_offset, source_data_size), ErrorCodes::LOGICAL_ERROR);
    }
    return physical_offset - source_data_offset;
}

}
