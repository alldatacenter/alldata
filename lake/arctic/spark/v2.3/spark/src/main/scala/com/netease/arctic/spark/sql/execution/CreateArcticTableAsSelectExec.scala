/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.source.{ArcticSource, ArcticSparkTable, SupportsDynamicOverwrite}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.catalog.{CatalogTable, CatalogTableType}
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.DataWritingSparkTask
import org.apache.spark.sql.internal.StaticSQLConf
import org.apache.spark.sql.sources.v2.writer.{DataSourceWriter, SupportsWriteInternalRow, WriterCommitMessage}
import org.apache.spark.{SparkException, TaskContext}

import scala.util.control.NonFatal

case class CreateArcticTableAsSelectExec(arctic: ArcticSource, tableDesc: CatalogTable, query: SparkPlan) extends SparkPlan {

  override def output: Seq[Attribute] = Nil

  override def children: Seq[SparkPlan] = Seq(query)

  override protected def doExecute(): RDD[InternalRow] = {
    val arcticTable = createArcticTable()

    val optWriter = arcticTable.createWriter("", query.schema, SaveMode.Overwrite, null)
    if (!optWriter.isPresent) {
      throw AnalysisException.message(s"failed to create writer for table ${tableDesc.identifier}")
    }
    val writer = optWriter.get match {
      case w: SupportsDynamicOverwrite =>
        w.overwriteDynamicPartitions()
    }

    writeRows(writer)
    sparkContext.emptyRDD
  }

  def createArcticTable(): ArcticSparkTable = {
    assert(tableDesc.tableType != CatalogTableType.VIEW)
    assert(tableDesc.provider.isDefined)

    val spark = sqlContext.sparkSession
    val sparkCatalogImpl = spark.conf.get(StaticSQLConf.CATALOG_IMPLEMENTATION.key)
    if (!"hive".equalsIgnoreCase(sparkCatalogImpl)) {
      throw AnalysisException.message(s"failed to create table ${tableDesc.identifier} not use hive catalog")
    }
    val table = arctic.createTable(tableDesc.identifier, tableDesc.schema,
      scala.collection.JavaConversions.seqAsJavaList(tableDesc.partitionColumnNames),
      scala.collection.JavaConversions.mapAsJavaMap(tableDesc.properties))
    table
  }

  def writeRows(writer: DataSourceWriter) : Unit = {
    val writeTask = writer match {
      case w: SupportsWriteInternalRow => w.createInternalRowWriterFactory()
      case _ => throw AnalysisException.message(s"failed to create writer for table ${tableDesc.identifier}, " +
        s"not support create internalRow writer factory")
    }

    val rdd = query.execute()
    val messages = new Array[WriterCommitMessage](rdd.partitions.length)
    logInfo(s"Start writer data to: ${tableDesc.identifier}. " +
      s"The input query has ${messages.length} partitions.")

    try {
      val runTask = (context: TaskContext, iter: Iterator[InternalRow]) =>
        DataWritingSparkTask.run(writeTask, context, iter)

      sparkContext.runJob(
        rdd,
        runTask,
        rdd.partitions.indices,
        (index, message: WriterCommitMessage) => messages(index) = message
      )

      writer.commit(messages)
    } catch {
      case cause: Throwable =>
        logError(s"Data source writer $writer is aborting.")
        try {
          writer.abort(messages)
        } catch {
          case t: Throwable =>
            logError(s"Data source writer $writer failed to abort.")
            cause.addSuppressed(t)
            throw new SparkException("Writing job failed.", cause)
        }
        logError(s"Data source writer $writer aborted.")
        cause match {
          // Only wrap non fatal exceptions.
          case NonFatal(e) => throw new SparkException("Writing job aborted.", e)
          case _ => throw cause
        }
    }
  }
}
