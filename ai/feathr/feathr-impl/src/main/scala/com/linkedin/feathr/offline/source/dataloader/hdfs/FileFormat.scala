package com.linkedin.feathr.offline.source.dataloader.hdfs

import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.offline.source.dataloader._
import com.linkedin.feathr.offline.source.dataloader.jdbc.{JdbcUtils, SnowflakeUtils}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import com.linkedin.feathr.offline.util.DelimiterUtils.checkDelimiterOption


object FileFormat {
  private lazy val ss = SparkSession.builder.getOrCreate

  // The each of the following are corresponding to one File format
  // Please update when new Format is supported
  val CSV = "CSV"
  // This type is used for local test scenario of AVRO data source
  val AVRO_JSON = "AVRO_JSON"
  val AVRO = "AVRO"
  val ORC = "ORC"
  val PARQUET = "PARQUET"
  // Path list concept is used in SourceUtils to treat source as a list of path
  val PATHLIST = "PATHLIST"
  // Detail JDBC Sql Type, please refer to dataloader.jdbc.SqlDbType
  val JDBC = "JDBC"
  // Snowflake type
  val SNOWFLAKE = "SNOWFLAKE"

  private val AVRO_DATASOURCE = "avro"
  // Use Spark native orc reader instead of hive-orc since Spark 2.3
  private val ORC_DATASOURCE = "orc"

  val DATA_FORMAT = "data.format"

  /**
   * To define if the file is JDBC, Single File or Path list (default)
   * @param path
   * @return
   */
  def getType(path: String): String = {
    val p = path.toLowerCase()
    p match {
      case p if p.endsWith(".csv") => CSV
      case p if p.endsWith(".parquet") => PARQUET
      case p if p.endsWith(".orc") => ORC
      case p if p.endsWith(".avro.json") => AVRO_JSON
      case p if p.endsWith(".avro") => AVRO
      case p if p.startsWith("jdbc:") => JDBC
      case p if p.startsWith("snowflake:") => SNOWFLAKE
      case _ =>
        // if we cannot tell the file format from the file extensions, we should read from `spark.feathr.inputFormat` to get the format that's sepcified by user.
        if (ss.conf.get("spark.feathr.inputFormat","").nonEmpty) ss.conf.get("spark.feathr.inputFormat") else PATHLIST
    }
  }

  // TODO: Complete a general loadDataFrame and replace current adhoc load data frame code
  def loadDataFrame(ss: SparkSession, path: String, format: String = CSV): DataFrame = {

    // Get csvDelimiterOption set with spark.feathr.inputFormat.csvOptions.sep and check if it is set properly (Only for CSV and TSV)
    val csvDelimiterOption = checkDelimiterOption(ss.sqlContext.getConf("spark.feathr.inputFormat.csvOptions.sep", ","))

    format match {
      case AVRO => new AvroJsonDataLoader(ss, path).loadDataFrame()
      case CSV => ss.read.format("csv").option("header", "true").option("delimiter", csvDelimiterOption).load(path)
      case PARQUET => new ParquetDataLoader(ss, path).loadDataFrame()
      case _ => ???
    }
  }

  // TODO: How can we merge below 2 functions into the general logic? They are refactored from SparkIOUtils
  // existingHdfsPaths may be folder or file with suffix
  // Currently only support parquet file but not folder with parquet files
  def getTypeForUnionDF(existingHdfsPaths: Seq[String], dataIOParameters: Map[String, String] = Map()): String = {
    // if we cannot detect the file type by extension, we will detect "spark.feathr.inputFormat" and use that as the option;
    // this is a global config (i.e. affecting all the inputs) so customers should use it as the last resort.
    // If this is not set, throw an exception (in `loadHdfsDataFrame()`)
    val p = existingHdfsPaths.head.toLowerCase()
    p match {
      case p if p.endsWith(".csv") => CSV
      // Tab-separated Format will be treated as CSV (Enum) here but with tab as the delimiter
      case p if p.endsWith(".tsv") => CSV
      case p if p.endsWith(".parquet") => PARQUET
      case p if p.endsWith(".orc") => ORC
      case p if p.endsWith(".avro.json") => AVRO_JSON
      case p if p.endsWith(".avro") => AVRO
      case p if p.startsWith("jdbc:") => JDBC
      case p if p.startsWith("snowflake:") => SNOWFLAKE
      case _ =>
        // if we cannot tell the file format from the file extensions, we should read from `spark.feathr.inputFormat` to get the format that's sepcified by user.
        dataIOParameters.getOrElse(DATA_FORMAT, ss.conf.get("spark.feathr.inputFormat", AVRO)).toUpperCase
    }


  }

  def loadHdfsDataFrame(format: String, existingHdfsPaths: Seq[String]): DataFrame = {

    // Get csvDelimiterOption set with spark.feathr.inputFormat.csvOptions.sep and check if it is set properly (Only for CSV and TSV)
    val csvDelimiterOption = checkDelimiterOption(ss.sqlContext.getConf("spark.feathr.inputFormat.csvOptions.sep", ","))

    val df = format match {
      case CSV =>
        ss.read.format("csv").option("header", "true").option("delimiter", csvDelimiterOption).load(existingHdfsPaths: _*)
      case AVRO =>
        ss.read.format(AVRO_DATASOURCE).load(existingHdfsPaths: _*)
      case ORC =>
        ss.read.format(ORC_DATASOURCE).load(existingHdfsPaths: _*)
      case PARQUET =>
        ss.read.format(PARQUET).load(existingHdfsPaths: _*)
      case JDBC =>
        // TODO: We should stop using JDBC URL as simple path, otherwise the code will be full of such hack
        JdbcUtils.loadDataFrame(ss, existingHdfsPaths.head)
      case SNOWFLAKE =>
        SnowflakeUtils.loadDataFrame(ss, existingHdfsPaths.head)
      case _ =>
        // Allow dynamic config of the file format if users want to use one
        if (ss.conf.getOption("spark.feathr.inputFormat").nonEmpty) ss.read.format(ss.conf.get("spark.feathr.inputFormat")).load(existingHdfsPaths: _*)
        else throw new FeathrException(s"Unsupported data format $format and 'spark.feathr.inputFormat' not set.")
    }
    df
  }
}