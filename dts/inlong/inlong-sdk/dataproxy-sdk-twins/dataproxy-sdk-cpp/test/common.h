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
#include "sdk_core.h"
#include "executor_thread_pool.h"
#include "logger.h"
#include "msg_protocol.h"
#include "pack_queue.h"
#include "send_buffer.h"
#include "socket_connection.h"
#include "tc_api.h"
#include "utils.h"
#include <algorithm>
#include <asio.hpp>
#include <chrono>
#include <functional>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <iostream>
#include <map>
#include <memory>
#include <stdint.h>
#include <string>
#include <thread>
#include <utility>
using namespace std;
using namespace dataproxy_sdk;
using namespace testing;

class BufferPoolMock : public BufferPool
{
  public:
    BufferPoolMock(int32_t id, uint32_t buf_num, uint32_t buf_size) : BufferPool(id, buf_num, buf_size) {}
};
using BufferPoolMockPtr = shared_ptr<BufferPoolMock>;

class TotalPoolsMock : public TotalPools
{
  public:
    TotalPoolsMock() : TotalPools() {}
    MOCK_METHOD1(getPool, BufferPoolPtr(int32_t));
};

class GlobalQueuesMock : public GlobalQueues
{
    GlobalQueuesMock() : GlobalQueues() {}
    MOCK_METHOD2(getPackQueue, PackQueuePtr(const std::string&, const std::string&));
};

class ClusterProxyListMock : public ClusterProxyList
{
    ClusterProxyListMock() : ClusterProxyList() {}
    MOCK_METHOD0(isNeedLoadBalance, bool(void));
    MOCK_METHOD0(initConn, int32_t(void));
};
using ClusterBuslistMockPtr = shared_ptr<ClusterProxyListMock>;

class GlobalClusterMock : public GlobalCluster
{
  public:
    GlobalClusterMock() {}
    MOCK_METHOD1(getSendConn, ConnectionPtr(const std::string&));
    MOCK_METHOD1(printAckNum, void(const std::error_code&));
};
