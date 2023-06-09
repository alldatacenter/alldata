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
package org.apache.drill.exec.rpc.control;

import java.util.List;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.work.batch.ControlMessageHandler;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

/**
 * Manages communication tunnels between nodes.
 */
public class ControllerImpl implements Controller {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControllerImpl.class);

  private volatile ControlServer server;
  private final ConnectionManagerRegistry connectionRegistry;
  private final CustomHandlerRegistry handlerRegistry;
  private final ControlConnectionConfig config;

  public ControllerImpl(BootStrapContext context, BufferAllocator allocator, ControlMessageHandler handler)
      throws DrillbitStartupException {
    config = new ControlConnectionConfig(allocator, context, handler);
    this.connectionRegistry = new ConnectionManagerRegistry(config);
    this.handlerRegistry = handler.getHandlerRegistry();

    // Initialize the singleton instance of ControlRpcMetrics.
    ((ControlRpcMetrics)ControlRpcMetrics.getInstance()).initialize(config.isEncryptionEnabled(), allocator);
  }

  @Override
  public DrillbitEndpoint start(DrillbitEndpoint partialEndpoint, final boolean allowPortHunting) {
    server = new ControlServer(config, connectionRegistry);
    int port = config.getBootstrapContext().getConfig().getInt(ExecConstants.INITIAL_BIT_PORT);
    port = server.bind(port, allowPortHunting);
    DrillbitEndpoint completeEndpoint = partialEndpoint.toBuilder().setControlPort(port).build();
    connectionRegistry.setLocalEndpoint(completeEndpoint);
    handlerRegistry.setEndpoint(completeEndpoint);
    return completeEndpoint;
  }

  @Override
  public ControlTunnel getTunnel(DrillbitEndpoint endpoint) {
    return new ControlTunnel(connectionRegistry.getConnectionManager(endpoint));
  }


  @SuppressWarnings("unchecked")
  @Override
  public <REQUEST extends MessageLite, RESPONSE extends MessageLite> void registerCustomHandler(int messageTypeId,
      CustomMessageHandler<REQUEST, RESPONSE> handler, Parser<REQUEST> parser) {
    handlerRegistry.registerCustomHandler(
        messageTypeId,
        handler,
        new ControlTunnel.ProtoSerDe<REQUEST>(parser),
        (CustomSerDe<RESPONSE>) new ControlTunnel.ProtoSerDe<Message>(null));
  }

  @Override
  public <REQUEST, RESPONSE> void registerCustomHandler(int messageTypeId,
      CustomMessageHandler<REQUEST, RESPONSE> handler,
      CustomSerDe<REQUEST> requestSerde,
      CustomSerDe<RESPONSE> responseSerde) {
    handlerRegistry.registerCustomHandler(messageTypeId, handler, requestSerde, responseSerde);
  }


  @Override
  public void close() throws Exception {
    List<AutoCloseable> closeables = Lists.newArrayList();
    closeables.add(server);

    for (ControlConnectionManager bt : connectionRegistry) {
      closeables.add(bt);
    }

    AutoCloseables.close(closeables);
  }


}
