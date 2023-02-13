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
#define private public
#include "../common.h"

ExecutorThreadPtr th1 = make_shared<ExecutorThread>(1);
ProxyInfoPtr proxy        = make_shared<ProxyInfo>(1, "127.0.0.1", 4000);
ConnectionPtr conn1   = make_shared<Connection>(th1, proxy);

TEST(connection, sendBufTest1)
{
    g_config.msg_type_    = 3;
    g_config.retry_num_   = 100;
    g_config.enable_pack_ = false;

    EXPECT_EQ(conn1->getThreadId(), 1);
    EXPECT_EQ(conn1->getWaitingSend(), 0);
    conn1->decreaseWaiting();
    EXPECT_EQ(conn1->getWaitingSend(), 0);
    EXPECT_EQ(conn1->getRemoteInfo(), "[ip:127.0.0.1, port:4000]");

    this_thread::sleep_for(chrono::seconds(3));
    EXPECT_EQ(conn1->isConnected(), true);

    string inlong_group_id     = "inlong_groupid_test";
    string inlong_stream_id     = "inlong_streamid_test";
    PackQueuePtr q = make_shared<PackQueue>(inlong_group_id, inlong_stream_id);
    string msg     = "test testwoehgorhgklwpgJpwjgehreahethtn aethrtshtrs";

    EXPECT_EQ(q->appendMsg(msg, "", 0, NULL), 0);
    EXPECT_NE(q->dataTime(), 0);
    EXPECT_EQ(q->curLen(), msg.size());
    EXPECT_EQ(q->data(), msg.c_str());
    EXPECT_EQ(q->inlong_group_id(), inlong_group_id);
    string topic = "inlong_group_id=" + inlong_group_id + "&inlong_stream_id=" + inlong_stream_id;
    EXPECT_EQ(q->topicDesc(), topic);

    this_thread::sleep_for(chrono::minutes(2));
}

int main(int argc, char* argv[])
{
    g_config.parseConfig("config.json");

    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}