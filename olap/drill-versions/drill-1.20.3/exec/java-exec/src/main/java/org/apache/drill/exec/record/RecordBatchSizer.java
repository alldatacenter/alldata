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
package org.apache.drill.exec.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.Map;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.memory.BaseAllocator;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.UInt1Vector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.UntypedNullVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.AbstractRepeatedMapVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;

import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.exec.vector.complex.RepeatedVariableWidthVectorLike;
import org.bouncycastle.util.Strings;

import static org.apache.drill.exec.vector.AllocationHelper.STD_REPETITION_FACTOR;

/**
 * Given a record batch or vector container, determines the actual memory
 * consumed by each column, the average row, and the entire record batch.
 */

public class RecordBatchSizer {
  private static final int OFFSET_VECTOR_WIDTH = UInt4Vector.VALUE_WIDTH;
  private static final int BIT_VECTOR_WIDTH = UInt1Vector.VALUE_WIDTH;

  public static long multiplyByFactors(long size, double... factors)
  {
    double doubleSize = size;

    for (double factor: factors) {
      doubleSize *= factor;
    }

    return (long) doubleSize;
  }

  public static long multiplyByFactor(long size, double factor)
  {
    return (long) ((size) * factor);
  }

  /**
   * Column size information.
   */
  public class ColumnSize {
    public final String prefix;
    public final MaterializedField metadata;

    /**
     * This is the total size of just pure data for the column
     * for all entries.
     */

    private int totalDataSize;

    /**
     * This is the total size of data for the column + additional
     * metadata vector overhead we add for cardinality, variable length etc.
     * for all entries.
     */

    private int totalNetSize;

    /**
     * Number of occurrences of the value in the batch. This is trivial
     * for top-level scalars: it is the record count. For a top-level
     * repeated vector, this is the number of arrays, also the record
     * count. For a value nested inside a repeated map, it is the
     * total number of values across all maps, and may be less than,
     * greater than (but unlikely) same as the row count.
     */

    private final int valueCount;

    /**
     * Total number of elements for a repeated type, or same as
     * valueCount if this is a non-repeated type. That is, a batch
     * of 100 rows may have an array with 10 elements per row.
     * In this case, the element count is 1000.
     */

    private int elementCount;

    /**
     * The estimated, average number of elements per parent value.
     * Always 1 for a non-repeated type. For a repeated type,
     * this is the average entries per array (per repeated element).
     */

    private float cardinality;

    /**
     * Indicates if it is variable width column.
     * For map columns, this is true if any of the children is variable
     * width column.
     */

    private boolean isVariableWidth;

    /**
     * Indicates if cardinality is repeated(top level only).
     */

    private boolean isRepeated;

    /**
     * Indicates if cardinality is optional i.e. nullable(top level only).
     */
    private boolean isOptional;

    /**
     * Child columns if this is a map column.
     */
    private final Map<String, ColumnSize> children = CaseInsensitiveMap.newHashMap();

    /**
     * Returns true if there is an accurate std size. Otherwise it returns false.
     * @return True if there is an accurate std size. Otherwise it returns false.
     */
    public boolean hasStdDataSize() {
      return !isVariableWidth && !isRepeated;
    }

    /**
     * std pure data size per entry from Drill metadata, based on type.
     * Does not include metadata vector overhead we add for cardinality,
     * variable length etc.
     * For variable-width columns, we use 50 as std size for entry width.
     * For repeated column, we assume repetition of 10.
     */
    public int getStdDataSizePerEntry() {
      int stdDataSize;

      try {
        stdDataSize = TypeHelper.getSize(metadata.getType());

        // For variable width, typeHelper includes offset vector width. Adjust for that.
        if (isVariableWidth) {
          stdDataSize -= OFFSET_VECTOR_WIDTH;
        }

        if (isRepeated) {
          stdDataSize = stdDataSize * STD_REPETITION_FACTOR;
        }
      } catch (Exception e) {
        // For unsupported types, just set stdSize to 0.
        // Map, Union, List etc.
        stdDataSize = 0;
      }

      // Add sizes of children.
      for (ColumnSize columnSize : children.values()) {
        stdDataSize += columnSize.getStdDataSizePerEntry();
      }

      if (isRepeatedList()) {
        stdDataSize = stdDataSize * STD_REPETITION_FACTOR;
      }

      return stdDataSize;
    }

