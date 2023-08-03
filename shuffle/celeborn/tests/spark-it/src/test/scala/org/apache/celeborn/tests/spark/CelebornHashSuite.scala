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
import org.apache.celeborn.common.protocol.ShuffleMode

class CelebornHashSuite extends AnyFunSuite
  with SparkTestBase
  with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    ShuffleClient.reset()
  }

  override def afterEach(): Unit = {
    System.gc()
  }

  test("celeborn spark integration test - hash") {
    val sparkConf = new SparkConf().setAppName("celeborn-demo").setMaster("local[2]")
    val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    val combineResult = combine(sparkSession)
    val groupbyResult = groupBy(sparkSession)
    val repartitionResult = repartition(sparkSession)
    val sqlResult = runsql(sparkSession)

    Thread.sleep(3000L)
    sparkSession.stop()

    val celebornSparkSession = SparkSession.builder()
      .config(updateSparkConf(sparkConf, ShuffleMode.HASH))
      .getOrCreate()
    val celebornCombineResult = combine(celebornSparkSession)
    val celebornGroupbyResult = groupBy(celebornSparkSession)
    val celebornRepartitionResult = repartition(celebornSparkSession)
    val celebornSqlResult = runsql(celebornSparkSession)

    assert(combineResult.equals(celebornCombineResult))
    assert(groupbyResult.equals(celebornGroupbyResult))
    assert(repartitionResult.equals(celebornRepartitionResult))
    assert(combineResult.equals(celebornCombineResult))
    assert(sqlResult.equals(celebornSqlResult))

    celebornSparkSession.stop()
  }
}
