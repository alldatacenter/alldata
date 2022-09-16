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

#include "utils.h"

#include <arpa/inet.h>
#include <ctime>
#include <errno.h>
#include <fstream>
#include <iostream>
#include <iterator>
#include <net/if.h>
#include <netinet/in.h>
#include <pthread.h>
#include <regex>
#include <sstream>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/sysinfo.h>
#include <sys/time.h>

#include "logger.h"
#include "tc_api.h"
#include "curl/curl.h"
namespace dataproxy_sdk
{
uint16_t Utils::sequence     = 0;
uint64_t Utils::last_msstamp = 0;
char Utils::snowflake_id[35] = {0};
char base64_table[] = {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
      'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
      'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
      'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
      'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
      'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
      'w', 'x', 'y', 'z', '0', '1', '2', '3',
      '4', '5', '6', '7', '8', '9', '+', '/'
    };

void Utils::taskWaitTime(int32_t sec)
{
    struct timeval tv;
    tv.tv_sec  = sec;
    tv.tv_usec = 0;
    int err;
    do
    {
        err = select(0, NULL, NULL, NULL, &tv);
    } while (err < 0 && errno == EINTR);
}

uint64_t Utils::getCurrentMsTime()
{
    uint64_t ms_time = 0;
    struct timeval tv;
    gettimeofday(&tv, NULL);
    ms_time = ((uint64_t)tv.tv_sec * 1000 + tv.tv_usec / 1000);
    return ms_time;
}
uint64_t Utils::getCurrentWsTime()
{
    uint64_t ws_time = 0;
    struct timeval tv;
    gettimeofday(&tv, NULL);
    ws_time = ((uint64_t)tv.tv_sec * 1000000 + tv.tv_usec);
    return ws_time;
}

std::string Utils::getFormatTime(uint64_t data_time)
{
    struct tm timeinfo;
    char buffer[80];

    // time(&rawtime);
    time_t m_time = data_time / 1000;
    localtime_r(&m_time, &timeinfo);

    strftime(buffer, sizeof(buffer), "%Y%m%d%H%M%S", &timeinfo);

    return std::string(buffer);
}

size_t Utils::zipData(const char* input, uint32_t input_len, std::string& zip_res)
{
    size_t len_after_zip = snappy::Compress((char*)input, input_len, &zip_res);
    LOG_TRACE("data zip: input len is %u, output len is %u.", input_len, len_after_zip);
    return len_after_zip;
}

char* Utils::getSnowflakeId()
{
    std::string local_host;
    getFirstIpAddr(local_host);
    uint64_t ipaddr = htonl(inet_addr(local_host.c_str()));
    uint32_t pidid  = static_cast<uint16_t>((getpid() & 0xFFFF));
    uint32_t selfid = static_cast<uint16_t>((pthread_self() & 0xFFFF00) >> 8);

    // 22bit ms
    uint64_t sequence_mask = -1LL ^ (-1LL << 22);

    uint64_t time_id  = 0LL;
    uint64_t local_id = (ipaddr << 32) | (pidid << 16) | (selfid);

    uint64_t since_date = 1288834974657LL;  // Thu, 04 Nov 2010 01:42:54 GMT

    // 41bit ms
    uint64_t msstamp = getCurrentMsTime();

    uint64_t rand      = 0;
    uint64_t rand_mask = -1LL ^ (-1LL << (32 + 5 + 12));

    // error timestap 
    if (msstamp < last_msstamp)
    {
        LOG_ERROR("ms(%llx) time less last(%llx).", msstamp, last_msstamp);

        //last ms 
        last_msstamp = msstamp;

        srand(static_cast<uint32_t>(msstamp));
        rand = random();

        // generate id
        time_id = ((msstamp - since_date) << 22 | (rand & rand_mask));

        snprintf(&snowflake_id[0], sizeof(snowflake_id), "0x%.16llx%.16llx", local_id, time_id);
        return &snowflake_id[0];
    }

    // increase id
    if (last_msstamp == msstamp)
    {
        sequence = (sequence + 1) & sequence_mask;

        if (0 == sequence) { msstamp = waitNextMills(last_msstamp); }
    }
    else
    {
        sequence = 0;
    }

    last_msstamp = msstamp;

    time_id = (((msstamp - since_date) << 22) | sequence);

    LOG_TRACE("ms:0x%llx, ip:0x%.16llx, seq:0x:%x, selfid:%u, "
              "local_id:0x%.16llx, time_id:0x%.16llx.",
              (msstamp - since_date) << (22), ipaddr, sequence, static_cast<uint32_t>(pthread_self()), local_id, time_id);

    snprintf(&snowflake_id[0], sizeof(snowflake_id), "0x%.16llx%.16llx", local_id, time_id);
    return &snowflake_id[0];
}

int64_t Utils::waitNextMills(int64_t last_ms)
{
    int64_t msstamp = getCurrentMsTime();

    while (msstamp <= last_ms)
    {
        msstamp = getCurrentMsTime();
    }

    return msstamp;
}

bool Utils::getFirstIpAddr(std::string& local_host)
{
    int32_t sockfd;
    int32_t ip_num = 0;
    char buf[1024] = {0};
    struct ifreq* ifreq;
    struct ifreq if_flag;
    struct ifconf ifconf;

    ifconf.ifc_len = sizeof(buf);
    ifconf.ifc_buf = buf;
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
    {
        LOG_ERROR("open the local socket(AF_INET, SOCK_DGRAM) failure!");
        return false;
    }

    ioctl(sockfd, SIOCGIFCONF, &ifconf);

    ifreq  = (struct ifreq*)buf;
    ip_num = ifconf.ifc_len / sizeof(struct ifreq);
    for (int32_t i = 0; i < ip_num; i++, ifreq++)
    {
        //exclude ipv6 addr
        if (ifreq->ifr_flags != AF_INET) { continue; }

        if (0 == strncmp(&ifreq->ifr_name[0], "lo", sizeof("lo"))) { continue; }

        memcpy(&if_flag.ifr_name[0], &ifreq->ifr_name[0], sizeof(ifreq->ifr_name));

        if ((ioctl(sockfd, SIOCGIFFLAGS, (char*)&if_flag)) < 0) { continue; }

        if ((if_flag.ifr_flags & IFF_LOOPBACK) || !(if_flag.ifr_flags & IFF_UP)) { continue; }

        if (!strncmp(inet_ntoa(((struct sockaddr_in*)&(ifreq->ifr_addr))->sin_addr), "127.0.0.1", 7)) { continue; }

        local_host = inet_ntoa(((struct sockaddr_in*)&(ifreq->ifr_addr))->sin_addr);
        close(sockfd);
        return true;
    }
    close(sockfd);
    // local_host = "127.0.0.1";
    return false;
}

bool Utils::bindCPU(int32_t cpu_id)
{
    int32_t cpunum  = get_nprocs();
    int32_t cpucore = cpu_id;
    cpu_set_t mask;

    if (abs(cpu_id) > cpunum)
    {
        LOG_ERROR("mask<%d> more than total cpu num<%d>.", cpu_id, cpunum);
        return false;
    }

    if (cpu_id < 0) { cpucore = cpunum + cpu_id; }

    CPU_ZERO(&mask);
    CPU_SET(cpucore, &mask);

    if (sched_setaffinity(0, sizeof(mask), &mask) < 0) { LOG_ERROR("set CPU affinity<%d>/<%d> errno<%d>", cpu_id, cpunum, errno); }

    return true;
}

std::string Utils::base64_encode(const std::string& data)
{
    size_t in_len = data.size();
    size_t out_len = 4 * ((in_len + 2) / 3);
    std::string ret(out_len, '\0');
    size_t i;
    char *p = const_cast<char*>(ret.c_str());

    for (i = 0; i < in_len - 2; i += 3) {
      *p++ = base64_table[(data[i] >> 2) & 0x3F];
      *p++ = base64_table[((data[i] & 0x3) << 4) | ((int) (data[i + 1] & 0xF0) >> 4)];
      *p++ = base64_table[((data[i + 1] & 0xF) << 2) | ((int) (data[i + 2] & 0xC0) >> 6)];
      *p++ = base64_table[data[i + 2] & 0x3F];
    }
    if (i < in_len) {
      *p++ = base64_table[(data[i] >> 2) & 0x3F];
      if (i == (in_len - 1)) {
        *p++ = base64_table[((data[i] & 0x3) << 4)];
        *p++ = '=';
      }
      else {
        *p++ = base64_table[((data[i] & 0x3) << 4) | ((int) (data[i + 1] & 0xF0) >> 4)];
        *p++ = base64_table[((data[i + 1] & 0xF) << 2)];
      }
      *p++ = '=';
    }
    return ret;
}

std::string Utils::genBasicAuthCredential(const std::string& id, const std::string& key)
{
    std::string credential = id + constants::kBasicAuthJoiner + key;
    std::string result = constants::kBasicAuthPrefix;
    result.append(constants::kBasicAuthSeparator);
    result.append(base64_encode(credential));
    return result;
}

int32_t Utils::requestUrl(std::string& res, const HttpRequest* request)
{
    CURL* curl = NULL;
    struct curl_slist* list = NULL;

    curl_global_init(CURL_GLOBAL_ALL);

    curl = curl_easy_init();
    if (!curl)
    {
        LOG_ERROR("failed to init curl object");
        return SDKInvalidResult::kErrorCURL;
    }

    // http header
    list = curl_slist_append(list, "Content-Type: application/x-www-form-urlencoded");
    if ( request->need_auth && !request->auth_id.empty() && !request->auth_key.empty())
    {
        // Authorization: Basic xxxxxxxx
        std::string auth = constants::kBasicAuthHeader;
        auth.append(constants::kBasicAuthSeparator);
        auth.append(genBasicAuthCredential(request->auth_id, request->auth_key));
        LOG_INFO("request manager, auth-header:%s", auth.c_str());
        list = curl_slist_append(list, auth.c_str());
    }
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, list);
    
