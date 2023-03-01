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

package org.apache.celeborn.service.deploy

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.util.Utils
import org.apache.celeborn.service.deploy.master.{Master, MasterArguments}
import org.apache.celeborn.service.deploy.worker.{Worker, WorkerArguments}

trait MiniClusterFeature extends Logging {
  val workerPrometheusPort = new AtomicInteger(12378)
  val masterPrometheusPort = new AtomicInteger(22378)
  var masterInfo: (Master, Thread) = _
  val workerInfos = new mutable.HashMap[Worker, Thread]()

  private def runnerWrap[T](code: => T): Thread = new Thread(new Runnable {
    override def run(): Unit = {
      Utils.tryLogNonFatalError(code)
    }
  })

  private def createTmpDir(): String = {
    val tmpDir = Files.createTempDirectory("celeborn-")
    logInfo(s"created temp dir: $tmpDir")
    tmpDir.toFile.deleteOnExit()
    tmpDir.toAbsolutePath.toString
  }

  private def createMaster(map: Map[String, String] = null): Master = {
    val conf = new CelebornConf()
    conf.set("celeborn.metrics.enabled", "false")
    val prometheusPort = masterPrometheusPort.getAndIncrement()
    conf.set("celeborn.master.metrics.prometheus.port", s"$prometheusPort")
    logInfo(s"set celeborn.master.metrics.prometheus.port to $prometheusPort")
    if (map != null) {
      map.foreach(m => conf.set(m._1, m._2))
    }

    val masterArguments = new MasterArguments(Array(), conf)
    val master = new Master(conf, masterArguments)
    master.startHttpServer()

    Thread.sleep(5000L)
    master
  }

  private def createWorker(map: Map[String, String] = null): Worker = {
    logInfo("start create worker for mini cluster")
    val conf = new CelebornConf()
    conf.set("celeborn.worker.storage.dirs", createTmpDir())
    conf.set("celeborn.worker.monitor.disk.enabled", "false")
    conf.set("celeborn.push.buffer.size", "256K")
    conf.set(
      "celeborn.worker.metrics.prometheus.port",
      s"${workerPrometheusPort.incrementAndGet()}")
    conf.set("celeborn.fetch.io.threads", "4")
    conf.set("celeborn.push.io.threads", "4")
    if (map != null) {
      map.foreach(m => conf.set(m._1, m._2))
    }
    logInfo("rss conf created")

    val workerArguments = new WorkerArguments(Array(), conf)
    logInfo("worker argument created")
    try {
      val worker = new Worker(conf, workerArguments)
      logInfo("worker created for mini cluster")
      worker
    } catch {
      case e: Exception =>
        logError("create worker failed, detail:", e)
        System.exit(-1)
        null
    }
  }

  def setUpMiniCluster(
      masterConfs: Map[String, String] = null,
      workerConfs: Map[String, String] = null,
      workerNum: Int = 3): (Master, collection.Set[Worker]) = {
    val master = createMaster(masterConfs)
    val masterThread = runnerWrap(master.rpcEnv.awaitTermination())
    masterThread.start()
    masterInfo = (master, masterThread)
    Thread.sleep(5000L)

    for (_ <- 1 to workerNum) {
      val worker = createWorker(workerConfs)
      val workerThread = runnerWrap(worker.initialize())
      workerThread.start()
      workerInfos.put(worker, workerThread)
    }
    Thread.sleep(5000L)

    workerInfos.foreach {
      case (worker, _) => assert(worker.isRegistered())
    }
    (master, workerInfos.keySet)
  }

  def shutdownMiniCluster(): Unit = {
    // shutdown workers
    workerInfos.foreach {
      case (worker, _) =>
        worker.close()
        worker.rpcEnv.shutdown()
    }

    // shutdown masters
    masterInfo._1.close()
    masterInfo._1.rpcEnv.shutdown()

    // interrupt threads
    Thread.sleep(5000)
    workerInfos.foreach {
      case (_, thread) =>
        thread.interrupt()
    }

    masterInfo._2.interrupt()
  }
}
