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

public interface Tokens {
  /**
   * Key of {@link this} tokens table.
   */
  String getKey();

  /**
   * Gets the current access token.
   *
   * @return The current access token
   */
  String getAccessToken();

  /**
   * Sets the access token.
   *
   * @param accessToken Sets the access token.
   */
  void setAccessToken(String accessToken);

  String getRefreshToken();

  void setRefreshToken(String refreshToken);

  /**
   * Returns value from tokens table that corresponds to provided plugin.
   *
   * @param token token of the value to obtain
   * @return value from token table that corresponds to provided plugin
   */
  String get(String token);

  /**
   * Associates provided token with provided plugin in token table.
   *
   * @param token   Token of the value to associate with
   * @param value   Value that will be associated with provided alias
   * @param replace Whether existing value for the same token should be replaced
   * @return {@code true} if provided token was associated with
   * the provided value in tokens table
   */
  boolean put(String token, String value, boolean replace);

  /**
   * Removes value for specified token from tokens table.
   * @param token token of the value to remove
   * @return {@code true} if the value associated with
   * provided token was removed from the tokens table.
   */
  boolean remove(String token);
}
