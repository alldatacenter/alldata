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
package org.apache.drill.exec.rpc.security.plain;

import org.apache.drill.exec.rpc.user.security.UserAuthenticationException;
import org.apache.drill.exec.rpc.user.security.UserAuthenticator;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Plain SaslServer implementation.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4616">RFC for PLAIN SASL mechanism</a>
 */
class PlainServer implements SaslServer {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PlainServer.class);

  private static final String UTF_8_NULL = "\u0000";

  public static final String MECHANISM_NAME = "PLAIN";

  private final UserAuthenticator authenticator;

  private boolean completed = false;
  private String authorizationID;

  PlainServer(final UserAuthenticator authenticator, final Map<String, ?> properties) throws SaslException {
    if (properties != null) {
      if ("true".equalsIgnoreCase((String) properties.get(Sasl.POLICY_NOPLAINTEXT))) {
        throw new SaslException("PLAIN authentication is not permitted.");
      }
    }
    this.authenticator = authenticator;
  }

  @Override
  public String getMechanismName() {
    return MECHANISM_NAME;
  }

  @Override
  public byte[] evaluateResponse(byte[] response) throws SaslException {
    if (completed) {
      throw new IllegalStateException("PLAIN authentication already completed");
    }

    if (response == null) {
      throw new SaslException("Received null response");
    }

    final String payload = new String(response, StandardCharsets.UTF_8);

    // Separator defined in PlainClient is 0
    // three parts: [ authorizationID, authenticationID, password ]
    final String[] parts = payload.split(UTF_8_NULL, 3);
    if (parts.length != 3) {
      throw new SaslException("Received corrupt response. Expected 3 parts, but received "
          + parts.length);
    }
    String authorizationID = parts[0];
    final String authenticationID = parts[1];
    final String password = parts[2];

    if (authorizationID.isEmpty()) {
      authorizationID = authenticationID;
    }

    try {
      authenticator.authenticate(authenticationID, password);
    } catch (final UserAuthenticationException e) {
      throw new SaslException(e.getMessage());
    }

    if (!authorizationID.equals(authenticationID)) {
      throw new SaslException("Drill expects authorization ID and authentication ID to match. " +
          "Use inbound impersonation feature so one entity can act on behalf of another.");
    }

    this.authorizationID = authorizationID;
    completed = true;
    return null;
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public String getAuthorizationID() {
    if (completed) {
      return authorizationID;
    }
    throw new IllegalStateException("PLAIN authentication not completed");
  }

  @Override
  public Object getNegotiatedProperty(String propName) {
    if (completed) {
      return Sasl.QOP.equals(propName) ? "auth" : null;
    }
    throw new IllegalStateException("PLAIN authentication not completed");
  }

  @Override
  public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
    if (completed) {
      throw new SaslException("PLAIN supports neither integrity nor privacy");
    } else {
      throw new IllegalStateException("PLAIN authentication not completed");
    }
  }

  @Override
  public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
    if (completed) {
      throw new SaslException("PLAIN supports neither integrity nor privacy");
    } else {
      throw new IllegalStateException("PLAIN authentication not completed");
    }
  }

  @Override
  public void dispose() throws SaslException {
    authorizationID = null;
  }
}
