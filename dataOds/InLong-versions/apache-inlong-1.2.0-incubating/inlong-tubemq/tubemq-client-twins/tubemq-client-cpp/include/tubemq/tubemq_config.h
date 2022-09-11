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

#ifndef TUBEMQ_CLIENT_CONFIGURE_H_
#define TUBEMQ_CLIENT_CONFIGURE_H_

#include <stdint.h>
#include <stdio.h>

#include <map>
#include <set>
#include <string>

namespace tubemq {

using std::map;
using std::set;
using std::string;


// configure for log, thread pool etc.
class TubeMQServiceConfig {
 public:
  TubeMQServiceConfig();
  ~TubeMQServiceConfig();
  void SetLogCofigInfo(int32_t log_max_num,
                int32_t log_max_size, int32_t log_level, const string& log_path);
  const int32_t GetMaxLogFileNum() const;
  const int32_t GetMaxLogFileSize() const;
  const int32_t GetLogPrintLevel() const;
  const string& GetLogStorePath() const;
  void SetDnsXfsPeriodInMs(int32_t dns_xfs_period_ms);
  const int32_t GetDnsXfsPeriodInMs() const;
  void SetServiceThreads(int32_t timer_threads,
    int32_t network_threads, int32_t signal_threads);
  const int32_t GetTimerThreads() const;
  const int32_t GetNetWorkThreads() const;
  const int32_t GetSignalThreads() const;
  const string ToString() const;

 private:
  // max log file count
  int32_t log_num_;
  // unit MB
  int32_t log_size_;
  // 0:trace, 1:debug, 2:info, 3:warn, 4:error
  int32_t log_level_;
  // need include log filename
  string log_path_;
  int32_t dns_xfs_period_ms_;
  int32_t timer_threads_;
  int32_t network_threads_;
  int32_t signal_threads_;
};

class BaseConfig {
 public:
  BaseConfig();
  ~BaseConfig();
  BaseConfig& operator=(const BaseConfig& target);
  bool SetMasterAddrInfo(string& err_info, const string& master_addrinfo);
  bool SetTlsInfo(string& err_info, bool tls_enable, const string& trust_store_path,
                  const string& trust_store_password);
  bool SetAuthenticInfo(string& err_info, bool authentic_enable, const string& usr_name,
                        const string& usr_password);
  const string& GetMasterAddrInfo() const;
  bool IsTlsEnabled();
  const string& GetTrustStorePath() const;
  const string& GetTrustStorePassword() const;
  bool IsAuthenticEnabled();
  const string& GetUsrName() const;
  const string& GetUsrPassWord() const;
  // set the rpc timout, unit Millis-second, duration [8000, 300000],
  // default 15000 Millis-seconds;
  void SetRpcReadTimeoutMs(int32_t rpc_read_timeout_ms);
  int32_t GetRpcReadTimeoutMs();
  // Set the duration of the client's heartbeat cycle, in Millis-seconds,
  // the default is 10000 Millis-seconds
  void SetHeartbeatPeriodMs(int32_t heartbeat_period_ms);
  int32_t GetHeartbeatPeriodMs();
  void SetMaxHeartBeatRetryTimes(int32_t max_heartbeat_retry_times);
  int32_t GetMaxHeartBeatRetryTimes();
  void SetHeartbeatPeriodAftFailMs(int32_t heartbeat_period_afterfail_ms);
  int32_t GetHeartbeatPeriodAftFailMs();
  const string ToString() const;

