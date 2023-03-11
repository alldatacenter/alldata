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

package com.netease.arctic.flink.write;

import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.WriteOperationKind;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

/**
 * This is an Arctic table writer factory.
 */
public class ArcticRowDataTaskWriterFactory implements TaskWriterFactory<RowData> {

  private final ArcticTable table;
  private final RowType flinkSchema;

  private final boolean overwrite;

  private transient Long mask = null;
  private final transient Long transactionId = null;
  private transient Integer taskId = null;
  private transient Integer attemptId = null;

  public ArcticRowDataTaskWriterFactory(ArcticTable table,
                                        RowType flinkSchema,
                                        boolean overwrite) {
    this.table = table;
    this.flinkSchema = flinkSchema;
    this.overwrite = overwrite;
  }

  public void setMask(long mask) {
    this.mask = mask;
  }

  @Override
  public void initialize(int taskId, int attemptId) {
    this.taskId = taskId;
    this.attemptId = attemptId;
  }

  @Override
  public TaskWriter<RowData> create() {
    Preconditions.checkNotNull(mask, "Mask should be set first. Invoke setMask() before this method");

    FlinkTaskWriterBuilder builder = FlinkTaskWriterBuilder.buildFor(table)
        .withTaskId(taskId)
        .withMask(mask)
        .withTransactionId(transactionId)
        .withFlinkSchema(flinkSchema)
        .withPartitionId(attemptId);
    if (overwrite) {
      return builder.buildWriter(WriteOperationKind.OVERWRITE);
    } else {
      return builder.buildWriter(WriteOperationKind.APPEND);
    }
  }

}
