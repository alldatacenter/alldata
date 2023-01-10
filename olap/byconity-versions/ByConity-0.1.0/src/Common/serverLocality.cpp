/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Common/serverLocality.h>
#include <Common/isLocalAddress.h>
#include <Common/DNSResolver.h>
#include <common/getFQDNOrHostName.h>
#include <common/logger_useful.h>

bool isLocalServer(const std::string & target, const std::string & port)
{
    try
    {
        const size_t pos = target.find_last_of(':');
        if (std::string::npos == pos)
        {
            LOG_ERROR(&Poco::Logger::get(__PRETTY_FUNCTION__),
                "Parse isLocalServer failed because cannot find colon in address {}", target);
            return false;
        }

        const std::string target_port = target.substr(pos+1, std::string::npos);

        if (target_port != port)
            return false;

        const std::string target_host = target.substr(0, pos);
        if (target_host.empty())
            return false;

        const std::string target_ip = DB::DNSResolver::instance().resolveHost(target_host).toString();

        if ((target_ip == "127.0.0.1") || (target_ip == "::1") || (target_ip == getIPOrFQDNOrHostName()))
        {
            return true;
        }
        else
        {
            Poco::Net::IPAddress ip_address{target_ip};
            return DB::isLocalAddress(ip_address);
        }
    }
    catch (...)
    {
        DB::tryLogCurrentException(__PRETTY_FUNCTION__, fmt::format("Parse isLocalServer failed for {}" , target));
    }
    return false;
}
