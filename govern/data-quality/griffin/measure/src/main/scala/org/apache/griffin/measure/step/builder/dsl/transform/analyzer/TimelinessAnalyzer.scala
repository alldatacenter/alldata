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

case class TimelinessAnalyzer(expr: TimelinessClause, sourceName: String) extends BasicAnalyzer {

//  val tsExpr = expr.desc

//  val seqAlias = (expr: Expr, v: Seq[String]) => {
//    expr match {
//      case apr: AliasableExpr => v ++ apr.alias
//      case _ => v
//    }
//  }
//  val combAlias = (a: Seq[String], b: Seq[String]) => a ++ b
//
//  private val exprs = expr.exprs.toList
//  val selectionPairs = exprs.map { pr =>
//    val res = pr.preOrderTraverseDepthFirst(Seq[String]())(seqAlias, combAlias)
//    println(res)
//    println(pr)
//    (pr, res.headOption)
//  }
//
//  val (tsExprPair, endTsPairOpt) = selectionPairs match {
//    case Nil => throw new Exception(s"timeliness analyzer error: ts column not set")
//    case tsPair :: Nil => (tsPair, None)
//    case tsPair :: endTsPair :: _ => (tsPair, Some(endTsPair))
//  }
//
//  def getSelAlias(pair: (Expr, Option[String]), defAlias: String): (String, String) = {
//    val (pr, aliasOpt) = pair
//    val alias = aliasOpt.getOrElse(defAlias)
//    (pr.desc, alias)
//  }

  private val exprs = expr.exprs.map(_.desc).toList

  val (btsExpr, etsExprOpt) = exprs match {
    case Nil => throw new Exception("timeliness analyzer error: ts column not set")
    case btsExpr :: Nil => (btsExpr, None)
    case btsExpr :: etsExpr :: _ => (btsExpr, Some(etsExpr))
  }

}
