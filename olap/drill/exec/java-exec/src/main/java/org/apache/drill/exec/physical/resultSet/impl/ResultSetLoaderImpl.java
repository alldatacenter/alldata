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
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.TupleState.RowState;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the result set loader. Caches vectors
 * for a row or map.
 *
 * @see {@link ResultSetLoader}
 */
public class ResultSetLoaderImpl implements ResultSetLoader, LoaderInternals {
  protected static final Logger logger = LoggerFactory.getLogger(ResultSetLoaderImpl.class);

  /**
   * Read-only set of options for the result set loader.
   */
  public static class ResultSetOptions {
    protected final int vectorSizeLimit;
    protected final int rowCountLimit;
    protected final ResultVectorCache vectorCache;
    protected final ProjectionFilter projectionSet;
    protected final TupleMetadata schema;
    protected final long maxBatchSize;
    protected final long scanLimit;

    /**
     * Context for error messages.
     */
    protected final CustomErrorContext errorContext;

    public ResultSetOptions() {
      vectorSizeLimit = ValueVector.MAX_BUFFER_SIZE;
      rowCountLimit = DEFAULT_ROW_COUNT;
      projectionSet = ProjectionFilter.PROJECT_ALL;
      vectorCache = null;
      schema = null;
      maxBatchSize = -1;
      errorContext = null;
      scanLimit = Long.MAX_VALUE;
    }

    public ResultSetOptions(ResultSetOptionBuilder builder) {
      vectorSizeLimit = builder.vectorSizeLimit;
      rowCountLimit = builder.rowCountLimit;
      vectorCache = builder.vectorCache;
      schema = builder.readerSchema;
      maxBatchSize = builder.maxBatchSize;
      scanLimit = builder.scanLimit;
      errorContext = builder.errorContext == null
          ? EmptyErrorContext.INSTANCE : builder.errorContext;
      if (builder.projectionFilter != null) {
        projectionSet = builder.projectionFilter;
      } else if (builder.projectionSet != null) {
        projectionSet = ProjectionFilter.projectionFilter(builder.projectionSet, errorContext);
      } else {
        projectionSet = ProjectionFilter.PROJECT_ALL;
      }
      if (schema != null && MetadataUtils.hasDynamicColumns(schema)) {
        throw UserException.validationError()
          .message("Reader input schema must not contain dynamic columns")
          .addContext(errorContext)
          .build(logger);
      }
    }

    public void dump(HierarchicalFormatter format) {
      format
        .startObject(this)
        .attribute("vectorSizeLimit", vectorSizeLimit)
        .attribute("rowCountLimit", rowCountLimit)
        .endObject();
    }
  }

  private enum State {

    /**
     * Before the first batch.
     */
    START,

    /**
     * Writing to a batch normally.
     */
    ACTIVE,

    /**
     * Batch overflowed a vector while writing. Can continue
     * to write to a temporary "overflow" batch until the
     * end of the current row.
     */
    OVERFLOW,

    /**
     * Temporary state to avoid batch-size related overflow while
     * an overflow is in progress.
     */
    IN_OVERFLOW,

    /**
     * Batch is full due to reaching the row count or scan limit
     * when saving a row.
     * No more writes allowed until harvesting the current batch.
     */
    FULL_BATCH,

    /**
     * Current batch was harvested: data is gone. No lookahead
     * batch exists.
     */
    HARVESTED,

    /**
     * Current batch was harvested and its data is gone. However,
     * overflow occurred during that batch and the data exists
     * in the overflow vectors.
     * <p>
     * This state needs special consideration. The column writer
     * structure maintains its state (offsets, etc.) from the OVERFLOW
     * state, but the buffers currently in the vectors are from the
     * complete batch. <b>No writes can be done in this state!</b>
     * The writer state does not match the data in the buffers.
     * The code here does what it can to catch this state. But, if
     * some client tries to write to a column writer in this state,
     * bad things will happen. Doing so is invalid (the write is outside
     * of a batch), so this is not a terrible restriction.
     * <p>
     * Said another way, the current writer state is invalid with respect
     * to the active buffers, but only if the writers try to act on the
     * buffers. Since the writers won't do so, this temporary state is
     * fine. The correct buffers are restored once a new batch is started
     * and the state moves to ACTIVE.
     */
    LOOK_AHEAD,

    /**
     * The loader has reached the given scan limit. No further batches
     * can be started.
     */
    LIMITED,

    /**
     * Mutator is closed: no more operations are allowed.
     */
    CLOSED
  }

  /**
   * Options provided to this loader.
   */
  private final ResultSetOptions options;

