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

using namespace testing;

// #if 0
TEST(packqueue, basetest)
{
    g_config.msg_type_    = 3;
    g_config.retry_num_   = 100;
    g_config.enable_pack_ = false;

    ExecutorThreadPtr th1 = make_shared<ExecutorThread>(1);
    ProxyInfoPtr proxy        = make_shared<ProxyInfo>(1, "127.0.0.1", 4000);
    ConnectionPtr conn1   = make_shared<Connection>(th1, proxy);

    string inlong_group_id     = "inlong_groupid_1";
    string inlong_stream_id     = "inlong_streamid_1";
    PackQueuePtr q = make_shared<PackQueue>(inlong_group_id, inlong_stream_id);
    string msg     = "test testwoehgorirehjtirjhhgklwpgJpwjgehreahethtn aethrtshtrs";
    cout << msg.size() << endl;
    string attr = "inlong_group_id=inlong_groupid_1&inlong_stream_id=inlong_streamid_1&dt=1637484644807&mid=1&cnt=1&sid=0x098714192b3bed27144b48f621800000";
    cout << "attrlen:" << attr.size() << endl;

    EXPECT_EQ(q->appendMsg(msg, "", 0, NULL), 0);
    EXPECT_NE(q->dataTime(), 0);
    EXPECT_EQ(q->curLen(), msg.size() + 1);
    cout << q->data() << endl;
    EXPECT_EQ(q->inlong_group_id(), inlong_group_id);
    string topic = "inlong_group_id=" + inlong_group_id + "&inlong_stream_id=" + inlong_stream_id;
    EXPECT_EQ(q->topicDesc(), topic);
    // EXPECT_EQ(q->isTriggerPack(0, msg.size()), false);

    TotalPoolsMock* gMock      = new TotalPoolsMock();
    BufferPoolMockPtr poolMock = make_shared<BufferPoolMock>(0, 10, 1000);
    EXPECT_CALL(*gMock, getPool).WillRepeatedly(Return(poolMock));
    g_pools = gMock;
    // EXPECT_EQ(g_pools->getPool(0), poolMock);

    GlobalClusterMock* clusterMock = new GlobalClusterMock();
    EXPECT_CALL(*clusterMock, getSendConn).WillRepeatedly(Return(conn1));
    g_clusters = clusterMock;
    // EXPECT_EQ(g_clusters->getSendConn("bidd"), conn1);

    // EXPECT_EQ(q->writeToBuf(), 0);

    this_thread::sleep_for(chrono::minutes(1));

    delete gMock;
    delete clusterMock;
}
// #endif
TEST(packzip, test)
{
    g_config.msg_type_    = 3;
    g_config.enable_pack_ = false;
    g_config.enable_zip_  = false;

    string msg2(1024, 'a');
    msg2 += "end";
    cout << "msg2 len" << msg2.size() << endl;

    string inlong_group_id     = "inlong_groupid_1";
    string inlong_stream_id     = "inlong_streamid_1";
    PackQueuePtr q = make_shared<PackQueue>(inlong_group_id, inlong_stream_id);
    q->appendMsg(msg2, "", 0, NULL);

    SendBuffer* buf = new SendBuffer(40000000);
    uint32_t outlen = 0;
    q->packOperate(buf->content(), outlen, 10);
    buf->setLen(outlen);
}

TEST(packqueue, packtest)
{
    g_config.msg_type_    = 5;
    g_config.retry_num_   = 100;
    g_config.enable_pack_ = false;
    g_config.enable_zip_  = false;

    string inlong_group_id     = "inlong_groupid_1";
    string inlong_stream_id     = "inlong_streamid_1";
    PackQueuePtr q = make_shared<PackQueue>(inlong_group_id, inlong_stream_id);
    string msg(1024, 'a');
    cout << msg.size() << endl;
    string attr = "inlong_group_id=inlong_groupid_1&inlong_stream_id=inlong_streamid_1&dt=1637484644807&mid=1&cnt=1&sid=0x098714192b3bed27144b48f621800000";
    cout << "attrlen:" << attr.size() << endl;

    q->appendMsg(msg, "", 0, NULL);
    string topic = "inlong_group_id=" + inlong_group_id + "&inlong_stream_id=" + inlong_stream_id;

    SendBuffer* buf = new SendBuffer(20000);

    if (!buf) return;
    uint32_t out_len = 0;
    LOG_DEBUG("buf content is%d", buf->content());
    q->packOperate(buf->content(), out_len, 1);
    buf->setLen(out_len);
    LOG_DEBUG("buf content is%d", buf->content());
    delete buf;
}

int main(int argc, char* argv[])
{

    g_config.parseConfig("config.json");
    Utils::getFirstIpAddr(g_config.ser_ip_);

    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}