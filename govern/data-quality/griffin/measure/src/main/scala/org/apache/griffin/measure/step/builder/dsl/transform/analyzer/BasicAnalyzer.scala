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

/**
 * analyzer of expr, to help generate dq steps by expr
 */
trait BasicAnalyzer extends Serializable {

  val expr: Expr

  val seqDataSourceNames: (Expr, Set[String]) => Set[String] = (expr: Expr, v: Set[String]) => {
    expr match {
      case DataSourceHeadExpr(name) => v + name
      case _ => v
    }
  }
  val combDataSourceNames: (Set[String], Set[String]) => Set[String] =
    (a: Set[String], b: Set[String]) => a ++ b

  val seqSelectionExprs: String => (Expr, Seq[SelectionExpr]) => Seq[SelectionExpr] =
    (dsName: String) =>
      (expr: Expr, v: Seq[SelectionExpr]) => {
        expr match {
          case se @ SelectionExpr(head: DataSourceHeadExpr, _, _) if head.name == dsName =>
            v :+ se
          case _ => v
        }
      }
  val combSelectionExprs: (Seq[SelectionExpr], Seq[SelectionExpr]) => Seq[SelectionExpr] =
    (a: Seq[SelectionExpr], b: Seq[SelectionExpr]) => a ++ b

  val seqWithAliasExprs: (Expr, Seq[AliasableExpr]) => Seq[AliasableExpr] =
    (expr: Expr, v: Seq[AliasableExpr]) => {
      expr match {
        case _: SelectExpr => v
        case a: AliasableExpr if a.alias.nonEmpty => v :+ a
        case _ => v
      }
    }
  val combWithAliasExprs: (Seq[AliasableExpr], Seq[AliasableExpr]) => Seq[AliasableExpr] =
    (a: Seq[AliasableExpr], b: Seq[AliasableExpr]) => a ++ b

}
