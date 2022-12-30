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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.syncer;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.syncer.AbstractMetaSyncer;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive.HiveTableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.HiveMeta;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Created 2020/11/23.
 */
public class HiveMetaInfoSyncer extends AbstractMetaSyncer {
  private static final Logger LOG =
      LoggerFactory.getLogger(HiveMetaInfoSyncer.class);
  private static final String SYNC_NAME = "meta-sync-fetcher";
  private final HiveTableMetaStoreFactory factory;
  private final long discoverIntervals;
  private HiveMeta current;
  private Thread fetcher;

  private boolean running;

  @Getter
  private long lastUpdateTime;

  public HiveMetaInfoSyncer(HiveTableMetaStoreFactory factory,
                            HiveMeta meta,
                            long discoverIntervals) {
    this.factory = factory;
    this.current = meta;
    this.discoverIntervals = discoverIntervals;
  }

  @Override
  public void start() {
    running = true;
    lastUpdateTime = System.currentTimeMillis();
    fetcher = new Thread(() -> {
      while (running) {
        long processingTime = System.currentTimeMillis();
        if (processingTime - lastUpdateTime > discoverIntervals) {
          LOG.info("Time to trigger fetch latest meta.");
          try {
            current = HiveTableMetaStoreFactory.createHiveMeta(factory);
            lastUpdateTime = processingTime;
          } catch (Exception e) {
            LOG.error("Syncer create latest meta info failed.", e);
          }
        }
        try {
          Thread.sleep(discoverIntervals);
        } catch (InterruptedException e) {
          LOG.error("Syncer need to exit because interrupt.", e);
          running = false;
        }
      }
    }, SYNC_NAME);
    fetcher.start();
  }

  @Override
  public FileSystemMeta current() {
    return current;
  }

  @Override
  public void close() {
    running = false;
    if (Objects.nonNull(fetcher)) {
      fetcher.interrupt();
    }
  }
}
