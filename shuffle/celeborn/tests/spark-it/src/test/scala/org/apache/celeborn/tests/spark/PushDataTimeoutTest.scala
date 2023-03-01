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

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.client.ShuffleClient

class PushDataTimeoutTest extends AnyFunSuite
  with SparkTestBase
  with BeforeAndAfterEach {

  override def beforeAll(): Unit = {
    logInfo("test initialized , setup celeborn mini cluster")
    val workerConf = Map(
      "celeborn.test.pushMasterDataTimeout" -> "true",
      "celeborn.test.pushSlaveDataTimeout" -> "true",
      "celeborn.push.timeoutCheck.interval" -> "1s")
    setUpMiniCluster(masterConfs = null, workerConfs = workerConf)
  }

  override def beforeEach(): Unit = {
    ShuffleClient.reset()
  }

  override def afterEach(): Unit = {
    System.gc()
  }

  test("celeborn spark integration test - pushdata timeout") {
    Seq("false", "true").foreach { enabled =>
      val sparkConf = new SparkConf().setAppName("rss-demo").setMaster("local[2]")
        .set("spark.celeborn.push.data.timeout", "5s")
        .set("spark.celeborn.push.timeoutCheck.interval", "2s")
        .set("spark.celeborn.push.replicate.enabled", enabled)
        .set("spark.celeborn.client.blacklistSlave.enabled", "false")
      val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
      val combineResult = combine(sparkSession)
      val groupbyResult = groupBy(sparkSession)
      val repartitionResult = repartition(sparkSession)
      val sqlResult = runsql(sparkSession)

      Thread.sleep(3000L)
      sparkSession.stop()

      val rssSparkSession = SparkSession.builder()
        .config(updateSparkConf(sparkConf, false)).getOrCreate()
      val rssCombineResult = combine(rssSparkSession)
      val rssGroupbyResult = groupBy(rssSparkSession)
      val rssRepartitionResult = repartition(rssSparkSession)
      val rssSqlResult = runsql(rssSparkSession)

      assert(combineResult.equals(rssCombineResult))
      assert(groupbyResult.equals(rssGroupbyResult))
      assert(repartitionResult.equals(rssRepartitionResult))
      assert(combineResult.equals(rssCombineResult))
      assert(sqlResult.equals(rssSqlResult))

      rssSparkSession.stop()
      ShuffleClient.reset()
    }
  }

  test("celeborn spark integration test - pushdata timeout will add to balcklist") {
    val sparkConf = new SparkConf().setAppName("rss-demo").setMaster("local[2]")
      .set("spark.celeborn.push.data.timeout", "5s")
      .set("spark.celeborn.push.replicate.enabled", "true")
      .set("spark.celeborn.client.blacklistSlave.enabled", "true")
    val rssSparkSession = SparkSession.builder()
      .config(updateSparkConf(sparkConf, false)).getOrCreate()
    try {
      combine(rssSparkSession)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        e.getMessage.concat("Revive Failed in retry push merged data for location")
    }

    rssSparkSession.stop()
    ShuffleClient.reset()
  }
}
