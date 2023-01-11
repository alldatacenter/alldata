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

package org.apache.ranger.authorization.hadoop.config;

public class RangerConfigConstants {
	//SECURITY CONFIG DEFAULTS
	public static final String  RANGER_SERVICE_NAME 							= "ranger.plugin.<ServiceType>.service.name";
	public static final String  RANGER_PLUGIN_POLICY_SOURCE_IMPL 				= "ranger.plugin.<ServiceType>.policy.source.impl";
	public static final String  RANGER_PLUGIN_POLICY_SOURCE_IMPL_DEFAULT 		= "org.apache.ranger.admin.client.RangerAdminRESTClient";
	public static final String  RANGER_PLUGIN_POLICY_REST_URL					= "ranger.plugin.<ServiceType>.policy.rest.url";
	public static final String 	RANGER_PLUGIN_REST_SSL_CONFIG_FILE				= "ranger.plugin.<ServiceType>.policy.rest.ssl.config.file";
	public static final String 	RANGER_PLUGIN_POLICY_POLLINVETERVALMS			= "ranger.plugin.<ServiceType>.policy.pollIntervalMs";
    public static final String	RANGER_PLUGIN_POLICY_CACHE_DIR					= "ranger.plugin.<ServiceType>.policy.cache.dir";
    public static final	String 	RANGER_PLUGIN_ADD_HADDOOP_AUTHORIZATION			= "xasecure.add-hadoop-authorization";
    public static final String  RANGER_KEYSTORE_TYPE                            = "ranger.keystore.file.type";

    //CHANGE MAP CONSTANTS
    public static final String	XASECURE_POLICYMGR_URL							= "xasecure.<ServiceType>.policymgr.url";
    public static final String  XASECURE_POLICYMGR_URL_LASTSTOREDFILE			= "xasecure.<ServiceType>.policymgr.url.laststoredfile";		
    public static final String  XASECURE_POLICYMGR_GRL_RELOADINTERVALINMILLIS   = "xasecure.<ServiceType>.policymgr.url.reloadIntervalInMillis";
    public static final String 	XASECURE_ADD_HADDOP_AUTHORZATION				= "xasecure.add-hadoop-authorization";
    public static final	String  XASECURE_UPDATE_XAPOLICIES_ON_GRANT				= "xasecure.<ServiceType>.update.xapolicies.on.grant.revoke";	

    //Legacy Files
    public static final String  XASECURE_AUDIT_FILE								= "xasecure-audit.xml";
    public static final String  XASECURE_SECURITY_FILE							= "xasecure-<ServiceType>-security.xml";
    public static final String  XASECURE_POLICYMGR_SSL_FILE						= "/etc/<ServiceType>/conf/xasecure-policymgr-ssl.xml";

    //KNOX
    public static final String  RANGER_KNOX_PLUGIN_POLICY_SOURCE_IMPL_DEFAULT   = "org.apache.ranger.admin.client.RangerAdminJersey2RESTClient";
}

