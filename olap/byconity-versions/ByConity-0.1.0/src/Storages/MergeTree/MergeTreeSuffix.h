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

namespace DB
{
constexpr auto DATA_FILE_EXTENSION = ".bin";
constexpr auto INDEX_FILE_EXTENSION = ".idx";
constexpr auto MARKS_FILE_EXTENSION = ".mrk";

constexpr auto BLOOM_FILTER_FILE_EXTENSION = ".bmf";
constexpr auto RANGE_BLOOM_FILTER_FILE_EXTENSION = ".rbmf";

constexpr auto COMPRESSION_COLUMN_EXTENSION = "_encoded";
constexpr auto COMPRESSION_DATA_FILE_EXTENSION = "_encoded.bin";
constexpr auto COMPRESSION_MARKS_FILE_EXTENSION = "_encoded.mrk";

constexpr auto AB_IDX_EXTENSION = ".adx";
constexpr auto AB_IRK_EXTENSION = ".ark";

constexpr auto MARK_BITMAP_IDX_EXTENSION = ".m_adx";
constexpr auto MARK_BITMAP_IRK_EXTENSION = ".m_ark";

constexpr auto DELETE_BITMAP_FILE_EXTENSION = ".del";
constexpr auto DELETE_BITMAP_FILE_NAME = "delete.del";

constexpr auto KLL_FILE_EXTENSION = ".kll";

constexpr auto UKI_FILE_NAME = "unique_key.idx";   /// name of unique key index file

constexpr auto UNIQUE_ROW_STORE_DATA_NAME = "row_store.data";
constexpr auto UNIQUE_ROW_STORE_META_NAME = "row_store.meta";

bool isEngineReservedWord(const String & column);

}
