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

#include "baseproducer.h"

#include <unistd.h>

#include <iostream>
#include <sstream>

#include "const_config.h"
#include "const_rpc.h"
#include "singleton.h"
#include "transport.h"
#include "tubemq/tubemq_errcode.h"
#include "tubemq_transport.h"
#include "utils.h"
#include "version.h"

namespace tubemq {

using std::stringstream;

BaseProducer::BaseProducer() : BaseClient(true) {
  status_.Set(0);
  masters_map_.clear();
  is_master_actived_.Set(false);
  master_reg_status_.Set(0);
  master_hb_status_.Set(0);
  broker_info_checksum_ = -1;
  master_sh_retry_cnt_ = 0;
}

BaseProducer::~BaseProducer() {}

bool BaseProducer::Start(string& err_info, const ProducerConfig& config) {
  if (!status_.CompareAndSet(tb_config::kMasterUnRegistered, tb_config::kMasterRegistering)) {
    err_info = "Ok";
    return true;
  }

  // check master addr info
  if (config.GetMasterAddrInfo().length() == 0) {
    err_info = "Parameter error: not set master address info!";
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }

  if (!TubeMQService::Instance()->IsRunning()) {
    err_info = "TubeMQ Service not startted!";
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }

  // add to TubeMQService
  if (!TubeMQService::Instance()->AddClientObj(err_info, shared_from_this())) {
    printf("Add to tubemq service failed!!!\n");
    client_index_ = tb_config::kInvalidValue;
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }

  config_ = config;
  if (!initMasterAddress(err_info, config.GetMasterAddrInfo())) {
    TubeMQService::Instance()->RmvClientObj(shared_from_this());
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }

  // set client_uuid_
  client_uuid_ = buildUUID();

  // producer register to master
  int32_t error_code;
  if (!register2Master(error_code, err_info, false)) {
    TubeMQService::Instance()->RmvClientObj(shared_from_this());
    status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterUnRegistered);
    return false;
  }
  // register2Master done, change status_ to `2`
  status_.CompareAndSet(tb_config::kMasterRegistering, tb_config::kMasterRegistered);

  // set heartbeat timer
  heart_beat_timer_ = TubeMQService::Instance()->CreateTimer();
  heart_beat_timer_->expires_after(std::chrono::milliseconds(config_.GetHeartbeatPeriodMs() / 2));
  auto self = shared_from_this();
  heart_beat_timer_->async_wait([self, this](const std::error_code& ec) {
    if (ec) {
      return;
    }
    heartBeat2Master();
  });

  return true;
}

void BaseProducer::ShutDown() {
  if (!status_.CompareAndSet(tb_config::kMasterRegistered, tb_config::kMasterUnRegistered)) {
    return;
  }

  close2Master();

  TubeMQService::Instance()->RmvClientObj(shared_from_this());
  client_index_ = tb_config::kInvalidValue;
}

bool BaseProducer::Publish(string& err_info, const string& topic) {
  std::lock_guard<std::mutex> lck(pub_topic_list_lock_);
  pub_topic_list_[topic] = 1;
  err_info = "OK";
  return true;
}

bool BaseProducer::Publish(string& err_info, const set<string>& topic_list) {
  std::lock_guard<std::mutex> lck(pub_topic_list_lock_);
  for (const string& topic_name : topic_list) {
    pub_topic_list_[topic_name] = 1;
  }
  err_info = "OK";
  return true;
}

