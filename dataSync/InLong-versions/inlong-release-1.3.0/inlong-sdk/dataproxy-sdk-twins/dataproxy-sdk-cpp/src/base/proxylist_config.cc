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

#include "proxylist_config.h"

#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <functional>
#include <iostream>
#include <map>
#include <rapidjson/document.h>
#include <stdlib.h>

#include "sdk_constant.h"
#include "executor_thread_pool.h"
#include "ini_help.h"
#include "logger.h"
#include "pack_queue.h"
#include "socket_connection.h"
#include "tc_api.h"
#include "utils.h"
namespace dataproxy_sdk
{
    ClusterProxyList::ClusterProxyList()
        : cluster_id_(-1), size_(0), load_(0), is_inter_visit_(false), switch_value_(0), get_flag_(false), msg_num_(0), active_proxy_num_(-1), backup_proxy_num_(-1)
    {
    }

    ClusterProxyList::~ClusterProxyList() { clearAllConn(); }
 
    bool ClusterProxyList::isNeedLoadBalance()
    {
        // #if 0
        if (!g_config.enable_heart_beat_ || !g_config.heart_beat_interval_)
        {
            return false;
        }
        if (active_proxy_num_ < 0)
        {
            LOG_ERROR("active_proxy_num_:%d is negative", active_proxy_num_);
        }
        if (load_ <= 0 || backup_proxy_num_ == 0)
        {
            return false;
        }
        // #endif
        return true;
    }

    void ClusterProxyList::addBusAddress(const ProxyInfoPtr &proxy_info)
    {
        proxylist_.push_back(proxy_info);
        unused_proxylist_[proxy_info->getString()] = proxy_info;
    }

    void ClusterProxyList::initUnusedBus()
    {
        for (auto it : proxylist_)
        {
            unused_proxylist_.emplace(it->getString(), it);
        }
    }

    void ClusterProxyList::setActiveAndBackupBusNum(const int32_t &default_set)
    {
        active_proxy_num_ = std::min(default_set, size_);
        LOG_INFO("active proxy num in config is %d, real active proxy num is %d, available proxy num is %d", default_set, active_proxy_num_, size_);
        backup_proxy_num_ = std::max(0, std::min(size_ - active_proxy_num_ - 1, constants::kBackupBusNum));
        if (isNeedLoadBalance())
        {
            LOG_INFO("cluster(id:%d) need balance, backup proxy num is %d", cluster_id_, backup_proxy_num_);
        }
        else
        {
            backup_proxy_num_ = 0;
            LOG_INFO("cluster(id:%d) don't do balance, cluster load:%d, left_avaliable proxy num:%d", cluster_id_, load_, backup_proxy_num_);
        }
    }

    int32_t ClusterProxyList::initConn()
    {
        unique_write_lock<read_write_mutex> rdlck(rwmutex_);
        return createConnSet();
    }

    int32_t ClusterProxyList::createConnSet()
    {
        int err_count = 0;
        for (int i = 0; i < active_proxy_num_; i++)
        {
            auto res = createRandomConnForActiveSet();
            if (!res)
            {
                ++err_count;
    
            }
            LOG_INFO("add conn%s in active_proxy_set", res->getRemoteInfo().c_str());
        }

        if (isNeedLoadBalance())
        {
            for (int i = 0; i < backup_proxy_num_; i++)
            {
                auto res = createRandomConnForBackupSet();
                if (!res)
                {
                    ++err_count;
                }
                LOG_INFO("add conn%s in backup_proxy_set", res->getRemoteInfo().c_str());
            }
        }
        return err_count;
    }

    void ClusterProxyList::clearAllConn()
    {
        unique_write_lock<read_write_mutex> rdlck(rwmutex_);

        for (auto &it : active_proxy_set_)
        {
            if (it.second)
            {
                it.second->connClose();
            }
        }
        active_proxy_set_.clear();
        for (auto &it : backup_proxy_set_)
        {
            if (it.second)
            {
                it.second->connClose();
            }
        }
        backup_proxy_set_.clear();
    }

