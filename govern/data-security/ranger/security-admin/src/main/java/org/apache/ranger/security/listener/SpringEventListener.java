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

 package org.apache.ranger.security.listener;

import java.util.Calendar;
import org.apache.ranger.biz.SessionMgr;
import org.apache.ranger.entity.XXAuthSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;


public class SpringEventListener implements
	ApplicationListener<AbstractAuthenticationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SpringEventListener.class);

    @Autowired
    SessionMgr sessionMgr;

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
	try {
	    if (event instanceof AuthenticationSuccessEvent) {
		process((AuthenticationSuccessEvent) event);
	    } else if (event instanceof AuthenticationFailureBadCredentialsEvent) {
		process((AuthenticationFailureBadCredentialsEvent) event);
	    } else if (event instanceof AuthenticationFailureLockedEvent) {
		process((AuthenticationFailureLockedEvent) event);
	    } else if (event instanceof AuthenticationFailureDisabledEvent) {
		process((AuthenticationFailureDisabledEvent) event);
	    }
	    // igonre all other events

	} catch (Exception e) {
	    logger.error("Exception in Spring Event Listener.", e);
	}
    }

    protected void process(AuthenticationSuccessEvent authSuccessEvent) {
	Authentication auth = authSuccessEvent.getAuthentication();
	WebAuthenticationDetails details = (WebAuthenticationDetails) auth
		.getDetails();
	String remoteAddress = details != null ? details.getRemoteAddress()
		: "";
	String sessionId = details != null ? details.getSessionId() : "";

	Calendar cal = Calendar.getInstance();
	logger.info("Login Successful:" + auth.getName() + " | Ip Address:"
			+ remoteAddress + " | sessionId=" + sessionId +  " | Epoch=" +cal.getTimeInMillis() );

	// success logins are processed further in
	// AKASecurityContextFormationFilter
    }

    protected void process(
	    AuthenticationFailureBadCredentialsEvent authFailEvent) {
	Authentication auth = authFailEvent.getAuthentication();
	WebAuthenticationDetails details = (WebAuthenticationDetails) auth
		.getDetails();
	String remoteAddress = details != null ? details.getRemoteAddress()
		: "";
	String sessionId = details != null ? details.getSessionId() : "";

	logger.info("Login Unsuccessful:" + auth.getName() + " | Ip Address:"
		+ remoteAddress + " | Bad Credentials");

	sessionMgr.processFailureLogin(
		XXAuthSession.AUTH_STATUS_WRONG_PASSWORD,
		XXAuthSession.AUTH_TYPE_PASSWORD, auth.getName(),
		remoteAddress, sessionId);
    }

    protected void process(AuthenticationFailureLockedEvent authFailEvent) {
		Authentication           auth          = authFailEvent.getAuthentication();
		WebAuthenticationDetails details       = (WebAuthenticationDetails) auth.getDetails();
		String                   remoteAddress = details != null ? details.getRemoteAddress() : "";
		String                   sessionId     = details != null ? details.getSessionId() : "";

		logger.info("Login Unsuccessful:" + auth.getName() + " | Ip Address:" + remoteAddress + " | User account is locked");

		sessionMgr.processFailureLogin(XXAuthSession.AUTH_STATUS_LOCKED, XXAuthSession.AUTH_TYPE_PASSWORD, auth.getName(), remoteAddress, sessionId);
	}

    protected void process(AuthenticationFailureDisabledEvent authFailEvent) {
	Authentication auth = authFailEvent.getAuthentication();
	WebAuthenticationDetails details = (WebAuthenticationDetails) auth
		.getDetails();
	String remoteAddress = details != null ? details.getRemoteAddress()
		: "";
	String sessionId = details != null ? details.getSessionId() : "";

	logger.info("Login Unsuccessful:" + auth.getName() + " | Ip Address:"
		+ remoteAddress + " | User Disabled");

	sessionMgr.processFailureLogin(XXAuthSession.AUTH_STATUS_DISABLED,
		XXAuthSession.AUTH_TYPE_PASSWORD, auth.getName(),
		remoteAddress, sessionId);

    }

}
