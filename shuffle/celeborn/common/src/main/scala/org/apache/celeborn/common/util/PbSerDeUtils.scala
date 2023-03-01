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

package org.apache.celeborn.common.util

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.function.IntFunction

import scala.collection.JavaConverters._

import com.google.protobuf.InvalidProtocolBufferException

import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.meta.{AppDiskUsage, AppDiskUsageSnapShot, DiskInfo, FileInfo, WorkerInfo}
import org.apache.celeborn.common.protocol._
import org.apache.celeborn.common.protocol.PartitionLocation.Mode
import org.apache.celeborn.common.protocol.message.ControlMessages.WorkerResource
import org.apache.celeborn.common.quota.ResourceConsumption

object PbSerDeUtils {

  @throws[InvalidProtocolBufferException]
  def fromPbSortedShuffleFileSet(data: Array[Byte]): util.Set[String] = {
    val pbSortedShuffleFileSet = PbSortedShuffleFileSet.parseFrom(data)
    val files = ConcurrentHashMap.newKeySet[String]()
    files.addAll(pbSortedShuffleFileSet.getFilesList)
    files
  }

  def toPbSortedShuffleFileSet(files: util.Set[String]): Array[Byte] =
    PbSortedShuffleFileSet.newBuilder
      .addAllFiles(files)
      .build
      .toByteArray

  @throws[InvalidProtocolBufferException]
  def fromPbStoreVersion(data: Array[Byte]): util.ArrayList[Integer] = {
    val pbStoreVersion = PbStoreVersion.parseFrom(data)
    val versions = new util.ArrayList[Integer]()
    versions.add(pbStoreVersion.getMajor)
    versions.add(pbStoreVersion.getMinor)
    versions
  }

  def toPbStoreVersion(major: Int, minor: Int): Array[Byte] =
    PbStoreVersion.newBuilder
      .setMajor(major)
      .setMinor(minor)
      .build.toByteArray

  def fromPbDiskInfo(pbDiskInfo: PbDiskInfo): DiskInfo =
    new DiskInfo(
      pbDiskInfo.getMountPoint,
      pbDiskInfo.getUsableSpace,
      pbDiskInfo.getAvgFlushTime,
      pbDiskInfo.getUsedSlots)
      .setStatus(Utils.toDiskStatus(pbDiskInfo.getStatus))

  def toPbDiskInfo(diskInfo: DiskInfo): PbDiskInfo =
    PbDiskInfo.newBuilder
      .setMountPoint(diskInfo.mountPoint)
      .setUsableSpace(diskInfo.actualUsableSpace)
      .setAvgFlushTime(diskInfo.avgFlushTime)
      .setUsedSlots(diskInfo.activeSlots)
      .setStatus(diskInfo.status.getValue)
      .build

  def fromPbFileInfo(pbFileInfo: PbFileInfo): FileInfo =
    new FileInfo(
      pbFileInfo.getFilePath,
      pbFileInfo.getChunkOffsetsList,
      fromPbUserIdentifier(pbFileInfo.getUserIdentifier),
      Utils.toPartitionType(pbFileInfo.getPartitionType))

  def fromPbFileInfo(pbFileInfo: PbFileInfo, userIdentifier: UserIdentifier) =
    new FileInfo(
      pbFileInfo.getFilePath,
      pbFileInfo.getChunkOffsetsList,
      userIdentifier,
      Utils.toPartitionType(pbFileInfo.getPartitionType))

  def toPbFileInfo(fileInfo: FileInfo): PbFileInfo =
    PbFileInfo.newBuilder
      .setFilePath(fileInfo.getFilePath)
      .addAllChunkOffsets(fileInfo.getChunkOffsets)
      .setUserIdentifier(toPbUserIdentifier(fileInfo.getUserIdentifier))
      .setPartitionType(fileInfo.getPartitionType.getValue)
      .build

  @throws[InvalidProtocolBufferException]
  def fromPbFileInfoMap(
      data: Array[Byte],
      cache: ConcurrentHashMap[String, UserIdentifier]): ConcurrentHashMap[String, FileInfo] = {
    val pbFileInfoMap = PbFileInfoMap.parseFrom(data)
    val fileInfoMap = new ConcurrentHashMap[String, FileInfo]
    pbFileInfoMap.getValuesMap.entrySet().asScala.foreach { entry =>
      val fileName = entry.getKey
      val pbFileInfo = entry.getValue
      val pbUserIdentifier = pbFileInfo.getUserIdentifier
      val userIdentifierKey = pbUserIdentifier.getTenantId + "-" + pbUserIdentifier.getName
      if (!cache.containsKey(userIdentifierKey)) {
        val fileInfo = fromPbFileInfo(pbFileInfo)
        cache.put(userIdentifierKey, fileInfo.getUserIdentifier)
        fileInfoMap.put(fileName, fileInfo)
      } else {
        val fileInfo = fromPbFileInfo(pbFileInfo, cache.get(userIdentifierKey))
        fileInfoMap.put(fileName, fileInfo)
      }
    }
    fileInfoMap
  }

