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

package org.apache.celeborn.tests.client

import java.io.IOException

import org.apache.celeborn.client.{LifecycleManager, ShuffleClientImpl, WithShuffleClientSuite}
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.service.deploy.MiniClusterFeature

class ShuffleClientSuite extends WithShuffleClientSuite with MiniClusterFeature {
  private val masterPort = 19097

  celebornConf.set(CelebornConf.MASTER_ENDPOINTS.key, s"localhost:$masterPort")
    .set(CelebornConf.CLIENT_PUSH_REPLICATE_ENABLED.key, "true")
    .set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE.key, "256K")

  override def beforeAll(): Unit = {
    super.beforeAll()
    val masterConf = Map(
      "celeborn.master.host" -> "localhost",
      "celeborn.master.port" -> masterPort.toString)
    val workerConf = Map(
      "celeborn.master.endpoints" -> s"localhost:$masterPort")
    setUpMiniCluster(masterConf, workerConf)
  }

  test("test register when master not available") {
    val celebornConf: CelebornConf = new CelebornConf()
    celebornConf.set(CelebornConf.MASTER_ENDPOINTS.key, "localhost:19098")
    celebornConf.set(CelebornConf.MASTER_CLIENT_MAX_RETRIES.key, "0")

    val lifecycleManager: LifecycleManager = new LifecycleManager(APP, celebornConf)
    val shuffleClient: ShuffleClientImpl = {
      val client = new ShuffleClientImpl(APP, celebornConf, userIdentifier, false)
      client.setupLifecycleManagerRef(lifecycleManager.self)
      client
    }

    assertThrows[IOException] {
      shuffleClient.registerMapPartitionTask(1, 1, 0, 0, 1)
    }

    lifecycleManager.stop()
  }

  override def afterAll(): Unit = {
    logInfo("all test complete , stop celeborn mini cluster")
    shutdownMiniCluster()
  }
}
