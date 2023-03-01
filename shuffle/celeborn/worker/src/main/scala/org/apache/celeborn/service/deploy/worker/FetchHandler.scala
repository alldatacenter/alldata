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

import org.apache.celeborn.common.exception.CelebornException
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{FileInfo, FileManagedBuffers}
import org.apache.celeborn.common.metrics.source.RPCSource
import org.apache.celeborn.common.network.buffer.NioManagedBuffer
import org.apache.celeborn.common.network.client.TransportClient
import org.apache.celeborn.common.network.protocol._
import org.apache.celeborn.common.network.protocol.Message.Type
import org.apache.celeborn.common.network.server.{BaseMessageHandler, BufferStreamManager, ChunkStreamManager}
import org.apache.celeborn.common.network.util.{NettyUtils, TransportConf}
import org.apache.celeborn.common.protocol.PartitionType
import org.apache.celeborn.service.deploy.worker.storage.{PartitionFilesSorter, StorageManager}

class FetchHandler(val conf: TransportConf) extends BaseMessageHandler with Logging {
  var chunkStreamManager = new ChunkStreamManager()
  val bufferStreamManager = new BufferStreamManager(
    conf.getCelebornConf.partitionReadBuffersMin,
    conf.getCelebornConf.partitionReadBuffersMax,
    conf.getCelebornConf.bufferStreamThreadsPerMountpoint)
  var workerSource: WorkerSource = _
  var rpcSource: RPCSource = _
  var storageManager: StorageManager = _
  var partitionsSorter: PartitionFilesSorter = _
  var registered: AtomicBoolean = new AtomicBoolean(false)

  def init(worker: Worker): Unit = {
    this.workerSource = worker.workerSource
    this.rpcSource = worker.rpcSource
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
      case r: ReadAddCredit =>
        rpcSource.updateMessageMetrics(r, 0)
        handleReadAddCredit(client, r)
      case r: ChunkFetchRequest =>
        rpcSource.updateMessageMetrics(r, 0)
        handleChunkFetchRequest(client, r)
      case r: RpcRequest =>
        handleOpenStream(client, r)
      case unknown: RequestMessage =>
        throw new IllegalArgumentException(s"Unknown message type id: ${unknown.`type`.id}")
    }
  }

  // here are BackLogAnnouncement,OpenStream and OpenStreamWithCredit RPCs to handle
  def handleOpenStream(client: TransportClient, request: RpcRequest): Unit = {
    val msg = Message.decode(request.body().nioByteBuffer())
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
    workerSource.startTimer(WorkerSource.OpenStreamTime, shuffleKey)
    try {
      var fileInfo = getRawFileInfo(shuffleKey, fileName)
      try fileInfo.getPartitionType() match {
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
            val buffers = new FileManagedBuffers(fileInfo, conf)
            val streamId = chunkStreamManager.registerStream(shuffleKey, buffers)
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
                new NioManagedBuffer(bufferStreamHandle
                  .toByteBuffer)))
            }
          }

          bufferStreamManager.registerStream(
            callback,
            client.getChannel,
            initialCredit,
            startIndex,
            endIndex,
            fileInfo)

        case PartitionType.MAPGROUP =>
      } catch {
        case e: IOException =>
          client.getChannel.writeAndFlush(new RpcFailure(
            request.requestId,
            Throwables.getStackTraceAsString(
              new CelebornException("Chunk offsets meta exception", e))))
      } finally {
        // metrics end
        workerSource.stopTimer(WorkerSource.OpenStreamTime, shuffleKey)
        request.body().release()
      }
    } catch {
      case ioe: IOException =>
        workerSource.stopTimer(WorkerSource.OpenStreamTime, shuffleKey)
        client.getChannel.writeAndFlush(new RpcFailure(
          request.requestId,
          Throwables.getStackTraceAsString(ioe)))
    }
  }

  def handleReadAddCredit(client: TransportClient, req: ReadAddCredit): Unit = {
    bufferStreamManager.addCredit(req.getCredit, req.getStreamId)
  }

  def handleChunkFetchRequest(client: TransportClient, req: ChunkFetchRequest): Unit = {
    workerSource.startTimer(WorkerSource.FetchChunkTime, req.toString)
    logTrace(s"Received req from ${NettyUtils.getRemoteAddress(client.getChannel)}" +
      s" to fetch block ${req.streamChunkSlice}")

    val chunksBeingTransferred = chunkStreamManager.chunksBeingTransferred
    if (chunksBeingTransferred > conf.maxChunksBeingTransferred) {
      val message = "Worker is too busy. The number of chunks being transferred " +
        s"$chunksBeingTransferred exceeds celeborn.shuffle.maxChunksBeingTransferred " +
        s"${conf.maxChunksBeingTransferred}."
      logError(message)
      client.getChannel.writeAndFlush(
        new ChunkFetchFailure(req.streamChunkSlice, message))
      workerSource.stopTimer(WorkerSource.FetchChunkTime, req.toString)
    } else {
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
              workerSource.stopTimer(WorkerSource.FetchChunkTime, req.toString)
            }
          })
      } catch {
        case e: Exception =>
          logError(
            String.format(s"Error opening block ${req.streamChunkSlice} for request from" +
              s" ${NettyUtils.getRemoteAddress(client.getChannel)}"),
            e)
          client.getChannel.writeAndFlush(new ChunkFetchFailure(
            req.streamChunkSlice,
            Throwables.getStackTraceAsString(e)))
          workerSource.stopTimer(WorkerSource.FetchChunkTime, req.toString)
      }
    }
  }

  override def checkRegistered: Boolean = registered.get

  override def channelInactive(client: TransportClient): Unit = {
    bufferStreamManager.connectionTerminated(client.getChannel)
    logDebug(s"channel inactive ${client.getSocketAddress}")
  }

  override def exceptionCaught(cause: Throwable, client: TransportClient): Unit = {
    logWarning(s"exception caught ${client.getSocketAddress}", cause)
  }

  def cleanupExpiredShuffleKey(expiredShuffleKeys: util.HashSet[String]): Unit = {
    chunkStreamManager.cleanupExpiredShuffleKey(expiredShuffleKeys)
  }
}
