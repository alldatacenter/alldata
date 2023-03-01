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

class WorkerPartitionLocationInfo extends Logging {

  // key: ShuffleKey, values: (partitionId -> (encodedPartitionId -> PartitionLocation))
  type PartitionInfo = ConcurrentHashMap[String, ConcurrentHashMap[Long, PartitionLocation]]
  private val masterPartitionLocations = new PartitionInfo
  private val slavePartitionLocations = new PartitionInfo

  def encode(partitionId: Int, epoch: Int): Long = {
    partitionId.toLong << 32 | epoch
  }

  def encodeUniqueId(uniqueId: String): Long = {
    val tokens = uniqueId.split("-", 2)
    val partitionId = tokens(0).toInt
    val epoch = tokens(1).toInt
    encode(partitionId, epoch)
  }

  def shuffleKeySet: util.HashSet[String] = {
    val shuffleKeySet = new util.HashSet[String]()
    shuffleKeySet.addAll(masterPartitionLocations.keySet())
    shuffleKeySet.addAll(slavePartitionLocations.keySet())
    shuffleKeySet
  }

  def containsShuffle(shuffleKey: String): Boolean = {
    masterPartitionLocations.containsKey(shuffleKey) ||
    slavePartitionLocations.containsKey(shuffleKey)
  }

  def addMasterPartitions(
      shuffleKey: String,
      locations: util.List[PartitionLocation]): Unit = {
    addPartitions(shuffleKey, locations, masterPartitionLocations)
  }

  def addSlavePartitions(
      shuffleKey: String,
      locations: util.List[PartitionLocation]): Unit = {
    addPartitions(shuffleKey, locations, slavePartitionLocations)
  }

  def getMasterLocation(shuffleKey: String, uniqueId: String): PartitionLocation = {
    getLocation(shuffleKey, uniqueId, PartitionLocation.Mode.MASTER)
  }

  def getSlaveLocation(shuffleKey: String, uniqueId: String): PartitionLocation = {
    getLocation(shuffleKey, uniqueId, PartitionLocation.Mode.SLAVE)
  }

  def removeShuffle(shuffleKey: String): Unit = {
    masterPartitionLocations.remove(shuffleKey)
    slavePartitionLocations.remove(shuffleKey)
  }

  def getAllMasterLocations(shuffleKey: String): util.List[PartitionLocation] = {
    getMasterLocations(shuffleKey)
  }

  def getAllSlaveLocations(shuffleKey: String): util.List[PartitionLocation] = {
    getSlaveLocations(shuffleKey)
  }

  def getMasterLocations(
      shuffleKey: String,
      partitionIdOpt: Option[Int] = None): util.List[PartitionLocation] = {
    getLocations(shuffleKey, masterPartitionLocations, partitionIdOpt)
  }

  def getSlaveLocations(
      shuffleKey: String,
      partitionIdOpt: Option[Int] = None): util.List[PartitionLocation] = {
    getLocations(shuffleKey, slavePartitionLocations, partitionIdOpt)
  }

  def getLocations(
      shuffleKey: String,
      partitionInfo: PartitionInfo,
      partitionIdOpt: Option[Int] = None): util.List[PartitionLocation] = {
    val partitionMap = partitionInfo.get(shuffleKey)
    if (partitionMap != null) {
      partitionIdOpt match {
        case Some(partitionId) =>
          partitionMap.values().asScala.filter(_.getId == partitionId)
            .toList.asJava
        case None =>
          new util.ArrayList(partitionMap.values())
      }
    } else {
      new util.ArrayList[PartitionLocation]()
    }
  }

  def removeMasterPartitions(shuffleKey: String): (util.Map[String, Integer], Integer) = {
    val uniqueIds = getAllMasterIds(shuffleKey)
    removeMasterPartitions(shuffleKey, uniqueIds)
  }

  def removeSlavePartitions(shuffleKey: String): (util.Map[String, Integer], Integer) = {
    val uniqueIds = getAllSlaveIds(shuffleKey)
    removeSlavePartitions(shuffleKey, uniqueIds)
  }

  def removeMasterPartitions(
      shuffleKey: String,
      uniqueIds: util.Collection[String]): (util.Map[String, Integer], Integer) = {
    removePartitions(shuffleKey, uniqueIds, masterPartitionLocations)
  }

  def removeSlavePartitions(
      shuffleKey: String,
      uniqueIds: util.Collection[String]): (util.Map[String, Integer], Integer) = {
    removePartitions(shuffleKey, uniqueIds, slavePartitionLocations)
  }

  def addPartitions(
      shuffleKey: String,
      locations: util.List[PartitionLocation],
      partitionInfo: PartitionInfo): Unit = {
    if (locations != null && locations.size() > 0) {
      partitionInfo.putIfAbsent(shuffleKey, new ConcurrentHashMap[Long, PartitionLocation]())
      val partitionMap = partitionInfo.get(shuffleKey)
      locations.asScala.foreach { loc =>
        partitionMap.putIfAbsent(encode(loc.getId, loc.getEpoch), loc)
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
      val loc = partitionMap.remove(encodeUniqueId(id))
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
      mode: PartitionLocation.Mode): PartitionLocation = {
    val partitionInfo =
      if (mode == PartitionLocation.Mode.MASTER) {
        masterPartitionLocations
      } else {
        slavePartitionLocations
      }

    val partitionMap = partitionInfo.get(shuffleKey)
    if (partitionMap != null) {
      partitionMap.get(encodeUniqueId(uniqueId))
    } else null
  }

  private def getAllIds(
      shuffleKey: String,
      partitionInfo: PartitionInfo): util.List[String] = {
    val partitionMap = partitionInfo.get(shuffleKey)
    if (partitionMap != null) {
      partitionMap.values().asScala.map(_.getUniqueId).toList.asJava
    } else null
  }

  private def getAllMasterIds(shuffleKey: String): util.List[String] = {
    getAllIds(shuffleKey, masterPartitionLocations)
  }

  private def getAllSlaveIds(shuffleKey: String): util.List[String] = {
    getAllIds(shuffleKey, slavePartitionLocations)
  }

  def isEmpty: Boolean = {
    masterPartitionLocations.isEmpty && slavePartitionLocations.isEmpty
  }

  override def toString: String = {
    s"""
       | Partition Location Info:
       | master: ${masterPartitionLocations.asScala}
       | slave: ${slavePartitionLocations.asScala}
       |""".stripMargin
  }
}
