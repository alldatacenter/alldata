/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.exec.physical.resultSet.PushResultSetReader;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.IndirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class PushResultSetReaderImpl implements PushResultSetReader {

  public interface UpstreamSource {
    int schemaVersion();
    VectorContainer batch();
    SelectionVector2 sv2();
  }

  public static class BatchHolder implements UpstreamSource {

    private final VectorContainer container;
    private int schemaVersion;

    public BatchHolder(VectorContainer container) {
      this.container = container;
    }

    public void newBatch() {
      if (schemaVersion == 0) {
        schemaVersion = 1;
      } else if (container.isSchemaChanged()) {
        schemaVersion++;
      }
    }

    @Override
    public int schemaVersion() { return schemaVersion; }

    @Override
    public VectorContainer batch() { return container; }

    @Override
    public SelectionVector2 sv2() { return null; }
  }

  private final UpstreamSource source;
  private int priorSchemaVersion;
  private RowSetReader rowSetReader;

  public PushResultSetReaderImpl(UpstreamSource source) {
    this.source = source;
  }

  @Override
  public RowSetReader start() {
    int sourceSchemaVersion = source.schemaVersion();
    Preconditions.checkState(sourceSchemaVersion > 0);
    Preconditions.checkState(priorSchemaVersion <= sourceSchemaVersion);

    // If new schema, discard the old reader (if any, and create
    // a new one that matches the new schema. If not a new schema,
    // then the old reader is reused: it points to vectors which
    // Drill requires be the same vectors as the previous batch,
    // but with different buffers.
    boolean newSchema = priorSchemaVersion != sourceSchemaVersion;
    if (newSchema) {
      rowSetReader = createRowSet().reader();
      priorSchemaVersion = sourceSchemaVersion;
    } else {
      rowSetReader.newBatch();
    }
    return rowSetReader;
  }

  // TODO: Build the reader without the need for a row set
  private RowSet createRowSet() {
    VectorContainer container = source.batch();
    switch (container.getSchema().getSelectionVectorMode()) {
    case FOUR_BYTE:
      throw new IllegalArgumentException("Build from SV4 not yet supported");
    case NONE:
      return DirectRowSet.fromContainer(container);
    case TWO_BYTE:
      return IndirectRowSet.fromSv2(container, source.sv2());
    default:
      throw new IllegalStateException("Invalid selection mode");
    }
  }
}
