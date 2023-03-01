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
import org.apache.spark.sql.execution.columnar.RssColumnBuilder.ensureFreeSpace
import org.apache.spark.sql.types._

trait RssColumnBuilder {

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
  def columnStats: RssColumnStats

  /**
   * Returns the final columnar byte buffer.
   */
  def build(): ByteBuffer

  def getTotalSize: Long = 0
}

class RssBasicColumnBuilder[JvmType](
    val columnStats: RssColumnStats,
    val columnType: RssColumnType[JvmType])
  extends RssColumnBuilder {

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

class RssNullColumnBuilder
  extends RssBasicColumnBuilder[Any](new RssObjectColumnStats(NullType), RSS_NULL)
  with RssNullableColumnBuilder

abstract class RssComplexColumnBuilder[JvmType](
    columnStats: RssColumnStats,
    columnType: RssColumnType[JvmType])
  extends RssBasicColumnBuilder[JvmType](columnStats, columnType)
  with RssNullableColumnBuilder

abstract class RssNativeColumnBuilder[T <: AtomicType](
    override val columnStats: RssColumnStats,
    override val columnType: NativeRssColumnType[T])
  extends RssBasicColumnBuilder[T#InternalType](columnStats, columnType)
  with RssNullableColumnBuilder
  with AllRssCompressionSchemes
  with RssCompressibleColumnBuilder[T]

class RssBooleanColumnBuilder extends RssNativeColumnBuilder(new RssBooleanColumnStats, RSS_BOOLEAN)

class RssByteColumnBuilder extends RssNativeColumnBuilder(new RssByteColumnStats, RSS_BYTE)

class RssShortColumnBuilder extends RssNativeColumnBuilder(new RssShortColumnStats, RSS_SHORT)

class RssIntColumnBuilder extends RssNativeColumnBuilder(new RssIntColumnStats, RSS_INT)

class RssLongColumnBuilder extends RssNativeColumnBuilder(new RssLongColumnStats, RSS_LONG)

class RssFloatColumnBuilder extends RssNativeColumnBuilder(new RssFloatColumnStats, RSS_FLOAT)

class RssDoubleColumnBuilder extends RssNativeColumnBuilder(new RssDoubleColumnStats, RSS_DOUBLE)

class RssStringColumnBuilder extends RssNativeColumnBuilder(new RssStringColumnStats, RSS_STRING)

class RssCompactMiniDecimalColumnBuilder(dataType: DecimalType)
  extends RssNativeColumnBuilder(
    new RssDecimalColumnStats(dataType),
    RSS_COMPACT_MINI_DECIMAL(dataType))

class RssCompactDecimalColumnBuilder(dataType: DecimalType)
  extends RssNativeColumnBuilder(new RssDecimalColumnStats(dataType), RSS_COMPACT_DECIMAL(dataType))

class RssDecimalColumnBuilder(dataType: DecimalType)
  extends RssComplexColumnBuilder(new RssDecimalColumnStats(dataType), RSS_LARGE_DECIMAL(dataType))

class RssBooleanCodeGenColumnBuilder
  extends RssNativeColumnBuilder(new RssBooleanColumnStats, RSS_BOOLEAN) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_BOOLEAN.actualSize(row, ordinal))
      RSS_BOOLEAN.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssByteCodeGenColumnBuilder extends RssNativeColumnBuilder(new RssByteColumnStats, RSS_BYTE) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_BYTE.actualSize(row, ordinal))
      RSS_BYTE.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssShortCodeGenColumnBuilder
  extends RssNativeColumnBuilder(new RssShortColumnStats, RSS_SHORT) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_SHORT.actualSize(row, ordinal))
      RSS_SHORT.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssIntCodeGenColumnBuilder extends RssNativeColumnBuilder(new RssIntColumnStats, RSS_INT) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_INT.actualSize(row, ordinal))
      RSS_INT.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssLongCodeGenColumnBuilder extends RssNativeColumnBuilder(new RssLongColumnStats, RSS_LONG) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_LONG.actualSize(row, ordinal))
      RSS_LONG.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssFloatCodeGenColumnBuilder
  extends RssNativeColumnBuilder(new RssFloatColumnStats, RSS_FLOAT) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_FLOAT.actualSize(row, ordinal))
      RSS_FLOAT.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssDoubleCodeGenColumnBuilder
  extends RssNativeColumnBuilder(new RssDoubleColumnStats, RSS_DOUBLE) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_DOUBLE.actualSize(row, ordinal))
      RSS_DOUBLE.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssStringCodeGenColumnBuilder
  extends RssNativeColumnBuilder(new RssStringColumnStats, RSS_STRING) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_STRING.actualSize(row, ordinal))
      RSS_STRING.append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssCompactDecimalCodeGenColumnBuilder(dataType: DecimalType)
  extends RssNativeColumnBuilder(
    new RssDecimalColumnStats(dataType),
    RSS_COMPACT_DECIMAL(dataType)) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_COMPACT_DECIMAL(dataType).actualSize(row, ordinal))
      RSS_COMPACT_DECIMAL(dataType).append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssCompactMiniDecimalCodeGenColumnBuilder(dataType: DecimalType)
  extends RssNativeColumnBuilder(
    new RssDecimalColumnStats(dataType),
    RSS_COMPACT_MINI_DECIMAL(dataType)) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_COMPACT_MINI_DECIMAL(dataType).actualSize(row, ordinal))
      RSS_COMPACT_MINI_DECIMAL(dataType).append(row, ordinal, buffer)
    }
    pos += 1
  }
}

class RssDecimalCodeGenColumnBuilder(dataType: DecimalType)
  extends RssComplexColumnBuilder(
    new RssDecimalColumnStats(dataType),
    RSS_LARGE_DECIMAL(dataType)) {
  override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      buffer = ensureFreeSpace(buffer, RSS_LARGE_DECIMAL(dataType).actualSize(row, ordinal))
      RSS_LARGE_DECIMAL(dataType).append(row, ordinal, buffer)
    }
    pos += 1
  }
}

object RssColumnBuilder {
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
      encoder: Encoder[_ <: AtomicType]): RssColumnBuilder = {
    val builder: RssColumnBuilder = dataType match {
      case NullType => new RssNullColumnBuilder
      case ByteType => new RssByteColumnBuilder
      case BooleanType => new RssBooleanColumnBuilder
      case ShortType => new RssShortColumnBuilder
      case IntegerType =>
        val builder = new RssIntColumnBuilder
        builder.init(encoder.asInstanceOf[Encoder[IntegerType.type]])
        builder
      case LongType =>
        val builder = new RssLongColumnBuilder
        builder.init(encoder.asInstanceOf[Encoder[LongType.type]])
        builder
      case FloatType => new RssFloatColumnBuilder
      case DoubleType => new RssDoubleColumnBuilder
      case StringType =>
        val builder = new RssStringColumnBuilder
        builder.init(encoder.asInstanceOf[Encoder[StringType.type]])
        builder
      case dt: DecimalType if dt.precision <= Decimal.MAX_INT_DIGITS =>
        new RssCompactMiniDecimalColumnBuilder(dt)
      case dt: DecimalType if dt.precision <= Decimal.MAX_LONG_DIGITS =>
        new RssCompactDecimalColumnBuilder(dt)
      case dt: DecimalType => new RssDecimalColumnBuilder(dt)
      case other =>
        throw new Exception(s"not support type: $other")
    }

    builder.initialize(rowCnt, columnName, encodingEnabled)
    builder
  }
}
