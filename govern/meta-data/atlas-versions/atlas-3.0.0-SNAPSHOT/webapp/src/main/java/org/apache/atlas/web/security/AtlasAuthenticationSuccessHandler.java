/**
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

package org.apache.atlas.web.security;

import org.apache.atlas.AtlasConfiguration;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
public class AtlasAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static Logger LOG = LoggerFactory.getLogger(AuthenticationSuccessHandler.class);
    private int sessionTimeout = 3600;
    public static final String LOCALLOGIN = "locallogin";

    @PostConstruct
    public void setup() {
        sessionTimeout = AtlasConfiguration.SESSION_TIMEOUT_SECS.getInt();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        LOG.debug("Login Success " + authentication.getPrincipal());

        JSONObject json = new JSONObject();
        json.put("msgDesc", "Success");

        if (request.getSession() != null) { // incase of form based login mark it as local login in session
            request.getSession().setAttribute(LOCALLOGIN,"true");
            request.getServletContext().setAttribute(request.getSession().getId(), LOCALLOGIN);

            if (this.sessionTimeout != -1) {
                request.getSession().setMaxInactiveInterval(sessionTimeout);
            }
        }
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toJSONString());
    }
}
