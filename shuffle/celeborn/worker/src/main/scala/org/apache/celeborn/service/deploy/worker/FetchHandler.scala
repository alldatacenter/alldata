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

import java.io.{FileNotFoundException, IOException}
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

import com.google.common.base.Throwables
import io.netty.util.concurrent.{Future, GenericFutureListener}

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{FileInfo, FileManagedBuffers}
import org.apache.celeborn.common.network.buffer.NioManagedBuffer
import org.apache.celeborn.common.network.client.TransportClient
import org.apache.celeborn.common.network.protocol._
import org.apache.celeborn.common.network.protocol.Message.Type
import org.apache.celeborn.common.network.server.BaseMessageHandler
import org.apache.celeborn.common.network.util.{NettyUtils, TransportConf}
import org.apache.celeborn.common.protocol.PartitionType
import org.apache.celeborn.common.util.ExceptionUtils
import org.apache.celeborn.service.deploy.worker.storage.{ChunkStreamManager, CreditStreamManager, PartitionFilesSorter, StorageManager}

class FetchHandler(val conf: CelebornConf, val transportConf: TransportConf)
  extends BaseMessageHandler with Logging {
  var chunkStreamManager = new ChunkStreamManager()
  val creditStreamManager = new CreditStreamManager(
    conf.partitionReadBuffersMin,
    conf.partitionReadBuffersMax,
    conf.creditStreamThreadsPerMountpoint,
    conf.readBuffersToTriggerReadMin)
  var workerSource: WorkerSource = _
  var storageManager: StorageManager = _
  var partitionsSorter: PartitionFilesSorter = _
  var registered: AtomicBoolean = new AtomicBoolean(false)

  def init(worker: Worker): Unit = {
    this.workerSource = worker.workerSource

    workerSource.addGauge(WorkerSource.CREDIT_STREAM_COUNT) { () =>
      creditStreamManager.getStreamsCount
    }

    workerSource.addGauge(WorkerSource.ACTIVE_MAP_PARTITION_COUNT) { () =>
      creditStreamManager.getActiveMapPartitionCount
    }

    this.storageManager = worker.storageManager
    this.partitionsSorter = worker.partitionsSorter
    this.registered = worker.registered
  }

  def getRawFileInfo(
      shuffleKey: String,
      fileName: String): FileInfo = {
    // find FileWriter responsible for the data
    val fileInfo = storageManager.getFileInfo(shuffleKey, fileName)
    if (fileInfo == null) {
      val errMsg = s"Could not find file $fileName for $shuffleKey."
      logWarning(errMsg)
      throw new FileNotFoundException(errMsg)
    }
    fileInfo
  }

  override def receive(client: TransportClient, msg: RequestMessage): Unit = {
    msg match {
      case r: BufferStreamEnd =>
        handleEndStreamFromClient(r)
      case r: ReadAddCredit =>
        handleReadAddCredit(r)
      case r: ChunkFetchRequest =>
        handleChunkFetchRequest(client, r)
      case r: RpcRequest =>
        val msg = Message.decode(r.body().nioByteBuffer())
        handleOpenStream(client, r, msg)
      case unknown: RequestMessage =>
        throw new IllegalArgumentException(s"Unknown message type id: ${unknown.`type`.id}")
    }
  }

  // here are BackLogAnnouncement,OpenStream and OpenStreamWithCredit RPCs to handle
  def handleOpenStream(client: TransportClient, request: RpcRequest, msg: Message): Unit = {
    val (shuffleKey, fileName) =
      if (msg.`type`() == Type.OPEN_STREAM) {
        val openStream = msg.asInstanceOf[OpenStream]
        (
          new String(openStream.shuffleKey, StandardCharsets.UTF_8),
          new String(openStream.fileName, StandardCharsets.UTF_8))
      } else {
        val openStreamWithCredit = msg.asInstanceOf[OpenStreamWithCredit]
        (
          new String(openStreamWithCredit.shuffleKey, StandardCharsets.UTF_8),
          new String(openStreamWithCredit.fileName, StandardCharsets.UTF_8))
      }
    // metrics start
    workerSource.startTimer(WorkerSource.OPEN_STREAM_TIME, shuffleKey)
    try {
      var fileInfo = getRawFileInfo(shuffleKey, fileName)
      try fileInfo.getPartitionType match {
        case PartitionType.REDUCE =>
          val startMapIndex = msg.asInstanceOf[OpenStream].startMapIndex
          val endMapIndex = msg.asInstanceOf[OpenStream].endMapIndex
          if (endMapIndex != Integer.MAX_VALUE) {
            fileInfo = partitionsSorter.getSortedFileInfo(
              shuffleKey,
              fileName,
              fileInfo,
              startMapIndex,
              endMapIndex)
          }
          logDebug(s"Received chunk fetch request $shuffleKey $fileName " +
            s"$startMapIndex $endMapIndex get file info $fileInfo")
          if (fileInfo.isHdfs) {
            val streamHandle = new StreamHandle(0, 0)
            client.getChannel.writeAndFlush(new RpcResponse(
              request.requestId,
              new NioManagedBuffer(streamHandle.toByteBuffer)))
          } else {
            val buffers = new FileManagedBuffers(fileInfo, transportConf)
            val fetchTimeMetrics = storageManager.getFetchTimeMetric(fileInfo.getFile)
            val streamId = chunkStreamManager.registerStream(
              shuffleKey,
              buffers,
              fetchTimeMetrics)
            val streamHandle = new StreamHandle(streamId, fileInfo.numChunks())
            if (fileInfo.numChunks() == 0)
              logDebug(s"StreamId $streamId fileName $fileName startMapIndex" +
                s" $startMapIndex endMapIndex $endMapIndex is empty.")
            else logDebug(
              s"StreamId $streamId fileName $fileName numChunks ${fileInfo.numChunks()} " +
                s"startMapIndex $startMapIndex endMapIndex $endMapIndex")
            client.getChannel.writeAndFlush(new RpcResponse(
              request.requestId,
              new NioManagedBuffer(streamHandle.toByteBuffer)))
          }
        case PartitionType.MAP =>
          val initialCredit = msg.asInstanceOf[OpenStreamWithCredit].initialCredit
          val startIndex = msg.asInstanceOf[OpenStreamWithCredit].startIndex
          val endIndex = msg.asInstanceOf[OpenStreamWithCredit].endIndex

          val callback = new Consumer[java.lang.Long] {
            override def accept(streamId: java.lang.Long): Unit = {
              val bufferStreamHandle = new StreamHandle(streamId, 0)
              client.getChannel.writeAndFlush(new RpcResponse(
                request.requestId,
                new NioManagedBuffer(bufferStreamHandle.toByteBuffer)))
            }
          }

          creditStreamManager.registerStream(
            callback,
            client.getChannel,
            initialCredit,
            startIndex,
            endIndex,
            fileInfo)

        case PartitionType.MAPGROUP =>
      } catch {
        case e: IOException =>
          handleRpcIOException(client, request.requestId, e)
      } finally {
        // metrics end
        workerSource.stopTimer(WorkerSource.OPEN_STREAM_TIME, shuffleKey)
        request.body().release()
      }
    } catch {
      case ioe: IOException =>
        workerSource.stopTimer(WorkerSource.OPEN_STREAM_TIME, shuffleKey)
        handleRpcIOException(client, request.requestId, ioe)
    }
  }

  private def handleRpcIOException(
      client: TransportClient,
      requestId: Long,
      ioe: IOException): Unit = {
    // if open stream rpc failed, this IOException actually should be FileNotFoundException
    // we wrapper this IOException(Other place may have other exception like FileCorruptException) unify to
    // PartitionUnRetryableException for reader can give up this partition and choose to regenerate the partition data
    client.getChannel.writeAndFlush(new RpcFailure(
      requestId,
      Throwables.getStackTraceAsString(ExceptionUtils.wrapIOExceptionToUnRetryable(ioe, false))))
  }

  def handleEndStreamFromClient(req: BufferStreamEnd): Unit = {
    creditStreamManager.notifyStreamEndByClient(req.getStreamId)
  }

  def handleReadAddCredit(req: ReadAddCredit): Unit = {
    creditStreamManager.addCredit(req.getCredit, req.getStreamId)
  }

  def handleChunkFetchRequest(client: TransportClient, req: ChunkFetchRequest): Unit = {
    logTrace(s"Received req from ${NettyUtils.getRemoteAddress(client.getChannel)}" +
      s" to fetch block ${req.streamChunkSlice}")

    val chunksBeingTransferred = chunkStreamManager.chunksBeingTransferred
    if (chunksBeingTransferred > conf.shuffleIoMaxChunksBeingTransferred) {
      val message = "Worker is too busy. The number of chunks being transferred " +
        s"$chunksBeingTransferred exceeds celeborn.shuffle.maxChunksBeingTransferred " +
        s"${conf.shuffleIoMaxChunksBeingTransferred}."
      logError(message)
      client.getChannel.writeAndFlush(new ChunkFetchFailure(req.streamChunkSlice, message))
    } else {
      workerSource.startTimer(WorkerSource.FETCH_CHUNK_TIME, req.toString)
      val fetchTimeMetric = chunkStreamManager.getFetchTimeMetric(req.streamChunkSlice.streamId)
      val fetchBeginTime = System.nanoTime()
      try {
        val buf = chunkStreamManager.getChunk(
          req.streamChunkSlice.streamId,
          req.streamChunkSlice.chunkIndex,
          req.streamChunkSlice.offset,
          req.streamChunkSlice.len)
        chunkStreamManager.chunkBeingSent(req.streamChunkSlice.streamId)
        client.getChannel.writeAndFlush(new ChunkFetchSuccess(req.streamChunkSlice, buf))
          .addListener(new GenericFutureListener[Future[_ >: Void]] {
            override def operationComplete(future: Future[_ >: Void]): Unit = {
              chunkStreamManager.chunkSent(req.streamChunkSlice.streamId)
              if (fetchTimeMetric != null) {
                fetchTimeMetric.update(System.nanoTime() - fetchBeginTime)
              }
              workerSource.stopTimer(WorkerSource.FETCH_CHUNK_TIME, req.toString)
            }
          })
      } catch {
        case e: Exception =>
          logError(
            s"Error opening block ${req.streamChunkSlice} for request from " +
              NettyUtils.getRemoteAddress(client.getChannel),
            e)
          client.getChannel.writeAndFlush(new ChunkFetchFailure(
            req.streamChunkSlice,
            Throwables.getStackTraceAsString(e)))
          workerSource.stopTimer(WorkerSource.FETCH_CHUNK_TIME, req.toString)
      }
    }
  }

  override def checkRegistered: Boolean = registered.get

  override def channelInactive(client: TransportClient): Unit = {
    creditStreamManager.connectionTerminated(client.getChannel)
    logDebug(s"channel inactive ${client.getSocketAddress}")
  }

  override def exceptionCaught(cause: Throwable, client: TransportClient): Unit = {
    logWarning(s"exception caught ${client.getSocketAddress}", cause)
  }

  def cleanupExpiredShuffleKey(expiredShuffleKeys: util.HashSet[String]): Unit = {
    chunkStreamManager.cleanupExpiredShuffleKey(expiredShuffleKeys)
  }
}
