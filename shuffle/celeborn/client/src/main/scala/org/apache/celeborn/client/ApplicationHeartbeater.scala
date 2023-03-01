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

import java.util.concurrent.{ScheduledFuture, TimeUnit}

import scala.concurrent.duration.DurationInt

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.haclient.RssHARetryClient
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.protocol.message.ControlMessages.{HeartbeatFromApplication, ZERO_UUID}
import org.apache.celeborn.common.util.ThreadUtils

class ApplicationHeartbeater(
    appId: String,
    conf: CelebornConf,
    rssHARetryClient: RssHARetryClient,
    shuffleMetrics: () => (Long, Long)) extends Logging {

  // Use independent app heartbeat threads to avoid being blocked by other operations.
  private val appHeartbeatIntervalMs = conf.appHeartbeatIntervalMs
  private val appHeartbeatHandlerThread =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("app-heartbeat")
  private var appHeartbeat: ScheduledFuture[_] = _

  def start(): Unit = {
    appHeartbeat = appHeartbeatHandlerThread.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = {
          try {
            require(rssHARetryClient != null, "When sending a heartbeat, client shouldn't be null.")
            val (tmpTotalWritten, tmpFileCount) = shuffleMetrics()
            logDebug(s"Send app heartbeat with $tmpTotalWritten $tmpFileCount")
            val appHeartbeat =
              HeartbeatFromApplication(appId, tmpTotalWritten, tmpFileCount, ZERO_UUID)
            rssHARetryClient.send(appHeartbeat)
            logDebug("Successfully send app heartbeat.")
          } catch {
            case it: InterruptedException =>
              logWarning("Interrupted while sending app heartbeat.")
              Thread.currentThread().interrupt()
              throw it
            case t: Throwable =>
              logError("Error while send heartbeat", t)
          }
        }
      },
      0,
      appHeartbeatIntervalMs,
      TimeUnit.MILLISECONDS)
  }

  def stop(): Unit = {
    appHeartbeat.cancel(true)
    ThreadUtils.shutdown(appHeartbeatHandlerThread, 800.millis)
  }
}
