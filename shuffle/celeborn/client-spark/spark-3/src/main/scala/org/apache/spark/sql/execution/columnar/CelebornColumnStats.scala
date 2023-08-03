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

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

/**
 * Used to collect statistical information when building in-memory columns.
 *
 * NOTE: we intentionally avoid using `Ordering[T]` to compare values here because `Ordering[T]`
 * brings significant performance penalty.
 */
sealed private[columnar] trait CelebornColumnStats extends Serializable {
  protected var count = 0
  protected var nullCount = 0
  private[columnar] var sizeInBytes = 0L

  /**
   * Gathers statistics information from `row(ordinal)`.
   */
  def gatherStats(row: InternalRow, ordinal: Int): Unit

  /**
   * Gathers statistics information on `null`.
   */
  def gatherNullStats(): Unit = {
    nullCount += 1
    // 4 bytes for null position
    sizeInBytes += 4
    count += 1
  }

  /**
   * Column statistics represented as an array, currently including closed lower bound, closed
   * upper bound and null count.
   */
  def collectedStatistics: Array[Any]
}

final private[columnar] class CelebornBooleanColumnStats extends CelebornColumnStats {
  protected var upper = false
  protected var lower = true

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getBoolean(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Boolean): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_BOOLEAN.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornByteColumnStats extends CelebornColumnStats {
  protected var upper = Byte.MinValue
  protected var lower = Byte.MaxValue

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getByte(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Byte): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_BYTE.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornShortColumnStats extends CelebornColumnStats {
  protected var upper = Short.MinValue
  protected var lower = Short.MaxValue

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getShort(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Short): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_SHORT.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornIntColumnStats extends CelebornColumnStats {
  protected var upper = Int.MinValue
  protected var lower = Int.MaxValue

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getInt(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Int): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_INT.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornLongColumnStats extends CelebornColumnStats {
  protected var upper = Long.MinValue
  protected var lower = Long.MaxValue

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getLong(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Long): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_LONG.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornFloatColumnStats extends CelebornColumnStats {
  protected var upper = Float.MinValue
  protected var lower = Float.MaxValue

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getFloat(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Float): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_FLOAT.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornDoubleColumnStats extends CelebornColumnStats {
  protected var upper = Double.MinValue
  protected var lower = Double.MaxValue

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getDouble(ordinal)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Double): Unit = {
    if (value > upper) upper = value
    if (value < lower) lower = value
    sizeInBytes += CELEBORN_DOUBLE.defaultSize
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornStringColumnStats extends CelebornColumnStats {
  protected var upper: UTF8String = null
  protected var lower: UTF8String = null

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getUTF8String(ordinal)
      val size = CELEBORN_STRING.actualSize(row, ordinal)
      gatherValueStats(value, size)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: UTF8String, size: Int): Unit = {
    if (upper == null || value.compareTo(upper) > 0) upper = value.clone()
    if (lower == null || value.compareTo(lower) < 0) lower = value.clone()
    sizeInBytes += size
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornBinaryColumnStats extends CelebornColumnStats {
  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val size = CELEBORN_BINARY.actualSize(row, ordinal)
      sizeInBytes += size
      count += 1
    } else {
      gatherNullStats
    }
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](null, null, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornDecimalColumnStats(precision: Int, scale: Int)
  extends CelebornColumnStats {
  def this(dt: DecimalType) = this(dt.precision, dt.scale)

  protected var upper: Decimal = null
  protected var lower: Decimal = null

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val value = row.getDecimal(ordinal, precision, scale)
      gatherValueStats(value)
    } else {
      gatherNullStats
    }
  }

  def gatherValueStats(value: Decimal): Unit = {
    if (upper == null || value.compareTo(upper) > 0) upper = value
    if (lower == null || value.compareTo(lower) < 0) lower = value
    if (precision <= Decimal.MAX_INT_DIGITS) {
      sizeInBytes += 4
    } else if (precision <= Decimal.MAX_LONG_DIGITS) {
      sizeInBytes += 8
    } else {
      sizeInBytes += (4 + value.toJavaBigDecimal.unscaledValue().bitLength() / 8 + 1)
    }
    count += 1
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](lower, upper, nullCount, count, sizeInBytes)
}

final private[columnar] class CelebornObjectColumnStats(dataType: DataType)
  extends CelebornColumnStats {
  val columnType = CelebornColumnType(dataType)

  override def gatherStats(row: InternalRow, ordinal: Int): Unit = {
    if (!row.isNullAt(ordinal)) {
      val size = columnType.actualSize(row, ordinal)
      sizeInBytes += size
      count += 1
    } else {
      gatherNullStats
    }
  }

  override def collectedStatistics: Array[Any] =
    Array[Any](null, null, nullCount, count, sizeInBytes)
}
