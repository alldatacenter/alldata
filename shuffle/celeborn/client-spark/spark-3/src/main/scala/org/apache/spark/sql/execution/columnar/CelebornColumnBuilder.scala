/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.columnar

import java.nio.{ByteBuffer, ByteOrder}

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.execution.columnar.CelebornColumnBuilder.ensureFreeSpace
import org.apache.spark.sql.types._

trait CelebornColumnBuilder {

  /**
   * Initializes with an approximate lower bound on the expected number of elements in this column.
   */
  def initialize(
      rowCnt: Int,
      columnName: String = "",
      encodingEnabled: Boolean = false): Unit

  /**
   * Appends `row(ordinal)` to the column builder.
   */
  def appendFrom(row: InternalRow, ordinal: Int): Unit

  /**
   * Column statistics information
   */
  def columnStats: CelebornColumnStats

  /**
   * Returns the final columnar byte buffer.
   */
  def build(): ByteBuffer

  def getTotalSize: Long = 0
}

class CelebornBasicColumnBuilder[JvmType](
    val columnStats: CelebornColumnStats,
    val columnType: CelebornColumnType[JvmType])
  extends CelebornColumnBuilder {

  protected var columnName: String = _

  protected var buffer: ByteBuffer = _

  override def initialize(
      rowCnt: Int,
      columnName: String = "",
      encodingEnabled: Boolean = false): Unit = {

    this.columnName = columnName

    buffer = ByteBuffer.allocate(rowCnt * columnType.defaultSize)
    buffer.order(ByteOrder.nativeOrder())
  }

  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    buffer = ensureFreeSpace(buffer, columnType.actualSize(row, ordinal))
    columnType.append(row, ordinal, buffer)
  }

  override def build(): ByteBuffer = {
    if (buffer.capacity() > buffer.position() * 1.1) {
      // trim the buffer
      buffer = ByteBuffer
        .allocate(buffer.position())
        .order(ByteOrder.nativeOrder())
        .put(buffer.array(), 0, buffer.position())
    }
    buffer.flip().asInstanceOf[ByteBuffer]
  }
}

class CelebornNullColumnBuilder
  extends CelebornBasicColumnBuilder[Any](new CelebornObjectColumnStats(NullType), CELEBORN_NULL)
  with CelebornNullableColumnBuilder

abstract class CelebornComplexColumnBuilder[JvmType](
    columnStats: CelebornColumnStats,
    columnType: CelebornColumnType[JvmType])
  extends CelebornBasicColumnBuilder[JvmType](columnStats, columnType)
  with CelebornNullableColumnBuilder

abstract class CelebornNativeColumnBuilder[T <: AtomicType](
    override val columnStats: CelebornColumnStats,
    override val columnType: NativeCelebornColumnType[T])
  extends CelebornBasicColumnBuilder[T#InternalType](columnStats, columnType)
  with CelebornNullableColumnBuilder
  with AllCelebornCompressionSchemes
  with CelebornCompressibleColumnBuilder[T]

class CelebornBooleanColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornBooleanColumnStats, CELEBORN_BOOLEAN)

class CelebornByteColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornByteColumnStats, CELEBORN_BYTE)

class CelebornShortColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornShortColumnStats, CELEBORN_SHORT)

class CelebornIntColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornIntColumnStats, CELEBORN_INT)

class CelebornLongColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornLongColumnStats, CELEBORN_LONG)

class CelebornFloatColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornFloatColumnStats, CELEBORN_FLOAT)

class CelebornDoubleColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornDoubleColumnStats, CELEBORN_DOUBLE)

class CelebornStringColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornStringColumnStats, CELEBORN_STRING)

class CelebornCompactMiniDecimalColumnBuilder(dataType: DecimalType)
  extends CelebornNativeColumnBuilder(
    new CelebornDecimalColumnStats(dataType),
    CELEBORN_COMPACT_MINI_DECIMAL(dataType))

class CelebornCompactDecimalColumnBuilder(dataType: DecimalType)
  extends CelebornNativeColumnBuilder(
    new CelebornDecimalColumnStats(dataType),
    CELEBORN_COMPACT_DECIMAL(dataType))

class CelebornDecimalColumnBuilder(dataType: DecimalType)
  extends CelebornComplexColumnBuilder(
    new CelebornDecimalColumnStats(dataType),
    CELEBORN_LARGE_DECIMAL(dataType))

class CelebornBooleanCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornBooleanColumnStats, CELEBORN_BOOLEAN) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_BOOLEAN.actualSize(row, ordinal))
      CELEBORN_BOOLEAN.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornByteCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornByteColumnStats, CELEBORN_BYTE) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_BYTE.actualSize(row, ordinal))
      CELEBORN_BYTE.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornShortCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornShortColumnStats, CELEBORN_SHORT) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_SHORT.actualSize(row, ordinal))
      CELEBORN_SHORT.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornIntCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornIntColumnStats, CELEBORN_INT) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_INT.actualSize(row, ordinal))
      CELEBORN_INT.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornLongCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornLongColumnStats, CELEBORN_LONG) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_LONG.actualSize(row, ordinal))
      CELEBORN_LONG.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornFloatCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornFloatColumnStats, CELEBORN_FLOAT) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_FLOAT.actualSize(row, ordinal))
      CELEBORN_FLOAT.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornDoubleCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornDoubleColumnStats, CELEBORN_DOUBLE) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_DOUBLE.actualSize(row, ordinal))
      CELEBORN_DOUBLE.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornStringCodeGenColumnBuilder
  extends CelebornNativeColumnBuilder(new CelebornStringColumnStats, CELEBORN_STRING) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_STRING.actualSize(row, ordinal))
      CELEBORN_STRING.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornCompactDecimalCodeGenColumnBuilder(dataType: DecimalType)
  extends CelebornNativeColumnBuilder(
    new CelebornDecimalColumnStats(dataType),
    CELEBORN_COMPACT_DECIMAL(dataType)) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_COMPACT_DECIMAL(dataType).actualSize(row, ordinal))
      CELEBORN_COMPACT_DECIMAL(dataType).append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornCompactMiniDecimalCodeGenColumnBuilder(dataType: DecimalType)
  extends CelebornNativeColumnBuilder(
    new CelebornDecimalColumnStats(dataType),
    CELEBORN_COMPACT_MINI_DECIMAL(dataType)) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer =
        ensureFreeSpace(buffer, CELEBORN_COMPACT_MINI_DECIMAL(dataType).actualSize(row, ordinal))
      CELEBORN_COMPACT_MINI_DECIMAL(dataType).append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class CelebornDecimalCodeGenColumnBuilder(dataType: DecimalType)
  extends CelebornComplexColumnBuilder(
    new CelebornDecimalColumnStats(dataType),
    CELEBORN_LARGE_DECIMAL(dataType)) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = CelebornColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, CELEBORN_LARGE_DECIMAL(dataType).actualSize(row, ordinal))
      CELEBORN_LARGE_DECIMAL(dataType).append(row, ordinal, buffer)
    }
    pos += 1
  }
}

object CelebornColumnBuilder {
  val MAX_BATCH_SIZE_IN_BYTE: Long = 4 * 1024 * 1024L

  def ensureFreeSpace(orig: ByteBuffer, size: Int): ByteBuffer = {
    if (orig.remaining >= size) {
      orig
    } else {
      // grow in steps of initial size
      val capacity = orig.capacity()
      val newSize = capacity + size.max(capacity)
      val pos = orig.position()

      ByteBuffer
        .allocate(newSize)
        .order(ByteOrder.nativeOrder())
        .put(orig.array(), 0, pos)
    }
  }

  def apply(
      dataType: DataType,
      rowCnt: Int,
      columnName: String,
      encodingEnabled: Boolean,
      encoder: Encoder[_ <: AtomicType]): CelebornColumnBuilder = {
    val builder: CelebornColumnBuilder = dataType match {
      case NullType => new CelebornNullColumnBuilder
      case ByteType => new CelebornByteColumnBuilder
      case BooleanType => new CelebornBooleanColumnBuilder
      case ShortType => new CelebornShortColumnBuilder
      case IntegerType =>
        val builder = new CelebornIntColumnBuilder
        builder.init(encoder.asInstanceOf[Encoder[IntegerType.type]])
        builder
      case LongType =>
        val builder = new CelebornLongColumnBuilder
        builder.init(encoder.asInstanceOf[Encoder[LongType.type]])
        builder
      case FloatType => new CelebornFloatColumnBuilder
      case DoubleType => new CelebornDoubleColumnBuilder
      case StringType =>
        val builder = new CelebornStringColumnBuilder
        builder.init(encoder.asInstanceOf[Encoder[StringType.type]])
        builder
      case dt: DecimalType if dt.precision <= Decimal.MAX_INT_DIGITS =>
        new CelebornCompactMiniDecimalColumnBuilder(dt)
      case dt: DecimalType if dt.precision <= Decimal.MAX_LONG_DIGITS =>
        new CelebornCompactDecimalColumnBuilder(dt)
      case dt: DecimalType => new CelebornDecimalColumnBuilder(dt)
      case other =>
        throw new Exception(s"not support type: $other")
    }

    builder.initialize(rowCnt, columnName, encodingEnabled)
    builder
  }
}
