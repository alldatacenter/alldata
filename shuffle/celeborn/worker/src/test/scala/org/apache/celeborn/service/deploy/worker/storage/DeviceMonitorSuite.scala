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
import java.util.{ArrayList => jArrayList}
import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

import org.junit.Assert.assertEquals
import org.mockito.MockitoSugar._
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.CelebornConf.WORKER_DISK_MONITOR_CHECK_INTERVAL
import org.apache.celeborn.common.meta.{DeviceInfo, DiskInfo, DiskStatus}
import org.apache.celeborn.common.protocol.StorageInfo
import org.apache.celeborn.common.util.Utils
import org.apache.celeborn.service.deploy.worker.WorkerSource

class DeviceMonitorSuite extends AnyFunSuite {
  val dfCmd = "df -ah"
  val dfOut =
    """
      |Filesystem      Size  Used Avail  Use% Mounted on
      |devtmpfs         32G     0   32G    0% /dev
      |tmpfs            32G  108K   32G    1% /dev/shm
      |tmpfs            32G  672K   32G    1% /run
      |tmpfs            32G     0   32G    0% /sys/fs/cgroup
      |/dev/vda1       118G   43G   71G   38% /
      |tmpfs           6.3G     0  6.3G    0% /run/user/0
      |/dev/vda        1.8T   91G  1.7T    6% /mnt/disk1
      |/dev/vdb        1.8T   91G  1.7T    6% /mnt/disk2
      |tmpfs           6.3G     0  6.3G    0% /run/user/1001
      |""".stripMargin
  val dfOut2 =
    """
      |Filesystem      Size  Used Avail  Use% Mounted on
      |devtmpfs         32G     0   32G    0% /dev
      |tmpfs            32G  108K   32G    1% /dev/shm
      |tmpfs            32G  672K   32G    1% /run
      |tmpfs            32G     0   32G    0% /sys/fs/cgroup
      |/dev/vda1       118G   43G   71G   38% /
      |tmpfs           6.3G     0  6.3G    0% /run/user/0
      |/dev/vda        1.3T   95G  1.2T    7% /mnt/disk1
      |/dev/vdb        1.8T   91G  1.7T    6% /mnt/disk2
      |/dev/vda        1.3T   95G  1.2T    7% /mnt/disk3
      |/dev/vdb        1.8T   91G  1.7T    6% /mnt/disk4
      |/dev/vdb        1.8T   91G  1.7T    6% /mnt/disk5
      |tmpfs           6.3G     0  6.3G    0% /run/user/1001
      |""".stripMargin

  val lsCmd = "ls /sys/block/"
  val lsOut = "loop0  loop1  loop2  loop3  loop4  loop5  loop6  loop7  vda  vdb"

  val dfBCmd1 = "df -B1 /mnt/disk1"
  val dfBCmd2 = "df -B1 /mnt/disk2"
  val dfBCmd3 = "df -B1 /mnt/disk3"
  val dfBCmd4 = "df -B1 /mnt/disk4"
  val dfBCmd5 = "df -B1 /mnt/disk5"

  val dfBOut1 =
    """
      |Filesystem     1B-blocks            Used          Available Use% Mounted on
      |/dev/vda   1395864371200    102005473280      1293858897920   7% /mnt/disk1
      |""".stripMargin
  val dfBOut2 =
    """
      |Filesystem     1B-blocks            Used          Available Use% Mounted on
      |/dev/vdb   1932735283200     97710505984      1835024777216   6% /mnt/disk2
      |""".stripMargin
  val dfBOut3 =
    """
      |Filesystem     1B-blocks            Used          Available Use% Mounted on
      |/dev/vda   1395864371200    102005473280      1293858897920   7% /mnt/disk3
      |""".stripMargin
  val dfBOut4 =
    """
      |Filesystem     1B-blocks            Used          Available Use% Mounted on
      |/dev/vdb   1932735283200     97710505984      1835024777216   6% /mnt/disk4
      |""".stripMargin
  val dfBOut5 =
    """
      |Filesystem     1B-blocks            Used          Available Use% Mounted on
      |/dev/vdb   1932735283200     97710505984      1835024777216   6% /mnt/disk5
      |""".stripMargin

  val dirs = new jArrayList[File]()
  val workingDir1 = ListBuffer[File](new File("/mnt/disk1/data1"))
  val workingDir2 = ListBuffer[File](new File("/mnt/disk1/data2"))
  val workingDir3 = ListBuffer[File](new File("/mnt/disk2/data3"))
  val workingDir4 = ListBuffer[File](new File("/mnt/disk2/data4"))
  dirs.addAll(workingDir1.asJava)
  dirs.addAll(workingDir2.asJava)
  dirs.addAll(workingDir3.asJava)
  dirs.addAll(workingDir4.asJava)