    ConnectionPtr ClusterProxyList::getSendConn()
    {
        unique_write_lock<read_write_mutex> wtlck(rwmutex_); // FIXME: is readlock ok?

        if (active_proxy_set_.empty())
        {
            LOG_ERROR("cluster(id:%d) active_proxy_set is empty", cluster_id_);
            return nullptr;
        }

        ConnectionPtr res = nullptr;

        ++msg_num_;
        int32_t rr_idx = msg_num_ % active_proxy_set_.size();
        auto rr_con = std::next(std::begin(active_proxy_set_), rr_idx)->second;

        srand((uint32_t)Utils::getCurrentMsTime());
        int32_t rand_idx = random() % active_proxy_set_.size();
        auto rand_con = std::next(std::begin(active_proxy_set_), rand_idx)->second;

        if (rr_con->getWaitingSend() < rand_con->getWaitingSend()) // choosing less waiting conn 
        {
            res = rr_con;
        }
        else
        {
            res = rand_con;
        }

        if (res->isStop())
        {
            LOG_INFO("conn %s is closed, create new conn for send", res->getRemoteInfo().c_str());
            active_proxy_set_.erase(res->getRemoteInfo());

            auto old_proxyinfo = res->getBusInfo();

            if (unused_proxylist_.empty()) //if there is no available proxy
            {
                unused_proxylist_[old_proxyinfo->getString()] = old_proxyinfo;

                res = createRandomConnForActiveSet();
            }
            else
            {
                res = createRandomConnForActiveSet();

                unused_proxylist_[old_proxyinfo->getString()] = old_proxyinfo;
            }
        }

        return res;
    }

    ConnectionPtr ClusterProxyList::createRandomConnForActiveSet() { return createRandomConn(active_proxy_set_); }

    ConnectionPtr ClusterProxyList::createRandomConnForBackupSet() { return createRandomConn(backup_proxy_set_); }

    ConnectionPtr ClusterProxyList::createRandomConn(std::unordered_map<std::string, ConnectionPtr> &conn_set)
    {
        if (!g_executors)
            return nullptr;
        if (unused_proxylist_.empty())
        {
            clearInvalidConns(); // proxy侧短时间内全部断连，先清除close的conn
        }
        if (unused_proxylist_.empty())
        {
            LOG_ERROR("all proxyes in proxylist have already established connections, something is wrong！");
            return nullptr;
        }

        srand((uint32_t)Utils::getCurrentMsTime());
        int32_t rand_idx = random() % unused_proxylist_.size();
        auto proxy_info = std::next(std::begin(unused_proxylist_), rand_idx)->second;
        // create conn
        auto executor = g_executors->nextExecutor();
        if(!executor){
            return nullptr;
        }
        auto res = std::make_shared<Connection>(executor, proxy_info);
        if (!res)
        {
            LOG_ERROR("failed to create new connection %s", proxy_info->getString().c_str());
            return nullptr;
        }
        LOG_DEBUG("create new connection: post %s connect request successfully on network_thread(id:%d)", proxy_info->getString().c_str(),
                  executor->threadId());
        conn_set[proxy_info->getString()] = res;
        unused_proxylist_.erase(proxy_info->getString());
        return res;
    }

    // if switch_value_, cluster_id_ or size_ changes, return true
    bool ClusterProxyList::enableUpdate(const ClusterProxyListPtr &other)
    {
        unique_read_lock<read_write_mutex> rdlck(rwmutex_);

        if (this->switch_value_ != other->switchValue())
        {
            LOG_INFO("proxy ip switch_value is diff, new:%d, old:%d", other->switchValue(), this->switch_value_);
            return true;
        }
        if (this->cluster_id_ != other->clusterId())
        {
            LOG_INFO("proxy ip cluster_id is diff, new:%d, old:%d", other->clusterId(), this->cluster_id_);
            return true;
        }
        if (this->size_ != other->size())
        {
            LOG_INFO("proxy ip size is diff, new:%d, old:%d", other->size(), this->size_);
            return true;
        }
        return false;
    }

    void ClusterProxyList::clearInvalidConns()
    {

        // remove invalid conn in active set
        for (auto it = active_proxy_set_.begin(); it != active_proxy_set_.end();)
        {
            if (it->second->isStop())
            {
                LOG_INFO("active_proxy_set remove stop conn:%s", it->second->getRemoteInfo().c_str());
                unused_proxylist_.emplace(it->second->getRemoteInfo(), it->second->getBusInfo()); //close and add this proxyinfo into unused
                it = active_proxy_set_.erase(it);
                continue;
            }
            ++it;
        }
        // remove invalid conn in backup set
        for (auto it = backup_proxy_set_.begin(); it != backup_proxy_set_.end();)
        {
            if (it->second->isStop())
            {
                LOG_INFO("backup_proxy_set_ remove stop conn:%s", it->second->getRemoteInfo().c_str());
                unused_proxylist_.emplace(it->second->getRemoteInfo(), it->second->getBusInfo()); //close and add this proxyinfo into unused
                it = backup_proxy_set_.erase(it);
                continue;
            }
            ++it;
        }
    }

