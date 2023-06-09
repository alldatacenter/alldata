package com.netease.arctic.spark.sql.execution

import com.netease.arctic.spark.source.ArcticSource
import org.apache.spark.sql.arctic.AnalysisException
import org.apache.spark.sql.catalyst.catalog.{CatalogTable, CatalogTableType}
import org.apache.spark.sql.execution.command.RunnableCommand
import org.apache.spark.sql.internal.StaticSQLConf
import org.apache.spark.sql.{Row, SparkSession}

case class CreateArcticTableCommand(arctic: ArcticSource, catalogTable: CatalogTable, ignoreIfExists: Boolean)
  extends RunnableCommand {
  override def run(sparkSession: SparkSession): Seq[Row] = {
    assert(catalogTable.tableType != CatalogTableType.VIEW)
    assert(catalogTable.provider.isDefined)
    val spark = SparkSession.getActiveSession.get
    val sparkCatalogImpl = spark.conf.get(StaticSQLConf.CATALOG_IMPLEMENTATION.key)
    if (!"hive".equalsIgnoreCase(sparkCatalogImpl)) {
      throw AnalysisException.message(s"failed to create table ${catalogTable.identifier} not use hive catalog")
    }
    arctic.createTable(catalogTable.identifier, catalogTable.schema,
      scala.collection.JavaConversions.seqAsJavaList(catalogTable.partitionColumnNames),
      scala.collection.JavaConversions.mapAsJavaMap(catalogTable.properties))
    Seq.empty[Row]
  }
}
