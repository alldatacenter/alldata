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

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionType;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.PageDataInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.FieldOverflowStateContainer;

/** Provides bulk reads when accessing Parquet's page payload for variable length columns */
final class VarLenBulkPageReader {

  /**
   * Using small buffers so that they could fit in the L1 cache
   * NOTE - This buffer size is used in several places of the bulk processing implementation; please analyze
   *        the impact of changing this buffer size.
   */
  static final int BUFF_SZ = 1 << 12; // 4k
  static final int PADDING = 1 << 6; // 128bytes padding to allow for access optimizations

  /** byte buffer used for buffering page data */
  private final ByteBuffer buffer = ByteBuffer.allocate(BUFF_SZ + PADDING);
  /** Page Data Information */
  private final PageDataInfo pageInfo = new PageDataInfo();
  /** expected precision type: fixed or variable length */
  private final ColumnPrecisionInfo columnPrecInfo;
  /** Bulk entry */
  private final VarLenColumnBulkEntry entry;
  /** A callback to allow bulk readers interact with their container */
  private final VarLenColumnBulkInputCallback containerCallback;
  /** A reference to column's overflow data (could be null) */
  private FieldOverflowStateContainer fieldOverflowStateContainer;

  // Various BulkEntry readers
  final VarLenAbstractEntryReader fixedReader;
  final VarLenAbstractEntryReader nullableFixedReader;
  final VarLenAbstractEntryReader variableLengthReader;
  final VarLenAbstractEntryReader nullableVLReader;
  final VarLenAbstractEntryReader dictionaryReader;
  final VarLenAbstractEntryReader nullableDictionaryReader;

  // Overflow reader
  private VarLenOverflowReader overflowReader;

  VarLenBulkPageReader(
    PageDataInfo pageInfoInput,
    ColumnPrecisionInfo columnPrecInfoInput,
    VarLenColumnBulkInputCallback containerCallbackInput,
    FieldOverflowStateContainer fieldOverflowStateContainer) {

    // Set the buffer to the native byte order
    this.buffer.order(ByteOrder.nativeOrder());

    if (pageInfoInput != null) {
      set(pageInfoInput, false);
    }

    this.columnPrecInfo = columnPrecInfoInput;
    this.entry = new VarLenColumnBulkEntry(this.columnPrecInfo);
    this.containerCallback = containerCallbackInput;
    this.fieldOverflowStateContainer = fieldOverflowStateContainer;

    // Initialize the Variable Length Entry Readers
    fixedReader = new VarLenFixedEntryReader(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
    nullableFixedReader = new VarLenNullableFixedEntryReader(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
    variableLengthReader = new VarLenEntryReader(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
    nullableVLReader = new VarLenNullableEntryReader(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
    dictionaryReader = new VarLenEntryDictionaryReader(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
    nullableDictionaryReader = new VarLenNullableDictionaryReader(buffer, pageInfo, columnPrecInfo, entry, containerCallback);

    // Overflow reader is initialized only when a previous batch produced overflow data for this column
    if (this.fieldOverflowStateContainer == null) {
      overflowReader = null;
    } else {
      overflowReader = new VarLenOverflowReader(buffer, entry, containerCallback, fieldOverflowStateContainer);
    }
  }

  final void set(PageDataInfo pageInfoInput, boolean clear) {
    pageInfo.pageData = pageInfoInput.pageData;
    pageInfo.pageDataOff = pageInfoInput.pageDataOff;
    pageInfo.pageDataLen = pageInfoInput.pageDataLen;
    pageInfo.numPageFieldsRead = pageInfoInput.numPageFieldsRead;
    pageInfo.definitionLevels = pageInfoInput.definitionLevels;
    pageInfo.encodedValueReader = pageInfoInput.encodedValueReader;
    pageInfo.numPageValues = pageInfoInput.numPageValues;
    if (clear) {
      buffer.clear();
    }
  }

  final VarLenColumnBulkEntry getEntry(int valuesToRead) {
    Preconditions.checkArgument(valuesToRead > 0, "Number of values to read [%s] should be greater than zero", valuesToRead);

    VarLenColumnBulkEntry entry = null;

    // If there is overflow data, then we need to consume it first
    if (overflowDataAvailable()) {
      entry = overflowReader.getEntry(valuesToRead);
      entry.setReadFromPage(false); // entry was read from the overflow data

      return entry;
    }

    // It seems there is no overflow data anymore; if we previously were reading from it, then it
    // needs to get de-initialized before reading new page data.
    deinitOverflowDataIfNeeded();

    if (ColumnPrecisionType.isPrecTypeFixed(columnPrecInfo.columnPrecisionType)) {
      if ((entry = getFixedEntry(valuesToRead)) == null) {
        // The only reason for a null to be returned is when the "getFixedEntry" method discovers
        // the column is not fixed length; this false positive happens if the sample data was not
        // representative of all the column values.

        // If this is an optional column, then we need to reset the definition-level reader
        if (pageInfo.definitionLevels.hasDefinitionLevels()) {
          try {
            containerCallback.resetDefinitionLevelReader(pageInfo.numPageFieldsRead);
            // Update the definition level object reference
            pageInfo.definitionLevels.set(containerCallback.getDefinitionLevelsReader(),
              pageInfo.numPageValues - pageInfo.numPageFieldsRead);

          } catch (IOException ie) {
            throw new DrillRuntimeException(ie);
          }
        }

        columnPrecInfo.columnPrecisionType = ColumnPrecisionType.DT_PRECISION_IS_VARIABLE;
        entry = getVarLenEntry(valuesToRead);
      }

    } else {
      entry = getVarLenEntry(valuesToRead);
    }

    if (entry != null) {
      entry.setReadFromPage(true); // entry was read from a Parquet page
      pageInfo.numPageFieldsRead += entry.getNumValues();
    }
    return entry;
  }

  private final VarLenColumnBulkEntry getFixedEntry(int valuesToRead) {
    if (pageInfo.definitionLevels.hasDefinitionLevels()) {
      return nullableFixedReader.getEntry(valuesToRead);
    } else {
      return fixedReader.getEntry(valuesToRead);
    }
  }

  private final VarLenColumnBulkEntry getVarLenEntry(int valuesToRead) {
    // Let start with non-dictionary encoding as it is predominant
    if (!pageInfo.encodedValueReader.isDefined()) {
      if (pageInfo.definitionLevels.hasDefinitionLevels()) {
        return nullableVLReader.getEntry(valuesToRead);
      } else {
        return variableLengthReader.getEntry(valuesToRead);
      }
    } else {
      if (pageInfo.definitionLevels.hasDefinitionLevels()) {
        return nullableDictionaryReader.getEntry(valuesToRead);
      } else {
        return dictionaryReader.getEntry(valuesToRead);
      }
    }
  }

  private boolean overflowDataAvailable() {
    if (overflowReader == null) {
      return false;
    }
    return overflowReader.getRemainingOverflowData() > 0;
  }

  private void deinitOverflowDataIfNeeded() {
    if (overflowReader != null) {
      containerCallback.deinitOverflowData();
      overflowReader = null;
      fieldOverflowStateContainer = null;
    }
  }

}
