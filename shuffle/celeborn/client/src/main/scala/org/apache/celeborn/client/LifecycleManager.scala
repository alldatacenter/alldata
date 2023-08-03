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
import java.util.{function, List => JList}
import java.util.concurrent.{ConcurrentHashMap, ScheduledFuture, TimeUnit}
import java.util.function.Predicate

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

import com.google.common.annotations.VisibleForTesting

import org.apache.celeborn.client.LifecycleManager.{ShuffleAllocatedWorkers, ShuffleFailedWorkers}
import org.apache.celeborn.client.listener.WorkerStatusListener
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.client.MasterClient
import org.apache.celeborn.common.identity.{IdentityProvider, UserIdentifier}
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{ShufflePartitionLocationInfo, WorkerInfo}
import org.apache.celeborn.common.protocol._
import org.apache.celeborn.common.protocol.RpcNameConstants.WORKER_EP
import org.apache.celeborn.common.protocol.message.ControlMessages._
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.rpc._
import org.apache.celeborn.common.util.{JavaUtils, PbSerDeUtils, ThreadUtils, Utils}
// Can Remove this if celeborn don't support scala211 in future
import org.apache.celeborn.common.util.FunctionConverter._

object LifecycleManager {
  // shuffle id -> partition id -> partition locations
  type ShuffleFileGroups =
    ConcurrentHashMap[Int, ConcurrentHashMap[Integer, util.Set[PartitionLocation]]]
  type ShuffleAllocatedWorkers =
    ConcurrentHashMap[Int, ConcurrentHashMap[WorkerInfo, ShufflePartitionLocationInfo]]
  type ShuffleFailedWorkers = ConcurrentHashMap[WorkerInfo, (StatusCode, Long)]
}

