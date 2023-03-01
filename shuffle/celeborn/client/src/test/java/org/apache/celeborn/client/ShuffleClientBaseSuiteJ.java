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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import scala.reflect.ClassTag$;

import io.netty.channel.ChannelFuture;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.protocol.CompressionCodec;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.PbRegisterShuffleResponse;
import org.apache.celeborn.common.protocol.message.ControlMessages;
import org.apache.celeborn.common.protocol.message.StatusCode;
import org.apache.celeborn.common.rpc.RpcEndpointRef;

public abstract class ShuffleClientBaseSuiteJ {
  protected ShuffleClientImpl shuffleClient = null;
  protected static final RpcEndpointRef endpointRef = mock(RpcEndpointRef.class);
  protected static final TransportClientFactory clientFactory = mock(TransportClientFactory.class);
  protected final TransportClient client = mock(TransportClient.class);

  protected static final String TEST_APPLICATION_ID = "testapp1";
  protected static final int TEST_SHUFFLE_ID = 1;
  protected static final int TEST_ATTEMPT_ID = 0;
  protected static final int TEST_REDUCRE_ID = 0;

  protected static final int MASTER_RPC_PORT = 1234;
  protected static final int MASTER_PUSH_PORT = 1235;
  protected static final int MASTER_FETCH_PORT = 1236;
  protected static final int MASTER_REPLICATE_PORT = 1237;
  protected static final int SLAVE_RPC_PORT = 4321;
  protected static final int SLAVE_PUSH_PORT = 4322;
  protected static final int SLAVE_FETCH_PORT = 4323;
  protected static final int SLAVE_REPLICATE_PORT = 4324;
  protected static final PartitionLocation masterLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          MASTER_RPC_PORT,
          MASTER_PUSH_PORT,
          MASTER_FETCH_PORT,
          MASTER_REPLICATE_PORT,
          PartitionLocation.Mode.MASTER);
  protected static final PartitionLocation slaveLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          SLAVE_RPC_PORT,
          SLAVE_PUSH_PORT,
          SLAVE_FETCH_PORT,
          SLAVE_REPLICATE_PORT,
          PartitionLocation.Mode.SLAVE);

  protected final int BATCH_HEADER_SIZE = 4 * 4;
  protected ChannelFuture mockedFuture = mock(ChannelFuture.class);

  protected CelebornConf setupEnv(CompressionCodec codec) throws IOException, InterruptedException {
    CelebornConf conf = new CelebornConf();
    conf.set("celeborn.shuffle.compression.codec", codec.name());
    conf.set("celeborn.push.retry.threads", "1");
    conf.set("celeborn.push.buffer.size", "1K");
    shuffleClient = new ShuffleClientImpl(conf, new UserIdentifier("mock", "mock"));
    masterLocation.setPeer(slaveLocation);

    when(endpointRef.askSync(
            ControlMessages.RegisterShuffle$.MODULE$.apply(
                TEST_APPLICATION_ID, TEST_SHUFFLE_ID, 1, 1),
            ClassTag$.MODULE$.apply(PbRegisterShuffleResponse.class)))
        .thenAnswer(
            t ->
                ControlMessages.RegisterShuffleResponse$.MODULE$.apply(
                    StatusCode.SUCCESS, new PartitionLocation[] {masterLocation}));

    shuffleClient.setupMetaServiceRef(endpointRef);
    when(clientFactory.createClient(
            masterLocation.getHost(), masterLocation.getPushPort(), TEST_REDUCRE_ID))
        .thenAnswer(t -> client);

    shuffleClient.dataClientFactory = clientFactory;
    return conf;
  }
}
