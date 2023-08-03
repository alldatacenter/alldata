/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.elasticsearch.plugin.authc.user;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

public class UsernamePasswordToken {

	public static final String USERNAME = "username";

	public static final String BASIC_AUTH_PREFIX = "Basic ";

	public static final String BASIC_AUTH_HEADER = "Authorization";

	private String username;

	private String password;

	public UsernamePasswordToken(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public static UsernamePasswordToken parseToken(RestRequest request) {

		Map<String, List<String>> headers = request.getHeaders();
		if (MapUtils.isEmpty(headers)) {
			return null;
		}
		List<String> authStrs = headers.get(BASIC_AUTH_HEADER);
		if (CollectionUtils.isEmpty(authStrs)) {
			return null;
		}

		String authStr = authStrs.get(0);
		if (StringUtils.isEmpty(authStr)) {
			return null;
		}

		String userPass = "";
		try {
			userPass = new String(Base64.getUrlDecoder().decode(authStr.substring(BASIC_AUTH_PREFIX.length())));
		} catch (IllegalArgumentException e) {
			throw new ElasticsearchStatusException("Error: Failed to parse user authentication.",
					RestStatus.UNAUTHORIZED, e);
		}

		int i = StringUtils.indexOf(userPass, ':');
		if (i <= 0) {
			throw new ElasticsearchStatusException(
					"Error: Parse user authentication to get the wrong userPass[{}].",
					RestStatus.UNAUTHORIZED, userPass);
		}
		return new UsernamePasswordToken(StringUtils.substring(userPass, 0, i),
				StringUtils.substring(userPass, i + 1, userPass.length()));

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "UsernamePasswordToken [username=" + username + ", password=" + "******" + "]";
	}
}
