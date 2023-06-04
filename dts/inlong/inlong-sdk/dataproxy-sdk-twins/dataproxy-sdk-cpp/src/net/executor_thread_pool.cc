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

#include "executor_thread_pool.h"

namespace dataproxy_sdk
{
    ExecutorThread::ExecutorThread(int32_t id)
        : io_context_(std::make_shared<asio::io_context>()), work_(asio::make_work_guard(*io_context_)), worker_(std::bind(&ExecutorThread::startWorker, this)), thread_id_(id)
    {
    }

    ExecutorThread::~ExecutorThread()
    {
        LOG_DEBUG("thread:%d destructor", thread_id_);
        close();
        if (worker_.joinable())
        {
            worker_.detach();
        }
    }

    void ExecutorThread::close()
    {
        LOG_DEBUG("thread:%d close() --> io_context stop", thread_id_);
        io_context_->stop();
        if (std::this_thread::get_id() != worker_.get_id() && worker_.joinable())
        {
            LOG_DEBUG("join thread %d", thread_id_);
            worker_.join();
        }
    }

    void ExecutorThread::startWorker() //start listen
    {
        LOG_DEBUG("thread:%d start io_context run", thread_id_);
        io_context_->run();
    }

    TcpSocketPtr ExecutorThread::createTcpSocket() //create socket based on io_context
    {
        return std::make_shared<asio::ip::tcp::socket>(*io_context_);
    }

    SteadyTimerPtr ExecutorThread::createSteadyTimer() //create timer based on io_context
    {
        return std::make_shared<asio::steady_timer>(*io_context_);
    }

    ExecutorThreadPool::ExecutorThreadPool() : next_idx_(0)
    {
        for (int i = 0; i < g_config.thread_nums_; i++)
        {
            executors_.emplace_back(std::make_shared<ExecutorThread>(i));
        }
        
    }

    ExecutorThreadPtr ExecutorThreadPool::nextExecutor()
    {
        if (executors_.empty())
        {
            LOG_ERROR("fail to find executor thread pool");
            return nullptr;
        }
        int32_t idx = (next_idx_++) % executors_.size();
        return executors_[idx];       
    }

    ExecutorThreadPtr ExecutorThreadPool::getExecutor(int32_t id)
    {
        if (id < 0 || id >= executors_.size() || executors_.empty())
        {
            LOG_ERROR("fail to get network_thread<id:%d>, max id is %d", id, executors_.size() - 1);
            return nullptr;
        }
        return executors_[id];
        
    }

    void ExecutorThreadPool::showState()
    {
        for (auto &executor : executors_)
        {
            executor->showState();
        }
        
    }

    void ExecutorThreadPool::close()
    {
        for (auto it : executors_)
        {
            if (it != nullptr)
            {
                it->close();
                LOG_DEBUG("thread_id:%d, shared_count:%d", it->threadId(), it.use_count());
            }
            it.reset();
        }
        executors_.clear();
        
    }
} // namespace dataproxy_sdk
