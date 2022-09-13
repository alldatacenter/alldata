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

#ifndef DATAPROXY_SDK_BASE_PACK_QUEUE_H_
#define DATAPROXY_SDK_BASE_PACK_QUEUE_H_

#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <unordered_map>

#include "atomic.h"
#include "buffer_pool.h"
#include "sdk_core.h"
#include "noncopyable.h"
#include "user_msg.h"
namespace dataproxy_sdk
{
// groupid:packqueue=1:1 
class PackQueue
{
  private:
    char* data_;                       
    std::vector<UserMsgPtr> msg_set_;  
    uint32_t data_capacity_;           // pack_size+extend_pack_size
    uint32_t cur_len_;                 //data+additional attr
    AtomicInt pack_redotime_cnt_;  //msg_cnt, report time is illegal
    AtomicInt pack_err_;           //pack err metrics
    uint64_t first_use_;           
    uint64_t last_use_;            
    uint64_t data_time_;           //data time
    std::string inlong_group_id_;
    std::string inlong_stream_id_;
    uint16_t groupId_num_;        
    uint16_t streamId_num_;      
    std::string topic_desc_;  // inlong_group_id=xxx&inlong_stream_id=xxx
    uint32_t msg_type_;
    mutable std::mutex mutex_;

  public:
    AtomicUInt success_num_;        //ack msg num in every one min
    AtomicUInt total_success_num_;  //total ack msg num

    AtomicUInt pack_num_;  //package num in every one min

  public:
    PackQueue(const std::string& inlong_group_id, const std::string& inlong_stream_id);
    ~PackQueue();

    int32_t sendMsg(const std::string& msg,
                    const std::string& inlong_group_id,
                    const std::string& inlong_stream_id,
                    const std::string& client_ip,
                    uint64_t report_time,
                    UserCallBack cb);
    bool packOperate(char* pack_data, uint32_t& out_len, uint32_t uniq_id);
    void checkQueue(bool isLastPack);  //if it's last pack, pack and send the rest data
    uint32_t curLen() const { return cur_len_; }
    void setCurLen(const uint32_t& cur_len) { cur_len_ = cur_len; }
    uint64_t firstUse() const { return first_use_; }
    void setFirstUse(const uint64_t& first_use) { first_use_ = first_use; }
    uint64_t lastUse() const { return last_use_; }
    void setLastUse(const uint64_t& last_use) { last_use_ = last_use; }
    uint64_t dataTime() const { return data_time_; }
    void setDataTime(const uint64_t& data_time) { data_time_ = data_time; }
    char* data() const { return data_; }

    std::string inlong_group_id() const { return inlong_group_id_; }
    void setGroupid(const std::string& inlong_group_id) { inlong_group_id_ = inlong_group_id; }

    void increasePackErr() { pack_err_.increment(); }

    std::string topicDesc() const { return topic_desc_; }

  private:
    bool isTriggerPack(uint64_t msg_time, int32_t msg_len);
    int32_t writeToBuf();
    int32_t appendMsg(const std::string& msg, std::string client_ip, int64_t report_time, UserCallBack call_back);
    bool isZipAndOperate(std::string& res, uint32_t real_cur_len);
    inline void resetPackQueue()
    {
        memset(data_, 0x0, data_capacity_);
        cur_len_ = 0;
        msg_set_.clear();
    }
};

using PackQueuePtr = std::shared_ptr<PackQueue>;

// all packqueues
class GlobalQueues : noncopyable
{
  private:
    std::unordered_map<std::string, PackQueuePtr> queues_;
    mutable std::mutex mutex_;
    std::thread worker_;  //pack thread
    bool exit_flag_;

  public:
    GlobalQueues() : exit_flag_(false){};
    virtual ~GlobalQueues();
    void startCheckSubroutine();
    virtual PackQueuePtr getPackQueue(const std::string& inlong_group_id, const std::string& inlong_stream_id);
    void closeCheckSubroutine() { exit_flag_ = true; }
    void printAck();
    void printTotalAck();

    void showState();

  private:
    void checkPackQueueSubroutine();
};

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_PACK_QUEUE_H_