    /**
     * std net size per entry taking into account additional metadata vectors
     * we add on top for variable length, cardinality etc.
     * For variable-width columns, we use 50 as std data size for entry width.
     * For repeated column, we assume repetition of 10.
     */
    public int getStdNetSizePerEntry() {
      return getStdNetSizePerEntryCommon(metadata.getType(), isOptional, isRepeated, isRepeatedList(), children);
    }

    /**
     * This is the average actual per entry data size in bytes. Does not
     * include any overhead of metadata vectors.
     * For repeated columns, it is average for the repeated array, not
     * individual entry in the array.
     */
    public int getDataSizePerEntry() {
      return safeDivide(getTotalDataSize(), getValueCount());
    }

    /**
     * This is the average per entry size of just pure data plus
     * overhead of additional vectors we add on top like bits vector,
     * offset vector etc. This
     * size is larger than the actual data size since this size includes per-
     * column overhead for additional vectors we add for
     * cardinality, variable length etc.
     */
    public int getNetSizePerEntry() {
      return safeDivide(getTotalNetSize(), getValueCount());
    }

    /**
     * This returns actual entry size if rowCount > 0 or allocation size otherwise.
     * Use this for the cases when you might get empty batches with schema
     * and you still need to do memory calculations based on just schema.
     */
    public int getAllocSizePerEntry() {
      if (rowCount() != 0) {
        return getNetSizePerEntry();
      }

      int stdNetSize;
      try {
        stdNetSize = TypeHelper.getSize(metadata.getType());

        // TypeHelper estimates 50 bytes for variable length. That is pretty high number
        // to use as approximation for empty batches. Use 8 instead.
        switch (metadata.getType().getMinorType()) {
          case VARBINARY:
          case VARCHAR:
          case VAR16CHAR:
          case VARDECIMAL:
            stdNetSize = 4 + 8;
            break;
          default:
        }
      } catch (Exception e) {
        stdNetSize = 0;
      }

      if (isOptional) {
        stdNetSize += BIT_VECTOR_WIDTH;
      }

      if (isRepeated) {
        stdNetSize = (stdNetSize * STD_REPETITION_FACTOR) + OFFSET_VECTOR_WIDTH;
      }

      for (ColumnSize columnSize : children.values()) {
        stdNetSize += columnSize.getAllocSizePerEntry();
      }

      if (isRepeatedList()) {
        stdNetSize = (stdNetSize * STD_REPETITION_FACTOR) + OFFSET_VECTOR_WIDTH;
      }

      return stdNetSize;
    }

    /**
     * If there is an accurate std net size, that is returned. Otherwise the net size is returned.
     * @return If there is an accurate std net size, that is returned. Otherwise the net size is returned.
     */
    public int getStdNetOrNetSizePerEntry() {
      if (hasStdDataSize()) {
        return getStdNetSizePerEntry();
      } else {
        return getNetSizePerEntry();
      }
    }

    /**
     * This is the total data size for the column, including children for map
     * columns. Does not include any overhead of metadata vectors.
     */
    public int getTotalDataSize() {
      int dataSize = this.totalDataSize;
      for (ColumnSize columnSize : children.values()) {
        dataSize += columnSize.getTotalDataSize();
      }
      return dataSize;
    }

    /**
     * This is the total net size for the column, including children for map
     * columns. Includes overhead of metadata vectors.
     */
    public int getTotalNetSize() {
      return this.totalNetSize;
    }

    public int getValueCount() {
      return valueCount;
    }

    public int getElementCount() {
      return elementCount;
    }

    public float getCardinality() {
      return cardinality;
    }

    public boolean isVariableWidth() {
      return isVariableWidth;
    }

    public Map<String, ColumnSize> getChildren() {
      return children;
    }

    public boolean isComplex() {
      switch (metadata.getType().getMinorType()) {
        case LIST:
        case MAP:
        case DICT:
        case UNION:
          return true;
        default:
          return false;
      }
    }

    public boolean isRepeatedList() {
      if (metadata.getType().getMinorType() == MinorType.LIST &&
        metadata.getDataMode() == DataMode.REPEATED) {
        return true;
      }
      return false;
    }

