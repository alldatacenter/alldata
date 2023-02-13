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

#include "executor_pool.h"

#include <memory>
#include <functional>

#include <asio.hpp>


namespace tubemq {

Executor::Executor()
    : io_context_(new asio::io_context()),
      work_(asio::make_work_guard(*io_context_)),
      worker_(std::bind(&Executor::StartWorker, this, io_context_)) {}

Executor::~Executor() {
  Close();
  if (worker_.joinable()) {
    worker_.detach();
  }
}

void Executor::StartWorker(std::shared_ptr<asio::io_context> io_context) { io_context_->run(); }

TcpSocketPtr Executor::CreateTcpSocket() {
  return std::make_shared<asio::ip::tcp::socket>(*io_context_);
}

TlsSocketPtr Executor::CreateTlsSocket(TcpSocketPtr &socket, asio::ssl::context &ctx) {
  return std::make_shared<asio::ssl::stream<asio::ip::tcp::socket &>>(*socket, ctx);
}

TcpResolverPtr Executor::CreateTcpResolver() {
  return std::make_shared<asio::ip::tcp::resolver>(*io_context_);
}

SteadyTimerPtr Executor::CreateSteadyTimer() {
  return std::make_shared<asio::steady_timer>(*io_context_);
}

void Executor::Close() {
  io_context_->stop();
  if (std::this_thread::get_id() != worker_.get_id() && worker_.joinable()) {
    worker_.join();
  }
}

ExecutorPool::ExecutorPool(int nthreads) : executors_(nthreads), executorIdx_(0), mutex_() {}

ExecutorPtr ExecutorPool::Get() {
  Lock lock(mutex_);

  int idx = executorIdx_++ % executors_.size();
  if (!executors_[idx]) {
    executors_[idx] = std::make_shared<Executor>();
  }

  return executors_[idx];
}

void ExecutorPool::Close() {
  for (auto it = executors_.begin(); it != executors_.end(); ++it) {
    if (*it != nullptr) {
      (*it)->Close();
    }
    it->reset();
  }
}
}  // namespace tubemq
