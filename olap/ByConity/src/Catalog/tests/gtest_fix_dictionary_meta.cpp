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

#include <Catalog/Catalog.h>
#include <Catalog/CatalogFactory.h>
#include <Parsers/ASTCreateQuery.h>
#include <DaemonManager/FixCatalogMetaDataTask.h>
#include <gtest/gtest.h>

namespace
{

using namespace DB;
using DB::DaemonManager::getUUIDFromCreateQuery;
TEST(CatalogMeta, fillUUIDForDictionaryTest)
{
    const String expected_uuid_str{"1c3f0f76-e616-4195-9c7a-fdf8ce23c8b9"};
    const UUID expected_uuid = UUIDHelpers::toUUID(expected_uuid_str);
    EXPECT_EQ(toString(UUIDHelpers::Nil) , "00000000-0000-0000-0000-000000000000");

    {
        /// case 1: no uuid in uuid field in proto dict model
        String create_query = "CREATE DICTIONARY test.dict_complex_hash UUID '1c3f0f76-e616-4195-9c7a-fdf8ce23c8b9' ( `app_id` UInt32, `key_name` String, `origin_value` String, `target_value` String) PRIMARY KEY app_id, key_name, origin_value SOURCE(CLICKHOUSE(USER 'default' TABLE 'table_for_hash_dict2' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(COMPLEX_KEY_HASHED())";
        DB::Protos::DataModelDictionary dic_model;
        dic_model.set_database("some_database_name");
        dic_model.set_definition(create_query);
        EXPECT_EQ(getUUIDFromCreateQuery(dic_model), expected_uuid);
        EXPECT_EQ(RPCHelpers::createUUID(dic_model.uuid()), UUIDHelpers::Nil);

        DB::Catalog::fillUUIDForDictionary(dic_model);

        EXPECT_EQ(RPCHelpers::createUUID(dic_model.uuid()), expected_uuid);
        EXPECT_EQ(getUUIDFromCreateQuery(dic_model), expected_uuid);
        EXPECT_EQ(dic_model.database(), "some_database_name");
    }

    {
        /// case 2: no uuid in create query field in proto dict model
        String create_query = "CREATE DICTIONARY test.dict_complex_hash ( `app_id` UInt32, `key_name` String, `origin_value` String, `target_value` String) PRIMARY KEY app_id, key_name, origin_value SOURCE(CLICKHOUSE(USER 'default' TABLE 'table_for_hash_dict2' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(COMPLEX_KEY_HASHED())";
        DB::Protos::DataModelDictionary dic_model;
        RPCHelpers::fillUUID(expected_uuid, *(dic_model.mutable_uuid()));
        dic_model.set_definition(create_query);
        EXPECT_EQ(RPCHelpers::createUUID(dic_model.uuid()), expected_uuid);
        EXPECT_EQ(getUUIDFromCreateQuery(dic_model), UUIDHelpers::Nil);

        DB::Catalog::fillUUIDForDictionary(dic_model);

        EXPECT_EQ(RPCHelpers::createUUID(dic_model.uuid()), expected_uuid);
        EXPECT_EQ(getUUIDFromCreateQuery(dic_model), expected_uuid);
    }

    {
        /// case 3: no uuid in both create query field and uuid field in proto dict model
        String create_query = "CREATE DICTIONARY test.dict_complex_hash ( `app_id` UInt32, `key_name` String, `origin_value` String, `target_value` String) PRIMARY KEY app_id, key_name, origin_value SOURCE(CLICKHOUSE(USER 'default' TABLE 'table_for_hash_dict2' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(COMPLEX_KEY_HASHED())";
        DB::Protos::DataModelDictionary dic_model;
        dic_model.set_definition(create_query);
        EXPECT_EQ(RPCHelpers::createUUID(dic_model.uuid()), UUIDHelpers::Nil);
        EXPECT_EQ(getUUIDFromCreateQuery(dic_model), UUIDHelpers::Nil);

        DB::Catalog::fillUUIDForDictionary(dic_model);

        EXPECT_EQ(getUUIDFromCreateQuery(dic_model), RPCHelpers::createUUID(dic_model.uuid()));
    }
}

}
