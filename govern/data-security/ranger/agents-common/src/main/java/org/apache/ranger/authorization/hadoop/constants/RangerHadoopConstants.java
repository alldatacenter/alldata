/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.hadoop.constants;

public class RangerHadoopConstants {
	
	public static final String RANGER_ADD_HDFS_PERMISSION_PROP = "xasecure.add-hadoop-authorization";
	public static final String RANGER_OPTIMIZE_SUBACCESS_AUTHORIZATION_PROP = "ranger.optimize-subaccess-authorization" ;
	public static final boolean RANGER_ADD_HDFS_PERMISSION_DEFAULT = false;
	public static final boolean RANGER_OPTIMIZE_SUBACCESS_AUTHORIZATION_DEFAULT = false ;
	public static final String READ_ACCCESS_TYPE = "read";
	public static final String WRITE_ACCCESS_TYPE = "write";
	public static final String EXECUTE_ACCCESS_TYPE = "execute";

	public static final String READ_EXECUTE_PERM  = "READ_EXECUTE";
	public static final String WRITE_EXECUTE_PERM = "WRITE_EXECUTE";
	public static final String READ_WRITE_PERM    = "READ_WRITE";
	public static final String ALL_PERM           = "ALL";

	public static final String HDFS_ROOT_FOLDER_PATH_ALT = "";
	public static final String HDFS_ROOT_FOLDER_PATH = "/";
	
	public static final String  HIVE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_PROP 	     = "xasecure.hive.update.xapolicies.on.grant.revoke";
	public static final boolean HIVE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_DEFAULT_VALUE = true;
	public static final String  HIVE_BLOCK_UPDATE_IF_ROWFILTER_COLUMNMASK_SPECIFIED_PROP          = "xasecure.hive.block.update.if.rowfilter.columnmask.specified";
	public static final boolean HIVE_BLOCK_UPDATE_IF_ROWFILTER_COLUMNMASK_SPECIFIED_DEFAULT_VALUE = true;
	public static final String  HIVE_DESCRIBE_TABLE_SHOW_COLUMNS_AUTH_OPTION_PROP	= "xasecure.hive.describetable.showcolumns.authorization.option";
	public static final String  HIVE_DESCRIBE_TABLE_SHOW_COLUMNS_AUTH_OPTION_PROP_DEFAULT_VALUE	= "NONE";
	public static final String  HIVE_URI_PERMISSION_COARSE_CHECK = "xasecure.hive.uri.permission.coarse.check";
	public static final boolean HIVE_URI_PERMISSION_COARSE_CHECK_DEFAULT_VALUE = false;

	public static final String  HBASE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_PROP 	     = "xasecure.hbase.update.xapolicies.on.grant.revoke";
	public static final boolean HBASE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_DEFAULT_VALUE = true;
	
	public static final String KNOX_ACCESS_VERIFIER_CLASS_NAME_PROP 	= "knox.authorization.verifier.classname";
	public static final String KNOX_ACCESS_VERIFIER_CLASS_NAME_DEFAULT_VALUE = "org.apache.ranger.pdp.knox.RangerAuthorizer";

	public static final String STORM_ACCESS_VERIFIER_CLASS_NAME_PROP 	= "storm.authorization.verifier.classname";
	public static final String STORM_ACCESS_VERIFIER_CLASS_NAME_DEFAULT_VALUE = "org.apache.ranger.pdp.storm.RangerAuthorizer";

	public static final String  RANGER_ADD_YARN_PERMISSION_PROP    = "ranger.add-yarn-authorization";
	public static final boolean RANGER_ADD_YARN_PERMISSION_DEFAULT = true;

	//
	// Logging constants
	//
	public static final String AUDITLOG_FIELD_DELIMITER_PROP 			= "xasecure.auditlog.fieldDelimiterString";
	public static final String AUDITLOG_RANGER_MODULE_ACL_NAME_PROP  	= "xasecure.auditlog.xasecureAcl.name";
	public static final String AUDITLOG_HADOOP_MODULE_ACL_NAME_PROP    	= "xasecure.auditlog.hadoopAcl.name";
	public static final String AUDITLOG_YARN_MODULE_ACL_NAME_PROP    	= "ranger.auditlog.yarnAcl.name";
	
	public static final String DEFAULT_LOG_FIELD_DELIMITOR  			= "|";
	public static final String DEFAULT_XASECURE_MODULE_ACL_NAME  	= "xasecure-acl";
	public static final String DEFAULT_RANGER_MODULE_ACL_NAME  		= "ranger-acl";
	public static final String DEFAULT_HADOOP_MODULE_ACL_NAME    		= "hadoop-acl";
	public static final String DEFAULT_YARN_MODULE_ACL_NAME    		= "yarn-acl";
	

	public static final String AUDITLOG_FIELDINFO_VISIBLE_PROP 			= "xasecure.auditlog.fieldInfoVisible";
	public static final boolean DEFAULT_AUDITLOG_FIELDINFO_VISIBLE    	= false;

	public static final String AUDITLOG_ACCESS_GRANTED_TEXT_PROP 		= "xasecure.auditlog.accessgranted.text";
	public static final String AUDITLOG_ACCESS_DENIED_TEXT_PROP 		= "xasecure.auditlog.accessdenied.text";

	public static final String DEFAULT_ACCESS_GRANTED_TEXT 				= "granted";
	public static final String DEFAULT_ACCESS_DENIED_TEXT 				= "denied";
	
	public static final String AUDITLOG_EMPTY_STRING 					= "";
	
	public static final String AUDITLOG_HDFS_EXCLUDE_LIST_PROP 			= "xasecure.auditlog.hdfs.excludeusers";
	public static final String AUDITLOG_REPOSITORY_NAME_PROP 			= "xasecure.audit.repository.name";
	public static final String AUDITLOG_IS_ENABLED_PROP 			    = "xasecure.audit.is.enabled";
	
	public static final String KEYMGR_URL_PROP = "hdfs.keymanager.url";
	public static final String ACCESS_TYPE_MONITOR_HEALTH               = "monitorHealth";
}