bool BaseProducer::SendMessage(string& err_info, const Message& message, bool is_sync,
                               const std::function<void(const ErrorCode&)>& callback) {
  Partition partition = selectPartition(message);
  std::string broker_id = std::to_string(partition.GetBrokerId());

  auto request = std::make_shared<RequestContext>();
  TubeMQCodec::ReqProtocolPtr req_protocol = TubeMQCodec::GetReqProtocol();
  buildSendMessageRequestP2B(partition, message, req_protocol);
  string target_ip;
  int target_port;
  getCurrentMasterAddr(target_ip, target_port);
  request->codec_ = std::make_shared<TubeMQCodec>();
  request->ip_ = brokers_map_[broker_id].GetHost();
  request->port_ = brokers_map_[broker_id].GetPort();
  request->timeout_ = config_.GetRpcReadTimeoutMs();
  request->request_id_ = Singleton<UniqueSeqId>::Instance().Next();
  req_protocol->request_id_ = request->request_id_;
  req_protocol->rpc_read_timeout_ = config_.GetRpcReadTimeoutMs();

  if (is_sync) {
    ResponseContext response_context;
    ErrorCode error = SyncRequest(response_context, request, req_protocol);
    if (error.Value() != err_code::kErrSuccess) {
      return false;
    }
    int32_t error_code = 0;
    std::string err_msg;
    auto rsp = any_cast<TubeMQCodec::RspProtocolPtr>(response_context.rsp_);
    bool res = processSendMessageResponseB2P(error_code, err_msg, rsp);
    if (!res) {
      return false;
    }
  } else {
    AsyncRequest(request, req_protocol)
        .AddCallBack([=](ErrorCode error, const ResponseContext& response_context) {
          if (error.Value() == err_code::kErrSuccess) {
            auto rsp = any_cast<TubeMQCodec::RspProtocolPtr>(response_context.rsp_);
            int32_t error_code = 0;
            std::string err_msg;
            bool res = processSendMessageResponseB2P(error_code, err_msg, rsp);
            if (!res) {
              LOG_ERROR("Send Message to Broker successully, but response is not successful!!!");
              std::cout << "*** return from tubemq server, error_code = " << error_code
                        << ", err_msg = " << err_msg << std::endl;
            }
          } else {
            LOG_ERROR("Async Send Message to Broker failed!!!");
          }
          callback(error);
        });
  }

  return true;
}

bool BaseProducer::register2Master(int32_t& error_code, string& err_info, bool need_change) {
  // set regist process status to begin
  if (!master_reg_status_.CompareAndSet(0, 1)) {
    err_info = "register2Master process has began!";
    return false;
  }

  LOG_INFO("[PRODUCER] register2Master process begin:");

  string target_ip;
  int target_port;
  getCurrentMasterAddr(target_ip, target_port);
  bool result = false;
  int retry_count = 0;
  int max_retry_count = masters_map_.size();
  err_info = "Master register failure, no online master service!";
  while (retry_count < max_retry_count) {
    if (!TubeMQService::Instance()->IsRunning()) {
      err_info = "TubeMQ Service is stopped!";
      master_reg_status_.CompareAndSet(1, 0);
      return false;
    }
    // Construct the request
    auto request = std::make_shared<RequestContext>();
    TubeMQCodec::ReqProtocolPtr req_protocol = TubeMQCodec::GetReqProtocol();
    buildRegisterRequestP2M(req_protocol);
    // set parameters
    request->codec_ = std::make_shared<TubeMQCodec>();
    request->ip_ = target_ip;
    request->port_ = target_port;
    request->timeout_ = config_.GetRpcReadTimeoutMs();
    request->request_id_ = Singleton<UniqueSeqId>::Instance().Next();
    req_protocol->request_id_ = request->request_id_;
    req_protocol->rpc_read_timeout_ = config_.GetRpcReadTimeoutMs() - 500;
    // send request sync
    ResponseContext response_context;
    ErrorCode error = SyncRequest(response_context, request, req_protocol);
    LOG_INFO("register2Master response come, error.value is %d", error.Value());
    if (error.Value() == err_code::kErrSuccess) {
      // process response
      auto rsp = any_cast<TubeMQCodec::RspProtocolPtr>(response_context.rsp_);
      result = processRegisterResponseM2P(error_code, err_info, rsp);
      if (result) {
        err_info = "Ok";
        is_master_actived_.Set(true);
        break;
      } else {
        is_master_actived_.Set(false);
      }
    }
    getNextMasterAddr(target_ip, target_port);
    retry_count++;
  }
  if (result) {
    printf("Producer register2Master successfully, the registered mater is: %s:%d\n",
           target_ip.c_str(), target_port);
  }
  return result;
}

