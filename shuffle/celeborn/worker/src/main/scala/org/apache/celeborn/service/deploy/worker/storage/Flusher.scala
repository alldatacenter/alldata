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

package org.apache.celeborn.service.deploy.worker.storage

import java.io.IOException
import java.nio.channels.ClosedByInterruptException
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLongArray, LongAdder}

import scala.collection.JavaConverters._
import scala.util.Random

import io.netty.buffer.{CompositeByteBuf, Unpooled}

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.DiskStatus
import org.apache.celeborn.common.metrics.source.AbstractSource
import org.apache.celeborn.common.network.server.memory.MemoryManager
import org.apache.celeborn.common.protocol.StorageInfo
import org.apache.celeborn.service.deploy.worker.WorkerSource
import org.apache.celeborn.service.deploy.worker.congestcontrol.CongestionController

abstract private[worker] class Flusher(
    val workerSource: AbstractSource,
    val threadCount: Int,
    val avgFlushTimeSlidingWindowSize: Int,
    val avgFlushTimeSlidingWindowMinCount: Int) extends Logging {
  protected lazy val flusherId = System.identityHashCode(this)
  protected val workingQueues = new Array[LinkedBlockingQueue[FlushTask]](threadCount)
  protected val bufferQueue = new LinkedBlockingQueue[CompositeByteBuf]()
  protected val workers = new Array[Thread](threadCount)
  protected var nextWorkerIndex: Int = 0
  protected val flushCount = new LongAdder
  protected val flushTotalTime = new LongAdder
  protected val avgTimeWindow = new Array[(Long, Long)](avgFlushTimeSlidingWindowSize)
  protected var avgTimeWindowCurrentIndex = 0

  val lastBeginFlushTime: AtomicLongArray = new AtomicLongArray(threadCount)
  val stopFlag = new AtomicBoolean(false)
  val rand = new Random()

  init()

  private def init(): Unit = {
    for (i <- 0 until avgFlushTimeSlidingWindowSize) {
      avgTimeWindow(i) = (0L, 0L)
    }
    for (i <- 0 until lastBeginFlushTime.length()) {
      lastBeginFlushTime.set(i, -1)
    }
    for (index <- 0 until threadCount) {
      workingQueues(index) = new LinkedBlockingQueue[FlushTask]()
      workers(index) = new Thread(s"$this-$index") {
        override def run(): Unit = {
          while (!stopFlag.get()) {
            val task = workingQueues(index).take()
            val key = s"Flusher-$this-${rand.nextInt()}"
            workerSource.sample(WorkerSource.FlushDataTime, key) {
              if (!task.notifier.hasException) {
                try {
                  val flushBeginTime = System.nanoTime()
                  lastBeginFlushTime.set(index, flushBeginTime)
                  task.flush()
                  flushTotalTime.add(System.nanoTime() - flushBeginTime)
                  flushCount.increment()
                } catch {
                  case _: ClosedByInterruptException =>
                  case e: IOException =>
                    task.notifier.setException(e)
                    processIOException(e, DiskStatus.READ_OR_WRITE_FAILURE)
                }
                lastBeginFlushTime.set(index, -1)
              }
              returnBuffer(task.buffer)
              task.notifier.numPendingFlushes.decrementAndGet()
            }
          }
        }
      }
      workers(index).setDaemon(true)
      workers(index).setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler {
        override def uncaughtException(t: Thread, e: Throwable): Unit = {
          logError(s"$this thread terminated.", e)
        }
      })
      workers(index).start()
    }
  }

  def getWorkerIndex: Int = synchronized {
    nextWorkerIndex = (nextWorkerIndex + 1) % threadCount
    nextWorkerIndex
  }

  def averageFlushTime(): Long = {
    if (this.isInstanceOf[LocalFlusher]) {
      logInfo(s"Flush count in ${this.asInstanceOf[LocalFlusher].mountPoint}" +
        s" last heartbeat interval: $flushCount")
    }
    val currentFlushTime = flushTotalTime.sumThenReset()
    val currentFlushCount = flushCount.sumThenReset()
    if (currentFlushCount >= avgFlushTimeSlidingWindowMinCount) {
      avgTimeWindow(avgTimeWindowCurrentIndex) = (currentFlushTime, currentFlushCount)
      avgTimeWindowCurrentIndex = (avgTimeWindowCurrentIndex + 1) % avgFlushTimeSlidingWindowSize
    }

    var totalFlushTime = 0L
    var totalFlushCount = 0L
    avgTimeWindow.foreach { case (flushTime, flushCount) =>
      totalFlushTime = totalFlushTime + flushTime
      totalFlushCount = totalFlushCount + flushCount
    }

    if (totalFlushCount != 0) {
      totalFlushTime / totalFlushCount
    } else {
      0L
    }
  }

  def takeBuffer(): CompositeByteBuf = {
    var buffer = bufferQueue.poll()
    if (buffer == null) {
      buffer = Unpooled.compositeBuffer(256)
    }
    buffer
  }

  def returnBuffer(buffer: CompositeByteBuf): Unit = {
    MemoryManager.instance().releaseDiskBuffer(buffer.readableBytes())
    Option(CongestionController.instance())
      .foreach(
        _.consumeBytes(buffer.readableBytes()))
    buffer.removeComponents(0, buffer.numComponents())
    buffer.clear()

    bufferQueue.put(buffer)
  }

  def addTask(task: FlushTask, timeoutMs: Long, workerIndex: Int): Boolean = {
    workingQueues(workerIndex).offer(task, timeoutMs, TimeUnit.MILLISECONDS)
  }

  def bufferQueueInfo(): String = s"$this used buffers: ${bufferQueue.size()}"

  def stopAndCleanFlusher(): Unit = {
    stopFlag.set(true)
    try {
      workers.foreach(_.interrupt())
    } catch {
      case e: Exception =>
        logError(s"Exception when interrupt worker: ${workers.mkString(",")}, $e")
    }
    workingQueues.foreach { queue =>
      queue.asScala.foreach { task =>
        returnBuffer(task.buffer)
      }
    }
  }

  def processIOException(e: IOException, deviceErrorType: DiskStatus): Unit
}

