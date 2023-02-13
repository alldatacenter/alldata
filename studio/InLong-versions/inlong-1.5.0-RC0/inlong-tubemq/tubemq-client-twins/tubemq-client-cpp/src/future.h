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

#ifndef _TUBEMQ_FUTURE_H_
#define _TUBEMQ_FUTURE_H_

#include <condition_variable>
#include <functional>
#include <memory>
#include <mutex>
#include <vector>

#include "tubemq/tubemq_errcode.h"

namespace tubemq {

template <typename Value>
struct FutureInnerState {
  std::mutex mutex_;
  std::condition_variable condition_;
  ErrorCode error_code_;
  Value value_;
  bool ready_ = false;
  bool failed_ = false;
  using FutureCallBackFunc = std::function<void(ErrorCode, const Value&)>;
  std::vector<FutureCallBackFunc> callbacks_;
};

template <typename Value>
class Future {
 public:
  using FutureInnerStatePtr = std::shared_ptr<FutureInnerState<Value> >;
  using FutureCallBackFunc = std::function<void(ErrorCode, const Value&)>;
  Future& AddCallBack(FutureCallBackFunc callback) {
    Lock lock(state_->mutex_);

    if (state_->ready_) {
      lock.unlock();
      callback(state_->error_code_, state_->value_);
    } else {
      state_->callbacks_.push_back(callback);
    }
    return *this;
  }

  ErrorCode Get(Value& value) {
    Lock lock(state_->mutex_);

    if (!state_->ready_) {
      // Wait for error_code_
      while (!state_->ready_) {
        state_->condition_.wait(lock);
      }
    }

    value = state_->value_;
    return state_->error_code_;
  }

 private:
  using Lock = std::unique_lock<std::mutex>;
  explicit Future(FutureInnerStatePtr state) : state_(state) {}
  FutureInnerStatePtr state_;

  template <typename V>
  friend class Promise;
};

template <typename Value>
class Promise {
 public:
  using FutureInnerStatePtr = std::shared_ptr<FutureInnerState<Value> >;
  using FutureCallBackFunc = std::function<void(ErrorCode, const Value&)>;
  Promise() : state_(std::make_shared<FutureInnerState<Value> >()) {}

  bool SetValue(const Value& value) {
    Lock lock(state_->mutex_);

    if (state_->ready_) {
      return false;
    }

    state_->value_ = value;
    state_->ready_ = true;

    callbackAndNotify();
    return true;
  }

  bool SetFailed(const ErrorCode& error_code_) {
    Lock lock(state_->mutex_);

    if (state_->ready_) {
      return false;
    }

    state_->error_code_ = error_code_;
    state_->ready_ = true;
    state_->failed_ = true;

    callbackAndNotify();
    return true;
  }

  bool IsReady() const { return state_->ready_; }

  bool IsFailed() const { return state_->failed_; }

  Future<Value> GetFuture() const { return Future<Value>(state_); }

 private:
  void callbackAndNotify() {
    for (auto callback : state_->callbacks_) {
      callback(state_->error_code_, state_->value_);
    }
    state_->callbacks_.clear();
    state_->condition_.notify_all();
  }

 private:
  using Lock = std::unique_lock<std::mutex>;
  FutureInnerStatePtr state_;
};

} /* namespace tubemq */

#endif /* _TUBEMQ_FUTURE_H_ */
