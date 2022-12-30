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

#include <chrono>
#include <iostream>
#include <string>
#include <tc_api.h>

using namespace std;
using namespace dataproxy_sdk;

// user set call_back func
int call_back_func(const char *inlong_group_id, const char *inlong_stream_id, const char *msg, int msg_len, long report_time, const char *ip)
{
    cout << "******this is call back, print info******" << endl;
    cout << "inlong_group_id: " << inlong_group_id << ", inlong_stream_id: " << inlong_stream_id << endl;
    cout << "msg_len: " << msg_len << ", msg content: " << msg << endl;
    cout << "report_time: " << report_time << ", client ip: " << ip << endl;
    cout << "******call back end******" << endl;

    return 0;
}

int main(int argc, char const *argv[])
{
    if (2 != argc)
    {
        cout << "USAGE: ./send_demo ../config/config_example.json" << endl;
        return 0;
    }

    // step1. init
    if (tc_api_init(argv[1]))
    {
        cout << "init error" << endl;
        return 0;
    }
    cout << "---->start sdk successfully" << endl;

    int count = 1000;
    string inlong_group_id = "test_20220727_86";
    string inlong_stream_id = "test_20220727_86_str_17";
    string msg = "this is a test ttttttttttttttt; eiwhgreuhg jfdiowaehgorerlea; test end";

    // step2. send
    cout << "---->start tc_api_send" << endl;
    for (size_t i = 0; i < count; i++)
    {
        if (tc_api_send(inlong_group_id.c_str(), inlong_stream_id.c_str(), msg.c_str(), msg.length(), call_back_func))
        {
            cout << "tc_api_send error;"
                 << " ";
        }
    }

    string bad_groupid="test_wrong_groupid";
    int32_t bad_res=tc_api_send(bad_groupid.c_str(), inlong_stream_id.c_str(), msg.c_str(), msg.length(), call_back_func);
    cout << endl << "send bad inlong_group_id res:"<<bad_res;

    cout << endl
         << "---->start tc_api_send_ext" << endl;
    for (size_t i = 0; i < count; i++)
    {
        auto time_now = chrono::system_clock::now();
        auto duration_in_ms = chrono::duration_cast<chrono::milliseconds>(time_now.time_since_epoch());
        auto dt = duration_in_ms.count();
        if (tc_api_send_ext(inlong_group_id.c_str(), inlong_stream_id.c_str(), msg.c_str(), msg.length(), dt, call_back_func))
        {
            cout << "tc_api_send_ext error;"
                 << " ";
        }
    }

    cout << endl
         << "---->start tc_api_send_base" << endl;
    for (size_t i = 0; i < count; i++)
    {
        auto time_now = chrono::system_clock::now();
        auto duration_in_ms = chrono::duration_cast<chrono::milliseconds>(time_now.time_since_epoch());
        auto dt = duration_in_ms.count();
        if (tc_api_send_base(inlong_group_id.c_str(), inlong_stream_id.c_str(), msg.c_str(), msg.length(), dt, "127.0.0.1"))
        {
            cout << "tc_api_send_base error;"
                 << " ";
        }
    }

    // std::this_thread::sleep_for(std::chrono::minutes(2));

    // step3. close
    if (tc_api_close(1000))
    {
        cout << "close sdk error" << endl;
    }
    else
    {
        cout << "---->close sdk successfully" << endl;
    }

    return 0;
}
