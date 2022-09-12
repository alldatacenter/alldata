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

#ifndef TUBEMQ_CLIENT_ATOMIC_DEF_H_
#define TUBEMQ_CLIENT_ATOMIC_DEF_H_

#include <stdlib.h>

#include <atomic>

namespace tubemq {

template <class T>
class Atomic {
 public:
  Atomic() : counter_(0) {}
  explicit Atomic(T initial_value) : counter_(initial_value) {}

  inline T Get() const { return counter_.load(std::memory_order_relaxed); }
  inline void Set(T new_value) { counter_.store(new_value, std::memory_order_relaxed); }

  inline int64_t LongValue() const {
    return static_cast<int64_t>(counter_.load(std::memory_order_relaxed));
  }
  inline int32_t IntValue() const {
    return static_cast<int32_t>(counter_.load(std::memory_order_relaxed));
  }

  inline T GetAndSet(T new_value) { return counter_.exchange(new_value); }

  inline bool CompareAndSet(T expect, T update) {
    return counter_.compare_exchange_strong(expect, update, std::memory_order_relaxed);
  }

  // return old value
  inline T GetAndIncrement() { return counter_.fetch_add(1, std::memory_order_relaxed); }

  inline T GetAndDecrement() { return counter_.fetch_add(-1, std::memory_order_relaxed); }

  inline T GetAndAdd(T delta) { return counter_.fetch_add(delta, std::memory_order_relaxed); }

  // return new value
  inline T IncrementAndGet() { return AddAndGet(1); }

  inline T DecrementAndGet() { return AddAndGet(-1); }

  inline T AddAndGet(T delta) {
    return counter_.fetch_add(delta, std::memory_order_relaxed) + delta;
  }

 private:
  std::atomic<T> counter_;
};

using AtomicInteger = Atomic<int32_t>;
using AtomicLong = Atomic<int64_t>;

class AtomicBoolean {
 public:
  AtomicBoolean() : counter_(false) {}
  explicit AtomicBoolean(bool initial_value) : counter_(initial_value) {}

  inline bool Get() const { return counter_.load(std::memory_order_relaxed); }
  inline void Set(bool new_value) { counter_.store(new_value, std::memory_order_relaxed); }

  // return old value
  inline bool GetAndSet(bool new_value) { return counter_.exchange(new_value); }

  // CAS SET
  inline bool CompareAndSet(bool expect, bool update) {
    return counter_.compare_exchange_strong(expect, update, std::memory_order_relaxed);
  }

 private:
  std::atomic<bool> counter_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_ATOMIC_DEF_H_