void BaseProducer::heartBeat2Master() {
  // check status
  if (!master_hb_status_.CompareAndSet(tb_config::kMasterHBWaiting, tb_config::kMasterHBRunning)) {
    LOG_INFO("check hb process status, heartBeat2Master process has began!");
    return;
  }

  // check the service is running
  if (!TubeMQService::Instance()->IsRunning()) {
    master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
    LOG_INFO("[PRODUCER] heartBeat2Master failure: TubeMQ Service is stopped! client=%s",
             client_uuid_.c_str());
    return;
  }

  // check the status of client, wheter register
  if (!isClientRunning()) {
    master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
    LOG_INFO("[PRODUCER] heartBeat2Master failure: TubeMQ Client stopped! client=%s",
             client_uuid_.c_str());
    return;
  }

  // todo, check master is actived

  string target_ip;
  int target_port;
  getCurrentMasterAddr(target_ip, target_port);
  auto request = std::make_shared<RequestContext>();
  TubeMQCodec::ReqProtocolPtr req_protocol = TubeMQCodec::GetReqProtocol();
  // build heartbeat 2 master request
  buildHeartRequestP2M(req_protocol);
  request->codec_ = std::make_shared<TubeMQCodec>();
  request->ip_ = target_ip;
  request->port_ = target_port;
  request->timeout_ = config_.GetRpcReadTimeoutMs();
  request->request_id_ = Singleton<UniqueSeqId>::Instance().Next();
  req_protocol->request_id_ = request->request_id_;
  req_protocol->rpc_read_timeout_ = config_.GetRpcReadTimeoutMs() - 500;
  // async send request
  AsyncRequest(request, req_protocol)
      .AddCallBack([=](ErrorCode error, const ResponseContext& response_context) {
        if (GetClientIndex() == tb_config::kInvalidValue ||
            !TubeMQService::Instance()->IsRunning() || !isClientRunning()) {
          master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
          return;
        }

        if (error.Value() != err_code::kErrSuccess) {
          master_sh_retry_cnt_++;
          LOG_WARN("[PRODUCER] heartBeat2Master failue to (%s:%d) : %s, client=%s",
                   target_ip.c_str(), target_port, error.Message().c_str(), client_uuid_.c_str());
          if (master_sh_retry_cnt_ >= tb_config::kMaxMasterHBRetryCount) {
            LOG_WARN("[PRODUCER] heartBeat2Master found over max-hb-retry(%d), client=%s",
                     master_sh_retry_cnt_, client_uuid_.c_str());
            master_sh_retry_cnt_ = 0;
            is_master_actived_.Set(false);
            //     asyncRegister2Master(true);
            master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
            return;
          }
        } else {
          // process response
          auto rsp = any_cast<TubeMQCodec::RspProtocolPtr>(response_context.rsp_);
          int32_t error_code = 0;
          std::string error_info;
          bool ret_result = processHBResponseM2P(error_code, error_info, rsp);
          if (ret_result) {
            is_master_actived_.Set(true);
            master_sh_retry_cnt_ = 0;
          } else {
            master_sh_retry_cnt_++;
            if (error_code == err_code::kErrHbNoNode ||
                error_info.find("StandbyException") != string::npos) {
              is_master_actived_.Set(false);
              bool need_change = !(error_code == err_code::kErrHbNoNode);
              LOG_WARN("[PRODUCER] heartBeat2Master found no-node or standby, client=%s, change=%d",
                       client_uuid_.c_str(), need_change);
              //       asyncRegister2Master(need_change);
              master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
              return;
            }
          }
        }
        if (GetClientIndex() == tb_config::kInvalidValue ||
            !TubeMQService::Instance()->IsRunning() || !isClientRunning()) {
          master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
          return;
        }
        heart_beat_timer_->expires_after(std::chrono::milliseconds(2000));
        auto self = shared_from_this();
        heart_beat_timer_->async_wait([self, this](const std::error_code& ec) {
          if (ec) {
            return;
          }
          heartBeat2Master();
        });
        master_hb_status_.CompareAndSet(tb_config::kMasterHBRunning, tb_config::kMasterHBWaiting);
      });
  return;
}

void BaseProducer::close2Master() {
  string target_ip;
  int target_port;
  getCurrentMasterAddr(target_ip, target_port);
  LOG_INFO("[PRODUCER] close2Master begin, clientid=%s", client_uuid_.c_str());
  auto request = std::make_shared<RequestContext>();
  TubeMQCodec::ReqProtocolPtr req_protocol = TubeMQCodec::GetReqProtocol();
  request->codec_ = std::make_shared<TubeMQCodec>();
  request->ip_ = target_ip;
  request->port_ = target_port;
  request->timeout_ = config_.GetRpcReadTimeoutMs();
  request->request_id_ = Singleton<UniqueSeqId>::Instance().Next();
  req_protocol->request_id_ = request->request_id_;
  req_protocol->rpc_read_timeout_ = config_.GetRpcReadTimeoutMs() - 500;
  // send message to target
  AsyncRequest(request, req_protocol);
  LOG_INFO("[PRODUCER] close2Master finished, clientid=%s", client_uuid_.c_str());
  // not need wait response
  return;
}

