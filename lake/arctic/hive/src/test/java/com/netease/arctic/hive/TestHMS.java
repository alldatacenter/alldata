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

package com.netease.arctic.hive;

import com.netease.arctic.SingletonResourceUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

public class TestHMS extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(TestHMS.class);
  private static HMSMockServer SINGLETON;
  private static TemporaryFolder SINGLETON_FOLDER;

  private final HMSMockServer mockHms;
  private TemporaryFolder hmsFolder;

  static {
    try {
      if (SingletonResourceUtil.isUseSingletonResource()) {
        SINGLETON_FOLDER = new TemporaryFolder();
        SINGLETON_FOLDER.create();
        SINGLETON = new HMSMockServer(SINGLETON_FOLDER.newFile());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public TestHMS() {
    if (SingletonResourceUtil.isUseSingletonResource()) {
      mockHms = SINGLETON;
    } else {
      try {
        hmsFolder = new TemporaryFolder();
        hmsFolder.create();
        mockHms = new HMSMockServer(hmsFolder.newFile());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public HiveConf getHiveConf() {
    return mockHms.hiveConf();
  }

  public HiveMetaStoreClient getHiveClient() {
    return mockHms.getClient();
  }

  @Override
  protected void before() throws Throwable {
    if (SingletonResourceUtil.isUseSingletonResource()) {
      if (!mockHms.isStarted()) {
        mockHms.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          SINGLETON.stop();
          SINGLETON_FOLDER.delete();
          LOG.info("Stop singleton mock HMS after testing.");
        }));
        LOG.info("Start singleton mock HMS before testing.");
      }
    } else {
      mockHms.start();
      LOG.info("Start mock HMS before testing.");
    }
  }

  @Override
  protected void after() {
    if (!SingletonResourceUtil.isUseSingletonResource()) {
      mockHms.stop();
      hmsFolder.delete();
      LOG.info("Stop mock HMS after testing.");
    }
  }
}
