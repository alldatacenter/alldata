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

import java.io.File
import java.lang.{Long => JLong}
import java.util.{HashMap => JHashMap, HashSet => JHashSet, Map => JMap}
import java.util.concurrent._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicIntegerArray}

import scala.collection.JavaConverters._

import com.google.common.annotations.VisibleForTesting
import io.netty.util.HashedWheelTimer

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.CelebornConf._
import org.apache.celeborn.common.client.MasterClient
import org.apache.celeborn.common.exception.CelebornException
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DiskInfo, WorkerInfo, WorkerPartitionLocationInfo}
import org.apache.celeborn.common.metrics.MetricsSystem
import org.apache.celeborn.common.metrics.source.{JVMCPUSource, JVMSource, SystemMiscSource}
import org.apache.celeborn.common.network.TransportContext
import org.apache.celeborn.common.protocol.{PartitionType, PbRegisterWorkerResponse, PbWorkerLostResponse, RpcNameConstants, TransportModuleConstants}
import org.apache.celeborn.common.protocol.message.ControlMessages._
import org.apache.celeborn.common.quota.ResourceConsumption
import org.apache.celeborn.common.rpc._
import org.apache.celeborn.common.util.{JavaUtils, ShutdownHookManager, ThreadUtils, Utils}
// Can Remove this if celeborn don't support scala211 in future
import org.apache.celeborn.common.util.FunctionConverter._
import org.apache.celeborn.server.common.{HttpService, Service}
import org.apache.celeborn.service.deploy.worker.congestcontrol.CongestionController
import org.apache.celeborn.service.deploy.worker.memory.{ChannelsLimiter, MemoryManager}
import org.apache.celeborn.service.deploy.worker.storage.{PartitionFilesSorter, StorageManager}

