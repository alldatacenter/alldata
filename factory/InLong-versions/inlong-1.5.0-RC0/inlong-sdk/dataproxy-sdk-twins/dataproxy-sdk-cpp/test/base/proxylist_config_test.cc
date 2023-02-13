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

TEST(businfo, basetest)
{
    ProxyInfoPtr proxy = make_shared<ProxyInfo>(1, "0.0.0.0", 11009);
    EXPECT_EQ(proxy->getString(), "[ip:0.0.0.0, port:11009]");
}

TEST(clusterProxylist, readCachelist) //.bus_list.ini文件中缓存有groupid_test1和groupid_test2
{
    EXPECT_EQ(g_config.parseConfig("config.json"), true);
    EXPECT_EQ(Utils::getFirstIpAddr(g_config.ser_ip_), true);

    GlobalClusterMock *m_clusters = new GlobalClusterMock();

    g_clusters = m_clusters;
    EXPECT_CALL(*m_clusters, printAckNum).Times(1);

    g_executors = new ExecutorThreadPool();

    this_thread::sleep_for(std::chrono::seconds(5));
    g_clusters->startUpdateSubroutine();

    g_clusters->addBuslist("test_inlong_group_id1");
    g_clusters->addBuslist("groupid_test1");
    g_clusters->addBuslist("groupid_test2");

    this_thread::sleep_for(std::chrono::minutes(10));

    delete g_clusters;
    delete g_executors;
}

TEST(clusterProxylist, basetest)
{
    EXPECT_EQ(g_config.parseConfig("config.json"), true);
    ClusterProxyListPtr cluster = make_shared<ClusterProxyList>();
    GlobalCluster *g_cluster = new GlobalCluster();
    string meta_info;
    EXPECT_EQ(Utils::readFile("proxylist_old.json", meta_info), true);
    // LOG_INFO("%s", meta_info.c_str());

    EXPECT_EQ(g_cluster->parseAndGet("test_group", meta_info, cluster), 0);
    EXPECT_EQ(cluster->size(), 53);
    EXPECT_EQ(cluster->clusterId(), 70);
    EXPECT_EQ(cluster->switchValue(), 1);
    EXPECT_EQ(cluster->isNeedLoadBalance(), true);

    string info;
    ClusterProxyListPtr cluster2 = make_shared<ClusterProxyList>();
    EXPECT_EQ(Utils::readFile("proxy_new.json", info), true);
    EXPECT_EQ(g_cluster->parseAndGet("test_group", info, cluster2), 0);
    EXPECT_EQ(cluster2->size(), 2);
    EXPECT_EQ(cluster2->clusterId(), 45);
    EXPECT_EQ(cluster2->switchValue(), 0);
    EXPECT_EQ(cluster2->load(), 20);
    EXPECT_EQ(cluster2->isNeedLoadBalance(), false);
    delete g_cluster;
}

TEST(clusterProxylist, addBuslistAndUpdate1)
{
    EXPECT_EQ(g_config.parseConfig("config.json"), true);
    g_clusters = new GlobalCluster();
    g_executors = new ExecutorThreadPool();
    this_thread::sleep_for(std::chrono::seconds(5));

    g_clusters->startUpdateSubroutine();

    this_thread::sleep_for(std::chrono::seconds(2));

    g_clusters->addBuslist("test_inlong_group_id1");

    this_thread::sleep_for(std::chrono::minutes(10));

    delete g_clusters;
    delete g_executors;
    // this_thread::sleep_for(std::chrono::seconds(10));
}

void addWrongBidFunc(GlobalCluster *gcluster)
{
    // this_thread::sleep_for(std::chrono::seconds(30));

    gcluster->addBuslist("b_wrong_bid");
    gcluster->addBuslist("b_ssssssssid");
}

void addExistBidFunc(GlobalCluster *gcluster)
{
    // this_thread::sleep_for(std::chrono::seconds(15));

    gcluster->addBuslist("test_inlong_group_id1");
}

TEST(clusterProxylist, connInit)
{
    EXPECT_EQ(g_config.parseConfig("config.json"), true);
    EXPECT_EQ(Utils::getFirstIpAddr(g_config.ser_ip_), true);

    g_clusters = new GlobalCluster();
    g_executors = new ExecutorThreadPool();

    this_thread::sleep_for(std::chrono::seconds(5));
    g_clusters->startUpdateSubroutine();

    g_clusters->addBuslist("test_inlong_group_id1");

    this_thread::sleep_for(std::chrono::seconds(1));

    std::thread th1(bind(addWrongBidFunc, g_clusters));
    th1.detach();
    std::thread th2(bind(addExistBidFunc, g_clusters));
    th2.detach();

    this_thread::sleep_for(std::chrono::minutes(20));

    delete g_clusters;
    delete g_executors;

    this_thread::sleep_for(std::chrono::minutes(1));
}

int main(int argc, char *argv[])
{
    testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}