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

package com.netease.arctic.iceberg;

import com.netease.arctic.utils.StructLikeSet;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.Accessor;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.Comparators;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.Filter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Copy from Iceberg {@link org.apache.iceberg.deletes.Deletes}.
 */
public class Deletes {

  private static final Schema POSITION_DELETE_SCHEMA = new Schema(
      MetadataColumns.DELETE_FILE_PATH,
      MetadataColumns.DELETE_FILE_POS
  );

  private static final Accessor<StructLike> FILENAME_ACCESSOR = POSITION_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_PATH.fieldId());
  private static final Accessor<StructLike> POSITION_ACCESSOR = POSITION_DELETE_SCHEMA
      .accessorForField(MetadataColumns.DELETE_FILE_POS.fieldId());

  public static <T> CloseableIterable<T> filter(CloseableIterable<T> rows, Function<T, Long> rowToPosition,
      Set<Long> deleteSet) {
    if (deleteSet.isEmpty()) {
      return rows;
    }

    PositionSetDeleteFilter<T>
        filter = new PositionSetDeleteFilter<>(rowToPosition, deleteSet);
    return filter.filter(rows);
  }

  public static StructLikeSet toEqualitySet(CloseableIterable<StructLike> eqDeletes,
                                            Types.StructType eqType,
                                            StructLikeCollections structLikeCollections) {
    try (CloseableIterable<StructLike> deletes = eqDeletes) {
      StructLikeSet deleteSet = structLikeCollections.createStructLikeSet(eqType);
      for (StructLike delete : deletes) {
        deleteSet.add(delete);
      }
      return deleteSet;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close equality delete source", e);
    }
  }

  public static Set<Long> toPositionSet(CharSequence dataLocation, CloseableIterable<? extends StructLike> deleteFile) {
    return toPositionSet(dataLocation, ImmutableList.of(deleteFile));
  }

  public static <T extends StructLike> Set<Long> toPositionSet(CharSequence dataLocation,
      List<CloseableIterable<T>> deleteFiles) {
    DataFileFilter<T> locationFilter = new DataFileFilter<>(dataLocation);
    List<CloseableIterable<Long>> positions = Lists.transform(deleteFiles, deletes ->
        CloseableIterable.transform(locationFilter.filter(deletes), row -> (Long) POSITION_ACCESSOR.get(row)));
    return toPositionSet(CloseableIterable.concat(positions));
  }

  public static Set<Long> toPositionSet(CloseableIterable<Long> posDeletes) {
    try (CloseableIterable<Long> deletes = posDeletes) {
      return Sets.newHashSet(deletes);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close position delete source", e);
    }
  }

  private static class DataFileFilter<T extends StructLike> extends Filter<T> {
    private static final Comparator<CharSequence> CHARSEQ_COMPARATOR = Comparators.charSequences();
    private final CharSequence dataLocation;

    DataFileFilter(CharSequence dataLocation) {
      this.dataLocation = dataLocation;
    }

    @Override
    protected boolean shouldKeep(T posDelete) {
      return CHARSEQ_COMPARATOR.compare(dataLocation, (CharSequence) FILENAME_ACCESSOR.get(posDelete)) == 0;
    }
  }

  private static class PositionSetDeleteFilter<T> extends Filter<T> {
    private final Function<T, Long> rowToPosition;
    private final Set<Long> deleteSet;

    private PositionSetDeleteFilter(Function<T, Long> rowToPosition, Set<Long> deleteSet) {
      this.rowToPosition = rowToPosition;
      this.deleteSet = deleteSet;
    }

    @Override
    protected boolean shouldKeep(T row) {
      return !deleteSet.contains(rowToPosition.apply(row));
    }
  }
}
