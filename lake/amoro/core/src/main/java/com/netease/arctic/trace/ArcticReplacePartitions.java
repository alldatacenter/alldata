/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trace;

import com.netease.arctic.AmsClient;
import com.netease.arctic.op.ArcticUpdate;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;

import java.util.function.Supplier;

public class ArcticReplacePartitions extends ArcticUpdate<ReplacePartitions> implements ReplacePartitions {

  private final ReplacePartitions replacePartitions;

  public static ArcticReplacePartitions.Builder buildFor(ArcticTable table) {
    return new ArcticReplacePartitions.Builder(table);
  }

  private ArcticReplacePartitions(ArcticTable arcticTable, ReplacePartitions replacePartitions, TableTracer tracer) {
    super(arcticTable, replacePartitions, tracer);
    this.replacePartitions = replacePartitions;
  }

  private ArcticReplacePartitions(ArcticTable arcticTable, ReplacePartitions replacePartitions, TableTracer tracer,
      Transaction transaction, boolean autoCommitTransaction) {
    super(arcticTable, replacePartitions, tracer, transaction, autoCommitTransaction);
    this.replacePartitions = replacePartitions;
  }

  @Override
  public ReplacePartitions addFile(DataFile file) {
    replacePartitions.addFile(file);
    addIcebergDataFile(file);
    return this;
  }

  @Override
  public ReplacePartitions validateAppendOnly() {
    replacePartitions.validateAppendOnly();
    return this;
  }

  @Override
  public ReplacePartitions validateFromSnapshot(long snapshotId) {
    replacePartitions.validateFromSnapshot(snapshotId);
    return this;
  }

  @Override
  public ReplacePartitions validateNoConflictingDeletes() {
    replacePartitions.validateNoConflictingDeletes();
    return this;
  }

  @Override
  public ReplacePartitions validateNoConflictingData() {
    replacePartitions.validateNoConflictingData();
    return this;
  }


  @Override
  protected ReplacePartitions self() {
    return this;
  }

  public static class Builder extends ArcticUpdate.Builder<ArcticReplacePartitions, ReplacePartitions> {

    private Builder(ArcticTable table) {
      super(table);
      generateWatermark();
    }

    @Override
    public ArcticUpdate.Builder<ArcticReplacePartitions, ReplacePartitions> traceTable(
        AmsClient client, UnkeyedTable traceTable) {
      if (client != null) {
        TableTracer tracer = new AmsTableTracer(traceTable, TraceOperations.OVERWRITE, client, true);
        traceTable(tracer);
      }
      return this;
    }

    @Override
    protected ArcticReplacePartitions updateWithWatermark(
        TableTracer tableTracer, Transaction transaction, boolean autoCommitTransaction) {
      return new ArcticReplacePartitions(table, transaction.newReplacePartitions(),
          tableTracer, transaction, autoCommitTransaction);
    }

    @Override
    protected ArcticReplacePartitions updateWithoutWatermark(
        TableTracer tableTracer,
        Supplier<ReplacePartitions> delegateSupplier) {
      return new ArcticReplacePartitions(table, delegateSupplier.get(), tableTracer);
    }

    @Override
    protected Supplier<ReplacePartitions> transactionDelegateSupplier(Transaction transaction) {
      return transaction::newReplacePartitions;
    }

    @Override
    protected Supplier<ReplacePartitions> tableStoreDelegateSupplier(Table tableStore) {
      return tableStore::newReplacePartitions;
    }
  }
}
