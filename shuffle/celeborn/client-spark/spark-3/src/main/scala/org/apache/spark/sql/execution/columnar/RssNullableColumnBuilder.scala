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

trait RssNullableColumnBuilder extends RssColumnBuilder {
  protected var nulls: ByteBuffer = _
  protected var nullCount: Int = _
  var pos: Int = _

  abstract override def initialize(
      rowCnt: Int,
      columnName: String,
      encodingEnabled: Boolean): Unit = {

    nulls = ByteBuffer.allocate(1024)
    nulls.order(ByteOrder.nativeOrder())
    pos = 0
    nullCount = 0
    super.initialize(rowCnt, columnName, encodingEnabled)
  }

  abstract override def appendFrom(row: InternalRow, ordinal: Int): Unit = {
    if (row.isNullAt(ordinal)) {
      nulls = RssColumnBuilder.ensureFreeSpace(nulls, 4)
      nulls.putInt(pos)
      nullCount += 1
    } else {
      super.appendFrom(row, ordinal)
    }
    pos += 1
  }

  abstract override def build(): ByteBuffer = {
    val nonNulls = super.build()
    val nullDataLen = nulls.position()

    nulls.limit(nullDataLen)
    nulls.rewind()

    val buffer = ByteBuffer
      .allocate(4 + nullDataLen + nonNulls.remaining())
      .order(ByteOrder.nativeOrder())
      .putInt(nullCount)
      .put(nulls)
      .put(nonNulls)

    buffer.rewind()
    buffer
  }

  protected def buildNonNulls(): ByteBuffer = {
    nulls.limit(nulls.position()).rewind()
    super.build()
  }

  override def getTotalSize: Long = {
    4 + columnStats.sizeInBytes
  }
}
