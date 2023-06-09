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

import io.netty.buffer.DrillBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchStatsContext;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;

/**
 * Logic for handling batch record overflow; this class essentially serializes overflow vector data in a
 * compact manner so that it is reused for building the next record batch.
 */
public final class RecordBatchOverflow {
  /** Record Overflow Definition */
  final RecordOverflowDefinition recordOverflowDef;
  /** Buffer allocator */
  final BufferAllocator allocator;

  /**
   * @param allocator buffer allocator
   * @param batchStatsContext batch statistics context
   * @return new builder object
   */
  public static Builder newBuilder(BufferAllocator allocator, RecordBatchStatsContext batchStatsContext) {
    return new Builder(allocator, batchStatsContext);
  }

  /**
   * @return the record overflow definition
   */
  public RecordOverflowDefinition getRecordOverflowDefinition() {
    return recordOverflowDef;
  }

  /**
   * Constructor.
   *
   * @param recordOverflowDef record overflow definition
   * @param allocator buffer allocator
   */
  private RecordBatchOverflow(RecordOverflowDefinition recordOverflowDef,
    BufferAllocator allocator) {

    this.recordOverflowDef = recordOverflowDef;
    this.allocator = allocator;
  }

// ----------------------------------------------------------------------------
// Inner Data Structure
// ----------------------------------------------------------------------------

  /** Builder class to construct a {@link RecordBatchOverflow} object */
  public static final class Builder {
    /** Field overflow list */
    private final List<FieldOverflowEntry> fieldOverflowEntries = new ArrayList<FieldOverflowEntry>();
    /** Buffer allocator */
    private final BufferAllocator allocator;
    /** Batch statistics context */
    private final RecordBatchStatsContext batchStatsContext;

    /**
     * Build class to construct a {@link RecordBatchOverflow} object.
     * @param allocator buffer allocator
     * @param batchStatsContext batch statistics context
     */
    private Builder(BufferAllocator allocator, RecordBatchStatsContext batchStatsContext) {
      this.allocator = allocator;
      this.batchStatsContext = batchStatsContext;
    }

    /**
     * Add an overflow field to this batch record overflow object; note that currently only
     * variable numValues objects are supported.
     *
     * @param vector a value vector with overflow values
     * @param firstValueIdx index of first overflow value
     * @param numValues the number of overflow values starting at index "firstValueIdx"
     */
    public void addFieldOverflow(ValueVector vector, int firstValueIdx, int numValues) {
      assert vector instanceof VariableWidthVector;
      fieldOverflowEntries.add(new FieldOverflowEntry(vector, firstValueIdx, numValues));
    }

    /**
     * @return a new built {link BatchRecordOverflow} object instance
     */
    public RecordBatchOverflow build() {
      RecordOverflowContainer overflowContainer = OverflowSerDeUtil.serialize(fieldOverflowEntries, allocator, batchStatsContext);
      RecordBatchOverflow result = new RecordBatchOverflow(overflowContainer.recordOverflowDef, allocator);

      return result;
    }
  } // End of Builder

  /** Field overflow entry */
  static final class FieldOverflowEntry {
    /** A value vector with overflow values */
    final ValueVector vector;
    /** index of first overflow value */
    final int firstValueIdx;
    /** The number of overflow values starting at index "firstValueIdx" */
    final int numValues;

    /**
     * Field overflow entry constructor
     *
     * @param vector a value vector with overflow values
     * @param firstValueIdx index of first overflow value
     * @param numValues the number of overflow values starting at index "firstValueIdx"
     */
    private FieldOverflowEntry(ValueVector vector, int firstValueIdx, int numValues) {
      this.vector = vector;
      this.firstValueIdx = firstValueIdx;
      this.numValues = numValues;
    }
  } // End of FieldOverflowEntry

  /** Record batch definition */
  public static final class RecordOverflowDefinition {
    private final Map<String, FieldOverflowDefinition> fieldOverflowDefs = CaseInsensitiveMap.newHashMap();

    public Map<String, FieldOverflowDefinition> getFieldOverflowDefs() {
      return fieldOverflowDefs;
    }
  } // End of RecordOverflowDefinition

  /** Field overflow definition */
  public static final class FieldOverflowDefinition {
    /** Materialized field */
    public final MaterializedField field;
    /** Number of values */
    public final int numValues;
    /** Data byte length */
    public final int dataByteLen;
    /** DrillBuf where the serialized data is stored */
    public final DrillBuf buffer;

    /**
     * Field overflow definition constructor
     * @param field materialized field
     * @param numValues number of values
     * @param dataByteLen data byte length
     * @param buffer DrillBuf where the serialized data is stored
     */
    FieldOverflowDefinition(MaterializedField field, int numValues, int dataByteLen, DrillBuf buffer) {
      this.field = field;
      this.numValues = numValues;
      this.dataByteLen = dataByteLen;
      this.buffer = buffer;
    }
  } // End of FieldOverflowDefinition

  /** Record overflow container */
  static final class RecordOverflowContainer {
    /** Record Overflow Definition */
    final RecordOverflowDefinition recordOverflowDef = new RecordOverflowDefinition();

  } // End of RecordOverflowContainer

}
