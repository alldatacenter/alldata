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

import org.apache.spark.SPARK_VERSION
import org.apache.spark.sql.catalyst.expressions.Literal
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version.VersionStringInterpolator
import za.co.absa.spline.producer.model.AttrOrExprRef
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

class DeltaDSV2Spec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with SparkDatabaseFixture {

  it should "support AppendData V2 command" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .enableHiveSupport
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        withDatabase("testDB") {
          val testData = {
            import spark.implicits._
            Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
          }

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              spark.sql(s"CREATE TABLE foo (ID int, NAME string) USING delta")
              testData.write.format("delta").mode("append").saveAsTable("foo")
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe true
            plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/testdb.db/foo"
          }
        }
      }
    }

  it should "support OverwriteByExpression V2 command without deleteExpression" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .enableHiveSupport
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        withDatabase("testDB") {
          val testData = {
            import spark.implicits._
            Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
          }

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              spark.sql(s"CREATE TABLE foo (ID int, NAME string) USING delta")
              testData.write.format("delta").mode("overwrite").insertInto("foo")
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe false
            plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
            val deleteExprId = plan1.operations.write.params.get.apply("deleteExpr")
              .asInstanceOf[AttrOrExprRef].__exprId.get
            val literal = plan1.expressions.get.constants.get.find(_.id == deleteExprId).get
            literal.value shouldEqual true
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/testdb.db/foo"
          }
        }
      }
    }

  it should "support OverwriteByExpression V2 command with deleteExpression" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .enableHiveSupport
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        withDatabase("testDB") {
          val testData = {
            import spark.implicits._
            Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
          }
          testData.createOrReplaceTempView("tempdata")

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              spark.sql(s"CREATE TABLE foo (ID int, NAME string) USING delta PARTITIONED BY (ID)")
              spark.sql("""
                          |INSERT OVERWRITE foo PARTITION (ID = 222222)
                          |  (SELECT NAME FROM tempdata WHERE NAME = 'Warsaw')
                          |""".stripMargin)
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe false
            plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
            plan1.operations.write.params.get.apply("deleteExpr") should not be(Literal(true))
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/testdb.db/foo"
          }
        }
      }
    }

  /**
    * Even though the code actually does dynamic partition overwrite,
    * the spark command generated is OverwriteByExpression.
    * Keeping this test in case the command will be used in future Spark versions.
    */
  it should "support OverwritePartitionsDynamic V2 command" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .enableHiveSupport
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nonstrict")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        withDatabase("testDB") {
          val testData = {
            import spark.implicits._
            Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
          }
          testData.createOrReplaceTempView("tempdata")

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              spark.sql(s"CREATE TABLE foo (ID int, NAME string) USING delta PARTITIONED BY (NAME)")
              spark.sql("""
                          |INSERT OVERWRITE foo PARTITION (NAME)
                          |  (SELECT ID, NAME FROM tempdata WHERE NAME = 'Warsaw')
                          |""".stripMargin)
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe false
            plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/testdb.db/foo"
          }
        }
      }
    }

  it should "support CreateTableAsSelect V2 command" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .enableHiveSupport
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        withDatabase("testDB") {
          val testData = {
            import spark.implicits._
            Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
          }

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              testData.write.format("delta").saveAsTable("foo")
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe false
            plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/testdb.db/foo"
          }
        }
      }
    }

  it should "support ReplaceTableAsSelect V2 command" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withCustomSparkSession(_
      .enableHiveSupport
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    ) { implicit spark =>
      withLineageTracking { lineageCaptor =>
        withDatabase("testDB") {
          val testData = {
            import spark.implicits._
            Seq((1014, "Warsaw"), (1002, "Corte")).toDF("ID", "NAME")
          }

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              spark.sql(s"CREATE TABLE foo (toBeOrNotToBe boolean) USING delta")
              testData.write.format("delta").mode("overwrite").option("overwriteSchema", "true").saveAsTable("foo")
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe false
            plan1.operations.write.extra.get("destinationType") shouldBe Some("delta")
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/testdb.db/foo"
          }
        }
      }
    }
}
