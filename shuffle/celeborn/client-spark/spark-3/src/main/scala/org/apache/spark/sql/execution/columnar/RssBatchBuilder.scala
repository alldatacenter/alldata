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
import org.apache.spark.sql.types.{BooleanType, ByteType, DecimalType, DoubleType, FloatType, IntegerType, LongType, ShortType, StringType, StructType}

abstract class RssBatchBuilder {

  def newBuilders(): Unit

  def buildColumnBytes(): Array[Byte]

  def writeRow(row: InternalRow): Unit

  def getRowCnt(): Int

  def int2ByteArray(i: Int): Array[Byte] = {
    val result = new Array[Byte](4)
    result(0) = ((i >> 24) & 0xFF).toByte
    result(1) = ((i >> 16) & 0xFF).toByte
    result(2) = ((i >> 8) & 0xFF).toByte
    result(3) = (i & 0xFF).toByte
    result
  }
}

object RssBatchBuilder {
  def supportsColumnarType(schema: StructType): Boolean = {
    schema.fields.forall(f =>
      f.dataType match {
        case BooleanType | ByteType | ShortType | IntegerType | LongType |
            FloatType | DoubleType | StringType => true
        case dt: DecimalType => true
        case _ => false
      })
  }
}
