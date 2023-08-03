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

#include <MergeTreeCommon/CnchServerTopology.h>
#include <sstream>

namespace DB
{

CnchServerTopology::CnchServerTopology(const UInt64 & lease_expiration_, HostWithPortsVec && servers_)
    :lease_expiration(lease_expiration_), servers(std::move(servers_))
{
}

HostWithPortsVec CnchServerTopology::getServerList() const
{
    return servers;
}

size_t CnchServerTopology::getServerSize() const
{
    return servers.size();
}

void CnchServerTopology::setExpiration(const UInt64 & new_expiration)
{
    lease_expiration = new_expiration;
}

UInt64 CnchServerTopology::getExpiration() const
{
    return lease_expiration;
}

String CnchServerTopology::format() const
{
    std::stringstream ss;
    ss << "{expiration: " << lease_expiration;
    ss << ", [";
    for (size_t i=0; i<servers.size(); i++)
    {
        if (i>0)
            ss << ", ";
        ss << servers[i].getHost();
    }
    ss << "]}";

    return ss.str();
}

String dumpTopologies(const std::list<CnchServerTopology> & topologies)
{
    std::stringstream ss;
    ss << "[";
    for (auto d_it = topologies.begin(); d_it != topologies.end(); d_it++)
    {
        if (d_it != topologies.begin())
            ss << ", ";
        ss << d_it->format();
    }
    ss << "]";

    return ss.str();
}

}