  /**
   * Allocator for vectors created by this loader.
   */
  private final BufferAllocator allocator;

  /**
   * Builds columns (vector, writer, state).
   */
  private final ColumnBuilder columnBuilder;

  /**
   * Internal structure used to work with the vectors (real or dummy) used
   * by this loader.
   */
  private final RowState rootState;

  /**
   * Top-level writer index that steps through the rows as they are written.
   * When an overflow batch is in effect, indexes into that batch instead.
   * Since a batch is really a tree of tuples, in which some branches of
   * the tree are arrays, the root indexes here feeds into array indexes
   * within the writer structure that points to the current position within
   * an array column.
   */
  private final WriterIndexImpl writerIndex;

  /**
   * The row-level writer for stepping through rows as they are written,
   * and for accessing top-level columns.
   */
  private final RowSetLoaderImpl rootWriter;

  /**
   * Tracks the state of the row set loader. Handling vector overflow requires
   * careful stepping through a variety of states as the write proceeds.
   */
  private State state = State.START;

  /**
   * Track the current schema as seen by the writer. Each addition of a column
   * anywhere in the schema causes the active schema version to increase by one.
   * This allows very easy checks for schema changes: save the prior version number
   * and compare it against the current version number.
   */
  private int activeSchemaVersion;

  /**
   * Track the current schema as seen by the consumer of the batches that this
   * loader produces. The harvest schema version can be behind the active schema
   * version in the case in which new columns are added to the overflow row.
   * Since the overflow row won't be visible to the harvested batch, that batch
   * sees the schema as it existed at a prior version: the harvest schema
   * version.
   */
  private int harvestSchemaVersion;

  /**
   * Counts the batches harvested (sent downstream) from this loader. Does
   * not include the current, in-flight batch.
   */
  private int harvestBatchCount;

  /**
   * Counts the rows included in previously-harvested batches. Does not
   * include the number of rows in the current batch.
   */
  private long previousRowCount;

  /**
   * Number of rows in the harvest batch. If an overflow batch is in effect,
   * then this is the number of rows in the "main" batch before the overflow;
   * that is the number of rows in the batch that will be harvested. If no
   * overflow row is in effect, then this number is undefined (and should be
   * zero.)
   */
  private int pendingRowCount;

  /**
   * The number of rows per batch. Starts with the configured amount. Can be
   * adjusted between batches, perhaps based on the actual observed size of
   * input data.
   */
  private int targetRowCount;

  /**
   * The number of rows allowed for the current batch. Is the lesser of the
   * maximum batch size, the target row count, or the remaining margin on
   * the scan limit.
   */
  private int batchSizeLimit;

  /**
   * Total bytes allocated to the current batch.
   */
  protected int accumulatedBatchSize;

  public ResultSetLoaderImpl(BufferAllocator allocator, ResultSetOptions options) {
    this.allocator = allocator;
    this.options = options;
    targetRowCount = options.rowCountLimit;
    writerIndex = new WriterIndexImpl(this);
    columnBuilder = new ColumnBuilder();

    // Determine the root vector cache

    ResultVectorCache vectorCache;
    if (options.vectorCache == null) {
      vectorCache = new NullResultVectorCacheImpl(allocator);
    } else {
      vectorCache = options.vectorCache;
    }

    // Build the row set model depending on whether a schema is provided.

    rootState = new RowState(this, vectorCache);
    rootWriter = rootState.rootWriter();

    // If no schema, columns will be added incrementally as they
    // are discovered. Start with an empty model.

    if (options.schema != null) {

      // Schema provided. Populate a model (and create vectors) for the
      // provided schema. The schema can be extended later, but normally
      // won't be if known up front.

      logger.debug("Schema: " + options.schema.toString());
      BuildFromSchema.instance().buildTuple(rootWriter, options.schema);
    }

    // If we want to project nothing, then we do, in fact, have a
    // valid schema, it just happens to be an empty schema. Bump the
    // schema version so we know we have that empty schema.
    //
    // This accomplishes a result similar to the "legacy" readers
    // achieve by adding a dummy column.
    if (projectionSet().isEmpty()) {
      bumpVersion();
    }
  }

  public ProjectionFilter projectionSet() {
    return options.projectionSet;
  }

  private void updateCardinality() {
    rootState.updateCardinality();
  }

  public ResultSetLoaderImpl(BufferAllocator allocator) {
    this(allocator, new ResultSetOptions());
  }

  @Override
  public BufferAllocator allocator() { return allocator; }

