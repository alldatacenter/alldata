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

import scala.collection.JavaConverters._

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.client.ShuffleClient
import org.apache.celeborn.service.deploy.worker.Worker

class RssHashCheckDiskSuite extends AnyFunSuite
  with SparkTestBase
  with BeforeAndAfterEach {
  var workers: collection.Set[Worker] = null
  override def beforeAll(): Unit = {
    logInfo("RssHashCheckDiskSuite test initialized , setup rss mini cluster")
    val masterConfs = Map("celeborn.application.heartbeat.timeout" -> "10s")
    val workerConfs = Map(
      "celeborn.worker.storage.dirs" -> "/tmp:capacity=1000",
      "celeborn.worker.heartbeat.timeout" -> "10s")
    workers = setUpMiniCluster(masterConfs, workerConfs)._2
  }

  override def beforeEach(): Unit = {
    ShuffleClient.reset()
  }

  override def afterEach(): Unit = {
    System.gc()
  }

  test("celeborn spark integration test - hash-checkDiskFull") {
    val sparkConf = new SparkConf().setAppName("rss-demo").setMaster("local[2]").set(
      "spark.celeborn.shuffle.expired.checkInterval",
      "5s")
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

    // shuffle key not expired, diskInfo.actualUsableSpace < 0, no space
    workers.map(worker => {
      worker.storageManager.disksSnapshot().map(diskInfo => {
        assert(diskInfo.actualUsableSpace < 0)
      })
    })

    rssSparkSession.stop()
    // wait shuffle key expired
    Thread.sleep(30 * 1000L)
    logInfo("after shuffle key expired")
    // after shuffle key expired, storageManager.workingDirWriters will be empty
    workers.map(worker => {
      worker.storageManager.workingDirWriters.values().asScala.map(t => assert(t.size() == 0))
    })

    // after shuffle key expired, diskInfo.actualUsableSpace will equal capacity=1000
    workers.map(worker => {
      worker.storageManager.disksSnapshot().map(diskInfo => {
        assert(diskInfo.actualUsableSpace == 1000)
      })
    })

  }
}