    void ClusterProxyList::keepConnsAlive()
    {
        unique_read_lock<read_write_mutex> rdlck(rwmutex_);
        for (auto it : active_proxy_set_)
        {
            it.second->sendHB(isNeedLoadBalance());
        }
        for (auto it : backup_proxy_set_)
        {
            it.second->sendHB(isNeedLoadBalance());
        }
    }

    void ClusterProxyList::balanceConns()
    {
        std::map<std::string, int32_t> active_load;
        std::map<std::string, int32_t> backup_load;
        unique_write_lock<read_write_mutex> wtlck(rwmutex_);

        if (!isNeedLoadBalance() || backup_proxy_set_.empty())
            return; //无需进行balance

        for (auto &it : active_proxy_set_)
        {
            int32_t avg_load = it.second->getAvgLoad();
            if (avg_load >= 0)
            {
                active_load[it.first] = avg_load;
                LOG_DEBUG("active conn:%s, avgLoad:%d", it.first.c_str(), avg_load);
            }
        }
        for (auto &it : backup_proxy_set_)
        {
            int32_t avg_load = it.second->getAvgLoad();
            if (avg_load >= 0)
            {
                backup_load[it.first] = avg_load;
                LOG_DEBUG("backup conn:%s, avgLoad:%d", it.first.c_str(), avg_load);
            }
        }

        // avg_load desc sort
        std::vector<PAIR> active_list(active_load.begin(), active_load.end());
        std::sort(active_list.begin(), active_list.end(), &Utils::downValueSort);

        // avg_load asec sort
        std::vector<PAIR> backup_list(backup_load.begin(), backup_load.end());
        std::sort(backup_list.begin(), backup_list.end(), &Utils::upValueSort);

        // int32_t small_size = active_list.size() < backup_list.size() ? active_list : backup_list;
        int32_t small_size = 1;
        for (int32_t i = 0; i < small_size; i++)
        {
            if (active_list.empty() || backup_list.empty())
                break;

            if (active_list[i].second - backup_list[i].second >= load_) // do switch
            {
                LOG_INFO("do balance, active conn:%s, load:%d <--> backup conn:%s, load:%d", active_list[i].first.c_str(), active_list[i].second,
                         backup_list[i].first.c_str(), backup_list[i].second);

                ConnectionPtr &tmp = active_proxy_set_[active_list[i].first];
                active_proxy_set_.erase(active_list[i].first);
                active_proxy_set_[backup_list[i].first] = backup_proxy_set_[backup_list[i].first];
                backup_proxy_set_.erase(backup_list[i].first);
                backup_proxy_set_[tmp->getRemoteInfo()] = tmp;
            }
        }
    }

    void ClusterProxyList::updateBackupConns()
    {
        unique_write_lock<read_write_mutex> wtlck(rwmutex_);

        if (!isNeedLoadBalance())
            return;

        // update backup conns
        for (auto it : backup_proxy_set_)
        {
            if (it.second)
            {
                LOG_DEBUG("update backup_conns regularly, close old conns and then create new conns");
                it.second->connClose();
                unused_proxylist_.emplace(it.second->getRemoteInfo(), it.second->getBusInfo()); //lose and add this proxyinfo into unused
            }
        }
        backup_proxy_set_.clear();
        for (int i = 0; i < backup_proxy_num_; i++)
        {
            auto res = createRandomConnForBackupSet();
            if (!res)
            {
                LOG_ERROR("create backup conn error, check it");
                continue;
            }
            LOG_DEBUG("new create conn%s in backup_proxy_set", res->getRemoteInfo().c_str());
        }
    }

