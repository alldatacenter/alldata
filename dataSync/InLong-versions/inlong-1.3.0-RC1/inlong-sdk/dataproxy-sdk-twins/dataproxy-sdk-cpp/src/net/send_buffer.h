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

#ifndef DATAPROXY_SDK_NET_SEND_BUFFER_H_
#define DATAPROXY_SDK_NET_SEND_BUFFER_H_

#include <mutex>
#include <string>

#include "sdk_core.h"
// #include "executor_thread_pool.h"
#include "logger.h"
#include "noncopyable.h"
// #include "socket_connection.h"

namespace dataproxy_sdk
{
class SendBuffer : noncopyable
{
  private:
    uint32_t uniq_id_;                      
    bool is_used_;                          
    bool is_packed_;                        //is packed completed
    char* content_;                         //send data
    uint32_t size_;                         //buf_size
    int32_t msg_cnt_;                       
    uint32_t len_;                          //send data len
    std::string inlong_group_id_;                      
    std::string inlong_stream_id_;                      
    AtomicInt already_send_;                
    uint64_t first_send_time_;              //ms
    uint64_t latest_send_time_;             //ms
    ConnectionPtr target_;                  //send conn
    std::vector<UserMsgPtr> user_msg_set_;

  public:
    std::mutex mutex_;
    SteadyTimerPtr timeout_timer_;  //timeout, resend
    AtomicInt fail_create_conn_;    //create conn fail count

  public:
    SendBuffer(uint32_t size)
        : is_used_(false)
        , is_packed_(false)
        , mutex_()
        , msg_cnt_(0)
        , len_(0)
        , inlong_group_id_()
        , inlong_stream_id_()
        , first_send_time_(0)
        , latest_send_time_(0)
        , target_(nullptr)
        , uniq_id_(0)
        , timeout_timer_(nullptr)
        , size_(size)
    {
        content_ = new char[size];
        if (content_)
        {
            memset(content_, 0x0, size);
        }
    }
    ~SendBuffer()
    {
        if (content_) { delete[] content_; }
        content_ = nullptr;
    }

    char* content() { return content_; }
    int32_t msgCnt() const { return msg_cnt_; }
    void setMsgCnt(const int32_t& msg_cnt) { msg_cnt_ = msg_cnt; }
    uint32_t len() { return len_; }
    void setLen(const uint32_t len) { len_ = len; }
    std::string inlong_group_id() { return inlong_group_id_; }
    std::string inlong_stream_id() { return inlong_stream_id_; }
    void setGroupid(const std::string& inlong_group_id) { inlong_group_id_ = inlong_group_id; }
    void setStreamid(const std::string& inlong_stream_id) { inlong_stream_id_ = inlong_stream_id; }
    uint64_t firstSendTime() const { return first_send_time_; }
    void setFirstSendTime(const uint64_t& first_send_time) { first_send_time_ = first_send_time; }
    uint64_t latestSendTime() const { return latest_send_time_; }
    void setLatestSendTime(const uint64_t& latest_send_time) { latest_send_time_ = latest_send_time; }

    ConnectionPtr target() const { return target_; }
    void setTarget(ConnectionPtr& target) { target_ = target; }

    inline void increaseRetryNum() { already_send_.increment(); }
    inline int32_t getAlreadySend() { return already_send_.get(); }

    uint32_t uniqId() const { return uniq_id_; }
    void setUniqId(const uint32_t& uniq_id) { uniq_id_ = uniq_id; }

    void addUserMsg(UserMsgPtr u_msg) { user_msg_set_.push_back(u_msg); }
    void doUserCallBack()
    {
        LOG_TRACE("failed to send msg, start user call_back");
        for (auto it : user_msg_set_)
        {
            if (it->cb) { it->cb(inlong_group_id_.data(), inlong_stream_id_.data(), it->msg.data(), it->msg.size(), it->user_report_time, it->user_client_ip.data()); }
        }
    }

    void reset()
    {
        uint32_t record_uid = uniq_id_;  // for debug

        uniq_id_   = 0;
        is_used_   = false;
        is_packed_ = false;
        memset(content_, 0x0, size_);
        msg_cnt_ = 0;
        len_     = 0;
        inlong_group_id_     = "";
        inlong_stream_id_     = "";
        already_send_.getAndSet(0);
        first_send_time_  = 0;
        latest_send_time_ = 0;
        target_           = nullptr;
        if (timeout_timer_)
        {
            timeout_timer_->cancel();
            timeout_timer_ = nullptr;
        }
        user_msg_set_.clear();
        fail_create_conn_.getAndSet(0);
        LOG_TRACE("reset senfbuf(uid:%d) successfully", record_uid);
    }

    bool isPacked() const { return is_packed_; }
    void setIsPacked(bool is_packed) { is_packed_ = is_packed; }

    bool isUsed() const { return is_used_; }
    void setIsUsed(bool is_used) { is_used_ = is_used; }
};

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_NET_SEND_BUFFER_H_