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

#include "client_config.h"

#include <rapidjson/document.h>

#include "sdk_constant.h"
#include "logger.h"
#include "utils.h"

namespace dataproxy_sdk
{
    bool ClientConfig::parseConfig(const std::string& config_path)
    {
        config_path_ = config_path;
        std::string file_content;
        if (!Utils::readFile(config_path_, file_content))
        {
            return false;
        }

        rapidjson::Document root;

        if (root.Parse(file_content.c_str()).HasParseError())
        {
            LOG_ERROR("failed to parse user config file: %s", config_path_.c_str());
            return false;
        }

        if (!root.HasMember("init-param"))
        {
            LOG_ERROR("param is not an object");
            return false;
        }
        const rapidjson::Value &doc = root["init-param"];

        // thread_num
        if (doc.HasMember("thread_num") && doc["thread_num"].IsInt() && doc["thread_num"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["thread_num"];

            thread_nums_ = obj.GetInt();
            LOG_WARN("thread_num in user config  is: %d", thread_nums_);
        }
        else
        {
            thread_nums_ = constants::kThreadNums;
            LOG_WARN("thread_num in user config is not expect, then use default: %d", thread_nums_);
        }
        // shared_buf_nums
        if (doc.HasMember("shared_buf_num") && doc["shared_buf_num"].IsInt() && doc["shared_buf_num"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["shared_buf_num"];

            shared_buf_nums_ = obj.GetInt();
            LOG_WARN("shared_buf_num in user config  is: %d", shared_buf_nums_);
        }
        else
        {
            shared_buf_nums_ = constants::kSharedBufferNums;
            LOG_WARN("shared_buf_num in user config is not expect, then use default: %d", shared_buf_nums_);
        }

        // inlong_group_ids, split by comma
        if (doc.HasMember("inlong_group_ids") && doc["inlong_group_ids"].IsString())
        {
            const rapidjson::Value &obj = doc["inlong_group_ids"];
            std::string groupIds_str = obj.GetString();
            int32_t size = Utils::splitOperate(groupIds_str, inlong_group_ids_, ",");
            LOG_WARN("inlong_group_ids in user config is: <%s>", Utils::getVectorStr(inlong_group_ids_).c_str());
        }
        else
        {
            LOG_WARN("inlong_group_ids in user config is empty");
        }

        // enable_groupId_isolation
        if (doc.HasMember("enable_groupId_isolation") && doc["enable_groupId_isolation"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_groupId_isolation"];

            enable_groupId_isolation_ = obj.GetBool();
            LOG_WARN("enable_groupId_isolation in user config  is: %s", enable_groupId_isolation_ ? "true" : "false");
        }
        else
        {
            enable_groupId_isolation_ = constants::kEnableGroupidIsolation;
            LOG_WARN("enable_groupId_isolation in user config is not expect, then use default: %s", enable_groupId_isolation_ ? "true" : "false");
        }

        // if enable_groupId_isolation_ is true, groupIds cann't be empty
        if (enable_groupId_isolation_ && inlong_group_ids_.empty())
        {
            LOG_ERROR("inlong_group_ids is empty, check config!");
            return false;
        }

        // auth settings
        if (doc.HasMember("need_auth") && doc["need_auth"].IsBool() && doc["need_auth"].GetBool()){
            if (!doc.HasMember("auth_id") || !doc.HasMember("auth_key") || !doc["auth_id"].IsString() || !doc["auth_key"].IsString() ||doc["auth_id"].IsNull() || doc["auth_key"].IsNull())
            {
                LOG_ERROR("need_auth, but auth_id or auth_key is empty or not string type, check config!");
                return false;
            }
            need_auth_ = true;
            auth_id_ = doc["auth_id"].GetString();
            auth_key_ = doc["auth_key"].GetString();
            LOG_WARN("need_auth, auth_id:%s, auth_key:%s", auth_id_.c_str(), auth_key_.c_str());        
        }
        else
        {
            need_auth_ = constants::kNeedAuth;
            LOG_WARN("need_auth is not expect, then use default:%s", need_auth_ ? "true" : "false");
        }

        // buffer_num_per_groupId
        if (doc.HasMember("buffer_num_per_groupId") && doc["buffer_num_per_groupId"].IsInt() && doc["buffer_num_per_groupId"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["buffer_num_per_groupId"];

            buffer_num_per_groupId_ = obj.GetInt();
            LOG_WARN("buffer_num_per_groupId in user config  is: %d", buffer_num_per_groupId_);
        }
        else
        {
            buffer_num_per_groupId_ = constants::kBufferNumPerGroupid;
            LOG_WARN("buffer_num_per_groupId in user config is not expect, then use default: %d", buffer_num_per_groupId_);
        }

        // ser_ip
        if (doc.HasMember("ser_ip") && doc["ser_ip"].IsString())
        {
            const rapidjson::Value &obj = doc["ser_ip"];

            ser_ip_ = obj.GetString();
            LOG_WARN("ser_ip in user config  is: %s", ser_ip_.c_str());
        }
        else
        {
            ser_ip_ = constants::kSerIP;
            LOG_WARN("ser_ip in user config is not expect, then use default: %s", ser_ip_.c_str());
        }
        // enable_pack
        if (doc.HasMember("enable_pack") && doc["enable_pack"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_pack"];

            enable_pack_ = obj.GetBool();
            LOG_WARN("enable_pack in user config  is: %s", enable_pack_ ? "true" : "false");
        }
        else
        {
            enable_pack_ = constants::kEnablePack;
            LOG_WARN("enable_pack in user config is not expect, then use default: %s", enable_pack_ ? "true" : "false");
        }
        // pack_size
        if (doc.HasMember("pack_size") && doc["pack_size"].IsInt() && doc["pack_size"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["pack_size"];

            pack_size_ = obj.GetInt();
            LOG_WARN("pack_size in user config  is: %d", pack_size_);
        }
        else
        {
            pack_size_ = constants::kPackSize;
            LOG_WARN("pack_size in user config is not expect, then use default: %d ms", pack_size_);
        }
        // pack_timeout
        if (doc.HasMember("pack_timeout") && doc["pack_timeout"].IsInt() && doc["pack_timeout"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["pack_timeout"];

            pack_timeout_ = obj.GetInt();
            LOG_WARN("pack_timeout in user config  is: %d ms", pack_timeout_);
        }
        else
        {
            pack_timeout_ = constants::kPackTimeout;
            LOG_WARN("pack_timeout in user config is not expect, then use default: %d", pack_timeout_);
        }
        // ext_pack_size
        if (doc.HasMember("ext_pack_size") && doc["ext_pack_size"].IsInt() && doc["ext_pack_size"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["ext_pack_size"];

            ext_pack_size_ = obj.GetInt();
            LOG_WARN("ext_pack_size in user config  is: %d", ext_pack_size_);
        }
        else
        {
            ext_pack_size_ = constants::kExtPackSize;
            LOG_WARN("ext_pack_size in user config is not expect, then use default: %d", ext_pack_size_);
        }
        // enable_zip
        if (doc.HasMember("enable_zip") && doc["enable_zip"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_zip"];

            enable_zip_ = obj.GetBool();
            LOG_WARN("enable_zip in user config  is: %s", enable_zip_ ? "true" : "false");
        }
        else
        {
            enable_zip_ = constants::kEnablePack;
            LOG_WARN("enable_zip in user config is not expect, then use default: %s", enable_zip_ ? "true" : "false");
        }
        // min_zip_len
        if (doc.HasMember("min_zip_len") && doc["min_zip_len"].IsInt() && doc["min_zip_len"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["min_zip_len"];

            min_zip_len_ = obj.GetInt();
            LOG_WARN("min_zip_len in user config  is: %d", min_zip_len_);
        }
        else
        {
            min_zip_len_ = constants::kMinZipLen;
            LOG_WARN("min_zip_len in user config is not expect, then use default: %d", min_zip_len_);
        }
        // enable_retry
        if (doc.HasMember("enable_retry") && doc["enable_retry"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_retry"];

            enable_retry_ = obj.GetBool();
            LOG_WARN("enable_retry in user config  is: %s", enable_retry_ ? "true" : "false");
        }
        else
        {
            enable_retry_ = constants::kEnableRetry;
            LOG_WARN("enable_retry in user config is not expect, then use default: %s", enable_retry_ ? "true" : "false");
        }
        // retry_interval
        if (doc.HasMember("retry_ms") && doc["retry_ms"].IsInt() && doc["retry_ms"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["retry_ms"];

            retry_interval_ = obj.GetInt();
            LOG_WARN("retry_interval in user config  is: %d ms", retry_interval_);
        }
        else
        {
            retry_interval_ = constants::kRetryInterval;
            LOG_WARN("retry_interval in user config is not expect, then use default: %d ms", retry_interval_);
        }
        // retry_num
        if (doc.HasMember("retry_num") && doc["retry_num"].IsInt() && doc["retry_num"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["retry_num"];

            retry_num_ = obj.GetInt();
            LOG_WARN("retry_num in user config  is: %d times", retry_num_);
        }
        else
        {
            retry_num_ = constants::kRetryNum;
            LOG_WARN("retry_num in user config is not expect, then use default: %d times", retry_num_);
        }   
        // log_num
        if (doc.HasMember("log_num") && doc["log_num"].IsInt() && doc["log_num"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["log_num"];

            log_num_ = obj.GetInt();
            LOG_WARN("log_num in user config  is: %d", log_num_);
        }
        else
        {
            log_num_ = constants::kLogNum;
            LOG_WARN("log_num in user config is not expect, then use default: %d", log_num_);
        }
        // log_size
        if (doc.HasMember("log_size") && doc["log_size"].IsInt() && doc["log_size"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["log_size"];

            log_size_ = obj.GetInt();
            LOG_WARN("log_size in user config  is: %dM", log_size_);
        }
        else
        {
            log_size_ = constants::kLogSize;
            LOG_WARN("log_size in user config is not expect, then use default: %dM", log_size_);
        }
        // log_level
        if (doc.HasMember("log_level") && doc["log_level"].IsInt() && doc["log_level"].GetInt() >= 0 && doc["log_level"].GetInt() <= 4)
        {
            const rapidjson::Value &obj = doc["log_level"];

            log_level_ = obj.GetInt();
            LOG_WARN("log_level in user config  is: %d", log_level_);
        }
        else
        {
            log_level_ = constants::kLogLevel;
            LOG_WARN("log_level in user config is not expect, then use default: %d", log_level_);
        }
        // log_file_type
        if (doc.HasMember("log_file_type") && doc["log_file_type"].IsInt() && doc["log_file_type"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["log_file_type"];

            log_file_type_ = obj.GetInt();
            LOG_WARN("log_file_type in user config  is: %d", log_file_type_);
        }
        else
        {
            log_file_type_ = constants::kLogFileType;
            LOG_WARN("log_file_type in user config is not expect, then use default: %d", log_file_type_);
        }
        // log_path
        if (doc.HasMember("log_path") && doc["log_path"].IsString())
        {
            const rapidjson::Value &obj = doc["log_path"];

            log_path_ = obj.GetString();
            LOG_WARN("log_path in user config  is: %s", log_path_.c_str());
        }
        else
        {
            log_path_ = constants::kLogPath;
            LOG_WARN("log_path in user config is not expect, then use default: %s", log_path_.c_str());
        }
        // log_enable_limit
        if (doc.HasMember("log_enable_limit") && doc["log_enable_limit"].IsBool())
        {
            const rapidjson::Value &obj = doc["log_enable_limit"];

            log_enable_limit_ = obj.GetBool();
            LOG_WARN("log_enable_limit in user config  is: %s", log_enable_limit_ ? "true" : "false");
        }
        else
        {
            log_enable_limit_ = constants::kLogEnableLimit;
            LOG_WARN("log_enable_limit in user config is not expect, then use default: %s", log_enable_limit_ ? "true" : "false");
        }
        // proxy_URL
        if (doc.HasMember("proxy_cfg_preurl") && doc["proxy_cfg_preurl"].IsString())
        {
            const rapidjson::Value &obj = doc["proxy_cfg_preurl"];

            proxy_URL_ = obj.GetString();
            LOG_WARN("proxy_cfg_preurl in user config  is: %s", proxy_URL_.c_str());
        }
        else if (doc.HasMember("bus_cfg_preurl") && doc["bus_cfg_preurl"].IsString()) // compatible with internal usage
        {
            const rapidjson::Value &obj = doc["bus_cfg_preurl"];

            proxy_URL_ = obj.GetString();
            LOG_WARN("proxy_cfg_preurl in user config  is: %s", proxy_URL_.c_str());
        }
        
        else
        {
            proxy_URL_ = constants::kProxyURL;
            LOG_WARN("proxy_cfg_url in user config is not expect, then use default: %s", proxy_URL_.c_str());
        }
        // proxy_cluster_URL_, only internal usage
        if (doc.HasMember("bus_cfg_url") && doc["bus_cfg_url"].IsString())
        {
            const rapidjson::Value &obj = doc["bus_cfg_url"];

            proxy_cluster_URL_ = obj.GetString();
            LOG_WARN("proxy_cluster_URL(proxy_cfg_url) in user config  is: %s", proxy_cluster_URL_.c_str());
        }
        else
        {
            proxy_cluster_URL_ = constants::kProxyClusterURL;
            LOG_WARN("proxy_cluster_URL(proxy_cfg_url) in user config is not expect, then use default: %s", proxy_cluster_URL_.c_str());
        }
        // enable_proxy_URL_from_cluster
        if (doc.HasMember("enable_proxy_cfg_url") && doc["enable_proxy_cfg_url"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_proxy_cfg_url"];

            enable_proxy_URL_from_cluster_ = obj.GetBool();
            LOG_WARN("enable_proxy_URL_from_cluster in user config  is: %s", enable_proxy_URL_from_cluster_ ? "true" : "false");
        }
        else
        {
            enable_proxy_URL_from_cluster_ = constants::kEnableProxyURLFromCluster;
            LOG_WARN("enable_proxy_URL_from_cluster in user config is not expect, then use default: %s", enable_proxy_URL_from_cluster_ ? "true" : "false");
        }
        // proxy_update_interval
        if (doc.HasMember("proxy_update_interval") && doc["proxy_update_interval"].IsInt() && doc["proxy_update_interval"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["proxy_update_interval"];

            proxy_update_interval_ = obj.GetInt();
            LOG_WARN("proxy_update_interval in user config  is: %d minutes", proxy_update_interval_);
        }
        else if (doc.HasMember("bus_update_interval") && doc["bus_update_interval"].IsInt() && doc["bus_update_interval"].GetInt() > 0)//internal usage
        {
            const rapidjson::Value &obj = doc["bus_update_interval"];

            proxy_update_interval_ = obj.GetInt();
            LOG_WARN("proxy_update_interval in user config  is: %d minutes", proxy_update_interval_);
        }        
        else
        {
            proxy_update_interval_ = constants::kProxyUpdateInterval;
            LOG_WARN("proxy_update_interval in user config is not expect, then use default: %d minutes", proxy_update_interval_);
        }
        // proxy_URL_timeout
        if (doc.HasMember("proxy_url_timeout") && doc["proxy_url_timeout"].IsInt() && doc["proxy_url_timeout"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["proxy_url_timeout"];

            proxy_URL_timeout_ = obj.GetInt();
            LOG_WARN("proxy_url_timeout in user config  is: %ds", proxy_URL_timeout_);
        }
        else if (doc.HasMember("bus_url_timeout") && doc["bus_url_timeout"].IsInt() && doc["bus_url_timeout"].GetInt() > 0) //internal usage
        {
            const rapidjson::Value &obj = doc["bus_url_timeout"];

            proxy_URL_timeout_ = obj.GetInt();
            LOG_WARN("proxy_url_timeout in user config  is: %ds", proxy_URL_timeout_);
        }
        else
        {
            proxy_URL_timeout_ = constants::kProxyURLTimeout;
            LOG_WARN("proxy_url_timeout in user config is not expect, then use default: %ds", proxy_URL_timeout_);
        }
        // max_active_proxy_num
        if (doc.HasMember("max_active_proxy") && doc["max_active_proxy"].IsInt() && doc["max_active_proxy"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["max_active_proxy"];

            max_active_proxy_num_ = obj.GetInt();
            LOG_WARN("max_active_proxy in user config  is: %d", max_active_proxy_num_);
        }
        else if (doc.HasMember("max_active_bus") && doc["max_active_bus"].IsInt() && doc["max_active_bus"].GetInt() > 0) //internal usage
        {
            const rapidjson::Value &obj = doc["max_active_bus"];

            max_active_proxy_num_ = obj.GetInt();
            LOG_WARN("max_active_proxy in user config  is: %d", max_active_proxy_num_);
        }
        else
        {
            max_active_proxy_num_ = constants::kMaxActiveProxyNum;
            LOG_WARN("max_active_proxy in user config is not expect, then use default: %d", max_active_proxy_num_);
        }
        // max_buf_pool_
        if (doc.HasMember("max_buf_pool") && doc["max_buf_pool"].IsUint() && doc["max_buf_pool"].GetUint() > 0)
        {
            const rapidjson::Value &obj = doc["max_buf_pool"];

            max_buf_pool_ = obj.GetUint();
            LOG_WARN("max_buf_pool in user config  is: %lu", max_buf_pool_);
        }
        else
        {
            max_buf_pool_ = constants::kMaxBufPool;
            LOG_WARN("max_buf_pool in user config is not expect, then use default: %lu", max_buf_pool_);
        }
        // msg_type
        if (doc.HasMember("msg_type") && doc["msg_type"].IsInt() && doc["msg_type"].GetInt() > 0 && doc["msg_type"].GetInt() < 9)
        {
            const rapidjson::Value &obj = doc["msg_type"];

            msg_type_ = obj.GetInt();
            LOG_WARN("msg_type in user config  is: %d", msg_type_);
        }
        else
        {
            msg_type_ = constants::kMsgType;
            LOG_WARN("max_active_proxy in user config is not expect, then use default: %d", msg_type_);
        }
        // enable_TCP_nagle
        if (doc.HasMember("enable_tcp_nagle") && doc["enable_tcp_nagle"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_tcp_nagle"];

            enable_TCP_nagle_ = obj.GetBool();
            LOG_WARN("enable_tcp_nagle in user config  is: %s", enable_TCP_nagle_ ? "true" : "false");
        }
        else
        {
            enable_TCP_nagle_ = constants::kEnableTCPNagle;
            LOG_WARN("enable_tcp_nagle in user config is not expect, then use default: %s", enable_TCP_nagle_ ? "true" : "false");
        }
        // enable_heart_beat
        if (doc.HasMember("enable_hb") && doc["enable_hb"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_hb"];

            enable_heart_beat_ = obj.GetBool();
            LOG_WARN("enable_hb in user config  is: %s", enable_heart_beat_ ? "true" : "false");
        }
        else
        {
            enable_heart_beat_ = constants::kEnableHeartBeat;
            LOG_WARN("enable_hb in user config is not expect, then use default: %s", enable_heart_beat_ ? "true" : "false");
        }

        // heart_beat_interval
        if (doc.HasMember("hb_interval") && doc["hb_interval"].IsInt() && doc["hb_interval"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["hb_interval"];

            heart_beat_interval_ = obj.GetInt();
            LOG_WARN("heart_beat_interval in user config  is: %ds", heart_beat_interval_);
        }
        else
        {
            heart_beat_interval_ = constants::kHeartBeatInterval;
            LOG_WARN("heart_beat_interval in user config is not expect, then use default: %ds", heart_beat_interval_);
        }
        // enable_setaffinity
        if (doc.HasMember("enable_setaffinity") && doc["enable_setaffinity"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_setaffinity"];

            enable_setaffinity_ = obj.GetBool();
            LOG_WARN("enable_setaffinity in user config  is: %s", enable_setaffinity_ ? "true" : "false");
        }
        else
        {
            enable_setaffinity_ = constants::kEnableSetAffinity;
            LOG_WARN("enable_setaffinity in user config is not expect, then use default: %s", enable_setaffinity_ ? "true" : "false");
        }
        // mask_cpu_affinity
        if (doc.HasMember("mask_cpuaffinity") && doc["mask_cpuaffinity"].IsInt() && doc["mask_cpuaffinity"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["mask_cpuaffinity"];

            mask_cpu_affinity_ = obj.GetInt();
            LOG_WARN("mask_cpuaffinity in user config  is: %d", mask_cpu_affinity_);
        }
        else
        {
            mask_cpu_affinity_ = constants::kMaskCPUAffinity;
            LOG_WARN("mask_cpuaffinity in user config is not expect, then use default: %d", mask_cpu_affinity_);
        }
        // is_from_DC
        if (doc.HasMember("enable_from_dc") && doc["enable_from_dc"].IsBool())
        {
            const rapidjson::Value &obj = doc["enable_from_dc"];

            is_from_DC_ = obj.GetBool();
            LOG_WARN("enable_from_dc in user config  is: %s", is_from_DC_ ? "true" : "false");
        }
        else
        {
            is_from_DC_ = constants::kIsFromDC;
            LOG_WARN("enable_from_dc in user config is not expect, then use default: %s", is_from_DC_ ? "true" : "false");
        }
        // extend_field
        if (doc.HasMember("extend_field") && doc["extend_field"].IsInt() && doc["extend_field"].GetInt() > 0)
        {
            const rapidjson::Value &obj = doc["extend_field"];

            extend_field_ = obj.GetInt();
            LOG_WARN("extend_field in user config  is: %d", extend_field_);
        }
        else
        {
            extend_field_ = constants::kExtendField;
            LOG_WARN("extend_field in user config is not expect, then use default: %d", extend_field_);
        }

        // net_tag
        if (doc.HasMember("net_tag") && doc["net_tag"].IsString())
        {
            const rapidjson::Value &obj = doc["net_tag"];

            net_tag_ = obj.GetString();
            LOG_WARN("net_tag in user config  is: %s", net_tag_.c_str());
        }
        else
        {
            net_tag_ = constants::kNetTag;
            LOG_WARN("net_tag in user config is not expect, then use default: %s", net_tag_.c_str());
        }

        // set bufNum
        updateBufSize();
        
        LOG_WARN("sendBuf num of a pool is %d", buf_num_);

        return true;
    }

    void ClientConfig::updateBufSize()
    {
        buf_size_ = ext_pack_size_ + 400;
        buf_num_ = max_buf_pool_ / (buf_size_);
    }

    void ClientConfig::defaultInit(){

        thread_nums_=constants::kThreadNums;
        shared_buf_nums_=constants::kSharedBufferNums;
        enable_groupId_isolation_=constants::kEnableGroupidIsolation;
        buffer_num_per_groupId_=constants::kBufferNumPerGroupid;
        net_tag_=constants::kNetTag;
    
        enable_pack_=constants::kEnablePack;
        pack_size_=constants::kPackSize;
        pack_timeout_=constants::kPackTimeout;
        ext_pack_size_=constants::kExtPackSize;

        enable_zip_=constants::kEnableZip;
        min_zip_len_=constants::kMinZipLen;

        enable_retry_=constants::kEnableRetry;
        retry_interval_=constants::kRetryInterval;
        retry_num_=constants::kRetryNum;

        log_num_=constants::kLogNum;
        log_size_=constants::kLogSize;
        log_level_=constants::kLogLevel;
        log_file_type_=constants::kLogFileType;
        log_path_=constants::kLogPath;
        log_enable_limit_=constants::kLogEnableLimit;

        proxy_URL_=constants::kProxyURL;
        enable_proxy_URL_from_cluster_=constants::kEnableProxyURLFromCluster;

        proxy_cluster_URL_=constants::kProxyClusterURL;
        proxy_update_interval_=constants::kProxyUpdateInterval;
        proxy_URL_timeout_=constants::kProxyURLTimeout;
        max_active_proxy_num_=constants::kMaxActiveProxyNum;

        ser_ip_=constants::kSerIP;
        max_buf_pool_=constants::kMaxBufPool;
        msg_type_=constants::kMsgType;
        enable_TCP_nagle_=constants::kEnableTCPNagle;
        enable_heart_beat_=constants::kEnableHeartBeat;
        heart_beat_interval_=constants::kHeartBeatInterval;
        enable_setaffinity_=constants::kEnableSetAffinity;
        mask_cpu_affinity_=constants::kMaskCPUAffinity;
        is_from_DC_=constants::kIsFromDC;
        extend_field_=constants::kExtendField;

        need_auth_=constants::kNeedAuth;

        buf_size_ = ext_pack_size_ + 400;
        buf_num_ = max_buf_pool_ / (buf_size_);
    }

    void ClientConfig::showClientConfig()
    {
        LOG_WARN("thread_num: %d", thread_nums_);
        LOG_WARN("shared_buf_num: %d", shared_buf_nums_);
        LOG_WARN("inlong_group_ids: <%s>", Utils::getVectorStr(inlong_group_ids_).c_str());
        LOG_WARN("enable_groupId_isolation: %s", enable_groupId_isolation_ ? "true" : "fasle");
        LOG_WARN("buffer_num_per_groupId: %d", buffer_num_per_groupId_);
        LOG_WARN("ser_ip: %s", ser_ip_.c_str());
        LOG_WARN("enable_pack: %s", enable_pack_ ? "true" : "false");
        LOG_WARN("pack_size: %d", pack_size_);
        LOG_WARN("pack_timeout: %dms", pack_timeout_);
        LOG_WARN("ext_pack_size: %d", ext_pack_size_);
        LOG_WARN("enable_zip: %s", enable_zip_ ? "true" : "false");
        LOG_WARN("min_zip_len: %d", min_zip_len_);
        LOG_WARN("enable_retry: %s", enable_retry_ ? "true" : "false");
        LOG_WARN("retry_interval: %dms", retry_interval_);
        LOG_WARN("retry_num: %d times", retry_num_);
        LOG_WARN("log_num: %d", log_num_);
        LOG_WARN("log_size: %dM", log_size_);
        LOG_WARN("log_level: %d", log_level_);
        LOG_WARN("log_file_type: %d", log_file_type_);
        LOG_WARN("log_path: %s", log_path_.c_str());
        LOG_WARN("log_enable_limit: %s", log_enable_limit_ ? "true" : "false");
        LOG_WARN("proxy_cfg_preurl: %s", proxy_URL_.c_str());
        LOG_WARN("net_tag: %s", net_tag_.c_str());
        LOG_WARN("proxy_cluster_URL(proxy_cfg_url): %s", proxy_cluster_URL_.c_str());
        LOG_WARN("enable_proxy_URL_from_cluster: %s", enable_proxy_URL_from_cluster_ ? "true" : "false");
        LOG_WARN("proxy_update_interval: %d minutes", proxy_update_interval_);
        LOG_WARN("proxy_url_timeout: %ds", proxy_URL_timeout_);
        LOG_WARN("max_active_proxy: %d", max_active_proxy_num_);
        LOG_WARN("max_buf_pool: %lu", max_buf_pool_);
        LOG_WARN("msg_type: %d", msg_type_);
        LOG_WARN("enable_tcp_nagle: %s", enable_TCP_nagle_ ? "true" : "false");
        LOG_WARN("enable_hb: %s", enable_heart_beat_ ? "true" : "false");
        LOG_WARN("heart_beat_interval: %ds", heart_beat_interval_);
        LOG_WARN("enable_setaffinity: %s", enable_setaffinity_ ? "true" : "false");
        LOG_WARN("mask_cpuaffinity: %d", mask_cpu_affinity_);
        LOG_WARN("enable_from_dc: %s", is_from_DC_ ? "true" : "false");
        LOG_WARN("extend_field: %d", extend_field_);
        LOG_WARN("sendBuf num of a pool: %d", buf_num_);
        LOG_WARN("need_auth: %s", need_auth_ ? "true" : "false");
        LOG_WARN("auth_id: %s", auth_id_.c_str());
        LOG_WARN("auth_key: %s", auth_key_.c_str());
    }
} // namespace dataproxy_sdk
