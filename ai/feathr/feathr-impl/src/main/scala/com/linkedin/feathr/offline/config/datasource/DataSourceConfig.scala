package com.linkedin.feathr.offline.config.datasource

import com.typesafe.config.{Config, ConfigFactory}

/**
 * A base class to parse config context from Config String
 *
 * @param configStr
 */
class DataSourceConfig(val configStr: Option[String] = None) {
  val config: Option[Config] = configStr.map(configStr =>
    try {
      ConfigFactory.parseString(configStr)
    } catch {
          // There's a bug in GNUParser when used with HOCON config. Try adding a trailing " to hack around.
      case _ => ConfigFactory.parseString(configStr + "\"")
    }
  )
}