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

/**
 * AmbariProxyUserDetailsImpl is a general implementation of a {@link AmbariProxyUserDetails}.
 */
public class AmbariProxyUserDetailsImpl implements AmbariProxyUserDetails {
  private final String username;
  private final UserAuthenticationType authenticationType;

  /**
   * Constructor
   *
   * @param username           the local username
   * @param authenticationType the {@link UserAuthenticationType}
   */
  public AmbariProxyUserDetailsImpl(String username, UserAuthenticationType authenticationType) {
    this.username = username;
    this.authenticationType = authenticationType;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public UserAuthenticationType getAuthenticationType() {
    return authenticationType;
  }
}
