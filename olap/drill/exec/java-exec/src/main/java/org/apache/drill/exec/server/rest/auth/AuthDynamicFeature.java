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

import org.apache.drill.exec.server.rest.WebServerConstants;
import org.glassfish.jersey.server.model.AnnotatedMethod;

import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URLEncoder;

/**
 * Implementation of {@link DynamicFeature}. As part of the setup it adds the auth check filter {@link AuthCheckFilter}
 * for resources that need to have user authenticated. If authentication is not done, request is forwarded to login
 * page.
 */
public class AuthDynamicFeature implements DynamicFeature {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthDynamicFeature.class);

  @Override
  public void configure(final ResourceInfo resourceInfo, final FeatureContext configuration) {
    AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

    // RolesAllowed on the method takes precedence over PermitAll
    RolesAllowed ra = am.getAnnotation(RolesAllowed.class);
    if (ra != null) {
      configuration.register(AuthCheckFilter.INSTANCE);
      return;
    }

    // PermitAll takes precedence over RolesAllowed on the class
    // This avoids putting AuthCheckFilter in the request flow for all path's which
    // are defined under PermitAll annotation. That is requests for "/", "/login", "/mainLogin" and "/spnegoLogin"
    // path's doesn't go through AuthCheckFilter.
    if (am.isAnnotationPresent(PermitAll.class)) {
      // Do nothing.
      return;
    }

    // RolesAllowed on the class takes precedence over PermitAll
    ra = resourceInfo.getResourceClass().getAnnotation(RolesAllowed.class);
    if (ra != null) {
      configuration.register(AuthCheckFilter.INSTANCE);
    }
  }

  @Priority(Priorities.AUTHENTICATION) // authentication filter - should go first before all other filters.
  private static class AuthCheckFilter implements ContainerRequestFilter {
    private static AuthCheckFilter INSTANCE = new AuthCheckFilter();

    @Override
    public void filter(ContainerRequestContext requestContext) {
      final SecurityContext sc = requestContext.getSecurityContext();
      if (!isUserLoggedIn(sc)) {
        try {
          final String destResource =
              URLEncoder.encode(requestContext.getUriInfo().getRequestUri().getPath(), "UTF-8");
          final URI loginURI = requestContext.getUriInfo().getBaseUriBuilder()
              .path(WebServerConstants.MAIN_LOGIN_RESOURCE_NAME)
              .queryParam(WebServerConstants.REDIRECT_QUERY_PARM, destResource)
              .build();
          requestContext.abortWith(Response.temporaryRedirect(loginURI).build()
          );
        } catch (final Exception ex) {
          final String errMsg = String.format("Failed to forward the request to login page: %s", ex.getMessage());
          logger.error(errMsg, ex);
          requestContext.abortWith(
              Response.serverError()
                  .entity(errMsg)
                  .build());
        }
      }
    }
  }

  public static boolean isUserLoggedIn(final SecurityContext sc) {
    return sc != null && sc.getUserPrincipal() != null;
  }
}