    GlobalCluster::GlobalCluster()
        : groupid2cluster_rwmutex_(), update_flag_(false), cond_mutex_(), cond_(), exit_flag_(false), timer_worker_(std::make_shared<ExecutorThread>(100)), clear_timer_(timer_worker_->createSteadyTimer()), doBalance_timer_(timer_worker_->createSteadyTimer()), updateBackup_timer_(timer_worker_->createSteadyTimer()), printAckNum_timer_(timer_worker_->createSteadyTimer())
    {
        clear_timer_->expires_after(std::chrono::seconds(kClearTimerSecond));
        clear_timer_->async_wait([this](const std::error_code &ec)
                                 { clearInvalidConn(ec); });

        // whether need do balance
        if (g_config.enable_heart_beat_ && g_config.heart_beat_interval_ > 0)
        {
            keepAlive_timer_ = timer_worker_->createSteadyTimer();
            keepAlive_timer_->expires_after(std::chrono::seconds(g_config.heart_beat_interval_));
            keepAlive_timer_->async_wait([this](const std::error_code &ec)
                                         { keepConnAlive(ec); });
        }

        doBalance_timer_->expires_after(std::chrono::minutes(kDoBalanceMin));
        doBalance_timer_->async_wait([this](const std::error_code &ec)
                                     { doBalance(ec); });

        printAckNum_timer_->expires_after(std::chrono::minutes(kPrintAckNumMin));
        printAckNum_timer_->async_wait([this](const std::error_code &ec)
                                       { printAckNum(ec); });

        updateBackup_timer_->expires_after(std::chrono::seconds(kUpdateBackupSecond + 3));
        updateBackup_timer_->async_wait([this](const std::error_code &ec)
                                        { updateBackup(ec); });

    }

    GlobalCluster::~GlobalCluster()
    {
        // FIXME:need other close work?
        timer_worker_->close();

        closeBuslistUpdate();
        if (worker_.joinable())
        {
            worker_.join();
        }
    }

    void GlobalCluster::closeBuslistUpdate()
    {
        exit_flag_ = true;

        std::unique_lock<std::mutex> con_lck(cond_mutex_);
        update_flag_ = true;
        con_lck.unlock();
        cond_.notify_one();
    }

    int32_t GlobalCluster::initBuslistAndCreateConns()
    {
        for (auto &inlong_group_id : g_config.inlong_group_ids_)
        {
            groupid2cluster_map_[inlong_group_id] = -1;
        }

        doUpdate();

        return 0;
    }

    void GlobalCluster::startUpdateSubroutine() { worker_ = std::thread(&GlobalCluster::updateSubroutine, this); }

    //FIXME: improve, using getconn err num, if it exceeds a limit, return errcode when user send next
    ConnectionPtr GlobalCluster::getSendConn(const std::string &inlong_group_id)
    {
        unique_read_lock<read_write_mutex> rdlck1(groupid2cluster_rwmutex_);
        auto it1 = groupid2cluster_map_.find(inlong_group_id);
        if (it1 == groupid2cluster_map_.end())
        {
            LOG_ERROR("there is no proxylist and connection for inlong_group_id:%s , please check inlong_group_id/url or retry later", inlong_group_id.c_str());
            return nullptr;
        }

        unique_read_lock<read_write_mutex> rdlck2(cluster_set_rwmutex_);
        auto it2 = cluster_set_.find(it1->second);
        if (it2 == cluster_set_.end())
        {
            LOG_ERROR("there is no cluster(id:%d) for inlong_group_id:%s in cluster_set, please check inlong_group_id/url or retry later ", it1->second, inlong_group_id.c_str());
            return nullptr;
        }

        return it2->second->getSendConn();
    }

    ConnectionPtr GlobalCluster::createActiveConn(const std::string &inlong_group_id, int32_t pool_id)
    {
        if (1 == user_exit_flag.get())// if user is closing sdk
        {
            return nullptr;
        }

        unique_read_lock<read_write_mutex> rdlck1(groupid2cluster_rwmutex_);
        auto it1 = groupid2cluster_map_.find(inlong_group_id); // inlong_group_id->cluster_id
        if (it1 == groupid2cluster_map_.end())     // all proxy conn are broken
        {
            LOG_ERROR("there is no proxylist or avaliable connection for inlong_group_id:%s , please check inlong_group_id/url or retry later", inlong_group_id.c_str());
            return nullptr;
        }
        unique_read_lock<read_write_mutex> rdlck2(cluster_set_rwmutex_);
        auto it2 = cluster_set_.find(it1->second); // cluster_id->proxylist
        if (it2 == cluster_set_.end())
        {
            LOG_ERROR("there is no cluster(id:%d) for inlong_group_id:%s in cluster_set, please check inlong_group_id/url or retry later", it1->second, inlong_group_id.c_str());
            return nullptr;
        }

        unique_write_lock<read_write_mutex> wtlck(it2->second->rwmutex_);

        auto res = it2->second->createRandomConnForActiveSet();
        return res;
    }

