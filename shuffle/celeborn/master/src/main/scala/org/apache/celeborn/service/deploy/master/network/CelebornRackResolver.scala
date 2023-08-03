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

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

import com.google.common.base.Strings
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.CommonConfigurationKeysPublic
import org.apache.hadoop.net.{CachedDNSToSwitchMapping, DNSToSwitchMapping, NetworkTopology, Node, NodeBase, ScriptBasedMapping}
import org.apache.hadoop.util.ReflectionUtils

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.util.CelebornHadoopUtils

class CelebornRackResolver(celebornConf: CelebornConf) extends Logging {

  private val dnsToSwitchMapping: DNSToSwitchMapping = {
    val conf: Configuration = CelebornHadoopUtils.newConfiguration(celebornConf)
    val dnsToSwitchMappingClass =
      conf.getClass(
        CommonConfigurationKeysPublic.NET_TOPOLOGY_NODE_SWITCH_MAPPING_IMPL_KEY,
        classOf[ScriptBasedMapping],
        classOf[DNSToSwitchMapping])
    ReflectionUtils.newInstance(dnsToSwitchMappingClass, conf)
      .asInstanceOf[DNSToSwitchMapping] match {
      case c: CachedDNSToSwitchMapping => c
      case o => new CachedDNSToSwitchMapping(o)
    }
  }

  def resolve(hostName: String): Node = {
    coreResolve(Seq(hostName)).head
  }

  def resolve(hostNames: Seq[String]): Seq[Node] = {
    coreResolve(hostNames)
  }

  private def coreResolve(hostNames: Seq[String]): Seq[Node] = {
    if (hostNames.isEmpty) {
      return Seq.empty
    }
    val nodes = new ArrayBuffer[Node]
    // dnsToSwitchMapping is thread-safe
    val rNameList = dnsToSwitchMapping.resolve(hostNames.toList.asJava).asScala
    if (rNameList == null || rNameList.isEmpty) {
      hostNames.foreach(nodes += new NodeBase(_, NetworkTopology.DEFAULT_RACK))
      logInfo(s"Got an error when resolving hostNames. " +
        s"Falling back to ${NetworkTopology.DEFAULT_RACK} for all")
    } else {
      for ((hostName, rName) <- hostNames.zip(rNameList)) {
        if (Strings.isNullOrEmpty(rName)) {
          nodes += new NodeBase(hostName, NetworkTopology.DEFAULT_RACK)
          logDebug(s"Could not resolve $hostName. " +
            s"Falling back to ${NetworkTopology.DEFAULT_RACK}")
        } else {
          nodes += new NodeBase(hostName, rName)
        }
      }
    }
    nodes.toList
  }

  def isOnSameRack(primaryHost: String, replicaHost: String): Boolean = {
    val primaryNode = resolve(primaryHost)
    val replicaNode = resolve(replicaHost)
    if (primaryNode == null || replicaNode == null) {
      false
    } else {
      primaryNode.getNetworkLocation == replicaNode.getNetworkLocation
    }
  }
}
