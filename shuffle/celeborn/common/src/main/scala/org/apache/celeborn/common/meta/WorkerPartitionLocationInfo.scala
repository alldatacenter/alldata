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
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

import scala.collection.JavaConverters._

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.protocol.PartitionLocation
import org.apache.celeborn.common.util.JavaUtils

class WorkerPartitionLocationInfo extends Logging {

  // key: ShuffleKey, values: (uniqueId -> PartitionLocation))
  type PartitionInfo = ConcurrentHashMap[String, ConcurrentHashMap[String, PartitionLocation]]
  private val primaryPartitionLocations = new PartitionInfo
  private val replicaPartitionLocations = new PartitionInfo

  def shuffleKeySet: util.HashSet[String] = {
    val shuffleKeySet = new util.HashSet[String]()
    shuffleKeySet.addAll(primaryPartitionLocations.keySet())
    shuffleKeySet.addAll(replicaPartitionLocations.keySet())
    shuffleKeySet
  }

  def containsShuffle(shuffleKey: String): Boolean = {
    primaryPartitionLocations.containsKey(shuffleKey) ||
    replicaPartitionLocations.containsKey(shuffleKey)
  }

  def addPrimaryPartitions(
      shuffleKey: String,
      locations: util.List[PartitionLocation]): Unit = {
    addPartitions(shuffleKey, locations, primaryPartitionLocations)
  }

  def addReplicaPartitions(
      shuffleKey: String,
      locations: util.List[PartitionLocation]): Unit = {
    addPartitions(shuffleKey, locations, replicaPartitionLocations)
  }

  def getPrimaryLocation(shuffleKey: String, uniqueId: String): PartitionLocation = {
    getLocation(shuffleKey, uniqueId, primaryPartitionLocations)
  }

  def getPrimaryLocations(
      shuffleKey: String,
      uniqueIds: Array[String]): Array[(String, PartitionLocation)] = {
    val locations = new Array[(String, PartitionLocation)](uniqueIds.length)
    var i = 0
    while (i < uniqueIds.length) {
      val uniqueId = uniqueIds(i)
      locations(i) = uniqueId -> getPrimaryLocation(shuffleKey, uniqueId)
      i += 1
    }
    locations
  }

  def getReplicaLocation(shuffleKey: String, uniqueId: String): PartitionLocation = {
    getLocation(shuffleKey, uniqueId, replicaPartitionLocations)
  }

  def getReplicaLocations(
      shuffleKey: String,
      uniqueIds: Array[String]): Array[(String, PartitionLocation)] = {
    val locations = new Array[(String, PartitionLocation)](uniqueIds.length)
    var i = 0
    while (i < uniqueIds.length) {
      val uniqueId = uniqueIds(i)
      locations(i) = uniqueId -> getReplicaLocation(shuffleKey, uniqueId)
      i += 1
    }
    locations
  }

  def removeShuffle(shuffleKey: String): Unit = {
    primaryPartitionLocations.remove(shuffleKey)
    replicaPartitionLocations.remove(shuffleKey)
  }

  def removePrimaryPartitions(
      shuffleKey: String,
      uniqueIds: util.Collection[String]): (util.Map[String, Integer], Integer) = {
    removePartitions(shuffleKey, uniqueIds, primaryPartitionLocations)
  }

  def removeReplicaPartitions(
      shuffleKey: String,
      uniqueIds: util.Collection[String]): (util.Map[String, Integer], Integer) = {
    removePartitions(shuffleKey, uniqueIds, replicaPartitionLocations)
  }

  def addPartitions(
      shuffleKey: String,
      locations: util.List[PartitionLocation],
      partitionInfo: PartitionInfo): Unit = {
    if (locations != null && locations.size() > 0) {
      partitionInfo.putIfAbsent(
        shuffleKey,
        JavaUtils.newConcurrentHashMap[String, PartitionLocation]())
      val partitionMap = partitionInfo.get(shuffleKey)
      locations.asScala.foreach { loc =>
        partitionMap.putIfAbsent(loc.getUniqueId, loc)
      }
    }
  }

  /**
   * @param shuffleKey
   * @param uniqueIds
   * @param partitionInfo
   * @return disk related freed slot number and total freed slots number
   */
  private def removePartitions(
      shuffleKey: String,
      uniqueIds: util.Collection[String],
      partitionInfo: PartitionInfo): (util.Map[String, Integer], Integer) = {
    val partitionMap = partitionInfo.get(shuffleKey)
    if (partitionMap == null) {
      return (Map.empty[String, Integer].asJava, 0)
    }
    val locMap = new util.HashMap[String, Integer]()
    var numSlotsReleased: Int = 0
    uniqueIds.asScala.foreach { id =>
      val loc = partitionMap.remove(id)
      if (loc != null) {
        numSlotsReleased += 1
        locMap.compute(
          loc.getStorageInfo.getMountPoint,
          new BiFunction[String, Integer, Integer] {
            override def apply(t: String, u: Integer): Integer = {
              if (u == null) 1 else u + 1
            }
          })
      }
    }

    // some locations might have no disk hint
    (locMap, numSlotsReleased)
  }

  private def getLocation(
      shuffleKey: String,
      uniqueId: String,
      partitionInfo: PartitionInfo): PartitionLocation = {
    val partitionMap = partitionInfo.get(shuffleKey)
    if (partitionMap != null) {
      partitionMap.get(uniqueId)
    } else null
  }

  def isEmpty: Boolean = {
    (primaryPartitionLocations.isEmpty ||
      primaryPartitionLocations.asScala.values.forall(_.isEmpty)) &&
    (replicaPartitionLocations.isEmpty ||
      replicaPartitionLocations.asScala.values.forall(_.isEmpty))
  }

  override def toString: String = {
    s"""
       | Partition Location Info:
       | primary: ${primaryPartitionLocations.asScala}
       | replica: ${replicaPartitionLocations.asScala}
       |""".stripMargin
  }
}
