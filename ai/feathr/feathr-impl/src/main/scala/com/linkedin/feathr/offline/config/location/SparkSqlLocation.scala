package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.module.caseclass.annotation.CaseClassDeserialize
import com.linkedin.feathr.common.Header
import org.apache.spark.sql.{DataFrame, SparkSession}

@CaseClassDeserialize()
case class SparkSqlLocation(sql: Option[String] = None, table: Option[String] = None) extends DataLocation {
  /**
   * Backward Compatibility
   * Many existing codes expect a simple path
   *
   * @return the `path` or `url` of the data source
   *
   *         WARN: This method is deprecated, you must use match/case on InputLocation,
   *         and get `path` from `SimplePath` only
   */
  override def getPath: String = sql.getOrElse(table.getOrElse(""))

  /**
   * Backward Compatibility
   *
   * @return the `path` or `url` of the data source, wrapped in an List
   *
   *         WARN: This method is deprecated, you must use match/case on InputLocation,
   *         and get `paths` from `PathList` only
   */
  override def getPathList: List[String] = List(getPath)

  /**
   * Load DataFrame from Spark session
   *
   * @param ss SparkSession
   * @return
   */
  override def loadDf(ss: SparkSession, dataIOParameters: Map[String, String]): DataFrame = {
    sql match {
      case Some(sql) => {
        ss.sql(sql)
      }
      case None => table match {
        case Some(table) => {
          ss.sqlContext.table(table)
        }
        case None => {
          throw new IllegalArgumentException("`sql` and `table` parameter should not both be empty")
        }
      }
    }
  }

  /**
   * Write DataFrame to the location
   *
   * NOTE: SparkSqlLocation doesn't support writing, it cannot be used as data sink
   *
   * @param ss SparkSession
   * @param df DataFrame to write
   */
  override def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header]): Unit = ???

  /**
   * Tell if this location is file based
   *
   * @return boolean
   */
  override def isFileBasedLocation(): Boolean = false
}

