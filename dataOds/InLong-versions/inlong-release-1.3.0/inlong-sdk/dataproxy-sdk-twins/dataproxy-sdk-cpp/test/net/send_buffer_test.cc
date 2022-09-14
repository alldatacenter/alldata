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
#include "executor_thread_pool.h"
#include "logger.h"
#include "send_buffer.h"
#include "tc_api.h"
#include "utils.h"
#include <asio.hpp>
#include <chrono>
#include <functional>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <iostream>
#include <stdint.h>
#include <string>
#include <thread>
using namespace std;
using namespace dataproxy_sdk;

int32_t callBackFunc(const char* inlong_group_id, const char* inlong_stream_id, const char* msg, int32_t msglen, const int64_t rep_time, const char* ip)
{
    cout << "inlong_group_id----" << inlong_group_id << "----inlong_stream_id" << inlong_stream_id << "-----" << msg << "-----"
         << "send error" << endl;
    return -1;
}

TEST(sendBuffer, callbacktest)
{
    SendBuffer* buf = new SendBuffer(102400);
    buf->setGroupid("inlong_group_id_1");
    buf->setStreamid("inlong_stream_id");

    UserMsgPtr req = make_shared<UserMsg>("lksdewoigiore", "127.0.0.1", 0, callBackFunc, "no_attr", "127.0.0.1", 0);
    for (size_t i = 0; i < 10; i++)
    {
        buf->addUserMsg(req);
    }

    buf->doUserCallBack();
    delete buf;
}

TEST(sendBuffer, basetest)
{
    SendBuffer* buf = new SendBuffer(102400);
    buf->increaseRetryNum();
    buf->increaseRetryNum();
    EXPECT_EQ(buf->getAlreadySend(), 2);

    char s[100] = "gooooooooo";
    memcpy(buf->content(), s, strlen(s));
    buf->setLen(strlen(s));
    LOG_INFO("buf content is:%s, len:%d", buf->content(), buf->len());

    buf->reset();
    EXPECT_EQ(buf->len(), 0);
    EXPECT_EQ(buf->getAlreadySend(), 0);
    LOG_INFO("buf content is:%s", buf->content());

    delete buf;
}

TEST(senBuffer, mutextest) { SendBuffer* buf = new SendBuffer(1024); }

int main(int argc, char* argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}