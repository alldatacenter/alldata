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

package org.apache.celeborn.common.protocol.message

import java.util
import java.util.UUID

import scala.collection.JavaConverters._

import org.roaringbitmap.RoaringBitmap

import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DiskInfo, WorkerInfo}
import org.apache.celeborn.common.network.protocol.TransportMessage
import org.apache.celeborn.common.protocol._
import org.apache.celeborn.common.protocol.MessageType._
import org.apache.celeborn.common.quota.ResourceConsumption
import org.apache.celeborn.common.util.{PbSerDeUtils, Utils}

sealed trait Message extends Serializable

sealed trait MasterMessage extends Message

sealed abstract class MasterRequestMessage extends MasterMessage {
  var requestId: String

  def requestId_(id: String): Unit = {
    this.requestId = id
  }
}

sealed trait WorkerMessage extends Message

sealed trait ClientMessage extends Message

object ControlMessages extends Logging {
  val ZERO_UUID = new UUID(0L, 0L).toString

  type WorkerResource = java.util.HashMap[
    WorkerInfo,
    (java.util.List[PartitionLocation], java.util.List[PartitionLocation])]

  /**
   * ==========================================
   * handled by master
   * ==========================================
   */
  val pbCheckForWorkerTimeout: PbCheckForWorkerTimeout =
    PbCheckForWorkerTimeout.newBuilder().build()

  case object CheckForApplicationTimeOut extends Message

  case object RemoveExpiredShuffle extends Message

  /**
   * The response message for one-way message. Due to the Master HA, we must know whether a Master
   * is the leader, and the one-way message does not care about the response, so there is no
   * corresponding response message. So add a response message to get the response.
   */
  case object OneWayMessageResponse extends Message

  object RegisterWorker {
    def apply(
        host: String,
        rpcPort: Int,
        pushPort: Int,
        fetchPort: Int,
        replicatePort: Int,
        disks: Map[String, DiskInfo],
        userResourceConsumption: Map[UserIdentifier, ResourceConsumption],
        requestId: String): PbRegisterWorker = {
      val pbDisks = disks.values.map(PbSerDeUtils.toPbDiskInfo).asJava
      val pbUserResourceConsumption =
        PbSerDeUtils.toPbUserResourceConsumption(userResourceConsumption.asJava)
      PbRegisterWorker.newBuilder()
        .setHost(host)
        .setRpcPort(rpcPort)
        .setPushPort(pushPort)
        .setFetchPort(fetchPort)
        .setReplicatePort(replicatePort)
        .addAllDisks(pbDisks)
        .putAllUserResourceConsumption(pbUserResourceConsumption)
        .setRequestId(requestId)
        .build()
    }
  }

  case class HeartbeatFromWorker(
      host: String,
      rpcPort: Int,
      pushPort: Int,
      fetchPort: Int,
      replicatePort: Int,
      disks: Seq[DiskInfo],
      userResourceConsumption: util.Map[UserIdentifier, ResourceConsumption],
      activeShuffleKeys: util.Set[String],
      estimatedAppDiskUsage: util.HashMap[String, java.lang.Long],
      override var requestId: String = ZERO_UUID) extends MasterRequestMessage

  case class HeartbeatResponse(
      expiredShuffleKeys: util.HashSet[String],
      registered: Boolean) extends MasterMessage

  object RegisterShuffle {
    def apply(
        appId: String,
        shuffleId: Int,
        numMappers: Int,
        numPartitions: Int): PbRegisterShuffle =
      PbRegisterShuffle.newBuilder()
        .setApplicationId(appId)
        .setShuffleId(shuffleId)
        .setNumMapppers(numMappers)
        .setNumPartitions(numPartitions)
        .build()
  }

  object RegisterMapPartitionTask {
    def apply(
        appId: String,
        shuffleId: Int,
        numMappers: Int,
        mapId: Int,
        attemptId: Int,
        partitionId: Int): PbRegisterMapPartitionTask =
      PbRegisterMapPartitionTask.newBuilder()
        .setApplicationId(appId)
        .setShuffleId(shuffleId)
        .setNumMappers(numMappers)
        .setMapId(mapId)
        .setAttemptId(attemptId)
        .setPartitionId(partitionId)
        .build()
  }

  object RegisterShuffleResponse {
    def apply(
        status: StatusCode,
        partitionLocations: Array[PartitionLocation]): PbRegisterShuffleResponse =
      PbRegisterShuffleResponse.newBuilder()
        .setStatus(status.getValue)
        .addAllPartitionLocations(
          partitionLocations.map(PbSerDeUtils.toPbPartitionLocation).toSeq.asJava)
        .build()
  }

  case class RequestSlots(
      applicationId: String,
      shuffleId: Int,
      partitionIdList: util.ArrayList[Integer],
      hostname: String,
      shouldReplicate: Boolean,
      userIdentifier: UserIdentifier,
      override var requestId: String = ZERO_UUID)
    extends MasterRequestMessage

  case class ReleaseSlots(
      applicationId: String,
      shuffleId: Int,
      workerIds: util.List[String],
      slots: util.List[util.Map[String, Integer]],
      override var requestId: String = ZERO_UUID)
    extends MasterRequestMessage

  case class ReleaseSlotsResponse(status: StatusCode)
    extends MasterMessage

  case class RequestSlotsResponse(
      status: StatusCode,
      workerResource: WorkerResource)
    extends MasterMessage

  object Revive {
    def apply(
        appId: String,
        shuffleId: Int,
        mapId: Int,
        attemptId: Int,
        partitionId: Int,
        epoch: Int,
        oldPartition: PartitionLocation,
        cause: StatusCode): PbRevive =
      PbRevive.newBuilder()
        .setApplicationId(appId)
        .setShuffleId(shuffleId)
        .setMapId(mapId)
        .setAttemptId(attemptId)
        .setPartitionId(partitionId)
        .setEpoch(epoch)
        .setOldPartition(PbSerDeUtils.toPbPartitionLocation(oldPartition))
        .setStatus(cause.getValue)
        .build()
  }

