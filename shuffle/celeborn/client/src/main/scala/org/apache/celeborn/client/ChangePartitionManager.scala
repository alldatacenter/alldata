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

import java.util
import java.util.{Set => JSet}
import java.util.concurrent.{ConcurrentHashMap, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.WorkerInfo
import org.apache.celeborn.common.protocol.PartitionLocation
import org.apache.celeborn.common.protocol.message.ControlMessages.WorkerResource
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.util.{ThreadUtils, Utils}

case class ChangePartitionRequest(
    context: RequestLocationCallContext,
    applicationId: String,
    shuffleId: Int,
    partitionId: Int,
    epoch: Int,
    oldPartition: PartitionLocation,
    causes: Option[StatusCode])

class ChangePartitionManager(
    conf: CelebornConf,
    lifecycleManager: LifecycleManager) extends Logging {

  private val pushReplicateEnabled = conf.pushReplicateEnabled
  // shuffleId -> (partitionId -> set of ChangePartition)
  private val changePartitionRequests =
    new ConcurrentHashMap[Int, ConcurrentHashMap[Integer, JSet[ChangePartitionRequest]]]()
  // shuffleId -> set of partition id
  private val inBatchPartitions = new ConcurrentHashMap[Int, JSet[Integer]]()

  private val batchHandleChangePartitionEnabled = conf.batchHandleChangePartitionEnabled
  private val batchHandleChangePartitionExecutors = ThreadUtils.newDaemonCachedThreadPool(
    "rss-lifecycle-manager-change-partition-executor",
    conf.batchHandleChangePartitionNumThreads)
  private val batchHandleChangePartitionRequestInterval =
    conf.batchHandleChangePartitionRequestInterval
  private val batchHandleChangePartitionSchedulerThread: Option[ScheduledExecutorService] =
    if (batchHandleChangePartitionEnabled) {
      Some(ThreadUtils.newDaemonSingleThreadScheduledExecutor(
        "rss-lifecycle-manager-change-partition-scheduler"))
    } else {
      None
    }

  private var batchHandleChangePartition: Option[ScheduledFuture[_]] = _

  def start(): Unit = {
    batchHandleChangePartition = batchHandleChangePartitionSchedulerThread.map {
      // noinspection ConvertExpressionToSAM
      _.scheduleAtFixedRate(
        new Runnable {
          override def run(): Unit = {
            try {
              changePartitionRequests.asScala.foreach { case (shuffleId, requests) =>
                requests.synchronized {
                  batchHandleChangePartitionExecutors.submit {
                    new Runnable {
                      override def run(): Unit = {
                        // For each partition only need handle one request
                        val distinctPartitions = requests.asScala.filter { case (partitionId, _) =>
                          !inBatchPartitions.get(shuffleId).contains(partitionId)
                        }.map { case (partitionId, request) =>
                          inBatchPartitions.get(shuffleId).add(partitionId)
                          request.asScala.toArray.maxBy(_.epoch)
                        }.toArray
                        if (distinctPartitions.nonEmpty) {
                          handleRequestPartitions(
                            distinctPartitions.head.applicationId,
                            shuffleId,
                            distinctPartitions)
                        }
                      }
                    }
                  }
                }
              }
            } catch {
              case e: InterruptedException =>
                logError("Partition split scheduler thread is shutting down, detail: ", e)
                throw e
            }
          }
        },
        0,
        batchHandleChangePartitionRequestInterval,
        TimeUnit.MILLISECONDS)
    }
  }

  def stop(): Unit = {
    batchHandleChangePartition.foreach(_.cancel(true))
    batchHandleChangePartitionSchedulerThread.foreach(ThreadUtils.shutdown(_, 800.millis))
  }

  private val rpcContextRegisterFunc =
    new util.function.Function[
      Int,
      ConcurrentHashMap[Integer, util.Set[ChangePartitionRequest]]]() {
      override def apply(s: Int): ConcurrentHashMap[Integer, util.Set[ChangePartitionRequest]] =
        new ConcurrentHashMap()
    }

  private val inBatchShuffleIdRegisterFunc = new util.function.Function[Int, util.Set[Integer]]() {
    override def apply(s: Int): util.Set[Integer] = new util.HashSet[Integer]()
  }

  def handleRequestPartitionLocation(
      context: RequestLocationCallContext,
      applicationId: String,
      shuffleId: Int,
      partitionId: Int,
      oldEpoch: Int,
      oldPartition: PartitionLocation,
      cause: Option[StatusCode] = None): Unit = {

    val changePartition = ChangePartitionRequest(
      context,
      applicationId,
      shuffleId,
      partitionId,
      oldEpoch,
      oldPartition,
      cause)
    // check if there exists request for the partition, if do just register
    val requests = changePartitionRequests.computeIfAbsent(shuffleId, rpcContextRegisterFunc)
    inBatchPartitions.computeIfAbsent(shuffleId, inBatchShuffleIdRegisterFunc)

    lifecycleManager.commitManager.registerCommitPartitionRequest(
      shuffleId,
      oldPartition,
      cause)

    requests.synchronized {
      if (requests.containsKey(partitionId)) {
        requests.get(partitionId).add(changePartition)
        logTrace(s"[handleRequestPartitionLocation] For $shuffleId, request for same partition" +
          s"$partitionId-$oldEpoch exists, register context.")
        return
      } else {
        // If new slot for the partition has been allocated, reply and return.
        // Else register and allocate for it.
        getLatestPartition(shuffleId, partitionId, oldEpoch).foreach { latestLoc =>
          context.reply(StatusCode.SUCCESS, Some(latestLoc))
          logDebug(s"New partition found, old partition $partitionId-$oldEpoch return it." +
            s" shuffleId: $shuffleId $latestLoc")
          return
        }
        val set = new util.HashSet[ChangePartitionRequest]()
        set.add(changePartition)
        requests.put(partitionId, set)
      }
    }
    if (!batchHandleChangePartitionEnabled) {
      handleRequestPartitions(applicationId, shuffleId, Array(changePartition))
    }
  }

  private def getLatestPartition(
      shuffleId: Int,
      partitionId: Int,
      epoch: Int): Option[PartitionLocation] = {
    val map = lifecycleManager.latestPartitionLocation.get(shuffleId)
    if (map != null) {
      val loc = map.get(partitionId)
      if (loc != null && loc.getEpoch > epoch) {
        return Some(loc)
      }
    }
    None
  }

  def handleRequestPartitions(
      applicationId: String,
      shuffleId: Int,
      changePartitions: Array[ChangePartitionRequest]): Unit = {
    val requestsMap = changePartitionRequests.get(shuffleId)

    val changes = changePartitions.map { change =>
      s"${change.shuffleId}-${change.partitionId}-${change.epoch}"
    }.mkString("[", ",", "]")
    logWarning(s"Batch handle change partition for $applicationId of $changes")

    // Blacklist all failed workers
    if (changePartitions.exists(_.causes.isDefined)) {
      changePartitions.filter(_.causes.isDefined).foreach { changePartition =>
        lifecycleManager.blacklistPartition(
          shuffleId,
          changePartition.oldPartition,
          changePartition.causes.get)
      }
    }

    // remove together to reduce lock time
    def replySuccess(locations: Array[PartitionLocation]): Unit = {
      requestsMap.synchronized {
        locations.map { location =>
          if (batchHandleChangePartitionEnabled) {
            inBatchPartitions.get(shuffleId).remove(location.getId)
          }
          // Here one partition id can be remove more than once,
          // so need to filter null result before reply.
          location -> Option(requestsMap.remove(location.getId))
        }
      }.foreach { case (newLocation, requests) =>
        requests.map(_.asScala.toList.foreach(_.context.reply(
          StatusCode.SUCCESS,
          Option(newLocation))))
      }
    }

    // remove together to reduce lock time
    def replyFailure(status: StatusCode): Unit = {
      requestsMap.synchronized {
        changePartitions.map { changePartition =>
          if (batchHandleChangePartitionEnabled) {
            inBatchPartitions.get(shuffleId).remove(changePartition.partitionId)
          }
          Option(requestsMap.remove(changePartition.partitionId))
        }
      }.foreach { requests =>
        requests.map(_.asScala.toList.foreach(_.context.reply(status, None)))
      }
    }

    // Get candidate worker that not in blacklist of shuffleId
    val candidates =
      lifecycleManager
        .workerSnapshots(shuffleId)
        .keySet()
        .asScala
        .filter(w => !lifecycleManager.blacklist.keySet().contains(w))
        .toList
    if (candidates.size < 1 || (pushReplicateEnabled && candidates.size < 2)) {
      logError("[Update partition] failed for not enough candidates for revive.")
      replyFailure(StatusCode.SLOT_NOT_AVAILABLE)
      return
    }

    // PartitionSplit all contains oldPartition
    val newlyAllocatedLocations =
      reallocateChangePartitionRequestSlotsFromCandidates(changePartitions.toList, candidates)

    if (!lifecycleManager.registeredShuffle.contains(shuffleId)) {
      logError(s"[handleChangePartition] shuffle $shuffleId not registered!")
      replyFailure(StatusCode.SHUFFLE_NOT_REGISTERED)
      return
    }

    if (lifecycleManager.commitManager.isStageEnd(shuffleId)) {
      logError(s"[handleChangePartition] shuffle $shuffleId already ended!")
      replyFailure(StatusCode.STAGE_ENDED)
      return
    }

    if (!lifecycleManager.reserveSlotsWithRetry(
        applicationId,
        shuffleId,
        new util.HashSet(candidates.toSet.asJava),
        newlyAllocatedLocations)) {
      logError(s"[Update partition] failed for $shuffleId.")
      replyFailure(StatusCode.RESERVE_SLOTS_FAILED)
      return
    }

    val newMasterLocations =
      newlyAllocatedLocations.asScala.flatMap {
        case (workInfo, (masterLocations, slaveLocations)) =>
          // Add all re-allocated slots to worker snapshots.
          lifecycleManager.workerSnapshots(shuffleId).asScala
            .get(workInfo)
            .foreach { partitionLocationInfo =>
              partitionLocationInfo.addMasterPartitions(masterLocations)
              lifecycleManager.updateLatestPartitionLocations(shuffleId, masterLocations)
              partitionLocationInfo.addSlavePartitions(slaveLocations)
            }
          // partition location can be null when call reserveSlotsWithRetry().
          val locations = (masterLocations.asScala ++ slaveLocations.asScala.map(_.getPeer))
            .distinct.filter(_ != null)
          if (locations.nonEmpty) {
            val changes = locations.map { partition =>
              s"(partition ${partition.getId} epoch from ${partition.getEpoch - 1} to ${partition.getEpoch})"
            }.mkString("[", ", ", "]")
            logDebug(s"[Update partition] success for " +
              s"shuffle ${Utils.makeShuffleKey(applicationId, shuffleId)}, succeed partitions: " +
              s"$changes.")
          }
          locations
      }
    replySuccess(newMasterLocations.toArray)
  }

  private def reallocateChangePartitionRequestSlotsFromCandidates(
      changePartitionRequests: List[ChangePartitionRequest],
      candidates: List[WorkerInfo]): WorkerResource = {
    val slots = new WorkerResource()
    changePartitionRequests.foreach { partition =>
      lifecycleManager.allocateFromCandidates(
        partition.partitionId,
        partition.epoch,
        candidates,
        slots)
    }
    slots
  }

  def removeExpiredShuffle(shuffleId: Int): Unit = {
    changePartitionRequests.remove(shuffleId)
    inBatchPartitions.remove(shuffleId)
  }
}
