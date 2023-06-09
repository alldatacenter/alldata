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

#pragma once
#include <cstdint>
#include <functional>
#include <ostream>
#include <string>
#include <vector>
#include <fmt/core.h>
#include <common/getFQDNOrHostName.h>
#include <Interpreters/Context_fwd.h>

namespace DB
{
class HostWithPorts;
using HostWithPortsVec = std::vector<HostWithPorts>;

inline const std::string & getHostIPFromEnv()
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

inline const char * getLoopbackIPFromEnv()
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

inline std::string addBracketsIfIpv6(const std::string & host_name)
{
    if (host_name.find_first_of(':') != std::string::npos && !host_name.empty() && host_name.back() != ']')
        return fmt::format("[{}]", host_name);
    else
        return host_name;
}

inline std::string createHostPortString(const std::string & host, uint16_t port)
{
    return fmt::format("{}:{}", addBracketsIfIpv6(host), port);
}

inline std::string createHostPortString(const std::string & host, const std::string & port)
{
    return fmt::format("{}:{}", addBracketsIfIpv6(host), port);
}

std::string getWorkerID(ContextPtr context);
std::string getWorkerGroupID(ContextPtr context);
std::string getVirtualWareHouseID(ContextPtr context);

inline std::string_view removeBracketsIfIpv6(const std::string & host_name)
{
    if (host_name.find_first_of(':') != std::string::npos &&
        !host_name.empty() &&
        host_name.back() == ']' &&
        host_name.front() == '['
    )
        return std::string_view(host_name.data() + 1, host_name.size() - 2);
    return std::string_view(host_name.c_str());
}

inline bool isSameHost(const std::string & lhs, const std::string & rhs)
{
    if (lhs == rhs)
        return true;
    return removeBracketsIfIpv6(lhs) == removeBracketsIfIpv6(rhs);
}

class HostWithPorts
{
public:
    HostWithPorts() = default;
    HostWithPorts(const std::string & host_, uint16_t rpc_port_ = 0, uint16_t tcp_port_ = 0, uint16_t http_port_ = 0, uint16_t exchange_port_ = 0, uint16_t exchange_status_port_ = 0, std::string id_ = {})
        : id{std::move(id_)},
          rpc_port{rpc_port_},
          tcp_port{tcp_port_},
          http_port{http_port_},
          exchange_port{exchange_port_},
          exchange_status_port{exchange_status_port_},
          host{removeBracketsIfIpv6(host_)}
    {}

    std::string id;
    uint16_t rpc_port{0};
    uint16_t tcp_port{0};
    uint16_t http_port{0};
    uint16_t exchange_port{0};
    uint16_t exchange_status_port{0};
private:
    std::string host;
public:

    bool empty() const { return host.empty() || (rpc_port == 0 && tcp_port == 0); }

    std::string getRPCAddress() const { return fmt::format("{}:{}", addBracketsIfIpv6(host), std::to_string(rpc_port)); }
    std::string getTCPAddress() const { return fmt::format("{}:{}", addBracketsIfIpv6(host), std::to_string(tcp_port)); }
    std::string getHTTPAddress() const { return fmt::format("{}:{}", addBracketsIfIpv6(host), std::to_string(http_port)); }
    std::string getExchangeAddress() const { return fmt::format("{}:{}", addBracketsIfIpv6(host), std::to_string(exchange_port)); }
    std::string getExchangeStatusAddress() const { return fmt::format("{}:{}", addBracketsIfIpv6(host), std::to_string(exchange_status_port)); }

    const std::string & getHost() const { return host; }
    uint16_t getTCPPort() const { return tcp_port; }
    uint16_t getHTTPPort() const { return http_port; }
    uint16_t getRPCPort() const { return rpc_port; }
    std::string toDebugString() const;

    static HostWithPorts fromRPCAddress(const std::string & s);

    /// NOTE: PLEASE DO NOT implement any comparison operator which is a kind of bad code style

    struct IsSameEndpoint
    {
        bool operator()(const HostWithPorts & lhs, const HostWithPorts & rhs) const
        {
            return isSameHost(lhs.host, rhs.host) && lhs.rpc_port == rhs.rpc_port && lhs.tcp_port == rhs.tcp_port;
        }
    };

    struct IsExactlySame
    {
        bool operator()(const HostWithPorts & lhs, const HostWithPorts & rhs) const
        {
            return lhs.id == rhs.id && isSameHost(lhs.host, rhs.host) && lhs.rpc_port == rhs.rpc_port && lhs.tcp_port == rhs.tcp_port
                && lhs.http_port == rhs.http_port && lhs.exchange_port == rhs.exchange_port
                && lhs.exchange_status_port == rhs.exchange_status_port;
        }
    };

    bool isSameEndpoint(const HostWithPorts & rhs) const
    {
        return IsSameEndpoint{}(*this, rhs);
    }

    bool isExactlySame(const HostWithPorts & rhs) const { return IsExactlySame{}(*this, rhs); }

    static bool isExactlySameVec(const HostWithPortsVec & lhs, const HostWithPortsVec & rhs);
};

std::ostream & operator<<(std::ostream & os, const HostWithPorts & host_ports);

}

namespace std
{
template <>
struct hash<DB::HostWithPorts>
{
    std::size_t operator()(const DB::HostWithPorts & hp) const
    {
        return std::hash<string>()(DB::addBracketsIfIpv6(hp.getHost())) ^ std::hash<uint16_t>()(hp.rpc_port) ^ (std::hash<uint16_t>()(hp.tcp_port) << 16);
    }
};

template <>
struct equal_to<DB::HostWithPorts>
{
    bool operator()(const DB::HostWithPorts & lhs, const DB::HostWithPorts & rhs) const
    {
        return DB::HostWithPorts::IsSameEndpoint{}(lhs, rhs);
    }
};

}
