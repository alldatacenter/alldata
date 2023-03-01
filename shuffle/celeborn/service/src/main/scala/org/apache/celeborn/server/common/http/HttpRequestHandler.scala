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

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil

import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.metrics.sink.PrometheusHttpRequestHandler
import org.apache.celeborn.server.common.{HttpService, Service}

@Sharable
class HttpRequestHandler(
    service: HttpService,
    prometheusHttpRequestHandler: PrometheusHttpRequestHandler)
  extends SimpleChannelInboundHandler[FullHttpRequest] with Logging {

  private val INVALID = "invalid"

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  override def channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    val uri = req.uri()
    val msg = handleRequest(uri)
    val response = msg match {
      case INVALID =>
        if (prometheusHttpRequestHandler != null) {
          prometheusHttpRequestHandler.handleRequest(uri)
        } else {
          s"invalid uri ${uri}"
        }
      case _ => msg
    }

    val res = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.OK,
      Unpooled.copiedBuffer(response, CharsetUtil.UTF_8))
    res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
    ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
  }

  def handleRequest(uri: String): String = {
    uri match {
      case "/workerInfo" =>
        service.getWorkerInfo
      case "/threadDump" =>
        service.getThreadDump
      case "/hostnames" if service.serviceName == Service.MASTER =>
        service.getHostnameList
      case "/applications" if service.serviceName == Service.MASTER =>
        service.getApplicationList
      case "/shuffles" =>
        service.getShuffleList
      case "/listTopDiskUsedApps" =>
        service.listTopDiskUseApps
      case _ => INVALID
    }
  }
}
