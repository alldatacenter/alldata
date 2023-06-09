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

package com.netease.arctic.iceberg;

import com.netease.arctic.data.file.DeleteFileWithSequence;
import com.netease.arctic.iceberg.optimize.InternalRecordWrapper;
import com.netease.arctic.iceberg.optimize.StructProjection;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.CloseableIterableWrapper;
import com.netease.arctic.io.CloseablePredicate;
import com.netease.arctic.scan.CombinedIcebergScanTask;
import com.netease.arctic.utils.map.StructLikeBaseMap;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.Accessor;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.avro.Avro;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.avro.DataReader;
import org.apache.iceberg.data.orc.GenericOrcReader;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.DeleteSchemaUtil;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.orc.ORC;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableSet;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.Filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Special point:
 * 1. Apply all delete file to all data file
 * 2. EQUALITY_DELETES only be written by flink in current, so the schemas of  all EQUALITY_DELETES is primary key
 */
public abstract class CombinedDeleteFilter<T> {
  private static final Schema POS_DELETE_SCHEMA = DeleteSchemaUtil.pathPosSchema();

  private static final Accessor<StructLike> FILENAME_ACCESSOR = POS_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_PATH.fieldId());
  private static final Accessor<StructLike> POSITION_ACCESSOR = POS_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_POS.fieldId());

  private final List<DeleteFileWithSequence> posDeletes;
  private final List<DeleteFileWithSequence> eqDeletes;
  private final Schema requiredSchema;
  private final Accessor<StructLike> posAccessor;
  private final Accessor<StructLike> filePathAccessor;
  private final Accessor<StructLike> dataTransactionIdAccessor;
  private final Set<String> pathSets;
  private final Schema deleteSchema;
  private Map<String, Set<Long>> positionMap;
  // equity-field is primary key
  private Set<Integer> deleteIds = new HashSet<>();
  private CloseablePredicate<T> eqPredicate;
  private StructLikeCollections structLikeCollections = StructLikeCollections.DEFAULT;

  protected CombinedDeleteFilter(
      CombinedIcebergScanTask task,
      Schema tableSchema,
      Schema requestedSchema,
      StructLikeCollections structLikeCollections) {
    this(task, tableSchema, requestedSchema);
    this.structLikeCollections = structLikeCollections;
  }

  protected CombinedDeleteFilter(CombinedIcebergScanTask task, Schema tableSchema, Schema requestedSchema) {
    ImmutableList.Builder<DeleteFileWithSequence> posDeleteBuilder = ImmutableList.builder();
    ImmutableList.Builder<DeleteFileWithSequence> eqDeleteBuilder = ImmutableList.builder();
    for (DeleteFileWithSequence delete : task.getDeleteFiles()) {
      switch (delete.content()) {
        case POSITION_DELETES:
          posDeleteBuilder.add(delete);
          break;
        case EQUALITY_DELETES:
          if (deleteIds.isEmpty()) {
            deleteIds = ImmutableSet.copyOf(delete.equalityFieldIds());
          } else {
            Preconditions.checkArgument(
                deleteIds.equals(ImmutableSet.copyOf(delete.equalityFieldIds())),
                "Equality delete files have different delete fields");
          }
          eqDeleteBuilder.add(delete);
          break;
        default:
          throw new UnsupportedOperationException("Unknown delete file content: " + delete.content());
      }
    }

    this.pathSets =
        task.getDataFiles().stream().map(s -> s.path().toString()).collect(Collectors.toSet());

    this.posDeletes = posDeleteBuilder.build();
    this.eqDeletes = eqDeleteBuilder.build();
    this.requiredSchema = fileProjection(tableSchema, requestedSchema, !posDeletes.isEmpty(), deleteIds);
    this.deleteSchema = TypeUtil.select(requiredSchema, deleteIds);
    this.dataTransactionIdAccessor = requiredSchema
        .accessorForField(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED_ID);
    this.posAccessor = requiredSchema.accessorForField(MetadataColumns.ROW_POSITION.fieldId());
    this.filePathAccessor = requiredSchema.accessorForField(org.apache.iceberg.MetadataColumns.FILE_PATH.fieldId());
  }

  private static Schema fileProjection(
      Schema tableSchema, Schema requestedSchema,
      boolean hasPosDelete, Set<Integer> eqDeleteIds) {
    if (!hasPosDelete && eqDeleteIds == null) {
      return requestedSchema;
    }

    Set<Integer> requiredIds = Sets.newLinkedHashSet();
    if (hasPosDelete) {
      requiredIds.add(MetadataColumns.FILE_PATH.fieldId());
      requiredIds.add(MetadataColumns.ROW_POSITION.fieldId());
    }

    if (eqDeleteIds != null) {
      requiredIds.addAll(eqDeleteIds);
      requiredIds.add(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED.fieldId());
    }

    requiredIds.add(MetadataColumns.IS_DELETED.fieldId());

    Set<Integer> missingIds = Sets.newLinkedHashSet(
        Sets.difference(requiredIds, TypeUtil.getProjectedIds(requestedSchema)));

    if (missingIds.isEmpty()) {
      return requestedSchema;
    }

    // TODO: support adding nested columns. this will currently fail when finding nested columns to add
    List<Types.NestedField> columns = Lists.newArrayList(requestedSchema.columns());
    for (int fieldId : missingIds) {
      if (fieldId == MetadataColumns.ROW_POSITION.fieldId() ||
          fieldId == MetadataColumns.IS_DELETED.fieldId() ||
          fieldId == com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED.fieldId() ||
          fieldId == MetadataColumns.FILE_PATH.fieldId()) {
        continue; // add _pos and _deleted at the end
      }

      Types.NestedField field = tableSchema.asStruct().field(fieldId);
      Preconditions.checkArgument(field != null, "Cannot find required field for ID %s", fieldId);

      columns.add(field);
    }

    if (missingIds.contains(MetadataColumns.FILE_PATH.fieldId())) {
      columns.add(MetadataColumns.FILE_PATH);
    }

    if (missingIds.contains(MetadataColumns.ROW_POSITION.fieldId())) {
      columns.add(MetadataColumns.ROW_POSITION);
    }

    if (missingIds.contains(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED.fieldId())) {
      columns.add(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED);
    }

    if (missingIds.contains(MetadataColumns.IS_DELETED.fieldId())) {
      columns.add(MetadataColumns.IS_DELETED);
    }

    return new Schema(columns);
  }

  public Schema requiredSchema() {
    return requiredSchema;
  }

  protected long pos(T record) {
    return (Long) posAccessor.get(asStructLike(record));
  }

  protected String filePath(T record) {
    return filePathAccessor.get(asStructLike(record)).toString();
  }

  private Long dataLSN(StructLike structLike) {
    return (Long) dataTransactionIdAccessor.get(structLike);
  }

  public CloseableIterable<T> filter(CloseableIterable<T> records) {
    return new CloseableIterableWrapper<>(applyEqDeletes(applyPosDeletes(records)), eqPredicate);
  }

  public CloseableIterable<T> filterNegate(CloseableIterable<T> records) {
    Predicate<T> inEq = applyEqDeletes();
    Predicate<T> inPos = applyPosDeletes();
    Predicate<T> or = inEq.or(inPos);
    Filter<T> remainingRowsFilter = new Filter<T>() {
      @Override
      protected boolean shouldKeep(T item) {
        return or.test(item);
      }
    };

    return new CloseableIterableWrapper<>(remainingRowsFilter.filter(records), eqPredicate);
  }

  private Predicate<T> applyEqDeletes() {
    if (eqPredicate != null) {
      return eqPredicate;
    }

    if (eqDeletes.isEmpty()) {
      return record -> false;
    }

    Schema pkSchema = TypeUtil.select(requiredSchema, deleteIds);
    // a projection to select and reorder fields of the file schema to match the delete rows
    StructProjection deletePKProjectRow = StructProjection.create(deleteSchema, pkSchema);
    StructProjection dataPKProjectRow = StructProjection.create(requiredSchema, pkSchema);

    CloseableIterable<RecordWithLsn> deleteRecords = CloseableIterable.transform(
        CloseableIterable.concat(
            Iterables.transform(
                eqDeletes, s -> CloseableIterable.transform(
                    openDeletes(s, deleteSchema),
                    r -> new RecordWithLsn(s.getSequenceNumber(), r)))),
        RecordWithLsn::recordCopy);

    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(deleteSchema.asStruct());

    StructLikeBaseMap<Long> structLikeMap = structLikeCollections.createStructLikeMap(pkSchema.asStruct());

    //init map
    try (CloseableIterable<RecordWithLsn> deletes = deleteRecords) {
      Iterator<RecordWithLsn> it = getArcticFileIo() == null ? deletes.iterator()
          : getArcticFileIo().doAs(deletes::iterator);
      while (it.hasNext()) {
        RecordWithLsn recordWithLsn = it.next();
        Long lsn = recordWithLsn.getLsn();
        StructLike structLike = internalRecordWrapper.copyFor(recordWithLsn.getRecord());
        StructLike deletePK = deletePKProjectRow.copyWrap(structLike);
        Long old = structLikeMap.get(deletePK);
        if (old == null || old.compareTo(lsn) <= 0) {
          structLikeMap.put(deletePK, lsn);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Predicate<T> isInDeleteSet = record -> {
      StructLike data = asStructLike(record);
      StructLike dataPk = dataPKProjectRow.copyWrap(data);
      Long dataLSN = dataLSN(data);
      Long deleteLsn = structLikeMap.get(dataPk);
      if (deleteLsn == null) {
        return false;
      }

      return deleteLsn.compareTo(dataLSN) > 0;
    };

    CloseablePredicate<T> closeablePredicate = new CloseablePredicate<>(isInDeleteSet, structLikeMap);
    this.eqPredicate = closeablePredicate;
    return isInDeleteSet;
  }

  private CloseableIterable<T> applyEqDeletes(CloseableIterable<T> records) {
    Predicate<T> remainingRows = applyEqDeletes()
        .negate();
    return eqDeletesBase(records, remainingRows);
  }

  private CloseableIterable<T> eqDeletesBase(CloseableIterable<T> records, Predicate<T> predicate) {
    // Predicate to test whether a row should be visible to user after applying equality deletions.
    if (eqDeletes.isEmpty()) {
      return records;
    }

    Filter<T> remainingRowsFilter = new Filter<T>() {
      @Override
      protected boolean shouldKeep(T item) {
        return predicate.test(item);
      }
    };

    return remainingRowsFilter.filter(records);
  }

  private CloseableIterable<T> applyPosDeletes(CloseableIterable<T> records) {
    return applyPosDeletesBase(records, applyPosDeletes().negate());
  }

  private Predicate<T> applyPosDeletes() {

    if (posDeletes.isEmpty()) {
      return record -> false;
    }

    // if there are fewer deletes than a reasonable number to keep in memory, use a set
    if (positionMap == null) {
      positionMap = new HashMap<>();
      List<CloseableIterable<Record>> deletes = Lists.transform(
          posDeletes,
          this::openPosDeletes);
      CloseableIterator<Record> iterator = CloseableIterable.concat(deletes).iterator();
      while (iterator.hasNext()) {
        Record deleteRecord = iterator.next();
        String path = FILENAME_ACCESSOR.get(deleteRecord).toString();
        if (!pathSets.contains(path)) {
          continue;
        }
        Set<Long> posSet = positionMap.computeIfAbsent(path, k -> new HashSet<>());
        posSet.add((Long) POSITION_ACCESSOR.get(deleteRecord));
      }
    }

    return record -> {
      Set<Long> posSet;
      posSet = positionMap.get(filePath(record));

      if (posSet == null) {
        return false;
      }
      return posSet.contains(pos(record));
    };
  }

  private CloseableIterable<T> applyPosDeletesBase(CloseableIterable<T> records, Predicate<T> predicate) {
    if (posDeletes.isEmpty()) {
      return records;
    }

    Filter<T> filter = new Filter<T>() {
      @Override
      protected boolean shouldKeep(T item) {
        return predicate.test(item);
      }
    };

    return filter.filter(records);
  }

  private CloseableIterable<Record> openPosDeletes(DeleteFile file) {
    return openDeletes(file, POS_DELETE_SCHEMA);
  }

  private CloseableIterable<Record> openDeletes(DeleteFile deleteFile, Schema deleteSchema) {
    InputFile input = getInputFile(deleteFile.path().toString());
    switch (deleteFile.format()) {
      case AVRO:
        return Avro.read(input)
            .project(deleteSchema)
            .reuseContainers()
            .createReaderFunc(DataReader::create)
            .build();

      case PARQUET:
        return Parquet.read(input)
            .project(deleteSchema)
            .reuseContainers()
            .createReaderFunc(fileSchema -> GenericParquetReaders.buildReader(deleteSchema, fileSchema))
            .build();

      case ORC:
        // Reusing containers is automatic for ORC. No need to set 'reuseContainers' here.
        return ORC.read(input)
            .project(deleteSchema)
            .createReaderFunc(fileSchema -> GenericOrcReader.buildReader(deleteSchema, fileSchema))
            .build();

      default:
        throw new UnsupportedOperationException(String.format(
            "Cannot read deletes, %s is not a supported format: %s", deleteFile.format().name(), deleteFile.path()));
    }
  }

  static class RecordWithLsn {
    private final Long lsn;
    private Record record;

    public RecordWithLsn(Long lsn, Record record) {
      this.lsn = lsn;
      this.record = record;
    }

    public Long getLsn() {
      return lsn;
    }

    public Record getRecord() {
      return record;
    }

    public RecordWithLsn recordCopy() {
      record = record.copy();
      return this;
    }
  }

  protected abstract StructLike asStructLike(T record);

  protected abstract InputFile getInputFile(String location);

  protected abstract ArcticFileIO getArcticFileIo();
}
