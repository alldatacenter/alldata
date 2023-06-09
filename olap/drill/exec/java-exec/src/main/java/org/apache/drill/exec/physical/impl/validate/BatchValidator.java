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
package org.apache.drill.exec.physical.impl.validate;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.SimpleVectorWrapper;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.NullableVar16CharVector;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.NullableVarDecimalVector;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.RepeatedBitVector;
import org.apache.drill.exec.vector.UInt1Vector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarBinaryVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.VarDecimalVector;
import org.apache.drill.exec.vector.VariableWidthVector;
import org.apache.drill.exec.vector.ZeroVector;
import org.apache.drill.exec.vector.complex.AbstractRepeatedMapVector;
import org.apache.drill.exec.vector.complex.BaseRepeatedValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validate a batch of value vectors. It is not possible to validate the
 * data, but we can validate the structure, especially offset vectors.
 * Only handles single (non-hyper) vectors at present. Current form is
 * self-contained. Better checks can be done by moving checks inside
 * vectors or by exposing more metadata from vectors.
 * <p>
 * Drill is not clear on how to handle a batch of zero records. Offset
 * vectors normally have one more entry than the record count. If a
 * batch has 1 record, the offset vector has 2 entries. The entry at
 * 0 is always 0, the entry at 1 marks the end of the 0th record.
 * <p>
 * But, this gets a bit murky. If a batch has one record, and contains
 * a repeated map, and the map has no entries, then the nested offset
 * vector usually has 0 entries, not 1.
 * <p>
 * Generalizing, sometimes when a batch has zero records, the "top-level"
 * offset vectors have 1 items, sometimes zero items.
 * <p>
 * The simplest solution would be to simply enforce here that all offset
 * vectors must have n+1 entries, where n is the row count (top-level
 * vectors) or item count (nested vectors.)
 * <p>
 * But, after fighting with the code, this seems an unobtainable goal.
 * For one thing, deserialization seems to rely on nested offset vectors
 * having zero entries when the value count is zero.
 * <p>
 * Instead, this code assumes that any offset vector, top-level or
 * nested, will have zero entries if the value count is zero. That is
 * an offset vector has either zero entries or n+1 entries.
 */

public class BatchValidator {
  private static final Logger logger = LoggerFactory.getLogger(BatchValidator.class);

  public static final boolean LOG_TO_STDOUT = true;
  public static final int MAX_ERRORS = 100;

  public interface ErrorReporter {
    void error(String name, ValueVector vector, String msg);
    void warn(String name, ValueVector vector, String msg);
    void error(String msg);
    int errorCount();
  }

  public abstract static class BaseErrorReporter implements ErrorReporter {

    private final String opName;
    private int errorCount;

    public BaseErrorReporter(String opName) {
      this.opName = opName;
    }

    protected boolean startError() {
      if (errorCount == 0) {
        warn("Found one or more vector errors from " + opName);
      }
      errorCount++;
      if (errorCount >= MAX_ERRORS) {
        return false;
      }
      return true;
    }

    @Override
    public void error(String name, ValueVector vector, String msg) {
      error(String.format("%s - %s: %s",
            name, vector.getClass().getSimpleName(), msg));
    }

    @Override
    public void warn(String name, ValueVector vector, String msg) {
      warn(String.format("%s - %s: %s",
            name, vector.getClass().getSimpleName(), msg));
    }

    public abstract void warn(String msg);

    @Override
    public int errorCount() { return errorCount; }
  }

  private static class StdOutReporter extends BaseErrorReporter {

    public StdOutReporter(String opName) {
      super(opName);
    }

    @Override
    public void error(String msg) {
      if (startError()) {
        System.out.println(msg);
      }
    }

    @Override
    public void warn(String msg) {
      System.out.println(msg);
    }
  }

  private static class LogReporter extends BaseErrorReporter {

    public LogReporter(String opName) {
      super(opName);
    }

    @Override
    public void error(String msg) {
      if (startError()) {
        logger.error(msg);
      }
    }

    @Override
    public void warn(String msg) {
      logger.error(msg);
    }
  }

  private final ErrorReporter errorReporter;

