package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.table.ArcticSparkTable
import org.apache.spark.SparkException
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.{SupportsOverwrite, SupportsTruncate}
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.datasources.v2.{ArcticTableWriteExec, BatchWriteHelper}
import org.apache.spark.sql.sources.{AlwaysTrue, Filter}
import org.apache.spark.sql.util.CaseInsensitiveStringMap

case class OverwriteArcticByExpressionExec(table: ArcticSparkTable,
                                           deleteWhere: Array[Filter],
                                           writeOptions: CaseInsensitiveStringMap,
                                           queryInsert: SparkPlan,
                                           validateQuery: SparkPlan,
                                           refreshCache: () => Unit) extends ArcticTableWriteExec with BatchWriteHelper {

  private def isTruncate(filters: Array[Filter]): Boolean = {
    filters.length == 1 && filters(0).isInstanceOf[AlwaysTrue]
  }

  override protected def run(): Seq[InternalRow] = {
    validateData()
    val writtenRows = newWriteBuilder() match {
      case builder: SupportsTruncate if isTruncate(deleteWhere) =>
        writeInsert(builder.truncate().buildForBatch())

      case builder: SupportsOverwrite =>
        writeInsert(builder.overwrite(deleteWhere).buildForBatch())
      case _ =>
        throw new SparkException(s"Table does not support dynamic partition overwrite: $table")
    }
    refreshCache()
    writtenRows
  }


  override def query: SparkPlan = queryInsert

  override def left: SparkPlan = queryInsert

  override def right: SparkPlan = validateQuery
}