  def toPbFileInfoMap(fileInfoMap: ConcurrentHashMap[String, FileInfo]): Array[Byte] = {
    val pbFileInfoMap = new ConcurrentHashMap[String, PbFileInfo]()
    fileInfoMap.entrySet().asScala.foreach { entry =>
      pbFileInfoMap.put(entry.getKey, toPbFileInfo(entry.getValue))
    }
    PbFileInfoMap.newBuilder.putAllValues(pbFileInfoMap).build.toByteArray
  }

  def fromPbUserIdentifier(pbUserIdentifier: PbUserIdentifier): UserIdentifier =
    UserIdentifier(pbUserIdentifier.getTenantId, pbUserIdentifier.getName)

  def toPbUserIdentifier(userIdentifier: UserIdentifier): PbUserIdentifier =
    PbUserIdentifier.newBuilder
      .setTenantId(userIdentifier.tenantId)
      .setName(userIdentifier.name)
      .build

  def fromPbResourceConsumption(pbResourceConsumption: PbResourceConsumption) = ResourceConsumption(
    pbResourceConsumption.getDiskBytesWritten,
    pbResourceConsumption.getDiskFileCount,
    pbResourceConsumption.getHdfsBytesWritten,
    pbResourceConsumption.getHdfsFileCount)

  def toPbResourceConsumption(resourceConsumption: ResourceConsumption): PbResourceConsumption =
    PbResourceConsumption.newBuilder
      .setDiskBytesWritten(resourceConsumption.diskBytesWritten)
      .setDiskFileCount(resourceConsumption.diskFileCount)
      .setHdfsBytesWritten(resourceConsumption.hdfsBytesWritten)
      .setHdfsFileCount(resourceConsumption.hdfsFileCount)
      .build

  def fromPbUserResourceConsumption(pbUserResourceConsumption: util.Map[
    String,
    PbResourceConsumption]): util.Map[UserIdentifier, ResourceConsumption] = {
    pbUserResourceConsumption.asScala.map {
      case (userIdentifierString: String, pbResourceConsumption: PbResourceConsumption) =>
        (UserIdentifier(userIdentifierString), fromPbResourceConsumption(pbResourceConsumption))
    }.asJava
  }

  def toPbUserResourceConsumption(userResourceConsumption: util.Map[
    UserIdentifier,
    ResourceConsumption]): util.Map[String, PbResourceConsumption] = {
    userResourceConsumption.asScala.map {
      case (userIdentifier: UserIdentifier, resourceConsumption: ResourceConsumption) =>
        (userIdentifier.toString, toPbResourceConsumption(resourceConsumption))
    }.asJava
  }

  def fromPbWorkerInfo(pbWorkerInfo: PbWorkerInfo): WorkerInfo = {
    val disks = new ConcurrentHashMap[String, DiskInfo]
    if (pbWorkerInfo.getDisksCount > 0) {
      pbWorkerInfo.getDisksList.asScala.foreach(pbDiskInfo =>
        disks.put(pbDiskInfo.getMountPoint, fromPbDiskInfo(pbDiskInfo)))
    }
    val userResourceConsumption =
      PbSerDeUtils.fromPbUserResourceConsumption(pbWorkerInfo.getUserResourceConsumptionMap)
    new WorkerInfo(
      pbWorkerInfo.getHost,
      pbWorkerInfo.getRpcPort,
      pbWorkerInfo.getPushPort,
      pbWorkerInfo.getFetchPort,
      pbWorkerInfo.getReplicatePort,
      disks,
      userResourceConsumption,
      null)
  }

  def toPbWorkerInfo(
      workerInfo: WorkerInfo,
      eliminateUserResourceConsumption: Boolean): PbWorkerInfo = {
    val diskInfos = workerInfo.diskInfos.values
    val pbDiskInfos = new util.ArrayList[PbDiskInfo]()
    diskInfos.asScala.foreach(k => pbDiskInfos.add(PbSerDeUtils.toPbDiskInfo(k)))
    val builder = PbWorkerInfo.newBuilder
      .setHost(workerInfo.host)
      .setRpcPort(workerInfo.rpcPort)
      .setFetchPort(workerInfo.fetchPort)
      .setPushPort(workerInfo.pushPort)
      .setReplicatePort(workerInfo.replicatePort)
      .addAllDisks(pbDiskInfos)
    if (!eliminateUserResourceConsumption) {
      builder.putAllUserResourceConsumption(
        PbSerDeUtils.toPbUserResourceConsumption(workerInfo.userResourceConsumption))
    }
    builder.build
  }

