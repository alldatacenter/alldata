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

package org.apache.ranger.services.hdfs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HdfsResourceMgr {

	private static final Logger LOG = LoggerFactory.getLogger(HdfsResourceMgr.class);
	public static final String PATH	= "path";

	public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) throws Exception {
		Map<String, Object> ret = null;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HdfsResourceMgr.connectionTest ServiceName: "+ serviceName + "Configs" + configs );
		}	
		
		try {
			ret = HdfsClient.connectionTest(serviceName, configs);
		} catch (HadoopException e) {
			LOG.error("<== HdfsResourceMgr.testConnection Error: " + e.getMessage(),  e);
			throw e;
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HdfsResourceMgr.connectionTest Result : "+ ret  );
		}	
		return ret;
	}
	
	public static List<String> getHdfsResources(String serviceName, String serviceType, Map<String, String> configs,ResourceLookupContext context) throws Exception {
		
		List<String> resultList 			  = null;
		String userInput 					  = context.getUserInput();
		String resource						  = context.getResourceName();
		Map<String, List<String>> resourceMap = context.getResources();
		final List<String>		  pathList	  = new ArrayList<String>();
		
		if( resource != null && resourceMap != null && resourceMap.get(PATH) != null) {
			for (String path: resourceMap.get(PATH)) {
				pathList.add(path);
			}
		}
		
		if (serviceName != null && userInput != null) {
			try {
				if(LOG.isDebugEnabled()) {
					LOG.debug("<== HdfsResourceMgr.getHdfsResources() UserInput: "+ userInput  + "configs: " + configs + "context: "  + context);
				}
				
				String wildCardToMatch;
				final HdfsClient hdfsClient = new HdfsConnectionMgr().getHadoopConnection(serviceName, serviceType, configs);
				if (hdfsClient != null) {
					Integer lastIndex = userInput.lastIndexOf("/");
					if (lastIndex < 0) {
						wildCardToMatch = userInput + "*";
						userInput = "/";
					} else if (lastIndex == 0 && userInput.length() == 1) {
						wildCardToMatch = null;
						userInput = "/";
					} else if ((lastIndex + 1) == userInput.length()) {
						wildCardToMatch = null;
						userInput = userInput.substring(0, lastIndex + 1);
					} else {
						wildCardToMatch = userInput.substring(lastIndex + 1)
								+ "*";
						userInput = userInput.substring(0, lastIndex + 1);
					}

					final String finalBaseDir = userInput;
					final String finalWildCardToMatch = wildCardToMatch;
					final Callable<List<String>> callableObj = new Callable<List<String>>() {

						@Override
						public List<String> call() throws Exception {
							return hdfsClient.listFiles(finalBaseDir,
									finalWildCardToMatch, pathList);
						}

					};
					if ( callableObj != null) {
						synchronized(hdfsClient) {
							resultList = TimedEventUtil.timedTask(callableObj, 5,TimeUnit.SECONDS);
						}
					}
					if(LOG.isDebugEnabled()) {
						LOG.debug("Resource dir : " + userInput
							+ " wild card to match : " + wildCardToMatch
							+ "\n Matching resources : " + resultList);
					}
				}
			} catch (HadoopException e) {
				LOG.error("Unable to get hdfs resources.", e);
				throw e;
			}

		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HdfsResourceMgr.getHdfsResources() Result : "+ resultList  );
		}	
		return resultList;
    }
}
