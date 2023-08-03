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

#include <DaemonManager/DaemonJobGlobalGC.h>
#include <CloudServices/CnchServerClient.h>
#include <gtest/gtest.h>

namespace GtestDaemonJobGlobalGC
{

using namespace DB;
using namespace DB::DaemonManager::GlobalGCHelpers;

TEST(DaemonJobGlobalGC, sortByValue_test_normal_case)
{
    using DB::DaemonManager::GlobalGCHelpers::sortByValue;
    std::vector<std::pair<String, long>> num_of_table_can_send {
        {"169.128.0.1:1223", 10},
        {"169.128.0.2:1223", 11},
        {"169.128.0.3:1223", 1},
        {"169.128.0.4:1223", 2}
    };

    std::vector<std::pair<String, long>> sorted = sortByValue(std::move(num_of_table_can_send));

    std::vector<std::pair<String, long>> expected_result {
        {"169.128.0.3:1223", 1},
        {"169.128.0.4:1223", 2},
        {"169.128.0.1:1223", 10},
        {"169.128.0.2:1223", 11}
    };

    EXPECT_EQ(sorted, expected_result);
}

TEST(DaemonJobGlobalGC, sortByValue_test_empty_case)
{
    using DB::DaemonManager::GlobalGCHelpers::sortByValue;
    std::vector<std::pair<String, long>> num_of_table_can_send;
    std::vector<std::pair<String, long>> sorted = sortByValue(std::move(num_of_table_can_send));
    EXPECT_EQ(sorted.empty(), true);
}

bool sendToServerForGCImplDummy(CnchServerClient & client, const std::vector<DB::Protos::DataModelTable> &)
{
    if (client.getRPCAddress() == "127.0.0.1:1606")
        return true;
    return false;
}

std::vector<CnchServerClientPtr> makeClients(const std::vector<HostWithPorts> & hps)
{
    std::vector<CnchServerClientPtr> res;
    for (auto & hp : hps)
        res.push_back(std::make_shared<CnchServerClient>(hp));
    return res;
}

std::vector<Protos::DataModelTable> createTables(unsigned int begin, unsigned int end)
{
    int size = end - begin;
    std::vector<Protos::DataModelTable> res(size);
    for (unsigned int i = 0; i < res.size(); ++i)
    {
        RPCHelpers::fillUUID(UUID{UInt128(std::initializer_list<uint64_t>{begin + i, 0})}, *res[i].mutable_uuid());
    }

    return res;
}


TEST(DaemonJobGlobalGC, sendToServerForGC_test)
{
    using DB::DaemonManager::GlobalGCHelpers::sendToServerForGC;
    Poco::Logger * log = &Poco::Logger::get("test_log");
    std::vector<HostWithPorts> hps {
        {"127.0.0.1", 1606, 1224, 1225},
        {"127.0.0.2", 1606, 1224, 1225},
    };
    std::vector<CnchServerClientPtr> clients = makeClients(hps);
    std::vector<DB::Protos::DataModelTable> tables = createTables(1,6);

    {
        std::vector<std::pair<String, long>> num_of_table_can_send {
            {"127.0.0.1:1606", 4},
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3}
        };

        num_of_table_can_send = sortByValue(std::move(num_of_table_can_send));
        bool ret = sendToServerForGC(
            tables,
            num_of_table_can_send,
            clients,
            sendToServerForGCImplDummy,
            log);

        EXPECT_EQ(ret, false);
        EXPECT_EQ(num_of_table_can_send.empty(), true);
    }

    {
        std::vector<std::pair<String, long>> num_of_table_can_send {
            {"127.0.0.1:1606", 20},
            {"127.0.0.2:1606", 50},
            {"127.0.0.3:1606", 100}
        };

        num_of_table_can_send = sortByValue(std::move(num_of_table_can_send));
        bool ret = sendToServerForGC(
            tables,
            num_of_table_can_send,
            clients,
            sendToServerForGCImplDummy,
            log);

        EXPECT_EQ(ret, true);
        std::vector<std::pair<String, long>> num_of_table_can_send_after {
            {"127.0.0.1:1606", 15}
        };
        EXPECT_EQ(num_of_table_can_send_after, num_of_table_can_send);
    }

    {
        std::vector<std::pair<String, long>> num_of_table_can_send {
            {"127.0.0.1:1606", 15},
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 30}
        };

        num_of_table_can_send = sortByValue(std::move(num_of_table_can_send));
        bool ret = sendToServerForGC(
            tables,
            num_of_table_can_send,
            clients,
            sendToServerForGCImplDummy,
            log);

        EXPECT_EQ(ret, true);
        std::vector<std::pair<String, long>> num_of_table_can_send_after {
            {"127.0.0.2:1606", 2},
            {"127.0.0.1:1606", 10}
        };
        EXPECT_EQ(num_of_table_can_send_after, num_of_table_can_send);
    }

    {
        std::vector<std::pair<String, long>> num_of_table_can_send {
            {"127.0.0.1:1606", 15},
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3}
        };

        num_of_table_can_send = sortByValue(std::move(num_of_table_can_send));
        bool ret = sendToServerForGC(
            tables,
            num_of_table_can_send,
            clients,
            sendToServerForGCImplDummy,
            log);

        EXPECT_EQ(ret, true);
        std::vector<std::pair<String, long>> num_of_table_can_send_after {
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3},
            {"127.0.0.1:1606", 10}
        };
        EXPECT_EQ(num_of_table_can_send_after, num_of_table_can_send);
    }

    {
        std::vector<std::pair<String, long>> num_of_table_can_send {
            {"127.0.0.1:1606", 5},
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3}
        };

        num_of_table_can_send = sortByValue(std::move(num_of_table_can_send));
        bool ret = sendToServerForGC(
            tables,
            num_of_table_can_send,
            clients,
            sendToServerForGCImplDummy,
            log);

        EXPECT_EQ(ret, true);
        std::vector<std::pair<String, long>> num_of_table_can_send_after {
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3}
        };
        EXPECT_EQ(num_of_table_can_send_after, num_of_table_can_send);
    }

    {
        std::vector<std::pair<String, long>> num_of_table_can_send {
            {"127.0.0.1:1606", 6},
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3}
        };

        num_of_table_can_send = sortByValue(std::move(num_of_table_can_send));
        bool ret = sendToServerForGC(
            tables,
            num_of_table_can_send,
            clients,
            sendToServerForGCImplDummy,
            log);

        EXPECT_EQ(ret, true);
        std::vector<std::pair<String, long>> num_of_table_can_send_after {
            {"127.0.0.1:1606", 1},
            {"127.0.0.2:1606", 2},
            {"127.0.0.3:1606", 3}
        };
        EXPECT_EQ(num_of_table_can_send_after, num_of_table_can_send);
    }
}
} /// end namespace GtestGlobalGC
