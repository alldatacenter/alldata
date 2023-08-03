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

 package org.apache.ranger.common;

import org.apache.ranger.security.context.RangerAdminOpContext;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;

public class ContextUtil {

	/**
	 * Singleton class
	 */
        public ContextUtil() {
	}

	public static Long getCurrentUserId() {
		RangerSecurityContext context = RangerContextHolder.getSecurityContext();
		if (context != null) {
			UserSessionBase userSession = context.getUserSession();
			if (userSession != null) {
				return userSession.getUserId();
			}
		}
		return null;
	}

	public static String getCurrentUserPublicName() {
		RangerSecurityContext context = RangerContextHolder.getSecurityContext();
		if (context != null) {
			UserSessionBase userSession = context.getUserSession();
			if (userSession != null) {
				return userSession.getXXPortalUser().getPublicScreenName();
				// return userSession.getGjUser().getPublicScreenName();
			}
		}
		return null;
	}

	public static UserSessionBase getCurrentUserSession() {
		UserSessionBase userSession = null;
		RangerSecurityContext context = RangerContextHolder.getSecurityContext();
		if (context != null) {
			userSession = context.getUserSession();
		}
		return userSession;
	}

	public static RequestContext getCurrentRequestContext() {
		RangerSecurityContext context = RangerContextHolder.getSecurityContext();
		if (context != null) {
			return context.getRequestContext();
		}
		return null;
	}

	public static String getCurrentUserLoginId() {
		RangerSecurityContext context = RangerContextHolder.getSecurityContext();
		if (context != null) {
			UserSessionBase userSession = context.getUserSession();
			if (userSession != null) {
				return userSession.getLoginId();
			}
		}
		return null;
	}

	public static boolean isBulkModeContext() {
		RangerAdminOpContext context = RangerContextHolder.getOpContext();
		boolean bulkMode = false;
		if (context != null) {
			bulkMode = context.isBulkModeContext();
		}
		return bulkMode;
	}

}
