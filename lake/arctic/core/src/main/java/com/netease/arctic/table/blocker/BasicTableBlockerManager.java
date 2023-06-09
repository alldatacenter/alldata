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
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Basic {@link TableBlockerManager} implementation.
 */
public class BasicTableBlockerManager implements TableBlockerManager {

  private final TableIdentifier tableIdentifier;
  private final AmsClient client;

  public BasicTableBlockerManager(TableIdentifier tableIdentifier, AmsClient client) {
    this.tableIdentifier = tableIdentifier;
    this.client = client;
  }

  public static TableBlockerManager build(TableIdentifier tableIdentifier, AmsClient amsClient) {
    return new BasicTableBlockerManager(tableIdentifier, amsClient);
  }

  @Override
  public Blocker block(List<BlockableOperation> operations, Map<String, String> properties)
      throws OperationConflictException {
    try {
      Preconditions.checkNotNull(properties, "properties should not be null");
      return buildBlocker(client.block(tableIdentifier.buildTableIdentifier(), operations, properties), true);
    } catch (OperationConflictException e) {
      throw e;
    } catch (TException e) {
      throw new IllegalStateException("failed to block table " + tableIdentifier + " with " + operations, e);
    }
  }

  @Override
  public void release(Blocker blocker) {
    try {
      client.releaseBlocker(tableIdentifier.buildTableIdentifier(), blocker.blockerId());
    } catch (TException e) {
      throw new IllegalStateException("failed to release " + tableIdentifier + "'s blocker " + blocker.blockerId(), e);
    }
  }

  @Override
  public List<Blocker> getBlockers() {
    try {
      return client.getBlockers(tableIdentifier.buildTableIdentifier())
          .stream().map(this::buildBlocker).collect(Collectors.toList());
    } catch (TException e) {
      throw new IllegalStateException("failed to get blockers of " + tableIdentifier, e);
    }
  }

  public TableIdentifier tableIdentifier() {
    return tableIdentifier;
  }

  private Blocker buildBlocker(com.netease.arctic.ams.api.Blocker blocker) {
    return buildBlocker(blocker, false);
  }

  private Blocker buildBlocker(com.netease.arctic.ams.api.Blocker blocker, boolean needInit) {
    if (blocker.getProperties() != null &&
        blocker.getProperties().get(RenewableBlocker.EXPIRATION_TIME_PROPERTY) != null &&
        blocker.getProperties().get(RenewableBlocker.BLOCKER_TIMEOUT) != null) {
      Map<String, String> properties = Maps.newHashMap(blocker.getProperties());
      long createTime = PropertyUtil.propertyAsLong(properties, RenewableBlocker.CREATE_TIME_PROPERTY, 0);
      long expirationTime = PropertyUtil.propertyAsLong(properties, RenewableBlocker.EXPIRATION_TIME_PROPERTY, 0);
      long blockerTimeout = PropertyUtil.propertyAsLong(properties, RenewableBlocker.BLOCKER_TIMEOUT, 0);
      properties.remove(RenewableBlocker.CREATE_TIME_PROPERTY);
      properties.remove(RenewableBlocker.EXPIRATION_TIME_PROPERTY);
      properties.remove(RenewableBlocker.BLOCKER_TIMEOUT);
      RenewableBlocker renewableBlocker =
          new RenewableBlocker(blocker.getBlockerId(), blocker.getOperations(), createTime, expirationTime,
              blockerTimeout, properties, tableIdentifier, client);
      if (needInit) {
        renewableBlocker.renewAsync();
      }
      return renewableBlocker;
    }
    throw new IllegalArgumentException("illegal blocker " + blocker);
  }
}
