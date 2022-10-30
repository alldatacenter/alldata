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

#ifndef TUBEMQ_CLIENT_META_INFO_H_
#define TUBEMQ_CLIENT_META_INFO_H_

#include <stdint.h>

#include <list>
#include <string>

#include "flowctrl_def.h"

namespace tubemq {

using std::list;
using std::map;
using std::string;

class NodeInfo {
 public:
  NodeInfo();
  NodeInfo(bool is_broker, const string& node_info);
  NodeInfo(const string& node_host, uint32_t node_port);
  NodeInfo(int32_t node_id, const string& node_host, uint32_t node_port);
  ~NodeInfo();
  NodeInfo& operator=(const NodeInfo& target);
  bool operator==(const NodeInfo& target);
  bool operator<(const NodeInfo& target) const;
  const uint32_t GetNodeId() const;
  const string& GetHost() const;
  const uint32_t GetPort() const;
  const string& GetAddrInfo() const;
  const string& GetNodeInfo() const;

 private:
  void buildStrInfo();

 private:
  uint32_t node_id_;
  string node_host_;
  uint32_t node_port_;
  // ip:port
  string addr_info_;
  // id:ip:port
  string node_info_;
};

class Partition {
 public:
  Partition();
  explicit Partition(const string& partition_info);
  Partition(const NodeInfo& broker_info, const string& part_str);
  Partition(const NodeInfo& broker_info, const string& topic, uint32_t partition_id);
  ~Partition();
  Partition& operator=(const Partition& target);
  bool operator==(const Partition& target);
  const uint32_t GetBrokerId() const;
  const string& GetBrokerHost() const;
  const uint32_t GetBrokerPort() const;
  const string& GetPartitionKey() const;
  const string& GetTopic() const;
  const NodeInfo& GetBrokerInfo() const;
  const uint32_t GetPartitionId() const;
  const string& ToString() const;

 private:
  void buildPartitionKey();

 private:
  string topic_;
  NodeInfo broker_info_;
  uint32_t partition_id_;
  string partition_key_;
  string partition_info_;
};

class PartitionExt : public Partition {
 public:
  PartitionExt();
  explicit PartitionExt(const string& partition_info);
  PartitionExt(const NodeInfo& broker_info, const string& part_str);
  ~PartitionExt();
  PartitionExt& operator=(const PartitionExt& target);
  void BookConsumeData(int32_t errcode, int32_t msg_size, bool req_esc_limit,
    int64_t rsp_dlt_limit, int64_t last_datadlt, bool require_slow);
  int64_t ProcConsumeResult(const FlowCtrlRuleHandler& def_flowctrl_handler,
    const FlowCtrlRuleHandler& group_flowctrl_handler, bool filter_consume, bool last_consumed);
  int64_t ProcConsumeResult(const FlowCtrlRuleHandler& def_flowctrl_handler,
    const FlowCtrlRuleHandler& group_flowctrl_handler, bool filter_consume, bool last_consumed,
    int32_t errcode, int32_t msg_size, bool req_esc_limit, int64_t rsp_dlt_limit,
    int64_t last_datadlt, bool require_slow);
  void SetLastConsumed(bool last_consumed);
  bool IsLastConsumed() const;

 private:
  void resetParameters();
  void updateStrategyData(const FlowCtrlRuleHandler& def_flowctrl_handler,
    const FlowCtrlRuleHandler& group_flowctrl_handler, int32_t msg_size, int64_t last_datadlt);

 private:
  bool is_last_consumed_;
  FlowCtrlResult cur_flowctrl_;
  FlowCtrlItem cur_freqctrl_;
  int64_t next_stage_updtime_;
  int64_t next_slice_updtime_;
  int64_t limit_slice_msgsize_;
  int64_t cur_stage_msgsize_;
  int64_t cur_slice_msgsize_;
  int32_t total_zero_cnt_;
  int64_t booked_time_;
  int32_t booked_errcode_;
  bool    booked_esc_limit_;
  int32_t booked_msgsize_;
  int64_t booked_dlt_limit_;
  int64_t booked_curdata_dlt_;
  bool    booked_require_slow_;
};

class SubscribeInfo {
 public:
  SubscribeInfo();
  explicit SubscribeInfo(const string& sub_info);
  SubscribeInfo(const string& consumer_id,
        const string& group_name, const PartitionExt& partition_ext);
  SubscribeInfo& operator=(const SubscribeInfo& target);
  const string& GetConsumerId() const;
  const string& GetGroup() const;
  const PartitionExt& GetPartitionExt() const;
  const uint32_t GgetBrokerId() const;
  const string& GetBrokerHost() const;
  const uint32_t GetBrokerPort() const;
  const string& GetTopic() const;
  const uint32_t GetPartitionId() const;
  const string& ToString() const;

 private:
  void buildSubInfo();

 private:
  string consumer_id_;
  string group_;
  PartitionExt partitionext_;
  string sub_info_;
};

class ConsumerEvent {
 public:
  ConsumerEvent();
  ConsumerEvent(const ConsumerEvent& target);
  ConsumerEvent(int64_t rebalance_id, int32_t event_type,
                const list<SubscribeInfo>& subscribeInfo_lst, int32_t event_status);
  ConsumerEvent& operator=(const ConsumerEvent& target);
  const int64_t GetRebalanceId() const;
  const int32_t GetEventType() const;
  const int32_t GetEventStatus() const;
  void SetEventType(int32_t event_type);
  void SetEventStatus(int32_t event_status);
  const list<SubscribeInfo>& GetSubscribeInfoList() const;
  string ToString();

 private:
  int64_t rebalance_id_;
  int32_t event_type_;
  int32_t event_status_;
  list<SubscribeInfo> subscribe_list_;
};


}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_META_INFO_H_
