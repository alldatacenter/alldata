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

package org.apache.griffin.measure.step.builder.dsl.transform.analyzer

import org.apache.griffin.measure.step.builder.dsl.expr._

case class CompletenessAnalyzer(expr: CompletenessClause, sourceName: String)
    extends BasicAnalyzer {

  val seqAlias: (Expr, Seq[String]) => Seq[String] = (expr: Expr, v: Seq[String]) => {
    expr match {
      case apr: AliasableExpr => v ++ apr.alias
      case _ => v
    }
  }
  val combAlias: (Seq[String], Seq[String]) => Seq[String] = (a: Seq[String], b: Seq[String]) =>
    a ++ b

  private val exprs = expr.exprs
  private def genAlias(idx: Int): String = s"alias_$idx"
  val selectionPairs: Seq[(Expr, String)] = exprs.zipWithIndex.map { pair =>
    val (pr, idx) = pair
    val res = pr.preOrderTraverseDepthFirst(Seq[String]())(seqAlias, combAlias)
    (pr, res.headOption.getOrElse(genAlias(idx)))
  }

  if (selectionPairs.isEmpty) {
    throw new Exception("completeness analyzer error: empty selection")
  }

}
