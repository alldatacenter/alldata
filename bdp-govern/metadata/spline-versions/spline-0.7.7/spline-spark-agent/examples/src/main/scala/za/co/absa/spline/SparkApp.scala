/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline

import org.apache.spark.sql.{SQLContext, SQLImplicits, SparkSession}

/**
  * The class represents skeleton of a example application and looks after initialization of SparkSession, etc
  * @param name A spark application name
  * @param master A spark master
  * @param conf Custom properties
  */
abstract class SparkApp
(
  name: String,
  master: String = "local[*]",
  conf: Seq[(String, String)] = Nil
) extends SQLImplicits with App {

  private val sparkBuilder = SparkSession.builder()

  sparkBuilder.appName(name)
  sparkBuilder.master(master)

  sparkBuilder.config("spark.spline.postProcessingFilter.composite.filters", "userExtraMeta")
  sparkBuilder.config("spark.spline.postProcessingFilter.userExtraMeta.rules",
    """
      |{
      |  "executionPlan": {
      |    "labels": {
      |      "tags": [ "example" ] \,
      |      "appName": { "$js": "session.conf().get('spark.app.name')" }
      |    }
      |  }
      |}""".stripMargin)

  for ((k, v) <- conf) sparkBuilder.config(k, v)

  /**
    * A Spark session.
    */
  val spark: SparkSession = sparkBuilder.getOrCreate()

  protected override def _sqlContext: SQLContext = spark.sqlContext
}
