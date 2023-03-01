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

import java.net.{InetAddress, InetSocketAddress}

import org.apache.hadoop.yarn.nodelabels.CommonNodeLabelsManager.Host

case class MasterNode(
    nodeId: String,
    ratisAddr: InetSocketAddress,
    rpcAddr: InetSocketAddress) {

  def isRatisHostUnresolved: Boolean = ratisAddr.isUnresolved

  def ratisIpAddr: InetAddress = ratisAddr.getAddress

  def ratisPort: Int = ratisAddr.getPort

  def rpcPort: Int = rpcAddr.getPort

  def ratisEndpoint: String = ratisAddr.getHostName + ":" + ratisAddr.getPort

  def rpcEndpoint: String = rpcAddr.getHostName + ":" + rpcPort
}

object MasterNode {

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

    def build: MasterNode = MasterNode(
      nodeId,
      new InetSocketAddress(ratisHost, ratisPort),
      new InetSocketAddress(rpcHost, rpcPort))
  }
}
