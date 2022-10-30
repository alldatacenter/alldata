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

#ifndef TUBEMQ_CLIENT_IMP_CONSUMER_API_H_
#define TUBEMQ_CLIENT_IMP_CONSUMER_API_H_

#include <stdlib.h>

#include <list>
#include <mutex>
#include <string>

#include "BrokerService.pb.h"
#include "MasterService.pb.h"
#include "RPC.pb.h"
#include "client_service.h"
#include "client_subinfo.h"
#include "meta_info.h"
#include "rmt_data_cache.h"
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_message.h"
#include "tubemq/tubemq_return.h"
#include "tubemq_codec.h"

namespace tubemq {

using std::mutex;
using std::string;

class BaseConsumer : public BaseClient, public std::enable_shared_from_this<BaseConsumer> {
 public:
  BaseConsumer();
  ~BaseConsumer();
  bool Start(string& err_info, const ConsumerConfig& config);
  virtual void ShutDown();
  bool GetMessage(ConsumerResult& result);
  bool Confirm(const string& confirm_context, bool is_consumed, ConsumerResult& result);
  bool GetCurConsumedInfo(map<string, ConsumeOffsetInfo>& consume_info_map);

 private:
  bool register2Master(int32_t& error_code, string& err_info, bool need_change);
  void heartBeat2Master();
  void processRebalanceEvent();
  void close2Master();
  void closeAllBrokers();

 private:
  string buildUUID();
  bool isClientRunning();
  bool IsConsumeReady(ConsumerResult& result);
  int32_t getConsumeReadStatus(bool is_first_reg);
  bool initMasterAddress(string& err_info, const string& master_info);
  void getNextMasterAddr(string& ipaddr, int32_t& port);
  void getCurrentMasterAddr(string& ipaddr, int32_t& port);
  bool needGenMasterCertificateInfo(bool force);
  void genBrokerAuthenticInfo(AuthorizedInfo* p_authInfo, bool force);
  void processAuthorizedToken(const MasterAuthorizedInfo& authorized_token_info);
  void addBrokerHBTimer(const NodeInfo broker);
  void reSetBrokerHBTimer(const NodeInfo broker);

 private:
  void buidRegisterRequestC2M(TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidHeartRequestC2M(TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidCloseRequestC2M(TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidRegisterRequestC2B(const PartitionExt& partition,
                              TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidUnRegRequestC2B(const PartitionExt& partition,
                           TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidHeartBeatC2B(const list<PartitionExt>& partitions,
                        TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidGetMessageC2B(const PartitionExt& partition, TubeMQCodec::ReqProtocolPtr& req_protocol);
  void buidCommitC2B(const PartitionExt& partition, bool is_last_consumed,
                     TubeMQCodec::ReqProtocolPtr& req_protocol);
  void genMasterAuthenticateToken(AuthenticateInfo* pauthinfo, const string& username,
                                  const string usrpassword);
  bool processRegisterResponseM2C(int32_t& error_code, string& err_info,
                                  const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  bool processHBResponseM2C(int32_t& error_code, string& err_info,
                            const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  void processDisConnect2Broker(ConsumerEvent& event);
  void processConnect2Broker(ConsumerEvent& event);
  void unregister2Brokers(map<NodeInfo, list<PartitionExt> >& unreg_partitions, bool wait_rsp);
  bool processRegResponseB2C(int32_t& error_code, string& err_info,
                             const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  void processHeartBeat2Broker(NodeInfo broker_info);
  bool processGetMessageRspB2C(ConsumerResult& result, PeerInfo& peer_info, bool filter_consume,
                               const PartitionExt& partition_ext, const string& confirm_context,
                               const TubeMQCodec::RspProtocolPtr& rsp_protocol);
  void convertMessages(int32_t& msg_size, list<Message>& message_list, bool filter_consume,
                       const string& topic_name, GetMessageResponseB2C& rsp_b2c);
  inline int32_t nextHeartBeatPeriodms();
  void asyncRegister2Master(bool need_change);

 private:
  int32_t client_indexid_;
  string client_uuid_;
  AtomicInteger status_;
  ConsumerConfig config_;
  ClientSubInfo sub_info_;
  RmtDataCacheCsm rmtdata_cache_;
  AtomicLong visit_token_;
  mutable mutex auth_lock_;
  string authorized_info_;
  AtomicBoolean nextauth_2_master;
  AtomicBoolean nextauth_2_broker;
  string curr_master_addr_;
  map<string, int32_t> masters_map_;
  AtomicBoolean is_master_actived_;
  AtomicInteger master_reg_status_;
  int32_t master_sh_retry_cnt_;
  int64_t last_master_hbtime_;
  int32_t unreport_times_;
  // master heartbeat timer
  SteadyTimerPtr heart_beat_timer_;
  AtomicInteger master_hb_status_;
  std::shared_ptr<std::thread> rebalance_thread_ptr_;
  // broker heartbeat timer
  mutable mutex broker_timer_lock_;
  map<NodeInfo, SteadyTimerPtr> broker_timer_map_;
};
using BaseConsumerPtr = std::shared_ptr<BaseConsumer>;
}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_IMP_CONSUMER_API_H_

