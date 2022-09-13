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

#ifndef DATAPROXY_SDK_BASE_UTILS_H_
#define DATAPROXY_SDK_BASE_UTILS_H_

#include <stdint.h>
#include <string>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <utility>
#include <vector>

#include "snappy.h"
namespace dataproxy_sdk
{
using PAIR = std::pair<std::string, int32_t>;
struct HttpRequest
{
  std::string url;
  uint32_t timeout;
  bool need_auth;
  std::string auth_id;
  std::string auth_key;
  std::string post_data;

};

class Utils
{
  private:
    static char snowflake_id[35];
    static uint16_t sequence;
    static uint64_t last_msstamp;

  public:
    static void taskWaitTime(int32_t sec);
    static uint64_t getCurrentMsTime();                                                  
    static uint64_t getCurrentWsTime();                                                 
    static std::string getFormatTime(uint64_t date_time);                                //format time: yyyymmddHHMMSS
    static size_t zipData(const char* input, uint32_t input_len, std::string& zip_res);  //snappy data
    static char* getSnowflakeId();                                                       //get 64bit snowflakeId
    static bool getFirstIpAddr(std::string& local_host);                                 
    inline static bool isLegalTime(uint64_t report_time)                                 
    {
        return ((report_time > 1435101567000LL) && (report_time < 4103101567000LL));
    }
    static bool bindCPU(int32_t cpu_id);
    static std::string base64_encode(const std::string& data);
    static std::string genBasicAuthCredential(const std::string& id, const std::string& key);                                                   
    static int32_t requestUrl(std::string& res, const HttpRequest* request);
    static bool readFile(const std::string& file_path, std::string& content);  //read file content, save as res, return true is success
    static int32_t splitOperate(const std::string& source, std::vector<std::string>& result, const std::string& delimiter);
    static std::string getVectorStr(std::vector<std::string>& vs);

    static bool upValueSort(const PAIR& lhs, const PAIR& rhs) { return lhs.second < rhs.second; }
    static bool downValueSort(const PAIR& lhs, const PAIR& rhs) { return lhs.second > rhs.second; }

  private:
    static size_t getUrlResponse(void* buffer, size_t size, size_t count, void* response);
    static int64_t waitNextMills(int64_t last_ms);
    static std::string trim(const std::string& source);
};

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_UTILS_H_