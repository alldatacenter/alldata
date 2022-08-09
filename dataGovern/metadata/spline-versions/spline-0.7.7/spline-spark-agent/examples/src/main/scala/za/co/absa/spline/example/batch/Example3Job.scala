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

import org.apache.spark.sql.types._
import za.co.absa.spline.SparkApp

object Example3Job extends SparkApp("Example 3") {

  import org.apache.spark.sql._
  import org.apache.spark.sql.functions._
  import za.co.absa.spline.harvester.SparkLineageInitializer._

  spark.enableLineageTracking()

  val refType = StructType(Array(
    StructField("title", StringType),
    StructField("author", ArrayType(StructType(Array(
      StructField("initial", StringType),
      StructField("lastName", StringType)
    ))))
  ))

  val nasaSchema = StructType(Array(
    StructField("_subject", StringType),
    StructField("reference", StructType(Array(
      StructField("source", StructType(Array(
        StructField("journal", ArrayType(refType)),
        StructField("other", ArrayType(refType))
      )))
    )))
  ))

  val ds = spark.read
    .format("com.databricks.spark.xml")
    .option("rowTag", "dataset")
    .option("rootTag", "datasets")
    .schema(nasaSchema)
    .load("data/input/batch/nasa.xml")

  val astronomySubjectsDS = ds.filter($"_subject" === lit("astronomy")).cache
  val journalReferencesDS = astronomySubjectsDS
    .select(explode($"reference.source.journal") as "ref")
    .select($"ref.title" as "title", $"ref.author" as "authors")
  val otherReferencesDS = astronomySubjectsDS
    .select(explode($"reference.source.other") as "ref")
    .select(monotonically_increasing_id() as "id", $"ref.title" as "title", explode($"ref.author") as "author")
    .select($"id", $"title", struct($"author.initial", $"author.lastName") as "author")
    .groupBy($"id", $"title").agg(collect_list($"author") as "authors")
    .drop($"id")

  (journalReferencesDS union otherReferencesDS).limit(100)
    .write
    .mode(SaveMode.Overwrite)
    .parquet("data/output/batch/job3_results")
}
