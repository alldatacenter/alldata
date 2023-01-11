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

package org.apache.ranger.services.knox.client;

import java.util.Map;

import org.apache.ranger.plugin.model.RangerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class KnoxConnectionMgr {

	private static final Logger LOG = LoggerFactory.getLogger(KnoxConnectionMgr.class);
	
	public KnoxClient getKnoxClientbyService(RangerService service) {
		KnoxClient 		   knoxClient = null;
		Map<String,String> configs 	  = null;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Getting knoxClient for ServiceName: " + service.toString());
		}
		
		if (service != null) {
			configs = service.getConfigs();
			knoxClient = getKnoxClientByConfig(configs);
		}
		return knoxClient;
	}
	
	public KnoxClient getKnoxClientByConfig( final Map<String,String> configs) {
		KnoxClient knoxClient = null;
		if (configs == null) {
			LOG.error("Connection Config is empty");
				
		} else {
			
			String knoxUrl = configs.get("knox.url");
			String knoxAdminUser = configs.get("username");
			String knoxAdminPassword = configs.get("password");
			knoxClient =  new KnoxClient(knoxUrl, knoxAdminUser, knoxAdminPassword);
		}
		return knoxClient;
	}

	public KnoxClient getKnoxClient(String serviceName,
			Map<String, String> configs) {
		KnoxClient knoxClient = null;
		LOG.debug("Getting knoxClient for datasource: " + serviceName +
				"configMap: " + configs);
		if (configs == null) {
			LOG.error("Connection ConfigMap is empty");
		} else {
			String knoxUrl = configs.get("knox.url");
			String knoxAdminUser = configs.get("username");
			String knoxAdminPassword = configs.get("password");
			knoxClient =  new KnoxClient(knoxUrl, knoxAdminUser, knoxAdminPassword);
		}
		return knoxClient;
	}
	
	
	public KnoxClient getKnoxClient(final String knoxUrl, String knoxAdminUser, String knoxAdminPassword) {
		KnoxClient knoxClient = null;
		if (knoxUrl == null || knoxUrl.isEmpty()) {
			LOG.error("Can not create KnoxClient: knoxUrl is empty");
		} else if (knoxAdminUser == null || knoxAdminUser.isEmpty()) {
			LOG.error("Can not create KnoxClient: knoxAdminUser is empty");
		} else if (knoxAdminPassword == null || knoxAdminPassword.isEmpty()) {
			LOG.error("Can not create KnoxClient: knoxAdminPassword is empty");
		} else {
			knoxClient =  new KnoxClient(knoxUrl, knoxAdminUser, knoxAdminPassword);
		}
		return knoxClient;
	}	
}
