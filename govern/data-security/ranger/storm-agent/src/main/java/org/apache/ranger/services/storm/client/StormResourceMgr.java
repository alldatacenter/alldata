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

package org.apache.ranger.services.storm.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormResourceMgr {
	private static final 	Logger 	LOG 		= LoggerFactory.getLogger(StormResourceMgr.class);
	private static final 	String  TOPOLOGY	= "topology";
	
	public static Map<String, Object> validateConfig(String serviceName, Map<String, String> configs) throws Exception {
		Map<String, Object> ret = null;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> StormResourceMgr.validateConfig ServiceName: "+ serviceName + "Configs" + configs );
		}	
		
		try {
			ret = StormClient.connectionTest(serviceName, configs);
		} catch (Exception e) {
			LOG.error("<== StormResourceMgr.validateConfig Error: " + e);
		  throw e;
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== StormResourceMgr.validateConfig Result : "+ ret  );
		}	
		return ret;
	}
	
    public static List<String> getStormResources(String serviceName, Map<String, String> configs,ResourceLookupContext context) {
        String 		 userInput 				  = context.getUserInput();
		Map<String, List<String>> resourceMap = context.getResources();
	    List<String> 		resultList        = null;
		List<String> 		StormTopologyList = null;
		String  			stormTopologyName = null;
		
		if ( resourceMap != null && !resourceMap.isEmpty() &&
			resourceMap.get(TOPOLOGY) != null ) {
			stormTopologyName = userInput;
			StormTopologyList = resourceMap.get(TOPOLOGY);
		} else {
			stormTopologyName = userInput;
		}
		
		
        if (configs == null || configs.isEmpty()) {
                LOG.error("Connection Config is empty");

        } else {
                String url 		= configs.get("nimbus.url");
                String username = configs.get("username");
                String password = configs.get("password");
                String lookupPrincipal = configs.get("lookupprincipal");
                String lookupKeytab = configs.get("lookupkeytab");
                String nameRules = configs.get("namerules");
                resultList = getStormResources(url, username, password,lookupPrincipal, lookupKeytab, nameRules, stormTopologyName,StormTopologyList);
        }
        return resultList;
    }

    public static List<String> getStormResources(String url, String username, String password, String lookupPrincipal, String lookupKeytab, String nameRules, String topologyName, List<String> StormTopologyList) {
    	List<String> topologyList	  = null;
        final StormClient stormClient = StormConnectionMgr.getStormClient(url, username, password, lookupPrincipal, lookupKeytab, nameRules);
	    if (stormClient == null) {
		    LOG.error("Storm Client is null");
		    return new ArrayList<String>();
	    }
	    synchronized(stormClient){
	    	topologyList = stormClient.getTopologyList(topologyName,StormTopologyList);
	    }
        return topologyList;
    }

}
