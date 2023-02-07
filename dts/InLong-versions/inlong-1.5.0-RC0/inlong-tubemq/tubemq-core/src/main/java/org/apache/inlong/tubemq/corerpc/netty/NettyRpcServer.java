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

import static org.apache.inlong.tubemq.corebase.utils.AddressUtils.getRemoteAddressIP;
import com.google.protobuf.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLEngine;
import org.apache.inlong.tubemq.corebase.protobuf.generated.RPCProtos;
import org.apache.inlong.tubemq.corerpc.RequestWrapper;
import org.apache.inlong.tubemq.corerpc.RpcConfig;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.RpcDataPack;
import org.apache.inlong.tubemq.corerpc.codec.PbEnDecoder;
import org.apache.inlong.tubemq.corerpc.exception.ServerNotReadyException;
import org.apache.inlong.tubemq.corerpc.protocol.Protocol;
import org.apache.inlong.tubemq.corerpc.protocol.ProtocolFactory;
import org.apache.inlong.tubemq.corerpc.protocol.RpcProtocol;
import org.apache.inlong.tubemq.corerpc.server.RequestContext;
import org.apache.inlong.tubemq.corerpc.server.ServiceRpcServer;
import org.apache.inlong.tubemq.corerpc.utils.MixUtils;
import org.apache.inlong.tubemq.corerpc.utils.TSSLEngineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty Rpc Server
 */
public class NettyRpcServer implements ServiceRpcServer {

    private static final Logger logger =
            LoggerFactory.getLogger(NettyRpcServer.class);
    private static final ConcurrentHashMap<String, AtomicLong> errParseAddrMap =
            new ConcurrentHashMap<>();
    private static AtomicLong lastParseTime = new AtomicLong(0);
    private final ConcurrentHashMap<Integer, Protocol> protocols =
            new ConcurrentHashMap<>();
    private ServerBootstrap bootstrap;
    private EventLoopGroup acceptorGroup;
    private EventLoopGroup workerGroup;
    private boolean enableBusyWait;
    private AtomicBoolean started = new AtomicBoolean(false);
    private int protocolType = RpcProtocol.RPC_PROTOCOL_TCP;
    private boolean isOverTLS;
    private String keyStorePath = "";
    private String keyStorePassword = "";
    private boolean needTwoWayAuthentic = false;
    private String trustStorePath = "";
    private String trustStorePassword = "";

