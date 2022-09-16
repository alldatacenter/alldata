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

#include "tc_api.h"

#include <chrono>
#include <signal.h>
#include <stdio.h>
#include <string>
#include <unistd.h>
#include <unordered_map>
#include <vector>
#include <algorithm>

#include "atomic.h"
#include "buffer_pool.h"
#include "proxylist_config.h"
#include "sdk_constant.h"
#include "client_config.h"
#include "logger.h"
#include "pack_queue.h"
#include "utils.h"
#include "rapidjson/document.h"

namespace dataproxy_sdk
{
    AtomicInt init_flag{0};
    AtomicInt user_exit_flag{0};
    AtomicUInt g_send_msgid{0}; // msg uuid
    AtomicInt g_buf_full{0};    // used for buf full log limit
    ClientConfig g_config;
    GlobalCluster *g_clusters = nullptr;
    GlobalQueues *g_queues = nullptr;
    TotalPools *g_pools = nullptr;
    ExecutorThreadPool *g_executors = nullptr;

    int32_t init_helper ()
    {
        debug_init_log();
        
        LOG_WARN("dataproxy_sdk_cpp start init, version:%s", constants::kTDBusCAPIVersion);
        
        g_config.showClientConfig();

        // get local ip
        if (!Utils::getFirstIpAddr(g_config.ser_ip_))
        {
            LOG_WARN("not found the localHost in local OS, use user's ser_ip(%s) in config", g_config.ser_ip_.c_str());
        }

        if (g_config.enable_setaffinity_)
        {
            Utils::bindCPU(g_config.mask_cpu_affinity_);
        }

        signal(SIGPIPE, SIG_IGN);

        g_pools = new TotalPools();
        if (!g_pools)
        {
            LOG_ERROR("fail to init global buffer pools");
            return SDKInvalidResult::kErrorInit;
        }
        LOG_INFO("buf pools init complete");

        g_clusters = new GlobalCluster();
        if (!g_clusters)
        {
            LOG_ERROR("fail to init global clusterlist");
            return SDKInvalidResult::kErrorInit;
        }
        LOG_INFO("global clusterlist init complete");

        g_queues = new GlobalQueues();
        if (!g_queues)
        {
            LOG_ERROR("fail to init global packqueue");
            return SDKInvalidResult::kErrorInit;
        }
        LOG_INFO("global packqueue init complete");

        // init network threadpools
        g_executors = new ExecutorThreadPool();
        if (!g_executors)
        {
            LOG_ERROR("fail to init network threads");
            return SDKInvalidResult::kErrorInit;
        }

        LOG_INFO("read cache proxylist for disaster tolerance");
        g_clusters->readCacheBuslist();

        if (!g_config.inlong_group_ids_.empty())
        {
            int32_t ret = g_clusters->initBuslistAndCreateConns();
            // FIXME: improve, return ret to user?
        }

        // packqueue flush thread
        g_queues->startCheckSubroutine();

        // proxylist update thread
        g_clusters->startUpdateSubroutine();

        LOG_WARN("dataproxy_sdk_cpp init complete!");

        return 0;

    }

    int32_t tc_api_init(const char *config_file)
    {

        // one process is only initialized once
        if (!init_flag.compareAndSwap(0, 1))
        {
            LOG_ERROR("dataproxy_sdk_cpp has been initialized before!");
            return SDKInvalidResult::kMultiInit;
        }
        user_exit_flag.getAndSet(0);

        if (!g_config.parseConfig(config_file)){
            LOG_ERROR("dataproxy_sdk_cpp init error, something configs use default value");
        }

        return init_helper();
        
    }


    int32_t tc_api_init(ClientConfig& client_config)
    {
        if (!init_flag.compareAndSwap(0, 1))
        {
            return SDKInvalidResult::kMultiInit;
        }
        
        // check and proxy url
        if (client_config.proxy_URL_.empty())
        {
            init_flag.compareAndSwap(1, 0);
            return SDKInvalidResult::kErrorInit;
            
        }
        // check auth setting
        if (client_config.need_auth_ && (client_config.auth_id_.empty() || client_config.auth_key_.empty()))
        {
            init_flag.compareAndSwap(1, 0);
            return SDKInvalidResult::kErrorAuthInfo;
        }

        g_config = client_config;
        g_config.updateBufSize(); 

        return init_helper();
       
    }

    int32_t tc_api_init_ext(const char *config_file, int32_t use_def)
    {
        if (!init_flag.compareAndSwap(0, 1))
        {
            LOG_ERROR("dataproxy_sdk_cpp has been initialized before!");
            return SDKInvalidResult::kMultiInit;
        }
        user_exit_flag.getAndSet(0);
        if (!g_config.parseConfig(config_file)){
            LOG_ERROR("dataproxy_sdk_cpp init error");
            
            // don't use default, return directly
            if (!use_def)
            {
                LOG_ERROR("not using default config, dataproxy_sdk_cpp should be inited again!");
                init_flag.compareAndSwap(1, 0);
                return SDKInvalidResult::kErrorInit;
            }
            
        }
        return init_helper();
    }

