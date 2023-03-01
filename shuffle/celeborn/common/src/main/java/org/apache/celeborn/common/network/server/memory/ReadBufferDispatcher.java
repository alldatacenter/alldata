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

package org.apache.celeborn.common.network.server.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.util.NettyUtils;

public class ReadBufferDispatcher extends Thread {
  private final Logger logger = LoggerFactory.getLogger(ReadBufferDispatcher.class);
  private final LinkedBlockingQueue<ReadBufferRequest> requests = new LinkedBlockingQueue<>();
  private final MemoryManager memoryManager;
  private final PooledByteBufAllocator readBufferAllocator;

  public ReadBufferDispatcher(MemoryManager memoryManager) {
    readBufferAllocator = NettyUtils.createPooledByteBufAllocator(true, true, 1);
    this.memoryManager = memoryManager;
    this.setName("Read-Buffer-Dispatcher");
    this.start();
  }

  public void addBufferRequest(ReadBufferRequest request) {
    requests.add(request);
  }

  public void recycle(ByteBuf buf) {
    int bufferSize = buf.capacity();
    if (buf.refCnt() == 0) {
      logger.warn("recycle encounter: {}", buf.refCnt());
    } else {
      buf.release(buf.refCnt());
    }
    memoryManager.changeReadBufferCounter(-1 * bufferSize);
  }

  @Override
  public void run() {
    while (true) {
      ReadBufferRequest request = null;
      try {
        request = requests.poll(500, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        logger.info("Buffer dispatcher is closing");
      }

      if (request != null) {
        List<ByteBuf> buffers = new ArrayList<>();
        int bufferSize = request.getBufferSize();
        while (buffers.size() < request.getMin()) {
          if (memoryManager.readBufferAvailable(bufferSize)) {
            memoryManager.changeReadBufferCounter(bufferSize);
            ByteBuf buf = readBufferAllocator.buffer(bufferSize, bufferSize);
            buffers.add(buf);
          } else {
            try {
              // If dispatcher can not allocate minimum buffers, it will wait here until necessary
              // buffers are get.
              Thread.sleep(3);
            } catch (InterruptedException e) {
              logger.info("Buffer dispatcher is closing");
              request.getBufferListener().notifyBuffers(null, e);
              return;
            }
          }
        }
        while (memoryManager.readBufferAvailable(request.getBufferSize())
            && buffers.size() < request.getMax()) {
          memoryManager.changeReadBufferCounter(bufferSize);
          ByteBuf buf = readBufferAllocator.buffer(bufferSize, bufferSize);
          buffers.add(buf);
        }
        request.getBufferListener().notifyBuffers(buffers, null);
      } else {
        // Free buffer pool memory to main direct memory when dispatcher is idle.
        readBufferAllocator.trimCurrentThreadCache();
      }
    }
  }
}
