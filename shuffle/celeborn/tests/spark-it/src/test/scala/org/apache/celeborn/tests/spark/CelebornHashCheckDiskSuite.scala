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
import org.scalatest.concurrent.Eventually._
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import org.apache.celeborn.client.ShuffleClient
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.protocol.ShuffleMode
import org.apache.celeborn.service.deploy.worker.Worker

class CelebornHashCheckDiskSuite extends SparkTestBase {

  var workers: collection.Set[Worker] = _
  override def beforeAll(): Unit = {
    logInfo("celebornHashCheckDiskSuite test initialized , setup Celeborn mini cluster")
    val masterConf = Map(
      CelebornConf.APPLICATION_HEARTBEAT_TIMEOUT.key -> "10s")
    val workerConf = Map(
      CelebornConf.WORKER_STORAGE_DIRS.key -> "/tmp:capacity=1000",
      CelebornConf.WORKER_HEARTBEAT_TIMEOUT.key -> "10s")
    workers = setUpMiniCluster(masterConf, workerConf)._2
  }

  override def beforeEach(): Unit = {
    ShuffleClient.reset()
  }

  override def afterEach(): Unit = {
    System.gc()
  }

  test("celeborn spark integration test - hash-checkDiskFull") {
    val sparkConf = new SparkConf().setAppName("celeborn-demo")
      .setMaster("local[2]")
      .set(s"spark.${CelebornConf.SHUFFLE_EXPIRED_CHECK_INTERVAL.key}", "20s")

    val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    val combineResult = combine(sparkSession)
    val groupByResult = groupBy(sparkSession)
    val repartitionResult = repartition(sparkSession)
    val sqlResult = runsql(sparkSession)
    sparkSession.stop()

    val sparkSessionEnableCeleborn = SparkSession.builder()
      .config(updateSparkConf(sparkConf, ShuffleMode.HASH))
      .getOrCreate()
    val celebornCombineResult = combine(sparkSessionEnableCeleborn)
    val celebornGroupByResult = groupBy(sparkSessionEnableCeleborn)
    val celebornRepartitionResult = repartition(sparkSessionEnableCeleborn)
    val celebornSqlResult = runsql(sparkSessionEnableCeleborn)

    assert(combineResult.equals(celebornCombineResult))
    assert(groupByResult.equals(celebornGroupByResult))
    assert(repartitionResult.equals(celebornRepartitionResult))
    assert(combineResult.equals(celebornCombineResult))
    assert(sqlResult.equals(celebornSqlResult))

    // shuffle key not expired, diskInfo.actualUsableSpace < 0, no space
    workers.foreach { worker =>
      worker.storageManager.disksSnapshot().foreach { diskInfo =>
        assert(diskInfo.actualUsableSpace < 0)
      }
    }
    sparkSessionEnableCeleborn.stop()

    logInfo("after shuffle key expired")
    eventually(timeout(60.seconds), interval(2.seconds)) {
      workers.foreach { worker =>
        // after shuffle key expired, storageManager.workingDirWriters will be empty
        worker.storageManager.workingDirWriters.values().asScala.foreach { t =>
          assert(t.size() === 0)
        }
        // after shuffle key expired, diskInfo.actualUsableSpace will equal capacity=1000
        worker.storageManager.disksSnapshot().foreach { diskInfo =>
          assert(diskInfo.actualUsableSpace === 1000)
        }
      }
    }
  }
}