  def fromPbPartitionLocation(pbLoc: PbPartitionLocation): PartitionLocation = {
    var mode = Mode.MASTER
    if (pbLoc.getMode.equals(PbPartitionLocation.Mode.Slave)) {
      mode = Mode.SLAVE
    }
    val partitionLocation = new PartitionLocation(
      pbLoc.getId,
      pbLoc.getEpoch,
      pbLoc.getHost,
      pbLoc.getRpcPort,
      pbLoc.getPushPort,
      pbLoc.getFetchPort,
      pbLoc.getReplicatePort,
      mode,
      null,
      StorageInfo.fromPb(pbLoc.getStorageInfo),
      Utils.byteStringToRoaringBitmap(pbLoc.getMapIdBitmap))
    if (pbLoc.hasPeer) {
      val peerPb = pbLoc.getPeer
      var peerMode = Mode.MASTER
      if (peerPb.getMode eq PbPartitionLocation.Mode.Slave) peerMode = Mode.SLAVE
      val peerLocation = new PartitionLocation(
        peerPb.getId,
        peerPb.getEpoch,
        peerPb.getHost,
        peerPb.getRpcPort,
        peerPb.getPushPort,
        peerPb.getFetchPort,
        peerPb.getReplicatePort,
        peerMode,
        partitionLocation,
        StorageInfo.fromPb(peerPb.getStorageInfo),
        Utils.byteStringToRoaringBitmap(peerPb.getMapIdBitmap))
      partitionLocation.setPeer(peerLocation)
    }
    partitionLocation
  }

  def toPbPartitionLocation(location: PartitionLocation): PbPartitionLocation = {
    val builder = PbPartitionLocation.newBuilder
    if (location.getMode eq Mode.MASTER) {
      builder.setMode(PbPartitionLocation.Mode.Master)
    } else {
      builder.setMode(PbPartitionLocation.Mode.Slave)
    }
    builder
      .setHost(location.getHost)
      .setEpoch(location.getEpoch)
      .setId(location.getId)
      .setRpcPort(location.getRpcPort)
      .setPushPort(location.getPushPort)
      .setFetchPort(location.getFetchPort)
      .setReplicatePort(location.getReplicatePort)
      .setStorageInfo(StorageInfo.toPb(location.getStorageInfo))
      .setMapIdBitmap(Utils.roaringBitmapToByteString(location.getMapIdBitMap))
    if (location.getPeer != null) {
      val peerBuilder = PbPartitionLocation.newBuilder
      if (location.getPeer.getMode eq Mode.MASTER) {
        peerBuilder.setMode(PbPartitionLocation.Mode.Master)
      } else {
        peerBuilder.setMode(PbPartitionLocation.Mode.Slave)
      }
      peerBuilder
        .setHost(location.getPeer.getHost)
        .setEpoch(location.getPeer.getEpoch)
        .setId(location.getPeer.getId)
        .setRpcPort(location.getPeer.getRpcPort)
        .setPushPort(location.getPeer.getPushPort)
        .setFetchPort(location.getPeer.getFetchPort)
        .setReplicatePort(location.getPeer.getReplicatePort)
        .setStorageInfo(StorageInfo.toPb(location.getPeer.getStorageInfo))
        .setMapIdBitmap(Utils.roaringBitmapToByteString(location.getMapIdBitMap))
      builder.setPeer(peerBuilder.build)
    }
    builder.build
  }

  def fromPbWorkerResource(pbWorkerResource: util.Map[String, PbWorkerResource]): WorkerResource = {
    val slots = new WorkerResource()
    pbWorkerResource.asScala.foreach(item => {
      val Array(host, rpcPort, pushPort, fetchPort, replicatePort) = item._1.split(":")
      val workerInfo =
        new WorkerInfo(host, rpcPort.toInt, pushPort.toInt, fetchPort.toInt, replicatePort.toInt)
      val masterPartitionLocation = new util.ArrayList[PartitionLocation](item._2
        .getMasterPartitionsList.asScala.map(PbSerDeUtils.fromPbPartitionLocation).asJava)
      val slavePartitionLocation = new util.ArrayList[PartitionLocation](item._2
        .getSlavePartitionsList.asScala.map(PbSerDeUtils.fromPbPartitionLocation).asJava)
      slots.put(workerInfo, (masterPartitionLocation, slavePartitionLocation))
    })
    slots
  }

