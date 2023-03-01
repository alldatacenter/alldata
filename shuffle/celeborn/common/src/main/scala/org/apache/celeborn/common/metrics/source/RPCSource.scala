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

package org.apache.celeborn.common.metrics.source

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.network.protocol.{ChunkFetchRequest, PushData, PushMergedData}
import org.apache.celeborn.common.protocol.{PbRegisterWorker, PbUnregisterShuffle}
import org.apache.celeborn.common.protocol.message.ControlMessages._

class RPCSource(conf: CelebornConf, role: String) extends AbstractSource(conf, role) {
  override val sourceName = "rpc"

  import RPCSource._

  // Worker RPC
  addCounter(RPCReserveSlotsNum)
  addCounter(RPCReserveSlotsSize)
  addCounter(RPCCommitFilesNum)
  addCounter(RPCCommitFilesSize)
  addCounter(RPCDestroyNum)
  addCounter(RPCDestroySize)
  addCounter(RPCPushDataNum)
  addCounter(RPCPushDataSize)
  addCounter(RPCPushMergedDataNum)
  addCounter(RPCPushMergedDataSize)
  addCounter(RPCChunkFetchRequestNum)

  // Master RPC
  addCounter(RPCHeartbeatFromApplicationNum)
  addCounter(RPCHeartbeatFromWorkerNum)
  addCounter(RPCRegisterWorkerNum)
  addCounter(RPCRequestSlotsNum)
  addCounter(RPCReleaseSlotsNum)
  addCounter(RPCReleaseSlotsSize)
  addCounter(RPCUnregisterShuffleNum)
  addCounter(RPCGetBlacklistNum)
  addCounter(RPCReportWorkerUnavailableNum)
  addCounter(RPCReportWorkerUnavailableSize)
  addCounter(RPCCheckQuotaNum)

  def updateMessageMetrics(message: Any, messageLen: Long): Unit = {
    message match {
      case _: ReserveSlots =>
        incCounter(RPCReserveSlotsNum)
        incCounter(RPCReserveSlotsSize, messageLen)
      case _: CommitFiles =>
        incCounter(RPCCommitFilesNum)
        incCounter(RPCCommitFilesSize, messageLen)
      case _: Destroy =>
        incCounter(RPCDestroyNum)
        incCounter(RPCDestroySize, messageLen)
      case _: PushData =>
        incCounter(RPCPushDataNum)
        incCounter(RPCPushDataSize, messageLen)
      case _: PushMergedData =>
        incCounter(RPCPushMergedDataNum)
        incCounter(RPCPushMergedDataSize, messageLen)
      case _: ChunkFetchRequest =>
        incCounter(RPCChunkFetchRequestNum)
      case _: HeartbeatFromApplication =>
        incCounter(RPCHeartbeatFromApplicationNum)
      case _: HeartbeatFromWorker =>
        incCounter(RPCHeartbeatFromWorkerNum)
      case _: PbRegisterWorker =>
        incCounter(RPCRegisterWorkerNum)
      case _: RequestSlots =>
        incCounter(RPCRequestSlotsNum)
      case _: ReleaseSlots =>
        incCounter(RPCReleaseSlotsNum)
        incCounter(RPCReleaseSlotsSize, messageLen)
      case _: PbUnregisterShuffle =>
        incCounter(RPCUnregisterShuffleNum)
      case _: GetBlacklist =>
        incCounter(RPCGetBlacklistNum)
      case _: ReportWorkerUnavailable =>
        incCounter(RPCReportWorkerUnavailableNum)
        incCounter(RPCReportWorkerUnavailableSize, messageLen)
      case CheckQuota =>
        incCounter(RPCCheckQuotaNum)
      case _ => // Do nothing
    }
  }
}

object RPCSource {
  // Worker RPC
  val RPCReserveSlotsNum = "RPCReserveSlotsNum"
  val RPCReserveSlotsSize = "RPCReserveSlotsSize"
  val RPCCommitFilesNum = "RPCCommitFilesNum"
  val RPCCommitFilesSize = "RPCCommitFilesSize"
  val RPCDestroyNum = "RPCDestroyNum"
  val RPCDestroySize = "RPCDestroySize"
  val RPCPushDataNum = "RPCPushDataNum"
  val RPCPushDataSize = "RPCPushDataSize"
  val RPCPushMergedDataNum = "RPCPushMergedDataNum"
  val RPCPushMergedDataSize = "RPCPushMergedDataSize"
  val RPCChunkFetchRequestNum = "RPCChunkFetchRequestNum"

  // Master RPC
  val RPCHeartbeatFromApplicationNum = "RPCHeartbeatFromApplicationNum"
  val RPCHeartbeatFromWorkerNum = "RPCHeartbeatFromWorkerNum"
  val RPCRegisterWorkerNum = "RPCRegisterWorkerNum"
  val RPCRequestSlotsNum = "RPCRequestSlotsNum"
  val RPCReleaseSlotsNum = "RPCReleaseSlotsNum"
  val RPCReleaseSlotsSize = "RPCReleaseSlotsSize"
  val RPCUnregisterShuffleNum = "RPCUnregisterShuffleNum"
  val RPCGetBlacklistNum = "RPCGetBlacklistNum"
  val RPCReportWorkerUnavailableNum = "RPCReportWorkerUnavailableNum"
  val RPCReportWorkerUnavailableSize = "RPCReportWorkerUnavailableSize"
  val RPCCheckQuotaNum = "RPCCheckQuotaNum"
}
