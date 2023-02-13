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

package org.apache.inlong.sdk.dataproxy.utils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

public class EventLoopUtil {

    public EventLoopUtil() {
    }

    /**
     * Create loop of event about group.
     */
    public static EventLoopGroup newEventLoopGroup(int nThreads, boolean enableBusyWait, ThreadFactory threadFactory) {
        if (!Epoll.isAvailable()) {
            return new NioEventLoopGroup(nThreads, threadFactory);
        } else if (!enableBusyWait) {
            return new EpollEventLoopGroup(nThreads, threadFactory);
        } else {
            EpollEventLoopGroup eventLoopGroup = new EpollEventLoopGroup(nThreads, threadFactory, () -> {
                return (selectSupplier, hasTasks) -> {
                    return -3;
                };
            });
            return eventLoopGroup;
        }
    }

    /**
     * Get class of socket's channel about client.
     */
    public static Class<? extends SocketChannel> getClientSocketChannelClass(EventLoopGroup eventLoopGroup) {
        return eventLoopGroup instanceof EpollEventLoopGroup
                ? EpollSocketChannel.class
                : NioSocketChannel.class;
    }

    /**
     * Get class of socket's channel about server.
     */
    public static Class<? extends ServerSocketChannel> getServerSocketChannelClass(EventLoopGroup eventLoopGroup) {
        return eventLoopGroup instanceof EpollEventLoopGroup
                ? EpollServerSocketChannel.class
                : NioServerSocketChannel.class;
    }

    /**
     * Get class of datagram's channel.
     */
    public static Class<? extends DatagramChannel> getDatagramChannelClass(EventLoopGroup eventLoopGroup) {
        return eventLoopGroup instanceof EpollEventLoopGroup
                ? EpollDatagramChannel.class
                : NioDatagramChannel.class;
    }

    /**
     * Epoll mode.
     */
    public static void enableTriggeredMode(ServerBootstrap bootstrap) {
        if (Epoll.isAvailable()) {
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }

    }

    /**
     * Gracefully close the thread pool.
     */
    public static CompletableFuture<Void> shutdownGracefully(EventLoopGroup eventLoopGroup) {
        return toCompletableFutureVoid(eventLoopGroup.shutdownGracefully());
    }

    /**
     * Convert to CompletableFuture.
     */
    public static CompletableFuture<Void> toCompletableFutureVoid(Future<?> future) {
        Objects.requireNonNull(future, "future cannot be null");

        CompletableFuture<Void> adapter = new CompletableFuture<>();
        if (future.isDone()) {
            if (future.isSuccess()) {
                adapter.complete(null);
            } else {
                adapter.completeExceptionally(future.cause());
            }
        } else {
            future.addListener(f -> {
                if (f.isSuccess()) {
                    adapter.complete(null);
                } else {
                    adapter.completeExceptionally(f.cause());
                }
            });
        }
        return adapter;
    }
}
