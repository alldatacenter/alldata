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

#include <chrono>
#include <cstddef>
#include <exception>
#include <iostream>
#include <string>
#include <thread>
#include <fmt/core.h>
#include <fmt/format.h>
#include <deque>

#include <TSO/TSOClient.h>
#include <common/logger_useful.h>
#include <Common/Exception.h>


int main(int argc, char ** argv)
{
    std::deque<DB::TSO::TSOClient> tso_clients;
    for (int i = 1; i < argc; ++i)
    {
        tso_clients.emplace_back(std::string{argv[i]});
    }

    UInt64 prev_ts = 0;
    for (size_t i = 1; i != 0; ++i)
    {
        for (auto & client: tso_clients)
        {
            try
            {
                auto res = client.getTimestamp();

                if (res.is_leader() && res.timestamp() <= prev_ts)
                {
                    std::cerr << fmt::format("Error timestamp, curr_timestamp {}, prev_ts: ", res.timestamp(), prev_ts) << std::endl;
                    std::terminate();
                }
                else if (res.is_leader())
                {
                    prev_ts = res.timestamp();
                }

                auto message = fmt::format(
                    "ts: {}, is_leader: {}, i: {}",
                    res.has_timestamp() ? res.timestamp(): 0,
                    res.has_is_leader() ? res.is_leader(): false,
                    i);

                if (i % 10000 == 0)
                    std::cout << message << std::endl;
            }
            catch (...)
            {
                DB::tryLogCurrentException(__PRETTY_FUNCTION__);
            }
        }
    }
    return 0;
}
