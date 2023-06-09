/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.server.rest;

import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.rpc.TransportCheck;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Validates {@link WebSessionResources} close works as expected w.r.t {@link io.netty.channel.AbstractChannel.CloseFuture}
 * associated with it.
 */
public class WebSessionResourcesTest extends BaseTest {

  private WebSessionResources webSessionResources;
  private boolean listenerComplete;
  private CountDownLatch latch;
  private EventExecutor executor;

  // A close listener added in close future in one of the test to see if it's invoked correctly.
  private class TestClosedListener implements GenericFutureListener<Future<Void>> {
    @Override
    public void operationComplete(Future<Void> future) throws Exception {
      listenerComplete = true;
      latch.countDown();
    }
  }

  /**
   * Validates {@link WebSessionResources#close()} throws NPE when closefuture passed to WebSessionResources doesn't
   * have a valid channel and EventExecutor associated with it.
   * @throws Exception
   */
  @Test
  public void testChannelPromiseWithNullExecutor() throws Exception {
    try {
      Promise<Void> closeFuture = new DefaultPromise(null);
      webSessionResources = new WebSessionResources(mock(BufferAllocator.class), mock(SocketAddress.class), mock
          (UserSession.class), closeFuture);
      webSessionResources.close();
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

  /**
   * Validates successful {@link WebSessionResources#close()} with valid CloseFuture and other parameters.
   * @throws Exception
   */
  @Test
  public void testChannelPromiseWithValidExecutor() throws Exception {
    try {
      EventExecutor mockExecutor = mock(EventExecutor.class);
      Promise<Void> closeFuture = new DefaultPromise(mockExecutor);
      webSessionResources = new WebSessionResources(mock(BufferAllocator.class), mock(SocketAddress.class), mock
          (UserSession.class), closeFuture);
      webSessionResources.close();
      verify(webSessionResources.getAllocator()).close();
      verify(webSessionResources.getSession()).close();
      assertTrue(webSessionResources.getCloseFuture() == null);
      assertTrue(!listenerComplete);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Validates double call to {@link WebSessionResources#close()} doesn't throw any exception.
   * @throws Exception
   */
  @Test
  public void testDoubleClose() throws Exception {
    try {
      Promise<Void> closeFuture = new DefaultPromise(mock(EventExecutor.class));
      webSessionResources = new WebSessionResources(mock(BufferAllocator.class), mock(SocketAddress.class), mock
          (UserSession.class), closeFuture);
      webSessionResources.close();

      verify(webSessionResources.getAllocator()).close();
      verify(webSessionResources.getSession()).close();
      assertTrue(webSessionResources.getCloseFuture() == null);

      webSessionResources.close();
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Validates successful {@link WebSessionResources#close()} with valid CloseFuture and {@link TestClosedListener}
   * getting invoked which is added to the close future.
   * @throws Exception
   */
  @Test
  public void testCloseWithListener() throws Exception {
    try {
      // Assign latch, executor and closeListener for this test case
      GenericFutureListener<Future<Void>> closeListener = new TestClosedListener();
      latch = new CountDownLatch(1);
      executor = TransportCheck.createEventLoopGroup(1, "Test-Thread").next();
      Promise<Void> closeFuture = new DefaultPromise(executor);

      // create WebSessionResources with above ChannelPromise to notify listener
      webSessionResources = new WebSessionResources(mock(BufferAllocator.class), mock(SocketAddress.class),
          mock(UserSession.class), closeFuture);

      // Add the Test Listener to close future
      assertTrue(!listenerComplete);
      closeFuture.addListener(closeListener);

      // Close the WebSessionResources
      webSessionResources.close();

      // Verify the states
      verify(webSessionResources.getAllocator()).close();
      verify(webSessionResources.getSession()).close();
      assertTrue(webSessionResources.getCloseFuture() == null);

      // Since listener will be invoked so test should not wait forever
      latch.await();
      assertTrue(listenerComplete);
    } catch (Exception e) {
      fail();
    } finally {
      listenerComplete = false;
      executor.shutdownGracefully();
    }
  }
}