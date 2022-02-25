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

package org.apache.ambari.server.security.authorization.internal;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Generates single token for internal authentication
 */
@Singleton
public class InternalTokenStorage {
  private final SecureRandom random;
  private final String token;

  @Inject
  public InternalTokenStorage(SecureRandom secureRandom) {
    this.random = secureRandom;
    token = createNewToken();
  }

  public String getInternalToken() {
    return token;
  }

  public boolean isValidInternalToken(String token) {
    return this.token.equals(token);
  }

  public String createNewToken() {
    return new BigInteger(130, random).toString(32);
  }
}
