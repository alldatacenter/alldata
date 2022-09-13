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
#include "tc_api.h"
#include "utils.h"
#include <asio.hpp>
#include <chrono>
#include <functional>
#include <gtest/gtest.h>
#include <iostream>
#include <stdint.h>
#include <string>
#include <thread>
using namespace std;
using namespace dataproxy_sdk;

void handler(int a, const asio::error_code& error)
{
    if (error)
    {
        LOG_INFO("timer%d is canceled", a);
        return;
    }
    LOG_INFO("async_wait end and invoke handler:%d", a);
}

void connectHandler(const asio::error_code& error, TcpSocketPtr& socketptr)
{
    if (error)
    {
        LOG_ERROR("connect 0.0.0.0:8080, errno:%s", error.message().c_str());
        return;
    }
    LOG_WARN("errno:%s", error.message().c_str());

    LOG_WARN("connect 0.0.0.0:8080 successfully,local:%s:%d", socketptr->local_endpoint().address().to_string().c_str(),
             socketptr->local_endpoint().port());
}

int times = 1;
void repeatHandler(SteadyTimerPtr timer, const asio::error_code& error)
{
    if (error)
    {
        LOG_INFO("timer is canceled");
        return;
    }
    LOG_INFO("async_wait end and invoke handler:%d", times);
    ++times;
    timer->expires_after(std::chrono::seconds(3));
    timer->async_wait(std::bind(repeatHandler, timer, std::placeholders::_1));
}

TEST(executor, sockettest)
{
    ExecutorThreadPtr th = make_shared<ExecutorThread>(0);
    auto socketptr       = th->createTcpSocket();
    asio::ip::tcp::endpoint ep(asio::ip::address::from_string("0.0.0.0"), static_cast<uint16_t>(8080));
    socketptr->async_connect(ep, std::bind(connectHandler, std::placeholders::_1, socketptr));
    // asio::async_connect(*socketptr, ep, std::bind(connectHandler, std::placeholders::_1));
    // socketptr->connect(ep);
    LOG_WARN("connect successful, connect %d", socketptr->is_open());

    auto buf = new char[1024];
    memset(buf, 'q', 1024);
    asio::async_write(*socketptr, asio::buffer(buf, 1024), [buf](const std::error_code& ec, std::size_t sz) {
        delete buf;
        if (ec)
        {
            LOG_ERROR("write error");
            return;
        }
        LOG_WARN("errno:%s", ec.message().c_str());
        LOG_WARN("async_write callback, write_len=%d", sz);
    });
    this_thread::sleep_for(std::chrono::minutes(1));
}

TEST(executorPool, test1)
{
    ExecutorThreadPool* pool = new ExecutorThreadPool();
    EXPECT_EQ(pool->getExecutor(0)->threadId(), 0);
    EXPECT_EQ(pool->getExecutor(2)->threadId(), 2);
    EXPECT_EQ(pool->getExecutor(5), nullptr);

    EXPECT_EQ(pool->nextExecutor()->threadId(), 0);
    for (int i = 0; i < 8; i++)
    {
        pool->nextExecutor();
    }
    EXPECT_EQ(pool->nextExecutor()->threadId(), 4);
    // pool->close();
    delete pool;
}

TEST(executor, timertest)
{
    ExecutorThreadPtr th1 = make_shared<ExecutorThread>(1);
    auto timer            = th1->createSteadyTimer();
    timer->expires_after(std::chrono::seconds(5));
    LOG_INFO("timer start sync wait");
    timer->wait();
    LOG_INFO("timer end sync wait");

    timer->expires_after(std::chrono::seconds(3));
    LOG_INFO("timer start async_wait1");
    timer->async_wait(std::bind(handler, 1, std::placeholders::_1));
    LOG_INFO("timer end async_wait1");

    timer->expires_after(std::chrono::seconds(10));
    LOG_INFO("timer start async_wait2");
    timer->async_wait(std::bind(handler, 2, std::placeholders::_1));
    LOG_INFO("timer end async_wait2");

    this_thread::sleep_for(std::chrono::seconds(1));
}

TEST(executor, multimertest)
{
    LOG_INFO("\n");
    ExecutorThreadPtr th1 = make_shared<ExecutorThread>(1);
    auto timer1           = th1->createSteadyTimer();
    timer1->expires_after(std::chrono::seconds(5));
    LOG_INFO("timer1 request async_wait");
    timer1->async_wait(std::bind(handler, 1, std::placeholders::_1));
    LOG_INFO("timer1 end async_wait1");

    auto timer2 = th1->createSteadyTimer();
    timer2->expires_after(std::chrono::seconds(2));
    LOG_INFO("timer2 request async_wait2");
    timer2->async_wait(std::bind(handler, 2, std::placeholders::_1));
    LOG_INFO("timer end async_wait2");

    this_thread::sleep_for(std::chrono::seconds(10));
}

TEST(executor, repeattimer)
{
    LOG_INFO("\n");
    ExecutorThreadPtr th1 = make_shared<ExecutorThread>(1);
    auto timer            = th1->createSteadyTimer();
    timer->expires_after(std::chrono::seconds(3));
    timer->async_wait(std::bind(repeatHandler, timer, std::placeholders::_1));

    this_thread::sleep_for(std::chrono::minutes(1));
}

int main(int argc, char* argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}