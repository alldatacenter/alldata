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

 /**
 *
 */
package org.apache.ranger.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class MapUtil implements Serializable{
    static Map<Integer, String> policyExportAuditSyncStatusMessageMap=new HashMap<Integer, String>();

    public static void init() {
    	policyExportAuditSyncStatusMessageMap=new HashMap<Integer, String>();
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(200), "Policies synced to plugin");
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(202), "Error syncing policies");
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(400), "Error syncing policies");
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(401), "Bad Credentials");
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(403), "Error syncing policies");
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(404), "Error syncing policies");
    	policyExportAuditSyncStatusMessageMap.put(Integer.valueOf(500), "Error syncing policies");
    }

    public static String getPolicyExportAuditSyncStatus(int key) {
    	String status="";
    	if(policyExportAuditSyncStatusMessageMap==null || policyExportAuditSyncStatusMessageMap.isEmpty()){
    		init();
    	}
    	if(policyExportAuditSyncStatusMessageMap!=null && policyExportAuditSyncStatusMessageMap.containsKey(key)){
    		status=policyExportAuditSyncStatusMessageMap.get(key);
    	}
    	return status;
    }
}