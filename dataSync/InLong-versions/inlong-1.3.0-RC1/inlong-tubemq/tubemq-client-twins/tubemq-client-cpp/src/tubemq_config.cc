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

#include "tubemq/tubemq_config.h"

#include <sstream>
#include <vector>

#include "const_config.h"
#include "const_rpc.h"
#include "utils.h"

namespace tubemq {

using std::set;
using std::stringstream;
using std::vector;



TubeMQServiceConfig::TubeMQServiceConfig() {
  log_num_ = tb_config::kLogNumDef;
  log_size_ = tb_config::kLogSizeDefMB;
  log_level_ = tb_config::kLogLevelDef;
  log_path_ = tb_config::kLogPathDef;
  dns_xfs_period_ms_ = tb_config::kDnsXfsPeriodInMsDef;
  timer_threads_ = tb_config::kTimerThreadNumDef;
  network_threads_ = tb_config::kNetworkThreadNumDef;
  signal_threads_ = tb_config::kSignalThreadNumDef;
}

TubeMQServiceConfig::~TubeMQServiceConfig() {
  //
}

void TubeMQServiceConfig::SetLogCofigInfo(int32_t log_max_num,
                            int32_t log_max_size, int32_t log_level, const string& log_path) {
  log_num_   = log_max_num;
  log_size_  = log_max_size;
  log_level_ = log_level;
  log_path_  = log_path;
  log_level_ = TUBEMQ_MID(log_level, 4, 0);
}

void TubeMQServiceConfig::SetDnsXfsPeriodInMs(int32_t dns_xfs_period_ms) {
    dns_xfs_period_ms_ =
        TUBEMQ_MID(dns_xfs_period_ms, tb_config::kMaxIntValue, 10000);
}

void TubeMQServiceConfig::SetServiceThreads(int32_t timer_threads,
                                 int32_t network_threads, int32_t signal_threads) {
  timer_threads_   = TUBEMQ_MID(timer_threads, 50, 2);
  network_threads_ = TUBEMQ_MID(network_threads, 50, 4);
  signal_threads_  = TUBEMQ_MID(signal_threads, 50, 4);
}

const int32_t TubeMQServiceConfig::GetMaxLogFileNum() const {
  return log_num_;
}

const int32_t TubeMQServiceConfig::GetMaxLogFileSize() const {
  return log_size_;
}

const int32_t TubeMQServiceConfig::GetLogPrintLevel() const {
  return log_level_;
}

const string& TubeMQServiceConfig::GetLogStorePath() const {
  return log_path_;
}

const int32_t TubeMQServiceConfig::GetDnsXfsPeriodInMs() const {
  return dns_xfs_period_ms_;
}

const int32_t TubeMQServiceConfig::GetTimerThreads() const {
  return timer_threads_;
}

const int32_t TubeMQServiceConfig::GetNetWorkThreads() const {
  return network_threads_;
}

const int32_t TubeMQServiceConfig::GetSignalThreads() const {
  return signal_threads_;
}

const string TubeMQServiceConfig::ToString() const {
    stringstream ss;
    ss << "TubeMQServiceConfig={log_num_=";
    ss << log_num_;
    ss << ", log_size_=";
    ss << log_size_;
    ss << ", log_level_=";
    ss << log_level_;
    ss << ", log_path_='";
    ss << log_path_;
    ss << "', dns_xfs_period_ms_=";
    ss << dns_xfs_period_ms_;
    ss << ", timer_threads_=";
    ss << timer_threads_;
    ss << ", network_threads_=";
    ss << network_threads_;
    ss << ", signal_threads_=";
    ss << signal_threads_;
    ss << "}";
    return ss.str();
}

BaseConfig::BaseConfig() {
  master_addrinfo_ = "";
  auth_enable_ = false;
  auth_usrname_ = "";
  auth_usrpassword_ = "";
  tls_enabled_ = false;
  tls_trust_store_path_ = "";
  tls_trust_store_password_ = "";
  rpc_read_timeout_ms_ = tb_config::kRpcTimoutDefMs;
  heartbeat_period_ms_ = tb_config::kHeartBeatPeriodDefMs;
  max_heartbeat_retry_times_ = tb_config::kHeartBeatFailRetryTimesDef;
  heartbeat_period_afterfail_ms_ = tb_config::kHeartBeatSleepPeriodDefMs;
}

BaseConfig::~BaseConfig() {
  //
}

BaseConfig& BaseConfig::operator=(const BaseConfig& target) {
  if (this != &target) {
    master_addrinfo_ = target.master_addrinfo_;
    auth_enable_ = target.auth_enable_;
    auth_usrname_ = target.auth_usrname_;
    auth_usrpassword_ = target.auth_usrpassword_;
    tls_enabled_ = target.tls_enabled_;
    tls_trust_store_path_ = target.tls_trust_store_path_;
    tls_trust_store_password_ = target.tls_trust_store_password_;
    rpc_read_timeout_ms_ = target.rpc_read_timeout_ms_;
    heartbeat_period_ms_ = target.heartbeat_period_ms_;
    max_heartbeat_retry_times_ = target.max_heartbeat_retry_times_;
    heartbeat_period_afterfail_ms_ = target.heartbeat_period_afterfail_ms_;
  }
  return *this;
}

bool BaseConfig::SetMasterAddrInfo(string& err_info, const string& master_addrinfo) {
  // check parameter masterAddrInfo
  string trimed_master_addr_info = Utils::Trim(master_addrinfo);
  if (trimed_master_addr_info.empty()) {
    err_info = "Illegal parameter: master_addrinfo is blank!";
    return false;
  }
  if (trimed_master_addr_info.length() > tb_config::kMasterAddrInfoMaxLength) {
    stringstream ss;
    ss << "Illegal parameter: over max ";
    ss << tb_config::kMasterAddrInfoMaxLength;
    ss << " length of master_addrinfo parameter!";
    err_info = ss.str();
    return false;
  }
  // parse and verify master address info
  // master_addrinfo's format like ip1:port1,ip2:port2,ip3:port3
  map<string, int32_t> tgt_address_map;
  Utils::Split(master_addrinfo, tgt_address_map, delimiter::kDelimiterComma,
               delimiter::kDelimiterColon);
  if (tgt_address_map.empty()) {
    err_info = "Illegal parameter: unrecognized master_addrinfo address information!";
    return false;
  }
  master_addrinfo_ = trimed_master_addr_info;
  err_info = "Ok";
  return true;
}

bool BaseConfig::SetTlsInfo(string& err_info, bool tls_enable, const string& trust_store_path,
                            const string& trust_store_password) {
  tls_enabled_ = tls_enable;
  if (tls_enable) {
    string trimed_trust_store_path = Utils::Trim(trust_store_path);
    if (trimed_trust_store_path.empty()) {
      err_info = "Illegal parameter: trust_store_path is empty!";
      return false;
    }
    string trimed_trust_store_password = Utils::Trim(trust_store_password);
    if (trimed_trust_store_password.empty()) {
      err_info = "Illegal parameter: trust_store_password is empty!";
      return false;
    }
    tls_trust_store_path_ = trimed_trust_store_path;
    tls_trust_store_password_ = trimed_trust_store_password;
  } else {
    tls_trust_store_path_ = "";
    tls_trust_store_password_ = "";
  }
  err_info = "Ok";
  return true;
}

bool BaseConfig::SetAuthenticInfo(string& err_info, bool authentic_enable, const string& usr_name,
                                  const string& usr_password) {
  auth_enable_ = authentic_enable;
  if (authentic_enable) {
    string trimed_usr_name = Utils::Trim(usr_name);
    if (trimed_usr_name.empty()) {
      err_info = "Illegal parameter: usr_name is empty!";
      return false;
    }
    string trimed_usr_password = Utils::Trim(usr_password);
    if (trimed_usr_password.empty()) {
      err_info = "Illegal parameter: usr_password is empty!";
      return false;
    }
    auth_usrname_ = trimed_usr_name;
    auth_usrpassword_ = trimed_usr_password;
  } else {
    auth_usrname_ = "";
    auth_usrpassword_ = "";
  }
  err_info = "Ok";
  return true;
}

const string& BaseConfig::GetMasterAddrInfo() const { return master_addrinfo_; }

bool BaseConfig::IsTlsEnabled() { return tls_enabled_; }

const string& BaseConfig::GetTrustStorePath() const { return tls_trust_store_path_; }

const string& BaseConfig::GetTrustStorePassword() const { return tls_trust_store_password_; }

bool BaseConfig::IsAuthenticEnabled() { return auth_enable_; }

const string& BaseConfig::GetUsrName() const { return auth_usrname_; }

const string& BaseConfig::GetUsrPassWord() const { return auth_usrpassword_; }

void BaseConfig::SetRpcReadTimeoutMs(int rpc_read_timeout_ms) {
  if (rpc_read_timeout_ms >= tb_config::kRpcTimoutMaxMs) {
    rpc_read_timeout_ms_ = tb_config::kRpcTimoutMaxMs;
  } else if (rpc_read_timeout_ms <= tb_config::kRpcTimoutMinMs) {
    rpc_read_timeout_ms_ = tb_config::kRpcTimoutMinMs;
  } else {
    rpc_read_timeout_ms_ = rpc_read_timeout_ms;
  }
}

int32_t BaseConfig::GetRpcReadTimeoutMs() { return rpc_read_timeout_ms_; }

void BaseConfig::SetHeartbeatPeriodMs(int32_t heartbeat_period_ms) {
  heartbeat_period_ms_ = heartbeat_period_ms;
}

int32_t BaseConfig::GetHeartbeatPeriodMs() { return heartbeat_period_ms_; }

void BaseConfig::SetMaxHeartBeatRetryTimes(int32_t max_heartbeat_retry_times) {
  max_heartbeat_retry_times_ = max_heartbeat_retry_times;
}

int32_t BaseConfig::GetMaxHeartBeatRetryTimes() { return max_heartbeat_retry_times_; }

void BaseConfig::SetHeartbeatPeriodAftFailMs(int32_t heartbeat_period_afterfail_ms) {
  heartbeat_period_afterfail_ms_ = heartbeat_period_afterfail_ms;
}

int32_t BaseConfig::GetHeartbeatPeriodAftFailMs() { return heartbeat_period_afterfail_ms_; }

const string BaseConfig::ToString() const {
  stringstream ss;
  ss << "BaseConfig={master_addrinfo_='";
  ss << master_addrinfo_;
  ss << "', authEnable=";
  ss << auth_enable_;
  ss << ", auth_usrname_='";
  ss << auth_usrname_;
  ss << "', auth_usrpassword_='";
  ss << auth_usrpassword_;
  ss << "', tls_enabled_=";
  ss << tls_enabled_;
  ss << ", tls_trust_store_path_='";
  ss << tls_trust_store_path_;
  ss << "', tls_trust_store_password_='";
  ss << tls_trust_store_password_;
  ss << "', rpc_read_timeout_ms_=";
  ss << rpc_read_timeout_ms_;
  ss << ", heartbeat_period_ms_=";
  ss << heartbeat_period_ms_;
  ss << ", max_heartbeat_retry_times_=";
  ss << max_heartbeat_retry_times_;
  ss << ", heartbeat_period_afterfail_ms_=";
  ss << heartbeat_period_afterfail_ms_;
  ss << "}";
  return ss.str();
}

ConsumerConfig::ConsumerConfig() {
  group_name_ = "";
  is_bound_consume_ = false;
  session_key_ = "";
  source_count_ = 0;
  is_select_big_ = true;
  consume_position_ = kConsumeFromLatestOffset;
  is_rollback_if_confirm_timout_ = true;
  max_subinfo_report_intvl_ = tb_config::kSubInfoReportMaxIntervalTimes;
  max_part_check_period_ms_ = tb_config::kMaxPartCheckPeriodMsDef;
  part_check_slice_ms_ = tb_config::kPartCheckSliceMsDef;
  msg_notfound_wait_period_ms_ = tb_config::kMsgNotfoundWaitPeriodMsDef;
  reb_confirm_wait_period_ms_ = tb_config::kRebConfirmWaitPeriodMsDef;
  max_confirm_wait_period_ms_ = tb_config::kConfirmWaitPeriodMsMax;
  shutdown_reb_wait_period_ms_ = tb_config::kRebWaitPeriodWhenShutdownMs;
}

ConsumerConfig::~ConsumerConfig() {
  //
}

ConsumerConfig& ConsumerConfig::operator=(const ConsumerConfig& target) {
  if (this != &target) {
    // parent class
    BaseConfig::operator=(target);
    // child class
    group_name_ = target.group_name_;
    sub_topic_and_filter_map_ = target.sub_topic_and_filter_map_;
    is_bound_consume_ = target.is_bound_consume_;
    session_key_ = target.session_key_;
    source_count_ = target.source_count_;
    is_select_big_ = target.is_select_big_;
    part_offset_map_ = target.part_offset_map_;
    consume_position_ = target.consume_position_;
    max_subinfo_report_intvl_ = target.max_subinfo_report_intvl_;
    max_part_check_period_ms_ = target.max_part_check_period_ms_;
    part_check_slice_ms_ = target.part_check_slice_ms_;
    msg_notfound_wait_period_ms_ = target.msg_notfound_wait_period_ms_;
    is_rollback_if_confirm_timout_ = target.is_rollback_if_confirm_timout_;
    reb_confirm_wait_period_ms_ = target.reb_confirm_wait_period_ms_;
    max_confirm_wait_period_ms_ = target.max_confirm_wait_period_ms_;
    shutdown_reb_wait_period_ms_ = target.shutdown_reb_wait_period_ms_;
  }
  return *this;
}

bool ConsumerConfig::SetGroupConsumeTarget(string& err_info, const string& group_name,
                                           const set<string>& subscribed_topicset) {
  string tgt_group_name;
  bool is_success = Utils::ValidGroupName(err_info, group_name, tgt_group_name);
  if (!is_success) {
    return false;
  }
  if (subscribed_topicset.empty()) {
    err_info = "Illegal parameter: subscribed_topicset is empty!";
    return false;
  }
  string topic_name;
  map<string, set<string> > tmp_sub_map;
  for (set<string>::iterator it = subscribed_topicset.begin(); it != subscribed_topicset.end();
       ++it) {
    topic_name = Utils::Trim(*it);
    is_success =
        Utils::ValidString(err_info, topic_name, false, true, true, tb_config::kTopicNameMaxLength);
    if (!is_success) {
      err_info = "Illegal parameter: subscribed_topicset's item error, " + err_info;
      return false;
    }
    set<string> tmp_filters;
    tmp_sub_map[topic_name] = tmp_filters;
  }
  is_bound_consume_ = false;
  group_name_ = tgt_group_name;
  sub_topic_and_filter_map_ = tmp_sub_map;
  err_info = "Ok";
  return true;
}

bool ConsumerConfig::SetGroupConsumeTarget(
    string& err_info, const string& group_name,
    const map<string, set<string> >& subscribed_topic_and_filter_map) {
  string session_key;
  int source_count = 0;
  bool is_select_big = false;
  map<string, int64_t> part_offset_map;
  return setGroupConsumeTarget(err_info, false, group_name, subscribed_topic_and_filter_map,
                               session_key, source_count, is_select_big, part_offset_map);
}

bool ConsumerConfig::SetGroupConsumeTarget(
    string& err_info, const string& group_name,
    const map<string, set<string> >& subscribed_topic_and_filter_map, const string& session_key,
    uint32_t source_count, bool is_select_big, const map<string, int64_t>& part_offset_map) {
  return setGroupConsumeTarget(err_info, true, group_name, subscribed_topic_and_filter_map,
                               session_key, source_count, is_select_big, part_offset_map);
}

bool ConsumerConfig::setGroupConsumeTarget(
    string& err_info, bool is_bound_consume, const string& group_name,
    const map<string, set<string> >& subscribed_topic_and_filter_map, const string& session_key,
    uint32_t source_count, bool is_select_big, const map<string, int64_t>& part_offset_map) {
  // check parameter group_name
  string tgt_group_name;
  bool is_success = Utils::ValidGroupName(err_info, group_name, tgt_group_name);
  if (!is_success) {
    return false;
  }
  // check parameter subscribed_topic_and_filter_map
  if (subscribed_topic_and_filter_map.empty()) {
    err_info = "Illegal parameter: subscribed_topic_and_filter_map is empty!";
    return false;
  }
  map<string, set<string> > tmp_sub_map;
  map<string, set<string> >::const_iterator it_map;
  for (it_map = subscribed_topic_and_filter_map.begin();
       it_map != subscribed_topic_and_filter_map.end(); ++it_map) {
    uint32_t count = 0;
    string tmp_filteritem;
    set<string> tgt_filters;
    // check topic_name info
    is_success = Utils::ValidString(err_info, it_map->first, false, true, true,
                                    tb_config::kTopicNameMaxLength);
    if (!is_success) {
      stringstream ss;
      ss << "Check parameter subscribed_topic_and_filter_map error: topic ";
      ss << it_map->first;
      ss << " ";
      ss << err_info;
      err_info = ss.str();
      return false;
    }
    string topic_name = Utils::Trim(it_map->first);
    // check filter info
    set<string> subscribed_filters = it_map->second;
    for (set<string>::iterator it = subscribed_filters.begin(); it != subscribed_filters.end();
         ++it) {
      is_success = Utils::ValidFilterItem(err_info, *it, tmp_filteritem);
      if (!is_success) {
        stringstream ss;
        ss << "Check parameter subscribed_topic_and_filter_map error: topic ";
        ss << topic_name;
        ss << "'s filter item ";
        ss << err_info;
        err_info = ss.str();
        return false;
      }
      tgt_filters.insert(tmp_filteritem);
      count++;
    }
    if (count > tb_config::kFilterItemMaxCount) {
      stringstream ss;
      ss << "Check parameter subscribed_topic_and_filter_map error: topic ";
      ss << it_map->first;
      ss << "'s filter item over max item count : ";
      ss << tb_config::kFilterItemMaxCount;
      err_info = ss.str();
      return false;
    }
    tmp_sub_map[topic_name] = tgt_filters;
  }
  // check if bound consume
  if (!is_bound_consume) {
    is_bound_consume_ = false;
    group_name_ = tgt_group_name;
    sub_topic_and_filter_map_ = tmp_sub_map;
    err_info = "Ok";
    return true;
  }
  // check session_key
  string tgt_session_key = Utils::Trim(session_key);
  if (tgt_session_key.length() == 0
    || tgt_session_key.length() > tb_config::kSessionKeyMaxLength) {
    if (tgt_session_key.length() == 0) {
      err_info = "Illegal parameter: session_key is empty!";
    } else {
      stringstream ss;
      ss << "Illegal parameter: session_key's length over max length ";
      ss << tb_config::kSessionKeyMaxLength;
      err_info = ss.str();
    }
    return false;
  }
  // check part_offset_map
  string part_key;
  map<string, int64_t> tmp_parts_map;
  map<string, int64_t>::const_iterator it_part;
  for (it_part = part_offset_map.begin(); it_part != part_offset_map.end(); ++it_part) {
    vector<string> result;
    Utils::Split(it_part->first, result, delimiter::kDelimiterColon);
    if (result.size() != 3) {
      stringstream ss;
      ss << "Illegal parameter: part_offset_map's key ";
      ss << it_part->first;
      ss << " format error, value must be aaaa:bbbb:cccc !";
      err_info = ss.str();
      return false;
    }
    if (tmp_sub_map.find(result[1]) == tmp_sub_map.end()) {
      stringstream ss;
      ss << "Illegal parameter: ";
      ss << it_part->first;
      ss << " subscribed topic ";
      ss << result[1];
      ss << " not included in subscribed_topic_and_filter_map's topic list!";
      err_info = ss.str();
      return false;
    }
    if (it_part->first.find_first_of(delimiter::kDelimiterComma) != string::npos) {
      stringstream ss;
      ss << "Illegal parameter: key ";
      ss << it_part->first;
      ss << " include ";
      ss << delimiter::kDelimiterComma;
      ss << " char!";
      err_info = ss.str();
      return false;
    }
    if (it_part->second < 0) {
      stringstream ss;
      ss << "Illegal parameter: ";
      ss << it_part->first;
      ss << "'s offset must over or equal zero, value is ";
      ss << it_part->second;
      err_info = ss.str();
      return false;
    }
    Utils::Join(result, delimiter::kDelimiterColon, part_key);
    tmp_parts_map[part_key] = it_part->second;
  }
  // set verified data
  is_bound_consume_ = true;
  group_name_ = tgt_group_name;
  sub_topic_and_filter_map_ = tmp_sub_map;
  session_key_ = tgt_session_key;
  source_count_ = source_count;
  is_select_big_ = is_select_big;
  part_offset_map_ = tmp_parts_map;
  err_info = "Ok";
  return true;
}

const string& ConsumerConfig::GetGroupName() const { return group_name_; }

const map<string, set<string> >& ConsumerConfig::GetSubTopicAndFilterMap() const {
  return sub_topic_and_filter_map_;
}

void ConsumerConfig::SetConsumePosition(ConsumePosition consume_from_where) {
  consume_position_ = consume_from_where;
}

const ConsumePosition ConsumerConfig::GetConsumePosition() const { return consume_position_; }

const int32_t ConsumerConfig::GetMsgNotFoundWaitPeriodMs() const {
  return msg_notfound_wait_period_ms_;
}

void ConsumerConfig::SetMsgNotFoundWaitPeriodMs(int32_t msg_notfound_wait_period_ms) {
  msg_notfound_wait_period_ms_ = msg_notfound_wait_period_ms;
}

const uint32_t ConsumerConfig::GetPartCheckSliceMs() const {
  return part_check_slice_ms_;
}

void ConsumerConfig::SetPartCheckSliceMs(uint32_t part_check_slice_ms) {
  if (part_check_slice_ms >= 0
    && part_check_slice_ms <= 1000) {
    part_check_slice_ms_ = part_check_slice_ms;
  }
}

const int32_t ConsumerConfig::GetMaxPartCheckPeriodMs() const {
  return max_part_check_period_ms_;
}

void ConsumerConfig::SetMaxPartCheckPeriodMs(int32_t max_part_check_period_ms) {
  max_part_check_period_ms_ = max_part_check_period_ms;
}

const int32_t ConsumerConfig::GetMaxSubinfoReportIntvl() const {
  return max_subinfo_report_intvl_;
}

void ConsumerConfig::SetMaxSubinfoReportIntvl(int32_t max_subinfo_report_intvl) {
  max_subinfo_report_intvl_ = max_subinfo_report_intvl;
}

bool ConsumerConfig::IsRollbackIfConfirmTimeout() {
  return is_rollback_if_confirm_timout_; }

void ConsumerConfig::setRollbackIfConfirmTimeout(
  bool is_rollback_if_confirm_timeout) {
  is_rollback_if_confirm_timout_ = is_rollback_if_confirm_timeout;
}

const int ConsumerConfig::GetWaitPeriodIfConfirmWaitRebalanceMs() const {
  return reb_confirm_wait_period_ms_;
}

void ConsumerConfig::SetWaitPeriodIfConfirmWaitRebalanceMs(
  int32_t reb_confirm_wait_period_ms) {
  reb_confirm_wait_period_ms_ = reb_confirm_wait_period_ms;
}

const int32_t ConsumerConfig::GetMaxConfirmWaitPeriodMs() const {
  return max_confirm_wait_period_ms_; }

void ConsumerConfig::SetMaxConfirmWaitPeriodMs(
  int32_t max_confirm_wait_period_ms) {
  max_confirm_wait_period_ms_ = max_confirm_wait_period_ms;
}

const int32_t ConsumerConfig::GetShutdownRebWaitPeriodMs() const {
  return shutdown_reb_wait_period_ms_;
}

void ConsumerConfig::SetShutdownRebWaitPeriodMs(
  int32_t wait_period_when_shutdown_ms) {
  shutdown_reb_wait_period_ms_ = wait_period_when_shutdown_ms;
}

const string ConsumerConfig::ToString() const {
  int32_t i = 0;
  stringstream ss;
  map<string, int64_t>::const_iterator it;
  map<string, set<string> >::const_iterator it_map;

  // print info
  ss << "ConsumerConfig = {";
  ss << BaseConfig::ToString();
  ss << ", group_name_='";
  ss << group_name_;
  ss << "', sub_topic_and_filter_map_={";
  for (it_map = sub_topic_and_filter_map_.begin();
       it_map != sub_topic_and_filter_map_.end(); ++it_map) {
    if (i++ > 0) {
      ss << ",";
    }
    ss << "'";
    ss << it_map->first;
    ss << "'=[";
    int32_t j = 0;
    set<string> topic_set = it_map->second;
    for (set<string>::const_iterator it = topic_set.begin(); it != topic_set.end(); ++it) {
      if (j++ > 0) {
        ss << ",";
      }
      ss << "'";
      ss << *it;
      ss << "'";
    }
    ss << "]";
  }
  ss << "}, is_bound_consume_=";
  ss << is_bound_consume_;
  ss << ", session_key_='";
  ss << session_key_;
  ss << "', source_count_=";
  ss << source_count_;
  ss << ", is_select_big_=";
  ss << is_select_big_;
  ss << ", part_offset_map_={";
  i = 0;
  for (it = part_offset_map_.begin(); it != part_offset_map_.end(); ++it) {
    if (i++ > 0) {
      ss << ",";
    }
    ss << "'";
    ss << it->first;
    ss << "'=";
    ss << it->second;
  }
  ss << "}, consume_position_=";
  ss << consume_position_;
  ss << ", max_subinfo_report_intvl_=";
  ss << max_subinfo_report_intvl_;
  ss << ", msg_notfound_wait_period_ms_=";
  ss << msg_notfound_wait_period_ms_;
  ss << ", max_part_check_period_ms_=";
  ss << max_part_check_period_ms_;
  ss << ", part_check_slice_ms_=";
  ss << part_check_slice_ms_;
  ss << ", is_rollback_if_confirm_timout_=";
  ss << is_rollback_if_confirm_timout_;
  ss << ", reb_confirm_wait_period_ms_=";
  ss << reb_confirm_wait_period_ms_;
  ss << ", max_confirm_wait_period_ms_=";
  ss << max_confirm_wait_period_ms_;
  ss << ", shutdown_reb_wait_period_ms_=";
  ss << shutdown_reb_wait_period_ms_;
  ss << "}";
  return ss.str();
}

}  // namespace tubemq
