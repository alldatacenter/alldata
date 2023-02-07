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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

public class WebUtils {

  /**
   * Retrieves the CSRF protection token from the HTTP request.
   *
   * @param request HTTP request that contains a session that stores a CSRF protection token.
   *                If there is no session, that means that authentication is disabled.
   * @return CSRF protection token, or an empty string if there is no session present.
   */
  public static String getCsrfTokenFromHttpRequest(HttpServletRequest request) {
    // No need to create a session if not present (i.e. if a user is logged in)
    HttpSession session = request.getSession(false);
    return session == null ? "" : (String) session.getAttribute(WebConstants.CSRF_TOKEN);
  }

  /**
   * Generates a BASE64 encoded CSRF token from randomly generated 256-bit buffer
   * according to the <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html">OWASP CSRF Prevention Cheat Sheet</a>
   *
   * @return randomly generated CSRF token.
   */
  public static String generateCsrfToken() {
    byte[] buffer = new byte[32];
    new SecureRandom().nextBytes(buffer);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
  }
}
