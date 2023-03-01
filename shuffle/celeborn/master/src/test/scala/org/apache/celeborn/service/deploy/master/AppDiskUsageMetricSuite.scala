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

package org.apache.celeborn.service.deploy.master

import java.util

import scala.util.Random

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{AppDiskUsageMetric, AppDiskUsageSnapShot, WorkerInfo}

class AppDiskUsageMetricSuite extends AnyFunSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Logging {
  val WORKER1 = new WorkerInfo("host1", 111, 112, 113, 114)
  val WORKER2 = new WorkerInfo("host2", 211, 212, 213, 214)
  val WORKER3 = new WorkerInfo("host3", 311, 312, 313, 314)

  test("test snapshot ordering") {
    val snapShot = new AppDiskUsageSnapShot(50)
    val rand = new Random()
    for (i <- 1 to 60) {
      snapShot.updateAppDiskUsage(s"app-${i}", rand.nextInt(100000000) + 1)
    }
    println(snapShot.toString)
  }

  test("test snapshot ordering with duplicate entries") {
    val snapShot = new AppDiskUsageSnapShot(50)
    val rand = new Random()
    for (i <- 1 to 60) {
      snapShot.updateAppDiskUsage(s"app-${i}", rand.nextInt(100000000) + 1)
    }
    for (i <- 1 to 15) {
      snapShot.updateAppDiskUsage(s"app-${i}", rand.nextInt(100000000) + 1000000000)
    }

    println(snapShot.toString)
  }

  test("test app usage snapshot") {
    Thread.sleep(5000)

    val conf = new CelebornConf()
    conf.set("celeborn.metrics.app.topDiskUsage.windowSize", "5")
    conf.set("celeborn.metrics.app.topDiskUsage.interval", "2s")
    val usageMetric = new AppDiskUsageMetric(conf)

    val map1 = new util.HashMap[String, java.lang.Long]()
    map1.put("app1", 2874371)
    map1.put("app2", 43452)
    map1.put("app3", 2134526)
    map1.put("app4", 23465463)
    map1.put("app5", 132456)
    map1.put("app6", 6535635)
    usageMetric.update(map1)
    println(usageMetric.summary())
    Thread.sleep(2000)

    map1.clear()
    map1.put("app1", 374524)
    map1.put("app2", 5234665)
    map1.put("app3", 24453)
    map1.put("app4", 2345637)
    map1.put("app5", 4534)
    map1.put("app6", 5357)
    usageMetric.update(map1)
    println(usageMetric.summary())
    Thread.sleep(2000)

    map1.clear()
    map1.put("app1", 12343)
    map1.put("app2", 3456565)
    map1.put("app3", 4345)
    map1.put("app4", 35245268)
    map1.put("app5", 45367)
    map1.put("app6", 64345)
    usageMetric.update(map1)
    println(usageMetric.summary())
    Thread.sleep(2500)
    println(usageMetric.summary())
  }
}
