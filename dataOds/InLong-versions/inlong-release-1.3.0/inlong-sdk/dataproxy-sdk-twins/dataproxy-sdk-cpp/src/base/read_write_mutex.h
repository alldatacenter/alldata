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

#ifndef DATAPROXY_SDK_BASE_READ_WRITE_MUTEX_H_
#define DATAPROXY_SDK_BASE_READ_WRITE_MUTEX_H_

#include <condition_variable>
#include <mutex>

namespace dataproxy_sdk
{
// wirte operation add lock：unique_read_lock<read_write_mutex> lock( rwmutex );
// read operation add lock：unique_write_lock<read_write_mutex> lock(rwmutex);

class read_write_mutex
{
  public:
    read_write_mutex()  = default;
    ~read_write_mutex() = default;

    read_write_mutex(const read_write_mutex&) = delete;
    read_write_mutex& operator=(const read_write_mutex&) = delete;

    read_write_mutex(const read_write_mutex&&) = delete;
    read_write_mutex& operator=(const read_write_mutex&&) = delete;

    void lock_read()
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        m_cond_read.wait(lock, [this]() -> bool { return m_write_count == 0; });
        ++m_read_count;
    }

    void unlock_read()
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        if (--m_read_count == 0 && m_write_count > 0) { m_cond_write.notify_one(); }
    }

    void lock_write()
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        ++m_write_count;
        m_cond_write.wait(lock, [this]() -> bool { return m_read_count == 0 && !m_writing; });
        m_writing = true;
    }

    void unlock_write()
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        if (--m_write_count == 0) { m_cond_read.notify_all(); }
        else
        {
            m_cond_write.notify_one();
        }
        m_writing = false;
    }

  private:
    volatile size_t m_read_count  = 0;
    volatile size_t m_write_count = 0;
    volatile bool m_writing       = false;
    mutable std::mutex m_mutex;  // KEYPOINT: add mutable
    std::condition_variable m_cond_read;
    std::condition_variable m_cond_write;
};

template <typename _ReadWriteLock>
class unique_read_lock
{
  public:
    explicit unique_read_lock(_ReadWriteLock& rwLock) : m_ptr_rw_lock(&rwLock) { m_ptr_rw_lock->lock_read(); }

    ~unique_read_lock()
    {
        if (m_ptr_rw_lock) { m_ptr_rw_lock->unlock_read(); }
    }

    unique_read_lock()                        = delete;
    unique_read_lock(const unique_read_lock&) = delete;
    unique_read_lock& operator=(const unique_read_lock&) = delete;
    unique_read_lock(const unique_read_lock&&)           = delete;
    unique_read_lock& operator=(const unique_read_lock&&) = delete;

  private:
    _ReadWriteLock* m_ptr_rw_lock = nullptr;
};

template <typename _ReadWriteLock>
class unique_write_lock
{
  public:
    explicit unique_write_lock(_ReadWriteLock& rwLock) : m_ptr_rw_lock(&rwLock) { m_ptr_rw_lock->lock_write(); }

    ~unique_write_lock()
    {
        if (m_ptr_rw_lock) { m_ptr_rw_lock->unlock_write(); }
    }

    unique_write_lock()                         = delete;
    unique_write_lock(const unique_write_lock&) = delete;
    unique_write_lock& operator=(const unique_write_lock&) = delete;
    unique_write_lock(const unique_write_lock&&)           = delete;
    unique_write_lock& operator=(const unique_write_lock&&) = delete;

    void unlock()
    {
        if (m_ptr_rw_lock) { m_ptr_rw_lock->unlock_write(); }
    }

  private:
    _ReadWriteLock* m_ptr_rw_lock = nullptr;
};

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_READ_WRITE_MUTEX_H_