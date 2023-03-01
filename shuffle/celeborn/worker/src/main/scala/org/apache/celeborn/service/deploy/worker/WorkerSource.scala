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

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.metrics.MetricsSystem
import org.apache.celeborn.common.metrics.source.AbstractSource

class WorkerSource(conf: CelebornConf) extends AbstractSource(conf, MetricsSystem.ROLE_WORKER) {
  override val sourceName = "worker"

  import WorkerSource._
  // add counters
  addCounter(PushDataFailCount)

  // add Timers
  addTimer(CommitFilesTime)
  addTimer(ReserveSlotsTime)
  addTimer(FlushDataTime)
  addTimer(MasterPushDataTime)
  addTimer(SlavePushDataTime)

  addTimer(MasterPushDataHandshakeTime)
  addTimer(SlavePushDataHandshakeTime)
  addTimer(MasterRegionStartTime)
  addTimer(SlaveRegionStartTime)
  addTimer(MasterRegionFinishTime)
  addTimer(SlaveRegionFinishTime)

  addTimer(FetchChunkTime)
  addTimer(OpenStreamTime)
  addTimer(TakeBufferTime)
  addTimer(SortTime)

  // start cleaner thread
  startCleaner()
}

object WorkerSource {
  val ServletPath = "/metrics/prometheus"

  val CommitFilesTime = "CommitFilesTime"

  val ReserveSlotsTime = "ReserveSlotsTime"

  val FlushDataTime = "FlushDataTime"

  val OpenStreamTime = "OpenStreamTime"

  val FetchChunkTime = "FetchChunkTime"

  // push data
  val MasterPushDataTime = "MasterPushDataTime"
  val SlavePushDataTime = "SlavePushDataTime"
  val PushDataFailCount = "PushDataFailCount"
  val PushDataHandshakeFailCount = "PushDataHandshakeFailCount"
  val RegionStartFailCount = "RegionStartFailCount"
  val RegionFinishFailCount = "RegionFinishFailCount"
  val MasterPushDataHandshakeTime = "MasterPushDataHandshakeTime"
  val SlavePushDataHandshakeTime = "SlavePushDataHandshakeTime"
  val MasterRegionStartTime = "MasterRegionStartTime"
  val SlaveRegionStartTime = "SlaveRegionStartTime"
  val MasterRegionFinishTime = "MasterRegionFinishTime"
  val SlaveRegionFinishTime = "SlaveRegionFinishTime"

  // flush
  val TakeBufferTime = "TakeBufferTime"
  val TakeBufferTimeIndex = "TakeBufferTimeIndex"

  val RegisteredShuffleCount = "RegisteredShuffleCount"

  // slots
  val SlotsAllocated = "SlotsAllocated"

  // memory
  val NettyMemory = "NettyMemory"
  val SortTime = "SortTime"
  val SortMemory = "SortMemory"
  val SortingFiles = "SortingFiles"
  val SortedFiles = "SortedFiles"
  val SortedFileSize = "SortedFileSize"
  val DiskBuffer = "DiskBuffer"
  val PausePushDataCount = "PausePushData"
  val PausePushDataAndReplicateCount = "PausePushDataAndReplicate"
  val BufferStreamReadBuffer = "BufferStreamReadBuffer"

  // local device
  val DeviceOSFreeCapacity = "DeviceOSFreeCapacity(B)"
  val DeviceOSTotalCapacity = "DeviceOSTotalCapacity(B)"
  val DeviceCelebornFreeCapacity = "DeviceCelebornFreeCapacity(B)"
  val DeviceCelebornTotalCapacity = "DeviceCelebornTotalCapacity(B)"

  // Congestion control
  val PotentialConsumeSpeed = "PotentialConsumeSpeed"
  val UserProduceSpeed = "UserProduceSpeed"
}
