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

trait HeadExpr extends Expr with AliasableExpr {
  def alias: Option[String] = None
}

case class DataSourceHeadExpr(name: String) extends HeadExpr {
  def desc: String = s"`$name`"
  def coalesceDesc: String = desc
}

case class FieldNameHeadExpr(field: String) extends HeadExpr {
  def desc: String = s"`$field`"
  def coalesceDesc: String = desc
  override def alias: Option[String] = Some(field)
}

case class AllSelectHeadExpr() extends HeadExpr {
  def desc: String = "*"
  def coalesceDesc: String = desc
}

case class OtherHeadExpr(expr: Expr) extends HeadExpr {

  addChild(expr)

  def desc: String = expr.desc
  def coalesceDesc: String = expr.coalesceDesc
  override def alias: Option[String] = Some(expr.desc)

  override def map(func: Expr => Expr): OtherHeadExpr = {
    OtherHeadExpr(func(expr))
  }
}

// -------------

trait SelectExpr extends Expr with AliasableExpr {}

case class AllFieldsSelectExpr() extends SelectExpr {
  def desc: String = ".*"
  def coalesceDesc: String = desc
  def alias: Option[String] = None
}

case class FieldSelectExpr(field: String) extends SelectExpr {
  def desc: String = s".`$field`"
  def coalesceDesc: String = desc
  override def alias: Option[String] = Some(field)
}

case class IndexSelectExpr(index: Expr) extends SelectExpr {

  addChild(index)

  def desc: String = s"[${index.desc}]"
  def coalesceDesc: String = desc
  def alias: Option[String] = Some(index.desc)

  override def map(func: Expr => Expr): IndexSelectExpr = {
    IndexSelectExpr(func(index))
  }
}

case class FunctionSelectExpr(functionName: String, args: Seq[Expr]) extends SelectExpr {

  addChildren(args)

  def desc: String = ""
  def coalesceDesc: String = desc
  def alias: Option[String] = Some(functionName)

  override def map(func: Expr => Expr): FunctionSelectExpr = {
    FunctionSelectExpr(functionName, args.map(func(_)))
  }
}

// -------------

case class SelectionExpr(head: HeadExpr, selectors: Seq[SelectExpr], aliasOpt: Option[String])
    extends SelectExpr {

  addChildren(head +: selectors)

  def desc: String = {
    selectors.foldLeft(head.desc) { (hd, sel) =>
      sel match {
        case FunctionSelectExpr(funcName, args) =>
          val nargs = hd +: args.map(_.desc)
          s"$funcName(${nargs.mkString(", ")})"
        case _ => s"$hd${sel.desc}"
      }
    }
  }
  def coalesceDesc: String = {
    selectors.lastOption match {
      case None => desc
      case Some(_: FunctionSelectExpr) => desc
      case _ => s"coalesce($desc, '')"
    }
  }
  def alias: Option[String] = {
    if (aliasOpt.isEmpty) {
      val aliasSeq = (head +: selectors).flatMap(_.alias)
      if (aliasSeq.nonEmpty) Some(aliasSeq.mkString("_")) else None
    } else aliasOpt
  }

  override def map(func: Expr => Expr): SelectionExpr = {
    SelectionExpr(
      func(head).asInstanceOf[HeadExpr],
      selectors.map(func(_).asInstanceOf[SelectExpr]),
      aliasOpt)
  }
}
