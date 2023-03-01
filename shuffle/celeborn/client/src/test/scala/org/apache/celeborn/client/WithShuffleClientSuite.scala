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

package org.apache.celeborn.client

import scala.collection.JavaConverters._

import org.junit.Assert

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.util.PackedPartitionId

trait WithShuffleClientSuite extends CelebornFunSuite {

  protected val celebornConf: CelebornConf = new CelebornConf()

  protected val APP = "app-1"
  protected val userIdentifier: UserIdentifier = UserIdentifier("mock", "mock")

  protected lazy val lifecycleManager: LifecycleManager = new LifecycleManager(APP, celebornConf)
  protected lazy val shuffleClient: ShuffleClientImpl = {
    val client = new ShuffleClientImpl(celebornConf, userIdentifier)
    client.setupMetaServiceRef(lifecycleManager.self)
    client
  }

  private val numMappers = 8
  private val mapId = 1
  private val attemptId = 0

  test("test register map partition task") {
    Assert.assertNotNull(lifecycleManager)
    Assert.assertNotNull(shuffleClient)
    val shuffleId = 1
    var location =
      shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId, attemptId)
    Assert.assertEquals(location.getId, PackedPartitionId.packedPartitionId(mapId, attemptId))

    // retry register
    location = shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId, attemptId)
    Assert.assertEquals(location.getId, PackedPartitionId.packedPartitionId(mapId, attemptId))

    // check all allocated slots
    var partitionLocationInfos = lifecycleManager.workerSnapshots(shuffleId).values().asScala
    var count =
      partitionLocationInfos.map(r => r.getMasterPartitions().size()).sum
    Assert.assertEquals(count, numMappers)

    // another mapId
    location =
      shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId + 1, attemptId)
    Assert.assertEquals(location.getId, PackedPartitionId.packedPartitionId(mapId + 1, attemptId))

    // another mapId with another attemptId
    location =
      shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId + 1, attemptId + 1)
    Assert.assertEquals(
      location.getId,
      PackedPartitionId.packedPartitionId(mapId + 1, attemptId + 1))

    // check all allocated all slots
    partitionLocationInfos = lifecycleManager.workerSnapshots(shuffleId).values().asScala
    logInfo(partitionLocationInfos.toString())
    count =
      partitionLocationInfos.map(r => r.getMasterPartitions().size()).sum
    Assert.assertEquals(count, numMappers + 1)
  }

  test("test map end & get reducer file group") {
    val shuffleId = 2
    shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId, attemptId)
    shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId + 1, attemptId)
    shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId + 2, attemptId)
    shuffleClient.registerMapPartitionTask(APP, shuffleId, numMappers, mapId, attemptId + 1)
    shuffleClient.mapPartitionMapperEnd(APP, shuffleId, mapId, attemptId, numMappers, mapId)
    // retry
    shuffleClient.mapPartitionMapperEnd(APP, shuffleId, mapId, attemptId, numMappers, mapId)
    // another attempt
    shuffleClient.mapPartitionMapperEnd(
      APP,
      shuffleId,
      mapId,
      attemptId + 1,
      numMappers,
      PackedPartitionId
        .packedPartitionId(mapId, attemptId + 1))
    // another mapper
    shuffleClient.mapPartitionMapperEnd(APP, shuffleId, mapId + 1, attemptId, numMappers, mapId + 1)

    // reduce file group size (for empty partitions)
    Assert.assertEquals(shuffleClient.getReduceFileGroupsMap.size(), 0)

    // reduce normal empty RssInputStream
    var stream = shuffleClient.readPartition(APP, shuffleId, 1, 1)
    Assert.assertEquals(stream.read(), -1)

    // reduce normal null partition for RssInputStream
    stream = shuffleClient.readPartition(APP, shuffleId, 3, 1)
    Assert.assertEquals(stream.read(), -1)
  }
}
