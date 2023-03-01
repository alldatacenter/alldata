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
import org.apache.celeborn.common.util.Utils

class CelebornConfSuite extends CelebornFunSuite {

  test("celeborn.master.endpoints support multi nodes") {
    val conf = new CelebornConf()
      .set("celeborn.master.endpoints", "localhost1:9097,localhost2:9097")
    val masterEndpoints = conf.masterEndpoints
    assert(masterEndpoints.length == 2)
    assert(masterEndpoints(0) == "localhost1:9097")
    assert(masterEndpoints(1) == "localhost2:9097")
  }

  test("storage test") {
    val conf = new CelebornConf()
    val defaultMaxUsableSpace = 1024L * 1024 * 1024 * 1024 * 1024
    conf.set("celeborn.worker.storage.dirs", "/mnt/disk1")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 1)
    assert(workerBaseDirs.head._3 == 2)
    assert(workerBaseDirs.head._2 == defaultMaxUsableSpace)
  }

  test("storage test2") {
    val conf = new CelebornConf()
    val defaultMaxUsableSpace = 1024L * 1024 * 1024 * 1024 * 1024
    conf.set("celeborn.worker.storage.dirs", "/mnt/disk1:disktype=SSD:capacity=10g")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 1)
    assert(workerBaseDirs.head._3 == 8)
    assert(workerBaseDirs.head._2 == 10 * 1024 * 1024 * 1024L)
  }

  test("storage test3") {
    val conf = new CelebornConf()
    conf.set("celeborn.worker.storage.dirs", "/mnt/disk1:disktype=SSD:capacity=10g:flushthread=3")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.size == 1)
    assert(workerBaseDirs.head._3 == 3)
    assert(workerBaseDirs.head._2 == 10 * 1024 * 1024 * 1024L)
  }

  test("storage test4") {
    val conf = new CelebornConf()
    conf.set(
      "celeborn.worker.storage.dirs",
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
    conf.set("celeborn.worker.storage.dirs", "/mnt/disk1")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 2)
  }

  test("storage test6") {
    val conf = new CelebornConf()
    conf.set("celeborn.worker.flusher.threads", "4")
      .set("celeborn.worker.storage.dirs", "/mnt/disk1")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 4)
  }

  test("storage test7") {
    val conf = new CelebornConf()
    conf.set("celeborn.worker.flusher.threads", "4")
      .set("celeborn.worker.storage.dirs", "/mnt/disk1:flushthread=8")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 8)
  }

  test("storage test8") {
    val conf = new CelebornConf()
    conf.set("celeborn.worker.flusher.threads", "4")
      .set("celeborn.worker.storage.dirs", "/mnt/disk1:disktype=SSD")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 8)
  }

  test("storage test9") {
    val conf = new CelebornConf()
    conf.set("celeborn.worker.flusher.threads", "4")
      .set("celeborn.worker.storage.dirs", "/mnt/disk1:flushthread=9:disktype=HDD")
    val workerBaseDirs = conf.workerBaseDirs
    assert(workerBaseDirs.head._3 == 9)
  }

  test("zstd level") {
    val conf = new CelebornConf()
    val error1 = intercept[IllegalArgumentException] {
      conf.set("celeborn.shuffle.compression.zstd.level", "-100")
      assert(conf.shuffleCompressionZstdCompressLevel == -100)
    }.getMessage
    assert(error1.contains("'-100' in celeborn.shuffle.compression.zstd.level is invalid. " +
      "Compression level for Zstd compression codec should be an integer between -5 and 22."))
    conf.set("celeborn.shuffle.compression.zstd.level", "-5")
    assert(conf.shuffleCompressionZstdCompressLevel == -5)
    conf.set("celeborn.shuffle.compression.zstd.level", "0")
    assert(conf.shuffleCompressionZstdCompressLevel == 0)
    conf.set("celeborn.shuffle.compression.zstd.level", "22")
    assert(conf.shuffleCompressionZstdCompressLevel == 22)
    val error2 = intercept[IllegalArgumentException] {
      conf.set("celeborn.shuffle.compression.zstd.level", "100")
      assert(conf.shuffleCompressionZstdCompressLevel == 100)
    }.getMessage
    assert(error2.contains("'100' in celeborn.shuffle.compression.zstd.level is invalid. " +
      "Compression level for Zstd compression codec should be an integer between -5 and 22."))
  }

  test("replace <localhost> placeholder") {
    val conf = new CelebornConf()
    val replacedHost = conf.masterHost
    assert(!replacedHost.contains("<localhost>"))
    assert(replacedHost === Utils.localHostName)
    val replacedHosts = conf.masterEndpoints
    replacedHosts.foreach { replacedHost =>
      assert(!replacedHost.contains("<localhost>"))
      assert(replacedHost contains Utils.localHostName)
    }
  }

  test("extract masterNodeIds") {
    val conf = new CelebornConf()
      .set("celeborn.ha.master.node.id", "1")
      .set("celeborn.ha.master.node.1.host", "clb-1")
      .set("celeborn.ha.master.node.2.host", "clb-1")
      .set("celeborn.ha.master.node.3.host", "clb-1")
    assert(conf.haMasterNodeIds.sorted === Array("1", "2", "3"))
  }
}
