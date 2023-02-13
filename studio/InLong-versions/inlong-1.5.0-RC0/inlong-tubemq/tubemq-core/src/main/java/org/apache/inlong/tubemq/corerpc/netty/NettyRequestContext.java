/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.netty;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.inlong.tubemq.corebase.protobuf.generated.RPCProtos;
import org.apache.inlong.tubemq.corerpc.RequestWrapper;
import org.apache.inlong.tubemq.corerpc.ResponseWrapper;
import org.apache.inlong.tubemq.corerpc.RpcDataPack;
import org.apache.inlong.tubemq.corerpc.codec.PbEnDecoder;
import org.apache.inlong.tubemq.corerpc.server.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyRequestContext implements RequestContext {

    private static final Logger logger =
            LoggerFactory.getLogger(NettyRequestContext.class);

    private RequestWrapper request;
    private ChannelHandlerContext ctx;
    private long receiveTime;

    public NettyRequestContext(RequestWrapper request,
            ChannelHandlerContext ctx,
            long receiveTime) {
        this.request = request;
        this.ctx = ctx;
        this.receiveTime = receiveTime;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return this.ctx.channel().remoteAddress();
    }

    @Override
    public RequestWrapper getRequest() {
        return request;
    }

    @Override
    public void write(ResponseWrapper response) throws Exception {
        RpcDataPack dataPack;
        if ((System.currentTimeMillis() - receiveTime) >= request.getTimeout()) {
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuilder(512)
                        .append("Timeout,so give up send response to client.RequestId:")
                        .append(request.getSerialNo()).append(".client:")
                        .append(ctx.channel().remoteAddress())
                        .append(",process time:")
                        .append(System.currentTimeMillis() - receiveTime)
                        .append(",timeout:").append(request.getTimeout()).toString());
            }
            return;
        }
        dataPack = new RpcDataPack(response.getSerialNo(), prepareResponse(response));
        ChannelFuture wf = ctx.channel().writeAndFlush(dataPack);
        wf.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    Throwable exception = future.cause();
                    if (exception != null) {
                        if (logger.isDebugEnabled()) {
                            if (IOException.class.isAssignableFrom(exception.getClass())) {
                                logger.debug(new StringBuilder(512)
                                        .append("server write response error.")
                                        .append("reason: ")
                                        .append(future.channel().toString())
                                        .append(exception.toString()).toString());
                            } else {
                                logger.debug(new StringBuilder(512)
                                        .append("server write response error.")
                                        .append("reason: ")
                                        .append(future.channel().toString())
                                        .append(future.cause()).toString());
                            }
                        }
                    }
                }
            }
        });
    }

    protected List<ByteBuffer> prepareResponse(ResponseWrapper response) {
        ByteBufferOutputStream buf = new ByteBufferOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        try {
            RPCProtos.RpcConnHeader.Builder connBuilder =
                    RPCProtos.RpcConnHeader.newBuilder();
            connBuilder.setFlag(response.getFlagId());
            connBuilder.build().writeDelimitedTo(out);
            RPCProtos.ResponseHeader.Builder rpcBuilder =
                    RPCProtos.ResponseHeader.newBuilder();
            if (response.isSuccess()) {
                rpcBuilder.setStatus(RPCProtos.ResponseHeader.Status.SUCCESS);
                rpcBuilder.setProtocolVer(response.getProtocolVersion());
                rpcBuilder.build().writeDelimitedTo(out);
                RPCProtos.RspResponseBody.Builder dataBuilder =
                        RPCProtos.RspResponseBody.newBuilder();
                dataBuilder.setMethod(response.getMethodId());
                if (response.getResponseData() != null) {
                    try {
                        dataBuilder.setData(ByteString
                                .copyFrom(PbEnDecoder.pbEncode(response.getResponseData())));
                    } catch (Throwable ee) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(new StringBuilder(512)
                                    .append("Exception while creating response ")
                                    .append(ee).toString());
                        }
                    }
                }
                dataBuilder.build().writeDelimitedTo(out);
            } else {
                rpcBuilder.setStatus(RPCProtos.ResponseHeader.Status.ERROR);
                rpcBuilder.setProtocolVer(response.getProtocolVersion());
                rpcBuilder.build().writeDelimitedTo(out);
                RPCProtos.RspExceptionBody.Builder b =
                        RPCProtos.RspExceptionBody.newBuilder();
                b.setExceptionName(response.getErrMsg());
                b.setStackTrace(response.getStackTrace());
                b.build().writeDelimitedTo(out);
            }
        } catch (IOException e) {
            logger.warn(new StringBuilder(512)
                    .append("Exception while creating response ")
                    .append(e).toString());
        }
        return buf.getBufferList();
    }

    @Override
    public long getReceiveTime() {
        return this.receiveTime;
    }
}