  object PartitionSplit {
    def apply(
        appId: String,
        shuffleId: Int,
        partitionId: Int,
        epoch: Int,
        oldPartition: PartitionLocation): PbPartitionSplit =
      PbPartitionSplit.newBuilder()
        .setApplicationId(appId)
        .setShuffleId(shuffleId)
        .setPartitionId(partitionId)
        .setEpoch(epoch)
        .setOldPartition(PbSerDeUtils.toPbPartitionLocation(oldPartition))
        .build()
  }

  object ChangeLocationResponse {
    def apply(
        status: StatusCode,
        partitionLocationOpt: Option[PartitionLocation]): PbChangeLocationResponse = {
      val builder = PbChangeLocationResponse.newBuilder()
      builder.setStatus(status.getValue)
      partitionLocationOpt.foreach { partitionLocation =>
        builder.setLocation(PbSerDeUtils.toPbPartitionLocation(partitionLocation))
      }
      builder.build()
    }
  }

  case class MapperEnd(
      applicationId: String,
      shuffleId: Int,
      mapId: Int,
      attemptId: Int,
      numMappers: Int,
      partitionId: Int)
    extends MasterMessage

  case class MapperEndResponse(status: StatusCode) extends MasterMessage

  case class GetReducerFileGroup(applicationId: String, shuffleId: Int) extends MasterMessage

  // util.Set[String] -> util.Set[Path.toString]
  // Path can't be serialized
  case class GetReducerFileGroupResponse(
      status: StatusCode,
      fileGroup: util.Map[Integer, util.Set[PartitionLocation]],
      attempts: Array[Int],
      partitionIds: util.Set[Integer] = new util.HashSet[Integer]())
    extends MasterMessage

  object WorkerLost {
    def apply(
        host: String,
        rpcPort: Int,
        pushPort: Int,
        fetchPort: Int,
        replicatePort: Int,
        requestId: String): PbWorkerLost = PbWorkerLost.newBuilder()
      .setHost(host)
      .setRpcPort(rpcPort)
      .setPushPort(pushPort)
      .setFetchPort(fetchPort)
      .setReplicatePort(replicatePort)
      .setRequestId(requestId)
      .build()
  }

  object WorkerLostResponse {
    def apply(success: Boolean): PbWorkerLostResponse =
      PbWorkerLostResponse.newBuilder()
        .setSuccess(success)
        .build()
  }

  case class StageEnd(applicationId: String, shuffleId: Int) extends MasterMessage

  case class StageEndResponse(status: StatusCode)
    extends MasterMessage

  object UnregisterShuffle {
    def apply(
        appId: String,
        shuffleId: Int,
        requestId: String): PbUnregisterShuffle =
      PbUnregisterShuffle.newBuilder()
        .setAppId(appId)
        .setShuffleId(shuffleId)
        .setRequestId(requestId)
        .build()
  }

  object UnregisterShuffleResponse {
    def apply(status: StatusCode): PbUnregisterShuffleResponse =
      PbUnregisterShuffleResponse.newBuilder()
        .setStatus(status.getValue)
        .build()
  }

  case class ApplicationLost(
      appId: String,
      override var requestId: String = ZERO_UUID) extends MasterRequestMessage

  case class ApplicationLostResponse(status: StatusCode) extends MasterMessage

  case class HeartbeatFromApplication(
      appId: String,
      totalWritten: Long,
      fileCount: Long,
      override var requestId: String = ZERO_UUID) extends MasterRequestMessage

  case class GetBlacklist(localBlacklist: util.List[WorkerInfo]) extends MasterMessage

  case class GetBlacklistResponse(
      statusCode: StatusCode,
      blacklist: util.List[WorkerInfo],
      unknownWorkers: util.List[WorkerInfo]) extends Message

  case class CheckQuota(userIdentifier: UserIdentifier) extends Message

  case class CheckQuotaResponse(isAvailable: Boolean, reason: String) extends Message

  case class ReportWorkerUnavailable(
      unavailable: util.List[WorkerInfo],
      override var requestId: String = ZERO_UUID) extends MasterRequestMessage

  /**
   * ==========================================
   *         handled by worker
   *  ==========================================
   */
  object RegisterWorkerResponse {
    def apply(success: Boolean, message: String): PbRegisterWorkerResponse =
      PbRegisterWorkerResponse.newBuilder()
        .setSuccess(success)
        .setMessage(message)
        .build()
  }

  case class ReregisterWorkerResponse(success: Boolean) extends WorkerMessage

  case class ReserveSlots(
      applicationId: String,
      shuffleId: Int,
      masterLocations: util.List[PartitionLocation],
      slaveLocations: util.List[PartitionLocation],
      splitThreshold: Long,
      splitMode: PartitionSplitMode,
      partitionType: PartitionType,
      rangeReadFilter: Boolean,
      userIdentifier: UserIdentifier,
      pushDataTimeout: Long)
    extends WorkerMessage

  case class ReserveSlotsResponse(
      status: StatusCode,
      reason: String = "") extends WorkerMessage

  case class CommitFiles(
      applicationId: String,
      shuffleId: Int,
      masterIds: util.List[String],
      slaveIds: util.List[String],
      mapAttempts: Array[Int],
      epoch: Long)
    extends WorkerMessage

