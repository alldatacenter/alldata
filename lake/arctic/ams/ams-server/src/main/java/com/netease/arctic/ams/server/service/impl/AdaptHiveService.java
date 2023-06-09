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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.server.model.UpgradeHiveMeta;
import com.netease.arctic.ams.server.model.UpgradeRunningInfo;
import com.netease.arctic.ams.server.model.UpgradeStatus;
import com.netease.arctic.ams.server.utils.AmsUtils;
import com.netease.arctic.hive.catalog.ArcticHiveCatalog;
import com.netease.arctic.hive.utils.UpgradeHiveTableUtil;
import com.netease.arctic.table.TableIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class AdaptHiveService {

  private static final Logger LOG = LoggerFactory.getLogger(AdaptHiveService.class);

  private static final int CORE_POOL_SIZE = 5;
  private static final long QUEUE_CAPACITY = 5;
  private static ConcurrentHashMap<TableIdentifier, UpgradeRunningInfo> runningInfoCache = new ConcurrentHashMap<>(10);
  private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE * 2,
      QUEUE_CAPACITY, TimeUnit.SECONDS, new LinkedBlockingDeque<>(5));

  public Object upgradeHiveTable(ArcticHiveCatalog arcticHiveCatalog, TableIdentifier tableIdentifier,
                                 UpgradeHiveMeta upgradeHiveMeta) {
    LOG.info("Start to upgrade hive table to arctic" + tableIdentifier.toString());
    executor.submit(() -> {
      runningInfoCache.put(tableIdentifier, new UpgradeRunningInfo());
      try {
        List<String> pkList = upgradeHiveMeta.getPkList().stream()
            .map(UpgradeHiveMeta.PrimaryKeyField::getFieldName).collect(Collectors.toList());
        UpgradeHiveTableUtil.upgradeHiveTable(arcticHiveCatalog, tableIdentifier,
            pkList, upgradeHiveMeta.getProperties());
        runningInfoCache.get(tableIdentifier).setStatus(UpgradeStatus.SUCCESS.toString());
      } catch (Throwable t) {
        LOG.error("Failed to upgrade hive table to arctic ", t);
        runningInfoCache.get(tableIdentifier).setErrorMessage(AmsUtils.getStackTrace(t));
        runningInfoCache.get(tableIdentifier).setStatus(UpgradeStatus.FAILED.toString());
      }
    });
    return null;
  }

  public UpgradeRunningInfo getUpgradeRunningInfo(TableIdentifier tableIdentifier) {
    UpgradeRunningInfo upgradeRunningInfo = runningInfoCache.get(tableIdentifier);
    if (upgradeRunningInfo != null) {
      return upgradeRunningInfo;
    } else {
      return new UpgradeRunningInfo(UpgradeStatus.NONE.toString());
    }
  }

  public void removeTableCache(TableIdentifier tableIdentifier) {
    runningInfoCache.remove(tableIdentifier);
  }
}