    int32_t sendBaseMsg(const std::string msg,
                        const std::string inlong_group_id,
                        const std::string inlong_stream_id,
                        const std::string client_ip,
                        int64_t report_time,
                        UserCallBack call_back)
    {
        // should init first, and close before
        if (init_flag.get() == 0 || user_exit_flag.get() == 1)
        {
            LOG_ERROR("capi has been closed, init first and then send");
            return SDKInvalidResult::kSendAfterClose;
        }

        // input check
        if (msg.empty() || inlong_group_id.empty() || inlong_stream_id.empty())
        {
            LOG_ERROR("invalid input, inlong_group_id:%s, inlong_stream_id:%s, msg:%s", inlong_group_id.c_str(), inlong_stream_id.c_str(), msg.c_str());
            return SDKInvalidResult::kInvalidInput;
        }

        // msg len check
        if (msg.size() > g_config.ext_pack_size_)
        {
            LOG_ERROR("msg len is too long, cur msg_len:%d, ext_pack_size:%d", msg.size(), g_config.ext_pack_size_);
            return SDKInvalidResult::kMsgTooLong;
        }

        if(g_config.enable_groupId_isolation_){
            if(std::find(g_config.inlong_group_ids_.begin(),g_config.inlong_group_ids_.end(),inlong_group_id)==g_config.inlong_group_ids_.end()){
                LOG_ERROR("inlong_group_id:%s is not specified in config file, check it", inlong_group_id.c_str());
                return SDKInvalidResult::kInvalidGroupId;
            }
        }

        g_clusters->addBuslist(inlong_group_id);

        if (!g_pools->isPoolAvailable(inlong_group_id))
        {
            if (g_buf_full.get() == 0)
                LOG_ERROR("buf pool is full, send later, datalen:%d", msg.size());
            g_buf_full.increment();
            if (g_buf_full.get() > 100)
            {
                g_buf_full.getAndSet(1);
                LOG_ERROR("buf pool is full 100 times, send later, datalen:%d", msg.size());
            }

            return SDKInvalidResult::kBufferPoolFull;
        }

        //get packqueue
        auto pack_queue = g_queues->getPackQueue(inlong_group_id, inlong_stream_id);
        if (!pack_queue)
        {
            LOG_ERROR("fail to get pack queue, inlong_group_id:%s, inlong_stream_id:%s", inlong_group_id.c_str(), inlong_stream_id.c_str());
            return SDKInvalidResult::kFailGetPackQueue;
        }

        return pack_queue->sendMsg(msg, inlong_group_id, inlong_stream_id, client_ip, report_time, call_back);
    }

    int32_t tc_api_send(const char *proxyiness_id, const char *inlong_stream_id, const char *msg, int32_t msg_len, UserCallBack call_back)
    {
        int64_t msg_time = Utils::getCurrentMsTime();
        return sendBaseMsg(msg, proxyiness_id, inlong_stream_id, "", msg_time, call_back);
    }

    int32_t tc_api_send_batch(const char *proxyiness_id, const char *inlong_stream_id, const char **msg_list, int32_t msg_cnt, UserCallBack call_back)
    {
        if (!msg_cnt)
            return SDKInvalidResult::kInvalidInput;
        int64_t msg_time = Utils::getCurrentMsTime();
        int32_t ret = 0;
        for (size_t i = 0; i < msg_cnt; i++)
        {
            ret = sendBaseMsg(msg_list[i], proxyiness_id, inlong_stream_id, "", msg_time, call_back);
            if (ret)
                return ret;
        }
        return 0;
    }

    int32_t tc_api_send_ext(const char *proxyiness_id, const char *inlong_stream_id, const char *msg, int32_t msg_len, const int64_t msg_time, UserCallBack call_back)
    {
        return sendBaseMsg(msg, proxyiness_id, inlong_stream_id, "", msg_time, call_back);
    }

    int32_t tc_api_send_base(const char *proxyiness_id,
                             const char *inlong_stream_id,
                             const char *msg,
                             int32_t msg_len,
                             const int64_t report_time,
                             const char *client_ip,
                             UserCallBack call_back)
    {
        return sendBaseMsg(msg, proxyiness_id, inlong_stream_id, client_ip, report_time, call_back);
    }

    int32_t tc_api_close(int32_t max_waitms)
    {
        // only exit once
        if (!init_flag.compareAndSwap(1, 0))
        {
            LOG_ERROR("dataproxy_sdk_cpp has been closed!");
            return SDKInvalidResult::kMultiExits;
        }

        user_exit_flag.getAndSet(1);

        std::this_thread::sleep_for(std::chrono::milliseconds(max_waitms));

        if(g_queues)
        {
            g_queues->printTotalAck(); // pring ack msg count of each groupid+streamid
        }

        delete g_queues;
        g_queues=nullptr;
        std::this_thread::sleep_for(std::chrono::seconds(1));
        delete g_executors;
        g_executors=nullptr;
        delete g_pools;
        g_pools=nullptr;
        delete g_clusters;
        g_clusters=nullptr;

        // std::this_thread::sleep_for(std::chrono::seconds(5));

        LOG_WARN("close dataproxy_sdk_cpp!");

        return 0;
    }

} // namespace dataproxy_sdk