  val dirs2 = new jArrayList[File]()
  dirs2.add(new File("/mnt/disk1/data1"))
  dirs2.add(new File("/mnt/disk2/data1"))
  dirs2.add(new File("/mnt/disk3/data1"))
  dirs2.add(new File("/mnt/disk4/data1"))
  dirs2.add(new File("/mnt/disk5/data1"))

  val conf = new CelebornConf()
  conf.set(WORKER_DISK_MONITOR_CHECK_INTERVAL.key, "3600s")
  val workerSource = new WorkerSource(conf)
  val workerSource2 = new WorkerSource(conf)

  val storageManager = mock[DeviceObserver]
  val storageManager2 = mock[DeviceObserver]

  var (deviceInfos, diskInfos): (
      java.util.Map[String, DeviceInfo],
      java.util.Map[String, DiskInfo]) = (null, null)
  var (deviceInfos2, diskInfos2): (
      java.util.Map[String, DeviceInfo],
      java.util.Map[String, DiskInfo]) = (null, null)

  withObjectMocked[org.apache.celeborn.common.util.Utils.type] {
    when(Utils.runCommand(dfCmd)) thenReturn dfOut
    when(Utils.runCommand(lsCmd)) thenReturn lsOut
    val (tdeviceInfos, tdiskInfos) = DeviceInfo.getDeviceAndDiskInfos(dirs.asScala.toArray.map(f =>
      (f, Long.MaxValue, 1, StorageInfo.Type.HDD)))
    deviceInfos = tdeviceInfos
    diskInfos = tdiskInfos
    when(Utils.runCommand(dfCmd)) thenReturn dfOut2
    val (tdeviceInfos2, tdiskInfos2) =
      DeviceInfo.getDeviceAndDiskInfos(dirs2.asScala.toArray.map(f =>
        (f, Int.MaxValue.toLong, 8, StorageInfo.Type.SSD)))
    deviceInfos2 = tdeviceInfos2
    diskInfos2 = tdiskInfos2
  }

  val deviceMonitor =
    new LocalDeviceMonitor(conf, storageManager, deviceInfos, diskInfos, workerSource)
  val deviceMonitor2 =
    new LocalDeviceMonitor(conf, storageManager2, deviceInfos2, diskInfos2, workerSource2)

  val vdaDeviceInfo = new DeviceInfo("vda")
  val vdbDeviceInfo = new DeviceInfo("vdb")