    /**
     * This is the average per entry width, used for vector allocation.
     */
    private int getEntryWidthForAlloc() {
      int width = 0;
      if (isVariableWidth) {
        width = getAllocSizePerEntry() - OFFSET_VECTOR_WIDTH;

        // Subtract out the bits (is-set) vector width
        if (isOptional) {
          width -= BIT_VECTOR_WIDTH;
        }

        if (isRepeated && rowCount() == 0) {
          return (safeDivide(width, STD_REPETITION_FACTOR));
        }
      }

      return (safeDivide(width, getEntryCardinalityForAlloc()));
    }

    /**
     * This is the average per entry cardinality, used for vector allocation.
     */
    private float getEntryCardinalityForAlloc() {
      return getCardinality() == 0 ? (isRepeated ? STD_REPETITION_FACTOR : 1) :getCardinality();
    }

    public ColumnSize(ValueVector v, String prefix) {
      this.prefix = prefix;
      valueCount = v.getAccessor().getValueCount();
      metadata = v.getField();
      isVariableWidth = (v instanceof VariableWidthVector || v instanceof RepeatedVariableWidthVectorLike);
      elementCount = valueCount;
      cardinality = valueCount == 0 ? 0 : 1;
      totalNetSize = v.getPayloadByteCount(valueCount);

      // Special case. For union and list vectors, it is very complex
      // to figure out raw data size. Make it same as net size.
      if (metadata.getType().getMinorType() == MinorType.UNION ||
        (metadata.getType().getMinorType() == MinorType.LIST && v.getField().getDataMode() != DataMode.REPEATED)) {
        totalDataSize = totalNetSize;
      }

      switch(v.getField().getDataMode()) {
        case REPEATED:
          isRepeated = true;
          elementCount  = getElementCount(v);
          cardinality = valueCount == 0 ? 0 : elementCount * 1.0f / valueCount;

          // For complex types, there is nothing more to do for top columns.
          // Data size is calculated recursively for children later.
          if (isComplex()) {
            return;
          }

          // Calculate pure data size.
          if (isVariableWidth) {
            VariableWidthVector dataVector = ((VariableWidthVector) ((RepeatedValueVector) v).getDataVector());
            totalDataSize = dataVector.getCurrentSizeInBytes();
          } else {
            ValueVector dataVector = ((RepeatedValueVector) v).getDataVector();
            totalDataSize = dataVector.getPayloadByteCount(elementCount);
          }

          break;
        case OPTIONAL:
          isOptional = true;

          // For complex types, there is nothing more to do for top columns.
          // Data size is calculated recursively for children later.
          if (isComplex()) {
            return;
          }

          // Calculate pure data size.
          if (isVariableWidth) {
            VariableWidthVector variableWidthVector = ((VariableWidthVector) ((NullableVector) v).getValuesVector());
            totalDataSize = variableWidthVector.getCurrentSizeInBytes();
          } else {
            // Another special case.
            if (v instanceof UntypedNullVector) {
              return;
            }
            totalDataSize = ((NullableVector) v).getValuesVector().getPayloadByteCount(valueCount);
          }
          break;

        case REQUIRED:
          // For complex types, there is nothing more to do for top columns.
          // Data size is calculated recursively for children later.
          if (isComplex()) {
            return;
          }

          // Calculate pure data size.
          if (isVariableWidth) {
            totalDataSize = ((VariableWidthVector) v).getCurrentSizeInBytes();
          } else {
            totalDataSize = v.getPayloadByteCount(valueCount);
          }
          break;

        default:
            break;
      }
    }

    private int getElementCount(ValueVector v) {
      // Repeated vectors are special: they have an associated offset vector
      // that changes the value count of the contained vectors.
      UInt4Vector offsetVector = ((RepeatedValueVector) v).getOffsetVector();
      int childCount = valueCount == 0 ? 0 : offsetVector.getAccessor().get(valueCount);

      return childCount;
    }

    private void allocateMap(AbstractMapVector map, int recordCount) {
      if (map instanceof AbstractRepeatedMapVector) {
        ((AbstractRepeatedMapVector) map).allocateOffsetsNew(recordCount);
          recordCount *= Math.round(getEntryCardinalityForAlloc());
        }

      for (ValueVector vector : map) {
        children.get(vector.getField().getName()).allocateVector(vector, recordCount);
      }
    }

