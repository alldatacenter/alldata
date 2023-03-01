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

package org.apache.celeborn.service.deploy.worker

import java.nio.ByteBuffer
import java.util.concurrent.{ConcurrentHashMap, ThreadPoolExecutor}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicIntegerArray}

import com.google.common.base.Throwables
import io.netty.buffer.ByteBuf

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.exception.{AlreadyClosedException, CelebornIOException}
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{WorkerInfo, WorkerPartitionLocationInfo}
import org.apache.celeborn.common.metrics.source.{RPCSource, Source}
import org.apache.celeborn.common.network.buffer.{NettyManagedBuffer, NioManagedBuffer}
import org.apache.celeborn.common.network.client.{RpcResponseCallback, TransportClient, TransportClientFactory}
import org.apache.celeborn.common.network.protocol.{Message, PushData, PushDataHandShake, PushMergedData, RegionFinish, RegionStart, RequestMessage, RpcFailure, RpcRequest, RpcResponse}
import org.apache.celeborn.common.network.protocol.Message.Type
import org.apache.celeborn.common.network.server.BaseMessageHandler
import org.apache.celeborn.common.protocol.{PartitionLocation, PartitionSplitMode, PartitionType}
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.unsafe.Platform
import org.apache.celeborn.common.util.PackedPartitionId
import org.apache.celeborn.service.deploy.worker.congestcontrol.CongestionController
import org.apache.celeborn.service.deploy.worker.storage.{FileWriter, HdfsFlusher, LocalFlusher, MapPartitionFileWriter, StorageManager}

class PushDataHandler extends BaseMessageHandler with Logging {

  var workerSource: WorkerSource = _
  var rpcSource: RPCSource = _
  var partitionLocationInfo: WorkerPartitionLocationInfo = _
  var shuffleMapperAttempts: ConcurrentHashMap[String, AtomicIntegerArray] = _
  var shufflePartitionType: ConcurrentHashMap[String, PartitionType] = _
  var shufflePushDataTimeout: ConcurrentHashMap[String, Long] = _
  var replicateThreadPool: ThreadPoolExecutor = _
  var unavailablePeers: ConcurrentHashMap[WorkerInfo, Long] = _
  var pushClientFactory: TransportClientFactory = _
  var registered: AtomicBoolean = new AtomicBoolean(false)
  var workerInfo: WorkerInfo = _
  var diskReserveSize: Long = _
  var partitionSplitMinimumSize: Long = _
  var shutdown: AtomicBoolean = _
  var storageManager: StorageManager = _
  var conf: CelebornConf = _
  @volatile var pushMasterDataTimeoutTested = false
  @volatile var pushSlaveDataTimeoutTested = false

  def init(worker: Worker): Unit = {
    workerSource = worker.workerSource
    rpcSource = worker.rpcSource
    partitionLocationInfo = worker.partitionLocationInfo
    shufflePartitionType = worker.shufflePartitionType
    shufflePushDataTimeout = worker.shufflePushDataTimeout
    shuffleMapperAttempts = worker.shuffleMapperAttempts
    replicateThreadPool = worker.replicateThreadPool
    unavailablePeers = worker.unavailablePeers
    pushClientFactory = worker.pushClientFactory
    registered = worker.registered
    workerInfo = worker.workerInfo
    diskReserveSize = worker.conf.diskReserveSize
    partitionSplitMinimumSize = worker.conf.partitionSplitMinimumSize
    storageManager = worker.storageManager
    shutdown = worker.shutdown
    conf = worker.conf

    logInfo(s"diskReserveSize $diskReserveSize")
  }

  override def receive(client: TransportClient, msg: RequestMessage): Unit =
    msg match {
      case pushData: PushData =>
        handleCore(
          client,
          pushData,
          pushData.requestId,
          () => {
            val callback = new SimpleRpcResponseCallback(
              client,
              pushData.requestId,
              pushData.shuffleKey)
            shufflePartitionType.getOrDefault(pushData.shuffleKey, PartitionType.REDUCE) match {
              case PartitionType.REDUCE => handlePushData(
                  pushData,
                  callback)
              case PartitionType.MAP => handleMapPartitionPushData(
                  pushData,
                  callback)
            }
          })
      case pushMergedData: PushMergedData => {
        handleCore(
          client,
          pushMergedData,
          pushMergedData.requestId,
          () =>
            handlePushMergedData(
              pushMergedData,
              new SimpleRpcResponseCallback(
                client,
                pushMergedData.requestId,
                pushMergedData.shuffleKey)))
      }
      case rpcRequest: RpcRequest => handleRpcRequest(client, rpcRequest)
    }

