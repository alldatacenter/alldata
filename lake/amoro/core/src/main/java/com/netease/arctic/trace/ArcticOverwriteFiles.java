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

package com.netease.arctic.trace;

import com.netease.arctic.AmsClient;
import com.netease.arctic.op.ArcticUpdate;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.expressions.Expression;

import java.util.function.Supplier;

/**
 * Wrap {@link OverwriteFiles} with {@link TableTracer}.
 */
public class ArcticOverwriteFiles extends ArcticUpdate<OverwriteFiles> implements OverwriteFiles {

  private final OverwriteFiles overwriteFiles;

  public static ArcticOverwriteFiles.Builder buildFor(ArcticTable table) {
    return new ArcticOverwriteFiles.Builder(table);
  }

  private ArcticOverwriteFiles(ArcticTable arcticTable, OverwriteFiles overwriteFiles, TableTracer tracer) {
    super(arcticTable, overwriteFiles, tracer);
    this.overwriteFiles = overwriteFiles;
  }

  private ArcticOverwriteFiles(ArcticTable arcticTable, OverwriteFiles overwriteFiles, TableTracer tracer,
      Transaction transaction, boolean autoCommitTransaction) {
    super(arcticTable, overwriteFiles, tracer, transaction, autoCommitTransaction);
    this.overwriteFiles = overwriteFiles;
  }

  @Override
  public OverwriteFiles overwriteByRowFilter(Expression expr) {
    overwriteFiles.overwriteByRowFilter(expr);
    return this;
  }

  @Override
  public OverwriteFiles addFile(DataFile file) {
    overwriteFiles.addFile(file);
    addIcebergDataFile(file);
    return this;
  }

  @Override
  public OverwriteFiles deleteFile(DataFile file) {
    overwriteFiles.deleteFile(file);
    deleteIcebergDataFile(file);
    return this;
  }

  @Override
  public OverwriteFiles validateAddedFilesMatchOverwriteFilter() {
    overwriteFiles.validateAddedFilesMatchOverwriteFilter();
    return this;
  }

  @Override
  public OverwriteFiles validateFromSnapshot(long snapshotId) {
    overwriteFiles.validateFromSnapshot(snapshotId);
    return this;
  }

  @Override
  public OverwriteFiles caseSensitive(boolean caseSensitive) {
    overwriteFiles.caseSensitive(caseSensitive);
    return this;
  }

  @Override
  public OverwriteFiles conflictDetectionFilter(Expression conflictDetectionFilter) {
    overwriteFiles.conflictDetectionFilter(conflictDetectionFilter);
    return this;
  }

  @Override
  public OverwriteFiles validateNoConflictingData() {
    overwriteFiles.validateNoConflictingData();
    return this;
  }

  @Override
  public OverwriteFiles validateNoConflictingDeletes() {
    overwriteFiles.validateNoConflictingDeletes();
    return this;
  }

  @Override
  protected OverwriteFiles self() {
    return this;
  }

  public static class Builder extends ArcticUpdate.Builder<ArcticOverwriteFiles, OverwriteFiles> {

    private Builder(ArcticTable table) {
      super(table);
      generateWatermark();
    }

    @Override
    public ArcticUpdate.Builder<ArcticOverwriteFiles, OverwriteFiles> traceTable(
        AmsClient client, UnkeyedTable traceTable) {
      if (client != null) {
        TableTracer tracer = new AmsTableTracer(traceTable, TraceOperations.OVERWRITE, client, true);
        traceTable(tracer);
      }
      return this;
    }

    @Override
    protected ArcticOverwriteFiles updateWithWatermark(
        TableTracer tableTracer, Transaction transaction, boolean autoCommitTransaction) {
      return new ArcticOverwriteFiles(table, transaction.newOverwrite(),
          tableTracer, transaction, autoCommitTransaction);
    }

    @Override
    protected ArcticOverwriteFiles updateWithoutWatermark(
        TableTracer tableTracer, Supplier<OverwriteFiles> delegateSupplier) {
      return new ArcticOverwriteFiles(table, delegateSupplier.get(), tableTracer);
    }

    @Override
    protected Supplier<OverwriteFiles> transactionDelegateSupplier(Transaction transaction) {
      return transaction::newOverwrite;
    }

    @Override
    protected Supplier<OverwriteFiles> tableStoreDelegateSupplier(Table tableStore) {
      return tableStore::newOverwrite;
    }
  }
}
