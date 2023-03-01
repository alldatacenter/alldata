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

package org.apache.celeborn.common.rpc

import org.apache.celeborn.common.exception.CelebornException

/**
 * An address identifier for an RPC endpoint.
 *
 * The `rpcAddress` may be null, in which case the endpoint is registered via a client-only
 * connection and can only be reached via the client that sent the endpoint reference.
 *
 * @param rpcAddress The socket address of the endpoint. It's `null` when this address pointing to
 *                   an endpoint in a client `NettyRpcEnv`.
 * @param name Name of the endpoint.
 */
private[celeborn] case class RpcEndpointAddress(rpcAddress: RpcAddress, name: String) {

  require(name != null, "RpcEndpoint name must be provided.")

  def this(host: String, port: Int, name: String) = {
    this(RpcAddress(host, port), name)
  }

  override val toString =
    if (rpcAddress != null) {
      s"rss://$name@${rpcAddress.host}:${rpcAddress.port}"
    } else {
      s"rss-client://$name"
    }
}

private[celeborn] object RpcEndpointAddress {

  def apply(host: String, port: Int, name: String): RpcEndpointAddress = {
    new RpcEndpointAddress(host, port, name)
  }

  def apply(essUrl: String): RpcEndpointAddress = {
    try {
      val uri = new java.net.URI(essUrl)
      val host = uri.getHost
      val port = uri.getPort
      val name = uri.getUserInfo
      if (uri.getScheme != "rss" ||
        host == null ||
        port < 0 ||
        name == null ||
        (uri.getPath != null && !uri.getPath.isEmpty) || // uri.getPath returns "" instead of null
        uri.getFragment != null ||
        uri.getQuery != null) {
        throw new CelebornException("Invalid RSS URL: " + essUrl)
      }
      new RpcEndpointAddress(host, port, name)
    } catch {
      case e: java.net.URISyntaxException =>
        throw new CelebornException("Invalid RSS URL: " + essUrl, e)
    }
  }
}
