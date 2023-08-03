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

package org.apache.celeborn.common.network.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.metrics.source.AbstractSource;
import org.apache.celeborn.common.util.JavaUtils;

/** Utilities for creating various Netty constructs based on whether we're using EPOLL or NIO. */
public class NettyUtils {
  private static volatile PooledByteBufAllocator _allocator;
  private static ConcurrentHashMap<String, Integer> allocatorsIndex =
      JavaUtils.newConcurrentHashMap();
  /** Creates a new ThreadFactory which prefixes each thread with the given name. */
  public static ThreadFactory createThreadFactory(String threadPoolPrefix) {
    return new DefaultThreadFactory(threadPoolPrefix, true);
  }

  /** Creates a Netty EventLoopGroup based on the IOMode. */
  public static EventLoopGroup createEventLoop(IOMode mode, int numThreads, String threadPrefix) {
    ThreadFactory threadFactory = createThreadFactory(threadPrefix);

    switch (mode) {
      case NIO:
        return new NioEventLoopGroup(numThreads, threadFactory);
      case EPOLL:
        return new EpollEventLoopGroup(numThreads, threadFactory);
      default:
        throw new IllegalArgumentException("Unknown io mode: " + mode);
    }
  }

  /** Returns the correct (client) SocketChannel class based on IOMode. */
  public static Class<? extends Channel> getClientChannelClass(IOMode mode) {
    switch (mode) {
      case NIO:
        return NioSocketChannel.class;
      case EPOLL:
        return EpollSocketChannel.class;
      default:
        throw new IllegalArgumentException("Unknown io mode: " + mode);
    }
  }

  /** Returns the correct ServerSocketChannel class based on IOMode. */
  public static Class<? extends ServerChannel> getServerChannelClass(IOMode mode) {
    switch (mode) {
      case NIO:
        return NioServerSocketChannel.class;
      case EPOLL:
        return EpollServerSocketChannel.class;
      default:
        throw new IllegalArgumentException("Unknown io mode: " + mode);
    }
  }

  /** Returns the remote address on the channel or "&lt;unknown remote&gt;" if none exists. */
  public static String getRemoteAddress(Channel channel) {
    if (channel != null && channel.remoteAddress() != null) {
      return channel.remoteAddress().toString();
    }
    return "<unknown remote>";
  }

  /**
   * Create a pooled ByteBuf allocator but disables the thread-local cache. Thread-local caches are
   * disabled for TransportClients because the ByteBufs are allocated by the event loop thread, but
   * released by the executor thread rather than the event loop thread. Those thread-local caches
   * actually delay the recycling of buffers, leading to larger memory usage.
   */
  private static PooledByteBufAllocator createPooledByteBufAllocator(
      boolean allowDirectBufs, boolean allowCache, int numCores) {
    if (numCores == 0) {
      numCores = Runtime.getRuntime().availableProcessors();
    }
    return new PooledByteBufAllocator(
        allowDirectBufs && PlatformDependent.directBufferPreferred(),
        Math.min(PooledByteBufAllocator.defaultNumHeapArena(), numCores),
        Math.min(PooledByteBufAllocator.defaultNumDirectArena(), allowDirectBufs ? numCores : 0),
        PooledByteBufAllocator.defaultPageSize(),
        PooledByteBufAllocator.defaultMaxOrder(),
        allowCache ? PooledByteBufAllocator.defaultSmallCacheSize() : 0,
        allowCache ? PooledByteBufAllocator.defaultNormalCacheSize() : 0,
        allowCache && PooledByteBufAllocator.defaultUseCacheForAllThreads());
  }

  private static PooledByteBufAllocator getSharedPooledByteBufAllocator(
      CelebornConf conf, AbstractSource source) {
    synchronized (PooledByteBufAllocator.class) {
      if (_allocator == null) {
        // each core should have one arena to allocate memory
        _allocator = createPooledByteBufAllocator(true, true, conf.networkAllocatorArenas());
        if (source != null) {
          new NettyMemoryMetrics(
              _allocator,
              "shared-pool",
              conf.networkAllocatorVerboseMetric(),
              source,
              Collections.emptyMap());
        }
      }
      return _allocator;
    }
  }

  public static PooledByteBufAllocator getPooledByteBufAllocator(
      TransportConf conf, AbstractSource source, boolean allowCache) {
    if (conf.getCelebornConf().networkShareMemoryAllocator()) {
      return getSharedPooledByteBufAllocator(conf.getCelebornConf(), source);
    }
    PooledByteBufAllocator allocator =
        createPooledByteBufAllocator(conf.preferDirectBufs(), allowCache, conf.clientThreads());
    if (source != null) {
      String poolName = "default-netty-pool";
      Map<String, String> labels = new HashMap<>();
      String moduleName = conf.getModuleName();
      if (!moduleName.isEmpty()) {
        poolName = moduleName;
        int index = allocatorsIndex.compute(moduleName, (k, v) -> v == null ? 0 : v + 1);
        labels.put("allocatorIndex", String.valueOf(index));
      }
      new NettyMemoryMetrics(
          allocator,
          poolName,
          conf.getCelebornConf().networkAllocatorVerboseMetric(),
          source,
          labels);
    }
    return allocator;
  }
}
