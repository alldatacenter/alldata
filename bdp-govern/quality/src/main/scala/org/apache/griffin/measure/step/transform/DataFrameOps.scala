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

package org.apache.griffin.measure.step.transform

import java.util.Date

import org.apache.spark.sql.{Encoders, Row, _}
import org.apache.spark.sql.types._

import org.apache.griffin.measure.context.ContextId
import org.apache.griffin.measure.context.streaming.metric._
import org.apache.griffin.measure.context.streaming.metric.CacheResults.CacheResult
import org.apache.griffin.measure.step.builder.ConstantColumns
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * pre-defined data frame operations
 */
object DataFrameOps {

  final val _fromJson = "from_json"
  final val _accuracy = "accuracy"
  final val _clear = "clear"

  object AccuracyOprKeys {
    val _dfName = "df.name"
    val _miss = "miss"
    val _total = "total"
    val _matched = "matched"
    val _matchedFraction = "matchedFraction"
  }

  def fromJson(
      sparkSession: SparkSession,
      inputDfName: String,
      details: Map[String, Any]): DataFrame = {
    val _colName = "col.name"
    val colNameOpt = details.get(_colName).map(_.toString)

    implicit val encoder: Encoder[String] = Encoders.STRING

    val df: DataFrame = sparkSession.table(s"`$inputDfName`")
    val rdd = colNameOpt match {
      case Some(colName: String) => df.map(r => r.getAs[String](colName))
      case _ => df.map(_.getAs[String](0))
    }
    sparkSession.read.json(rdd) // slow process
  }

  def accuracy(
      sparkSession: SparkSession,
      inputDfName: String,
      contextId: ContextId,
      details: Map[String, Any]): DataFrame = {
    import AccuracyOprKeys._

    val miss = details.getStringOrKey(_miss)
    val total = details.getStringOrKey(_total)
    val matched = details.getStringOrKey(_matched)
    val matchedFraction = details.getStringOrKey(_matchedFraction)

    val updateTime = new Date().getTime

    def getLong(r: Row, k: String): Option[Long] = {
      try {
        Some(r.getAs[Long](k))
      } catch {
        case _: Throwable => None
      }
    }

    val df = sparkSession.table(s"`$inputDfName`")

    val results = df.rdd.flatMap { row =>
      try {
        val tmst = getLong(row, ConstantColumns.tmst).getOrElse(contextId.timestamp)
        val missCount = getLong(row, miss).getOrElse(0L)
        val totalCount = getLong(row, total).getOrElse(0L)
        val ar = AccuracyMetric(missCount, totalCount)
        if (ar.isLegal) Some((tmst, ar)) else None
      } catch {
        case _: Throwable => None
      }
    }.collect

    // cache and update results
    val updatedResults = CacheResults.update(results.map { pair =>
      val (t, r) = pair
      CacheResult(t, updateTime, r)
    })

    // generate metrics
    val schema = StructType(
      Array(
        StructField(ConstantColumns.tmst, LongType),
        StructField(miss, LongType),
        StructField(total, LongType),
        StructField(matched, LongType),
        StructField(matchedFraction, DoubleType),
        StructField(ConstantColumns.record, BooleanType),
        StructField(ConstantColumns.empty, BooleanType)))
    val rows = updatedResults.map { r =>
      val ar = r.result.asInstanceOf[AccuracyMetric]
      Row(
        r.timeStamp,
        ar.miss,
        ar.total,
        ar.getMatch,
        ar.matchFraction,
        !ar.initial,
        ar.eventual())
    }.toArray
    val rowRdd = sparkSession.sparkContext.parallelize(rows)
    val retDf = sparkSession.createDataFrame(rowRdd, schema)

    retDf
  }

  def clear(
      sparkSession: SparkSession,
      inputDfName: String,
      details: Map[String, Any]): DataFrame = {
    val df = sparkSession.table(s"`$inputDfName`")
    val emptyRdd = sparkSession.sparkContext.emptyRDD[Row]
    sparkSession.createDataFrame(emptyRdd, df.schema)
  }

}
