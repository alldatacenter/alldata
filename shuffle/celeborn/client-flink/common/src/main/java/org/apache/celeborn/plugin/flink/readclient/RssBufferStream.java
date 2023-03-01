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

package org.apache.celeborn.plugin.flink.readclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.protocol.*;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.plugin.flink.network.FlinkTransportClientFactory;

public class RssBufferStream {

  private static Logger logger = LoggerFactory.getLogger(RssBufferStream.class);
  private CelebornConf conf;
  private FlinkTransportClientFactory clientFactory;
  private String shuffleKey;
  private PartitionLocation[] locations;
  private int subIndexStart;
  private int subIndexEnd;
  private TransportClient client;
  private int currentLocationIndex = 0;
  private long streamId = 0;

  public RssBufferStream() {}

  public RssBufferStream(
      CelebornConf conf,
      FlinkTransportClientFactory dataClientFactory,
      String shuffleKey,
      PartitionLocation[] locations,
      int subIndexStart,
      int subIndexEnd) {
    this.conf = conf;
    this.clientFactory = dataClientFactory;
    this.shuffleKey = shuffleKey;
    this.locations = locations;
    this.subIndexStart = subIndexStart;
    this.subIndexEnd = subIndexEnd;
  }

  public void open(
      Supplier<ByteBuf> supplier,
      int initialCredit,
      FlinkShuffleClientImpl mapShuffleClient,
      Consumer<RequestMessage> messageConsumer)
      throws IOException, InterruptedException {
    this.client =
        clientFactory.createClient(
            locations[currentLocationIndex].getHost(),
            locations[currentLocationIndex].getFetchPort());

    String fileName = locations[currentLocationIndex].getFileName();
    OpenStreamWithCredit openBufferStream =
        new OpenStreamWithCredit(shuffleKey, fileName, subIndexStart, subIndexEnd, initialCredit);
    client.sendRpc(
        openBufferStream.toByteBuffer(),
        new RpcResponseCallback() {

          @Override
          public void onSuccess(ByteBuffer response) {
            StreamHandle streamHandle = (StreamHandle) Message.decode(response);
            RssBufferStream.this.streamId = streamHandle.streamId;
            clientFactory.registerSupplier(RssBufferStream.this.streamId, supplier);
            mapShuffleClient
                .getReadClientHandler()
                .registerHandler(streamId, messageConsumer, client);
            logger.debug("open stream success stream id:{}, fileName: {}", streamId, fileName);
          }

          @Override
          public void onFailure(Throwable e) {
            throw new RuntimeException("OpenStream failed.", e);
          }
        });
  }

  public void addCredit(ReadAddCredit addCredit) {
    this.client
        .getChannel()
        .writeAndFlush(addCredit)
        .addListener(
            future -> {
              if (future.isSuccess()) {
                // Send ReadAddCredit do not expect response.
              } else {
                logger.warn(
                    "Send ReadAddCredit to {} failed, detail {}",
                    this.client.getSocketAddress().toString(),
                    future.cause());
              }
            });
  }

  public static RssBufferStream empty() {
    return emptyRssBufferStream;
  }

  public long getStreamId() {
    return streamId;
  }

  public static RssBufferStream create(
      CelebornConf conf,
      FlinkTransportClientFactory dataClientFactory,
      String shuffleKey,
      PartitionLocation[] locations,
      int subIndexStart,
      int subIndexEnd) {
    if (locations == null || locations.length == 0) {
      return empty();
    } else {
      return new RssBufferStream(
          conf, dataClientFactory, shuffleKey, locations, subIndexStart, subIndexEnd);
    }
  }

  private static final RssBufferStream emptyRssBufferStream = new RssBufferStream();

  public TransportClient getClient() {
    return client;
  }

  public void close() {
    clientFactory.unregisterSupplier(this.getStreamId());
  }
}
