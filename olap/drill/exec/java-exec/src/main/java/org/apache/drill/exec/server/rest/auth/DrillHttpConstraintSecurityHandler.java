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
package org.apache.drill.exec.server.rest.auth;

import org.apache.drill.exec.rpc.security.plain.PlainFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.server.DrillbitContext;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;

import java.util.Collections;
import java.util.Set;

import static org.apache.drill.exec.server.rest.auth.DrillUserPrincipal.ADMIN_ROLE;
import static org.apache.drill.exec.server.rest.auth.DrillUserPrincipal.AUTHENTICATED_ROLE;

/**
 * Accessor class that extends the {@link ConstraintSecurityHandler} to expose
 * protected method's for start and stop of Handler. This is needed since now
 * {@code DrillHttpSecurityHandlerProvider} composes of 2 security handlers -
 * For FORM and SPNEGO and has responsibility to start/stop of those handlers.
 **/
public abstract class DrillHttpConstraintSecurityHandler extends ConstraintSecurityHandler {

  @Override
  public void doStart() throws Exception {
    super.doStart();
  }

  @Override
  public void doStop() throws Exception {
    super.doStop();
  }

  public abstract void doSetup(DrillbitContext dbContext) throws DrillException;

  public void setup(LoginAuthenticator authenticator, LoginService loginService) {
    final Set<String> knownRoles = ImmutableSet.of(AUTHENTICATED_ROLE, ADMIN_ROLE);
    setConstraintMappings(Collections.<ConstraintMapping>emptyList(), knownRoles);
    setAuthenticator(authenticator);
    setLoginService(loginService);
  }

  protected void requireAuthProvider(DrillbitContext dbContext, String name) throws DrillException {
    // Check if PAMAuthenticator is available or not which is required for FORM authentication
    if (!dbContext.getAuthProvider().containsFactory(PlainFactory.SIMPLE_NAME)) {
      throw new DrillException(String.format("%1$s auth mechanism was configured but %2$s mechanism is not enabled to provide an " +
        "authenticator. Please configure user authentication with %2$s mechanism and authenticator to use " +
        "%1$s authentication", getImplName(), name));
    }
  }

  public abstract String getImplName();
}
