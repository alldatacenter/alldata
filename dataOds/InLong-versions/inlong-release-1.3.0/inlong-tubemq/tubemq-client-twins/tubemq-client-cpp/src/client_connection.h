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

#ifndef _TUBEMQ_CLIENT_CONNECTION_
#define _TUBEMQ_CLIENT_CONNECTION_

#include <stdlib.h>

#include <chrono>
#include <deque>
#include <exception>
#include <functional>
#include <iostream>
#include <list>
#include <memory>
#include <sstream>
#include <string>
#include <unordered_map>

#include "asio.hpp"
#include "connection.h"
#include "const_rpc.h"
#include "executor_pool.h"
#include "logger.h"
#include "noncopyable.h"
#include "transport.h"

namespace tubemq {

struct TransportRequest {
  RequestContextPtr req_;
  SteadyTimerPtr deadline_;
};

class ClientConnection : public Connection, public std::enable_shared_from_this<ClientConnection> {
 public:
  // executor: ExecutorPool.Get()
  // endpoints: executor->CreateTcpResolver()->resolve("ip", port);
  ClientConnection(ExecutorPtr& executor, const std::string& ip, uint16_t port)
      : Connection(ip, port),
        executor_(executor),
        socket_(std::move(executor->CreateTcpSocket())),
        recv_buffer_(std::make_shared<Buffer>(rpc_config::kRpcConnectInitBufferSize)),
        deadline_(std::move(executor->CreateSteadyTimer())) {
    auto endpoints = executor_->CreateTcpResolver()->resolve(ip_, std::to_string(port_));
    connect(endpoints);
  }

  virtual ~ClientConnection() {}

  virtual void AsyncWrite(RequestContextPtr& req);

  virtual void Close();

 private:
  void requestTimeoutHandle(const std::error_code& ec, RequestContextPtr req);

  void close(const std::error_code* err = nullptr);

  void releaseAllRequest(const std::error_code* err = nullptr);
  void connect(const asio::ip::tcp::resolver::results_type& endpoints);
  void checkDeadline(const std::error_code& ec);
  void contextString();
  void asyncRead();
  int checkPackageDone();
  void requestCallback(uint32_t request_id, ErrorCode* err = nullptr, Any* check_out = nullptr);
  TransportRequest* nextTransportRequest();
  void asyncWrite();

 private:
  using BufferQueue = std::deque<uint32_t>;
  static const uint32_t kConnnectMaxTimeMs{1000 * 20};  // ms
  static const uint32_t kReadMaxTimeMs{1000 * 30};      // ms
  ExecutorPtr executor_;
  TcpSocketPtr socket_;
  BufferPtr recv_buffer_;
  BufferQueue write_queue_;
  std::unordered_map<uint32_t, TransportRequest> request_list_;  // requestid->request context
  SteadyTimerPtr deadline_;
};
using ClientConnectionPtr = std::shared_ptr<ClientConnection>;

}  // namespace tubemq

#endif  // _TUBEMQ_CLIENT_CONNECTION_
