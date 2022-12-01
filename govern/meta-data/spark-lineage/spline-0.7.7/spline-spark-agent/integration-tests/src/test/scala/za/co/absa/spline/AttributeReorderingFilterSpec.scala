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

import org.apache.spark.SPARK_VERSION
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version.VersionStringInterpolator
import za.co.absa.spline.AttributeReorderingFilterSpec._
import za.co.absa.spline.producer.model._
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

class AttributeReorderingFilterSpec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with SparkDatabaseFixture {

  "AttributeOrderEnrichingFilter" should "produce lineage with correct attribute order" taggedAs
    ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withRestartingSparkContext {
      withCustomSparkSession(_
        .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
        .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      ) { implicit spark =>
        withLineageTracking { lineageCaptor =>
          withDatabase("testDB") {

            import spark.implicits._

            for {
              (plan, _) <- lineageCaptor.lineageOf {
                spark.sql(s"CREATE TABLE t1 (i int, j int) USING delta")

                Seq((3, 4)).toDF("j", "i")
                  .write.format("delta")
                  .mode("append")
                  .saveAsTable("t1")
              }
            } yield {
              val write = plan.operations.write
              val writeChild = getOtherOpById(plan, write.childIds.head)

              getAttributeById(plan, writeChild.output.get(0)).name should be("i")
              getAttributeById(plan, writeChild.output.get(1)).name should be("j")

              val writeChild2 = getOtherOpById(plan, writeChild.childIds.get.head)
              val writeChild3 = getOtherOpById(plan, writeChild2.childIds.get.head)
              writeChild3.name.get should be("LocalRelation")
            }
          }
        }
      }
    }

}

object AttributeReorderingFilterSpec {
  def getOtherOpById(plan: ExecutionPlan, opId :String): DataOperation =
    plan.operations.other.flatMap(_.find(_.id == opId)).get

  def getAttributeById(plan: ExecutionPlan, attId :String): Attribute =
    plan.attributes.flatMap(_.find(_.id == attId)).get
}
