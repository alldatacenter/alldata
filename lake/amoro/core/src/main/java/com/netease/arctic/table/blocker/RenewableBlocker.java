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

package com.netease.arctic.table.blocker;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Renewable {@link Blocker} implementation.
 * This Blocker has expiration time, after which it will be invalid.
 * After blocked, this blocker will renew periodically.
 */
public class RenewableBlocker implements Blocker {
  private static final Logger LOG = LoggerFactory.getLogger(RenewableBlocker.class);

  public static final String CREATE_TIME_PROPERTY = "create.time";
  public static final String EXPIRATION_TIME_PROPERTY = "expiration.time";
  public static final String BLOCKER_TIMEOUT = "blocker.timeout";

  private static volatile ScheduledExecutorService EXECUTOR;

  private final String blockerId;
  private final List<BlockableOperation> operations;
  private final long createTime;
  private long expirationTime;
  private final long blockerTimeout;
  private final Map<String, String> properties;
  private final TableIdentifier tableIdentifier;
  private final AmsClient amsClient;

  private volatile ScheduledFuture<?> renewTaskFuture;

  public RenewableBlocker(String blockerId, List<BlockableOperation> operations, long createTime, long expirationTime,
                          long blockerTimeout, Map<String, String> properties, TableIdentifier tableIdentifier,
                          AmsClient amsClient) {
    Preconditions.checkArgument(blockerTimeout > 0, "blockerTimeout must > 0");
    this.blockerId = blockerId;
    this.operations = operations;
    this.createTime = createTime;
    this.expirationTime = expirationTime;
    this.blockerTimeout = blockerTimeout;
    this.properties = properties;
    this.tableIdentifier = tableIdentifier;
    this.amsClient = amsClient;
  }

  private static ScheduledExecutorService getExecutorService() {
    if (EXECUTOR == null) {
      synchronized (RenewableBlocker.class) {
        if (EXECUTOR == null) {
          EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        }
      }
    }
    return EXECUTOR;
  }

  public void renewAsync() {
    cancelRenew();
    long interval = this.blockerTimeout / 5;
    this.renewTaskFuture =
        getExecutorService().scheduleAtFixedRate(this::doRenew, interval, interval,
            TimeUnit.MILLISECONDS);
  }

  private void doRenew() {
    try {
      this.expirationTime = amsClient.renewBlocker(tableIdentifier.buildTableIdentifier(), blockerId());
      LOG.info("renew blocker {} success of {}", blockerId(), tableIdentifier);
    } catch (NoSuchObjectException e) {
      cancelRenew();
    } catch (Throwable t) {
      LOG.warn("failed to renew block {} of table {}, ignore", blockerId(),
          tableIdentifier, t);
    }
  }

  public void cancelRenew() {
    if (this.renewTaskFuture != null) {
      this.renewTaskFuture.cancel(true);
      LOG.info("blocker released, blocker {} of {}", blockerId(), tableIdentifier);
      this.renewTaskFuture = null;
    }
  }

  @Override
  public String blockerId() {
    return blockerId;
  }

  @Override
  public Map<String, String> properties() {
    return properties;
  }

  @Override
  public List<BlockableOperation> operations() {
    return operations;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getExpirationTime() {
    return expirationTime;
  }

  public long getBlockerTimeout() {
    return blockerTimeout;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  @Override
  public String toString() {
    return "BaseBlocker{" +
        "blockerId='" + blockerId + '\'' +
        ", operations=" + operations +
        ", createTime=" + createTime +
        ", expirationTime=" + expirationTime +
        ", properties=" + properties +
        '}';
  }
}
