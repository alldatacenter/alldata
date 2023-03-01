/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.config;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.util.ThreadUtils;

public abstract class ReconfigurableBase implements Reconfigurable {

  private static final Logger LOG = LoggerFactory.getLogger(ReconfigurableBase.class);
  public static final String RECONFIGURABLE_FILE_NAME = "reconfigurable.file.name";

  private final RssConf rssConf;

  private final ScheduledExecutorService scheduledExecutorService;
  private final long checkIntervalSec;
  private final AtomicLong lastModify = new AtomicLong(0L);

  public ReconfigurableBase(RssConf rssConf) {
    this.rssConf = rssConf;
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadUtils.getThreadFactory("ReconfigurableThread-%d"));
    checkIntervalSec = rssConf.getLong(RssBaseConf.RSS_RECONFIGURE_INTERVAL_SEC);
  }

  public void startReconfigureThread() {
    scheduledExecutorService.scheduleAtFixedRate(
        this::checkConfiguration, checkIntervalSec, checkIntervalSec, TimeUnit.SECONDS);
  }

  public void stopReconfigureThread() {
    scheduledExecutorService.shutdown();
  }

  private void checkConfiguration() {
    String fileName = rssConf.getString(RECONFIGURABLE_FILE_NAME, "");
    if (fileName.isEmpty()) {
      LOG.warn("Config file name isn't set, we skip checking");
      return;
    }
    File configFile = new File(fileName);
    if (!configFile.exists()) {
      LOG.warn("Config file doesn't exist, we skip checking");
      return;
    }
    long newLastModify = configFile.lastModified();
    if (lastModify.get() == 0) {
      lastModify.set(newLastModify);
      return;
    }
    if (newLastModify != lastModify.get()) {
      LOG.warn("Server detect the modification of file {}, we start to reconfigure", fileName);
      lastModify.set(newLastModify);
      reconfigure(reloadConfiguration());
    }
  }

  protected abstract RssConf reloadConfiguration();
}
