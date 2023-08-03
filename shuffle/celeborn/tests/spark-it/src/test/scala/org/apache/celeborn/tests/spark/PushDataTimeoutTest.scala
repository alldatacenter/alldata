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

import org.apache.spark.{SparkConf, SparkContextHelper}
import org.apache.spark.shuffle.celeborn.SparkShuffleManager
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.client.ShuffleClient
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.protocol.ShuffleMode
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.service.deploy.worker.PushDataHandler

class PushDataTimeoutTest extends AnyFunSuite
  with SparkTestBase
  with BeforeAndAfterEach {

  override def beforeAll(): Unit = {
    logInfo("test initialized, setup celeborn mini cluster")
    val workerConf = Map(
      CelebornConf.TEST_CLIENT_PUSH_PRIMARY_DATA_TIMEOUT.key -> "true",
      CelebornConf.TEST_WORKER_PUSH_REPLICA_DATA_TIMEOUT.key -> "true")
    // required at least 4 workers, the reason behind this requirement is that when replication is
    // enabled, there is a possibility that two workers might be added to the excluded list due to
    // primary/replica timeout issues, then there are not enough workers to do replication if
    // available workers number = 1
    setUpMiniCluster(masterConf = null, workerConf = workerConf, workerNum = 4)
  }

  override def beforeEach(): Unit = {
    ShuffleClient.reset()
    PushDataHandler.pushPrimaryDataTimeoutTested.set(false)
    PushDataHandler.pushReplicaDataTimeoutTested.set(false)
    PushDataHandler.pushPrimaryMergeDataTimeoutTested.set(false)
    PushDataHandler.pushReplicaMergeDataTimeoutTested.set(false)
  }

  override def afterEach(): Unit = {
    System.gc()
  }

  Seq(false, true).foreach { enabled =>
    test(s"celeborn spark integration test - pushdata timeout w/ replicate = $enabled") {
      val sparkConf = new SparkConf().setAppName("celeborn-demo").setMaster("local[2]")
        .set(s"spark.${CelebornConf.CLIENT_PUSH_DATA_TIMEOUT.key}", "5s")
        .set(s"spark.celeborn.data.push.timeoutCheck.interval", "2s")
        .set(s"spark.${CelebornConf.CLIENT_PUSH_REPLICATE_ENABLED.key}", enabled.toString)
        .set(s"spark.${CelebornConf.CLIENT_EXCLUDE_PEER_WORKER_ON_FAILURE_ENABLED.key}", "false")
        // make sure PushDataHandler.handlePushData be triggered
        .set(s"spark.${CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE.key}", "5")

      val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
      val sqlResult = runsql(sparkSession)

      sparkSession.stop()

      val celebornSparkSession = SparkSession.builder()
        .config(updateSparkConf(sparkConf, ShuffleMode.HASH))
        .getOrCreate()
      val celebornSqlResult = runsql(celebornSparkSession)

      assert(sqlResult.equals(celebornSqlResult))

      celebornSparkSession.stop()
      ShuffleClient.reset()

      assert(PushDataHandler.pushPrimaryDataTimeoutTested.get())
      if (enabled) {
        assert(PushDataHandler.pushReplicaDataTimeoutTested.get())
      }
    }
  }

  Seq(false, true).foreach { enabled =>
    test(s"celeborn spark integration test - pushMergeData timeout w/ replicate = $enabled") {
      val sparkConf = new SparkConf().setAppName("celeborn-demo").setMaster("local[2]")
        .set(s"spark.${CelebornConf.CLIENT_PUSH_DATA_TIMEOUT.key}", "5s")
        .set(s"spark.celeborn.data.push.timeoutCheck.interval", "2s")
        .set(s"spark.${CelebornConf.CLIENT_PUSH_REPLICATE_ENABLED.key}", enabled.toString)
        .set(s"spark.${CelebornConf.CLIENT_EXCLUDE_PEER_WORKER_ON_FAILURE_ENABLED.key}", "false")

      val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
      val sqlResult = runsql(sparkSession)

      sparkSession.stop()

      val celebornSparkSession = SparkSession.builder()
        .config(updateSparkConf(sparkConf, ShuffleMode.HASH))
        .getOrCreate()
      val celebornSqlResult = runsql(celebornSparkSession)

      assert(sqlResult.equals(celebornSqlResult))

      celebornSparkSession.stop()
      ShuffleClient.reset()
      assert(PushDataHandler.pushPrimaryMergeDataTimeoutTested.get())
      if (enabled) {
        assert(PushDataHandler.pushReplicaMergeDataTimeoutTested.get())
      }
    }
  }

  test("celeborn spark integration test - pushdata timeout will add to pushExcludedWorkers") {
    val sparkConf = new SparkConf().setAppName("celeborn-demo").setMaster("local[2]")
      .set(s"spark.${CelebornConf.CLIENT_PUSH_DATA_TIMEOUT.key}", "5s")
      .set(s"spark.${CelebornConf.CLIENT_EXCLUDE_PEER_WORKER_ON_FAILURE_ENABLED.key}", "true")
      .set(s"spark.${CelebornConf.CLIENT_PUSH_REPLICATE_ENABLED.key}", "true")
    val celebornSparkSession = SparkSession.builder()
      .config(updateSparkConf(sparkConf, ShuffleMode.HASH))
      .getOrCreate()
    try {
      combine(celebornSparkSession)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        e.getMessage.concat("Revive Failed in retry push merged data for location")
    }

    assert(PushDataHandler.pushPrimaryMergeDataTimeoutTested.get())
    assert(PushDataHandler.pushReplicaMergeDataTimeoutTested.get())
    val excludedWorkers = SparkContextHelper.env
      .shuffleManager
      .asInstanceOf[SparkShuffleManager]
      .getLifecycleManager
      .workerStatusTracker
      .excludedWorkers

    assert(excludedWorkers.size() > 0)
    excludedWorkers.asScala.foreach { case (_, (code, _)) =>
      assert(code == StatusCode.PUSH_DATA_TIMEOUT_PRIMARY ||
        code == StatusCode.PUSH_DATA_TIMEOUT_REPLICA)
    }
    celebornSparkSession.stop()
    ShuffleClient.reset()
  }
}
