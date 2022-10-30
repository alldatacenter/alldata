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

string base_Streamid  = "cpptest_";
string base_bid  = "inlong_groupid_test";
int milliseconds = 0;

string strRand(int length)
{               
    char tmp;       
    string buffer;  

    random_device rd;                    
    default_random_engine random(rd());  
    for (int i = 0; i < length; i++)
    {
        tmp = random() % 36;  
        if (tmp < 10)
        {
            tmp += '0';
        }
        else
        {
            tmp -= 10;
            tmp += 'A';
        }
        buffer += tmp;
    }
    return buffer;
}

void sendFunc()
{
    while (true)
    {
        srand((unsigned)time(NULL));
        string inlong_stream_id = base_Streamid + to_string(rand() % 10);

        srand((unsigned)time(NULL));
        string inlong_group_id = base_bid + to_string(rand() % 3);

        srand((unsigned)time(NULL));
        int32_t base   = 1024 * 1024 * 2;
        int32_t msglen = 100 + rand() % base;
        string msg     = strRand(msglen);

        int32_t ret = tc_api_send(inlong_group_id.data(), inlong_stream_id.data(), msg.data(), msg.size());
        if (ret) this_thread::sleep_for(chrono::milliseconds(milliseconds));
    }
}

int main(int argc, char* argv[])  //input: thread_num, msgsize_idx, send_times
{
    if (argc < 3)
    {
        cout << "usage: ./random_send.cc <thread_num> <milliseconds>" << endl;
        return 0;
    }

    int thread_num = atoi(argv[1]);  //send thread num
    milliseconds   = atol(argv[2]);

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
        t_set[i] = thread(sendFunc);
    }
    for (size_t i = 0; i < thread_num; i++)
    {
        t_set[i].join();
    }

    tc_api_close(10000);
    // #endif
    return 0;
}