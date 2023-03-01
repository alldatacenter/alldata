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

package org.apache.celeborn.service.deploy.cluster

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.client.{LifecycleManager, ShuffleClientImpl}
import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.protocol.CompressionCodec
import org.apache.celeborn.service.deploy.MiniClusterFeature

trait ReadWriteTestBase extends AnyFunSuite
  with Logging with MiniClusterFeature with BeforeAndAfterAll {
  val masterPort = 19097

  override def beforeAll(): Unit = {
    val masterConf = Map(
      "celeborn.master.host" -> "localhost",
      "celeborn.master.port" -> masterPort.toString)
    val workerConf = Map(
      "celeborn.master.endpoints" -> s"localhost:$masterPort")
    logInfo("test initialized , setup rss mini cluster")
    setUpMiniCluster(masterConf, workerConf)
  }

  override def afterAll(): Unit = {
    logInfo("all test complete , stop rss mini cluster")
    shutdownMiniCluster()
  }

  def testReadWriteByCode(codec: CompressionCodec): Unit = {
    val APP = "app-1"

    val clientConf = new CelebornConf()
      .set("celeborn.master.endpoints", s"localhost:$masterPort")
      .set("celeborn.shuffle.compression.codec", codec.name)
      .set("celeborn.push.replicate.enabled", "true")
      .set("celeborn.push.buffer.size", "256K")
      .set("celeborn.data.io.numConnectionsPerPeer", "1")
    val lifecycleManager = new LifecycleManager(APP, clientConf)
    val shuffleClient = new ShuffleClientImpl(clientConf, UserIdentifier("mock", "mock"))
    shuffleClient.setupMetaServiceRef(lifecycleManager.self)

    val STR1 = RandomStringUtils.random(1024)
    val DATA1 = STR1.getBytes(StandardCharsets.UTF_8)
    val OFFSET1 = 0
    val LENGTH1 = DATA1.length

    val dataSize1 = shuffleClient.pushData(APP, 1, 0, 0, 0, DATA1, OFFSET1, LENGTH1, 1, 1)
    logInfo(s"push data data size $dataSize1")

    val STR2 = RandomStringUtils.random(32 * 1024)
    val DATA2 = STR2.getBytes(StandardCharsets.UTF_8)
    val OFFSET2 = 0
    val LENGTH2 = DATA2.length
    val dataSize2 = shuffleClient.pushData(APP, 1, 0, 0, 0, DATA2, OFFSET2, LENGTH2, 1, 1)
    logInfo("push data data size " + dataSize2)

    val STR3 = RandomStringUtils.random(32 * 1024)
    val DATA3 = STR3.getBytes(StandardCharsets.UTF_8)
    val LENGTH3 = DATA3.length
    shuffleClient.mergeData(APP, 1, 0, 0, 0, DATA3, 0, LENGTH3, 1, 1)

    val STR4 = RandomStringUtils.random(16 * 1024)
    val DATA4 = STR4.getBytes(StandardCharsets.UTF_8)
    val LENGTH4 = DATA4.length
    shuffleClient.mergeData(APP, 1, 0, 0, 0, DATA4, 0, LENGTH4, 1, 1)
    shuffleClient.pushMergedData(APP, 1, 0, 0)
    Thread.sleep(1000)

    shuffleClient.mapperEnd(APP, 1, 0, 0, 1)

    val inputStream = shuffleClient.readPartition(APP, 1, 0, 0)
    val outputStream = new ByteArrayOutputStream()

    var b = inputStream.read()
    while (b != -1) {
      outputStream.write(b)
      b = inputStream.read()
    }

    val readBytes = outputStream.toByteArray
    Assert.assertEquals(LENGTH1 + LENGTH2 + LENGTH3 + LENGTH4, readBytes.length)
    val targetArr = Array.concat(DATA1, DATA2, DATA3, DATA4)
    Assert.assertArrayEquals(targetArr, readBytes)

    Thread.sleep(5000L)
    shuffleClient.shutdown()
    lifecycleManager.rpcEnv.shutdown()

  }

}
