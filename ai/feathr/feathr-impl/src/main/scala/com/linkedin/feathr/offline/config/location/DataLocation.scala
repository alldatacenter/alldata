package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.caseclass.mapper.CaseClassObjectMapper
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.common.{FeathrJacksonScalaModule, Header}
import com.linkedin.feathr.offline.config.DataSourceLoader
import com.linkedin.feathr.offline.source.DataSource
import com.typesafe.config.{Config, ConfigException}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters._

/**
 * An InputLocation is a data source definition, it can either be HDFS files or a JDBC database connection
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[SimplePath])
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[KafkaEndpoint], name = "kafka"),
    new JsonSubTypes.Type(value = classOf[SimplePath], name = "path"),
    new JsonSubTypes.Type(value = classOf[PathList], name = "pathlist"),
    new JsonSubTypes.Type(value = classOf[Jdbc], name = "jdbc"),
    new JsonSubTypes.Type(value = classOf[GenericLocation], name = "generic"),
    new JsonSubTypes.Type(value = classOf[SparkSqlLocation], name = "sparksql"),
    new JsonSubTypes.Type(value = classOf[Snowflake], name = "snowflake"),
  ))
trait DataLocation {
  /**
   * Backward Compatibility
   * Many existing codes expect a simple path
   *
   * @return the `path` or `url` of the data source
   *
   *         WARN: This method is deprecated, you must use match/case on InputLocation,
   *         and get `path` from `SimplePath` only
   */
  @deprecated("Do not use this method in any new code, it will be removed soon")
  def getPath: String

  /**
   * Backward Compatibility
   *
   * @return the `path` or `url` of the data source, wrapped in an List
   *
   *         WARN: This method is deprecated, you must use match/case on InputLocation,
   *         and get `paths` from `PathList` only
   */
  @deprecated("Do not use this method in any new code, it will be removed soon")
  def getPathList: List[String]

  /**
   * Load DataFrame from Spark session
   *
   * @param ss SparkSession
   * @return
   */
  def loadDf(ss: SparkSession, dataIOParameters: Map[String, String] = Map()): DataFrame

  /**
   * Write DataFrame to the location
   *
   * @param ss SparkSession
   * @param df DataFrame to write
   */
  def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header])

  /**
   * Tell if this location is file based
   *
   * @return boolean
   */
  def isFileBasedLocation(): Boolean

  override def toString: String = getPath
}

object LocationUtils {
  private def propOrEnvOrElse(key: String, alt: String): String = {
    // Try properties, then env, then properties and env again with uppercase
    // GitHub secrets must have uppercase name, so client can only use uppercase keys if they're taken from GH secrets
    scala.util.Properties.propOrElse(key,
      scala.util.Properties.envOrElse(key,
        scala.util.Properties.propOrElse(key.toUpperCase,
          scala.util.Properties.envOrElse(key.toUpperCase, alt))))
  }

  /**
   * String template substitution, replace "...${VAR}.." with corresponding System property or environment variable
   * Non-existent pattern is replaced by empty string.
   *
   * @param s String template to be processed
   * @return Processed result
   */
  def envSubstitute(s: String): String = {
    """(\$\{[A-Za-z0-9_-]+})""".r.replaceAllIn(s, m => propOrEnvOrElse(m.toString().substring(2).dropRight(1), ""))
  }

  /**
   * Get an ObjectMapper to deserialize DataSource
   *
   * @return the ObjectMapper
   */
  def getMapper(): ObjectMapper = {
    (new ObjectMapper(new HoconFactory) with CaseClassObjectMapper)
      .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new SimpleModule().addDeserializer(classOf[DataSource], new DataSourceLoader))
  }
}

object DataLocation {
  /**
   * Create DataLocation from string, try parsing the string as JSON and fallback to SimplePath
   *
   * @param cfg the input string
   * @return DataLocation
   */
  def apply(cfg: String): DataLocation = {
    val jackson = (new ObjectMapper(new HoconFactory) with CaseClassObjectMapper)
      .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new SimpleModule().addDeserializer(classOf[DataSource], new DataSourceLoader))
    try {
      // Cfg is either a plain path or a JSON object
      if (cfg.trim.startsWith("{")) {
        val location = jackson.readValue(cfg, classOf[DataLocation])
        location
      } else {
        SimplePath(cfg)
      }
    } catch {
      case _@(_: ConfigException | _: JacksonException) => SimplePath(cfg)
    }
  }

  def apply(cfg: Config): DataLocation = {
    apply(cfg.root().keySet().asScala.map(key ⇒ key → cfg.getString(key)).toMap)
  }

  def apply(cfg: Any): DataLocation = {
    val jackson = (new ObjectMapper(new HoconFactory) with CaseClassObjectMapper)
      .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new SimpleModule().addDeserializer(classOf[DataSource], new DataSourceLoader))
    try {
      val location = jackson.convertValue(cfg, classOf[DataLocation])
      location
    } catch {
      case e: JacksonException => {
        print(e)
        SimplePath(cfg.toString)
      }
    }
  }
}
