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

#ifndef DATAPROXY_SDK_BASE_BUSLIST_CONFIG_H_
#define DATAPROXY_SDK_BASE_BUSLIST_CONFIG_H_

#include <asio.hpp>
#include <chrono>
#include <condition_variable>
#include <fstream>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <unordered_map>

#include "sdk_core.h"
#include "noncopyable.h"
#include "read_write_mutex.h"
namespace dataproxy_sdk
{
  // proxyinfo: {"port":46801,"ip":"100.125.58.214","id":1}
  class ProxyInfo
  {
  private:
    int32_t proxy_id_;
    std::string ip_;
    int32_t port_num_;

  public:
    ProxyInfo(int32_t proxy_id, std::string ip, int32_t port_num) : proxy_id_(proxy_id), ip_(ip), port_num_(port_num) {}
    std::string getString() const { return "[" + ip_ + ":" + std::to_string(port_num_) + "]"; }

    int32_t proxyId() const { return proxy_id_; }

    std::string ip() const { return ip_; }
    void setIp(const std::string &ip) { ip_ = ip; }

    int32_t portNum() const { return port_num_; }
    void setPortNum(const int32_t &port_num) { port_num_ = port_num; }
  };
  //using ProxyInfoPtr = std::shared_ptr<BusInfo>;

  class ClusterProxyList;
  using ClusterProxyListPtr = std::shared_ptr<ClusterProxyList>;
  class ClusterProxyList
  {
  private:
    int32_t cluster_id_;
    std::vector<ProxyInfoPtr> proxylist_;
    int32_t size_;
    int32_t load_;
    bool is_inter_visit_;
    int32_t switch_value_;
    bool get_flag_;
    int32_t active_proxy_num_; //min(config->max_active_proxy_num_, available proxy num)
    int32_t backup_proxy_num_; //ess than (size_-active_proxy_num_-1)

    std::unordered_map<std::string, ConnectionPtr> active_proxy_set_; //key:ip+port
    std::unordered_map<std::string, ConnectionPtr> backup_proxy_set_;

    //should be initialized as proxylist
    std::unordered_map<std::string, ProxyInfoPtr> unused_proxylist_;

    uint32_t msg_num_; //cumulate sent msg count

  public:
    read_write_mutex rwmutex_;

  public:
    explicit ClusterProxyList();
    virtual ~ClusterProxyList();

    virtual bool isNeedLoadBalance();
    void addBusAddress(const ProxyInfoPtr &proxy_info);
    bool enableUpdate(const ClusterProxyListPtr &other);
    virtual int32_t initConn(); //init active conns and backup conns
    void clearAllConn();
    void clearInvalidConns();
    void keepConnsAlive();
    void balanceConns();
    void updateBackupConns();
    ConnectionPtr getSendConn();

    ConnectionPtr createRandomConnForActiveSet();
    ConnectionPtr createRandomConnForBackupSet();

    int32_t size() const { return size_; }
    void setSize(const int32_t &size) { size_ = size; }
    int32_t clusterId() const { return cluster_id_; }
    void setClusterId(const int32_t &cluster_id) { cluster_id_ = cluster_id; }
    int32_t switchValue() const { return switch_value_; }
    void setSwitchValue(const int32_t &switch_value) { switch_value_ = switch_value; }
    int32_t load() const { return load_; }
    void setLoad(const int32_t &load) { load_ = load; }
    bool isInterVisit() const { return is_inter_visit_; }
    void setIsInterVisit(bool is_inter_visit) { is_inter_visit_ = is_inter_visit; }
    bool getFlag() const { return get_flag_; }
    void setGetFlag(bool get_flag) { get_flag_ = get_flag; }
    int32_t activeBusNum() const { return active_proxy_num_; }
    void setActiveAndBackupBusNum(const int32_t &default_set);
    int32_t backupBusNum() const { return backup_proxy_num_; }

    int32_t backupBusNeedCreate() const
    {
      if (backup_proxy_set_.size() < backup_proxy_num_)
        return backup_proxy_num_ - backup_proxy_set_.size();
      return 0;
    }
    int32_t activeBusNeedCreate() const
    {
      if (active_proxy_set_.size() < active_proxy_num_)
        return active_proxy_num_ - active_proxy_set_.size();
      return 0;
    }
    void initUnusedBus();
    
  private:
    ConnectionPtr createRandomConn(std::unordered_map<std::string, ConnectionPtr> &conn_set);
    int32_t createConnSet();

  };

  class GlobalCluster : noncopyable
  {
  private:
    std::unordered_map<std::string, int32_t> groupid2cluster_map_;   //<inlong_group_id,cluster_id>
    std::unordered_map<int32_t, ClusterProxyListPtr> cluster_set_; //key:cluster_id

    std::unordered_map<std::string, std::string> cache_groupid2metaInfo_; //<inlong_group_id,proxylist>, for disaster, read from local cache file

    read_write_mutex groupid2cluster_rwmutex_;
    read_write_mutex cluster_set_rwmutex_;

    bool update_flag_;
    std::mutex cond_mutex_;
    std::condition_variable cond_;
    bool exit_flag_; //exit proxylist update thread
    std::thread worker_;
    ExecutorThreadPtr timer_worker_; //clean invalid conn, send hb, and do balance betweeen activeconn and backupconn

    SteadyTimerPtr clear_timer_;
    SteadyTimerPtr keepAlive_timer_;
    SteadyTimerPtr doBalance_timer_; // do balance switch between active and backup
    SteadyTimerPtr printAckNum_timer_;
    SteadyTimerPtr updateBackup_timer_;

    enum
    { 
      kClearTimerSecond = 10, // FIXME: whether need modification
      kDoBalanceMin = 5,
      kPrintAckNumMin = 1,       // ack msg in one min cycle
      kUpdateBackupSecond = 300, // kDoBalanceMin
    };

  public:
    explicit GlobalCluster();
    ~GlobalCluster();

    int32_t initBuslistAndCreateConns();
    void startUpdateSubroutine();
    int32_t addBuslist(const std::string &inlong_group_id);
    virtual ConnectionPtr getSendConn(const std::string &inlong_group_id);
    ConnectionPtr createActiveConn(const std::string &inlong_group_id, int32_t pool_id); 
    void closeBuslistUpdate();
    int32_t readCacheBuslist();

  private:                    
    void updateSubroutine();//update proxylist
    void doUpdate();
    int32_t parseAndGet(const std::string &inlong_group_id, const std::string &meta_data, ClusterProxyListPtr proxylist_config);
    void writeMetaData2File(std::ofstream &file, int32_t group, const std::string &inlong_group_id, const std::string &meta_data);

    void clearInvalidConn(const std::error_code &ec);
    void keepConnAlive(const std::error_code &ec); // send hb
    void doBalance(const std::error_code &ec);
    void updateBackup(const std::error_code &ec);
    virtual void printAckNum(const std::error_code &ec);
    // void cancelAllTimer();
  };

} // namespace dataproxy_sdk

#endif // DATAPROXY_SDK_BASE_BUSLIST_CONFIG_H_