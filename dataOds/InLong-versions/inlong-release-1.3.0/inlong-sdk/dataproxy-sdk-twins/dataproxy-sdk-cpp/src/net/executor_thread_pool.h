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

#ifndef CPAI_NET_EXECUTOR_THREAD_POOL_H_
#define CPAI_NET_EXECUTOR_THREAD_POOL_H_

#include <asio.hpp>
#include <functional>
#include <memory>
#include <stdint.h>
#include <thread>

#include "sdk_core.h"
#include "logger.h"
#include "noncopyable.h"

namespace dataproxy_sdk
{
  class ExecutorThread : noncopyable
  {
  private:
    std::shared_ptr<asio::io_context> io_context_;
    io_context_work work_; //guarantee io_context.run() don't exit
    std::thread worker_;
    int32_t thread_id_;

  public:
    AtomicUInt waiting_send_;

  public:
    ExecutorThread(int32_t id);
    virtual ~ExecutorThread();

    inline void postTask(std::function<void(void)> task) { io_context_->post(task); }
    std::shared_ptr<asio::io_context> getIoContext() { return io_context_; }
    TcpSocketPtr createTcpSocket();
    SteadyTimerPtr createSteadyTimer();
    inline void showState(){LOG_DEBUG("------->thread_id:%d, waiting_send:%d", thread_id_, waiting_send_.get());}
    void close();

    int32_t threadId() const { return thread_id_; }
    
  private:
    void startWorker();
  };

  //network threads
  class ExecutorThreadPool : noncopyable
  {
  private:
    // round-robin
    std::vector<ExecutorThreadPtr> executors_;
    int32_t next_idx_;

  public:
    explicit ExecutorThreadPool();
    virtual ~ExecutorThreadPool() { close(); }

    ExecutorThreadPtr nextExecutor(); // round-robbin, get executor thread, for new conn creating
    ExecutorThreadPtr getExecutor(int32_t id);
    void showState();
    void close();
  };

} // namespace dataproxy_sdk

#endif // CPAI_NET_EXECUTOR_THREAD_POOL_H_