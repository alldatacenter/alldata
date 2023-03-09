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

package org.apache.griffin.measure.step.builder.dsl.expr

trait ClauseExpression extends Expr {}

case class SelectClause(exprs: Seq[Expr], extraConditionOpt: Option[ExtraConditionExpr])
    extends ClauseExpression {

  addChildren(exprs)

  def desc: String = {
    extraConditionOpt match {
      case Some(cdtn) => s"${cdtn.desc} ${exprs.map(_.desc).mkString(", ")}"
      case _ => s"${exprs.map(_.desc).mkString(", ")}"
    }
  }
  def coalesceDesc: String = desc

  override def map(func: Expr => Expr): SelectClause = {
    SelectClause(
      exprs.map(func(_)),
      extraConditionOpt.map(func(_).asInstanceOf[ExtraConditionExpr]))
  }

}

case class FromClause(dataSource: String) extends ClauseExpression {

  def desc: String = s"FROM `$dataSource`"
  def coalesceDesc: String = desc

}

case class WhereClause(expr: Expr) extends ClauseExpression {

  addChild(expr)

  def desc: String = s"WHERE ${expr.desc}"
  def coalesceDesc: String = s"WHERE ${expr.coalesceDesc}"

  override def map(func: Expr => Expr): WhereClause = {
    WhereClause(func(expr))
  }

}

case class GroupbyClause(exprs: Seq[Expr], havingClauseOpt: Option[Expr])
    extends ClauseExpression {

  addChildren(exprs ++ havingClauseOpt.toSeq)

  def desc: String = {
    val gbs = exprs.map(_.desc).mkString(", ")
    havingClauseOpt match {
      case Some(having) => s"GROUP BY $gbs HAVING ${having.desc}"
      case _ => s"GROUP BY $gbs"
    }
  }
  def coalesceDesc: String = {
    val gbs = exprs.map(_.desc).mkString(", ")
    havingClauseOpt match {
      case Some(having) => s"GROUP BY $gbs HAVING ${having.coalesceDesc}"
      case _ => s"GROUP BY $gbs"
    }
  }

  def merge(other: GroupbyClause): GroupbyClause = {
    val newHavingClauseOpt = (havingClauseOpt, other.havingClauseOpt) match {
      case (Some(hc), Some(ohc)) =>
        val logical1 = LogicalFactorExpr(hc, withBracket = false, None)
        val logical2 = LogicalFactorExpr(ohc, withBracket = false, None)
        Some(BinaryLogicalExpr(logical1, ("AND", logical2) :: Nil))
      case (a @ Some(_), _) => a
      case (_, b @ Some(_)) => b
      case (_, _) => None
    }
    GroupbyClause(exprs ++ other.exprs, newHavingClauseOpt)
  }

  override def map(func: Expr => Expr): GroupbyClause = {
    GroupbyClause(exprs.map(func(_)), havingClauseOpt.map(func(_)))
  }

}

case class OrderItem(expr: Expr, orderOpt: Option[String]) extends Expr {
  addChild(expr)
  def desc: String = {
    orderOpt match {
      case Some(os) => s"${expr.desc} ${os.toUpperCase}"
      case _ => s"${expr.desc}"
    }
  }
  def coalesceDesc: String = desc

  override def map(func: Expr => Expr): OrderItem = {
    OrderItem(func(expr), orderOpt)
  }
}

case class OrderbyClause(items: Seq[OrderItem]) extends ClauseExpression {

  addChildren(items.map(_.expr))

  def desc: String = {
    val obs = items.map(_.desc).mkString(", ")
    s"ORDER BY $obs"
  }
  def coalesceDesc: String = {
    val obs = items.map(_.desc).mkString(", ")
    s"ORDER BY $obs"
  }

  override def map(func: Expr => Expr): OrderbyClause = {
    OrderbyClause(items.map(func(_).asInstanceOf[OrderItem]))
  }
}

case class SortbyClause(items: Seq[OrderItem]) extends ClauseExpression {

  addChildren(items.map(_.expr))

  def desc: String = {
    val obs = items.map(_.desc).mkString(", ")
    s"SORT BY $obs"
  }
  def coalesceDesc: String = {
    val obs = items.map(_.desc).mkString(", ")
    s"SORT BY $obs"
  }

  override def map(func: Expr => Expr): SortbyClause = {
    SortbyClause(items.map(func(_).asInstanceOf[OrderItem]))
  }
}

case class LimitClause(expr: Expr) extends ClauseExpression {

  addChild(expr)

  def desc: String = s"LIMIT ${expr.desc}"
  def coalesceDesc: String = s"LIMIT ${expr.coalesceDesc}"

  override def map(func: Expr => Expr): LimitClause = {
    LimitClause(func(expr))
  }
}