    //set url
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "POST");
    curl_easy_setopt(curl, CURLOPT_URL, request->url.c_str());
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, request->post_data.c_str());
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, request->timeout);

    //register callback and get res
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, &Utils::getUrlResponse);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &res);
    curl_easy_setopt(curl, CURLOPT_NOSIGNAL, 1L);

    //execute curl request
    CURLcode ret = curl_easy_perform(curl);
    if (ret != 0)
    {
        LOG_ERROR("%s", curl_easy_strerror(ret));
        LOG_ERROR("failed to request data from %s", request->url.c_str());
        if (curl) curl_easy_cleanup(curl);
        curl_global_cleanup();

        return SDKInvalidResult::kErrorCURL;
    }

    int32_t code;
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);
    if (code != 200)
    {
        LOG_ERROR("tdm responsed with code %d", code);
        if (curl) curl_easy_cleanup(curl);
        curl_global_cleanup();

        return SDKInvalidResult::kErrorCURL;
    }

    if (res.empty())
    {
        LOG_ERROR("tdm return empty data");
        if (curl) curl_easy_cleanup(curl);
        curl_global_cleanup();

        return SDKInvalidResult::kErrorCURL;
    }

    //clean work
    curl_easy_cleanup(curl);
    curl_global_cleanup();

    return 0;
}