  test("init") {
    withObjectMocked[org.apache.celeborn.common.util.Utils.type] {
      when(Utils.runCommand(dfCmd)) thenReturn dfOut
      when(Utils.runCommand(lsCmd)) thenReturn lsOut

      deviceMonitor.init()

      assertEquals(2, deviceMonitor.observedDevices.size())

      assert(deviceMonitor.observedDevices.containsKey(vdaDeviceInfo))
      assert(deviceMonitor.observedDevices.containsKey(vdbDeviceInfo))

      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).diskInfos.size, 1)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).diskInfos.size, 1)

      assert(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).diskInfos.containsKey("/mnt/disk1"))
      assert(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).diskInfos.containsKey("/mnt/disk2"))

      assertEquals(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).diskInfos.get("/mnt/disk1").dirs(0),
        new File("/mnt/disk1/data1"))
      assertEquals(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).diskInfos.get("/mnt/disk1").dirs(1),
        new File("/mnt/disk1/data2"))
      assertEquals(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).diskInfos.get("/mnt/disk2").dirs(0),
        new File("/mnt/disk2/data3"))
      assertEquals(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).diskInfos.get("/mnt/disk2").dirs(1),
        new File("/mnt/disk2/data4"))

      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 1)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 1)
    }
  }

  test("register/unregister/notify/report") {
    withObjectMocked[org.apache.celeborn.common.util.Utils.type] {
      when(Utils.runCommand(dfCmd)) thenReturn dfOut
      when(Utils.runCommand(lsCmd)) thenReturn lsOut

      deviceMonitor.init()

      val fw1 = mock[FileWriter]
      val fw2 = mock[FileWriter]
      val fw3 = mock[FileWriter]
      val fw4 = mock[FileWriter]

      val f1 = new File("/mnt/disk1/data1/f1")
      val f2 = new File("/mnt/disk1/data2/f2")
      val f3 = new File("/mnt/disk2/data3/f3")
      val f4 = new File("/mnt/disk2/data4/f4")
      when(fw1.getFile).thenReturn(f1)
      when(fw2.getFile).thenReturn(f2)
      when(fw3.getFile).thenReturn(f3)
      when(fw4.getFile).thenReturn(f4)

      deviceMonitor.registerFileWriter(fw1)
      deviceMonitor.registerFileWriter(fw2)
      deviceMonitor.registerFileWriter(fw3)
      deviceMonitor.registerFileWriter(fw4)

      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 3)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 3)
      assert(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(fw1))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(fw2))
      assert(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(fw3))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(fw4))

      deviceMonitor.unregisterFileWriter(fw1)
      deviceMonitor.unregisterFileWriter(fw3)
      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 2)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 2)
      assert(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(fw2))
      assert(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(fw4))

      val df1 = mock[LocalFlusher]
      val df2 = mock[LocalFlusher]
      val df3 = mock[LocalFlusher]
      val df4 = mock[LocalFlusher]

      when(df1.stopFlag).thenReturn(new AtomicBoolean(false))
      when(df2.stopFlag).thenReturn(new AtomicBoolean(false))
      when(df3.stopFlag).thenReturn(new AtomicBoolean(false))
      when(df4.stopFlag).thenReturn(new AtomicBoolean(false))

      when(df1.mountPoint).thenReturn("/mnt/disk1")
      when(df2.mountPoint).thenReturn("/mnt/disk1")
      when(df3.mountPoint).thenReturn("/mnt/disk2")
      when(df4.mountPoint).thenReturn("/mnt/disk2")

      deviceMonitor.registerFlusher(df1)
      deviceMonitor.registerFlusher(df2)
      deviceMonitor.registerFlusher(df3)
      deviceMonitor.registerFlusher(df4)
      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 4)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 4)
      assert(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(df1))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(df2))
      assert(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(df3))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(df4))

      deviceMonitor.unregisterFlusher(df1)
      deviceMonitor.unregisterFlusher(df3)
      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 3)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 3)
      assert(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(df2))
      assert(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(df4))

      when(fw2.notifyError("vda", DiskStatus.CRITICAL_ERROR))
        .thenAnswer((a: String, b: List[File]) => {
          deviceMonitor.unregisterFileWriter(fw2)
        })
      when(fw4.notifyError("vdb", DiskStatus.CRITICAL_ERROR))
        .thenAnswer((a: String, b: List[File]) => {
          deviceMonitor.unregisterFileWriter(fw4)
        })
      when(df2.notifyError("vda", DiskStatus.CRITICAL_ERROR))
        .thenAnswer((a: String, b: List[File]) => {
          df2.stopFlag.set(true)
        })
      when(df4.notifyError("vdb", DiskStatus.CRITICAL_ERROR))
        .thenAnswer((a: String, b: List[File]) => {
          df4.stopFlag.set(true)
        })

      deviceMonitor.observedDevices
        .get(vdaDeviceInfo)
        .notifyObserversOnError(List("/mnt/disk1"), DiskStatus.IO_HANG)
      deviceMonitor.observedDevices
        .get(vdbDeviceInfo)
        .notifyObserversOnError(List("/mnt/disk2"), DiskStatus.IO_HANG)
      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 3)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 3)
      assert(
        deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.contains(df2))
      assert(
        deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(storageManager))
      assert(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.contains(df4))

      deviceMonitor.registerFileWriter(fw1)
      deviceMonitor.registerFileWriter(fw2)
      deviceMonitor.registerFileWriter(fw3)
      deviceMonitor.registerFileWriter(fw4)
      assertEquals(deviceMonitor.observedDevices.get(vdaDeviceInfo).observers.size(), 4)
      assertEquals(deviceMonitor.observedDevices.get(vdbDeviceInfo).observers.size(), 4)
    }
  }

  test("tryWithTimeoutAndCallback") {
    val fn = (i: Int) => {
      0 until 100 foreach (x => {
        // scalastyle:off println
        println(i + Thread.currentThread().getName)
        Thread.sleep(2000)
        // scalastyle:on println
      })
      true
    }
    0 until 3 foreach (i => {
      val result = Utils.tryWithTimeoutAndCallback({
        fn(i)
      })(false)(DeviceMonitor.deviceCheckThreadPool, 1)
      assert(!result)
    })
    DeviceMonitor.deviceCheckThreadPool.shutdownNow()
  }

  test("monitor non-critical error metrics") {
    withObjectMocked[org.apache.celeborn.common.util.Utils.type] {
      when(Utils.runCommand(dfCmd)) thenReturn dfOut
      when(Utils.runCommand(lsCmd)) thenReturn lsOut

      deviceMonitor.init()

      val device1 = deviceMonitor.observedDevices.values().asScala.head
      val mountPoints1 = device1.diskInfos.keySet().asScala.toList

      device1.notifyObserversOnNonCriticalError(mountPoints1, DiskStatus.READ_OR_WRITE_FAILURE)
      device1.notifyObserversOnNonCriticalError(mountPoints1, DiskStatus.IO_HANG)
      val deviceMonitorMetrics =
        workerSource.gauges().filter(_.name.startsWith("Device_" + device1.deviceInfo.name))
          .sortBy(_.name)

      assertEquals("Device_vda_IoHang_Count", deviceMonitorMetrics.head.name)
      assertEquals("Device_vda_ReadOrWriteFailure_Count", deviceMonitorMetrics.last.name)
      assertEquals(1, deviceMonitorMetrics.head.gauge.getValue)
      assertEquals(1, deviceMonitorMetrics.last.gauge.getValue)

      device1.notifyObserversOnNonCriticalError(mountPoints1, DiskStatus.READ_OR_WRITE_FAILURE)
      device1.notifyObserversOnNonCriticalError(mountPoints1, DiskStatus.IO_HANG)
      assertEquals(2, deviceMonitorMetrics.head.gauge.getValue)
      assertEquals(2, deviceMonitorMetrics.last.gauge.getValue)
    }
  }

  test("monitor device usage metrics") {
    withObjectMocked[org.apache.celeborn.common.util.Utils.type] {
      when(Utils.runCommand(dfBCmd1)).thenReturn(dfBOut1)
      when(Utils.runCommand(dfBCmd2)).thenReturn(dfBOut2)
      when(Utils.runCommand(dfBCmd3)).thenReturn(dfBOut3)
      when(Utils.runCommand(dfBCmd4)).thenReturn(dfBOut4)
      when(Utils.runCommand(dfBCmd5)).thenReturn(dfBOut5)
      val dfBOut6 =
        """
          |Filesystem     1B-blocks            Used          Available Use% Mounted on
          |/dev/vda   1395864371200    130996502528      1264867868672   9% /mnt/disk1
          |""".stripMargin

      deviceMonitor2.init()

      val metrics1 = workerSource2.gauges().filter(
        _.name.startsWith(WorkerSource.DeviceOSTotalCapacity)).sortBy(_.name)
      val metrics2 = workerSource2.gauges().filter(
        _.name.startsWith(WorkerSource.DeviceOSFreeCapacity)).sortBy(_.name)
      val metrics3 = workerSource2.gauges().filter(
        _.name.startsWith(WorkerSource.DeviceCelebornTotalCapacity)).sortBy(_.name)
      val metrics4 = workerSource2.gauges().filter(
        _.name.startsWith(WorkerSource.DeviceCelebornFreeCapacity)).sortBy(_.name)

      assertEquals(s"${WorkerSource.DeviceOSTotalCapacity}_vda", metrics1.head.name)
      assertEquals(1395864371200L, metrics1.head.gauge.getValue)
      assertEquals(s"${WorkerSource.DeviceOSTotalCapacity}_vdb", metrics1.last.name)
      assertEquals(1932735283200L, metrics1.last.gauge.getValue)

      assertEquals(s"${WorkerSource.DeviceOSFreeCapacity}_vda", metrics2.head.name)
      assertEquals(1293858897920L, metrics2.head.gauge.getValue)
      assertEquals(s"${WorkerSource.DeviceOSFreeCapacity}_vdb", metrics2.last.name)
      assertEquals(1835024777216L, metrics2.last.gauge.getValue)

      assertEquals(s"${WorkerSource.DeviceCelebornTotalCapacity}_vda", metrics3.head.name)
      assertEquals(Int.MaxValue.toLong * 2, metrics3.head.gauge.getValue)
      assertEquals(s"${WorkerSource.DeviceCelebornTotalCapacity}_vdb", metrics3.last.name)
      assertEquals(Int.MaxValue.toLong * 3, metrics3.last.gauge.getValue)

      // test if metrics will change when disk usage change
      diskInfos2.values().asScala.foreach(diskInfo => diskInfo.setUsableSpace(1024))
      assertEquals(s"${WorkerSource.DeviceCelebornFreeCapacity}_vda", metrics4.head.name)
      assertEquals(1024L * 2, metrics4.head.gauge.getValue)
      assertEquals(s"${WorkerSource.DeviceCelebornFreeCapacity}_vdb", metrics4.last.name)
      assertEquals(1024L * 3, metrics4.last.gauge.getValue)

      when(Utils.runCommand(dfBCmd1)).thenReturn(dfBOut6)
      assertEquals(1264867868672L, metrics2.head.gauge.getValue)
    }
  }
}
