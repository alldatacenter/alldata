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

#include "flowctrl_def.h"

#include <stdio.h>
#include <time.h>
#include <unistd.h>

#include <sstream>

#include "const_config.h"
#include "logger.h"
#include "utils.h"

namespace tubemq {

using std::stringstream;
using std::lock_guard;

FlowCtrlResult::FlowCtrlResult() {
  datasize_limit_ = tb_config::kMaxIntValue;
  freqms_limit_ = 0;
}

FlowCtrlResult::FlowCtrlResult(int64_t datasize_limit, int32_t freqms_limit) {
  datasize_limit_ = datasize_limit;
  freqms_limit_ = freqms_limit;
}

FlowCtrlResult& FlowCtrlResult::operator=(const FlowCtrlResult& target) {
  if (this == &target) return *this;
  datasize_limit_ = target.datasize_limit_;
  freqms_limit_ = target.freqms_limit_;
  return *this;
}

void FlowCtrlResult::SetDataDltAndFreqLimit(int64_t datasize_limit, int32_t freqms_limit) {
  datasize_limit_ = datasize_limit;
  freqms_limit_ = freqms_limit;
}

void FlowCtrlResult::SetDataSizeLimit(int64_t datasize_limit) {
  datasize_limit_ = datasize_limit;
}

void FlowCtrlResult::SetFreqMsLimit(int32_t freqms_limit) { freqms_limit_ = freqms_limit; }

int64_t FlowCtrlResult::GetDataSizeLimit() { return datasize_limit_; }

int32_t FlowCtrlResult::GetFreqMsLimit() { return freqms_limit_; }

FlowCtrlItem::FlowCtrlItem() {
  type_ = 0;
  start_time_ = 2500;
  end_time_ = tb_config::kInvalidValue;
  datadlt_m_ = tb_config::kInvalidValue;
  datasize_limit_ = tb_config::kInvalidValue;
  freqms_limit_ = tb_config::kInvalidValue;
  zero_cnt_ = tb_config::kInvalidValue;
}

FlowCtrlItem::FlowCtrlItem(int32_t type, int32_t zero_cnt, int32_t freqms_limit) {
  type_ = type;
  start_time_ = 2500;
  end_time_ = tb_config::kInvalidValue;
  datadlt_m_ = tb_config::kInvalidValue;
  datasize_limit_ = tb_config::kInvalidValue;
  freqms_limit_ = freqms_limit;
  zero_cnt_ = zero_cnt;
}

FlowCtrlItem::FlowCtrlItem(int32_t type, int32_t datasize_limit, int32_t freqms_limit,
                           int32_t min_data_filter_freqms) {
  type_ = type;
  start_time_ = 2500;
  end_time_ = tb_config::kInvalidValue;
  datadlt_m_ = tb_config::kInvalidValue;
  datasize_limit_ = datasize_limit;
  freqms_limit_ = freqms_limit;
  zero_cnt_ = min_data_filter_freqms;
}

FlowCtrlItem::FlowCtrlItem(int32_t type, int32_t start_time, int32_t end_time, int64_t datadlt_m,
                           int64_t datasize_limit, int32_t freqms_limit) {
  type_ = type;
  start_time_ = start_time;
  end_time_ = end_time;
  datadlt_m_ = datadlt_m;
  datasize_limit_ = datasize_limit;
  freqms_limit_ = freqms_limit;
  zero_cnt_ = tb_config::kInvalidValue;
}

FlowCtrlItem& FlowCtrlItem::operator=(const FlowCtrlItem& target) {
  if (this == &target) return *this;
  type_ = target.type_;
  start_time_ = target.start_time_;
  end_time_ = target.end_time_;
  datadlt_m_ = target.datadlt_m_;
  datasize_limit_ = target.datasize_limit_;
  freqms_limit_ = target.freqms_limit_;
  zero_cnt_ = target.zero_cnt_;
  return *this;
}

int32_t FlowCtrlItem::GetFreLimit(int32_t msg_zero_cnt) const {
  if (type_ != 1) {
    return -1;
  }
  if (msg_zero_cnt >= zero_cnt_) {
    return freqms_limit_;
  }
  return -1;
}

void FlowCtrlItem::ResetFlowCtrlValue(int32_t type, int32_t datasize_limit, int32_t freqms_limit,
                                      int32_t min_data_filter_freqms) {
  type_ = type;
  start_time_ = 2500;
  end_time_ = tb_config::kInvalidValue;
  datadlt_m_ = tb_config::kInvalidValue;
  datasize_limit_ = datasize_limit;
  freqms_limit_ = freqms_limit;
  zero_cnt_ = min_data_filter_freqms;
}

void FlowCtrlItem::Clear() {
  type_ = 0;
  start_time_ = 2500;
  end_time_ = tb_config::kInvalidValue;
  datadlt_m_ = tb_config::kInvalidValue;
  datasize_limit_ = tb_config::kInvalidValue;
  freqms_limit_ = tb_config::kInvalidValue;
  zero_cnt_ = tb_config::kInvalidValue;
}

bool FlowCtrlItem::GetDataLimit(int64_t datadlt_m, int32_t curr_time,
                                FlowCtrlResult& flowctrl_result) const {
  if (type_ != 0 || datadlt_m <= datadlt_m_) {
    return false;
  }
  if (curr_time < start_time_ || curr_time > end_time_) {
    return false;
  }
  flowctrl_result.SetDataDltAndFreqLimit(datasize_limit_, freqms_limit_);
  return true;
}

FlowCtrlRuleHandler::FlowCtrlRuleHandler() {
  flowctrl_id_.GetAndSet(tb_config::kInvalidValue);
  flowctrl_info_ = "";
  min_zero_cnt_.Set(tb_config::kMaxIntValue);
  qrypriority_id_.Set(tb_config::kInvalidValue);
  min_datadlt_limt_.Set(tb_config::kMaxLongValue);
  datalimit_start_time_.Set(2500);
  datalimit_end_time_.Set(tb_config::kInvalidValue);
  last_update_time_ = Utils::GetCurrentTimeMillis();
}

FlowCtrlRuleHandler::~FlowCtrlRuleHandler() {
  //
}

void FlowCtrlRuleHandler::UpdateDefFlowCtrlInfo(bool is_default, int32_t qrypriority_id,
                                                int64_t flowctrl_id, const string& flowctrl_info) {
  map<int32_t, vector<FlowCtrlItem> > tmp_flowctrl_map;
  if (flowctrl_id == flowctrl_id_.Get()) {
    return;
  }
  int64_t curr_flowctrl_id = flowctrl_id_.Get();
  if (flowctrl_info.length() > 0) {
    parseFlowCtrlInfo(flowctrl_info, tmp_flowctrl_map);
  }
  lock_guard<mutex> lck(config_lock_);
  flowctrl_id_.Set(flowctrl_id);
  qrypriority_id_.Set(qrypriority_id);
  clearStatisData();
  if (tmp_flowctrl_map.empty()) {
    flowctrl_rules_.clear();
    flowctrl_info_ = "";
  } else {
    flowctrl_rules_ = tmp_flowctrl_map;
    flowctrl_info_ = flowctrl_info;
    initialStatisData();
  }
  last_update_time_ = Utils::GetCurrentTimeMillis();
  if (is_default) {
    LOG_INFO("[Flow Ctrl] Default FlowCtrl's flowctrl_id from %ld to %ld\n", curr_flowctrl_id,
             flowctrl_id);
  } else {
    LOG_INFO("[Flow Ctrl] Group FlowCtrl's flowctrl_id from %ld to %ld\n", curr_flowctrl_id,
             flowctrl_id);
  }
  return;
}

void FlowCtrlRuleHandler::initialStatisData() {
  vector<FlowCtrlItem>::iterator it_vec;
  map<int, vector<FlowCtrlItem> >::iterator it_map;

  it_map = flowctrl_rules_.find(0);
  if (it_map != flowctrl_rules_.end()) {
    for (it_vec = it_map->second.begin(); it_vec != it_map->second.end(); ++it_vec) {
      if (it_vec->GetType() != 0) {
        continue;
      }

      if (it_vec->GetDltInM() < min_datadlt_limt_.Get()) {
        min_datadlt_limt_.Set(it_vec->GetDltInM());
      }
      if (it_vec->GetStartTime() < datalimit_start_time_.Get()) {
        datalimit_start_time_.Set(it_vec->GetStartTime());
      }
      if (it_vec->GetEndTime() > datalimit_end_time_.Get()) {
        datalimit_end_time_.Set(it_vec->GetEndTime());
      }
    }
  }
  it_map = flowctrl_rules_.find(1);
  if (it_map != flowctrl_rules_.end()) {
    for (it_vec = it_map->second.begin(); it_vec != it_map->second.end(); ++it_vec) {
      if (it_vec->GetType() != 1) {
        continue;
      }
      if (it_vec->GetZeroCnt() < min_zero_cnt_.Get()) {
        min_zero_cnt_.Set(it_vec->GetZeroCnt());
      }
    }
  }
  it_map = flowctrl_rules_.find(3);
  if (it_map != flowctrl_rules_.end()) {
    for (it_vec = it_map->second.begin(); it_vec != it_map->second.end(); ++it_vec) {
      if (it_vec->GetType() != 3) {
        continue;
      }
      it_vec->GetDataSizeLimit();
      filter_ctrl_item_.ResetFlowCtrlValue(3, (int32_t)(it_vec->GetDataSizeLimit()),
                                                 it_vec->GetFreqMsLimit(), it_vec->GetZeroCnt());
    }
  }
}

void FlowCtrlRuleHandler::clearStatisData() {
  min_zero_cnt_.GetAndSet(tb_config::kMaxIntValue);
  min_datadlt_limt_.GetAndSet(tb_config::kMaxLongValue);
  qrypriority_id_.Set(tb_config::kInvalidValue);
  datalimit_start_time_.Set(2500);
  datalimit_end_time_.Set(tb_config::kInvalidValue);
  filter_ctrl_item_.Clear();
}

bool FlowCtrlRuleHandler::GetCurDataLimit(int64_t last_datadlt,
                                          FlowCtrlResult& flowctrl_result) const {
  struct tm utc_tm;
  bool result = false;
  vector<FlowCtrlItem>::const_iterator it_vec;
  map<int, vector<FlowCtrlItem> >::const_iterator it_map;
  // get current data limit
  time_t cur_time = time(NULL);
  gmtime_r(&cur_time, &utc_tm);
  int curr_time = (utc_tm.tm_hour + 8) % 24 * 100 + utc_tm.tm_min;
  if ((last_datadlt < min_datadlt_limt_.Get())
      || (curr_time < datalimit_start_time_.Get())
      || (curr_time > datalimit_end_time_.Get())) {
    return false;
  }
  // search total flowctrl rule
  lock_guard<mutex> lck(config_lock_);
  it_map = flowctrl_rules_.find(0);
  if (it_map != flowctrl_rules_.end()) {
    for (it_vec = it_map->second.begin(); it_vec != it_map->second.end(); ++it_vec) {
      if (it_vec->GetDataLimit(last_datadlt, curr_time, flowctrl_result)) {
        result = true;
        break;
      }
    }
  }
  return result;
}

int32_t FlowCtrlRuleHandler::GetCurFreqLimitTime(int32_t msg_zero_cnt,
                                                 int32_t received_limit) const {
  int32_t limit_data = received_limit;
  vector<FlowCtrlItem>::const_iterator it_vec;
  map<int, vector<FlowCtrlItem> >::const_iterator it_map;
  // check min zero count
  if (msg_zero_cnt < min_zero_cnt_.Get()) {
    return limit_data;
  }
  // search rule allow value
  lock_guard<mutex> lck(config_lock_);
  it_map = flowctrl_rules_.find(1);
  if (it_map != flowctrl_rules_.end()) {
    for (it_vec = it_map->second.begin(); it_vec != it_map->second.end(); ++it_vec) {
      limit_data = it_vec->GetFreLimit(msg_zero_cnt);
      if (limit_data >= 0) {
        break;
      }
    }
  }
  return limit_data;
}

void FlowCtrlRuleHandler::GetFilterCtrlItem(FlowCtrlItem& result) const {
  result.Clear();
  lock_guard<mutex> lck(config_lock_);
  result = filter_ctrl_item_;
}

void FlowCtrlRuleHandler::GetFlowCtrlInfo(string& flowctrl_info) const {
  flowctrl_info.clear();
  lock_guard<mutex> lck(config_lock_);
  flowctrl_info = flowctrl_info_;
}

bool FlowCtrlRuleHandler::compareDataLimitQueue(const FlowCtrlItem& o1, const FlowCtrlItem& o2) {
  if (o1.GetStartTime() >= o2.GetStartTime()) {
    return true;
  }
  return false;
}

bool FlowCtrlRuleHandler::compareFeqQueue(const FlowCtrlItem& queue1, const FlowCtrlItem& queue2) {
  return (queue1.GetZeroCnt() < queue2.GetZeroCnt());
}

bool FlowCtrlRuleHandler::parseFlowCtrlInfo(
    const string& flowctrl_info, map<int32_t, vector<FlowCtrlItem> >& flowctrl_info_map) {
  int32_t type;
  string err_info;
  stringstream ss;
  rapidjson::Document doc;
  // check flowctrl info length
  if (flowctrl_info.length() == 0) {
    return false;
  }
  // parse flowctrl info
  if (doc.Parse(flowctrl_info.c_str()).HasParseError()) {
    LOG_ERROR("Parsing flowCtrlInfo failure! flowctrl_info=%s\n", flowctrl_info.c_str());
    return false;
  }
  if (!doc.IsArray()) {
    LOG_ERROR("flowCtrlInfo's value must be array! flowctrl_info=%s\n", flowctrl_info.c_str());
    return false;
  }
  for (uint32_t i = 0; i < doc.Size(); i++) {
    vector<FlowCtrlItem> flowctrl_item_vec;
    const rapidjson::Value& node_item = doc[i];
    if (!node_item.IsObject()) {
      continue;
    }
    if (!parseIntMember(err_info, node_item, "type", type, false, -2)) {
      ss << "Decode Failure: ";
      ss << err_info;
      ss << " of type field in parse flowctrl_info!";
      err_info = ss.str();
      LOG_ERROR("parse flowCtrlInfo failure %s", err_info.c_str());
      return false;
    }
    if (type < 0 || type > 3) {
      ss << "type value must in [0,1,2,3] in index(";
      ss << i;
      ss << ") of flowctrl_info value!";
      err_info = ss.str();
      LOG_ERROR("parse flowCtrlInfo failure %s", err_info.c_str());
      return false;
    }

    switch (type) {
      case 1: {
        if (FlowCtrlRuleHandler::parseFreqLimit(err_info, node_item, flowctrl_item_vec)) {
          flowctrl_info_map[1] = flowctrl_item_vec;
        } else {
          LOG_ERROR("parse flowCtrlInfo's freqLimit failure: %s", err_info.c_str());
        }
      } break;

      case 3: {
        if (FlowCtrlRuleHandler::parseLowFetchLimit(err_info, node_item, flowctrl_item_vec)) {
          flowctrl_info_map[3] = flowctrl_item_vec;
        } else {
          LOG_ERROR("parse flowCtrlInfo's lowFetchLimit failure: %s", err_info.c_str());
        }
      } break;

      case 0: {
        if (FlowCtrlRuleHandler::parseDataLimit(err_info, node_item, flowctrl_item_vec)) {
          flowctrl_info_map[0] = flowctrl_item_vec;
        } else {
          LOG_ERROR("parse flowCtrlInfo's dataLimit failure: %s", err_info.c_str());
        }
      } break;

      default:
        break;
    }
  }
  return true;
}

bool FlowCtrlRuleHandler::parseDataLimit(string& err_info, const rapidjson::Value& root,
                                         vector<FlowCtrlItem>& flowctrl_items) {
  int32_t type_val;
  stringstream ss;
  string attr_sep = delimiter::kDelimiterColon;
  if (!parseIntMember(err_info, root, "type", type_val, true, 0)) {
    ss << "Decode Failure: ";
    ss << err_info;
    ss << " of type field in parse data limit!";
    err_info = ss.str();
    return false;
  }
  // check rule type
  if (!root.HasMember("rule")) {
    err_info = "rule field not existed";
    return false;
  }
  if (!root["rule"].IsArray()) {
    err_info = "Illegal value, rule must be list type";
    return false;
  }
  // parse rule info
  const rapidjson::Value& obj_set = root["rule"];
  for (uint32_t index = 0; index < obj_set.Size(); index++) {
    int32_t start_time = 0;
    int32_t end_time = 0;
    int64_t datadlt_m = 0;
    int64_t datasize_limit = 0;
    int32_t freqms_limit = 0;
    const rapidjson::Value& node_item = obj_set[index];
    if (!node_item.IsObject()) {
      err_info = "Illegal rule'value item, must be dict type";
      return false;
    }
    if (!parseTimeMember(err_info, node_item, "start", start_time)) {
      return false;
    }
    if (!parseTimeMember(err_info, node_item, "end", end_time)) {
      return false;
    }
    if (start_time > end_time) {
      ss << "start value must lower than the End value in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    if (!parseLongMember(err_info, node_item, "dltInM", datadlt_m, false, -1)) {
      ss << "dltInM key is required in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    if (datadlt_m <= 20) {
      ss << "dltInM value must over than 20 in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    datadlt_m = datadlt_m * 1024 * 1024;
    if (!parseLongMember(err_info, node_item, "limitInM", datasize_limit, false, -1)) {
      ss << "limitInM key is required in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    if (datasize_limit < 0) {
      ss << "limitInM value must over than equal or bigger than zero in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    datasize_limit = datasize_limit * 1024 * 1024;
    if (!parseIntMember(err_info, node_item, "freqInMs", freqms_limit, false, -1)) {
      ss << "freqInMs key is required in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    if (freqms_limit < 200) {
      ss << "freqInMs value must over than equal or bigger than 200 in index(";
      ss << index;
      ss << ") of data limit rule!";
      err_info = ss.str();
      return false;
    }
    FlowCtrlItem flowctrl_item(0, start_time, end_time, datadlt_m, datasize_limit, freqms_limit);
    flowctrl_items.push_back(flowctrl_item);
  }
  if (!flowctrl_items.empty()) {
    std::sort(flowctrl_items.begin(), flowctrl_items.end(), compareDataLimitQueue);
  }
  err_info = "Ok";
  return true;
}

bool FlowCtrlRuleHandler::parseFreqLimit(string& err_info, const rapidjson::Value& root,
                                         vector<FlowCtrlItem>& flowctrl_items) {
  int32_t type_val;
  stringstream ss;

  if (!parseIntMember(err_info, root, "type", type_val, true, 1)) {
    ss << "Decode Failure: ";
    ss << err_info;
    ss << " of type field in parse freq limit!";
    err_info = ss.str();
    return false;
  }
  if (!root.HasMember("rule")) {
    err_info = "rule field not existed";
    return false;
  }
  if (!root["rule"].IsArray()) {
    err_info = "Illegal value, rule must be list type";
    return false;
  }
  // parse rule info
  const rapidjson::Value& obj_set = root["rule"];
  for (uint32_t i = 0; i < obj_set.Size(); i++) {
    int32_t zeroCnt = -2;
    int32_t freqms_limit = -2;
    const rapidjson::Value& node_item = obj_set[i];
    if (!node_item.IsObject()) {
      err_info = "Illegal rule'value item, must be dict type";
      return false;
    }
    if (!parseIntMember(err_info, node_item, "zeroCnt", zeroCnt, false, -2)) {
      ss << "Decode Failure: ";
      ss << err_info;
      ss << " of zeroCnt field in parse freq limit!";
      err_info = ss.str();
      return false;
    }
    if (!parseIntMember(err_info, node_item, "freqInMs", freqms_limit, false, -2)) {
      ss << "Decode Failure: ";
      ss << err_info;
      ss << " of freqInMs field in parse freq limit!";
      err_info = ss.str();
      return false;
    }
    FlowCtrlItem flowctrl_item(1, zeroCnt, freqms_limit);
    flowctrl_items.push_back(flowctrl_item);
  }
  if (!flowctrl_items.empty()) {
    std::sort(flowctrl_items.begin(), flowctrl_items.end(), compareFeqQueue);
  }
  err_info = "Ok";
  return true;
}

bool FlowCtrlRuleHandler::parseLowFetchLimit(string& err_info, const rapidjson::Value& root,
                                             vector<FlowCtrlItem>& flowctrl_items) {
  int32_t type_val;
  stringstream ss;
  if (!parseIntMember(err_info, root, "type", type_val, true, 3)) {
    ss << "Decode Failure: ";
    ss << err_info;
    ss << " of type field in parse low fetch limit!";
    err_info = ss.str();
    return false;
  }
  if (!root.HasMember("rule")) {
    err_info = "rule field not existed";
    return false;
  }
  if (!root["rule"].IsArray()) {
    err_info = "Illegal value, rule must be list type";
    return false;
  }
  // parse rule info
  const rapidjson::Value& nodes = root["rule"];
  for (uint32_t i = 0; i < nodes.Size(); ++i) {
    int32_t norm_freq_ms = 0;
    int32_t filter_freq_ms = 0;
    int32_t min_filter_freq_ms = 0;
    FlowCtrlItem flowctrl_item;
    const rapidjson::Value& node_item = nodes[i];
    if (!node_item.IsObject()) {
      err_info = "Illegal rule'value item, must be dict type";
      return false;
    }
    if (node_item.HasMember("filterFreqInMs") || node_item.HasMember("minDataFilterFreqInMs")) {
      if (!parseIntMember(err_info, node_item, "filterFreqInMs", filter_freq_ms, false, -1)) {
        ss << "Decode Failure: ";
        ss << err_info;
        ss << " of filterFreqInMs field in parse low fetch limit!";
        err_info = ss.str();
        return false;
      }
      if (!parseIntMember(err_info, node_item, "minDataFilterFreqInMs", min_filter_freq_ms, false,
                          -1)) {
        ss << "Decode Failure: ";
        ss << err_info;
        ss << " of minDataFilterFreqInMs field in parse low fetch limit!";
        err_info = ss.str();
        return false;
      }
      if (filter_freq_ms < 0 || filter_freq_ms > 300000) {
        ss << "Decode Failure: ";
        ss << "filterFreqInMs value must in [0, 300000] in index(";
        ss << i;
        ss << ") of low fetch limit rule!";
        err_info = ss.str();
        return false;
      }
      if (min_filter_freq_ms < 0 || min_filter_freq_ms > 300000) {
        ss << "Decode Failure: ";
        ss << "minDataFilterFreqInMs value must in [0, 300000] in index(";
        ss << i;
        ss << ") of low fetch limit rule!";
        err_info = ss.str();
        return false;
      }
      if (min_filter_freq_ms < filter_freq_ms) {
        ss << "Decode Failure: ";
        ss << "minDataFilterFreqInMs must lower than filterFreqInMs in index(";
        ss << i;
        ss << ") of low fetch limit rule!";
        err_info = ss.str();
        return false;
      }
    }
    if (node_item.HasMember("normFreqInMs")) {
      if (!parseIntMember(err_info, node_item, "normFreqInMs", norm_freq_ms, false, -1)) {
        ss << "Decode Failure: ";
        ss << err_info;
        ss << " of normFreqInMs field in parse low fetch limit!";
        err_info = ss.str();
        return false;
      }
      if (norm_freq_ms < 0 || norm_freq_ms > 300000) {
        ss << "Decode Failure: ";
        ss << "normFreqInMs value must in [0, 300000] in index(";
        ss << i;
        ss << ") of low fetch limit rule!";
        err_info = ss.str();
        return false;
      }
    }
    flowctrl_item.ResetFlowCtrlValue(3, norm_freq_ms, filter_freq_ms, min_filter_freq_ms);
    flowctrl_items.push_back(flowctrl_item);
  }
  err_info = "Ok";
  return true;
}

bool FlowCtrlRuleHandler::parseStringMember(string& err_info, const rapidjson::Value& root,
                                            const char* key, string& value, bool compare_value,
                                            string required_val) {
  // check key if exist
  if (!root.HasMember(key)) {
    err_info = "Field not existed";
    return false;
  }
  if (!root[key].IsString()) {
    err_info = "Illegal value, must be string type";
    return false;
  }

  if (compare_value) {
    if (root[key].GetString() != required_val) {
      err_info = "Illegal value, not required value content";
      return false;
    }
  }
  value = root[key].GetString();
  return true;
}

bool FlowCtrlRuleHandler::parseLongMember(string& err_info, const rapidjson::Value& root,
                                          const char* key, int64_t& value, bool compare_value,
                                          int64_t required_val) {
  if (!root.HasMember(key)) {
    err_info = "Field not existed";
    return false;
  }
  if (!root[key].IsNumber()) {
    err_info = "Illegal value, must be number type";
    return false;
  }
  if (compare_value) {
    if ((int64_t)root[key].GetInt64() != required_val) {
      err_info = "Illegal value, not required value content";
      return false;
    }
  }
  value = (int64_t)root[key].GetInt64();
  return true;
}

bool FlowCtrlRuleHandler::parseIntMember(string& err_info, const rapidjson::Value& root,
                                         const char* key, int32_t& value, bool compare_value,
                                         int32_t required_val) {
  if (!root.HasMember(key)) {
    err_info = "Field not existed";
    return false;
  }
  if (!root[key].IsInt()) {
    err_info = "Illegal value, must be int type";
    return false;
  }
  if (compare_value) {
    if (root[key].GetInt() != required_val) {
      err_info = "Illegal value, not required value content";
      return false;
    }
  }
  value = root[key].GetInt();
  return true;
}

bool FlowCtrlRuleHandler::parseTimeMember(string& err_info, const rapidjson::Value& root,
                                          const char* key, int32_t& value) {
  // check key if exist
  stringstream ss;
  if (!root.HasMember(key)) {
    ss << "field ";
    ss << key;
    ss << " not existed!";
    err_info = ss.str();
    return false;
  }
  if (!root[key].IsString()) {
    ss << "field ";
    ss << key;
    ss << " must be string type!";
    err_info = ss.str();
    return false;
  }
  string::size_type pos1;
  string str_value = root[key].GetString();
  string attr_sep = delimiter::kDelimiterColon;
  pos1 = str_value.find(attr_sep);
  if (string::npos == pos1) {
    ss << "field ";
    ss << key;
    ss << " must be 'aa:bb' and 'aa','bb' must be int value format!";
    err_info = ss.str();
    return false;
  }
  string sub_str_1 = str_value.substr(0, pos1);
  string sub_str_2 = str_value.substr(pos1 + attr_sep.size(), str_value.size());
  int32_t in_hour = atoi(sub_str_1.c_str());
  int32_t in_minute = atoi(sub_str_2.c_str());
  if (in_hour < 0 || in_hour > 24) {
    ss << "field ";
    ss << key;
    ss << " -hour value must in [0,23]!";
    err_info = ss.str();
    return false;
  }
  if (in_minute < 0 || in_minute > 59) {
    ss << "field ";
    ss << key;
    ss << " -minute value must in [0,59]!";
    err_info = ss.str();
    return false;
  }
  value = in_hour * 100 + in_minute;
  return true;
}

}  // namespace tubemq
