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

package org.apache.celeborn.common.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalChannel;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.buffer.NioManagedBuffer;
import org.apache.celeborn.common.network.client.ChunkReceivedCallback;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.network.client.TransportResponseHandler;
import org.apache.celeborn.common.network.protocol.*;
import org.apache.celeborn.common.protocol.TransportModuleConstants;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.common.write.PushRequestInfo;

public class TransportResponseHandlerSuiteJ {
  @Test
  public void handleSuccessfulFetch() throws Exception {
    StreamChunkSlice streamChunkSlice = new StreamChunkSlice(1, 0);

    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.FETCH_MODULE, 8),
            new LocalChannel());
    ChunkReceivedCallback callback = mock(ChunkReceivedCallback.class);
    handler.addFetchRequest(streamChunkSlice, callback);
    assertEquals(1, handler.numOutstandingRequests());

    handler.handle(new ChunkFetchSuccess(streamChunkSlice, new TestManagedBuffer(123)));
    verify(callback, times(1)).onSuccess(eq(0), any());
    assertEquals(0, handler.numOutstandingRequests());
  }

  @Test
  public void handleFailedFetch() throws Exception {
    StreamChunkSlice streamChunkSlice = new StreamChunkSlice(1, 0);
    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.FETCH_MODULE, 8),
            new LocalChannel());
    ChunkReceivedCallback callback = mock(ChunkReceivedCallback.class);
    handler.addFetchRequest(streamChunkSlice, callback);
    assertEquals(1, handler.numOutstandingRequests());

    handler.handle(new ChunkFetchFailure(streamChunkSlice, "some error msg"));
    verify(callback, times(1)).onFailure(eq(0), any());
    assertEquals(0, handler.numOutstandingRequests());
  }

  @Test
  public void clearAllOutstandingRequests() throws Exception {
    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.DATA_MODULE, 8),
            new LocalChannel());
    ChunkReceivedCallback callback = mock(ChunkReceivedCallback.class);
    handler.addFetchRequest(new StreamChunkSlice(1, 0), callback);
    handler.addFetchRequest(new StreamChunkSlice(1, 1), callback);
    handler.addFetchRequest(new StreamChunkSlice(1, 2), callback);
    assertEquals(3, handler.numOutstandingRequests());

    handler.handle(new ChunkFetchSuccess(new StreamChunkSlice(1, 0), new TestManagedBuffer(12)));
    handler.exceptionCaught(new Exception("duh duh duhhhh"));

    // should fail both b2 and b3
    verify(callback, times(1)).onSuccess(eq(0), any());
    verify(callback, times(1)).onFailure(eq(1), any());
    verify(callback, times(1)).onFailure(eq(2), any());
    assertEquals(0, handler.numOutstandingRequests());
  }

  @Test
  public void handleSuccessfulRPC() throws Exception {
    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.RPC_MODULE, 8),
            new LocalChannel());
    RpcResponseCallback callback = mock(RpcResponseCallback.class);
    handler.addRpcRequest(12345, callback);
    assertEquals(1, handler.numOutstandingRequests());

    // This response should be ignored.
    handler.handle(new RpcResponse(54321, new NioManagedBuffer(ByteBuffer.allocate(7))));
    assertEquals(1, handler.numOutstandingRequests());

    ByteBuffer resp = ByteBuffer.allocate(10);
    handler.handle(new RpcResponse(12345, new NioManagedBuffer(resp)));
    verify(callback, times(1)).onSuccess(eq(ByteBuffer.allocate(10)));
    assertEquals(0, handler.numOutstandingRequests());
  }

  @Test
  public void handleFailedRPC() throws Exception {
    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.RPC_MODULE, 8),
            new LocalChannel());
    RpcResponseCallback callback = mock(RpcResponseCallback.class);
    handler.addRpcRequest(12345, callback);
    assertEquals(1, handler.numOutstandingRequests());

    handler.handle(new RpcFailure(54321, "uh-oh!")); // should be ignored
    assertEquals(1, handler.numOutstandingRequests());

    handler.handle(new RpcFailure(12345, "oh no"));
    verify(callback, times(1)).onFailure(any());
    assertEquals(0, handler.numOutstandingRequests());
  }

  @Test
  public void handleSuccessfulPush() throws Exception {
    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.DATA_MODULE, 8),
            new LocalChannel());
    RpcResponseCallback callback = mock(RpcResponseCallback.class);
    PushRequestInfo info = new PushRequestInfo(System.currentTimeMillis() + 30000, callback);
    info.setChannelFuture(mock(ChannelFuture.class));
    handler.addPushRequest(12345, info);
    assertEquals(1, handler.numOutstandingRequests());

    // This response should be ignored.
    handler.handle(new RpcResponse(54321, new NioManagedBuffer(ByteBuffer.allocate(7))));
    assertEquals(1, handler.numOutstandingRequests());

    ByteBuffer resp = ByteBuffer.allocate(10);
    handler.handle(new RpcResponse(12345, new NioManagedBuffer(resp)));
    verify(callback, times(1)).onSuccess(eq(ByteBuffer.allocate(10)));
    assertEquals(0, handler.numOutstandingRequests());
  }

  @Test
  public void handleFailedPush() throws Exception {
    TransportResponseHandler handler =
        new TransportResponseHandler(
            Utils.fromCelebornConf(new CelebornConf(), TransportModuleConstants.DATA_MODULE, 8),
            new LocalChannel());
    RpcResponseCallback callback = mock(RpcResponseCallback.class);
    PushRequestInfo info = new PushRequestInfo(System.currentTimeMillis() + 30000L, callback);
    info.setChannelFuture(mock(ChannelFuture.class));
    handler.addPushRequest(12345, info);
    assertEquals(1, handler.numOutstandingRequests());

    handler.handle(new RpcFailure(54321, "uh-oh!")); // should be ignored
    assertEquals(1, handler.numOutstandingRequests());

    handler.handle(new RpcFailure(12345, "oh no"));
    verify(callback, times(1)).onFailure(any());
    assertEquals(0, handler.numOutstandingRequests());
  }
}
