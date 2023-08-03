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

package org.apache.celeborn.common

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.CelebornConf._
import org.apache.celeborn.common.util.Utils

class CelebornConfSuite extends CelebornFunSuite {

  test("celeborn.master.endpoints support multi nodes") {
    val conf = new CelebornConf()
      .set(CelebornConf.MASTER_ENDPOINTS.key, "localhost1:9097,localhost2:9097")
    val masterEndpoints = conf.masterEndpoints
    assert(masterEndpoints.length == 2)
    assert(masterEndpoints(0) == "localhost1:9097")
    assert(masterEndpoints(1) == "localhost2:9097")
  }

  test("storage test") {
    val conf = new CelebornConf()
    val defaultMaxUsableSpace = 1024L * 1024 * 1024 * 1024 * 1024
    conf.set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 1)
    assert(workerBaseDirs.head._3 == 16)
    assert(workerBaseDirs.head._2 == defaultMaxUsableSpace)
  }

  test("storage test2") {
    val conf = new CelebornConf()
    conf.set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1:disktype=SSD:capacity=10g")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 1)
    assert(workerBaseDirs.head._3 == 16)
    assert(workerBaseDirs.head._2 == 10 * 1024 * 1024 * 1024L)
  }

  test("storage test3") {
    val conf = new CelebornConf()
    conf.set(
      CelebornConf.WORKER_STORAGE_DIRS.key,
      "/mnt/disk1:disktype=SSD:capacity=10g:flushthread=3")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 1)
    assert(workerBaseDirs.head._3 == 3)
    assert(workerBaseDirs.head._2 == 10 * 1024 * 1024 * 1024L)
  }

  test("storage test4") {
    val conf = new CelebornConf()
    conf.set(
      CelebornConf.WORKER_STORAGE_DIRS.key,
      "/mnt/disk1:disktype=SSD:capacity=10g:flushthread=3," +
        "/mnt/disk2:disktype=HDD:capacity=15g:flushthread=7")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 2)
    assert(workerBaseDirs.head._1 == "/mnt/disk1")
    assert(workerBaseDirs.head._3 == 3)
    assert(workerBaseDirs.head._2 == 10 * 1024 * 1024 * 1024L)

    assert(workerBaseDirs(1)._1 == "/mnt/disk2")
    assert(workerBaseDirs(1)._3 == 7)
    assert(workerBaseDirs(1)._2 == 15 * 1024 * 1024 * 1024L)
  }

  test("storage test5") {
    val conf = new CelebornConf()
    conf.set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 16)
  }

  test("storage test6") {
    val conf = new CelebornConf()
    conf.set(CelebornConf.WORKER_FLUSHER_THREADS.key, "4")
      .set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 4)
  }

  test("storage test7") {
    val conf = new CelebornConf()
    conf.set(CelebornConf.WORKER_FLUSHER_THREADS.key, "4")
      .set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1:flushthread=8")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 8)
  }

  test("storage test8") {
    val conf = new CelebornConf()
    conf.set(CelebornConf.WORKER_FLUSHER_THREADS.key, "4")
      .set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1:disktype=SSD")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 16)
  }

  test("storage test9") {
    val conf = new CelebornConf()
    conf.set(CelebornConf.WORKER_FLUSHER_THREADS.key, "4")
      .set(CelebornConf.WORKER_STORAGE_DIRS.key, "/mnt/disk1:flushthread=9:disktype=HDD")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 9)
  }

  test("zstd level") {
    val conf = new CelebornConf()
    val error1 = intercept[IllegalArgumentException] {
      conf.set(CelebornConf.SHUFFLE_COMPRESSION_ZSTD_LEVEL.key, "-100")
      assert(conf.shuffleCompressionZstdCompressLevel == -100)
    }.getMessage
    assert(error1.contains(s"'-100' in ${SHUFFLE_COMPRESSION_ZSTD_LEVEL.key} is invalid. " +
      "Compression level for Zstd compression codec should be an integer between -5 and 22."))
    conf.set(CelebornConf.SHUFFLE_COMPRESSION_ZSTD_LEVEL.key, "-5")
    assert(conf.shuffleCompressionZstdCompressLevel == -5)
    conf.set(CelebornConf.SHUFFLE_COMPRESSION_ZSTD_LEVEL.key, "0")
    assert(conf.shuffleCompressionZstdCompressLevel == 0)
    conf.set(CelebornConf.SHUFFLE_COMPRESSION_ZSTD_LEVEL.key, "22")
    assert(conf.shuffleCompressionZstdCompressLevel == 22)
    val error2 = intercept[IllegalArgumentException] {
      conf.set(CelebornConf.SHUFFLE_COMPRESSION_ZSTD_LEVEL.key, "100")
      assert(conf.shuffleCompressionZstdCompressLevel == 100)
    }.getMessage
    assert(error2.contains(s"'100' in ${SHUFFLE_COMPRESSION_ZSTD_LEVEL.key} is invalid. " +
      "Compression level for Zstd compression codec should be an integer between -5 and 22."))
  }

  test("replace <localhost> placeholder") {
    val conf = new CelebornConf()
    val replacedHost = conf.masterHost
    assert(!replacedHost.contains("<localhost>"))
    assert(replacedHost === Utils.localHostName(conf))
    val replacedHosts = conf.masterEndpoints
    replacedHosts.foreach { replacedHost =>
      assert(!replacedHost.contains("<localhost>"))
      assert(replacedHost contains Utils.localHostName(conf))
    }
  }

  test("extract masterNodeIds") {
    val conf = new CelebornConf()
      .set("celeborn.master.ha.node.id", "1")
      .set("celeborn.master.ha.node.1.host", "clb-1")
      .set("celeborn.master.ha.node.2.host", "clb-1")
      .set("celeborn.master.ha.node.3.host", "clb-1")
    assert(conf.haMasterNodeIds.sorted === Array("1", "2", "3"))
  }

  test("CELEBORN-593: Test each RPC timeout value") {
    val conf = new CelebornConf()
      .set(CelebornConf.RPC_ASK_TIMEOUT, 1000L)
      .set(CelebornConf.NETWORK_TIMEOUT, 20000L)
      .set(CelebornConf.NETWORK_CONNECT_TIMEOUT, 2000L)

    assert(conf.rpcAskTimeout.duration.toMillis == 1000L)
    assert(conf.masterClientRpcAskTimeout.duration.toMillis == 1000L)
    assert(conf.clientRpcReserveSlotsRpcTimeout.duration.toMillis == 1000L)
    assert(conf.networkTimeout.duration.toMillis == 20000L)
    assert(conf.networkIoConnectionTimeoutMs("data") == 20000L)
    assert(conf.clientPushStageEndTimeout == 20000L)
    assert(conf.clientRpcRegisterShuffleRpcAskTimeout.duration.toMillis == 20000L)
    assert(conf.clientRpcRequestPartitionLocationRpcAskTimeout.duration.toMillis == 20000L)
    assert(conf.clientRpcGetReducerFileGroupRpcAskTimeout.duration.toMillis == 20000L)
    assert(conf.networkConnectTimeout.duration.toMillis == 2000L)
    assert(conf.networkIoConnectTimeoutMs("data") == 2000L)
  }

  test("CELEBORN-601: Consolidate configsWithAlternatives with `ConfigBuilder.withAlternative`") {
    val conf = new CelebornConf()
      .set(CelebornConf.TEST_ALTERNATIVE.alternatives.head._1, "celeborn")

    assert(conf.testAlternative == "celeborn")
  }

  test("Test empty working dir") {
    val conf = new CelebornConf()
    conf.set("celeborn.storage.activeTypes", "HDFS")
    conf.set("celeborn.storage.hdfs.dir", "hdfs:///xxx")
    assert(conf.workerBaseDirs.isEmpty)

    conf.set("celeborn.storage.activeTypes", "SDD,HDD,HDFS")
    conf.set("celeborn.storage.hdfs.dir", "hdfs:///xxx")
    assert(conf.workerBaseDirs.isEmpty)

    conf.set("celeborn.storage.activeTypes", "SDD,HDD")
    assert(!conf.workerBaseDirs.isEmpty)
  }

  test("Test commit file threads") {
    val conf = new CelebornConf()
    conf.set("celeborn.storage.activeTypes", "HDFS")
    conf.set("celeborn.storage.hdfs.dir", "hdfs:///xxx")
    assert(conf.workerCommitThreads === 128)

    conf.set("celeborn.storage.activeTypes", "SDD,HDD")
    assert(conf.workerCommitThreads === 32)
  }
}
