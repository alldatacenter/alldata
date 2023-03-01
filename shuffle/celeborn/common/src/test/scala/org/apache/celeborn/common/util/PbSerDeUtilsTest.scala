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

package org.apache.celeborn.common.util

import java.io.File
import java.util
import java.util.concurrent.ConcurrentHashMap

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.identity.UserIdentifier
import org.apache.celeborn.common.meta.{DeviceInfo, DiskInfo, FileInfo, WorkerInfo}
import org.apache.celeborn.common.protocol.PartitionLocation
import org.apache.celeborn.common.protocol.message.ControlMessages.WorkerResource
import org.apache.celeborn.common.quota.ResourceConsumption

class PbSerDeUtilsTest extends CelebornFunSuite {

  val fileSet = new util.HashSet[String]()
  fileSet.add("a")
  fileSet.add("b")
  fileSet.add("c")

  val major = 1
  val minor = 0

  val file1 = new File("/mnt/disk/1")
  val file2 = new File("/mnt/disk/2")
  val files = List(file1, file2)

  val device = new DeviceInfo("device-a")
  val diskInfo1 = new DiskInfo("/mnt/disk/0", 1000, 1000, 1000, files, device)
  val diskInfo2 = new DiskInfo("/mnt/disk/1", 2000, 2000, 2000, files, device)
  val diskInfos = new util.HashMap[String, DiskInfo]()
  diskInfos.put("disk1", diskInfo1)
  diskInfos.put("disk2", diskInfo2)

  val userIdentifier1 = UserIdentifier("tenant-a", "user-a")
  val userIdentifier2 = UserIdentifier("tenant-b", "user-b")

  val chunkOffsets1 = util.Arrays.asList[java.lang.Long](1000, 2000, 3000)
  val chunkOffsets2 = util.Arrays.asList[java.lang.Long](2000, 4000, 6000)

  val fileInfo1 = new FileInfo("/tmp/1", chunkOffsets1, userIdentifier1)
  val fileInfo2 = new FileInfo("/tmp/2", chunkOffsets2, userIdentifier2)
  val fileInfoMap = new ConcurrentHashMap[String, FileInfo]()
  fileInfoMap.put("file1", fileInfo1)
  fileInfoMap.put("file2", fileInfo2)
  val cache = new ConcurrentHashMap[String, UserIdentifier]()

  val resourceConsumption1 = ResourceConsumption(1000, 2000, 3000, 4000)
  val resourceConsumption2 = ResourceConsumption(2000, 4000, 6000, 8000)
  val userResourceConsumption = new util.HashMap[UserIdentifier, ResourceConsumption]()
  userResourceConsumption.put(userIdentifier1, resourceConsumption1)
  userResourceConsumption.put(userIdentifier2, resourceConsumption2)

  val workerInfo1 =
    new WorkerInfo("localhost", 1001, 1002, 1003, 1004, diskInfos, userResourceConsumption, null)
  val workerInfo2 =
    new WorkerInfo("localhost", 2001, 2002, 2003, 2004, diskInfos, userResourceConsumption, null)

  val partitionLocation1 =
    new PartitionLocation(0, 0, "host1", 10, 9, 8, 14, PartitionLocation.Mode.SLAVE)
  val partitionLocation2 =
    new PartitionLocation(1, 1, "host2", 20, 19, 18, 24, PartitionLocation.Mode.SLAVE)

  val workerResource = new WorkerResource()
  workerResource.put(
    workerInfo1,
    (util.Arrays.asList(partitionLocation1), util.Arrays.asList(partitionLocation2)))

  test("fromAndToPbSortedShuffleFileSet") {
    val pbFileSet = PbSerDeUtils.toPbSortedShuffleFileSet(fileSet)
    val restoredFileSet = PbSerDeUtils.fromPbSortedShuffleFileSet(pbFileSet)

    assert(restoredFileSet.equals(fileSet))
    // test if the restored is mutable
    restoredFileSet.add("d")
    assert(restoredFileSet.size() == 4)
  }

  test("fromAndToPbStoreVersion") {
    val pbVersion = PbSerDeUtils.toPbStoreVersion(major, minor)
    val restoredVersion = PbSerDeUtils.fromPbStoreVersion(pbVersion)

    assert(restoredVersion.get(0) == major)
    assert(restoredVersion.get(1) == minor)
  }

  test("fromAndToPbDiskInfo") {
    val pbDiskInfo = PbSerDeUtils.toPbDiskInfo(diskInfo1)
    val restoredDiskInfo = PbSerDeUtils.fromPbDiskInfo(pbDiskInfo)

    assert(restoredDiskInfo.mountPoint.equals(diskInfo1.mountPoint))
    assert(restoredDiskInfo.actualUsableSpace.equals(diskInfo1.actualUsableSpace))
    assert(restoredDiskInfo.avgFlushTime.equals(diskInfo1.avgFlushTime))
    assert(restoredDiskInfo.activeSlots.equals(diskInfo1.activeSlots))
    assert(restoredDiskInfo.dirs.equals(List.empty))
    assert(restoredDiskInfo.deviceInfo == null)
  }