class LifecycleManager(val appUniqueId: String, val conf: CelebornConf) extends RpcEndpoint
  with Logging {

  private val lifecycleHost = Utils.localHostName(conf)

  private val shuffleExpiredCheckIntervalMs = conf.shuffleExpiredCheckIntervalMs
  private val pushReplicateEnabled = conf.clientPushReplicateEnabled
  private val pushRackAwareEnabled = conf.clientReserveSlotsRackAwareEnabled
  private val partitionSplitThreshold = conf.shufflePartitionSplitThreshold
  private val partitionSplitMode = conf.shufflePartitionSplitMode
  // shuffle id -> partition type
  private val shufflePartitionType = JavaUtils.newConcurrentHashMap[Int, PartitionType]()
  private val rangeReadFilter = conf.shuffleRangeReadFilterEnabled
  private val unregisterShuffleTime = JavaUtils.newConcurrentHashMap[Int, Long]()

  val registeredShuffle = ConcurrentHashMap.newKeySet[Int]()
  // maintain each shuffle's map relation of WorkerInfo and partition location
  val shuffleAllocatedWorkers = new ShuffleAllocatedWorkers
  // shuffle id -> (partitionId -> newest PartitionLocation)
  val latestPartitionLocation =
    JavaUtils.newConcurrentHashMap[Int, ConcurrentHashMap[Int, PartitionLocation]]()
  private val userIdentifier: UserIdentifier = IdentityProvider.instantiate(conf).provide()

  @VisibleForTesting
  def workerSnapshots(shuffleId: Int): util.Map[WorkerInfo, ShufflePartitionLocationInfo] =
    shuffleAllocatedWorkers.get(shuffleId)

  val newMapFunc: function.Function[Int, ConcurrentHashMap[Int, PartitionLocation]] =
    new util.function.Function[Int, ConcurrentHashMap[Int, PartitionLocation]]() {
      override def apply(s: Int): ConcurrentHashMap[Int, PartitionLocation] = {
        JavaUtils.newConcurrentHashMap[Int, PartitionLocation]()
      }
    }

  def updateLatestPartitionLocations(
      shuffleId: Int,
      locations: util.List[PartitionLocation]): Unit = {
    val map = latestPartitionLocation.computeIfAbsent(shuffleId, newMapFunc)
    locations.asScala.foreach(location => map.put(location.getId, location))
  }

  case class RegisterCallContext(context: RpcCallContext, partitionId: Int = -1) {
    def reply(response: PbRegisterShuffleResponse) = {
      context.reply(response)
    }
  }

  // register shuffle request waiting for response
  private val registeringShuffleRequest =
    JavaUtils.newConcurrentHashMap[Int, util.Set[RegisterCallContext]]()

  // Threads
  private val forwardMessageThread =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("master-forward-message-thread")
  private var checkForShuffleRemoval: ScheduledFuture[_] = _

  // init driver celeborn LifecycleManager rpc service
  override val rpcEnv: RpcEnv = RpcEnv.create(
    RpcNameConstants.LIFECYCLE_MANAGER_SYS,
    lifecycleHost,
    conf.shuffleManagerPort,
    conf)
  rpcEnv.setupEndpoint(RpcNameConstants.LIFECYCLE_MANAGER_EP, this)

  logInfo(s"Starting LifecycleManager on ${rpcEnv.address}")

  private val masterClient = new MasterClient(rpcEnv, conf)
  val commitManager = new CommitManager(appUniqueId, conf, this)
  val workerStatusTracker = new WorkerStatusTracker(conf, this)
  private val heartbeater =
    new ApplicationHeartbeater(
      appUniqueId,
      conf,
      masterClient,
      () => commitManager.commitMetrics(),
      workerStatusTracker)
  private val changePartitionManager = new ChangePartitionManager(conf, this)
  private val releasePartitionManager = new ReleasePartitionManager(conf, this)

  // Since method `onStart` is executed when `rpcEnv.setupEndpoint` is executed, and
  // `masterClient` is initialized after `rpcEnv` is initialized, if method `onStart` contains
  // a reference to `masterClient`, there may be cases where `masterClient` is null when
  // `masterClient` is called. Therefore, it's necessary to uniformly execute the initialization
  // method at the end of the construction of the class to perform the initialization operations.
  private def initialize(): Unit = {
    // noinspection ConvertExpressionToSAM
    commitManager.start()
    heartbeater.start()
    changePartitionManager.start()
    releasePartitionManager.start()
  }

  override def onStart(): Unit = {
    // noinspection ConvertExpressionToSAM
    checkForShuffleRemoval = forwardMessageThread.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryLogNonFatalError {
          self.send(RemoveExpiredShuffle)
        }
      },
      shuffleExpiredCheckIntervalMs,
      shuffleExpiredCheckIntervalMs,
      TimeUnit.MILLISECONDS)
  }

  override def onStop(): Unit = {
    import scala.concurrent.duration._

    checkForShuffleRemoval.cancel(true)
    ThreadUtils.shutdown(forwardMessageThread, 800.millis)

    commitManager.stop()
    changePartitionManager.stop()
    releasePartitionManager.stop()
    heartbeater.stop()

    masterClient.close()
    if (rpcEnv != null) {
      rpcEnv.shutdown()
      rpcEnv.awaitTermination()
    }
  }

  def getUserIdentifier: UserIdentifier = {
    userIdentifier
  }

  def getHost: String = {
    lifecycleHost
  }

  def getPort: Int = {
    rpcEnv.address.port
  }

  def getPartitionType(shuffleId: Int): PartitionType = {
    shufflePartitionType.getOrDefault(shuffleId, conf.shufflePartitionType)
  }

  override def receive: PartialFunction[Any, Unit] = {
    case RemoveExpiredShuffle =>
      removeExpiredShuffle()
    case StageEnd(shuffleId) =>
      logInfo(s"Received StageEnd request, shuffleId $shuffleId.")
      handleStageEnd(shuffleId)
    case pb: PbUnregisterShuffle =>
      val shuffleId = pb.getShuffleId
      logDebug(s"Received UnregisterShuffle request," +
        s"shuffleId $shuffleId.")
      handleUnregisterShuffle(shuffleId)
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case pb: PbRegisterShuffle =>
      val shuffleId = pb.getShuffleId
      val numMappers = pb.getNumMappers
      val numPartitions = pb.getNumPartitions
      logDebug(s"Received RegisterShuffle request, " +
        s"$shuffleId, $numMappers, $numPartitions.")
      offerAndReserveSlots(
        RegisterCallContext(context),
        shuffleId,
        numMappers,
        numPartitions)

    case pb: PbRegisterMapPartitionTask =>
      val shuffleId = pb.getShuffleId
      val numMappers = pb.getNumMappers
      val mapId = pb.getMapId
      val attemptId = pb.getAttemptId
      val partitionId = pb.getPartitionId
      logDebug(s"Received Register map partition task request, " +
        s"$shuffleId, $numMappers, $mapId, $attemptId, $partitionId.")
      shufflePartitionType.putIfAbsent(shuffleId, PartitionType.MAP)
      offerAndReserveSlots(
        RegisterCallContext(context, partitionId),
        shuffleId,
        numMappers,
        numMappers,
        partitionId)

    case pb: PbRevive =>
      val shuffleId = pb.getShuffleId
      val mapIds = pb.getMapIdList
      val partitionInfos = pb.getPartitionInfoList

      val partitionIds = new util.ArrayList[Integer]()
      val epochs = new util.ArrayList[Integer]()
      val oldPartitions = new util.ArrayList[PartitionLocation]()
      val causes = new util.ArrayList[StatusCode]()
      (0 until partitionInfos.size()).foreach { idx =>
        val info = partitionInfos.get(idx)
        partitionIds.add(info.getPartitionId)
        epochs.add(info.getEpoch)
        if (info.hasPartition) {
          oldPartitions.add(PbSerDeUtils.fromPbPartitionLocation(info.getPartition))
        } else {
          oldPartitions.add(null)
        }
        causes.add(Utils.toStatusCode(info.getStatus))
      }
      logWarning(s"Received Revive request, number of partitions ${partitionIds.size()}")
      handleRevive(
        context,
        shuffleId,
        mapIds,
        partitionIds,
        epochs,
        oldPartitions,
        causes)

    case pb: PbPartitionSplit =>
      val shuffleId = pb.getShuffleId
      val partitionId = pb.getPartitionId
      val epoch = pb.getEpoch
      val oldPartition = PbSerDeUtils.fromPbPartitionLocation(pb.getOldPartition)
      logTrace(s"Received split request, " +
        s"$shuffleId, $partitionId, $epoch, $oldPartition")
      changePartitionManager.handleRequestPartitionLocation(
        ChangeLocationsCallContext(context, 1),
        shuffleId,
        partitionId,
        epoch,
        oldPartition)

    case MapperEnd(shuffleId, mapId, attemptId, numMappers, partitionId) =>
      logTrace(s"Received MapperEnd TaskEnd request, " +
        s"${Utils.makeMapKey(shuffleId, mapId, attemptId)}")
      val partitionType = getPartitionType(shuffleId)
      partitionType match {
        case PartitionType.REDUCE =>
          handleMapperEnd(context, shuffleId, mapId, attemptId, numMappers)
        case PartitionType.MAP =>
          handleMapPartitionEnd(
            context,
            shuffleId,
            mapId,
            attemptId,
            partitionId,
            numMappers)
        case _ =>
          throw new UnsupportedOperationException(s"Not support $partitionType yet")
      }

    case GetReducerFileGroup(shuffleId: Int) =>
      logDebug(s"Received GetShuffleFileGroup request," +
        s"shuffleId $shuffleId.")
      handleGetReducerFileGroup(context, shuffleId)

    case pb: PbHeartbeatFromClient =>
      handleHeartbeatFromClient(context, pb)
  }

  private def offerAndReserveSlots(
      context: RegisterCallContext,
      shuffleId: Int,
      numMappers: Int,
      numPartitions: Int,
      partitionId: Int = -1): Unit = {
    val partitionType = getPartitionType(shuffleId)
    registeringShuffleRequest.synchronized {
      if (registeringShuffleRequest.containsKey(shuffleId)) {
        // If same request already exists in the registering request list for the same shuffle,
        // just register and return.
        logDebug(s"[handleRegisterShuffle] request for shuffle $shuffleId exists, just register")
        registeringShuffleRequest.get(shuffleId).add(context)
        return
      } else {
        // If shuffle is registered, reply this shuffle's partition location and return.
        // Else add this request to registeringShuffleRequest.
        if (registeredShuffle.contains(shuffleId)) {
          val initialLocs = workerSnapshots(shuffleId)
            .values()
            .asScala
            .flatMap(_.getAllPrimaryLocationsWithMinEpoch().asScala)
            .filter(p =>
              (partitionType == PartitionType.REDUCE && p.getEpoch == 0) || (partitionType == PartitionType.MAP && p.getId == partitionId))
            .toArray
          partitionType match {
            case PartitionType.MAP => processMapTaskReply(
                shuffleId,
                context.context,
                partitionId,
                initialLocs)
            case PartitionType.REDUCE =>
              context.reply(RegisterShuffleResponse(StatusCode.SUCCESS, initialLocs))
            case _ =>
              throw new UnsupportedOperationException(s"Not support $partitionType yet")
          }
          return
        }

        logInfo(s"New shuffle request, shuffleId $shuffleId, partitionType: $partitionType " +
          s"numMappers: $numMappers, numReducers: $numPartitions.")
        val set = new util.HashSet[RegisterCallContext]()
        set.add(context)
        registeringShuffleRequest.put(shuffleId, set)
      }
    }

    def processMapTaskReply(
        shuffleId: Int,
        context: RpcCallContext,
        partitionId: Int,
        partitionLocations: Array[PartitionLocation]): Unit = {
      // if any partition location resource exist just reply
      if (partitionLocations.size > 0) {
        context.reply(RegisterShuffleResponse(StatusCode.SUCCESS, partitionLocations))
      } else {
        // request new resource for this task
        changePartitionManager.handleRequestPartitionLocation(
          ApplyNewLocationCallContext(context),
          shuffleId,
          partitionId,
          -1,
          null)
      }
    }

    // Reply to all RegisterShuffle request for current shuffle id.
    def reply(response: PbRegisterShuffleResponse): Unit = {
      registeringShuffleRequest.synchronized {
        registeringShuffleRequest.asScala
          .get(shuffleId)
          .foreach(_.asScala.foreach(context => {
            partitionType match {
              case PartitionType.MAP =>
                if (response.getStatus == StatusCode.SUCCESS.getValue) {
                  val partitionLocations =
                    response.getPartitionLocationsList.asScala.filter(
                      _.getId == context.partitionId).map(r =>
                      PbSerDeUtils.fromPbPartitionLocation(r)).toArray
                  processMapTaskReply(
                    shuffleId,
                    context.context,
                    context.partitionId,
                    partitionLocations)
                } else {
                  // when register not success, need reply origin response,
                  // otherwise will lost original exception message
                  context.reply(response)
                }
              case PartitionType.REDUCE => context.reply(response)
              case _ =>
                throw new UnsupportedOperationException(s"Not support $partitionType yet")
            }
          }))
        registeringShuffleRequest.remove(shuffleId)
      }
    }

    // First, request to get allocated slots from Primary
    val ids = new util.ArrayList[Integer](numPartitions)
    (0 until numPartitions).foreach(idx => ids.add(Integer.valueOf(idx)))
    val res = requestPrimaryRequestSlotsWithRetry(shuffleId, ids)

    res.status match {
      case StatusCode.REQUEST_FAILED =>
        logDebug(s"OfferSlots RPC request failed for $shuffleId!")
        reply(RegisterShuffleResponse(StatusCode.REQUEST_FAILED, Array.empty))
        return
      case StatusCode.SLOT_NOT_AVAILABLE =>
        logDebug(s"OfferSlots for $shuffleId failed!")
        reply(RegisterShuffleResponse(StatusCode.SLOT_NOT_AVAILABLE, Array.empty))
        return
      case StatusCode.SUCCESS =>
        logInfo(s"OfferSlots for $shuffleId Success!")
        logDebug(s" Slots Info: ${res.workerResource}")
      case _ => // won't happen
        throw new UnsupportedOperationException()
    }

    // Reserve slots for each PartitionLocation. When response status is SUCCESS, WorkerResource
    // won't be empty since primary will reply SlotNotAvailable status when reserved slots is empty.
    val slots = res.workerResource
    val candidatesWorkers = new util.HashSet(slots.keySet())
    val connectFailedWorkers = new ShuffleFailedWorkers()

    // Second, for each worker, try to initialize the endpoint.
    val parallelism = Math.min(Math.max(1, slots.size()), conf.clientRpcMaxParallelism)
    ThreadUtils.parmap(slots.asScala.to, "InitWorkerRef", parallelism) { case (workerInfo, _) =>
      try {
        workerInfo.endpoint =
          rpcEnv.setupEndpointRef(RpcAddress.apply(workerInfo.host, workerInfo.rpcPort), WORKER_EP)
      } catch {
        case t: Throwable =>
          logError(s"Init rpc client failed for $shuffleId on $workerInfo during reserve slots.", t)
          connectFailedWorkers.put(
            workerInfo,
            (StatusCode.WORKER_UNKNOWN, System.currentTimeMillis()))
      }
    }

    candidatesWorkers.removeAll(connectFailedWorkers.asScala.keys.toList.asJava)
    workerStatusTracker.recordWorkerFailure(connectFailedWorkers)
    // If newly allocated from primary and can setup endpoint success, LifecycleManager should remove worker from
    // the excluded worker list to improve the accuracy of the list.
    workerStatusTracker.removeFromExcludedWorkers(candidatesWorkers)

    // Third, for each slot, LifecycleManager should ask Worker to reserve the slot
    // and prepare the pushing data env.
    val reserveSlotsSuccess =
      reserveSlotsWithRetry(
        shuffleId,
        candidatesWorkers,
        slots,
        updateEpoch = false)

    // If reserve slots failed, clear allocated resources, reply ReserveSlotFailed and return.
    if (!reserveSlotsSuccess) {
      logError(s"reserve buffer for $shuffleId failed, reply to all.")
      reply(RegisterShuffleResponse(StatusCode.RESERVE_SLOTS_FAILED, Array.empty))
      // tell Primary to release slots
      requestPrimaryReleaseSlots(
        ReleaseSlots(appUniqueId, shuffleId, List.empty.asJava, List.empty.asJava))
    } else {
      logInfo(s"ReserveSlots for $shuffleId success!")
      logDebug(s"Allocated Slots: $slots")
      // Forth, register shuffle success, update status
      val allocatedWorkers =
        JavaUtils.newConcurrentHashMap[WorkerInfo, ShufflePartitionLocationInfo]()
      slots.asScala.foreach { case (workerInfo, (primaryLocations, replicaLocations)) =>
        val partitionLocationInfo = new ShufflePartitionLocationInfo()
        partitionLocationInfo.addPrimaryPartitions(primaryLocations)
        updateLatestPartitionLocations(shuffleId, primaryLocations)
        partitionLocationInfo.addReplicaPartitions(replicaLocations)
        allocatedWorkers.put(workerInfo, partitionLocationInfo)
      }
      shuffleAllocatedWorkers.put(shuffleId, allocatedWorkers)
      registeredShuffle.add(shuffleId)
      commitManager.registerShuffle(shuffleId, numMappers)

      // Fifth, reply the allocated partition location to ShuffleClient.
      logInfo(s"Handle RegisterShuffle Success for $shuffleId.")
      val allPrimaryPartitionLocations = slots.asScala.flatMap(_._2._1.asScala).toArray
      reply(RegisterShuffleResponse(StatusCode.SUCCESS, allPrimaryPartitionLocations))
    }
  }

  private def handleRevive(
      context: RpcCallContext,
      shuffleId: Int,
      mapIds: util.List[Integer],
      partitionIds: util.List[Integer],
      oldEpochs: util.List[Integer],
      oldPartitions: util.List[PartitionLocation],
      causes: util.List[StatusCode]): Unit = {
    val contextWrapper =
      ChangeLocationsCallContext(context, partitionIds.size())
    // If shuffle not registered, reply ShuffleNotRegistered and return
    if (!registeredShuffle.contains(shuffleId)) {
      logError(s"[handleRevive] shuffle $shuffleId not registered!")
      contextWrapper.reply(
        -1,
        StatusCode.SHUFFLE_NOT_REGISTERED,
        None,
        false)
      return
    }

    if (commitManager.isStageEnd(shuffleId)) {
      logError(s"[handleRevive] shuffle $shuffleId stage ended!")
      contextWrapper.reply(
        -1,
        StatusCode.STAGE_ENDED,
        None,
        false)
      return
    }

    mapIds.asScala.foreach { mapId =>
      if (commitManager.isMapperEnded(shuffleId, mapId)) {
        logWarning(s"[handleRevive] Mapper ended, mapId $mapId, ended attemptId ${commitManager.getMapperAttempts(
          shuffleId)(mapId)}, shuffleId $shuffleId")
        contextWrapper.markMapperEnd(mapId)
      }
    }

    (0 until partitionIds.size()).foreach { idx =>
      changePartitionManager.handleRequestPartitionLocation(
        contextWrapper,
        shuffleId,
        partitionIds.get(idx),
        oldEpochs.get(idx),
        oldPartitions.get(idx),
        Some(causes.get(idx)))
    }
  }

  private def handleMapperEnd(
      context: RpcCallContext,
      shuffleId: Int,
      mapId: Int,
      attemptId: Int,
      numMappers: Int): Unit = {

    val (mapperAttemptFinishedSuccess, allMapperFinished) =
      commitManager.finishMapperAttempt(shuffleId, mapId, attemptId, numMappers)
    if (mapperAttemptFinishedSuccess && allMapperFinished) {
      // last mapper finished. call mapper end
      logInfo(s"Last MapperEnd, call StageEnd with shuffleKey:" +
        s"shuffleId $shuffleId.")
      self.send(StageEnd(shuffleId))
    }

    // reply success
    context.reply(MapperEndResponse(StatusCode.SUCCESS))
  }

  private def handleGetReducerFileGroup(
      context: RpcCallContext,
      shuffleId: Int): Unit = {
    commitManager.handleGetReducerFileGroup(context, shuffleId)
  }

  private def handleHeartbeatFromClient(
      context: RpcCallContext,
      pb: PbHeartbeatFromClient): Unit = {
    val shuffleIds = new util.ArrayList[Integer](pb.getShuffleIdList)
    val pred = new Predicate[Integer] {
      override def test(id: Integer): Boolean = registeredShuffle.contains(id)
    }
    shuffleIds.removeIf(pred)
    context.reply(HeartbeatFromClientResponse(shuffleIds))
  }

  private def handleStageEnd(shuffleId: Int): Unit = {
    // check whether shuffle has registered
    if (!registeredShuffle.contains(shuffleId)) {
      logInfo(s"[handleStageEnd]" +
        s"$shuffleId not registered, maybe no shuffle data within this stage.")
      // record in stageEndShuffleSet
      commitManager.setStageEnd(shuffleId)
      return
    }

    if (commitManager.tryFinalCommit(shuffleId)) {
      // Here we only clear PartitionLocation info in shuffleAllocatedWorkers.
      // Since rerun or speculation task may running after we handle StageEnd.
      workerSnapshots(shuffleId).asScala.foreach { case (_, partitionLocationInfo) =>
        partitionLocationInfo.removeAllPrimaryPartitions()
        partitionLocationInfo.removeAllReplicaPartitions()
      }
      requestPrimaryReleaseSlots(
        ReleaseSlots(appUniqueId, shuffleId, List.empty.asJava, List.empty.asJava))
    }
  }

  private def handleMapPartitionEnd(
      context: RpcCallContext,
      shuffleId: Int,
      mapId: Int,
      attemptId: Int,
      partitionId: Int,
      numMappers: Int): Unit = {
    def reply(result: Boolean): Unit = {
      val message =
        s"to handle MapPartitionEnd for ${Utils.makeMapKey(shuffleId, mapId, attemptId)}, " +
          s"$partitionId.";
      result match {
        case true => // if already committed by another try
          logDebug(s"Succeed $message")
          context.reply(MapperEndResponse(StatusCode.SUCCESS))
        case false =>
          logError(s"Failed $message")
          context.reply(MapperEndResponse(StatusCode.SHUFFLE_DATA_LOST))
      }
    }

    val (mapperAttemptFinishedSuccess, _) = commitManager.finishMapperAttempt(
      shuffleId,
      mapId,
      attemptId,
      numMappers,
      partitionId)
    reply(mapperAttemptFinishedSuccess)
  }

  def handleUnregisterShuffle(
      shuffleId: Int): Unit = {
    if (getPartitionType(shuffleId) == PartitionType.REDUCE) {
      // if StageEnd has not been handled, trigger StageEnd
      if (!commitManager.isStageEnd(shuffleId)) {
        logInfo(s"Call StageEnd before Unregister Shuffle $shuffleId.")
        // try call stage end
        handleStageEnd(shuffleId)
        // wait stage end
        val (isTimeOut, cost) = commitManager.waitStageEnd(shuffleId)
        if (isTimeOut) {
          logError(s"[handleUnregisterShuffle] trigger StageEnd Timeout! $shuffleId.")
        } else {
          logInfo(s"[handleUnregisterShuffle] Wait for handleStageEnd complete costs ${cost}ms")
        }
      }
    }

    if (shuffleResourceExists(shuffleId)) {
      logWarning(s"Partition exists for shuffle $shuffleId, " +
        "maybe caused by task rerun or speculative.")
      requestPrimaryReleaseSlots(
        ReleaseSlots(appUniqueId, shuffleId, List.empty.asJava, List.empty.asJava))
    }

    // add shuffleKey to delay shuffle removal set
    unregisterShuffleTime.put(shuffleId, System.currentTimeMillis())

    logInfo(s"Unregister for $shuffleId success.")
  }

  /* ========================================================== *
   |        END OF EVENT HANDLER                                |
   * ========================================================== */

  /**
   * After getting WorkerResource, LifecycleManger needs to ask each Worker to
   * reserve corresponding slot and prepare push data env in Worker side.
   *
   * @param shuffleId     Application shuffle id
   * @param slots         WorkerResource to reserve slots
   * @return List of reserving slot failed workers
   */
  private def reserveSlots(
      shuffleId: Int,
      slots: WorkerResource): util.List[WorkerInfo] = {
    val reserveSlotFailedWorkers = new ShuffleFailedWorkers()
    val failureInfos = new util.concurrent.CopyOnWriteArrayList[String]()
    val workerPartitionLocations = slots.asScala.filter(p => !p._2._1.isEmpty || !p._2._2.isEmpty)
    val parallelism =
      Math.min(Math.max(1, workerPartitionLocations.size), conf.clientRpcMaxParallelism)
    ThreadUtils.parmap(workerPartitionLocations.to, "ReserveSlot", parallelism) {
      case (workerInfo, (primaryLocations, replicaLocations)) =>
        val res = requestWorkerReserveSlots(
          workerInfo.endpoint,
          ReserveSlots(
            appUniqueId,
            shuffleId,
            primaryLocations,
            replicaLocations,
            partitionSplitThreshold,
            partitionSplitMode,
            getPartitionType(shuffleId),
            rangeReadFilter,
            userIdentifier,
            conf.pushDataTimeoutMs))
        if (res.status.equals(StatusCode.SUCCESS)) {
          logDebug(s"Successfully allocated " +
            s"partitions buffer for shuffleId $shuffleId" +
            s" from worker ${workerInfo.readableAddress()}.")
        } else {
          failureInfos.add(s"[reserveSlots] Failed to" +
            s" reserve buffers for shuffleId $shuffleId" +
            s" from worker ${workerInfo.readableAddress()}. Reason: ${res.reason}")
          reserveSlotFailedWorkers.put(workerInfo, (res.status, System.currentTimeMillis()))
        }
    }
    if (failureInfos.asScala.nonEmpty) {
      logError(s"Aggregated error of reserveSlots for " +
        s"shuffleId $shuffleId " +
        s"failure:${failureInfos.asScala.foldLeft("")((x, y) => s"$x \n $y")}")
    }
    workerStatusTracker.recordWorkerFailure(reserveSlotFailedWorkers)
    new util.ArrayList[WorkerInfo](reserveSlotFailedWorkers.asScala.keys.toList.asJava)
  }

  /**
   * When enabling replicate, if one of the partition location reserve slots failed,
   * LifecycleManager also needs to release another corresponding partition location.
   * To release the corresponding partition location, LifecycleManager should:
   *   1. Remove the peer partition location of failed partition location from slots.
   *   2. Request the Worker to destroy the slot's FileWriter.
   *   3. Request the Primary to release the worker slots status.
   *
   * @param shuffleId                shuffle id
   * @param slots                    allocated WorkerResource
   * @param failedPartitionLocations reserve slot failed partition location
   */
  private def releasePeerPartitionLocation(
      shuffleId: Int,
      slots: WorkerResource,
      failedPartitionLocations: mutable.HashMap[Int, PartitionLocation]): Unit = {
    val destroyResource = new WorkerResource
    failedPartitionLocations.values
      .flatMap { partition => Option(partition.getPeer) }
      .foreach { partition =>
        var destroyWorkerInfo = partition.getWorker
        val workerInfoWithRpcRef = slots.keySet().asScala.find(_.equals(destroyWorkerInfo))
          .getOrElse {
            logWarning(s"Cannot find workInfo for $shuffleId from previous success workResource:" +
              s" ${destroyWorkerInfo.readableAddress()}, init according to partition info")
            try {
              if (workerStatusTracker.workerAvailable(destroyWorkerInfo)) {
                destroyWorkerInfo.endpoint = rpcEnv.setupEndpointRef(
                  RpcAddress.apply(destroyWorkerInfo.host, destroyWorkerInfo.rpcPort),
                  WORKER_EP)
              } else {
                logInfo(
                  s"${destroyWorkerInfo.toUniqueId()} is unavailable, set destroyWorkerInfo to null")
                destroyWorkerInfo = null
              }
            } catch {
              case t: Throwable =>
                logError(
                  s"Init rpc client failed for $shuffleId on ${destroyWorkerInfo.readableAddress()} during release peer partition.",
                  t)
                destroyWorkerInfo = null
            }
            destroyWorkerInfo
          }
        if (slots.containsKey(workerInfoWithRpcRef)) {
          val (primaryPartitionLocations, replicaPartitionLocations) =
            slots.get(workerInfoWithRpcRef)
          partition.getMode match {
            case PartitionLocation.Mode.PRIMARY =>
              primaryPartitionLocations.remove(partition)
              destroyResource.computeIfAbsent(workerInfoWithRpcRef, newLocationFunc)
                ._1.add(partition)
            case PartitionLocation.Mode.REPLICA =>
              replicaPartitionLocations.remove(partition)
              destroyResource.computeIfAbsent(workerInfoWithRpcRef, newLocationFunc)
                ._2.add(partition)
          }
          if (primaryPartitionLocations.isEmpty && replicaPartitionLocations.isEmpty) {
            slots.remove(workerInfoWithRpcRef)
          }
        }
      }
    if (!destroyResource.isEmpty) {
      destroySlotsWithRetry(shuffleId, destroyResource)
      logInfo(s"Destroyed peer partitions for reserve buffer failed workers " +
        s"shuffleId $shuffleId, $destroyResource")

      val workerIds = new util.ArrayList[String]()
      val workerSlotsPerDisk = new util.ArrayList[util.Map[String, Integer]]()
      Utils.getSlotsPerDisk(destroyResource).asScala.foreach {
        case (workerInfo, slotsPerDisk) =>
          workerIds.add(workerInfo.toUniqueId())
          workerSlotsPerDisk.add(slotsPerDisk)
      }
      val msg = ReleaseSlots(appUniqueId, shuffleId, workerIds, workerSlotsPerDisk)
      requestPrimaryReleaseSlots(msg)
      logDebug(s"Released slots for reserve buffer failed workers " +
        s"${workerIds.asScala.mkString(",")}" + s"${slots.asScala.mkString(",")}" +
        s"shuffleId $shuffleId")
    }
  }

  /**
   * Collect all allocated partition locations on reserving slot failed workers
   * and remove failed worker's partition locations from total slots.
   * For each reduce id, we only need to maintain one of the pair locations
   * even if enabling replicate. If Celeborn wants to release the failed partition location,
   * the corresponding peers will be handled in [[releasePeerPartitionLocation]]
   *
   * @param reserveFailedWorkers reserve slot failed WorkerInfo list of slots
   * @param slots                the slots tried to reserve a slot
   * @return reserving slot failed partition locations
   */
  def getFailedPartitionLocations(
      reserveFailedWorkers: util.List[WorkerInfo],
      slots: WorkerResource): mutable.HashMap[Int, PartitionLocation] = {
    val failedPartitionLocations = new mutable.HashMap[Int, PartitionLocation]()
    reserveFailedWorkers.asScala.foreach { workerInfo =>
      val (failedPrimaryLocations, failedReplicaLocations) = slots.remove(workerInfo)
      if (null != failedPrimaryLocations) {
        failedPrimaryLocations.asScala.foreach { failedPrimaryLocation =>
          failedPartitionLocations += (failedPrimaryLocation.getId -> failedPrimaryLocation)
        }
      }
      if (null != failedReplicaLocations) {
        failedReplicaLocations.asScala.foreach { failedReplicaLocation =>
          val partitionId = failedReplicaLocation.getId
          if (!failedPartitionLocations.contains(partitionId)) {
            failedPartitionLocations += (partitionId -> failedReplicaLocation)
          }
        }
      }
    }
    failedPartitionLocations
  }

  /**
   * Reserve buffers with retry, retry on another node will cause slots to be inconsistent.
   *
   * @param shuffleId     shuffle id
   * @param candidates    working worker list
   * @param slots         the total allocated worker resources that need to be applied for the slot
   * @return If reserve all slots success
   */
  def reserveSlotsWithRetry(
      shuffleId: Int,
      candidates: util.HashSet[WorkerInfo],
      slots: WorkerResource,
      updateEpoch: Boolean = true): Boolean = {
    var requestSlots = slots
    val reserveSlotsMaxRetries = conf.clientReserveSlotsMaxRetries
    val reserveSlotsRetryWait = conf.clientReserveSlotsRetryWait
    var retryTimes = 1
    var noAvailableSlots = false
    var success = false
    while (retryTimes <= reserveSlotsMaxRetries && !success && !noAvailableSlots) {
      if (retryTimes > 1) {
        Thread.sleep(reserveSlotsRetryWait)
      }
      // reserve buffers
      logInfo(s"Try reserve slots for $shuffleId for $retryTimes times.")
      val reserveFailedWorkers = reserveSlots(shuffleId, requestSlots)
      if (reserveFailedWorkers.isEmpty) {
        success = true
      } else {
        // Should remove failed workers from candidates during retry to avoid reallocate in failed workers.
        candidates.removeAll(reserveFailedWorkers)
        // Find out all failed partition locations and remove failed worker's partition location
        // from slots.
        val failedPartitionLocations = getFailedPartitionLocations(reserveFailedWorkers, slots)
        // When enable replicate, if one of the partition location reserve slots failed, we also
        // need to release another corresponding partition location and remove it from slots.
        if (pushReplicateEnabled && failedPartitionLocations.nonEmpty && !slots.isEmpty) {
          releasePeerPartitionLocation(shuffleId, slots, failedPartitionLocations)
        }
        if (retryTimes < reserveSlotsMaxRetries) {
          // get retryCandidates resource and retry reserve buffer
          val retryCandidates = new util.HashSet(slots.keySet())
          // add candidates to avoid revive action passed in slots only 2 worker
          retryCandidates.addAll(candidates)
          // remove excluded workers from retryCandidates
          retryCandidates.removeAll(
            workerStatusTracker.excludedWorkers.keys().asScala.toList.asJava)
          retryCandidates.removeAll(workerStatusTracker.shuttingWorkers.asScala.toList.asJava)
          if (retryCandidates.size < 1 || (pushReplicateEnabled && retryCandidates.size < 2)) {
            logError(s"Retry reserve slots for $shuffleId failed caused by not enough slots.")
            noAvailableSlots = true
          } else {
            // Only when the LifecycleManager needs to retry reserve slots again, re-allocate slots
            // and put the new allocated slots to the total slots, the re-allocated slots won't be
            // duplicated with existing partition locations.
            requestSlots = reallocateSlotsFromCandidates(
              failedPartitionLocations.values.toList,
              retryCandidates.asScala.toList,
              updateEpoch)
            requestSlots.asScala.foreach {
              case (workerInfo, (retryPrimaryLocs, retryReplicaLocs)) =>
                val (primaryPartitionLocations, replicaPartitionLocations) =
                  slots.computeIfAbsent(workerInfo, newLocationFunc)
                primaryPartitionLocations.addAll(retryPrimaryLocs)
                replicaPartitionLocations.addAll(retryReplicaLocs)
            }
          }
        } else {
          logError(s"Try reserve slots for $shuffleId failed after $reserveSlotsMaxRetries retry.")
        }
      }
      retryTimes += 1
    }
    // if failed after retry, destroy all allocated buffers
    if (!success) {
      // Reserve slot failed workers' partition location and corresponding peer partition location
      // has been removed from slots by call [[getFailedPartitionLocations]] and
      // [[releasePeerPartitionLocation]]. Now in the slots are all the successful partition
      // locations.
      logWarning(s"Reserve buffers for $shuffleId still fail after retrying, clear buffers.")
      destroySlotsWithRetry(shuffleId, slots)
    } else {
      logInfo(s"Reserve buffer success for shuffleId $shuffleId")
    }
    success
  }

  val newLocationFunc =
    new util.function.Function[WorkerInfo, (JList[PartitionLocation], JList[PartitionLocation])] {
      override def apply(w: WorkerInfo): (JList[PartitionLocation], JList[PartitionLocation]) =
        (new util.LinkedList[PartitionLocation](), new util.LinkedList[PartitionLocation]())
    }

  /**
   * Allocate a new primary/replica PartitionLocation pair from the current WorkerInfo list.
   *
   * @param oldEpochId Current partition reduce location last epoch id
   * @param candidates WorkerInfo list can be used to offer worker slots
   * @param slots      Current WorkerResource
   */
  def allocateFromCandidates(
      id: Int,
      oldEpochId: Int,
      candidates: List[WorkerInfo],
      slots: WorkerResource,
      updateEpoch: Boolean = true): Unit = {

    def isOnSameRack(primaryIndex: Int, replicaIndex: Int): Boolean = {
      candidates(primaryIndex).networkLocation.equals(candidates(replicaIndex).networkLocation)
    }

    val primaryIndex = Random.nextInt(candidates.size)
    val primaryLocation = new PartitionLocation(
      id,
      if (updateEpoch) oldEpochId + 1 else oldEpochId,
      candidates(primaryIndex).host,
      candidates(primaryIndex).rpcPort,
      candidates(primaryIndex).pushPort,
      candidates(primaryIndex).fetchPort,
      candidates(primaryIndex).replicatePort,
      PartitionLocation.Mode.PRIMARY)

    if (pushReplicateEnabled) {
      var replicaIndex = (primaryIndex + 1) % candidates.size
      while (pushRackAwareEnabled && isOnSameRack(primaryIndex, replicaIndex)
        && replicaIndex != primaryIndex) {
        replicaIndex = (replicaIndex + 1) % candidates.size
      }
      // If one turn no suitable peer, then just use the next worker.
      if (replicaIndex == primaryIndex) {
        replicaIndex = (primaryIndex + 1) % candidates.size
      }
      val replicaLocation = new PartitionLocation(
        id,
        if (updateEpoch) oldEpochId + 1 else oldEpochId,
        candidates(replicaIndex).host,
        candidates(replicaIndex).rpcPort,
        candidates(replicaIndex).pushPort,
        candidates(replicaIndex).fetchPort,
        candidates(replicaIndex).replicatePort,
        PartitionLocation.Mode.REPLICA,
        primaryLocation)
      primaryLocation.setPeer(replicaLocation)
      val primaryAndReplicaPairs = slots.computeIfAbsent(candidates(replicaIndex), newLocationFunc)
      primaryAndReplicaPairs._2.add(replicaLocation)
    }

    val primaryAndReplicaPairs = slots.computeIfAbsent(candidates(primaryIndex), newLocationFunc)
    primaryAndReplicaPairs._1.add(primaryLocation)
  }

  private def reallocateSlotsFromCandidates(
      oldPartitions: List[PartitionLocation],
      candidates: List[WorkerInfo],
      updateEpoch: Boolean = true): WorkerResource = {
    val slots = new WorkerResource()
    oldPartitions.foreach { partition =>
      allocateFromCandidates(partition.getId, partition.getEpoch, candidates, slots, updateEpoch)
    }
    slots
  }

  /**
   * For the slots that need to be destroyed, LifecycleManager will ask the corresponding worker
   * to destroy related FileWriter.
   *
   * @param shuffleId      shuffle id
   * @param slotsToDestroy worker resource to be destroyed
   * @return destroy failed primary and replica location unique id
   */
  def destroySlotsWithRetry(
      shuffleId: Int,
      slotsToDestroy: WorkerResource): Unit = {
    val shuffleKey = Utils.makeShuffleKey(appUniqueId, shuffleId)
    val parallelism = Math.min(Math.max(1, slotsToDestroy.size()), conf.clientRpcMaxParallelism)
    ThreadUtils.parmap(
      slotsToDestroy.asScala,
      "DestroySlot",
      parallelism) { case (workerInfo, (primaryLocations, replicaLocations)) =>
      val destroy = DestroyWorkerSlots(
        shuffleKey,
        primaryLocations.asScala.map(_.getUniqueId).asJava,
        replicaLocations.asScala.map(_.getUniqueId).asJava)
      var res = requestWorkerDestroySlots(workerInfo.endpoint, destroy)
      if (res.status != StatusCode.SUCCESS) {
        logDebug(s"Request $destroy return ${res.status} for $shuffleKey, " +
          s"will retry request destroy.")
        res = requestWorkerDestroySlots(
          workerInfo.endpoint,
          DestroyWorkerSlots(shuffleKey, res.failedPrimarys, res.failedReplicas))
      }
    }
  }

  private def removeExpiredShuffle(): Unit = {
    val currentTime = System.currentTimeMillis()
    unregisterShuffleTime.keys().asScala.foreach { shuffleId =>
      if (unregisterShuffleTime.get(shuffleId) < currentTime - shuffleExpiredCheckIntervalMs) {
        logInfo(s"Clear shuffle $shuffleId.")
        // clear for the shuffle
        registeredShuffle.remove(shuffleId)
        registeringShuffleRequest.remove(shuffleId)
        shuffleAllocatedWorkers.remove(shuffleId)
        latestPartitionLocation.remove(shuffleId)
        commitManager.removeExpiredShuffle(shuffleId)
        changePartitionManager.removeExpiredShuffle(shuffleId)
        val unregisterShuffleResponse = requestPrimaryUnregisterShuffle(
          UnregisterShuffle(appUniqueId, shuffleId, MasterClient.genRequestId()))
        // if unregister shuffle not success, wait next turn
        if (StatusCode.SUCCESS == Utils.toStatusCode(unregisterShuffleResponse.getStatus)) {
          unregisterShuffleTime.remove(shuffleId)
        }
      }
    }
  }

  private def requestPrimaryRequestSlotsWithRetry(
      shuffleId: Int,
      ids: util.ArrayList[Integer]): RequestSlotsResponse = {
    val req =
      RequestSlots(
        appUniqueId,
        shuffleId,
        ids,
        lifecycleHost,
        pushReplicateEnabled,
        pushRackAwareEnabled,
        userIdentifier)
    val res = requestPrimaryRequestSlots(req)
    if (res.status != StatusCode.SUCCESS) {
      requestPrimaryRequestSlots(req)
    } else {
      res
    }
  }

  private def requestPrimaryRequestSlots(message: RequestSlots): RequestSlotsResponse = {
    val shuffleKey = Utils.makeShuffleKey(message.applicationId, message.shuffleId)
    try {
      masterClient.askSync[RequestSlotsResponse](message, classOf[RequestSlotsResponse])
    } catch {
      case e: Exception =>
        logError(s"AskSync RegisterShuffle for $shuffleKey failed.", e)
        RequestSlotsResponse(StatusCode.REQUEST_FAILED, new WorkerResource())
    }
  }

  private def requestWorkerReserveSlots(
      endpoint: RpcEndpointRef,
      message: ReserveSlots): ReserveSlotsResponse = {
    val shuffleKey = Utils.makeShuffleKey(message.applicationId, message.shuffleId)
    try {
      endpoint.askSync[ReserveSlotsResponse](message, conf.clientRpcReserveSlotsRpcTimeout)
    } catch {
      case e: Exception =>
        val msg = s"Exception when askSync ReserveSlots for $shuffleKey " +
          s"on worker $endpoint."
        logError(msg, e)
        ReserveSlotsResponse(StatusCode.REQUEST_FAILED, msg + s" ${e.getMessage}")
    }
  }

  private def requestWorkerDestroySlots(
      endpoint: RpcEndpointRef,
      message: DestroyWorkerSlots): DestroyWorkerSlotsResponse = {
    try {
      endpoint.askSync[DestroyWorkerSlotsResponse](message)
    } catch {
      case e: Exception =>
        logError(s"AskSync Destroy for ${message.shuffleKey} failed.", e)
        DestroyWorkerSlotsResponse(
          StatusCode.REQUEST_FAILED,
          message.primaryLocations,
          message.replicaLocations)
    }
  }

  private def requestPrimaryReleaseSlots(message: ReleaseSlots): ReleaseSlotsResponse = {
    try {
      masterClient.askSync[ReleaseSlotsResponse](message, classOf[ReleaseSlotsResponse])
    } catch {
      case e: Exception =>
        logError(s"AskSync ReleaseSlots for ${message.shuffleId} failed.", e)
        ReleaseSlotsResponse(StatusCode.REQUEST_FAILED)
    }
  }

  private def requestPrimaryUnregisterShuffle(message: PbUnregisterShuffle)
      : PbUnregisterShuffleResponse = {
    try {
      masterClient.askSync[PbUnregisterShuffleResponse](
        message,
        classOf[PbUnregisterShuffleResponse])
    } catch {
      case e: Exception =>
        logError(s"AskSync UnregisterShuffle for ${message.getShuffleId} failed.", e)
        UnregisterShuffleResponse(StatusCode.REQUEST_FAILED)
    }
  }

  def checkQuota(): CheckQuotaResponse = {
    try {
      masterClient.askSync[CheckQuotaResponse](
        CheckQuota(userIdentifier),
        classOf[CheckQuotaResponse])
    } catch {
      case e: Exception =>
        val msg = s"AskSync Cluster check quota for $userIdentifier failed."
        logError(msg, e)
        CheckQuotaResponse(false, msg)
    }
  }

  private def shuffleResourceExists(shuffleId: Int): Boolean = {
    val workerPartitionInfos = workerSnapshots(shuffleId)
    workerPartitionInfos != null && workerPartitionInfos.values().asScala.exists(!_.isEmpty())
  }

  // Once a partition is released, it will be never needed anymore
  def releasePartition(shuffleId: Int, partitionId: Int): Unit = {
    commitManager.releasePartitionResource(shuffleId, partitionId)
    val partitionLocation = latestPartitionLocation.get(shuffleId)
    if (partitionLocation != null) {
      partitionLocation.remove(partitionId)
    }

    releasePartitionManager.releasePartition(shuffleId, partitionId)
  }

  def getAllocatedWorkers(): Set[WorkerInfo] = {
    shuffleAllocatedWorkers.asScala.values.flatMap(_.keys().asScala).toSet
  }

  // delegate workerStatusTracker to register listener
  def registerWorkerStatusListener(workerStatusListener: WorkerStatusListener): Unit = {
    workerStatusTracker.registerWorkerStatusListener(workerStatusListener)
  }

  // Initialize at the end of LifecycleManager construction.
  initialize()
}
