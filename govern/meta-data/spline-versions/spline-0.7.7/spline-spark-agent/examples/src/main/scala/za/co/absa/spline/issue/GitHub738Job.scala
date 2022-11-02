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
import org.apache.spark.internal.Logging
import za.co.absa.commons.io.TempFile
import za.co.absa.spline.SparkApp

import scala.util.Random

object GitHub738Job extends SparkApp("GitHub spline-738") with Logging {

  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val (primarySourceKeys, secondarySourceKeys) = 'A' to 'Q' splitAt 5
  val primarySourcePaths = primarySourceKeys.map(key => createMockCSV(s"Primary-$key"))
  val secondarySourcePaths = secondarySourceKeys.map(key => createMockCSV(s"Secondary-$key"))

  val nIters = 20
  val nAppends = 10
  for {
    i <- 0 until nIters
    j <- primarySourcePaths.indices
  } {
    val prmWritePath = primarySourcePaths(j)
    val prmReadPaths = primarySourcePaths.take(j) ++ primarySourcePaths.drop(j + 1)
    val sndPaths = Random.shuffle(secondarySourcePaths).take(Random.nextInt(secondarySourcePaths.length))

    val df = spark.read.csv(prmReadPaths ++ sndPaths: _*).limit(1).cache()
    df.write.mode("overwrite").csv(prmWritePath)
    0 until nAppends foreach (_ => df.write.mode("append").csv(prmWritePath))

    logInfo("###############################")
    logInfo(s"Done ${i * primarySourcePaths.length + j + 1} of ${nIters * primarySourcePaths.length}")
    logInfo("###############################")
  }

  private def createMockCSV(prefix: String) = {
    val path = TempFile(s"$prefix.", ".csv", pathOnly = false).deleteOnExit().path
    FileUtils.write(path.toFile, "Foo, Bar", "UTF-8")
    path.toString
  }
}
