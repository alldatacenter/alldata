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

package org.apache.uniffle.client.impl.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GrpcClient {

  private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);
  protected String host;
  protected int port;
  protected boolean usePlaintext;
  protected int maxRetryAttempts;
  protected ManagedChannel channel;

  protected GrpcClient(String host, int port, int maxRetryAttempts, boolean usePlaintext) {
    this.host = host;
    this.port = port;
    this.maxRetryAttempts = maxRetryAttempts;
    this.usePlaintext = usePlaintext;

    // build channel
    ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);

    if (usePlaintext) {
      channelBuilder.usePlaintext();
    }

    if (maxRetryAttempts > 0) {
      channelBuilder.enableRetry().maxRetryAttempts(maxRetryAttempts);
    }
    channelBuilder.maxInboundMessageSize(Integer.MAX_VALUE);

    channel = channelBuilder.build();
  }

  protected GrpcClient(ManagedChannel channel) {
    this.channel = channel;
  }

  public void close() {
    try {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Can't close GRPC client to " + host + ":" + port);
    }
  }

}
