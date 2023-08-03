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

import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.CloseablePredicate;
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
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Special point:
 * 1. Apply all delete file to all data file
 * 2. EQUALITY_DELETES only be written by flink in current, so the schemas of  all EQUALITY_DELETES is primary key
 */
public abstract class CombinedDeleteFilter<T extends StructLike> {

  private static final Logger LOG = LoggerFactory.getLogger(CombinedDeleteFilter.class);

  private static final Schema POS_DELETE_SCHEMA = DeleteSchemaUtil.pathPosSchema();

  private static final Accessor<StructLike> FILENAME_ACCESSOR = POS_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_PATH.fieldId());
  private static final Accessor<StructLike> POSITION_ACCESSOR = POS_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_POS.fieldId());

  private final List<IcebergDeleteFile> posDeletes;
  private final List<IcebergDeleteFile> eqDeletes;

  private Map<String, Set<Long>> positionMap;

  private final Set<String> positionPathSets;

  private Set<Integer> deleteIds = new HashSet<>();

  private CloseablePredicate<StructForDelete<T>> eqPredicate;

  private final Schema deleteSchema;

  private StructLikeCollections structLikeCollections = StructLikeCollections.DEFAULT;

  protected CombinedDeleteFilter(
      IcebergContentFile<?>[] deleteFiles,
      Set<String> positionPathSets,
      Schema tableSchema,
      StructLikeCollections structLikeCollections) {
    ImmutableList.Builder<IcebergDeleteFile> posDeleteBuilder = ImmutableList.builder();
    ImmutableList.Builder<IcebergDeleteFile> eqDeleteBuilder = ImmutableList.builder();
    if (deleteFiles != null) {
      for (IcebergContentFile<?> delete : deleteFiles) {
        switch (delete.content()) {
          case POSITION_DELETES:
            posDeleteBuilder.add(delete.asDeleteFile());
            break;
          case EQUALITY_DELETES:
            if (deleteIds.isEmpty()) {
              deleteIds = ImmutableSet.copyOf(delete.asDeleteFile().equalityFieldIds());
            } else {
              Preconditions.checkArgument(
                  deleteIds.equals(ImmutableSet.copyOf(delete.asDeleteFile().equalityFieldIds())),
                  "Equality delete files have different delete fields");
            }
            eqDeleteBuilder.add(delete.asDeleteFile());
            break;
          default:
            throw new UnsupportedOperationException("Unknown delete file content: " + delete.content());
        }
      }
    }

    this.positionPathSets = positionPathSets;
    this.posDeletes = posDeleteBuilder.build();
    this.eqDeletes = eqDeleteBuilder.build();
    this.deleteSchema = TypeUtil.select(tableSchema, deleteIds);

    if (structLikeCollections != null) {
      this.structLikeCollections = structLikeCollections;
    }
  }

  protected abstract InputFile getInputFile(String location);

  protected abstract ArcticFileIO getArcticFileIo();

  public Set<Integer> deleteIds() {
    return deleteIds;
  }

  public boolean hasPosition() {
    return posDeletes != null && posDeletes.size() > 0;
  }

  public void close() {
    positionMap = null;
    try {
      if (eqPredicate != null) {
        eqPredicate.close();
      }
    } catch (IOException e) {
      LOG.error("", e);
    }
    eqPredicate = null;
  }

  public CloseableIterable<StructForDelete<T>> filter(CloseableIterable<StructForDelete<T>> records) {
    return applyEqDeletes(applyPosDeletes(records));
  }

  public CloseableIterable<StructForDelete<T>> filterNegate(CloseableIterable<StructForDelete<T>> records) {
    Predicate<StructForDelete<T>> inEq = applyEqDeletes();
    Predicate<StructForDelete<T>> inPos = applyPosDeletes();
    Predicate<StructForDelete<T>> or = inEq.or(inPos);
    Filter<StructForDelete<T>> remainingRowsFilter = new Filter<StructForDelete<T>>() {
      @Override
      protected boolean shouldKeep(StructForDelete<T> item) {
        return or.test(item);
      }
    };

    return remainingRowsFilter.filter(records);
  }

  private Predicate<StructForDelete<T>> applyEqDeletes() {
    if (eqPredicate != null) {
      return eqPredicate;
    }

    if (eqDeletes.isEmpty()) {
      return record -> false;
    }

    CloseableIterable<RecordWithLsn> deleteRecords = CloseableIterable.transform(
        CloseableIterable.concat(
            Iterables.transform(
                eqDeletes, s -> CloseableIterable.transform(
                    openDeletes(s.asDeleteFile(), deleteSchema),
                    r -> new RecordWithLsn(s.getSequenceNumber(), r)))),
        RecordWithLsn::recordCopy);

    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(deleteSchema.asStruct());

    StructLikeBaseMap<Long> structLikeMap = structLikeCollections.createStructLikeMap(deleteSchema.asStruct());

    //init map
    try (CloseableIterable<RecordWithLsn> deletes = deleteRecords) {
      Iterator<RecordWithLsn> it = getArcticFileIo() == null ? deletes.iterator()
          : getArcticFileIo().doAs(deletes::iterator);
      while (it.hasNext()) {
        RecordWithLsn recordWithLsn = it.next();
        Long lsn = recordWithLsn.getLsn();
        StructLike deletePK = internalRecordWrapper.copyFor(recordWithLsn.getRecord());
        Long old = structLikeMap.get(deletePK);
        if (old == null || old.compareTo(lsn) <= 0) {
          structLikeMap.put(deletePK, lsn);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Predicate<StructForDelete<T>> isInDeleteSet = structForDelete -> {
      StructLike dataPk = structForDelete.getPk();
      Long dataLSN = structForDelete.getLsn();
      Long deleteLsn = structLikeMap.get(dataPk);
      if (deleteLsn == null) {
        return false;
      }

      return deleteLsn.compareTo(dataLSN) > 0;
    };

    CloseablePredicate<StructForDelete<T>> closeablePredicate = new CloseablePredicate<>(isInDeleteSet, structLikeMap);
    this.eqPredicate = closeablePredicate;
    return isInDeleteSet;
  }

  private CloseableIterable<StructForDelete<T>> applyEqDeletes(CloseableIterable<StructForDelete<T>> records) {
    Predicate<StructForDelete<T>> remainingRows = applyEqDeletes()
        .negate();
    return eqDeletesBase(records, remainingRows);
  }

  private CloseableIterable<StructForDelete<T>> eqDeletesBase(
      CloseableIterable<StructForDelete<T>> records,
      Predicate<StructForDelete<T>> predicate) {
    // Predicate to test whether a row should be visible to user after applying equality deletions.
    if (eqDeletes.isEmpty()) {
      return records;
    }

    Filter<StructForDelete<T>> remainingRowsFilter = new Filter<StructForDelete<T>>() {
      @Override
      protected boolean shouldKeep(StructForDelete<T> item) {
        return predicate.test(item);
      }
    };

    return remainingRowsFilter.filter(records);
  }

  private CloseableIterable<StructForDelete<T>> applyPosDeletes(CloseableIterable<StructForDelete<T>> records) {
    return applyPosDeletesBase(records, applyPosDeletes().negate());
  }

  private Predicate<StructForDelete<T>> applyPosDeletes() {

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
        if (positionPathSets != null && !positionPathSets.contains(path)) {
          continue;
        }
        Set<Long> posSet = positionMap.computeIfAbsent(path, k -> new HashSet<>());
        posSet.add((Long) POSITION_ACCESSOR.get(deleteRecord));
      }
    }

    return structLikeForDelete -> {
      Set<Long> posSet;
      posSet = positionMap.get(structLikeForDelete.filePath());

      if (posSet == null) {
        return false;
      }
      return posSet.contains(structLikeForDelete.getPosition());
    };
  }

  private CloseableIterable<StructForDelete<T>> applyPosDeletesBase(
      CloseableIterable<StructForDelete<T>> records,
      Predicate<StructForDelete<T>> predicate) {
    if (posDeletes.isEmpty()) {
      return records;
    }

    Filter<StructForDelete<T>> filter = new Filter<StructForDelete<T>>() {
      @Override
      protected boolean shouldKeep(StructForDelete<T> item) {
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
}
