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

package com.netease.arctic.ams.api;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

/**
 * Provides mock zookeeper server.
 */
public class MockZookeeperServer {

  private static TestingServer server;
  private static CuratorFramework client;
  private static int port;

  public static CuratorFramework getClient() {
    if (client == null) {
      try {
        init();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return client;
  }

  public static String getUri() {
    return "127.0.0.1:" + port;
  }

  public static void stopServer() throws IOException {
    server.stop();
    client.close();
  }

  private static void init() throws Exception {
    port = new Random().nextInt(4000) + 14000;
    server = new TestingServer(port, true);
    server.start();

    client = CuratorFrameworkFactory.newClient("127.0.0.1",
        new ExponentialBackoffRetry(1000, 3));
    client.start();
  }

  @Test
  public void testFoobar() throws Exception {
    System.out.println("client: " + client);
    getClient().create().forPath("/test", "test-data".getBytes());

    byte[] data = client.getData().forPath("/test");
    System.out.println("data: " + new String(data));
  }
}