string BaseProducer::buildUUID() {
  stringstream ss;
  ss << "PRODUCER_";  // producer client doesn't have `group_name`, use constant string instead
  ss << TubeMQService::Instance()->GetLocalHost();
  ss << "-";
  ss << getpid();
  ss << "-";
  ss << Utils::GetCurrentTimeMillis();
  ss << "-";
  ss << GetClientIndex();
  ss << "-";
  ss << kTubeMQClientVersion;
  return ss.str();
}

bool BaseProducer::isClientRunning() { return (status_.Get() == tb_config::kMasterRegistered); }

bool BaseProducer::initMasterAddress(string& err_info, const string& master_info) {
  // Set master ip info
  masters_map_.clear();
  Utils::Split(master_info, masters_map_, delimiter::kDelimiterComma, delimiter::kDelimiterColon);
  if (masters_map_.empty()) {
    err_info = "Illegal master_addr parameter, the format is `ip1:port1,ip2:port2...`";
    return false;
  }

  // whether translate hostname to ip
  bool need_xfs = false;
  map<string, int32_t>::iterator it;
  for (it = masters_map_.begin(); it != masters_map_.end(); it++) {
    if (Utils::NeedDnsXfs(it->first)) {
      need_xfs = true;
      break;
    }
  }
  it = masters_map_.begin();
  curr_master_addr_ = it->first;
  if (need_xfs) {
    TubeMQService::Instance()->AddMasterAddress(err_info, master_info);
  }

  err_info = "ok";
  return true;
}

void BaseProducer::getNextMasterAddr(string& ipaddr, int32_t& port) {
  map<string, int32_t>::iterator it;
  it = masters_map_.find(curr_master_addr_);
  if (it != masters_map_.end()) {
    it++;
    if (it == masters_map_.end()) {
      it = masters_map_.begin();
    }
  } else {
    it = masters_map_.begin();
  }
  ipaddr = it->first;
  port = it->second;
  curr_master_addr_ = it->first;
  if (Utils::NeedDnsXfs(ipaddr)) {
    TubeMQService::Instance()->GetXfsMasterAddress(curr_master_addr_, ipaddr);
  }
  LOG_TRACE("getNextMasterAddr address is %s:%d", ipaddr.c_str(), port);
}

void BaseProducer::getCurrentMasterAddr(string& ipaddr, int32_t& port) {
  ipaddr = curr_master_addr_;
  port = masters_map_[curr_master_addr_];
  if (Utils::NeedDnsXfs(ipaddr)) {
    TubeMQService::Instance()->GetXfsMasterAddress(curr_master_addr_, ipaddr);
  }
  LOG_TRACE("getCurrentMasterAddr address is %s:%d", ipaddr.c_str(), port);
}

bool BaseProducer::needGenMasterCertificateInfo(bool force) {
  bool need_add = false;
  if (config_.IsAuthenticEnabled()) {
    if (force) {
      need_add = true;
      nextauth_2_master.Set(false);
    } else if (nextauth_2_master.Get()) {
      if (nextauth_2_master.CompareAndSet(true, false)) {
        need_add = true;
      }
    }
  }
  return need_add;
}

void BaseProducer::processAuthorizedToken(const MasterAuthorizedInfo& authorized_token_info) {
  visit_token_.Set(authorized_token_info.visitauthorizedtoken());
  if (authorized_token_info.has_authauthorizedtoken()) {
    std::lock_guard<std::mutex> lck(auth_lock_);

    if (authorized_info_ != authorized_token_info.authauthorizedtoken()) {
      authorized_info_ = authorized_token_info.authauthorizedtoken();
    }
  }
}

void BaseProducer::genBrokerAuthenticInfo(AuthorizedInfo* p_authInfo, bool force) {
  bool need_add = false;
  p_authInfo->set_visitauthorizedtoken(visit_token_.Get());
  if (config_.IsAuthenticEnabled()) {
    if (force) {
      need_add = true;
      nextauth_2_broker.Set(false);
    } else if (nextauth_2_broker.Get()) {
      if (nextauth_2_broker.CompareAndSet(true, false)) {
        need_add = true;
      }
    }
    if (need_add) {
      string auth_token =
          Utils::GenBrokerAuthenticateToken(config_.GetUsrName(), config_.GetUsrPassWord());
      p_authInfo->set_authauthorizedtoken(auth_token);
    }
  }
}

