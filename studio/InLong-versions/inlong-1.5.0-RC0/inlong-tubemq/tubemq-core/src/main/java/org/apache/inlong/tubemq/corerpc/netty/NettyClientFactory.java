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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLEngine;

import org.apache.inlong.tubemq.corebase.cluster.NodeAddrInfo;
import org.apache.inlong.tubemq.corerpc.RpcConfig;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.client.Client;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.apache.inlong.tubemq.corerpc.exception.LocalConnException;
import org.apache.inlong.tubemq.corerpc.utils.TSSLEngineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network communication between service processes based on netty
 * see @link MessageSessionFactory Manage network connections
 */
public class NettyClientFactory implements ClientFactory {

    private static final Logger logger =
            LoggerFactory.getLogger(NettyClientFactory.class);
    protected final ConcurrentHashMap<String, Client> clients =
            new ConcurrentHashMap<>();
    protected AtomicBoolean shutdown = new AtomicBoolean(true);
    private EventLoopGroup eventLoopGroup;
    private AtomicInteger workerIdCounter = new AtomicInteger(0);
    // TSL encryption and need Two Way Authentic
    private boolean enableTLS = false;
    private boolean needTwoWayAuthentic = false;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStorePath;
    private String trustStorePassword;

    public NettyClientFactory() {

    }

    /**
     * initial the network by rpc config object
     *
     * @param conf      the configure information
     * @throws IllegalArgumentException  the exception while configuring object
     */
    public void configure(final RpcConfig conf) throws IllegalArgumentException {
        if (this.shutdown.compareAndSet(true, false)) {
            enableTLS = conf.getBoolean(RpcConstants.TLS_OVER_TCP, false);
            needTwoWayAuthentic = conf.getBoolean(RpcConstants.TLS_TWO_WAY_AUTHENTIC, false);
            if (enableTLS) {
                trustStorePath = conf.getString(RpcConstants.TLS_TRUSTSTORE_PATH);
                trustStorePassword = conf.getString(RpcConstants.TLS_TRUSTSTORE_PASSWORD);
                if (needTwoWayAuthentic) {
                    keyStorePath = conf.getString(RpcConstants.TLS_KEYSTORE_PATH);
                    keyStorePassword = conf.getString(RpcConstants.TLS_KEYSTORE_PASSWORD);
                } else {
                    keyStorePath = null;
                    keyStorePassword = null;
                }
            } else {
                keyStorePath = null;
                keyStorePassword = null;
                trustStorePath = null;
                trustStorePassword = null;
            }
            final int workerCount =
                    conf.getInt(RpcConstants.WORKER_COUNT,
                            RpcConstants.CFG_DEFAULT_CLIENT_WORKER_COUNT);
            String threadName = new StringBuilder(256)
                    .append(conf.getString(RpcConstants.WORKER_THREAD_NAME,
                            RpcConstants.CFG_DEFAULT_WORKER_THREAD_NAME))
                    .append(workerIdCounter.incrementAndGet()).toString();
            eventLoopGroup = EventLoopUtil.newEventLoopGroup(workerCount,
                    conf.getBoolean(RpcConstants.NETTY_TCP_ENABLEBUSYWAIT, false),
                    new DefaultThreadFactory(threadName,
                            Thread.currentThread().isDaemon()));
        }
    }

    @Override
    public Client getClient(NodeAddrInfo addressInfo, RpcConfig conf) throws Exception {
        Client client = clients.get(addressInfo.getHostPortStr());
        // use the cache network client
        if (client != null && client.isReady()) {
            return client;
        }
        synchronized (this) {
            // check client has been build already
            client = clients.get(addressInfo.getHostPortStr());
            if (client != null && client.isReady()) {
                return client;
            }

            // clean and build a new network client
            if (client != null) {
                client = clients.remove(addressInfo.getHostPortStr());
                if (client != null) {
                    client.close();
                }
                client = null;
            }
            int connectTimeout = conf.getInt(RpcConstants.CONNECT_TIMEOUT, 3000);
            try {
                client = createClient(addressInfo, connectTimeout, conf);
                Client existClient =
                        clients.putIfAbsent(addressInfo.getHostPortStr(), client);
                if (existClient != null) {
                    client.close(false);
                    client = existClient;
                }
            } catch (Exception e) {
                if (client != null) {
                    client.close(false);
                }
                throw e;
            } catch (Throwable ee) {
                if (client != null) {
                    client.close(false);
                }
                throw new Exception(ee);
            }
        }
        return client;
    }