private[celeborn] class Worker(
    override val conf: CelebornConf,
    val workerArgs: WorkerArguments)
  extends HttpService with Logging {

  @volatile private var stopped = false

  override def serviceName: String = Service.WORKER

  override val metricsSystem: MetricsSystem =
    MetricsSystem.createMetricsSystem(serviceName, conf, MetricsSystem.SERVLET_PATH)

  val rpcEnv: RpcEnv = RpcEnv.create(
    RpcNameConstants.WORKER_SYS,
    workerArgs.host,
    workerArgs.host,
    workerArgs.port,
    conf,
    Math.min(64, Math.max(4, Runtime.getRuntime.availableProcessors())))

  private val host = rpcEnv.address.host
  private val rpcPort = rpcEnv.address.port
  Utils.checkHost(host)

  private val WORKER_SHUTDOWN_PRIORITY = 100
  val shutdown = new AtomicBoolean(false)
  private val gracefulShutdown = conf.workerGracefulShutdown
  assert(
    !gracefulShutdown || (gracefulShutdown &&
      conf.workerRpcPort > 0 && conf.workerFetchPort > 0 &&
      conf.workerPushPort > 0 && conf.workerReplicatePort > 0),
    "If enable graceful shutdown, the worker should use stable server port.")
  if (gracefulShutdown) {
    try {
      val recoverRoot = new File(conf.workerGracefulShutdownRecoverPath)
      if (!recoverRoot.exists()) {
        logInfo(s"Recover root path ${conf.workerGracefulShutdownRecoverPath} does not exists, create it first.")
        recoverRoot.mkdirs()
      }
    } catch {
      case e: Exception =>
        logError("Check or create recover root path failed: ", e)
        throw e
    }
  }

  val workerSource = new WorkerSource(conf)
  metricsSystem.registerSource(workerSource)
  metricsSystem.registerSource(new JVMSource(conf, MetricsSystem.ROLE_WORKER))
  metricsSystem.registerSource(new JVMCPUSource(conf, MetricsSystem.ROLE_WORKER))
  metricsSystem.registerSource(new SystemMiscSource(conf, MetricsSystem.ROLE_WORKER))

  val storageManager = new StorageManager(conf, workerSource)

  val memoryManager: MemoryManager = MemoryManager.initialize(conf)
  memoryManager.registerMemoryListener(storageManager)

  val partitionsSorter = new PartitionFilesSorter(memoryManager, conf, workerSource)

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
  rpcEnv.setupEndpoint(RpcNameConstants.WORKER_EP, controller)

  val pushDataHandler = new PushDataHandler()
  val (pushServer, pushClientFactory) = {
    val closeIdleConnections = conf.workerCloseIdleConnections
    val numThreads = conf.workerPushIoThreads.getOrElse(storageManager.totalFlusherThread)
    val transportConf =
      Utils.fromCelebornConf(conf, TransportModuleConstants.PUSH_MODULE, numThreads)
    val pushServerLimiter = new ChannelsLimiter(TransportModuleConstants.PUSH_MODULE, conf)
    val transportContext: TransportContext =
      new TransportContext(
        transportConf,
        pushDataHandler,
        closeIdleConnections,
        pushServerLimiter,
        conf.workerPushHeartbeatEnabled,
        workerSource)
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
    val replicateLimiter = new ChannelsLimiter(TransportModuleConstants.REPLICATE_MODULE, conf)
    val transportContext: TransportContext =
      new TransportContext(
        transportConf,
        replicateHandler,
        closeIdleConnections,
        replicateLimiter,
        false,
        workerSource)
    transportContext.createServer(conf.workerReplicatePort)
  }

  var fetchHandler: FetchHandler = _
  private val fetchServer = {
    val closeIdleConnections = conf.workerCloseIdleConnections
    val numThreads = conf.workerFetchIoThreads.getOrElse(storageManager.totalFlusherThread)
    val transportConf =
      Utils.fromCelebornConf(conf, TransportModuleConstants.FETCH_MODULE, numThreads)
    fetchHandler = new FetchHandler(conf, transportConf)
    val transportContext: TransportContext =
      new TransportContext(
        transportConf,
        fetchHandler,
        closeIdleConnections,
        conf.workerFetchHeartbeatEnabled,
        workerSource)
    transportContext.createServer(conf.workerFetchPort)
  }

  private val pushPort = pushServer.getPort
  assert(pushPort > 0, "worker push bind port should be positive")

  private val fetchPort = fetchServer.getPort
  assert(fetchPort > 0, "worker fetch bind port should be positive")

  private val replicatePort = replicateServer.getPort
  assert(replicatePort > 0, "worker replica bind port should be positive")

  storageManager.updateDiskInfos()

  // WorkerInfo's diskInfos is a reference to storageManager.diskInfos
  val diskInfos = JavaUtils.newConcurrentHashMap[String, DiskInfo]()
  storageManager.disksSnapshot().foreach { diskInfo =>
    diskInfos.put(diskInfo.mountPoint, diskInfo)
  }

  // need to ensure storageManager has recovered fileinfos data if enable graceful shutdown before retrieve consumption
  val userResourceConsumption: ConcurrentHashMap[UserIdentifier, ResourceConsumption] =
    JavaUtils.newConcurrentHashMap[UserIdentifier, ResourceConsumption](
      storageManager.userResourceConsumptionSnapshot().asJava)

  val workerInfo =
    new WorkerInfo(
      host,
      rpcPort,
      pushPort,
      fetchPort,
      replicatePort,
      diskInfos,
      userResourceConsumption)

  // whether this Worker registered to Master successfully
  val registered = new AtomicBoolean(false)
  val shuffleMapperAttempts: ConcurrentHashMap[String, AtomicIntegerArray] =
    JavaUtils.newConcurrentHashMap[String, AtomicIntegerArray]()
  val shufflePartitionType: ConcurrentHashMap[String, PartitionType] =
    JavaUtils.newConcurrentHashMap[String, PartitionType]
  var shufflePushDataTimeout: ConcurrentHashMap[String, Long] =
    JavaUtils.newConcurrentHashMap[String, Long]
  val partitionLocationInfo = new WorkerPartitionLocationInfo

  val shuffleCommitInfos: ConcurrentHashMap[String, ConcurrentHashMap[Long, CommitInfo]] =
    JavaUtils.newConcurrentHashMap[String, ConcurrentHashMap[Long, CommitInfo]]()

  private val masterClient = new MasterClient(rpcEnv, conf)

  // (workerInfo -> last connect timeout timestamp)
  val unavailablePeers: ConcurrentHashMap[WorkerInfo, Long] =
    JavaUtils.newConcurrentHashMap[WorkerInfo, Long]()

  // Threads
  private val forwardMessageScheduler: ScheduledExecutorService =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("worker-forward-message-scheduler")
  private var sendHeartbeatTask: ScheduledFuture[_] = _
  private var checkFastFailTask: ScheduledFuture[_] = _

  val replicateThreadPool: ThreadPoolExecutor =
    ThreadUtils.newDaemonCachedThreadPool("worker-replicate-data", conf.workerReplicateThreads)
  val commitThreadPool: ThreadPoolExecutor =
    ThreadUtils.newDaemonCachedThreadPool("Worker-CommitFiles", conf.workerCommitThreads)
  val asyncReplyPool: ScheduledExecutorService =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("async-reply")
  val timer = new HashedWheelTimer()

  // Configs
  private val heartbeatInterval = conf.workerHeartbeatTimeout / 4
  private val replicaFastFailDuration = conf.workerReplicateFastFailDuration

  private val cleanTaskQueue = new LinkedBlockingQueue[JHashSet[String]]
  var cleaner: Thread = _

  workerSource.addGauge(WorkerSource.REGISTERED_SHUFFLE_COUNT) { () =>
    workerInfo.getShuffleKeySet.size
  }
  workerSource.addGauge(WorkerSource.SLOTS_ALLOCATED) { () =>
    workerInfo.allocationsInLastHour()
  }
  workerSource.addGauge(WorkerSource.SORT_MEMORY) { () =>
    memoryManager.getSortMemoryCounter.get()
  }
  workerSource.addGauge(WorkerSource.SORTING_FILES) { () =>
    partitionsSorter.getSortingCount
  }
  workerSource.addGauge(WorkerSource.SORTED_FILES) { () =>
    partitionsSorter.getSortedCount
  }
  workerSource.addGauge(WorkerSource.SORTED_FILE_SIZE) { () =>
    partitionsSorter.getSortedSize
  }
  workerSource.addGauge(WorkerSource.DISK_BUFFER) { () =>
    memoryManager.getDiskBufferCounter.get()
  }
  workerSource.addGauge(WorkerSource.NETTY_MEMORY) { () =>
    memoryManager.getNettyUsedDirectMemory
  }
  workerSource.addGauge(WorkerSource.PAUSE_PUSH_DATA_COUNT) { () =>
    memoryManager.getPausePushDataCounter
  }
  workerSource.addGauge(WorkerSource.PAUSE_PUSH_DATA_AND_REPLICATE_COUNT) { () =>
    memoryManager.getPausePushDataAndReplicateCounter
  }
  workerSource.addGauge(WorkerSource.BUFFER_STREAM_READ_BUFFER) { () =>
    memoryManager.getReadBufferCounter
  }
  workerSource.addGauge(WorkerSource.READ_BUFFER_DISPATCHER_REQUESTS_LENGTH) { () =>
    memoryManager.dispatchRequestsLength
  }
  workerSource.addGauge(WorkerSource.READ_BUFFER_ALLOCATED_COUNT) { () =>
    memoryManager.getAllocatedReadBuffers
  }

  private def heartbeatToMaster(): Unit = {
    val activeShuffleKeys = new JHashSet[String]()
    val estimatedAppDiskUsage = new JHashMap[String, JLong]()
    activeShuffleKeys.addAll(partitionLocationInfo.shuffleKeySet)
    activeShuffleKeys.addAll(storageManager.shuffleKeySet())
    storageManager.topAppDiskUsage.asScala.foreach { case (shuffleId, usage) =>
      estimatedAppDiskUsage.put(shuffleId, usage)
    }
    storageManager.updateDiskInfos()
    val diskInfos =
      workerInfo.updateThenGetDiskInfos(storageManager.disksSnapshot().map { disk =>
        disk.mountPoint -> disk
      }.toMap.asJava).values().asScala.toSeq
    val resourceConsumption = workerInfo.updateThenGetUserResourceConsumption(
      storageManager.userResourceConsumptionSnapshot().asJava)

    val response = masterClient.askSync[HeartbeatFromWorkerResponse](
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
      classOf[HeartbeatFromWorkerResponse])
    response.expiredShuffleKeys.asScala.foreach(shuffleKey => workerInfo.releaseSlots(shuffleKey))
    cleanTaskQueue.put(response.expiredShuffleKeys)
    if (!response.registered) {
      logError("Worker not registered in master, clean expired shuffle data and register again.")
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
        override def run(): Unit = Utils.tryLogNonFatalError { heartbeatToMaster() }
      },
      heartbeatInterval,
      heartbeatInterval,
      TimeUnit.MILLISECONDS)

    checkFastFailTask = forwardMessageScheduler.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryLogNonFatalError {
          unavailablePeers.entrySet().forEach { entry: JMap.Entry[WorkerInfo, Long] =>
            if (System.currentTimeMillis() - entry.getValue > replicaFastFailDuration) {
              unavailablePeers.remove(entry.getKey)
            }
          }
        }
      },
      0,
      replicaFastFailDuration,
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
    shutdown(gracefulShutdown)
  }

  override def shutdown(graceful: Boolean): Unit = {
    if (!stopped) {
      logInfo("Stopping Worker.")

      if (sendHeartbeatTask != null) {
        if (graceful) {
          sendHeartbeatTask.cancel(false)
        } else {
          sendHeartbeatTask.cancel(true)
        }
        sendHeartbeatTask = null
      }
      if (checkFastFailTask != null) {
        if (graceful) {
          checkFastFailTask.cancel(false)
        } else {
          checkFastFailTask.cancel(true)
        }
        checkFastFailTask = null
      }
      if (graceful) {
        forwardMessageScheduler.shutdown()
        replicateThreadPool.shutdown()
        commitThreadPool.shutdown()
        asyncReplyPool.shutdown()
        partitionsSorter.close()
      } else {
        forwardMessageScheduler.shutdownNow()
        replicateThreadPool.shutdownNow()
        commitThreadPool.shutdownNow()
        asyncReplyPool.shutdownNow()
        partitionsSorter.close()
      }

      if (null != storageManager) {
        storageManager.close()
      }
      memoryManager.close()

      masterClient.close()
      replicateServer.shutdown(graceful)
      fetchServer.shutdown(graceful)
      pushServer.shutdown(graceful)

      super.shutdown(graceful)

      logInfo("Worker is stopped.")
      stopped = true
    }
    if (!graceful) {
      shutdown.set(true)
    }
  }

  private def registerWithMaster(): Unit = {
    var registerTimeout = conf.registerWorkerTimeout
    val interval = 2000
    var exception: Throwable = null
    while (registerTimeout > 0) {
      val resp =
        try {
          masterClient.askSync[PbRegisterWorkerResponse](
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
              MasterClient.genRequestId()),
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

  override def getWorkerInfo: String = {
    val sb = new StringBuilder
    sb.append("====================== WorkerInfo of Worker ===========================\n")
    sb.append(workerInfo.toString()).append("\n")
    sb.toString()
  }

  override def getThreadDump: String = {
    val sb = new StringBuilder
    sb.append("========================= Worker ThreadDump ==========================\n")
    sb.append(Utils.getThreadDump()).append("\n")
    sb.toString()
  }

  override def getShuffleList: String = {
    val sb = new StringBuilder
    sb.append("======================= Shuffle Key List ============================\n")
    storageManager.shuffleKeySet().asScala.foreach { shuffleKey =>
      sb.append(s"$shuffleKey\n")
    }
    sb.toString()
  }

  override def isShutdown: String = {
    val sb = new StringBuilder
    sb.append("========================= Worker Shutdown ==========================\n")
    sb.append(shutdown.get()).append("\n")
    sb.toString()
  }

  override def isRegistered: String = {
    val sb = new StringBuilder
    sb.append("========================= Worker Registered ==========================\n")
    sb.append(registered.get()).append("\n")
    sb.toString()
  }

  override def listTopDiskUseApps: String = {
    val sb = new StringBuilder
    sb.append("================== Top Disk Usage Applications =======================\n")
    storageManager.topAppDiskUsage.asScala.foreach { case (appId, usage) =>
      sb.append(s"Application $appId used ${Utils.bytesToString(usage)}\n")
    }
    sb.toString()
  }

  override def listPartitionLocationInfo: String = {
    val sb = new StringBuilder
    sb.append("==================== Partition Location Info =========================\n")
    sb.append(partitionLocationInfo.toString).append("\n")
    sb.toString()
  }

  override def getUnavailablePeers: String = {
    val sb = new StringBuilder
    sb.append("==================== Unavailable Peers of Worker =====================\n")
    unavailablePeers.asScala.foreach { case (peer, time) =>
      sb.append(s"${peer.toUniqueId().padTo(50, " ").mkString}${dateFmt.format(time)}\n")
    }
    sb.toString()
  }

  ShutdownHookManager.get().addShutdownHook(
    new Thread(new Runnable {
      override def run(): Unit = {
        logInfo("Shutdown hook called.")
        // During shutdown, to avoid allocate slots in this worker,
        // add this worker to master's excluded list. When restart, register worker will
        // make master remove this worker from excluded list.
        try {
          if (gracefulShutdown) {
            masterClient.askSync(
              ReportWorkerUnavailable(List(workerInfo).asJava),
              OneWayMessageResponse.getClass)
          } else {
            masterClient.askSync[PbWorkerLostResponse](
              WorkerLost(
                host,
                rpcPort,
                pushPort,
                fetchPort,
                replicatePort,
                MasterClient.genRequestId()),
              classOf[PbWorkerLostResponse])
          }
        } catch {
          case e: Throwable =>
            logError(
              s"Fail report to master, need wait PartitionLocation auto release: \n$partitionLocationInfo",
              e)
        }
        shutdown.set(true)
        if (gracefulShutdown) {
          val interval = conf.workerGracefulShutdownCheckSlotsFinishedInterval
          val timeout = conf.workerGracefulShutdownCheckSlotsFinishedTimeoutMs
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

  @VisibleForTesting
  def getPushFetchServerPort: (Int, Int) = (pushPort, fetchPort)
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
      conf.set(MASTER_ENDPOINTS.key, RpcAddress.fromCelebornURL(master).hostPort)
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
