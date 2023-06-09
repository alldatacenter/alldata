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
package org.apache.drill.exec.store.parquet.columnreaders;

import io.netty.buffer.DrillBuf;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenOverflowReader.FieldOverflowStateImpl;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.BatchSizingMemoryUtil;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.BatchSizingMemoryUtil.ColumnMemoryUsageInfo;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.ColumnMemoryQuota;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.FieldOverflowStateContainer;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarLenBulkEntry;
import org.apache.drill.exec.vector.VarLenBulkInput;
import org.apache.parquet.column.values.ValuesReader;
import org.apache.parquet.io.api.Binary;

/** Implements the {@link VarLenBulkInput} interface to optimize data copy */
public final class VarLenColumnBulkInput<V extends ValueVector> implements VarLenBulkInput<VarLenBulkEntry> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VarLenColumnBulkInput.class);

  /** A cut off number for bulk processing */
  private static final int BULK_PROCESSING_MAX_PREC_LEN = 1 << 10;

  /** parent object */
  private final VarLengthValuesColumn<V> parentInst;
  /** Batch sizer manager */
  private final RecordBatchSizerManager batchSizerMgr;
  /** Column precision type information (owner by caller) */
  private final ColumnPrecisionInfo columnPrecInfo;
  /** Custom definition level reader */
  private final DefLevelReaderWrapper custDefLevelReader;
  /** Custom encoded values reader */
  private final ValuesReaderWrapper custValuesReader;

  /** The records to read */
  private final int recordsToRead;
  /** Maximum Memory (in bytes) to use for loading this field's data
   * (soft limit as a single row can go beyond this value)
   */
  private ColumnMemoryQuota columnMemoryQuota;
  /** Current operation bulk reader state */
  private final OprBulkReadState oprReadState;
  /** Container class for holding page data information */
  private final PageDataInfo pageInfo = new PageDataInfo();
  /** Buffered page payload */
  private VarLenBulkPageReader buffPagePayload;
  /** A callback to allow child readers interact with this class */
  private final VarLenColumnBulkInputCallback callback;
  /** Column memory usage information */
  private final ColumnMemoryUsageInfo columnMemoryUsage = new ColumnMemoryUsageInfo();
  /** A reference to column's overflow data (could be null) */
  private FieldOverflowStateContainer fieldOverflowStateContainer;

  /**
   * CTOR.
   * @param parentInst parent object instance
   * @param recordsToRead number of records to read
   * @param bulkReaderState bulk reader state
   * @throws IOException runtime exception in case of processing error
   */
  VarLenColumnBulkInput(VarLengthValuesColumn<V> parentInst,
    int recordsToRead, BulkReaderState bulkReaderState) throws IOException {

    this.parentInst = parentInst;
    this.batchSizerMgr = this.parentInst.parentReader.getBatchSizesMgr();
    this.recordsToRead = recordsToRead;
    this.callback = new VarLenColumnBulkInputCallback(this);
    this.columnPrecInfo = bulkReaderState.columnPrecInfo;
    this.custDefLevelReader = bulkReaderState.definitionLevelReader;
    this.custValuesReader = bulkReaderState.encodedValuesReader;
    this.fieldOverflowStateContainer = this.batchSizerMgr.getFieldOverflowContainer(parentInst.valueVec.getField().getName());

    // Load page if none have been read
    loadPageIfNeed();

    // Create the internal READ_STATE object based on the current page-reader state
    this.oprReadState = new OprBulkReadState(parentInst.pageReader.readyToReadPosInBytes, parentInst.pageReader.valuesRead);

    // Let's try to figure out whether this columns is fixed or variable length; this information
    // is not always accurate within the Parquet schema metadata.
    if (ColumnPrecisionType.isPrecTypeUnknown(columnPrecInfo.columnPrecisionType)) {
      guessColumnPrecision(columnPrecInfo);
    }

    // Initialize the buffered-page-payload object
    setBufferedPagePayload();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasNext() {
    try {
      if (!batchConstraintsReached()) {
        // If there is overflow data, then proceed; otherwise, make sure there is still Parquet data to be
        // read.
        if (!overflowDataAvailable()) {
          // We need to ensure there is a page of data to be read
          if (!parentInst.pageReader.hasPage() || parentInst.pageReader.pageValueCount == oprReadState.numPageFieldsProcessed) {
            long totalValueCount = parentInst.columnChunkMetaData.getValueCount();

            if (totalValueCount == (parentInst.totalValuesRead + oprReadState.batchNumValuesReadFromPages)
                || !parentInst.pageReader.next()) {

              parentInst.hitRowGroupEnd();
              return false;
            }

            // Reset the state object page read metadata
            oprReadState.numPageFieldsProcessed = 0;
            oprReadState.pageReadPos = parentInst.pageReader.readyToReadPosInBytes;

            // Update the value readers information
            setValuesReadersOnNewPage();

            // Update the buffered-page-payload since we've read a new page
            setBufferedPagePayload();
          }
        }
        // Alright, we didn't hit a batch constraint and are able to read either from the overflow data or
        // the Parquet data.
        return true;
      } else {
        return false;
      }
    } catch (IOException ie) {
      throw new DrillRuntimeException(ie);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final VarLenBulkEntry next() {
    final int remaining = getRemainingRecords();
    final VarLenColumnBulkEntry result = buffPagePayload.getEntry(remaining);

    // Update position for next read
    if (result != null && result.getNumValues() > 0) {
      // We need to update page stats only when we are reading directly from Parquet pages; there are
      // situations where we have to return the overflow data (read in a previous batch)
      if (result.isReadFromPage()) {
        // Page read position is meaningful only when dictionary mode is off
        if (!pageInfo.encodedValueReader.isDefined()) {
          oprReadState.pageReadPos += (result.getTotalLength() + 4 * result.getNumNonNullValues());
        }
        oprReadState.numPageFieldsProcessed += result.getNumValues();
        oprReadState.batchNumValuesReadFromPages += result.getNumValues();
      }
      // Update the batch field index
      oprReadState.batchFieldIndex += result.getNumValues();
    }
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public final void remove() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public final int getStartIndex() {
    return oprReadState.batchFieldIndex;
  }

  /** {@inheritDoc} */
  @Override
  public final void done() {
    // Update the page reader state so that a future call to this method resumes
    // where we left off.

    // Page read position is meaningful only when dictionary mode is off
    if (pageInfo.encodedValueReader == null
        || !pageInfo.encodedValueReader.isDefined()) {
      parentInst.pageReader.readyToReadPosInBytes = oprReadState.pageReadPos;
    }
    parentInst.pageReader.valuesRead = oprReadState.numPageFieldsProcessed;
    parentInst.totalValuesRead += oprReadState.batchNumValuesReadFromPages;

    if (logger.isDebugEnabled()) {
      String message = String.format("requested=%d, returned=%d, total-returned=%d",
        recordsToRead,
        oprReadState.batchFieldIndex,
        parentInst.totalValuesRead);

      logger.debug(message);
    }
  }

  /**
   * @return minimum memory size required to process a variable column in a columnar manner
   */
  public static int getMinVLColumnMemorySize() {
    // How did we come up with this number?
    // Let's first lay down some facts
    // a) the allocator-rounding-to-next-power-of-two has to be accounted for
    // b) VL columns use up to three vectors "bits", "offsets", and "values"
    // c) The maximum number of entries per chunk is chunk-size / 4
    // d) "data" and "length" sizes within a chunk have a reverse relationship (if one grows, then the other shrinks)
    // e) "data" and "length" within a chunk cannot exceed 1 chunk-size each when loaded into the ValueVector
    // This information gives the following upper bound
    // - max-chunk-vv-footprint < (chunk-size/4 + 1chunk-size + 1/2 chunk-size) < 2  chunk-sizes
    // Why?
    // - max-bits footprint is controlled by c)
    // - "data" and "offsets" can have a max footprint of 1 chunk size when they are over chunk-size/2 since
    //   roundup happens; if one goes beyond chunk-size/2, then the other is less than that (inverse relationship)
    //   which leads to a maximum memory footprint of 1 chunk-size + 1/2 chunk-size
    return VarLenBulkPageReader.BUFF_SZ * 2;
  }

  final int getReadBatchFields() {
    return oprReadState.batchFieldIndex;
  }

  private final void setValuesReadersOnNewPage() {
    PageReader pageReader = parentInst.pageReader;
    if (pageReader.pageValueCount > 0) {
      custDefLevelReader.set(pageReader.definitionLevels, pageReader.pageValueCount);
      if (parentInst.recordsRequireDecoding()) {
        custValuesReader.set(parentInst.usingDictionary
          ? pageReader.getDictionaryValueReader()
          : pageReader.getValueReader()
        );
      } else {
        custValuesReader.set(null);
      }
    } else {
      custDefLevelReader.set(null, 0);
      custValuesReader.set(null);
    }
  }

  private final void setBufferedPagePayload() {

    if (parentInst.pageReader.hasPage() && oprReadState.numPageFieldsProcessed < parentInst.pageReader.pageValueCount) {
      if (!parentInst.usingDictionary) {
        pageInfo.pageData  = parentInst.pageReader.pageData;
        pageInfo.pageDataOff = (int) oprReadState.pageReadPos;
        pageInfo.pageDataLen = (int) parentInst.pageReader.byteLength;
      }

      pageInfo.numPageValues = parentInst.pageReader.pageValueCount;
      pageInfo.definitionLevels = custDefLevelReader;
      pageInfo.encodedValueReader = custValuesReader;
      pageInfo.numPageFieldsRead = oprReadState.numPageFieldsProcessed;

      if (buffPagePayload == null) {
        buffPagePayload = new VarLenBulkPageReader(pageInfo, columnPrecInfo, callback, fieldOverflowStateContainer);

      } else {
        buffPagePayload.set(pageInfo, true);
      }
    } else {
      if (buffPagePayload == null) {
        buffPagePayload = new VarLenBulkPageReader(null, columnPrecInfo, callback, fieldOverflowStateContainer);
      }
    }
  }

  final ColumnPrecisionInfo getColumnPrecisionInfo() {
    return columnPrecInfo;
  }

  /** Reads a data sample to evaluate this column's precision (variable or fixed); this is best effort, caller
   * should be ready to handle false positives.
   *
   * @param columnPrecInfo input/output precision info container
   * @throws IOException
   */
  private final void guessColumnPrecision(ColumnPrecisionInfo columnPrecInfo) throws IOException {
    columnPrecInfo.columnPrecisionType = ColumnPrecisionType.DT_PRECISION_IS_VARIABLE;

    loadPageIfNeed();

    // Minimum number of values within a data size to consider bulk processing
    final int minNumVals = VarLenBulkPageReader.BUFF_SZ / BULK_PROCESSING_MAX_PREC_LEN;
    final int maxDataToProcess =
      Math.min(VarLenBulkPageReader.BUFF_SZ + 4 * minNumVals,
        (int) (parentInst.pageReader.byteLength-parentInst.pageReader.readyToReadPosInBytes));

    if (parentInst.recordsRequireDecoding() || maxDataToProcess == 0) {
      // The number of values is small, there are lot of null values, or dictionary encoding is used. Bulk
      // processing should work fine for these use-cases
      columnPrecInfo.bulkProcess = true;
      return;
    }

    ByteBuffer buffer = ByteBuffer.allocate(maxDataToProcess);
    buffer.order(ByteOrder.nativeOrder());

    parentInst.pageReader.pageData.getBytes((int) parentInst.pageReader.readyToReadPosInBytes, buffer.array(), 0, maxDataToProcess);
    buffer.limit(maxDataToProcess);

    int numValues = 0;
    int fixedDataLen = -1;
    boolean isFixedPrecision = false;

    do {
      if (buffer.remaining() < 4) {
        break;
      }

      int data_len = buffer.getInt();

      if (fixedDataLen < 0) {
        fixedDataLen = data_len;
        isFixedPrecision = true;
      }

      if (isFixedPrecision && fixedDataLen != data_len) {
        isFixedPrecision = false;
      }

      if (buffer.remaining() < data_len) {
        break;
      }
      buffer.position(buffer.position() + data_len);

      ++numValues;

    } while (true);

    // We need to have encountered at least a couple of values with the same length; if the values
    // have long length, then fixed vs VL is not a big deal with regard to performance.
    if (isFixedPrecision && fixedDataLen >= 0) {
      columnPrecInfo.columnPrecisionType = ColumnPrecisionType.DT_PRECISION_IS_FIXED;
      columnPrecInfo.precision = fixedDataLen;

      if (fixedDataLen <= BULK_PROCESSING_MAX_PREC_LEN) {
        columnPrecInfo.bulkProcess = true;

      } else {
        columnPrecInfo.columnPrecisionType = ColumnPrecisionType.DT_PRECISION_IS_VARIABLE;
        columnPrecInfo.bulkProcess = false;

      }
    } else {
      // At this point we know this column is variable length; we need to figure out whether it is worth
      // processing it in a bulk-manner or not.

      if (numValues >= minNumVals) {
        columnPrecInfo.bulkProcess = true;
      } else {
        columnPrecInfo.bulkProcess = false;
      }
    }
  }

  private void loadPageIfNeed() throws IOException {
    if (!parentInst.pageReader.hasPage()) {
      // load a page
      parentInst.pageReader.next();
      // update the definition level information
      setValuesReadersOnNewPage();
    }
  }

  private boolean batchConstraintsReached() {
    // Let's update this column's memory quota
    columnMemoryQuota = batchSizerMgr.getCurrentFieldBatchMemory(parentInst.valueVec.getField().getName());
    assert columnMemoryQuota.getMaxMemoryUsage() > 0;

    // Now try to figure out whether the next chunk will take us beyond the memory quota
    final int maxNumRecordsInChunk = VarLenBulkPageReader.BUFF_SZ / BatchSizingMemoryUtil.INT_VALUE_WIDTH;

    if (this.parentInst.valueVec.getField().isNullable()) {
      return batchConstraintsReached(
        maxNumRecordsInChunk * BatchSizingMemoryUtil.BYTE_VALUE_WIDTH, // max "bits"    space within a chunk
        maxNumRecordsInChunk * BatchSizingMemoryUtil.INT_VALUE_WIDTH,  // max "offsets" space within a chunk
        VarLenBulkPageReader.BUFF_SZ                                   // max "data"    space within a chunk
      );

    } else {
      return batchConstraintsReached(
        0,
        maxNumRecordsInChunk * BatchSizingMemoryUtil.INT_VALUE_WIDTH,  // max "offsets" space within a chunk
        VarLenBulkPageReader.BUFF_SZ                                   // max "data"    space within a chunk
      );
    }
  }

  private boolean batchConstraintsReached(int newBitsMemory, int newOffsetsMemory, int newDataMemory) {
    assert oprReadState.batchFieldIndex <= recordsToRead; // cannot read beyond the batch size

    // Did we reach the batch size limit?
    if (oprReadState.batchFieldIndex == recordsToRead) {
      return true; // batch size reached
    }

    // Memory constraint check logic:
    // - if this is the first chunk to process, then let it proceed as we need to at least return
    //   one row; this also means the minimum batch memory shouldn't be lower than 2 chunks (please refer
    //   to getMinVLColumnMemorySize() method for more information).
    //
    // - Otherwise, we make sure that the memory growth after processing a chunk cannot go beyond the maximum
    //   batch memory for this column
    // - There is also a caveat that needs to be handled during processing:
    //   o The page-bulk-reader will stop loading entries if it encounters a large value (doesn't fit within
    //     the chunk)
    //   o There is an exception though, which is if the entry is the first one within the batch (this is to
    //     ensure that we always make progress)
    //   o In this situation a callback to this object is made to assess whether this large entry can be loaded
    //     into the ValueVector.

    // Is this the first chunk to be processed?
    if (oprReadState.batchFieldIndex == 0) {
      return false; // we should process at least one chunk
    }

    // Is the next processed chunk going to cause memory to overflow beyond the allowed limit?
    columnMemoryUsage.vector = parentInst.valueVec;
    columnMemoryUsage.memoryQuota = columnMemoryQuota;
    columnMemoryUsage.currValueCount = oprReadState.batchFieldIndex;

    // Return true if we cannot add this new payload
    return !BatchSizingMemoryUtil.canAddNewData(columnMemoryUsage, newBitsMemory, newOffsetsMemory, newDataMemory);
  }

  private int getRemainingRecords() {
    // remaining records to return within this batch
    final int toReadRemaining       = recordsToRead - oprReadState.batchFieldIndex;
    final int remainingOverflowData = getRemainingOverflowData();
    final int remaining;

    // This method remainder semantic depends on whether we are dealing with page data or
    // overflow data; now that overflow data is behaving like a source of input
    if (remainingOverflowData == 0) {
      final int pageRemaining = parentInst.pageReader.pageValueCount - oprReadState.numPageFieldsProcessed;
      remaining               = Math.min(toReadRemaining, pageRemaining);

    } else {
      remaining = Math.min(toReadRemaining, remainingOverflowData);
    }

    return remaining;
  }

  private boolean overflowDataAvailable() {
    return getRemainingOverflowData() > 0;
  }

  private int getRemainingOverflowData() {

    if (fieldOverflowStateContainer != null) {
      FieldOverflowStateImpl overflowState =
        (FieldOverflowStateImpl) fieldOverflowStateContainer.overflowState;

      if (overflowState != null) {
        return overflowState.getRemainingOverflowData();
      } else {
        // This can happen if this is the first time we are accessing this container as
        // the overflow reader didn't have the chance consume any overflow data yet.
        return fieldOverflowStateContainer.overflowDef.numValues;
      }
    }
    return 0;
  }

  private void deinitOverflowData() {
    batchSizerMgr.releaseFieldOverflowContainer(parentInst.valueVec.getField().getName());

    fieldOverflowStateContainer = null;
  }

  // --------------------------------------------------------------------------
  // Inner Classes
  // --------------------------------------------------------------------------

  /** Enumeration which indicates whether a column's type precision is unknown, variable, or fixed. */
  enum ColumnPrecisionType {
    DT_PRECISION_UNKNOWN,
    DT_PRECISION_IS_FIXED,
    DT_PRECISION_IS_VARIABLE;

    static boolean isPrecTypeUnknown(ColumnPrecisionType type) {
      return DT_PRECISION_UNKNOWN.equals(type);
    }

    static boolean isPrecTypeFixed(ColumnPrecisionType type) {
      return DT_PRECISION_IS_FIXED.equals(type);
    }

    static boolean isPrecTypeVariable(ColumnPrecisionType type) {
      return DT_PRECISION_IS_VARIABLE.equals(type);
    }
  }

  /** this class enables us to cache state across bulk reader operations */
  final static class BulkReaderState {

    /** Column Precision Type: variable or fixed length; used to overcome unreliable meta-data information  */
    final ColumnPrecisionInfo columnPrecInfo = new ColumnPrecisionInfo();
    /**
     * A custom definition level reader which overcomes Parquet's ValueReader limitations (that is,
     * no ability to peek)
     */
    final DefLevelReaderWrapper definitionLevelReader = new DefLevelReaderWrapper();
    /**
     * A custom values reader which overcomes Parquet's ValueReader limitations (that is,
     * no ability to peek)
     */
    final ValuesReaderWrapper encodedValuesReader = new ValuesReaderWrapper();
  }

  /** Container class to hold a column precision information */
  final static class ColumnPrecisionInfo {
    /** column precision type */
    ColumnPrecisionType columnPrecisionType = ColumnPrecisionType.DT_PRECISION_UNKNOWN;
    /** column precision; set only for fixed length precision */
    int precision;
    /** indicator on whether this column should be bulk processed */
    boolean bulkProcess;

    /** Copies source content into this object */
    void clone(final ColumnPrecisionInfo src) {
      columnPrecisionType = src.columnPrecisionType;
      precision = src.precision;
      bulkProcess = src.bulkProcess;
    }

  }

  /** Contains information about current bulk read operation */
  private final static class OprBulkReadState {
    /** reader position within current page */
    long pageReadPos;
    /** number of fields processed within the current page */
    int numPageFieldsProcessed;
    /** field index within current batch */
    int batchFieldIndex;
    /** number of values actually read from Parquet pages (not from overflow data) within this batch */
    int batchNumValuesReadFromPages;

      OprBulkReadState(long pageReadPos, int numPageFieldsRead) {
        this.pageReadPos = pageReadPos;
        this.numPageFieldsProcessed = numPageFieldsRead;
        this.batchFieldIndex = 0;
        this.batchNumValuesReadFromPages = 0;
      }
  }

  /** Container class for holding page data information */
  final static class PageDataInfo {
    /** Number of values within the current page */
    int numPageValues;
    /** Page data buffer */
    DrillBuf pageData;
    /** Offset within the page data */
    int pageDataOff;
    /** Page data length */
    int pageDataLen;
    /** number of fields read within current page */
    int numPageFieldsRead;
    /** Definition Level */
    DefLevelReaderWrapper definitionLevels;
    /** Encoded value reader */
    ValuesReaderWrapper encodedValueReader;
  }

  /** Callback to allow a bulk reader interact with its parent */
  final static class VarLenColumnBulkInputCallback {
    /** Parent instance */
    final VarLenColumnBulkInput<? extends ValueVector> parentInst;
    /** Page reader object */
    PageReader pageReader;

    VarLenColumnBulkInputCallback(VarLenColumnBulkInput<? extends ValueVector> parentInst) {
      this.parentInst = parentInst;
      this.pageReader = this.parentInst.parentInst.pageReader;
    }

    /**
     * Enables Parquet column readers to reset the definition level reader to a specific state.
     * @param skipCount the number of rows to skip (optional)
     *
     * @throws IOException
     */
    void resetDefinitionLevelReader(int skipCount) throws IOException {
      pageReader.resetDefinitionLevelReader(skipCount);
    }

    /**
     * @return current page definition level
     */
    PageReader.IntIterator getDefinitionLevelsReader() {
      return pageReader.definitionLevels;
    }

    /**
     * @param newBitsMemory new "bits" memory size
     * @param newOffsetsMemory new "offsets" memory size
     * @param newDataMemory new "data" memory size
     * @return true if the new payload ("bits", "offsets", "data") will trigger a constraint violation; false
     *         otherwise
     */
    boolean batchMemoryConstraintsReached(int newBitsMemory, int newOffsetsMemory, int newDataMemory) {
      return parentInst.batchConstraintsReached(newBitsMemory, newOffsetsMemory, newDataMemory);
    }

    /** Informs the parent the overflow data cannot be used anymore */
    void deinitOverflowData() {
      parentInst.deinitOverflowData();
    }
  }

  /** A wrapper value reader with the ability to control when to read the next value */
  final static class DefLevelReaderWrapper {
    /** Definition Level */
    private PageReader.IntIterator definitionLevels;
    /** Peeked value     */
    private int currValue;
    /** Remaining values */
    private int remaining;

    /**
     * @return true if the current page has definition levels to be read
     */
    public boolean hasDefinitionLevels() {
      return definitionLevels != null;
    }

    /**
     * Consume the first integer if not done; we want to empower the caller so to avoid extra checks
     * during access methods (e.g., some consumers will not invoke this method as they rather access
     * the raw reader..)
     */
    public void readFirstIntegerIfNeeded() {
      assert definitionLevels != null;
      if (currValue == -1) {
        setNextInteger();
      }
    }

    /**
     * Set the {@link PageReader#definitionLevels} object; if a null value is passed, then it is understood
     * the current page doesn't have definition levels to be processed
     * @param definitionLevels {@link ValuesReader} object
     * @param numValues total number of values that can be read from the stream
     */
    void set(PageReader.IntIterator definitionLevels, int numValues) {
      this.definitionLevels = definitionLevels;
      this.currValue = -1;
      this.remaining = numValues;
    }

    /**
     * @return the current integer from the page; this method has no side-effects (the underlying
     *         {@link ValuesReader} is not affected)
     */
    public int readCurrInteger() {
      assert currValue >= 0;
      return currValue;
    }

    /**
     * @return internally reads the next integer from the underlying {@link ValuesReader}; false if the stream
     *         reached EOF
     */
    public boolean nextIntegerIfNotEOF() {
      return setNextInteger();
    }

    /**
     * @return underlying reader object; this object is now unusable
     *         note that you have to invoke the {@link #set(PageReader.IntIterator, int)} method
     *         to update this object state in case a) you have used the {@link PageReader.IntIterator} object and b)
     *         want to resume using this {@link DefLevelReaderWrapper} object instance
     */
    public PageReader.IntIterator getUnderlyingReader() {
      currValue = -1; // to make this object unusable
      return definitionLevels;
    }

    private boolean setNextInteger() {
      if (remaining > 0) {
        --remaining;
        try {
          currValue = definitionLevels.nextInt();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return true;
      }
      currValue = -1;
      return false;
    }
  }

  /** A wrapper value reader with the ability to control when to read the next value */
  final static class ValuesReaderWrapper {
    /** Encoded values reader */
    private ValuesReader valuesReader;
    /** Pushed back value     */
    private Binary pushedBackValue;

    /**
     * @return true if the current page uses an encoded values reader for the data
     */
    public boolean isDefined() {
      return valuesReader != null;
    }

    /**
     * Set the ValuesReader object; if a null value is passed, then it is understood
     * the current page doesn't use an encoding like dictionary or delta.
     * @param _rawReader {@link ValuesReader} object
     */
    void set(ValuesReader _rawReader) {
      this.valuesReader    = _rawReader;
      this.pushedBackValue = null;
    }

    /**
     * @return the current entry from the page
     */
    public Binary getEntry() {
      Binary entry = null;
      if (pushedBackValue == null) {
        entry = getNextEntry();

      } else {
        entry           = pushedBackValue;
        pushedBackValue = null;
      }
      return entry;
    }

    public void pushBack(Binary entry) {
      pushedBackValue = entry;
    }

    private Binary getNextEntry() {
      try {
        return valuesReader.readBytes();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


}