size_t Utils::getUrlResponse(void* buffer, size_t size, size_t count, void* response)
{
    std::string* str = (std::string*)response;
    (*str).append((char*)buffer, size * count);

    return size * count;
}

bool Utils::readFile(const std::string& file_path, std::string& content)
{
    std::ifstream f(file_path.c_str());
    if (f.fail())
    {
        LOG_ERROR("fail to read file:%s, please check file path", file_path.c_str());
        return false;
    }
    std::stringstream ss;
    ss << f.rdbuf();
    content = ss.str();
    return true;
}

static const char kWhitespaceCharSet[] = " \n\r\t\f\v";

std::string Utils::trim(const std::string& source)
{
    std::string target = source;
    if (!target.empty())
    {
        size_t foud_pos = target.find_first_not_of(kWhitespaceCharSet);
        if (foud_pos != std::string::npos) { target = target.substr(foud_pos); }
        foud_pos = target.find_last_not_of(kWhitespaceCharSet);
        if (foud_pos != std::string::npos) { target = target.substr(0, foud_pos + 1); }
    }
    return target;
}

int32_t Utils::splitOperate(const std::string& source, std::vector<std::string>& result, const std::string& delimiter)
{
    std::string item_str;
    std::string::size_type pos1 = 0;
    std::string::size_type pos2 = 0;
    result.clear();
    if (!source.empty())
    {
        pos1 = 0;
        pos2 = source.find(delimiter);
        while (std::string::npos != pos2)
        {
            item_str = trim(source.substr(pos1, pos2 - pos1));
            pos1     = pos2 + delimiter.size();
            pos2     = source.find(delimiter, pos1);
            if (!item_str.empty()) { result.push_back(item_str); }
        }
        if (pos1 != source.length())
        {
            item_str = trim(source.substr(pos1));
            if (!item_str.empty()) { result.push_back(item_str); }
        }
    }
    return result.size();
}

std::string Utils::getVectorStr(std::vector<std::string>& vs)
{
    std::string res;
    for (auto& it : vs)
    {
        res += it + ", ";
    }
    return res;
}

}  // namespace dataproxy_sdk