  def toPbWorkerResource(workerResource: WorkerResource): util.Map[String, PbWorkerResource] = {
    workerResource.asScala.map(item => {
      val uniqueId = item._1.toUniqueId()
      val masterPartitions = item._2._1.asScala.map(PbSerDeUtils.toPbPartitionLocation).asJava
      val slavePartitions = item._2._2.asScala.map(PbSerDeUtils.toPbPartitionLocation).asJava
      val pbWorkerResource = PbWorkerResource.newBuilder()
        .addAllMasterPartitions(masterPartitions)
        .addAllSlavePartitions(slavePartitions).build()
      uniqueId -> pbWorkerResource
    }).asJava
  }

  def fromPbAppDiskUsage(pbAppDiskUsage: PbAppDiskUsage): AppDiskUsage = {
    AppDiskUsage(pbAppDiskUsage.getAppId, pbAppDiskUsage.getEstimatedUsage)
  }

  def toPbAppDiskUsage(appDiskUsage: AppDiskUsage): PbAppDiskUsage = {
    PbAppDiskUsage.newBuilder()
      .setAppId(appDiskUsage.appId)
      .setEstimatedUsage(appDiskUsage.estimatedUsage)
      .build()
  }

  def fromPbAppDiskUsageSnapshot(
      pbAppDiskUsageSnapShot: PbAppDiskUsageSnapshot): AppDiskUsageSnapShot = {
    val snapShot = new AppDiskUsageSnapShot(pbAppDiskUsageSnapShot.getTopItemCount)
    snapShot.startSnapShotTime = pbAppDiskUsageSnapShot.getStartSnapShotTime
    snapShot.endSnapShotTime = pbAppDiskUsageSnapShot.getEndSnapshotTime
    snapShot.restoreFromSnapshot(
      pbAppDiskUsageSnapShot
        .getTopNItemsList
        .asScala
        .map(fromPbAppDiskUsage)
        .asJava
        .stream()
        .toArray(new IntFunction[Array[AppDiskUsage]]() {
          override def apply(value: Int): Array[AppDiskUsage] = new Array[AppDiskUsage](value)
        }))
    snapShot
  }

  def toPbAppDiskUsageSnapshot(snapshots: AppDiskUsageSnapShot): PbAppDiskUsageSnapshot = {
    PbAppDiskUsageSnapshot.newBuilder()
      .setTopItemCount(snapshots.topItemCount)
      .setStartSnapShotTime(snapshots.startSnapShotTime)
      .setEndSnapshotTime(snapshots.endSnapShotTime)
      // topNItems some value could be null
      .addAllTopNItems(snapshots.topNItems.filter(_ != null).map(toPbAppDiskUsage).toList.asJava)
      .build()
  }

  def toPbSnapshotMetaInfo(
      estimatedPartitionSize: java.lang.Long,
      registeredShuffle: java.util.Set[String],
      hostnameSet: java.util.Set[String],
      blacklist: java.util.Set[WorkerInfo],
      workerLostEvent: java.util.Set[WorkerInfo],
      appHeartbeatTime: java.util.Map[String, java.lang.Long],
      workers: java.util.ArrayList[WorkerInfo],
      partitionTotalWritten: java.lang.Long,
      partitionTotalFileCount: java.lang.Long,
      appDiskUsageMetricSnapshots: Array[AppDiskUsageSnapShot],
      currentAppDiskUsageMetricsSnapshot: AppDiskUsageSnapShot): PbSnapshotMetaInfo = {
    val builder = PbSnapshotMetaInfo.newBuilder()
      .setEstimatedPartitionSize(estimatedPartitionSize)
      .addAllRegisteredShuffle(registeredShuffle)
      .addAllHostnameSet(hostnameSet)
      .addAllBlacklist(blacklist.asScala.map(toPbWorkerInfo(_, true)).asJava)
      .addAllWorkerLostEvents(workerLostEvent.asScala.map(toPbWorkerInfo(_, true)).asJava)
      .putAllAppHeartbeatTime(appHeartbeatTime)
      .addAllWorkers(workers.asScala.map(toPbWorkerInfo(_, true)).asJava)
      .setPartitionTotalWritten(partitionTotalWritten)
      .setPartitionTotalFileCount(partitionTotalFileCount)
      // appDiskUsageMetricSnapshots can have null values,
      // protobuf repeated value can't support null value in list.
      .addAllAppDiskUsageMetricSnapshots(appDiskUsageMetricSnapshots.filter(_ != null)
        .map(toPbAppDiskUsageSnapshot).toList.asJava)
    if (currentAppDiskUsageMetricsSnapshot != null) {
      builder.setCurrentAppDiskUsageMetricsSnapshot(
        toPbAppDiskUsageSnapshot(currentAppDiskUsageMetricsSnapshot))
    }
    builder.build()
  }
}
