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

#ifndef _TUBEMQ_THREAD_POOL_
#define _TUBEMQ_THREAD_POOL_

#include <stdlib.h>

#include <chrono>
#include <functional>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

#include <asio.hpp>
#include <asio/ssl.hpp>
#include "noncopyable.h"


namespace tubemq {
// ThreadPool use one io_context for thread pool
class ThreadPool : noncopyable {
 public:
  explicit ThreadPool(std::size_t size)
      : io_context_(size), work_(asio::make_work_guard(io_context_)) {
    for (size_t i = 0; i < size; ++i) {
      workers_.emplace_back([this] { io_context_.run(); });
    }
  }

  ~ThreadPool() {
    work_.reset();
    io_context_.stop();
    for (std::thread &worker : workers_) {
      worker.join();
    }
    workers_.clear();
  }

  template <class function>
  void Post(function f) {
    io_context_.post(f);
  }

 private:
  asio::io_context io_context_;
  using io_context_work = asio::executor_work_guard<asio::io_context::executor_type>;
  io_context_work work_;
  std::vector<std::thread> workers_;
};  // namespace tubemq
}  // namespace tubemq
#endif  // _TUBEMQ_THREAD_POOL_