void BaseProducer::genMasterAuthenticateToken(AuthenticateInfo* pauthinfo, const string& username,
                                              const string usrpassword) {}

void BaseProducer::showNodeInfo(const NodeInfo& node_info) {
  std::cout << "node_id: " << node_info.GetNodeId() << " node_host: " << node_info.GetHost()
            << " node_port: " << node_info.GetPort() << std::endl;
}

Partition BaseProducer::selectPartition(const Message& message) {
  std::string topic = message.GetTopic();
  std::vector<Partition> partition_list;
  // todo, get legal broker partitions
  for (auto it = topic_partition_map_[topic].begin(); it != topic_partition_map_[topic].end();
       it++) {
    int broker_id = it->first;
    for (size_t i = 0; i < topic_partition_map_[topic][broker_id].size(); i++) {
      partition_list.push_back(topic_partition_map_[topic][broker_id][i]);
    }
  }
  int select_index = partition_router_.GetPartition(message, partition_list);
  return Partition(partition_list[select_index]);
}

void BaseProducer::getAllowedPartitions() {}

const char* BaseProducer::encodePayload(BufferPtr buffer, const Message& message) {
  const char* payload = message.GetData();
  if (message.GetProperties().size() == 0) {
    return payload;
  }
  // generate attribute string
  std::string attribute;
  Utils::Join(message.GetProperties(), attribute, delimiter::kDelimiterComma,
              delimiter::kDelimiterEqual);
  // allocate
  buffer->AppendInt32(attribute.size());
  buffer->Append(attribute.c_str(), attribute.size());
  buffer->Append(payload, message.GetDataLength());

  return buffer->data();
}

void BaseProducer::buildRegisterRequestP2M(TubeMQCodec::ReqProtocolPtr& req_protocol) {
  RegisterRequestP2M p2m_request;
  // set client_id
  p2m_request.set_clientid(client_uuid_);
  // set topic list, to be pub in `publish method`
  // list<string>::iterator it_topics;

  // set checksum
  p2m_request.set_brokerchecksum(broker_info_checksum_);

  // set hostname
  p2m_request.set_hostname(TubeMQService::Instance()->GetLocalHost());

  // authenticate info
  if (needGenMasterCertificateInfo(true)) {
    MasterCertificateInfo* pmst_certinfo = p2m_request.mutable_authinfo();
    AuthenticateInfo* pauthinfo = pmst_certinfo->mutable_authinfo();
    genMasterAuthenticateToken(pauthinfo, config_.GetUsrName(), config_.GetUsrPassWord());
  }

  string req_msg;
  p2m_request.SerializeToString(&req_msg);
  req_protocol->method_id_ = rpc_config::kMasterMethoddProducerRegister;
  req_protocol->prot_msg_ = req_msg;
}

void BaseProducer::buildHeartRequestP2M(TubeMQCodec::ReqProtocolPtr& req_protocol) {
  HeartRequestP2M p2m_request;
  p2m_request.set_clientid(client_uuid_);
  p2m_request.set_brokerchecksum(broker_info_checksum_);
  p2m_request.set_hostname(TubeMQService::Instance()->GetLocalHost());
  for (auto it = pub_topic_list_.begin(); it != pub_topic_list_.end(); it++) {
    p2m_request.add_topiclist(it->first);
  }

  if (needGenMasterCertificateInfo(true)) {
    MasterCertificateInfo* pmst_certinfo = p2m_request.mutable_authinfo();
    AuthenticateInfo* pauthinfo = pmst_certinfo->mutable_authinfo();
    genMasterAuthenticateToken(pauthinfo, config_.GetUsrName(), config_.GetUsrPassWord());
  }

  string req_msg;
  p2m_request.SerializeToString(&req_msg);
  req_protocol->method_id_ = rpc_config::kMasterMethoddProducerHeatbeat;
  req_protocol->prot_msg_ = req_msg;
}

void BaseProducer::buildCloseRequestP2M(TubeMQCodec::ReqProtocolPtr& req_protocol) {
  string close_msg;
  CloseRequestP2M p2m_request;
  p2m_request.set_clientid(client_uuid_);
  p2m_request.SerializeToString(&close_msg);
  req_protocol->method_id_ = rpc_config::kMasterMethoddProducerClose;
  req_protocol->prot_msg_ = close_msg;
}