  test("fromAndToPbFileInfo") {
    val pbFileInfo = PbSerDeUtils.toPbFileInfo(fileInfo1)
    val restoredFileInfo = PbSerDeUtils.fromPbFileInfo(pbFileInfo)

    assert(restoredFileInfo.getFilePath.equals(fileInfo1.getFilePath))
    assert(restoredFileInfo.getChunkOffsets.equals(fileInfo1.getChunkOffsets))
    assert(restoredFileInfo.getUserIdentifier.equals(fileInfo1.getUserIdentifier))
    assert(restoredFileInfo.getPartitionType.equals(fileInfo1.getPartitionType))
  }

  test("fromAndToPbFileInfoMap") {
    val pbFileInfoMap = PbSerDeUtils.toPbFileInfoMap(fileInfoMap)
    val restoredFileInfoMap = PbSerDeUtils.fromPbFileInfoMap(pbFileInfoMap, cache)
    val restoredFileInfo1 = restoredFileInfoMap.get("file1")
    val restoredFileInfo2 = restoredFileInfoMap.get("file2")

    assert(restoredFileInfoMap.size().equals(fileInfoMap.size()))
    assert(restoredFileInfo1.getFilePath.equals(fileInfo1.getFilePath))
    assert(restoredFileInfo1.getChunkOffsets.equals(fileInfo1.getChunkOffsets))
    assert(restoredFileInfo1.getUserIdentifier.equals(fileInfo1.getUserIdentifier))
    assert(restoredFileInfo2.getFilePath.equals(fileInfo2.getFilePath))
    assert(restoredFileInfo2.getChunkOffsets.equals(fileInfo2.getChunkOffsets))
    assert(restoredFileInfo2.getUserIdentifier.equals(fileInfo2.getUserIdentifier))
  }

  test("fromAndToPbUserIdentifier") {
    val pbUserIdentifier = PbSerDeUtils.toPbUserIdentifier(userIdentifier1)
    val restoredUserIdentifier = PbSerDeUtils.fromPbUserIdentifier(pbUserIdentifier)

    assert(restoredUserIdentifier.equals(userIdentifier1))
  }

  test("fromAndToPbResourceConsumption") {
    val pbResourceConsumption = PbSerDeUtils.toPbResourceConsumption(resourceConsumption1)
    val restoredResourceConsumption = PbSerDeUtils.fromPbResourceConsumption(pbResourceConsumption)

    assert(restoredResourceConsumption.equals(resourceConsumption1))
  }

  test("fromAndToPbUserResourceConsumption") {
    val pbUserResourceConsumption =
      PbSerDeUtils.toPbUserResourceConsumption(userResourceConsumption)
    val restoredUserResourceConsumption =
      PbSerDeUtils.fromPbUserResourceConsumption(pbUserResourceConsumption)

    assert(restoredUserResourceConsumption.equals(userResourceConsumption))
  }

  test("fromAndToPbWorkerInfo") {
    val pbWorkerInfo = PbSerDeUtils.toPbWorkerInfo(workerInfo1, false)
    val pbWorkerInfoWithEmptyResource = PbSerDeUtils.toPbWorkerInfo(workerInfo1, true)
    val restoredWorkerInfo = PbSerDeUtils.fromPbWorkerInfo(pbWorkerInfo)
    val restoredWorkerInfoWithEmptyResource =
      PbSerDeUtils.fromPbWorkerInfo(pbWorkerInfoWithEmptyResource)

    assert(restoredWorkerInfo.equals(workerInfo1))
    assert(restoredWorkerInfoWithEmptyResource.userResourceConsumption.equals(new util.HashMap[
      UserIdentifier,
      ResourceConsumption]()))
  }

  test("fromAndToPbPartitionLocation") {
    val pbPartitionLocation = PbSerDeUtils.toPbPartitionLocation(partitionLocation1)
    val restoredPartitionLocation = PbSerDeUtils.fromPbPartitionLocation(pbPartitionLocation)

    assert(restoredPartitionLocation.equals(partitionLocation1))
  }

  test("fromAndToPbWorkerResource") {
    val pbWorkerResource = PbSerDeUtils.toPbWorkerResource(workerResource)
    val restoredWorkerResource = PbSerDeUtils.fromPbWorkerResource(pbWorkerResource)

    assert(restoredWorkerResource.equals(workerResource))
  }
}
