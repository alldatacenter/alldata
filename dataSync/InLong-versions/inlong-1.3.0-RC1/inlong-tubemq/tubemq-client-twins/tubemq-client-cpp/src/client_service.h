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

#ifndef TUBEMQ_CLIENT_BASE_CLIENT_H_
#define TUBEMQ_CLIENT_BASE_CLIENT_H_

#include <stdint.h>

#include <map>
#include <mutex>
#include <string>
#include <thread>

#include "connection_pool.h"
#include "file_ini.h"
#include "noncopyable.h"
#include "rmt_data_cache.h"
#include "thread_pool.h"
#include "tubemq/tubemq_atomic.h"
#include "tubemq/tubemq_config.h"
#include "tubemq/tubemq_message.h"
#include "tubemq/tubemq_return.h"

namespace tubemq {

using std::map;
using std::mutex;
using std::string;
using std::thread;

class BaseClient {
 public:
  explicit BaseClient(bool is_producer);
  virtual ~BaseClient();
  virtual void ShutDown() {}
  void SetClientIndex(int32_t client_index) { client_index_ = client_index; }
  bool IsProducer() { return is_producer_; }
  const int32_t GetClientIndex() { return client_index_; }

 protected:
  bool is_producer_;
  int32_t client_index_;
};
using BaseClientPtr = std::shared_ptr<BaseClient>;

class TubeMQService : public noncopyable {
 public:
  static TubeMQService* Instance();
  bool Start(string& err_info, const TubeMQServiceConfig& serviceConfig);
  // Deprecated method
  bool Start(string& err_info, string conf_file = "../conf/tubemqclient.conf");
  bool Stop(string& err_info);
  bool IsRunning();
  const int32_t GetServiceStatus() const { return service_status_.Get(); }
  int32_t GetClientObjCnt();
  bool AddClientObj(string& err_info, BaseClientPtr client_obj);
  BaseClientPtr GetClientObj(int32_t client_index) const;
  void RmvClientObj(BaseClientPtr client_obj);
  const string& GetLocalHost() const { return local_host_; }
  ExecutorPoolPtr GetTimerExecutorPool() { return timer_executor_; }
  SteadyTimerPtr CreateTimer() { return timer_executor_->Get()->CreateSteadyTimer(); }
  ExecutorPoolPtr GetNetWorkExecutorPool() { return network_executor_; }
  ConnectionPoolPtr GetConnectionPool() { return connection_pool_; }
  template <class function>
  void Post(function f) {
    if (thread_pool_ != nullptr) {
      thread_pool_->Post(f);
    }
  }
  bool AddMasterAddress(string& err_info, const string& master_info);
  void GetXfsMasterAddress(const string& source, string& target);

 protected:
  void updMasterAddrByDns();

 private:
  TubeMQService();
  ~TubeMQService();
  void iniServiceConfigure();
  void thread_task_dnsxfs(int dns_xfs_period_ms);
  void shutDownClinets() const;
  bool hasXfsTask(map<string, int32_t>& src_addr_map);
  bool addNeedDnsXfsAddr(map<string, int32_t>& src_addr_map);
  bool getServiceConfByFile(string& err_info,
    string conf_file, TubeMQServiceConfig& serviceConfig);

 private:
  static TubeMQService* _instance;
  string local_host_;
  TubeMQServiceConfig serviceConfig_;
  AtomicInteger service_status_;
  AtomicInteger client_index_base_;
  mutable mutex mutex_;
  map<int32_t, BaseClientPtr> clients_map_;
  ExecutorPoolPtr timer_executor_;
  ExecutorPoolPtr network_executor_;
  ConnectionPoolPtr connection_pool_;
  std::shared_ptr<ThreadPool> thread_pool_;
  thread dns_xfs_thread_;
  mutable mutex dns_mutex_;
  int64_t last_check_time_;
  map<string, int32_t> master_source_;
  map<string, string> master_target_;
};

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_BASE_CLIENT_H_

