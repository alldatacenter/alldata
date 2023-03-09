package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.module.caseclass.annotation.CaseClassDeserialize
import com.linkedin.feathr.common.Header
import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.offline.generation.FeatureGenUtils
import com.linkedin.feathr.offline.join.DataFrameKeyCombiner
import net.minidev.json.annotate.JsonIgnore
import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.spark.sql.functions.monotonically_increasing_id
import org.apache.spark.sql.{DataFrame, DataFrameWriter, Row, SparkSession}

@CaseClassDeserialize()
case class GenericLocation(format: String, mode: Option[String] = None) extends DataLocation {
  val log: Logger = LogManager.getLogger(getClass)
  val options: collection.mutable.Map[String, String] = collection.mutable.Map[String, String]()
  val conf: collection.mutable.Map[String, String] = collection.mutable.Map[String, String]()

  /**
   * Backward Compatibility
   * Many existing codes expect a simple path
   *
   * @return the `path` or `url` of the data source
   *
   *         WARN: This method is deprecated, you must use match/case on DataLocation,
   *         and get `path` from `SimplePath` only
   */
  override def getPath: String = s"GenericLocation(${format})"

  /**
   * Backward Compatibility
   *
   * @return the `path` or `url` of the data source, wrapped in an List
   *
   *         WARN: This method is deprecated, you must use match/case on DataLocation,
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
    GenericLocationAdHocPatches.readDf(ss, this)
  }

  /**
   * Write DataFrame to the location
   *
   * @param ss SparkSession
   * @param df DataFrame to write
   */
  override def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header]): Unit = {
    GenericLocationAdHocPatches.writeDf(ss, df, header, this)
  }

  /**
   * Tell if this location is file based
   *
   * @return boolean
   */
  override def isFileBasedLocation(): Boolean = false

  @JsonAnySetter
  def setOption(key: String, value: Any): Unit = {
    println(s"GenericLocation.setOption(key: $key, value: $value)")
    if (key == null) {
      log.warn("Got null key, skipping")
      return
    }
    if (value == null) {
      log.warn(s"Got null value for key '$key', skipping")
      return
    }
    val v = value.toString
    if (v == null) {
      log.warn(s"Got invalid value for key '$key', skipping")
      return
    }
    if (key.startsWith("__conf__")) {
      conf += (key.stripPrefix("__conf__").replace("__", ".") -> LocationUtils.envSubstitute(v))
    } else {
      options += (key.replace("__", ".") -> LocationUtils.envSubstitute(v))
    }
  }
}

/**
 * Some Spark connectors need extra actions before read or write, namely CosmosDb and ElasticSearch
 * Need to run specific ad-hoc patch base on `format`
 */
object GenericLocationAdHocPatches {
  def readDf(ss: SparkSession, location: GenericLocation): DataFrame = {
    location.conf.foreach(e => {
      ss.conf.set(e._1, e._2)
    })

    location.format.toLowerCase() match {
      case "org.elasticsearch.spark.sql" => {
        ss.read.format(location.format)
          .option("es.nodes.wan.only", "true")  // Otherwise Spark will try to find node via broadcast ping, which will always fail
          .option("pushdown", true) // Enable pushdown to speed up extraction
          .options(location.options)
          .load()
      }
      case _ => {
        ss.read.format(location.format)
          .options(location.options)
          .load()
      }
    }
  }

