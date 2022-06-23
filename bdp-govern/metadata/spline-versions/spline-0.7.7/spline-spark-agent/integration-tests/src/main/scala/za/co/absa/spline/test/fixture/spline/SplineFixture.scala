/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.test.fixture.spline

import org.apache.commons.configuration.BaseConfiguration
import org.apache.spark.sql.SparkSession


trait SplineFixture {
  def withLineageTracking[T](testBody: LineageCaptor => T)(implicit session: SparkSession): T = {
    testBody(new LineageCaptor)
  }
}

object SplineFixture {
  def EmptyConf = new BaseConfiguration

  def extractTableIdentifier(paramsOption: Option[Map[String, Any]]): Map[String, Any] =
    paramsOption.get
      .apply("table").asInstanceOf[Map[String, _]]
      .apply("identifier").asInstanceOf[Map[String, _]]
}
