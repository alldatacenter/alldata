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
package org.apache.iceberg.data;

import org.apache.iceberg.BaseCombinedScanTask;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.DataTask;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.io.CloseableGroup;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScanTaskTableScanIterable extends CloseableGroup implements CloseableIterable<Record> {
  private final GenericReader reader;

  private final CombinedScanTask genericTask;

  private final List<DataTask> dataTasks;

  private final Types.StructType schema;

  public ScanTaskTableScanIterable(TableScan scan, CombinedScanTask task) {
    this.reader = new GenericReader(scan, false);
    this.dataTasks = new ArrayList<>();
    this.schema = scan.schema().asStruct();
    List<FileScanTask> fileScanTasks = new ArrayList<>();
    task.files().forEach(scanTask -> {
      if (scanTask.isDataTask()) {
        dataTasks.add(scanTask.asDataTask());
      } else {
        fileScanTasks.add(scanTask);
      }
    });

    this.genericTask = new BaseCombinedScanTask(fileScanTasks);
  }

  @Override
  public CloseableIterator<Record> iterator() {
    List<CloseableIterable<Record>> dataTasksIterators = dataTasks.stream()
      .map(DataTask::rows)
      .map(rows -> CloseableIterable.transform(rows, sl -> structLikeToRecord(sl, schema)))
      .collect(Collectors.toList());

    dataTasksIterators.add(reader.open(genericTask));
    CloseableIterator<Record> iterator = CloseableIterable.concat(dataTasksIterators).iterator();
    addCloseable(iterator);
    return iterator;
  }

  private static Record structLikeToRecord(StructLike structLike, Types.StructType schema) {
    GenericRecord genericRecord = GenericRecord.create(schema);
    for (int i = 0; i < schema.fields().size(); i++) {
      Object value = structLike.get(i, Object.class);
      if (value instanceof StructProjection) {
        // convert StructLike value to Record recursively
        value = structLikeToRecord(
          ((StructLike) value),
          schema.fields().get(i).type().asStructType());
      }
      genericRecord.set(i, value);
    }
    return genericRecord;
  }
}
