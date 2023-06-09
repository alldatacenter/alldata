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
package org.apache.drill.exec.store.hbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.PathSegment.NameSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarBinaryVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class HBaseRecordReader extends AbstractRecordReader implements DrillHBaseConstants {
  private static final Logger logger = LoggerFactory.getLogger(HBaseRecordReader.class);

  // batch should not exceed this value to avoid OOM on a busy system
  private static final int MAX_ALLOCATED_MEMORY_PER_BATCH = 64 * 1024 * 1024; // 64 mb in bytes

  // batch size should not exceed max allowed record count
  private static final int TARGET_RECORD_COUNT = 4000;

  private OutputMutator outputMutator;

  private Map<String, MapVector> familyVectorMap;
  private VarBinaryVector rowKeyVector;

  private Table hTable;
  private ResultScanner resultScanner;

  private final TableName hbaseTableName;
  private final Scan hbaseScan;
  // scan instance to capture columns for vector creation
  private final Scan hbaseScanColumnsOnly;
  private Set<String> completeFamilies;
  private OperatorContext operatorContext;

  private boolean rowKeyOnly;

  private final Connection connection;

  public HBaseRecordReader(Connection connection, HBaseSubScan.HBaseSubScanSpec subScanSpec, List<SchemaPath> projectedColumns) {
    this.connection = connection;
    hbaseTableName = TableName.valueOf(
        Preconditions.checkNotNull(subScanSpec, "HBase reader needs a sub-scan spec").getTableName());
    hbaseScan = new Scan(subScanSpec.getStartRow(), subScanSpec.getStopRow());
    hbaseScanColumnsOnly = new Scan();
    hbaseScan
        .setFilter(subScanSpec.getScanFilter())
        .setCaching(TARGET_RECORD_COUNT);

    setColumns(projectedColumns);
  }

  /**
   * Provides the projected columns information to the Hbase Scan instance. If the
   * projected columns list contains a column family and also a column in the
   * column family, only the column family is passed to the Scan instance.
   *
   * For example, if the projection list is {cf1, cf1.col1, cf2.col1} then we only
   * pass {cf1, cf2.col1} to the Scan instance.
   *
   * @param columns collection of projected columns
   * @return collection of projected column family names
   */
  @Override
  protected Collection<SchemaPath> transformColumns(Collection<SchemaPath> columns) {
    Set<SchemaPath> transformed = Sets.newLinkedHashSet();
    completeFamilies = Sets.newHashSet();

    rowKeyOnly = true;
    if (!isStarQuery()) {
      for (SchemaPath column : columns) {
        if (column.getRootSegment().getPath().equalsIgnoreCase(ROW_KEY)) {
          transformed.add(ROW_KEY_PATH);
          continue;
        }
        rowKeyOnly = false;
        NameSegment root = column.getRootSegment();
        byte[] family = root.getPath().getBytes();
        transformed.add(SchemaPath.getSimplePath(root.getPath()));
        PathSegment child = root.getChild();
        if (child != null && child.isNamed()) {
          byte[] qualifier = child.getNameSegment().getPath().getBytes();
          hbaseScanColumnsOnly.addColumn(family, qualifier);
          if (!completeFamilies.contains(root.getPath())) {
            hbaseScan.addColumn(family, qualifier);
          }
        } else {
          hbaseScan.addFamily(family);
          completeFamilies.add(root.getPath());
        }
      }

      /* if only the row key was requested, add a FirstKeyOnlyFilter to the scan
       * to fetch only one KV from each row. If a filter is already part of this
       * scan, add the FirstKeyOnlyFilter as the LAST filter of a MUST_PASS_ALL
       * FilterList.
       */
      if (rowKeyOnly) {
        hbaseScan.setFilter(
            HBaseUtils.andFilterAtIndex(hbaseScan.getFilter(), HBaseUtils.LAST_FILTER, new FirstKeyOnlyFilter()));
      }
    } else {
      rowKeyOnly = false;
      transformed.add(ROW_KEY_PATH);
    }

    return transformed;
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    this.operatorContext = context;
    this.outputMutator = output;
    familyVectorMap = new HashMap<>();

    try {
      hTable = connection.getTable(hbaseTableName);

      // Add top-level column-family map vectors to output in the order specified
      // when creating reader (order of first appearance in query).
      for (SchemaPath column : getColumns()) {
        if (column.equals(ROW_KEY_PATH)) {
          MaterializedField field = MaterializedField.create(column.getAsNamePart().getName(), ROW_KEY_TYPE);
          rowKeyVector = outputMutator.addField(field, VarBinaryVector.class);
        } else {
          getOrCreateFamilyVector(column.getRootSegment().getPath(), false);
        }
      }

      // Add map and child vectors for any HBase columns that are requested (in
      // order to avoid later creation of dummy NullableIntVectors for them).
      final Set<Map.Entry<byte[], NavigableSet<byte []>>> familiesEntries =
          hbaseScanColumnsOnly.getFamilyMap().entrySet();
      for (Map.Entry<byte[], NavigableSet<byte []>> familyEntry : familiesEntries) {
        final String familyName = new String(familyEntry.getKey(),
                                             StandardCharsets.UTF_8);
        final MapVector familyVector = getOrCreateFamilyVector(familyName, false);
        final Set<byte []> children = familyEntry.getValue();
        if (null != children) {
          for (byte[] childNameBytes : children) {
            final String childName = new String(childNameBytes,
                                                StandardCharsets.UTF_8);
            getOrCreateColumnVector(familyVector, childName);
          }
        }
      }

      // Add map vectors for any HBase column families that are requested.
      for (String familyName : completeFamilies) {
        getOrCreateFamilyVector(familyName, false);
      }

      resultScanner = hTable.getScanner(hbaseScan);
    } catch (SchemaChangeException | IOException e) {
      throw new ExecutionSetupException(e);
    }
  }

  @Override
  public int next() {
    Stopwatch watch = Stopwatch.createStarted();
    if (rowKeyVector != null) {
      rowKeyVector.clear();
      rowKeyVector.allocateNew();
    }
    for (ValueVector v : familyVectorMap.values()) {
      v.clear();
      v.allocateNew();
    }

    int rowCount = 0;
    // if allocated memory for the first row is larger than allowed max in batch, it will be added anyway
    do {
      Result result = null;
      final OperatorStats operatorStats = operatorContext == null ? null : operatorContext.getStats();
      try {
        if (operatorStats != null) {
          operatorStats.startWait();
        }
        try {
          result = resultScanner.next();
        } finally {
          if (operatorStats != null) {
            operatorStats.stopWait();
          }
        }
      } catch (IOException e) {
        throw new DrillRuntimeException(e);
      }
      if (result == null) {
        break;
      }

      // parse the result and populate the value vectors
      Cell[] cells = result.rawCells();
      if (rowKeyVector != null) {
        rowKeyVector.getMutator().setSafe(
            rowCount,
            cells[0].getRowArray(),
            cells[0].getRowOffset(),
            cells[0].getRowLength());
      }
      if (!rowKeyOnly) {
        for (final Cell cell : cells) {
          final int familyOffset = cell.getFamilyOffset();
          final int familyLength = cell.getFamilyLength();
          final byte[] familyArray = cell.getFamilyArray();
          final MapVector mv = getOrCreateFamilyVector(new String(familyArray, familyOffset, familyLength), true);

          final int qualifierOffset = cell.getQualifierOffset();
          final int qualifierLength = cell.getQualifierLength();
          final byte[] qualifierArray = cell.getQualifierArray();
          final NullableVarBinaryVector v = getOrCreateColumnVector(mv,
              new String(qualifierArray, qualifierOffset, qualifierLength));

          final int valueOffset = cell.getValueOffset();
          final int valueLength = cell.getValueLength();
          final byte[] valueArray = cell.getValueArray();
          v.getMutator().setSafe(rowCount, valueArray, valueOffset, valueLength);
        }
      }
      rowCount++;
    } while (canAddNewRow(rowCount));

    setOutputRowCount(rowCount);
    logger.debug("Took {} ms to get {} records", watch.elapsed(TimeUnit.MILLISECONDS), rowCount);
    return rowCount;
  }

  private MapVector getOrCreateFamilyVector(String familyName, boolean allocateOnCreate) {
    try {
      MapVector v = familyVectorMap.get(familyName);
      if(v == null) {
        SchemaPath column = SchemaPath.getSimplePath(familyName);
        MaterializedField field = MaterializedField.create(column.getAsNamePart().getName(), COLUMN_FAMILY_TYPE);
        v = outputMutator.addField(field, MapVector.class);
        if (allocateOnCreate) {
          v.allocateNew();
        }
        getColumns().add(column);
        familyVectorMap.put(familyName, v);
      }
      return v;
    } catch (SchemaChangeException e) {
      throw new DrillRuntimeException(e);
    }
  }

  private NullableVarBinaryVector getOrCreateColumnVector(MapVector mv, String qualifier) {
    int oldSize = mv.size();
    NullableVarBinaryVector v = mv.addOrGet(qualifier, COLUMN_TYPE, NullableVarBinaryVector.class);
    if (oldSize != mv.size()) {
      v.allocateNew();
    }
    return v;
  }

  @Override
  public void close() {
    try {
      if (resultScanner != null) {
        resultScanner.close();
      }
      if (hTable != null) {
        hTable.close();
      }
    } catch (IOException e) {
      logger.warn("Failure while closing HBase table: " + hbaseTableName, e);
    }
  }

  private void setOutputRowCount(int count) {
    for (ValueVector vv : familyVectorMap.values()) {
      vv.getMutator().setValueCount(count);
    }
    if (rowKeyVector != null) {
      rowKeyVector.getMutator().setValueCount(count);
    }
  }

  /**
   * Checks if new row can be added in batch. Row can be added if:
   * <ul>
   *   <li>current row count does not exceed max allowed one</li>
   *   <li>allocated memory does not exceed max allowed one</li>
   * </ul>
   *
   * @param rowCount current row count
   * @return true if new row can be added in batch, false otherwise
   */
  private boolean canAddNewRow(int rowCount) {
    return rowCount < TARGET_RECORD_COUNT &&
        operatorContext.getAllocator().getAllocatedMemory() < MAX_ALLOCATED_MEMORY_PER_BATCH;
  }

  @Override
  public String toString() {
    return "HBaseRecordReader[Table=" + hbaseTableName.getNamespaceAsString() + "]";
  }
}
