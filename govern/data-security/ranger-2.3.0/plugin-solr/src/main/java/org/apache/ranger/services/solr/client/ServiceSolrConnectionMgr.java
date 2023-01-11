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

package org.apache.ranger.services.solr.client;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class ServiceSolrConnectionMgr {

	private static final String SOLR_ZOOKEEPER_QUORUM = "solr.zookeeper.quorum";
	private static final String SOLR_URL = "solr.url";

	static public ServiceSolrClient getSolrClient(String serviceName,
			Map<String, String> configs) throws Exception {
		String solr_url = configs.get(SOLR_URL);
		String zk_url = configs.get(SOLR_ZOOKEEPER_QUORUM);
		if (solr_url != null || zk_url != null) {
			final boolean isSolrCloud = StringUtils.isNotEmpty(zk_url);
			final String url = isSolrCloud ? zk_url : solr_url;
			ServiceSolrClient serviceSolrClient = new ServiceSolrClient(serviceName, configs, url, isSolrCloud);
			return serviceSolrClient;
		}
		throw new Exception("Required properties are not set for "
				+ serviceName + ". URL or Zookeeper information not provided.");
	}

	/**
	 * @param serviceName
	 * @param configs
	 * @return
	 */
	public static Map<String, Object> connectionTest(String serviceName,
			Map<String, String> configs) throws Exception {
		ServiceSolrClient serviceSolrClient = getSolrClient(serviceName,
				configs);
		return serviceSolrClient.connectionTest();
	}

}
