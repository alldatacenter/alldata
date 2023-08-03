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

package org.apache.ranger.services.hbase.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HBaseResourceMgr {

	private static final Logger LOG = LoggerFactory.getLogger(HBaseResourceMgr.class);
	
	private static final String TABLE 		 		    = "table";
	private static final String COLUMNFAMILY 		    = "column-family";
		
	public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) throws Exception {
		Map<String, Object> ret = null;
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HBaseResourceMgr.connectionTest() ServiceName: "+ serviceName + "Configs" + configs );
		}	
		
		try {
			ret = HBaseClient.connectionTest(serviceName, configs);
		} catch (HadoopException e) {
			LOG.error("<== HBaseResourceMgr.connectionTest() Error: " + e);
		  throw e;
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HBaseResourceMgr.connectionTest() Result: "+ ret  );
		}	
		return ret;
	}
	
	public static List<String> getHBaseResource(String serviceName, String serviceType, Map<String, String> configs,ResourceLookupContext context) throws Exception{
		
		String 					  userInput 		= context.getUserInput();
		String 					  resource		    = context.getResourceName();
		Map<String, List<String>> resourceMap  		= context.getResources();
		List<String> 			  resultList 		= null;
		String  				  tableName	   		= null;
		String  				  columnFamilies   	= null;
		List<String> 		  	  tableList			= null;
		List<String> 		      columnFamilyList  = null;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HBaseResourceMgr.getHBaseResource UserInput: \""+ userInput  + "\" resource : " + resource + " resourceMap: "  + resourceMap);
		}	
		
		if ( userInput != null && resource != null) {
			if ( resourceMap != null && !resourceMap.isEmpty() ) {
				tableList 		 = resourceMap.get(TABLE);
				columnFamilyList = resourceMap.get(COLUMNFAMILY);
			 }
		     switch (resource.trim().toLowerCase()) {
			 case TABLE:
					tableName = userInput;
					break;
			 case COLUMNFAMILY:
					columnFamilies = userInput;
					break;
			 default:
					break;
			}
		}

		if (serviceName != null && userInput != null) {
			final List<String> finaltableList 		 = tableList;
			final List<String> finalcolumnFamilyList = columnFamilyList;
			
			try {
				if(LOG.isDebugEnabled()) {
					LOG.debug("<== HBaseResourceMgr.getHBaseResource UserInput: \""+ userInput  + "\" configs: " + configs + " context: "  + context);
				}
				final HBaseClient hBaseClient = new HBaseConnectionMgr().getHBaseConnection(serviceName,serviceType,configs);
				Callable<List<String>> callableObj = null;
				
				if ( hBaseClient != null) {
					if ( tableName != null && !tableName.isEmpty()) {
						final String finalTableName;
						//get tableList
						if (!tableName.endsWith("*")) {
							tableName += "*";
						}

						tableName = tableName.replaceAll("\\*", ".\\*");
						finalTableName = tableName;

						callableObj = new Callable<List<String>>() {
							@Override
							public List<String> call() {
								return hBaseClient.getTableList(finalTableName,finaltableList);
								}
							};
						} else {
							//get columfamilyList
							final String finalColFamilies;
							if (columnFamilies != null && !columnFamilies.isEmpty()) {
								if (!columnFamilies.endsWith("*")) {
								columnFamilies += "*";
								}

							columnFamilies = columnFamilies.replaceAll("\\*",
									".\\*");
							finalColFamilies = columnFamilies;

							callableObj = new Callable<List<String>>() {
								@Override
								public List<String> call() {
									return hBaseClient.getColumnFamilyList(finalColFamilies,finaltableList,finalcolumnFamilyList);
								}
							};
						}
					}
					if (callableObj != null) {
						synchronized(hBaseClient) {
							resultList = TimedEventUtil.timedTask(callableObj, 5,
									TimeUnit.SECONDS);
						}
					}
					
				}
			} catch (Exception e) {
				LOG.error("Unable to get hbase resources.", e);
				throw e;
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HBaseResourceMgr.getHBaseResource() Result :" + resultList);
		}
		
		return resultList;
	}
}
