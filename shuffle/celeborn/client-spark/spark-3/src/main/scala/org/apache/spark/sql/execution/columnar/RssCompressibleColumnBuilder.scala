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

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types.AtomicType
import org.apache.spark.unsafe.Platform

trait RssCompressibleColumnBuilder[T <: AtomicType]
  extends RssColumnBuilder with Logging {

  this: RssNativeColumnBuilder[T] with WithRssCompressionSchemes =>

  var compressionEncoder: Encoder[T] = RssPassThrough.encoder(columnType)

  def init(encoder: Encoder[T]): Unit = {
    compressionEncoder = encoder
  }

  abstract override def initialize(
      rowCnt: Int,
      columnName: String,
      encodingEnabled: Boolean): Unit = {
    super.initialize(rowCnt, columnName, encodingEnabled)
  }

  // The various compression schemes, while saving memory use, cause all of the data within
  // the row to become unaligned, thus causing crashes.  Until a way of fixing the compression
  // is found to also allow aligned accesses this must be disabled for SPARK.

  protected def isWorthCompressing(encoder: Encoder[T]) = {
    RssCompressibleColumnBuilder.unaligned && encoder.compressionRatio < 0.8
  }

  private def gatherCompressibilityStats(row: InternalRow, ordinal: Int): Unit = {
    compressionEncoder.gatherCompressibilityStats(row, ordinal)
  }

  abstract override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    super.appendFrom(row, ordinal)
    if (!row.isNullAt(ordinal)) {
      gatherCompressibilityStats(row, ordinal)
    }
  }

  override def build(): ByteBuffer = {
    val nonNullBuffer = buildNonNulls()
    val encoder: Encoder[T] = {
      if (isWorthCompressing(compressionEncoder)) compressionEncoder
      else RssPassThrough.encoder(columnType)
    }

    // Header = null count + null positions
    val headerSize = 4 + nulls.limit()
    val compressedSize =
      if (encoder.compressedSize == 0) {
        nonNullBuffer.remaining()
      } else {
        encoder.compressedSize
      }

    val compressedBuffer = ByteBuffer
      // Reserves 4 bytes for compression scheme ID
      .allocate(headerSize + 4 + compressedSize)
      .order(ByteOrder.nativeOrder)
      // Write the header
      .putInt(nullCount)
      .put(nulls)

    logDebug(s"Compressor for [$columnName]: $encoder, ratio: ${encoder.compressionRatio}")
    encoder.compress(nonNullBuffer, compressedBuffer)
  }

  override def getTotalSize: Long = {
    val encoder: Encoder[T] = {
      if (isWorthCompressing(compressionEncoder)) compressionEncoder
      else RssPassThrough.encoder(columnType)
    }
    if (encoder.compressedSize == 0) {
      4 + 4 + columnStats.sizeInBytes
    } else {
      4 + 4 * nullCount + 4 + encoder.compressedSize
    }
  }
}

object RssCompressibleColumnBuilder {
  val unaligned = Platform.unaligned()
}