    private void allocateRepeatedList(RepeatedListVector vector, int recordCount) {
      vector.allocateOffsetsNew(recordCount);
      recordCount *= Math.round(getEntryCardinalityForAlloc());
      ColumnSize child = children.get(vector.getField().getName());
      if (vector.getDataVector() != null) {
        child.allocateVector(vector.getDataVector(), recordCount);
      }
    }

    public void allocateVector(ValueVector vector, int recordCount) {
      if (vector instanceof AbstractMapVector) {
        allocateMap((AbstractMapVector) vector, recordCount);
        return;
      }

      if (vector instanceof RepeatedListVector) {
        allocateRepeatedList((RepeatedListVector) vector, recordCount);
        return;
      }

      AllocationHelper.allocate(vector, recordCount, getEntryWidthForAlloc(), getEntryCardinalityForAlloc());
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder()
          .append(prefix)
          .append(metadata.getName())
          .append("(type: ")
          .append(metadata.getType().getMode().name())
          .append(" ")
          .append(metadata.getType().getMinorType().name())
          .append(", count: ")
          .append(valueCount);
      if (metadata.getDataMode() == DataMode.REPEATED) {
        buf.append(", elements: ")
           .append(elementCount)
           .append(", per-array: ")
           .append(cardinality);
      }
      buf.append(", Per entry: std data size: ")
         .append(getStdDataSizePerEntry())
         .append(", std net size: ")
         .append(getStdNetSizePerEntry())
         .append(", actual data size: ")
         .append(getDataSizePerEntry())
         .append(", actual net size: ")
         .append(getNetSizePerEntry())
         .append("  Totals: data size: ")
         .append(getTotalDataSize())
         .append(", net size: ")
         .append(getTotalNetSize())
         .append(")");
      return buf.toString();
    }

    /**
     * Add a single vector initializer to a collection for the entire batch.
     * Uses the observed column size information to predict the size needed
     * when allocating a new vector for the same data. Adds a hint only for
     * variable-width or repeated types; no extra information is needed for
     * fixed width, non-repeated columns.
     *
     * @param initializer the vector initializer to hold the hints
     * for this column
     */

    public void buildVectorInitializer(VectorInitializer initializer) {
      int width = 0;
      switch(metadata.getType().getMinorType()) {
      case VAR16CHAR:
      case VARBINARY:
      case VARCHAR:

        // Subtract out the offset vector width
        width = getNetSizePerEntry() - OFFSET_VECTOR_WIDTH;

        // Subtract out the bits (is-set) vector width
        if (metadata.getDataMode() == DataMode.OPTIONAL) {
          width -= BIT_VECTOR_WIDTH;
        }
        break;
      default:
        break;
      }
      String name = prefix + metadata.getName();
      if (metadata.getDataMode() == DataMode.REPEATED) {
        if (width > 0) {
          // Estimated width is width of entire column. Divide
          // by element count to get per-element size.
          initializer.variableWidthArray(name, width / cardinality, cardinality);
        } else {
          initializer.fixedWidthArray(name, cardinality);
        }
      }
      else if (width > 0) {
        initializer.variableWidth(name, width);
      }

      for (ColumnSize columnSize : children.values()) {
        columnSize.buildVectorInitializer(initializer);
      }
    }

  }

   public static int getStdNetSizePerEntryCommon(TypeProtos.MajorType majorType, boolean isOptional, boolean isRepeated,
                                                 boolean isRepeatedList, Map<String, ColumnSize> children) {
    int stdNetSize;
    try {
      stdNetSize = TypeHelper.getSize(majorType);
    } catch (Exception e) {
      stdNetSize = 0;
    }

    if (isOptional) {
      stdNetSize += BIT_VECTOR_WIDTH;
    }

    if (isRepeated) {
      stdNetSize = (stdNetSize * STD_REPETITION_FACTOR) + OFFSET_VECTOR_WIDTH;
    }

    if (children != null) {
      for (ColumnSize columnSize : children.values()) {
        stdNetSize += columnSize.getStdNetSizePerEntry();
      }
    }

    if (isRepeatedList) {
      stdNetSize = (stdNetSize * STD_REPETITION_FACTOR) + OFFSET_VECTOR_WIDTH;
    }

    return stdNetSize;
  }

