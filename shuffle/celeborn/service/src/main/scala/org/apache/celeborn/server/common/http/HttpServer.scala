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

package org.apache.celeborn.server.common.http

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelFuture, ChannelInitializer}
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LoggingHandler, LogLevel}

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.network.util.{IOMode, NettyUtils}
import org.apache.celeborn.common.util.Utils

class HttpServer(
    role: String,
    host: String,
    port: Int,
    channelInitializer: ChannelInitializer[_]) extends Logging {

  private var bootstrap: ServerBootstrap = _
  private var bindFuture: ChannelFuture = _
  @volatile private var isStarted = false

  @throws[Exception]
  def start(): Unit = synchronized {
    val boss = NettyUtils.createEventLoop(IOMode.NIO, 1, role + "-http-boss")
    val worker = NettyUtils.createEventLoop(IOMode.NIO, 2, role + "-http-worker")
    bootstrap = new ServerBootstrap
    bootstrap
      .group(boss, worker)
      .handler(new LoggingHandler(LogLevel.DEBUG))
      .channel(classOf[NioServerSocketChannel])
      .childHandler(channelInitializer)

    bindFuture = bootstrap.bind(new InetSocketAddress(host, port)).sync
    bindFuture.syncUninterruptibly()
    logInfo(s"$role: HttpServer started on port $port.")
    isStarted = true
  }

  def stop(): Unit = synchronized {
    if (isStarted) {
      logInfo(s"$role: Stopping HttpServer")
      if (bindFuture != null) {
        // close is a local operation and should finish within milliseconds; timeout just to be safe
        bindFuture.channel.close.awaitUninterruptibly(10, TimeUnit.SECONDS)
        bindFuture = null
      }
      if (bootstrap != null && bootstrap.config.group != null) {
        Utils.tryLogNonFatalError {
          bootstrap.config.group.shutdownGracefully(3, 5, TimeUnit.SECONDS)
        }
      }
      if (bootstrap != null && bootstrap.config.childGroup != null) {
        Utils.tryLogNonFatalError {
          bootstrap.config.childGroup.shutdownGracefully(3, 5, TimeUnit.SECONDS)
        }
      }
      bootstrap = null
      logInfo(s"$role: HttpServer stopped.")
      isStarted = false
    }
  }
}
