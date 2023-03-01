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
import java.util.concurrent.{ConcurrentHashMap, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import java.util.concurrent.atomic.{AtomicInteger, LongAdder}

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

import org.roaringbitmap.RoaringBitmap

import org.apache.celeborn.client.CommitManager.CommittedPartitionInfo
import org.apache.celeborn.client.LifecycleManager.ShuffleFailedWorkers
import org.apache.celeborn.client.commit.{CommitHandler, MapPartitionCommitHandler, ReducePartitionCommitHandler}
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.WorkerInfo
import org.apache.celeborn.common.protocol.{PartitionLocation, PartitionType, StorageInfo}
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.rpc.RpcCallContext
// Can Remove this if celeborn don't support scala211 in future
import org.apache.celeborn.common.util.FunctionConverter._
import org.apache.celeborn.common.util.ThreadUtils

case class ShuffleCommittedInfo(
    committedMasterIds: ConcurrentHashMap[Int, util.List[String]],
    committedSlaveIds: ConcurrentHashMap[Int, util.List[String]],
    failedMasterPartitionIds: ConcurrentHashMap[String, WorkerInfo],
    failedSlavePartitionIds: ConcurrentHashMap[String, WorkerInfo],
    committedMasterStorageInfos: ConcurrentHashMap[String, StorageInfo],
    committedSlaveStorageInfos: ConcurrentHashMap[String, StorageInfo],
    committedMapIdBitmap: ConcurrentHashMap[String, RoaringBitmap],
    currentShuffleFileCount: LongAdder,
    unHandledPartitionLocations: util.Set[PartitionLocation],
    handledPartitionLocations: util.Set[PartitionLocation],
    allInFlightCommitRequestNum: AtomicInteger,
    partitionInFlightCommitRequestNum: ConcurrentHashMap[Int, AtomicInteger])

object CommitManager {
  type CommittedPartitionInfo = ConcurrentHashMap[Int, ShuffleCommittedInfo]
}

class CommitManager(appId: String, val conf: CelebornConf, lifecycleManager: LifecycleManager)
  extends Logging {

  // shuffle id -> ShuffleCommittedInfo
  private val committedPartitionInfo = new CommittedPartitionInfo
  private val batchHandleCommitPartitionEnabled = conf.batchHandleCommitPartitionEnabled
  private val batchHandleCommitPartitionExecutors = ThreadUtils.newDaemonCachedThreadPool(
    "rss-lifecycle-manager-commit-partition-executor",
    conf.batchHandleCommitPartitionNumThreads)
  private val batchHandleCommitPartitionRequestInterval =
    conf.batchHandleCommitPartitionRequestInterval
  private val batchHandleCommitPartitionSchedulerThread: Option[ScheduledExecutorService] =
    if (batchHandleCommitPartitionEnabled) {
      Some(ThreadUtils.newDaemonSingleThreadScheduledExecutor(
        "rss-lifecycle-manager-commit-partition-scheduler"))
    } else {
      None
    }
  private var batchHandleCommitPartition: Option[ScheduledFuture[_]] = _
  private val commitHandlers = new ConcurrentHashMap[PartitionType, CommitHandler]()

  def start(): Unit = {
    batchHandleCommitPartition = batchHandleCommitPartitionSchedulerThread.map {
      _.scheduleAtFixedRate(
        new Runnable {
          override def run(): Unit = {
            committedPartitionInfo.asScala.foreach { case (shuffleId, shuffleCommittedInfo) =>
              batchHandleCommitPartitionExecutors.submit {
                new Runnable {
                  val commitHandler = getCommitHandler(shuffleId)
                  override def run(): Unit = {
                    var workerToRequests: Map[WorkerInfo, collection.Set[PartitionLocation]] = null
                    shuffleCommittedInfo.synchronized {
                      workerToRequests =
                        commitHandler.batchUnHandledRequests(shuffleId, shuffleCommittedInfo)
                      // when batch commit thread starts to commit these requests, we should increment inFlightNum,
                      // then stage/partition end would be able to recognize all requests are over.
                      commitHandler.incrementInFlightNum(shuffleCommittedInfo, workerToRequests)
                    }

                    if (workerToRequests.nonEmpty) {
                      val commitFilesFailedWorkers = new ShuffleFailedWorkers()
                      val parallelism = Math.min(workerToRequests.size, conf.rpcMaxParallelism)
                      try {
                        ThreadUtils.parmap(
                          workerToRequests.to,
                          "CommitFiles",
                          parallelism) {
                          case (worker, requests) =>
                            val workerInfo =
                              lifecycleManager.shuffleAllocatedWorkers
                                .get(shuffleId)
                                .asScala
                                .find(_._1.equals(worker))
                                .get
                                ._1
                            val mastersIds =
                              requests
                                .filter(_.getMode == PartitionLocation.Mode.MASTER)
                                .map(_.getUniqueId)
                                .toList
                                .asJava
                            val slaveIds =
                              requests
                                .filter(_.getMode == PartitionLocation.Mode.SLAVE)
                                .map(_.getUniqueId)
                                .toList
                                .asJava

                            commitHandler.commitFiles(
                              appId,
                              shuffleId,
                              shuffleCommittedInfo,
                              workerInfo,
                              mastersIds,
                              slaveIds,
                              commitFilesFailedWorkers)
                        }
                        lifecycleManager.recordWorkerFailure(commitFilesFailedWorkers)
                      } finally {
                        // when batch commit thread ends, we need decrementInFlightNum
                        commitHandler.decrementInFlightNum(shuffleCommittedInfo, workerToRequests)
                      }
                    }
                  }
                }
              }
            }
          }
        },
        0,
        batchHandleCommitPartitionRequestInterval,
        TimeUnit.MILLISECONDS)
    }
  }

  def stop(): Unit = {
    batchHandleCommitPartition.foreach(_.cancel(true))
    batchHandleCommitPartitionSchedulerThread.foreach(ThreadUtils.shutdown(_, 800.millis))
  }

  def registerShuffle(shuffleId: Int, numMappers: Int): Unit = {
    committedPartitionInfo.put(
      shuffleId,
      ShuffleCommittedInfo(
        new ConcurrentHashMap[Int, util.List[String]](),
        new ConcurrentHashMap[Int, util.List[String]](),
        new ConcurrentHashMap[String, WorkerInfo](),
        new ConcurrentHashMap[String, WorkerInfo](),
        new ConcurrentHashMap[String, StorageInfo](),
        new ConcurrentHashMap[String, StorageInfo](),
        new ConcurrentHashMap[String, RoaringBitmap](),
        new LongAdder,
        new util.HashSet[PartitionLocation](),
        new util.HashSet[PartitionLocation](),
        new AtomicInteger(),
        new ConcurrentHashMap[Int, AtomicInteger]()))

    getCommitHandler(shuffleId).registerShuffle(shuffleId, numMappers);
  }

  def isMapperEnded(shuffleId: Int, mapId: Int): Boolean = {
    getCommitHandler(shuffleId).isMapperEnded(shuffleId, mapId)
  }

  def getMapperAttempts(shuffleId: Int): Array[Int] = {
    getCommitHandler(shuffleId).getMapperAttempts(shuffleId)
  }

  def finishMapperAttempt(
      shuffleId: Int,
      mapId: Int,
      attemptId: Int,
      numMappers: Int,
      partitionId: Int = -1): (Boolean, Boolean) = {
    getCommitHandler(shuffleId).finishMapperAttempt(
      shuffleId,
      mapId,
      attemptId,
      numMappers,
      partitionId,
      r =>
        lifecycleManager.recordWorkerFailure(r))
  }

  def removeExpiredShuffle(shuffleId: Int): Unit = {
    committedPartitionInfo.remove(shuffleId)
    getCommitHandler(shuffleId).removeExpiredShuffle(shuffleId)
  }

  def registerCommitPartitionRequest(
      shuffleId: Int,
      partitionLocation: PartitionLocation,
      cause: Option[StatusCode]): Unit = {
    if (batchHandleCommitPartitionEnabled && cause.isDefined && cause.get == StatusCode.HARD_SPLIT) {
      val shuffleCommittedInfo = committedPartitionInfo.get(shuffleId)
      shuffleCommittedInfo.synchronized {
        shuffleCommittedInfo.unHandledPartitionLocations.add(partitionLocation)
      }
    }
  }

  def tryFinalCommit(shuffleId: Int): Boolean = {
    getCommitHandler(shuffleId).tryFinalCommit(
      shuffleId,
      r => lifecycleManager.recordWorkerFailure(r))
  }

  def isStageEnd(shuffleId: Int): Boolean = {
    getCommitHandler(shuffleId).isStageEnd(shuffleId)
  }

  def setStageEnd(shuffleId: Int): Unit = {
    getCommitHandler(shuffleId).setStageEnd(shuffleId)
  }

  def waitStageEnd(shuffleId: Int): (Boolean, Long) = {
    getCommitHandler(shuffleId).waitStageEnd(shuffleId)
  }

  def handleGetReducerFileGroup(context: RpcCallContext, shuffleId: Int): Unit = {
    getCommitHandler(shuffleId).handleGetReducerFileGroup(context, shuffleId)
  }

  private def getCommitHandler(shuffleId: Int): CommitHandler = {
    val partitionType = lifecycleManager.getPartitionType(shuffleId)
    if (commitHandlers.containsKey(partitionType)) {
      commitHandlers.get(partitionType)
    } else {
      commitHandlers.computeIfAbsent(
        partitionType,
        (partitionType: PartitionType) => {
          partitionType match {
            case PartitionType.REDUCE => new ReducePartitionCommitHandler(
                appId,
                conf,
                lifecycleManager.shuffleAllocatedWorkers,
                committedPartitionInfo)
            case PartitionType.MAP => new MapPartitionCommitHandler(
                appId,
                conf,
                lifecycleManager.shuffleAllocatedWorkers,
                committedPartitionInfo)
            case _ => throw new UnsupportedOperationException(
                s"Unexpected ShufflePartitionType for CommitManager: $partitionType")
          }
        })
    }
  }

  def commitMetrics(): (Long, Long) = {
    var totalWritten = 0L
    var totalFileCount = 0L
    commitHandlers.asScala.values.foreach { commitHandler =>
      totalWritten += commitHandler.commitMetrics._1
      totalFileCount += commitHandler.commitMetrics._2
    }
    (totalWritten, totalFileCount)
  }
}