    void GlobalCluster::updateSubroutine()
    {
        LOG_INFO("proxylist update thread start");

        while (true)
        {
            std::unique_lock<std::mutex> con_lck(cond_mutex_);
            if (cond_.wait_for(con_lck, std::chrono::minutes(g_config.proxy_update_interval_), [this]()
                               { return update_flag_; }))
            {
                if (exit_flag_)
                    break;
                update_flag_ = false;
                con_lck.unlock();
                LOG_DEBUG("new inlong_group_id is added, update proxylist");
                doUpdate(); // FIXME:improve, only update new groupid 
            }
            else
            {
                LOG_INFO("proxy update interval is %d mins, update proxylist", g_config.proxy_update_interval_);
                doUpdate();
            }
        }
        LOG_INFO("proxylist update thread exit");
    }

    // add inlong_group_id's proxylist into groupid2cluster_map_, if it is new, trigger updating groupid2cluster_map and cluster_set
    int32_t GlobalCluster::addBuslist(const std::string &inlong_group_id)
    {
        {
            unique_read_lock<read_write_mutex> rdlck(groupid2cluster_rwmutex_);
            auto it = groupid2cluster_map_.find(inlong_group_id);
            if (it != groupid2cluster_map_.end())
            {
                return 0;
            }
        }
        //not exist, add
        {
            unique_write_lock<read_write_mutex> wtlck(groupid2cluster_rwmutex_);
            groupid2cluster_map_.emplace(inlong_group_id, -1);
        }

        //set proxy update notification
        std::unique_lock<std::mutex> con_lck(cond_mutex_);
        update_flag_ = true;
        con_lck.unlock();
        cond_.notify_one();

        LOG_DEBUG("add inlong_group_id:%s to global  groupid2cluster_map, and set notify proxy updating", inlong_group_id.c_str());
        return 0;
    }

