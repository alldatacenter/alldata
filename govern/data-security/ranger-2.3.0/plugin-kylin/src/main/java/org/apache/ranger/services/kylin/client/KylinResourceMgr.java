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

package org.apache.ranger.services.kylin.client;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KylinResourceMgr {

	public static final String PROJECT = "project";

	private static final Logger LOG = LoggerFactory.getLogger(KylinResourceMgr.class);

	public static Map<String, Object> validateConfig(String serviceName, Map<String, String> configs) throws Exception {
		Map<String, Object> ret = null;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> KylinResourceMgr.validateConfig ServiceName: " + serviceName + "Configs" + configs);
		}

		try {
			ret = KylinClient.connectionTest(serviceName, configs);
		} catch (Exception e) {
			LOG.error("<== KylinResourceMgr.validateConfig Error: " + e);
			throw e;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== KylinResourceMgr.validateConfig Result: " + ret);
		}
		return ret;
	}

	public static List<String> getKylinResources(String serviceName, Map<String, String> configs,
			ResourceLookupContext context) {
		String userInput = context.getUserInput();
		String resource = context.getResourceName();
		Map<String, List<String>> resourceMap = context.getResources();
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> KylinResourceMgr.getKylinResources()  userInput: " + userInput + ", resource: " + resource
					+ ", resourceMap: " + resourceMap);
		}

		if (MapUtils.isEmpty(configs)) {
			LOG.error("Connection Config is empty!");
			return null;
		}

		if (StringUtils.isEmpty(userInput)) {
			LOG.warn("User input is empty, set default value : *");
			userInput = "*";
		}

		List<String> projectList = null;
		if (MapUtils.isNotEmpty(resourceMap)) {
			projectList = resourceMap.get(PROJECT);
		}

		final KylinClient kylinClient = KylinClient.getKylinClient(serviceName, configs);
		if (kylinClient == null) {
			LOG.error("Failed to getKylinClient!");
			return null;
		}

		List<String> resultList = null;

		resultList = kylinClient.getProjectList(userInput, projectList);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== KylinResourceMgr.getKylinResources() result: " + resultList);
		}
		return resultList;
	}

}