  @Override
  public int bumpVersion() {

    // Update the active schema version. We cannot update the published
    // schema version at this point because a column later in this same
    // row might cause overflow, and any new columns in this row will
    // be hidden until a later batch. But, if we are between batches,
    // then it is fine to add the column to the schema.

    activeSchemaVersion++;
    switch (state) {
      case HARVESTED:
      case START:
      case LOOK_AHEAD:
        harvestSchemaVersion = activeSchemaVersion;
        break;
      default:
    }
    return activeSchemaVersion;
  }

  @Override
  public int activeSchemaVersion( ) { return activeSchemaVersion; }

  @Override
  public int schemaVersion() {
    switch (state) {
      case ACTIVE:
      case IN_OVERFLOW:
      case OVERFLOW:
      case FULL_BATCH:

        // Write in progress: use current writer schema
        return activeSchemaVersion;
      case HARVESTED:
      case LOOK_AHEAD:
      case START:
      case LIMITED:

        // Batch is published. Use harvest schema.
        return harvestSchemaVersion;
      default:

        // Not really in a position to give a schema version.
        throw new IllegalStateException("Unexpected state: " + state);
    }
  }

  @Override
  public boolean startBatch() {
    return startBatch(false);
  }

  /**
   * Start a batch to report only schema without data.
   */
  public boolean startEmptyBatch() {
    return startBatch(true);
  }

  public boolean startBatch(boolean schemaOnly) {
    switch (state) {
      case HARVESTED:
      case START:
        logger.trace("Start batch");
        accumulatedBatchSize = 0;
        updateCardinality();
        rootState.startBatch(schemaOnly);
        checkInitialAllocation();

        // The previous batch ended without overflow, so start
        // a new batch, and reset the write index to 0.
        writerIndex.reset();
        rootWriter.startWrite();
        break;

      case LOOK_AHEAD:

        // A row overflowed so keep the writer index at its current value
        // as it points to the second row in the overflow batch. However,
        // the last write position of each writer must be restored on
        // a column-by-column basis, which is done by the visitor.
        logger.trace("Start batch after overflow");
        rootState.startBatch(schemaOnly);

        // Note: no need to do anything with the writers; they were left
        // pointing to the correct positions in the look-ahead batch.
        // The above simply puts the look-ahead vectors back "under"
        // the writers.
        break;

      case LIMITED:
        return false;

      default:
        throw new IllegalStateException("Unexpected state: " + state);
    }

    // Update the visible schema with any pending overflow batch
    // updates
    harvestSchemaVersion = activeSchemaVersion;
    pendingRowCount = 0;
    batchSizeLimit = (int) Math.min(targetRowCount, options.scanLimit - totalRowCount());
    state = State.ACTIVE;
    return true;
  }

  @Override
  public boolean hasRows() {
    switch (state) {
      case ACTIVE:
      case HARVESTED:
      case FULL_BATCH:
        return rootWriter.rowCount() > 0;
      case LOOK_AHEAD:
        return true;
      default:
        return false;
    }
  }

  @Override
  public RowSetLoader writer() {
    if (state == State.CLOSED) {
      throw new IllegalStateException("Unexpected state: " + state);
    }
    return rootWriter;
  }

  @Override
  public ResultSetLoader setRow(Object... values) {
    startRow();
    writer().setObject(values);
    saveRow();
    return this;
  }

  /**
   * Called before writing a new row. Implementation of
   * {@link RowSetLoader#start()}.
   */
  protected void startRow() {
    switch (state) {
      case ACTIVE:

        // Update the visible schema with any pending overflow batch
        // updates.
        harvestSchemaVersion = activeSchemaVersion;
        rootWriter.startRow();
        break;
      case LIMITED:
        throw new IllegalStateException("Attempt to write past the scan limit.");
      default:
        throw new IllegalStateException("Unexpected state: " + state);
    }
  }

  /**
   * Finalize the current row. Implementation of
   * {@link RowSetLoader#save()}.
   */
  protected void saveRow() {
    switch (state) {
      case ACTIVE:
        rootWriter.endArrayValue();
        rootWriter.saveRow();
        if (!writerIndex.next()) {
          state = State.FULL_BATCH;
        }

        // No overflow row. Advertise the schema version to the client.
        harvestSchemaVersion = activeSchemaVersion;
        break;

      case OVERFLOW:

        // End the value of the look-ahead row in the look-ahead vectors.
        rootWriter.endArrayValue();
        rootWriter.saveRow();

        // Advance the writer index relative to the look-ahead batch.
        writerIndex.next();

        // Stay in the overflow state. Doing so will cause the writer
        // to report that it is full.
        //
        // Also, do not change the harvest schema version. We will
        // expose to the downstream operators the schema in effect
        // at the start of the row. Columns added within the row won't
        // appear until the next batch.
        break;

      default:
        throw new IllegalStateException("Unexpected state: " + state);
    }
  }

