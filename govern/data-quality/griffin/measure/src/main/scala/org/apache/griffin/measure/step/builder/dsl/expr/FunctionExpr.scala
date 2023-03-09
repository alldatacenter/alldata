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

case class FunctionExpr(
    functionName: String,
    args: Seq[Expr],
    extraConditionOpt: Option[ExtraConditionExpr],
    aliasOpt: Option[String])
    extends Expr
    with AliasableExpr {

  addChildren(args)

  def desc: String = {
    extraConditionOpt match {
      case Some(cdtn) => s"$functionName(${cdtn.desc} ${args.map(_.desc).mkString(", ")})"
      case _ => s"$functionName(${args.map(_.desc).mkString(", ")})"
    }
  }
  def coalesceDesc: String = desc
  def alias: Option[String] = {
    if (aliasOpt.isEmpty) {
      Some(functionName)
    } else aliasOpt
  }

  override def map(func: Expr => Expr): FunctionExpr = {
    FunctionExpr(
      functionName,
      args.map(func(_)),
      extraConditionOpt.map(func(_).asInstanceOf[ExtraConditionExpr]),
      aliasOpt)
  }
}
