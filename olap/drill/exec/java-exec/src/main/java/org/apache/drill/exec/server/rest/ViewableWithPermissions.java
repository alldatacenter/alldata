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
package org.apache.drill.exec.server.rest;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.exec.server.rest.auth.AuthDynamicFeature;
import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal;
import org.glassfish.jersey.server.mvc.Viewable;

import javax.ws.rs.core.SecurityContext;
import java.util.Map;

/**
 * Overrides {@link Viewable} to create a model which contains additional info of what control to display in menubar.
 */
public class ViewableWithPermissions extends Viewable {

  /**
   * Create the web page using the given template and {@link SecurityContext} after authentication is done.
   * @param templateName
   * @param sc
   * @return
   */
  public static Viewable create(final boolean authEnabled, final String templateName, final SecurityContext sc) {
    return new ViewableWithPermissions(authEnabled, templateName, sc, true, null);
  }

  /**
   * Create a web page using the given template, {@link SecurityContext} and model data.
   * @param templateName
   * @param sc
   * @param model
   * @return
   */
  public static Viewable create(final boolean authEnabled, final String templateName,
                                final SecurityContext sc, final Object model) {
    return new ViewableWithPermissions(authEnabled, templateName, sc, true, model);
  }

  /**
   * Create a login page.
   * @param errorMsg Optional error messages to be shown in case when the page is requested after login attempt failure.
   * @return
   */
  public static Viewable createLoginPage(final String errorMsg) {
    return new ViewableWithPermissions(true, "/rest/login.ftl", null, false, errorMsg);
  }

  public static Viewable createMainLoginPage(Object mainPageModel) {
    return new ViewableWithPermissions(true, "/rest/mainLogin.ftl", null, false, mainPageModel);
  }

  private ViewableWithPermissions(final boolean authEnabled, final String templateName,
                                  final SecurityContext sc, final boolean showControls,
                                  final Object model) throws IllegalArgumentException {
    super(templateName, createModel(authEnabled, sc, showControls, model));
  }

  private static Map<String, Object> createModel(final boolean authEnabled, final SecurityContext sc,
                                                 final boolean showControls, final Object pageModel) {

    final boolean isAdmin = !authEnabled /* when auth is disabled every user is an admin user */
        || (showControls && sc.isUserInRole(DrillUserPrincipal.ADMIN_ROLE));

    final boolean isUserLoggedIn = AuthDynamicFeature.isUserLoggedIn(sc);

    final ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.<String, Object>builder()
        .put("showStorage", isAdmin)
        .put("showOptions", isAdmin)
        .put("showThreads", isAdmin)
        .put("showLogs", isAdmin)
        .put("showLogin", authEnabled && showControls && !isUserLoggedIn)
        .put("showLogout", authEnabled && showControls && isUserLoggedIn)
        .put("loggedInUserName", authEnabled && showControls &&
            isUserLoggedIn ? sc.getUserPrincipal().getName()
                           : DrillUserPrincipal.ANONYMOUS_USER).put("showControls", showControls);

    if (pageModel != null) {
      mapBuilder.put("model", pageModel);
    }

    return mapBuilder.build();
  }
}
