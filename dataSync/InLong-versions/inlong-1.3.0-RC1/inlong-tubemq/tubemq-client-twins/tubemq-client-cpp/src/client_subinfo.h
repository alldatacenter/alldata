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

#ifndef TUBEMQ_CLIENT_SUBINFO_H_
#define TUBEMQ_CLIENT_SUBINFO_H_

#include <stdint.h>
#include <list>
#include <map>
#include <mutex>
#include <set>
#include <string>
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_config.h"

namespace tubemq {

using std::list;
using std::map;
using std::set;
using std::string;


class ClientSubInfo {
 public:
  ClientSubInfo();
  void SetConsumeTarget(const ConsumerConfig& config);
  bool CompAndSetNotAllocated(bool expect, bool update);
  void BookRegistered() { is_registered_.Set(true); }
  bool IsBoundConsume() const { return bound_consume_; }
  bool IsNotAllocated() const { return not_allocated_.Get(); }
  const int64_t GetSubscribedTime() const { return subscribed_time_; }
  const string& GetSessionKey() const { return session_key_; }
  const uint32_t GetSourceCnt() const { return source_count_; }
  bool SelectBig() { return select_big_; }
  bool IsFilterConsume(const string& topic);
  void GetAssignedPartOffset(const string& partition_key, int64_t& offset);
  const string& GetBoundPartInfo() const { return bound_partions_; }
  const list<string>& GetSubTopics() const { return topics_; }
  const list<string>& GetTopicConds() const { return topic_conds_; }
  const map<string, set<string> >& GetTopicFilterMap() const;

 private:
  bool bound_consume_;
  AtomicBoolean is_registered_;
  AtomicBoolean not_allocated_;
  int64_t  subscribed_time_;
  map<string, set<string> > topic_and_filter_map_;
  list<string> topics_;
  list<string> topic_conds_;
  map<string, bool> topic_filter_map_;
  // bound info
  string session_key_;
  uint32_t source_count_;
  bool select_big_;
  map<string, int64_t> assigned_part_map_;
  string bound_partions_;
};

}  // namespace tubemq


#endif  // TUBEMQ_CLIENT_SUBINFO_H_
