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

#include "tubemq/tubemq_return.h"
#include "const_config.h"



namespace tubemq {


PeerInfo::PeerInfo() {
  broker_host_ = "";
  partition_key_ = "";
  partition_id_ = 0;
  curr_offset_ = tb_config::kInvalidValue;
}


PeerInfo::PeerInfo(const string& broker_host, uint32_t partition_id,
  const string& partiton_key, int64_t offset) {
  broker_host_ = broker_host;
  partition_id_ = partition_id;
  partition_key_ = partiton_key;
  curr_offset_ = offset;
}

PeerInfo& PeerInfo::operator=(const PeerInfo& target) {
  if (this != &target) {
    partition_id_ = target.partition_id_;
    broker_host_ = target.broker_host_;
    partition_key_ = target.partition_key_;
    curr_offset_ = target.curr_offset_;
  }
  return *this;
}

ConsumeOffsetInfo::ConsumeOffsetInfo() {
  partition_key_ = "";
  curr_offset_ = tb_config::kInvalidValue;
}

ConsumeOffsetInfo::ConsumeOffsetInfo(
  const string& part_key, int64_t curr_offset) {
  partition_key_ = part_key;
  curr_offset_ = curr_offset;
}

void ConsumeOffsetInfo::SetConsumeOffsetInfo(
  const string& part_key, int64_t curr_offset) {
  partition_key_ = part_key;
  curr_offset_ = curr_offset;
}

ConsumeOffsetInfo& ConsumeOffsetInfo::operator=(
  const ConsumeOffsetInfo& target) {
  if (this != &target) {
    partition_key_ = target.partition_key_;
    curr_offset_ = target.curr_offset_;
  }
  return *this;
}


ConsumerResult::ConsumerResult() {
  success_ = false;
  err_code_ = tb_config::kInvalidValue;
  err_msg_ = "";
  topic_name_ = "";
  confirm_context_ = "";
}

ConsumerResult::ConsumerResult(const ConsumerResult& target) {
  success_ = target.success_;
  err_code_ = target.err_code_;
  err_msg_ = target.err_msg_;
  topic_name_ = target.topic_name_;
  peer_info_ = target.peer_info_;
  confirm_context_ = target.confirm_context_;
  message_list_ = target.message_list_;
}

ConsumerResult::ConsumerResult(int32_t error_code, string err_msg) {
  success_ = false;
  err_code_ = error_code;
  err_msg_ = err_msg;
  topic_name_ = "";
  confirm_context_ = "";
}

ConsumerResult::~ConsumerResult() {
  message_list_.clear();
  success_ = false;
  err_code_ = tb_config::kInvalidValue;
  err_msg_ = "";
  topic_name_ = "";
  confirm_context_ = "";
}

ConsumerResult& ConsumerResult::operator=(const ConsumerResult& target) {
  if (this != &target) {
    success_ = target.success_;
    err_code_ = target.err_code_;
    err_msg_ = target.err_msg_;
    topic_name_ = target.topic_name_;
    peer_info_ = target.peer_info_;
    confirm_context_ = target.confirm_context_;
    message_list_ = target.message_list_;
  }
  return *this;
}

void ConsumerResult::SetFailureResult(int32_t error_code, string err_msg) {
  success_ = false;
  err_code_ = error_code;
  err_msg_ = err_msg;
}

void ConsumerResult::SetFailureResult(int32_t error_code, string err_msg,
                            const string& topic_name, const PeerInfo& peer_info) {
  success_ = false;
  err_code_ = error_code;
  err_msg_ = err_msg;
  topic_name_ = topic_name;
  peer_info_ = peer_info;
}

void ConsumerResult::SetSuccessResult(int32_t error_code,
                                             const string& topic_name,
                                             const PeerInfo& peer_info) {
  success_ = true;
  err_code_ = error_code;
  err_msg_ = "Ok";
  topic_name_ = topic_name;
  peer_info_ = peer_info;
  confirm_context_ = "";
  message_list_.clear();
}

void ConsumerResult::SetSuccessResult(int32_t error_code,
                                             const string& topic_name,
                                             const PeerInfo& peer_info,
                                             const string& confirm_context,
                                             const list<Message>& message_list) {
  success_ = true;
  err_code_ = error_code;
  err_msg_ = "Ok";
  topic_name_ = topic_name;
  peer_info_ = peer_info;
  confirm_context_ = confirm_context;
  message_list_ = message_list;
}

const string& ConsumerResult::GetPartitionKey() const {
  return peer_info_.GetPartitionKey();
}

const int64_t ConsumerResult::GetCurrOffset() const {
  return peer_info_.GetCurrOffset();
}


}  // namespace tubemq

