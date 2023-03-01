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

import java.io._
import java.nio.charset.Charset
import java.util
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.meta.{DeviceInfo, DiskInfo, DiskStatus}
import org.apache.celeborn.common.metrics.source.AbstractSource
import org.apache.celeborn.common.util.{ThreadUtils, Utils}
import org.apache.celeborn.common.util.Utils._
import org.apache.celeborn.service.deploy.worker.WorkerSource

trait DeviceMonitor {
  def startCheck() {}
  def registerFileWriter(fileWriter: FileWriter): Unit = {}
  def unregisterFileWriter(fileWriter: FileWriter): Unit = {}
  // Only local flush needs device monitor.
  def registerFlusher(flusher: LocalFlusher): Unit = {}
  def unregisterFlusher(flusher: LocalFlusher): Unit = {}
  def reportNonCriticalError(mountPoint: String, e: IOException, diskStatus: DiskStatus): Unit = {}
  def close() {}
}

object EmptyDeviceMonitor extends DeviceMonitor

class LocalDeviceMonitor(
    conf: CelebornConf,
    observer: DeviceObserver,
    deviceInfos: util.Map[String, DeviceInfo],
    diskInfos: util.Map[String, DiskInfo],
    workerSource: AbstractSource) extends DeviceMonitor {
  val logger = LoggerFactory.getLogger(classOf[LocalDeviceMonitor])

  // (deviceName -> ObservedDevice)
  var observedDevices: util.Map[DeviceInfo, ObservedDevice] = _

  val diskCheckInterval = conf.diskMonitorCheckInterval

  // we should choose what the device needs to detect
  val deviceMonitorCheckList = conf.diskMonitorCheckList
  val checkIoHang = deviceMonitorCheckList.contains("iohang")
  val checkReadWrite = deviceMonitorCheckList.contains("readwrite")
  val checkDiskUsage = deviceMonitorCheckList.contains("diskusage")
  private val diskChecker =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("worker-disk-checker")

  def init(): Unit = {
    this.observedDevices = new util.HashMap[DeviceInfo, ObservedDevice]()
    deviceInfos.asScala.filter(_._2.deviceStatAvailable).foreach { case (deviceName, _) =>
      logger.warn(s"device monitor may not worker properly " +
        s"because noDevice device $deviceName exists.")
    }
    deviceInfos.asScala.foreach(entry => {
      val observedDevice = new ObservedDevice(entry._2, conf, workerSource)
      observedDevice.addObserver(observer)
      observedDevices.put(entry._2, observedDevice)
    })
    diskInfos
      .asScala
      .values
      .toList
      .groupBy(_.deviceInfo)
      .foreach { case (deviceInfo: DeviceInfo, diskInfos: List[DiskInfo]) =>
        def usage = DeviceMonitor.getDiskUsageInfos(diskInfos.head)
        workerSource.addGauge(
          s"${WorkerSource.DeviceOSTotalCapacity}_${deviceInfo.name}",
          _ => usage(usage.length - 5).toLong)
        workerSource.addGauge(
          s"${WorkerSource.DeviceOSFreeCapacity}_${deviceInfo.name}",
          _ => usage(usage.length - 3).toLong)
        workerSource.addGauge(
          s"${WorkerSource.DeviceCelebornTotalCapacity}_${deviceInfo.name}",
          _ => diskInfos.map(_.configuredUsableSpace).sum)
        workerSource.addGauge(
          s"${WorkerSource.DeviceCelebornFreeCapacity}_${deviceInfo.name}",
          _ => diskInfos.map(_.actualUsableSpace).sum)
      }
  }

  override def startCheck(): Unit = {
    diskChecker.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = {
          logger.debug("Device check start")
          try {
            observedDevices.values().asScala.foreach(device => {
              val mountPoints = device.diskInfos.keySet.asScala.toList
              // tolerate time accuracy for better performance
              val now = System.currentTimeMillis()
              for (concurrentSet <- device.nonCriticalErrors.values().asScala) {
                for (time <- concurrentSet.asScala) {
                  if (now - time > device.notifyErrorExpireTimeout) {
                    concurrentSet.remove(time)
                  }
                }
              }
              val nonCriticalErrorSum = device.nonCriticalErrors.values().asScala.map(_.size).sum
              if (nonCriticalErrorSum > device.notifyErrorThreshold) {
                logger.error(s"Device ${device.deviceInfo.name} has accumulated $nonCriticalErrorSum non-critical " +
                  s"error within the past ${Utils.msDurationToString(device.notifyErrorExpireTimeout)} , its sum has " +
                  s"exceed the threshold (${device.notifyErrorThreshold}), device monitor will notify error to " +
                  s"observed device.")
                val mountPoints = device.diskInfos.values().asScala.map(_.mountPoint).toList
                device.notifyObserversOnError(mountPoints, DiskStatus.CRITICAL_ERROR)
              } else {
                if (checkIoHang && device.ioHang()) {
                  logger.error(s"Encounter device io hang error!" +
                    s"${device.deviceInfo.name}, notify observers")
                  device.notifyObserversOnNonCriticalError(mountPoints, DiskStatus.IO_HANG)
                } else {
                  device.diskInfos.values().asScala.foreach { case diskInfo =>
                    if (checkDiskUsage && DeviceMonitor.highDiskUsage(conf, diskInfo)) {
                      logger.error(
                        s"${diskInfo.mountPoint} high_disk_usage error, notify observers")
                      device.notifyObserversOnHighDiskUsage(diskInfo.mountPoint)
                    } else if (checkReadWrite &&
                      DeviceMonitor.readWriteError(conf, diskInfo.dirs.head)) {
                      logger.error(s"${diskInfo.mountPoint} read-write error, notify observers")
                      // We think that if one dir in device has read-write problem, if possible all
                      // dirs in this device have the problem
                      device.notifyObserversOnNonCriticalError(
                        List(diskInfo.mountPoint),
                        DiskStatus.READ_OR_WRITE_FAILURE)
                    } else if (nonCriticalErrorSum <= device.notifyErrorThreshold * 0.5) {
                      device.notifyObserversOnHealthy(diskInfo.mountPoint)
                    }
                  }
                }
              }
            })
          } catch {
            case t: Throwable =>
              logger.error("Device check failed.", t)
          }
        }
      },
      diskCheckInterval,
      diskCheckInterval,
      TimeUnit.MILLISECONDS)
  }

  override def registerFileWriter(fileWriter: FileWriter): Unit = {
    val mountPoint = DeviceInfo.getMountPoint(fileWriter.getFile.getAbsolutePath, diskInfos)
    observedDevices.get(diskInfos.get(mountPoint).deviceInfo).addObserver(fileWriter)
  }

  override def unregisterFileWriter(fileWriter: FileWriter): Unit = {
    val mountPoint = DeviceInfo.getMountPoint(fileWriter.getFile.getAbsolutePath, diskInfos)
    observedDevices.get(diskInfos.get(mountPoint).deviceInfo).removeObserver(fileWriter)
  }

  override def registerFlusher(flusher: LocalFlusher): Unit = {
    observedDevices.get(diskInfos.get(flusher.mountPoint).deviceInfo).addObserver(flusher)
  }

  override def unregisterFlusher(flusher: LocalFlusher): Unit = {
    observedDevices.get(diskInfos.get(flusher.mountPoint).deviceInfo).removeObserver(flusher)
  }

  override def reportNonCriticalError(
      mountPoint: String,
      e: IOException,
      diskStatus: DiskStatus): Unit = {
    logger.error(s"Receive non-critical exception, disk: $mountPoint, $e")
    observedDevices.get(diskInfos.get(mountPoint).deviceInfo)
      .notifyObserversOnNonCriticalError(List(mountPoint), diskStatus)
  }

  override def close(): Unit = {
    if (null != DeviceMonitor.deviceCheckThreadPool) {
      DeviceMonitor.deviceCheckThreadPool.shutdownNow()
    }
  }
}

