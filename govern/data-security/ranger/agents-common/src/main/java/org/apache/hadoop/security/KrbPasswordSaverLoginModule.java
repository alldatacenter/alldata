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

 package org.apache.hadoop.security;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class KrbPasswordSaverLoginModule implements LoginModule {
    public static final String USERNAME_PARAM = "javax.security.auth.login.name";
    public static final String PASSWORD_PARAM = "javax.security.auth.login.password";

	@SuppressWarnings("rawtypes")
	private Map sharedState;
	
	public KrbPasswordSaverLoginModule() {
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackhandler, Map<String, ?> sharedMap, Map<String, ?> options) {
		
		this.sharedState = sharedMap;
		
		String userName = (options != null) ? (String)options.get(USERNAME_PARAM) : null;
		if (userName != null) {
			this.sharedState.put(USERNAME_PARAM,userName);
		}
		String password = (options != null) ? (String)options.get(PASSWORD_PARAM) : null;
		
		if (password != null) {
			this.sharedState.put(PASSWORD_PARAM,password.toCharArray());
		}
	}

	@Override
	public boolean login() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		return true;
	}

}
