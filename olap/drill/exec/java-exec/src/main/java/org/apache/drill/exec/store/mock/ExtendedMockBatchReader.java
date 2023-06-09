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
package org.apache.drill.exec.store.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.store.mock.MockTableDef.MockColumn;
import org.apache.drill.exec.store.mock.MockTableDef.MockScanEntry;

/**
 * Extended form of the mock record reader that uses generator class
 * instances to create the mock values. This is a work in progress.
 * Generators exist for a few simple required types. One also exists
 * to generate strings that contain dates.
 * <p>
 * The definition is provided inside the sub scan used to create the
 * {@link ScanBatch} used to create this record reader.
 */

public class ExtendedMockBatchReader implements ManagedReader<SchemaNegotiator> {

  private final MockScanEntry config;
  private final ColumnDef fields[];
  private ResultSetLoader loader;
  private RowSetLoader writer;

  public ExtendedMockBatchReader(MockScanEntry config) {
    this.config = config;
    fields = buildColumnDefs();
  }

  private ColumnDef[] buildColumnDefs() {
    final List<ColumnDef> defs = new ArrayList<>();

    // Look for duplicate names. Bad things happen when the same name
    // appears twice. We must do this here because some tests create
    // a physical plan directly, meaning that this is the first
    // opportunity to review the column definitions.

    final Set<String> names = new HashSet<>();
    final MockColumn cols[] = config.getTypes();
    for (int i = 0; i < cols.length; i++) {
      final MockTableDef.MockColumn col = cols[i];
      if (names.contains(col.name)) {
        throw new IllegalArgumentException("Duplicate column name: " + col.name);
      }
      names.add(col.name);
      final int repeat = Math.max(1, col.getRepeatCount());
      if (repeat == 1) {
        defs.add(new ColumnDef(col));
      } else {
        for (int j = 0; j < repeat; j++) {
          defs.add(new ColumnDef(col, j+1));
        }
      }
    }
    final ColumnDef[] defArray = new ColumnDef[defs.size()];
    defs.toArray(defArray);
    return defArray;
  }

  @Override
  public boolean open(SchemaNegotiator schemaNegotiator) {
    final TupleMetadata schema = new TupleSchema();
    for (int i = 0; i < fields.length; i++) {
      final ColumnDef col = fields[i];
      final MaterializedField field = MaterializedField.create(col.getName(),
                                          col.getConfig().getMajorType());
      schema.add(field);
    }
    schemaNegotiator.tableSchema(schema, true);

    // Set the batch size. Ideally, we'd leave that to the framework based
    // on the bytes per batch. But, several legacy tests depend on a known,
    // fixed batch size of 10K, so encode that until we can change those
    // tests. If the operator definition specifies a size, use that.

    // TODO: Defer batch size to framework, update tests accordingly.

    final int batchSize = config.getBatchSize();
    if (batchSize > 0) {
      schemaNegotiator.batchSize(batchSize);
    }
    schemaNegotiator.limit(config.getRecords());

    loader = schemaNegotiator.build();
    writer = loader.writer();
    for (int i = 0; i < fields.length; i++) {
      fields[i].generator.setup(fields[i], writer.scalar(i));
    }
    return true;
  }

  @Override
  public boolean next() {
    final Random rand = new Random();
    while (!writer.isFull()) {
      writer.start();
      for (int j = 0; j < fields.length; j++) {
        if (fields[j].nullable && rand.nextInt(100) < fields[j].nullablePercent) {
          writer.scalar(j).setNull();
        } else {
          fields[j].generator.setValue();
        }
      }
      writer.save();
    }

    return !loader.atLimit();
  }

  @Override
  public void close() { }
}
