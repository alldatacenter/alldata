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
// this file copy form apache spark
package com.netease.arctic.spark.sql.utils

import org.apache.spark.sql.catalyst
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.connector.expressions.{Literal, NamedReference, Transform}
import org.apache.spark.sql.types.{DataType, IntegerType, StringType}

/**
 * Helper methods for working with the logical expressions API.
 *
 * Factory methods can be used when referencing the logical expression nodes is ambiguous because
 * logical and internal expressions are used.
 */
object LogicalExpressions {
  def parseReference(name: String): NamedReference =
    FieldReference(Seq(name))
}

case class FieldReference(parts: Seq[String]) extends NamedReference {
  override def fieldNames: Array[String] = parts.toArray

  override def describe: String = parts.map(quoteIfNeeded).mkString(".")

  override def toString: String = describe

  def quoteIfNeeded(part: String): String = {
    if (part.contains(".") || part.contains("`")) {
      s"`${part.replace("`", "``")}`"
    } else {
      part
    }
  }
}

object FieldReference {
  def apply(column: String): NamedReference = {
    LogicalExpressions.parseReference(column)
  }
}
