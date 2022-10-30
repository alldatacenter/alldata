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

#include "meta_info.h"

#include <stdlib.h>

#include <sstream>
#include <vector>

#include "const_config.h"
#include "tubemq/tubemq_errcode.h"
#include "utils.h"

namespace tubemq {

using std::stringstream;
using std::vector;

NodeInfo::NodeInfo() {
  node_id_ = 0;
  node_host_ = " ";
  node_port_ = tb_config::kBrokerPortDef;
  buildStrInfo();
}

// node_info = node_id:host:port
NodeInfo::NodeInfo(bool is_broker, const string& node_info) {
  vector<string> result;
  Utils::Split(node_info, result, delimiter::kDelimiterColon);
  if (is_broker) {
    node_id_ = atoi(result[0].c_str());
    node_host_ = result[1];
    node_port_ = tb_config::kBrokerPortDef;
    if (result.size() >= 3) {
      node_port_ = atoi(result[2].c_str());
    }
  } else {
    node_id_ = 0;
    node_host_ = result[0];
    node_port_ = tb_config::kBrokerPortDef;
    if (result.size() >= 2) {
      node_port_ = atoi(result[1].c_str());
    }
  }
  buildStrInfo();
}

NodeInfo::NodeInfo(const string& node_host, uint32_t node_port) {
  node_id_ = tb_config::kInvalidValue;
  node_host_ = node_host;
  node_port_ = node_port;
  buildStrInfo();
}

NodeInfo::NodeInfo(int node_id, const string& node_host, uint32_t node_port) {
  node_id_ = node_id;
  node_host_ = node_host;
  node_port_ = node_port;
  buildStrInfo();
}

NodeInfo::~NodeInfo() {
  //
}

NodeInfo& NodeInfo::operator=(const NodeInfo& target) {
  if (this != &target) {
    node_id_ = target.node_id_;
    node_host_ = target.node_host_;
    node_port_ = target.node_port_;
    addr_info_ = target.addr_info_;
    node_info_ = target.node_info_;
    buildStrInfo();
  }
  return *this;
}

bool NodeInfo::operator==(const NodeInfo& target) {
  if (this == &target) {
    return true;
  }
  if (node_info_ == target.node_info_) {
    return true;
  }
  return false;
}

bool NodeInfo::operator<(const NodeInfo& target) const {
  return node_info_ < target.node_info_;
}
const uint32_t NodeInfo::GetNodeId() const { return node_id_; }

const string& NodeInfo::GetHost() const { return node_host_; }

const uint32_t NodeInfo::GetPort() const { return node_port_; }

const string& NodeInfo::GetAddrInfo() const { return addr_info_; }

const string& NodeInfo::GetNodeInfo() const { return node_info_; }

void NodeInfo::buildStrInfo() {
  stringstream ss1;
  ss1 << node_host_;
  ss1 << delimiter::kDelimiterColon;
  ss1 << node_port_;
  addr_info_ = ss1.str();

  stringstream ss2;
  ss2 << node_id_;
  ss2 << delimiter::kDelimiterColon;
  ss2 << addr_info_;
  node_info_ = ss2.str();
}

Partition::Partition() {
  topic_ = " ";
  partition_id_ = 0;
  buildPartitionKey();
}

// partition_info = broker_info#topic:partitionId
Partition::Partition(const string& partition_info) {
  // initial process
  topic_ = " ";
  partition_id_ = 0;
  // parse partition_info string
  string::size_type pos = 0;
  string seg_key = delimiter::kDelimiterPound;
  string token_key = delimiter::kDelimiterColon;
  // parse broker_info
  pos = partition_info.find(seg_key);
  if (pos != string::npos) {
    string broker_info = partition_info.substr(0, pos);
    broker_info = Utils::Trim(broker_info);
    NodeInfo tmp_node(true, broker_info);
    broker_info_ = tmp_node;
    string part_str = partition_info.substr(pos + seg_key.size(), partition_info.size());
    part_str = Utils::Trim(part_str);
    pos = part_str.find(token_key);
    if (pos != string::npos) {
      string topic_str = part_str.substr(0, pos);
      string part_id_str = part_str.substr(pos + token_key.size(), part_str.size());
      topic_str = Utils::Trim(topic_str);
      part_id_str = Utils::Trim(part_id_str);
      topic_ = topic_str;
      partition_id_ = atoi(part_id_str.c_str());
    }
  }
  buildPartitionKey();
}

// part_str = topic:partition_id
Partition::Partition(const NodeInfo& broker_info, const string& part_str) {
  vector<string> result;
  topic_ = " ";
  partition_id_ = 0;
  broker_info_ = broker_info;
  Utils::Split(part_str, result, delimiter::kDelimiterColon);
  if (result.size() >= 2) {
    topic_ = result[0];
    partition_id_ = atoi(result[1].c_str());
  }
  buildPartitionKey();
}

Partition::Partition(const NodeInfo& broker_info, const string& topic, uint32_t partition_id) {
  topic_ = topic;
  partition_id_ = partition_id;
  broker_info_ = broker_info;
  buildPartitionKey();
}

Partition::~Partition() {
  //
}

Partition& Partition::operator=(const Partition& target) {
  if (this != &target) {
    topic_ = target.topic_;
    partition_id_ = target.partition_id_;
    broker_info_ = target.broker_info_;
    partition_key_ = target.partition_key_;
    partition_info_ = target.partition_info_;
    buildPartitionKey();
  }
  return *this;
}

bool Partition::operator==(const Partition& target) {
  if (this == &target) {
    return true;
  }
  if (partition_info_ == target.partition_info_) {
    return true;
  }
  return false;
}

const uint32_t Partition::GetBrokerId() const { return broker_info_.GetNodeId(); }

const string& Partition::GetBrokerHost() const { return broker_info_.GetHost(); }

const uint32_t Partition::GetBrokerPort() const { return broker_info_.GetPort(); }

const string& Partition::GetPartitionKey() const { return partition_key_; }

const string& Partition::GetTopic() const { return topic_; }

const NodeInfo& Partition::GetBrokerInfo() const { return broker_info_; }

const uint32_t Partition::GetPartitionId() const { return partition_id_; }

const string& Partition::ToString() const { return partition_info_; }

void Partition::buildPartitionKey() {
  stringstream ss1;
  ss1 << broker_info_.GetNodeId();
  ss1 << delimiter::kDelimiterColon;
  ss1 << topic_;
  ss1 << delimiter::kDelimiterColon;
  ss1 << partition_id_;
  partition_key_ = ss1.str();

  stringstream ss2;
  ss2 << broker_info_.GetNodeInfo();
  ss2 << delimiter::kDelimiterPound;
  ss2 << topic_;
  ss2 << delimiter::kDelimiterColon;
  ss2 << partition_id_;
  partition_info_ = ss2.str();
}

PartitionExt::PartitionExt() : Partition() {
  resetParameters();
}

PartitionExt::PartitionExt(const string& partition_info) : Partition(partition_info) {
  resetParameters();
}

PartitionExt::PartitionExt(const NodeInfo& broker_info, const string& part_str)
  : Partition(broker_info, part_str) {
  resetParameters();
}

PartitionExt::~PartitionExt() {
  //
}

PartitionExt& PartitionExt::operator=(const PartitionExt& target) {
  if (this != &target) {
    // parent class
    Partition::operator=(target);
    // child class
    is_last_consumed_ = target.is_last_consumed_;
    cur_flowctrl_ = target.cur_flowctrl_;
    cur_freqctrl_ = target.cur_freqctrl_;
    next_stage_updtime_ = target.next_stage_updtime_;
    next_slice_updtime_ = target.next_slice_updtime_;
    limit_slice_msgsize_ = target.limit_slice_msgsize_;
    cur_stage_msgsize_ = target.cur_stage_msgsize_;
    cur_slice_msgsize_ = target.cur_slice_msgsize_;
    total_zero_cnt_ = target.total_zero_cnt_;
    booked_time_ = target.booked_time_;
    booked_errcode_ = target.booked_errcode_;
    booked_esc_limit_ = target.booked_esc_limit_;
    booked_msgsize_ = target.booked_msgsize_;
    booked_dlt_limit_ = target.booked_dlt_limit_;
    booked_curdata_dlt_ = target.booked_curdata_dlt_;
    booked_require_slow_ = target.booked_require_slow_;
    booked_errcode_ = target.booked_errcode_;
    booked_errcode_ = target.booked_errcode_;
  }
  return *this;
}


void PartitionExt::BookConsumeData(int32_t errcode, int32_t msg_size,
  bool req_esc_limit, int64_t rsp_dlt_limit, int64_t last_datadlt, bool require_slow) {
  booked_time_ = Utils::GetCurrentTimeMillis();
  booked_errcode_ = errcode;
  booked_esc_limit_ = req_esc_limit;
  booked_msgsize_ = msg_size;
  booked_dlt_limit_ = rsp_dlt_limit;
  booked_curdata_dlt_ = last_datadlt;
  booked_require_slow_ = require_slow;
}

int64_t PartitionExt::ProcConsumeResult(const FlowCtrlRuleHandler& def_flowctrl_handler,
  const FlowCtrlRuleHandler& group_flowctrl_handler, bool filter_consume, bool last_consumed) {
  int64_t dlt_time = Utils::GetCurrentTimeMillis() - booked_time_;
  return ProcConsumeResult(def_flowctrl_handler, group_flowctrl_handler, filter_consume,
    last_consumed, booked_errcode_, booked_msgsize_, booked_esc_limit_,
    booked_dlt_limit_, booked_curdata_dlt_, booked_require_slow_) - dlt_time;
}

int64_t PartitionExt::ProcConsumeResult(const FlowCtrlRuleHandler& def_flowctrl_handler,
  const FlowCtrlRuleHandler& group_flowctrl_handler, bool filter_consume, bool last_consumed,
  int32_t errcode, int32_t msg_size, bool req_esc_limit, int64_t rsp_dlt_limit,
  int64_t last_datadlt, bool require_slow) {
  // #lizard forgives
  // record consume status
  is_last_consumed_ = last_consumed;
  // Update strategy data values
  updateStrategyData(def_flowctrl_handler, group_flowctrl_handler, msg_size, last_datadlt);
  // Perform different strategies based on error codes
  switch (errcode) {
    case err_code::kErrNotFound:
    case err_code::kErrSuccess:
      if (msg_size == 0 && errcode != err_code::kErrSuccess) {
        total_zero_cnt_ += 1;
      } else {
        total_zero_cnt_ = 0;
      }
      if (total_zero_cnt_ > 0) {
        if (group_flowctrl_handler.GetMinZeroCnt() != tb_config::kMaxIntValue) {
          return (int64_t)(group_flowctrl_handler.GetCurFreqLimitTime(
            total_zero_cnt_, (int32_t)rsp_dlt_limit));
        } else {
          return (int64_t)def_flowctrl_handler.GetCurFreqLimitTime(
            total_zero_cnt_, (int32_t)rsp_dlt_limit);
        }
      }
      if (req_esc_limit) {
        return 0;
      } else {
        if (cur_stage_msgsize_ >= cur_flowctrl_.GetDataSizeLimit()
          || cur_slice_msgsize_ >= limit_slice_msgsize_) {
          return cur_flowctrl_.GetFreqMsLimit() > rsp_dlt_limit
            ? cur_flowctrl_.GetFreqMsLimit() : rsp_dlt_limit;
        }
        if (errcode == err_code::kErrSuccess) {
          if (filter_consume && cur_freqctrl_.GetFreqMsLimit() >= 0) {
            if (require_slow) {
              return cur_freqctrl_.GetZeroCnt();
            } else {
              return cur_freqctrl_.GetFreqMsLimit();
            }
          } else if (!filter_consume && cur_freqctrl_.GetDataSizeLimit() >=0) {
            return cur_freqctrl_.GetDataSizeLimit();
          }
        }
        return rsp_dlt_limit;
      }
      break;

    default:
      return rsp_dlt_limit;
  }
}

void PartitionExt::SetLastConsumed(bool last_consumed) {
  is_last_consumed_ = last_consumed;
}

bool PartitionExt::IsLastConsumed() const {
  return is_last_consumed_;
}

void PartitionExt::resetParameters() {
  is_last_consumed_ = false;
  cur_flowctrl_.SetDataDltAndFreqLimit(tb_config::kMaxLongValue, 20);
  next_stage_updtime_ = 0;
  next_slice_updtime_ = 0;
  limit_slice_msgsize_ = 0;
  cur_stage_msgsize_ = 0;
  cur_slice_msgsize_ = 0;
  total_zero_cnt_ = 0;
  booked_time_ = 0;
  booked_errcode_ = 0;
  booked_esc_limit_ = false;
  booked_msgsize_ = 0;
  booked_dlt_limit_ = 0;
  booked_curdata_dlt_ = 0;
  booked_require_slow_ = false;
}

void PartitionExt::updateStrategyData(const FlowCtrlRuleHandler& def_flowctrl_handler,
  const FlowCtrlRuleHandler& group_flowctrl_handler, int32_t msg_size, int64_t last_datadlt) {
  bool result = false;
  // Accumulated data received
  cur_stage_msgsize_ += msg_size;
  cur_slice_msgsize_ += msg_size;
  int64_t curr_time = Utils::GetCurrentTimeMillis();
  // Update strategy data values
  if (curr_time > next_stage_updtime_) {
    cur_stage_msgsize_ = 0;
    cur_slice_msgsize_ = 0;
    if (last_datadlt >= 0) {
      result = group_flowctrl_handler.GetCurDataLimit(last_datadlt, cur_flowctrl_);
      if (!result) {
        result = def_flowctrl_handler.GetCurDataLimit(last_datadlt, cur_flowctrl_);
        if (!result) {
          cur_flowctrl_.SetDataDltAndFreqLimit(tb_config::kMaxLongValue, 0);
        }
      }
      group_flowctrl_handler.GetFilterCtrlItem(cur_freqctrl_);
      if (cur_freqctrl_.GetFreqMsLimit() < 0) {
        def_flowctrl_handler.GetFilterCtrlItem(cur_freqctrl_);
      }
      curr_time = Utils::GetCurrentTimeMillis();
    }
    limit_slice_msgsize_ = cur_flowctrl_.GetDataSizeLimit() / 12;
    next_stage_updtime_ = curr_time + 60000;
    next_slice_updtime_ = curr_time + 5000;
  } else if (curr_time > next_slice_updtime_) {
    cur_slice_msgsize_ = 0;
    next_slice_updtime_ = curr_time + 5000;
  }
}

SubscribeInfo::SubscribeInfo() {
  consumer_id_ = " ";
  group_ = " ";
  buildSubInfo();
}

// sub_info = consumerId@group#broker_info#topic:partitionId
SubscribeInfo::SubscribeInfo(const string& sub_info) {
  string::size_type pos = 0;
  string seg_key = delimiter::kDelimiterPound;
  string at_key = delimiter::kDelimiterAt;
  consumer_id_ = " ";
  group_ = " ";
  // parse sub_info
  pos = sub_info.find(seg_key);
  if (pos != string::npos) {
    string consumer_info = sub_info.substr(0, pos);
    consumer_info = Utils::Trim(consumer_info);
    string partition_info = sub_info.substr(pos + seg_key.size(), sub_info.size());
    partition_info = Utils::Trim(partition_info);
    PartitionExt tmp_part(partition_info);
    partitionext_ = tmp_part;
    pos = consumer_info.find(at_key);
    consumer_id_ = consumer_info.substr(0, pos);
    consumer_id_ = Utils::Trim(consumer_id_);
    group_ = consumer_info.substr(pos + at_key.size(), consumer_info.size());
    group_ = Utils::Trim(group_);
  }
  buildSubInfo();
}

SubscribeInfo::SubscribeInfo(const string& consumer_id,
        const string& group_name, const PartitionExt& partition_ext) {
  consumer_id_ = consumer_id;
  group_ = group_name;
  partitionext_ = partition_ext;
  buildSubInfo();
}

SubscribeInfo& SubscribeInfo::operator=(const SubscribeInfo& target) {
  if (this != &target) {
    consumer_id_ = target.consumer_id_;
    group_ = target.group_;
    partitionext_ = target.partitionext_;
    buildSubInfo();
  }
  return *this;
}

const string& SubscribeInfo::GetConsumerId() const { return consumer_id_; }

const string& SubscribeInfo::GetGroup() const { return group_; }

const PartitionExt& SubscribeInfo::GetPartitionExt() const { return partitionext_; }

const uint32_t SubscribeInfo::GgetBrokerId() const { return partitionext_.GetBrokerId(); }

const string& SubscribeInfo::GetBrokerHost() const { return partitionext_.GetBrokerHost(); }

const uint32_t SubscribeInfo::GetBrokerPort() const { return partitionext_.GetBrokerPort(); }

const string& SubscribeInfo::GetTopic() const { return partitionext_.GetTopic(); }

const uint32_t SubscribeInfo::GetPartitionId() const {
  return partitionext_.GetPartitionId();
}

const string& SubscribeInfo::ToString() const { return sub_info_; }

void SubscribeInfo::buildSubInfo() {
  stringstream ss;
  ss << consumer_id_;
  ss << delimiter::kDelimiterAt;
  ss << group_;
  ss << delimiter::kDelimiterPound;
  ss << partitionext_.ToString();
  sub_info_ = ss.str();
}

ConsumerEvent::ConsumerEvent() {
  rebalance_id_ = tb_config::kInvalidValue;
  event_type_ = tb_config::kInvalidValue;
  event_status_ = tb_config::kInvalidValue;
}

ConsumerEvent::ConsumerEvent(const ConsumerEvent& target) {
  rebalance_id_ = target.rebalance_id_;
  event_type_ = target.event_type_;
  event_status_ = target.event_status_;
  subscribe_list_ = target.subscribe_list_;
}

ConsumerEvent::ConsumerEvent(int64_t rebalance_id, int32_t event_type,
                             const list<SubscribeInfo>& subscribeInfo_lst, int32_t event_status) {
  list<SubscribeInfo>::const_iterator it;
  rebalance_id_ = rebalance_id;
  event_type_ = event_type;
  event_status_ = event_status;
  for (it = subscribeInfo_lst.begin(); it != subscribeInfo_lst.end(); ++it) {
    subscribe_list_.push_back(*it);
  }
}

ConsumerEvent& ConsumerEvent::operator=(const ConsumerEvent& target) {
  if (this != &target) {
    rebalance_id_ = target.rebalance_id_;
    event_type_ = target.event_type_;
    event_status_ = target.event_status_;
    subscribe_list_ = target.subscribe_list_;
  }
  return *this;
}

const int64_t ConsumerEvent::GetRebalanceId() const { return rebalance_id_; }

const int32_t ConsumerEvent::GetEventType() const { return event_type_; }

const int32_t ConsumerEvent::GetEventStatus() const { return event_status_; }

void ConsumerEvent::SetEventType(int32_t event_type) { event_type_ = event_type; }

void ConsumerEvent::SetEventStatus(int32_t event_status) { event_status_ = event_status; }

const list<SubscribeInfo>& ConsumerEvent::GetSubscribeInfoList() const {
  return subscribe_list_;
}

string ConsumerEvent::ToString() {
  uint32_t count = 0;
  stringstream ss;
  list<SubscribeInfo>::const_iterator it;
  ss << "ConsumerEvent [rebalanceId=";
  ss << rebalance_id_;
  ss << ", type=";
  ss << event_type_;
  ss << ", status=";
  ss << event_status_;
  ss << ", subscribeInfoList=[";
  for (it = subscribe_list_.begin(); it != subscribe_list_.end(); ++it) {
    if (count++ > 0) {
      ss << ",";
    }
    ss << it->ToString();
  }
  ss << "]]";
  return ss.str();
}




};  // namespace tubemq
