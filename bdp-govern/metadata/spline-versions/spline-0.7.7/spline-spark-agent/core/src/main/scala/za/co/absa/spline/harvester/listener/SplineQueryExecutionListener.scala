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

package za.co.absa.spline.harvester.listener

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.QueryExecution
import org.apache.spark.sql.util.QueryExecutionListener
import za.co.absa.spline.harvester.SparkLineageInitializer

/**
 * Spline Codeless init entry point.
 * This class is supposed to be instantiated exclusively by Spark via the `spark.sql.queryExecutionListeners` configuration parameter.
 *
 * @see [[https://spark.apache.org/docs/latest/configuration.html#static-sql-configuration]]
 */
class SplineQueryExecutionListener extends QueryExecutionListener {

  private val maybeListener: Option[QueryExecutionListener] = {
    val sparkSession = SparkSession.getActiveSession
      .orElse(SparkSession.getDefaultSession)
      .getOrElse(throw new IllegalStateException("Session is unexpectedly missing. Spline cannot be initialized."))

    new SparkLineageInitializer(sparkSession).createListener(isCodelessInit = true)
  }

  override def onSuccess(funcName: String, qe: QueryExecution, durationNs: Long): Unit = {
    maybeListener.foreach(_.onSuccess(funcName, qe, durationNs))
  }

  override def onFailure(funcName: String, qe: QueryExecution, exception: Exception): Unit = {
    maybeListener.foreach(_.onFailure(funcName, qe, exception))
  }
}
