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

import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KnoxResourceMgr {

	private static final Logger LOG = LoggerFactory.getLogger(KnoxResourceMgr.class);
	
	private static final String TOPOLOGY	  	 = "topology";
	private static final String SERVICE 	 	 = "service";

	public static Map<String, Object> validateConfig(String serviceName, Map<String, String> configs) throws Exception {
		Map<String, Object> ret = null;
		if (LOG.isDebugEnabled()) {
		   LOG.debug("==> KnoxResourceMgr.testConnection ServiceName: "+ serviceName + "Configs" + configs );
		}
		try {
			ret = KnoxClient.connectionTest(serviceName, configs);
		} catch (Exception e) {
		  LOG.error("<== KnoxResourceMgr.connectionTest Error: " + e);
		  throw e;
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== KnoxResourceMgr.HdfsResourceMgr Result : "+ ret  );
		}
		return ret;
	 }
	
	public static List<String> getKnoxResources(String serviceName, Map<String, String> configs, ResourceLookupContext context) throws Exception  {
		
		
		String 		 userInput 				  = context.getUserInput();
		String 		 resource				  = context.getResourceName();
		Map<String, List<String>> resourceMap = context.getResources();
		List<String> resultList 			  = null;
		List<String> knoxTopologyList		  = null;
		List<String> knoxServiceList		  = null;
		String  	 knoxTopologyName		  = null;
		String  	 knoxServiceName		  = null;
		
		if ( userInput != null && resource != null) {
			if ( resourceMap != null && !resourceMap.isEmpty() ) {
				knoxTopologyList = resourceMap.get(TOPOLOGY);
				knoxServiceList  = resourceMap.get(SERVICE);
			}
			switch (resource.trim().toLowerCase()) {
			 case TOPOLOGY:
				 knoxTopologyName = userInput;
				 break;
			case SERVICE:
				 knoxServiceName = userInput;
				 break;
			default:
				 break;
			}
		}
		
		String knoxUrl = configs.get("knox.url");
		String knoxAdminUser = configs.get("username");
		String knoxAdminPassword = configs.get("password");

		if (knoxUrl == null || knoxUrl.isEmpty()) {
			LOG.error("Unable to get knox resources: knoxUrl is empty");
			return resultList;
		} else if (knoxAdminUser == null || knoxAdminUser.isEmpty()) {
			LOG.error("Unable to get knox resources: knoxAdminUser is empty");
			return resultList;
		} else if (knoxAdminPassword == null || knoxAdminPassword.isEmpty()) {
			LOG.error("Unable to get knox resources: knoxAdminPassword is empty");
			return resultList;
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== KnoxResourceMgr.getKnoxResources()  knoxUrl: "+ knoxUrl  + " knoxAdminUser: " + knoxAdminUser + " topologyName: "  + knoxTopologyName + " KnoxServiceName: " + knoxServiceName);
		}
		
		final KnoxClient knoxClient = new KnoxConnectionMgr().getKnoxClient(knoxUrl, knoxAdminUser, knoxAdminPassword);
		if ( knoxClient != null) {
			synchronized(knoxClient) {
				resultList = KnoxClient.getKnoxResources(knoxClient, knoxTopologyName, knoxServiceName,knoxTopologyList,knoxServiceList);
			}
		}
		return  resultList;
	}
}
