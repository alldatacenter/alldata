/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester.conf

import org.apache.spark.SparkConf

import java.util
import scala.collection.JavaConverters._

/**
 * {@link org.apache.spark.SparkConf} to {@link org.apache.commons.configuration.Configuration} adapter
 *
 * @param conf A source of Spark configuration
 */
class SparkConfiguration(conf: SparkConf)
  extends ReadOnlyConfiguration
    with MapLikeConfigurationAdapter {

  import SparkConfiguration._

  override protected def propertiesMap: util.Map[String, _ <: AnyRef] = conf.getAll
    .map { case (k, v) => k.stripPrefix(KEY_PREFIX) -> v }
    .toMap
    .asJava
}

object SparkConfiguration {
  private val KEY_PREFIX = "spark."
}