object DeviceMonitor {
  val logger = LoggerFactory.getLogger(classOf[DeviceMonitor])
  val deviceCheckThreadPool = ThreadUtils.newDaemonCachedThreadPool("device-check-thread", 5)

  def createDeviceMonitor(
      conf: CelebornConf,
      deviceObserver: DeviceObserver,
      deviceInfos: util.Map[String, DeviceInfo],
      diskInfos: util.Map[String, DiskInfo],
      workerSource: AbstractSource): DeviceMonitor = {
    try {
      if (conf.diskMonitorEnabled) {
        val monitor =
          new LocalDeviceMonitor(conf, deviceObserver, deviceInfos, diskInfos, workerSource)
        monitor.init()
        logger.info("Device monitor init success")
        monitor
      } else {
        EmptyDeviceMonitor
      }
    } catch {
      case t: Throwable =>
        logger.error("Device monitor init failed.", t)
        throw t
    }
  }

  // unit is byte
  def getDiskUsageInfos(diskInfo: DiskInfo): Array[String] = {
    runCommand(s"df -B1 ${diskInfo.mountPoint}").trim.split("[ \t]+")
  }

  /**
   * check if the disk is high usage
   *
   * @param conf     conf
   * @param diskInfo diskInfo
   * @return true if high disk usage
   */
  def highDiskUsage(conf: CelebornConf, diskInfo: DiskInfo): Boolean = {
    tryWithTimeoutAndCallback({
      val usage = getDiskUsageInfos(diskInfo)
      val totalSpace = usage(usage.length - 5)
      val freeSpace = usage(usage.length - 3)
      val used_percent = usage(usage.length - 2)
      // assume no single device capacity exceeds 1EB in this era
      val highDiskUsage =
        freeSpace.toLong < conf.diskReserveSize || diskInfo.actualUsableSpace <= 0
      if (highDiskUsage) {
        logger.warn(s"${diskInfo.mountPoint} usage is above threshold." +
          s" Disk usage(Report by OS):{total:${Utils.bytesToString(totalSpace.toLong)}," +
          s" free:${Utils.bytesToString(freeSpace.toLong)}, used_percent:$used_percent} " +
          s"usage(Report by Celeborn):{" +
          s"total:${Utils.bytesToString(diskInfo.configuredUsableSpace)}" +
          s" free:${Utils.bytesToString(diskInfo.actualUsableSpace)} }")
      }
      highDiskUsage
    })(false)(
      deviceCheckThreadPool,
      conf.workerDeviceStatusCheckTimeout,
      s"Disk: ${diskInfo.mountPoint} Usage Check Timeout")
  }

