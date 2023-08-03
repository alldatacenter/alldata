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

package org.apache.celeborn.common.meta

import java.util

import org.apache.celeborn.CelebornFunSuite

class DeviceInfoSuite extends CelebornFunSuite {

  test("test getMountPoint") {
    val mountPoints = new util.HashSet[String]()
    mountPoints.add("/")
    mountPoints.add("/mnt/disk1")
    mountPoints.add("/mnt/disk2")
    mountPoints.add("/data")

    assert(DeviceInfo.getMountPoint("/mnt/disk1/data", mountPoints) === "/mnt/disk1")
    assert(DeviceInfo.getMountPoint("/mnt/disk1/data", mountPoints) === "/mnt/disk1")
    assert(DeviceInfo.getMountPoint("/mnt/disk2/data", mountPoints) === "/mnt/disk2")
    assert(DeviceInfo.getMountPoint("/mnt/disk3/data", mountPoints) === "/")
    assert(DeviceInfo.getMountPoint("/data", mountPoints) === "/data")
    assert(DeviceInfo.getMountPoint("/data/data", mountPoints) === "/data")
    assert(DeviceInfo.getMountPoint("/data1/data", mountPoints) === "/")
  }
}
