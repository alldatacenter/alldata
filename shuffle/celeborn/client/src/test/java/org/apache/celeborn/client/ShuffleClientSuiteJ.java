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

package org.apache.celeborn.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import org.apache.celeborn.client.compress.Compressor;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.protocol.CompressionCodec;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.message.ControlMessages.*;
import org.apache.celeborn.common.protocol.message.StatusCode;
import org.apache.celeborn.common.rpc.RpcEndpointRef;

public class ShuffleClientSuiteJ {

  private ShuffleClientImpl shuffleClient;
  private final RpcEndpointRef endpointRef = mock(RpcEndpointRef.class);
  private final TransportClientFactory clientFactory = mock(TransportClientFactory.class);
  private final TransportClient client = mock(TransportClient.class);

  private static final String TEST_APPLICATION_ID = "testapp1";
  private static final int TEST_SHUFFLE_ID = 1;
  private static final int TEST_ATTEMPT_ID = 0;
  private static final int TEST_REDUCRE_ID = 0;

  private static final int PRIMARY_RPC_PORT = 1234;
  private static final int PRIMARY_PUSH_PORT = 1235;
  private static final int PRIMARY_FETCH_PORT = 1236;
  private static final int PRIMARY_REPLICATE_PORT = 1237;
  private static final int REPLICA_RPC_PORT = 4321;
  private static final int REPLICA_PUSH_PORT = 4322;
  private static final int REPLICA_FETCH_PORT = 4323;
  private static final int REPLICA_REPLICATE_PORT = 4324;
  private static final PartitionLocation primaryLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          PRIMARY_RPC_PORT,
          PRIMARY_PUSH_PORT,
          PRIMARY_FETCH_PORT,
          PRIMARY_REPLICATE_PORT,
          PartitionLocation.Mode.PRIMARY);
  private static final PartitionLocation replicaLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          REPLICA_RPC_PORT,
          REPLICA_PUSH_PORT,
          REPLICA_FETCH_PORT,
          REPLICA_REPLICATE_PORT,
          PartitionLocation.Mode.REPLICA);

  private static final byte[] TEST_BUF1 = "hello world".getBytes(StandardCharsets.UTF_8);
  private final int BATCH_HEADER_SIZE = 4 * 4;

  @Test
  public void testPushData() throws IOException, InterruptedException {
    for (CompressionCodec codec : CompressionCodec.values()) {
      CelebornConf conf = setupEnv(codec);

      int pushDataLen =
          shuffleClient.pushData(
              TEST_SHUFFLE_ID,
              TEST_ATTEMPT_ID,
              TEST_ATTEMPT_ID,
              TEST_REDUCRE_ID,
              TEST_BUF1,
              0,
              TEST_BUF1.length,
              1,
              1);

      if (codec.equals(CompressionCodec.NONE)) {
        assert (pushDataLen == TEST_BUF1.length + BATCH_HEADER_SIZE);
      } else {
        Compressor compressor = Compressor.getCompressor(conf);
        compressor.compress(TEST_BUF1, 0, TEST_BUF1.length);
        final int compressedTotalSize = compressor.getCompressedTotalSize();
        assert (pushDataLen == compressedTotalSize + BATCH_HEADER_SIZE);
      }
    }
  }

  @Test
  public void testMergeData() throws IOException, InterruptedException {
    for (CompressionCodec codec : CompressionCodec.values()) {
      CelebornConf conf = setupEnv(codec);

      int mergeSize =
          shuffleClient.mergeData(
              TEST_SHUFFLE_ID,
              TEST_ATTEMPT_ID,
              TEST_ATTEMPT_ID,
              TEST_REDUCRE_ID,
              TEST_BUF1,
              0,
              TEST_BUF1.length,
              1,
              1);

      if (codec.equals(CompressionCodec.NONE)) {
        assert (mergeSize == TEST_BUF1.length + BATCH_HEADER_SIZE);
      } else {
        Compressor compressor = Compressor.getCompressor(conf);
        compressor.compress(TEST_BUF1, 0, TEST_BUF1.length);
        final int compressedTotalSize = compressor.getCompressedTotalSize();
        assert (mergeSize == compressedTotalSize + BATCH_HEADER_SIZE);
      }

      byte[] buf1k = RandomStringUtils.random(4000).getBytes(StandardCharsets.UTF_8);
      int largeMergeSize =
          shuffleClient.mergeData(
              TEST_SHUFFLE_ID,
              TEST_ATTEMPT_ID,
              TEST_ATTEMPT_ID,
              TEST_REDUCRE_ID,
              buf1k,
              0,
              buf1k.length,
              1,
              1);

      if (codec.equals(CompressionCodec.NONE)) {
        assert (largeMergeSize == buf1k.length + BATCH_HEADER_SIZE);
      } else {
        Compressor compressor = Compressor.getCompressor(conf);
        compressor.compress(buf1k, 0, buf1k.length);
        final int compressedTotalSize = compressor.getCompressedTotalSize();
        assert (largeMergeSize == compressedTotalSize + BATCH_HEADER_SIZE);
      }
    }
  }

  private CelebornConf setupEnv(CompressionCodec codec) throws IOException, InterruptedException {
    CelebornConf conf = new CelebornConf();
    conf.set(CelebornConf.SHUFFLE_COMPRESSION_CODEC().key(), codec.name());
    conf.set(CelebornConf.CLIENT_PUSH_RETRY_THREADS().key(), "1");
    conf.set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "1K");
    shuffleClient =
        new ShuffleClientImpl(TEST_APPLICATION_ID, conf, new UserIdentifier("mock", "mock"), false);

    primaryLocation.setPeer(replicaLocation);
    when(endpointRef.askSync(any(), any(), any()))
        .thenAnswer(
            t ->
                RegisterShuffleResponse$.MODULE$.apply(
                    StatusCode.SUCCESS, new PartitionLocation[] {primaryLocation}));

    shuffleClient.setupLifecycleManagerRef(endpointRef);

    ChannelFuture mockedFuture =
        new ChannelFuture() {
          @Override
          public Channel channel() {
            return null;
          }

          @Override
          public ChannelFuture addListener(
              GenericFutureListener<? extends Future<? super Void>> listener) {
            return null;
          }

          @SafeVarargs
          @Override
          public final ChannelFuture addListeners(
              GenericFutureListener<? extends Future<? super Void>>... listeners) {
            return null;
          }

          @Override
          public ChannelFuture removeListener(
              GenericFutureListener<? extends Future<? super Void>> listener) {
            return null;
          }

          @SafeVarargs
          @Override
          public final ChannelFuture removeListeners(
              GenericFutureListener<? extends Future<? super Void>>... listeners) {
            return null;
          }

          @Override
          public ChannelFuture sync() {
            return null;
          }

          @Override
          public ChannelFuture syncUninterruptibly() {
            return null;
          }

          @Override
          public ChannelFuture await() {
            return null;
          }

          @Override
          public ChannelFuture awaitUninterruptibly() {
            return null;
          }

          @Override
          public boolean isVoid() {
            return false;
          }

          @Override
          public boolean isSuccess() {
            return true;
          }

          @Override
          public boolean isCancellable() {
            return false;
          }

          @Override
          public Throwable cause() {
            return null;
          }

          @Override
          public boolean await(long timeout, TimeUnit unit) {
            return true;
          }

          @Override
          public boolean await(long timeoutMillis) {
            return true;
          }

          @Override
          public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
          }

          @Override
          public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
          }

          @Override
          public Void getNow() {
            return null;
          }

          @Override
          public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
          }

          @Override
          public boolean isCancelled() {
            return false;
          }

          @Override
          public boolean isDone() {
            return true;
          }

          @Override
          public Void get() {
            return null;
          }

          @SuppressWarnings("NullableProblems")
          @Override
          public Void get(long timeout, TimeUnit unit) {
            return null;
          }
        };

    when(client.pushData(any(), anyLong(), any())).thenAnswer(t -> mockedFuture);
    when(clientFactory.createClient(
            primaryLocation.getHost(), primaryLocation.getPushPort(), TEST_REDUCRE_ID))
        .thenAnswer(t -> client);

    when(client.pushMergedData(any(), anyLong(), any())).thenAnswer(t -> mockedFuture);
    when(clientFactory.createClient(primaryLocation.getHost(), primaryLocation.getPushPort()))
        .thenAnswer(t -> client);

    shuffleClient.dataClientFactory = clientFactory;
    return conf;
  }
}
