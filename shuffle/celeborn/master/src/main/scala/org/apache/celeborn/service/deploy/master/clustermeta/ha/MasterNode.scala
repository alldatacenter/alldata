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

package org.apache.celeborn.service.deploy.master.clustermeta.ha

import java.io.IOException
import java.net.{InetAddress, InetSocketAddress}

import org.apache.ratis.util.NetUtils

import org.apache.celeborn.common.internal.Logging

case class MasterNode(
    nodeId: String,
    ratisHost: String,
    ratisPort: Int,
    rpcHost: String,
    rpcPort: Int) {

  def isRatisHostUnresolved: Boolean = ratisAddr.isUnresolved

  def ratisIpAddr: InetAddress = ratisAddr.getAddress

  def ratisEndpoint: String = ratisHost + ":" + ratisPort

  def rpcEndpoint: String = rpcHost + ":" + rpcPort

  lazy val ratisAddr = MasterNode.createSocketAddr(ratisHost, ratisPort)

  lazy val rpcAddr = MasterNode.createSocketAddr(rpcHost, rpcPort)
}

object MasterNode extends Logging {

  class Builder {
    private var nodeId: String = _
    private var ratisHost: String = _
    private var ratisPort = 0
    private var rpcHost: String = _
    private var rpcPort = 0

    def setNodeId(nodeId: String): this.type = {
      this.nodeId = nodeId
      this
    }

    def setHost(host: String): this.type = {
      this.ratisHost = host
      this.rpcHost = host
      this
    }

    def setRatisHost(ratisHost: String): this.type = {
      this.ratisHost = ratisHost
      this
    }

    def setRpcHost(rpcHost: String): this.type = {
      this.rpcHost = rpcHost
      this
    }

    def setRatisPort(ratisPort: Int): this.type = {
      this.ratisPort = ratisPort
      this
    }

    def setRpcPort(rpcPort: Int): this.type = {
      this.rpcPort = rpcPort
      this
    }

    def build: MasterNode = MasterNode(nodeId, ratisHost, ratisPort, rpcHost, rpcPort)
  }

  private def createSocketAddr(host: String, port: Int): InetSocketAddress = {
    val socketAddr: InetSocketAddress = {
      try {
        NetUtils.createSocketAddr(host, port)
      } catch {
        case e: Throwable =>
          throw new IOException(
            s"Couldn't create socket address for $host:$port",
            e)
      }
    }
    if (socketAddr.isUnresolved)
      logError(s"Address of $host:$port couldn't be resolved. " +
        s"Proceeding with unresolved host to create Ratis ring.")

    socketAddr
  }
}
