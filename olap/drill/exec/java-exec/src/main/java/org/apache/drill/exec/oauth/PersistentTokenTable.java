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
package org.apache.drill.exec.oauth;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.exec.store.sys.PersistentStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of tokens table that updates its version in persistent store after modifications.
 * For OAuth tokens, the only possible tokens are the access_token, the refresh_token and authorization_code.
 */
public class PersistentTokenTable implements Tokens {
  public final String ACCESS_TOKEN_KEY = "access_token";
  public final String REFRESH_TOKEN_KEY = "refresh_token";

  private final Map<String, String> tokens;

  private final String key;

  private final PersistentStore<PersistentTokenTable> store;

  @JsonCreator
  public PersistentTokenTable(
    @JsonProperty("tokens") Map<String, String> tokens,
    @JsonProperty("key") String key,
    @JacksonInject PersistentTokenRegistry.StoreProvider storeProvider) {
    this.tokens = tokens != null ? tokens : new HashMap<>();
    this.key = key;
    this.store = storeProvider.getStore();
  }

  @Override
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  @Override
  @JsonIgnore
  public String get(String token) {
    return tokens.get(token);
  }

  @Override
  @JsonIgnore
  public boolean put(String token, String value, boolean replace) {
    if (replace || ! tokens.containsKey(token)) {
      tokens.put(token, value);
      store.put(key, this);
      return true;
    }
    return false;
  }

  @Override
  @JsonIgnore
  public String getAccessToken() {
    return get(ACCESS_TOKEN_KEY);
  }

  @Override
  @JsonIgnore
  public String getRefreshToken() {
    return get(REFRESH_TOKEN_KEY);
  }

  @Override
  @JsonIgnore
  public void setAccessToken(String token) {
    // Only update the access token if it is not the same as the previous token
    if (!tokens.containsKey(ACCESS_TOKEN_KEY) || !token.equals(getAccessToken())) {
      put(ACCESS_TOKEN_KEY, token, true);
    }
  }

  @Override
  @JsonIgnore
  public void setRefreshToken(String token) {
    // Only update the access token if it is not the same as the previous token
    if (!tokens.containsKey(REFRESH_TOKEN_KEY) || !getAccessToken().equals(token)) {
      put(REFRESH_TOKEN_KEY, token,true);
    }
  }

  @Override
  @JsonIgnore
  public boolean remove(String token) {
    boolean isRemoved = tokens.remove(token) != null;
    store.put(key, this);
    return isRemoved;
  }

  @JsonProperty("tokens")
  public Map<String, String> getTokens() {
    return tokens;
  }
}
