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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.cluster.NodeAddrInfo;
import org.apache.inlong.tubemq.corebase.protobuf.generated.RPCProtos;
import org.apache.inlong.tubemq.corerpc.RequestWrapper;
import org.apache.inlong.tubemq.corerpc.ResponseWrapper;
import org.apache.inlong.tubemq.corerpc.RpcDataPack;
import org.apache.inlong.tubemq.corerpc.client.CallFuture;
import org.apache.inlong.tubemq.corerpc.client.Callback;
import org.apache.inlong.tubemq.corerpc.client.Client;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.apache.inlong.tubemq.corerpc.codec.PbEnDecoder;
import org.apache.inlong.tubemq.corerpc.exception.ClientClosedException;
import org.apache.inlong.tubemq.corerpc.exception.NetworkException;
import org.apache.inlong.tubemq.corerpc.utils.MixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The network Client for tube rpc service
 */
public class NettyClient implements Client {

    private static final Logger logger =
            LoggerFactory.getLogger(NettyClientHandler.class);
    private static final AtomicLong init = new AtomicLong(0);
    private static Timer timer;
    private final ConcurrentHashMap<Integer, Callback<ResponseWrapper>> requests =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Timeout> timeouts =
            new ConcurrentHashMap<>();
    private final AtomicInteger serialNoGenerator =
            new AtomicInteger(0);
    private final AtomicBoolean released = new AtomicBoolean(false);
    private NodeAddrInfo addressInfo;
    private final ClientFactory clientFactory;
    private Channel channel;
    private final long connectTimeout;
    private final AtomicBoolean closed = new AtomicBoolean(true);

