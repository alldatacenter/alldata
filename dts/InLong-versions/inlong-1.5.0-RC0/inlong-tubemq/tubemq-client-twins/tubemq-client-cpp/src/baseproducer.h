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

#ifndef TUBEMQ_CLIENT_IMP_PRODUCER_API_H_
#define TUBEMQ_CLIENT_IMP_PRODUCER_API_H_

#include <list>
#include <mutex>
#include <set>
#include <unordered_map>

#include "BrokerService.pb.h"
#include "MasterService.pb.h"
#include "RPC.pb.h"
#include "client_service.h"
#include "partition_router.h"
#include "tubemq/tubemq_atomic.h"
#include "tubemq_codec.h"

namespace tubemq {

class BaseProducer : public BaseClient, public std::enable_shared_from_this<BaseProducer> {
 public:
  BaseProducer();
  ~BaseProducer();

  bool Start(string& err_info, const ProducerConfig& config);
  void ShutDown();

  bool Publish(string& err_info, const string& topic);
  bool Publish(string& err_info, const set<string>& topic_list);

  bool IsTopicCurAcceptPublish(const string& topic);
  const set<string>& GetPublishedTopicSet();

  bool SendMessage(string& err_info, const Message& message, bool is_sync,
                   const std::function<void(const ErrorCode&)>& callback);

  // methods for interacting with tubemq server
 private:
  bool register2Master(int32_t& error_code, string& err_info, bool need_change);
  void heartBeat2Master();
  void close2Master();

  // private util methods
 private:
  string buildUUID();
  bool isClientRunning();
  bool initMasterAddress(string& err_info, const string& master_info);
  void getNextMasterAddr(string& ipaddr, int32_t& port);
  void getCurrentMasterAddr(string& ipaddr, int32_t& port);
  bool needGenMasterCertificateInfo(bool force);
  void genMasterAuthenticateToken(AuthenticateInfo* pauthinfo, const string& username,
                                  const string usrpassword);
  void genBrokerAuthenticInfo(AuthorizedInfo* p_authInfo, bool force);
  void processAuthorizedToken(const MasterAuthorizedInfo& authorized_token_info);
  void showNodeInfo(const NodeInfo& node_info);
  Partition selectPartition(const Message& message);
  void getAllowedPartitions();
  const char* encodePayload(BufferPtr buffer, const Message& message);

  // methods for build and process protobuf mssages
 private:
  void buildRegisterRequestP2M(TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buildHeartRequestP2M(TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buildCloseRequestP2M(TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buildSendMessageRequestP2B(const Partition& partition, const Message& message,
                                  TubeMQCodec::ReqProtocolPtr& req_protocol);
  bool processRegisterResponseM2P(int32_t& error_code, string& err_info,
                                  const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  bool processHBResponseM2P(int32_t& error_code, string& err_info,
                            const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  bool processSendMessageResponseB2P(int32_t& error_code, string& err_info,
                                     const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  void updateBrokerInfoList(bool is_register, const vector<string>& pkg_broker_infos,
                            int64_t pkg_checksum);
  void updateTopicConfigure(const std::vector<std::string>& str_topic_infos);

 private:
  string client_uuid_;
  AtomicInteger status_;
  ProducerConfig config_;
  map<string, int32_t> masters_map_;
  mutex brokers_map_lock_;
  map<string, NodeInfo> brokers_map_;
  AtomicBoolean is_master_actived_;
  AtomicInteger master_reg_status_;
  int32_t master_sh_retry_cnt_;
  string curr_master_addr_;
  int64_t broker_info_checksum_;
  SteadyTimerPtr heart_beat_timer_;
  AtomicInteger master_hb_status_;
  mutex pub_topic_list_lock_;
  std::unordered_map<string, uint64_t> pub_topic_list_;
  mutex topic_partition_map_lock_;
  std::unordered_map<std::string, std::unordered_map<int, std::vector<Partition> > >
      topic_partition_map_;
  RoundRobinPartitionRouter partition_router_;
  AtomicLong visit_token_;
  mutable mutex auth_lock_;
  string authorized_info_;
  AtomicBoolean nextauth_2_master;
  AtomicBoolean nextauth_2_broker;
};

using BaseProducerPtr = std::shared_ptr<BaseProducer>;

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_IMP_PRODUCER_API_H_
