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

package org.apache.ambari.server.security.authorization;


import org.springframework.security.core.AuthenticationException;

/**
 * This exception signals that duplicate user entries were found in LDAP during authentication.
 * The filter used to match user entry in LDAP that corresponds to the user being authenticated
 * should be refined to match only one entry.
 */
public class DuplicateLdapUserFoundAuthenticationException extends AuthenticationException {

  /**
   * Constructs an {@code DuplicateLdapUserFoundAuthenticationException} with the specified message.
   *
   * @param msg the detail message
   */
  public DuplicateLdapUserFoundAuthenticationException(String msg) {
    super(msg);
  }

  /**
   * Constructs an {@code DuplicateLdapUserFoundAuthenticationException} with the specified message and root cause.
   *
   * @param msg the detail message
   * @param t   the root cause
   */
  public DuplicateLdapUserFoundAuthenticationException(String msg, Throwable t) {
    super(msg, t);
  }


}
