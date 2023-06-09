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

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.security.AuthenticatorFactory;
import org.apache.drill.exec.rpc.security.SaslProperties;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility class providing common methods shared between {@link org.apache.drill.exec.rpc.data.DataClient} and
 * {@link org.apache.drill.exec.rpc.control.ControlClient}
 */
public final class BitRpcUtility {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BitRpcUtility.class);

  /**
   * Method to do validation on the handshake message received from server side. Only used by BitClients NOT UserClient.
   * Verify if rpc version of handshake message matches the supported RpcVersion and also validates the
   * security configuration between client and server
   * @param handshakeRpcVersion - rpc version received in handshake message
   * @param remoteAuthMechs - authentication mechanisms supported by server
   * @param rpcVersion - supported rpc version on client
   * @param connection - client connection
   * @param config - client connectin config
   * @param client - data client or control client
   * @return - Immutable list of authentication mechanisms supported by server or null
   * @throws RpcException - exception is thrown if rpc version or authentication configuration mismatch is found
   */
  public static List<String> validateHandshake(int handshakeRpcVersion, List<String> remoteAuthMechs, int rpcVersion,
                                               ClientConnection connection, BitConnectionConfig config,
                                               BasicClient client) throws RpcException {

    if (handshakeRpcVersion != rpcVersion) {
      throw new RpcException(String.format("Invalid rpc version.  Expected %d, actual %d.",
        handshakeRpcVersion, rpcVersion));
    }

    if (remoteAuthMechs.size() != 0) { // remote requires authentication
      client.setAuthComplete(false);
      return ImmutableList.copyOf(remoteAuthMechs);
    } else {
      if (config.getAuthMechanismToUse() != null) { // local requires authentication
        throw new RpcException(String.format("Remote Drillbit does not require auth, but auth is enabled in " +
          "local Drillbit configuration. [Details: connection: (%s) and LocalAuthMechanism: (%s). Please check " +
          "security configuration for bit-to-bit.", connection.getName(), config.getAuthMechanismToUse()));
      }
    }
    return null;
  }

  /**
   * Creates various instances needed to start the SASL handshake. This is called from
   * {@link BasicClient#prepareSaslHandshake(RpcConnectionHandler, List)} only for
   * {@link org.apache.drill.exec.rpc.data.DataClient} and {@link org.apache.drill.exec.rpc.control.ControlClient}
   *
   * @param connectionHandler    - Connection handler used by client's to know about success/failure conditions.
   * @param serverAuthMechanisms - List of auth mechanisms configured on server side
   * @param connection - ClientConnection used for authentication
   * @param config - ClientConnection config
   * @param endpoint - Remote DrillbitEndpoint
   * @param client - Either of DataClient/ControlClient instance
   * @param saslRpcType - SASL_MESSAGE RpcType for Data and Control channel
   */
  public static <T extends EnumLite, CC extends ClientConnection, HS extends MessageLite, HR extends MessageLite>
  void prepareSaslHandshake(final RpcConnectionHandler<CC> connectionHandler, List<String> serverAuthMechanisms,
                            CC connection, BitConnectionConfig config, DrillbitEndpoint endpoint,
                            final BasicClient<T, CC, HS, HR> client, T saslRpcType) {
    try {
      final Map<String, String> saslProperties = SaslProperties.getSaslProperties(connection.isEncryptionEnabled(),
        connection.getMaxWrappedSize());
      final UserGroupInformation ugi = UserGroupInformation.getLoginUser();
      final AuthenticatorFactory factory = config.getAuthFactory(serverAuthMechanisms);
      client.startSaslHandshake(connectionHandler, config.getSaslClientProperties(endpoint, saslProperties),
        ugi, factory, saslRpcType);
    } catch (final IOException e) {
      logger.error("Failed while doing setup for starting sasl handshake for connection {}", connection.getName());
      final Exception ex = new RpcException(String.format("Failed to initiate authentication to %s",
        endpoint.getAddress()), e);
      connectionHandler.connectionFailed(RpcConnectionHandler.FailureType.AUTHENTICATION, ex);
    }
  }

  /**
   * Verifies if local and remote Drillbit Endpoint has same control server by using address and control port
   * information. This method is used instead of equals in {@link DrillbitEndpoint} because DrillbitEndpoint stores
   * state information in it.
   * For local Drillbit a reference is stored in {@link org.apache.drill.exec.server.DrillbitContext} as soon as
   * Drillbit is started in {@link org.apache.drill.exec.service.ServiceEngine#start} with state as STARTUP, but
   * while planning minor fragment the assignment list is used from active list of Drillbits in which state for local
   * Drillbit will not be STARTUP
   * @param local - DrillbitEndpoint instance for local bit
   * @param remote - DrillbitEndpoint instance for remote bit
   * @return true if address and control port for local and remote are same.
   *         false - otherwise
   */
  public static boolean isLocalControlServer(DrillbitEndpoint local, DrillbitEndpoint remote) {
    return local.hasAddress() && local.hasControlPort() && remote.hasAddress() && remote.hasControlPort() &&
      local.getAddress().equals(remote.getAddress()) && local.getControlPort() == remote.getControlPort();
  }

  // Suppress default constructor
  private BitRpcUtility() {
  }
}
