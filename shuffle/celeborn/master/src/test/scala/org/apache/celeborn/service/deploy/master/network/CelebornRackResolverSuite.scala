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

package org.apache.celeborn.service.deploy.master.network

import java.io.File

import org.apache.hadoop.fs.CommonConfigurationKeysPublic.{NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY, NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY}
import org.apache.hadoop.net.{Node, TableMapping}
import org.apache.hadoop.shaded.com.google.common.base.Charsets
import org.apache.hadoop.shaded.com.google.common.io.Files
import org.junit.Assert.assertEquals
import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.CelebornConf

class CelebornRackResolverSuite extends AnyFunSuite {

  test("Test TableMapping") {
    val hostName1 = "1.2.3.4"
    val hostName2 = "5.6.7.8"
    val mapFile: File = File.createTempFile(getClass.getSimpleName + ".testResolve1", ".txt")
    Files.asCharSink(mapFile, Charsets.UTF_8).write(
      hostName1 + " /rack1\n" + hostName2 + "\t/rack2\n")
    mapFile.deleteOnExit()

    val conf = new CelebornConf
    conf.set(
      "celeborn.hadoop." + NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY,
      classOf[TableMapping].getName)
    conf.set("celeborn.hadoop." + NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY, mapFile.getCanonicalPath)
    val resolver = new CelebornRackResolver(conf)

    val names = Seq(hostName1, hostName2)

    val result: Seq[Node] = resolver.resolve(names)
    assertEquals(names.size, result.size)
    assertEquals("/rack1", result(0).getNetworkLocation)
    assertEquals("/rack2", result(1).getNetworkLocation)
  }

  test("CELEBORN-446: RackResolver support getDistance") {
    val hostName1 = "1.2.3.4"
    val hostName2 = "1.2.3.5"
    val hostName3 = "1.2.3.6"
    val hostName4 = "1.2.3.7"
    val hostName5 = "1.2.3.8"
    val hostName6 = "1.2.3.9"
    val mapFile: File = File.createTempFile(getClass.getSimpleName + ".testResolve2", ".txt")
    Files.asCharSink(mapFile, Charsets.UTF_8).write(
      s"""
         |$hostName1 /default/rack1
         |$hostName2 /default/rack1
         |$hostName3 /default/rack2
         |$hostName4 /default/rack3
         |""".stripMargin)
    val conf = new CelebornConf
    conf.set(
      "celeborn.hadoop." + NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY,
      classOf[TableMapping].getName)
    conf.set("celeborn.hadoop." + NET_TOPOLOGY_TABLE_MAPPING_FILE_KEY, mapFile.getCanonicalPath)
    val resolver = new CelebornRackResolver(conf)

    assertEquals("/default/rack1", resolver.resolve(hostName1).getNetworkLocation)
    assertEquals("/default/rack2", resolver.resolve(hostName3).getNetworkLocation)

    assertEquals(true, resolver.isOnSameRack(hostName1, hostName2))
    assertEquals(false, resolver.isOnSameRack(hostName1, hostName3))
    assertEquals(false, resolver.isOnSameRack(hostName3, hostName4))

    // check one side don't have rack info
    assertEquals(false, resolver.isOnSameRack(hostName1, hostName5))

    // check both side don't have rack info
    assertEquals(true, resolver.isOnSameRack(hostName5, hostName6))
  }
}
