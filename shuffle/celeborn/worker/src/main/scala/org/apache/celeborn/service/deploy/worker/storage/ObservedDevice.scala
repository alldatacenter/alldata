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
import java.util.{Set => JSet}
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.io.Source

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DeviceInfo, DiskInfo, DiskStatus}
import org.apache.celeborn.common.metrics.source.AbstractSource
// Can Remove this if celeborn don't support scala211 in future
import org.apache.celeborn.common.util.FunctionConverter._
import org.apache.celeborn.common.util.JavaUtils

class ObservedDevice(val deviceInfo: DeviceInfo, conf: CelebornConf, workerSource: AbstractSource)
  extends Logging {

  val diskInfos = JavaUtils.newConcurrentHashMap[String, DiskInfo]()
  deviceInfo.diskInfos.foreach { diskInfo => diskInfos.put(diskInfo.mountPoint, diskInfo) }

  val observers: JSet[DeviceObserver] = ConcurrentHashMap.newKeySet[DeviceObserver]()

  val sysBlockDir: String = conf.workerDiskMonitorSysBlockDir
  val statFile = new File(s"$sysBlockDir/${deviceInfo.name}/stat")
  val inFlightFile = new File(s"$sysBlockDir/${deviceInfo.name}/inflight")

  val nonCriticalErrors: ConcurrentHashMap[DiskStatus, JSet[Long]] =
    JavaUtils.newConcurrentHashMap[DiskStatus, JSet[Long]]()
  val notifyErrorThreshold: Int = conf.workerDiskMonitorNotifyErrorThreshold
  val notifyErrorExpireTimeout: Long = conf.workerDiskMonitorNotifyErrorExpireTimeout

  var lastReadComplete: Long = -1
  var lastWriteComplete: Long = -1
  var lastReadInFlight: Long = -1
  var lastWriteInFlight: Long = -1

  def addObserver(observer: DeviceObserver): Unit = {
    observers.add(observer)
  }

  def removeObserver(observer: DeviceObserver): Unit = {
    observers.remove(observer)
  }

  def notifyObserversOnError(mountPoints: List[String], diskStatus: DiskStatus): Unit =
    this.synchronized {
      mountPoints.foreach { mountPoint =>
        diskInfos.get(mountPoint).setStatus(diskStatus)
      }
      // observer.notifyDeviceError might remove itself from observers,
      // so we need to use tmpObservers
      val tmpObservers = new util.HashSet[DeviceObserver](observers)
      tmpObservers.forEach { ob: DeviceObserver =>
        mountPoints.foreach { mountPoint =>
          ob.notifyError(mountPoint, diskStatus)
        }
      }
    }

  def notifyObserversOnNonCriticalError(mountPoints: List[String], diskStatus: DiskStatus): Unit =
    this.synchronized {
      nonCriticalErrors.computeIfAbsent(
        diskStatus,
        (_: DiskStatus) => {
          val set = ConcurrentHashMap.newKeySet[Long]()
          workerSource.addGauge(s"Device_${deviceInfo.name}_${diskStatus.toMetric}_Count") { () =>
            set.size()
          }
          set
        }).add(System.currentTimeMillis())
      mountPoints.foreach { mountPoint =>
        diskInfos.get(mountPoint).setStatus(diskStatus)
      }
      val tmpObservers = new util.HashSet[DeviceObserver](observers)
      tmpObservers.forEach { ob: DeviceObserver =>
        mountPoints.foreach { mountPoint =>
          ob.notifyNonCriticalError(mountPoint, diskStatus)
        }
      }
    }

  def notifyObserversOnHealthy(mountPoint: String): Unit = this.synchronized {
    diskInfos.get(mountPoint).setStatus(DiskStatus.HEALTHY)
    val tmpObservers = new util.HashSet[DeviceObserver](observers)
    tmpObservers.forEach { ob: DeviceObserver =>
      ob.notifyHealthy(mountPoint)
    }
  }

  def notifyObserversOnHighDiskUsage(mountPoint: String): Unit = this.synchronized {
    diskInfos.get(mountPoint).setStatus(DiskStatus.HIGH_DISK_USAGE)
    val tmpObservers = new util.HashSet[DeviceObserver](observers)
    tmpObservers.forEach { ob: DeviceObserver =>
      ob.notifyHighDiskUsage(mountPoint)
    }
  }

  /**
   * @return true if device is hang
   */
  def ioHang(): Boolean = {
    if (!deviceInfo.deviceStatAvailable) {
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
          lastReadInFlight = readInflight
          lastWriteInFlight = writeInflight
          false
        } else {
          val isReadHang = lastReadComplete == readComplete &&
            readInflight >= lastReadInFlight && lastReadInFlight > 0
          val isWriteHang = lastWriteComplete == writeComplete &&
            writeInflight >= lastWriteInFlight && lastWriteInFlight > 0

          if (isReadHang || isWriteHang) {
            logInfo(s"Result of DeviceInfo.checkIoHang, DeviceName: ${deviceInfo.name}" +
              s"($readComplete,$writeComplete,$readInflight,$writeInflight)\t" +
              s"($lastReadComplete,$lastWriteComplete,$lastReadInFlight,$lastWriteInFlight)\t" +
              s"Observer cnt: ${observers.size()}")
            logError(s"IO Hang! ReadHang: $isReadHang, WriteHang: $isWriteHang")
          }

          lastReadComplete = readComplete
          lastWriteComplete = writeComplete
          lastReadInFlight = readInflight
          lastWriteInFlight = writeInflight

          isReadHang || isWriteHang
        }
      } catch {
        case e: Exception =>
          logWarning(s"Encounter Exception when check IO hang for device ${deviceInfo.name}", e)
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