 private:
  string master_addrinfo_;
  // user authenticate
  bool auth_enable_;
  string auth_usrname_;
  string auth_usrpassword_;
  // TLS configuration
  bool tls_enabled_;
  string tls_trust_store_path_;
  string tls_trust_store_password_;
  // other setting
  int32_t rpc_read_timeout_ms_;
  int32_t heartbeat_period_ms_;
  int32_t max_heartbeat_retry_times_;
  int32_t heartbeat_period_afterfail_ms_;
};

enum ConsumePosition {
  kConsumeFromFirstOffset = -1,
  kConsumeFromLatestOffset = 0,
  kComsumeFromMaxOffsetAlways = 1
};  // enum ConsumePosition

class ConsumerConfig : public BaseConfig {
 public:
  ConsumerConfig();
  ~ConsumerConfig();
  ConsumerConfig& operator=(const ConsumerConfig& target);
  bool SetGroupConsumeTarget(string& err_info, const string& group_name,
                             const set<string>& subscribed_topicset);
  bool SetGroupConsumeTarget(string& err_info, const string& group_name,
                             const map<string, set<string> >& subscribed_topic_and_filter_map);
  bool SetGroupConsumeTarget(string& err_info, const string& group_name,
                             const map<string, set<string> >& subscribed_topic_and_filter_map,
                             const string& session_key, uint32_t source_count, bool is_select_big,
                             const map<string, int64_t>& part_offset_map);
  bool IsBoundConsume() const { return is_bound_consume_; }
  const string& GetSessionKey() const { return session_key_; }
  const uint32_t GetSourceCount() const { return source_count_; }
  bool IsSelectBig() const { return is_select_big_; }
  const map<string, int64_t>& GetPartOffsetInfo() const { return part_offset_map_; }
  const string& GetGroupName() const;
  const map<string, set<string> >& GetSubTopicAndFilterMap() const;
  void SetConsumePosition(ConsumePosition consume_from_where);
  const ConsumePosition GetConsumePosition() const;
  const int32_t GetMsgNotFoundWaitPeriodMs() const;
  // SetMaxPartCheckPeriodMs() use note:
  // The value range is [negative value, 0, positive value] and the value directly determines
  // the behavior of the TubeMQConsumer.GetMessage() function:
  // 1. if it is set to a negative value, it means that the GetMessage() calling thread will
  //    be blocked forever and will not return until the consumption conditions are met;
  // 2. if If it is set to 0, it means that the GetMessage() calling thread will only block
  //    the ConsumerConfig.GetPartCheckSliceMs() interval when the consumption conditions
  //    are not met and then return;
  // 3. if it is set to a positive number, it will not meet the current user usage (including
  //    unused partitions or allocated partitions, but these partitions do not meet the usage
  //    conditions), the GetMessage() calling thread will be blocked until the total time of
  //    ConsumerConfig.GetMaxPartCheckPeriodMs expires
  void SetMaxPartCheckPeriodMs(int32_t max_part_check_period_ms);
  const int32_t GetMaxPartCheckPeriodMs() const;
  void SetPartCheckSliceMs(uint32_t part_check_slice_ms);
  const uint32_t GetPartCheckSliceMs() const;
  void SetMsgNotFoundWaitPeriodMs(int32_t msg_notfound_wait_period_ms);
  const int32_t GetMaxSubinfoReportIntvl() const;
  void SetMaxSubinfoReportIntvl(int32_t max_subinfo_report_intvl);
  bool IsRollbackIfConfirmTimeout();
  void setRollbackIfConfirmTimeout(bool is_rollback_if_confirm_timeout);
  const int32_t GetWaitPeriodIfConfirmWaitRebalanceMs() const;
  void SetWaitPeriodIfConfirmWaitRebalanceMs(int32_t reb_confirm_wait_period_ms);
  const int32_t GetMaxConfirmWaitPeriodMs() const;
  void SetMaxConfirmWaitPeriodMs(int32_t max_confirm_wait_period_ms);
  const int32_t GetShutdownRebWaitPeriodMs() const;
  void SetShutdownRebWaitPeriodMs(int32_t wait_period_when_shutdown_ms);
  const string ToString() const;

 private:
  bool setGroupConsumeTarget(string& err_info, bool is_bound_consume, const string& group_name,
                             const map<string, set<string> >& subscribed_topic_and_filter_map,
                             const string& session_key, uint32_t source_count, bool is_select_big,
                             const map<string, int64_t>& part_offset_map);

 private:
  string group_name_;
  map<string, set<string> > sub_topic_and_filter_map_;
  bool is_bound_consume_;
  string session_key_;
  uint32_t source_count_;
  bool is_select_big_;
  map<string, int64_t> part_offset_map_;
  ConsumePosition consume_position_;
  int32_t max_subinfo_report_intvl_;
  int32_t max_part_check_period_ms_;
  uint32_t part_check_slice_ms_;
  int32_t msg_notfound_wait_period_ms_;
  bool is_rollback_if_confirm_timout_;
  int32_t reb_confirm_wait_period_ms_;
  int32_t max_confirm_wait_period_ms_;
  int32_t shutdown_reb_wait_period_ms_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_CONFIGURE_H_