  case class CommitFilesResponse(
      status: StatusCode,
      committedMasterIds: util.List[String],
      committedSlaveIds: util.List[String],
      failedMasterIds: util.List[String],
      failedSlaveIds: util.List[String],
      committedMasterStorageInfos: util.Map[String, StorageInfo] =
        Map.empty[String, StorageInfo].asJava,
      committedSlaveStorageInfos: util.Map[String, StorageInfo] =
        Map.empty[String, StorageInfo].asJava,
      committedMapIdBitMap: util.Map[String, RoaringBitmap] =
        Map.empty[String, RoaringBitmap].asJava,
      totalWritten: Long = 0,
      fileCount: Int = 0) extends WorkerMessage

  case class Destroy(
      shuffleKey: String,
      masterLocations: util.List[String],
      slaveLocations: util.List[String])
    extends WorkerMessage

  case class DestroyResponse(
      status: StatusCode,
      failedMasters: util.List[String],
      failedSlaves: util.List[String])
    extends WorkerMessage

  /**
   * ==========================================
   *              common
   *  ==========================================
   */
  case class SlaveLostResponse(status: StatusCode, slaveLocation: PartitionLocation) extends Message

  case object GetWorkerInfos extends Message

  case class GetWorkerInfosResponse(status: StatusCode, workerInfos: WorkerInfo*) extends Message

  case object ThreadDump extends Message

  case class ThreadDumpResponse(threadDump: String) extends Message

