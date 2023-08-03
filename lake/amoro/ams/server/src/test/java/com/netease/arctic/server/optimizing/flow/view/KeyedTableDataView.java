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

import com.google.common.base.Preconditions;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.writer.RecordWithAction;
import com.netease.arctic.server.optimizing.flow.DataReader;
import com.netease.arctic.server.optimizing.flow.RandomRecordGenerator;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructLikeMap;
import org.apache.iceberg.util.StructLikeSet;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.netease.arctic.table.TableProperties.WRITE_TARGET_FILE_SIZE_BYTES;

public class KeyedTableDataView extends AbstractTableDataView {

  private final Random random;

  private final int schemaSize;

  private final int primaryUpperBound;

  private final StructLikeMap<Record> view;

  private final List<RecordWithAction> changeLog = new ArrayList();

  private final RandomRecordGenerator generator;

  public KeyedTableDataView(
      ArcticTable arcticTable,
      Schema primary,
      int partitionCount,
      int primaryUpperBound,
      long targetFileSize,
      Long seed) throws Exception {
    super(arcticTable, primary, targetFileSize);
    org.apache.iceberg.relocated.com.google.common.base.Preconditions.checkArgument(
        primary.columns().size() == 1 && primary.columns().get(0).type().typeId() == Type.TypeID.INTEGER);
    this.schemaSize = schema.columns().size();

    this.primaryUpperBound = primaryUpperBound;
    if (arcticTable.format() != TableFormat.ICEBERG) {
      arcticTable.updateProperties().set(WRITE_TARGET_FILE_SIZE_BYTES, targetFileSize + "");
    }

    this.view = StructLikeMap.create(primary.asStruct());
    List<Record> records = new DataReader(arcticTable).allData();
    for (Record record : records) {
      view.put(record, record);
    }

    Map<Integer, Map<Integer, Object>> primaryRelationWithPartition = new HashMap<>();
    if (!arcticTable.spec().isUnpartitioned()) {
      Integer primaryField = primary.columns()
          .stream().map(Types.NestedField::fieldId).findAny().get();
      Set<Integer> partitionFields = arcticTable.spec().fields().stream()
          .map(PartitionField::sourceId).collect(Collectors.toSet());
      for (Record record : records) {
        Integer primaryValue = null;
        Map<Integer, Object> partitionValues = new HashMap<>();
        for (int i = 0; i < schemaSize; i++) {
          Types.NestedField field = schema.columns().get(i);
          if (field.fieldId() == primaryField) {
            primaryValue = (Integer) record.get(i);
            break;
          }
          if (partitionFields.contains(field.fieldId())) {
            partitionValues.put(field.fieldId(), record.get(i));
            break;
          }
        }
        primaryRelationWithPartition.put(primaryValue, partitionValues);
      }
    }

    this.generator = new RandomRecordGenerator(arcticTable.schema(), arcticTable.spec(),
        primary, partitionCount, primaryRelationWithPartition, seed);
    random = seed == null ? new Random() : new Random(seed);
  }

  public WriteResult append(int count) throws IOException {
    Preconditions.checkArgument(count <= primaryUpperBound - view.size());
    List<RecordWithAction> records = new ArrayList<>();
    for (int i = 0; i < primaryUpperBound; i++) {
      Record record = generator.randomRecord(i);
      if (!view.containsKey(record)) {
        records.add(new RecordWithAction(record, ChangeAction.INSERT));
      }
      if (records.size() == count) {
        break;
      }
    }
    return doWrite(records);
  }

  public WriteResult upsert(int count) throws IOException {
    List<Record> scatter = randomRecord(count);
    List<RecordWithAction> upsert = new ArrayList<>();
    for (Record record : scatter) {
      upsert.add(new RecordWithAction(record, ChangeAction.DELETE));
      upsert.add(new RecordWithAction(record, ChangeAction.INSERT));
    }
    return doWrite(upsert);
  }

