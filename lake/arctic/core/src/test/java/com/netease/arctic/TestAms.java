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

package com.netease.arctic;

import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAms extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(TestAms.class);
  private static MockArcticMetastoreServer SINGLETON;

  static {
    if (SingletonResourceUtil.isUseSingletonResource()) {
      SINGLETON = new MockArcticMetastoreServer();
    }
  }

  private final MockArcticMetastoreServer mockAms;

  public TestAms() {
    if (SingletonResourceUtil.isUseSingletonResource()) {
      mockAms = SINGLETON;
    } else {
      mockAms = new MockArcticMetastoreServer();
    }
  }

  public String getServerUrl() {
    return mockAms.getServerUrl();
  }

  public MockArcticMetastoreServer.AmsHandler getAmsHandler() {
    return mockAms.handler();
  }

  @Override
  protected void before() throws Throwable {
    if (SingletonResourceUtil.isUseSingletonResource()) {
      if (!mockAms.isStarted()) {
        mockAms.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          SINGLETON.stopAndCleanUp();
          LOG.info("Stop singleton mock AMS after testing.");
        }));
        LOG.info("Start singleton mock AMS before testing.");
      }
    } else {
      mockAms.start();
      LOG.info("Start mock AMS before testing.");
    }
  }

  @Override
  protected void after() {
    if (!SingletonResourceUtil.isUseSingletonResource()) {
      mockAms.stopAndCleanUp();
      LOG.info("Stop mock AMS after testing.");
    }
  }
}
