/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.context

import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import org.apache.griffin.measure.SparkSuiteBase

class DataFrameCacheTest extends AnyFlatSpec with Matchers with SparkSuiteBase {

  def createDataFrame(arr: Seq[Int]): DataFrame = {
    val schema = StructType(
      Array(
        StructField("id", LongType),
        StructField("name", StringType),
        StructField("age", IntegerType)))
    val rows = arr.map { i =>
      Row(i.toLong, s"name_$i", i + 15)
    }
    val rowRdd = spark.sparkContext.parallelize(rows)
    spark.createDataFrame(rowRdd, schema)
  }

  "data frame cache" should "be able to cache and uncache data frames" in {
    val dfCache = DataFrameCache()
    val df1 = createDataFrame(1 to 5)
    val df2 = createDataFrame(1 to 10)
    val df3 = createDataFrame(1 to 15)

    // cache
    dfCache.cacheDataFrame("t1", df1)
    dfCache.cacheDataFrame("t2", df2)
    dfCache.cacheDataFrame("t3", df3)
    dfCache.dataFrames.get("t2") should be(Some(df2))

    // uncache
    dfCache.uncacheDataFrame("t2")
    dfCache.dataFrames.get("t2") should be(None)
    dfCache.trashDataFrames.toList should be(df2 :: Nil)

    // uncache all
    dfCache.uncacheAllDataFrames()
    dfCache.dataFrames.toMap should be(Map[String, DataFrame]())
    dfCache.trashDataFrames.toList should be(df2 :: df1 :: df3 :: Nil)

    // clear all trash
    dfCache.clearAllTrashDataFrames()
    dfCache.trashDataFrames.toList should be(Nil)
  }

}
