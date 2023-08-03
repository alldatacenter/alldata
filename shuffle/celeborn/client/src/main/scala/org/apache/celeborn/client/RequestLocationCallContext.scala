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
import java.util.concurrent.ConcurrentHashMap

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.protocol.PartitionLocation
import org.apache.celeborn.common.protocol.message.ControlMessages.{ChangeLocationResponse, RegisterShuffleResponse}
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.rpc.RpcCallContext

trait RequestLocationCallContext {
  def reply(
      partitionId: Int,
      status: StatusCode,
      partitionLocationOpt: Option[PartitionLocation],
      available: Boolean): Unit
}

case class ChangeLocationsCallContext(
    context: RpcCallContext,
    partitionCount: Int)
  extends RequestLocationCallContext with Logging {
  val endedMapIds = new util.HashSet[Integer]()
  val newLocs =
    new ConcurrentHashMap[Integer, (StatusCode, Boolean, PartitionLocation)](partitionCount)

  def markMapperEnd(mapId: Int): Unit = this.synchronized {
    endedMapIds.add(mapId)
  }

  override def reply(
      partitionId: Int,
      status: StatusCode,
      partitionLocationOpt: Option[PartitionLocation],
      available: Boolean): Unit = this.synchronized {
    if (newLocs.containsKey(partitionId)) {
      logError(s"PartitionId $partitionId already exists!")
    }
    newLocs.put(partitionId, (status, available, partitionLocationOpt.getOrElse(null)))

    if (newLocs.size() == partitionCount || StatusCode.SHUFFLE_NOT_REGISTERED == status
      || StatusCode.STAGE_ENDED == status) {
      context.reply(ChangeLocationResponse(endedMapIds, newLocs))
    }
  }
}

case class ApplyNewLocationCallContext(context: RpcCallContext) extends RequestLocationCallContext {
  override def reply(
      partitionId: Int,
      status: StatusCode,
      partitionLocationOpt: Option[PartitionLocation],
      available: Boolean): Unit = {
    partitionLocationOpt match {
      case Some(partitionLocation) =>
        context.reply(RegisterShuffleResponse(status, Array(partitionLocation)))
      case None => context.reply(RegisterShuffleResponse(status, Array.empty))
    }
  }
}
