package com.linkedin.feathr

import com.typesafe.config.Config
import scala.collection.JavaConverters._

/**
 * parameter map(config) utility class, help user to get parameter value with a default value,
 * example usage:
 *
 * import com.linkedin.feathr.common.RichConfig._
 * val batchValue = _params.map(_.getBooleanWithDefault(batchPath, true)).get
 *
 */
package object common {

  val SELECTED_FEATURES = "selectedFeatures"
  implicit class RichConfig(val config: Config) {
    /*
      get a parameter at 'path' with default value
     */
    def getStringWithDefault(path: String, default: String): String = if (config.hasPath(path)) {
      config.getString(path)
    } else {
      default
    }

    /*
      get a parameter at 'path' with default value
     */
    def getBooleanWithDefault(path: String, default: Boolean): Boolean = if (config.hasPath(path)) {
      config.getBoolean(path)
    } else {
      default
    }

    /*
      get a parameter at 'path' with default value
     */
    def getIntWithDefault(path: String, default: Int): Int = if (config.hasPath(path)) {
      config.getInt(path)
    } else {
      default
    }

    /*
      get a parameter at 'path' with default value
     */
    def getDoubleWithDefault(path: String, default: Double): Double = if (config.hasPath(path)) {
      config.getDouble(path)
    } else {
      default
    }
    /*
      get a parameter at 'path' with default value
     */
    def getMapWithDefault(path: String, default: Map[String, Object]): Map[String, Object] = if (config.hasPath(path)) {
      config.getObject(path).unwrapped().asScala.toMap
    } else {
      default
    }

    /*
      get a parameter with optional string list
     */
    def getStringListOpt(path: String): Option[Seq[String]] = if (config.hasPath(path)) {
      Some(config.getStringList(path).asScala.toSeq)
    } else {
      None
    }

    /*
      get a parameter with optional string
     */
    def getStringOpt(path: String): Option[String] = if (config.hasPath(path)) {
      Some(config.getString(path))
    } else {
      None
    }

    /*
      get a parameter with optional number
     */
    def getNumberOpt(path: String): Option[Number] = if (config.hasPath(path)) {
      Some(config.getNumber(path))
    } else {
      None
    }
  }
}
