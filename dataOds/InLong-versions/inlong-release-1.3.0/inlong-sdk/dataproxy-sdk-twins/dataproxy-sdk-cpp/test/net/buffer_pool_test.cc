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

#include "atomic.h"
#include "buffer_pool.h"
#include "proxylist_config.h"
#include "executor_thread_pool.h"
#include "logger.h"
#include "send_buffer.h"
#include "socket_connection.h"
#include "tc_api.h"
#include "utils.h"
#include <algorithm>
#include <chrono>
#include <functional>
#include <gtest/gtest.h>
#include <iostream>
#include <map>
#include <stdint.h>
#include <string>
#include <thread>
using namespace std;
using namespace dataproxy_sdk;

TEST(bufpool, basetest)
{
    EXPECT_EQ(g_config.parseConfig("config.json"), true);
    cout << g_config.bufNum() << endl;

    g_pools = new TotalPools();
    EXPECT_NE(g_pools->getPool("groupid_1"), nullptr);
    EXPECT_NE(g_pools->getPool("groupid_2"), nullptr);

    SendBuffer* buf = nullptr;
    EXPECT_EQ(g_pools->getPool("groupid_1")->writeId(), 0);
    EXPECT_EQ(g_pools->getPool("groupid_2")->getSendBuf(buf), 0);
    EXPECT_NE(buf, nullptr);
    EXPECT_EQ(g_pools->getPool("groupid_1")->writeId(), 1);
}

int main(int argc, char* argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}