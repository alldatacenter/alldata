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

import org.apache.celeborn.common.CelebornConf;

/** A central location that tracks all the settings we expose to users. */
public class TransportConf {

  private final CelebornConf celebornConf;

  private final String module;

  public TransportConf(String module, CelebornConf celebornConf) {
    this.module = module;
    this.celebornConf = celebornConf;
  }

  public String getModuleName() {
    return module;
  }

  /** IO mode: nio or epoll */
  public String ioMode() {
    return celebornConf.networkIoMode(module);
  }

  /** If true, we will prefer allocating off-heap byte buffers within Netty. */
  public boolean preferDirectBufs() {
    return celebornConf.networkIoPreferDirectBufs(module);
  }

  /** Connect timeout in milliseconds. Default 10 secs. */
  public int connectTimeoutMs() {
    return celebornConf.networkIoConnectTimeoutMs(module);
  }

  /** Connection active timeout in milliseconds. Default 240 secs. */
  public int connectionTimeoutMs() {
    return celebornConf.networkIoConnectionTimeoutMs(module);
  }

  /** Number of concurrent connections between two nodes for fetching data. */
  public int numConnectionsPerPeer() {
    return celebornConf.networkIoNumConnectionsPerPeer(module);
  }

  /** Requested maximum length of the queue of incoming connections. Default 0 for no backlog. */
  public int backLog() {
    return celebornConf.networkIoBacklog(module);
  }

  /** Number of threads used in the server thread pool. Default to 0, which is 2x#cores. */
  public int serverThreads() {
    return celebornConf.networkIoServerThreads(module);
  }

  /** Number of threads used in the client thread pool. Default to 0, which is 2x#cores. */
  public int clientThreads() {
    return celebornConf.networkIoClientThreads(module);
  }

  /**
   * Receive buffer size (SO_RCVBUF). Note: the optimal size for receive buffer and send buffer
   * should be latency * network_bandwidth. Assuming latency = 1ms, network_bandwidth = 10Gbps
   * buffer size should be ~ 1.25MB
   */
  public int receiveBuf() {
    return celebornConf.networkIoReceiveBuf(module);
  }

  /** Send buffer size (SO_SNDBUF). */
  public int sendBuf() {
    return celebornConf.networkIoSendBuf(module);
  }

  /**
   * Max number of times we will try IO exceptions (such as connection timeouts) per request. If set
   * to 0, we will not do any retries.
   */
  public int maxIORetries() {
    return celebornConf.networkIoMaxRetries(module);
  }

  /**
   * Time (in milliseconds) that we will wait in order to perform a retry after an IOException. Only
   * relevant if maxIORetries &gt; 0.
   */
  public int ioRetryWaitTimeMs() {
    return celebornConf.networkIoRetryWaitMs(module);
  }

  /**
   * Minimum size of a block that we should start using memory map rather than reading in through
   * normal IO operations. This prevents Celeborn from memory mapping very small blocks. In general,
   * memory mapping has high overhead for blocks close to or below the page size of the OS.
   */
  public int memoryMapBytes() {
    return celebornConf.networkIoMemoryMapBytes(module);
  }

  /**
   * Whether to initialize FileDescriptor lazily or not. If true, file descriptors are created only
   * when data is going to be transferred. This can reduce the number of open files.
   */
  public boolean lazyFileDescriptor() {
    return celebornConf.networkIoLazyFileDescriptor(module);
  }

  /**
   * Whether to track Netty memory detailed metrics. If true, the detailed metrics of Netty
   * PoolByteBufAllocator will be gotten, otherwise only general memory usage will be tracked.
   */
  public boolean verboseMetrics() {
    return celebornConf.networkIoVerboseMetrics(module);
  }

  /**
   * The max number of chunks allowed to be transferred at the same time on shuffle service. Note
   * that new incoming connections will be closed when the max number is hit. The client will retry
   * according to the shuffle retry configs (see `celeborn.shuffle.io.maxRetries` and
   * `celeborn.shuffle.io.retryWait`), if those limits are reached the task will fail with fetch
   * failure.
   */
  public long maxChunksBeingTransferred() {
    return celebornConf.networkIoMaxChunksBeingTransferred(module);
  }

  public CelebornConf getCelebornConf() {
    return celebornConf;
  }

  public long pushDataTimeoutCheckIntervalMs() {
    return celebornConf.pushTimeoutCheckInterval();
  }
}
