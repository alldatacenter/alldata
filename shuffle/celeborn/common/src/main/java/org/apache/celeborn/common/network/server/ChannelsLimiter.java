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

package org.apache.celeborn.common.network.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.server.memory.MemoryManager;

@ChannelHandler.Sharable
public class ChannelsLimiter extends ChannelDuplexHandler
    implements MemoryManager.MemoryPressureListener {

  private static final Logger logger = LoggerFactory.getLogger(ChannelsLimiter.class);
  private final Set<Channel> channels = ConcurrentHashMap.newKeySet();
  private final String moduleName;
  private final AtomicBoolean isPaused = new AtomicBoolean(false);

  public ChannelsLimiter(String moduleName) {
    this.moduleName = moduleName;
    MemoryManager memoryManager = MemoryManager.instance();
    memoryManager.registerMemoryListener(this);
  }

  private void pauseAllChannels() {
    isPaused.set(true);
    channels.forEach(
        c -> {
          if (c.config().isAutoRead()) {
            c.config().setAutoRead(false);
          }
        });
  }

  private void trimCache() {
    channels.forEach(c -> c.pipeline().fireUserEventTriggered(new TrimCache()));
  }

  private void resumeAllChannels() {
    synchronized (isPaused) {
      isPaused.set(false);
      channels.forEach(
          c -> {
            if (!c.config().isAutoRead()) {
              c.config().setAutoRead(true);
            }
          });
    }
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    channels.add(ctx.channel());
    synchronized (isPaused) {
      if (isPaused.get()) {
        // If thread A runs here,and its time slice is run out while
        // another thread B running "resumeAllChannels" method.
        // It is possible that the channel connected with ctx will
        // pause auto read because of concurrent modification.
        // So we need to make sure checking the paused flag and
        // changing its status will be an atomic operation.
        ctx.channel().config().setAutoRead(false);
      }
    }
    super.handlerAdded(ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().config().isAutoRead()) {
      ctx.channel().config().setAutoRead(true);
    }
    channels.remove(ctx.channel());
    super.handlerRemoved(ctx);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof TrimCache) {
      ((PooledByteBufAllocator) ctx.alloc()).trimCurrentThreadCache();
    }
  }

  @Override
  public void onPause(String moduleName) {
    if (this.moduleName.equals(moduleName)) {
      logger.info(this.moduleName + " channels pause read.");
      pauseAllChannels();
    }
  }

  @Override
  public void onResume(String moduleName) {
    if (moduleName.equalsIgnoreCase("all")) {
      logger.info(this.moduleName + " channels resume read.");
      resumeAllChannels();
    }
    if (this.moduleName.equals(moduleName)) {
      logger.info(this.moduleName + " channels resume read.");
      resumeAllChannels();
    }
  }

  @Override
  public void onTrim() {
    trimCache();
  }

  class TrimCache {}
}
