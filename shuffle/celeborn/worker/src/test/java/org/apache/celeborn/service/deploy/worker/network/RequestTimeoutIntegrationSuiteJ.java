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

package org.apache.celeborn.service.deploy.worker.network;

import static org.apache.celeborn.common.util.JavaUtils.getLocalHost;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.TransportContext;
import org.apache.celeborn.common.network.buffer.ManagedBuffer;
import org.apache.celeborn.common.network.buffer.NioManagedBuffer;
import org.apache.celeborn.common.network.client.ChunkReceivedCallback;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.network.protocol.*;
import org.apache.celeborn.common.network.server.BaseMessageHandler;
import org.apache.celeborn.common.network.server.TransportServer;
import org.apache.celeborn.common.network.util.TransportConf;
import org.apache.celeborn.service.deploy.worker.storage.ChunkStreamManager;

/**
 * Suite which ensures that requests that go without a response for the network timeout period are
 * failed, and the connection closed.
 *
 * <p>In this suite, we use 10 seconds as the connection timeout, with some slack given in the
 * tests, to ensure stability in different test environments.
 */
public class RequestTimeoutIntegrationSuiteJ {

  private TransportServer server;
  private TransportClientFactory clientFactory;

  private TransportConf conf;

  // A large timeout that "shouldn't happen", for the sake of faulty tests not hanging forever.
  private static final int FOREVER = 60 * 1000;

