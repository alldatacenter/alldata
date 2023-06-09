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
package org.apache.drill.yarn.appMaster.http;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal;
import org.apache.drill.yarn.appMaster.Dispatcher;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.glassfish.jersey.server.ResourceConfig;

import com.typesafe.config.Config;

/**
 * Base class for a tree of web pages (or REST resources) represented
 * as POJOs. Since the AM web UI is simple, this is the most convenient,
 * compact way to implement the UI.
 */

public class PageTree extends ResourceConfig {
  // These items are a bit clumsy. We need them, but we can't make them
  // instance variables without a bunch of messiness in the page classes.
  // So, we let them be static. No harm in setting them multiple times.

  static Dispatcher dispatcher;
  static Config config;

  public PageTree(Dispatcher dispatcher) {
    PageTree.dispatcher = dispatcher;
    config = DrillOnYarnConfig.config();
  }

  /**
   * Creates a FreeMarker model that contains two top-level items:
   * the model itself (as in the default implementation) and the
   * cluster name (used as a title on each UI page.)
   *
   * @param base
   * @return
   */

  public static Map<String, Object> toModel(SecurityContext sc, Object base) {
    return toModel(sc, base, null);
  }

  public static Map<String, Object> toModel(SecurityContext sc, Object base, HttpServletRequest request) {
    Map<String, Object> model = new HashMap<>();
    model.put("model", base);
    if (request != null) {
      model.put(WebConstants.CSRF_TOKEN, WebUtils.getCsrfTokenFromHttpRequest(request));
    }
    return toMapModel(sc, model);
  }

  public static Map<String, Object> toMapModel(SecurityContext sc,
      Map<String, Object> model) {
    model.put("clusterName", config.getString(DrillOnYarnConfig.APP_NAME));
    boolean useAuth = AMSecurityManagerImpl.isEnabled();
    final boolean isUserLoggedIn = (useAuth)
        ? AuthDynamicFeature.isUserLoggedIn(sc) : false;
    model.put("showLogin", useAuth && !isUserLoggedIn);
    model.put("showLogout", isUserLoggedIn);
    model.put("docsLink", config.getString(DrillOnYarnConfig.HTTP_DOCS_LINK));
    String userName = isUserLoggedIn ? sc.getUserPrincipal().getName()
        : DrillUserPrincipal.ANONYMOUS_USER;
    model.put("loggedInUserName", userName);
    return model;
  }
}
