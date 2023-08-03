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

package com.netease.arctic.server.table.executor;

import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.OptimizingMapper;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.table.TableRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizingExpiringExecutor extends BaseTableExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizingExpiringExecutor.class);

  // 1 days
  private static final long INTERVAL = 24 * 60 * 60 * 1000L;
  // 30 days
  private static final long KEEP_TIME = 30 * 24 * 60 * 60 * 1000L;

  private Persistency persistency = new Persistency();

  public OptimizingExpiringExecutor(TableManager tableRuntimes) {
    super(tableRuntimes, 1);
  }

  @Override
  protected long getNextExecutingTime(TableRuntime tableRuntime) {
    return INTERVAL;
  }

  @Override
  protected boolean enabled(TableRuntime tableRuntime) {
    return true;
  }

  @Override
  protected void execute(TableRuntime tableRuntime) {
    try {
      persistency.doExpiring(tableRuntime);
    } catch (Throwable throwable) {
      LOG.error("Expiring table runtimes of {} failed.", tableRuntime.getTableIdentifier(), throwable);
    }
  }

  private class Persistency extends PersistentBase {
    public void doExpiring(TableRuntime tableRuntime) {
      long expireTime = System.currentTimeMillis() - KEEP_TIME;
      doAsTransaction(
          () -> doAs(OptimizingMapper.class, mapper ->
              mapper.deleteOptimizingProcessBefore(tableRuntime.getTableIdentifier().getId(), expireTime)),
          () -> doAs(OptimizingMapper.class, mapper ->
              mapper.deleteTaskRuntimesBefore(tableRuntime.getTableIdentifier().getId(), expireTime)),
          () -> doAs(OptimizingMapper.class, mapper ->
              mapper.deleteOptimizingQuotaBefore(tableRuntime.getTableIdentifier().getId(), expireTime))
      );
    }
  }
}