  @Before
  public void setUp() throws Exception {
    CelebornConf _conf = new CelebornConf();
    _conf.set("celeborn.shuffle.io.connectionTimeout", "2s");
    conf = new TransportConf("shuffle", _conf);
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.close();
    }
    if (clientFactory != null) {
      clientFactory.close();
    }
  }

  // Basic suite: First request completes quickly, and second waits for longer than network timeout.
  @Test
  public void timeoutInactiveRequests() throws Exception {
    final Semaphore semaphore = new Semaphore(1);
    final int responseSize = 16;
    BaseMessageHandler handler =
        new BaseMessageHandler() {
          @Override
          public void receive(TransportClient client, RequestMessage message) {
            try {
              semaphore.acquire();
              client
                  .getChannel()
                  .writeAndFlush(
                      new RpcResponse(
                          ((RpcRequest) message).requestId,
                          new NioManagedBuffer(ByteBuffer.allocate(responseSize))));
            } catch (InterruptedException e) {
              // do nothing
            }
          }

          @Override
          public boolean checkRegistered() {
            return true;
          }
        };

    TransportContext context = new TransportContext(conf, handler, true);
    server = context.createServer();
    clientFactory = context.createClientFactory();
    TransportClient client = clientFactory.createClient(getLocalHost(), server.getPort());

    // First completes quickly (semaphore starts at 1).
    TestCallback callback0 = new TestCallback();
    client.sendRpc(ByteBuffer.allocate(0), callback0);
    callback0.latch.await();
    assertEquals(responseSize, callback0.successLength);

    // Second times out after 10 seconds, with slack. Must be IOException.
    TestCallback callback1 = new TestCallback();
    client.sendRpc(ByteBuffer.allocate(0), callback1);
    callback1.latch.await(60, TimeUnit.SECONDS);
    assertNotNull(callback1.failure);
    assertTrue(callback1.failure instanceof IOException);

    semaphore.release();
  }

  // A timeout will cause the connection to be closed, invalidating the current TransportClient.
  // It should be the case that requesting a client from the factory produces a new, valid one.
  @Test
  public void timeoutCleanlyClosesClient() throws Exception {
    final Semaphore semaphore = new Semaphore(0);
    final int responseSize = 16;
    BaseMessageHandler handler =
        new BaseMessageHandler() {
          @Override
          public void receive(TransportClient client, RequestMessage message) {
            try {
              semaphore.acquire();
              client
                  .getChannel()
                  .writeAndFlush(
                      new RpcResponse(
                          ((RpcRequest) message).requestId,
                          new NioManagedBuffer(ByteBuffer.allocate(responseSize))));
            } catch (InterruptedException e) {
              // do nothing
            }
          }

          @Override
          public boolean checkRegistered() {
            return true;
          }
        };

    TransportContext context = new TransportContext(conf, handler, true);
    server = context.createServer();
    clientFactory = context.createClientFactory();

    // First request should eventually fail.
    TransportClient client0 = clientFactory.createClient(getLocalHost(), server.getPort());
    TestCallback callback0 = new TestCallback();
    client0.sendRpc(ByteBuffer.allocate(0), callback0);
    callback0.latch.await();
    assertTrue(callback0.failure instanceof IOException);
    assertFalse(client0.isActive());

    // Increment the semaphore and the second request should succeed quickly.
    semaphore.release(2);
    TransportClient client1 = clientFactory.createClient(getLocalHost(), server.getPort());
    TestCallback callback1 = new TestCallback();
    client1.sendRpc(ByteBuffer.allocate(0), callback1);
    callback1.latch.await();
    assertEquals(responseSize, callback1.successLength);
    assertNull(callback1.failure);
  }

  // The timeout is relative to the LAST request sent, which is kinda weird, but still.
  // This test also makes sure the timeout works for Fetch requests as well as RPCs.
  @Test
  public void furtherRequestsDelay() throws Exception {
    final byte[] response = new byte[16];
    final ChunkStreamManager manager =
        new ChunkStreamManager() {
          @Override
          public ManagedBuffer getChunk(long streamId, int chunkIndex, int offset, int len) {
            Uninterruptibles.sleepUninterruptibly(FOREVER, TimeUnit.MILLISECONDS);
            return new NioManagedBuffer(ByteBuffer.wrap(response));
          }
        };
    BaseMessageHandler handler =
        new BaseMessageHandler() {
          @Override
          public void receive(TransportClient client, RequestMessage msg) {
            StreamChunkSlice slice = ((ChunkFetchRequest) msg).streamChunkSlice;
            ManagedBuffer buf =
                manager.getChunk(slice.streamId, slice.chunkIndex, slice.offset, slice.len);
            client.getChannel().writeAndFlush(new ChunkFetchSuccess(slice, buf));
          }

          @Override
          public boolean checkRegistered() {
            return true;
          }
        };

    TransportContext context = new TransportContext(conf, handler, true);
    server = context.createServer();
    clientFactory = context.createClientFactory();
    TransportClient client = clientFactory.createClient(getLocalHost(), server.getPort());

    // Send one request, which will eventually fail.
    TestCallback callback0 = new TestCallback();
    client.fetchChunk(0, 0, 10000, callback0);
    Uninterruptibles.sleepUninterruptibly(1200, TimeUnit.MILLISECONDS);

    // Send a second request before the first has failed.
    TestCallback callback1 = new TestCallback();
    client.fetchChunk(0, 1, 10000, callback1);
    Uninterruptibles.sleepUninterruptibly(1200, TimeUnit.MILLISECONDS);

    // not complete yet, but should complete soon
    assertEquals(-1, callback0.successLength);
    assertNull(callback0.failure);
    callback0.latch.await(10, TimeUnit.SECONDS);
    assertTrue(callback0.failure instanceof IOException);

    // make sure callback1 is called.
    callback1.latch.await(60, TimeUnit.SECONDS);
    // failed at same time as previous
    assertTrue(callback1.failure instanceof IOException);
  }

  /**
   * Callback which sets 'success' or 'failure' on completion. Additionally notifies all waiters on
   * this callback when invoked.
   */
  static class TestCallback implements RpcResponseCallback, ChunkReceivedCallback {

    int successLength = -1;
    Throwable failure;
    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onSuccess(ByteBuffer response) {
      successLength = response.remaining();
      latch.countDown();
    }

    @Override
    public void onFailure(Throwable e) {
      failure = e;
      latch.countDown();
    }

    @Override
    public void onSuccess(int chunkIndex, ManagedBuffer buffer) {
      try {
        successLength = buffer.nioByteBuffer().remaining();
      } catch (IOException e) {
        // weird
      } finally {
        latch.countDown();
      }
    }

    @Override
    public void onFailure(int chunkIndex, Throwable e) {
      failure = e;
      latch.countDown();
    }
  }
}