  def handlePushData(pushData: PushData, callback: RpcResponseCallback): Unit = {
    val shuffleKey = pushData.shuffleKey
    val mode = PartitionLocation.getMode(pushData.mode)
    val body = pushData.body.asInstanceOf[NettyManagedBuffer].getBuf
    val isMaster = mode == PartitionLocation.Mode.MASTER

    // For test
    if (isMaster && conf.testPushMasterDataTimeout && !pushMasterDataTimeoutTested) {
      pushMasterDataTimeoutTested = true
      return
    }

    if (!isMaster && conf.testPushSlaveDataTimeout && !pushSlaveDataTimeoutTested) {
      pushSlaveDataTimeoutTested = true
      return
    }

    val key = s"${pushData.requestId}"
    val callbackWithTimer =
      if (isMaster) {
        new RpcResponseCallbackWithTimer(
          workerSource,
          WorkerSource.MasterPushDataTime,
          key,
          callback)
      } else {
        new RpcResponseCallbackWithTimer(
          workerSource,
          WorkerSource.SlavePushDataTime,
          key,
          callback)
      }

    // find FileWriter responsible for the data
    val location =
      if (isMaster) {
        partitionLocationInfo.getMasterLocation(shuffleKey, pushData.partitionUniqueId)
      } else {
        partitionLocationInfo.getSlaveLocation(shuffleKey, pushData.partitionUniqueId)
      }

    // Fetch real batchId from body will add more cost and no meaning for replicate.
    val doReplicate = location != null && location.getPeer != null && isMaster
    val softSplit = new AtomicBoolean(false)

    if (location == null) {
      val (mapId, attemptId) = getMapAttempt(body)
      // MapperAttempts for a shuffle exists after any CommitFiles request succeeds.
      // A shuffle can trigger multiple CommitFiles requests, for reasons like: Hard-Split happens, StageEnd.
      // If MapperAttempts but the value is -1 for the mapId(-1 means the map has not yet finished),
      // it's probably because commitFiles for Had-Split happens.
      if (shuffleMapperAttempts.containsKey(shuffleKey)) {
        if (-1 != shuffleMapperAttempts.get(shuffleKey).get(mapId)) {
          // partition data has already been committed
          logInfo(s"Receive push data from speculative task(shuffle $shuffleKey, map $mapId, " +
            s" attempt $attemptId), but this mapper has already been ended.")
          callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.STAGE_ENDED.getValue)))
        } else {
          logInfo(
            s"Receive push data for committed hard split partition of (shuffle $shuffleKey, " +
              s"map $mapId attempt $attemptId)")
          callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
        }
      } else {
        if (storageManager.shuffleKeySet().contains(shuffleKey)) {
          // If there is no shuffle key in shuffleMapperAttempts but there is shuffle key
          // in StorageManager. This partition should be HARD_SPLIT partition and
          // after worker restart, some task still push data to this HARD_SPLIT partition.
          logInfo(s"Receive push data for committed hard split partition of " +
            s"(shuffle $shuffleKey, map $mapId attempt $attemptId)")
          callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
        } else {
          logWarning(s"While handle PushData, Partition location wasn't found for " +
            s"task(shuffle $shuffleKey, map $mapId, attempt $attemptId, uniqueId ${pushData.partitionUniqueId}).")
          callbackWithTimer.onFailure(
            new CelebornIOException(StatusCode.PUSH_DATA_FAIL_PARTITION_NOT_FOUND))
        }
      }
      return
    }

    // During worker shutdown, worker will return HARD_SPLIT for all existed partition.
    // This should before return exception to make current push data can revive and retry.
    if (shutdown.get()) {
      logInfo(s"Push data return HARD_SPLIT for shuffle $shuffleKey since worker shutdown.")
      callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
      return
    }

    val fileWriter = location.asInstanceOf[WorkingPartition].getFileWriter
    val exception = fileWriter.getException
    if (exception != null) {
      val cause =
        if (isMaster) {
          StatusCode.PUSH_DATA_WRITE_FAIL_MASTER
        } else {
          StatusCode.PUSH_DATA_WRITE_FAIL_SLAVE
        }
      logError(
        s"While handling PushData, throw $cause, fileWriter $fileWriter has exception.",
        exception)
      callbackWithTimer.onFailure(new CelebornIOException(cause))
      return
    }
    val diskFull =
      if (fileWriter.flusher.isInstanceOf[LocalFlusher]) {
        workerInfo.diskInfos
          .get(fileWriter.flusher.asInstanceOf[LocalFlusher].mountPoint)
          .actualUsableSpace < diskReserveSize
      } else {
        false
      }
    if ((diskFull && fileWriter.getFileInfo.getFileLength > partitionSplitMinimumSize) ||
      (isMaster && fileWriter.getFileInfo.getFileLength > fileWriter.getSplitThreshold())) {
      if (fileWriter.getSplitMode == PartitionSplitMode.SOFT) {
        softSplit.set(true)
      } else {
        callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
        return
      }
    }
    fileWriter.incrementPendingWrites()

    // for master, send data to slave
    if (doReplicate) {
      pushData.body().retain()
      replicateThreadPool.submit(new Runnable {
        override def run(): Unit = {
          val peer = location.getPeer
          val peerWorker = new WorkerInfo(
            peer.getHost,
            peer.getRpcPort,
            peer.getPushPort,
            peer.getFetchPort,
            peer.getReplicatePort)
          if (unavailablePeers.containsKey(peerWorker)) {
            pushData.body().release()
            workerSource.incCounter(WorkerSource.PushDataFailCount)
            logError(
              s"PushData replication failed caused by unavailable peer for partitionLocation: $location")
            callbackWithTimer.onFailure(
              new CelebornIOException(StatusCode.PUSH_DATA_CREATE_CONNECTION_FAIL_SLAVE))
            return
          }

          // Handle the response from replica
          val wrappedCallback = new RpcResponseCallback() {
            override def onSuccess(response: ByteBuffer): Unit = {
              if (response.remaining() > 0) {
                val resp = ByteBuffer.allocate(response.remaining())
                resp.put(response)
                resp.flip()
                callbackWithTimer.onSuccess(resp)
              } else if (softSplit.get()) {
                // TODO Currently if the worker is in soft split status, given the guess that the client
                // will fast stop pushing data to the worker, we won't return congest status. But
                // in the long term, especially if this issue could frequently happen, we may need to return
                // congest&softSplit status together
                callbackWithTimer.onSuccess(
                  ByteBuffer.wrap(Array[Byte](StatusCode.SOFT_SPLIT.getValue)))
              } else {
                Option(CongestionController.instance()) match {
                  case Some(congestionController) =>
                    if (congestionController.isUserCongested(
                        fileWriter.getFileInfo.getUserIdentifier)) {
                      // Check whether master congest the data though the replicas doesn't congest
                      // it(the response is empty)
                      callbackWithTimer.onSuccess(
                        ByteBuffer.wrap(
                          Array[Byte](StatusCode.PUSH_DATA_SUCCESS_MASTER_CONGESTED.getValue)))
                    } else {
                      callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
                    }
                  case None =>
                    callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
                }
              }
            }

            override def onFailure(e: Throwable): Unit = {
              logError(s"PushData replication failed for partitionLocation: $location", e)
              workerSource.incCounter(WorkerSource.PushDataFailCount)
              // 1. Throw PUSH_DATA_FAIL_SLAVE by slave peer worker
              // 2. Throw PUSH_DATA_TIMEOUT_SLAVE by TransportResponseHandler
              // 3. Throw IOException by channel, convert to PUSH_DATA_CONNECTION_EXCEPTION_SLAVE
              if (e.getMessage.startsWith(StatusCode.PUSH_DATA_WRITE_FAIL_SLAVE.name()) ||
                e.getMessage.startsWith(StatusCode.PUSH_DATA_TIMEOUT_SLAVE.name())) {
                callbackWithTimer.onFailure(e)
              } else {
                callbackWithTimer.onFailure(
                  new CelebornIOException(StatusCode.PUSH_DATA_CONNECTION_EXCEPTION_SLAVE))
              }
            }
          }
          try {
            val client = getClient(peer.getHost, peer.getReplicatePort, location.getId)
            val newPushData = new PushData(
              PartitionLocation.Mode.SLAVE.mode(),
              shuffleKey,
              pushData.partitionUniqueId,
              pushData.body)
            client.pushData(newPushData, shufflePushDataTimeout.get(shuffleKey), wrappedCallback)
          } catch {
            case e: Exception =>
              pushData.body().release()
              unavailablePeers.put(peerWorker, System.currentTimeMillis())
              workerSource.incCounter(WorkerSource.PushDataFailCount)
              logError(
                s"PushData replication failed during connecting peer for partitionLocation: $location",
                e)
              callbackWithTimer.onFailure(
                new CelebornIOException(StatusCode.PUSH_DATA_CREATE_CONNECTION_FAIL_SLAVE))
          }
        }
      })
    } else {
      // The codes here could be executed if
      // 1. the client doesn't enable push data to the replica, the master worker could hit here
      // 2. the client enables push data to the replica, and the replica worker could hit here
      // TODO Currently if the worker is in soft split status, given the guess that the client
      // will fast stop pushing data to the worker, we won't return congest status. But
      // in the long term, especially if this issue could frequently happen, we may need to return
      // congest&softSplit status together
      if (softSplit.get()) {
        callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.SOFT_SPLIT.getValue)))
      } else {
        Option(CongestionController.instance()) match {
          case Some(congestionController) =>
            if (congestionController.isUserCongested(fileWriter.getFileInfo.getUserIdentifier)) {
              if (isMaster) {
                callbackWithTimer.onSuccess(
                  ByteBuffer.wrap(
                    Array[Byte](StatusCode.PUSH_DATA_SUCCESS_MASTER_CONGESTED.getValue)))
              } else {
                callbackWithTimer.onSuccess(
                  ByteBuffer.wrap(
                    Array[Byte](StatusCode.PUSH_DATA_SUCCESS_SLAVE_CONGESTED.getValue)))
              }
            } else {
              callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
            }
          case None =>
            callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
        }
      }
    }

    try {
      fileWriter.write(body)
    } catch {
      case e: AlreadyClosedException =>
        fileWriter.decrementPendingWrites()
        val (mapId, attemptId) = getMapAttempt(body)
        val endedAttempt =
          if (shuffleMapperAttempts.containsKey(shuffleKey)) {
            shuffleMapperAttempts.get(shuffleKey).get(mapId)
          } else -1
        // TODO just info log for ended attempt
        logWarning(s"Append data failed for task(shuffle $shuffleKey, map $mapId, attempt" +
          s" $attemptId), caused by ${e.getMessage}")
      case e: Exception =>
        logError("Exception encountered when write.", e)
    }
  }

  def handlePushMergedData(
      pushMergedData: PushMergedData,
      callback: RpcResponseCallback): Unit = {
    val shuffleKey = pushMergedData.shuffleKey
    val mode = PartitionLocation.getMode(pushMergedData.mode)
    val batchOffsets = pushMergedData.batchOffsets
    val body = pushMergedData.body.asInstanceOf[NettyManagedBuffer].getBuf
    val isMaster = mode == PartitionLocation.Mode.MASTER

    val key = s"${pushMergedData.requestId}"
    val callbackWithTimer =
      if (isMaster) {
        new RpcResponseCallbackWithTimer(
          workerSource,
          WorkerSource.MasterPushDataTime,
          key,
          callback)
      } else {
        new RpcResponseCallbackWithTimer(
          workerSource,
          WorkerSource.SlavePushDataTime,
          key,
          callback)
      }

    // For test
    if (isMaster && conf.testPushMasterDataTimeout && !PushDataHandler.pushMasterDataTimeoutTested) {
      PushDataHandler.pushMasterDataTimeoutTested = true
      return
    }

    if (!isMaster && conf.testPushSlaveDataTimeout && !PushDataHandler.pushSlaveDataTimeoutTested) {
      PushDataHandler.pushSlaveDataTimeoutTested = true
      return
    }

    val partitionIdToLocations = pushMergedData.partitionUniqueIds.map { id =>
      if (isMaster) {
        id -> partitionLocationInfo.getMasterLocation(shuffleKey, id)
      } else {
        id -> partitionLocationInfo.getSlaveLocation(shuffleKey, id)
      }
    }

    // Fetch real batchId from body will add more cost and no meaning for replicate.
    val doReplicate =
      partitionIdToLocations.head._2 != null && partitionIdToLocations.head._2.getPeer != null && isMaster

    // find FileWriters responsible for the data
    partitionIdToLocations.foreach { case (id, loc) =>
      if (loc == null) {
        val (mapId, attemptId) = getMapAttempt(body)
        // MapperAttempts for a shuffle exists after any CommitFiles request succeeds.
        // A shuffle can trigger multiple CommitFiles requests, for reasons like: Hard-Split happens, StageEnd.
        // If MapperAttempts but the value is -1 for the mapId(-1 means the map has not yet finished),
        // it's probably because commitFiles for Had-Split happens.
        if (shuffleMapperAttempts.containsKey(shuffleKey)) {
          if (-1 != shuffleMapperAttempts.get(shuffleKey).get(mapId)) {
            logInfo(s"Receive push merged data from speculative " +
              s"task(shuffle $shuffleKey, map $mapId, attempt $attemptId), " +
              s"but this mapper has already been ended.")
            callbackWithTimer.onSuccess(
              ByteBuffer.wrap(Array[Byte](StatusCode.STAGE_ENDED.getValue)))
          } else {
            logInfo(s"Receive push merged data for committed hard split partition of " +
              s"(shuffle $shuffleKey, map $mapId attempt $attemptId)")
            callbackWithTimer.onSuccess(
              ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
          }
        } else {
          if (storageManager.shuffleKeySet().contains(shuffleKey)) {
            // If there is no shuffle key in shuffleMapperAttempts but there is shuffle key
            // in StorageManager. This partition should be HARD_SPLIT partition and
            // after worker restart, some task still push data to this HARD_SPLIT partition.
            logInfo(s"Receive push merged data for committed hard split partition of " +
              s"(shuffle $shuffleKey, map $mapId attempt $attemptId)")
            callbackWithTimer.onSuccess(
              ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
          } else {
            logWarning(s"While handling PushMergedData, Partition location wasn't found for " +
              s"task(shuffle $shuffleKey, map $mapId, attempt $attemptId, uniqueId $id).")
            callbackWithTimer.onFailure(
              new CelebornIOException(StatusCode.PUSH_DATA_FAIL_PARTITION_NOT_FOUND))
          }
        }
        return
      }
    }

    // During worker shutdown, worker will return HARD_SPLIT for all existed partition.
    // This should before return exception to make current push data can revive and retry.
    if (shutdown.get()) {
      callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
      return
    }

    val fileWriters =
      partitionIdToLocations.map(_._2).map(_.asInstanceOf[WorkingPartition].getFileWriter)
    val fileWriterWithException = fileWriters.find(_.getException != null)
    if (fileWriterWithException.nonEmpty) {
      val exception = fileWriterWithException.get.getException
      val cause =
        if (isMaster) {
          StatusCode.PUSH_DATA_WRITE_FAIL_MASTER
        } else {
          StatusCode.PUSH_DATA_WRITE_FAIL_SLAVE
        }
      logError(
        s"While handling PushMergedData, throw $cause, fileWriter $fileWriterWithException has exception.",
        exception)
      callbackWithTimer.onFailure(new CelebornIOException(cause))
      return
    }
    fileWriters.foreach(_.incrementPendingWrites())

    // for master, send data to slave
    if (doReplicate) {
      pushMergedData.body().retain()
      replicateThreadPool.submit(new Runnable {
        override def run(): Unit = {
          val location = partitionIdToLocations.head._2
          val peer = location.getPeer
          val peerWorker = new WorkerInfo(
            peer.getHost,
            peer.getRpcPort,
            peer.getPushPort,
            peer.getFetchPort,
            peer.getReplicatePort)
          if (unavailablePeers.containsKey(peerWorker)) {
            pushMergedData.body().release()
            workerSource.incCounter(WorkerSource.PushDataFailCount)
            logError(
              s"PushMergedData replication failed caused by unavailable peer for partitionLocation: $location")
            callbackWithTimer.onFailure(
              new CelebornIOException(StatusCode.PUSH_DATA_CREATE_CONNECTION_FAIL_SLAVE))
            return
          }

          // Handle the response from replica
          val wrappedCallback = new RpcResponseCallback() {
            override def onSuccess(response: ByteBuffer): Unit = {
              // Only master data enable replication will push data to slave
              if (response.remaining() > 0) {
                val resp = ByteBuffer.allocate(response.remaining())
                resp.put(response)
                resp.flip()
                callbackWithTimer.onSuccess(resp)
              } else {
                Option(CongestionController.instance()) match {
                  case Some(congestionController) if fileWriters.nonEmpty =>
                    if (congestionController.isUserCongested(
                        fileWriters.head.getFileInfo.getUserIdentifier)) {
                      // Check whether master congest the data though the replicas doesn't congest
                      // it(the response is empty)
                      callbackWithTimer.onSuccess(
                        ByteBuffer.wrap(
                          Array[Byte](StatusCode.PUSH_DATA_SUCCESS_MASTER_CONGESTED.getValue)))
                    } else {
                      callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
                    }
                  case None =>
                    callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
                }
              }
            }

            override def onFailure(e: Throwable): Unit = {
              logError(s"PushMergedData replicate failed for partitionLocation: $location", e)
              workerSource.incCounter(WorkerSource.PushDataFailCount)
              // 1. Throw PUSH_DATA_FAIL_SLAVE by slave peer worker
              // 2. Throw PUSH_DATA_TIMEOUT_SLAVE by TransportResponseHandler
              // 3. Throw IOException by channel, convert to PUSH_DATA_CONNECTION_EXCEPTION_SLAVE
              if (e.getMessage.startsWith(StatusCode.PUSH_DATA_WRITE_FAIL_SLAVE.name()) ||
                e.getMessage.startsWith(StatusCode.PUSH_DATA_TIMEOUT_SLAVE.name())) {
                callbackWithTimer.onFailure(e)
              } else {
                callbackWithTimer.onFailure(
                  new CelebornIOException(StatusCode.PUSH_DATA_CONNECTION_EXCEPTION_SLAVE))
              }
            }
          }

          try {
            val client = getClient(peer.getHost, peer.getReplicatePort, location.getId)
            val newPushMergedData = new PushMergedData(
              PartitionLocation.Mode.SLAVE.mode(),
              shuffleKey,
              pushMergedData.partitionUniqueIds,
              batchOffsets,
              pushMergedData.body)
            client.pushMergedData(
              newPushMergedData,
              shufflePushDataTimeout.get(shuffleKey),
              wrappedCallback)
          } catch {
            case e: Exception =>
              pushMergedData.body().release()
              unavailablePeers.put(peerWorker, System.currentTimeMillis())
              workerSource.incCounter(WorkerSource.PushDataFailCount)
              logError(
                s"PushMergedData replication failed during connecting peer for partitionLocation: $location",
                e)
              callbackWithTimer.onFailure(
                new CelebornIOException(StatusCode.PUSH_DATA_CREATE_CONNECTION_FAIL_SLAVE))
          }
        }
      })
    } else {
      // The codes here could be executed if
      // 1. the client doesn't enable push data to the replica, the master worker could hit here
      // 2. the client enables push data to the replica, and the replica worker could hit here
      Option(CongestionController.instance()) match {
        case Some(congestionController) if fileWriters.nonEmpty =>
          if (congestionController.isUserCongested(
              fileWriters.head.getFileInfo.getUserIdentifier)) {
            if (isMaster) {
              callbackWithTimer.onSuccess(
                ByteBuffer.wrap(
                  Array[Byte](StatusCode.PUSH_DATA_SUCCESS_MASTER_CONGESTED.getValue)))
            } else {
              callbackWithTimer.onSuccess(
                ByteBuffer.wrap(Array[Byte](StatusCode.PUSH_DATA_SUCCESS_SLAVE_CONGESTED.getValue)))
            }
          } else {
            callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
          }
        case None =>
          callbackWithTimer.onSuccess(ByteBuffer.wrap(Array[Byte]()))
      }
    }

    var index = 0
    var fileWriter: FileWriter = null
    var alreadyClosed = false
    while (index < fileWriters.length) {
      fileWriter = fileWriters(index)
      val offset = body.readerIndex() + batchOffsets(index)
      val length =
        if (index == fileWriters.length - 1) {
          body.readableBytes() - batchOffsets(index)
        } else {
          batchOffsets(index + 1) - batchOffsets(index)
        }
      val batchBody = body.slice(offset, length)

      try {
        if (!alreadyClosed) {
          fileWriter.write(batchBody)
        } else {
          fileWriter.decrementPendingWrites()
        }
      } catch {
        case e: AlreadyClosedException =>
          fileWriter.decrementPendingWrites()
          alreadyClosed = true
          val (mapId, attemptId) = getMapAttempt(body)
          val endedAttempt =
            if (shuffleMapperAttempts.containsKey(shuffleKey)) {
              shuffleMapperAttempts.get(shuffleKey).get(mapId)
            } else -1
          // TODO just info log for ended attempt
          logWarning(s"Append data failed for task(shuffle $shuffleKey, map $mapId, attempt" +
            s" $attemptId), caused by ${e.getMessage}")
        case e: Exception =>
          logError("Exception encountered when write.", e)
      }
      index += 1
    }
  }

  private def getMapAttempt(body: ByteBuf): (Int, Int) = {
    // header: mapId attemptId batchId compressedTotalSize
    val header = new Array[Byte](8)
    body.getBytes(body.readerIndex(), header)
    val mapId = Platform.getInt(header, Platform.BYTE_ARRAY_OFFSET)
    val attemptId = Platform.getInt(header, Platform.BYTE_ARRAY_OFFSET + 4)
    (mapId, attemptId)
  }

  override def checkRegistered(): Boolean = registered.get()

  class RpcResponseCallbackWithTimer(
      source: Source,
      metricName: String,
      key: String,
      callback: RpcResponseCallback)
    extends RpcResponseCallback {
    source.startTimer(metricName, key)

    override def onSuccess(response: ByteBuffer): Unit = {
      callback.onSuccess(response)
      source.stopTimer(metricName, key)
    }

    override def onFailure(e: Throwable): Unit = {
      callback.onFailure(e)
      source.stopTimer(metricName, key)
    }
  }

  class SimpleRpcResponseCallback(
      client: TransportClient,
      requestId: Long,
      shuffleKey: String)
    extends RpcResponseCallback {
    override def onSuccess(response: ByteBuffer): Unit = {
      client.getChannel.writeAndFlush(new RpcResponse(
        requestId,
        new NioManagedBuffer(response)))
    }

    override def onFailure(e: Throwable): Unit = {
      client.getChannel.writeAndFlush(new RpcFailure(requestId, e.getMessage))
    }
  }

  private def handleCore(
      client: TransportClient,
      message: RequestMessage,
      requestId: Long,
      handler: () => Unit): Unit = {
    try {
      rpcSource.updateMessageMetrics(message, message.body().size())
      handler()
    } catch {
      case e: Exception =>
        logError(s"Error while handle${message.`type`()} $message", e)
        client.getChannel.writeAndFlush(new RpcFailure(
          requestId,
          Throwables.getStackTraceAsString(e)));
    } finally {
      message.body().release()
    }
  }

  def handleMapPartitionPushData(pushData: PushData, callback: RpcResponseCallback): Unit = {
    val shuffleKey = pushData.shuffleKey
    val mode = PartitionLocation.getMode(pushData.mode)
    val body = pushData.body.asInstanceOf[NettyManagedBuffer].getBuf
    val isMaster = mode == PartitionLocation.Mode.MASTER

    val key = s"${pushData.requestId}"
    if (isMaster) {
      workerSource.startTimer(WorkerSource.MasterPushDataTime, key)
    } else {
      workerSource.startTimer(WorkerSource.SlavePushDataTime, key)
    }

    // find FileWriter responsible for the data
    val location =
      if (isMaster) {
        partitionLocationInfo.getMasterLocation(shuffleKey, pushData.partitionUniqueId)
      } else {
        partitionLocationInfo.getSlaveLocation(shuffleKey, pushData.partitionUniqueId)
      }

    val wrappedCallback =
      new WrappedRpcResponseCallback(
        pushData.`type`(),
        isMaster,
        pushData.requestId,
        null,
        location,
        if (isMaster) WorkerSource.MasterPushDataTime else WorkerSource.SlavePushDataTime,
        callback)

    if (checkLocationNull(
        pushData.`type`(),
        shuffleKey,
        pushData.partitionUniqueId,
        null,
        location,
        callback,
        wrappedCallback)) return

    // During worker shutdown, worker will return HARD_SPLIT for all existed partition.
    // This should before return exception to make current push data can revive and retry.
    if (shutdown.get()) {
      logInfo(s"Push data return HARD_SPLIT for shuffle $shuffleKey since worker shutdown.")
      callback.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
      return
    }

    val fileWriter =
      getFileWriterAndCheck(pushData.`type`(), location, isMaster, callback) match {
        case (true, _) => return
        case (false, f: FileWriter) => f
      }

    // for mappartition we will not check whether disk full or split partition

    fileWriter.incrementPendingWrites()

    // for master, send data to slave
    if (location.getPeer != null && isMaster) {
      // to do
      wrappedCallback.onSuccess(ByteBuffer.wrap(Array[Byte]()))
    } else {
      wrappedCallback.onSuccess(ByteBuffer.wrap(Array[Byte]()))
    }

    try {
      fileWriter.write(body)
    } catch {
      case e: AlreadyClosedException =>
        fileWriter.decrementPendingWrites()
        val (mapId, attemptId) = getMapAttempt(body)
        val endedAttempt =
          if (shuffleMapperAttempts.containsKey(shuffleKey)) {
            shuffleMapperAttempts.get(shuffleKey).get(mapId)
          } else -1
        // TODO just info log for ended attempt
        logWarning(s"Append data failed for task(shuffle $shuffleKey, map $mapId, attempt" +
          s" $attemptId), caused by ${e.getMessage}")
      case e: Exception =>
        logError("Exception encountered when write.", e)
    }
  }

  private def handleRpcRequest(client: TransportClient, rpcRequest: RpcRequest): Unit = {
    val msg = Message.decode(rpcRequest.body().nioByteBuffer())
    val requestId = rpcRequest.requestId
    val (mode, shuffleKey, partitionUniqueId, isCheckSplit) = msg match {
      case p: PushDataHandShake => (p.mode, p.shuffleKey, p.partitionUniqueId, false)
      case rs: RegionStart => (rs.mode, rs.shuffleKey, rs.partitionUniqueId, true)
      case rf: RegionFinish => (rf.mode, rf.shuffleKey, rf.partitionUniqueId, false)
    }
    handleCore(
      client,
      rpcRequest,
      requestId,
      () =>
        handleRpcRequestCore(
          mode,
          msg,
          shuffleKey,
          partitionUniqueId,
          requestId,
          isCheckSplit,
          new SimpleRpcResponseCallback(
            client,
            requestId,
            shuffleKey)))

  }

  private def handleRpcRequestCore(
      mode: Byte,
      message: Message,
      shuffleKey: String,
      partitionUniqueId: String,
      requestId: Long,
      isCheckSplit: Boolean,
      callback: RpcResponseCallback): Unit = {
    val isMaster = PartitionLocation.getMode(mode) == PartitionLocation.Mode.MASTER
    val messageType = message.`type`()
    log.info(s"requestId:$requestId, pushdata rpc:$messageType, mode:$mode, shuffleKey:$shuffleKey, partitionUniqueId:$partitionUniqueId")
    val (workerSourceMaster, workerSourceSlave) =
      messageType match {
        case Type.PUSH_DATA_HAND_SHAKE =>
          (WorkerSource.MasterPushDataHandshakeTime, WorkerSource.SlavePushDataHandshakeTime)
        case Type.REGION_START =>
          (WorkerSource.MasterRegionStartTime, WorkerSource.SlaveRegionStartTime)
        case Type.REGION_FINISH =>
          (WorkerSource.MasterRegionFinishTime, WorkerSource.SlaveRegionFinishTime)
      }

    val location = isMaster match {
      case true => partitionLocationInfo.getMasterLocation(shuffleKey, partitionUniqueId)
      case false => partitionLocationInfo.getSlaveLocation(shuffleKey, partitionUniqueId)
    }
    workerSource.startTimer(if (isMaster) workerSourceMaster else workerSourceSlave, s"$requestId")
    val wrappedCallback =
      new WrappedRpcResponseCallback(
        messageType,
        isMaster,
        requestId,
        null,
        location,
        if (isMaster) workerSourceMaster else workerSourceSlave,
        callback)

    if (checkLocationNull(
        messageType,
        shuffleKey,
        partitionUniqueId,
        null,
        location,
        callback,
        wrappedCallback)) return

    val fileWriter =
      getFileWriterAndCheck(messageType, location, isMaster, callback) match {
        case (true, _) => return
        case (false, f: FileWriter) => f
      }

    if (isCheckSplit && checkDiskFullAndSplit(fileWriter, isMaster, null, callback)) return

    try {
      messageType match {
        case Type.PUSH_DATA_HAND_SHAKE => {
          fileWriter.asInstanceOf[MapPartitionFileWriter].pushDataHandShake(
            message.asInstanceOf[PushDataHandShake].numPartitions,
            message.asInstanceOf[PushDataHandShake].bufferSize)
        }
        case Type.REGION_START => {
          fileWriter.asInstanceOf[MapPartitionFileWriter].regionStart(
            message.asInstanceOf[RegionStart].currentRegionIndex,
            message.asInstanceOf[RegionStart].isBroadcast)
        }
        case Type.REGION_FINISH => {
          fileWriter.asInstanceOf[MapPartitionFileWriter].regionFinish()
        }
      }
      // for master, send data to slave
      if (location.getPeer != null && isMaster) {
        // to do replica
        wrappedCallback.onSuccess(ByteBuffer.wrap(Array[Byte]()))
      } else {
        wrappedCallback.onSuccess(ByteBuffer.wrap(Array[Byte]()))
      }
    } catch {
      case t: Throwable =>
        callback.onFailure(new CelebornIOException(s"$messageType failed", t))
    }
  }

  class WrappedRpcResponseCallback(
      messageType: Message.Type,
      isMaster: Boolean,
      requestId: Long,
      softSplit: AtomicBoolean,
      location: PartitionLocation,
      workerSourceTime: String,
      callback: RpcResponseCallback)
    extends RpcResponseCallback {
    override def onSuccess(response: ByteBuffer): Unit = {
      workerSource.stopTimer(workerSourceTime, s"$requestId")
      if (isMaster) {
        if (response.remaining() > 0) {
          val resp = ByteBuffer.allocate(response.remaining())
          resp.put(response)
          resp.flip()
          callback.onSuccess(resp)
        } else if (softSplit != null && softSplit.get()) {
          callback.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.SOFT_SPLIT.getValue)))
        } else {
          callback.onSuccess(response)
        }
      } else {
        callback.onSuccess(response)
      }
    }

    override def onFailure(e: Throwable): Unit = {
      if (location != null) {
        logError(s"[handle$messageType.onFailure] partitionLocation: $location")
      }
      messageType match {
        case Type.PUSH_DATA_HAND_SHAKE =>
          workerSource.incCounter(WorkerSource.PushDataHandshakeFailCount)
          callback.onFailure(new CelebornIOException(
            StatusCode.PUSH_DATA_HANDSHAKE_FAIL_SLAVE,
            e))
        case Type.REGION_START =>
          workerSource.incCounter(WorkerSource.RegionStartFailCount)
          callback.onFailure(new CelebornIOException(StatusCode.REGION_START_FAIL_SLAVE, e))
        case Type.REGION_FINISH =>
          workerSource.incCounter(WorkerSource.RegionFinishFailCount)
          callback.onFailure(new CelebornIOException(StatusCode.REGION_FINISH_FAIL_SLAVE, e))
        case _ =>
          workerSource.incCounter(WorkerSource.PushDataFailCount)
          if (e.isInstanceOf[CelebornIOException]) {
            callback.onFailure(e)
          } else {
            callback.onFailure(new CelebornIOException(StatusCode.REPLICATE_DATA_FAILED, e))
          }
      }
    }
  }

  private def checkLocationNull(
      messageType: Message.Type,
      shuffleKey: String,
      partitionUniqueId: String,
      body: ByteBuf,
      location: PartitionLocation,
      callback: RpcResponseCallback,
      wrappedCallback: RpcResponseCallback): Boolean = {
    if (location == null) {
      val (mapId, attemptId) = getMapAttempt(partitionUniqueId)
      if (shuffleMapperAttempts.containsKey(shuffleKey) &&
        -1 != shuffleMapperAttempts.get(shuffleKey).get(mapId)) {
        // partition data has already been committed
        logInfo(s"Receive push data from speculative task(shuffle $shuffleKey, map $mapId, " +
          s" attempt $attemptId), but this mapper has already been ended.")
        wrappedCallback.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.STAGE_ENDED.getValue)))
      } else {
        val msg = s"Partition location wasn't found for task(shuffle $shuffleKey, map $mapId, " +
          s"attempt $attemptId, uniqueId $partitionUniqueId)."
        logWarning(s"[handle$messageType] $msg")
        messageType match {
          case Type.PUSH_MERGED_DATA => callback.onFailure(new CelebornIOException(msg))
          case _ => callback.onFailure(
              new CelebornIOException(StatusCode.PUSH_DATA_FAIL_PARTITION_NOT_FOUND))
        }
      }
      return true
    }
    false
  }

  private def checkFileWriterException(
      messageType: Message.Type,
      isMaster: Boolean,
      fileWriter: FileWriter,
      callback: RpcResponseCallback): Unit = {
    logWarning(
      s"[handle$messageType] fileWriter $fileWriter has Exception ${fileWriter.getException}")

    val (messageMaster, messageSlave) =
      messageType match {
        case Type.PUSH_DATA | Type.PUSH_DATA_HAND_SHAKE =>
          (
            StatusCode.PUSH_DATA_WRITE_FAIL_MASTER,
            StatusCode.PUSH_DATA_WRITE_FAIL_SLAVE)
        case Type.PUSH_DATA_HAND_SHAKE => (
            StatusCode.PUSH_DATA_HANDSHAKE_FAIL_MASTER,
            StatusCode.PUSH_DATA_HANDSHAKE_FAIL_SLAVE)
        case Type.REGION_START => (
            StatusCode.REGION_START_FAIL_MASTER,
            StatusCode.REGION_START_FAIL_SLAVE)
        case Type.REGION_FINISH => (
            StatusCode.REGION_FINISH_FAIL_MASTER,
            StatusCode.REGION_FINISH_FAIL_SLAVE)
      }
    callback.onFailure(new CelebornIOException(
      if (isMaster) messageMaster else messageSlave,
      fileWriter.getException))
  }

  private def getFileWriterAndCheck(
      messageType: Message.Type,
      location: PartitionLocation,
      isMaster: Boolean,
      callback: RpcResponseCallback): (Boolean, FileWriter) = {
    val fileWriter = location.asInstanceOf[WorkingPartition].getFileWriter
    val exception = fileWriter.getException
    if (exception != null) {
      checkFileWriterException(messageType, isMaster, fileWriter, callback)
      return (true, fileWriter)
    }
    (false, fileWriter)
  }

  private def checkDiskFull(fileWriter: FileWriter): Boolean = {
    if (fileWriter.flusher.isInstanceOf[HdfsFlusher]) {
      return false
    }
    val diskFull = workerInfo.diskInfos
      .get(fileWriter.flusher.asInstanceOf[LocalFlusher].mountPoint)
      .actualUsableSpace < diskReserveSize
    diskFull
  }

  private def checkDiskFullAndSplit(
      fileWriter: FileWriter,
      isMaster: Boolean,
      softSplit: AtomicBoolean,
      callback: RpcResponseCallback): Boolean = {
    val diskFull = checkDiskFull(fileWriter)
    if ((diskFull && fileWriter.getFileInfo.getFileLength > partitionSplitMinimumSize) ||
      (isMaster && fileWriter.getFileInfo.getFileLength > fileWriter.getSplitThreshold())) {
      if (softSplit != null && fileWriter.getSplitMode == PartitionSplitMode.SOFT) {
        softSplit.set(true)
      } else {
        callback.onSuccess(ByteBuffer.wrap(Array[Byte](StatusCode.HARD_SPLIT.getValue)))
        return true
      }
    }
    false
  }

  private def getMapAttempt(
      partitionUniqueId: String): (Int, Int) = {
    val id = partitionUniqueId.split("-")(0).toInt
    (PackedPartitionId.getRawPartitionId(id), PackedPartitionId.getAttemptId(id))
  }

  private def getClient(host: String, port: Int, partitionId: Int): TransportClient = {
    if (conf.workerReplicateRandomConnectionEnabled) {
      pushClientFactory.createClient(host, port)
    } else {
      pushClientFactory.createClient(host, port, partitionId)
    }
  }
}

object PushDataHandler {
  @volatile var pushMasterDataTimeoutTested = false
  @volatile var pushSlaveDataTimeoutTested = false
}
