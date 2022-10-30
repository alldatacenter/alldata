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

#ifndef DATAPROXY_SDK_NET_BUFFER_POOL_H_
#define DATAPROXY_SDK_NET_BUFFER_POOL_H_

#include <deque>
#include <mutex>
#include <system_error>
#include <thread>
#include <unordered_map>
#include <vector>

#include "sdk_core.h"
#include "noncopyable.h"
#include "pack_queue.h"
#include "recv_buffer.h"
#include "executor_thread_pool.h"

namespace dataproxy_sdk
{
  class SendBuffer;
  class BufferPool : noncopyable, public std::enable_shared_from_this<BufferPool>
  {
  private:
    uint32_t pool_id_;
    uint32_t buf_num_; //sendbuf count
    uint32_t read_, write_;
    int32_t current_use_;//sendbuf being used count                                           
    std::vector<SendBuffer *> buffers_; // allocate memory                               
    std::unordered_map<uint32_t, SendBuffer *> already_send_buf_list_; //key:uniq_id_, sent but not ack
    mutable std::mutex pool_mutex_;
    ExecutorThreadPtr executor_;

    AtomicInt send_total_;   //send buf num, including resend
    AtomicInt send_err_;     //fail send buf num, including resend
    AtomicInt resend_total_; //total resent package
    AtomicInt success_ack_;  //total ack msg

  public:
    // bufusage metrics
    AtomicUInt waiting_ack_;
    AtomicUInt has_ack_;
    AtomicUInt has_send_buf_; //sent to conn

  public:
    BufferPool(uint32_t pool_id, uint32_t buf_num, uint32_t buf_size);
    virtual ~BufferPool();

    bool isAvaliable(); //is bufpool avaliable
    virtual int32_t getSendBuf(SendBuffer *&send_buf);
    int32_t sendBufToConn(SendBuffer *&send_buf);
    
    void ackBufHelper(uint32_t uinq_id); // ack
    void ackBuf(uint32_t uniq_id) { executor_->postTask(std::bind(&BufferPool::ackBufHelper, shared_from_this(), uniq_id)); }

    uint32_t poolId() const { return pool_id_; }

    uint32_t writeId() const { return write_; }

    int32_t currentUse() const { return current_use_; }

    void showState(const std::string &inlong_group_id);

    void close();

  private:
    void RetryHandler(const std::error_code &ec, SendBuffer *buf);
  };

  using BufferPoolPtr = std::shared_ptr<BufferPool>;

  class TotalPools : noncopyable
  {
  private:
    // round-robin
    std::vector<BufferPoolPtr> pools_;
    int32_t next_;

    // groupid_isolation
    std::unordered_map<std::string, std::vector<BufferPoolPtr>> groupid2pool_map_;
    std::unordered_map<std::string, int32_t> groupid2next_;

    std::unordered_map<uint32_t, BufferPoolPtr> uid2buf_pool_;// <sent buf uid, bufpool>

    mutable std::mutex mutex_;

    void close(); // shutdown buffer thread
    void showStateHelper(const std::string &inlong_group_id, std::vector<BufferPoolPtr> &pools);

  public:
    TotalPools();
    virtual ~TotalPools() { close(); } // FIXME: need other operations?
    bool isPoolAvailable(const std::string &inlong_group_id);
    virtual BufferPoolPtr getPool(const std::string &inlong_group_id);
    void addUid2BufPool(uint32_t uniqId, BufferPoolPtr& bpr);
    BufferPoolPtr getUidBufPool(uint32_t uniqId);
    void showState();
  };

} // namespace dataproxy_sdk

#endif // DATAPROXY_SDK_NET_BUFFER_POOL_H_