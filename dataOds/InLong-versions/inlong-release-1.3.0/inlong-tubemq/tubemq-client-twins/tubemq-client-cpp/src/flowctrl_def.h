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

#ifndef TUBEMQ_CLIENT_FLOW_CONTROL_H_
#define TUBEMQ_CLIENT_FLOW_CONTROL_H_

#include <rapidjson/document.h>
#include <stdint.h>

#include <algorithm>
#include <list>
#include <map>
#include <mutex>
#include <string>
#include <vector>

#include "tubemq/tubemq_atomic.h"

namespace tubemq {

using std::map;
using std::mutex;
using std::string;
using std::vector;

class FlowCtrlResult {
 public:
  FlowCtrlResult();
  FlowCtrlResult(int64_t datasize_limit, int32_t freqms_limit);
  FlowCtrlResult& operator=(const FlowCtrlResult& target);
  void SetDataDltAndFreqLimit(int64_t datasize_limit, int32_t freqms_limit);
  void SetDataSizeLimit(int64_t datasize_limit);
  void SetFreqMsLimit(int32_t freqms_limit);
  int64_t GetDataSizeLimit();
  int32_t GetFreqMsLimit();

 private:
  int64_t datasize_limit_;
  int32_t freqms_limit_;
};

class FlowCtrlItem {
 public:
  FlowCtrlItem();
  FlowCtrlItem(int32_t type, int32_t zero_cnt, int32_t freqms_limit);
  FlowCtrlItem(int32_t type, int32_t datasize_limit, int32_t freqms_limit,
               int32_t min_data_filter_freqms);
  FlowCtrlItem(int32_t type, int32_t start_time, int32_t end_time, int64_t datadlt_m,
               int64_t datasize_limit, int32_t freqms_limit);
  FlowCtrlItem& operator=(const FlowCtrlItem& target);
  void Clear();
  void ResetFlowCtrlValue(int32_t type, int32_t datasize_limit, int32_t freqms_limit,
                          int32_t min_data_filter_freqms);
  int32_t GetFreLimit(int32_t msg_zero_cnt) const;
  bool GetDataLimit(int64_t datadlt_m,
    int32_t curr_time, FlowCtrlResult& flowctrl_result) const;
  const int32_t GetType() const { return type_; }
  const int32_t GetZeroCnt() const { return zero_cnt_; }
  const int32_t GetStartTime() const { return start_time_; }
  const int32_t GetEndTime() const { return end_time_; }
  const int64_t GetDataSizeLimit() const { return datasize_limit_; }
  const int32_t GetFreqMsLimit() const { return freqms_limit_; }
  const int64_t GetDltInM() const { return datadlt_m_; }

 private:
  int32_t type_;
  int32_t start_time_;
  int32_t end_time_;
  int64_t datadlt_m_;
  int64_t datasize_limit_;
  int32_t freqms_limit_;
  int32_t zero_cnt_;
};

class FlowCtrlRuleHandler {
 public:
  FlowCtrlRuleHandler();
  ~FlowCtrlRuleHandler();
  void UpdateDefFlowCtrlInfo(bool is_default, int32_t qrypriority_id, int64_t flowctrl_id,
                             const string& flowctrl_info);
  bool GetCurDataLimit(int64_t last_datadlt, FlowCtrlResult& flowctrl_result) const;
  int32_t GetCurFreqLimitTime(int32_t msg_zero_cnt, int32_t received_limit) const;
  void GetFilterCtrlItem(FlowCtrlItem& result) const;
  void GetFlowCtrlInfo(string& flowctrl_info) const;
  int32_t GetMinZeroCnt() const { return this->min_zero_cnt_.Get(); }
  int32_t GetQryPriorityId() const { return this->qrypriority_id_.Get(); }
  void SetQryPriorityId(int32_t qrypriority_id) { this->qrypriority_id_.Set(qrypriority_id); }
  const int64_t GetFlowCtrlId() const { return this->flowctrl_id_.Get(); }

 private:
  void initialStatisData();
  void clearStatisData();
  static bool compareFeqQueue(const FlowCtrlItem& queue1, const FlowCtrlItem& queue2);
  static bool compareDataLimitQueue(const FlowCtrlItem& o1, const FlowCtrlItem& o2);
  bool parseStringMember(string& err_info, const rapidjson::Value& root, const char* key,
                         string& value, bool compare_value, string required_val);
  bool parseLongMember(string& err_info, const rapidjson::Value& root, const char* key,
                       int64_t& value, bool compare_value, int64_t required_val);
  bool parseIntMember(string& err_info, const rapidjson::Value& root, const char* key,
                      int32_t& value, bool compare_value, int32_t required_val);
  bool parseFlowCtrlInfo(const string& flowctrl_info,
                         map<int32_t, vector<FlowCtrlItem> >& flowctrl_info_map);
  bool parseDataLimit(string& err_info, const rapidjson::Value& root,
                      vector<FlowCtrlItem>& flowCtrlItems);
  bool parseFreqLimit(string& err_info, const rapidjson::Value& root,
                      vector<FlowCtrlItem>& flowctrl_items);
  bool parseLowFetchLimit(string& err_info, const rapidjson::Value& root,
                          vector<FlowCtrlItem>& flowctrl_items);
  bool parseTimeMember(string& err_info, const rapidjson::Value& root, const char* key,
                       int32_t& value);

 private:
  mutable mutex config_lock_;
  string flowctrl_info_;
  FlowCtrlItem filter_ctrl_item_;
  map<int32_t, vector<FlowCtrlItem> > flowctrl_rules_;
  int64_t last_update_time_;
  AtomicLong flowctrl_id_;
  AtomicInteger qrypriority_id_;
  AtomicInteger min_zero_cnt_;
  AtomicLong min_datadlt_limt_;
  AtomicInteger datalimit_start_time_;
  AtomicInteger datalimit_end_time_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_FLOW_CONTROL_H_
