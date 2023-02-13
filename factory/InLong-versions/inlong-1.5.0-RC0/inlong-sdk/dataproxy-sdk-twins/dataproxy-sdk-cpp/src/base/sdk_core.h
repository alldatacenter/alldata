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

#ifndef DATAPROXY_SDK_BASE_CORE_H_
#define DATAPROXY_SDK_BASE_CORE_H_

#include <memory>
#include <string>
#include <unordered_map>
#include <asio.hpp>

#include "atomic.h"
#include "user_msg.h"
#include "client_config.h"

// #include "buffer_pool.h"
namespace dataproxy_sdk {
class GlobalCluster;
class GlobalQueues;
class TotalPools;

class ExecutorThread;
using ExecutorThreadPtr = std::shared_ptr<ExecutorThread>;

class ExecutorThreadPool;

class Connection;
using ConnectionPtr = std::shared_ptr<Connection>;

class ProxyInfo;
using ProxyInfoPtr = std::shared_ptr<ProxyInfo>;

class SendBuffer;

using TcpSocketPtr = std::shared_ptr<asio::ip::tcp::socket>;
using SteadyTimerPtr = std::shared_ptr<asio::steady_timer>;
using io_context_work = asio::executor_work_guard<asio::io_context::executor_type>;

extern AtomicUInt          g_send_msgid;
extern ClientConfig        g_config;
extern GlobalCluster*      g_clusters;
extern GlobalQueues*       g_queues;
extern TotalPools*         g_pools;
extern ExecutorThreadPool* g_executors;
extern AtomicInt           user_exit_flag;
}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_CORE_H_