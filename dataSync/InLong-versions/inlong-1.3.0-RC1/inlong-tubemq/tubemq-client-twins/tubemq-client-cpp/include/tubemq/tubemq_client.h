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

#ifndef TUBEMQ_CLIENT_HEADER_H_
#define TUBEMQ_CLIENT_HEADER_H_

#include <stdint.h>
#include <map>
#include <string>
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_message.h"
#include "tubemq/tubemq_return.h"


namespace tubemq {

bool StartTubeMQService(string& err_info,
  const TubeMQServiceConfig& serviceConfig);
// Deprecated method
bool StartTubeMQService(string& err_info,
  const string& conf_file = "../conf/client.conf");
bool StopTubeMQService(string& err_info);


class TubeMQConsumer {
 public:
  TubeMQConsumer();
  ~TubeMQConsumer();
  bool Start(string& err_info, const ConsumerConfig& config);
  virtual void ShutDown();
  const int32_t GetClientId() const { return client_id_; }
  // GetMessage() use note:
  // This GetMessage have a blocking situation: when the current
  // consumer consumption situation is not satisfied (including
  // without partitions to consumption, or allocated partitions but
  // the partitions do not meet the consumption situation),
  // the call will sleep at intervals of ConsumerConfig.GetPartCheckSliceMs(),
  // until the total time of ConsumerConfig.GetMaxPartCheckPeriodMs
  bool GetMessage(ConsumerResult& result);
  bool Confirm(const string& confirm_context, bool is_consumed, ConsumerResult& result);
  bool GetCurConsumedInfo(map<string, ConsumeOffsetInfo>& consume_info_map);

 private:
  int32_t client_id_;
  AtomicInteger status_;
};


}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_HEADER_H_

