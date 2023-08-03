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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HBaseConnectionMgr {

	private static final Logger LOG = LoggerFactory.getLogger(HBaseConnectionMgr.class);

	protected ConcurrentMap<String, HBaseClient> hbaseConnectionCache;
	
	protected ConcurrentMap<String, Boolean> repoConnectStatusMap;

	public HBaseConnectionMgr() {
		hbaseConnectionCache = new ConcurrentHashMap<String, HBaseClient>();
		repoConnectStatusMap = new ConcurrentHashMap<String, Boolean>();
	}

	public HBaseClient getHBaseConnection(final String serviceName, final String serviceType, final Map<String,String> configs) {
		
		HBaseClient client = null;
		if (serviceType != null) {
			// get it from the cache
				client = hbaseConnectionCache.get(serviceName);
				if (client == null) {
					if ( configs == null ) {
						final Callable<HBaseClient> connectHBase = new Callable<HBaseClient>() {
							@Override
							public HBaseClient call() throws Exception {
								HBaseClient hBaseClient=null;
								if(serviceName!=null){
									try{
										hBaseClient=new HBaseClient(serviceName, configs);
									}catch(Exception ex){
										LOG.error("Error connecting HBase repository : ", ex);
									}
								}
								return hBaseClient;
							}
						};
						
						try {
							if(connectHBase!=null){
								client = TimedEventUtil.timedTask(connectHBase, 5, TimeUnit.SECONDS);
							}
						} catch(Exception e){
							LOG.error("Error connecting HBase repository : " + serviceName);
						}
					} else {
					
						final Callable<HBaseClient> connectHBase = new Callable<HBaseClient>() {
							@Override
							public HBaseClient call() throws Exception {
								HBaseClient hBaseClient=null;
								if(serviceName!=null && configs !=null){
									try{
										hBaseClient=new HBaseClient(serviceName,configs);
									}catch(Exception ex){
										LOG.error("Error connecting HBase repository : ", ex);
									}
								}
								return hBaseClient;
							}
						};
						
						try {
							if(connectHBase!=null){
								client = TimedEventUtil.timedTask(connectHBase, 5, TimeUnit.SECONDS);
							}
						} catch(Exception e){
							LOG.error("Error connecting HBase repository : "+
									serviceName +" using config : "+ configs);
						}
					}

					if(client!=null){
						HBaseClient oldClient = hbaseConnectionCache.putIfAbsent(serviceName, client);
						if (oldClient != null) {
							// in the meantime someone else has put a valid client into the cache, let's use that instead.
							client = oldClient;
						}
					}
	
				} else {
					
				  List<String> testConnect = client.getTableList(".\\*",null);
				
				  if(testConnect == null){
						hbaseConnectionCache.remove(serviceName);
						client = getHBaseConnection(serviceName,serviceType,configs);
				  }
			 }
			 repoConnectStatusMap.put(serviceName, true);
		} else {
			LOG.error("Service Name not found with name " + serviceName,
					new Throwable());
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HBaseConnectionMgr.getHBaseConnection() HbaseClient : "+ client  );
		}	
		return client;
	}
}
