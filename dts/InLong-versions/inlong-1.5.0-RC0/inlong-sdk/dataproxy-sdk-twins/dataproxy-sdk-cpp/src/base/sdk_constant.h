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

#ifndef DATAPROXY_SDK_BASE_CONSTANT_H_
#define DATAPROXY_SDK_BASE_CONSTANT_H_

#include <stdint.h>
#include <string>

namespace dataproxy_sdk
{
    namespace constants
    {
        // load weight
        static const int32_t kWeight[30] = {1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 6, 6, 6, 6, 6, 12, 12, 12, 12, 12, 48, 96, 192, 384, 1000};

        static const int32_t kPrime[6] = {1, 3, 5, 7, 11, 13};
        static const int32_t kPrimeSize = 6;

        static const int32_t kMaxRequestTDMTimes = 4;
        static const int32_t kMaxRetryConnection = 20;                                                           //create conn failed more than 20 times, start sendbuf callback
        static const char kAttrFormat[] = "__addcol1__reptime=yyyymmddHHMMSS&__addcol2_ip=xxx.xxx.xxx.xxx"; // msg_type 7 body's attr format
        static const int32_t kAttrLen = strlen(kAttrFormat);

        static const char kTDBusCAPIVersion[] = "dataproxy_sdk_cpp-v2.0.0";
        static const char kLogName[] = "dataproxy_cpp.log";
        static const uint16_t kBinaryMagic = 0xEE01;
        static const uint32_t kBinPackMethod = 7;     // msg>=7
        static const uint8_t kBinSnappyFlag = 1 << 5; // snappy flag
        static const int32_t kBackupBusNum = 4;       // backup_proxy_num

        static const int32_t kThreadNums = 10; 
        static const int32_t kSharedBufferNums=5;
        static const bool kEnableGroupidIsolation = false; 
        static const int32_t kBufferNumPerGroupid = 5; 
        static const bool kEnablePack = true;
        static const uint32_t kPackSize = 4096;    
        static const uint32_t kPackTimeout = 3000; 
        static const uint32_t kExtPackSize = 16384; 
        static const bool kEnableZip = true;
        static const uint32_t kMinZipLen = 512; 

        static const bool kEnableRetry = true;
        static const uint32_t kRetryInterval = 3000; 
        static const uint32_t kRetryNum = 3;         
        static const uint32_t kLogNum = 10;
        static const uint32_t kLogSize = 10;   
        static const uint8_t kLogLevel = 2;    
        static const uint8_t kLogFileType = 2;
        static const char kLogPath[] = "./sdklogs/";
        static const bool kLogEnableLimit = true;

        static const char kProxyURL[] = "http://127.0.0.1:8099/inlong/manager/openapi/dataproxy/getIpList";
        static const bool kEnableProxyURLFromCluster = false;
        static const char kProxyClusterURL[] =
            "http://127.0.0.1:8099/heartbeat/dataproxy_ip_v2?cluster_id=0&net_tag=normal";
        static const uint32_t kProxyUpdateInterval = 10;
        static const uint32_t kProxyURLTimeout = 2;
        static const uint32_t kMaxActiveProxyNum = 3;

        static const char kSerIP[] = "127.0.0.1"; 
        static const uint32_t kMaxBufPool = 50 * 1024 * 1024;
        static const uint32_t kMsgType = 7;

        static const bool kEnableTCPNagle = true;
        static const bool kEnableHeartBeat = true;
        static const uint32_t kHeartBeatInterval = 60; 
        static const bool kEnableSetAffinity = false;
        static const uint32_t kMaskCPUAffinity = 0xff;
        static const bool kIsFromDC = false;
        static const uint16_t kExtendField = 0;
        static const char kNetTag[] = "all";

        static const bool kNeedAuth = false;

        // http basic auth  
        static const char kBasicAuthHeader[] = "Authorization:";
        static const char kBasicAuthPrefix[] = "Basic";
        static const char kBasicAuthSeparator[] = " ";
        static const char kBasicAuthJoiner[] = ":";

    } // namespace constants

} // namespace dataproxy_sdk

#endif // DATAPROXY_SDK_BASE_CONSTANT_H_