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

package org.apache.celeborn.service.deploy.master

import java.io.IOException
import java.net.BindException
import java.util
import java.util.concurrent.{ConcurrentHashMap, ScheduledFuture, TimeUnit}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

import org.apache.hadoop.fs.{FileSystem, Path}

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.client.MasterClient
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DiskInfo, WorkerInfo}
import org.apache.celeborn.common.metrics.MetricsSystem
import org.apache.celeborn.common.metrics.source.{JVMCPUSource, JVMSource, ResourceConsumptionSource, SystemMiscSource}
import org.apache.celeborn.common.protocol._
import org.apache.celeborn.common.protocol.message.{ControlMessages, StatusCode}
import org.apache.celeborn.common.protocol.message.ControlMessages._
import org.apache.celeborn.common.quota.{QuotaManager, ResourceConsumption}
import org.apache.celeborn.common.rpc._
import org.apache.celeborn.common.util.{CelebornHadoopUtils, JavaUtils, PbSerDeUtils, ThreadUtils, Utils}
import org.apache.celeborn.server.common.{HttpService, Service}
import org.apache.celeborn.service.deploy.master.clustermeta.SingleMasterMetaManager
import org.apache.celeborn.service.deploy.master.clustermeta.ha.{HAHelper, HAMasterMetaManager, MetaHandler}

