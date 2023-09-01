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

package org.apache.uniffle.server.storage;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.server.ShuffleDataFlushEvent;
import org.apache.uniffle.server.ShuffleServerConf;

public abstract class AbstractStorageManagerFallbackStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractStorageManagerFallbackStrategy.class);
  protected ShuffleServerConf conf;

  public AbstractStorageManagerFallbackStrategy(ShuffleServerConf conf) {
    this.conf = conf;
  }
  
  public abstract StorageManager tryFallback(
      StorageManager current, ShuffleDataFlushEvent event, StorageManager... candidates);

  protected StorageManager findNextStorageManager(
      StorageManager current, Set<Class<? extends StorageManager>> excludeTypes,
      ShuffleDataFlushEvent event, StorageManager... candidates) {
    int nextIdx = -1;
    for (int i = 0; i < candidates.length; i++) {
      if (current == candidates[i]) {
        nextIdx = (i + 1) % candidates.length;
        break;
      }
    }
    if (nextIdx == -1) {
      throw new RuntimeException("Current StorageManager is not in candidates");
    }
    for (int i = 0; i < candidates.length - 1; i++) {
      StorageManager storageManager = candidates[(i + nextIdx) % candidates.length];
      if (excludeTypes != null && excludeTypes.contains(storageManager.getClass())) {
        continue;
      }
      if (!storageManager.canWrite(event)) {
        continue;
      }
      return storageManager;
    }
    LOG.warn("Find next storageManager failed, all candidates are not available.");
    return current;
  }
}
