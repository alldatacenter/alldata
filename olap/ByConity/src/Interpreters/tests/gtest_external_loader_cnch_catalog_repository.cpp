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

#include <gtest/gtest.h>
#include <Interpreters/ExternalLoaderCnchCatalogRepository.h>

namespace UnitTestExternalLoader
{

using namespace DB;

TEST(TestExternalLoaderCnchCatalogRepository, TestParseStorageID)
{
    {
        StorageID storage_id = ExternalLoaderCnchCatalogRepository::parseStorageID("a.b");
        EXPECT_EQ(storage_id.getDatabaseName(), "a");
        EXPECT_EQ(storage_id.getTableName(), "b");
    }

    {
        StorageID storage_id = ExternalLoaderCnchCatalogRepository::parseStorageID("`\\`a`.`a.b`");
        EXPECT_EQ(storage_id.getDatabaseName(), "`a");
        EXPECT_EQ(storage_id.getTableName(), "a.b");
    }

    {
        StorageID storage_id = ExternalLoaderCnchCatalogRepository::parseStorageID("```a`.`\\`b`");
        EXPECT_EQ(storage_id.getDatabaseName(), "`a");
        EXPECT_EQ(storage_id.getTableName(), "`b");
    }

    EXPECT_THROW(ExternalLoaderCnchCatalogRepository::parseStorageID(""), Exception);
    EXPECT_THROW(ExternalLoaderCnchCatalogRepository::parseStorageID("``a.b"), Exception);
    EXPECT_THROW(ExternalLoaderCnchCatalogRepository::parseStorageID(".b"), Exception);
    EXPECT_THROW(ExternalLoaderCnchCatalogRepository::parseStorageID("a.``b"), Exception);
    EXPECT_THROW(ExternalLoaderCnchCatalogRepository::parseStorageID("a."), Exception);
    EXPECT_THROW(ExternalLoaderCnchCatalogRepository::parseStorageID("```a`."), Exception);
}

}
