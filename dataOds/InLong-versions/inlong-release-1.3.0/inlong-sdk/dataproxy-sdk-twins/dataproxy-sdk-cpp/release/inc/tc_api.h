/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef  DATAPROXY_SDK_RELEASE_INC_TC_API_H_
#define  DATAPROXY_SDK_RELEASE_INC_TC_API_H_

#include <stdint.h>
#include "user_msg.h"
#include "client_config.h"

namespace dataproxy_sdk
{
// send result
enum SDKInvalidResult {
    kMultiInit    = 4,  // invoke tc_api_init repeatedly in the same process
    kErrorInit    = 5,  // tc_api_init error, user should check config file
    kMsgTooLong   = 6,  // msg len is more than ext_pack_size
    kInvalidInput = 7,  // empty groupId or streamId or msg body
    kBufferPoolFull = 8,  // sdk buffer is full, should adjust sending frequency, or increase max_buf_pool/thread_num in config file
    kFailGetConn       = 9,   // fail to get proxy connection, check whether groupId or proxy_cfg_preurl is correct
    kSendAfterClose    = 10,  // invoke send function after closing sdk
    kMultiExits        = 11,  // invoke tc_api_close function repeatedly
    kInvalidGroupId =12, // groupId is not set in config
    kFailGetBufferPool = 13,
    kFailGetSendBuf    = 14,
    kFailWriteToBuf    = 15,
    kErrorCURL         = 16,  // request manager error
    kErrorParseJson    = 17,
    kFailGetPackQueue  = 18,  // failed to get pack queue
    kErrorAuthInfo     = 19 // wrong authen

};

/**
 * @description: Before using sdk to send data, tc_api_init must be invoked
 * @return 0 if success
 * @param {char*} config_file, user configfile, prefer using absolute path
 */
int32_t tc_api_init(const char* config_file);

/**
 * @description: tc_api_init ext function
 * @return 0 if success
 * @param {char*} config_file - user configfile, prefer using absolute path
 * @param {int32_t} use_def - if use_def is zero, directly return if parsing config_file failed, which means this init failed 
 */
int32_t tc_api_init_ext(const char* config_file, int32_t use_def);

/**
 * @description: Using ClientConfig to init api
 * @return 0 if success
 * @param {ClientConfig&} refer to client_config.h
 */
int32_t tc_api_init(ClientConfig& client_config);


/**
 * @description: send data
 * @return {*} 0 if success; non-zero means this send failed, refer to SDKInvalidResult above
 * @param {char*} inlong_group_id
 * @param {char*} inlong_stream_id
 * @param {char*} msg
 * @param {int32_t} msg_len
 * @param {UserCallBack} call_back
 */
/*
 * UserCallBack function signature:
 *  return: int32_t
 *  parameters: (const char* inlong_group_id, const char* inlong_stream_id, const char* msg, int32_t msg_len, const int64_t report_time, const char* client_ip)
 */
int32_t tc_api_send(const char* inlong_group_id, const char* inlong_stream_id, const char* msg, int32_t msg_len, UserCallBack call_back = NULL);

int32_t tc_api_send_batch(const char* inlong_group_id, const char* inlong_stream_id, const char** msg_list, int32_t msg_cnt, UserCallBack call_back = NULL);


/**
 * @description: send data, add msg_time based on tc_api_send
 * @return 0 if success
 */
int32_t tc_api_send_ext(const char* inlong_group_id,
                        const char* inlong_stream_id,
                        const char* msg,
                        int32_t msg_len,
                        const int64_t msg_time,
                        UserCallBack call_back = NULL);

int32_t tc_api_send_base(const char* inlong_group_id,
                         const char* inlong_stream_id,
                         const char* msg,
                         int32_t msg_len,
                         const int64_t report_time,
                         const char* client_ip,
                         UserCallBack call_back = NULL);

/**
 * @description: close sdk; if sdk is closed, you can't send data any more
 * @return 0 if success
 * @param {int32_t} max_waitms, millisecond, waiting data in memory to be sent
 */
int32_t tc_api_close(int32_t max_waitms);

}  // namespace dataproxy_sdk

#endif  //  DATAPROXY_SDK_RELEASE_INC_TC_API_H_