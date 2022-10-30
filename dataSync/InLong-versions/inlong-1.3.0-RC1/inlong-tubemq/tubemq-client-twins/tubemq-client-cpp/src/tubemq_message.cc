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

#include "tubemq/tubemq_message.h"

#include <string.h>
#include <sstream>

#include "const_config.h"
#include "utils.h"

namespace tubemq {

using std::stringstream;

Message::Message() {
  flag_ = 0;
  message_id_ = tb_config::kInvalidValue;
  data_ = NULL;
  datalen_ = 0;
}

Message::Message(const Message& target) {
  topic_ = target.topic_;
  message_id_ = target.message_id_;
  copyData(target.data_, target.datalen_);
  copyProperties(target.properties_);
  flag_ = target.flag_;
}

Message::Message(const string& topic, const char* data, uint32_t datalen) {
  topic_ = topic;
  flag_ = 0;
  message_id_ = tb_config::kInvalidValue;
  copyData(data, datalen);
  properties_.clear();
}

Message::Message(const string& topic, int32_t flag,
  int64_t message_id, const char* data, uint32_t datalen,
  const map<string, string>& properties) {
  topic_ = topic;
  flag_ = flag;
  message_id_ = message_id;
  copyData(data, datalen);
  copyProperties(properties);
}

Message::~Message() { clearData(); }

Message& Message::operator=(const Message& target) {
  if (this == &target) return *this;
  topic_ = target.topic_;
  message_id_ = target.message_id_;
  clearData();
  copyData(target.data_, target.datalen_);
  copyProperties(target.properties_);
  flag_ = target.flag_;
  return *this;
}

const int64_t Message::GetMessageId() const { return message_id_; }

void Message::SetMessageId(int64_t message_id) { message_id_ = message_id; }

const string& Message::GetTopic() const { return topic_; }

void Message::SetTopic(const string& topic) { topic_ = topic; }

const char* Message::GetData() const { return data_; }

vector<char> Message::GetVectorData() const {
  vector<char> vector_data(data_, data_ + datalen_);
  return vector_data;
}

uint32_t Message::GetDataLength() const { return datalen_; }

void Message::setData(const char* data, uint32_t datalen) {
  clearData();
  copyData(data, datalen);
}

const int32_t Message::GetFlag() const { return flag_; }

void Message::SetFlag(int32_t flag) { flag_ = flag; }

const map<string, string>& Message::GetProperties() const { return properties_; }

int32_t Message::GetPropertie(string& attribute) {
  attribute.clear();
  map<string, string>::iterator it_map;
  for (it_map = properties_.begin(); it_map != properties_.end(); ++it_map) {
    if (!attribute.empty()) {
      attribute += delimiter::kDelimiterComma;
    }
    attribute += it_map->first;
    attribute += delimiter::kDelimiterEqual;
    attribute += it_map->second;
  }
  return attribute.length();
}

bool Message::HasProperty(const string& key) {
  map<string, string>::iterator it_map;
  string trimed_key = Utils::Trim(key);
  if (!trimed_key.empty()) {
    it_map = properties_.find(trimed_key);
    if (it_map != properties_.end()) {
      return true;
    }
  }
  return false;
}

bool Message::GetProperty(const string& key, string& value) {
  map<string, string>::iterator it_map;
  string trimed_key = Utils::Trim(key);
  if (!trimed_key.empty()) {
    it_map = properties_.find(trimed_key);
    if (it_map != properties_.end()) {
      value = it_map->second;
      return true;
    }
  }
  return false;
}

bool Message::GetFilterItem(string& value) {
  return GetProperty(tb_config::kRsvPropKeyFilterItem, value);
}

bool Message::AddProperty(string& err_info, const string& key, const string& value) {
  string trimed_key = Utils::Trim(key);
  string trimed_value = Utils::Trim(value);
  if (trimed_key.empty() || trimed_value.empty()) {
    err_info = "Not allowed null value of parmeter key or value";
    return false;
  }
  if ((string::npos != trimed_key.find(delimiter::kDelimiterComma)) ||
      (string::npos != trimed_key.find(delimiter::kDelimiterEqual))) {
    stringstream ss;
    ss << "Reserved token '";
    ss << delimiter::kDelimiterComma;
    ss << "' or '";
    ss << delimiter::kDelimiterEqual;
    ss << "' in parmeter key!";
    err_info = ss.str();
    return false;
  }
  if ((string::npos != trimed_value.find(delimiter::kDelimiterComma)) ||
      (string::npos != trimed_value.find(delimiter::kDelimiterEqual))) {
    stringstream ss;
    ss << "Reserved token '";
    ss << delimiter::kDelimiterComma;
    ss << "' or '";
    ss << delimiter::kDelimiterEqual;
    ss << "' in parmeter value!";
    err_info = ss.str();
    return false;
  }
  if (trimed_key == tb_config::kRsvPropKeyFilterItem
    || trimed_key == tb_config::kRsvPropKeyMsgTime) {
    stringstream ss;
    ss << "Reserved token '";
    ss << tb_config::kRsvPropKeyFilterItem;
    ss << "' or '";
    ss << tb_config::kRsvPropKeyMsgTime;
    ss << "' must not be used in parmeter key!";
    err_info = ss.str();
    return false;
  }
  // add key and value
  properties_[trimed_key] = trimed_value;
  if (!properties_.empty()) {
    flag_ |= tb_config::kMsgFlagIncProperties;
  }
  err_info = "Ok";
  return true;
}

void Message::clearData() {
  if (data_ != NULL) {
    delete[] data_;
    data_ = NULL;
    datalen_ = 0;
  }
}

void Message::copyData(const char* data, uint32_t datalen) {
  if (data == NULL) {
    data_ = NULL;
    datalen_ = 0;
  } else {
    datalen_ = datalen;
    data_ = new char[datalen + 1];
    memset(data_, 0, datalen + 1);
    memcpy(data_, data, datalen);
  }
}

void Message::copyProperties(const map<string, string>& properties) {
  properties_.clear();
  map<string, string>::const_iterator it_map;
  for (it_map = properties.begin(); it_map != properties.end(); ++it_map) {
    properties_[it_map->first] = it_map->second;
  }
  if (!properties_.empty()) {
    flag_ |= tb_config::kMsgFlagIncProperties;
  }
}

}  // namespace tubemq