case class CombinedClause(
    selectClause: SelectClause,
    fromClauseOpt: Option[FromClause],
    tails: Seq[ClauseExpression])
    extends ClauseExpression {

  addChildren({
    val headClauses: Seq[ClauseExpression] = selectClause +: fromClauseOpt.toSeq
    headClauses ++ tails
  })

  def desc: String = {
    val selectDesc = s"SELECT ${selectClause.desc}"
    val fromDesc = fromClauseOpt.map(_.desc).mkString(" ")
    val headDesc = s"$selectDesc $fromDesc"
    tails.foldLeft(headDesc) { (head, tail) =>
      s"$head ${tail.desc}"
    }
  }
  def coalesceDesc: String = {
    val selectDesc = s"SELECT ${selectClause.coalesceDesc}"
    val fromDesc = fromClauseOpt.map(_.coalesceDesc).mkString(" ")
    val headDesc = s"$selectDesc $fromDesc"
    tails.foldLeft(headDesc) { (head, tail) =>
      s"$head ${tail.coalesceDesc}"
    }
  }

  override def map(func: Expr => Expr): CombinedClause = {
    CombinedClause(
      func(selectClause).asInstanceOf[SelectClause],
      fromClauseOpt.map(func(_).asInstanceOf[FromClause]),
      tails.map(func(_).asInstanceOf[ClauseExpression]))
  }
}

case class ProfilingClause(
    selectClause: SelectClause,
    fromClauseOpt: Option[FromClause],
    groupbyClauseOpt: Option[GroupbyClause],
    preGroupbyClauses: Seq[ClauseExpression],
    postGroupbyClauses: Seq[ClauseExpression])
    extends ClauseExpression {
  addChildren({
    val headClauses: Seq[ClauseExpression] = selectClause +: fromClauseOpt.toSeq
    groupbyClauseOpt match {
      case Some(gc) => (headClauses ++ preGroupbyClauses) ++ (gc +: postGroupbyClauses)
      case _ => (headClauses ++ preGroupbyClauses) ++ postGroupbyClauses
    }
  })

  def desc: String = {
    val selectDesc = selectClause.desc
    val fromDesc = fromClauseOpt.map(_.desc).mkString(" ")
    val groupbyDesc = groupbyClauseOpt.map(_.desc).mkString(" ")
    val preDesc = preGroupbyClauses.map(_.desc).mkString(" ")
    val postDesc = postGroupbyClauses.map(_.desc).mkString(" ")
    s"$selectDesc $fromDesc $preDesc $groupbyDesc $postDesc"
  }
  def coalesceDesc: String = {
    val selectDesc = selectClause.coalesceDesc
    val fromDesc = fromClauseOpt.map(_.coalesceDesc).mkString(" ")
    val groupbyDesc = groupbyClauseOpt.map(_.coalesceDesc).mkString(" ")
    val preDesc = preGroupbyClauses.map(_.coalesceDesc).mkString(" ")
    val postDesc = postGroupbyClauses.map(_.coalesceDesc).mkString(" ")
    s"$selectDesc $fromDesc $preDesc $groupbyDesc $postDesc"
  }

  override def map(func: Expr => Expr): ProfilingClause = {
    ProfilingClause(
      func(selectClause).asInstanceOf[SelectClause],
      fromClauseOpt.map(func(_).asInstanceOf[FromClause]),
      groupbyClauseOpt.map(func(_).asInstanceOf[GroupbyClause]),
      preGroupbyClauses.map(func(_).asInstanceOf[ClauseExpression]),
      postGroupbyClauses.map(func(_).asInstanceOf[ClauseExpression]))
  }
}

case class UniquenessClause(exprs: Seq[Expr]) extends ClauseExpression {
  addChildren(exprs)

  def desc: String = exprs.map(_.desc).mkString(", ")
  def coalesceDesc: String = exprs.map(_.coalesceDesc).mkString(", ")
  override def map(func: Expr => Expr): UniquenessClause = UniquenessClause(exprs.map(func(_)))
}

case class DistinctnessClause(exprs: Seq[Expr]) extends ClauseExpression {
  addChildren(exprs)

  def desc: String = exprs.map(_.desc).mkString(", ")
  def coalesceDesc: String = exprs.map(_.coalesceDesc).mkString(", ")
  override def map(func: Expr => Expr): DistinctnessClause =
    DistinctnessClause(exprs.map(func(_)))
}

case class TimelinessClause(exprs: Seq[Expr]) extends ClauseExpression {
  addChildren(exprs)

  def desc: String = exprs.map(_.desc).mkString(", ")
  def coalesceDesc: String = exprs.map(_.coalesceDesc).mkString(", ")
  override def map(func: Expr => Expr): TimelinessClause = TimelinessClause(exprs.map(func(_)))
}

case class CompletenessClause(exprs: Seq[Expr]) extends ClauseExpression {
  addChildren(exprs)

  def desc: String = exprs.map(_.desc).mkString(", ")
  def coalesceDesc: String = exprs.map(_.coalesceDesc).mkString(", ")
  override def map(func: Expr => Expr): CompletenessClause =
    CompletenessClause(exprs.map(func(_)))
}
