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

  protected static final int PRIMARY_RPC_PORT = 1234;
  protected static final int PRIMARY_PUSH_PORT = 1235;
  protected static final int PRIMARY_FETCH_PORT = 1236;
  protected static final int PRIMARY_REPLICATE_PORT = 1237;
  protected static final int REPLICA_RPC_PORT = 4321;
  protected static final int REPLICA_PUSH_PORT = 4322;
  protected static final int REPLICA_FETCH_PORT = 4323;
  protected static final int REPLICA_REPLICATE_PORT = 4324;
  protected static final PartitionLocation primaryLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          PRIMARY_RPC_PORT,
          PRIMARY_PUSH_PORT,
          PRIMARY_FETCH_PORT,
          PRIMARY_REPLICATE_PORT,
          PartitionLocation.Mode.PRIMARY);
  protected static final PartitionLocation replicaLocation =
      new PartitionLocation(
          0,
          1,
          "localhost",
          REPLICA_RPC_PORT,
          REPLICA_PUSH_PORT,
          REPLICA_FETCH_PORT,
          REPLICA_REPLICATE_PORT,
          PartitionLocation.Mode.REPLICA);

  protected final int BATCH_HEADER_SIZE = 4 * 4;
  protected ChannelFuture mockedFuture = mock(ChannelFuture.class);

  protected CelebornConf setupEnv(CompressionCodec codec) throws IOException, InterruptedException {
    CelebornConf conf = new CelebornConf();
    conf.set(CelebornConf.SHUFFLE_COMPRESSION_CODEC().key(), codec.name());
    conf.set(CelebornConf.CLIENT_PUSH_RETRY_THREADS().key(), "1");
    conf.set(CelebornConf.CLIENT_PUSH_BUFFER_MAX_SIZE().key(), "1K");
    shuffleClient =
        new ShuffleClientImpl(TEST_APPLICATION_ID, conf, new UserIdentifier("mock", "mock"), false);
    primaryLocation.setPeer(replicaLocation);

    when(endpointRef.askSync(
            ControlMessages.RegisterShuffle$.MODULE$.apply(TEST_SHUFFLE_ID, 1, 1),
            ClassTag$.MODULE$.apply(PbRegisterShuffleResponse.class)))
        .thenAnswer(
            t ->
                ControlMessages.RegisterShuffleResponse$.MODULE$.apply(
                    StatusCode.SUCCESS, new PartitionLocation[] {primaryLocation}));

    shuffleClient.setupLifecycleManagerRef(endpointRef);
    when(clientFactory.createClient(
            primaryLocation.getHost(), primaryLocation.getPushPort(), TEST_REDUCRE_ID))
        .thenAnswer(t -> client);

    shuffleClient.dataClientFactory = clientFactory;
    return conf;
  }
}
