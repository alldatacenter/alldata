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
package org.apache.drill.exec.store.parquet.columnreaders.batchsizing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetColumnMetadata;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetSchema;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.FieldOverflowDefinition;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchStatsContext;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * This class is tasked with managing all aspects of flat Parquet reader record batch sizing logic.
 * Currently a record batch size is constrained with two parameters: Number of rows and Memory usage.
 */
public final class RecordBatchSizerManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordBatchSizerManager.class);


  /** Minimum column memory size */
  private static final int MIN_COLUMN_MEMORY_SZ = VarLenColumnBulkInput.getMinVLColumnMemorySize();

  /** Parquet schema object */
  private final ParquetSchema schema;
  /** Total records to read */
  private final long totalRecordsToRead;
  /** Logic to minimize overflow occurrences */
  private final BatchOverflowOptimizer overflowOptimizer;

  /** Configured Parquet records per batch */
  private final int configRecordsPerBatch;
  /** Configured Parquet memory size per batch */
  private final long configMemorySizePerBatch;
  /** An upper bound on the Parquet records per batch based on the configured value and schema */
  private int maxRecordsPerBatch;
  /** An upper bound on the Parquet memory size per batch based on the configured value and schema  */
  private long maxMemorySizePerBatch;
  /** The current number of records per batch as it can be dynamically optimized */
  private int recordsPerBatch;

  /** List of fixed columns */
  private final List<ColumnMemoryInfo> fixedLengthColumns = new ArrayList<ColumnMemoryInfo>();
  /** List of variable columns */
  private final List<ColumnMemoryInfo> variableLengthColumns = new ArrayList<ColumnMemoryInfo>();
  /** Field to column memory information map */
  private final Map<String, ColumnMemoryInfo> columnMemoryInfoMap = CaseInsensitiveMap.newHashMap();
  /** Indicator invoked when column(s) precision change */
  private boolean columnPrecisionChanged;

  /**
   * Field overflow map; this information is stored within this class for two reasons:
   * a) centralization to simplify resource deallocation (overflow data is backed by Direct Memory)
   * b) overflow is a result of batch constraints enforcement which this class manages the overflow logic
   */
  private Map<String, FieldOverflowStateContainer> fieldOverflowMap = CaseInsensitiveMap.newHashMap();

  /** For controlling batch statistics logging */
  private final RecordBatchStatsContext batchStatsContext;

  /**
   * Constructor.
   *
   * @param options drill options
   * @param schema current reader schema
   * @param totalRecordsToRead total number of rows to read
   */
  public RecordBatchSizerManager(OptionManager options,
    ParquetSchema schema,
    long totalRecordsToRead,
    RecordBatchStatsContext batchStatsContext) {

    this.schema = schema;
    this.totalRecordsToRead = totalRecordsToRead;
    this.configRecordsPerBatch = (int) options.getLong(ExecConstants.PARQUET_FLAT_BATCH_NUM_RECORDS);
    this.configMemorySizePerBatch = getConfiguredMaxBatchMemory(options);
    this.maxMemorySizePerBatch = this.configMemorySizePerBatch;
    this.maxRecordsPerBatch = this.configRecordsPerBatch;
    this.recordsPerBatch = this.configRecordsPerBatch;
    this.overflowOptimizer = new BatchOverflowOptimizer(columnMemoryInfoMap);
    this.batchStatsContext = batchStatsContext;
  }

  /**
   * Tunes record batch parameters based on configuration and schema.
   */
  public void setup() {

    // Normalize batch parameters
    this.maxMemorySizePerBatch = normalizeMemorySizePerBatch();
    this.maxRecordsPerBatch = normalizeNumRecordsPerBatch();

    // Let's load the column metadata
    loadColumnsPrecisionInfo();

    if (getNumColumns() == 0) {
      return; // there are cases where downstream operators don't select any columns
              // in such a case, Parquet will return the pseudo column _DEFAULT_COL_TO_READ_
    }

    // We need to divide the overall memory pool amongst all columns
    assignColumnsBatchMemory();

    // Initialize the overflow optimizer
    overflowOptimizer.setup();
  }

  /**
   * @return the schema
   */
  public ParquetSchema getSchema() {
    return schema;
  }

  /**
   * @return batch statistics context
   */
  public RecordBatchStatsContext getBatchStatsContext() {
    return batchStatsContext;
  }

  /**
   * Allocates value vectors for the current batch.
   *
   * @param vectorMap a collection of value vectors keyed by their field names
   * @throws OutOfMemoryException
   */
  public void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException {

    if (columnPrecisionChanged) {
      // We need to divide the overall memory pool amongst all columns
      assignColumnsBatchMemory();
    }

    try {
      for (final ValueVector v : vectorMap.values()) {
        ColumnMemoryInfo columnMemoryInfo = columnMemoryInfoMap.get(v.getField().getName());

        if (columnMemoryInfo != null) {
          Preconditions.checkState(columnMemoryInfo.columnPrecision <= Integer.MAX_VALUE, "Column precision cannot exceed 2GB");
          AllocationHelper.allocate(v, recordsPerBatch, (int) columnMemoryInfo.columnPrecision, 0);
        } else {
          // This column was found in another Parquet file but not the current one; so we inject
          // a null value. At this time, we do not account for such columns. Why? the right design is
          // to create a ZERO byte all-nulls value vector to handle such columns (there could be hundred of these).
          AllocationHelper.allocate(v, recordsPerBatch, 0, 0); // the helper will still use a precision of 1
        }
      }
    } catch (NullPointerException e) {
      throw new OutOfMemoryException();
    }
  }

  /**
   * @return the field overflow state map
   */
  public Map<String, FieldOverflowStateContainer> getFieldOverflowMap() {
    return fieldOverflowMap;
  }

  /**
   * @param field materialized field
   * @return field overflow state container
   */
  public FieldOverflowStateContainer getFieldOverflowContainer(String field) {
    return fieldOverflowMap.get(field);
  }

  /**
   * Releases the overflow data resources associated with this field; also removes the overflow
   * container from the overflow containers map.
   *
   * @param field materialized field
   * @return true if this field's overflow container was removed from the overflow containers map
   */
  public boolean releaseFieldOverflowContainer(String field) {
    return releaseFieldOverflowContainer(field, true);
  }

  /**
   * @param field materialized field
   * @return field batch memory quota
   */
  public ColumnMemoryQuota getCurrentFieldBatchMemory(String field) {
    return columnMemoryInfoMap.get(field).columnMemoryQuota;
  }

  /**
   * @return current number of records per batch (may change across batches)
   */
  public int getCurrentRecordsPerBatch() {
    return recordsPerBatch;
  }

  /**
   * @return current total memory per batch (may change across batches)
   */
  public long getCurrentMemorySizePerBatch() {
    return maxMemorySizePerBatch; // Current logic doesn't mutate the max-memory after it has been set
  }

  /**
   * @return configured number of records per batch (may be different from the enforced one)
   */
  public int getConfigRecordsPerBatch() {
    return configRecordsPerBatch;
  }

  /**
   * @return configured memory size per batch (may be different from the enforced one)
   */
  public long getConfigMemorySizePerBatch() {
    return configMemorySizePerBatch;
  }

  /**
   * Enables this object to optimize the impact of overflows by computing more
   * accurate VL column precision.
   *
   * @param batchNumRecords number of records in this batch
   * @param batchStats columns statistics
   */
  public void onEndOfBatch(int batchNumRecords, List<VarLenColumnBatchStats> batchStats) {
    columnPrecisionChanged = overflowOptimizer.onEndOfBatch(batchNumRecords, batchStats);
  }

  /**
   * Closes all resources managed by this object
   */
  public void close() {

    for (String field: fieldOverflowMap.keySet()) {
      releaseFieldOverflowContainer(field, false);
    }

    // now clear the map
    fieldOverflowMap.clear();
  }

