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

import java.lang.{Long => JLong}
import java.util.{HashMap => JHashMap, HashSet => JHashSet}
import java.util.concurrent._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicIntegerArray}

import scala.collection.JavaConverters._

import com.google.common.annotations.VisibleForTesting
import io.netty.util.HashedWheelTimer

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.CelebornConf._
import org.apache.celeborn.common.exception.CelebornException
import org.apache.celeborn.common.haclient.RssHARetryClient
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DiskInfo, WorkerInfo, WorkerPartitionLocationInfo}
import org.apache.celeborn.common.metrics.MetricsSystem
import org.apache.celeborn.common.metrics.source.{JVMCPUSource, JVMSource, RPCSource}
import org.apache.celeborn.common.network.TransportContext
import org.apache.celeborn.common.network.server.ChannelsLimiter
import org.apache.celeborn.common.network.server.memory.MemoryManager
import org.apache.celeborn.common.protocol.{PartitionType, PbRegisterWorkerResponse, RpcNameConstants, TransportModuleConstants}
import org.apache.celeborn.common.protocol.message.ControlMessages._
import org.apache.celeborn.common.quota.ResourceConsumption
import org.apache.celeborn.common.rpc._
import org.apache.celeborn.common.util.{ShutdownHookManager, ThreadUtils, Utils}
import org.apache.celeborn.server.common.{HttpService, Service}
import org.apache.celeborn.service.deploy.worker.congestcontrol.CongestionController
import org.apache.celeborn.service.deploy.worker.storage.{FileWriter, PartitionFilesSorter, StorageManager}

