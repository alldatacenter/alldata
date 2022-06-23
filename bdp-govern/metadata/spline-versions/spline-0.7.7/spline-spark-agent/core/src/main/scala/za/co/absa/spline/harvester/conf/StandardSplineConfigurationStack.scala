/*
 * Copyright 2019 ABSA Group Limited
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

import org.apache.commons.configuration._
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Standard Spline configuration comprises several configuration sources, prioritizing broader scopes over the narrower ones.
 * E.g. a property defined in a Hadoop or Spark session would have higher priority than system settings or parameters passed into the method directly.
 *
 * It might look counter-intuitive, as logically explicitly provided params should override ones defined in the outer scope.
 * But that was done in order to allow managed environment administrators to enforce Spline configuration properties.
 * For example, a company could require lineage metadata from all jobs executed on a particular environment to be stored in a particular place
 * (a database, file, Spline server etc). So even if some jobs happen to contain hardcoded properties (e.g. `System.setProperty(...)` or an embedded
 * property files) that a job developer used during development but forgot to remove before submitting the job into a production environment,
 * it should still work correctly. Those local settings need be ignored and overridden by the ones received from the execution environment.
 * For instance, centrally managed settings could be enforced through the Spark Session config
 * (if the Spark session is provided by the environment such as Databricks Notebook or alike), or a Hadoop config.
 *
 */
object StandardSplineConfigurationStack {
  private val PropertiesFileName = "spline.properties"
  private val DefaultPropertiesFileName = "spline.default.properties"

  def defaultConfig: Configuration = new PropertiesConfiguration(StandardSplineConfigurationStack.DefaultPropertiesFileName)

  def configStack(sparkSession: SparkSession, userConfig: Configuration = null): Seq[Configuration] = {
    Seq(
      Some(new HadoopConfiguration(sparkSession.sparkContext.hadoopConfiguration)),
      Some(new SparkConfiguration(sparkSession.sparkContext.getConf)),
      Some(new SystemConfiguration),
      Try(new PropertiesConfiguration(PropertiesFileName)).toOption,
      Option(userConfig)
    ).flatten
      .map(withBackwardCompatibility)
  }

  private def withBackwardCompatibility(config: Configuration) = {
    new CompositeConfiguration(Seq(
      config,
      new Spline05ConfigurationAdapter(config)
    ).asJava)
  }

}
