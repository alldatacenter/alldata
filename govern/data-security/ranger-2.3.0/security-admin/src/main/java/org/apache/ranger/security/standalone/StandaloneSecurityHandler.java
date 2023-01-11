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

 /**
 *
 */
package org.apache.ranger.security.standalone;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ranger.biz.SessionMgr;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.entity.XXAuthSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class StandaloneSecurityHandler {
	public static final String AUTH_MANAGER_BEAN_NAME = "authenticationManager";
	public static final String ACCESS_DECISION_MANAGER_BEAN_NAME = "customAccessDecisionManager";

	@Autowired
	SessionMgr sessionMgr;

	public void login(String userName, String password,
			ApplicationContext context) throws Exception {
		// [1] Create AUTH Token
		Authentication token = new UsernamePasswordAuthenticationToken(
				userName, password);

		// [2] Authenticate User
		AuthenticationManager am = (AuthenticationManager) context
				.getBean(AUTH_MANAGER_BEAN_NAME);
		token = am.authenticate(token);

		// [3] Check User Access
		AffirmativeBased accessDecisionManager = (AffirmativeBased) context
				.getBean(ACCESS_DECISION_MANAGER_BEAN_NAME);
		Collection<ConfigAttribute> list = new ArrayList<ConfigAttribute>();
		SecurityConfig config = new SecurityConfig(RangerConstants.ROLE_SYS_ADMIN);
		list.add(config);
		accessDecisionManager.decide(token, null, list);

		// [4] set token in spring context
		SecurityContextHolder.getContext().setAuthentication(token);

		// [5] Process Success login
		InetAddress thisIp = InetAddress.getLocalHost();
		sessionMgr.processStandaloneSuccessLogin(
				XXAuthSession.AUTH_TYPE_PASSWORD, thisIp.getHostAddress());
	}
}
