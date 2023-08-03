/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <queue>
#include <type_traits>
#include <bthread/condition_variable.h>
#include <bthread/mutex.h>
#include <Common/CurrentMetrics.h>
#include <Common/CurrentThread.h>
#include <Common/Exception.h>
#include <common/MoveOrCopyIfThrow.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int STD_EXCEPTION;
}

template <typename T>
class BoundedDataQueue
{
public:
    explicit BoundedDataQueue(size_t capacity_ = 20) : full_cv(), empty_cv(), capacity(capacity_) { }

    void push(const T & x)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        while (queue.size() == capacity && !is_closed)
        {
            full_cv.wait(lock);
        }
        if (is_closed)
            throw Exception("Queue is closed", ErrorCodes::STD_EXCEPTION);
        queue.push(x);
        empty_cv.notify_all();
    }

    void pop(T & x)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        while (queue.empty() && !is_closed)
        {
            empty_cv.wait(lock);
        }
        if (is_closed)
            throw Exception("Queue is closed", ErrorCodes::STD_EXCEPTION);
        ::detail::moveOrCopyIfThrow(std::move(queue.front()), x);
        queue.pop();
        full_cv.notify_all();
    }

    bool tryPush(const T & x, UInt64 milliseconds = 0)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        while (queue.size() == capacity && !is_closed)
        {
            if (ETIMEDOUT == full_cv.wait_for(lock, milliseconds * 1000))
                return false;
        }
        if (is_closed)
            return false;
        queue.push(x);
        empty_cv.notify_all();
        return true;
    }

    bool tryPop(T & x, UInt64 milliseconds = 0)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        while (queue.empty() && !is_closed)
        {
            if (ETIMEDOUT == empty_cv.wait_for(lock, milliseconds * 1000))
                return false;
        }

        if (is_closed)
            return false;

        ::detail::moveOrCopyIfThrow(std::move(queue.front()), x);
        queue.pop();
        full_cv.notify_all();
        return true;
    }

    template <typename... Args>
    bool tryEmplace(UInt64 milliseconds, Args &&... args)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        while (queue.size() == capacity && !is_closed)
        {
            if (ETIMEDOUT == full_cv.wait_for(lock, milliseconds * 1000))
                return false;
        }
        if (is_closed)
            return false;
        queue.emplace(std::forward<Args>(args)...);
        empty_cv.notify_all();
        return true;
    }

    size_t size()
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        return queue.size();
    }

    bool empty()
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        return queue.empty();
    }

    void clear()
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        while (!queue.empty())
        {
            queue.pop();
        }
        std::queue<T> empty_queue;
        std::swap(empty_queue, queue);
    }

    void setCapacity(size_t queue_capacity)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        capacity = queue_capacity;
    }

    bool close()
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        if (is_closed)
            return false;

        is_closed = true;
        empty_cv.notify_all();
        full_cv.notify_all();
        return true;
    }

    bool closed() { return is_closed; }

private:
    std::queue<T> queue;
    bthread::Mutex mutex;
    bthread::ConditionVariable full_cv;
    bthread::ConditionVariable empty_cv;
    size_t capacity;
    bool is_closed = false;
};

}
