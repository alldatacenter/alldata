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

import org.apache.drill.exec.server.rest.header.ResponseHeadersSettingFilter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.rpc.security.AuthStringUtil;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.rest.WebServerConstants;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.security.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class DrillHttpSecurityHandlerProvider extends ConstraintSecurityHandler {
  private static final Logger logger = LoggerFactory.getLogger(DrillHttpSecurityHandlerProvider.class);

  private final Map<String, DrillHttpConstraintSecurityHandler> securityHandlers =
      CaseInsensitiveMap.newHashMapWithExpectedSize(2);

  private final Map<String, String> responseHeaders;

  @SuppressWarnings("unchecked")
  public DrillHttpSecurityHandlerProvider(DrillConfig config, DrillbitContext drillContext)
      throws DrillbitStartupException {

    Preconditions.checkState(config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED));
    this.responseHeaders = ResponseHeadersSettingFilter.retrieveResponseHeaders(config);
    final Set<String> configuredMechanisms = getHttpAuthMechanisms(config);

    final ScanResult scan = drillContext.getClasspathScan();
    final Collection<Class<? extends DrillHttpConstraintSecurityHandler>> factoryImpls =
        scan.getImplementations(DrillHttpConstraintSecurityHandler.class);
    logger.debug("Found DrillHttpConstraintSecurityHandler implementations: {}", factoryImpls);

    for (final Class<? extends DrillHttpConstraintSecurityHandler> clazz : factoryImpls) {

      // If all the configured mechanisms handler is added then break out of this loop
      if (configuredMechanisms.isEmpty()) {
        break;
      }

      Constructor<? extends DrillHttpConstraintSecurityHandler> validConstructor = null;
      for (final Constructor<?> c : clazz.getConstructors()) {
        final Class<?>[] params = c.getParameterTypes();
        if (params.length == 0) {
          validConstructor = (Constructor<? extends DrillHttpConstraintSecurityHandler>) c; // unchecked
          break;
        }
      }

      if (validConstructor == null) {
        logger.warn("Skipping DrillHttpConstraintSecurityHandler class {}. It must implement at least one" +
            " constructor with signature [{}()]", clazz.getCanonicalName(), clazz.getName());
        continue;
      }

      try {
        final DrillHttpConstraintSecurityHandler instance = validConstructor.newInstance();
        if (configuredMechanisms.remove(instance.getImplName())) {
          instance.doSetup(drillContext);
          securityHandlers.put(instance.getImplName(), instance);
        }
      } catch (IllegalArgumentException | ReflectiveOperationException | DrillException e) {
        logger.warn(String.format("Failed to create DrillHttpConstraintSecurityHandler of type '%s'",
            clazz.getCanonicalName()), e);
      }
    }

    if (securityHandlers.size() == 0) {
      throw new DrillbitStartupException(
          "Authentication is enabled for WebServer but none of the security mechanism " +
          "was configured properly. Please verify the configurations and try again.");
    }

    logger.info("Configure auth mechanisms for WebServer are: {}", securityHandlers.keySet());
  }

  @Override
  public void doStart() throws Exception {
    super.doStart();
    for (DrillHttpConstraintSecurityHandler securityHandler : securityHandlers.values()) {
      securityHandler.doStart();
    }
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    Preconditions.checkState(securityHandlers.size() > 0);
    responseHeaders.forEach(response::setHeader);
    HttpSession session = request.getSession(true);
    SessionAuthentication authentication =
        (SessionAuthentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
    String uri = request.getRequestURI();
    final DrillHttpConstraintSecurityHandler securityHandler;

    // Before authentication, all requests go through the FormAuthenticator if configured except for /spnegoLogin
    // request. For SPNEGO authentication all requests will be forced going via /spnegoLogin before authentication is
    // done, this is to ensure that we don't have to authenticate same client session multiple times for each resource.
    //
    // If this authentication is null, user hasn't logged in yet
    if (authentication == null) {

      // 1) If only SPNEGOSecurity handler then use SPNEGOSecurity
      // 2) If both but uri equals spnegoLogin then use SPNEGOSecurity
      // 3) If both but uri doesn't equals spnegoLogin then use FORMSecurity
      // 4) If only FORMSecurity handler then use FORMSecurity
      if (isSpnegoEnabled() && (!isFormEnabled() || uri.equals(WebServerConstants.SPENGO_LOGIN_RESOURCE_PATH))) {
        securityHandler = securityHandlers.get(Constraint.__SPNEGO_AUTH);
        securityHandler.handle(target, baseRequest, request, response);
      } else if(isBasicEnabled() && request.getHeader(HttpHeader.AUTHORIZATION.asString()) != null) {
        securityHandler = securityHandlers.get(Constraint.__BASIC_AUTH);
        securityHandler.handle(target, baseRequest, request, response);
      } else if (isFormEnabled()) {
        securityHandler = securityHandlers.get(Constraint.__FORM_AUTH);
        securityHandler.handle(target, baseRequest, request, response);
      }

    }
    // If user has logged in, use the corresponding handler to handle the request
    else {
      final String authMethod = authentication.getAuthMethod();
      securityHandler = securityHandlers.get(authMethod);
      securityHandler.handle(target, baseRequest, request, response);
    }
  }

  @Override
  public void setHandler(Handler handler) {
    super.setHandler(handler);
    for (DrillHttpConstraintSecurityHandler securityHandler : securityHandlers.values()) {
      securityHandler.setHandler(handler);
    }
  }

  @Override
  public void doStop() throws Exception {
    super.doStop();
    for (DrillHttpConstraintSecurityHandler securityHandler : securityHandlers.values()) {
      securityHandler.doStop();
    }
  }

  public boolean isSpnegoEnabled() {
    return securityHandlers.containsKey(Constraint.__SPNEGO_AUTH);
  }

  public boolean isFormEnabled() {
    return securityHandlers.containsKey(Constraint.__FORM_AUTH);
  }

  public boolean isBasicEnabled() {
    return securityHandlers.containsKey(Constraint.__BASIC_AUTH);
  }

  /**
   * Returns a list of configured mechanisms for HTTP authentication. For backward
   * compatibility if authentication is enabled it will include FORM mechanism by default.
   * @param config - {@link DrillConfig}
   * @return
   */
  public static Set<String> getHttpAuthMechanisms(DrillConfig config) {
    final Set<String> configuredMechs = new HashSet<>();
    final boolean authEnabled = config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED);

    if (authEnabled) {
      if (config.hasPath(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS)) {
        configuredMechs.addAll(
            AuthStringUtil.asSet(config.getStringList(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS)));
      } else {
        // For backward compatibility
        configuredMechs.add(Constraint.__FORM_AUTH);
      }
    }
    return configuredMechs;
  }
}
