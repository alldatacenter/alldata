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

package org.apache.uniffle.test;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.server.storage.LocalStorageManager;
import org.apache.uniffle.server.storage.LocalStorageManagerFallbackStrategy;
import org.apache.uniffle.server.storage.MultiStorageManager;
import org.apache.uniffle.storage.common.LocalStorage;
import org.apache.uniffle.storage.common.Storage;
import org.apache.uniffle.storage.util.StorageType;

public class MultiStorageLocalFileFallbackTest extends MultiStorageFaultToleranceBase {

  @BeforeAll
  public static void setupServers(@TempDir File tmpDir) throws Exception {
    final CoordinatorConf coordinatorConf = getCoordinatorConf();
    ShuffleServerConf shuffleServerConf = getShuffleServerConf();
    String basePath = generateBasePath(tmpDir);
    shuffleServerConf.setDouble(ShuffleServerConf.CLEANUP_THRESHOLD, 0.0);
    shuffleServerConf.setDouble(ShuffleServerConf.HIGH_WATER_MARK_OF_WRITE, 100.0);
    shuffleServerConf.setLong(ShuffleServerConf.DISK_CAPACITY, 1024L * 1024L * 100);
    shuffleServerConf.setLong(ShuffleServerConf.PENDING_EVENT_TIMEOUT_SEC, 30L);
    shuffleServerConf.setLong(ShuffleServerConf.SHUFFLE_EXPIRED_TIMEOUT_MS, 5000L);
    shuffleServerConf.setLong(ShuffleServerConf.SERVER_APP_EXPIRED_WITHOUT_HEARTBEAT, 60L * 1000L * 60L);
    shuffleServerConf.setLong(ShuffleServerConf.SERVER_COMMIT_TIMEOUT, 20L * 1000L);
    shuffleServerConf.setString(ShuffleServerConf.RSS_STORAGE_TYPE, StorageType.LOCALFILE_HDFS.name());
    shuffleServerConf.set(ShuffleServerConf.RSS_STORAGE_BASE_PATH, Arrays.asList(basePath));
    shuffleServerConf.setLong(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE, 1000L * 1024L * 1024L);
    shuffleServerConf.setString(
        ShuffleServerConf.MULTISTORAGE_FALLBACK_STRATEGY_CLASS,
        LocalStorageManagerFallbackStrategy.class.getCanonicalName()
    );
    createAndStartServers(shuffleServerConf, coordinatorConf);
  }

  @Override
  public void makeChaos() {
    LocalStorageManager warmStorageManager =
        (LocalStorageManager) ((MultiStorageManager)shuffleServers.get(0).getStorageManager()).getWarmStorageManager();
    for (Storage storage : warmStorageManager.getStorages()) {
      LocalStorage localStorage = (LocalStorage) storage;
      localStorage.markSpaceFull();
    }
  }
}
