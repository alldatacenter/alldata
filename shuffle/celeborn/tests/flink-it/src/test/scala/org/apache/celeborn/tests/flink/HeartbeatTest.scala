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

package org.apache.celeborn.tests.flink

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl
import org.apache.celeborn.service.deploy.{HeartbeatFeature, MiniClusterFeature}
import org.apache.celeborn.service.deploy.worker.memory.MemoryManager;

class HeartbeatTest extends AnyFunSuite with Logging with MiniClusterFeature with HeartbeatFeature
  with BeforeAndAfterAll with BeforeAndAfterEach {

  test("celeborn flink hearbeat test - client <- worker") {
    val (_, clientConf) = getTestHeartbeatFromWorker2ClientConf
    val flinkShuffleClientImpl =
      new FlinkShuffleClientImpl(
        "",
        "",
        0,
        System.currentTimeMillis(),
        clientConf,
        new UserIdentifier("1", "1")) {
        override def setupLifecycleManagerRef(host: String, port: Int): Unit = {}
      }
    testHeartbeatFromWorker2Client(flinkShuffleClientImpl.getDataClientFactory)
  }

  test("celeborn flink hearbeat test - client <- worker no heartbeat") {
    val (_, clientConf) = getTestHeartbeatFromWorker2ClientWithNoHeartbeatConf
    val flinkShuffleClientImpl =
      new FlinkShuffleClientImpl(
        "",
        "",
        0,
        System.currentTimeMillis(),
        clientConf,
        new UserIdentifier("1", "1")) {
        override def setupLifecycleManagerRef(host: String, port: Int): Unit = {}
      }
    testHeartbeatFromWorker2ClientWithNoHeartbeat(flinkShuffleClientImpl.getDataClientFactory)
  }

  test("celeborn flink hearbeat test - client <- worker timeout") {
    val (_, clientConf) = getTestHeartbeatFromWorker2ClientWithCloseChannelConf
    val flinkShuffleClientImpl =
      new FlinkShuffleClientImpl(
        "",
        "",
        0,
        System.currentTimeMillis(),
        clientConf,
        new UserIdentifier("1", "1")) {
        override def setupLifecycleManagerRef(host: String, port: Int): Unit = {}
      }
    testHeartbeatFromWorker2ClientWithCloseChannel(flinkShuffleClientImpl.getDataClientFactory)
  }
}