    /**
     * create a server with rpc config info
     *
     * @param conf
     * @throws Exception
     */
    public NettyRpcServer(RpcConfig conf) throws Exception {
        this.isOverTLS = conf.getBoolean(RpcConstants.TLS_OVER_TCP, false);
        if (this.isOverTLS) {
            this.protocolType = RpcProtocol.RPC_PROTOCOL_TLS;
            this.keyStorePath = conf.getString(RpcConstants.TLS_KEYSTORE_PATH);
            this.keyStorePassword = conf.getString(RpcConstants.TLS_KEYSTORE_PASSWORD);
            this.needTwoWayAuthentic = conf.getBoolean(RpcConstants.TLS_TWO_WAY_AUTHENTIC, false);
            if (this.needTwoWayAuthentic) {
                this.trustStorePath = conf.getString(RpcConstants.TLS_TRUSTSTORE_PATH);
                this.trustStorePassword = conf.getString(RpcConstants.TLS_TRUSTSTORE_PASSWORD);
            }
            if (keyStorePath == null || keyStorePassword == null) {
                throw new Exception(new StringBuilder(512).append("Required parameters: ")
                        .append(RpcConstants.TLS_KEYSTORE_PATH).append(" or ")
                        .append(RpcConstants.TLS_KEYSTORE_PASSWORD).append(" for TLS!").toString());
            }
            if (this.needTwoWayAuthentic) {
                if (trustStorePath == null || trustStorePassword == null) {
                    throw new Exception(new StringBuilder(512).append("Required parameters: ")
                            .append(RpcConstants.TLS_TRUSTSTORE_PATH).append(" or ")
                            .append(RpcConstants.TLS_TRUSTSTORE_PASSWORD).append(" for TLS!").toString());
                }
            }
        }
        this.enableBusyWait = conf.getBoolean(RpcConstants.NETTY_TCP_ENABLEBUSYWAIT, false);
        int bossCount =
                conf.getInt(RpcConstants.BOSS_COUNT,
                        RpcConstants.CFG_DEFAULT_BOSS_COUNT);
        int workerCount =
                conf.getInt(RpcConstants.WORKER_COUNT,
                        RpcConstants.CFG_DEFAULT_SERVER_WORKER_COUNT);
        this.acceptorGroup = EventLoopUtil.newEventLoopGroup(bossCount, false,
                new DefaultThreadFactory("tcpSource-nettyBoss-threadGroup"));
        this.workerGroup = EventLoopUtil
                .newEventLoopGroup(workerCount, enableBusyWait,
                        new DefaultThreadFactory("tcpSource-nettyWorker-threadGroup"));
        this.bootstrap = new ServerBootstrap();
        bootstrap.channel(EventLoopUtil.getServerSocketChannelClass(workerGroup));
        EventLoopUtil.enableTriggeredMode(bootstrap);
        bootstrap.group(acceptorGroup, workerGroup);
        bootstrap.childOption(ChannelOption.TCP_NODELAY,
                conf.getBoolean(RpcConstants.TCP_NODELAY, true));
        bootstrap.childOption(ChannelOption.SO_REUSEADDR,
                conf.getBoolean(RpcConstants.TCP_REUSEADDRESS, true));
        int nettyWriteHighMark =
                conf.getInt(RpcConstants.NETTY_WRITE_HIGH_MARK, 64 * 1024);
        int nettyWriteLowMark =
                conf.getInt(RpcConstants.NETTY_WRITE_LOW_MARK, 32 * 1024);
        bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(nettyWriteLowMark, nettyWriteHighMark));
        int nettySendBuf = conf.getInt(RpcConstants.NETTY_TCP_SENDBUF, -1);
        if (nettySendBuf > 0) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, nettySendBuf);
        }
        int nettyRecvBuf = conf.getInt(RpcConstants.NETTY_TCP_RECEIVEBUF, -1);
        if (nettyRecvBuf > 0) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, nettyRecvBuf);
        }
    }

    @Override
    public void start(int listenPort) throws Exception {
        if (this.started.get()) {
            return;
        }
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel socketChannel) {
                if (isOverTLS) {
                    try {
                        SSLEngine sslEngine =
                                TSSLEngineUtil.createSSLEngine(keyStorePath, trustStorePath,
                                        keyStorePassword, trustStorePassword, false, needTwoWayAuthentic);
                        socketChannel.pipeline().addLast("ssl", new SslHandler(sslEngine));
                    } catch (Throwable t) {
                        logger.error(
                                "TLS NettyRpcServer init SSLEngine error, system auto exit!", t);
                        System.exit(1);
                    }
                }
                // Encode the data handler
                socketChannel.pipeline().addLast("protocolEncoder", new NettyProtocolDecoder());
                // Decode the bytes into a Rpc Data Pack
                socketChannel.pipeline().addLast("protocolDecoder", new NettyProtocolEncoder());
                // tube netty Server handler
                socketChannel.pipeline().addLast("serverHandler", new NettyServerHandler(protocolType));
            }
        });
        bootstrap.bind(new InetSocketAddress(listenPort)).sync();
        this.started.set(true);
        if (isOverTLS) {
            logger.info(new StringBuilder(256)
                    .append("TLS RpcServer started, listen port: ")
                    .append(listenPort).toString());
        } else {
            logger.info(new StringBuilder(256)
                    .append("TCP RpcServer started, listen port: ")
                    .append(listenPort).toString());
        }
    }

    @Override
    public void publishService(String serviceName, Object serviceInstance,
            ExecutorService threadPool) throws Exception {
        Protocol protocol = protocols.get(protocolType);
        if (protocol == null) {
            if (ProtocolFactory.getProtocol(protocolType) == null) {
                throw new Exception(new StringBuilder(256)
                        .append("Invalid protocol type ").append(protocolType)
                        .append("! You have to register you new protocol before publish service.").toString());
            }
            protocol = ProtocolFactory.getProtocolInstance(protocolType);
            protocols.put(protocolType, protocol);
        }
        protocol.registerService(isOverTLS, serviceName, serviceInstance, threadPool);
    }

    @Override
    public void removeService(int protocolType, String serviceName) throws Exception {
        Protocol protocol = protocols.get(protocolType);
        if (protocol != null) {
            protocol.removeService(serviceName);
        }
    }

    @Override
    public void removeAllService(int protocolType) throws Exception {
        Protocol protocol = protocols.get(protocolType);
        if (protocol != null) {
            protocol.removeAllService();
        }
    }

    @Override
    public boolean isServiceStarted() {
        return this.started.get();
    }

    @Override
    public void stop() throws Exception {
        if (!this.started.get()) {
            return;
        }
        if (this.started.compareAndSet(true, false)) {
            logger.info("Stopping RpcServer...");
            logger.info("RpcServer stop successfully.");
        }
    }

    /**
     * Netty Server Handler
     */
    private class NettyServerHandler extends ChannelInboundHandlerAdapter {

        private int protocolType = RpcProtocol.RPC_PROTOCOL_TCP;

        public NettyServerHandler(int protocolType) {
            this.protocolType = protocolType;
        }

        /**
         * Invoked when an exception was raised by an I/O thread or a
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            if (!(e.getCause() instanceof IOException)) {
                logger.error("catch some exception not IOException {}", e);
            }
            ctx.fireExceptionCaught(e);
        }

        /**
         * Invoked when a message object was received
         * from a remote peer.
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            logger.debug("server message receive!");
            if (!(msg instanceof RpcDataPack)) {
                return;
            }
            logger.debug("server RpcDataPack message receive!");
            RpcDataPack dataPack = (RpcDataPack) msg;
            RPCProtos.RpcConnHeader connHeader;
            RPCProtos.RequestHeader requestHeader;
            RPCProtos.RequestBody rpcRequestBody;
            int rmtVersion = RpcProtocol.RPC_PROTOCOL_VERSION;
            Channel channel = ctx.channel();
            if (channel == null) {
                return;
            }
            String rmtaddrIp = getRemoteAddressIP(channel);
            try {
                if (!isServiceStarted()) {
                    throw new ServerNotReadyException("RpcServer is not running yet");
                }
                List<ByteBuffer> req = dataPack.getDataLst();
                ByteBufferInputStream dis = new ByteBufferInputStream(req);
                connHeader = RPCProtos.RpcConnHeader.parseDelimitedFrom(dis);
                requestHeader = RPCProtos.RequestHeader.parseDelimitedFrom(dis);
                rmtVersion = requestHeader.getProtocolVer();
                rpcRequestBody = RPCProtos.RequestBody.parseDelimitedFrom(dis);
            } catch (Throwable e1) {
                if (!(e1 instanceof ServerNotReadyException)) {
                    if (rmtaddrIp != null) {
                        AtomicLong count = errParseAddrMap.get(rmtaddrIp);
                        if (count == null) {
                            AtomicLong tmpCount = new AtomicLong(0);
                            count = errParseAddrMap.putIfAbsent(rmtaddrIp, tmpCount);
                            if (count == null) {
                                count = tmpCount;
                            }
                        }
                        count.incrementAndGet();
                        long befTime = lastParseTime.get();
                        long curTime = System.currentTimeMillis();
                        if (curTime - befTime > 180000) {
                            if (lastParseTime.compareAndSet(befTime, System.currentTimeMillis())) {
                                logger.warn(new StringBuilder(512)
                                        .append("[Abnormal Visit] Abnormal Message Content visit list is :")
                                        .append(errParseAddrMap).toString());
                                errParseAddrMap.clear();
                            }
                        }
                    }
                }
                List<ByteBuffer> res =
                        prepareResponse(null, rmtVersion, RPCProtos.ResponseHeader.Status.FATAL,
                                e1.getClass().getName(), new StringBuilder(512)
                                        .append("IPC server unable to read call parameters:")
                                        .append(e1.getMessage()).toString());
                if (res != null) {
                    dataPack.setDataLst(res);
                    channel.writeAndFlush(dataPack);
                }
                return;
            }
            try {
                RequestWrapper requestWrapper =
                        new RequestWrapper(requestHeader.getServiceType(),
                                this.protocolType, requestHeader.getProtocolVer(),
                                connHeader.getFlag(), rpcRequestBody.getTimeout());
                requestWrapper.setMethodId(rpcRequestBody.getMethod());
                requestWrapper.setRequestData(PbEnDecoder.pbDecode(true,
                        rpcRequestBody.getMethod(), rpcRequestBody.getRequest().toByteArray()));
                requestWrapper.setSerialNo(dataPack.getSerialNo());
                RequestContext context =
                        new NettyRequestContext(requestWrapper, ctx, System.currentTimeMillis());
                protocols.get(this.protocolType).handleRequest(context, rmtaddrIp);
            } catch (Throwable ee) {
                List<ByteBuffer> res =
                        prepareResponse(null, rmtVersion, RPCProtos.ResponseHeader.Status.FATAL,
                                ee.getClass().getName(), new StringBuilder(512)
                                        .append("IPC server handle request error :")
                                        .append(ee.getMessage()).toString());
                if (res != null) {
                    dataPack.setDataLst(res);
                    ctx.channel().writeAndFlush(dataPack);
                }
                return;
            }
        }

        /**
         * prepare and write the message into an list of byte buffers
         *
         * @param value
         * @param status
         * @param errorClass
         * @param error
         * @return
         */
        protected List<ByteBuffer> prepareResponse(Object value, int rmtVersion,
                RPCProtos.ResponseHeader.Status status,
                String errorClass, String error) {
            ByteBufferOutputStream buf = new ByteBufferOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            errorClass = MixUtils.replaceClassNamePrefix(errorClass, true, rmtVersion);
            try {
                RPCProtos.RpcConnHeader.Builder connBuilder =
                        RPCProtos.RpcConnHeader.newBuilder();
                connBuilder.setFlag(RpcConstants.RPC_FLAG_MSG_TYPE_RESPONSE);
                connBuilder.build().writeDelimitedTo(out);
                RPCProtos.ResponseHeader.Builder builder =
                        RPCProtos.ResponseHeader.newBuilder();
                builder.setStatus(status);
                builder.build().writeDelimitedTo(out);
                if (error != null) {
                    RPCProtos.RspExceptionBody.Builder b =
                            RPCProtos.RspExceptionBody.newBuilder();
                    b.setExceptionName(errorClass);
                    b.setStackTrace(error);
                    b.build().writeDelimitedTo(out);
                } else {
                    if (value != null) {
                        ((Message) value).writeDelimitedTo(out);
                    }
                }
            } catch (IOException e) {
                logger.warn(new StringBuilder(512)
                        .append("Exception while creating response ")
                        .append(e).toString());
            }
            return buf.getBufferList();
        }
    }
}
