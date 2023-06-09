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

#include <Common/HostWithPorts.h>
#include <Common/parseAddress.h>

#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/Operators.h>
#include <Common/Exception.h>
#include <Interpreters/Context.h>
#include <common/getFQDNOrHostName.h>
#include <Interpreters/Context.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

std::string HostWithPorts::toDebugString() const
{
    WriteBufferFromOwnString wb;

    wb << '{';
    if (!id.empty())
        wb << id << " ";
    if (!host.empty())
        wb << host << " ";
    if (rpc_port != 0)
        wb << " rpc/" << rpc_port;
    if (tcp_port != 0)
        wb << " tcp/" << tcp_port;
    if (exchange_port != 0)
        wb << " exc/" << exchange_port;
    if (exchange_status_port != 0)
        wb << " exs/" << exchange_status_port;
    wb << '}';

    return wb.str();
}

HostWithPorts HostWithPorts::fromRPCAddress(const std::string & s)
{
    std::pair<std::string, UInt16> host_port = parseAddress(s, 0);
    HostWithPorts res{std::string{removeBracketsIfIpv6(host_port.first)}};
    res.rpc_port = host_port.second;
    return res;
}

bool HostWithPorts::isExactlySameVec(const HostWithPortsVec & lhs, const HostWithPortsVec & rhs)
{
    return std::equal(lhs.begin(), lhs.end(), rhs.begin(), rhs.end(), HostWithPorts::IsExactlySame{});
}

std::ostream & operator<<(std::ostream & os, const HostWithPorts & host_ports)
{
    os << host_ports.toDebugString();
    return os;
}

namespace
{
std::string getFromEnvOrConfig(ContextPtr context, const std::string & name)
{
    char * ret = std::getenv(name.c_str());
    if (ret)
        return ret;

    return context->getConfigRef().getString(name, "");
}
} /// end namespace

std::string getWorkerID(ContextPtr context)
{
    auto get_worker_id_lambda = [] (ContextPtr c) {
        std::string worker_id = getFromEnvOrConfig(c, "WORKER_ID");
        if (worker_id.empty())
            worker_id = getHostIPFromEnv();
        return worker_id;
    };

    static std::string worker_id = get_worker_id_lambda(context);
    return worker_id;
}

std::string getWorkerGroupID(ContextPtr context)
{
    static std::string worker_group_id = getFromEnvOrConfig(context, "WORKER_GROUP_ID");
    return worker_group_id;
}

std::string getVirtualWareHouseID(ContextPtr context)
{
    static std::string virtual_warehouse_id = getFromEnvOrConfig(context, "VIRTUAL_WAREHOUSE_ID");
    return virtual_warehouse_id;
}

}
