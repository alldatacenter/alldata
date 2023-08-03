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

import java.util.concurrent.{ConcurrentHashMap, ExecutorService}

import scala.util.{Failure, Success}

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.protocol.{PartitionLocation, PbChangeLocationResponse, PbPartitionSplit}
import org.apache.celeborn.common.protocol.message.StatusCode
import org.apache.celeborn.common.rpc.RpcEndpointRef
import org.apache.celeborn.common.util.{PbSerDeUtils, Utils}

object ShuffleClientHelper extends Logging {
  def sendShuffleSplitAsync(
      endpointRef: RpcEndpointRef,
      conf: CelebornConf,
      req: PbPartitionSplit,
      executors: ExecutorService,
      splittingSet: java.util.Set[Integer],
      partitionId: Int,
      shuffleId: Int,
      shuffleLocs: ConcurrentHashMap[Integer, PartitionLocation]): Unit = {
    endpointRef.ask[PbChangeLocationResponse](
      req,
      conf.clientRpcRequestPartitionLocationRpcAskTimeout).onComplete {
      case Success(resp) =>
        val partitionInfo = resp.getPartitionInfo(0)
        val respStatus = Utils.toStatusCode(partitionInfo.getStatus)
        if (respStatus == StatusCode.SUCCESS) {
          shuffleLocs.put(
            partitionId,
            PbSerDeUtils.fromPbPartitionLocation(partitionInfo.getPartition))
        } else if (respStatus == StatusCode.STAGE_ENDED) {
          logInfo(s"Stage ended for $shuffleId")
        } else {
          logInfo(s"split failed for $respStatus, " +
            s"shuffle file can be larger than expected, try split again");
        }
        splittingSet.remove(partitionId)
      case Failure(exception) =>
        splittingSet.remove(partitionId)
        logWarning(
          s"Shuffle file split failed for map ${shuffleId} partitionId ${partitionId}," +
            s" try again, detail : {}",
          exception);

    }(concurrent.ExecutionContext.fromExecutorService(executors))
  }
}
