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
package org.apache.drill.exec.planner;

import org.apache.drill.shaded.guava.com.google.common.primitives.Ints;
import org.apache.drill.shaded.guava.com.google.common.primitives.Longs;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.util.BitSets;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.drill.exec.store.parquet.AbstractParquetGroupScan;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.NullableBitVector;
import org.apache.drill.exec.vector.NullableDateVector;
import org.apache.drill.exec.vector.NullableFloat4Vector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableIntervalVector;
import org.apache.drill.exec.vector.NullableSmallIntVector;
import org.apache.drill.exec.vector.NullableTimeStampVector;
import org.apache.drill.exec.vector.NullableTimeVector;
import org.apache.drill.exec.vector.NullableTinyIntVector;
import org.apache.drill.exec.vector.NullableUInt1Vector;
import org.apache.drill.exec.vector.NullableUInt2Vector;
import org.apache.drill.exec.vector.NullableUInt4Vector;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.NullableVarDecimalVector;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PartitionDescriptor that describes partitions based on column names instead of directory structure
 */
public class ParquetPartitionDescriptor extends AbstractPartitionDescriptor {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetPartitionDescriptor.class);

  private final DrillScanRel scanRel;
  private final AbstractParquetGroupScan groupScan;
  private final List<SchemaPath> partitionColumns;

  public ParquetPartitionDescriptor(PlannerSettings settings, DrillScanRel scanRel) {
    this.scanRel = scanRel;
    assert scanRel.getGroupScan() instanceof AbstractParquetGroupScan;
    this.groupScan = (AbstractParquetGroupScan) scanRel.getGroupScan();
    this.partitionColumns = groupScan.getPartitionColumns();
  }

  @Override
  public int getPartitionHierarchyIndex(String partitionName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPartitionName(String name) {
    return partitionColumns.contains(SchemaPath.getSimplePath(name));
  }

  @Override
  public Integer getIdIfValid(String name) {
    SchemaPath schemaPath = SchemaPath.getSimplePath(name);
    int id = partitionColumns.indexOf(schemaPath);
    if (id == -1) {
      return null;
    }
    return id;
  }

  @Override
  public int getMaxHierarchyLevel() {
    return partitionColumns.size();
  }

  @Override
  public void populatePartitionVectors(ValueVector[] vectors, List<PartitionLocation> partitions,
                                       BitSet partitionColumnBitSet, Map<Integer, String> fieldNameMap) {
    int record = 0;
    for (PartitionLocation partitionLocation: partitions) {
      for (int partitionColumnIndex : BitSets.toIter(partitionColumnBitSet)) {
        SchemaPath column = SchemaPath.getSimplePath(fieldNameMap.get(partitionColumnIndex));
        populatePruningVector(vectors[partitionColumnIndex], record, column, partitionLocation.getEntirePartitionLocation());
      }
      record++;
    }

    for (ValueVector v : vectors) {
      if (v == null) {
        continue;
      }
      v.getMutator().setValueCount(partitions.size());
    }

  }

  @Override
  public TypeProtos.MajorType getVectorType(SchemaPath column, PlannerSettings plannerSettings) {
    return groupScan.getTypeForColumn(column);
  }

  @Override
  public Path getBaseTableLocation() {
    final FormatSelection origSelection = (FormatSelection) scanRel.getDrillTable().getSelection();
    return origSelection.getSelection().selectionRoot;
  }

  @Override
  public TableScan createTableScan(List<PartitionLocation> newPartitionLocation,
                                   Path cacheFileRoot,
                                   boolean wasAllPartitionsPruned,
                                   MetadataContext metaContext) throws Exception {
    List<Path> newFiles = new ArrayList<>();
    for (final PartitionLocation location : newPartitionLocation) {
      newFiles.add(location.getEntirePartitionLocation());
    }

    final GroupScan newGroupScan = createNewGroupScan(newFiles, cacheFileRoot, wasAllPartitionsPruned, metaContext);

    if (newGroupScan == null) {
      logger.warn("Unable to create new group scan, returning original table scan.");
      return scanRel;
    }

    return new DrillScanRel(scanRel.getCluster(),
        scanRel.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
        scanRel.getTable(),
        newGroupScan,
        scanRel.getRowType(),
        scanRel.getColumns(),
        true /*filter pushdown*/);
  }

  @Override
  public TableScan createTableScan(List<PartitionLocation> newPartitionLocation, boolean wasAllPartitionsPruned) throws Exception {
    return createTableScan(newPartitionLocation, null, wasAllPartitionsPruned, null);
  }

  @Override
  protected void createPartitionSublists() {
    Set<Path> fileLocations = groupScan.getFileSet();
    List<PartitionLocation> locations = new LinkedList<>();
    for (Path file : fileLocations) {
      locations.add(new ParquetPartitionLocation(file));
    }
    locationSuperList = Lists.partition(locations, PartitionDescriptor.PARTITION_BATCH_SIZE);
    sublistsCreated = true;
  }

  private GroupScan createNewGroupScan(List<Path> newFiles,
                                       Path cacheFileRoot,
                                       boolean wasAllPartitionsPruned,
                                       MetadataContext metaContext) throws IOException {

    FileSelection newSelection = FileSelection.create(null, newFiles, getBaseTableLocation(), cacheFileRoot, wasAllPartitionsPruned);
    if (newSelection == null) {
      return null;
    }
    newSelection.setMetaContext(metaContext);
    return groupScan.clone(newSelection);
  }

  private void populatePruningVector(ValueVector v, int index, SchemaPath column, Path file) {
    Path path = Path.getPathWithoutSchemeAndAuthority(file);
    TypeProtos.MajorType majorType = getVectorType(column, null);
    TypeProtos.MinorType type = majorType.getMinorType();
    switch (type) {
      case BIT: {
        NullableBitVector bitVector = (NullableBitVector) v;
        Boolean value = groupScan.getPartitionValue(path, column, Boolean.class);
        if (value == null) {
          bitVector.getMutator().setNull(index);
        } else {
          bitVector.getMutator().setSafe(index, value ? 1 : 0);
        }
        return;
      }
      case INT: {
        NullableIntVector intVector = (NullableIntVector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          intVector.getMutator().setNull(index);
        } else {
          intVector.getMutator().setSafe(index, value);
        }
        return;
      }
      case SMALLINT: {
        NullableSmallIntVector smallIntVector = (NullableSmallIntVector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          smallIntVector.getMutator().setNull(index);
        } else {
          smallIntVector.getMutator().setSafe(index, value.shortValue());
        }
        return;
      }
      case TINYINT: {
        NullableTinyIntVector tinyIntVector = (NullableTinyIntVector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          tinyIntVector.getMutator().setNull(index);
        } else {
          tinyIntVector.getMutator().setSafe(index, value.byteValue());
        }
        return;
      }
      case UINT1: {
        NullableUInt1Vector intVector = (NullableUInt1Vector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          intVector.getMutator().setNull(index);
        } else {
          intVector.getMutator().setSafe(index, value.byteValue());
        }
        return;
      }
      case UINT2: {
        NullableUInt2Vector intVector = (NullableUInt2Vector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          intVector.getMutator().setNull(index);
        } else {
          intVector.getMutator().setSafe(index, (char) value.shortValue());
        }
        return;
      }
      case UINT4: {
        NullableUInt4Vector intVector = (NullableUInt4Vector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          intVector.getMutator().setNull(index);
        } else {
          intVector.getMutator().setSafe(index, value);
        }
        return;
      }
      case BIGINT: {
        NullableBigIntVector bigIntVector = (NullableBigIntVector) v;
        Long value = groupScan.getPartitionValue(path, column, Long.class);
        if (value == null) {
          bigIntVector.getMutator().setNull(index);
        } else {
          bigIntVector.getMutator().setSafe(index, value);
        }
        return;
      }
      case FLOAT4: {
        NullableFloat4Vector float4Vector = (NullableFloat4Vector) v;
        Float value = groupScan.getPartitionValue(path, column, Float.class);
        if (value == null) {
          float4Vector.getMutator().setNull(index);
        } else {
          float4Vector.getMutator().setSafe(index, value);
        }
        return;
      }
      case FLOAT8: {
        NullableFloat8Vector float8Vector = (NullableFloat8Vector) v;
        Double value = groupScan.getPartitionValue(path, column, Double.class);
        if (value == null) {
          float8Vector.getMutator().setNull(index);
        } else {
          float8Vector.getMutator().setSafe(index, value);
        }
        return;
      }
      case VARBINARY: {
        NullableVarBinaryVector varBinaryVector = (NullableVarBinaryVector) v;
        Object s = groupScan.getPartitionValue(path, column, Object.class);
        byte[] bytes;
        if (s == null) {
          varBinaryVector.getMutator().setNull(index);
          return;
        } else {
          bytes = getBytes(type, s);
        }
        varBinaryVector.getMutator().setSafe(index, bytes, 0, bytes.length);
        return;
      }
      case VARDECIMAL: {
        NullableVarDecimalVector decimalVector = (NullableVarDecimalVector) v;
        Object s = groupScan.getPartitionValue(path, column, Object.class);
        byte[] bytes;
        if (s == null) {
          decimalVector.getMutator().setNull(index);
          return;
        } else if (s instanceof Integer) {
          bytes = Ints.toByteArray((int) s);
        } else if (s instanceof Long) {
          bytes = Longs.toByteArray((long) s);
        } else {
          bytes = getBytes(type, s);
        }
        decimalVector.getMutator().setSafe(index, bytes, 0, bytes.length);
        return;
      }
      case DATE: {
        NullableDateVector dateVector = (NullableDateVector) v;
        Long value = groupScan.getPartitionValue(path, column, Long.class);
        if (value == null) {
          dateVector.getMutator().setNull(index);
        } else {
          dateVector.getMutator().setSafe(index, value);
        }
        return;
      }
      case TIME: {
        NullableTimeVector timeVector = (NullableTimeVector) v;
        Integer value = groupScan.getPartitionValue(path, column, Integer.class);
        if (value == null) {
          timeVector.getMutator().setNull(index);
        } else {
          timeVector.getMutator().setSafe(index, value);
        }
        return;
      }
      case TIMESTAMP: {
        NullableTimeStampVector timeStampVector = (NullableTimeStampVector) v;
        Long value = groupScan.getPartitionValue(path, column, Long.class);
        if (value == null) {
          timeStampVector.getMutator().setNull(index);
        } else {
          timeStampVector.getMutator().setSafe(index, value);
        }
        return;
      }
      case VARCHAR: {
        NullableVarCharVector varCharVector = (NullableVarCharVector) v;
        Object s = groupScan.getPartitionValue(path, column, Object.class);
        byte[] bytes;
        if (s == null) {
          varCharVector.getMutator().setNull(index);
          return;
        } else {
          bytes = getBytes(type, s);
        }
        varCharVector.getMutator().setSafe(index, bytes, 0, bytes.length);
        return;
      }
      case INTERVAL: {
        NullableIntervalVector intervalVector = (NullableIntervalVector) v;
        Object s = groupScan.getPartitionValue(path, column, Object.class);
        byte[] bytes;
        if (s == null) {
          intervalVector.getMutator().setNull(index);
          return;
        } else {
          bytes = getBytes(type, s);
        }
        intervalVector.getMutator().setSafe(index, 1,
            ParquetReaderUtility.getIntFromLEBytes(bytes, 0),
            ParquetReaderUtility.getIntFromLEBytes(bytes, 4),
            ParquetReaderUtility.getIntFromLEBytes(bytes, 8));
        return;
      }
      default:
        throw new UnsupportedOperationException("Unsupported type: " + type);
    }
  }

  /**
   * Returns the sequence of bytes received from {@code Object source}.
   *
   * @param type the column type
   * @param source the source of the bytes sequence
   * @return bytes sequence obtained from {@code Object source}
   */
  private byte[] getBytes(TypeProtos.MinorType type, Object source) {
    byte[] bytes;
    if (source instanceof String) {
      bytes = ((String) source).getBytes();
    } else if (source instanceof byte[]) {
      bytes = (byte[]) source;
    } else {
      throw new UnsupportedOperationException("Unable to create column data for type: " + type);
    }
    return bytes;
  }

}
