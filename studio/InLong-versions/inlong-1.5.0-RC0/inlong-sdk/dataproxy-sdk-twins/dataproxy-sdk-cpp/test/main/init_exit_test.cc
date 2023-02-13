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

#include "../common.h"

void subroutine()
{
    ClientConfig client_config;
    tc_api_init(client_config);
    for (size_t i = 0; i < 1000; i++)
    {
        LOG_ERROR("log error %d", i);
        LOG_STAT("log stat %d", i);
        LOG_INFO("log info %d", i);
    }
    tc_api_close(100);
}

TEST(initAndclose, test)
{
    EXPECT_EQ(tc_api_init("config.json"), 0);
    EXPECT_EQ(tc_api_init("config.json"), SDKInvalidResult::kMultiInit);

    EXPECT_EQ(tc_api_close(100), 0);
    EXPECT_EQ(tc_api_close(100), SDKInvalidResult::kMultiExits);
}

TEST(multiThreadInit, test)
{
    thread threads[20];
    for (size_t i = 0; i < 20; i++)
    {
        threads[i] = thread(subroutine);
    }
    for (size_t i = 0; i < 20; i++)
    {
        threads[i].join();
    }
    
}

int main(int argc, char* argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}