private[celeborn] class Master(
    override val conf: CelebornConf,
    val masterArgs: MasterArguments)
  extends HttpService with RpcEndpoint with Logging {

  @volatile private var stopped = false

  override def serviceName: String = Service.MASTER

  override val metricsSystem: MetricsSystem =
    MetricsSystem.createMetricsSystem(serviceName, conf, MetricsSystem.SERVLET_PATH)

  override val rpcEnv: RpcEnv = RpcEnv.create(
    RpcNameConstants.MASTER_SYS,
    masterArgs.host,
    masterArgs.host,
    masterArgs.port,
    conf,
    Math.max(64, Runtime.getRuntime.availableProcessors()))

  private val statusSystem =
    if (conf.haEnabled) {
      val sys = new HAMasterMetaManager(rpcEnv, conf)
      val handler = new MetaHandler(sys)
      try {
        handler.setUpMasterRatisServer(conf, masterArgs.masterClusterInfo.get)
      } catch {
        case ioe: IOException =>
          if (ioe.getCause.isInstanceOf[BindException]) {
            val msg = s"HA port ${sys.getRatisServer.getRaftPort} of Ratis Server is occupied, " +
              s"Master process will stop. Please refer to configuration doc to modify the HA port " +
              s"in config file for each node."
            logError(msg, ioe)
            System.exit(1)
          } else {
            logError("Face unexpected IO exception during staring Ratis server", ioe)
          }
      }
      sys
    } else {
      new SingleMasterMetaManager(rpcEnv, conf)
    }

  // Threads
  private val forwardMessageThread =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("master-forward-message-thread")
  private var checkForWorkerTimeOutTask: ScheduledFuture[_] = _
  private var checkForApplicationTimeOutTask: ScheduledFuture[_] = _
  private var checkForHDFSRemnantDirsTimeOutTask: ScheduledFuture[_] = _
  private val nonEagerHandler = ThreadUtils.newDaemonCachedThreadPool("master-noneager-handler", 64)

  // Config constants
  private val workerHeartbeatTimeoutMs = conf.workerHeartbeatTimeout
  private val appHeartbeatTimeoutMs = conf.appHeartbeatTimeoutMs
  private val hdfsExpireDirsTimeoutMS = conf.hdfsExpireDirsTimeoutMS
  private val hasHDFSStorage = conf.hasHDFSStorage

  private val quotaManager = QuotaManager.instantiate(conf)
  private val masterResourceConsumptionInterval = conf.masterResourceConsumptionInterval
  private val userResourceConsumptions =
    JavaUtils.newConcurrentHashMap[UserIdentifier, (ResourceConsumption, Long)]()

  // States
  private def workersSnapShot: util.List[WorkerInfo] =
    statusSystem.workers.synchronized(new util.ArrayList[WorkerInfo](statusSystem.workers))
  private def lostWorkersSnapshot: ConcurrentHashMap[WorkerInfo, java.lang.Long] =
    statusSystem.workers.synchronized(JavaUtils.newConcurrentHashMap(statusSystem.lostWorkers))
  private def shutdownWorkerSnapshot: util.List[WorkerInfo] =
    statusSystem.workers.synchronized(new util.ArrayList[WorkerInfo](statusSystem.shutdownWorkers))

  private def diskReserveSize = conf.workerDiskReserveSize

  private val slotsAssignLoadAwareDiskGroupNum = conf.masterSlotAssignLoadAwareDiskGroupNum
  private val slotsAssignLoadAwareDiskGroupGradient =
    conf.masterSlotAssignLoadAwareDiskGroupGradient
  private val loadAwareFlushTimeWeight = conf.masterSlotAssignLoadAwareFlushTimeWeight
  private val loadAwareFetchTimeWeight = conf.masterSlotAssignLoadAwareFetchTimeWeight

  private val estimatedPartitionSizeUpdaterInitialDelay =
    conf.estimatedPartitionSizeUpdaterInitialDelay
  private val estimatedPartitionSizeForEstimationUpdateInterval =
    conf.estimatedPartitionSizeForEstimationUpdateInterval
  private val partitionSizeUpdateService =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("partition-size-updater")
  partitionSizeUpdateService.scheduleAtFixedRate(
    new Runnable {
      override def run(): Unit = {
        executeWithLeaderChecker(
          null, {
            statusSystem.handleUpdatePartitionSize()
            logInfo(s"Cluster estimate partition size ${Utils.bytesToString(statusSystem.estimatedPartitionSize)}")
          })
      }
    },
    estimatedPartitionSizeUpdaterInitialDelay,
    estimatedPartitionSizeForEstimationUpdateInterval,
    TimeUnit.MILLISECONDS)
  private val slotsAssignPolicy = conf.masterSlotAssignPolicy

  // init and register master metrics
  val resourceConsumptionSource = new ResourceConsumptionSource(conf)
  private val masterSource = new MasterSource(conf)
  private var hadoopFs: FileSystem = _
  masterSource.addGauge(MasterSource.REGISTERED_SHUFFLE_COUNT) { () =>
    statusSystem.registeredShuffle.size
  }
  masterSource.addGauge(MasterSource.EXCLUDED_WORKER_COUNT) { () =>
    statusSystem.excludedWorkers.size
  }
  masterSource.addGauge(MasterSource.WORKER_COUNT) { () => statusSystem.workers.size }
  masterSource.addGauge(MasterSource.LOST_WORKER_COUNT) { () => statusSystem.lostWorkers.size }
  masterSource.addGauge(MasterSource.PARTITION_SIZE) { () => statusSystem.estimatedPartitionSize }
  masterSource.addGauge(MasterSource.IS_ACTIVE_MASTER) { () => isMasterActive }

  metricsSystem.registerSource(resourceConsumptionSource)
  metricsSystem.registerSource(masterSource)
  metricsSystem.registerSource(new JVMSource(conf, MetricsSystem.ROLE_MASTER))
  metricsSystem.registerSource(new JVMCPUSource(conf, MetricsSystem.ROLE_MASTER))
  metricsSystem.registerSource(new SystemMiscSource(conf, MetricsSystem.ROLE_MASTER))

  rpcEnv.setupEndpoint(RpcNameConstants.MASTER_EP, this)

  // start threads to check timeout for workers and applications
  override def onStart(): Unit = {
    checkForWorkerTimeOutTask = forwardMessageThread.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryLogNonFatalError {
          self.send(ControlMessages.pbCheckForWorkerTimeout)
        }
      },
      0,
      workerHeartbeatTimeoutMs,
      TimeUnit.MILLISECONDS)

    checkForApplicationTimeOutTask = forwardMessageThread.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryLogNonFatalError {
          self.send(CheckForApplicationTimeOut)
        }
      },
      0,
      appHeartbeatTimeoutMs / 2,
      TimeUnit.MILLISECONDS)

    if (hasHDFSStorage) {
      checkForHDFSRemnantDirsTimeOutTask = forwardMessageThread.scheduleAtFixedRate(
        new Runnable {
          override def run(): Unit = Utils.tryLogNonFatalError {
            self.send(CheckForHDFSExpiredDirsTimeout)
          }
        },
        hdfsExpireDirsTimeoutMS,
        hdfsExpireDirsTimeoutMS,
        TimeUnit.MILLISECONDS)
    }

  }

  override def onStop(): Unit = {
    logInfo("Stopping Celeborn Master.")
    if (checkForWorkerTimeOutTask != null) {
      checkForWorkerTimeOutTask.cancel(true)
    }
    if (checkForApplicationTimeOutTask != null) {
      checkForApplicationTimeOutTask.cancel(true)
    }
    if (checkForHDFSRemnantDirsTimeOutTask != null) {
      checkForHDFSRemnantDirsTimeOutTask.cancel(true)
    }
    forwardMessageThread.shutdownNow()
    logInfo("Celeborn Master is stopped.")
  }

  override def onDisconnected(address: RpcAddress): Unit = {
    // The disconnected client could've been either a worker or an app; remove whichever it was
    logDebug(s"Client $address got disassociated.")
  }

  def executeWithLeaderChecker[T](context: RpcCallContext, f: => T): Unit =
    if (HAHelper.checkShouldProcess(context, statusSystem)) {
      try {
        f
      } catch {
        case e: Exception =>
          HAHelper.sendFailure(context, HAHelper.getRatisServer(statusSystem), e)
      }
    }

  override def receive: PartialFunction[Any, Unit] = {
    case _: PbCheckForWorkerTimeout =>
      executeWithLeaderChecker(null, timeoutDeadWorkers())
    case CheckForApplicationTimeOut =>
      executeWithLeaderChecker(null, timeoutDeadApplications())
    case CheckForHDFSExpiredDirsTimeout =>
      executeWithLeaderChecker(null, checkAndCleanExpiredAppDirsOnHDFS())
    case pb: PbWorkerLost =>
      val host = pb.getHost
      val rpcPort = pb.getRpcPort
      val pushPort = pb.getPushPort
      val fetchPort = pb.getFetchPort
      val replicatePort = pb.getReplicatePort
      val requestId = pb.getRequestId
      logDebug(s"Received worker lost $host:$rpcPort:$pushPort:$fetchPort:$replicatePort.")
      executeWithLeaderChecker(
        null,
        handleWorkerLost(null, host, rpcPort, pushPort, fetchPort, replicatePort, requestId))
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case HeartbeatFromApplication(
          appId,
          totalWritten,
          fileCount,
          needCheckedWorkerList,
          requestId,
          shouldResponse) =>
      logDebug(s"Received heartbeat from app $appId")
      executeWithLeaderChecker(
        context,
        handleHeartbeatFromApplication(
          context,
          appId,
          totalWritten,
          fileCount,
          needCheckedWorkerList,
          requestId,
          shouldResponse))

    case pbRegisterWorker: PbRegisterWorker =>
      val requestId = pbRegisterWorker.getRequestId
      val host = pbRegisterWorker.getHost
      val rpcPort = pbRegisterWorker.getRpcPort
      val pushPort = pbRegisterWorker.getPushPort
      val fetchPort = pbRegisterWorker.getFetchPort
      val replicatePort = pbRegisterWorker.getReplicatePort
      val disks = pbRegisterWorker.getDisksList.asScala
        .map { pbDiskInfo => pbDiskInfo.getMountPoint -> PbSerDeUtils.fromPbDiskInfo(pbDiskInfo) }
        .toMap.asJava
      val userResourceConsumption =
        PbSerDeUtils.fromPbUserResourceConsumption(pbRegisterWorker.getUserResourceConsumptionMap)

      logDebug(s"Received RegisterWorker request $requestId, $host:$pushPort:$replicatePort" +
        s" $disks.")
      executeWithLeaderChecker(
        context,
        handleRegisterWorker(
          context,
          host,
          rpcPort,
          pushPort,
          fetchPort,
          replicatePort,
          disks,
          userResourceConsumption,
          requestId))

    case requestSlots @ RequestSlots(_, _, _, _, _, _, _, _) =>
      logTrace(s"Received RequestSlots request $requestSlots.")
      executeWithLeaderChecker(context, handleRequestSlots(context, requestSlots))

    case ReleaseSlots(applicationId, shuffleId, workerIds, slots, requestId) =>
      logTrace(s"Received ReleaseSlots request $requestId, $applicationId, $shuffleId," +
        s"workers ${workerIds.asScala.mkString(",")}, slots ${slots.asScala.mkString(",")}")
      executeWithLeaderChecker(
        context,
        handleReleaseSlots(context, applicationId, shuffleId, workerIds, slots, requestId))

    case pb: PbUnregisterShuffle =>
      val applicationId = pb.getAppId
      val shuffleId = pb.getShuffleId
      val requestId = pb.getRequestId
      logDebug(s"Received UnregisterShuffle request $requestId, $applicationId, $shuffleId")
      executeWithLeaderChecker(
        context,
        handleUnregisterShuffle(context, applicationId, shuffleId, requestId))

    case msg: GetBlacklist =>
      executeWithLeaderChecker(context, handleGetBlacklist(context, msg))

    case ApplicationLost(appId, requestId) =>
      logDebug(s"Received ApplicationLost request $requestId, $appId.")
      executeWithLeaderChecker(context, handleApplicationLost(context, appId, requestId))

    case HeartbeatFromWorker(
          host,
          rpcPort,
          pushPort,
          fetchPort,
          replicatePort,
          disks,
          userResourceConsumption,
          activeShuffleKey,
          estimatedAppDiskUsage,
          requestId) =>
      logDebug(s"Received heartbeat from" +
        s" worker $host:$rpcPort:$pushPort:$fetchPort:$replicatePort with $disks.")
      executeWithLeaderChecker(
        context,
        handleHeartbeatFromWorker(
          context,
          host,
          rpcPort,
          pushPort,
          fetchPort,
          replicatePort,
          disks,
          userResourceConsumption,
          activeShuffleKey,
          estimatedAppDiskUsage,
          requestId))

    case ReportWorkerUnavailable(failedWorkers: util.List[WorkerInfo], requestId: String) =>
      executeWithLeaderChecker(
        context,
        handleReportNodeUnavailable(context, failedWorkers, requestId))

    case pb: PbWorkerLost =>
      val host = pb.getHost
      val rpcPort = pb.getRpcPort
      val pushPort = pb.getPushPort
      val fetchPort = pb.getFetchPort
      val replicatePort = pb.getReplicatePort
      val requestId = pb.getRequestId
      logInfo(s"Received worker lost $host:$rpcPort:$pushPort:$fetchPort:$replicatePort.")
      executeWithLeaderChecker(
        context,
        handleWorkerLost(context, host, rpcPort, pushPort, fetchPort, replicatePort, requestId))

    case CheckQuota(userIdentifier) =>
      executeWithLeaderChecker(context, handleCheckQuota(userIdentifier, context))
  }

  private def timeoutDeadWorkers() {
    val currentTime = System.currentTimeMillis()
    // Need increase timeout deadline to avoid long time leader election period
    if (HAHelper.getWorkerTimeoutDeadline(statusSystem) > currentTime) {
      return
    }

    var ind = 0
    workersSnapShot.asScala.foreach { worker =>
      if (worker.lastHeartbeat < currentTime - workerHeartbeatTimeoutMs
        && !statusSystem.workerLostEvents.contains(worker)) {
        logWarning(s"Worker ${worker.readableAddress()} timeout! Trigger WorkerLost event.")
        // trigger WorkerLost event
        self.send(WorkerLost(
          worker.host,
          worker.rpcPort,
          worker.pushPort,
          worker.fetchPort,
          worker.replicatePort,
          MasterClient.genRequestId()))
      }
      ind += 1
    }
  }

  private def timeoutDeadApplications(): Unit = {
    val currentTime = System.currentTimeMillis()
    // Need increase timeout deadline to avoid long time leader election period
    if (HAHelper.getAppTimeoutDeadline(statusSystem) > currentTime) {
      return
    }
    statusSystem.appHeartbeatTime.keySet().asScala.foreach { key =>
      if (statusSystem.appHeartbeatTime.get(key) < currentTime - appHeartbeatTimeoutMs) {
        logWarning(s"Application $key timeout, trigger applicationLost event.")
        val requestId = MasterClient.genRequestId()
        var res = self.askSync[ApplicationLostResponse](ApplicationLost(key, requestId))
        var retry = 1
        while (res.status != StatusCode.SUCCESS && retry <= 3) {
          res = self.askSync[ApplicationLostResponse](ApplicationLost(key, requestId))
          retry += 1
        }
        if (retry > 3) {
          logWarning(s"Handle ApplicationLost event for $key failed more than 3 times!")
        }
      }
    }
  }

  private def handleHeartbeatFromWorker(
      context: RpcCallContext,
      host: String,
      rpcPort: Int,
      pushPort: Int,
      fetchPort: Int,
      replicatePort: Int,
      disks: Seq[DiskInfo],
      userResourceConsumption: util.Map[UserIdentifier, ResourceConsumption],
      activeShuffleKeys: util.Set[String],
      estimatedAppDiskUsage: util.HashMap[String, java.lang.Long],
      requestId: String): Unit = {
    val targetWorker = new WorkerInfo(host, rpcPort, pushPort, fetchPort, replicatePort)
    val registered = workersSnapShot.asScala.contains(targetWorker)
    if (!registered) {
      logWarning(s"Received heartbeat from unknown worker " +
        s"$host:$rpcPort:$pushPort:$fetchPort:$replicatePort.")
    } else {
      statusSystem.handleWorkerHeartbeat(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        disks.map { disk => disk.mountPoint -> disk }.toMap.asJava,
        userResourceConsumption,
        estimatedAppDiskUsage,
        System.currentTimeMillis(),
        requestId)
    }

    val expiredShuffleKeys = new util.HashSet[String]
    activeShuffleKeys.asScala.foreach { shuffleKey =>
      if (!statusSystem.registeredShuffle.contains(shuffleKey)) {
        logWarning(
          s"Shuffle $shuffleKey expired on $host:$rpcPort:$pushPort:$fetchPort:$replicatePort.")
        expiredShuffleKeys.add(shuffleKey)
      }
    }
    context.reply(HeartbeatFromWorkerResponse(expiredShuffleKeys, registered))
  }

  private def handleWorkerLost(
      context: RpcCallContext,
      host: String,
      rpcPort: Int,
      pushPort: Int,
      fetchPort: Int,
      replicatePort: Int,
      requestId: String): Unit = {
    val targetWorker = new WorkerInfo(
      host,
      rpcPort,
      pushPort,
      fetchPort,
      replicatePort,
      new util.HashMap[String, DiskInfo](),
      JavaUtils.newConcurrentHashMap[UserIdentifier, ResourceConsumption]())
    val worker: WorkerInfo = workersSnapShot
      .asScala
      .find(_ == targetWorker)
      .orNull
    if (worker == null) {
      logWarning(s"Unknown worker $host:$rpcPort:$pushPort:$fetchPort:$replicatePort" +
        s" for WorkerLost handler!")
    } else {
      statusSystem.handleWorkerLost(host, rpcPort, pushPort, fetchPort, replicatePort, requestId)
    }
    if (context != null) {
      context.reply(WorkerLostResponse(true))
    }
  }

  def handleRegisterWorker(
      context: RpcCallContext,
      host: String,
      rpcPort: Int,
      pushPort: Int,
      fetchPort: Int,
      replicatePort: Int,
      disks: util.Map[String, DiskInfo],
      userResourceConsumption: util.Map[UserIdentifier, ResourceConsumption],
      requestId: String): Unit = {
    val workerToRegister =
      new WorkerInfo(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        disks,
        userResourceConsumption)
    if (workersSnapShot.contains(workerToRegister)) {
      logWarning(s"Receive RegisterWorker while worker" +
        s" ${workerToRegister.toString()} already exists, re-register.")
      statusSystem.handleWorkerRemove(host, rpcPort, pushPort, fetchPort, replicatePort, requestId)
      statusSystem.handleRegisterWorker(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        disks,
        userResourceConsumption,
        requestId)
      context.reply(RegisterWorkerResponse(true, "Worker in snapshot, re-register."))
    } else if (statusSystem.workerLostEvents.contains(workerToRegister)) {
      logWarning(s"Receive RegisterWorker while worker $workerToRegister " +
        s"in workerLostEvents.")
      statusSystem.workerLostEvents.remove(workerToRegister)
      statusSystem.handleRegisterWorker(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        disks,
        userResourceConsumption,
        requestId)
      context.reply(RegisterWorkerResponse(true, "Worker in workerLostEvents, re-register."))
    } else {
      statusSystem.handleRegisterWorker(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        disks,
        userResourceConsumption,
        requestId)
      logInfo(s"Registered worker $workerToRegister.")
      context.reply(RegisterWorkerResponse(true, ""))
    }
  }

  def handleRequestSlots(context: RpcCallContext, requestSlots: RequestSlots): Unit = {
    val numReducers = requestSlots.partitionIdList.size()
    val shuffleKey = Utils.makeShuffleKey(requestSlots.applicationId, requestSlots.shuffleId)

    val availableWorkers = workersAvailable()
    // offer slots
    val slots =
      masterSource.sample(MasterSource.OFFER_SLOTS_TIME, s"offerSlots-${Random.nextInt()}") {
        statusSystem.workers.synchronized {
          if (slotsAssignPolicy == SlotsAssignPolicy.LOADAWARE && !hasHDFSStorage) {
            SlotsAllocator.offerSlotsLoadAware(
              availableWorkers,
              requestSlots.partitionIdList,
              requestSlots.shouldReplicate,
              requestSlots.shouldRackAware,
              diskReserveSize,
              slotsAssignLoadAwareDiskGroupNum,
              slotsAssignLoadAwareDiskGroupGradient,
              loadAwareFlushTimeWeight,
              loadAwareFetchTimeWeight)
          } else {
            SlotsAllocator.offerSlotsRoundRobin(
              availableWorkers,
              requestSlots.partitionIdList,
              requestSlots.shouldReplicate,
              requestSlots.shouldRackAware)
          }
        }
      }

    if (log.isDebugEnabled()) {
      val distributions = SlotsAllocator.slotsToDiskAllocations(slots)
      logDebug(s"allocate slots for shuffle $shuffleKey $slots" +
        s" distributions: ${distributions.asScala.map(m => m._1.toUniqueId() -> m._2)}")
    }

    // reply false if offer slots failed
    if (slots == null || slots.isEmpty) {
      logError(s"Offer slots for $numReducers reducers of $shuffleKey failed!")
      context.reply(RequestSlotsResponse(StatusCode.SLOT_NOT_AVAILABLE, new WorkerResource()))
      return
    }

    // register shuffle success, update status
    statusSystem.handleRequestSlots(
      shuffleKey,
      requestSlots.hostname,
      Utils.getSlotsPerDisk(slots.asInstanceOf[WorkerResource])
        .asScala.map { case (worker, slots) => worker.toUniqueId() -> slots }.asJava,
      requestSlots.requestId)

    logInfo(s"Offer slots successfully for $numReducers reducers of $shuffleKey" +
      s" on ${slots.size()} workers.")

    val workersNotSelected = availableWorkers.asScala.filter(!slots.containsKey(_))
    val offerSlotsExtraSize = Math.min(conf.masterSlotAssignExtraSlots, workersNotSelected.size)
    if (offerSlotsExtraSize > 0) {
      var index = Random.nextInt(workersNotSelected.size)
      (1 to offerSlotsExtraSize).foreach(_ => {
        slots.put(
          workersNotSelected(index),
          (new util.ArrayList[PartitionLocation](), new util.ArrayList[PartitionLocation]()))
        index = (index + 1) % workersNotSelected.size
      })
      logInfo(s"Offered extra $offerSlotsExtraSize slots for $shuffleKey")
    }

    context.reply(RequestSlotsResponse(StatusCode.SUCCESS, slots.asInstanceOf[WorkerResource]))
  }

  def handleReleaseSlots(
      context: RpcCallContext,
      applicationId: String,
      shuffleId: Int,
      workerIds: util.List[String],
      slots: util.List[util.Map[String, Integer]],
      requestId: String): Unit = {
    // For compatibility, ignore this message
    context.reply(ReleaseSlotsResponse(StatusCode.SUCCESS))
  }

  def handleUnregisterShuffle(
      context: RpcCallContext,
      applicationId: String,
      shuffleId: Int,
      requestId: String): Unit = {
    val shuffleKey = Utils.makeShuffleKey(applicationId, shuffleId)
    statusSystem.handleUnRegisterShuffle(shuffleKey, requestId)
    logInfo(s"Unregister shuffle $shuffleKey")
    context.reply(UnregisterShuffleResponse(StatusCode.SUCCESS))
  }

  def handleGetBlacklist(context: RpcCallContext, msg: GetBlacklist): Unit = {
    msg.localExcludedWorkers.removeAll(workersSnapShot)
    context.reply(
      GetBlacklistResponse(
        StatusCode.SUCCESS,
        new util.ArrayList(statusSystem.excludedWorkers),
        msg.localExcludedWorkers))
  }

  private def handleReportNodeUnavailable(
      context: RpcCallContext,
      failedWorkers: util.List[WorkerInfo],
      requestId: String): Unit = {
    logInfo(s"Receive ReportNodeFailure $failedWorkers, current excluded workers" +
      s"${statusSystem.excludedWorkers}")
    statusSystem.handleReportWorkerUnavailable(failedWorkers, requestId)
    context.reply(OneWayMessageResponse)
  }

  def handleApplicationLost(context: RpcCallContext, appId: String, requestId: String): Unit = {
    nonEagerHandler.submit(new Runnable {
      override def run(): Unit = {
        statusSystem.handleAppLost(appId, requestId)
        logInfo(s"Removed application $appId")
        if (hasHDFSStorage) {
          checkAndCleanExpiredAppDirsOnHDFS(appId)
        }
        context.reply(ApplicationLostResponse(StatusCode.SUCCESS))
      }
    })
  }

  private def checkAndCleanExpiredAppDirsOnHDFS(expiredDir: String = ""): Unit = {
    if (hadoopFs == null) {
      hadoopFs = CelebornHadoopUtils.getHadoopFS(conf)
    }
    val hdfsWorkPath = new Path(conf.hdfsDir, conf.workerWorkingDir)
    if (hadoopFs.exists(hdfsWorkPath)) {
      if (!expiredDir.isEmpty) {
        val dirToDelete = new Path(hdfsWorkPath, expiredDir)
        // delete specific app dir on application lost
        CelebornHadoopUtils.deleteHDFSPathOrLogError(hadoopFs, dirToDelete, true)
      } else {
        val iter = hadoopFs.listStatusIterator(hdfsWorkPath)
        while (iter.hasNext && isMasterActive == 1) {
          val fileStatus = iter.next()
          if (!statusSystem.appHeartbeatTime.containsKey(fileStatus.getPath.getName)) {
            CelebornHadoopUtils.deleteHDFSPathOrLogError(hadoopFs, fileStatus.getPath, true)
          }
        }
      }
    }
  }

  private def handleHeartbeatFromApplication(
      context: RpcCallContext,
      appId: String,
      totalWritten: Long,
      fileCount: Long,
      needCheckedWorkerList: util.List[WorkerInfo],
      requestId: String,
      shouldResponse: Boolean): Unit = {
    statusSystem.handleAppHeartbeat(
      appId,
      totalWritten,
      fileCount,
      System.currentTimeMillis(),
      requestId)
    // unknown workers will retain in needCheckedWorkerList
    needCheckedWorkerList.removeAll(workersSnapShot)
    if (shouldResponse) {
      context.reply(HeartbeatFromApplicationResponse(
        StatusCode.SUCCESS,
        new util.ArrayList(statusSystem.excludedWorkers),
        needCheckedWorkerList,
        shutdownWorkerSnapshot))
    } else {
      context.reply(OneWayMessageResponse)
    }
  }

  private def computeUserResourceConsumption(userIdentifier: UserIdentifier)
      : ResourceConsumption = {
    val current = System.currentTimeMillis()
    if (userResourceConsumptions.containsKey(userIdentifier)) {
      val resourceConsumptionAndUpdateTime = userResourceConsumptions.get(userIdentifier)
      if (current - resourceConsumptionAndUpdateTime._2 > masterResourceConsumptionInterval) {
        val newResourceConsumption = statusSystem.workers.asScala.flatMap { workerInfo =>
          workerInfo.userResourceConsumption.asScala.get(userIdentifier)
        }.foldRight(ResourceConsumption(0, 0, 0, 0))(_ add _)
        userResourceConsumptions.put(userIdentifier, (newResourceConsumption, current))
        newResourceConsumption
      } else {
        resourceConsumptionAndUpdateTime._1
      }
    } else {
      val newResourceConsumption = statusSystem.workers.asScala.flatMap { workerInfo =>
        workerInfo.userResourceConsumption.asScala.get(userIdentifier)
      }.foldRight(ResourceConsumption(0, 0, 0, 0))(_ add _)
      userResourceConsumptions.put(userIdentifier, (newResourceConsumption, current))
      newResourceConsumption
    }
  }

  private def handleCheckQuota(
      userIdentifier: UserIdentifier,
      context: RpcCallContext): Unit = {

    resourceConsumptionSource.addGauge("diskFileCount", userIdentifier.toMap) { () =>
      computeUserResourceConsumption(userIdentifier).diskFileCount
    }
    resourceConsumptionSource.addGauge("diskBytesWritten", userIdentifier.toMap) { () =>
      computeUserResourceConsumption(userIdentifier).diskBytesWritten
    }
    resourceConsumptionSource.addGauge("hdfsFileCount", userIdentifier.toMap) { () =>
      computeUserResourceConsumption(userIdentifier).hdfsFileCount
    }
    resourceConsumptionSource.addGauge("hdfsBytesWritten", userIdentifier.toMap) { () =>
      computeUserResourceConsumption(userIdentifier).hdfsBytesWritten
    }

    val userResourceConsumption = computeUserResourceConsumption(userIdentifier)
    val quota = quotaManager.getQuota(userIdentifier)
    val (isAvailable, reason) =
      quota.checkQuotaSpaceAvailable(userIdentifier, userResourceConsumption)
    context.reply(CheckQuotaResponse(isAvailable, reason))
  }

  private def workersAvailable(
      tmpExcludedWorkerList: Set[WorkerInfo] = Set.empty): util.List[WorkerInfo] = {
    workersSnapShot.asScala.filter { w =>
      !statusSystem.excludedWorkers.contains(w) && !statusSystem.shutdownWorkers.contains(
        w) && !tmpExcludedWorkerList.contains(w)
    }.asJava
  }

  override def getWorkerInfo: String = {
    val sb = new StringBuilder
    sb.append("====================== Workers Info in Master =========================\n")
    workersSnapShot.asScala.foreach { w =>
      sb.append(w).append("\n")
    }
    sb.toString()
  }

  override def getLostWorkers: String = {
    val sb = new StringBuilder
    sb.append("======================= Lost Workers in Master ========================\n")
    lostWorkersSnapshot.asScala.toSeq.sortBy(_._2).foreach { case (worker, time) =>
      sb.append(s"${worker.toUniqueId().padTo(50, " ").mkString}${dateFmt.format(time)}\n")
    }
    sb.toString()
  }

  override def getShutdownWorkers: String = {
    val sb = new StringBuilder
    sb.append("===================== Shutdown Workers in Master ======================\n")
    shutdownWorkerSnapshot.asScala.foreach { worker =>
      sb.append(s"${worker.toUniqueId()}\n")
    }
    sb.toString()
  }

  override def getExcludedWorkers: String = {
    val sb = new StringBuilder
    sb.append("===================== Excluded Workers in Master ======================\n")
    statusSystem.excludedWorkers.asScala.foreach { worker =>
      sb.append(s"${worker.toUniqueId()}\n")
    }
    sb.toString()
  }

  override def getThreadDump: String = {
    val sb = new StringBuilder
    sb.append("========================= Master ThreadDump ==========================\n")
    sb.append(Utils.getThreadDump()).append("\n")
    sb.toString()
  }

  override def getHostnameList: String = {
    val sb = new StringBuilder
    sb.append("================= LifecycleManager Hostname List ======================\n")
    statusSystem.hostnameSet.asScala.foreach { host =>
      sb.append(s"$host\n")
    }
    sb.toString()
  }

  override def getApplicationList: String = {
    val sb = new StringBuilder
    sb.append("================= LifecycleManager Hostname List ======================\n")
    statusSystem.appHeartbeatTime.asScala.toSeq.sortBy(_._2).foreach { case (appId, time) =>
      sb.append(s"${appId.padTo(40, " ").mkString}${dateFmt.format(time)}\n")
    }
    sb.toString()
  }

  override def getShuffleList: String = {
    val sb = new StringBuilder
    sb.append("======================= Shuffle Key List ============================\n")
    statusSystem.registeredShuffle.asScala.foreach { shuffleKey =>
      sb.append(s"$shuffleKey\n")
    }
    sb.toString()
  }

  override def listTopDiskUseApps: String = {
    val sb = new StringBuilder
    sb.append("================== Top Disk Usage Applications =======================\n")
    sb.append(statusSystem.appDiskUsageMetric.summary)
    sb.toString()
  }

  override def listPartitionLocationInfo: String = throw new UnsupportedOperationException()

  override def getUnavailablePeers: String = throw new UnsupportedOperationException()

  override def isShutdown: String = throw new UnsupportedOperationException()

  override def isRegistered: String = throw new UnsupportedOperationException()

  private def isMasterActive: Int = {
    // use int rather than bool for better monitoring on dashboard
    val isActive =
      if (conf.haEnabled) {
        if (statusSystem.asInstanceOf[HAMasterMetaManager].getRatisServer.isLeader) {
          1
        } else {
          0
        }
      } else {
        1
      }
    isActive
  }

  override def initialize(): Unit = {
    super.initialize()
    logInfo("Master started.")
    rpcEnv.awaitTermination()
  }

  override def close(): Unit = synchronized {
    if (!stopped) {
      logInfo("Stopping Master")
      stop()
      super.close()
      logInfo("Master stopped.")
      stopped = true
    }
  }
}

private[deploy] object Master extends Logging {
  def main(args: Array[String]): Unit = {
    val conf = new CelebornConf()
    val masterArgs = new MasterArguments(args, conf)
    val master = new Master(conf, masterArgs)
    master.initialize()
  }
}
