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

#ifndef DATAPROXY_SDK_BASE_USER_MSG_H_
#define DATAPROXY_SDK_BASE_USER_MSG_H_

#include <memory>
#include <stdint.h>
#include <string>
#include <functional>
namespace dataproxy_sdk
{
// UserCallBack signature: inlong_group_id,inlong_stream_id,msg,msg_len, report_time, client_ip
using UserCallBack = std::function<int32_t(const char*, const char*, const char*, int32_t, const int64_t, const char*)>;

struct UserMsg
{
    std::string msg;
    std::string client_ip;  
    int64_t report_time;   
    UserCallBack cb;

    int64_t user_report_time;    
    std::string user_client_ip;

    std::string data_pack_format_attr;  //"__addcol1__reptime=" + Utils::getFormatTime(data_time_) + "&__addcol2__ip=" + client_ip
    UserMsg(const std::string& mmsg,
            const std::string& mclient_ip,
            int64_t mreport_time,
            UserCallBack mcb,
            const std::string& attr,
            const std::string& u_ip,
            int64_t u_time)
        : msg(mmsg)
        , client_ip(mclient_ip)
        , report_time(mreport_time)
        , cb(mcb)
        , data_pack_format_attr(attr)
        , user_client_ip(u_ip)
        , user_report_time(u_time)
    {
    }
};
using UserMsgPtr = std::shared_ptr<UserMsg>;

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_USER_MSG_H_