    /**
     * Initial a netty client object
     *
     * @param clientFactory    the client factory
     * @param connectTimeout   the connection timeout
     */
    public NettyClient(ClientFactory clientFactory, long connectTimeout) {
        this.clientFactory = clientFactory;
        this.connectTimeout = connectTimeout;
        if (init.incrementAndGet() == 1) {
            timer = new HashedWheelTimer();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     * Set a channel
     *
     * @param channel      the channel
     * @param addressInfo   the address of the channel
     */
    public void setChannel(Channel channel, final NodeAddrInfo addressInfo) {
        this.channel = channel;
        this.addressInfo = addressInfo;
        this.closed.set(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.inlong.tubemq.corerpc.client.Client#call( org.apache.inlong.tubemq.corerpc.RequestWrapper,
     * org.apache.inlong.tubemq.corerpc.client.Callback, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ResponseWrapper call(RequestWrapper request, Callback callback,
            long timeout, TimeUnit timeUnit) throws Exception {
        if (closed.get()) {
            throw new ClientClosedException("Netty client has bean closed!");
        }
        request.setSerialNo(serialNoGenerator.incrementAndGet());
        RPCProtos.RpcConnHeader.Builder builder =
                RPCProtos.RpcConnHeader.newBuilder();
        builder.setFlag(request.getFlagId());
        final RPCProtos.RpcConnHeader connectionHeader =
                builder.build();
        RPCProtos.RequestHeader.Builder headerBuilder =
                RPCProtos.RequestHeader.newBuilder();
        headerBuilder.setServiceType(request.getServiceType());
        headerBuilder.setProtocolVer(request.getProtocolVersion());
        final RPCProtos.RequestHeader rpcHeader =
                headerBuilder.build();
        RPCProtos.RequestBody.Builder rpcBodyBuilder =
                RPCProtos.RequestBody.newBuilder();
        rpcBodyBuilder.setMethod(request.getMethodId());
        rpcBodyBuilder.setTimeout(request.getTimeout());
        rpcBodyBuilder
                .setRequest(ByteString.copyFrom(PbEnDecoder.pbEncode(request.getRequestData())));
        RPCProtos.RequestBody rpcBodyRequest = rpcBodyBuilder.build();
        ByteBufferOutputStream bbo = new ByteBufferOutputStream();
        connectionHeader.writeDelimitedTo(bbo);
        rpcHeader.writeDelimitedTo(bbo);
        rpcBodyRequest.writeDelimitedTo(bbo);
        RpcDataPack pack = new RpcDataPack(request.getSerialNo(), bbo.getBufferList());
        CallFuture<ResponseWrapper> future = new CallFuture<ResponseWrapper>(callback);
        requests.put(request.getSerialNo(), future);
        if (callback == null) {
            try {
                getChannel().writeAndFlush(pack);
                return future.get(timeout, timeUnit);
            } catch (Throwable e) {
                Callback<ResponseWrapper> callback1 =
                        requests.remove(request.getSerialNo());
                if (callback1 != null) {
                    if (closed.get()) {
                        throw new ClientClosedException("Netty client has bean closed!");
                    } else if (getChannel() == null) {
                        throw new ClientClosedException("Send failure for channel is null!");
                    } else {
                        throw e;
                    }
                }
            }
        } else {
            boolean inserted = false;
            try {
                timeouts.put(request.getSerialNo(),
                        timer.newTimeout(new TimeoutTask(request.getSerialNo()), timeout, timeUnit));
                inserted = true;
                // write data after build Timeout to avoid one request processed twice
                getChannel().writeAndFlush(pack);
            } catch (Throwable e) {
                Callback<ResponseWrapper> callback1 =
                        requests.remove(request.getSerialNo());
                if (callback1 != null) {
                    if (inserted) {
                        Timeout timeout1 = timeouts.remove(request.getSerialNo());
                        if (timeout1 != null) {
                            timeout1.cancel();
                        }
                    }
                    if (closed.get()) {
                        throw new ClientClosedException("Netty client has bean closed!");
                    } else if (getChannel() == null) {
                        throw new ClientClosedException("Channel is null!");
                    } else {
                        throw e;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public NodeAddrInfo getServerAddressInfo() {
        return this.addressInfo;
    }

    @Override
    public long getConnectTimeout() {
        return this.connectTimeout;
    }

    @Override
    public boolean isReady() {
        return (!this.closed.get()
                && channel != null
                && channel.isOpen()
                && channel.isWritable()
                && channel.isActive());
    }

    @Override
    public boolean isWritable() {
        return (!this.closed.get()
                && channel != null
                && channel.isWritable());
    }

    @Override
    public void close() {
        close(true);
    }

    /**
     * stop timer
     * remove clientFactory cache
     * handler unfinished callbacks
     * and close the channel
     *
     * @param removeParent    whether remove the object from client factory
     */
    @Override
    public void close(boolean removeParent) {
        if (this.released.compareAndSet(false, true)) {
            if (init.decrementAndGet() == 0) {
                timer.stop();
            }
        }
        if (this.closed.compareAndSet(false, true)) {
            String clientStr;
            if (this.channel != null) {
                clientStr = channel.toString();
            } else {
                clientStr = this.addressInfo.getHostPortStr();
            }
            if (removeParent) {
                this.clientFactory.removeClient(this.getServerAddressInfo());
            }
            if (!requests.isEmpty()) {
                ClientClosedException exception =
                        new ClientClosedException("Client has bean closed.");
                for (Integer serial : requests.keySet()) {
                    if (serial != null) {
                        Callback<ResponseWrapper> callback = requests.remove(serial);
                        if (callback != null) {
                            callback.handleError(exception);
                        }
                    }
                }
            }
            if (this.channel != null) {
                this.channel.close();
                this.channel = null;
            }
            logger.info(new StringBuilder(256).append("Client(")
                    .append(clientStr).append(") closed").toString());
        }
    }

    @Override
    public ClientFactory getClientFactory() {
        return clientFactory;
    }

    /**
     * tube NettyClientHandler
     */
    public class NettyClientHandler extends ChannelInboundHandlerAdapter {

        /**
         * Invoked when a message object was received from a remote peer.
         *
         * @param ctx     the channel handler context
         * @param e       the message event
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object e) {
            if (e instanceof RpcDataPack) {
                RpcDataPack dataPack = (RpcDataPack) e;
                Callback callback = requests.remove(dataPack.getSerialNo());
                if (callback != null) {
                    Timeout timeout = timeouts.remove(dataPack.getSerialNo());
                    if (timeout != null) {
                        timeout.cancel();
                    }
                    ResponseWrapper responseWrapper;
                    try {
                        ByteBufferInputStream in = new ByteBufferInputStream(dataPack.getDataLst());
                        RPCProtos.RpcConnHeader connHeader =
                                RPCProtos.RpcConnHeader.parseDelimitedFrom(in);
                        if (connHeader == null) {
                            // When the stream is closed, protobuf doesn't raise an EOFException,
                            // instead, it returns a null message object.
                            throw new EOFException();
                        }
                        RPCProtos.ResponseHeader rpcResponse =
                                RPCProtos.ResponseHeader.parseDelimitedFrom(in);
                        if (rpcResponse == null) {
                            // When the stream is closed, protobuf doesn't raise an EOFException,
                            // instead, it returns a null message object.
                            throw new EOFException();
                        }
                        RPCProtos.ResponseHeader.Status status = rpcResponse.getStatus();
                        if (status == RPCProtos.ResponseHeader.Status.SUCCESS) {
                            RPCProtos.RspResponseBody pbRpcResponse =
                                    RPCProtos.RspResponseBody.parseDelimitedFrom(in);
                            if (pbRpcResponse == null) {
                                // When the RPCProtos parse failed , protobuf doesn't raise an Exception,
                                // instead, it returns a null response object.
                                throw new NetworkException("Not found PBRpcResponse data!");
                            }
                            Object responseResult =
                                    PbEnDecoder.pbDecode(false, pbRpcResponse.getMethod(),
                                            pbRpcResponse.getData().toByteArray());

                            responseWrapper =
                                    new ResponseWrapper(connHeader.getFlag(), dataPack.getSerialNo(),
                                            rpcResponse.getServiceType(), rpcResponse.getProtocolVer(),
                                            pbRpcResponse.getMethod(), responseResult);
                        } else {
                            RPCProtos.RspExceptionBody exceptionResponse =
                                    RPCProtos.RspExceptionBody.parseDelimitedFrom(in);
                            if (exceptionResponse == null) {
                                // When the RPCProtos parse failed , protobuf doesn't raise an Exception,
                                // instead, it returns a null response object.
                                throw new NetworkException("Not found RpcException data!");
                            }
                            String exceptionName = exceptionResponse.getExceptionName();
                            exceptionName = MixUtils.replaceClassNamePrefix(exceptionName,
                                    false, rpcResponse.getProtocolVer());
                            responseWrapper =
                                    new ResponseWrapper(connHeader.getFlag(), dataPack.getSerialNo(),
                                            rpcResponse.getServiceType(), rpcResponse.getProtocolVer(),
                                            exceptionName, exceptionResponse.getStackTrace());
                        }
                        if (!responseWrapper.isSuccess()) {
                            Throwable remote =
                                    MixUtils.unwrapException(new StringBuilder(512)
                                            .append(responseWrapper.getErrMsg()).append("#")
                                            .append(responseWrapper.getStackTrace()).toString());
                            if (IOException.class.isAssignableFrom(remote.getClass())) {
                                NettyClient.this.close();
                            }
                        }
                        callback.handleResult(responseWrapper);
                    } catch (Throwable ee) {
                        responseWrapper =
                                new ResponseWrapper(-2, dataPack.getSerialNo(), -2, -2, -2, ee);
                        if (ee instanceof EOFException) {
                            NettyClient.this.close();
                        }
                        callback.handleResult(responseWrapper);
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Missing previous call info, maybe it has been timeout.");
                    }
                }
            }
        }

        /**
         * Invoked when an exception was raised by an I/O thread
         *
         * @param ctx   the channel handler context
         * @param e     the exception object
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
            Throwable t = e.getCause();
            if ((t instanceof IOException || t instanceof ReadTimeoutException
                    || t instanceof UnresolvedAddressException)) {
                if (t instanceof ReadTimeoutException) {
                    logger.info("Close client {} due to idle.", ctx.channel());
                }
                if (t instanceof UnresolvedAddressException) {
                    logger.info("UnresolvedAddressException for connect {} closed.", addressInfo.getHostPortStr());
                }
                NettyClient.this.close();
            } else {
                logger.error("catch some exception not IOException", e.getCause());
            }
        }

        /**
         * Invoked when a {@link Channel} was closed and all its related resources were released.
         *
         * @param ctx   the channel handler context
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            NettyClient.this.close();
        }
    }

    /**
     * Time out task call back handle
     */
    public class TimeoutTask implements TimerTask {

        private final int serialNo;

        public TimeoutTask(int serialNo) {
            this.serialNo = serialNo;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            Timeout timeout1 = timeouts.remove(serialNo);
            if (timeout1 != null) {
                timeout1.cancel();
            }
            final Callback callback = requests.remove(serialNo);
            if (callback != null) {
                channel.eventLoop().execute(
                        () -> callback.handleError(new TimeoutException("Request is timeout!")));
            }
        }
    }
}