  /**
   * Implementation of {@link RowSetLoader#isFull()}
   * @return true if the batch is full (reached vector capacity or the
   * row count limit), false if more rows can be added
   */
  protected boolean isFull() {
    switch (state) {
      case ACTIVE:
        return !writerIndex.valid();
      case OVERFLOW:
      case FULL_BATCH:
        return true;
      case LIMITED:
        return true;
      default:
        return false;
    }
  }

  @Override
  public boolean writeable() {
    return (state == State.ACTIVE || state == State.OVERFLOW) &&
           !atLimit();
  }

  private boolean isBatchActive() {
    return state == State.ACTIVE || state == State.OVERFLOW ||
           state == State.FULL_BATCH;
  }

  /**
   * Implementation for {#link {@link RowSetLoader#rowCount()}.
   *
   * @return the number of rows to be sent downstream for this
   * batch. Does not include the overflow row.
   */
  protected int rowCount() {
    switch (state) {
      case ACTIVE:
      case FULL_BATCH:
        return writerIndex.size();
      case OVERFLOW:
        return pendingRowCount;
      default:
        return 0;
    }
  }

  protected WriterIndexImpl writerIndex() { return writerIndex; }

  @Override
  public void setTargetRowCount(int rowCount) {
    targetRowCount = Math.min(Math.max(1, rowCount), ValueVector.MAX_ROW_COUNT);
  }

  @Override
  public int targetRowCount() { return targetRowCount; }

  @Override
  public int targetVectorSize() { return options.vectorSizeLimit; }

  @Override
  public int maxBatchSize() { return batchSizeLimit; }

  @Override
  public int skipRows(int requestedCount) {

    // Can only skip rows when a batch is active.
    if (state != State.ACTIVE) {
      throw new IllegalStateException("No batch is active.");
    }

    // Skip as many rows as the vector limit allows.
    return writerIndex.skipRows(requestedCount);
  }

  @Override
  public boolean isProjectionEmpty() {
    return !rootState.hasProjections();
  }

  @Override
  public void overflowed() {
    logger.trace("Vector overflow");

    // If we see overflow when we are already handling overflow, it means
    // that a single value is too large to fit into an entire vector.
    // Fail the query.
    //
    // Note that this is a judgment call. It is possible to allow the
    // vector to double beyond the limit, but that will require a bit
    // of thought to get right -- and, of course, completely defeats
    // the purpose of limiting vector size to avoid memory fragmentation...
    //
    // Individual columns handle the case in which overflow occurs on the
    // first row of the main batch. This check handles the pathological case
    // in which we successfully overflowed, but then another column
    // overflowed during the overflow row -- that indicates that that one
    // column can't fit in an empty vector. That is, this check is for a
    // second-order overflow.

    if (state == State.OVERFLOW) {
      throw UserException
          .memoryError("A single column value is larger than the maximum allowed size of 16 MB")
          .build(logger);
    }
    if (state != State.ACTIVE) {
      throw new IllegalStateException("Unexpected state: " + state);
    }
    state = State.IN_OVERFLOW;

    // Preserve the number of rows in the now-complete batch.
    pendingRowCount = writerIndex.vectorIndex();

    // Roll-over will allocate new vectors. Update with the latest
    // array cardinality.
    updateCardinality();

    // Wrap up the completed rows into a batch. Sets
    // vector value counts. The rollover data still exists so
    // it can be moved, but it is now past the recorded
    // end of the vectors (though, obviously, not past the
    // physical end.)
    rootWriter.preRollover();

    // Roll over vector values.
    accumulatedBatchSize = 0;
    rootState.rollover();

    // Adjust writer state to match the new vector values. This is
    // surprisingly easy if we note that the current row is shifted to
    // the 0 position in the new vector, so we just shift all offsets
    // downward by the current row position at each repeat level.
    rootWriter.postRollover();

    // The writer index is reset back to 0. Because of the above roll-over
    // processing, some vectors may now already have values in the 0 slot.
    // However, the vector that triggered overflow has not yet written to
    // the current record, and so will now write to position 0. After the
    // completion of the row, all 0-position values should be written (or
    // at least those provided by the client.)
    //
    // For arrays, the writer might have written a set of values
    // (v1, v2, v3), and v4 might have triggered the overflow. In this case,
    // the array values have been moved, offset vectors adjusted, the
    // element writer adjusted, so that v4 will be written to index 3
    // to produce (v1, v2, v3, v4, v5, ...) in the look-ahead vector.
    writerIndex.rollover();
    checkInitialAllocation();

    // Remember that overflow is in effect.
    state = State.OVERFLOW;
  }