  def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header], location: GenericLocation) = {
    location.conf.foreach(e => {
      ss.conf.set(e._1, e._2)
    })

    location.format.toLowerCase() match {
      case "cosmos.oltp" =>
        // Ensure the database and the table exist before writing
        val endpoint = location.options.getOrElse("spark.cosmos.accountEndpoint", throw new FeathrException("Missing spark__cosmos__accountEndpoint"))
        val key = location.options.getOrElse("spark.cosmos.accountKey", throw new FeathrException("Missing spark__cosmos__accountKey"))
        val databaseName = location.options.getOrElse("spark.cosmos.database", throw new FeathrException("Missing spark__cosmos__database"))
        val tableName = location.options.getOrElse("spark.cosmos.container", throw new FeathrException("Missing spark__cosmos__container"))
        ss.conf.set("spark.sql.catalog.cosmosCatalog", "com.azure.cosmos.spark.CosmosCatalog")
        ss.conf.set("spark.sql.catalog.cosmosCatalog.spark.cosmos.accountEndpoint", endpoint)
        ss.conf.set("spark.sql.catalog.cosmosCatalog.spark.cosmos.accountKey", key)
        ss.sql(s"CREATE DATABASE IF NOT EXISTS cosmosCatalog.${databaseName};")
        ss.sql(s"CREATE TABLE IF NOT EXISTS cosmosCatalog.${databaseName}.${tableName} using cosmos.oltp TBLPROPERTIES(partitionKeyPath = '/id')")

        // CosmosDb requires the column `id` to exist and be the primary key, and `id` must be in `string` type
        val keyDf = if (!df.columns.contains("id")) {
          header match {
            case Some(h) => {
              // Generate key column from header info, which is required by CosmosDb
              val (keyCol, keyedDf) = DataFrameKeyCombiner().combine(df, FeatureGenUtils.getKeyColumnsFromHeader(h))
              // Rename key column to `id`
              keyedDf.withColumnRenamed(keyCol, "id")
            }
            case None => {
              // If there is no key column, we use a auto-generated monotonic id.
              // but in this case the result could be duplicated if you run job for multiple times
              // This function is for offline-storage usage, ideally user should create a new container for every run
              df.withColumn("id", (monotonically_increasing_id().cast("string")))
            }
          }
        } else {
          // We already have an `id` column
          // TODO: Should we do anything here?
          //  A corner case is that the `id` column exists but not unique, then the output will be incomplete as
          //  CosmosDb will overwrite the old entry with the new one with same `id`.
          //  We can either rename the existing `id` column and use header/autogen key column, or we can tell user
          //  to avoid using `id` column for non-unique data, but both workarounds have pros and cons.
          df
        }
        keyDf.write.format(location.format)
          .options(location.options)
          .mode(location.mode.getOrElse("append")) // CosmosDb doesn't support ErrorIfExist mode in batch mode
          .save()
      case "org.elasticsearch.spark.sql" => {
        val keyDf = header match {
          case Some(h) => {
            // Generate key column from header info, which is required by CosmosDb
            val (keyCol, keyedDf) = DataFrameKeyCombiner().combine(df, FeatureGenUtils.getKeyColumnsFromHeader(h))
            // Rename key column to `_id`
            keyedDf.withColumnRenamed(keyCol, "_id")
          }
          case None => {
            // If there is no key column, we use a auto-generated monotonic id.
            // but in this case the result could be duplicated if you run job for multiple times
            // This function is for offline-storage usage, ideally user should create a new container for every run
            df.withColumn("_id", (monotonically_increasing_id().cast("string")))
          }
        }
        keyDf.write.format(location.format)
          .option("es.nodes.wan.only", "true")
          .option("es.mapping.id", "_id") // Use generated key as the doc id
          .option("es.mapping.exclude", "_id") // Exclude doc id column from the doc body
          .option("es.write.operation", "upsert") // As we already have `_id` column, we should use "upsert", otherwise there could be duplicates in the output
          .options(location.options)
          .mode(location.mode.getOrElse("overwrite")) // I don't see if ElasticSearch uses it in any doc
          .save()
      }
      case "aerospike" =>
        val keyDf = if (!df.columns.contains("__key")) {
          df.withColumn("__key", (monotonically_increasing_id().cast("string")))
          }
         else {
          df
        }
        keyDf.write.format(location.format)
          .option("aerospike.updatebykey", "__key")
          .options(location.options)
          .mode(location.mode.getOrElse("append"))
          .save()
      case _ =>
        // Normal writing procedure, just set format and options then write
        df.write.format(location.format)
          .options(location.options)
          .mode(location.mode.getOrElse("default"))
          .save()
    }
  }
}
