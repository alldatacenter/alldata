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

case class ProfilingAnalyzer(expr: ProfilingClause, sourceName: String) extends BasicAnalyzer {

  val dataSourceNames: Set[String] =
    expr.preOrderTraverseDepthFirst(Set[String]())(seqDataSourceNames, combDataSourceNames)

  val selectionExprs: Seq[Expr] = {
    expr.selectClause.exprs.map(_.extractSelf).flatMap { expr =>
      expr match {
        case e: SelectionExpr => Some(e)
        case e: FunctionExpr => Some(e)
        case _ => None
      }
    }
  }

  val groupbyExprOpt: Option[GroupbyClause] = expr.groupbyClauseOpt
  val preGroupbyExprs: Seq[Expr] = expr.preGroupbyClauses.map(_.extractSelf)
  val postGroupbyExprs: Seq[Expr] = expr.postGroupbyClauses.map(_.extractSelf)

}