void BaseProducer::buildSendMessageRequestP2B(const Partition& partition, const Message& message,
                                              TubeMQCodec::ReqProtocolPtr& req_protocol) {
  SendMessageRequestP2B p2b_request;
  p2b_request.set_clientid(client_uuid_);
  p2b_request.set_topicname(partition.GetTopic());
  p2b_request.set_partitionid(partition.GetPartitionId());
  BufferPtr buffer = std::make_shared<Buffer>(rpc_config::kRpcConnectInitBufferSize);
  const char* data = encodePayload(buffer, message);
  size_t data_size = buffer->length() == 0 ? message.GetDataLength() : buffer->length();
  p2b_request.set_data(data, data_size);
  p2b_request.set_checksum(-1);
  string ipv4_local_address;
  string tmp_err_msg;
  Utils::GetLocalIPV4Address(tmp_err_msg, ipv4_local_address);
  uint32_t sent_addr = Utils::IpToInt(ipv4_local_address);
  p2b_request.set_sentaddr(sent_addr);
  p2b_request.set_flag(message.GetFlag());

  if (message.HasProperty(tb_config::kRsvPropKeyFilterItem)) {
    std::string msg_type;
    message.GetProperty(tb_config::kRsvPropKeyFilterItem, msg_type);
    p2b_request.set_msgtype(msg_type);
  }

  if (message.HasProperty(tb_config::kRsvPropKeyMsgTime)) {
    std::string msg_time;
    message.GetProperty(tb_config::kRsvPropKeyMsgTime, msg_time);
    p2b_request.set_msgtime(msg_time);
  }

  // if (needGenMasterCertificateInfo(true)) {
  //   AuthorizedInfo* pmst_certinfo = p2b_request.mutable_authinfo();
  //   AuthenticateInfo* pauthinfo = pmst_certinfo->mutable_authinfo();
  //   genMasterAuthenticateToken(pauthinfo, config_.GetUsrName(), config_.GetUsrPassWord());
  // }

  string req_msg;
  p2b_request.SerializeToString(&req_msg);
  req_protocol->method_id_ = rpc_config::kBrokerMethoddProducerSendMsg;
  req_protocol->prot_msg_ = req_msg;
}

bool BaseProducer::processRegisterResponseM2P(int32_t& error_code, string& err_info,
                                              const TubeMQCodec::RspProtocolPtr& rsp_protocol) {
  if (!rsp_protocol->success_) {
    error_code = rsp_protocol->code_;
    err_info = rsp_protocol->error_msg_;
    return false;
  }
  RegisterResponseM2P rsp_m2p;
  bool result = rsp_m2p.ParseFromArray(rsp_protocol->rsp_body_.data().c_str(),
                                       (int32_t)(rsp_protocol->rsp_body_.data().length()));

  if (!result) {
    error_code = err_code::kErrParseFailure;
    err_info = "Parse RegisterResponseM2P response failure!";
    return false;
  }

  if (!rsp_m2p.success()) {
    error_code = rsp_m2p.errcode();
    err_info = rsp_m2p.errmsg();
    return false;
  }

  std::vector<std::string> broker_infos;
  for (int i = 0; i < rsp_m2p.brokerinfos_size(); i++) {
    broker_infos.push_back(rsp_m2p.brokerinfos(i));
  }

  updateBrokerInfoList(true, broker_infos, rsp_m2p.brokerchecksum());

  if (rsp_m2p.has_authorizedinfo()) {
    processAuthorizedToken(rsp_m2p.authorizedinfo());
  }
  error_code = err_code::kErrSuccess;
  err_info = "Ok";
  return true;
}

