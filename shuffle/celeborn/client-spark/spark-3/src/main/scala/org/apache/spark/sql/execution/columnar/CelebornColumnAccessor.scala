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

trait CelebornColumnAccessor {
  initialize()

  protected def initialize(): Unit

  def hasNext: Boolean

  def extractTo(row: InternalRow, ordinal: Int): Unit

  def extractToColumnVector(columnVector: WritableColumnVector, ordinal: Int): Unit

  protected def underlyingBuffer: ByteBuffer
}

abstract class CelebornBasicColumnAccessor[JvmType](
    protected val buffer: ByteBuffer,
    protected val columnType: CelebornColumnType[JvmType])
  extends CelebornColumnAccessor {

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

class CelebornNullColumnAccessor(buffer: ByteBuffer)
  extends CelebornBasicColumnAccessor[Any](buffer, CELEBORN_NULL)
  with CelebornNullableColumnAccessor

abstract class CelebornNativeColumnAccessor[T <: AtomicType](
    override protected val buffer: ByteBuffer,
    override protected val columnType: NativeCelebornColumnType[T])
  extends CelebornBasicColumnAccessor(buffer, columnType)
  with CelebornNullableColumnAccessor
  with CelebornCompressibleColumnAccessor[T]

class CelebornBooleanColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_BOOLEAN)

class CelebornByteColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_BYTE)

class CelebornShortColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_SHORT)

class CelebornIntColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_INT)

class CelebornLongColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_LONG)

class CelebornFloatColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_FLOAT)

class CelebornDoubleColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_DOUBLE)

class CelebornStringColumnAccessor(buffer: ByteBuffer)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_STRING)

class CelebornCompactDecimalColumnAccessor(buffer: ByteBuffer, dataType: DecimalType)
  extends CelebornNativeColumnAccessor(buffer, CELEBORN_COMPACT_DECIMAL(dataType))

class CelebornDecimalColumnAccessor(buffer: ByteBuffer, dataType: DecimalType)
  extends CelebornBasicColumnAccessor[Decimal](buffer, CELEBORN_LARGE_DECIMAL(dataType))
  with CelebornNullableColumnAccessor

private[sql] object CelebornColumnAccessor {

  def apply(dataType: DataType, buffer: ByteBuffer): CelebornColumnAccessor = {
    val buf = buffer.order(ByteOrder.nativeOrder)

    dataType match {
      case NullType => new CelebornNullColumnAccessor(buf)
      case BooleanType => new CelebornBooleanColumnAccessor(buf)
      case ByteType => new CelebornByteColumnAccessor(buf)
      case ShortType => new CelebornShortColumnAccessor(buf)
      case IntegerType => new CelebornIntColumnAccessor(buf)
      case LongType => new CelebornLongColumnAccessor(buf)
      case FloatType => new CelebornFloatColumnAccessor(buf)
      case DoubleType => new CelebornDoubleColumnAccessor(buf)
      case StringType => new CelebornStringColumnAccessor(buf)
      case dt: DecimalType if dt.precision <= Decimal.MAX_LONG_DIGITS =>
        new CelebornCompactDecimalColumnAccessor(buf, dt)
      case dt: DecimalType => new CelebornDecimalColumnAccessor(buf, dt)
      case other => throw new Exception(s"not support type: $other")
    }
  }

  def decompress(
      columnAccessor: CelebornColumnAccessor,
      columnVector: WritableColumnVector,
      numRows: Int): Unit = {
    columnAccessor match {
      case nativeAccessor: CelebornNativeColumnAccessor[_] =>
        nativeAccessor.decompress(columnVector, numRows)
      case d: CelebornDecimalColumnAccessor =>
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
    val columnAccessor = CelebornColumnAccessor(dataType, byteBuffer)
    decompress(columnAccessor, columnVector, numRows)
  }
}
