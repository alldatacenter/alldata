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


package za.co.absa.spline

import org.apache.spark.SPARK_VERSION
import org.apache.spark.sql.SaveMode.Overwrite
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version._
import za.co.absa.spline.test.SplineMatchers._
import za.co.absa.spline.test.fixture.SparkFixture
import za.co.absa.spline.test.fixture.spline.SplineFixture

class SQLCommandsSpec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture {

  behavior of "spark.sql()"

  it should "capture lineage of 'CREATE TABLE AS` - Hive" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport) { implicit spark =>
        withLineageTracking { captor =>
          import spark.implicits._

          for {
            (_, _) <- captor.lineageOf(
              spark.sql("CREATE TABLE sourceTable (id int, name string)"))

            (plan1, _) <- captor.lineageOf(
              Seq((1, "AA"), (2, "BB"), (3, "CC"), (4, "DD"))
                .toDF("id", "name")
                .write.mode(Overwrite).insertInto("sourceTable"))

            (plan2, _) <- captor.lineageOf(
              spark.sql(
                """CREATE TABLE targetTable AS
                  | SELECT id, name
                  | FROM sourceTable
                  | WHERE id > 1""".stripMargin))

          } yield {
            plan1.operations.write.outputSource should equalToUri(s"file:$warehouseDir/sourcetable")
            plan2.operations.reads.get.head.inputSources.head should equalToUri(plan1.operations.write.outputSource)
            plan2.operations.write.outputSource should equalToUri(s"file:$warehouseDir/targettable")
          }
        }
      }
    }

  it should "capture lineage of 'INSERT OVERWRITE AS` - Hive" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport) { implicit spark =>
        withLineageTracking { captor =>
          import spark.implicits._

          val dir = TempDirectory("spline", ".table", pathOnly = true).deleteOnExit().path

          withNewSparkSession { innerSpark =>
            innerSpark.sql("CREATE TABLE IF NOT EXISTS sourceTable (id int, name string)")
          }

          for {
            (_, _) <- captor.lineageOf(
              Seq((1, "AA"), (2, "BB"), (3, "CC"), (4, "DD"))
                .toDF("id", "name")
                .write.mode(Overwrite).insertInto("sourceTable"))
            (plan, _) <- captor.lineageOf(
              spark.sql(
                s"""INSERT OVERWRITE DIRECTORY '${dir.toUri}'
                   | SELECT id, name
                   | FROM sourceTable
                   | WHERE id > 1""".stripMargin))
          } yield {
            plan.operations.reads.get.head.inputSources.head should equalToUri(s"file:$warehouseDir/sourcetable")
            plan.operations.write.outputSource should equalToUri(dir.toUri.toString.stripSuffix("/"))
          }
        }
      }
    }

  it should "capture lineage of 'INSERT OVERWRITE AS` - non-Hive" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in
    withRestartingSparkContext {
      withCustomSparkSession(_.enableHiveSupport) { implicit spark =>
        withLineageTracking { captor =>
          import spark.implicits._

          val csvFile = TempDirectory("spline", ".csv").deleteOnExit().path

          withNewSparkSession{ innerSpark =>
            innerSpark.sql("CREATE TABLE sourceTable (id int, name string)")
          }

          for {
            (_, _) <- captor.lineageOf(
              Seq((1, "AA"), (2, "BB"), (3, "CC"), (4, "DD"))
                .toDF("id", "name")
                .write.insertInto("sourceTable")
            )

            (plan, _) <- captor.lineageOf(
              spark.sql(
                s"""INSERT OVERWRITE DIRECTORY '${csvFile.toUri}'
                   | USING CSV
                   | SELECT id, name
                   | FROM sourceTable
                   | WHERE id > 1""".stripMargin)
            )
          } yield {
            plan.operations.reads.get.head.inputSources.head should equalToUri(s"file:$warehouseDir/sourcetable")
            plan.operations.write.outputSource should equalToUri(csvFile.toUri.toString.stripSuffix("/"))
          }
        }
      }
    }
}
