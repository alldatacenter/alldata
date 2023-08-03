/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing.flow.view;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.iceberg.InternalRecordWrapper;
import com.netease.arctic.io.writer.RecordWithAction;
import com.netease.arctic.server.optimizing.flow.RandomRecordGenerator;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.util.StructLikeMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.netease.arctic.table.TableProperties.WRITE_TARGET_FILE_SIZE_BYTES;

public class UnKeyedTableDataView extends AbstractTableDataView {

  private final Random random;

  private final StructLikeMap<Integer> view;

  private final RandomRecordGenerator generator;

  private final InternalRecordWrapper wrapper;

  public UnKeyedTableDataView(
      ArcticTable arcticTable,
      int partitionCount,
      long targetFileSize,
      Long seed) throws Exception {
    super(arcticTable, null, targetFileSize);

    this.wrapper = new InternalRecordWrapper(schema.asStruct());
    this.targetFileSize = targetFileSize;
    if (arcticTable.format() != TableFormat.ICEBERG) {
      arcticTable.updateProperties().set(WRITE_TARGET_FILE_SIZE_BYTES, targetFileSize + "");
    }

    this.generator = new RandomRecordGenerator(arcticTable.schema(), arcticTable.spec(),
        null, partitionCount, null, seed);
    random = seed == null ? new Random() : new Random(seed);

    this.view = StructLikeMap.create(schema.asStruct());
    // addRecords2Map(view, new DataReader(arcticTable).allData());
  }

  public WriteResult append(int count) throws IOException {
    List<RecordWithAction> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Record record = generator.randomRecord();
      records.add(new RecordWithAction(record, ChangeAction.INSERT));
      if (records.size() == count) {
        break;
      }
    }
    return doWrite(records);
  }

  public int getSize() {
    return view.size();
  }

  @Override
  public MatchResult match(List<Record> records) {
    if ((view.size() == 0 && CollectionUtils.isEmpty(records))) {
      return MatchResult.ok();
    }

    StructLikeMap<Integer> other = StructLikeMap.create(schema.asStruct());
    addRecords2Map(other, records);

    List<StructLike> notInView = new ArrayList<>();
    List<StructLike> inViewButCountError = new ArrayList<>();
    List<StructLike> inViewButMiss = new ArrayList<>();
    for (Map.Entry<StructLike, Integer> entry : view.entrySet()) {
      Integer integer = other.get(entry.getKey());
      if (integer == null) {
        notInView.add(entry.getKey());
      } else if (!integer.equals(entry.getValue())) {
        inViewButCountError.add(entry.getKey());
      }
    }
    for (Map.Entry<StructLike, Integer> entry : other.entrySet()) {
      Integer integer = view.get(entry.getKey());
      if (integer == null) {
        inViewButMiss.add(entry.getKey());
      }
    }
    return MatchResult.of(notInView, inViewButCountError, inViewButMiss);
  }

  private WriteResult doWrite(List<RecordWithAction> upsert) throws IOException {
    writeView(upsert);
    WriteResult writeResult = writeFile(upsert);
    appendCommit(writeResult);
    return writeResult;
  }

  private void appendCommit(WriteResult writeResult) {
    if (arcticTable.isKeyedTable()) {
      AppendFiles appendFiles = arcticTable.asKeyedTable().changeTable().newAppend();
      for (DataFile dataFile : writeResult.dataFiles()) {
        appendFiles.appendFile(dataFile);
      }
      appendFiles.commit();
    } else {
      RowDelta rowDelta = arcticTable.asUnkeyedTable().newRowDelta();
      for (DataFile dataFile : writeResult.dataFiles()) {
        rowDelta.addRows(dataFile);
      }
      for (DeleteFile deleteFile : writeResult.deleteFiles()) {
        rowDelta.addDeletes(deleteFile);
      }
      rowDelta.commit();
    }
  }

  private void writeView(List<RecordWithAction> records) {
    addRecords2Map(view, records);
  }

  private void addRecords2Map(StructLikeMap<Integer> map, List<? extends Record> records) {
    for (Record record : records) {
      InternalRecordWrapper wrap = wrapper.copyFor(record);
      if (map.containsKey(wrap)) {
        map.put(wrap, map.get(record) + 1);
      } else {
        map.put(wrap, 1);
      }
    }
  }
}
