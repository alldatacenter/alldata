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

object UnionJob extends SparkApp("Union Job") {

  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val df1 = Seq((1, 2, 3)).toDF()
  val df2 = Seq((4, 5, 6)).toDF()

  val df3 = df1.union(df2).filter('_1 > 0)
  val df4 = df1.union(df2).filter('_2 < 0)

  val df5 = df3.union(df4).union(df2)

  df5.write.mode(SaveMode.Overwrite).parquet("data/output/batch/union_job_results")
}
