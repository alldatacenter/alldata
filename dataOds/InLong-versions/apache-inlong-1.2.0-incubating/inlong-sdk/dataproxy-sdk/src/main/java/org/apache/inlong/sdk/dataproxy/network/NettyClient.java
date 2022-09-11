/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.codec.EncodeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private Channel channel = null;
    private final ReentrantLock stateLock = new ReentrantLock();

    private ConnState connState;
    private ProxyClientConfig configure;
    private Bootstrap bootstrap;
    private String serverIP;
    private int serverPort;

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public NettyClient(Bootstrap bootstrap, String serverIP,
                       int serverPort, ProxyClientConfig configure) {
        this.bootstrap = bootstrap;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.configure = configure;
        setState(ConnState.INIT);
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean connect() {
        // Connect to server.

        setState(ConnState.INIT);
        final CountDownLatch awaitLatch = new CountDownLatch(1);
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(
                serverIP, serverPort));
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture arg0) throws Exception {
                logger.info("connect ack! {}", serverIP);
                awaitLatch.countDown();
            }
        });

        try {
            // Wait until the connection is built.
            awaitLatch.await(configure.getConnectTimeoutMillis(),
                    TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("create connect exception! {}", e.getMessage());
            e.printStackTrace();
            return false;
        }

        // Return if no connection is built.
        if (!future.isSuccess()) {
            return false;
        }
        channel = future.channel();
        setState(ConnState.READY);
        logger.info("ip {} stat {}", serverIP, connState);
        return true;
    }

    public boolean close() {
        logger.debug("begin to close this channel{}", channel);
        final CountDownLatch awaitLatch = new CountDownLatch(1);
        boolean ret = true;
        try {
            if (channel != null) {
                ChannelFuture future = channel.close();
                future.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture arg0)
                            throws Exception {
                        logger.info("close client ack {}", serverIP);
                        awaitLatch.countDown();
                    }
                });
                // Wait until the connection is close.
                awaitLatch.await(configure.getRequestTimeoutMillis(),
                        TimeUnit.MILLISECONDS);
                // Return if close this connection fail.
                if (!future.isSuccess()) {
                    ret = false;
                }
            }
        } catch (Exception e) {
            logger.error("close connect {" + serverIP + ":" + serverPort + "} exception! {}", e.getMessage());
            e.printStackTrace();
            ret = false;
        } finally {
            setState(ConnState.DEAD);
        }
        logger.info("end to close {" + serverIP + ":" + serverPort + "} 's channel, bSuccess = " + ret);
        return ret;
    }

    public void reconnect() {
        this.close();
        this.connect();
    }

    public boolean isActive() {
        stateLock.lock();
        try {
            return (connState == ConnState.READY && channel != null && channel.isOpen() && channel.isActive());
        } catch (Exception e) {
            logger.error("channel maybe null!{}", e.getMessage());
            return false;
        } finally {
            stateLock.unlock();
        }
        // channel.isOpen();
    }

    private void setState(ConnState newState) {
        stateLock.lock();
        try {
            connState = newState;
        } catch (Exception e) {
            logger.error("setState maybe error!{}", e.getMessage());
        } finally {
            stateLock.unlock();
        }
    }

    private enum ConnState {
        INIT, READY, FROZEN, DEAD, BUSY
    }

    public ChannelFuture write(EncodeObject encodeObject) {
        // TODO Auto-generated method stub
        ChannelFuture future = null;
        try {
            future = channel.writeAndFlush(encodeObject);
        } catch (Exception e) {
            logger.error("channel write error {}", e.getMessage());
            e.printStackTrace();
        }
        return future;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NettyClient other = (NettyClient) obj;
        if (channel == null) {
            if (other.channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }
        return true;
    }

    public void setFrozen() {
        // TODO Auto-generated method stub
        setState(ConnState.FROZEN);

    }

    public void setBusy() {
        setState(ConnState.BUSY);
    }

}
