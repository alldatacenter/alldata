/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
#define private public
#include "atomic.h"
#include "client_config.h"
#include "logger.h"
#include "tc_api.h"
#include "utils.h"
#include <gtest/gtest.h>
#include <iostream>
#include <stdint.h>
#include <string>
#include <thread>
using namespace std;
using namespace dataproxy_sdk;

TEST(clientBase, test1)
{
    ClientConfig client;
    EXPECT_EQ(client.parseConfig("config.json"), true);
    // EXPECT_EQ(client.bufNum(), 1204);

    EXPECT_EQ(client.parseConfig("nochconfig.json"), false);
}

TEST(client, test2)
{
    ClientConfig client;
    EXPECT_EQ(client.parseConfig("emptyconfig.json"), true);
}

TEST(client, init)
{
    ClientConfig client;
    client.proxy_URL_ = "proxy_url";
    client.auth_key_ = "key";
    EXPECT_EQ(client.proxy_URL_, "proxy_url");
    EXPECT_EQ(client.need_auth_, false);
    EXPECT_EQ(client.auth_id_, "");
    EXPECT_EQ(client.auth_key_, "key");
    EXPECT_EQ(client.enable_pack_, constants::kEnablePack);
}

TEST(sdk, init)
{
    ClientConfig client;
    int32_t init_first = tc_api_init("./release/conf/config_example.json");
    int32_t init_second = tc_api_init(client);
    EXPECT_EQ(init_first, 0);
    EXPECT_EQ(init_second, SDKInvalidResult::kMultiInit);
    EXPECT_EQ(tc_api_close(1000), 0);

}

int main(int argc, char* argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}