bool BaseProducer::processHBResponseM2P(int32_t& error_code, string& err_info,
                                        const TubeMQCodec::RspProtocolPtr& rsp_protocol) {
  if (!rsp_protocol->success_) {
    error_code = rsp_protocol->code_;
    err_info = rsp_protocol->error_msg_;
    return false;
  }

  HeartResponseM2P rsp_m2p;
  bool result = rsp_m2p.ParseFromArray(rsp_protocol->rsp_body_.data().c_str(),
                                       (int32_t)(rsp_protocol->rsp_body_.data().length()));

  if (!result) {
    error_code = err_code::kErrParseFailure;
    err_info = "Parse HeartResponseM2P response failure!";
    return false;
  }
  if (!rsp_m2p.success()) {
    error_code = rsp_m2p.errcode();
    err_info = rsp_m2p.errmsg();
    return false;
  }

  std::vector<std::string> broker_infos;
  std::vector<std::string> topic_infos;
  for (int i = 0; i < rsp_m2p.brokerinfos_size(); i++) {
    broker_infos.push_back(rsp_m2p.brokerinfos(i));
  }
  updateBrokerInfoList(false, broker_infos, rsp_m2p.brokerchecksum());

  for (int i = 0; i < rsp_m2p.topicinfos_size(); i++) {
    topic_infos.push_back(rsp_m2p.topicinfos(i));
  }
  updateTopicConfigure(topic_infos);

  if (rsp_m2p.has_authorizedinfo()) {
    processAuthorizedToken(rsp_m2p.authorizedinfo());
  }

  return true;
}

bool BaseProducer::processSendMessageResponseB2P(int32_t& error_code, string& err_info,
                                                 const TubeMQCodec::RspProtocolPtr& rsp_protocol) {
  if (!rsp_protocol->success_) {
    error_code = rsp_protocol->code_;
    err_info = rsp_protocol->error_msg_;
    return false;
  }
  return true;
}

void BaseProducer::updateBrokerInfoList(bool is_register, const vector<string>& pkg_broker_infos,
                                        int64_t pkg_checksum) {
  std::lock_guard<std::mutex> lck(brokers_map_lock_);
  if (pkg_checksum != broker_info_checksum_) {
    if (!pkg_broker_infos.empty()) {
      broker_info_checksum_ = pkg_checksum;
      for (const auto& pkg_broker_info : pkg_broker_infos) {
        NodeInfo broker_info(true, Utils::Trim(pkg_broker_info));
        brokers_map_[std::to_string(broker_info.GetNodeId())] = broker_info;
      }
    }
  }
}

void BaseProducer::updateTopicConfigure(const std::vector<std::string>& str_topic_infos) {
  std::unordered_map<std::string, int> topic_maxsize_in_broker_map;
  std::vector<TopicInfo> topic_list;
  std::vector<std::string> str_topic_info_list;
  std::vector<std::string> strtopic_infoset;
  std::vector<std::string> topic_info_tokens;
  NodeInfo broker_info;

  std::lock_guard<std::mutex> lck(topic_partition_map_lock_);
  for (size_t i = 0; i < str_topic_infos.size(); i++) {
    if (str_topic_infos[i].empty()) continue;
    const std::string& str_topic_info = Utils::Trim(str_topic_infos[i]);
    Utils::Split(str_topic_info, str_topic_info_list, delimiter::kDelimiterPound);
    std::string str_topic_info_0 = str_topic_info_list[0];
    std::string str_topic_info_1 = str_topic_info_list[1];
    Utils::Split(str_topic_info_1, strtopic_infoset, delimiter::kDelimiterComma);
    for (const std::string& s : strtopic_infoset) {
      Utils::Split(s, topic_info_tokens, delimiter::kDelimiterColon);
      broker_info = brokers_map_[topic_info_tokens[0]];
      int partition_num = std::stoi(topic_info_tokens[1]);
      int store_num = std::stoi(topic_info_tokens[2]);
      topic_list.push_back(
          TopicInfo(broker_info, str_topic_info_0, partition_num, store_num, true, true));
    }

    if (str_topic_info_list.size() == 2 || str_topic_info_list[2].empty()) {
      continue;
    }
    topic_maxsize_in_broker_map[str_topic_info_0] = std::stoi(str_topic_info_list[2]);
  }

  for (const auto& topic_info : topic_list) {
    if (!topic_partition_map_.count(topic_info.GetTopic())) {
      std::unordered_map<int, std::vector<Partition>> broker_list;
      topic_partition_map_[topic_info.GetTopic()] = broker_list;
    }
    for (int j = 0; j < topic_info.GetTopicStoreNum(); j++) {
      int base_value = j * tb_config::kMetaStoreInsBase;
      for (int i = 0; i < topic_info.GetPartitionNum(); i++) {
        Partition tmp_part(topic_info.GetBroker(), topic_info.GetTopic(), base_value + i);
        topic_partition_map_[topic_info.GetTopic()][tmp_part.GetBrokerId()].push_back(tmp_part);
      }
    }
  }
}

}  // namespace tubemq