  public BatchValidator(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  public static boolean validate(RecordBatch batch) {
    // This is a handy place to trace batches as they flow up
    // the DAG. Works best for single-threaded runs with a few records.
    // System.out.println(batch.getClass().getSimpleName());
    // RowSetFormatter.print(batch);
    ErrorReporter reporter = errorReporter(batch);
    int rowCount = batch.getRecordCount();
    int valueCount = rowCount;
    VectorContainer container = batch.getContainer();
    if (!container.hasRecordCount()) {
      reporter.error(String.format(
          "%s: Container record count not set",
          batch.getClass().getSimpleName()));
    } else {
      // Row count will = container count for most operators.
      // Row count <= container count for the filter operator.

      int containerRowCount = container.getRecordCount();
      valueCount = containerRowCount;
      switch (batch.getSchema().getSelectionVectorMode()) {
      case FOUR_BYTE:
        int sv4Count = batch.getSelectionVector4().getCount();
        if (sv4Count != rowCount) {
          reporter.error(String.format(
              "Mismatch between %s record count = %d, SV4 record count = %d",
              batch.getClass().getSimpleName(),
              rowCount, sv4Count));
        }
        // TODO: Don't know how to check SV4 batches
        return true;
      case TWO_BYTE:
        int sv2Count = batch.getSelectionVector2().getCount();
        if (sv2Count != rowCount) {
          reporter.error(String.format(
              "Mismatch between %s record count = %d, SV2 record count = %d",
              batch.getClass().getSimpleName(),
              rowCount, sv2Count));
        }
        if (sv2Count > containerRowCount) {
          reporter.error(String.format(
              "Mismatch between %s container count = %d, SV2 record count = %d",
              batch.getClass().getSimpleName(),
              containerRowCount, sv2Count));
        }
        int svTotalCount = batch.getSelectionVector2().getBatchActualRecordCount();
        if (svTotalCount != containerRowCount) {
          reporter.error(String.format(
              "Mismatch between %s container count = %d, SV2 total count = %d",
              batch.getClass().getSimpleName(),
              containerRowCount, svTotalCount));
        }
        break;
      default:
        if (rowCount != containerRowCount) {
          reporter.error(String.format(
              "Mismatch between %s record count = %d, container record count = %d",
              batch.getClass().getSimpleName(),
              rowCount, containerRowCount));
        }
        break;
      }
    }
    new BatchValidator(reporter).validateBatch(batch, valueCount);
    return reporter.errorCount() == 0;
  }

  public static boolean validate(VectorAccessible batch) {
    ErrorReporter reporter = errorReporter(batch);
    new BatchValidator(reporter).validateBatch(batch, batch.getRecordCount());
    return reporter.errorCount() == 0;
  }

  private static ErrorReporter errorReporter(VectorAccessible batch) {
    String opName = batch.getClass().getSimpleName();
    if (LOG_TO_STDOUT) {
      return new StdOutReporter(opName);
    } else {
      return new LogReporter(opName);
    }
  }

  public void validateBatch(VectorAccessible batch, int rowCount) {
    for (VectorWrapper<? extends ValueVector> w : batch) {
      validateWrapper(rowCount, w);
    }
  }

  private void validateWrapper(int rowCount, VectorWrapper<? extends ValueVector> w) {
    if (w instanceof SimpleVectorWrapper) {
      validateVector(rowCount, w.getValueVector());
    }
  }

  private void validateVector(int expectedCount, ValueVector vector) {
    validateVector(vector.getField().getName(), expectedCount, vector);
  }

  private void validateVector(String name, int expectedCount, ValueVector vector) {
    int valueCount = vector.getAccessor().getValueCount();
    if (valueCount != expectedCount) {
      error(name, vector,
          String.format("Row count = %d, but value count = %d",
              expectedCount, valueCount));
    }
    validateVector(name, vector);
  }

  private void validateVector(String name, ValueVector vector) {
    if (vector instanceof BitVector) {
      validateBitVector(name, (BitVector) vector);
    } else if (vector instanceof RepeatedBitVector) {
      validateRepeatedBitVector(name, (RepeatedBitVector) vector);
    } else if (vector instanceof NullableVector) {
      validateNullableVector(name, (NullableVector) vector);
    } else if (vector instanceof VarCharVector) {
      validateVarCharVector(name, (VarCharVector) vector);
    } else if (vector instanceof VarBinaryVector) {
      validateVarBinaryVector(name, (VarBinaryVector) vector);
    } else if (vector instanceof FixedWidthVector ||
               vector instanceof ZeroVector) {
      // Not much to do. The only item to check is the vector
      // count itself, which was already done. There is no inner
      // structure to check.
    } else if (vector instanceof BaseRepeatedValueVector) {
      validateRepeatedVector(name, (BaseRepeatedValueVector) vector);
    } else if (vector instanceof AbstractRepeatedMapVector) { // RepeatedMapVector or DictVector
      // In case of DictVector, keys and values vectors are not validated explicitly to avoid NPE
      // when keys and values vectors are not set. This happens when output dict vector's keys and
      // values are not constructed while copying values from input reader to dict writer and the
      // input reader is an instance of NullReader for all rows which does not have schema.
      validateRepeatedMapVector(name, (AbstractRepeatedMapVector) vector);
    } else if (vector instanceof MapVector) {
      validateMapVector(name, (MapVector) vector);
    } else if (vector instanceof RepeatedListVector) {
      validateRepeatedListVector(name, (RepeatedListVector) vector);
    } else if (vector instanceof UnionVector) {
      validateUnionVector(name, (UnionVector) vector);
    } else if (vector instanceof VarDecimalVector) {
      validateVarDecimalVector(name, (VarDecimalVector) vector);
    } else {
      logger.debug("Don't know how to validate vector: {}  of class {}",
          name, vector.getClass().getSimpleName());
    }
  }

  private void validateNullableVector(String name, NullableVector vector) {
    int outerCount = vector.getAccessor().getValueCount();
    ValueVector valuesVector = vector.getValuesVector();
    int valueCount = valuesVector.getAccessor().getValueCount();
    if (valueCount != outerCount) {
      error(name, vector, String.format(
          "Outer value count = %d, but inner value count = %d",
          outerCount, valueCount));
    }
    int lastSet = getLastSet(vector);
    if (lastSet != -2) {
      if (lastSet != valueCount - 1) {
        error(name, vector, String.format(
            "Value count = %d, but last set = %d",
            valueCount, lastSet));
      }
    }
    verifyIsSetVector(vector, (UInt1Vector) vector.getBitsVector());
    validateVector(name + "-values", valuesVector);
  }

  // getLastSet() is visible per vector type, not on a super class.
  // There is no common nullable, variable width super class.

  private int getLastSet(NullableVector vector) {
    if (vector instanceof NullableVarCharVector) {
      return ((NullableVarCharVector) vector).getMutator().getLastSet();
    }
    if (vector instanceof NullableVarBinaryVector) {
      return ((NullableVarBinaryVector) vector).getMutator().getLastSet();
    }
    if (vector instanceof NullableVarDecimalVector) {
      return ((NullableVarDecimalVector) vector).getMutator().getLastSet();
    }
    if (vector instanceof NullableVar16CharVector) {
      return ((NullableVar16CharVector) vector).getMutator().getLastSet();
    }
    // Otherwise, return a value that is never legal for lastSet
    return -2;
  }

  private void validateVarCharVector(String name, VarCharVector vector) {
    int dataLength = vector.getBuffer().writerIndex();
    validateVarWidthVector(name, vector, dataLength);
  }

  private void validateVarBinaryVector(String name, VarBinaryVector vector) {
    int dataLength = vector.getBuffer().writerIndex();
    int lastOffset = validateVarWidthVector(name, vector, dataLength);
    if (lastOffset != dataLength) {
      error(name, vector, String.format(
          "Data vector has length %d, but offset vector has largest offset %d",
          dataLength, lastOffset));
    }
  }

  private void validateVarDecimalVector(String name, VarDecimalVector vector) {
    int dataLength = vector.getBuffer().writerIndex();
    validateVarWidthVector(name, vector, dataLength);
  }

  private int validateVarWidthVector(String name, VariableWidthVector vector, int dataLength) {
    int valueCount = vector.getAccessor().getValueCount();
    return validateOffsetVector(name + "-offsets", vector.getOffsetVector(),
        valueCount, dataLength);
  }

  private void validateRepeatedVector(String name, BaseRepeatedValueVector vector) {
    ValueVector dataVector = vector.getDataVector();
    int dataLength = dataVector.getAccessor().getValueCount();
    int valueCount = vector.getAccessor().getValueCount();
    int itemCount = validateOffsetVector(name + "-offsets", vector.getOffsetVector(),
        valueCount, dataLength);

    if (dataLength != itemCount) {
      error(name, vector, String.format(
          "Data vector has %d values, but offset vector has %d values",
          dataLength, itemCount));
    }

    // Special handling of repeated VarChar vectors
    // The nested data vectors are not quite exactly like top-level vectors.

    validateVector(name + "-data", dataVector);
  }

  private void validateRepeatedBitVector(String name, RepeatedBitVector vector) {
    int valueCount = vector.getAccessor().getValueCount();
    int maxBitCount = valueCount * 8;
    int elementCount = validateOffsetVector(name + "-offsets",
        vector.getOffsetVector(), valueCount, maxBitCount);
    BitVector dataVector = vector.getDataVector();
    if (dataVector.getAccessor().getValueCount() != elementCount) {
      error(name, vector, String.format(
          "Bit vector has %d values, but offset vector labels %d values",
          valueCount, elementCount));
    }
    validateBitVector(name + "-data", dataVector);
  }

  private void validateBitVector(String name, BitVector vector) {
    BitVector.Accessor accessor = vector.getAccessor();
    int valueCount = accessor.getValueCount();
    int dataLength = vector.getBuffer().writerIndex();
    int expectedLength = BitVector.getSizeFromCount(valueCount);
    if (dataLength != expectedLength) {
      error(name, vector, String.format(
          "Bit vector has %d values, buffer has length %d, expected %d",
          valueCount, dataLength, expectedLength));
    }
  }

  private void validateMapVector(String name, MapVector vector) {
    int valueCount = vector.getAccessor().getValueCount();
    for (ValueVector child: vector) {
      validateVector(valueCount, child);
    }
  }

  private void validateRepeatedMapVector(String name, AbstractRepeatedMapVector vector) {
    int valueCount = vector.getAccessor().getValueCount();
    int elementCount = validateOffsetVector(name + "-offsets",
        vector.getOffsetVector(), valueCount, Integer.MAX_VALUE);
    for (ValueVector child: vector) {
      validateVector(elementCount, child);
    }
  }

  private void validateRepeatedListVector(String name,
      RepeatedListVector vector) {
    int valueCount = vector.getAccessor().getValueCount();
    int elementCount = validateOffsetVector(name + "-offsets",
        vector.getOffsetVector(), valueCount, Integer.MAX_VALUE);
    validateVector(elementCount, vector.getDataVector());
  }

  private void validateUnionVector(String name, UnionVector vector) {
    int valueCount = vector.getAccessor().getValueCount();
    MapVector internalMap = vector.getTypeMap();
    for (MinorType type : vector.getSubTypes()) {
      if (type == MinorType.LATE) {
        error(name, vector, String.format(
            "Union vector includes illegal type LATE %s",
            type.name()));
        continue;
      }

      // Warning: do not call getMember(type), doing so will create an
      // empty map vector that causes validation to fail.
      ValueVector child = internalMap.getChild(type.name());
      if (child == null) {
        // Disabling this check for now. TopNBatch, SortBatch
        // and perhaps others will create vectors with a set of
        // types, but won't actually populate some of the types.
        //
        // error(name, vector, String.format(
        //     "Union vector includes type %s, but the internal map has no matching member",
        //     type.name()));
      } else {
        validateVector(name + "-type-" + type.name(),
            valueCount, child);
      }
    }
  }

  private int validateOffsetVector(String name, UInt4Vector offsetVector,
      int valueCount, int maxOffset) {
    // VectorPrinter.printOffsets(offsetVector, valueCount + 1);
    UInt4Vector.Accessor accessor = offsetVector.getAccessor();
    int offsetCount = accessor.getValueCount();
    if (valueCount == 0 && offsetCount > 1 || valueCount > 0 && offsetCount != valueCount + 1) {
      error(name, offsetVector, String.format(
          "Outer vector has %d values, but offset vector has %d, expected %d",
          valueCount, offsetCount, valueCount + 1));
    }
    if (valueCount == 0) {
      return 0;
    }

    // First value must be zero. (This is why offset vectors have one more
    // value than the number of rows.)

    int prevOffset;
    try {
      prevOffset = accessor.get(0);
    } catch (IndexOutOfBoundsException e) {
      error(name, offsetVector, "Offset (0) must be 0 but was undefined");
      return 0;
    }
    if (prevOffset != 0) {
      error(name, offsetVector, "Offset (0) must be 0 but was " + prevOffset);
    }

    for (int i = 1; i < offsetCount; i++) {
      int offset = accessor.get(i);
      if (offset < prevOffset) {
        error(name, offsetVector, String.format(
            "Offset vector [%d] contained %d, expected >= %d",
            i, offset, prevOffset));
      } else if (offset > maxOffset) {
        error(name, offsetVector, String.format(
            "Invalid offset at index %d: %d exceeds maximum of %d",
            i, offset, maxOffset));
      }
      prevOffset = offset;
    }
    return prevOffset;
  }

  private void error(String name, ValueVector vector, String msg) {
    errorReporter.error(name, vector, msg);
  }

  private void verifyIsSetVector(ValueVector parent, UInt1Vector bv) {
    String name = String.format("%s (%s)-bits",
        parent.getField().getName(),
        parent.getClass().getSimpleName());
    int rowCount = parent.getAccessor().getValueCount();
    int bitCount = bv.getAccessor().getValueCount();
    if (bitCount != rowCount) {
      error(name, bv, String.format(
          "Value count = %d, but bit count = %d",
          rowCount, bitCount));
    }
    UInt1Vector.Accessor ba = bv.getAccessor();
    for (int i = 0; i < bitCount; i++) {
      int value = ba.get(i);
      if (value != 0 && value != 1) {
        error(name, bv, String.format(
            "Bit vector[%d] = %d, expected 0 or 1",
            i, value));
      }
    }
  }
}
