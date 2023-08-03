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

package org.apache.celeborn.common.meta

import java.util

import scala.collection.JavaConverters._

import org.junit.Assert.assertEquals

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.protocol.PartitionLocation

class WorkerPartitionLocationInfoSuite extends CelebornFunSuite {

  test("CELEBORN-575: test after remove the partition location info is empty.") {
    val shuffleKey = "app_12345_12345_1-0"
    val partitionLocation00 = mockPartition(0, 0)
    val partitionLocation01 = mockPartition(0, 1)
    val partitionLocation02 = mockPartition(0, 2)
    val partitionLocation12 = mockPartition(1, 2)
    val partitionLocation11 = mockPartition(1, 1)

    val primaryLocations = new util.ArrayList[PartitionLocation]()
    primaryLocations.add(partitionLocation00)
    primaryLocations.add(partitionLocation01)
    primaryLocations.add(partitionLocation02)
    primaryLocations.add(partitionLocation11)
    primaryLocations.add(partitionLocation12)

    val replicaLocations = new util.ArrayList[PartitionLocation]()
    val partitionLocationReplica00 = mockPartition(0, 0)
    val partitionLocationReplica10 = mockPartition(1, 0)
    replicaLocations.add(partitionLocationReplica00)
    replicaLocations.add(partitionLocationReplica10)

    val workerPartitionLocationInfo = new WorkerPartitionLocationInfo
    workerPartitionLocationInfo.addPrimaryPartitions(shuffleKey, primaryLocations)
    workerPartitionLocationInfo.addReplicaPartitions(shuffleKey, replicaLocations)

    // test remove
    workerPartitionLocationInfo.removePrimaryPartitions(
      shuffleKey,
      primaryLocations.asScala.map(_.getUniqueId).asJava)
    workerPartitionLocationInfo.removeReplicaPartitions(
      shuffleKey,
      replicaLocations.asScala.map(_.getUniqueId).asJava)

    assertEquals(workerPartitionLocationInfo.isEmpty, true)
  }

  private def mockPartition(partitionId: Int, epoch: Int): PartitionLocation = {
    new PartitionLocation(
      partitionId,
      epoch,
      "mock",
      -1,
      -1,
      -1,
      -1,
      PartitionLocation.Mode.PRIMARY)
  }
}
