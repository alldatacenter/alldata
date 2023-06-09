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
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.rpc.security.SaslProperties;
import org.apache.hadoop.security.HadoopKerberosName;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;
import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkState;

public abstract class AbstractServerConnection<S extends ServerConnection<S>>
    extends AbstractRemoteConnection
    implements ServerConnection<S> {

  private final ConnectionConfig config;

  private RequestHandler<S> currentHandler;
  private SaslServer saslServer;

  public AbstractServerConnection(SocketChannel channel, String name, ConnectionConfig config,
                                  RequestHandler<S> handler) {
    super(channel, name, config.getEncryptionCtxt());
    this.config = config;
    this.currentHandler = handler;
  }

  public AbstractServerConnection(SocketChannel channel, ConnectionConfig config,
                                  RequestHandler<S> handler) {
    this(channel, config.getName(), config, handler);
  }

  @Override
  public BufferAllocator getAllocator() {
    return config.getAllocator();
  }

  protected abstract Logger getLogger();

  @Override
  public void initSaslServer(String mechanismName) throws SaslException {
    checkState(saslServer == null);
    try {
      this.saslServer = config.getAuthProvider()
          .getAuthenticatorFactory(mechanismName)
          .createSaslServer(UserGroupInformation.getLoginUser(),
              SaslProperties.getSaslProperties(isEncryptionEnabled(), getMaxWrappedSize()));
    } catch (final IOException e) {
      getLogger().debug("Login failed.", e);
      final Throwable cause = e.getCause();
      if (cause instanceof LoginException) {
        throw new SaslException("Failed to login.", cause);
      }
      throw new SaslException("Unexpected failure trying to login.", cause);
    }
    if (saslServer == null) {
      throw new SaslException(String.format("Server cannot initiate authentication using %s mechanism. Insufficient" +
          " parameters or selected mechanism doesn't support configured security layers ?", mechanismName));
    }

    // If encryption is enabled set the backend wrapper instance corresponding to this SaslServer in the connection
    // object. This is later used to do wrap/unwrap in handlers.
    if (isEncryptionEnabled()) {
      saslCodec = new SaslCodec() {

        @Override
        public byte[] wrap(byte[] data, int offset, int len) throws SaslException {
          checkState(saslServer != null);
          return saslServer.wrap(data, offset, len);
        }

        @Override
        public byte[] unwrap(byte[] data, int offset, int len) throws SaslException {
          checkState(saslServer != null);
          return saslServer.unwrap(data, offset, len);
        }
      };
    }
  }

  @Override
  public SaslServer getSaslServer() {
    checkState(saslServer != null);
    return saslServer;
  }

  @Override
  public void finalizeSaslSession() throws IOException {
    final String authorizationID = getSaslServer().getAuthorizationID();
    final String remoteShortName = new HadoopKerberosName(authorizationID).getShortName();
    final String localShortName = UserGroupInformation.getLoginUser().getShortUserName();
    if (!localShortName.equals(remoteShortName)) {
      throw new SaslException(String.format("'primary' part of remote drillbit's service principal " +
          "does not match with this drillbit's. Expected: '%s' Actual: '%s'", localShortName, remoteShortName));
    }
    getLogger().debug("Authenticated connection for {}", authorizationID);
  }

  @Override
  public RequestHandler<S> getCurrentHandler() {
    return currentHandler;
  }

  @Override
  public void changeHandlerTo(final RequestHandler<S> handler) {
    checkNotNull(handler);
    this.currentHandler = handler;
  }

  @Override
  public void setEncryption(boolean encrypted) {
    throw new UnsupportedOperationException("Changing encryption setting on server connection is not permitted.");
  }

  @Override
  public void setMaxWrappedSize(int maxWrappedSize) {
    throw new UnsupportedOperationException("Changing maxWrappedSize setting on server connection is not permitted.");
  }

  @Override
  public void disposeSaslServer() {
    try {
      if (saslServer != null) {
        saslServer.dispose();
        saslServer = null;
      }
    } catch (final SaslException e) {
      getLogger().warn("Unclean disposal.", e);
    }
  }

  @Override
  public void channelClosed(RpcException ex) {
    // This will be triggered from Netty when a channel is closed. We should cleanup here
    // as this will handle case for both client closing the connection or server closing the
    // connection.
    disposeSaslServer();

    // Decrease the connection counter here since the close handler will be triggered
    // for all the types of connection
    decConnectionCounter();
    super.channelClosed(ex);
  }
}