// ----------------------------------------------------------------------------
// Internal implementation logic
// ----------------------------------------------------------------------------

  private long getConfiguredMaxBatchMemory(OptionManager options) {
    // Use the parquet specific configuration if set
    long maxMemory = options.getLong(ExecConstants.PARQUET_FLAT_BATCH_MEMORY_SIZE);

    // Otherwise, use the common property
    if (maxMemory <= 0) {
      maxMemory = options.getLong(ExecConstants.OUTPUT_BATCH_SIZE);
    }
    return maxMemory;
  }

  private int normalizeNumRecordsPerBatch() {
    int normalizedNumRecords = configRecordsPerBatch;

    if (configRecordsPerBatch <= 0) {
      final String message = String.format("Invalid Parquet number of record(s) per batch [%d]",
        configRecordsPerBatch);

      throw new IllegalArgumentException(message);
    }

    if (normalizedNumRecords > totalRecordsToRead) {
      if (logger.isDebugEnabled()) {
        final String message = String.format("The requested number of record(s) to read is lower than the records per batch; "
          + "updating the number of record(s) per batch from [%d] to [%d]",
          normalizedNumRecords, totalRecordsToRead);
        logger.debug(message);
      }
      normalizedNumRecords = (int) totalRecordsToRead;
    }

    if (batchStatsContext.isEnableBatchSzLogging()) {
      final String message = String.format("The Parquet reader number of record(s) has been set to [%d]", normalizedNumRecords);
      RecordBatchStats.logRecordBatchStats(message, batchStatsContext);
    }

    return normalizedNumRecords;
  }

  private long normalizeMemorySizePerBatch() {
    long normalizedMemorySize = configMemorySizePerBatch;

    if (normalizedMemorySize <= 0) {
      final String message = String.format("Invalid Parquet memory per batch [%d] byte(s)",
        configMemorySizePerBatch);

      throw new IllegalArgumentException(message);
    }

    // Ensure the minimal memory size per column is satisfied
    final int numColumns = schema.getColumnMetadata().size();

    if (numColumns == 0) {
      return normalizedMemorySize; // NOOP
    }

    final long memorySizePerColumn = normalizedMemorySize / numColumns;

    if (memorySizePerColumn < MIN_COLUMN_MEMORY_SZ) {
      final long prevValue   = normalizedMemorySize;
      normalizedMemorySize  = MIN_COLUMN_MEMORY_SZ * numColumns;

      final String message = String.format("The Parquet memory per batch [%d] byte(s) is too low for this query ; using [%d] bytes",
        prevValue, normalizedMemorySize);
      logger.warn(message);
    }

    if (batchStatsContext.isEnableBatchSzLogging()) {
      RecordBatchStats.printConfiguredBatchSize(batchStatsContext, (int) normalizedMemorySize);
    }

    return normalizedMemorySize;
  }

  private void loadColumnsPrecisionInfo() {
    assert fixedLengthColumns.size() == 0;
    assert variableLengthColumns.size() == 0;

    for (ParquetColumnMetadata columnMetadata : schema.getColumnMetadata()) {
      assert !columnMetadata.isRepeated() : "This reader doesn't handle repeated columns..";

      ColumnMemoryInfo columnMemoryInfo = new ColumnMemoryInfo();
      columnMemoryInfoMap.put(columnMetadata.getField().getName(), columnMemoryInfo);

      if (columnMetadata.isFixedLength()) {
        columnMemoryInfo.columnMeta = columnMetadata;
        columnMemoryInfo.columnPrecision = BatchSizingMemoryUtil.getFixedColumnTypePrecision(columnMetadata);
        columnMemoryInfo.columnMemoryQuota.reset();

        fixedLengthColumns.add(columnMemoryInfo);
      } else {

        columnMemoryInfo.columnMeta = columnMetadata;
        columnMemoryInfo.columnPrecision = BatchSizingMemoryUtil.getAvgVariableLengthColumnTypePrecision(columnMetadata);
        columnMemoryInfo.columnMemoryQuota.reset();

        variableLengthColumns.add(columnMemoryInfo);
      }
    }
  }

  private void assignColumnsBatchMemory() {

    if (getNumColumns() == 0 ||
        maxRecordsPerBatch == 0) { // Happens when all row-groups are pruned, and only one is returned empty (TODO: currently not empty)
      return;
    }

    recordsPerBatch = maxRecordsPerBatch;

    // Cache the original records per batch as it may change
    int originalRecordsPerBatch = recordsPerBatch;

    // Perform the fine-grained memory quota assignment
    assignFineGrainedMemoryQuota();

    // log the new record batch if it changed
    if (batchStatsContext.isEnableBatchSzLogging()) {
      assert recordsPerBatch <= maxRecordsPerBatch;

      if (originalRecordsPerBatch != recordsPerBatch) {
        final String message = String.format("The Parquet records per batch [%d] has been decreased to [%d]", originalRecordsPerBatch, recordsPerBatch);
        RecordBatchStats.logRecordBatchStats(message, batchStatsContext);
      }

      // Now dump the per column memory quotas
      dumpColumnMemoryQuotas();
    }
  }

  private void assignFineGrainedMemoryQuota() {

    // - Compute the memory required based on the current batch size and assigned column precision
    // - Compute the ration have-memory / needed-memory
    // - if less than one, new-num-records = num-recs * ratio; go back to previous steps
    // - distribute the extra memory uniformly across the variable length columns

    MemoryRequirementContainer requiredMemory = new MemoryRequirementContainer();
    int newRecordsPerBatch = recordsPerBatch;

    while (true) {
      // Compute max-memory / needed-memory-for-current-num-records
      recordsPerBatch          = newRecordsPerBatch;
      double neededMemoryRatio = computeNeededMemoryRatio(requiredMemory);
      assert neededMemoryRatio <= 1;

      newRecordsPerBatch = (int) (recordsPerBatch * neededMemoryRatio);
      assert newRecordsPerBatch <= recordsPerBatch;

      if (newRecordsPerBatch <= 1) {
        recordsPerBatch = 1;
        computeNeededMemoryRatio(requiredMemory); // update the memory quota with this new number of records
        break; // we cannot process less than one row

      } else if (newRecordsPerBatch < recordsPerBatch) {
        // We computed a new number of records per batch; we need to
        // a) make sure this new number satisfies our needs and b)
        // per per column quota
        continue;
      }
      assert recordsPerBatch == newRecordsPerBatch;

      // Alright, we have now found the target number of records; we need
      // only to adjust the remaining memory (if any) amongst the variable
      // length columns.
      distributeExtraMemorySpace(requiredMemory);
      break; // we're done
    }
  }

  private void distributeExtraMemorySpace(MemoryRequirementContainer requiredMemory) {
    // Distribute uniformly the extra memory space to the variable length columns
    // to minimize the chance of overflow conditions.
    final int numVariableLengthColumns = variableLengthColumns.size();

    if (numVariableLengthColumns == 0) {
      return; // we're done
    }

    final long totalMemoryNeeded = requiredMemory.fixedLenRequiredMemory + requiredMemory.variableLenRequiredMemory;
    final long extraMemorySpace = maxMemorySizePerBatch - totalMemoryNeeded;
    final long perColumnExtraSpace = extraMemorySpace / numVariableLengthColumns;

    if (perColumnExtraSpace == 0) {
      return;
    }

    for (ColumnMemoryInfo columnInfo : variableLengthColumns) {
      columnInfo.columnMemoryQuota.maxMemoryUsage += perColumnExtraSpace;
    }
  }

  private int getNumColumns() {
    return fixedLengthColumns.size() + variableLengthColumns.size();
  }

  private boolean releaseFieldOverflowContainer(String field, boolean remove) {
    FieldOverflowStateContainer container = getFieldOverflowContainer(field);

    if (container == null) {
      return false; // NOOP
    }

    // We need to release resources associated with this container
    container.release();
    container.overflowDef = null;
    container.overflowState = null;

    if (remove) {
      // Finally remove this container from the map
      fieldOverflowMap.remove(field);
    }

    return remove;
  }

  private long computeVectorMemory(ColumnMemoryInfo columnInfo, int numValues) {
    if (columnInfo.columnMeta.isFixedLength()) {
      return BatchSizingMemoryUtil.computeFixedLengthVectorMemory(columnInfo.columnMeta, numValues);
    }
    return BatchSizingMemoryUtil.computeVariableLengthVectorMemory(
      columnInfo.columnMeta,
      columnInfo.columnPrecision,
      numValues);
  }

  private double computeNeededMemoryRatio(MemoryRequirementContainer requiredMemory) {
    requiredMemory.reset();

    for (ColumnMemoryInfo columnInfo : fixedLengthColumns) {
      columnInfo.columnMemoryQuota.maxMemoryUsage = computeVectorMemory(columnInfo, recordsPerBatch);
      columnInfo.columnMemoryQuota.maxNumValues   = recordsPerBatch;
      requiredMemory.fixedLenRequiredMemory      += columnInfo.columnMemoryQuota.maxMemoryUsage;
    }

    for (ColumnMemoryInfo columnInfo : variableLengthColumns) {
      columnInfo.columnMemoryQuota.maxMemoryUsage = computeVectorMemory(columnInfo, recordsPerBatch);
      columnInfo.columnMemoryQuota.maxNumValues   = recordsPerBatch;
      requiredMemory.variableLenRequiredMemory   += columnInfo.columnMemoryQuota.maxMemoryUsage;
    }

    final long totalMemoryNeeded = requiredMemory.fixedLenRequiredMemory + requiredMemory.variableLenRequiredMemory;
    assert totalMemoryNeeded > 0;

    double neededMemoryRatio = ((double) maxMemorySizePerBatch) / totalMemoryNeeded;

    return neededMemoryRatio > 1 ? 1 : neededMemoryRatio;
  }

  private void dumpColumnMemoryQuotas() {
    StringBuilder msg = new StringBuilder();
    msg.append(": Field Quotas:\n\tName\tType\tPrec\tQuota\n");

    for (ColumnMemoryInfo columnInfo : columnMemoryInfoMap.values()) {
      msg.append("\t");
      msg.append(RecordBatchStats.BATCH_STATS_PREFIX);
      msg.append("\t");
      msg.append(columnInfo.columnMeta.getField().getName());
      msg.append("\t");
      printType(columnInfo.columnMeta.getField(), msg);
      msg.append("\t");
      msg.append(columnInfo.columnPrecision);
      msg.append("\t");
      msg.append(columnInfo.columnMemoryQuota.maxMemoryUsage);
      msg.append("\n");
    }

    RecordBatchStats.logRecordBatchStats(msg.toString(), batchStatsContext);
  }

  private  static void printType(MaterializedField field, StringBuilder msg) {
    final MajorType type = field.getType();

    msg.append(type.getMinorType().name());
    msg.append(':');
    msg.append(type.getMode().name());
  }


