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

#ifndef _TUBEMQ_CONNECTION_
#define _TUBEMQ_CONNECTION_

#include <stdlib.h>

#include <chrono>
#include <ctime>
#include <deque>
#include <exception>
#include <functional>
#include <iostream>
#include <sstream>
#include <string>
#include <unordered_map>

#include "any.h"
#include "buffer.h"
#include "noncopyable.h"
#include "transport.h"
#include "unique_seq_id.h"

namespace tubemq {

// Check Package is done
using ProtocalCheckerFunction = std::function<int(BufferPtr&, Any&, uint32_t&, bool&, size_t&)>;

class Connection : noncopyable {
 public:
  enum Status { kConnecting, kConnected, kDisconnected };
  Connection()
      : connect_id_(unique_id_.Next()),
        status_(kConnecting),
        recv_time_(std::time(nullptr)),
        create_time_(std::time(nullptr)) {
    formatContextString();
  }
  Connection(const std::string& ip, uint16_t port)
      : ip_(ip),
        port_(port),
        connect_id_(unique_id_.Next()),
        status_(kConnecting),
        recv_time_(std::time(nullptr)),
        create_time_(std::time(nullptr)) {
    formatContextString();
  }
  virtual ~Connection() {}

  virtual void Close() = 0;

  virtual void AsyncWrite(RequestContextPtr& req) = 0;

  Status GetStatus() const { return status_; }
  uint32_t GetConnectID() const { return connect_id_; }

  inline bool IsStop() const { return status_ == kDisconnected; }
  inline bool IsConnected() const { return status_ == kConnected; }

  void SetCloseNotifier(CloseNotifier func) { notifier_ = func; }

  void SetProtocalCheck(ProtocalCheckerFunction func) { check_ = func; }

  inline std::time_t GetRecvTime() const { return recv_time_; }

  inline const std::string ToString() const {
    std::stringstream stream;
    stream << "[recvtime:" << recv_time_ << "]"
           << "[read_package:" << read_package_number_ << "]"
           << "[write_package:" << write_package_number_ << "]";
    return context_string_ + stream.str();
  }

 private:
  void formatContextString() {
    std::stringstream stream;
    stream << "[id:" << connect_id_ << "]"
           << "[time:" << create_time_ << "]";
    context_string_ += stream.str();
  }

 protected:
  CloseNotifier notifier_;
  ProtocalCheckerFunction check_;
  std::string ip_;
  uint16_t port_;
  uint32_t connect_id_;
  std::atomic<Status> status_;
  std::string context_string_;  // for log
  std::time_t recv_time_;
  size_t package_length_ = 0;
  size_t read_package_number_ = 0;
  size_t write_package_number_ = 0;

 private:
  std::time_t create_time_;
  static UniqueSeqId unique_id_;
};
using ConnectionPtr = std::shared_ptr<Connection>;
}  // namespace tubemq

#endif  // _TUBEMQ_CONNECTION_

