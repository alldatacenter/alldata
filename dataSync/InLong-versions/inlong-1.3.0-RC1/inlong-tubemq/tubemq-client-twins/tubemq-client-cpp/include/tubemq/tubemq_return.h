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

#ifndef TUBEMQ_CLIENT_RETURN_H_
#define TUBEMQ_CLIENT_RETURN_H_

#include <stdlib.h>

#include <list>
#include <string>

#include "tubemq/tubemq_message.h"




namespace tubemq {

using std::list;
using std::string;



class PeerInfo {
 public:
  PeerInfo();
  PeerInfo(const string& broker_host, uint32_t partition_id,
    const string& partiton_key, int64_t offset);
  PeerInfo& operator=(const PeerInfo& target);
  const uint32_t GetPartitionId() const { return partition_id_; }
  const string& GetBrokerHost() const { return broker_host_; }
  const string& GetPartitionKey() const { return partition_key_; }
  const int64_t GetCurrOffset() const { return curr_offset_; }
  void SetCurrOffset(int64_t offset) { curr_offset_ = offset; }

 private:
  uint32_t partition_id_;
  string broker_host_;
  string partition_key_;
  int64_t curr_offset_;
};


class ConsumeOffsetInfo {
 public:
  ConsumeOffsetInfo();
  ConsumeOffsetInfo(const string& part_key, int64_t curr_offset);
  void SetConsumeOffsetInfo(const string& part_key, int64_t curr_offset);
  ConsumeOffsetInfo& operator=(const ConsumeOffsetInfo& target);
  const string& GetPartitonKey() const { return partition_key_; }
  const int64_t& GetCurrOffset() const { return curr_offset_; }

 private:
  string partition_key_;
  int64_t curr_offset_;
};


class ConsumerResult {
 public:
  ConsumerResult();
  ConsumerResult(const ConsumerResult& target);
  ConsumerResult(int32_t error_code, string err_msg);
  ~ConsumerResult();
  ConsumerResult& operator=(const ConsumerResult& target);
  void SetFailureResult(int32_t error_code, string err_msg);
  void SetFailureResult(int32_t error_code, string err_msg,
    const string& topic_name, const PeerInfo& peer_info);
  void SetSuccessResult(int32_t error_code,
    const string& topic_name, const PeerInfo& peer_info);
  void SetSuccessResult(int32_t error_code, const string& topic_name,
                  const PeerInfo& peer_info, const string& confirm_context,
                  const list<Message>& message_list);
  bool IsSuccess() { return success_; }
  const int32_t  GetErrCode() const { return err_code_; }
  const string& GetErrMessage() const { return err_msg_; }
  const string& GetTopicName() const { return topic_name_; }
  const PeerInfo& GetPeerInfo() const { return peer_info_; }
  const string& GetConfirmContext() const { return confirm_context_; }
  const list<Message>& GetMessageList() const { return message_list_; }
  const string& GetPartitionKey() const;
  const int64_t GetCurrOffset() const;

 private:
  bool success_;
  int32_t  err_code_;
  string err_msg_;
  string topic_name_;
  PeerInfo peer_info_;
  string confirm_context_;
  list<Message> message_list_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_RETURN_H_

