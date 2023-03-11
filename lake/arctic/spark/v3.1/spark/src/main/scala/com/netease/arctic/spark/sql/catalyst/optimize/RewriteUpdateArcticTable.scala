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

package com.netease.arctic.spark.sql.catalyst.optimize

import com.netease.arctic.spark.sql.ArcticExtensionUtils.{ArcticTableHelper, asTableRelation, isArcticRelation}
import com.netease.arctic.spark.sql.catalyst.plans.ReplaceArcticData
import com.netease.arctic.spark.sql.utils.ArcticRewriteHelper
import com.netease.arctic.spark.table.{ArcticSparkTable, SupportsExtendIdentColumns, SupportsUpsert}
import com.netease.arctic.spark.writer.WriteMode
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.{Alias, And, AttributeReference, Cast, EqualTo, Expression, Literal}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.v2.{DataSourceV2Relation, DataSourceV2ScanRelation}
import org.apache.spark.sql.types.StructType

import java.util


/**
 * rewrite update table plan as append upsert data.
 */
case class RewriteUpdateArcticTable(spark: SparkSession) extends Rule[LogicalPlan] with ArcticRewriteHelper{

  override def apply(plan: LogicalPlan): LogicalPlan = plan match {
    case u: UpdateTable if isArcticRelation(u.table) =>
      val arcticRelation = asTableRelation(u.table)
      val upsertWrite = arcticRelation.table.asUpsertWrite
      val scanBuilder = upsertWrite.newUpsertScanBuilder(arcticRelation.options)
      if (u.condition.isEmpty) {
        val cond = Literal.TrueLiteral
        pushFilter(scanBuilder, cond, arcticRelation.output)
      } else {
        pushFilter(scanBuilder, u.condition.get, arcticRelation.output)
      }
      val upsertQuery = buildUpsertQuery(arcticRelation, upsertWrite, scanBuilder, u.assignments, u.condition)
      var query = upsertQuery
      var options: Map[String, String] = Map.empty
      options +=(WriteMode.WRITE_MODE_KEY -> WriteMode.UPSERT.toString)
      ReplaceArcticData(arcticRelation, query, options)

    case _ => plan
  }

  def buildUpsertQuery(r: DataSourceV2Relation, upsert: SupportsUpsert, scanBuilder: SupportsExtendIdentColumns,
                       assignments: Seq[Assignment],
                       condition: Option[Expression]): LogicalPlan = {
    r.table match {
      case table: ArcticSparkTable => {
        if (table.table().isUnkeyedTable) {
          if (upsert.requireAdditionIdentifierColumns()) {
            scanBuilder.withIdentifierColumns()
          }
        }
      }
    }
    val scan = scanBuilder.build()
    val outputAttr = toOutputAttrs(scan.readSchema(), r.output)
    val valuesRelation = DataSourceV2ScanRelation(r, scan, outputAttr)
    val matchedRowsQuery = if (condition.isDefined) {
      Filter(condition.get, valuesRelation)
    } else {
      valuesRelation
    }
    r.table match {
      case a: ArcticSparkTable =>
        if (a.table().isKeyedTable) {
          val updatedRowsQuery = buildKeyedTableUpdateInsertProjection(valuesRelation, matchedRowsQuery, assignments)
          val primaries = a.table().asKeyedTable().primaryKeySpec().fieldNames()
          validatePrimaryKey(primaries, assignments)
          updatedRowsQuery
        } else {
          val updatedRowsQuery = buildUnKeyedTableUpdateInsertProjection(valuesRelation, matchedRowsQuery, assignments)
          val deleteQuery = Project(Seq(Alias(Literal(SupportsUpsert.UPSERT_OP_VALUE_DELETE), SupportsUpsert.UPSERT_OP_COLUMN_NAME)())
            ++ matchedRowsQuery.output.iterator,
            matchedRowsQuery)
          val insertQuery = Project(Seq(Alias(Literal(SupportsUpsert.UPSERT_OP_VALUE_INSERT), SupportsUpsert.UPSERT_OP_COLUMN_NAME)())
            ++ updatedRowsQuery.output.iterator,
            updatedRowsQuery)
          Union(deleteQuery, insertQuery)
        }
    }
  }

  def validatePrimaryKey(primaries: util.List[String], assignments: Seq[Assignment]): Unit = {
    assignments.map(_.key).foreach(
      f => {
        val name = f.asInstanceOf[AttributeReference].name
        if (primaries.contains(name)) {
          throw new UnsupportedOperationException(s"primary key: ${name} can not be updated")
        }
      }
    )
  }

  protected def toOutputAttrs(schema: StructType, attrs: Seq[AttributeReference]): Seq[AttributeReference] = {
    val nameToAttr = attrs.map(_.name).zip(attrs).toMap
    schema.map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)()).map {
      a =>
        nameToAttr.get(a.name) match {
          case Some(ref) =>
            // keep the attribute id if it was present in the relation
            a.withExprId(ref.exprId)
          case _ =>
            // if the field is new, create a new attribute
            AttributeReference(a.name, a.dataType, a.nullable, a.metadata)()
        }
    }
  }

  private def buildKeyedTableUpdateInsertProjection(relation: LogicalPlan,
                                    scanPlan: LogicalPlan,
                                    assignments: Seq[Assignment]): LogicalPlan = {
    val output = relation.output
    val assignmentMap = assignments.map(
      a =>
        if (a.value.dataType.catalogString.equals(a.key.dataType.catalogString)) {
          a.key.asInstanceOf[AttributeReference].name -> a.value
        } else {
          a.key.asInstanceOf[AttributeReference].name -> Cast(a.value, a.key.dataType)
        }
    ).toMap
    val outputWithValues = relation.output ++ output.map( a => {
      if(assignmentMap.contains(a.name)) {
        Alias(assignmentMap(a.name), "_arctic_after_" + a.name)()
      } else {
        Alias(a, "_arctic_after_" + a.name)()
      }
    })
    Project(outputWithValues, scanPlan)
  }

  private def buildUnKeyedTableUpdateInsertProjection(relation: LogicalPlan,
                                                    scanPlan: LogicalPlan,
                                                    assignments: Seq[Assignment]): LogicalPlan = {
    val output = relation.output
    val assignmentMap = assignments.map(
      a =>
        if (a.value.dataType.catalogString.equals(a.key.dataType.catalogString)) {
          a.key.asInstanceOf[AttributeReference].name -> a.value
        } else {
          a.key.asInstanceOf[AttributeReference].name -> Cast(a.value, a.key.dataType)
        }
    ).toMap
    val outputWithValues = output.map(a => {
      if (assignmentMap.contains(a.name)) {
        Alias(assignmentMap(a.name), a.name)()
      } else {
        a
      }
    })
    Project(outputWithValues, scanPlan)
  }

  def buildJoinCondition(primaries: util.List[String], r: DataSourceV2Relation, insertPlan: LogicalPlan): Expression =  {
    var i = 0
    var joinCondition: Expression = null
    val expressions = new util.ArrayList[Expression]
    while ( i < primaries.size) {
      val primary = primaries.get(i)
      val primaryAttr = r.output.find(_.name == primary).get
      val joinAttribute = insertPlan.output.find(_.name.replace("_arctic_after_", "") == primary).get
      val experssion = EqualTo(primaryAttr, joinAttribute)
      expressions.add(experssion)
      i += 1
    }
    expressions.forEach(e => {
      if (joinCondition == null) {
        joinCondition = e
      } else {
        joinCondition = And(joinCondition, e)
      }
    });
    joinCondition
  }

}
