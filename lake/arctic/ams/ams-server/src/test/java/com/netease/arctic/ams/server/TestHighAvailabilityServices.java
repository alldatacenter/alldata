/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.MockZookeeperServer;
import com.netease.arctic.ams.api.client.AmsServerInfo;
import com.netease.arctic.ams.api.client.ArcticThriftUrl;
import com.netease.arctic.ams.api.client.ZookeeperService;
import com.netease.arctic.ams.api.properties.AmsHAProperties;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;

public class TestHighAvailabilityServices {
  CuratorFramework client = MockZookeeperServer.getClient();

  @Test
  public void testClient() throws Exception {
    String testHost = "127.0.0.1";
    int testPort = 1260;
    String testCluster = "testCluster";
    String testCatalog = "testCatalog";
    int socketTimeout = 1111;
    ZookeeperService zkService = ZookeeperService.getInstance(MockZookeeperServer.getUri());
    String masterPath = AmsHAProperties.getMasterPath(testCluster);
    zkService.create(masterPath);

    AmsServerInfo serverInfo = new AmsServerInfo();
    serverInfo.setHost(testHost);
    serverInfo.setThriftBindPort(testPort);
    zkService.setData(masterPath, JSONObject.toJSONString(serverInfo));

    String zkUrl = String.format("zookeeper://%s/%s/%s?socketTimeout=%d", MockZookeeperServer.getUri(), testCluster,
        testCatalog, socketTimeout);
    String thriftUrl =
        String.format("thrift://%s:%d/%s?socketTimeout=%d", testHost, testPort, testCatalog, socketTimeout);
    ArcticThriftUrl arcticThriftUrl = ArcticThriftUrl.parse(zkUrl);
    Assert.assertEquals(thriftUrl, arcticThriftUrl.url());
    Assert.assertEquals(testCatalog.toLowerCase(), arcticThriftUrl.catalogName());
    Assert.assertEquals(testHost, arcticThriftUrl.host());
    Assert.assertEquals(testPort, arcticThriftUrl.port());
    Assert.assertEquals(1111, arcticThriftUrl.socketTimeout());
    Assert.assertEquals("thrift", arcticThriftUrl.schema());
  }
}
