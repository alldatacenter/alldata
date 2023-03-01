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
import org.apache.spark.sql.types.AtomicType

trait Encoder[T <: AtomicType] {
  def gatherCompressibilityStats(row: InternalRow, ordinal: Int): Unit = {}

  def compressedSize: Int

  def uncompressedSize: Int

  def compressionRatio: Double = {
    if (uncompressedSize > 0) compressedSize.toDouble / uncompressedSize else 1.0
  }

  def compress(from: ByteBuffer, to: ByteBuffer): ByteBuffer
}

trait Decoder[T <: AtomicType] {
  def next(row: InternalRow, ordinal: Int): Unit

  def hasNext: Boolean

  def decompress(columnVector: WritableColumnVector, capacity: Int): Unit
}

trait RssCompressionScheme {
  def typeId: Int

  def supports(columnType: RssColumnType[_]): Boolean

  def encoder[T <: AtomicType](columnType: NativeRssColumnType[T]): Encoder[T]

  def decoder[T <: AtomicType](buffer: ByteBuffer, columnType: NativeRssColumnType[T]): Decoder[T]
}

trait WithRssCompressionSchemes {
  def schemes: Seq[RssCompressionScheme]
}

trait AllRssCompressionSchemes extends WithRssCompressionSchemes {
  override val schemes: Seq[RssCompressionScheme] = RssCompressionScheme.all
}

object RssCompressionScheme {
  val all: Seq[RssCompressionScheme] =
    Seq(RssPassThrough, RssDictionaryEncoding)

  private val typeIdToScheme = all.map(scheme => scheme.typeId -> scheme).toMap

  def apply(typeId: Int): RssCompressionScheme = {
    typeIdToScheme.getOrElse(
      typeId,
      throw new UnsupportedOperationException(s"Unrecognized compression scheme type ID: $typeId"))
  }

  def columnHeaderSize(columnBuffer: ByteBuffer): Int = {
    val header = columnBuffer.duplicate().order(ByteOrder.nativeOrder)
    val nullCount = header.getInt()
    // null count + null positions
    4 + 4 * nullCount
  }
}