// ----------------------------------------------------------------------------
// Inner Data Structure
// ----------------------------------------------------------------------------

  /** An abstraction to allow column readers attach custom field overflow state */
  public static interface FieldOverflowState {

    /** Overflow data can become an input source for the next batch(s); this method
     * allows the reader framework to inform individual readers on the number of
     * values that have been consumed from the current overflow data
     *
     * @param numValues the number of values consumed within the current batch
     */
    void onNewBatchValuesConsumed(int numValues);

    /**
     * @return true if the overflow data has been fully consumed (all overflow data consumed by
     *              the Parquet reader)
     */
    boolean isOverflowDataFullyConsumed();
  }

  /** Container object to hold current field overflow state */
  public static final class FieldOverflowStateContainer {
    /** Field overflow definition */
    public FieldOverflowDefinition overflowDef;
    /** Field overflow state */
    public FieldOverflowState overflowState;

    public FieldOverflowStateContainer(FieldOverflowDefinition overflowDef, FieldOverflowState overflowState) {
      this.overflowDef   = overflowDef;
      this.overflowState = overflowState;
    }

    private void release() {
      if (overflowDef != null) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
            "Releasing a buffer of length %d used to handle overflow data", overflowDef.buffer.capacity()));
        }
        overflowDef.buffer.release();
      }
      overflowDef = null;
      overflowState = null;
    }
  }

  /** Container object to supply variable columns statistics to the batch sizer */
  public final static class VarLenColumnBatchStats {
    /** Value vector associated with a VL column */
    public final ValueVector vector;
    /** Number of values read in the current batch */
    public final int numValuesRead;

    /**
     * Constructor.
     * @param vector value vector
     * @param numValuesRead number of values
     */
    public VarLenColumnBatchStats(ValueVector vector, int numValuesRead) {
      this.vector = vector;
      this.numValuesRead = numValuesRead;
    }
  }

  /** Field memory quota */
  public static final class ColumnMemoryQuota {
    /** Maximum cumulative memory that could be used */
    private long maxMemoryUsage;
    /** Maximum number of values that could be inserted */
    private int maxNumValues;

    public ColumnMemoryQuota() {
    }

    /**
     * @param maxMemoryUsage maximum cumulative memory that could be used
     */
    public ColumnMemoryQuota(long maxMemoryUsage) {
      this.maxMemoryUsage = maxMemoryUsage;
    }

    /**
     * @return the maxMemoryUsage
     */
    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    /**
     * @return the maxNumValues
     */
    public int getMaxNumValues() {
      return maxNumValues;
    }

    void reset() {
      maxMemoryUsage = 0;
      maxNumValues = 0;
    }
  }

  /** A container which holds a column memory precision & current quota information */
  static final class ColumnMemoryInfo {
    /** Column metadata */
    ParquetColumnMetadata columnMeta;
    /** Column value precision (maximum length for VL columns) */
    long columnPrecision;
    /** Column current memory quota within a batch */
    final ColumnMemoryQuota columnMemoryQuota = new ColumnMemoryQuota();
  }

  /** Memory requirements container */
  static final class MemoryRequirementContainer {
    /** Memory needed for the fixed length columns given a specific record size */
    private long fixedLenRequiredMemory;
    /** Memory needed for the fixed length columns given a specific record size */
    private long variableLenRequiredMemory;

    private void reset() {
      this.fixedLenRequiredMemory    = 0;
      this.variableLenRequiredMemory = 0;
    }
  }

}
