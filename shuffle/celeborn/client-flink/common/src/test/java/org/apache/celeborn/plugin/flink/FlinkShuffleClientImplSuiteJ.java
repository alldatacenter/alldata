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

package org.apache.celeborn.plugin.flink;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.message.StatusCode;
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl;

public class FlinkShuffleClientImplSuiteJ {
  static int BufferSize = 64;
  static byte[] TEST_BUF1 = new byte[BufferSize];
  protected ChannelFuture mockedFuture = mock(ChannelFuture.class);
  static CelebornConf conf;
  static FlinkShuffleClientImpl shuffleClient;
  protected static final TransportClientFactory clientFactory = mock(TransportClientFactory.class);
  protected final TransportClient client = mock(TransportClient.class);
  protected static final PartitionLocation primaryLocation =
      new PartitionLocation(0, 1, "localhost", 1, 1, 1, 1, PartitionLocation.Mode.PRIMARY);

  @Before
  public void setup() throws IOException, InterruptedException {
    conf = new CelebornConf();
    shuffleClient =
        new FlinkShuffleClientImpl(
            "APP", "localhost", 1232, System.currentTimeMillis(), conf, null) {
          @Override
          public void setupLifecycleManagerRef(String host, int port) {}
        };
    when(clientFactory.createClient(primaryLocation.getHost(), primaryLocation.getPushPort(), 1))
        .thenAnswer(t -> client);

    shuffleClient.setDataClientFactory(clientFactory);
  }

  public ByteBuf createByteBuf() {
    for (int i = 16; i < BufferSize; i++) {
      TEST_BUF1[i] = 1;
    }
    ByteBuf byteBuf = Unpooled.wrappedBuffer(TEST_BUF1);
    byteBuf.writerIndex(BufferSize);
    return byteBuf;
  }

  @Test
  public void testPushDataByteBufSuccess() throws IOException {
    ByteBuf byteBuf = createByteBuf();
    when(client.pushData(any(), anyLong(), any()))
        .thenAnswer(
            t -> {
              RpcResponseCallback rpcResponseCallback = t.getArgument(1, RpcResponseCallback.class);
              ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[0]);
              rpcResponseCallback.onSuccess(byteBuffer);
              return mockedFuture;
            });

    int pushDataLen =
        shuffleClient.pushDataToLocation(2, 3, 4, 5, byteBuf, primaryLocation, () -> {});
    Assert.assertEquals(BufferSize, pushDataLen);
  }

  @Test
  public void testPushDataByteBufHardSplit() throws IOException {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(TEST_BUF1);
    when(client.pushData(any(), anyLong(), any()))
        .thenAnswer(
            t -> {
              RpcResponseCallback rpcResponseCallback = t.getArgument(1, RpcResponseCallback.class);
              ByteBuffer byteBuffer =
                  ByteBuffer.wrap(new byte[] {StatusCode.HARD_SPLIT.getValue()});
              rpcResponseCallback.onSuccess(byteBuffer);
              return mockedFuture;
            });
    int pushDataLen =
        shuffleClient.pushDataToLocation(2, 3, 4, 5, byteBuf, primaryLocation, () -> {});
  }

  @Test
  public void testPushDataByteBufFail() throws IOException {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(TEST_BUF1);
    when(client.pushData(any(), anyLong(), any(), any()))
        .thenAnswer(
            t -> {
              RpcResponseCallback rpcResponseCallback = t.getArgument(1, RpcResponseCallback.class);
              rpcResponseCallback.onFailure(new Exception("pushDataFailed"));
              return mockedFuture;
            });
    // first push just  set pushdata.exception
    shuffleClient.pushDataToLocation(2, 3, 4, 5, byteBuf, primaryLocation, () -> {});

    boolean isFailed = false;
    // second push will throw exception
    try {
      shuffleClient.pushDataToLocation(2, 3, 4, 5, byteBuf, primaryLocation, () -> {});
    } catch (IOException e) {
      isFailed = true;
    } finally {
      Assert.assertTrue("should failed", isFailed);
    }
  }
}
