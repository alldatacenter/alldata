package com.linkedin.feathr.offline.source.dataloader.jdbc

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * This Utils contains all
 * Custom Spark Config Keys For Snowflake Options
 * Functions to parse Snowflake Configs
 * Basic Function to load dataframe from Snowflake data source
 */
object SnowflakeUtils {

  def loadDataFrame(ss: SparkSession, url: String): DataFrame = {
    val snowflakeLoader = new SnowflakeDataLoader(ss)
    val snowflakeOptions = snowflakeLoader.extractSFOptions(ss, url)
    snowflakeLoader.loadDataFrame(url, snowflakeOptions)
  }
}
