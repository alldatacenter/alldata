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
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._

import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.quota.ResourceConsumption
import org.apache.celeborn.common.rpc.RpcEndpointRef
import org.apache.celeborn.common.rpc.netty.NettyRpcEndpointRef

class WorkerInfo(
    val host: String,
    val rpcPort: Int,
    val pushPort: Int,
    val fetchPort: Int,
    val replicatePort: Int,
    val diskInfos: util.Map[String, DiskInfo],
    val userResourceConsumption: util.Map[UserIdentifier, ResourceConsumption],
    var endpoint: RpcEndpointRef) extends Serializable with Logging {
  var unknownDiskSlots = new java.util.HashMap[String, Integer]()
  var lastHeartbeat: Long = 0

  def this(host: String, rpcPort: Int, pushPort: Int, fetchPort: Int, replicatePort: Int) {
    this(
      host,
      rpcPort,
      pushPort,
      fetchPort,
      replicatePort,
      new util.HashMap[String, DiskInfo](),
      new ConcurrentHashMap[UserIdentifier, ResourceConsumption](),
      null)
  }

  def this(
      host: String,
      rpcPort: Int,
      pushPort: Int,
      fetchPort: Int,
      replicatePort: Int,
      endpoint: RpcEndpointRef) {
    this(
      host,
      rpcPort,
      pushPort,
      fetchPort,
      replicatePort,
      new util.HashMap[String, DiskInfo](),
      new ConcurrentHashMap[UserIdentifier, ResourceConsumption](),
      endpoint)
  }

  val allocationBuckets = new Array[Int](61)
  var bucketIndex = 0
  var bucketTime = System.currentTimeMillis()
  var bucketAllocations = 0
  0 until allocationBuckets.length foreach { case idx =>
    allocationBuckets(idx) = 0
  }

  def isActive: Boolean = {
    endpoint.asInstanceOf[NettyRpcEndpointRef].client.isActive
  }

  def usedSlots(): Long = this.synchronized {
    diskInfos.asScala.map(_._2.activeSlots).sum +
      unknownDiskSlots.values().asScala.map(_.intValue()).sum
  }

  def allocateSlots(shuffleKey: String, slotsPerDisk: util.Map[String, Integer]): Unit =
    this.synchronized {
      logDebug(s"shuffle $shuffleKey allocations $slotsPerDisk")
      var totalSlots = 0
      slotsPerDisk.asScala.foreach { case (disk, slots) =>
        if (!diskInfos.containsKey(disk)) {
          logDebug(s"Unknown disk $disk")
          if (unknownDiskSlots.containsKey(shuffleKey)) {
            unknownDiskSlots.put(shuffleKey, slots + unknownDiskSlots.get(shuffleKey))
          } else {
            unknownDiskSlots.put(shuffleKey, slots)
          }
        } else {
          diskInfos.get(disk).allocateSlots(shuffleKey, slots)
        }
        totalSlots += slots
      }

      val current = System.currentTimeMillis()
      if (current - bucketTime > 60 * 1000) {
        bucketIndex = (bucketIndex + 1) % allocationBuckets.length
        allocationBuckets(bucketIndex) = 0
        bucketTime = current
      }
      allocationBuckets(bucketIndex) = allocationBuckets(bucketIndex) + totalSlots
    }

  def releaseSlots(shuffleKey: String, slots: util.Map[String, Integer]): Unit = this.synchronized {
    slots.asScala.foreach { case (disk, slot) =>
      if (diskInfos.containsKey(disk)) {
        diskInfos.get(disk).releaseSlots(shuffleKey, slot)
      } else {
        if (unknownDiskSlots.containsKey(shuffleKey)) {
          unknownDiskSlots.put(shuffleKey, unknownDiskSlots.get(shuffleKey) - slot)
        }
      }
    }
  }

  def releaseSlots(shuffleKey: String): Unit = this.synchronized {
    diskInfos.asScala.foreach(_._2.releaseSlots(shuffleKey))
    unknownDiskSlots.remove(shuffleKey)
  }

  def getShuffleKeySet(): util.HashSet[String] = this.synchronized {
    val shuffleKeySet = new util.HashSet[String]()
    diskInfos.values().asScala.foreach { diskInfo =>
      shuffleKeySet.addAll(diskInfo.getShuffleKeySet())
    }
    shuffleKeySet
  }

  def allocationsInLastHour(): Int = this.synchronized {
    var total = 0
    1 to 60 foreach { case delta =>
      total += allocationBuckets((bucketIndex + delta) % allocationBuckets.length)
    }
    total
  }

  def hasSameInfoWith(other: WorkerInfo): Boolean = {
    rpcPort == other.rpcPort &&
    pushPort == other.pushPort &&
    host == other.host &&
    fetchPort == other.fetchPort &&
    replicatePort == other.replicatePort
  }

  def setupEndpoint(endpointRef: RpcEndpointRef): Unit = {
    if (this.endpoint == null) {
      this.endpoint = endpointRef
    }
  }

  def readableAddress(): String = {
    s"Host:$host:RpcPort:$rpcPort:PushPort:$pushPort:" +
      s"FetchPort:$fetchPort:ReplicatePort:$replicatePort"
  }

  def toUniqueId(): String = {
    s"$host:$rpcPort:$pushPort:$fetchPort:$replicatePort"
  }

  def slotAvailable(): Boolean = this.synchronized {
    diskInfos.asScala.exists { case (_, disk) => (disk.maxSlots - disk.activeSlots) > 0 }
  }

  def getTotalSlots(): Long = this.synchronized {
    diskInfos.asScala.map(_._2.maxSlots).sum
  }

  def updateDiskMaxSlots(estimatedPartitionSize: Long): Unit = this.synchronized {
    diskInfos.asScala.foreach { case (_, disk) =>
      disk.maxSlots_$eq(disk.actualUsableSpace / estimatedPartitionSize)
    }
  }

  def totalAvailableSlots(): Long = this.synchronized {
    diskInfos.asScala.map(_._2.availableSlots()).sum
  }

  def updateThenGetDiskInfos(
      newDiskInfos: java.util.Map[String, DiskInfo],
      estimatedPartitionSize: Long): util.Map[String, DiskInfo] = this.synchronized {
    import scala.collection.JavaConverters._
    for (newDisk <- newDiskInfos.values().asScala) {
      val mountPoint: String = newDisk.mountPoint
      val curDisk = diskInfos.get(mountPoint)
      if (curDisk != null) {
        curDisk.actualUsableSpace_$eq(newDisk.actualUsableSpace)
        curDisk.activeSlots_$eq(Math.max(curDisk.activeSlots, newDisk.activeSlots))
        curDisk.avgFlushTime_$eq(newDisk.avgFlushTime)
        curDisk.maxSlots_$eq(curDisk.actualUsableSpace / estimatedPartitionSize)
        curDisk.setStatus(newDisk.status)
      } else {
        newDisk.maxSlots_$eq(newDisk.actualUsableSpace / estimatedPartitionSize)
        diskInfos.put(mountPoint, newDisk)
      }
    }

    val nonExistsMountPoints: java.util.Set[String] = new util.HashSet[String]
    nonExistsMountPoints.addAll(diskInfos.keySet)
    nonExistsMountPoints.removeAll(newDiskInfos.keySet)
    if (!nonExistsMountPoints.isEmpty) {
      for (nonExistsMountPoint <- nonExistsMountPoints.asScala) {
        diskInfos.remove(nonExistsMountPoint)
      }
    }
    new ConcurrentHashMap[String, DiskInfo](diskInfos)
  }

  def updateThenGetUserResourceConsumption(consumption: util.Map[
    UserIdentifier,
    ResourceConsumption]): util.Map[UserIdentifier, ResourceConsumption] = {
    userResourceConsumption.clear()
    userResourceConsumption.putAll(consumption)
    userResourceConsumption
  }

  override def toString(): String = {
    val (diskInfosString, slots) =
      if (diskInfos == null || diskInfos.isEmpty) {
        ("empty", 0)
      } else if (diskInfos != null) {
        val str = diskInfos.values().asScala.zipWithIndex.map { case (diskInfo, index) =>
          s"\n  DiskInfo${index}: ${diskInfo}"
        }.mkString("")
        (str, usedSlots)
      }
    val userResourceConsumptionString =
      if (userResourceConsumption == null || userResourceConsumption.isEmpty) {
        "empty"
      } else if (userResourceConsumption != null) {
        userResourceConsumption.asScala.map { case (userIdentifier, resourceConsumption) =>
          s"\n  UserIdentifier: ${userIdentifier}, ResourceConsumption: ${resourceConsumption}"
        }.mkString("")
      }
    s"""
       |Host: $host
       |RpcPort: $rpcPort
       |PushPort: $pushPort
       |FetchPort: $fetchPort
       |ReplicatePort: $replicatePort
       |SlotsUsed: $slots
       |LastHeartbeat: $lastHeartbeat
       |Disks: $diskInfosString
       |UserResourceConsumption: $userResourceConsumptionString
       |WorkerRef: $endpoint
       |""".stripMargin
  }

  override def equals(other: Any): Boolean = other match {
    case that: WorkerInfo =>
      host == that.host &&
        rpcPort == that.rpcPort &&
        pushPort == that.pushPort &&
        fetchPort == that.fetchPort &&
        replicatePort == that.replicatePort
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(host, rpcPort, pushPort, fetchPort, replicatePort)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object WorkerInfo {

  def fromUniqueId(id: String): WorkerInfo = {
    val Array(host, rpcPort, pushPort, fetchPort, replicatePort) = id.split(":")
    new WorkerInfo(host, rpcPort.toInt, pushPort.toInt, fetchPort.toInt, replicatePort.toInt)
  }
}
