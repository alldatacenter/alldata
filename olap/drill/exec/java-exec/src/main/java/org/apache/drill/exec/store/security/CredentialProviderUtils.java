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
package org.apache.drill.exec.store.security;

import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.exec.store.security.oauth.OAuthTokenCredentials;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

public class CredentialProviderUtils {

  /**
   * Returns specified {@code CredentialsProvider credentialsProvider}
   * if it is not null or builds and returns {@link PlainCredentialsProvider}
   * with specified {@code USERNAME} and {@code PASSWORD}.
   */
  public static CredentialsProvider getCredentialsProvider(
      String username, String password,
      CredentialsProvider credentialsProvider) {
    if (credentialsProvider != null) {
      return credentialsProvider;
    }
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
    if (username != null) {
      mapBuilder.put(UsernamePasswordCredentials.USERNAME, username);
    }
    if (password != null) {
      mapBuilder.put(UsernamePasswordCredentials.PASSWORD, password);
    }
    return new PlainCredentialsProvider(mapBuilder.build());
  }

  /**
   * Constructor for OAuth based authentication.  Allows for tokens to be stored in whatever vault
   * mechanism the user chooses.
   * Returns specified {@code CredentialsProvider credentialsProvider}
   * if it is not null or builds and returns {@link PlainCredentialsProvider}
   * with specified {@code CLIENT_ID}, {@code CLIENT_SECRET}, {@code ACCESS_TOKEN}, {@code REFRESH_TOKEN}.
   * @param clientID The OAuth Client ID.  This is provided by the application during signup.
   * @param clientSecret The OAUth Client Secret.  This is provided by the application during signup.
   * @param tokenURI The URI from which you swap the auth code for access and refresh tokens.
   * @param username  Optional username for proxy or other services
   * @param password  Optional password for proxy or other services
   * @param credentialsProvider  The credential store which retains the credentials.
   * @return A credential provider with the access tokens
   */
  public static CredentialsProvider getCredentialsProvider(
    String clientID,
    String clientSecret,
    String tokenURI,
    String username,
    String password,
    CredentialsProvider credentialsProvider) {

    if (credentialsProvider != null) {
      return credentialsProvider;
    }
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
    if (clientID != null) {
      mapBuilder.put(OAuthTokenCredentials.CLIENT_ID, clientID);
    }
    if (clientSecret != null) {
      mapBuilder.put(OAuthTokenCredentials.CLIENT_SECRET, clientSecret);
    }
    if (username != null) {
      mapBuilder.put(OAuthTokenCredentials.USERNAME, username);
    }
    if (password != null) {
      mapBuilder.put(OAuthTokenCredentials.PASSWORD, password);
    }
    if (tokenURI != null) {
      mapBuilder.put(OAuthTokenCredentials.TOKEN_URI, tokenURI);
    }

    return new PlainCredentialsProvider(mapBuilder.build());
  }
}
