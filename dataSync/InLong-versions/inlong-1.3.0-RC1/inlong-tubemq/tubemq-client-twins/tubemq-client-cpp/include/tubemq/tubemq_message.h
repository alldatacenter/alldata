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

#ifndef TUBEMQ_CLIENT_MESSAGE_H_
#define TUBEMQ_CLIENT_MESSAGE_H_

#include <stdint.h>
#include <stdio.h>

#include <map>
#include <vector>
#include <string>

namespace tubemq {

using std::map;
using std::vector;
using std::string;

class Message {
 public:
  Message();
  Message(const Message& target);
  Message(const string& topic, const char* data, uint32_t datalen);
  Message(const string& topic, int32_t flag, int64_t message_id,
    const char* data, uint32_t datalen, const map<string, string>& properties);
  virtual ~Message();
  Message& operator=(const Message& target);
  const int64_t GetMessageId() const;
  void SetMessageId(int64_t message_id);
  const string& GetTopic() const;
  void SetTopic(const string& topic);
  const char* GetData() const;
  vector<char> GetVectorData() const;
  uint32_t GetDataLength() const;
  void setData(const char* data, uint32_t datalen);
  const int32_t GetFlag() const;
  void SetFlag(int32_t flag);
  const map<string, string>& GetProperties() const;
  int32_t GetPropertie(string& attribute);
  bool HasProperty(const string& key);
  bool GetProperty(const string& key, string& value);
  bool GetFilterItem(string& value);
  bool AddProperty(string& err_info, const string& key, const string& value);

 private:
  void clearData();
  void copyData(const char* data, uint32_t datalen);
  void copyProperties(const map<string, string>& properties);

 private:
  string topic_;
  char* data_;
  uint32_t datalen_;
  int64_t message_id_;
  int32_t flag_;
  map<string, string> properties_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_MESSAGE_H_