private[celeborn] class Worker(
    override val conf: CelebornConf,
    val workerArgs: WorkerArguments)
  extends HttpService with Logging {

  @volatile private var stopped = false

  override def serviceName: String = Service.WORKER

  override val metricsSystem: MetricsSystem =
    MetricsSystem.createMetricsSystem(serviceName, conf, WorkerSource.ServletPath)

  val rpcEnv = RpcEnv.create(
    RpcNameConstants.WORKER_SYS,
    workerArgs.host,
    workerArgs.host,
    workerArgs.port,
    conf,
    Math.max(64, Runtime.getRuntime.availableProcessors()))

  private val host = rpcEnv.address.host
  private val rpcPort = rpcEnv.address.port
  Utils.checkHost(host)

  private val WORKER_SHUTDOWN_PRIORITY = 100
  val shutdown = new AtomicBoolean(false)
  private val gracefulShutdown = conf.workerGracefulShutdown
  assert(
    !gracefulShutdown || (gracefulShutdown &&
      conf.workerRpcPort != 0 && conf.workerFetchPort != 0 &&
      conf.workerPushPort != 0 && conf.workerReplicatePort != 0),
    "If enable graceful shutdown, the worker should use stable server port.")

  val rpcSource = new RPCSource(conf, MetricsSystem.ROLE_WORKER)
  val workerSource = new WorkerSource(conf)
  metricsSystem.registerSource(workerSource)
  metricsSystem.registerSource(rpcSource)
  metricsSystem.registerSource(new JVMSource(conf, MetricsSystem.ROLE_WORKER))
  metricsSystem.registerSource(new JVMCPUSource(conf, MetricsSystem.ROLE_WORKER))

  val storageManager = new StorageManager(conf, workerSource)

  val memoryTracker = MemoryManager.initialize(
    conf.workerDirectMemoryRatioToPauseReceive,
    conf.workerDirectMemoryRatioToPauseReplicate,
    conf.workerDirectMemoryRatioToResume,
    conf.partitionSorterDirectMemoryRatioThreshold,
    conf.workerDirectMemoryRatioForReadBuffer,
    conf.workerDirectMemoryRatioForShuffleStorage,
    conf.workerDirectMemoryPressureCheckIntervalMs,
    conf.workerDirectMemoryReportIntervalSecond)
  memoryTracker.registerMemoryListener(storageManager)

  val partitionsSorter = new PartitionFilesSorter(memoryTracker, conf, workerSource)

  if (conf.workerCongestionControlEnabled) {
    if (conf.workerCongestionControlLowWatermark.isEmpty || conf.workerCongestionControlHighWatermark.isEmpty) {
      throw new IllegalArgumentException("High watermark and low watermark must be set" +
        " when enabling rate limit")
    }

    CongestionController.initialize(
      workerSource,
      conf.workerCongestionControlSampleTimeWindowSeconds.toInt,
      conf.workerCongestionControlHighWatermark.get,
      conf.workerCongestionControlLowWatermark.get,
      conf.workerCongestionControlUserInactiveIntervalMs)
  }

  var controller = new Controller(rpcEnv, conf, metricsSystem)
  rpcEnv.setupEndpoint(RpcNameConstants.WORKER_EP, controller, Some(rpcSource))

  val pushDataHandler = new PushDataHandler()
  val (pushServer, pushClientFactory) = {
    val closeIdleConnections = conf.workerCloseIdleConnections
    val numThreads = conf.workerPushIoThreads.getOrElse(storageManager.totalFlusherThread)
    val transportConf =
      Utils.fromCelebornConf(conf, TransportModuleConstants.PUSH_MODULE, numThreads)
    val pushServerLimiter = new ChannelsLimiter(TransportModuleConstants.PUSH_MODULE)
    val transportContext: TransportContext =
      new TransportContext(transportConf, pushDataHandler, closeIdleConnections, pushServerLimiter)
    (
      transportContext.createServer(conf.workerPushPort),
      transportContext.createClientFactory())
  }

  val replicateHandler = new PushDataHandler()
  private val replicateServer = {
    val closeIdleConnections = conf.workerCloseIdleConnections
    val numThreads =
      conf.workerReplicateIoThreads.getOrElse(storageManager.totalFlusherThread)
    val transportConf =
      Utils.fromCelebornConf(conf, TransportModuleConstants.REPLICATE_MODULE, numThreads)
    val replicateLimiter = new ChannelsLimiter(TransportModuleConstants.REPLICATE_MODULE)
    val transportContext: TransportContext =
      new TransportContext(transportConf, replicateHandler, closeIdleConnections, replicateLimiter)
    transportContext.createServer(conf.workerReplicatePort)
  }

  var fetchHandler: FetchHandler = _
  private val fetchServer = {
    val closeIdleConnections = conf.workerCloseIdleConnections
    val numThreads = conf.workerFetchIoThreads.getOrElse(storageManager.totalFlusherThread)
    val transportConf =
      Utils.fromCelebornConf(conf, TransportModuleConstants.FETCH_MODULE, numThreads)
    fetchHandler = new FetchHandler(transportConf)
    val transportContext: TransportContext =
      new TransportContext(transportConf, fetchHandler, closeIdleConnections)
    transportContext.createServer(conf.workerFetchPort)
  }

  private val pushPort = pushServer.getPort
  private val fetchPort = fetchServer.getPort
  private val replicatePort = replicateServer.getPort

  assert(pushPort > 0)
  assert(fetchPort > 0)
  assert(replicatePort > 0)

  storageManager.updateDiskInfos()
  // WorkerInfo's diskInfos is a reference to storageManager.diskInfos
  val diskInfos = new ConcurrentHashMap[String, DiskInfo]()
  storageManager.disksSnapshot().foreach { case diskInfo =>
    diskInfos.put(diskInfo.mountPoint, diskInfo)
  }

  // need to ensure storageManager has recovered fileinfos data if enable graceful shutdown before retrieve consumption
  val userResourceConsumption = new ConcurrentHashMap[UserIdentifier, ResourceConsumption](
    storageManager.userResourceConsumptionSnapshot().asJava)

  val workerInfo =
    new WorkerInfo(
      host,
      rpcPort,
      pushPort,
      fetchPort,
      replicatePort,
      diskInfos,
      userResourceConsumption,
      controller.self)

  // whether this Worker registered to Master successfully
  val registered = new AtomicBoolean(false)
  val shuffleMapperAttempts = new ConcurrentHashMap[String, AtomicIntegerArray]()
  val shufflePartitionType = new ConcurrentHashMap[String, PartitionType]
  var shufflePushDataTimeout = new ConcurrentHashMap[String, Long]
  val partitionLocationInfo = new WorkerPartitionLocationInfo

  val shuffleCommitInfos = new ConcurrentHashMap[String, ConcurrentHashMap[Long, CommitInfo]]()

  private val rssHARetryClient = new RssHARetryClient(rpcEnv, conf)

  // (workerInfo -> last connect timeout timestamp)
  val unavailablePeers = new ConcurrentHashMap[WorkerInfo, Long]()

  // Threads
  private val forwardMessageScheduler =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("worker-forward-message-scheduler")
  private var sendHeartbeatTask: ScheduledFuture[_] = _
  private var checkFastfailTask: ScheduledFuture[_] = _
  val replicateThreadPool = ThreadUtils.newDaemonCachedThreadPool(
    "worker-replicate-data",
    conf.workerReplicateThreads)
  val commitThreadPool = ThreadUtils.newDaemonCachedThreadPool(
    "Worker-CommitFiles",
    conf.workerCommitThreads)
  val asyncReplyPool = ThreadUtils.newDaemonSingleThreadScheduledExecutor("async-reply")
  val timer = new HashedWheelTimer()

  // Configs
  private val HEARTBEAT_MILLIS = conf.workerHeartbeatTimeout / 4
  private val REPLICATE_FAST_FAIL_DURATION = conf.workerReplicateFastFailDuration

  private val cleanTaskQueue = new LinkedBlockingQueue[JHashSet[String]]
  var cleaner: Thread = _

  workerSource.addGauge(
    WorkerSource.RegisteredShuffleCount,
    _ => workerInfo.getShuffleKeySet.size())
  workerSource.addGauge(WorkerSource.SlotsAllocated, _ => workerInfo.allocationsInLastHour())
  workerSource.addGauge(WorkerSource.SortMemory, _ => memoryTracker.getSortMemoryCounter.get())
  workerSource.addGauge(WorkerSource.SortingFiles, _ => partitionsSorter.getSortingCount)
  workerSource.addGauge(WorkerSource.SortedFiles, _ => partitionsSorter.getSortedCount)
  workerSource.addGauge(WorkerSource.SortedFileSize, _ => partitionsSorter.getSortedSize)
  workerSource.addGauge(WorkerSource.DiskBuffer, _ => memoryTracker.getDiskBufferCounter.get())
  workerSource.addGauge(WorkerSource.NettyMemory, _ => memoryTracker.getNettyMemoryCounter.get())
  workerSource.addGauge(WorkerSource.PausePushDataCount, _ => memoryTracker.getPausePushDataCounter)
  workerSource.addGauge(
    WorkerSource.PausePushDataAndReplicateCount,
    _ => memoryTracker.getPausePushDataAndReplicateCounter)
  workerSource.addGauge(
    WorkerSource.BufferStreamReadBuffer,
    _ => memoryTracker.getReadBufferCounter.get())

  private def heartBeatToMaster(): Unit = {
    val activeShuffleKeys = new JHashSet[String]()
    val estimatedAppDiskUsage = new JHashMap[String, JLong]()
    activeShuffleKeys.addAll(partitionLocationInfo.shuffleKeySet)
    activeShuffleKeys.addAll(storageManager.shuffleKeySet())
    storageManager.topAppDiskUsage.asScala.foreach { case (shuffleId, usage) =>
      estimatedAppDiskUsage.put(shuffleId, usage)
    }
    // During shutdown, return an empty diskInfo list to mark this worker as unavailable,
    // and avoid remove this from master's blacklist.
    val diskInfos =
      if (shutdown.get()) {
        Seq.empty[DiskInfo]
      } else {
        storageManager.updateDiskInfos()
        workerInfo.updateThenGetDiskInfos(
          storageManager.disksSnapshot().map { disk => disk.mountPoint -> disk }.toMap.asJava,
          conf.initialEstimatedPartitionSize).values().asScala.toSeq
      }
    val resourceConsumption = workerInfo.updateThenGetUserResourceConsumption(
      storageManager.userResourceConsumptionSnapshot().asJava)

    val response = rssHARetryClient.askSync[HeartbeatResponse](
      HeartbeatFromWorker(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        diskInfos,
        resourceConsumption,
        activeShuffleKeys,
        estimatedAppDiskUsage),
      classOf[HeartbeatResponse])
    if (response.registered) {
      response.expiredShuffleKeys.asScala.foreach(shuffleKey => workerInfo.releaseSlots(shuffleKey))
      cleanTaskQueue.put(response.expiredShuffleKeys)
    } else {
      logError("Worker not registered in master, clean expired shuffle data and register again.")
      // Clean expired shuffle.
      cleanup(response.expiredShuffleKeys)
      try {
        registerWithMaster()
      } catch {
        case e: Throwable =>
          logError("Re-register worker failed after worker lost.", e)
          // Register to master failed then stop server
          System.exit(-1)
      }
    }
  }

  override def initialize(): Unit = {
    super.initialize()
    logInfo(s"Starting Worker $host:$pushPort:$fetchPort:$replicatePort" +
      s" with ${workerInfo.diskInfos} slots.")
    registerWithMaster()

    // start heartbeat
    sendHeartbeatTask = forwardMessageScheduler.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryLogNonFatalError {
          heartBeatToMaster()
        }
      },
      HEARTBEAT_MILLIS,
      HEARTBEAT_MILLIS,
      TimeUnit.MILLISECONDS)

    checkFastfailTask = forwardMessageScheduler.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryLogNonFatalError {
          unavailablePeers.entrySet().asScala.foreach { entry =>
            if (System.currentTimeMillis() - entry.getValue > REPLICATE_FAST_FAIL_DURATION) {
              unavailablePeers.remove(entry.getKey)
            }
          }
        }
      },
      0,
      REPLICATE_FAST_FAIL_DURATION,
      TimeUnit.MILLISECONDS)

    cleaner = new Thread("Cleaner") {
      override def run(): Unit = {
        while (true) {
          val expiredShuffleKeys = cleanTaskQueue.take()
          try {
            cleanup(expiredShuffleKeys)
          } catch {
            case e: Throwable =>
              logError("Cleanup failed", e)
          }
        }
      }
    }

    pushDataHandler.init(this)
    replicateHandler.init(this)
    fetchHandler.init(this)
    controller.init(this)

    cleaner.setDaemon(true)
    cleaner.start()

    logInfo("Worker started.")
    rpcEnv.awaitTermination()
  }

  override def close(): Unit = synchronized {
    if (!stopped) {
      logInfo("Stopping Worker.")

      if (sendHeartbeatTask != null) {
        sendHeartbeatTask.cancel(true)
        sendHeartbeatTask = null
      }
      if (checkFastfailTask != null) {
        checkFastfailTask.cancel(true)
        checkFastfailTask = null
      }
      forwardMessageScheduler.shutdownNow()
      replicateThreadPool.shutdownNow()
      commitThreadPool.shutdownNow()
      asyncReplyPool.shutdownNow()
      partitionsSorter.close()

      if (null != storageManager) {
        storageManager.close()
      }

      rssHARetryClient.close()
      replicateServer.close()
      fetchServer.close()

      super.close()

      logInfo("Worker is stopped.")
      stopped = true
    }
  }

  private def registerWithMaster(): Unit = {
    var registerTimeout = conf.registerWorkerTimeout
    val interval = 2000
    var exception: Throwable = null
    while (registerTimeout > 0) {
      val resp =
        try {
          rssHARetryClient.askSync[PbRegisterWorkerResponse](
            RegisterWorker(
              host,
              rpcPort,
              pushPort,
              fetchPort,
              replicatePort,
              // Use WorkerInfo's diskInfo since re-register when heartbeat return not-registered,
              // StorageManager have update the disk info.
              workerInfo.diskInfos.asScala.toMap,
              workerInfo.updateThenGetUserResourceConsumption(
                storageManager.userResourceConsumptionSnapshot().asJava).asScala.toMap,
              RssHARetryClient.genRequestId()),
            classOf[PbRegisterWorkerResponse])
        } catch {
          case throwable: Throwable =>
            logWarning(
              s"Register worker to master failed, will retry after ${Utils.msDurationToString(interval)}",
              throwable)
            exception = throwable
            null
        }
      // Register successfully
      if (null != resp && resp.getSuccess) {
        registered.set(true)
        logInfo("Register worker successfully.")
        return
      }
      // Register failed, sleep and retry
      Thread.sleep(interval)
      registerTimeout = registerTimeout - interval
    }
    // If worker register still failed after retry, throw exception to stop worker process
    throw new CelebornException("Register worker failed.", exception)
  }
  @VisibleForTesting
  def cleanup(expiredShuffleKeys: JHashSet[String]): Unit = synchronized {
    expiredShuffleKeys.asScala.foreach { shuffleKey =>
      partitionLocationInfo.removeMasterPartitions(shuffleKey)
      partitionLocationInfo.removeSlavePartitions(shuffleKey)
      partitionLocationInfo.removeShuffle(shuffleKey)
      shufflePartitionType.remove(shuffleKey)
      shufflePushDataTimeout.remove(shuffleKey)
      shuffleMapperAttempts.remove(shuffleKey)
      shuffleCommitInfos.remove(shuffleKey)
      workerInfo.releaseSlots(shuffleKey)
      logInfo(s"Cleaned up expired shuffle $shuffleKey")
    }
    partitionsSorter.cleanup(expiredShuffleKeys)
    storageManager.cleanupExpiredShuffleKey(expiredShuffleKeys)
    fetchHandler.cleanupExpiredShuffleKey(expiredShuffleKeys)
  }

  override def getWorkerInfo: String = workerInfo.toString()

  override def getThreadDump: String = Utils.getThreadDump()

  override def getHostnameList: String = throw new UnsupportedOperationException()

  override def getApplicationList: String = throw new UnsupportedOperationException()

  override def getShuffleList: String = {
    storageManager.shuffleKeySet().asScala.mkString("\n")
  }

  override def listTopDiskUseApps: String = {
    val stringBuilder = new StringBuilder()
    storageManager.topAppDiskUsage.asScala.foreach { case (appId, usage) =>
      stringBuilder.append(s"application ${appId} used ${Utils.bytesToString(usage)} ")
    }
    stringBuilder.toString()
  }

  @VisibleForTesting
  def isRegistered(): Boolean = {
    registered.get()
  }

  ShutdownHookManager.get().addShutdownHook(
    new Thread(new Runnable {
      override def run(): Unit = {
        logInfo("Shutdown hook called.")
        shutdown.set(true)
        if (gracefulShutdown) {
          // During graceful shutdown, to avoid allocate slots in this worker,
          // add this worker to master's blacklist. When restart, register worker will
          // make master remove this worker from blacklist.
          rssHARetryClient.send(ReportWorkerUnavailable(List(workerInfo).asJava))
          val interval = conf.checkSlotsFinishedInterval
          val timeout = conf.checkSlotsFinishedTimeoutMs
          var waitTimes = 0

          def waitTime: Long = waitTimes * interval

          while (!partitionLocationInfo.isEmpty && waitTime < timeout) {
            Thread.sleep(interval)
            waitTimes += 1
          }
          if (partitionLocationInfo.isEmpty) {
            logInfo(s"Waiting for all PartitionLocation released cost ${waitTime}ms.")
          } else {
            logWarning(s"Waiting for all PartitionLocation release cost ${waitTime}ms, " +
              s"unreleased PartitionLocation: \n$partitionLocationInfo")
          }
        }
        close()
      }
    }),
    WORKER_SHUTDOWN_PRIORITY)
}

private[deploy] object Worker extends Logging {
  def main(args: Array[String]): Unit = {
    val conf = new CelebornConf
    val workerArgs = new WorkerArguments(args, conf)
    // There are many entries for setting the master address, and we should unify the entries as
    // much as possible. Therefore, if the user manually specifies the address of the Master when
    // starting the Worker, we should set it in the parameters and automatically calculate what the
    // address of the Master should be used in the end.
    workerArgs.master.foreach { master =>
      conf.set(
        MASTER_ENDPOINTS.key,
        RpcAddress.fromRssURL(master).toString.replace("rss://", ""))
    }

    val worker = new Worker(conf, workerArgs)
    try {
      worker.initialize()
    } catch {
      case e: Throwable =>
        logError("Initialize worker failed.", e)
        System.exit(-1)
    }

  }
}
