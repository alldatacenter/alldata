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

#include "../common.h"
#include "tc_api.h"
#include <chrono>
#include <iostream>
#include <string>
#include <thread>

using namespace std;
using namespace dataproxy_sdk;

int run_times = 1;

TEST(msgsize, size100Btest)
{
    string msg2(100, 'a');
    msg2 += "end";
    for (int32_t i = 0; i < run_times; i++)
    {
        int32_t ret2 = tc_api_send("inlong_groupid_test", "100Btest", msg2.c_str(), msg2.length());
        cout << ret2 << "; ";
    }
}

TEST(msgsize, size1Ktest)
{
    string msg2(1024, 'a');
    msg2 += "end";
    for (int32_t i = 0; i < run_times; i++)
    {
        int32_t ret2 = tc_api_send("inlong_groupid_test", "1Ktest", msg2.c_str(), msg2.length());
        cout << ret2 << "; ";
    }
}

TEST(msgsize, size100Ktest)
{
    string msg2(1024 * 100, 'a');
    msg2 += "end";
    for (int32_t i = 0; i < run_times; i++)
    {
        int32_t ret2 = tc_api_send("inlong_groupid_test", "100Ktest", msg2.c_str(), msg2.length());
        // this_thread::sleep_for(chrono::milliseconds(1));
        cout << ret2 << "; ";
    }
}

TEST(msgsize, size500Ktest)
{
    string msg2(1024 * 500, 'a');
    msg2 += "end";
    for (int32_t i = 0; i < run_times; i++)
    {
        int32_t ret2 = tc_api_send("inlong_groupid_test", "500Ktest", msg2.c_str(), msg2.length());
        cout << ret2 << "; ";
        // this_thread::sleep_for(chrono::milliseconds(1));
    }
}

TEST(msgsize, size1Mtest)
{
    string msg2(1024 * 1024, 'a');
    msg2 += "end";
    for (int32_t i = 0; i < run_times; i++)
    {
        int32_t ret2 = tc_api_send("inlong_groupid_test", "1Mtest", msg2.c_str(), msg2.length());
        cout << ret2 << "; ";
    }
}

TEST(msgsize, size2Mtest)
{
    string msg2(1024 * 1024 * 2, 'a');
    msg2 += "end";
    for (int32_t i = 0; i < run_times; i++)
    {
        int32_t ret2 = tc_api_send("inlong_groupid_test", "2Mtest", msg2.c_str(), msg2.length());
        cout << ret2 << "; ";
    }
}

TEST(msgsize, size5Mtest)
{
    string msg5(1024 * 1024 * 5, 'a');
    msg5 += "end";
    for (int i = 0; i < run_times; i++)
    {
        int32_t ret5 = tc_api_send("inlong_groupid_test", "5Mtest", msg5.c_str(), msg5.length());
        cout << ret5 << "; ";
    }
}

TEST(msgsize, size10Mtest)
{
    string msg10(1024 * 1024 * 10, 'a');
    msg10 += "end";
    for (int i = 0; i < run_times; i++)
    {
        int32_t ret10 = tc_api_send("inlong_groupid_test", "10Mtest", msg10.c_str(), msg10.length());
        cout << ret10 << "; ";
        // this_thread::sleep_for(chrono::milliseconds(10));
    }
}

int32_t callBackFunc(string inlong_group_id, string inlong_stream_id)
{
    cout << "inlong_group_id----" << inlong_group_id << "----inlong_stream_id" << inlong_stream_id << "send error" << endl;
    return -1;
}

int main(int argc, char* argv[])
{
    if (tc_api_init("config.json"))
    {
        cout << "init error" << endl;
        return 0;
    }
    // this_thread::sleep_for(chrono::milliseconds(2));
    run_times = atoi(argv[1]);
    InitGoogleTest(&argc, argv);

    RUN_ALL_TESTS();

    tc_api_close(10000);
    return 0;
}
