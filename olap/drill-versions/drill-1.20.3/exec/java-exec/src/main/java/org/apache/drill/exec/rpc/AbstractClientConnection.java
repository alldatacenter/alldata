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
package org.apache.drill.exec.rpc;

import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;

public abstract class AbstractClientConnection extends AbstractRemoteConnection implements ClientConnection {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractClientConnection.class);

  private SaslClient saslClient;

  public AbstractClientConnection(SocketChannel channel, String name,
                                  EncryptionContext encryptContext) {
    super(channel, name, encryptContext);
  }

  public AbstractClientConnection(SocketChannel channel, String name) {
    this(channel, name, new EncryptionContextImpl());
  }

  protected abstract Logger getLogger();

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
          checkState(saslClient != null);
          return saslClient.wrap(data, offset, len);
        }

        @Override
        public byte[] unwrap(byte[] data, int offset, int len) throws SaslException {
          checkState(saslClient != null);
          return saslClient.unwrap(data, offset, len);
        }
      };
    }
  }

  @Override
  public SaslClient getSaslClient() {
    checkState(this.saslClient != null);
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

    // Decrease the connection counter here since the close handler will be triggered
    // for all the types of connection
    decConnectionCounter();
    super.channelClosed(ex);
  }
}
