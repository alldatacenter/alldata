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

import com.google.protobuf.MessageLite;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.SocketChannel;

import org.apache.drill.exec.proto.BitControl.RpcType;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.AbstractServerConnection;
import org.apache.drill.exec.rpc.ClientConnection;
import org.apache.drill.exec.rpc.RequestHandler;
import org.apache.drill.exec.rpc.RpcBus;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.SaslCodec;

import org.slf4j.Logger;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.util.UUID;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;

public class ControlConnection extends AbstractServerConnection<ControlConnection> implements ClientConnection {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ControlConnection.class);

  private final RpcBus<RpcType, ControlConnection> bus;
  private final UUID id;

  private volatile DrillbitEndpoint endpoint;
  private volatile boolean active = false;

  private SaslClient saslClient;

  ControlConnection(SocketChannel channel, String name, ControlConnectionConfig config,
                    RequestHandler<ControlConnection> handler, RpcBus<RpcType, ControlConnection> bus) {
    super(channel, name, config, handler);
    this.bus = bus;
    this.id = UUID.randomUUID();
  }

  void setEndpoint(DrillbitEndpoint endpoint) {
    assert this.endpoint == null : "Endpoint should only be set once (only in the case in incoming server requests).";
    this.endpoint = endpoint;
    active = true;
  }

  public <SEND extends MessageLite, RECEIVE extends MessageLite>
  void send(RpcOutcomeListener<RECEIVE> outcomeListener, RpcType rpcType, SEND protobufBody,
            Class<RECEIVE> clazz, ByteBuf... dataBodies) {
    bus.send(outcomeListener, this, rpcType, protobufBody, clazz, dataBodies);
  }

  public <SEND extends MessageLite, RECEIVE extends MessageLite>
  void sendUnsafe(RpcOutcomeListener<RECEIVE> outcomeListener, RpcType rpcType, SEND protobufBody,
                  Class<RECEIVE> clazz, ByteBuf... dataBodies) {
    bus.send(outcomeListener, this, rpcType, protobufBody, clazz, true, dataBodies);
  }

  @Override
  public boolean isActive() {
    return active && super.isActive();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ControlConnection other = (ControlConnection) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public void setSaslClient(final SaslClient saslClient) {
    checkState(this.saslClient == null);
    this.saslClient = saslClient;

    // If encryption is enabled set the backend wrapper instance corresponding to this SaslClient in the connection
    // object. This is later used to do wrap/unwrap in handlers.
    if (isEncryptionEnabled()) {
      saslCodec = new SaslCodec() {
        @Override
        public byte[] wrap(byte[] data, int offset, int len) throws SaslException {
          assert saslClient != null;
          return saslClient.wrap(data, offset, len);
        }

        @Override
        public byte[] unwrap(byte[] data, int offset, int len) throws SaslException {
          assert saslClient != null;
          return saslClient.unwrap(data, offset, len);
        }
      };
    }
  }

  @Override
  public SaslClient getSaslClient() {
    checkState(saslClient != null);
    return saslClient;
  }

  @Override
  public void disposeSaslClient() {
    try {
      if (saslClient != null) {
        saslClient.dispose();
        saslClient = null;
      }
    } catch (final SaslException e) {
      getLogger().warn("Unclean disposal", e);
    }
  }

  @Override
  public void channelClosed(RpcException ex) {
    // This will be triggered from Netty when a channel is closed. We should cleanup here
    // as this will handle case for both client closing the connection or server closing the
    // connection.
    disposeSaslClient();

    super.channelClosed(ex);
  }

  @Override
  public void incConnectionCounter() {
    ControlRpcMetrics.getInstance().addConnectionCount();
  }

  @Override
  public void decConnectionCounter() {
    ControlRpcMetrics.getInstance().decConnectionCount();
  }
}