  private ColumnSize getComplexColumn(String path) {
    String[] segments = Strings.split(path, '.');
    Map<String, ColumnSize> map = columnSizes;
    return getComplexColumnImpl(segments, 0, map);
  }

  private ColumnSize getComplexColumnImpl(String[] segments, int level, Map<String, ColumnSize> map) {
    ColumnSize result = map.get(segments[level]);
    if (result == null || level == segments.length - 1) {
      return result;
    }
    map = result.getChildren();
    if (map == null) {
      return null;
    }
    return getComplexColumnImpl(segments, level + 1, map);
  }

  public ColumnSize getColumn(String name) {
    final RecordBatchSizer.ColumnSize columnSize =  columnSizes.get(name);
    if (columnSize != null) {
      return columnSize;
    } else {
      return getComplexColumn(name);
    }
  }

  // This keeps information for only top level columns. Information for nested
  // columns can be obtained from children of topColumns.
  private final Map<String, ColumnSize> columnSizes = new QuoteInsensitiveMap(CaseInsensitiveMap.newHashMap());

  /**
   * This field is used by the convenience method {@link #columnsList()}.
   */
  private final List<ColumnSize> columnSizesList = new ArrayList<>();

  /**
   * Number of records (rows) in the batch.
   */
  private final int rowCount;
  /**
   * Actual batch size summing all buffers used to store data
   * for the batch.
   */
  private long accountedMemorySize;
  /**
   * Actual row width computed by dividing total batch memory by the
   * record count.
   */
  private int grossRowWidth;
  /**
   * Actual row width computed by summing columns. Use this if the
   * vectors are partially full; prevents overestimating row width.
   */
  private int netRowWidth;
  private int netRowWidthCap50;

  /**
   * actual row size if input is not empty. Otherwise, standard size.
   */
  private int rowAllocWidth;
  private int stdRowWidth;

  public SelectionVector2 sv2 = null;
  private int sv2Size;

  private int avgDensity;

  private final Set<BufferLedger> ledgers = Sets.newIdentityHashSet();

  private long netBatchSize;

  /**
   *  Maximum width of a column; used for memory estimation in case of Varchars
   */

  public int maxSize;

  /**
   *  Count the nullable columns; used for memory estimation
   */

  public int nullableCount;


  public RecordBatchSizer(RecordBatch batch) {
    this(batch,
      (batch.getSchema() == null ? null : (batch.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.TWO_BYTE ?
        batch.getSelectionVector2() : null)));
  }
  /**
   * Create empirical metadata for a record batch given a vector accessible
   * (basically, an iterator over the vectors in the batch.)
   *
   * @param va iterator over the batch's vectors
   */

  public RecordBatchSizer(VectorAccessible va) {
    this(va, null);
  }

  /**
   * Create empirical metadata for a record batch given a vector accessible
   * (basically, an iterator over the vectors in the batch) along with a
   * selection vector for those records. The selection vector is used to
   * pad the estimated row width with the extra two bytes needed per record.
   * The selection vector memory is added to the total memory consumed by
   * this batch.
   *
   * @param va iterator over the batch's vectors
   * @param sv2 selection vector associated with this batch
   */

  public RecordBatchSizer(VectorAccessible va, SelectionVector2 sv2) {
    rowCount = va.getRecordCount();
    for (VectorWrapper<?> vw : va) {
      ColumnSize colSize = measureColumn(vw.getValueVector(), "");
      columnSizes.put(vw.getField().getName(), colSize);
      columnSizesList.add(colSize);
      stdRowWidth += colSize.getStdDataSizePerEntry();
      netBatchSize += colSize.getTotalNetSize();
      maxSize = Math.max(maxSize, colSize.getTotalDataSize());
      if (colSize.metadata.isNullable()) {
        nullableCount++;
      }
      netRowWidth += colSize.getNetSizePerEntry();
    }
    this.sv2 = sv2;
  }

  public void applySv2() {
    if (sv2 == null) {
      return;
    }

    sv2Size = BaseAllocator.nextPowerOfTwo(2 * rowCount);
    avgDensity = safeDivide(netBatchSize * 100L, getActualSize());
    accountedMemorySize += sv2Size;
  }

