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

#include <TSO/TSOClient.h>
#include <Common/ThreadPool.h>
#include <gflags/gflags.h>
#include <sstream>
#include <iostream>
#include <iomanip>
#include <vector>


#define log_message(msg) { \
    std::stringstream ss; \
    ss << msg; \
    std::cout << ss.str() << std::endl; \
    }

#define ALIGN_WIDTH 50

DEFINE_bool(h, false, "print out help message");
DEFINE_string(host, "127.0.0.1:8080", "TSO server address");
DEFINE_int32(client, 10, "Number of clients");
DEFINE_int32(task, 10, "Number of tasks for each client");
DEFINE_int32(interval, 10, "Interval of each task (milliseconds)");

constexpr size_t LOGICAL_BITS = 18;
constexpr size_t LOGICAL_BITMASK = 0x3FFFF;

std::string getPhysicalTime(DB::UInt64 timestamp)
{
    timestamp = timestamp >> LOGICAL_BITS;  /// get the physical time
    auto milli = timestamp + 8 * 60 * 60 * 1000;  /// specified to Beijing timezone
    auto mTime = std::chrono::milliseconds(milli);
    auto tp = std::chrono::time_point<std::chrono::system_clock,std::chrono::milliseconds>(mTime);
    auto tt = std::chrono::system_clock::to_time_t(tp);
    auto now = std::gmtime(&tt);

    std::stringstream ss;
    ss << now->tm_year + 1900
       << "/" << std::setfill('0') << std::setw(2) << now->tm_mon + 1
       << "/" << std::setfill('0') << std::setw(2) << now->tm_mday
       << " " << std::setfill('0') << std::setw(2) << now->tm_hour
       << ":" << std::setfill('0') << std::setw(2) << now->tm_min
       << ":" << std::setfill('0') << std::setw(2) << now->tm_sec;

    return ss.str();
}

std::string getLogicalTime(DB::UInt64 timestamp)
{
    std::stringstream ss;
    ss << (timestamp & LOGICAL_BITMASK);

    return ss.str();
}

int main(int argc, char ** argv)
{
    gflags::ParseCommandLineFlags(&argc, &argv, true);

    if (FLAGS_h)
    {
        log_message("Main options:\n"
                    << std::setiosflags(std::ios::left)
                    << std::setw(ALIGN_WIDTH) << "  --h" << "produce help message\n"
                    << std::setw(ALIGN_WIDTH) << "  --host arg (=127.0.0.1:8080)" << "tso server host address\n"
                    << std::setw(ALIGN_WIDTH) << "  --client arg (=10)" << "number of clients\n"
                    << std::setw(ALIGN_WIDTH) << "  --task arg (=10)" << "number of tasks for each client\n"
                    << std::setw(ALIGN_WIDTH) << "  --interval arg (=10)" << "interval of each task (milliseconds)\n")
        return 0;
    }

    std::string  server_addr = FLAGS_host;
    log_message("Connect to server : " << server_addr)

    /// single test
    log_message(std::endl << "TSO service Single Test:")

    DB::TSO::TSOClient client(server_addr);

    DB::UInt64 first_timestamp = client.getTimestamp().timestamp();
    log_message("Next TSO value: 0x" << std::setiosflags(std::ios::uppercase) << std::hex << first_timestamp)
    log_message("Physical time: " << getPhysicalTime(first_timestamp))
    log_message("Logical time: " << getLogicalTime(first_timestamp))

    /// concurrency test
    log_message(std::endl << "TSO service Concurrency Test:")

    size_t num_clients = FLAGS_client;
    size_t num_tasks = FLAGS_task;
    size_t interval = FLAGS_interval;
    log_message("Number of TSO clients: " << num_clients)
    log_message("Number of tasks for each client: " << num_tasks)
    log_message("Interval of each task: " << interval << " ms")

    std::vector<DB::TSO::TSOClientPtr> client_pool;
    for (size_t i = 0; i < num_clients; i++)
    {
        client_pool.push_back(std::make_shared<DB::TSO::TSOClient>(server_addr));
    }

    ThreadPool thread_pool(num_clients);

    for (size_t i = 0; i < num_clients; i++)
    {
        auto task = [&, i]
        {
            for (size_t j = 0; j < num_tasks; j++)
            {
                DB::UInt64 timestamp = client_pool[i]->getTimestamp().timestamp();
                log_message("Client[" << i << "] Task[" << j << "] get physical time: " << getPhysicalTime(timestamp)
                                      << ", logical time: " << getLogicalTime(timestamp))
                std::this_thread::sleep_for(std::chrono::milliseconds(interval));
            }
        };

        thread_pool.trySchedule(task);
    }

    /// wait for finish of all tasks
    thread_pool.wait();

    log_message(std::endl << "Finished test.")

    return 0;
}
