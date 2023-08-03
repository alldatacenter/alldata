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

package org.apache.ranger.services.sqoop.client;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqoopResourceMgr {

	public static final String CONNECTOR = "connector";
	public static final String LINK = "link";
	public static final String JOB = "job";

	private static final Logger LOG = LoggerFactory.getLogger(SqoopResourceMgr.class);

	public static Map<String, Object> validateConfig(String serviceName, Map<String, String> configs) throws Exception {
		Map<String, Object> ret = null;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> SqoopResourceMgr.validateConfig ServiceName: " + serviceName + "Configs" + configs);
		}

		try {
			ret = SqoopClient.connectionTest(serviceName, configs);
		} catch (Exception e) {
			LOG.error("<== SqoopResourceMgr.validateConfig Error: " + e);
			throw e;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== SqoopResourceMgr.validateConfig Result: " + ret);
		}
		return ret;
	}

	public static List<String> getSqoopResources(String serviceName, Map<String, String> configs,
			ResourceLookupContext context) {
		String userInput = context.getUserInput();
		String resource = context.getResourceName();
		Map<String, List<String>> resourceMap = context.getResources();
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> SqoopResourceMgr.getSqoopResources()  userInput: " + userInput + ", resource: " + resource
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

		final SqoopClient sqoopClient = SqoopClient.getSqoopClient(serviceName, configs);
		if (sqoopClient == null) {
			LOG.error("Failed to getSqoopResources!");
			return null;
		}

		List<String> resultList = null;

		if (StringUtils.isNotEmpty(resource)) {
			switch (resource) {
			case CONNECTOR:
				List<String> existingConnectors = resourceMap.get(CONNECTOR);
				resultList = sqoopClient.getConnectorList(userInput, existingConnectors);
				break;
			case LINK:
				List<String> existingLinks = resourceMap.get(LINK);
				resultList = sqoopClient.getLinkList(userInput, existingLinks);
				break;
			case JOB:
				List<String> existingJobs = resourceMap.get(JOB);
				resultList = sqoopClient.getJobList(userInput, existingJobs);
				break;
			default:
				break;
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== SqoopResourceMgr.getSqoopResources() result: " + resultList);
		}
		return resultList;
	}

}
