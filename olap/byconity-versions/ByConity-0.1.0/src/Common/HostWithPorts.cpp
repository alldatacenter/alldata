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
#include <common/getFQDNOrHostName.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

const std::string & getHostIPFromEnv()
{
    const auto get_host_ip_lambda = [] () -> std::string
    {
        {
            const char * byted_ipv6 = getenv("BYTED_HOST_IPV6");
            if (byted_ipv6 && byted_ipv6[0])
                return byted_ipv6;
        }

        {
            const char * my_ipv6 = getenv("MY_HOST_IPV6");
            if (my_ipv6 && my_ipv6[0])
                return my_ipv6;
        }

        {
            const char * byted_ipv4 = getenv("BYTED_HOST_IP");
            if (byted_ipv4 && byted_ipv4[0])
                return byted_ipv4;
        }

        {
            const char * my_ipv4 = getenv("MY_HOST_IP");
            if (my_ipv4 && my_ipv4[0])
                return my_ipv4;
        }

        return getIPOrFQDNOrHostName();
    };

    static std::string host_ip = get_host_ip_lambda();
    return host_ip;
}

const char * getLoopbackIPFromEnv()
{
    const auto get_loopback_ip_lambda = [] () -> const char *
    {
        {
            const char * byted_ipv6 = getenv("BYTED_HOST_IPV6");
            if (byted_ipv6 && byted_ipv6[0])
                return "::1";
        }

        {
            const char * my_ipv6 = getenv("MY_HOST_IPV6");
            if (my_ipv6 && my_ipv6[0])
                return "::1";
        }

        {
            const char * byted_ipv4 = getenv("BYTED_HOST_IP");
            if (byted_ipv4 && byted_ipv4[0])
                return "127.0.0.1";
        }

        {
            const char * my_ipv4 = getenv("MY_HOST_IP");
            if (my_ipv4 && my_ipv4[0])
                return "127.0.0.1";
        }

        return "127.0.0.1";
    };

    static const char * loopback_ip = get_loopback_ip_lambda();
    return loopback_ip;
}

std::string addBracketsIfIpv6(const std::string & host_name)
{
    std::string res;

    if (host_name.find_first_of(':') != std::string::npos && !host_name.empty() && host_name.back() != ']')
        res += '[' + host_name + ']';
    else
        res = host_name;
    return res;
}

std::string createHostPortString(const std::string & host, uint16_t port)
{
    return createHostPortString(host, toString(port));
}

std::string createHostPortString(const std::string & host, const std::string & port)
{
    return addBracketsIfIpv6(host) + ':' + port;
}

std::string_view removeBracketsIfIpv6(const std::string & host_name)
{
    if (host_name.find_first_of(':') != std::string::npos &&
        !host_name.empty() &&
        host_name.back() == ']' &&
        host_name.front() == '['
    )
        return std::string_view(host_name.data() + 1, host_name.size() - 2);
    return std::string_view(host_name.c_str());
}

bool isSameHost(const std::string & lhs, const std::string & rhs)
{
    if (lhs == rhs)
        return true;
    return removeBracketsIfIpv6(lhs) == removeBracketsIfIpv6(rhs);
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

}