    void GlobalCluster::doUpdate()
    {
        if (groupid2cluster_map_.empty())
        {
            LOG_INFO("empty inlong_group_id, no need to update proxylist");
            return;
        }

        std::ofstream outfile;
        outfile.open(".proxy_list.ini.tmp", std::ios::out | std::ios::trunc);
        int32_t groupId_count = 0; //flush to file, record index and count

        {
            unique_write_lock<read_write_mutex> wtlck(groupid2cluster_rwmutex_);

            // for (auto& it : proxylist_map_)
            for (auto &groupid2cluster : groupid2cluster_map_)
            {
                //拼接tdm请求的url
                std::string url;
                if (g_config.enable_proxy_URL_from_cluster_)
                    url = g_config.proxy_cluster_URL_;
                else
                {
                    url = g_config.proxy_URL_ + "/" + groupid2cluster.first;
                }
                std::string post_data = "ip=" + g_config.ser_ip_ + "&version=" + constants::kTDBusCAPIVersion;
                LOG_WARN("get inlong_group_id:%s proxy cfg url:%s, post_data:%s", groupid2cluster.first.c_str(), url.c_str(), post_data.c_str());

                // request proxylist from mananer, if failed multi-times, read from local cache file
                std::string meta_data;
                int32_t ret;
                for (int i = 0; i < constants::kMaxRequestTDMTimes; i++)
                {
                    HttpRequest request = {url, g_config.proxy_URL_timeout_, g_config.need_auth_, g_config.auth_id_, g_config.auth_key_, post_data};
                    ret = Utils::requestUrl(meta_data, &request);
                    if (!ret)
                    {
                        break;
                    } //request success
                }

                if (!ret) // success
                {
                    LOG_WARN("get inlong_group_id:%s proxy json list from tdm: %s", groupid2cluster.first.c_str(), meta_data.c_str());
                }
                else //request manager error
                {
                    LOG_ERROR("failed to request inlong_group_id:%s proxylist from tdm, has tried max_times(%d)", groupid2cluster.first.c_str(),
                              constants::kMaxRequestTDMTimes);

                    if (groupid2cluster.second != -1 && cluster_set_.find(groupid2cluster.second) != cluster_set_.end())
                    {
                        LOG_WARN("failed to request inlong_group_id:%s proxylist from tdm, use previous proxylist", groupid2cluster.first.c_str());
                        continue;
                    }
                    else //new groupid, try to read from cache proxylist
                    {
                        LOG_WARN("failed to request inlong_group_id:%s proxylist from tdm, also no previous proxylist, then try to find from cache file",
                                 groupid2cluster.first.c_str());
                        auto it = cache_groupid2metaInfo_.find(groupid2cluster.first);
                        if (it != cache_groupid2metaInfo_.end())
                        {
                            meta_data = it->second;
                            LOG_WARN("get inlong_group_id:%s proxy json from cache file: %s", groupid2cluster.first.c_str(), meta_data.c_str());
                        }
                        else
                        {
                            LOG_ERROR("failed to find inlong_group_id:%s proxylist from cache file", groupid2cluster.first.c_str());
                            continue;
                        }
                    }
                }

                ClusterProxyListPtr new_proxylist_cfg = std::make_shared<ClusterProxyList>();

                ret = parseAndGet(groupid2cluster.first, meta_data, new_proxylist_cfg);
                if (ret)
                {
                    LOG_ERROR("failed to parse inlong_group_id:%s json proxylist", groupid2cluster.first.c_str());
                    continue;
                }

                unique_write_lock<read_write_mutex> wtlck_cluster_set(cluster_set_rwmutex_);

                cache_groupid2metaInfo_[groupid2cluster.first] = meta_data; //for disaster

                // #if 0

                // case1. new groupid, but there is its clusterid in memory
                if (groupid2cluster.second == -1 && cluster_set_.find(new_proxylist_cfg->clusterId()) != cluster_set_.end())
                {
                    auto cluster_id = new_proxylist_cfg->clusterId();
                    if (cluster_set_[cluster_id]->enableUpdate(new_proxylist_cfg))
                    { //已有的cluster需要更新
                        new_proxylist_cfg->initConn();
                        cluster_set_[cluster_id] = new_proxylist_cfg;
                        LOG_INFO("update cluster(id:%d) info and connections", cluster_id);
                    }
                    groupid2cluster.second = cluster_id;
                    LOG_INFO("add inlong_group_id:%s to groupid2cluster, its cluster(id:%d) is already in global_cluster", groupid2cluster.first.c_str(), cluster_id);
                    continue;
                }

                // case2. new groupid, there is no its clusterid in memory, update groupid2cluster, add it into cluster
                if (groupid2cluster.second == -1 && cluster_set_.find(new_proxylist_cfg->clusterId()) == cluster_set_.end())
                {
                    groupid2cluster.second = new_proxylist_cfg->clusterId();
                    new_proxylist_cfg->initConn();
                    cluster_set_[groupid2cluster.second] = new_proxylist_cfg;
                    LOG_INFO("add inlong_group_id:%s to cluster(id:%d) map in global_cluster, and init connections completely", groupid2cluster.first.c_str(),
                             new_proxylist_cfg->clusterId());

                    continue;
                }

                // case3. already existing groupid, whether needs update
                if (cluster_set_[groupid2cluster.second]->enableUpdate(new_proxylist_cfg))
                {
                    new_proxylist_cfg->initConn();
                    cluster_set_[groupid2cluster.second] = new_proxylist_cfg;
                    LOG_INFO("update cluster(id:%d) info and connections", groupid2cluster.second);
                }
                // #endif
            }
            // flush cache_groupid2metaInfo to file
            for (auto &it : cache_groupid2metaInfo_)
            {
                writeMetaData2File(outfile, groupId_count, it.first, it.second);
                groupId_count++;
            }
        }

        if (outfile)
        {
            if (groupId_count)
            {
                outfile << "[main]" << std::endl;
                outfile << "groupId_count=" << groupId_count << std::endl;
            }
            outfile.close();
        }
        if (groupId_count)
            rename(".proxy_list.ini.tmp", ".proxy_list.ini");
    }

    void GlobalCluster::writeMetaData2File(std::ofstream &file, int32_t groupId_index, const std::string &inlong_group_id, const std::string &meta_data)
    {
        file << "[inlong_group_id" << groupId_index << "]" << std::endl;
        file << "inlong_group_id=" << inlong_group_id << std::endl;
        file << "proxy_cfg=" << meta_data << std::endl;
    }

    int32_t GlobalCluster::readCacheBuslist()
    {
        IniFile ini = IniFile();
        if (ini.load(".proxy_list.ini"))
        {
            LOG_INFO("there is no proxylist cache file");
            return 1;
        }
        int32_t groupId_count = 0;
        if (ini.getInt("main", "groupId_count", &groupId_count))
        {
            LOG_WARN("failed to parse .proxylist.ini file");
            return 1;
        }
        for (int32_t i = 0; i < groupId_count; i++)
        {
            std::string groupidlist = "inlong_group_id" + std::to_string(i);
            std::string inlong_group_id, proxy;
            if (ini.getString(groupidlist, "inlong_group_id", &inlong_group_id))
            {
                LOG_WARN("failed to get %s name from cache file", inlong_group_id.c_str());
                continue;
            }
            if (ini.getString(groupidlist, "proxy_cfg", &proxy))
            {
                LOG_WARN("failed to get %s cache proxylist", inlong_group_id.c_str());
                continue;
            }
            LOG_INFO("read cache file, inlong_group_id:%s, proxy_cfg:%s", inlong_group_id.c_str(), proxy.c_str());
            cache_groupid2metaInfo_[inlong_group_id] = proxy;
        }

        return 0;
    }

