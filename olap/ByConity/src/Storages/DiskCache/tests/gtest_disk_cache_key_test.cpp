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

#include <Core/UUID.h>
#include <Storages/DiskCache/DiskCacheLRU.h>
#include <Storages/DiskCache/IDiskCacheSegment.h>
#include <gtest/gtest.h>

namespace DB
{
TEST(DiskCache, UnhexKeyTest)
{
    String table_uuid = UUIDHelpers::UUIDToString(UUIDHelpers::generateV4());
    String seg_key = IDiskCacheSegment::formatSegmentName(table_uuid, "part_1", "col", 0, ".bin");

    DiskCacheLRU::KeyType key = DiskCacheLRU::hash(seg_key);
    String hex_key = DiskCacheLRU::hexKey(key);
    auto unhex = DiskCacheLRU::unhexKey(hex_key);
    EXPECT_TRUE(unhex.has_value());
    EXPECT_TRUE(unhex == key);
}

TEST(DiskCache, DiskCachePathTest)
{
    String table_uuid = UUIDHelpers::UUIDToString(UUIDHelpers::generateV4());
    String seg_key1 = IDiskCacheSegment::formatSegmentName(table_uuid, "part_1", "col", 0, ".bin");
    String seg_key2 = IDiskCacheSegment::formatSegmentName(table_uuid, "part_1", "col", 0, ".mrk");

    auto path1 = DiskCacheLRU::getRelativePath(DiskCacheLRU::hash(seg_key1));
    auto path2 = DiskCacheLRU::getRelativePath(DiskCacheLRU::hash(seg_key2));

    EXPECT_EQ(path1.parent_path(), path2.parent_path());
    EXPECT_NE(path1.filename(), path2.filename());
}

}
