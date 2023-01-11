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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.security.web.authentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.view.VXResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler
		implements LogoutSuccessHandler {

	@Autowired
	JSONUtil jsonUtil;

	@Override
	public void onLogoutSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {

		request.getServletContext().removeAttribute(request.getRequestedSessionId());
		
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("X-Frame-Options", "DENY");
		String jsonStr = "";
		try {
			VXResponse vXResponse = new VXResponse();
			vXResponse.setStatusCode(HttpServletResponse.SC_OK);
			vXResponse.setMsgDesc("Logout Successful");
			jsonStr = jsonUtil.writeObjectAsString(vXResponse);

			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(jsonStr);
			
			if (logger.isDebugEnabled()) {
				logger.debug("Log-out Successfully done. Returning Json : " +jsonStr);
			}
			
		} catch (IOException e) {
			logger.info("Error while writing JSON in HttpServletResponse");
		}
	}

}
