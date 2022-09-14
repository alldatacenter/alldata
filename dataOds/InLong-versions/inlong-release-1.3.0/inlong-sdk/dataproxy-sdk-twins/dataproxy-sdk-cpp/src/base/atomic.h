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

#ifndef DATAPROXY_SDK_BASE_ATOMIC_H_
#define DATAPROXY_SDK_BASE_ATOMIC_H_

#include <stdint.h>

#include "noncopyable.h"
namespace dataproxy_sdk {
template <typename T>
class AtomicIntegerT : noncopyable {
private:
    volatile T value_;

public:
    AtomicIntegerT() : value_(0) {}

    explicit AtomicIntegerT(T value) : value_(value) {}

    // if value_ equals oldval, update it as newval and return true;
    // otherwise, return false
    inline bool compareAndSwap(T oldval, T newval)
    {
        return __sync_bool_compare_and_swap(&value_, oldval, newval);
    }

    inline T get()
    {
        return __sync_val_compare_and_swap(&value_, 0, 0);
    }

    inline T getAndAdd(T x)
    {
        return __sync_fetch_and_add(&value_, x);
    }

    inline T getAndIncrease()
    {
        return __sync_fetch_and_add(&value_, 1);
    }

    inline T addAndGet(T x)
    {
        return getAndAdd(x) + x;
    }

    inline T incrementAndGet()
    {
        return addAndGet(1);
    }

    inline T decrementAndGet()
    {
        return addAndGet(-1);
    }

    inline void add(T x)
    {
        getAndAdd(x);
    }

    inline void increment()
    {
        incrementAndGet();
    }

    inline void decrement()
    {
        decrementAndGet();
    }

    inline T getAndSet(T newValue)
    {
        return __sync_lock_test_and_set(&value_, newValue);
    }
};

using AtomicInt = AtomicIntegerT<int32_t>;

using AtomicUInt = AtomicIntegerT<uint32_t>;

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_ATOMIC_H_