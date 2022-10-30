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

#ifndef TUBEMQ_CLIENT_RMT_DATA_CACHE_H_
#define TUBEMQ_CLIENT_RMT_DATA_CACHE_H_

#include <stdint.h>

#include <condition_variable>
#include <list>
#include <map>
#include <mutex>
#include <set>
#include <string>
#include <tuple>

#include "executor_pool.h"
#include "flowctrl_def.h"
#include "meta_info.h"
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_errcode.h"





namespace tubemq {

using std::condition_variable;
using std::map;
using std::set;
using std::list;
using std::mutex;
using std::string;
using std::tuple;



// consumer remote data cache
class RmtDataCacheCsm {
 public:
  RmtDataCacheCsm();
  ~RmtDataCacheCsm();
  void SetConsumerInfo(const string& client_id, const string& group_name);
  void UpdateDefFlowCtrlInfo(int64_t flowctrl_id,
                                     const string& flowctrl_info);
  void UpdateGroupFlowCtrlInfo(int32_t qyrpriority_id,
                 int64_t flowctrl_id, const string& flowctrl_info);
  const int64_t GetGroupQryPriorityId() const;
  const int64_t GetDefFlowCtrlId() const { return def_flowctrl_handler_.GetFlowCtrlId(); }
  const int64_t GetGroupFlowCtrlId() const { return group_flowctrl_handler_.GetFlowCtrlId(); }
  bool IsUnderGroupCtrl();
  int32_t GetCurConsumeStatus();
  void handleExpiredPartitions(int64_t max_wait_period_ms);
  int32_t GetCurPartCount() const { return cur_part_cnt_.Get(); }
  bool IsPartitionInUse(string partition_key, int64_t used_time);
  void AddNewPartition(const PartitionExt& partition_ext);
  bool SelectPartition(int32_t& error_code, string &err_info,
           PartitionExt& partition_ext, string& confirm_context);
  void BookedPartionInfo(const string& partition_key, int64_t curr_offset);
  void BookedPartionInfo(const string& partition_key, int64_t curr_offset,
                            int32_t error_code, bool esc_limit, int32_t msg_size,
                            int64_t limit_dlt, int64_t cur_data_dlt, bool require_slow);
  bool RelPartition(string &err_info, bool filter_consume,
                         const string& confirm_context, bool is_consumed);
  bool RelPartition(string &err_info, const string& confirm_context, bool is_consumed);
  bool RelPartition(string &err_info, bool filter_consume,
                         const string& confirm_context, bool is_consumed,
                         int64_t curr_offset, int32_t error_code, bool esc_limit,
                         int32_t msg_size, int64_t limit_dlt, int64_t cur_data_dlt);
  void FilterPartitions(const list<SubscribeInfo>& subscribe_info_lst,
          list<PartitionExt>& subscribed_partitions, list<PartitionExt>& unsub_partitions);
  void GetSubscribedInfo(list<SubscribeInfo>& subscribe_info_lst);
  bool GetPartitionExt(const string& part_key, PartitionExt& partition_ext);
  void GetRegBrokers(list<NodeInfo>& brokers);
  void GetPartitionByBroker(const NodeInfo& broker_info,
                                    list<PartitionExt>& partition_list);
  void GetCurPartitionOffsets(map<string, int64_t>& part_offset_map);
  void GetAllClosedBrokerParts(map<NodeInfo, list<PartitionExt> >& broker_parts);
  void RemovePartition(const list<PartitionExt>& partition_list);
  void RemovePartition(const set<string>& partition_keys);
  bool RemovePartition(string &err_info, const string& confirm_context);
  void RemoveAndGetPartition(const list<SubscribeInfo>& subscribe_infos,
        bool is_processing_rollback, map<NodeInfo, list<PartitionExt> >& broker_parts);
  bool IsPartitionFirstReg(const string& partition_key);
  void OfferEvent(const ConsumerEvent& event);
  void TakeEvent(ConsumerEvent& event);
  void ClearEvent();
  void OfferEventResult(const ConsumerEvent& event);
  bool PollEventResult(ConsumerEvent& event);
  void HandleTimeout(const string partition_key, const asio::error_code& error);
  int IncrAndGetHBError(NodeInfo broker);
  void ResetHBError(NodeInfo broker);

 private:
  void addDelayTimer(const string& part_key, int64_t delay_time);
  void resetIdlePartition(const string& partition_key, bool need_reuse);
  void rmvMetaInfo(const string& partition_key);
  void buildConfirmContext(const string& partition_key,
                    int64_t booked_time, string& confirm_context);
  bool parseConfirmContext(string &err_info,
    const string& confirm_context, string& partition_key, int64_t& booked_time);
  bool inRelPartition(string &err_info, bool need_delay_check,
    bool filter_consume, const string& confirm_context, bool is_consumed);

 private:
  //
  string consumer_id_;
  string group_name_;
  // flow ctrl
  AtomicInteger cur_part_cnt_;
  FlowCtrlRuleHandler group_flowctrl_handler_;
  FlowCtrlRuleHandler def_flowctrl_handler_;
  AtomicBoolean under_groupctrl_;
  AtomicLong last_checktime_;
  // meta info
  mutable mutex meta_lock_;
  // partiton allocated map
  map<string, PartitionExt> partitions_;
  // topic partiton map
  map<string, set<string> > topic_partition_;
  // broker parition map
  map<NodeInfo, set<string> > broker_partition_;
  map<string, SubscribeInfo>  part_subinfo_;
  // for idle partitions occupy
  list<string> index_partitions_;
  // for partition used map
  map<string, int64_t> partition_useds_;
  // for partiton timer map
  map<string, tuple<int64_t, SteadyTimerPtr> > partition_timeouts_;
  // data book
  mutable mutex data_book_mutex_;
  // for partition offset cache
  map<string, int64_t> partition_offset_;
  // for partiton register booked
  map<string, bool> part_reg_booked_;
  // event
  mutable mutex event_read_mutex_;
  condition_variable event_read_cond_;
  list<ConsumerEvent> rebalance_events_;
  mutable mutex event_write_mutex_;
  list<ConsumerEvent> rebalance_results_;
  // status check
  mutable mutex status_mutex_;
  map<NodeInfo, int> broker_status_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_RMT_DATA_CACHE_H_
