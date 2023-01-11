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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 *
 */
@Component
public class RangerConfigUtil {
	private static final Logger logger = LoggerFactory.getLogger(RangerConfigUtil.class);

	String webappRootURL;
	int defaultMaxRows = 250;
	String[] roles;
	boolean accessFilterEnabled = true;
	boolean isModerationEnabled = false;
	boolean isUserPrefEnabled = false;

	public RangerConfigUtil() {

		webappRootURL = PropertiesUtil.getProperty("ranger.externalurl");
		if (webappRootURL == null || webappRootURL.trim().length() == 0) {
			logger.error("webapp URL is not set. Please ranger.externalurl property");
		}

		defaultMaxRows = PropertiesUtil.getIntProperty(
				"ranger.db.maxrows.default", defaultMaxRows);
		roles = PropertiesUtil.getPropertyStringList("ranger.users.roles.list");

		accessFilterEnabled = PropertiesUtil.getBooleanProperty("ranger.db.access.filter.enable", true);
		isModerationEnabled = PropertiesUtil.getBooleanProperty("ranger.moderation.enabled", isModerationEnabled);
		isUserPrefEnabled = PropertiesUtil.getBooleanProperty("ranger.userpref.enabled", isUserPrefEnabled);
	}	

	/**
	 * @return the defaultMaxRows
	 */
	public int getDefaultMaxRows() {
		return defaultMaxRows;
	}

	/**
	 * @return the roles
	 */
	public String[] getRoles() {
		return roles;
	}

	/**
	 * @return the accessFilterEnabled
	 */
	public boolean isAccessFilterEnabled() {
		return accessFilterEnabled;
	}

	/**
	 * @return the webAppRootURL
	 */
	public String getWebAppRootURL() {
		return webappRootURL;
	}

}
