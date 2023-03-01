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

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.storage.StorageInfo;
import org.apache.uniffle.server.Checker;
import org.apache.uniffle.server.ShuffleDataFlushEvent;
import org.apache.uniffle.server.ShuffleDataReadEvent;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.server.event.PurgeEvent;
import org.apache.uniffle.storage.common.Storage;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;

public class MultiStorageManager implements StorageManager {

  private static final Logger LOG = LoggerFactory.getLogger(MultiStorageManager.class);

  private final StorageManager warmStorageManager;
  private final StorageManager coldStorageManager;
  private final long flushColdStorageThresholdSize;
  private AbstractStorageManagerFallbackStrategy storageManagerFallbackStrategy;
  private final Cache<ShuffleDataFlushEvent, StorageManager> eventOfUnderStorageManagers;

  MultiStorageManager(ShuffleServerConf conf) {
    warmStorageManager = new LocalStorageManager(conf);
    coldStorageManager = new HdfsStorageManager(conf);
    flushColdStorageThresholdSize = conf.getSizeAsBytes(ShuffleServerConf.FLUSH_COLD_STORAGE_THRESHOLD_SIZE);
    try {
      storageManagerFallbackStrategy = loadFallbackStrategy(conf);
    } catch (Exception e) {
      throw new RuntimeException("Load fallback strategy failed.", e);
    }
    long cacheTimeout = conf.getLong(ShuffleServerConf.STORAGEMANAGER_CACHE_TIMEOUT);
    eventOfUnderStorageManagers = CacheBuilder.newBuilder()
        .expireAfterAccess(cacheTimeout, TimeUnit.MILLISECONDS)
        .build();
  }

  public static AbstractStorageManagerFallbackStrategy loadFallbackStrategy(
      ShuffleServerConf conf) throws Exception {
    String name = conf.getString(ShuffleServerConf.MULTISTORAGE_FALLBACK_STRATEGY_CLASS,
        HdfsStorageManagerFallbackStrategy.class.getCanonicalName());
    Class<?> klass = Class.forName(name);
    Constructor<?> constructor;
    AbstractStorageManagerFallbackStrategy instance;
    try {
      constructor = klass.getConstructor(conf.getClass(), Boolean.TYPE);
      instance = (AbstractStorageManagerFallbackStrategy) constructor.newInstance(conf);
    } catch (NoSuchMethodException e) {
      constructor = klass.getConstructor(conf.getClass());
      instance = (AbstractStorageManagerFallbackStrategy) constructor.newInstance(conf);
    }
    return instance;
  }

  @Override
  public void registerRemoteStorage(String appId, RemoteStorageInfo remoteStorageInfo) {
    coldStorageManager.registerRemoteStorage(appId, remoteStorageInfo);
  }

  @Override
  public Storage selectStorage(ShuffleDataFlushEvent event) {
    return selectStorageManager(event).selectStorage(event);
  }

  @Override
  public Storage selectStorage(ShuffleDataReadEvent event) {
    return warmStorageManager.selectStorage(event);
  }

  @Override
  public void updateWriteMetrics(ShuffleDataFlushEvent event, long writeTime) {
    throw new UnsupportedOperationException();
  }

  private StorageManager selectStorageManager(ShuffleDataFlushEvent event) {
    StorageManager storageManager;
    if (event.getSize() > flushColdStorageThresholdSize) {
      storageManager = coldStorageManager;
    } else {
      storageManager = warmStorageManager;
    }

    if (!storageManager.canWrite(event) || event.getRetryTimes() > 0) {
      storageManager = storageManagerFallbackStrategy.tryFallback(
          storageManager, event, warmStorageManager, coldStorageManager);
    }
    eventOfUnderStorageManagers.put(event, storageManager);
    event.addCleanupCallback(() -> eventOfUnderStorageManagers.invalidate(event));
    return storageManager;
  }

  @Override
  public boolean write(Storage storage, ShuffleWriteHandler handler, ShuffleDataFlushEvent event) {
    StorageManager underStorageManager = eventOfUnderStorageManagers.getIfPresent(event);
    if (underStorageManager == null) {
      return false;
    }
    return underStorageManager.write(storage, handler, event);
  }

  public void start() {
  }

  public void stop() {
  }

  @Override
  public Checker getStorageChecker() {
    return warmStorageManager.getStorageChecker();
  }

  @Override
  public boolean canWrite(ShuffleDataFlushEvent event) {
    return warmStorageManager.canWrite(event) || coldStorageManager.canWrite(event);
  }

  @Override
  public void checkAndClearLeakedShuffleData(Collection<String> appIds) {
    warmStorageManager.checkAndClearLeakedShuffleData(appIds);
  }

  @Override
  public Map<String, StorageInfo> getStorageInfo() {
    Map<String, StorageInfo> localStorageInfo = warmStorageManager.getStorageInfo();
    localStorageInfo.putAll(coldStorageManager.getStorageInfo());
    return localStorageInfo;
  }

  public void removeResources(PurgeEvent event) {
    LOG.info("Start to remove resource of {}", event);
    warmStorageManager.removeResources(event);
    coldStorageManager.removeResources(event);
  }

  public StorageManager getColdStorageManager() {
    return coldStorageManager;
  }

  public StorageManager getWarmStorageManager() {
    return warmStorageManager;
  }
}
