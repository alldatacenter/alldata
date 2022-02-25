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

package org.apache.ambari.server.controller.utilities;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.google.inject.Singleton;

/**
 * LoginContextHelper is a helper class used for helping to use and manage {@link LoginContext} instances.
 */
@Singleton
public class LoginContextHelper {

  /**
   * Create a new {@link LoginContext}
   *
   * @param krb5ModuleEntryName the relevant com.sun.security.auth.module.Krb5LoginModule entry name
   * @param callbackHandler     a callback handler
   * @return a new {@link LoginContext}
   * @throws LoginException see {@link LoginContext#LoginContext(String, CallbackHandler)}
   */
  public LoginContext createLoginContext(String krb5ModuleEntryName, CallbackHandler callbackHandler)
      throws LoginException {
    return new LoginContext(krb5ModuleEntryName, callbackHandler);
  }

  /**
   * Create a new {@link LoginContext}
   *
   * @param krb5ModuleEntryName the relevant com.sun.security.auth.module.Krb5LoginModule entry name
   * @return a new {@link LoginContext}
   * @throws LoginException see {@link LoginContext#LoginContext(String, CallbackHandler)}
   */
  public LoginContext createLoginContext(String krb5ModuleEntryName) throws LoginException {
    return new LoginContext(krb5ModuleEntryName);
  }
}