    @Override
    public Client removeClient(NodeAddrInfo addressInfo) {
        return clients.remove(addressInfo.getHostPortStr());
    }

    @Override
    public boolean isShutdown() {
        return this.shutdown.get();
    }

    @Override
    public void shutdown() {
        // shutdown and release network resources
        if (this.shutdown.compareAndSet(false, true)) {
            try {
                if (!clients.isEmpty()) {
                    for (String key : clients.keySet()) {
                        if (key != null) {
                            Client client = clients.remove(key);
                            if (client != null) {
                                client.close();
                            }
                        }
                    }
                }
                if (this.eventLoopGroup != null && !eventLoopGroup.isShutdown()) {
                    this.eventLoopGroup.shutdownGracefully();
                }
            } catch (Exception e) {
                logger.error("has exception ", e);
            }
        }
    }

    /**
     * create a netty client
     *
     * @param addressInfo        the remote address information
     * @param connectTimeout     the connection timeout
     * @param conf               the configure information
     * @return the client object
     * @throws Exception         the exception while creating object.
     */
    private Client createClient(final NodeAddrInfo addressInfo,
            int connectTimeout, final RpcConfig conf) throws Exception {
        final NettyClient client =
                new NettyClient(this, connectTimeout);
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(eventLoopGroup);
        clientBootstrap.channel(EventLoopUtil.getClientSocketChannelClass(eventLoopGroup));
        clientBootstrap.option(ChannelOption.TCP_NODELAY, true);
        clientBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        clientBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);

        int nettyWriteHighMark =
                conf.getInt(RpcConstants.NETTY_WRITE_HIGH_MARK, 64 * 1024);
        int nettyWriteLowMark =
                conf.getInt(RpcConstants.NETTY_WRITE_LOW_MARK, 32 * 1024);
        clientBootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(nettyWriteLowMark, nettyWriteHighMark));
        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                if (enableTLS) {
                    try {
                        SSLEngine sslEngine =
                                TSSLEngineUtil.createSSLEngine(keyStorePath, trustStorePath,
                                        keyStorePassword, trustStorePassword, true,
                                        needTwoWayAuthentic);
                        pipeline.addLast("ssl", new SslHandler(sslEngine));
                    } catch (Throwable t) {
                        logger.error(new StringBuilder(256)
                                .append("Create SSLEngine to connection ")
                                .append(addressInfo.getHostPortStr()).append(" failure!").toString(), t);
                        throw new Exception(t);
                    }
                }
                // Encode the data
                pipeline.addLast("protocolEncoder", new NettyProtocolEncoder());
                // Decode the bytes into a Rpc Data Pack
                pipeline.addLast("protocolDecoder", new NettyProtocolDecoder());
                // handle the time out requests
                pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(
                        conf.getLong(RpcConstants.CONNECT_READ_IDLE_DURATION,
                                RpcConstants.CFG_CONNECT_READ_IDLE_TIME),
                        TimeUnit.MILLISECONDS));
                // tube netty client handler
                pipeline.addLast("clientHandler", client.new NettyClientHandler());
            }
        });
        ChannelFuture future =
                clientBootstrap.connect(new InetSocketAddress(addressInfo.getHost(), addressInfo.getPort()));
        future.awaitUninterruptibly(connectTimeout);
        if (!future.isDone()) {
            future.cancel(false);
            throw new LocalConnException(new StringBuilder(256).append("Create connection to ")
                    .append(addressInfo.getHostPortStr()).append(" timeout!").toString());
        }
        if (future.isCancelled()) {
            throw new LocalConnException(new StringBuilder(256).append("Create connection to ")
                    .append(addressInfo.getHostPortStr()).append(" cancelled by user!").toString());
        }
        if (!future.isSuccess()) {
            throw new LocalConnException(new StringBuilder(256).append("Create connection to ")
                    .append(addressInfo.getHostPortStr()).append(" error").toString(),
                    future.cause());
        }
        client.setChannel(future.channel(), addressInfo);
        return client;
    }

}
