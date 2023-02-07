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
package org.apache.drill.exec.rpc.user.security;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.exception.DrillbitStartupException;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface to provide various username/password based implementations for authentication.
 */
public interface UserAuthenticator extends Closeable {

  /**
   * Setup for authenticating user credentials.
   */
  public void setup(DrillConfig drillConfig) throws DrillbitStartupException;

  /**
   * Authenticate the given <i>user</i> and <i>password</i> combination.
   *
   * @param user
   * @param password
   * @throws UserAuthenticationException if authentication fails for given user and password.
   */
  public void authenticate(String user, String password) throws UserAuthenticationException;

  /**
   * Close the authenticator. Used to release resources. Ex. LDAP authenticator opens connections to LDAP server,
   * such connections resources are released in a safe manner as part of close.
   *
   * @throws IOException
   */
  @Override
  void close() throws IOException;
}