  @Override
  public boolean hasOverflow() { return state == State.OVERFLOW; }

  @Override
  public VectorContainer outputContainer() {
    return rootState.outputContainer();
  }

  @Override
  public VectorContainer harvest() {
    int rowCount;
    switch (state) {
      case ACTIVE:
      case FULL_BATCH:
        rowCount = harvestNormalBatch();
        logger.trace("Harvesting {} rows", rowCount);
        break;
      case OVERFLOW:
        rowCount = harvestOverflowBatch();
        logger.trace("Harvesting {} rows after overflow", rowCount);
        break;
      default:
        throw new IllegalStateException("Unexpected state: " + state);
    }

    rootState.updateOutput(harvestSchemaVersion);
    VectorContainer container = rootState.outputContainer();
    container.setRecordCount(rowCount);

    // Finalize: update counts, set state.

    harvestBatchCount++;
    previousRowCount += rowCount;

    if (atLimit()) {
      state = State.LIMITED;
    }
    return container;
  }

  private int harvestNormalBatch() {

    // Wrap up the vectors: final fill-in, set value count, etc.

    rootWriter.endBatch();
    harvestSchemaVersion = activeSchemaVersion;
    state = State.HARVESTED;
    return writerIndex.size();
  }

  private int harvestOverflowBatch() {
    rootState.harvestWithLookAhead();
    state = State.LOOK_AHEAD;
    return pendingRowCount;
  }

  @Override
  public boolean atLimit() {
    switch (state) {
    case LIMITED:
      return true;
    case ACTIVE:
    case HARVESTED:
      return totalRowCount() >= options.scanLimit;
    default:
      return false;
    }
  }

  @Override
  public TupleMetadata outputSchema() {
    return rootState.outputSchema();
  }

  @Override
  public TupleMetadata activeSchema() {
    return rootState.schema();
  }

  @Override
  public void close() {
    if (state == State.CLOSED) {
      return;
    }
    rootState.close();

    // Do not close the vector cache; the caller owns that and
    // will, presumably, reuse those vectors for another writer.
    state = State.CLOSED;
  }

  @Override
  public int batchCount() {
    return harvestBatchCount + (rowCount() == 0 ? 0 : 1);
  }

  @Override
  public long totalRowCount() {
    long total = previousRowCount;
    if (isBatchActive()) {
      total += pendingRowCount + writerIndex.size();
    }
    return total;
  }

  public RowState rootState() { return rootState; }

  @Override
  public boolean canExpand(int delta) {
    accumulatedBatchSize += delta;
    return state == State.IN_OVERFLOW ||
           options.maxBatchSize <= 0 ||
           accumulatedBatchSize <= options.maxBatchSize;
  }

  @Override
  public void tallyAllocations(int allocationBytes) {
    accumulatedBatchSize += allocationBytes;
  }

  /**
   * Log and check the initial vector allocation. If a batch size
   * limit is set, warn if the initial allocation exceeds the limit.
   * This will occur if the target row count is incorrect for the
   * data size.
   */
  private void checkInitialAllocation() {
    if (options.maxBatchSize < 0) {
      logger.debug("Initial vector allocation: {}, no batch limit specified",
          accumulatedBatchSize);
    }
    else if (accumulatedBatchSize > options.maxBatchSize) {
      logger.warn("Initial vector allocation: {}, but batch size limit is: {}",
          accumulatedBatchSize, options.maxBatchSize);
    } else {
      logger.debug("Initial vector allocation: {}, batch size limit: {}",
          accumulatedBatchSize, options.maxBatchSize);
    }
  }

  public void dump(HierarchicalFormatter format) {
    format
      .startObject(this)
      .attribute("options");
    options.dump(format);
    format
      .attribute("index", writerIndex.vectorIndex())
      .attribute("state", state)
      .attribute("activeSchemaVersion", activeSchemaVersion)
      .attribute("harvestSchemaVersion", harvestSchemaVersion)
      .attribute("pendingRowCount", pendingRowCount)
      .attribute("targetRowCount", targetRowCount);
    format.attribute("root");
    rootState.dump(format);
    format.attribute("rootWriter");
    rootWriter.dump(format);
    format.endObject();
  }

  @Override
  public ResultVectorCache vectorCache() {
    return rootState.vectorCache();
  }

  @Override
  public int rowIndex() {
    return writerIndex().vectorIndex();
  }

  @Override
  public ColumnBuilder columnBuilder() { return columnBuilder; }

  @Override
  public CustomErrorContext errorContext() { return options.errorContext; }
}
