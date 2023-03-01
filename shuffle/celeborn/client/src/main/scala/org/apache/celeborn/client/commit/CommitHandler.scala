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

package org.apache.celeborn.client.commit

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicLong, LongAdder}

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.apache.celeborn.client.CommitManager.CommittedPartitionInfo
import org.apache.celeborn.client.LifecycleManager.{ShuffleAllocatedWorkers, ShuffleFailedWorkers, ShuffleFileGroups}
import org.apache.celeborn.client.ShuffleCommittedInfo
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{ShufflePartitionLocationInfo, WorkerInfo}
import org.apache.celeborn.common.protocol.{PartitionLocation, PartitionType}
import org.apache.celeborn.common.protocol.message.ControlMessages.{CommitFiles, CommitFilesResponse}
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.rpc.{RpcCallContext, RpcEndpointRef}
import org.apache.celeborn.common.util.{CollectionUtils, ThreadUtils, Utils}
// Can Remove this if celeborn don't support scala211 in future
import org.apache.celeborn.common.util.FunctionConverter._

case class CommitResult(
    masterPartitionLocationMap: ConcurrentHashMap[String, PartitionLocation],
    slavePartitionLocationMap: ConcurrentHashMap[String, PartitionLocation],
    commitFilesFailedWorkers: ShuffleFailedWorkers)

