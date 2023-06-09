/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.catalyst.expressions

import org.apache.iceberg.spark.SparkSchemaUtil
import org.apache.iceberg.transforms.Transforms
import org.apache.iceberg.types.{Type, Types}
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.catalyst.expressions.{Expression, NullIntolerant, UnaryExpression}
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

import java.nio.ByteBuffer


case class BucketExpression(numBuckets: Int, child: Expression)
  extends UnaryExpression with CodegenFallback with NullIntolerant {

  override def nullable: Boolean = child.nullable

  @transient lazy val icebergInputType: Type = SparkSchemaUtil.convert(child.dataType)

  @transient lazy val bucketFunc: Any => Int = child.dataType match {
    case _: DecimalType =>
      val t = Transforms.bucket[Any](icebergInputType, numBuckets)
      d: Any => t(d.asInstanceOf[Decimal].toJavaBigDecimal).toInt
    case _: StringType =>
      // the spec requires that the hash of a string is equal to the hash of its UTF-8 encoded bytes
      // TODO: pass bytes without the copy out of the InternalRow
      val t = Transforms.bucket[ByteBuffer](Types.BinaryType.get(), numBuckets)
      s: Any => t(ByteBuffer.wrap(s.asInstanceOf[UTF8String].getBytes)).toInt
    case _ =>
      val t = Transforms.bucket[Any](icebergInputType, numBuckets)
      a: Any => t(a).toInt
  }

  override protected def nullSafeEval(value: Any): Any = {
    bucketFunc(value)
  }

  override def dataType: DataType = IntegerType
}
