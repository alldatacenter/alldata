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

package org.apache.celeborn.tests.spark

import scala.util.Random

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.service.deploy.MiniClusterFeature

trait SparkTestBase extends AnyFunSuite
  with Logging with MiniClusterFeature with BeforeAndAfterAll {
  private val sampleSeq = (1 to 78)
    .map(Random.alphanumeric)
    .toList
    .map(v => (v.toUpper, Random.nextInt(12) + 1))

  override def beforeAll(): Unit = {
    logInfo("test initialized , setup rss mini cluster")
    setUpMiniCluster(workerNum = 5)
  }

  override def afterAll(): Unit = {
    logInfo("all test complete , stop rss mini cluster")
    shutdownMiniCluster()
  }

  def updateSparkConf(sparkConf: SparkConf, sort: Boolean): SparkConf = {
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    sparkConf.set("spark.shuffle.manager", "org.apache.spark.shuffle.celeborn.RssShuffleManager")
    sparkConf.set("spark.shuffle.useOldFetchProtocol", "true")
    sparkConf.set("spark.sql.adaptive.enabled", "false")
    sparkConf.set("spark.shuffle.service.enabled", "false")
    sparkConf.set("spark.sql.adaptive.skewJoin.enabled", "false")
    sparkConf.set("spark.sql.adaptive.localShuffleReader.enabled", "false")
    sparkConf.set("spark.celeborn.master.endpoints", masterInfo._1.rpcEnv.address.toString)
    if (sort) {
      sparkConf.set("spark.celeborn.shuffle.writer", "sort")
    }
    sparkConf
  }

  def combine(sparkSession: SparkSession): collection.Map[Char, (Int, Int)] = {
    val inputRdd = sparkSession.sparkContext.parallelize(sampleSeq, 4)
    val resultWithOutRss = inputRdd
      .combineByKey(
        (k: Int) => (k, 1),
        (acc: (Int, Int), v: Int) => (acc._1 + v, acc._2 + 1),
        (acc1: (Int, Int), acc2: (Int, Int)) => (acc1._1 + acc2._1, acc1._2 + acc2._2))
      .collectAsMap()
    resultWithOutRss
  }

  def repartition(sparkSession: SparkSession): collection.Map[Char, Int] = {
    val inputRdd = sparkSession.sparkContext.parallelize(sampleSeq, 2)
    val result = inputRdd.repartition(8).reduceByKey((acc, v) => acc + v).collectAsMap()
    result
  }

  def groupBy(sparkSession: SparkSession): collection.Map[Char, String] = {
    val inputRdd = sparkSession.sparkContext.parallelize(sampleSeq, 2)
    val result = inputRdd.groupByKey().sortByKey().collectAsMap()
    result.map(k => (k._1, k._2.toList.sorted.mkString(","))).toMap
  }

  def runsql(sparkSession: SparkSession): Map[String, Long] = {
    import sparkSession.implicits._
    val df = Seq(("fa", "fb"), ("fa1", "fb1"), ("fa2", "fb2"), ("fa3", "fb3")).toDF("fa", "fb")
    df.createOrReplaceTempView("tmp")
    val result = sparkSession.sql("select fa,count(fb) from tmp group by fa order by fa")
    val outMap = result.collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    outMap
  }
}
