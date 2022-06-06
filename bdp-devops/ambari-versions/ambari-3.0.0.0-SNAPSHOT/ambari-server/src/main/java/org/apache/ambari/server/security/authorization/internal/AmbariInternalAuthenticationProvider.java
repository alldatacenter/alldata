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

import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.google.inject.Inject;

public class AmbariInternalAuthenticationProvider implements AuthenticationProvider {

  private final InternalTokenStorage internalTokenStorage;

  @Inject
  public AmbariInternalAuthenticationProvider(InternalTokenStorage internalTokenStorage) {
    this.internalTokenStorage = internalTokenStorage;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    InternalAuthenticationToken token = (InternalAuthenticationToken) authentication;
    if (internalTokenStorage.isValidInternalToken(token.getCredentials())) {
      token.setAuthenticated(true);
    } else {
      throw new InvalidUsernamePasswordCombinationException(null);
    }
    return token;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return InternalAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
