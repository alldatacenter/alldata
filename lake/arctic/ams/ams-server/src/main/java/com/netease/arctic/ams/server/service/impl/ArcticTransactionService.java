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

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.trace.SnapshotSummary;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.events.CreateSnapshotEvent;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ArcticTransactionService extends IJDBCService {
  private static final Logger LOG = LoggerFactory.getLogger(ArcticTransactionService.class);

  private static final Set<TableIdentifier> validTableIdentifiers = new HashSet<>();
  private final AmsClient metastoreClient;

  public ArcticTransactionService() {
    metastoreClient = ServiceContainer.getTableMetastoreHandler();
  }

  public long allocateTransactionId(TableIdentifier tableIdentifier, String signature) throws TException {

    if (!validTableIdentifiers.contains(tableIdentifier)) {
      throw new TException(tableIdentifier + " is not ready to allocate transactionId, try later");
    } else {
      LOG.warn("{} allocate TransactionId from AMS, flink/spark should update to 0.4.1+", tableIdentifier);
    }

    ArcticCatalog catalog = CatalogLoader.load(metastoreClient, tableIdentifier.getCatalog());
    ArcticTable arcticTable = catalog.loadTable(com.netease.arctic.table.TableIdentifier.of(tableIdentifier));
    if (arcticTable.isKeyedTable()) {
      // commit an empty snapshot to ChangeStore, and use the sequence of this empty snapshot as TransactionId
      ChangeTable changeTable = arcticTable.asKeyedTable().changeTable();
      AppendFiles appendFiles = changeTable.newAppend();
      appendFiles.set(SnapshotSummary.TRANSACTION_BEGIN_SIGNATURE, signature == null ? "" : signature);
      appendFiles.commit();
      CreateSnapshotEvent createSnapshotEvent = (CreateSnapshotEvent) appendFiles.updateEvent();
      return createSnapshotEvent.sequenceNumber();
    } else {
      throw new TException(tableIdentifier + " is not keyed table");
    }
  }

  public void validTable(TableIdentifier tableIdentifier) {
    validTableIdentifiers.add(tableIdentifier);
    LOG.info("{} is now valid for allocating transaction id", tableIdentifier);
  }

  public void inValidTable(TableIdentifier tableIdentifier) {
    validTableIdentifiers.remove(tableIdentifier);
    LOG.info("{} is now invalid for allocating transaction id", tableIdentifier);
  }
}
