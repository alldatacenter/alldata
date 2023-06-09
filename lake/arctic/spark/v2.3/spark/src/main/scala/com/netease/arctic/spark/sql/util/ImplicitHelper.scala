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

package com.netease.arctic.spark.sql.util


import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.expressions.AttributeReference
import org.apache.spark.sql.types.StructType

object ImplicitHelper {

  implicit class StructTypeImplicitHelper(types: StructType) {
    def toAttributes: Seq[AttributeReference] =
      types.map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)())
  }


  implicit class IdentifierImplicitHelper(parts: Seq[String]) {
    if (parts.isEmpty) {
      throw AnalysisException.message("multi-part identifier cannot be empty.")
    }

    def quoted: String = parts.map(quoteIfNeeded).mkString(".")

  }

  def quoteIfNeeded(part: String): String = {
    if (part.contains(".") || part.contains("`")) {
      s"`${part.replace("`", "``")}`"
    } else {
      part
    }
  }
}