  public WriteResult cdc(int count) throws IOException {
    List<Record> scatter = randomRecord(count);
    List<RecordWithAction> cdc = new ArrayList<>();
    for (Record record : scatter) {
      if (view.containsKey(record)) {
        if (random.nextBoolean()) {
          //delete
          cdc.add(new RecordWithAction(view.get(record), ChangeAction.DELETE));
        } else {
          // update
          cdc.add(new RecordWithAction(view.get(record), ChangeAction.UPDATE_BEFORE));
          cdc.add(new RecordWithAction(record, ChangeAction.UPDATE_AFTER));
        }
      } else {
        cdc.add(new RecordWithAction(record, ChangeAction.DELETE));
      }
    }
    return doWrite(cdc);
  }

  public WriteResult onlyDelete(int count) throws IOException {
    List<Record> scatter = randomRecord(count);
    List<RecordWithAction> delete =
        scatter.stream().map(s -> new RecordWithAction(s, ChangeAction.DELETE)).collect(Collectors.toList());
    return doWrite(delete);
  }

  public WriteResult custom(CustomData customData) throws IOException {
    customData.accept(view);
    List<PKWithAction> data = customData.data();
    return custom(data);
  }

  public WriteResult custom(List<PKWithAction> data) throws IOException {
    List<RecordWithAction> records = new ArrayList<>();
    for (PKWithAction pkWithAction : data) {
      records.add(new RecordWithAction(generator.randomRecord(pkWithAction.pk), pkWithAction.action));
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

    List<Record> notInView = new ArrayList<>();
    List<Record> inViewButDuplicate = new ArrayList<>();
    StructLikeSet intersection = StructLikeSet.create(schema.asStruct());
    for (Record record : records) {
      Record viewRecord = view.get(record);
      if (viewRecord == null) {
        notInView.add(record);
      }
      if (equRecord(viewRecord, record)) {
        if (intersection.contains(record)) {
          inViewButDuplicate.add(record);
        } else {
          intersection.add(record);
        }
      } else {
        notInView.add(record);
      }
    }

    if (intersection.size() == view.size()) {
      return MatchResult.of(notInView, inViewButDuplicate, null);
    }

    List<Record> missInView = new ArrayList<>();
    for (Record viewRecord : view.values()) {
      if (!intersection.contains(viewRecord)) {
        missInView.add(viewRecord);
      }
    }
    return MatchResult.of(notInView, inViewButDuplicate, missInView);
  }

  private WriteResult doWrite(List<RecordWithAction> upsert) throws IOException {
    writeView(upsert);
    WriteResult writeResult = writeFile(upsert);
    upsertCommit(writeResult);
    return writeResult;
  }

  private List<Record> randomRecord(int count) {
    int[] ids = new int[count];
    for (int i = 0; i < count; i++) {
      ids[i] = random.nextInt(primaryUpperBound);
    }
    return generator.scatter(ids);
  }

  private boolean equRecord(Record r1, Record r2) {
    if (r2.size() < schemaSize) {
      return false;
    }
    for (int i = 0; i < schemaSize; i++) {
      Object o1 = r1.get(i);
      Object o2 = r2.get(i);
      boolean equals;
      if (o1 == null && o2 == null) {
        return true;
      } else if (o1 == null || o2 == null) {
        return false;
      } else if (o1 instanceof OffsetDateTime) {
        equals = ((OffsetDateTime) o1).isEqual((OffsetDateTime) o2);
      } else {
        equals = o1.equals(o2);
      }
      if (!equals) {
        return false;
      }
    }
    return true;
  }

  private void upsertCommit(WriteResult writeResult) {
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
    for (RecordWithAction record : records) {
      changeLog.add(record);
      ChangeAction action = record.getAction();
      if (action == ChangeAction.DELETE || action == ChangeAction.UPDATE_BEFORE) {
        view.remove(record);
      } else {
        if (view.containsKey(record)) {
          throw new RuntimeException("You write duplicate pk");
        }
        view.put(record, record);
      }
    }
  }

  public static class PKWithAction {
    private final int pk;

    private final ChangeAction action;

    public PKWithAction(int pk, ChangeAction action) {
      this.pk = pk;
      this.action = action;
    }
  }

  public abstract static class CustomData {

    private StructLikeMap<Record> view;

    public abstract List<PKWithAction> data();

    private void accept(StructLikeMap<Record> view) {
      this.view = view;
    }

    protected final boolean alreadyExists(Record record) {
      return view.containsKey(record);
    }
  }
}
