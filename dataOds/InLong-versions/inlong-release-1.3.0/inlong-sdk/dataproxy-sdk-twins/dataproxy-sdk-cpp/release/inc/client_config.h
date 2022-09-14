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

#ifndef DATAPROXY_SDK_BASE_CLIENT_CONFIG_H_
#define DATAPROXY_SDK_BASE_CLIENT_CONFIG_H_

#include <stdint.h>
#include <string>
#include <vector>

namespace dataproxy_sdk
{
  class ClientConfig
  {
  private:
    std::string config_path_;
    int32_t buf_num_; //sendbuf num of each bufpool, max_buf_pool_/(ext_pack_size_+400)

  public:
    //new parameters
    int32_t thread_nums_;           //network thread nums
    int32_t shared_buf_nums_; //bufpool nums in shared method // TODO: cancel this, using buffer_num_per_groupId
    std::vector<std::string> inlong_group_ids_; // groupId set //TODO: using set instead of vector?
    bool enable_groupId_isolation_;     // different groupId data are dispatched to different bufpool
    int32_t buffer_num_per_groupId_;    // bufpool num of each groupId
    std::string net_tag_;

    //pack parameters
    bool enable_pack_;
    uint32_t pack_size_;    //byte
    uint32_t pack_timeout_; //ms
    uint32_t ext_pack_size_; //byte, max length of msg

    //zip parameters
    bool enable_zip_;
    uint32_t min_zip_len_; //ms

    //resend parameters
    bool enable_retry_;
    uint32_t retry_interval_; //ms, resend interval if not receiving ack
    uint32_t retry_num_;      //resend times

    //log parameters
    uint32_t log_num_;    
    uint32_t log_size_;     // M
    uint8_t log_level_;     // trace(4)>debug(3)>info(2)>warn(1)>error(0)
    uint8_t log_file_type_; // output type:2->file, 1->console
    std::string log_path_;
    bool log_enable_limit_;

    // proxy parameters
    std::string proxy_URL_;
    bool enable_proxy_URL_from_cluster_;

    std::string proxy_cluster_URL_;
    uint32_t proxy_update_interval_; // minute
    uint32_t proxy_URL_timeout_;     // second, request proxyList timeout 
    uint32_t max_active_proxy_num_;

    //other parameters
    std::string ser_ip_;        //local ip
    uint32_t max_buf_pool_;  //byte, size of single bufpool
    uint32_t msg_type_;
    bool enable_TCP_nagle_;
    bool enable_heart_beat_;
    uint32_t heart_beat_interval_; //second
    bool enable_setaffinity_;      // cpu setaffinity
    uint32_t mask_cpu_affinity_;   // cpu setaffinity mask
    bool is_from_DC_;       // sng_dc data
    uint16_t extend_field_;

    uint32_t buf_size_; // ext_pack_size+400;

    // auth settings
    bool need_auth_;
    std::string auth_id_;
    std::string auth_key_;

    void defaultInit();

    ClientConfig() { defaultInit(); }
    
    bool parseConfig(const std::string& config_path); // return false if parse failed
    void showClientConfig();
    void updateBufSize();

    inline bool enableCharGroupid() const { return (((extend_field_)&0x4) >> 2); } // use char type groupId、streadmId
    inline bool enableTraceIP() const { return (((extend_field_)&0x2) >> 1); }
    // datat type msg: datlen|data
    inline bool isNormalDataPackFormat() const { return ((5 == msg_type_) || ((msg_type_ >= 7) && (!(extend_field_ & 0x1)))); }
    //  data&attr type msg: datlen|data|attr_len|attr
    inline bool isAttrDataPackFormat() const { return ((6 == msg_type_) || ((msg_type_ >= 7) && (extend_field_ & 0x1))); }

    inline int32_t bufNum() const { return buf_num_; }
  };

} // namespace dataproxy_sdk

#endif // DATAPROXY_SDK_BASE_CLIENT_CONFIG_H_