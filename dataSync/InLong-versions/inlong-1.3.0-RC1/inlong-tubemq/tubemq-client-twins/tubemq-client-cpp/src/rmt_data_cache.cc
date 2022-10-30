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

#include "rmt_data_cache.h"

#include <stdlib.h>

#include <string>

#include "client_service.h"
#include "const_config.h"
#include "logger.h"
#include "meta_info.h"
#include "utils.h"

namespace tubemq {

using std::lock_guard;
using std::unique_lock;

RmtDataCacheCsm::RmtDataCacheCsm() {
  under_groupctrl_.Set(false);
  last_checktime_.Set(0);
  cur_part_cnt_.Set(0);
}

RmtDataCacheCsm::~RmtDataCacheCsm() {
  //
}

void RmtDataCacheCsm::SetConsumerInfo(const string& client_id, const string& group_name) {
  consumer_id_ = client_id;
  group_name_ = group_name;
}

void RmtDataCacheCsm::UpdateDefFlowCtrlInfo(int64_t flowctrl_id, const string& flowctrl_info) {
  if (flowctrl_id != def_flowctrl_handler_.GetFlowCtrlId()) {
    def_flowctrl_handler_.UpdateDefFlowCtrlInfo(true, tb_config::kInvalidValue, flowctrl_id,
                                                flowctrl_info);
  }
}
void RmtDataCacheCsm::UpdateGroupFlowCtrlInfo(int32_t qyrpriority_id, int64_t flowctrl_id,
                                              const string& flowctrl_info) {
  if (flowctrl_id != group_flowctrl_handler_.GetFlowCtrlId()) {
    group_flowctrl_handler_.UpdateDefFlowCtrlInfo(false, qyrpriority_id, flowctrl_id,
                                                  flowctrl_info);
  }
  if (qyrpriority_id != group_flowctrl_handler_.GetQryPriorityId()) {
    group_flowctrl_handler_.SetQryPriorityId(qyrpriority_id);
  }
  // update current if under group flowctrl
  int64_t cur_time = Utils::GetCurrentTimeMillis();
  if (cur_time - last_checktime_.Get() > 10000) {
    FlowCtrlResult flowctrl_result;
    under_groupctrl_.Set(
        group_flowctrl_handler_.GetCurDataLimit(tb_config::kMaxLongValue, flowctrl_result));
    last_checktime_.Set(cur_time);
  }
}

const int64_t RmtDataCacheCsm::GetGroupQryPriorityId() const {
  return group_flowctrl_handler_.GetQryPriorityId();
}

bool RmtDataCacheCsm::IsUnderGroupCtrl() { return under_groupctrl_.Get(); }

void RmtDataCacheCsm::AddNewPartition(const PartitionExt& partition_ext) {
  //
  map<string, PartitionExt>::iterator it_map;
  map<string, set<string> >::iterator it_topic;
  map<NodeInfo, set<string> >::iterator it_broker;
  //
  SubscribeInfo sub_info(consumer_id_, group_name_, partition_ext);
  string partition_key = partition_ext.GetPartitionKey();
  // lock operate
  lock_guard<mutex> lck(meta_lock_);
  it_map = partitions_.find(partition_key);
  if (it_map == partitions_.end()) {
    cur_part_cnt_.GetAndIncrement();
    partitions_[partition_key] = partition_ext;
    it_topic = topic_partition_.find(partition_ext.GetTopic());
    if (it_topic == topic_partition_.end()) {
      set<string> tmp_part_set;
      tmp_part_set.insert(partition_key);
      topic_partition_[partition_ext.GetTopic()] = tmp_part_set;
    } else {
      if (it_topic->second.find(partition_key) == it_topic->second.end()) {
        it_topic->second.insert(partition_key);
      }
    }
    it_broker = broker_partition_.find(partition_ext.GetBrokerInfo());
    if (it_broker == broker_partition_.end()) {
      set<string> tmp_part_set;
      tmp_part_set.insert(partition_key);
      broker_partition_[partition_ext.GetBrokerInfo()] = tmp_part_set;
    } else {
      if (it_broker->second.find(partition_key) == it_broker->second.end()) {
        it_broker->second.insert(partition_key);
      }
    }
    part_subinfo_[partition_key] = sub_info;
  }
  // check partition_key status
  resetIdlePartition(partition_key, true);
}

int32_t RmtDataCacheCsm::GetCurConsumeStatus() {
  lock_guard<mutex> lck(meta_lock_);
  if (partitions_.empty()) {
    return err_code::kErrNoPartAssigned;
  }
  if (index_partitions_.empty()) {
    if (partition_useds_.empty()) {
      return err_code::kErrAllPartWaiting;
    } else {
      return err_code::kErrAllPartInUse;
    }
  }
  return err_code::kErrSuccess;
}

bool RmtDataCacheCsm::SelectPartition(int32_t& error_code, string& err_info,
                                      PartitionExt& partition_ext, string& confirm_context) {
  bool result = false;
  int64_t booked_time = 0;
  string partition_key;
  map<string, PartitionExt>::iterator it_map;
  // lock operate
  lock_guard<mutex> lck(meta_lock_);
  if (partitions_.empty()) {
    error_code = err_code::kErrNoPartAssigned;
    err_info = "No partition info in local cache, please retry later!";
    result = false;
  } else {
    if (index_partitions_.empty()) {
      if (partition_useds_.empty()) {
        error_code = err_code::kErrAllPartWaiting;
        err_info = "All partitions reach max position, please retry later!";
      } else {
        error_code = err_code::kErrAllPartInUse;
        err_info = "No idle partition to consume, please retry later!";
      }
      result = false;
    } else {
      result = false;
      error_code = err_code::kErrAllPartInUse;
      err_info = "No idle partition to consume data 2, please retry later!";
      booked_time = Utils::GetCurrentTimeMillis();
      partition_key = index_partitions_.front();
      index_partitions_.pop_front();
      buildConfirmContext(partition_key, booked_time, confirm_context);
      it_map = partitions_.find(partition_key);
      if (it_map != partitions_.end()) {
        partition_ext = it_map->second;
        partition_useds_[partition_key] = booked_time;
        result = true;
        err_info = "Ok";
      }
    }
  }
  return result;
}

void RmtDataCacheCsm::BookedPartionInfo(const string& partition_key, int64_t curr_offset) {
  // book partition offset info
  if (curr_offset >= 0) {
    lock_guard<mutex> lck1(data_book_mutex_);
    partition_offset_[partition_key] = curr_offset;
  }
}

void RmtDataCacheCsm::BookedPartionInfo(const string& partition_key, int64_t curr_offset,
                                        int32_t error_code, bool esc_limit, int32_t msg_size,
                                        int64_t limit_dlt, int64_t cur_data_dlt,
                                        bool require_slow) {
  map<string, PartitionExt>::iterator it_part;
  // book partition offset info
  BookedPartionInfo(partition_key, curr_offset);
  // book partition temp info
  lock_guard<mutex> lck2(meta_lock_);
  it_part = partitions_.find(partition_key);
  if (it_part != partitions_.end()) {
    it_part->second.BookConsumeData(error_code, msg_size, esc_limit, limit_dlt, cur_data_dlt,
                                    require_slow);
  }
}

bool RmtDataCacheCsm::IsPartitionInUse(string partition_key, int64_t used_time) {
  map<string, int64_t>::iterator it_used;
  lock_guard<mutex> lck(meta_lock_);
  it_used = partition_useds_.find(partition_key);
  if (it_used == partition_useds_.end() || it_used->second != used_time) {
    return false;
  }
  return true;
}

// success process release partition
bool RmtDataCacheCsm::RelPartition(string& err_info, bool filter_consume,
                                   const string& confirm_context, bool is_consumed) {
  return inRelPartition(err_info, true, filter_consume, confirm_context, is_consumed);
}

// release partiton without response return
bool RmtDataCacheCsm::RelPartition(string& err_info, const string& confirm_context,
                                   bool is_consumed) {
  return inRelPartition(err_info, true, false, confirm_context, is_consumed);
}

// release partiton with error response return
bool RmtDataCacheCsm::RelPartition(string& err_info, bool filter_consume,
                                   const string& confirm_context, bool is_consumed,
                                   int64_t curr_offset, int32_t error_code, bool esc_limit,
                                   int32_t msg_size, int64_t limit_dlt, int64_t cur_data_dlt) {
  int64_t booked_time;
  string partition_key;
  // parse confirm context
  bool result = parseConfirmContext(err_info, confirm_context, partition_key, booked_time);
  if (!result) {
    return false;
  }
  BookedPartionInfo(partition_key, curr_offset, error_code, esc_limit, msg_size, limit_dlt,
                    cur_data_dlt, false);
  return inRelPartition(err_info, true, filter_consume, confirm_context, is_consumed);
}

void RmtDataCacheCsm::FilterPartitions(const list<SubscribeInfo>& subscribe_info_lst,
                                       list<PartitionExt>& subscribed_partitions,
                                       list<PartitionExt>& unsub_partitions) {
  //
  map<string, PartitionExt>::iterator it_part;
  list<SubscribeInfo>::const_iterator it_lst;
  // initial return;
  subscribed_partitions.clear();
  unsub_partitions.clear();
  lock_guard<mutex> lck(meta_lock_);
  if (partitions_.empty()) {
    for (it_lst = subscribe_info_lst.begin(); it_lst != subscribe_info_lst.end(); it_lst++) {
      unsub_partitions.push_back(it_lst->GetPartitionExt());
    }
  } else {
    for (it_lst = subscribe_info_lst.begin(); it_lst != subscribe_info_lst.end(); it_lst++) {
      it_part = partitions_.find(it_lst->GetPartitionExt().GetPartitionKey());
      if (it_part == partitions_.end()) {
        unsub_partitions.push_back(it_lst->GetPartitionExt());
      } else {
        subscribed_partitions.push_back(it_lst->GetPartitionExt());
      }
    }
  }
}

void RmtDataCacheCsm::GetSubscribedInfo(list<SubscribeInfo>& subscribe_info_lst) {
  map<string, SubscribeInfo>::iterator it_sub;
  subscribe_info_lst.clear();
  lock_guard<mutex> lck(meta_lock_);
  for (it_sub = part_subinfo_.begin(); it_sub != part_subinfo_.end(); ++it_sub) {
    subscribe_info_lst.push_back(it_sub->second);
  }
}

void RmtDataCacheCsm::GetAllClosedBrokerParts(map<NodeInfo, list<PartitionExt> >& broker_parts) {
  map<string, PartitionExt>::iterator it_part;
  map<NodeInfo, list<PartitionExt> >::iterator it_broker;

  broker_parts.clear();
  lock_guard<mutex> lck(meta_lock_);
  for (it_part = partitions_.begin(); it_part != partitions_.end(); ++it_part) {
    it_broker = broker_parts.find(it_part->second.GetBrokerInfo());
    if (it_broker == broker_parts.end()) {
      list<PartitionExt> tmp_part_lst;
      tmp_part_lst.push_back(it_part->second);
      broker_parts[it_part->second.GetBrokerInfo()] = tmp_part_lst;
    } else {
      it_broker->second.push_back(it_part->second);
    }
  }
}

bool RmtDataCacheCsm::GetPartitionExt(const string& part_key, PartitionExt& partition_ext) {
  bool result = false;
  map<string, PartitionExt>::iterator it_map;

  lock_guard<mutex> lck(meta_lock_);
  it_map = partitions_.find(part_key);
  if (it_map != partitions_.end()) {
    result = true;
    partition_ext = it_map->second;
  }
  return result;
}

void RmtDataCacheCsm::GetRegBrokers(list<NodeInfo>& brokers) {
  map<NodeInfo, set<string> >::iterator it;

  brokers.clear();
  lock_guard<mutex> lck(meta_lock_);
  for (it = broker_partition_.begin(); it != broker_partition_.end(); ++it) {
    brokers.push_back(it->first);
  }
}

void RmtDataCacheCsm::GetPartitionByBroker(const NodeInfo& broker_info,
                                           list<PartitionExt>& partition_list) {
  set<string>::iterator it_key;
  map<NodeInfo, set<string> >::iterator it_broker;
  map<string, PartitionExt>::iterator it_part;

  partition_list.clear();
  lock_guard<mutex> lck(meta_lock_);
  it_broker = broker_partition_.find(broker_info);
  if (it_broker != broker_partition_.end()) {
    for (it_key = it_broker->second.begin(); it_key != it_broker->second.end(); it_key++) {
      it_part = partitions_.find(*it_key);
      if (it_part != partitions_.end()) {
        partition_list.push_back(it_part->second);
      }
    }
  }
}

void RmtDataCacheCsm::GetCurPartitionOffsets(map<string, int64_t>& part_offset_map) {
  part_offset_map.clear();
  lock_guard<mutex> lck(data_book_mutex_);
  part_offset_map = partition_offset_;
}

//
bool RmtDataCacheCsm::RemovePartition(string& err_info, const string& confirm_context) {
  int64_t booked_time;
  string partition_key;
  set<string> partition_keys;
  // parse confirm context
  bool result = parseConfirmContext(err_info, confirm_context, partition_key, booked_time);
  if (!result) {
    return false;
  }
  // remove partiton
  partition_keys.insert(partition_key);
  RemovePartition(partition_keys);
  err_info = "Ok";
  return true;
}

void RmtDataCacheCsm::RemovePartition(const list<PartitionExt>& partition_list) {
  set<string> partition_keys;
  list<PartitionExt>::const_iterator it_lst;
  for (it_lst = partition_list.begin(); it_lst != partition_list.end(); it_lst++) {
    partition_keys.insert(it_lst->GetPartitionKey());
  }
  if (!partition_keys.empty()) {
    RemovePartition(partition_keys);
  }
}

void RmtDataCacheCsm::RemovePartition(const set<string>& partition_keys) {
  set<string>::const_iterator it_lst;

  lock_guard<mutex> lck(meta_lock_);
  for (it_lst = partition_keys.begin(); it_lst != partition_keys.end(); it_lst++) {
    resetIdlePartition(*it_lst, false);
    // remove meta info set info
    rmvMetaInfo(*it_lst);
  }
}

void RmtDataCacheCsm::RemoveAndGetPartition(const list<SubscribeInfo>& subscribe_infos,
                                            bool is_processing_rollback,
                                            map<NodeInfo, list<PartitionExt> >& broker_parts) {
  //
  string part_key;
  list<SubscribeInfo>::const_iterator it;
  map<string, PartitionExt>::iterator it_part;
  map<NodeInfo, list<PartitionExt> >::iterator it_broker;

  broker_parts.clear();
  // check if empty
  if (subscribe_infos.empty()) {
    return;
  }
  lock_guard<mutex> lck(meta_lock_);
  for (it = subscribe_infos.begin(); it != subscribe_infos.end(); ++it) {
    part_key = it->GetPartitionExt().GetPartitionKey();
    it_part = partitions_.find(part_key);
    if (it_part != partitions_.end()) {
      if (partition_useds_.find(part_key) != partition_useds_.end()) {
        if (is_processing_rollback) {
          it_part->second.SetLastConsumed(false);
        } else {
          it_part->second.SetLastConsumed(true);
        }
      }
      it_broker = broker_parts.find(it_part->second.GetBrokerInfo());
      if (it_broker == broker_parts.end()) {
        list<PartitionExt> tmp_part_list;
        tmp_part_list.push_back(it_part->second);
        broker_parts[it_part->second.GetBrokerInfo()] = tmp_part_list;
      } else {
        it_broker->second.push_back(it_part->second);
      }
      rmvMetaInfo(part_key);
    }
    resetIdlePartition(part_key, false);
  }
}

void RmtDataCacheCsm::handleExpiredPartitions(int64_t max_wait_period_ms) {
  int64_t curr_time;
  set<string> expired_keys;
  set<string>::iterator it_lst;
  map<string, int64_t>::iterator it_used;
  map<string, PartitionExt>::iterator it_map;

  lock_guard<mutex> lck(meta_lock_);
  if (!partition_useds_.empty()) {
    curr_time = Utils::GetCurrentTimeMillis();
    for (it_used = partition_useds_.begin();
      it_used != partition_useds_.end(); ++it_used) {
      if (curr_time - it_used->second > max_wait_period_ms) {
        expired_keys.insert(it_used->first);
        it_map = partitions_.find(it_used->first);
        if (it_map != partitions_.end()) {
          it_map->second.SetLastConsumed(false);
        }
      }
    }
    if (!expired_keys.empty()) {
      for (it_lst = expired_keys.begin();
        it_lst != expired_keys.end(); it_lst++) {
        resetIdlePartition(*it_lst, true);
      }
    }
  }
}



bool RmtDataCacheCsm::IsPartitionFirstReg(const string& partition_key) {
  map<string, bool>::iterator it;
  lock_guard<mutex> lck(data_book_mutex_);
  it = part_reg_booked_.find(partition_key);
  if (it == part_reg_booked_.end()) {
    part_reg_booked_[partition_key] = false;
    return true;
  }
  return part_reg_booked_[partition_key];
}

void RmtDataCacheCsm::OfferEvent(const ConsumerEvent& event) {
  unique_lock<mutex> lck(event_read_mutex_);
  rebalance_events_.push_back(event);
  event_read_cond_.notify_all();
}

void RmtDataCacheCsm::TakeEvent(ConsumerEvent& event) {
  unique_lock<mutex> lck(event_read_mutex_);
  while (rebalance_events_.empty()) {
    event_read_cond_.wait(lck);
  }
  event = rebalance_events_.front();
  rebalance_events_.pop_front();
}

void RmtDataCacheCsm::ClearEvent() {
  unique_lock<mutex> lck(event_read_mutex_);
  rebalance_events_.clear();
}

void RmtDataCacheCsm::OfferEventResult(const ConsumerEvent& event) {
  lock_guard<mutex> lck(event_write_mutex_);
  rebalance_results_.push_back(event);
}

bool RmtDataCacheCsm::PollEventResult(ConsumerEvent& event) {
  bool result = false;
  lock_guard<mutex> lck(event_write_mutex_);
  if (!rebalance_results_.empty()) {
    event = rebalance_results_.front();
    rebalance_results_.pop_front();
    result = true;
  }
  return result;
}

void RmtDataCacheCsm::HandleTimeout(const string partition_key, const asio::error_code& error) {
  if (!error) {
    lock_guard<mutex> lck(meta_lock_);
    resetIdlePartition(partition_key, true);
  }
}

int RmtDataCacheCsm::IncrAndGetHBError(NodeInfo broker) {
  int count = 0;
  map<NodeInfo, int>::iterator it_map;
  lock_guard<mutex> lck(status_mutex_);
  it_map = broker_status_.find(broker);
  if (it_map == broker_status_.end()) {
    broker_status_[broker] = 1;
    count = 1;
  } else {
    count = ++it_map->second;
  }
  return count;
}

void RmtDataCacheCsm::ResetHBError(NodeInfo broker) {
  map<NodeInfo, int>::iterator it_map;
  lock_guard<mutex> lck(status_mutex_);
  it_map = broker_status_.find(broker);
  if (it_map != broker_status_.end()) {
    it_map->second = 0;
  }
}


void RmtDataCacheCsm::addDelayTimer(const string& partition_key, int64_t delay_time) {
  // add timer
  tuple<int64_t, SteadyTimerPtr> timer = std::make_tuple(
      Utils::GetCurrentTimeMillis(),
      TubeMQService::Instance()->CreateTimer());
  std::get<1>(timer)->expires_after(std::chrono::milliseconds(delay_time));
  std::get<1>(timer)->async_wait(
      std::bind(&RmtDataCacheCsm::HandleTimeout, this, partition_key, std::placeholders::_1));
  partition_timeouts_.insert(std::make_pair(partition_key, timer));
}

void RmtDataCacheCsm::resetIdlePartition(const string& partition_key, bool need_reuse) {
  map<string, PartitionExt>::iterator it_map;
  map<string, tuple<int64_t, SteadyTimerPtr> >::iterator it_timeout;
  partition_useds_.erase(partition_key);
  it_timeout = partition_timeouts_.find(partition_key);
  if (it_timeout != partition_timeouts_.end()) {
    std::get<1>(it_timeout->second)->cancel();
    partition_timeouts_.erase(partition_key);
  }
  index_partitions_.remove(partition_key);
  if (need_reuse) {
    if (partitions_.find(partition_key) != partitions_.end()) {
      index_partitions_.push_back(partition_key);
    }
  }
}

void RmtDataCacheCsm::buildConfirmContext(const string& partition_key, int64_t booked_time,
                                          string& confirm_context) {
  confirm_context.clear();
  confirm_context += partition_key;
  confirm_context += delimiter::kDelimiterAt;
  confirm_context += Utils::Long2str(booked_time);
}

bool RmtDataCacheCsm::parseConfirmContext(string& err_info, const string& confirm_context,
                                          string& partition_key, int64_t& booked_time) {
  //
  vector<string> result;
  Utils::Split(confirm_context, result, delimiter::kDelimiterAt);
  if (result.empty()) {
    err_info = "Illegel confirmContext content: unregular value format!";
    return false;
  }
  partition_key = result[0];
  booked_time = (int64_t)atol(result[1].c_str());
  err_info = "Ok";
  return true;
}

void RmtDataCacheCsm::rmvMetaInfo(const string& partition_key) {
  map<string, PartitionExt>::iterator it_part;
  map<string, set<string> >::iterator it_topic;
  map<NodeInfo, set<string> >::iterator it_broker;
  it_part = partitions_.find(partition_key);
  if (it_part != partitions_.end()) {
    it_topic = topic_partition_.find(it_part->second.GetTopic());
    if (it_topic != topic_partition_.end()) {
      it_topic->second.erase(it_part->second.GetPartitionKey());
      if (it_topic->second.empty()) {
        topic_partition_.erase(it_part->second.GetTopic());
      }
    }
    it_broker = broker_partition_.find(it_part->second.GetBrokerInfo());
    if (it_broker != broker_partition_.end()) {
      it_broker->second.erase(it_part->second.GetPartitionKey());
      if (it_broker->second.empty()) {
        broker_partition_.erase(it_part->second.GetBrokerInfo());
      }
    }
    partitions_.erase(partition_key);
    part_subinfo_.erase(partition_key);
    cur_part_cnt_.DecrementAndGet();
  }
}

bool RmtDataCacheCsm::inRelPartition(string& err_info, bool need_delay_check, bool filter_consume,
                                     const string& confirm_context, bool is_consumed) {
  int64_t delay_time;
  int64_t booked_time;
  string partition_key;
  map<string, PartitionExt>::iterator it_part;
  map<string, int64_t>::iterator it_used;
  // parse confirm context
  bool result = parseConfirmContext(err_info, confirm_context, partition_key, booked_time);
  if (!result) {
    return false;
  }
  lock_guard<mutex> lck(meta_lock_);
  it_part = partitions_.find(partition_key);
  if (it_part == partitions_.end()) {
    // partition is unregister, release partition
    partition_useds_.erase(partition_key);
    index_partitions_.remove(partition_key);
    err_info = "Not found the partition in Consume Partition set!";
    result = false;
  } else {
    it_used = partition_useds_.find(partition_key);
    if (it_used == partition_useds_.end()) {
      // partition is release but registered
      index_partitions_.remove(partition_key);
      index_partitions_.push_back(partition_key);
    } else {
      if (it_used->second == booked_time) {
        // wait release
        partition_useds_.erase(partition_key);
        index_partitions_.remove(partition_key);
        delay_time = 0;
        if (need_delay_check) {
          delay_time = it_part->second.ProcConsumeResult(
              def_flowctrl_handler_, group_flowctrl_handler_, filter_consume, is_consumed);
        }
        if (delay_time > 10) {
          addDelayTimer(partition_key, delay_time);
        } else {
          index_partitions_.push_back(partition_key);
        }
        err_info = "Ok";
        result = true;
      } else {
        // partiton is used by other thread
        err_info = "Illegel confirmContext content: context not equal!";
        result = false;
      }
    }
  }
  return result;
}

}  // namespace tubemq
