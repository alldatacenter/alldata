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
import org.apache.celeborn.service.deploy.worker.memory.MemoryManager

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
    conf.set(CelebornConf.METRICS_ENABLED.key, "false")
    val prometheusPort = masterPrometheusPort.getAndIncrement()
    conf.set(CelebornConf.MASTER_PROMETHEUS_PORT.key, s"$prometheusPort")
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
    conf.set(CelebornConf.WORKER_STORAGE_DIRS.key, createTmpDir())
    conf.set(CelebornConf.WORKER_DISK_MONITOR_ENABLED.key, "false")
    conf.set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE.key, "256K")
    conf.set(CelebornConf.WORKER_PROMETHEUS_PORT.key, s"${workerPrometheusPort.incrementAndGet()}")
    conf.set("celeborn.fetch.io.threads", "4")
    conf.set("celeborn.push.io.threads", "4")
    if (map != null) {
      map.foreach(m => conf.set(m._1, m._2))
    }
    logInfo("celeborn conf created")

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
      masterConf: Map[String, String] = null,
      workerConf: Map[String, String] = null,
      workerNum: Int = 3): (Master, collection.Set[Worker]) = {
    val master = createMaster(masterConf)
    val masterThread = runnerWrap(master.rpcEnv.awaitTermination())
    masterThread.start()
    masterInfo = (master, masterThread)
    Thread.sleep(5000L)
    (1 to workerNum).foreach { _ =>
      val worker = createWorker(workerConf)
      val workerThread = runnerWrap(worker.initialize())
      workerThread.start()
      workerInfos.put(worker, workerThread)
    }
    Thread.sleep(5000L)

    workerInfos.foreach { case (worker, _) => assert(worker.registered.get()) }
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
      case (worker, thread) =>
        worker.shutdown(graceful = false)
        thread.interrupt()
    }
    workerInfos.clear()
    masterInfo._2.interrupt()
    MemoryManager.reset()
  }
}
