/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security.authentication;

import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.springframework.security.core.AuthenticationException;

/**
 * AuthenticationMethodNotAllowedException is an AuthenticationException implementation to be thrown
 * when the specified authentication method is not allowed for the relevant user.
 */
public class AuthenticationMethodNotAllowedException extends AuthenticationException {
  private final String username;
  private final UserAuthenticationType userAuthenticationType;

  public AuthenticationMethodNotAllowedException(String username, UserAuthenticationType authenticationType) {
    this(username, authenticationType, createDefaultMessage(username, authenticationType));
  }

  public AuthenticationMethodNotAllowedException(String username, UserAuthenticationType authenticationType, Throwable cause) {
    this(username, authenticationType, createDefaultMessage(username, authenticationType), cause);
  }

  public AuthenticationMethodNotAllowedException(String username, UserAuthenticationType authenticationType, String message) {
    super(message);
    this.username = username;
    this.userAuthenticationType = authenticationType;
  }

  public AuthenticationMethodNotAllowedException(String username, UserAuthenticationType authenticationType, String message, Throwable cause) {
    super(message, cause);
    this.username = username;
    this.userAuthenticationType = authenticationType;
  }

  public String getUsername() {
    return username;
  }

  public UserAuthenticationType getUserAuthenticationType() {
    return userAuthenticationType;
  }

  private static String createDefaultMessage(String username, UserAuthenticationType authenticationType) {
    return String.format("%s is not authorized to authenticate using %s",
        username,
        (authenticationType == null) ? "null" : authenticationType.name());
  }
}
