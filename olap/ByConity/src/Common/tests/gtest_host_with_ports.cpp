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

#include <vector>
#include <string>
#include <Common/HostWithPorts.h>
#include <googletest/googletest/include/gtest/gtest.h>


using namespace DB;

namespace
{

TEST(HostWithPortsUtils, addBracketsIfIpv6)
{
    EXPECT_EQ(addBracketsIfIpv6(""), std::string{});
    EXPECT_EQ(addBracketsIfIpv6("127.0.0.1"), std::string{"127.0.0.1"});
    EXPECT_EQ(addBracketsIfIpv6("::1"), std::string{"[::1]"});
    EXPECT_EQ(addBracketsIfIpv6("[::1]"), std::string{"[::1]"});
    EXPECT_EQ(addBracketsIfIpv6("www.google.com"), std::string{"www.google.com"});
    EXPECT_EQ(addBracketsIfIpv6("::"), std::string{"[::]"});
    EXPECT_EQ(addBracketsIfIpv6("[::]"), std::string{"[::]"});
}

TEST(HostWithPortsUtils, removeBracketsIfIpv6)
{
    EXPECT_EQ(removeBracketsIfIpv6(""), std::string{});
    EXPECT_EQ(removeBracketsIfIpv6("127.0.0.1"), std::string{"127.0.0.1"});
    EXPECT_EQ(removeBracketsIfIpv6("::1"), std::string{"::1"});
    EXPECT_EQ(removeBracketsIfIpv6("[::1]"), std::string{"::1"});
    EXPECT_EQ(removeBracketsIfIpv6("www.google.com"), std::string{"www.google.com"});
    EXPECT_EQ(removeBracketsIfIpv6("::"), std::string{"::"});
    EXPECT_EQ(removeBracketsIfIpv6("[::]"), std::string{"::"});
}

TEST(HostWithPortsUtils, isSameHost)
{
    bool res;
    res = isSameHost("", "");
    EXPECT_TRUE(res);
    res = isSameHost("::1", "[::1]");
    EXPECT_TRUE(res);
    res = isSameHost("::1", "127.0.0.1");
    EXPECT_FALSE(res);
    res = isSameHost("127.0.0.1", "127.0.0.1");
    EXPECT_TRUE(res);
    res = isSameHost("[::1]", "[::1]");
    EXPECT_TRUE(res);
    res = isSameHost("www.google.com", "www.google.com");
    EXPECT_TRUE(res);
    res = isSameHost("www.google.com", "www.bytedance.com");
    EXPECT_FALSE(res);
}

TEST(HostWithPortsUtils, HostWithPortsGetAddress)
{
    constexpr uint16_t rpc_port = 9000;
    constexpr uint16_t tcp_port = 9001;
    constexpr uint16_t http_port = 9002;
    constexpr uint16_t exchange_port = 9003;
    constexpr uint16_t exchange_status_port = 9004;

    HostWithPorts hp0 {"::1", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hp0.getRPCAddress(), "[::1]:9000");
    EXPECT_EQ(hp0.getTCPAddress(), "[::1]:9001");
    EXPECT_EQ(hp0.getHTTPAddress(), "[::1]:9002");
    EXPECT_EQ(hp0.getExchangeAddress(), "[::1]:9003");
    EXPECT_EQ(hp0.getExchangeStatusAddress(), "[::1]:9004");

    HostWithPorts hp1 {"[::1]", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hp1.getRPCAddress(), "[::1]:9000");
    EXPECT_EQ(hp1.getTCPAddress(), "[::1]:9001");
    EXPECT_EQ(hp1.getHTTPAddress(), "[::1]:9002");
    EXPECT_EQ(hp1.getExchangeAddress(), "[::1]:9003");
    EXPECT_EQ(hp1.getExchangeStatusAddress(), "[::1]:9004");

    HostWithPorts hp2 {"127.0.0.1", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hp2.getRPCAddress(), "127.0.0.1:9000");
    EXPECT_EQ(hp2.getTCPAddress(), "127.0.0.1:9001");
    EXPECT_EQ(hp2.getHTTPAddress(), "127.0.0.1:9002");
    EXPECT_EQ(hp2.getExchangeAddress(), "127.0.0.1:9003");
    EXPECT_EQ(hp2.getExchangeStatusAddress(), "127.0.0.1:9004");

    HostWithPorts hp3 {"www.google.com", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hp3.getRPCAddress(), "www.google.com:9000");
    EXPECT_EQ(hp3.getTCPAddress(), "www.google.com:9001");
    EXPECT_EQ(hp3.getHTTPAddress(), "www.google.com:9002");
    EXPECT_EQ(hp3.getExchangeAddress(), "www.google.com:9003");
    EXPECT_EQ(hp3.getExchangeStatusAddress(), "www.google.com:9004");
}

TEST(HostWithPortsUtils, createHostPortString)
{
    std::string res;
    res = createHostPortString("::", 8000);
    EXPECT_EQ(res, "[::]:8000");
    res = createHostPortString("[::1]", 8000);
    EXPECT_EQ(res, "[::1]:8000");
    res = createHostPortString("127.0.0.1", 8000);
    EXPECT_EQ(res, "127.0.0.1:8000");
    res = createHostPortString("www.google.com", 8000);
    EXPECT_EQ(res, "www.google.com:8000");
}

TEST(HostWithPortsUtils, HostWithPortHash)
{
    constexpr uint16_t rpc_port = 9000;
    constexpr uint16_t tcp_port = 9001;
    constexpr uint16_t http_port = 9002;
    constexpr uint16_t exchange_port = 9003;
    constexpr uint16_t exchange_status_port = 9004;
    std::hash<DB::HostWithPorts> hasher;

    HostWithPorts hp0 {"::1", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hasher(hp0), hasher(hp0));
    HostWithPorts hp1 {"[::1]", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hasher(hp1), hasher(hp1));

    HostWithPorts hp2 {"[1:1:3:1::166]", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    HostWithPorts hp3 {"1:1:3:1::166", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hasher(hp2), hasher(hp3));
    EXPECT_EQ(hasher(hp2), hasher(hp2));
    EXPECT_EQ(hasher(hp3), hasher(hp3));

    HostWithPorts hp4 {"10.1.1.1", rpc_port, tcp_port, http_port, exchange_port, exchange_status_port, ""};
    EXPECT_EQ(hasher(hp4), hasher(hp4));
}

} // end anonymous namespace