  /**
   *  Round up (if needed) to the next power of 2 (only up to 64)
   * @param arg Number to round up (must be < 64)
   * @return power of 2 result
   */
  private int roundUpToPowerOf2(int arg) {
    if ( arg <= 2 ) { return 2; }
    if ( arg <= 4 ) { return 4; }
    if ( arg <= 8 ) { return 8; }
    if ( arg <= 16 ) { return 16; }
    if ( arg <= 32 ) { return 32; }
    return 64;
  }

  private ColumnSize measureColumn(ValueVector v, String prefix) {
    ColumnSize colSize = new ColumnSize(v, prefix);
    switch (v.getField().getType().getMinorType()) {
      case MAP:
      case DICT:
        // Maps consume no size themselves. However, their contained
        // vectors do consume space, so visit columns recursively.
        expandMap(colSize, v, prefix + v.getField().getName() + ".");
        break;
      case LIST:
        // complex ListVector cannot be casted to RepeatedListVector.
        // do not expand the list if it is not repeated mode.
        if (v.getField().getDataMode() == DataMode.REPEATED) {
          expandList(colSize, (RepeatedListVector) v, prefix + v.getField().getName() + ".");
        }
        break;
      default:
        v.collectLedgers(ledgers);
    }

    netRowWidthCap50 += ! colSize.isVariableWidth ? colSize.getNetSizePerEntry() :
        8 /* offset vector */ + roundUpToPowerOf2(Math.min(colSize.getNetSizePerEntry(),50));
        // above change 8 to 4 after DRILL-5446 is fixed

    return colSize;
  }

  private void expandMap(ColumnSize colSize, ValueVector mapVector, String prefix) {
    for (ValueVector vector : mapVector) {
      colSize.children.put(vector.getField().getName(), measureColumn(vector, prefix));
    }

    // For a repeated map, we need the memory for the offset vector (only).
    // Map elements are recursively expanded above.
    if (mapVector.getField().getDataMode() == DataMode.REPEATED) {
      ((RepeatedValueVector) mapVector).getOffsetVector().collectLedgers(ledgers);
    }
  }

  private void expandList(ColumnSize colSize, RepeatedListVector vector, String prefix) {
    colSize.children.put(vector.getField().getName(), measureColumn(vector.getDataVector(), prefix));

    // Determine memory for the offset vector (only).
    vector.collectLedgers(ledgers);
  }

  public static int safeDivide(long num, long denom) {
    if (denom == 0) {
      return 0;
    }
    return (int) Math.ceil((double) num / denom);
  }

  public static int safeDivide(int num, int denom) {
    if (denom == 0) {
      return 0;
    }
    return (int) Math.ceil((double) num / denom);
  }

  public static int safeDivide(int num, float denom) {
    if (denom == 0) {
      return 0;
    }
    return (int) Math.ceil((double) num / denom);
  }

  public static int safeDivide(int num, double denom) {
    if (denom == 0) {
      return 0;
    }
    return (int) Math.ceil(num / denom);
  }

  public int rowCount() { return rowCount; }

  public int getStdRowWidth() {
    if (stdRowWidth != 0) {
      return stdRowWidth;
    }

    for (ColumnSize columnSize : columnSizes.values()) {
      stdRowWidth += columnSize.getStdDataSizePerEntry();
    }

    return stdRowWidth;
  }

  public int getRowAllocWidth() {
    if (rowAllocWidth != 0) {
      return rowAllocWidth;
    }

    for (ColumnSize columnSize : columnSizes.values()) {
      rowAllocWidth += columnSize.getAllocSizePerEntry();
    }

    return rowAllocWidth;
  }

  public long getActualSize() {
    if (accountedMemorySize != 0) {
      return accountedMemorySize;
    }

    for (BufferLedger ledger : ledgers) {
      accountedMemorySize += ledger.getAccountedSize();
    }

    if (sv2 != null) {
      sv2Size = sv2.getBuffer(false).capacity();
      accountedMemorySize += sv2Size;
    }

    return accountedMemorySize;
  }

  public int getGrossRowWidth() {
    if (grossRowWidth != 0) {
      return grossRowWidth;
    }

    grossRowWidth = safeDivide(getActualSize(), rowCount);

    return grossRowWidth;
  }

  public int getAvgDensity() {
    return safeDivide(netBatchSize * 100L, getActualSize());
  }


  public int getNetRowWidth() { return netRowWidth; }
  public Map<String, ColumnSize> columns() { return columnSizes; }

