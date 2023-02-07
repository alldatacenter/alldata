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

#include "tubemq/tubemq_client.h"

#include <signal.h>
#include <unistd.h>

#include <sstream>

#include "baseconsumer.h"
#include "baseproducer.h"
#include "client_service.h"
#include "const_config.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_errcode.h"

namespace tubemq {

using std::lock_guard;
using std::stringstream;

bool StartTubeMQService(string& err_info, const TubeMQServiceConfig& serviceConfig) {
  signal(SIGPIPE, SIG_IGN);
  return TubeMQService::Instance()->Start(err_info, serviceConfig);
}

bool StartTubeMQService(string& err_info, const string& conf_file) {
  signal(SIGPIPE, SIG_IGN);
  return TubeMQService::Instance()->Start(err_info, conf_file);
}

bool StopTubeMQService(string& err_info) {
  int32_t count = TubeMQService::Instance()->GetClientObjCnt();
  if (count > 0) {
    stringstream ss;
    ss << "Check found ";
    ss << count;
    ss << " clients not shutdown, please shutdown clients first!";
    err_info = ss.str();
    return false;
  }
  return TubeMQService::Instance()->Stop(err_info);
}

TubeMQConsumer::TubeMQConsumer() {
  client_id_ = tb_config::kInvalidValue;
  status_.Set(0);
}

TubeMQConsumer::~TubeMQConsumer() { ShutDown(); }

bool TubeMQConsumer::Start(string& err_info, const ConsumerConfig& config) {
  if (!TubeMQService::Instance()->IsRunning()) {
    err_info = "TubeMQ Service not startted!";
    return false;
  }
  // check status
  if (!status_.CompareAndSet(0, 1)) {
    err_info = "Duplicated call!";
    return false;
  }
  BaseConsumerPtr rmt_client = std::make_shared<BaseConsumer>();
  if (rmt_client == nullptr) {
    err_info = "No memory for create CONSUMER remote object!";
    status_.CompareAndSet(1, 0);
    return false;
  }
  if (!rmt_client->Start(err_info, config)) {
    rmt_client->ShutDown();
    status_.CompareAndSet(1, 0);
    return false;
  }
  client_id_ = rmt_client->GetClientIndex();
  status_.Set(2);
  err_info = "Ok!";
  return true;
}

void TubeMQConsumer::ShutDown() {
  if (!status_.CompareAndSet(2, 0)) {
    return;
  }
  if (client_id_ != tb_config::kInvalidValue) {
    BaseConsumerPtr rmt_client = std::dynamic_pointer_cast<BaseConsumer>(
        TubeMQService::Instance()->GetClientObj(client_id_));
    if ((rmt_client != nullptr) && (rmt_client->GetClientIndex() == client_id_)) {
      rmt_client->ShutDown();
    }
    client_id_ = tb_config::kInvalidValue;
  }
}

bool TubeMQConsumer::GetMessage(ConsumerResult& result) {
  if (!TubeMQService::Instance()->IsRunning()) {
    result.SetFailureResult(err_code::kErrMQServiceStop, "TubeMQ Service stopped!");
    return false;
  }
  if (status_.Get() != 2) {
    result.SetFailureResult(err_code::kErrClientStop, "TubeMQ Service not startted!");
    return false;
  }
  if (client_id_ == tb_config::kInvalidValue) {
    result.SetFailureResult(err_code::kErrClientStop,
                            "Tube client not call init function, please initial first!");
    return false;
  }

  BaseConsumerPtr rmt_client =
      std::dynamic_pointer_cast<BaseConsumer>(TubeMQService::Instance()->GetClientObj(client_id_));
  if ((rmt_client == nullptr) || (rmt_client->GetClientIndex() != client_id_)) {
    result.SetFailureResult(err_code::kErrBadRequest,
                            "Rmt client CB has been released, please re-start this client");
    return false;
  }
  return rmt_client->GetMessage(result);
}

bool TubeMQConsumer::Confirm(const string& confirm_context, bool is_consumed,
                             ConsumerResult& result) {
  if (!TubeMQService::Instance()->IsRunning()) {
    result.SetFailureResult(err_code::kErrMQServiceStop, "TubeMQ Service stopped!");
    return false;
  }
  if (status_.Get() != 2) {
    result.SetFailureResult(err_code::kErrClientStop, "TubeMQ Service not startted!");
    return false;
  }
  if (client_id_ == tb_config::kInvalidValue) {
    result.SetFailureResult(err_code::kErrClientStop,
                            "Tube client not call init function, please initial first!");
    return false;
  }

  BaseConsumerPtr rmt_client =
      std::dynamic_pointer_cast<BaseConsumer>(TubeMQService::Instance()->GetClientObj(client_id_));
  if ((rmt_client == nullptr) || (rmt_client->GetClientIndex() != client_id_)) {
    result.SetFailureResult(err_code::kErrBadRequest,
                            "Rmt client CB has been released, please re-start this client");
    return false;
  }
  return rmt_client->Confirm(confirm_context, is_consumed, result);
}

bool TubeMQConsumer::GetCurConsumedInfo(map<string, ConsumeOffsetInfo>& consume_info_map) {
  if (!TubeMQService::Instance()->IsRunning()) {
    return false;
  }
  if ((status_.Get() != 2) || (client_id_ == tb_config::kInvalidValue)) {
    return false;
  }

  BaseConsumerPtr rmt_client =
      std::dynamic_pointer_cast<BaseConsumer>(TubeMQService::Instance()->GetClientObj(client_id_));
  if ((rmt_client == nullptr) || (rmt_client->GetClientIndex() != client_id_)) {
    return false;
  }
  return rmt_client->GetCurConsumedInfo(consume_info_map);
}

TubeMQProducer::TubeMQProducer() {
  client_id_ = tb_config::kInvalidValue;
  status_.Set(0);
}

TubeMQProducer::~TubeMQProducer() { ShutDown(); }

bool TubeMQProducer::Start(string& err_info, const ProducerConfig& config) {
  if (!TubeMQService::Instance()->IsRunning()) {
    err_info = "TubeMQ Service not started!";
    return false;
  }

  if (!status_.CompareAndSet(tb_config::kMasterUnRegistered, tb_config::kMasterRegistering)) {
    err_info = "Duplicated call on TubeMQProducer!";
    return false;
  }

  BaseProducerPtr rmt_client = std::make_shared<BaseProducer>();
  if (rmt_client == nullptr) {
    err_info = "No memory for create PRODUCER remote object!";
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }

  if (!rmt_client->Start(err_info, config)) {
    // rmt_client->ShutDown();
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }

  client_id_ = rmt_client->GetClientIndex();
  status_.Set(tb_config::kMasterRegistered);
  err_info = "Ok!";
  return true;
}

void TubeMQProducer::ShutDown() {
  if (!status_.CompareAndSet(tb_config::kMasterRegistered, tb_config::kMasterUnRegistered)) {
    return;
  }
  if (client_id_ != tb_config::kInvalidValue) {
    BaseProducerPtr rmt_client = std::dynamic_pointer_cast<BaseProducer>(
        TubeMQService::Instance()->GetClientObj(client_id_));
    if ((rmt_client != nullptr) && (rmt_client->GetClientIndex() == client_id_)) {
      rmt_client->ShutDown();
    }
    client_id_ = tb_config::kInvalidValue;
  }
}

bool TubeMQProducer::Publish(string& err_info, const std::set<std::string>& topic_list) {
  // if (!status_.CompareAndSet(2, 0)) {
  //   return false;
  // }
  if (client_id_ != tb_config::kInvalidValue) {
    BaseProducerPtr rmt_client = std::dynamic_pointer_cast<BaseProducer>(
        TubeMQService::Instance()->GetClientObj(client_id_));
    if ((rmt_client != nullptr) && (rmt_client->GetClientIndex() == client_id_)) {
      rmt_client->Publish(err_info, topic_list);
    }
    // client_id_ = tb_config::kInvalidValue;
  }
  err_info = "OK";
  return true;
}

bool TubeMQProducer::SendMessage(string& err_info, const Message& message) {
  // if (!status_.CompareAndSet(2, 0)) {
  //   return false;
  // }
  if (client_id_ != tb_config::kInvalidValue) {
    BaseProducerPtr rmt_client = std::dynamic_pointer_cast<BaseProducer>(
        TubeMQService::Instance()->GetClientObj(client_id_));
    if ((rmt_client != nullptr) && (rmt_client->GetClientIndex() == client_id_)) {
      rmt_client->SendMessage(err_info, message, true, [](const ErrorCode&) {});
    }
    // client_id_ = tb_config::kInvalidValue;
  }
  return true;
}

void TubeMQProducer::SendMessage(const Message& message,
                                 const std::function<void(const ErrorCode&)>& callback) {
  if (client_id_ != tb_config::kInvalidValue) {
    BaseProducerPtr rmt_client = std::dynamic_pointer_cast<BaseProducer>(
        TubeMQService::Instance()->GetClientObj(client_id_));
    if ((rmt_client != nullptr) && (rmt_client->GetClientIndex() == client_id_)) {
      string err_info;
      rmt_client->SendMessage(err_info, message, false, callback);
    }
  }
}

}  // namespace tubemq