abstract class CommitHandler(
    appId: String,
    conf: CelebornConf,
    allocatedWorkers: ShuffleAllocatedWorkers,
    committedPartitionInfo: CommittedPartitionInfo) extends Logging {

  private val pushReplicateEnabled = conf.pushReplicateEnabled
  private val testRetryCommitFiles = conf.testRetryCommitFiles

  private val commitEpoch = new AtomicLong()
  private val totalWritten = new LongAdder
  private val fileCount = new LongAdder
  protected val reducerFileGroupsMap = new ShuffleFileGroups

  def getPartitionType(): PartitionType

  def isStageEnd(shuffleId: Int): Boolean = false

  def isStageEndOrInProcess(shuffleId: Int): Boolean = false

  def isStageDataLost(shuffleId: Int): Boolean = false

  def setStageEnd(shuffleId: Int): Unit

  /**
   * return (waitStage isTimeOut, waitTime)
   */
  def waitStageEnd(shuffleId: Int): (Boolean, Long) = (true, 0)

  def isPartitionInProcess(shuffleId: Int, partitionId: Int): Boolean = false

  def batchUnHandledRequests(shuffleId: Int, shuffleCommittedInfo: ShuffleCommittedInfo)
      : Map[WorkerInfo, collection.Set[PartitionLocation]] = {
    // When running to here, if handleStageEnd got lock first and commitFiles,
    // then this batch get this lock, commitPartitionRequests may contains
    // partitions which are already committed by stageEnd process.
    // But inProcessStageEndShuffleSet should have contain this shuffle id,
    // can directly return empty.
    if (this.isStageEndOrInProcess(shuffleId)) {
      logWarning(s"Shuffle $shuffleId ended or during processing stage end.")
      shuffleCommittedInfo.unHandledPartitionLocations.clear()
      Map.empty[WorkerInfo, Set[PartitionLocation]]
    } else {
      val currentBatch = this.getUnHandledPartitionLocations(shuffleId, shuffleCommittedInfo)
      shuffleCommittedInfo.unHandledPartitionLocations.clear()
      currentBatch.foreach { partitionLocation =>
        shuffleCommittedInfo.handledPartitionLocations.add(partitionLocation)
        if (partitionLocation.getPeer != null) {
          shuffleCommittedInfo.handledPartitionLocations.add(partitionLocation.getPeer)
        }
      }

      if (currentBatch.nonEmpty) {
        logWarning(s"Commit current batch HARD_SPLIT partitions for $shuffleId: " +
          s"${currentBatch.map(_.getUniqueId).mkString("[", ",", "]")}")
        val workerToRequests = currentBatch.flatMap { partitionLocation =>
          if (partitionLocation.getPeer != null) {
            Seq(partitionLocation, partitionLocation.getPeer)
          } else {
            Seq(partitionLocation)
          }
        }.groupBy(_.getWorker)
        workerToRequests
      } else {
        Map.empty[WorkerInfo, Set[PartitionLocation]]
      }
    }
  }

  protected def getUnHandledPartitionLocations(
      shuffleId: Int,
      shuffleCommittedInfo: ShuffleCommittedInfo): mutable.Set[PartitionLocation]

  def incrementInFlightNum(
      shuffleCommittedInfo: ShuffleCommittedInfo,
      workerToRequests: Map[WorkerInfo, collection.Set[PartitionLocation]]): Unit = {
    shuffleCommittedInfo.allInFlightCommitRequestNum.addAndGet(workerToRequests.size)
  }

  def decrementInFlightNum(
      shuffleCommittedInfo: ShuffleCommittedInfo,
      workerToRequests: Map[WorkerInfo, collection.Set[PartitionLocation]]): Unit = {
    shuffleCommittedInfo.allInFlightCommitRequestNum.addAndGet(-workerToRequests.size)
  }

  /**
   * when someone calls tryFinalCommit, the function will return true if there is no one ever do final commit before,
   * otherwise it will return false.
   *
   * @return
   */
  def tryFinalCommit(
      shuffleId: Int,
      recordWorkerFailure: ShuffleFailedWorkers => Unit): Boolean

  /**
   * Only Reduce partition mode supports cache all file groups for reducer. Map partition doesn't guarantee that all
   * partitions are complete by the time the method is called, as downstream tasks may start early before all tasks
   * are completed.So map partition may need refresh reducer file group if needed.
   */
  def handleGetReducerFileGroup(context: RpcCallContext, shuffleId: Int): Unit

  def removeExpiredShuffle(shuffleId: Int): Unit = {
    reducerFileGroupsMap.remove(shuffleId)
  }

  /**
   * For reduce partition if shuffle registered and corresponding map finished, reply true.
   * For map partition would always return false, as one mapper attempt finished don't mean mapper ended.
   */
  def isMapperEnded(shuffleId: Int, mapId: Int): Boolean = false

  def getMapperAttempts(shuffleId: Int): Array[Int]

  /**
   * return (thisMapperAttemptedFinishedSuccessOrNot, allMapperFinishedOrNot)
   */
  def finishMapperAttempt(
      shuffleId: Int,
      mapId: Int,
      attemptId: Int,
      numMappers: Int,
      partitionId: Int,
      recordWorkerFailure: ShuffleFailedWorkers => Unit): (Boolean, Boolean)

  def registerShuffle(shuffleId: Int, numMappers: Int): Unit = {
    reducerFileGroupsMap.put(shuffleId, new ConcurrentHashMap())
  }

  def parallelCommitFiles(
      shuffleId: Int,
      allocatedWorkers: util.Map[WorkerInfo, ShufflePartitionLocationInfo],
      partitionIdOpt: Option[Int] = None): CommitResult = {
    val shuffleCommittedInfo = committedPartitionInfo.get(shuffleId)
    val masterPartMap = new ConcurrentHashMap[String, PartitionLocation]
    val slavePartMap = new ConcurrentHashMap[String, PartitionLocation]
    val commitFilesFailedWorkers = new ShuffleFailedWorkers()

    if (CollectionUtils.isEmpty(allocatedWorkers)) {
      return CommitResult(masterPartMap, slavePartMap, commitFilesFailedWorkers)
    }

    val commitFileStartTime = System.nanoTime()
    val parallelism = Math.min(allocatedWorkers.size(), conf.rpcMaxParallelism)
    ThreadUtils.parmap(
      allocatedWorkers.asScala.to,
      "CommitFiles",
      parallelism) { case (worker, partitionLocationInfo) =>
      val masterParts =
        partitionLocationInfo.getMasterPartitions(partitionIdOpt)
      val slaveParts = partitionLocationInfo.getSlavePartitions(partitionIdOpt)
      masterParts.asScala.foreach { p =>
        val partition = new PartitionLocation(p)
        partition.setFetchPort(worker.fetchPort)
        partition.setPeer(null)
        masterPartMap.put(partition.getUniqueId, partition)
      }
      slaveParts.asScala.foreach { p =>
        val partition = new PartitionLocation(p)
        partition.setFetchPort(worker.fetchPort)
        partition.setPeer(null)
        slavePartMap.put(partition.getUniqueId, partition)
      }

      val (masterIds, slaveIds) = shuffleCommittedInfo.synchronized {
        (
          masterParts.asScala
            .filterNot(shuffleCommittedInfo.handledPartitionLocations.contains)
            .map(_.getUniqueId).asJava,
          slaveParts.asScala
            .filterNot(shuffleCommittedInfo.handledPartitionLocations.contains)
            .map(_.getUniqueId).asJava)
      }

      commitFiles(
        appId,
        shuffleId,
        shuffleCommittedInfo,
        worker,
        masterIds,
        slaveIds,
        commitFilesFailedWorkers)

    }

    logInfo(s"Shuffle $shuffleId " +
      s"commit files complete. File count ${shuffleCommittedInfo.currentShuffleFileCount.sum()} " +
      s"using ${(System.nanoTime() - commitFileStartTime) / 1000000} ms")

    CommitResult(masterPartMap, slavePartMap, commitFilesFailedWorkers)
  }

  def commitFiles(
      applicationId: String,
      shuffleId: Int,
      shuffleCommittedInfo: ShuffleCommittedInfo,
      worker: WorkerInfo,
      masterIds: util.List[String],
      slaveIds: util.List[String],
      commitFilesFailedWorkers: ShuffleFailedWorkers): Unit = {

    if (CollectionUtils.isEmpty(masterIds) && CollectionUtils.isEmpty(slaveIds)) {
      return
    }

    val res =
      if (!testRetryCommitFiles) {
        val commitFiles = CommitFiles(
          applicationId,
          shuffleId,
          masterIds,
          slaveIds,
          getMapperAttempts(shuffleId),
          commitEpoch.incrementAndGet())
        val res = requestCommitFilesWithRetry(worker.endpoint, commitFiles)

        res.status match {
          case StatusCode.SUCCESS => // do nothing
          case StatusCode.PARTIAL_SUCCESS | StatusCode.SHUFFLE_NOT_REGISTERED | StatusCode.REQUEST_FAILED =>
            logDebug(s"Request $commitFiles return ${res.status} for " +
              s"${Utils.makeShuffleKey(applicationId, shuffleId)}")
            commitFilesFailedWorkers.put(worker, (res.status, System.currentTimeMillis()))
          case _ => // won't happen
        }
        res
      } else {
        // for test
        val commitFiles1 = CommitFiles(
          applicationId,
          shuffleId,
          masterIds.subList(0, masterIds.size() / 2),
          slaveIds.subList(0, slaveIds.size() / 2),
          getMapperAttempts(shuffleId),
          commitEpoch.incrementAndGet())
        val res1 = requestCommitFilesWithRetry(worker.endpoint, commitFiles1)

        val commitFiles = CommitFiles(
          applicationId,
          shuffleId,
          masterIds.subList(masterIds.size() / 2, masterIds.size()),
          slaveIds.subList(slaveIds.size() / 2, slaveIds.size()),
          getMapperAttempts(shuffleId),
          commitEpoch.incrementAndGet())
        val res2 = requestCommitFilesWithRetry(worker.endpoint, commitFiles)

        res1.committedMasterStorageInfos.putAll(res2.committedMasterStorageInfos)
        res1.committedSlaveStorageInfos.putAll(res2.committedSlaveStorageInfos)
        res1.committedMapIdBitMap.putAll(res2.committedMapIdBitMap)
        CommitFilesResponse(
          status = if (res1.status == StatusCode.SUCCESS) res2.status else res1.status,
          (res1.committedMasterIds.asScala ++ res2.committedMasterIds.asScala).toList.asJava,
          (res1.committedSlaveIds.asScala ++ res1.committedSlaveIds.asScala).toList.asJava,
          (res1.failedMasterIds.asScala ++ res1.failedMasterIds.asScala).toList.asJava,
          (res1.failedSlaveIds.asScala ++ res2.failedSlaveIds.asScala).toList.asJava,
          res1.committedMasterStorageInfos,
          res1.committedSlaveStorageInfos,
          res1.committedMapIdBitMap,
          res1.totalWritten + res2.totalWritten,
          res1.fileCount + res2.fileCount)
      }

    shuffleCommittedInfo.synchronized {
      // record committed partitionIds
      res.committedMasterIds.asScala.foreach({
        case commitMasterId =>
          val partitionUniqueIdList = shuffleCommittedInfo.committedMasterIds.computeIfAbsent(
            Utils.splitPartitionLocationUniqueId(commitMasterId)._1,
            (k: Int) => new util.ArrayList[String]())
          partitionUniqueIdList.add(commitMasterId)
      })

      res.committedSlaveIds.asScala.foreach({
        case commitSlaveId =>
          val partitionUniqueIdList = shuffleCommittedInfo.committedSlaveIds.computeIfAbsent(
            Utils.splitPartitionLocationUniqueId(commitSlaveId)._1,
            (k: Int) => new util.ArrayList[String]())
          partitionUniqueIdList.add(commitSlaveId)
      })

      // record committed partitions storage hint and disk hint
      shuffleCommittedInfo.committedMasterStorageInfos.putAll(res.committedMasterStorageInfos)
      shuffleCommittedInfo.committedSlaveStorageInfos.putAll(res.committedSlaveStorageInfos)

      // record failed partitions
      shuffleCommittedInfo.failedMasterPartitionIds.putAll(
        res.failedMasterIds.asScala.map((_, worker)).toMap.asJava)
      shuffleCommittedInfo.failedSlavePartitionIds.putAll(
        res.failedSlaveIds.asScala.map((_, worker)).toMap.asJava)

      shuffleCommittedInfo.committedMapIdBitmap.putAll(res.committedMapIdBitMap)

      totalWritten.add(res.totalWritten)
      fileCount.add(res.fileCount)
      shuffleCommittedInfo.currentShuffleFileCount.add(res.fileCount)
    }
  }

  def collectResult(
      shuffleId: Int,
      shuffleCommittedInfo: ShuffleCommittedInfo,
      masterPartitionUniqueIds: util.Iterator[String],
      slavePartitionUniqueIds: util.Iterator[String],
      masterPartMap: ConcurrentHashMap[String, PartitionLocation],
      slavePartMap: ConcurrentHashMap[String, PartitionLocation]): Unit = {
    val committedPartitions = new util.HashMap[String, PartitionLocation]
    masterPartitionUniqueIds.asScala.foreach { id =>
      masterPartMap.get(id).setStorageInfo(
        shuffleCommittedInfo.committedMasterStorageInfos.get(id))
      masterPartMap.get(id).setMapIdBitMap(shuffleCommittedInfo.committedMapIdBitmap.get(id))
      committedPartitions.put(id, masterPartMap.get(id))
    }

    slavePartitionUniqueIds.asScala.foreach { id =>
      val slavePartition = slavePartMap.get(id)
      slavePartition.setStorageInfo(shuffleCommittedInfo.committedSlaveStorageInfos.get(id))
      val masterPartition = committedPartitions.get(id)
      if (masterPartition ne null) {
        masterPartition.setPeer(slavePartition)
        slavePartition.setPeer(masterPartition)
      } else {
        logInfo(s"Shuffle $shuffleId partition $id: master lost, " +
          s"use slave $slavePartition.")
        slavePartition.setMapIdBitMap(shuffleCommittedInfo.committedMapIdBitmap.get(id))
        committedPartitions.put(id, slavePartition)
      }
    }

    committedPartitions.values().asScala.foreach { partition =>
      val partitionLocations = reducerFileGroupsMap.get(shuffleId).computeIfAbsent(
        partition.getId,
        (k: Integer) => new util.HashSet[PartitionLocation]())
      partitionLocations.add(partition)
    }
  }

  private def requestCommitFilesWithRetry(
      endpoint: RpcEndpointRef,
      message: CommitFiles): CommitFilesResponse = {
    val maxRetries = conf.requestCommitFilesMaxRetries
    var retryTimes = 0
    while (retryTimes < maxRetries) {
      try {
        if (testRetryCommitFiles && retryTimes < maxRetries - 1) {
          endpoint.ask[CommitFilesResponse](message)
          Thread.sleep(1000)
          throw new Exception("Mock fail for CommitFiles")
        } else {
          return endpoint.askSync[CommitFilesResponse](message)
        }
      } catch {
        case e: Throwable =>
          retryTimes += 1
          logError(
            s"AskSync CommitFiles for ${message.shuffleId} failed (attempt $retryTimes/$maxRetries).",
            e)
      }
    }

    CommitFilesResponse(
      StatusCode.REQUEST_FAILED,
      List.empty.asJava,
      List.empty.asJava,
      message.masterIds,
      message.slaveIds)
  }

  def checkDataLost(
      shuffleId: Int,
      masterPartitionUniqueIdMap: util.Map[String, WorkerInfo],
      slavePartitionUniqueIdMap: util.Map[String, WorkerInfo]): Boolean = {
    val shuffleKey = Utils.makeShuffleKey(appId, shuffleId)
    if (!pushReplicateEnabled && masterPartitionUniqueIdMap.size() != 0) {
      val msg =
        masterPartitionUniqueIdMap.asScala.map {
          case (partitionUniqueId, workerInfo) =>
            s"Lost partition $partitionUniqueId in worker [${workerInfo.readableAddress()}]"
        }.mkString("\n")
      logError(
        s"""
           |For shuffle $shuffleKey partition data lost:
           |$msg
           |""".stripMargin)
      true
    } else {
      val failedBothPartitionIdsToWorker = masterPartitionUniqueIdMap.asScala.flatMap {
        case (partitionUniqueId, worker) =>
          if (slavePartitionUniqueIdMap.asScala.contains(partitionUniqueId)) {
            Some(partitionUniqueId -> (worker, slavePartitionUniqueIdMap.get(partitionUniqueId)))
          } else {
            None
          }
      }
      if (failedBothPartitionIdsToWorker.nonEmpty) {
        val msg = failedBothPartitionIdsToWorker.map {
          case (partitionUniqueId, (masterWorker, slaveWorker)) =>
            s"Lost partition $partitionUniqueId " +
              s"in master worker [${masterWorker.readableAddress()}] and slave worker [$slaveWorker]"
        }.mkString("\n")
        logError(
          s"""
             |For shuffle $shuffleKey partition data lost:
             |$msg
             |""".stripMargin)
        true
      } else {
        false
      }
    }
  }

  def commitMetrics(): (Long, Long) = (totalWritten.sumThenReset(), fileCount.sumThenReset())
}
