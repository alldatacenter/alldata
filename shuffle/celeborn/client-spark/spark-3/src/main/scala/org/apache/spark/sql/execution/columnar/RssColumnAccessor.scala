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
import org.apache.spark.sql.execution.vectorized.WritableColumnVector
import org.apache.spark.sql.types._

trait RssColumnAccessor {
  initialize()

  protected def initialize(): Unit

  def hasNext: Boolean

  def extractTo(row: InternalRow, ordinal: Int): Unit

  def extractToColumnVector(columnVector: WritableColumnVector, ordinal: Int): Unit

  protected def underlyingBuffer: ByteBuffer
}

abstract class RssBasicColumnAccessor[JvmType](
    protected val buffer: ByteBuffer,
    protected val columnType: RssColumnType[JvmType])
  extends RssColumnAccessor {

  protected def initialize(): Unit = {}

  override def hasNext: Boolean = buffer.hasRemaining

  override def extractTo(row: InternalRow, ordinal: Int): Unit = {
    extractSingle(row, ordinal)
  }

  override def extractToColumnVector(columnVector: WritableColumnVector, ordinal: Int): Unit = {
    val length = buffer.getInt()
    val bytes = new Array[Byte](length)
    buffer.get(bytes, 0, length)
    columnVector.putByteArray(ordinal, bytes)
  }

  def extractSingle(row: InternalRow, ordinal: Int): Unit = {
    columnType.extract(buffer, row, ordinal)
  }

  protected def underlyingBuffer = buffer
}

class RssNullColumnAccessor(buffer: ByteBuffer)
  extends RssBasicColumnAccessor[Any](buffer, RSS_NULL)
  with RssNullableColumnAccessor

abstract class RssNativeColumnAccessor[T <: AtomicType](
    override protected val buffer: ByteBuffer,
    override protected val columnType: NativeRssColumnType[T])
  extends RssBasicColumnAccessor(buffer, columnType)
  with RssNullableColumnAccessor
  with RssCompressibleColumnAccessor[T]

class RssBooleanColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_BOOLEAN)

class RssByteColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_BYTE)

class RssShortColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_SHORT)

class RssIntColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_INT)

class RssLongColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_LONG)

class RssFloatColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_FLOAT)

class RssDoubleColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_DOUBLE)

class RssStringColumnAccessor(buffer: ByteBuffer)
  extends RssNativeColumnAccessor(buffer, RSS_STRING)

class RssCompactDecimalColumnAccessor(buffer: ByteBuffer, dataType: DecimalType)
  extends RssNativeColumnAccessor(buffer, RSS_COMPACT_DECIMAL(dataType))

class RssDecimalColumnAccessor(buffer: ByteBuffer, dataType: DecimalType)
  extends RssBasicColumnAccessor[Decimal](buffer, RSS_LARGE_DECIMAL(dataType))
  with RssNullableColumnAccessor

private[sql] object RssColumnAccessor {

  def apply(dataType: DataType, buffer: ByteBuffer): RssColumnAccessor = {
    val buf = buffer.order(ByteOrder.nativeOrder)

    dataType match {
      case NullType => new RssNullColumnAccessor(buf)
      case BooleanType => new RssBooleanColumnAccessor(buf)
      case ByteType => new RssByteColumnAccessor(buf)
      case ShortType => new RssShortColumnAccessor(buf)
      case IntegerType => new RssIntColumnAccessor(buf)
      case LongType => new RssLongColumnAccessor(buf)
      case FloatType => new RssFloatColumnAccessor(buf)
      case DoubleType => new RssDoubleColumnAccessor(buf)
      case StringType => new RssStringColumnAccessor(buf)
      case dt: DecimalType if dt.precision <= Decimal.MAX_LONG_DIGITS =>
        new RssCompactDecimalColumnAccessor(buf, dt)
      case dt: DecimalType => new RssDecimalColumnAccessor(buf, dt)
      case other => throw new Exception(s"not support type: $other")
    }
  }

  def decompress(
      columnAccessor: RssColumnAccessor,
      columnVector: WritableColumnVector,
      numRows: Int): Unit = {
    columnAccessor match {
      case nativeAccessor: RssNativeColumnAccessor[_] =>
        nativeAccessor.decompress(columnVector, numRows)
      case d: RssDecimalColumnAccessor =>
        (0 until numRows).foreach(columnAccessor.extractToColumnVector(columnVector, _))
      case _ =>
        throw new RuntimeException("Not support non-primitive type now")
    }
  }

  def decompress(
      array: Array[Byte],
      columnVector: WritableColumnVector,
      dataType: DataType,
      numRows: Int): Unit = {
    val byteBuffer = ByteBuffer.wrap(array)
    val columnAccessor = RssColumnAccessor(dataType, byteBuffer)
    decompress(columnAccessor, columnVector, numRows)
  }
}
