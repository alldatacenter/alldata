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

package za.co.absa.spline.harvester

import org.apache.commons.io.FileUtils
import org.apache.spark.SPARK_VERSION
import org.apache.spark.sql.SaveMode
import org.scalatest.Inside._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.commons.io.{TempDirectory, TempFile}
import za.co.absa.commons.lang.OptionImplicits._
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version._
import za.co.absa.spline.model.dt
import za.co.absa.spline.producer.model._
import za.co.absa.spline.test.LineageWalker
import za.co.absa.spline.test.ProducerModelImplicits._
import za.co.absa.spline.test.SplineMatchers.dependOn
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

import java.util.UUID
import scala.language.reflectiveCalls
import scala.util.Try

class LineageHarvesterSpec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with SparkDatabaseFixture {

  import za.co.absa.spline.harvester.LineageHarvesterSpec._

  it should "produce deterministic output" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        val testDest = tmpDest

        def testJob(): Unit = spark
          .createDataset(Seq(TestRow(1, 2.3, "text")))
          .filter('i > 0)
          .sort('s)
          .select('d + 42 as "x")
          .write
          .mode(SaveMode.Append)
          .save(testDest)

        for {
          (plan1, _) <- captor.lineageOf(testJob())
          (plan2, _) <- captor.lineageOf(testJob())
        } yield {
          plan1 shouldEqual plan2
        }
      }
    }

  it should "support empty data frame" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        for {
          (plan, _) <- captor.lineageOf(spark.emptyDataset[TestRow].write.save(tmpDest))
        } yield {
          inside(plan) {
            case ExecutionPlan(_, _, _, _, Operations(_, None, Some(Seq(op))), _, _, _, _, _) =>
              op.id should be("op-1")
              op.name should be(Some("LocalRelation"))
              op.childIds should be(None)
              op.output should not be None
              op.extra should be(empty)
          }
        }
      }
    }

  it should "support simple non-empty data frame" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val tmpPath = tmpDest

        val df = spark.createDataset(Seq(TestRow(1, 2.3, "text")))

        for {
          (plan, _) <- captor.lineageOf(df.write.save(tmpPath))
        } yield {
          val write = plan.operations.write
          write.id shouldBe "op-0"
          write.name should contain oneOf("InsertIntoHadoopFsRelationCommand", "SaveIntoDataSourceCommand")
          write.childIds should be(Seq("op-1"))
          write.outputSource should be(s"file:$tmpPath")
          write.params should contain(Map("path" -> tmpPath))
          write.extra should contain(Map("destinationType" -> Some("parquet")))

          plan.operations.reads should not be defined

          plan.operations.other.get.size should be(1)

          val op = plan.operations.other.get.head
          op.id should be("op-1")
          op.name should contain("LocalRelation")
          op.childIds should not be defined
          op.output should contain(Seq("attr-0", "attr-1", "attr-2"))

          plan.attributes should contain(Seq(
            Attribute("attr-0", Some(integerType.id), None, None, "i"),
            Attribute("attr-1", Some(doubleType.id), None, None, "d"),
            Attribute("attr-2", Some(stringType.id), None, None, "s")
          ))
        }
      }
    }

  it should "support filter and column renaming operations" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val tmpPath = tmpDest

        val df = spark.createDataset(Seq(TestRow(1, 2.3, "text")))
          .withColumnRenamed("i", "A")
          .filter($"A".notEqual(5))

        for {
          (plan, _) <- captor.lineageOf(df.write.save(tmpPath))
        } yield {
          val write = plan.operations.write
          write.id shouldBe "op-0"
          write.name should contain oneOf("InsertIntoHadoopFsRelationCommand", "SaveIntoDataSourceCommand")
          write.childIds should be(Seq("op-1"))
          write.outputSource should be(s"file:$tmpPath")
          write.append should be(false)
          write.params should contain(Map("path" -> tmpPath))
          write.extra should contain(Map("destinationType" -> Some("parquet")))

          plan.operations.reads should not be defined

          plan.operations.other.get.size should be(3)

          val localRelation = plan.operations.other.get.head
          localRelation.id should be("op-3")
          localRelation.name should contain("LocalRelation")
          localRelation.childIds should not be defined
          localRelation.output should contain(Seq("attr-0", "attr-1", "attr-2"))

          val project = plan.operations.other.get(1)
          project.id should be("op-2")
          project.name should contain("Project")
          project.childIds should contain(Seq("op-3"))
          project.output should contain(Seq("attr-3", "attr-1", "attr-2"))
          project.params.get("projectList") should be(Some(Seq(
            AttrOrExprRef(None, Some("expr-0")),
            AttrOrExprRef(Some("attr-1"), None),
            AttrOrExprRef(Some("attr-2"), None)
          )))

          val filter = plan.operations.other.get(2)
          filter.id should be("op-1")
          filter.name should contain("Filter")
          filter.childIds should contain(Seq("op-2"))
          filter.output should contain(Seq("attr-3", "attr-1", "attr-2"))
          filter.params.get("condition") should be(Some(AttrOrExprRef(None, Some("expr-1"))))

          plan.attributes should contain(Seq(
            Attribute("attr-0", Some(integerType.id), None, None, "i"),
            Attribute("attr-1", Some(doubleType.id), None, None, "d"),
            Attribute("attr-2", Some(stringType.id), None, None, "s"),
            Attribute("attr-3", Some(integerType.id), Seq(AttrOrExprRef(None, Some("expr-0"))).asOption, None, "A")
          ))
        }
      }
    }

  it should "support union operation, forming a diamond graph" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val tmpPath = tmpDest

        val initialDF = spark.createDataset(Seq(TestRow(1, 2.3, "text")))
        val filteredDF1 = initialDF.filter($"i".notEqual(5))
        val filteredDF2 = initialDF.filter($"d".notEqual(5.0))
        val df = filteredDF1.union(filteredDF2)

        for {
          (plan, _) <- captor.lineageOf(df.write.save(tmpPath))
        } yield {
          implicit val walker: LineageWalker = LineageWalker(plan)

          val write = plan.operations.write
          write.name should contain oneOf("InsertIntoHadoopFsRelationCommand", "SaveIntoDataSourceCommand")
          write.outputSource should be(s"file:$tmpPath")
          write.append should be(false)
          write.params should contain(Map("path" -> tmpPath))
          write.extra should contain(Map("destinationType" -> Some("parquet")))

          val union = write.precedingOp
          union.name should contain("Union")
          union.childIds.get.size should be(2)

          val Seq(filter1, maybeFilter2) = union.precedingOps
          filter1.name should contain("Filter")
          filter1.params.get should contain key "condition"

          val filter2 =
            if (maybeFilter2.name.get == "Project") {
              maybeFilter2.precedingOp // skip additional select (in Spark 3.0 and 3.1 only)
            } else {
              maybeFilter2
            }

          filter2.name should contain("Filter")
          filter2.params.get should contain key "condition"

          val localRelation1 = filter1.precedingOp
          localRelation1.name should contain("LocalRelation")

          val localRelation2 = filter2.precedingOp
          localRelation2.name should contain("LocalRelation")

          inside(union.outputAttributes) { case Seq(i, d, s) =>
            inside(filter1.outputAttributes) { case Seq(f1I, f1D, f1S) =>
              i should dependOn(f1I)
              d should dependOn(f1D)
              s should dependOn(f1S)
            }
            inside(filter2.outputAttributes) { case Seq(f2I, f2D, f2S) =>
              i should dependOn(f2I)
              d should dependOn(f2D)
              s should dependOn(f2S)
            }
          }

          inside(filter1.outputAttributes) { case Seq(i, d, s) =>
            inside(localRelation1.outputAttributes) { case Seq(lr1I, lr1D, lr1S) =>
              i should dependOn(lr1I)
              d should dependOn(lr1D)
              s should dependOn(lr1S)
            }
          }

          inside(filter2.outputAttributes) { case Seq(i, d, s) =>
            inside(localRelation2.outputAttributes) { case Seq(lr2I, lr2D, lr2S) =>
              i should dependOn(lr2I)
              d should dependOn(lr2D)
              s should dependOn(lr2S)
            }
          }

        }
      }
    }

  it should "support join operation, forming a diamond graph" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import org.apache.spark.sql.functions._
        import spark.implicits._
        val tmpPath = tmpDest

        val initialDF = spark.createDataset(Seq(TestRow(1, 2.3, "text")))
        val filteredDF = initialDF
          .filter($"i" =!= 5)
        val aggregatedDF = initialDF
          .withColumnRenamed("i", "A")
          .groupBy($"A")
          .agg(
            min("d").as("MIN"),
            max("s").as("MAX"))

        val joinExpr = filteredDF.col("i").eqNullSafe(aggregatedDF.col("A"))
        val df = filteredDF.join(aggregatedDF, joinExpr, "inner")

        for {
          (plan, _) <- captor.lineageOf(df.write.save(tmpPath))
        } yield {
          implicit val walker: LineageWalker = LineageWalker(plan)

          val write = plan.operations.write
          write.name should contain oneOf("InsertIntoHadoopFsRelationCommand", "SaveIntoDataSourceCommand")
          write.outputSource should be(s"file:$tmpPath")
          write.append should be(false)
          write.params should contain(Map("path" -> tmpPath))
          write.extra should contain(Map("destinationType" -> Some("parquet")))

          val join = write.precedingOp
          join.name should contain("Join")
          join.childIds.get.size should be(2)

          val Seq(filter, aggregate) = join.precedingOps
          filter.name should contain("Filter")
          filter.params.get should contain key "condition"

          aggregate.name should contain("Aggregate")
          aggregate.params.get should contain key "groupingExpressions"
          aggregate.params.get should contain key "aggregateExpressions"

          val project = aggregate.precedingOp
          project.name should contain("Project")

          val localRelation1 = filter.precedingOp
          localRelation1.name should contain("LocalRelation")

          val localRelation2 = project.precedingOp
          localRelation2.name should contain("LocalRelation")

          inside(join.outputAttributes) { case Seq(i, d, s, a, min, max) =>
            inside(filter.outputAttributes) { case Seq(inI, inD, inS) =>
              i should dependOn(inI)
              d should dependOn(inD)
              s should dependOn(inS)
            }
            inside(aggregate.outputAttributes) { case Seq(inA, inMin, inMax) =>
              a should dependOn(inA)
              min should dependOn(inMin)
              max should dependOn(inMax)
            }
          }

          inside(filter.outputAttributes) { case Seq(i, d, s) =>
            inside(localRelation1.outputAttributes) { case Seq(inI, inD, inS) =>
              i should dependOn(inI)
              d should dependOn(inD)
              s should dependOn(inS)
            }
          }

          inside(aggregate.outputAttributes) { case Seq(a, min, max) =>
            inside(localRelation2.outputAttributes) { case Seq(inI, inD, inS) =>
              a should dependOn(inI)
              min should dependOn(inD)
              max should dependOn(inS)
            }
          }

        }
      }
    }

  it should "support `CREATE TABLE ... AS SELECT` in Hive" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in
    withRestartingSparkContext {
      withCustomSparkSession(_
        .enableHiveSupport()
        .config("hive.exec.dynamic.partition.mode", "nonstrict")
      ) { implicit spark =>
        withDatabase(s"unitTestDatabase_${this.getClass.getSimpleName}") {
          withLineageTracking { captor =>
            import spark.implicits._

            val df = spark.createDataset(Seq(TestRow(1, 2.3, "text")))

            for {
              (plan, _) <- captor.lineageOf {
                df.createOrReplaceTempView("tempView")
                spark.sql("create table users_sales as select i, d, s from tempView ")
              }
            } yield {
              val writeOperation = plan.operations.write
              writeOperation.id shouldEqual "op-0"
              writeOperation.append shouldEqual false
              writeOperation.childIds shouldEqual Seq("op-1")
              writeOperation.extra.get("destinationType") shouldEqual Some("hive")

              val otherOperations = plan.operations.other.get.sortBy(_.id)

              val firstOperation = otherOperations.head
              firstOperation.id shouldEqual "op-1"
              firstOperation.childIds.get shouldEqual Seq("op-2")
              firstOperation.name shouldEqual Some("Project")
              firstOperation.extra shouldBe empty

              val secondOperation = otherOperations(1)
              secondOperation.id shouldEqual "op-2"
              secondOperation.childIds.get shouldEqual Seq("op-3")
              secondOperation.name should contain oneOf("SubqueryAlias", "`tempview`") // Spark 2.3/2.4
              secondOperation.extra shouldBe empty
            }
          }
        }
      }
    }

  it should "collect user extra metadata" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in {
    val injectRules =
      """
        |{
        |    "executionPlan": {
        |        "extra": { "test.extra": { "$js": "executionPlan" } }
        |    }\,
        |    "executionEvent": {
        |        "extra": { "test.extra": { "$js": "executionEvent" } }
        |    }\,
        |    "read": {
        |        "extra": { "test.extra": { "$js": "read" } }
        |    }\,
        |    "write": {
        |        "extra": { "test.extra": { "$js": "write" } }
        |    }\,
        |    "operation": {
        |        "extra": { "test.extra": { "$js": "operation" } }
        |    }
        |}
        |""".stripMargin

    withCustomSparkSession(_
      .config("spark.spline.postProcessingFilter", "userExtraMeta")
      .config("spark.spline.postProcessingFilter.userExtraMeta.rules", injectRules)
    ) { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        val dummyCSVFile = TempFile(prefix = "spline-test", suffix = ".csv").deleteOnExit().path
        FileUtils.write(dummyCSVFile.toFile, "1,2,3", "UTF-8")

        for {
          (plan, Seq(event)) <- captor.lineageOf {
            spark
              .read.csv(dummyCSVFile.toString)
              .filter($"_c0" =!= 42)
              .write.save(tmpDest)
          }
        } yield {
          inside(plan.operations) {
            case Operations(wop, Some(Seq(rop)), Some(Seq(dop))) =>
              wop.extra.get("test.extra") should equal(wop.copy(extra = Some(wop.extra.get - "test.extra")))
              rop.extra.get("test.extra") should equal(rop.copy(extra = Some(rop.extra.get - "test.extra")))
              dop.extra.get("test.extra") should equal(dop.copy(extra = None))
          }

          plan.extraInfo.get("test.extra") should equal(plan.copy(id = None, extraInfo = Some(plan.extraInfo.get - "test.extra")))
          event.extra.get("test.extra") should equal(event.copy(extra = Some(event.extra.get - "test.extra")))
        }
      }
    }
  }

  it should "capture execution duration" in {
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val dest = tmpDest
        for {
          (plan, Seq(event)) <- captor.lineageOf {
            spark.createDataset(Seq(TestRow(1, 2.3, "text"))).write.save(dest)
          }
        } yield {
          plan should not be null
          event.error should be(empty)
          event.durationNs should not be empty
          event.durationNs.get should be > 0L
        }
      }
    }
  }

  it should "capture execution error" in {
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._
        val dest = tmpDest
        val df = spark.createDataset(Seq(TestRow(1, 2.3, "text")))
        for {
          _ <- captor.lineageOf(df.write.save(dest))
          (plan, Seq(event)) <- captor.lineageOf {
            // simulate error during execution
            Try(df.write.mode(SaveMode.ErrorIfExists).save(dest))
          }
        } yield {
          plan should not be null
          event.durationNs should be(empty)
          event.error should not be empty
          event.error.get.toString should include(s"path file:$dest already exists")
        }
      }
    }
  }

  // https://github.com/AbsaOSS/spline-spark-agent/issues/39
  it should "not capture 'data'" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        for {
          (plan, _) <- captor.lineageOf {
            spark.createDataset(Seq(TestRow(1, 2.3, "text"))).write.save(tmpDest)
          }
        } yield {

          inside(plan.operations.other.toSeq.flatten.filter(_.name contains "LocalRelation")) {
            case Seq(localRelation) =>
              assert(localRelation.params.forall(p => !p.contains("data")))
          }
        }
      }
    }

  // https://github.com/AbsaOSS/spline-spark-agent/issues/72
  it should "support lambdas" in
    withNewSparkSession { implicit spark =>
      withLineageTracking { captor =>
        import spark.implicits._

        for {
          (plan, _) <- captor.lineageOf {
            spark.createDataset(Seq(TestRow(1, 2.3, "text"))).map(_.copy(i = 2)).write.save(tmpDest)
          }
        } yield {
          plan.operations.other.get should have size 4 // LocalRelation, DeserializeToObject, Map, SerializeFromObject
          plan.operations.other.get(2).params.get should contain key "func"
        }
      }
    }
}

object LineageHarvesterSpec extends Matchers with MockitoSugar {

  case class TestRow(i: Int, d: Double, s: String)

  private def tmpDest: String = TempDirectory(pathOnly = true).deleteOnExit().path.toString

  private val integerType = dt.Simple(UUID.fromString("e63adadc-648a-56a0-9424-3289858cf0bb"), "int", nullable = false)
  private val doubleType = dt.Simple(UUID.fromString("75fe27b9-9a00-5c7d-966f-33ba32333133"), "double", nullable = false)
  private val stringType = dt.Simple(UUID.fromString("a155e715-56ab-59c4-a94b-ed1851a6984a"), "string", nullable = true)

}
