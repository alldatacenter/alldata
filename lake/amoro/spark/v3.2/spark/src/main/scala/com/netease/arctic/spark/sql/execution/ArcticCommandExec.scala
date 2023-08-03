/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.command.{ArcticSparkCommand, MigrateToArcticCommand}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.{ExpressionEncoder, RowEncoder}
import org.apache.spark.sql.catalyst.expressions.{Attribute, AttributeReference}
import org.apache.spark.sql.catalyst.util.truncatedString
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.V2CommandExec

abstract class ArcticCommandExec(command: ArcticSparkCommand) extends V2CommandExec {

  val rowEncoder: ExpressionEncoder[Row] = RowEncoder(command.outputType()).resolveAndBind()

  override def run(): Seq[InternalRow] = {
    val rows = command.execute().toSeq
//    val encoder = RowEncoder(command.outputType()).resolveAndBind().createSerializer()
    val m = rows.map(r => rowEncoder.createSerializer()(r))
    m
  }

  override def output: Seq[Attribute] = {
    val out = command.outputType().map(f =>
      AttributeReference(f.name, f.dataType, f.nullable, f.metadata)())
    out
  }

  override def simpleString(maxFields: Int): String = {
    s"${command.name()}CommandExec${truncatedString(output, "[", ",", "]", maxFields)} ${command.execInfo}"
  }
}

case class MigrateToArcticExec(command: MigrateToArcticCommand) extends ArcticCommandExec(command) {
  override def children: Seq[SparkPlan] = Nil

  override protected def withNewChildrenInternal(newChildren: IndexedSeq[SparkPlan]): SparkPlan =
    null
}
