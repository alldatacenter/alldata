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

package za.co.absa.spline.example.batch

import org.apache.spark.sql.SaveMode
import za.co.absa.spline.SparkApp

import scala.util.Try

object Example1FailingJob extends SparkApp("Example 1 (failing)") {

  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  case class Foo(a: Int, b: Int, c: Int)

  Try {
    Seq(Foo(1, 2, 3))
      .toDS()
      .filter('a > 0)
      .map[Foo]((_: Foo) => sys.error("simulated example job error"))
      .write
      .mode(SaveMode.Append)
      .parquet("data/output/batch/job1_results")
  }
}
