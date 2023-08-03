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

#include <Columns/ListIndex.h>

#include <Common/StringUtils/StringUtils.h>
//#include <roaring.c>
#include <Compression/CompressedReadBufferFromFile.h>
#include <Common/escapeForFileName.h>

namespace DB
{
BitmapIndexWriter::BitmapIndexWriter(
    String path, String name, size_t rows, BitmapIndexMode bitmap_index_mode_, const bool & enable_run_optimization_)
{
    bitmap_index_mode = bitmap_index_mode_;
    String ark_suffix = ark_suffix_vec[bitmap_index_mode];
    String adx_suffix = adx_suffix_vec[bitmap_index_mode];

    if (!endsWith(path, "/"))
        path.append("/");

    total_rows = rows;
    enable_run_optimization = enable_run_optimization_;

    String column_name = escapeForFileName(name);

    idx = std::make_unique<WriteBufferFromFile>(path + column_name + adx_suffix, DBMS_DEFAULT_BUFFER_SIZE, O_WRONLY | O_APPEND | O_CREAT);
    hash_idx = std::make_unique<HashingWriteBuffer>(*idx);
    // use default CompressionSettings
    compressed_idx = std::make_unique<CompressedWriteBuffer>(*hash_idx);
    hash_compressed = std::make_unique<HashingWriteBuffer>(*compressed_idx);

    irk = std::make_unique<WriteBufferFromFile>(path + column_name + ark_suffix, DBMS_DEFAULT_BUFFER_SIZE, O_WRONLY | O_APPEND | O_CREAT);
    hash_irk = std::make_unique<HashingWriteBuffer>(*irk);
}

BitmapIndexWriter::BitmapIndexWriter(String path, size_t rows, BitmapIndexMode bitmap_index_mode_, const bool & enable_run_optimization_)
{
    bitmap_index_mode = bitmap_index_mode_;
    String ark_suffix = ark_suffix_vec[bitmap_index_mode];
    String adx_suffix = adx_suffix_vec[bitmap_index_mode];

    total_rows = rows;
    enable_run_optimization = enable_run_optimization_;

    idx = std::make_unique<WriteBufferFromFile>(path + adx_suffix, DBMS_DEFAULT_BUFFER_SIZE, O_WRONLY | O_APPEND | O_CREAT);
    hash_idx = std::make_unique<HashingWriteBuffer>(*idx);
    // use default CompressionSettings
    compressed_idx = std::make_unique<CompressedWriteBuffer>(*hash_idx);
    hash_compressed = std::make_unique<HashingWriteBuffer>(*compressed_idx);

    irk = std::make_unique<WriteBufferFromFile>(path + ark_suffix, DBMS_DEFAULT_BUFFER_SIZE, O_WRONLY | O_APPEND | O_CREAT);
    hash_irk = std::make_unique<HashingWriteBuffer>(*irk);
}


void BitmapIndexWriter::addToChecksums(MergeTreeData::DataPart::Checksums & checksums, const String & column_name)
{
    String ark_suffix = ark_suffix_vec[bitmap_index_mode];
    String adx_suffix = adx_suffix_vec[bitmap_index_mode];

    checksums.files[column_name + adx_suffix].is_compressed = true;
    checksums.files[column_name + adx_suffix].uncompressed_size = hash_compressed->count();
    checksums.files[column_name + adx_suffix].uncompressed_hash = hash_compressed->getHash();
    checksums.files[column_name + adx_suffix].file_size = hash_idx->count();
    checksums.files[column_name + adx_suffix].file_hash = hash_idx->getHash();

    checksums.files[column_name + ark_suffix].file_size = hash_irk->count();
    checksums.files[column_name + ark_suffix].file_hash = hash_irk->getHash();
}

BitmapIndexReader::BitmapIndexReader(String path_, String name, BitmapIndexMode bitmap_index_mode_)
    : path(path_), column_name(name), bitmap_index_mode(bitmap_index_mode_)
{
    if (!endsWith(path, "/"))
        path.append("/");
    column_name = escapeForFileName(name);
    init();
}

void BitmapIndexReader::init()
{
    String ark_suffix = ark_suffix_vec[bitmap_index_mode];
    String adx_suffix = adx_suffix_vec[bitmap_index_mode];
    try
    {
        compressed_idx = std::make_unique<CompressedReadBufferFromFile>(path + column_name + adx_suffix, 0, 0, 0, nullptr, DBMS_DEFAULT_BUFFER_SIZE);
        irk = std::make_unique<ReadBufferFromFile>(path + column_name + ark_suffix, DBMS_DEFAULT_BUFFER_SIZE);
    }
    catch (...)
    {
        tryLogCurrentException(&Poco::Logger::get("BitmapIndexReader"), __PRETTY_FUNCTION__);
        compressed_idx = nullptr;
        irk = nullptr;
    }
}

}
