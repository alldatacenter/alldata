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

#include <string>
#include <gtest/gtest.h>
#include <DaemonManager/BGJobStatusInCatalog.h>

using namespace DB::BGJobStatusInCatalog;
using namespace DB;

namespace GtestBGJobStatusInCatalog
{

TEST(BGJobStatusInCatalogTest, test_serialize_and_deserialize)
{
    std::vector<CnchBGThreadStatus> all_statuses {
        CnchBGThreadStatus::Running,
        CnchBGThreadStatus::Stopped,
        CnchBGThreadStatus::Removed,
    };

    for (CnchBGThreadStatus status : all_statuses)
    {
        char store_data = serializeToChar(status);
        CnchBGThreadStatus get_status_from_char = deserializeFromChar(store_data);
        EXPECT_EQ(get_status_from_char, status);
        std::string store_data_str{store_data};
        CnchBGThreadStatus get_status_from_string = deserializeFromString(store_data_str);
        EXPECT_EQ(get_status_from_string, status);
    }

    EXPECT_THROW(deserializeFromChar('3'), Exception);
    EXPECT_THROW(deserializeFromString("3"), Exception);
    EXPECT_THROW(deserializeFromString("10"), Exception);
    EXPECT_THROW(deserializeFromString(""), Exception);
}

class DummyProxy : public BGJobStatusInCatalog::CatalogBGJobStatusPersistentStoreProxy
{
public:
    using BGJobStatusInCatalog::CatalogBGJobStatusPersistentStoreProxy::CatalogBGJobStatusPersistentStoreProxy;
    CacheClearer fetchStatusesIntoCache() override
    {
        CacheClearer res = CatalogBGJobStatusPersistentStoreProxy::fetchStatusesIntoCache();
        statuses_cache = {
            {UUID{UInt128{0, 1}}, CnchBGThreadStatus::Running},
            {UUID{UInt128{0, 2}}, CnchBGThreadStatus::Stopped},
            {UUID{UInt128{0, 3}}, CnchBGThreadStatus::Removed}
        };
        return res;
    }

    std::unordered_map<UUID, CnchBGThreadStatus> getCache() const
    {
        return statuses_cache;
    }
};

TEST(BGJobStatusInCatalogTest, test_CatalogBGJobStatusPersistentStoreProxy)
{
    DummyProxy proxy{nullptr, CnchBGThreadType::MergeMutate};
    UUID uuid{UInt128{0, 1}};
    EXPECT_THROW(proxy.getStatus(uuid, false), Exception);
    EXPECT_THROW(proxy.getStatus(uuid, true), Exception);

    {
        IBGJobStatusPersistentStoreProxy::CacheClearer cache_clearer
            = proxy.fetchStatusesIntoCache();
        CnchBGThreadStatus status = proxy.getStatus(UUID{UInt128{0, 1}}, true);
        EXPECT_EQ(status, CnchBGThreadStatus::Running);
        EXPECT_THROW(proxy.fetchStatusesIntoCache(), Exception);
        std::unordered_map<UUID, CnchBGThreadStatus> cache_before_clear = proxy.getCache();
        EXPECT_FALSE(cache_before_clear.empty());

        IBGJobStatusPersistentStoreProxy::CacheClearer test_move_clearer;
        test_move_clearer = std::move(cache_clearer);
        cache_before_clear = proxy.getCache();
        EXPECT_FALSE(cache_before_clear.empty());
    }
    std::unordered_map<UUID, CnchBGThreadStatus> cache_after_clear = proxy.getCache();
    EXPECT_TRUE(cache_after_clear.empty());
}


} // end namespace

