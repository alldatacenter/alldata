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

package org.apache.celeborn.service.deploy.worker.storage

import java.io.File
import java.util
import java.util.{Set => jSet}
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.io.Source

import org.slf4j.LoggerFactory

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.meta.{DeviceInfo, DiskInfo, DiskStatus}
import org.apache.celeborn.common.metrics.source.AbstractSource

class ObservedDevice(val deviceInfo: DeviceInfo, conf: CelebornConf, workerSource: AbstractSource) {

  val logger = LoggerFactory.getLogger(classOf[ObservedDevice])

  val diskInfos = new ConcurrentHashMap[String, DiskInfo]()
  deviceInfo.diskInfos.foreach { case diskInfo =>
    diskInfos.put(diskInfo.mountPoint, diskInfo)
  }
  val observers: jSet[DeviceObserver] = ConcurrentHashMap.newKeySet[DeviceObserver]()

  val sysBlockDir = conf.diskMonitorSysBlockDir
  val statFile = new File(s"$sysBlockDir/${deviceInfo.name}/stat")
  val inFlightFile = new File(s"$sysBlockDir/${deviceInfo.name}/inflight")

  val nonCriticalErrors = new ConcurrentHashMap[DiskStatus, util.Set[Long]]()
  val notifyErrorThreshold = conf.diskMonitorNotifyErrorThreshold
  val notifyErrorExpireTimeout = conf.diskMonitorNotifyErrorExpireTimeout

  var lastReadComplete: Long = -1
  var lastWriteComplete: Long = -1
  var lastReadInflight: Long = -1
  var lastWriteInflight: Long = -1

  def addObserver(observer: DeviceObserver): Unit = {
    observers.add(observer)
  }

  def removeObserver(observer: DeviceObserver): Unit = {
    observers.remove(observer)
  }

  def notifyObserversOnError(mountPoints: List[String], diskStatus: DiskStatus): Unit =
    this.synchronized {
      mountPoints.foreach { case mountPoint =>
        diskInfos.get(mountPoint).setStatus(diskStatus)
      }
      // observer.notifyDeviceError might remove itself from observers,
      // so we need to use tmpObservers
      val tmpObservers = new util.HashSet[DeviceObserver](observers)
      tmpObservers.asScala.foreach { ob =>
        mountPoints.foreach { case mountPoint =>
          ob.notifyError(mountPoint, diskStatus)
        }
      }
    }

  def notifyObserversOnNonCriticalError(mountPoints: List[String], diskStatus: DiskStatus): Unit =
    this.synchronized {
      val nonCriticalErrorSetFunc = new util.function.Function[DiskStatus, util.Set[Long]] {
        override def apply(t: DiskStatus): util.Set[Long] = {
          val set = ConcurrentHashMap.newKeySet[Long]()
          workerSource.addGauge(
            s"Device_${deviceInfo.name}_${diskStatus.toMetric}_Count",
            _ => set.size())
          set
        }
      }
      nonCriticalErrors.computeIfAbsent(diskStatus, nonCriticalErrorSetFunc).add(
        System.currentTimeMillis())
      mountPoints.foreach { case mountPoint =>
        diskInfos.get(mountPoint).setStatus(diskStatus)
      }
      val tmpObservers = new util.HashSet[DeviceObserver](observers)
      tmpObservers.asScala.foreach { ob =>
        mountPoints.foreach { case mountPoint =>
          ob.notifyNonCriticalError(mountPoint, diskStatus)
        }
      }
    }

  def notifyObserversOnHealthy(mountPoint: String): Unit = this.synchronized {
    diskInfos.get(mountPoint).setStatus(DiskStatus.HEALTHY)
    val tmpObservers = new util.HashSet[DeviceObserver](observers)
    tmpObservers.asScala.foreach(ob => {
      ob.notifyHealthy(mountPoint)
    })
  }

  def notifyObserversOnHighDiskUsage(mountPoint: String): Unit = this.synchronized {
    diskInfos.get(mountPoint).setStatus(DiskStatus.HIGH_DISK_USAGE)
    val tmpObservers = new util.HashSet[DeviceObserver](observers)
    tmpObservers.asScala.foreach(ob => {
      ob.notifyHighDiskUsage(mountPoint)
    })
  }

  /**
   * @return true if device is hang
   */
  def ioHang(): Boolean = {
    if (deviceInfo.deviceStatAvailable) {
      false
    } else {
      var statsSource: Source = null
      var infligtSource: Source = null

      try {
        statsSource = Source.fromFile(statFile)
        infligtSource = Source.fromFile(inFlightFile)
        val stats = statsSource.getLines().next().trim.split("[ \t]+", -1)
        val inflight = infligtSource.getLines().next().trim.split("[ \t]+", -1)
        val readComplete = stats(0).toLong
        val writeComplete = stats(4).toLong
        val readInflight = inflight(0).toLong
        val writeInflight = inflight(1).toLong

        if (lastReadComplete == -1) {
          lastReadComplete = readComplete
          lastWriteComplete = writeComplete
          lastReadInflight = readInflight
          lastWriteInflight = writeInflight
          false
        } else {
          val isReadHang = lastReadComplete == readComplete &&
            readInflight >= lastReadInflight && lastReadInflight > 0
          val isWriteHang = lastWriteComplete == writeComplete &&
            writeInflight >= lastWriteInflight && lastWriteInflight > 0

          if (isReadHang || isWriteHang) {
            logger.info(s"Result of DeviceInfo.checkIoHang, DeviceName: ${deviceInfo.name}" +
              s"($readComplete,$writeComplete,$readInflight,$writeInflight)\t" +
              s"($lastReadComplete,$lastWriteComplete,$lastReadInflight,$lastWriteInflight)\t" +
              s"Observer cnt: ${observers.size()}")
            logger.error(s"IO Hang! ReadHang: $isReadHang, WriteHang: $isWriteHang")
          }

          lastReadComplete = readComplete
          lastWriteComplete = writeComplete
          lastReadInflight = readInflight
          lastWriteInflight = writeInflight

          isReadHang || isWriteHang
        }
      } catch {
        case e: Exception =>
          logger.warn(s"Encounter Exception when check IO hang for device ${deviceInfo.name}", e)
          // we should only return true if we have direct evidence that the device is hang
          false
      } finally {
        if (statsSource != null) {
          statsSource.close()
        }
        if (infligtSource != null) {
          infligtSource.close()
        }
      }
    }
  }

  override def toString: String = {
    s"DeviceName: ${deviceInfo.name}\tMount Infos: ${diskInfos.values().asScala.mkString("\n")}"
  }
}
