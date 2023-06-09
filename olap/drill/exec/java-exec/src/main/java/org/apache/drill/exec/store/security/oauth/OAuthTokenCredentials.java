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

package org.apache.drill.exec.store.security.oauth;

import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.oauth.PersistentTokenTable;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;

import java.util.HashMap;
import java.util.Map;

public class OAuthTokenCredentials extends UsernamePasswordCredentials {

  public static final String CLIENT_ID = "clientID";
  public static final String CLIENT_SECRET = "clientSecret";
  public static final String ACCESS_TOKEN = "accessToken";
  public static final String REFRESH_TOKEN = "refreshToken";
  public static final String TOKEN_URI = "tokenURI";

  private final String clientID;
  private final String clientSecret;
  private final String tokenURI;
  private PersistentTokenTable tokenTable;

  public OAuthTokenCredentials(CredentialsProvider credentialsProvider) {
   super(credentialsProvider);
   if (credentialsProvider == null) {
     this.clientID = null;
     this.clientSecret = null;
     this.tokenURI = null;
   } else {
     Map<String, String> credentials = credentialsProvider.getCredentials() == null
       ? new HashMap<>() : credentialsProvider.getCredentials();

     this.clientID = credentials.getOrDefault(CLIENT_ID, null);
     this.clientSecret = credentials.getOrDefault(CLIENT_SECRET, null);
     this.tokenURI = credentials.getOrDefault(TOKEN_URI, null);
   }
  }

  public OAuthTokenCredentials(CredentialsProvider credentialsProvider, PersistentTokenTable tokenTable) {
    this(credentialsProvider);
    this.tokenTable = tokenTable;
  }

  public String getClientID() {
    return clientID;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getAccessToken() {
    if (tokenTable == null) {
      return null;
    }
    return tokenTable.getAccessToken();
  }

  public String getRefreshToken() {
    if (tokenTable == null) {
      return null;
    }
    return tokenTable.getRefreshToken();
  }

  public String getTokenUri() {
    return tokenURI;
  }
}
