package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.annotation.{JsonAlias, JsonIgnoreProperties}
import com.fasterxml.jackson.module.caseclass.annotation.CaseClassDeserialize
import com.linkedin.feathr.common.Header
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.codehaus.jackson.annotate.JsonProperty
import com.linkedin.feathr.offline.generation.SparkIOUtils
import org.apache.hadoop.mapred.JobConf

/**
 * Snowflake source config.
 * Example:
 *  snowflakeBatchSource: {
      type: SNOWFLAKE
      config: {
        dbtable: "SNOWFLAKE_TABLE"
        database: "SNOWFLAKE_DB"
        schema: "SNOWFLAKE_SCHEMA"
      }
    }
 *
 *
 */
@CaseClassDeserialize()
@JsonIgnoreProperties(ignoreUnknown = true)
case class Snowflake(@JsonProperty("database") database: String,
                     @JsonProperty("schema") schema: String,
                     @JsonProperty("dbtable") dbtable: String = "",
                     @JsonProperty("query") query: String = "") extends DataLocation {

  override def loadDf(ss: SparkSession, dataIOParameters: Map[String, String] = Map()): DataFrame = {
    SparkIOUtils.createUnionDataFrame(getPathList, dataIOParameters, new JobConf(), List())
  }

  override def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header]): Unit = ???

  override def getPath: String = {
    val baseUrl = s"snowflake://snowflake_account/?sfDatabase=${database}&sfSchema=${schema}"
    if (dbtable.isEmpty) {
      baseUrl + s"&query=${query}"
    } else {
      baseUrl + s"&dbtable=${dbtable}"
    }
  }

  override def getPathList: List[String] = List(getPath)

  override def isFileBasedLocation(): Boolean = false
}

object Snowflake {
  /**
   * Create Snowflake InputLocation with required info
   *
   * @param database
   * @param schema
   * @param dbtable
   * @param query
   * @return Newly created InputLocation instance
   */
  def apply(database: String, schema: String, dbtable: String, query: String): Snowflake = Snowflake(database, schema, dbtable=dbtable, query=query)

}