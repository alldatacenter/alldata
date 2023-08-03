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

package com.netease.arctic.spark.sql.utils

import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.expressions.{AttributeReference, AttributeSet, Expression, PredicateHelper}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.execution.datasources.v2.PushDownUtils

trait ArcticRewriteHelper extends PredicateHelper with Logging {
  def pushFilter(
      scanBuilder: ScanBuilder,
      cond: Expression,
      tableAttrs: Seq[AttributeReference]): Unit = {
    val predicates = extractFilters(cond, tableAttrs)
    if (predicates.nonEmpty) {
      val normalizedPredicates = normalizeExprs(predicates, tableAttrs)
      PushDownUtils.pushFilters(scanBuilder, normalizedPredicates)
    }
  }

  private def extractFilters(
      cond: Expression,
      tableAttrs: Seq[AttributeReference]): Seq[Expression] = {
    val tableAttrSet = AttributeSet(tableAttrs)
    splitConjunctivePredicates(cond).filter(_.references.subsetOf(tableAttrSet))
  }

  def normalizeExprs(
      exprs: Seq[Expression],
      attributes: Seq[AttributeReference]): Seq[Expression] = {
    exprs.map { e =>
      e transform {
        case a: AttributeReference =>
          a.withName(attributes.find(_.semanticEquals(a)).getOrElse(a).name)
      }
    }
  }
}
