package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.source.ArcticSource
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.execution.command.RunnableCommand
import org.apache.spark.sql.internal.StaticSQLConf
import org.apache.spark.sql.{Row, SparkSession}

case class DropArcticTableCommand(arctic: ArcticSource, tableIdentifier: TableIdentifier, ignoreIfExists: Boolean, purge: Boolean)
  extends RunnableCommand {
  override def run(sparkSession: SparkSession): Seq[Row] = {
    val spark = SparkSession.getActiveSession.get
    val sparkCatalogImpl = spark.conf.get(StaticSQLConf.CATALOG_IMPLEMENTATION.key)
    if (!"hive".equalsIgnoreCase(sparkCatalogImpl)) {
      throw AnalysisException.message(s"failed to create table ${tableIdentifier} not use hive catalog")
    }
    arctic.dropTable(tableIdentifier, purge)
    Seq.empty[Row]
  }
}
