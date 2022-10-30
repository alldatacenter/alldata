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

#include "client_service.h"

#include <sstream>

#include "const_config.h"
#include "logger.h"
#include "utils.h"

namespace tubemq {

using std::lock_guard;
using std::stringstream;

const uint32_t ConnectionPool::kRegularTimerSecond;

BaseClient::BaseClient(bool is_producer) {
  is_producer_ = is_producer;
  client_index_ = tb_config::kInvalidValue;
}

BaseClient::~BaseClient() {
  // no code
}

TubeMQService* TubeMQService::_instance = NULL;

static mutex tubemq_mutex_service_;

TubeMQService* TubeMQService::Instance() {
  if (NULL == _instance) {
    lock_guard<mutex> lck(tubemq_mutex_service_);
    if (NULL == _instance) {
      _instance = new TubeMQService;
    }
  }
  return _instance;
}

TubeMQService::TubeMQService()
    : timer_executor_(std::make_shared<ExecutorPool>(2)),
      network_executor_(std::make_shared<ExecutorPool>(4)) {
  service_status_.Set(0);
  client_index_base_.Set(0);
  last_check_time_ = 0;
}

TubeMQService::~TubeMQService() {
  string err_info;
  Stop(err_info);
}

bool TubeMQService::Start(string& err_info,
    const TubeMQServiceConfig& serviceConfig) {
  bool result = false;
  result = Utils::GetLocalIPV4Address(err_info, local_host_);
  if (!result) {
    return result;
  }
  if (!service_status_.CompareAndSet(0, 1)) {
    err_info = "TubeMQ Service has startted or Stopped!";
    return false;
  }
  serviceConfig_ = serviceConfig;
  iniServiceConfigure();
  service_status_.Set(2);
  err_info = "Ok!";
  LOG_INFO("[TubeMQService] TubeMQ service startted! initial configure is %s ",
    serviceConfig.ToString().c_str());
  return true;
}


bool TubeMQService::Start(string& err_info, string conf_file) {
  // check configure file
  TubeMQServiceConfig serviceConfig;
  if (!getServiceConfByFile(err_info, conf_file, serviceConfig)) {
    return false;
  }
  return Start(err_info, serviceConfig);
}

bool TubeMQService::Stop(string& err_info) {
  if (service_status_.CompareAndSet(2, -1)) {
    LOG_INFO("[TubeMQService] TubeMQ service begin to stop!");
    if (dns_xfs_thread_.joinable()) {
      dns_xfs_thread_.join();
    }
    shutDownClinets();
    connection_pool_ = nullptr;
    thread_pool_ = nullptr;
    timer_executor_->Close();
    network_executor_->Close();
    LOG_INFO("[TubeMQService] TubeMQ service stopped!");
  }
  err_info = "OK!";
  return true;
}

bool TubeMQService::IsRunning() { return (service_status_.Get() == 2); }

void TubeMQService::iniServiceConfigure() {
  // initial logger parameters
  GetLogger().Init(serviceConfig_.GetLogStorePath(),
    Logger::Level(serviceConfig_.GetLogPrintLevel()),
    serviceConfig_.GetMaxLogFileSize(), serviceConfig_.GetMaxLogFileNum());
  // initial dns translate thread
  dns_xfs_thread_ = std::thread(&TubeMQService::thread_task_dnsxfs,
    this, serviceConfig_.GetDnsXfsPeriodInMs());
  // initial service thread pools
  timer_executor_->Resize(serviceConfig_.GetTimerThreads());
  network_executor_->Resize(serviceConfig_.GetNetWorkThreads());
  thread_pool_ = std::make_shared<ThreadPool>(serviceConfig_.GetSignalThreads());
  connection_pool_ = std::make_shared<ConnectionPool>(network_executor_);
}

int32_t TubeMQService::GetClientObjCnt() {
  lock_guard<mutex> lck(mutex_);
  return clients_map_.size();
}

bool TubeMQService::AddClientObj(string& err_info, BaseClientPtr client_obj) {
  if (!IsRunning()) {
    err_info = "Service not startted!";
    return false;
  }
  int32_t client_index = client_index_base_.IncrementAndGet();
  {
    lock_guard<mutex> lck(mutex_);
    clients_map_[client_index] = client_obj;
  }
  client_obj->SetClientIndex(client_index);
  err_info = "Ok";
  return true;
}

BaseClientPtr TubeMQService::GetClientObj(int32_t client_index) const {
  BaseClientPtr client_obj = nullptr;
  map<int32_t, BaseClientPtr>::const_iterator it;

  lock_guard<mutex> lck(mutex_);
  it = clients_map_.find(client_index);
  if (it != clients_map_.end()) {
    client_obj = it->second;
  }
  return client_obj;
}

void TubeMQService::RmvClientObj(BaseClientPtr client_obj) {
  if (client_obj != nullptr) {
    lock_guard<mutex> lck(mutex_);
    clients_map_.erase(client_obj->GetClientIndex());
    client_obj->SetClientIndex(tb_config::kInvalidValue);
  }
}

void TubeMQService::shutDownClinets() const {
  map<int32_t, BaseClientPtr>::const_iterator it;
  lock_guard<mutex> lck(mutex_);
  for (it = clients_map_.begin(); it != clients_map_.end(); it++) {
    it->second->ShutDown();
  }
}

bool TubeMQService::AddMasterAddress(string& err_info, const string& master_info) {
  map<string, int32_t>::iterator it;
  map<string, int32_t> tmp_addr_map;
  map<string, int32_t> new_addr_map;
  Utils::Split(master_info, tmp_addr_map, delimiter::kDelimiterComma, delimiter::kDelimiterColon);
  if (tmp_addr_map.empty()) {
    err_info = "Illegal parameter: master_info is blank!";
    return false;
  }
  for (it = tmp_addr_map.begin(); it != tmp_addr_map.end(); ++it) {
    if (Utils::NeedDnsXfs(it->first)) {
      new_addr_map[it->first] = it->second;
    }
  }
  if (new_addr_map.empty()) {
    err_info = "Ok";
    return true;
  }
  if (addNeedDnsXfsAddr(new_addr_map)) {
    updMasterAddrByDns();
  }
  err_info = "Ok";
  return true;
}

void TubeMQService::GetXfsMasterAddress(const string& source, string& target) {
  target = source;
  lock_guard<mutex> lck(dns_mutex_);
  if (master_source_.find(source) != master_source_.end()) {
    target = master_target_[source];
  }
}

void TubeMQService::thread_task_dnsxfs(int dns_xfs_period_ms) {
  LOG_INFO("[TubeMQService] DSN transfer thread startted!");
  while (true) {
    if (TubeMQService::Instance()->GetServiceStatus() <= 0) {
      break;
    }
    if ((Utils::GetCurrentTimeMillis() - last_check_time_) >= dns_xfs_period_ms) {
      TubeMQService::Instance()->updMasterAddrByDns();
      last_check_time_ = Utils::GetCurrentTimeMillis();
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
  }
  LOG_INFO("[TubeMQService] DSN transfer thread stopped!");
}

bool TubeMQService::hasXfsTask(map<string, int32_t>& src_addr_map) {
  lock_guard<mutex> lck(dns_mutex_);
  if (!master_source_.empty()) {
    src_addr_map = master_source_;
    return true;
  }
  return false;
}

bool TubeMQService::addNeedDnsXfsAddr(map<string, int32_t>& src_addr_map) {
  bool added = false;
  map<string, int32_t>::iterator it;
  if (!src_addr_map.empty()) {
    lock_guard<mutex> lck(dns_mutex_);
    for (it = src_addr_map.begin(); it != src_addr_map.end(); it++) {
      if (master_source_.find(it->first) == master_source_.end()) {
        added = true;
        master_source_[it->first] = it->second;
      }
    }
  }
  return added;
}

void TubeMQService::updMasterAddrByDns() {
  map<string, int32_t> tmp_src_addr_map;
  map<string, string> tmp_tgt_addr_map;
  map<string, string>::iterator it;
  if (!hasXfsTask(tmp_src_addr_map)) {
    return;
  }
  Utils::XfsAddrByDns(tmp_src_addr_map, tmp_tgt_addr_map);
  lock_guard<mutex> lck(dns_mutex_);
  for (it = tmp_tgt_addr_map.begin(); it != tmp_tgt_addr_map.end(); it++) {
    master_target_[it->first] = it->second;
  }
}

bool TubeMQService::getServiceConfByFile(string& err_info,
    string conf_file, TubeMQServiceConfig& serviceConfig) {
  // check configure file
  bool result = false;
  Fileini fileini;
  string sector = "TubeMQ";
  result = Utils::ValidConfigFile(err_info, conf_file);
  if (!result) {
    return result;
  }
  result = fileini.Loadini(err_info, conf_file);
  if (!result) {
    return result;
  }
  // get log paremeters
  int32_t log_num = 10;
  int32_t log_size = 100;
  int32_t log_level = 4;
  string log_path = "../log/tubemq";
  fileini.GetValue(err_info, sector, "log_num", log_num, 10);
  fileini.GetValue(err_info, sector, "log_size", log_size, 100);
  fileini.GetValue(err_info, sector, "log_path", log_path, "../log/tubemq");
  fileini.GetValue(err_info, sector, "log_level", log_level, 4);
  log_level = TUBEMQ_MID(log_level, 4, 0);
  serviceConfig.SetLogCofigInfo(log_num, log_size, log_level, log_path);
  // get dns translate period
  int32_t dns_xfs_period_ms = 30 * 1000;
  fileini.GetValue(err_info, sector, "dns_xfs_period_ms", dns_xfs_period_ms, 30 * 1000);
  serviceConfig.SetDnsXfsPeriodInMs(dns_xfs_period_ms);
  // get thread pools paremeters
  int32_t timer_threads = 2;
  int32_t network_threads = 4;
  int32_t signal_threads = 8;
  fileini.GetValue(err_info, sector, "timer_threads", timer_threads, 2);
  fileini.GetValue(err_info, sector, "network_threads", network_threads, 4);
  fileini.GetValue(err_info, sector, "signal_threads", signal_threads, 8);
  serviceConfig.SetServiceThreads(timer_threads, network_threads, signal_threads);
  err_info = "Ok";
  return true;
}

}  // namespace tubemq