  /**
   * This is a convenience method to get the sizes of columns in the same order that the corresponding value vectors
   * are stored within a {@link org.apache.drill.exec.record.VectorAccessible}.
   * @return The sizes of columns in the same order that the corresponding value vectors are stored within a
   * {@link org.apache.drill.exec.record.VectorAccessible}.
   */
  public List<ColumnSize> columnsList() { return columnSizesList; }

  /**
   * Compute the "real" width of the row, taking into account each varchar column size
   * (historically capped at 50, and rounded up to power of 2 to match drill buf allocation)
   * and null marking columns.
   * @return "real" width of the row
   */
  public int getNetRowWidthCap50() { return netRowWidthCap50 + nullableCount; }
  public boolean hasSv2() { return sv2 != null; }
  public long getNetBatchSize() { return netBatchSize; }
  public int getMaxAvgColumnSize() { return safeDivide(maxSize, rowCount); }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append("Batch size: {");
    buf.append( "  Records: " );
    buf.append(rowCount);
    buf.append(", Total size: ");
    buf.append(accountedMemorySize);
    buf.append(", Data size: ");
    buf.append(netBatchSize);
    buf.append(", Gross row width: ");
    buf.append(grossRowWidth);
    buf.append(", Net row width: ");
    buf.append(netRowWidth);
    buf.append(", Density: ");
    buf.append(avgDensity);
    buf.append("% }\n");

    buf.append("Batch schema & sizes: {\n");
    for (ColumnSize colSize : columnSizes.values()) {
      buf.append("  ");
      buf.append(colSize.toString());
      buf.append(" }\n");
    }
    buf.append(" }\n");
    return buf.toString();
  }

  /**
   * The column size information gathered here represents empirically-derived
   * schema metadata. Use that metadata to create an instance of a class that
   * allocates memory for new vectors based on the observed size information.
   * The caller provides the row count; the size information here provides
   * column widths and the number of elements in each array.
   */

  public VectorInitializer buildVectorInitializer() {
    VectorInitializer initializer = new VectorInitializer();
    for (ColumnSize colSize : columnSizes.values()) {
      colSize.buildVectorInitializer(initializer);
    }
    return initializer;
  }

  public void allocateVectors(VectorContainer container, int recordCount) {
    for (VectorWrapper<?> w : container) {
      ColumnSize colSize = columnSizes.get(w.getField().getName());
      colSize.allocateVector(w.getValueVector(), recordCount);
    }
  }

  /**
   * A map that can handle quoted and unquoted column names; ideally this logic temporary and
   * should be removed as soon as all readers standardize handling of missing columns. Quoted columns
   * have been added in DRILL-4264.
   */
  private static final class QuoteInsensitiveMap implements Map<String, ColumnSize> {
    /** Original Map */
    private final Map<String, ColumnSize> originalMap;

    private QuoteInsensitiveMap(Map<String, ColumnSize> originalMap) {
      this.originalMap = originalMap;
    }

    @Override
    public int size() {
      return originalMap.size();
    }

    @Override
    public boolean isEmpty() {
      return originalMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return originalMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return originalMap.containsValue(value);
    }

    @Override
    public ColumnSize get(Object key) {
      ColumnSize value = originalMap.get(key);

      if (value == null) {
        value = originalMap.get(quoteString(key));
      }
      return value;
    }

    @Override
    public ColumnSize put(String key, ColumnSize value) {
      return originalMap.put(key, value);
    }

    @Override
    public ColumnSize remove(Object key) {
      ColumnSize value = originalMap.remove(key);

      if (value == null) {
        value = originalMap.remove(quoteString(key));
      }
      return value;
    }

    @Override
    public void putAll(Map<? extends String, ? extends ColumnSize> m) {
      originalMap.putAll(m);
    }

    @Override
    public void clear() {
      originalMap.clear();
    }

    @Override
    public Set<String> keySet() {
      return originalMap.keySet();
    }

    @Override
    public Collection<ColumnSize> values() {
      return originalMap.values();
    }

    @Override
    public Set<Entry<String, ColumnSize>> entrySet() {
      return originalMap.entrySet();
    }

    private String quoteString(Object key) {
      if (key instanceof String) {
        return "`" + key + '`';
      }
      throw new IllegalArgumentException();
    }

  }
}