  // TODO change message type to GeneratedMessageV3
  def toTransportMessage(message: Any): TransportMessage = message match {
    case _: PbCheckForWorkerTimeoutOrBuilder =>
      new TransportMessage(MessageType.CHECK_FOR_WORKER_TIMEOUT, null)

    case CheckForApplicationTimeOut =>
      new TransportMessage(MessageType.CHECK_FOR_APPLICATION_TIMEOUT, null)

    case RemoveExpiredShuffle =>
      new TransportMessage(MessageType.REMOVE_EXPIRED_SHUFFLE, null)

    case pb: PbRegisterWorker =>
      new TransportMessage(MessageType.REGISTER_WORKER, pb.toByteArray)

    case HeartbeatFromWorker(
          host,
          rpcPort,
          pushPort,
          fetchPort,
          replicatePort,
          disks,
          userResourceConsumption,
          activeShuffleKeys,
          estimatedAppDiskUsage,
          requestId) =>
      val pbDisks = disks.map(PbSerDeUtils.toPbDiskInfo).asJava
      val pbUserResourceConsumption =
        PbSerDeUtils.toPbUserResourceConsumption(userResourceConsumption)
      val payload = PbHeartbeatFromWorker.newBuilder()
        .setHost(host)
        .setRpcPort(rpcPort)
        .setPushPort(pushPort)
        .setFetchPort(fetchPort)
        .addAllDisks(pbDisks)
        .putAllUserResourceConsumption(pbUserResourceConsumption)
        .setReplicatePort(replicatePort)
        .addAllActiveShuffleKeys(activeShuffleKeys)
        .putAllEstimatedAppDiskUsage(estimatedAppDiskUsage)
        .setRequestId(requestId)
        .build().toByteArray
      new TransportMessage(MessageType.HEARTBEAT_FROM_WORKER, payload)

    case HeartbeatResponse(expiredShuffleKeys, registered) =>
      val payload = PbHeartbeatResponse.newBuilder()
        .addAllExpiredShuffleKeys(expiredShuffleKeys)
        .setRegistered(registered)
        .build().toByteArray
      new TransportMessage(MessageType.HEARTBEAT_RESPONSE, payload)

    case pb: PbRegisterShuffle =>
      new TransportMessage(MessageType.REGISTER_SHUFFLE, pb.toByteArray)

    case pb: PbRegisterMapPartitionTask =>
      new TransportMessage(MessageType.REGISTER_MAP_PARTITION_TASK, pb.toByteArray)

    case pb: PbRegisterShuffleResponse =>
      new TransportMessage(MessageType.REGISTER_SHUFFLE_RESPONSE, pb.toByteArray)

    case RequestSlots(
          applicationId,
          shuffleId,
          partitionIdList,
          hostname,
          shouldReplicate,
          userIdentifier,
          requestId) =>
      val payload = PbRequestSlots.newBuilder()
        .setApplicationId(applicationId)
        .setShuffleId(shuffleId)
        .addAllPartitionIdList(partitionIdList)
        .setHostname(hostname)
        .setShouldReplicate(shouldReplicate)
        .setRequestId(requestId)
        .setUserIdentifier(PbSerDeUtils.toPbUserIdentifier(userIdentifier))
        .build().toByteArray
      new TransportMessage(MessageType.REQUEST_SLOTS, payload)

    case ReleaseSlots(applicationId, shuffleId, workerIds, slots, requestId) =>
      val pbSlots = slots.asScala.map(slot =>
        PbSlotInfo.newBuilder().putAllSlot(slot).build()).toList
      val payload = PbReleaseSlots.newBuilder()
        .setApplicationId(applicationId)
        .setShuffleId(shuffleId)
        .setRequestId(requestId)
        .addAllWorkerIds(workerIds)
        .addAllSlots(pbSlots.asJava)
        .build().toByteArray
      new TransportMessage(MessageType.RELEASE_SLOTS, payload)

    case ReleaseSlotsResponse(status) =>
      val payload = PbReleaseSlotsResponse.newBuilder()
        .setStatus(status.getValue).build().toByteArray
      new TransportMessage(MessageType.RELEASE_SLOTS_RESPONSE, payload)

    case RequestSlotsResponse(status, workerResource) =>
      val builder = PbRequestSlotsResponse.newBuilder()
        .setStatus(status.getValue)
      if (!workerResource.isEmpty) {
        builder.putAllWorkerResource(
          PbSerDeUtils.toPbWorkerResource(workerResource))
      }
      val payload = builder.build().toByteArray
      new TransportMessage(MessageType.REQUEST_SLOTS_RESPONSE, payload)

    case pb: PbRevive =>
      new TransportMessage(MessageType.REVIVE, pb.toByteArray)

    case pb: PbChangeLocationResponse =>
      new TransportMessage(MessageType.CHANGE_LOCATION_RESPONSE, pb.toByteArray)

    case MapperEnd(applicationId, shuffleId, mapId, attemptId, numMappers, partitionId) =>
      val payload = PbMapperEnd.newBuilder()
        .setApplicationId(applicationId)
        .setShuffleId(shuffleId)
        .setMapId(mapId)
        .setAttemptId(attemptId)
        .setNumMappers(numMappers)
        .setPartitionId(partitionId)
        .build().toByteArray
      new TransportMessage(MessageType.MAPPER_END, payload)

    case MapperEndResponse(status) =>
      val payload = PbMapperEndResponse.newBuilder()
        .setStatus(status.getValue)
        .build().toByteArray
      new TransportMessage(MessageType.MAPPER_END_RESPONSE, payload)

    case GetReducerFileGroup(applicationId, shuffleId) =>
      val payload = PbGetReducerFileGroup.newBuilder()
        .setApplicationId(applicationId).setShuffleId(shuffleId)
        .build().toByteArray
      new TransportMessage(MessageType.GET_REDUCER_FILE_GROUP, payload)

    case GetReducerFileGroupResponse(status, fileGroup, attempts, partitionIds) =>
      val builder = PbGetReducerFileGroupResponse
        .newBuilder()
        .setStatus(status.getValue)
      builder.putAllFileGroups(
        fileGroup.asScala.map { case (partitionId, fileGroup) =>
          (
            partitionId,
            PbFileGroup.newBuilder().addAllLocations(fileGroup.asScala.map(PbSerDeUtils
              .toPbPartitionLocation).toList.asJava).build())
        }.asJava)
      builder.addAllAttempts(attempts.map(new Integer(_)).toIterable.asJava)
      builder.addAllPartitionIds(partitionIds)
      val payload = builder.build().toByteArray
      new TransportMessage(MessageType.GET_REDUCER_FILE_GROUP_RESPONSE, payload)

    case pb: PbWorkerLost =>
      new TransportMessage(MessageType.WORKER_LOST, pb.toByteArray)

    case pb: PbWorkerLostResponse =>
      new TransportMessage(MessageType.WORKER_LOST_RESPONSE, pb.toByteArray)

    case StageEnd(applicationId, shuffleId) =>
      val payload = PbStageEnd.newBuilder()
        .setApplicationId(applicationId)
        .setShuffleId(shuffleId)
        .build().toByteArray
      new TransportMessage(MessageType.STAGE_END, payload)

    case StageEndResponse(status) =>
      val payload = PbStageEndResponse.newBuilder()
        .setStatus(status.getValue)
        .build().toByteArray
      new TransportMessage(MessageType.STAGE_END_RESPONSE, payload)

    case pb: PbUnregisterShuffle =>
      new TransportMessage(MessageType.UNREGISTER_SHUFFLE, pb.toByteArray)

    case pb: PbUnregisterShuffleResponse =>
      new TransportMessage(MessageType.UNREGISTER_SHUFFLE_RESPONSE, pb.toByteArray)

    case ApplicationLost(appId, requestId) =>
      val payload = PbApplicationLost.newBuilder()
        .setAppId(appId).setRequestId(requestId)
        .build().toByteArray
      new TransportMessage(MessageType.APPLICATION_LOST, payload)

    case ApplicationLostResponse(status) =>
      val payload = PbApplicationLostResponse.newBuilder()
        .setStatus(status.getValue).build().toByteArray
      new TransportMessage(MessageType.APPLICATION_LOST_RESPONSE, payload)

    case HeartbeatFromApplication(appId, totalWritten, fileCount, requestId) =>
      val payload = PbHeartbeatFromApplication.newBuilder()
        .setAppId(appId)
        .setRequestId(requestId)
        .setTotalWritten(totalWritten)
        .setFileCount(fileCount)
        .build().toByteArray
      new TransportMessage(MessageType.HEARTBEAT_FROM_APPLICATION, payload)

    case GetBlacklist(localBlacklist) =>
      val payload = PbGetBlacklist.newBuilder()
        .addAllLocalBlackList(localBlacklist.asScala.map { workerInfo =>
          PbSerDeUtils.toPbWorkerInfo(workerInfo, true)
        }
          .toList.asJava)
        .build().toByteArray
      new TransportMessage(MessageType.GET_BLACKLIST, payload)

    case GetBlacklistResponse(statusCode, blacklist, unknownWorkers) =>
      val builder = PbGetBlacklistResponse.newBuilder()
        .setStatus(statusCode.getValue)
      builder.addAllBlacklist(blacklist.asScala.map { workerInfo =>
        PbSerDeUtils.toPbWorkerInfo(workerInfo, true)
      }
        .toList.asJava)
      builder.addAllUnknownWorkers(unknownWorkers.asScala.map { workerInfo =>
        PbSerDeUtils.toPbWorkerInfo(workerInfo, true)
      }.toList.asJava)
      val payload = builder.build().toByteArray
      new TransportMessage(MessageType.GET_BLACKLIST_RESPONSE, payload)

    case CheckQuota(userIdentifier) =>
      val builder = PbCheckQuota.newBuilder()
        .setUserIdentifier(PbSerDeUtils.toPbUserIdentifier(userIdentifier))
      new TransportMessage(
        MessageType.CHECK_QUOTA,
        builder.build().toByteArray)

    case CheckQuotaResponse(available, reason) =>
      val payload = PbCheckQuotaResponse.newBuilder()
        .setAvailable(available)
        .setReason(reason)
        .build().toByteArray
      new TransportMessage(MessageType.CHECK_QUOTA_RESPONSE, payload)

    case ReportWorkerUnavailable(failed, requestId) =>
      val payload = PbReportWorkerUnavailable.newBuilder()
        .addAllUnavailable(failed.asScala.map { workerInfo =>
          PbSerDeUtils.toPbWorkerInfo(workerInfo, true)
        }
          .toList.asJava)
        .setRequestId(requestId).build().toByteArray
      new TransportMessage(MessageType.REPORT_WORKER_FAILURE, payload)

    case pb: PbRegisterWorkerResponse =>
      new TransportMessage(MessageType.REGISTER_WORKER_RESPONSE, pb.toByteArray)

    case ReregisterWorkerResponse(success) =>
      val payload = PbReregisterWorkerResponse.newBuilder()
        .setSuccess(success)
        .build().toByteArray
      new TransportMessage(MessageType.REREGISTER_WORKER_RESPONSE, payload)

    case ReserveSlots(
          applicationId,
          shuffleId,
          masterLocations,
          slaveLocations,
          splitThreshold,
          splitMode,
          partType,
          rangeReadFilter,
          userIdentifier,
          pushDataTimeout) =>
      val payload = PbReserveSlots.newBuilder()
        .setApplicationId(applicationId)
        .setShuffleId(shuffleId)
        .addAllMasterLocations(masterLocations.asScala
          .map(PbSerDeUtils.toPbPartitionLocation).toList.asJava)
        .addAllSlaveLocations(slaveLocations.asScala
          .map(PbSerDeUtils.toPbPartitionLocation).toList.asJava)
        .setSplitThreshold(splitThreshold)
        .setSplitMode(splitMode.getValue)
        .setPartitionType(partType.getValue)
        .setRangeReadFilter(rangeReadFilter)
        .setUserIdentifier(PbSerDeUtils.toPbUserIdentifier(userIdentifier))
        .setPushDataTimeout(pushDataTimeout)
        .build().toByteArray
      new TransportMessage(MessageType.RESERVE_SLOTS, payload)

    case ReserveSlotsResponse(status, reason) =>
      val payload = PbReserveSlotsResponse.newBuilder()
        .setStatus(status.getValue).setReason(reason)
        .build().toByteArray
      new TransportMessage(MessageType.RESERVE_SLOTS_RESPONSE, payload)

    case CommitFiles(applicationId, shuffleId, masterIds, slaveIds, mapAttempts, epoch) =>
      val payload = PbCommitFiles.newBuilder()
        .setApplicationId(applicationId)
        .setShuffleId(shuffleId)
        .addAllMasterIds(masterIds)
        .addAllSlaveIds(slaveIds)
        .addAllMapAttempts(mapAttempts.map(new Integer(_)).toIterable.asJava)
        .setEpoch(epoch)
        .build().toByteArray
      new TransportMessage(MessageType.COMMIT_FILES, payload)

    case CommitFilesResponse(
          status,
          committedMasterIds,
          committedSlaveIds,
          failedMasterIds,
          failedSlaveIds,
          committedMasterStorageInfos,
          committedSlaveStorageInfos,
          committedMapIdBitMap,
          totalWritten,
          fileCount) =>
      val builder = PbCommitFilesResponse.newBuilder()
        .setStatus(status.getValue)
      builder.addAllCommittedMasterIds(committedMasterIds)
      builder.addAllCommittedSlaveIds(committedSlaveIds)
      builder.addAllFailedMasterIds(failedMasterIds)
      builder.addAllFailedSlaveIds(failedSlaveIds)
      committedMasterStorageInfos.asScala.foreach(entry =>
        builder.putCommittedMasterStorageInfos(entry._1, StorageInfo.toPb(entry._2)))
      committedSlaveStorageInfos.asScala.foreach(entry =>
        builder.putCommittedSlaveStorageInfos(entry._1, StorageInfo.toPb(entry._2)))
      committedMapIdBitMap.asScala.foreach(entry => {
        builder.putMapIdBitmap(entry._1, Utils.roaringBitmapToByteString(entry._2))
      })
      builder.setTotalWritten(totalWritten)
      builder.setFileCount(fileCount)
      val payload = builder.build().toByteArray
      new TransportMessage(MessageType.COMMIT_FILES_RESPONSE, payload)

    case Destroy(shuffleKey, masterLocations, slaveLocations) =>
      val payload = PbDestroy.newBuilder()
        .setShuffleKey(shuffleKey)
        .addAllMasterLocations(masterLocations)
        .addAllSlaveLocation(slaveLocations)
        .build().toByteArray
      new TransportMessage(MessageType.DESTROY, payload)

    case DestroyResponse(status, failedMasters, failedSlaves) =>
      val builder = PbDestroyResponse.newBuilder()
        .setStatus(status.getValue)
      builder.addAllFailedMasters(failedMasters)
      builder.addAllFailedSlaves(failedSlaves)
      val payload = builder.build().toByteArray
      new TransportMessage(MessageType.DESTROY_RESPONSE, payload)

    case SlaveLostResponse(status, slaveLocation) =>
      val payload = PbSlaveLostResponse.newBuilder()
        .setStatus(status.getValue)
        .setSlaveLocation(PbSerDeUtils.toPbPartitionLocation(slaveLocation))
        .build().toByteArray
      new TransportMessage(MessageType.SLAVE_LOST_RESPONSE, payload)

    case GetWorkerInfos =>
      new TransportMessage(MessageType.GET_WORKER_INFO, null)

    case GetWorkerInfosResponse(status, workerInfos @ _*) =>
      val payload = PbGetWorkerInfosResponse.newBuilder()
        .setStatus(status.getValue)
        .addAllWorkerInfos(workerInfos.map { workerInfo =>
          PbSerDeUtils.toPbWorkerInfo(workerInfo, false)
        }
          .toList.asJava)
        .build().toByteArray
      new TransportMessage(MessageType.GET_WORKER_INFO_RESPONSE, payload)

    case ThreadDump =>
      new TransportMessage(MessageType.THREAD_DUMP, null)

    case ThreadDumpResponse(threadDump) =>
      val payload = PbThreadDumpResponse.newBuilder()
        .setThreadDump(threadDump).build().toByteArray
      new TransportMessage(MessageType.THREAD_DUMP_RESPONSE, payload)

    case pb: PbPartitionSplit =>
      new TransportMessage(MessageType.PARTITION_SPLIT, pb.toByteArray)

    case OneWayMessageResponse =>
      new TransportMessage(MessageType.ONE_WAY_MESSAGE_RESPONSE, null)
  }

