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

#ifndef _TUBEMQ_EXECUTOR_POOL_
#define _TUBEMQ_EXECUTOR_POOL_

#include <stdlib.h>

#include <memory>
#include <mutex>
#include <thread>
#include <vector>
#include <functional>

#include <asio.hpp>
#include <asio/ssl.hpp>
#include "noncopyable.h"

namespace tubemq {

using TcpSocketPtr = std::shared_ptr<asio::ip::tcp::socket>;
using TlsSocketPtr = std::shared_ptr<asio::ssl::stream<asio::ip::tcp::socket &> >;
using TcpResolverPtr = std::shared_ptr<asio::ip::tcp::resolver>;
using SteadyTimerPtr = std::shared_ptr<asio::steady_timer>;

class Executor : noncopyable {
 public:
  Executor();
  ~Executor();

  TcpSocketPtr CreateTcpSocket();
  TlsSocketPtr CreateTlsSocket(TcpSocketPtr &socket, asio::ssl::context &ctx);
  TcpResolverPtr CreateTcpResolver();
  SteadyTimerPtr CreateSteadyTimer();

  inline void Post(std::function<void(void)> task) { io_context_->post(task); }

  std::shared_ptr<asio::io_context> GetIoContext() { return io_context_; }

  // Close executor and exit thread
  void Close();

 private:
  void StartWorker(std::shared_ptr<asio::io_context> io_context);
  std::shared_ptr<asio::io_context> io_context_;
  using io_context_work = asio::executor_work_guard<asio::io_context::executor_type>;
  io_context_work work_;
  std::thread worker_;
};

typedef std::shared_ptr<Executor> ExecutorPtr;

class ExecutorPool : noncopyable {
 public:
  explicit ExecutorPool(int nthreads = 2);

  ExecutorPtr Get();

  // Resize executor thread
  void Resize(int nthreads) {
    Lock lock(mutex_);
    executors_.resize(nthreads);
  }

  void Close();

 private:
  typedef std::vector<ExecutorPtr> ExecutorList;
  ExecutorList executors_;
  uint32_t executorIdx_;
  std::mutex mutex_;
  typedef std::unique_lock<std::mutex> Lock;
};

typedef std::shared_ptr<ExecutorPool> ExecutorPoolPtr;

}  // namespace tubemq

#endif  // _TUBEMQ_EXECUTOR_POOL_
