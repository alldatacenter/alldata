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

package za.co.absa.spline.issue

import org.apache.commons.io.FileUtils
import za.co.absa.commons.io.TempFile
import za.co.absa.spline.SparkApp

object GitHub718Job extends SparkApp("GitHub spline-718") {

  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val Seq(pA, pB, pC, pD) = ('A' to 'D').map(ch => {
    val path = TempFile(s"$ch.", ".csv", pathOnly = false).deleteOnExit().path
    FileUtils.write(path.toFile, "Foo, Bar", "UTF-8")
    path.toString
  })

  val Seq(pE, pF, pG) = ('E' to 'G')
    .map(ch => TempFile(s"$ch.", ".csv", pathOnly = true).deleteOnExit().path.toString)

  spark.read.csv(pA).write.csv(pE)
  spark.read.csv(pC, pD).write.csv(pF)
  spark.read.csv(pE, pB, pC, pF).write.csv(pG)
  spark.read.csv(pG).write.mode("overwrite").csv(pB)
}