  // TODO change return type to GeneratedMessageV3
  def fromTransportMessage(message: TransportMessage): Any = {
    message.getType match {
      case UNKNOWN_MESSAGE | UNRECOGNIZED =>
        val msg = s"received unknown message $message"
        logError(msg)
        throw new UnsupportedOperationException(msg)

      case REGISTER_WORKER =>
        PbRegisterWorker.parseFrom(message.getPayload)

      case HEARTBEAT_FROM_WORKER =>
        val pbHeartbeatFromWorker = PbHeartbeatFromWorker.parseFrom(message.getPayload)
        val estimatedAppDiskUsage = new util.HashMap[String, java.lang.Long]()
        val userResourceConsumption = PbSerDeUtils.fromPbUserResourceConsumption(
          pbHeartbeatFromWorker.getUserResourceConsumptionMap)
        val pbDisks = pbHeartbeatFromWorker.getDisksList.asScala.map(PbSerDeUtils.fromPbDiskInfo)
        if (!pbHeartbeatFromWorker.getEstimatedAppDiskUsageMap.isEmpty) {
          estimatedAppDiskUsage.putAll(pbHeartbeatFromWorker.getEstimatedAppDiskUsageMap)
        }
        val activeShuffleKeys = new util.HashSet[String]()
        if (!pbHeartbeatFromWorker.getActiveShuffleKeysList.isEmpty) {
          activeShuffleKeys.addAll(pbHeartbeatFromWorker.getActiveShuffleKeysList)
        }
        HeartbeatFromWorker(
          pbHeartbeatFromWorker.getHost,
          pbHeartbeatFromWorker.getRpcPort,
          pbHeartbeatFromWorker.getPushPort,
          pbHeartbeatFromWorker.getFetchPort,
          pbHeartbeatFromWorker.getReplicatePort,
          pbDisks,
          userResourceConsumption,
          activeShuffleKeys,
          estimatedAppDiskUsage,
          pbHeartbeatFromWorker.getRequestId)

      case HEARTBEAT_RESPONSE =>
        val pbHeartbeatResponse = PbHeartbeatResponse.parseFrom(message.getPayload)
        val expiredShuffleKeys = new util.HashSet[String]()
        if (pbHeartbeatResponse.getExpiredShuffleKeysCount > 0) {
          expiredShuffleKeys.addAll(pbHeartbeatResponse.getExpiredShuffleKeysList)
        }
        HeartbeatResponse(expiredShuffleKeys, pbHeartbeatResponse.getRegistered)

      case REGISTER_SHUFFLE =>
        PbRegisterShuffle.parseFrom(message.getPayload)

      case REGISTER_MAP_PARTITION_TASK =>
        PbRegisterMapPartitionTask.parseFrom(message.getPayload)

      case REGISTER_SHUFFLE_RESPONSE =>
        PbRegisterShuffleResponse.parseFrom(message.getPayload)

      case REQUEST_SLOTS =>
        val pbRequestSlots = PbRequestSlots.parseFrom(message.getPayload)
        val userIdentifier = PbSerDeUtils.fromPbUserIdentifier(pbRequestSlots.getUserIdentifier)
        RequestSlots(
          pbRequestSlots.getApplicationId,
          pbRequestSlots.getShuffleId,
          new util.ArrayList[Integer](pbRequestSlots.getPartitionIdListList),
          pbRequestSlots.getHostname,
          pbRequestSlots.getShouldReplicate,
          userIdentifier,
          pbRequestSlots.getRequestId)

      case RELEASE_SLOTS =>
        val pbReleaseSlots = PbReleaseSlots.parseFrom(message.getPayload)
        val slotsList = pbReleaseSlots.getSlotsList.asScala.map(pbSlot =>
          new util.HashMap[String, Integer](pbSlot.getSlotMap)).toList.asJava
        ReleaseSlots(
          pbReleaseSlots.getApplicationId,
          pbReleaseSlots.getShuffleId,
          new util.ArrayList[String](pbReleaseSlots.getWorkerIdsList),
          new util.ArrayList[util.Map[String, Integer]](slotsList),
          pbReleaseSlots.getRequestId)

      case RELEASE_SLOTS_RESPONSE =>
        val pbReleaseSlotsResponse = PbReleaseSlotsResponse.parseFrom(message.getPayload)
        ReleaseSlotsResponse(Utils.toStatusCode(pbReleaseSlotsResponse.getStatus))

      case REQUEST_SLOTS_RESPONSE =>
        val pbRequestSlotsResponse = PbRequestSlotsResponse.parseFrom(message.getPayload)
        RequestSlotsResponse(
          Utils.toStatusCode(pbRequestSlotsResponse.getStatus),
          PbSerDeUtils.fromPbWorkerResource(
            pbRequestSlotsResponse.getWorkerResourceMap))

      case REVIVE =>
        PbRevive.parseFrom(message.getPayload)

      case CHANGE_LOCATION_RESPONSE =>
        PbChangeLocationResponse.parseFrom(message.getPayload)

      case MAPPER_END =>
        val pbMapperEnd = PbMapperEnd.parseFrom(message.getPayload)
        MapperEnd(
          pbMapperEnd.getApplicationId,
          pbMapperEnd.getShuffleId,
          pbMapperEnd.getMapId,
          pbMapperEnd.getAttemptId,
          pbMapperEnd.getNumMappers,
          pbMapperEnd.getPartitionId)

      case MAPPER_END_RESPONSE =>
        val pbMapperEndResponse = PbMapperEndResponse.parseFrom(message.getPayload)
        MapperEndResponse(Utils.toStatusCode(pbMapperEndResponse.getStatus))

      case GET_REDUCER_FILE_GROUP =>
        val pbGetReducerFileGroup = PbGetReducerFileGroup.parseFrom(message.getPayload)
        GetReducerFileGroup(
          pbGetReducerFileGroup.getApplicationId,
          pbGetReducerFileGroup.getShuffleId)

      case GET_REDUCER_FILE_GROUP_RESPONSE =>
        val pbGetReducerFileGroupResponse = PbGetReducerFileGroupResponse
          .parseFrom(message.getPayload)
        val fileGroup = pbGetReducerFileGroupResponse.getFileGroupsMap.asScala.map {
          case (partitionId, fileGroup) =>
            (
              partitionId,
              fileGroup.getLocationsList.asScala.map(
                PbSerDeUtils.fromPbPartitionLocation).toSet.asJava)
        }.asJava

        val attempts = pbGetReducerFileGroupResponse.getAttemptsList.asScala.map(_.toInt).toArray
        val partitionIds = new util.HashSet(pbGetReducerFileGroupResponse.getPartitionIdsList)
        GetReducerFileGroupResponse(
          Utils.toStatusCode(pbGetReducerFileGroupResponse.getStatus),
          fileGroup,
          attempts,
          partitionIds)

      case UNREGISTER_SHUFFLE =>
        PbUnregisterShuffle.parseFrom(message.getPayload)

      case UNREGISTER_SHUFFLE_RESPONSE =>
        PbUnregisterShuffleResponse.parseFrom(message.getPayload)

      case APPLICATION_LOST =>
        val pbApplicationLost = PbApplicationLost.parseFrom(message.getPayload)
        ApplicationLost(pbApplicationLost.getAppId, pbApplicationLost.getRequestId)

      case APPLICATION_LOST_RESPONSE =>
        val pbApplicationLostResponse = PbApplicationLostResponse.parseFrom(message.getPayload)
        ApplicationLostResponse(Utils.toStatusCode(pbApplicationLostResponse.getStatus))

      case HEARTBEAT_FROM_APPLICATION =>
        val pbHeartbeatFromApplication = PbHeartbeatFromApplication.parseFrom(message.getPayload)
        HeartbeatFromApplication(
          pbHeartbeatFromApplication.getAppId,
          pbHeartbeatFromApplication.getTotalWritten,
          pbHeartbeatFromApplication.getFileCount,
          pbHeartbeatFromApplication.getRequestId)

      case GET_BLACKLIST =>
        val pbGetBlacklist = PbGetBlacklist.parseFrom(message.getPayload)
        GetBlacklist(new util.ArrayList[WorkerInfo](pbGetBlacklist.getLocalBlackListList.asScala
          .map(PbSerDeUtils.fromPbWorkerInfo).toList.asJava))

      case GET_BLACKLIST_RESPONSE =>
        val pbGetBlacklistResponse = PbGetBlacklistResponse.parseFrom(message.getPayload)
        GetBlacklistResponse(
          Utils.toStatusCode(pbGetBlacklistResponse.getStatus),
          pbGetBlacklistResponse.getBlacklistList.asScala
            .map(PbSerDeUtils.fromPbWorkerInfo).toList.asJava,
          pbGetBlacklistResponse.getUnknownWorkersList.asScala
            .map(PbSerDeUtils.fromPbWorkerInfo).toList.asJava)

      case CHECK_QUOTA =>
        val pbCheckAvailable = PbCheckQuota.parseFrom(message.getPayload)
        CheckQuota(PbSerDeUtils.fromPbUserIdentifier(pbCheckAvailable.getUserIdentifier))

      case CHECK_QUOTA_RESPONSE =>
        val pbCheckAvailableResponse = PbCheckQuotaResponse
          .parseFrom(message.getPayload)
        CheckQuotaResponse(
          pbCheckAvailableResponse.getAvailable,
          pbCheckAvailableResponse.getReason)

      case REPORT_WORKER_FAILURE =>
        val pbReportWorkerUnavailable = PbReportWorkerUnavailable.parseFrom(message.getPayload)
        ReportWorkerUnavailable(
          new util.ArrayList[WorkerInfo](pbReportWorkerUnavailable.getUnavailableList
            .asScala.map(PbSerDeUtils.fromPbWorkerInfo).toList.asJava),
          pbReportWorkerUnavailable.getRequestId)

      case REGISTER_WORKER_RESPONSE =>
        PbRegisterWorkerResponse.parseFrom(message.getPayload)

      case REREGISTER_WORKER_RESPONSE =>
        val pbReregisterWorkerResponse = PbReregisterWorkerResponse.parseFrom(message.getPayload)
        ReregisterWorkerResponse(pbReregisterWorkerResponse.getSuccess)

      case RESERVE_SLOTS =>
        val pbReserveSlots = PbReserveSlots.parseFrom(message.getPayload)
        val userIdentifier = PbSerDeUtils.fromPbUserIdentifier(pbReserveSlots.getUserIdentifier)
        ReserveSlots(
          pbReserveSlots.getApplicationId,
          pbReserveSlots.getShuffleId,
          new util.ArrayList[PartitionLocation](pbReserveSlots.getMasterLocationsList.asScala
            .map(PbSerDeUtils.fromPbPartitionLocation).toList.asJava),
          new util.ArrayList[PartitionLocation](pbReserveSlots.getSlaveLocationsList.asScala
            .map(PbSerDeUtils.fromPbPartitionLocation).toList.asJava),
          pbReserveSlots.getSplitThreshold,
          Utils.toShuffleSplitMode(pbReserveSlots.getSplitMode),
          Utils.toPartitionType(pbReserveSlots.getPartitionType),
          pbReserveSlots.getRangeReadFilter,
          userIdentifier,
          pbReserveSlots.getPushDataTimeout)

      case RESERVE_SLOTS_RESPONSE =>
        val pbReserveSlotsResponse = PbReserveSlotsResponse.parseFrom(message.getPayload)
        ReserveSlotsResponse(
          Utils.toStatusCode(pbReserveSlotsResponse.getStatus),
          pbReserveSlotsResponse.getReason)

      case COMMIT_FILES =>
        val pbCommitFiles = PbCommitFiles.parseFrom(message.getPayload)
        CommitFiles(
          pbCommitFiles.getApplicationId,
          pbCommitFiles.getShuffleId,
          pbCommitFiles.getMasterIdsList,
          pbCommitFiles.getSlaveIdsList,
          pbCommitFiles.getMapAttemptsList.asScala.map(_.toInt).toArray,
          pbCommitFiles.getEpoch)

      case COMMIT_FILES_RESPONSE =>
        val pbCommitFilesResponse = PbCommitFilesResponse.parseFrom(message.getPayload)
        val committedMasterStorageInfos = new util.HashMap[String, StorageInfo]()
        val committedSlaveStorageInfos = new util.HashMap[String, StorageInfo]()
        val committedBitMap = new util.HashMap[String, RoaringBitmap]()
        pbCommitFilesResponse.getCommittedMasterStorageInfosMap.asScala.foreach(entry =>
          committedMasterStorageInfos.put(entry._1, StorageInfo.fromPb(entry._2)))
        pbCommitFilesResponse.getCommittedSlaveStorageInfosMap.asScala.foreach(entry =>
          committedSlaveStorageInfos.put(entry._1, StorageInfo.fromPb(entry._2)))
        pbCommitFilesResponse.getMapIdBitmapMap.asScala.foreach { entry =>
          committedBitMap.put(entry._1, Utils.byteStringToRoaringBitmap(entry._2))
        }
        CommitFilesResponse(
          Utils.toStatusCode(pbCommitFilesResponse.getStatus),
          pbCommitFilesResponse.getCommittedMasterIdsList,
          pbCommitFilesResponse.getCommittedSlaveIdsList,
          pbCommitFilesResponse.getFailedMasterIdsList,
          pbCommitFilesResponse.getFailedSlaveIdsList,
          committedMasterStorageInfos,
          committedSlaveStorageInfos,
          committedBitMap,
          pbCommitFilesResponse.getTotalWritten,
          pbCommitFilesResponse.getFileCount)

      case DESTROY =>
        val pbDestroy = PbDestroy.parseFrom(message.getPayload)
        Destroy(
          pbDestroy.getShuffleKey,
          pbDestroy.getMasterLocationsList,
          pbDestroy.getSlaveLocationList)

      case DESTROY_RESPONSE =>
        val pbDestroyResponse = PbDestroyResponse.parseFrom(message.getPayload)
        DestroyResponse(
          Utils.toStatusCode(pbDestroyResponse.getStatus),
          pbDestroyResponse.getFailedMastersList,
          pbDestroyResponse.getFailedSlavesList)

      case SLAVE_LOST_RESPONSE =>
        val pbSlaveLostResponse = PbSlaveLostResponse.parseFrom(message.getPayload)
        SlaveLostResponse(
          Utils.toStatusCode(pbSlaveLostResponse.getStatus),
          PbSerDeUtils.fromPbPartitionLocation(pbSlaveLostResponse.getSlaveLocation))

      case GET_WORKER_INFO =>
        GetWorkerInfos

      case GET_WORKER_INFO_RESPONSE =>
        val pbGetWorkerInfoResponse = PbGetWorkerInfosResponse.parseFrom(message.getPayload)
        GetWorkerInfosResponse(
          Utils.toStatusCode(pbGetWorkerInfoResponse.getStatus),
          pbGetWorkerInfoResponse.getWorkerInfosList.asScala
            .map(PbSerDeUtils.fromPbWorkerInfo).toList: _*)

      case THREAD_DUMP =>
        ThreadDump

      case THREAD_DUMP_RESPONSE =>
        val pbThreadDumpResponse = PbThreadDumpResponse.parseFrom(message.getPayload)
        ThreadDumpResponse(pbThreadDumpResponse.getThreadDump)

      case REMOVE_EXPIRED_SHUFFLE =>
        RemoveExpiredShuffle

      case ONE_WAY_MESSAGE_RESPONSE =>
        OneWayMessageResponse

      case CHECK_FOR_WORKER_TIMEOUT =>
        pbCheckForWorkerTimeout

      case CHECK_FOR_APPLICATION_TIMEOUT =>
        CheckForApplicationTimeOut

      case WORKER_LOST =>
        PbWorkerLost.parseFrom(message.getPayload)

      case WORKER_LOST_RESPONSE =>
        PbWorkerLostResponse.parseFrom(message.getPayload)

      case STAGE_END =>
        val pbStageEnd = PbStageEnd.parseFrom(message.getPayload)
        StageEnd(pbStageEnd.getApplicationId, pbStageEnd.getShuffleId)

      case PARTITION_SPLIT =>
        PbPartitionSplit.parseFrom(message.getPayload)

      case STAGE_END_RESPONSE =>
        val pbStageEndResponse = PbStageEndResponse.parseFrom(message.getPayload)
        StageEndResponse(Utils.toStatusCode(pbStageEndResponse.getStatus))
    }
  }
}
