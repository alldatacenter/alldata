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
package org.apache.drill.exec.store.iceberg.read;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.v3.FixedReceiver;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.store.iceberg.IcebergWork;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.ScanTaskTableScanIterable;
import org.apache.iceberg.io.CloseableIterable;

import java.util.Iterator;

public class IcebergRecordReader implements ManagedReader<SchemaNegotiator> {
  private final IcebergWork work;

  private final TableScan tableScan;

  private final int maxRecords;

  private ResultSetLoader loader;

  private Iterator<Record> records;

  private ColumnConverter converter;

  private CloseableIterable<Record> taskToClose;

  public IcebergRecordReader(TableScan tableScan, IcebergWork work, int maxRecords) {
    this.work = work;
    this.tableScan = tableScan;
    this.maxRecords = maxRecords;
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {
    TupleSchema tableSchema = new TupleSchema();
    tableScan.schema().columns().stream()
      .map(IcebergColumnConverterFactory::getColumnMetadata)
      .forEach(tableSchema::add);
    TupleMetadata providedSchema = negotiator.providedSchema();
    TupleMetadata tupleSchema = FixedReceiver.Builder.mergeSchemas(providedSchema, tableSchema);
    negotiator.tableSchema(tupleSchema, true);
    loader = negotiator.build();

    this.taskToClose = new ScanTaskTableScanIterable(tableScan, work.getScanTask());
    this.records = taskToClose.iterator();

    ColumnConverterFactory factory = new IcebergColumnConverterFactory(providedSchema);
    converter = factory.getRootConverter(providedSchema, tableSchema, loader.writer());
    return true;
  }

  @Override
  public boolean next() {
    RowSetLoader rowWriter = loader.writer();
    while (!rowWriter.isFull()) {
      if (!nextLine(rowWriter)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(taskToClose);
    loader.close();
  }

  private boolean nextLine(RowSetLoader rowWriter) {
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    if (!records.hasNext()) {
      return false;
    }

    rowWriter.start();
    converter.convert(records.next());
    rowWriter.save();

    return true;
  }
}
