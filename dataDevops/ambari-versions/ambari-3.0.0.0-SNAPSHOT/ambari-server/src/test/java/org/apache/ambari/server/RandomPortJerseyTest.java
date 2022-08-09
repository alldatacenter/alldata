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


package org.apache.ambari.server;

import java.io.IOException;
import java.net.ServerSocket;

import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * Makes JerseyTest use random port in case default one is unavailable.
 */
public class RandomPortJerseyTest extends JerseyTest {
  private static int testPort;

  public RandomPortJerseyTest(AppDescriptor ad) {
    super(ad);
  }

  @Override
  protected int getPort(int defaultPort) {
    ServerSocket server = null;
    int port = -1;
    try {
      server = new ServerSocket(defaultPort);
      port = server.getLocalPort();
    } catch (IOException e) {
    } finally {
      if (server != null) {
        try {
          server.close();
        } catch (IOException e) {
        }
      }
    }
    if ((port != -1) || (defaultPort == 0)) {
      this.testPort = port;
      return port;
    }
    return getPort(0);
  }

  public int getTestPort() {
    return testPort;
  }
}