    //parse proxylist meta
    //{"success":true,"errMsg":null,"data":{"clusterId":1,"isIntranet":null,"isSwitch":null,"load":20,"nodeList":[{"id":1,"ip":"127.0.0.1.160","port":46801}]}}
    int32_t GlobalCluster::parseAndGet(const std::string &inlong_group_id, const std::string &meta_data, ClusterProxyListPtr proxylist_config)
    {   
        rapidjson::Document doc;
        if (doc.Parse(meta_data.c_str()).HasParseError())
        {
            LOG_ERROR("failed to parse meta_data, error:(%d:%d)", doc.GetParseError(), doc.GetErrorOffset());
            return SDKInvalidResult::kErrorParseJson;
        }

        if (!(doc.HasMember("success") && doc["success"].IsBool() && doc["success"].GetBool()))
        {
           LOG_ERROR("failed to get proxy_list of inlong_group_id:%s, success: not exist or false", inlong_group_id.c_str());
            return SDKInvalidResult::kErrorParseJson;
        }
        // check data valid
        if (!doc.HasMember("data") || doc["data"].IsNull())
        {
           LOG_ERROR("failed to get proxy_list of inlong_group_id:%s, data: not exist or null", inlong_group_id.c_str());
            return SDKInvalidResult::kErrorParseJson;
        }

        // check nodelist valid
        const rapidjson::Value &clusterInfo = doc["data"];
        if (!clusterInfo.HasMember("nodeList") || clusterInfo["nodeList"].IsNull())
        {
            LOG_ERROR("invalid nodeList of inlong_group_id:%s, not exist or null", inlong_group_id.c_str());
            return SDKInvalidResult::kErrorParseJson;
        }

        // check nodeList isn't empty
        const rapidjson::Value &nodeList = clusterInfo["nodeList"];
        if (nodeList.GetArray().Size() == 0)
        {
            LOG_ERROR("empty nodeList of inlong_group_id:%s", inlong_group_id.c_str());
            return SDKInvalidResult::kErrorParseJson;
        }
        // check clusterId
        if (!clusterInfo.HasMember("clusterId") || !clusterInfo["clusterId"].IsInt() || clusterInfo["clusterId"].GetInt() < 0)
        {
            LOG_ERROR("clusterId of inlong_group_id:%s is not found or not a integer", inlong_group_id.c_str());
            return SDKInvalidResult::kErrorParseJson;
        }
        else
        {
            const rapidjson::Value &obj = clusterInfo["clusterId"];
            proxylist_config->setClusterId(obj.GetInt());
        }
        // check isSwitch
        if (clusterInfo.HasMember("isSwitch") && clusterInfo["isSwitch"].IsInt() && !clusterInfo["isSwitch"].IsNull())
        {
            const rapidjson::Value &obj = clusterInfo["isSwitch"];
            proxylist_config->setSwitchValue(obj.GetInt());
        }
        else
        {
            LOG_WARN("switch of inlong_group_id:%s is not found or not a integer", inlong_group_id.c_str());
            proxylist_config->setSwitchValue(0);
        }
        // check load
        if (clusterInfo.HasMember("load") && clusterInfo["load"].IsInt() && !clusterInfo["load"].IsNull())
        {
            const rapidjson::Value &obj = clusterInfo["load"];
            proxylist_config->setLoad(obj.GetInt());
        }
        else
        {
            LOG_WARN("load of inlong_group_id:%s is not found or not a integer", inlong_group_id.c_str());
            proxylist_config->setLoad(0);
        }
        // check isIntranet
        if (clusterInfo.HasMember("isIntranet") && clusterInfo["isIntranet"].IsInt() && !clusterInfo["isIntranet"].IsNull())
        {
            const rapidjson::Value &obj = clusterInfo["isIntranet"];
            if (!obj.GetInt())
                proxylist_config->setIsInterVisit(false);
        }
        else
        {
            LOG_WARN("isIntranet of inlong_group_id:%s is not found or not a integer", inlong_group_id.c_str());
             proxylist_config->setIsInterVisit(true);
        }
        // proxy list
        for (auto &proxy: nodeList.GetArray())
        {
            std::string ip;
            int32_t port, id;
            if (proxy.HasMember("ip") && !proxy["ip"].IsNull())
                ip = proxy["ip"].GetString();
            else
            {
                LOG_ERROR("this ip info is null");
                continue;
            }
            if (proxy.HasMember("port") && !proxy["port"].IsNull())
            {
                if (proxy["port"].IsString())
                    port = std::stoi(proxy["port"].GetString());
                else if (proxy["port"].IsInt())
                    port = proxy["port"].GetInt();
            }

            else
            {
                LOG_ERROR("this ip info is null or negative");
                continue;
            }
            if (proxy.HasMember("id") && !proxy["id"].IsNull())
            {
                if (proxy["id"].IsString())
                    id = std::stoi(proxy["id"].GetString());
                else if (proxy["id"].IsInt())
                    id = proxy["id"].GetInt();
            }
            else
            {
                LOG_WARN("there is no id info of inlong_group_id");
                continue;
            }
            proxylist_config->addBusAddress(std::make_shared<ProxyInfo>(id, ip, port));

        }
        // set size
        proxylist_config->setSize(nodeList.GetArray().Size());

        // init unused_proxylist_
        proxylist_config->initUnusedBus();

        //set active_proxy_num and backup_proxy_num
        proxylist_config->setActiveAndBackupBusNum(g_config.max_active_proxy_num_);

        return 0;
    }