private[worker] class LocalFlusher(
    workerSource: AbstractSource,
    val deviceMonitor: DeviceMonitor,
    threadCount: Int,
    val mountPoint: String,
    avgFlushTimeSlidingWindowSize: Int,
    flushAvgTimeMinimumCount: Int,
    val diskType: StorageInfo.Type) extends Flusher(
    workerSource,
    threadCount,
    avgFlushTimeSlidingWindowSize,
    flushAvgTimeMinimumCount)
  with DeviceObserver with Logging {

  deviceMonitor.registerFlusher(this)

  override def processIOException(e: IOException, deviceErrorType: DiskStatus): Unit = {
    logError(s"$this write failed, report to DeviceMonitor, exception: $e")
    deviceMonitor.reportNonCriticalError(mountPoint, e, deviceErrorType)
  }

  override def notifyError(mountPoint: String, diskStatus: DiskStatus): Unit = {
    logError(s"$this is notified Disk $mountPoint $diskStatus! Stop LocalFlusher.")
    stopAndCleanFlusher()
    deviceMonitor.unregisterFlusher(this)
  }

  override def hashCode(): Int = {
    mountPoint.hashCode()
  }

  override def equals(obj: Any): Boolean = {
    obj.isInstanceOf[LocalFlusher] &&
    obj.asInstanceOf[LocalFlusher].mountPoint.equals(mountPoint)
  }

  override def toString(): String = {
    s"LocalFlusher@$flusherId-$mountPoint"
  }
}

final private[worker] class HdfsFlusher(
    workerSource: AbstractSource,
    hdfsFlusherThreads: Int,
    flushAvgTimeWindowSize: Int,
    avgFlushTimeSlidingWindowMinCount: Int) extends Flusher(
    workerSource,
    hdfsFlusherThreads,
    flushAvgTimeWindowSize,
    avgFlushTimeSlidingWindowMinCount) with Logging {
  override def toString: String = s"HdfsFlusher@$flusherId"

  override def processIOException(e: IOException, deviceErrorType: DiskStatus): Unit = {
    stopAndCleanFlusher()
    logError(s"$this write failed, reason $deviceErrorType ,exception: $e")
  }

}
