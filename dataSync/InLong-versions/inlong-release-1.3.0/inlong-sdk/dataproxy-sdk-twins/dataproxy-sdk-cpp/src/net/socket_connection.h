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

#ifndef DATAPROXY_SDK_NET_CONNECTION_H_
#define DATAPROXY_SDK_NET_CONNECTION_H_

#include <asio.hpp>
#include <deque>
#include <functional>
#include <memory>
#include <mutex>
#include <system_error>
#include <unordered_map>

#include "sdk_core.h"
#include "executor_thread_pool.h"
#include "logger.h"
#include "noncopyable.h"
#include "recv_buffer.h"
#include "msg_protocol.h"

namespace dataproxy_sdk
{
  class SendBuffer;
  class Connection : noncopyable, public std::enable_shared_from_this<Connection>
  {
  public:
    enum Status
    {
      kConnecting,
      kConnected,
      kDisconnected
    };

  private:
    ExecutorThreadPtr executor_; //network sending thread, executor:socket=1:n
    TcpSocketPtr socket_;
    std::atomic<Status> status_;
    SteadyTimerPtr timer_;
    ProxyInfoPtr proxyinfo_;
    std::string local_info_; //local ip+port
    std::string remote_info_;

    std::deque<SendBuffer *> write_queue_; //waiting for sending 
    RecvBufferPtr recv_buf_;

    BinaryHB binHB_ = {0};               //binary hb
    char msgHB[5] = {0, 0, 0, 0x1, 0x1}; 

    std::vector<int32_t> loads_; // connection loads in last 30 times
    int32_t next_load_idx_;

    int32_t thread_id_;

    enum
    {
      kConnectTimeout = 1000 * 20, //ms //FIXME: improve, need fix?
    };

    AtomicUInt send_err_nums_; 
    AtomicUInt total_send_;    //total send buf
    AtomicUInt total_read_;    //total read ack package
    AtomicInt waiting_send_;
    AtomicInt retry_hb_; //hb retry times, reset while receiving ack

    mutable std::mutex load_mutex_; // loads lock

  public:
    Connection(ExecutorThreadPtr &executor, ProxyInfoPtr &proxyinfo);
    ~Connection();

    void sendBuf(SendBuffer* buf);
    void sendHB(bool isBinHB);
    void connClose();
    Status status() const { return status_; }
    inline bool isStop() const { return status_ == kDisconnected; }
    inline bool isConnected() const { return status_ == kConnected; }
    int32_t getThreadId() const { return thread_id_; }
    inline int32_t getWaitingSend() { return waiting_send_.get(); }
    inline void decreaseWaiting()
    {
      if (waiting_send_.get() > 0)
      {
        waiting_send_.decrement();
        executor_->waiting_send_.decrement();
      }
    }
    std::string getRemoteInfo() const { return remote_info_; }
    ProxyInfoPtr getBusInfo() const { return proxyinfo_; }

    int32_t getAvgLoad();

  private:
    void doConnect(const asio::ip::tcp::endpoint &ep);
    void doWrite(); //async send data
    void doRead();
    void connectHandler(const std::error_code &ec);
    void setLocalInfo();
    void doClose(const std::error_code *err = nullptr);
    int32_t doParse();                         
    bool parseProtocolAck(uint32_t total_len); 
    bool parseBinaryAck(uint32_t total_len);   
    bool parseBinaryHB(uint32_t total_len);
    uint32_t parseAttr(char *attr, int32_t attr_len);
  };

} // namespace dataproxy_sdk

#endif // DATAPROXY_SDK_NET_CONNECTION_H_