    void GlobalCluster::clearInvalidConn(const std::error_code &ec)
    {
        if (ec)
            return;

        {
            unique_read_lock<read_write_mutex> rdlck(cluster_set_rwmutex_);
            for (auto &it : cluster_set_)
            {
                unique_write_lock<read_write_mutex> wtlck(it.second->rwmutex_);
                it.second->clearInvalidConns();
                //补充连接
                int32_t count = it.second->activeBusNeedCreate();
                for (int32_t i = 0; i < count; i++)
                {
                    it.second->createRandomConnForActiveSet();
                }
                count = it.second->backupBusNeedCreate();
                for (int32_t i = 0; i < count; i++)
                {
                    it.second->createRandomConnForBackupSet();
                }
            }
        }

        clear_timer_->expires_after(std::chrono::seconds(kClearTimerSecond));
        clear_timer_->async_wait([this](const std::error_code &ec)
                                 { clearInvalidConn(ec); });
    }

    void GlobalCluster::keepConnAlive(const std::error_code &ec)
    {
        if (ec)
            return;

        {
            unique_read_lock<read_write_mutex> rdlck(cluster_set_rwmutex_);
            for (auto &it : cluster_set_)
            {
                it.second->keepConnsAlive();
            }
        }

        keepAlive_timer_->expires_after(std::chrono::seconds(g_config.heart_beat_interval_));
        keepAlive_timer_->async_wait([this](const std::error_code &ec)
                                     { keepConnAlive(ec); });
    }

    void GlobalCluster::doBalance(const std::error_code &ec)
    {
        if (ec)
            return;

        {
            unique_read_lock<read_write_mutex> rdlck(cluster_set_rwmutex_);
            for (auto &it : cluster_set_)
            {
                it.second->balanceConns();
            }
        }

        doBalance_timer_->expires_after(std::chrono::minutes(kDoBalanceMin));
        doBalance_timer_->async_wait([this](const std::error_code &ec)
                                     { doBalance(ec); });
    }

    void GlobalCluster::updateBackup(const std::error_code &ec)
    {
        if (ec)
            return;

        {
            unique_read_lock<read_write_mutex> rdlck(cluster_set_rwmutex_);
            for (auto &it : cluster_set_)
            {
                it.second->updateBackupConns();
            }
        }

        srand((uint32_t)Utils::getCurrentMsTime());
        int32_t rand_idx = random() % (constants::kPrimeSize);

        updateBackup_timer_->expires_after(std::chrono::seconds(kUpdateBackupSecond + constants::kPrime[rand_idx]));
        updateBackup_timer_->async_wait([this](const std::error_code &ec)
                                        { updateBackup(ec); });
    }

    void GlobalCluster::printAckNum(const std::error_code &ec)
    {
        if (ec)
            return;
        g_queues->printAck();
        g_queues->showState();

        printAckNum_timer_->expires_after(std::chrono::minutes(kPrintAckNumMin));
        printAckNum_timer_->async_wait([this](const std::error_code &ec)
                                       { printAckNum(ec); });
    }

} // namespace dataproxy_sdk
