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
#include <stdlib.h>
#include <string>
#include <thread>
#include <time.h>

using namespace std;
using namespace dataproxy_sdk;

int send_times = 1;

// 0:100B, 1:1K, 2:100K, 3:500K, 4:2M, 5:5M,6:10M
uint32_t msgsize[7] = {100, 1024, 1024 * 100, 1024 * 500, 1024 * 1024 * 2, 1024 * 1024 * 5, 1024 * 1024 * 10};

string base_Streamid  = "cpptest_";
string base_bid  = "inlong_groupid_test";
int Streamid_num      = 10;
int bid_num      = 1;
int milliseconds = 0;

void sendFunc(const string& msg)
{
    for (size_t i = 0; i < send_times; i++)
    {
        srand((unsigned)time(NULL));
        string inlong_stream_id = base_Streamid + to_string(rand() % Streamid_num);

        srand((unsigned)time(NULL));
        string inlong_group_id  = base_bid + to_string(rand() % bid_num);
        int32_t ret = tc_api_send(inlong_group_id.data(), inlong_stream_id.data(), msg.data(), msg.size());
        if (ret) this_thread::sleep_for(chrono::milliseconds(milliseconds));
    }
}

void sendLimitTimes(const string& msg)
{
    for (size_t i = 0; i < send_times; i++)
    {
        // if (i % 10 == 5) { this_thread::sleep_for(chrono::seconds(40)); }  //test normal hb

        srand((unsigned)time(NULL));
        string inlong_stream_id = base_Streamid + to_string(rand() % Streamid_num);

        srand((unsigned)time(NULL));
        string inlong_group_id  = base_bid + to_string(rand() % bid_num);
        int32_t ret = tc_api_send(inlong_group_id.data(), inlong_stream_id.data(), msg.data(), msg.size());
    }

    this_thread::sleep_for(chrono::seconds(100));
}

int main(int argc, char* argv[])  //input： thread_num, msgsize_idx, send_times
{
    if (argc < 5)
    {
        cout << "usage: ./multi_thread_send <thread_num> <msgsize_idx> <Streamid_num> <bid_num> <milliseconds> <rumtimes>" << endl;
        return 0;
    }

    int thread_num = atoi(argv[1]);  //send thread num
    int idx        = atoi(argv[2]);
    if (idx > 6) idx = 1;  //default: 10K
    uint32_t msglen = msgsize[idx];
    Streamid_num         = atol(argv[3]);
    bid_num         = atol(argv[4]);

    if (argc >= 6) milliseconds = atol(argv[5]);
    if (argc >= 7) send_times = atol(argv[6]);

    string msg(msglen, 'c');
    msg += "end";

    // #if 0

    if (tc_api_init("config.json"))
    {
        cout << "init error" << endl;
        return 0;
    }

    this_thread::sleep_for(chrono::seconds(1));
    thread t_set[thread_num];

    for (size_t i = 0; i < thread_num; i++)
    {
        if (send_times != 1) { t_set[i] = thread(bind(sendLimitTimes, msg)); }
        else
        {
            t_set[i] = thread(bind(sendFunc, msg));
        }
        // t.join();
    }
    for (size_t i = 0; i < thread_num; i++)
    {
        t_set[i].join();
    }

    tc_api_close(3000);
    // #endif
    return 0;
}