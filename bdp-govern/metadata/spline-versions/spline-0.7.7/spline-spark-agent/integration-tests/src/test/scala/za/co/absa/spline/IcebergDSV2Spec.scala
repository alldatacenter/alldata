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
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.scalatest.ConditionalTestTags.ignoreIf
import za.co.absa.commons.version.Version.VersionStringInterpolator
import za.co.absa.spline.test.fixture.spline.SplineFixture
import za.co.absa.spline.test.fixture.{SparkDatabaseFixture, SparkFixture}

class IcebergDSV2Spec extends AsyncFlatSpec
  with Matchers
  with SparkFixture
  with SplineFixture
  with SparkDatabaseFixture {

  it should "support Write to iceberg" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withRestartingSparkContext {
      withCustomSparkSession(_
        .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
        .config("spark.sql.catalog.iceberg_catalog", "org.apache.iceberg.spark.SparkCatalog")
        .config("spark.sql.catalog.iceberg_catalog.type", "hadoop")
        .config("spark.sql.catalog.iceberg_catalog.warehouse", warehouseDir)
      ) { implicit spark =>
        withLineageTracking { lineageCaptor =>
          spark.sql("CREATE TABLE iceberg_catalog.foo (id bigint, data string) USING iceberg;")

          for {
            (plan1, Seq(event1)) <- lineageCaptor.lineageOf {
              spark.sql("INSERT INTO iceberg_catalog.foo VALUES (1, 'a'), (2, 'b'), (3, 'c');")
            }
          } yield {
            plan1.id.get shouldEqual event1.planId
            plan1.operations.write.append shouldBe true
            plan1.operations.write.extra.get("destinationType") shouldBe Some("iceberg")
            plan1.operations.write.outputSource shouldBe s"file:$warehouseDir/foo"
          }
        }
      }
    }

  it should "support read from iceberg" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"3.0.0") in
    withRestartingSparkContext {
      withCustomSparkSession(_
        .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
        .config("spark.sql.catalog.iceberg_catalog", "org.apache.iceberg.spark.SparkCatalog")
        .config("spark.sql.catalog.iceberg_catalog.type", "hadoop")
        .config("spark.sql.catalog.iceberg_catalog.warehouse", warehouseDir)
      ) { implicit spark =>
        withLineageTracking { lineageCaptor =>
          spark.sql("CREATE TABLE iceberg_catalog.bar (id bigint, data string) USING iceberg;")
          spark.sql("CREATE TABLE iceberg_catalog.baz (id bigint, data string) USING iceberg;")

          for {
            (_, Seq(_)) <- lineageCaptor.lineageOf {
              spark.sql("INSERT INTO iceberg_catalog.bar VALUES (1, 'a'), (2, 'b'), (3, 'c');")
            }
            (plan2, Seq(event2)) <- lineageCaptor.lineageOf {
              spark.sql("INSERT INTO iceberg_catalog.baz SELECT * FROM iceberg_catalog.bar;")
            }
          } yield {
            plan2.id.get shouldEqual event2.planId
            plan2.operations.reads.get(0).extra.get("sourceType") shouldBe Some("iceberg")
            plan2.operations.reads.get(0).inputSources(0) shouldBe s"file:$warehouseDir/bar"
          }
        }
      }
    }
}