  /**
   * check if the data dir has read-write problem
   *
   * @param conf    conf
   * @param dataDir one of shuffle data dirs in mount disk
   * @return true if disk has read-write problem
   */
  def readWriteError(conf: CelebornConf, dataDir: File): Boolean = {
    if (null == dataDir || !dataDir.isDirectory) {
      return false
    }

    tryWithTimeoutAndCallback({
      try {
        val file = new File(dataDir, s"_SUCCESS_${System.currentTimeMillis()}")
        if (!file.exists() && !file.createNewFile()) {
          true
        } else {
          FileUtils.write(file, "test", Charset.defaultCharset)
          var fileInputStream: FileInputStream = null
          var inputStreamReader: InputStreamReader = null
          var bufferReader: BufferedReader = null
          try {
            fileInputStream = FileUtils.openInputStream(file)
            inputStreamReader = new InputStreamReader(fileInputStream, Charset.defaultCharset())
            bufferReader = new BufferedReader(inputStreamReader)
            bufferReader.readLine()
          } finally {
            bufferReader.close()
            inputStreamReader.close()
            fileInputStream.close()
          }
          FileUtils.forceDelete(file)
          false
        }
      } catch {
        case t: Throwable =>
          logger.error(s"Disk dir $dataDir cannot read or write", t)
          true
      }
    })(false)(
      deviceCheckThreadPool,
      conf.workerDeviceStatusCheckTimeout,
      s"Disk: $dataDir Read_Write Check Timeout")
  }

  def EmptyMonitor(): DeviceMonitor = EmptyDeviceMonitor
}
