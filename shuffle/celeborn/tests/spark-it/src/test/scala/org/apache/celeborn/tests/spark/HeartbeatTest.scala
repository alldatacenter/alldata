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

package org.apache.celeborn.tests.spark

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.client.{ShuffleClient, ShuffleClientImpl}
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.service.deploy.{HeartbeatFeature, MiniClusterFeature};

class HeartbeatTest extends AnyFunSuite with Logging with MiniClusterFeature with HeartbeatFeature
  with BeforeAndAfterAll with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    ShuffleClient.reset()
  }

  override def afterEach(): Unit = {
    System.gc()
  }

  test("celeborn spark heartbeat test - client <- worker") {
    val (_, clientConf) = getTestHeartbeatFromWorker2ClientConf
    val shuffleClientImpl =
      new ShuffleClientImpl("APP", clientConf, new UserIdentifier("1", "1"), false)
    testHeartbeatFromWorker2Client(shuffleClientImpl.getDataClientFactory)
  }

  test("celeborn spark heartbeat test - client <- worker on heartbeat") {
    val (_, clientConf) = getTestHeartbeatFromWorker2ClientWithNoHeartbeatConf
    val shuffleClientImpl =
      new ShuffleClientImpl("APP", clientConf, new UserIdentifier("1", "1"), false)
    testHeartbeatFromWorker2ClientWithNoHeartbeat(shuffleClientImpl.getDataClientFactory)
  }

  test("celeborn spark heartbeat test - client <- worker timeout") {
    val (_, clientConf) = getTestHeartbeatFromWorker2ClientWithCloseChannelConf
    val shuffleClientImpl =
      new ShuffleClientImpl("APP", clientConf, new UserIdentifier("1", "1"), false)
    testHeartbeatFromWorker2ClientWithCloseChannel(shuffleClientImpl.getDataClientFactory)
  }
}
