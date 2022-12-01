/*
 * Copyright 2021 ABSA Group Limited
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

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions.col
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.lang.CollectionImplicits._
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture


class DuplicatedExprIdsSpec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture {

  private val tempPath = TempDirectory(prefix = "dupl", pathOnly = true).deleteOnExit().path.toFile.getAbsolutePath

  it should "not create duplicated ids in spline plan" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        val testData = Seq(("adcde1_12938597", 162)).toDF("unique_id", "total_commission")

        for {
          (plan1, Seq(event1)) <- captor.lineageOf {

            val commonDf = testData
              .withColumn("commission", col("total_commission"))
              .drop("total_commission")

            val rightJoinerDf = commonDf
              .withColumnRenamed("commission", "foo")

            val joined = commonDf.join(rightJoinerDf, usingColumns = Seq("unique_id"))

            joined
              .write
              .mode(SaveMode.Overwrite)
              .json(tempPath)
          }
        } yield {
          val attributes = plan1.attributes.get
          attributes.size shouldEqual attributes.distinctBy(_.id).size

          val Seq(commission1, commission2) = attributes.filter(_.name == "commission")
          commission1.childRefs.get.head shouldNot equal(commission2.childRefs.get.head)
        }
      }
    }

}
