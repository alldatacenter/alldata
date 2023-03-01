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

import java.io._
import java.nio.ByteBuffer

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

import com.google.common.io.ByteStreams
import org.apache.spark.serializer.{DeserializationStream, SerializationStream, Serializer, SerializerInstance}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.execution.metric.SQLMetric
import org.apache.spark.sql.execution.vectorized.{OffHeapColumnVector, OnHeapColumnVector, WritableColumnVector}
import org.apache.spark.sql.types._
import org.apache.spark.sql.vectorized.{ColumnarBatch, ColumnVector}

class RssColumnarBatchSerializer(
    schema: StructType,
    columnBatchSize: Int,
    encodingEnabled: Boolean,
    offHeapColumnVectorEnabled: Boolean,
    dataSize: SQLMetric = null) extends Serializer with Serializable {
  override def newInstance(): SerializerInstance =
    new RssColumnarBatchSerializerInstance(
      schema,
      columnBatchSize,
      encodingEnabled,
      offHeapColumnVectorEnabled,
      dataSize)
  override def supportsRelocationOfSerializedObjects: Boolean = true
}

private class RssColumnarBatchSerializerInstance(
    schema: StructType,
    columnBatchSize: Int,
    encodingEnabled: Boolean,
    offHeapColumnVectorEnabled: Boolean,
    dataSize: SQLMetric) extends SerializerInstance {

  override def serializeStream(out: OutputStream): SerializationStream = new SerializationStream {
    private[this] var writeBuffer: Array[Byte] = new Array[Byte](4096)
    private[this] val dOut: DataOutputStream =
      new DataOutputStream(new BufferedOutputStream(out))

    override def writeValue[T: ClassTag](value: T): SerializationStream = {
      val row = value.asInstanceOf[UnsafeRow]
      if (dataSize != null) {
        dataSize.add(row.getSizeInBytes)
      }
      dOut.writeInt(row.getSizeInBytes)
      row.writeToStream(dOut, writeBuffer)
      this
    }

    override def writeKey[T: ClassTag](key: T): SerializationStream = {
      assert(null == key || key.isInstanceOf[Int])
      this
    }

    override def writeAll[T: ClassTag](iter: Iterator[T]): SerializationStream = {
      throw new UnsupportedOperationException
    }

    override def writeObject[T: ClassTag](t: T): SerializationStream = {
      throw new UnsupportedOperationException
    }

    override def flush(): Unit = {
      dOut.flush()
    }

    override def close(): Unit = {
      writeBuffer = null
      dOut.close()
    }
  }

  val toUnsafe: UnsafeProjection = UnsafeProjection.create(schema.fields.map(f => f.dataType))

  override def deserializeStream(in: InputStream): DeserializationStream = {
    val numFields = schema.fields.length
    new DeserializationStream {
      val dIn: DataInputStream = new DataInputStream(new BufferedInputStream(in))
      val EOF: Int = -1
      var colBuffer: Array[Byte] = new Array[Byte](1024)
      var numRows: Int = readSize()
      var rowIter: Iterator[InternalRow] = if (numRows != EOF) nextBatch() else Iterator.empty

      override def asKeyValueIterator: Iterator[(Int, InternalRow)] = {
        new Iterator[(Int, InternalRow)] {

          override def hasNext: Boolean = rowIter.hasNext || {
            if (numRows != EOF) {
              rowIter = nextBatch()
              true
            } else {
              false
            }
          }

          override def next(): (Int, InternalRow) = {
            (0, rowIter.next())
          }
        }
      }

      override def asIterator: Iterator[Any] = {
        throw new UnsupportedOperationException
      }

      override def readObject[T: ClassTag](): T = {
        throw new UnsupportedOperationException
      }

      def nextBatch(): Iterator[InternalRow] = {
        val columnVectors =
          if (!offHeapColumnVectorEnabled) {
            OnHeapColumnVector.allocateColumns(numRows, schema)
          } else {
            OffHeapColumnVector.allocateColumns(numRows, schema)
          }
        val columnarBatch = new ColumnarBatch(columnVectors.asInstanceOf[Array[ColumnVector]])
        columnarBatch.setNumRows(numRows)

        for (i <- 0 until numFields) {
          val colLen: Int = readSize()
          if (colBuffer.length < colLen) {
            colBuffer = new Array[Byte](colLen)
          }
          ByteStreams.readFully(dIn, colBuffer, 0, colLen)
          RssColumnAccessor.decompress(
            colBuffer,
            columnarBatch.column(i).asInstanceOf[WritableColumnVector],
            schema.fields(i).dataType,
            numRows)
        }
        numRows = readSize()
        columnarBatch.rowIterator().asScala.map(toUnsafe)
      }

      def readSize(): Int =
        try {
          dIn.readInt()
        } catch {
          case e: EOFException =>
            dIn.close()
            EOF
        }

      override def close(): Unit = {
        dIn.close()
      }
    }
  }

  override def serialize[T: ClassTag](t: T): ByteBuffer = throw new UnsupportedOperationException
  override def deserialize[T: ClassTag](bytes: ByteBuffer): T =
    throw new UnsupportedOperationException
  override def deserialize[T: ClassTag](bytes: ByteBuffer, loader: ClassLoader): T =
    throw new UnsupportedOperationException
}
