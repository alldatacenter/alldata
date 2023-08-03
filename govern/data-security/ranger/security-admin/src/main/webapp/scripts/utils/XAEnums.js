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

 
define(function(require) {

	var $ = require('jquery');
	var XAEnums = {};

        var mergeParams = function(defaults, params) {
		if (!params) {
			return defaults;
		}
		defaults || (defaults = {});
		$.extend(true, defaults, params);
		return defaults;
	};

	XAEnums.AccessResult = mergeParams(XAEnums.AccessResult, {
		ACCESS_RESULT_DENIED:{value:0, label:'Denied', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED', auditFilterLabel:'DENIED'},
		ACCESS_RESULT_ALLOWED:{value:1, label:'Allowed', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED', auditFilterLabel:'ALLOWED'},
		ACCESS_RESULT_NOT_DETERMINED:{value:2, label:'Not Determined', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_NOT_DETERMINED', tt: 'lbl.AccessResult_ACCESS_RESULT_NOT_DETERMINED', auditFilterLabel:'NOT_DETERMINED'}
	});
	
	XAEnums.UserSource = mergeParams(XAEnums.UserSource, {
		XA_PORTAL_USER:{value:0, label:'Allowed', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
		XA_USER:{value:1, label:'Denied', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'}
	});
	
	XAEnums.GroupSource = mergeParams(XAEnums.GroupSource, {
		XA_PORTAL_GROUP:{value:0, label:'Allowed', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
		XA_GROUP:{value:1, label:'Denied', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'}
	});
	
	XAEnums.RangerPolicyType = mergeParams(XAEnums.RangerPolicyType, {
		RANGER_ACCESS_POLICY_TYPE:{value:0, label:'Access', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
		RANGER_MASKING_POLICY_TYPE:{value:1, label:'Masking', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'},
		RANGER_ROW_FILTER_POLICY_TYPE:{value:2, label:'Row Level Filter', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'}
	});
	
	XAEnums.UserRoles = mergeParams(XAEnums.UserRoles, {
		ROLE_SYS_ADMIN:{value:0, label:'Admin', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
		ROLE_USER:{value:1, label:'User', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'},
		ROLE_KEY_ADMIN:{value:2, label:'KeyAdmin', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
        ROLE_ADMIN_AUDITOR:{value:3, label:'Auditor', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
        ROLE_KEY_ADMIN_AUDITOR:{value:4, label:'KMSAuditor', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'}
	});
	
	XAEnums.UserTypes = mergeParams(XAEnums.UserTypes, {
		USER_INTERNAL:{value:0, label:'Internal', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
		USER_EXTERNAL:{value:1, label:'External', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'}
	});
	
	XAEnums.GroupTypes = mergeParams(XAEnums.GroupTypes, {
		GROUP_INTERNAL:{value:0, label:'Internal', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_ALLOWED', tt: 'lbl.AccessResult_ACCESS_RESULT_ALLOWED'},
		GROUP_EXTERNAL:{value:1, label:'External', rbkey:'xa.enum.AccessResult.ACCESS_RESULT_DENIED', tt: 'lbl.AccessResult_ACCESS_RESULT_DENIED'}
	});
	
	XAEnums.ActivationStatus = mergeParams(XAEnums.ActivationStatus, {
		ACT_STATUS_DISABLED:{value:0, label:'Disabled', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_DISABLED', tt: 'lbl.ActivationStatus_ACT_STATUS_DISABLED'},
		ACT_STATUS_ACTIVE:{value:1, label:'Active', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_ACTIVE', tt: 'lbl.ActivationStatus_ACT_STATUS_ACTIVE'},
		ACT_STATUS_PENDING_APPROVAL:{value:2, label:'Pending Approval', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_PENDING_APPROVAL', tt: 'lbl.ActivationStatus_ACT_STATUS_PENDING_APPROVAL'},
		ACT_STATUS_PENDING_ACTIVATION:{value:3, label:'Pending Activation', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_PENDING_ACTIVATION', tt: 'lbl.ActivationStatus_ACT_STATUS_PENDING_ACTIVATION'},
		ACT_STATUS_REJECTED:{value:4, label:'Rejected', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_REJECTED', tt: 'lbl.ActivationStatus_ACT_STATUS_REJECTED'},
		ACT_STATUS_DEACTIVATED:{value:5, label:'Deactivated', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_DEACTIVATED', tt: 'lbl.ActivationStatus_ACT_STATUS_DEACTIVATED'},
		ACT_STATUS_PRE_REGISTRATION:{value:6, label:'Registration Pending', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_PRE_REGISTRATION', tt: 'lbl.ActivationStatus_ACT_STATUS_PRE_REGISTRATION'},
		ACT_STATUS_NO_LOGIN:{value:7, label:'No login privilege', rbkey:'xa.enum.ActivationStatus.ACT_STATUS_NO_LOGIN', tt: 'lbl.ActivationStatus_ACT_STATUS_NO_LOGIN'}
	});

	XAEnums.VisibilityStatus = mergeParams(XAEnums.VisibilityStatus, {
		STATUS_HIDDEN:{value:0, label:'Hidden', rbkey:'xa.enum.VisibilityStatus.IS_HIDDEN', tt: 'lbl.VisibilityStatus_IS_HIDDEN'},
		STATUS_VISIBLE:{value:1, label:'Visible', rbkey:'xa.enum.VisibilityStatus.IS_VISIBLE', tt: 'lbl.VisibilityStatus_IS_VISIBLE'}
	});

	XAEnums.AuditStatus = mergeParams(XAEnums.AuditStatus, {
		AUDIT_ENABLED:{value:true, label:'Yes', rbkey:'xa.enum.AuditStatus.ENABLED', tt: 'lbl.AuditStatus_ENABLED'},
		AUDIT_DISABLED:{value:false, label:'No', rbkey:'xa.enum.AuditStatus.DISABLED', tt: 'lbl.AuditStatus_DISABLED'}
	});

	XAEnums.RecursiveStatus = mergeParams(XAEnums.RecursiveStatus, {
		STATUS_RECURSIVE:{value:true, label:'recursive', rbkey:'xa.enum.RecursiveStatus.RECURSIVE', tt: 'lbl.RecursiveStatus_RECURSIVE'},
		STATUS_NONRECURSIVE:{value:false, label:'nonrecursive', rbkey:'xa.enum.RecursiveStatus.NONRECURSIVE', tt: 'lbl.RecursiveStatus_NONRECURSIVE'}
	});

	XAEnums.ExcludeStatus = mergeParams(XAEnums.ExcludeStatus, {
		STATUS_EXCLUDE:{value:true, label:'exclude', rbkey:'xa.enum.ExcludeStatus.EXCLUDE', tt: 'lbl.ExcludeStatus_EXCLUDE'},
		STATUS_INCLUDE:{value:false, label:'include', rbkey:'xa.enum.ExcludeStatus.INCLUDE', tt: 'lbl.ExcludeStatus_INCLUDE'}
	});

	XAEnums.ActiveStatus = mergeParams(XAEnums.ActiveStatus, {
		STATUS_DISABLED:{value:0, label:'Disabled', rbkey:'xa.enum.ActiveStatus.STATUS_DISABLED', tt: 'lbl.ActiveStatus_STATUS_DISABLED'},
		STATUS_ENABLED:{value:1, label:'Enabled', rbkey:'xa.enum.ActiveStatus.STATUS_ENABLED', tt: 'lbl.ActiveStatus_STATUS_ENABLED'},
		STATUS_DELETED:{value:2, label:'Deleted', rbkey:'xa.enum.ActiveStatus.STATUS_DELETED', tt: 'lbl.ActiveStatus_STATUS_DELETED'}
	});

	XAEnums.PolicyType = mergeParams(XAEnums.PolicyType, {
		POLICY_TYPE_ALLOW:{value:0, label:'Allow', rbkey:'xa.enum.PolicyType.POLICY_TYPE_ALLOW', tt: 'lbl.PolicyType_ALLOW'},
		POLICY_TYPE_DENY:{value:1, label:'Deny', rbkey:'xa.enum.PolicyType.POLICY_TYPE_DENY', tt: 'lbl.PolicyType_DENY'}
	});

	XAEnums.AssetType = mergeParams(XAEnums.AssetType, {
		ASSET_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.AssetType.ASSET_UNKNOWN', tt: 'lbl.AssetType_ASSET_UNKNOWN'},
		ASSET_HDFS:{value:1, label:'HDFS', rbkey:'xa.enum.AssetType.ASSET_HDFS', tt: 'lbl.AssetType_ASSET_HDFS'},
		ASSET_HBASE:{value:2, label:'HBase', rbkey:'xa.enum.AssetType.ASSET_HBASE', tt: 'lbl.AssetType_ASSET_HBASE'},
		ASSET_HIVE:{value:3, label:'Hive', rbkey:'xa.enum.AssetType.ASSET_HIVE', tt: 'lbl.AssetType_ASSET_HIVE'},
		ASSET_AGENT:{value:4, label:'Agent', rbkey:'xa.enum.AssetType.ASSET_KNOX', tt: 'lbl.AssetType_ASSET_KNOX'},
		ASSET_KNOX:{value:5, label:'Knox', rbkey:'xa.enum.AssetType.ASSET_KNOX', tt: 'lbl.AssetType_ASSET_KNOX'},
		ASSET_STORM:{value:6, label:'Storm', rbkey:'xa.enum.AssetType.ASSET_STORM', tt: 'lbl.AssetType_ASSET_STORM'},
		ASSET_SOLR:{value:7, label:'Solr', rbkey:'xa.enum.AssetType.ASSET_SOLR', tt: 'lbl.AssetType_ASSET_SOLR'}
	});
	
	XAEnums.ServiceType = mergeParams(XAEnums.ServiceType, {
		Service_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.AssetType.ASSET_UNKNOWN', tt: 'lbl.AssetType_ASSET_UNKNOWN'},
		Service_HDFS:{value:1, label:'hdfs', rbkey:'xa.enum.AssetType.ASSET_HDFS', tt: 'lbl.AssetType_ASSET_HDFS'},
		Service_HIVE:{value:2, label:'hive', rbkey:'xa.enum.AssetType.ASSET_HIVE', tt: 'lbl.AssetType_ASSET_HIVE'},
		Service_HBASE:{value:3, label:'hbase', rbkey:'xa.enum.AssetType.ASSET_HBASE', tt: 'lbl.AssetType_ASSET_HBASE'},
		Service_KNOX:{value:4, label:'knox', rbkey:'xa.enum.AssetType.ASSET_KNOX', tt: 'lbl.AssetType_ASSET_KNOX'},
		Service_STORM:{value:5, label:'storm', rbkey:'xa.enum.AssetType.ASSET_STORM', tt: 'lbl.AssetType_ASSET_STORM'},
		Service_SOLR:{value:6, label:'solr', rbkey:'xa.enum.AssetType.ASSET_SOLR', tt: 'lbl.AssetType_ASSET_SOLR'},
		SERVICE_TAG:{value:7, label:'tag', rbkey:'xa.enum.ServiceType.SERVICE_TAG', tt: 'lbl.ServiceType_SERVICE_TAG'},
		Service_KMS:{value:8, label:'kms', rbkey:'xa.enum.ServiceType.SERVICE_KMS', tt: 'lbl.ServiceType_SERVICE_KMS'},
		Service_YARN:{value:8, label:'yarn', rbkey:'xa.enum.ServiceType.SERVICE_YARN', tt: 'lbl.ServiceType_SERVICE_YARN'}
	});

	XAEnums.AuthStatus = mergeParams(XAEnums.AuthStatus, {
		AUTH_STATUS_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_UNKNOWN', tt: 'lbl.AuthStatus_AUTH_STATUS_UNKNOWN'},
		AUTH_STATUS_SUCCESS:{value:1, label:'Success', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_SUCCESS', tt: 'lbl.AuthStatus_AUTH_STATUS_SUCCESS'},
		AUTH_STATUS_WRONG_PASSWORD:{value:2, label:'Wrong Password', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_WRONG_PASSWORD', tt: 'lbl.AuthStatus_AUTH_STATUS_WRONG_PASSWORD'},
		AUTH_STATUS_DISABLED:{value:3, label:'Account Disabled', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_DISABLED', tt: 'lbl.AuthStatus_AUTH_STATUS_DISABLED'},
		AUTH_STATUS_LOCKED:{value:4, label:'Locked', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_LOCKED', tt: 'lbl.AuthStatus_AUTH_STATUS_LOCKED'},
		AUTH_STATUS_PASSWORD_EXPIRED:{value:5, label:'Password Expired', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_PASSWORD_EXPIRED', tt: 'lbl.AuthStatus_AUTH_STATUS_PASSWORD_EXPIRED'},
		AUTH_STATUS_USER_NOT_FOUND:{value:6, label:'User not found', rbkey:'xa.enum.AuthStatus.AUTH_STATUS_USER_NOT_FOUND', tt: 'lbl.AuthStatus_AUTH_STATUS_USER_NOT_FOUND'}
	});

	XAEnums.AuthType = mergeParams(XAEnums.AuthType, {
		AUTH_TYPE_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.AuthType.AUTH_TYPE_UNKNOWN', tt: 'lbl.AuthType_AUTH_TYPE_UNKNOWN'},
		AUTH_TYPE_PASSWORD:{value:1, label:'Username/Password', rbkey:'xa.enum.AuthType.AUTH_TYPE_PASSWORD', tt: 'lbl.AuthType_AUTH_TYPE_PASSWORD'},
		AUTH_TYPE_KERBEROS:{value:2, label:'Kerberos', rbkey:'xa.enum.AuthType.AUTH_TYPE_KERBEROS', tt: 'lbl.AuthType_AUTH_TYPE_KERBEROS'},
		AUTH_TYPE_SSO:{value:3, label:'SingleSignOn', rbkey:'xa.enum.AuthType.AUTH_TYPE_SSO', tt: 'lbl.AuthType_AUTH_TYPE_SSO'},
		AUTH_TYPE_TRUSTED_PROXY:{value:4, label:'Trusted Proxy', rbkey:'xa.enum.AuthType.AUTH_TYPE_TRUSTED_PROXY', tt: 'lbl.AuthType_AUTH_TYPE_TRUSTED_PROXY'}
	});

	XAEnums.BooleanValue = mergeParams(XAEnums.BooleanValue, {
		BOOL_NONE:{value:0, label:'None', rbkey:'xa.enum.BooleanValue.BOOL_NONE', tt: 'lbl.BooleanValue_BOOL_NONE'},
		BOOL_TRUE:{value:1, label:'True', rbkey:'xa.enum.BooleanValue.BOOL_TRUE', tt: 'lbl.BooleanValue_BOOL_TRUE'},
		BOOL_FALSE:{value:2, label:'False', rbkey:'xa.enum.BooleanValue.BOOL_FALSE', tt: 'lbl.BooleanValue_BOOL_FALSE'}
	});

	XAEnums.ClassTypes = mergeParams(XAEnums.ClassTypes, {
		CLASS_TYPE_NONE:{value:0, label:'None', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_NONE', tt: 'lbl.ClassTypes_CLASS_TYPE_NONE'},
		CLASS_TYPE_MESSAGE:{value:1, label:'Message', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_MESSAGE', modelName:'VXMessage', type:'vXMessage', tt: 'lbl.ClassTypes_CLASS_TYPE_MESSAGE'},
		CLASS_TYPE_USER_PROFILE:{value:2, label:'User Profile', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_USER_PROFILE', modelName:'VXPortalUser', type:'vXPortalUser', tt: 'lbl.ClassTypes_CLASS_TYPE_USER_PROFILE'},
		CLASS_TYPE_AUTH_SESS:{value:3, label:'Authentication Session', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_AUTH_SESS', modelName:'VXAuthSession', type:'vXAuthSession', tt: 'lbl.ClassTypes_CLASS_TYPE_AUTH_SESS'},
		CLASS_TYPE_DATA_OBJECT:{value:4, label:'CLASS_TYPE_DATA_OBJECT', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_DATA_OBJECT', modelName:'VXDataObject', type:'vXDataObject', tt: 'lbl.ClassTypes_CLASS_TYPE_DATA_OBJECT'},
		CLASS_TYPE_NAMEVALUE:{value:5, label:'CLASS_TYPE_NAMEVALUE', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_NAMEVALUE', tt: 'lbl.ClassTypes_CLASS_TYPE_NAMEVALUE'},
		CLASS_TYPE_LONG:{value:6, label:'CLASS_TYPE_LONG', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_LONG', modelName:'VXLong', type:'vXLong', tt: 'lbl.ClassTypes_CLASS_TYPE_LONG'},
		CLASS_TYPE_PASSWORD_CHANGE:{value:7, label:'Password Change', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_PASSWORD_CHANGE', modelName:'VXPasswordChange', type:'vXPasswordChange', tt: 'lbl.ClassTypes_CLASS_TYPE_PASSWORD_CHANGE'},
		CLASS_TYPE_STRING:{value:8, label:'CLASS_TYPE_STRING', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_STRING', modelName:'VXString', type:'vXString', tt: 'lbl.ClassTypes_CLASS_TYPE_STRING'},
		CLASS_TYPE_ENUM:{value:9, label:'CLASS_TYPE_ENUM', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_ENUM', tt: 'lbl.ClassTypes_CLASS_TYPE_ENUM'},
		CLASS_TYPE_ENUM_ELEMENT:{value:10, label:'CLASS_TYPE_ENUM_ELEMENT', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_ENUM_ELEMENT', tt: 'lbl.ClassTypes_CLASS_TYPE_ENUM_ELEMENT'},
		CLASS_TYPE_RESPONSE:{value:11, label:'Response', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_RESPONSE', modelName:'VXResponse', type:'vXResponse', tt: 'lbl.ClassTypes_CLASS_TYPE_RESPONSE'},
		CLASS_TYPE_XA_ASSET:{value:1000, label:'Asset', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_ASSET', modelName:'VXAsset', type:'vXAsset', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_ASSET'},
		CLASS_TYPE_XA_RESOURCE:{value:1001, label:'Resource', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_RESOURCE', modelName:'VXResource', type:'vXResource', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_RESOURCE'},
		CLASS_TYPE_XA_GROUP:{value:1002, label:'Ranger Group', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_GROUP', modelName:'VXGroup', type:'vXGroup', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_GROUP'},
		CLASS_TYPE_XA_USER:{value:1003, label:'Ranger User', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_USER', modelName:'VXUser', type:'vXUser', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_USER'},
	
		CLASS_TYPE_XA_GROUP_USER:{value:1004, label:'XA Group of Users', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_GROUP_USER', modelName:'VXGroupUser', type:'vXGroupUser', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_GROUP_USER'},
		CLASS_TYPE_XA_GROUP_GROUP:{value:1005, label:'XA Group of groups', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_GROUP_GROUP', modelName:'VXGroupGroup', type:'vXGroupGroup', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_GROUP_GROUP'},
		CLASS_TYPE_XA_PERM_MAP:{value:1006, label:'XA permissions for resource', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_PERM_MAP', modelName:'VXPermMap', type:'vXPermMap', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_PERM_MAP'},
		CLASS_TYPE_XA_AUDIT_MAP:{value:1007, label:'XA audits for resource', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_AUDIT_MAP', modelName:'VXAuditMap', type:'vXAuditMap', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_AUDIT_MAP'},
		CLASS_TYPE_XA_CRED_STORE:{value:1008, label:'XA credential store', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_CRED_STORE', modelName:'VXCredentialStore', type:'vXCredentialStore', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_CRED_STORE'},
		CLASS_TYPE_XA_POLICY_EXPORT_AUDIT:{value:1009, label:'XA Policy Export Audit', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_POLICY_EXPORT_AUDIT', modelName:'VXPolicyExportAudit', type:'vXPolicyExportAudit', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_POLICY_EXPORT_AUDIT'},
		CLASS_TYPE_TRX_LOG:{value:1010, label:'Transaction log', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_TRX_LOG', tt: 'lbl.ClassTypes_CLASS_TYPE_TRX_LOG'},
		CLASS_TYPE_XA_ACCESS_AUDIT:{value:1011, label:'Access Audit', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_ACCESS_AUDIT', modelName:'VXAccessAudit', type:'vXAccessAudit', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_ACCESS_AUDIT'},
		CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE:{value:1012, label:'Transaction log attribute', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE', tt: 'lbl.ClassTypes_CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE'},
		CLASS_TYPE_RANGER_POLICY:{value:1020, label:'Ranger Policy', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_RANGER_POLICY', modelName:'VXRangerPolicy', type:'vXResource', tt: 'lbl.ClassTypes_CLASS_TYPE_RANGER_POLICY'},
		CLASS_TYPE_RANGER_SERVICE:{value:1030, label:'Ranger Service', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_RANGER_SERVICE', modelName:'VXRangerService', type:'vXRangerService', tt: 'lbl.ClassTypes_CLASS_TYPE_RANGER_SERVICE'},
                CLASS_TYPE_RANGER_SECURITY_ZONE:{value:1056, label:'Ranger Security Zone', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE', modelName:'VXRangerService', type:'vXRangerService', tt: 'lbl.ClassTypes_CLASS_TYPE_RANGER_SECURITY_ZONE'},
                CLASS_TYPE_RANGER_ROLE:{value:1057, label:'Ranger Role', rbkey:'xa.enum.ClassTypes.CLASS_TYPE_RANGER_ROLE', modelName:'VXRole', type:'vXRole', tt: 'lbl.ClassTypes_CLASS_TYPE_RANGER_ROLE'}
	});

	XAEnums.DataType = mergeParams(XAEnums.DataType, {
		DATA_TYPE_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.DataType.DATA_TYPE_UNKNOWN', tt: 'lbl.DataType_DATA_TYPE_UNKNOWN'},
		DATA_TYPE_INTEGER:{value:1, label:'Integer', rbkey:'xa.enum.DataType.DATA_TYPE_INTEGER', tt: 'lbl.DataType_DATA_TYPE_INTEGER'},
		DATA_TYPE_DOUBLE:{value:2, label:'Double', rbkey:'xa.enum.DataType.DATA_TYPE_DOUBLE', tt: 'lbl.DataType_DATA_TYPE_DOUBLE'},
		DATA_TYPE_STRING:{value:3, label:'String', rbkey:'xa.enum.DataType.DATA_TYPE_STRING', tt: 'lbl.DataType_DATA_TYPE_STRING'},
		DATA_TYPE_BOOLEAN:{value:4, label:'Boolean', rbkey:'xa.enum.DataType.DATA_TYPE_BOOLEAN', tt: 'lbl.DataType_DATA_TYPE_BOOLEAN'},
		DATA_TYPE_DATE:{value:5, label:'Date', rbkey:'xa.enum.DataType.DATA_TYPE_DATE', tt: 'lbl.DataType_DATA_TYPE_DATE'},
		DATA_TYPE_STRING_ENUM:{value:6, label:'String enumeration', rbkey:'xa.enum.DataType.DATA_TYPE_STRING_ENUM', tt: 'lbl.DataType_DATA_TYPE_STRING_ENUM'},
		DATA_TYPE_LONG:{value:7, label:'Long', rbkey:'xa.enum.DataType.DATA_TYPE_LONG', tt: 'lbl.DataType_DATA_TYPE_LONG'},
		DATA_TYPE_INTEGER_ENUM:{value:8, label:'Integer enumeration', rbkey:'xa.enum.DataType.DATA_TYPE_INTEGER_ENUM', tt: 'lbl.DataType_DATA_TYPE_INTEGER_ENUM'}
	});

	XAEnums.DeviceType = mergeParams(XAEnums.DeviceType, {
		DEVICE_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.DeviceType.DEVICE_UNKNOWN', tt: 'lbl.DeviceType_DEVICE_UNKNOWN'},
		DEVICE_BROWSER:{value:1, label:'Browser', rbkey:'xa.enum.DeviceType.DEVICE_BROWSER', tt: 'lbl.DeviceType_DEVICE_BROWSER'},
		DEVICE_IPHONE:{value:2, label:'iPhone', rbkey:'xa.enum.DeviceType.DEVICE_IPHONE', tt: 'lbl.DeviceType_DEVICE_IPHONE'},
		DEVICE_IPAD:{value:3, label:'iPad', rbkey:'xa.enum.DeviceType.DEVICE_IPAD', tt: 'lbl.DeviceType_DEVICE_IPAD'},
		DEVICE_IPOD:{value:4, label:'iPod', rbkey:'xa.enum.DeviceType.DEVICE_IPOD', tt: 'lbl.DeviceType_DEVICE_IPOD'},
		DEVICE_ANDROID:{value:5, label:'Android', rbkey:'xa.enum.DeviceType.DEVICE_ANDROID', tt: 'lbl.DeviceType_DEVICE_ANDROID'}
	});

	XAEnums.DiffLevel = mergeParams(XAEnums.DiffLevel, {
		DIFF_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.DiffLevel.DIFF_UNKNOWN', tt: 'lbl.DiffLevel_DIFF_UNKNOWN'},
		DIFF_LOW:{value:1, label:'Low', rbkey:'xa.enum.DiffLevel.DIFF_LOW', tt: 'lbl.DiffLevel_DIFF_LOW'},
		DIFF_MEDIUM:{value:2, label:'Medium', rbkey:'xa.enum.DiffLevel.DIFF_MEDIUM', tt: 'lbl.DiffLevel_DIFF_MEDIUM'},
		DIFF_HIGH:{value:3, label:'High', rbkey:'xa.enum.DiffLevel.DIFF_HIGH', tt: 'lbl.DiffLevel_DIFF_HIGH'}
	});

	XAEnums.FileType = mergeParams(XAEnums.FileType, {
		FILE_FILE:{value:0, label:'File', rbkey:'xa.enum.FileType.FILE_FILE', tt: 'lbl.FileType_FILE_FILE'},
		FILE_DIR:{value:1, label:'Directory', rbkey:'xa.enum.FileType.FILE_DIR', tt: 'lbl.FileType_FILE_DIR'}
	});

	XAEnums.FreqType = mergeParams(XAEnums.FreqType, {
		FREQ_NONE:{value:0, label:'None', rbkey:'xa.enum.FreqType.FREQ_NONE', tt: 'lbl.FreqType_FREQ_NONE'},
		FREQ_MANUAL:{value:1, label:'Manual', rbkey:'xa.enum.FreqType.FREQ_MANUAL', tt: 'lbl.FreqType_FREQ_MANUAL'},
		FREQ_HOURLY:{value:2, label:'Hourly', rbkey:'xa.enum.FreqType.FREQ_HOURLY', tt: 'lbl.FreqType_FREQ_HOURLY'},
		FREQ_DAILY:{value:3, label:'Daily', rbkey:'xa.enum.FreqType.FREQ_DAILY', tt: 'lbl.FreqType_FREQ_DAILY'},
		FREQ_WEEKLY:{value:4, label:'Weekly', rbkey:'xa.enum.FreqType.FREQ_WEEKLY', tt: 'lbl.FreqType_FREQ_WEEKLY'},
		FREQ_BI_WEEKLY:{value:5, label:'Bi Weekly', rbkey:'xa.enum.FreqType.FREQ_BI_WEEKLY', tt: 'lbl.FreqType_FREQ_BI_WEEKLY'},
		FREQ_MONTHLY:{value:6, label:'Monthly', rbkey:'xa.enum.FreqType.FREQ_MONTHLY', tt: 'lbl.FreqType_FREQ_MONTHLY'}
	});

	XAEnums.MimeType = mergeParams(XAEnums.MimeType, {
		MIME_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.MimeType.MIME_UNKNOWN', tt: 'lbl.MimeType_MIME_UNKNOWN'},
		MIME_TEXT:{value:1, label:'Text', rbkey:'xa.enum.MimeType.MIME_TEXT', tt: 'lbl.MimeType_MIME_TEXT'},
		MIME_HTML:{value:2, label:'Html', rbkey:'xa.enum.MimeType.MIME_HTML', tt: 'lbl.MimeType_MIME_HTML'},
		MIME_PNG:{value:3, label:'png', rbkey:'xa.enum.MimeType.MIME_PNG', tt: 'lbl.MimeType_MIME_PNG'},
		MIME_JPEG:{value:4, label:'jpeg', rbkey:'xa.enum.MimeType.MIME_JPEG', tt: 'lbl.MimeType_MIME_JPEG'}
	});

	XAEnums.NumberFormat = mergeParams(XAEnums.NumberFormat, {
		NUM_FORMAT_NONE:{value:0, label:'None', rbkey:'xa.enum.NumberFormat.NUM_FORMAT_NONE', tt: 'lbl.NumberFormat_NUM_FORMAT_NONE'},
		NUM_FORMAT_NUMERIC:{value:1, label:'Numeric', rbkey:'xa.enum.NumberFormat.NUM_FORMAT_NUMERIC', tt: 'lbl.NumberFormat_NUM_FORMAT_NUMERIC'},
		NUM_FORMAT_ALPHA:{value:2, label:'Alphabhet', rbkey:'xa.enum.NumberFormat.NUM_FORMAT_ALPHA', tt: 'lbl.NumberFormat_NUM_FORMAT_ALPHA'},
		NUM_FORMAT_ROMAN:{value:3, label:'Roman', rbkey:'xa.enum.NumberFormat.NUM_FORMAT_ROMAN', tt: 'lbl.NumberFormat_NUM_FORMAT_ROMAN'}
	});

	XAEnums.ObjectStatus = mergeParams(XAEnums.ObjectStatus, {
		OBJ_STATUS_ACTIVE:{value:0, label:'Active', rbkey:'xa.enum.ObjectStatus.OBJ_STATUS_ACTIVE', tt: 'lbl.ObjectStatus_OBJ_STATUS_ACTIVE'},
		OBJ_STATUS_DELETED:{value:1, label:'Deleted', rbkey:'xa.enum.ObjectStatus.OBJ_STATUS_DELETED', tt: 'lbl.ObjectStatus_OBJ_STATUS_DELETED'},
		OBJ_STATUS_ARCHIVED:{value:2, label:'Archived', rbkey:'xa.enum.ObjectStatus.OBJ_STATUS_ARCHIVED', tt: 'lbl.ObjectStatus_OBJ_STATUS_ARCHIVED'}
	});

	XAEnums.PasswordResetStatus = mergeParams(XAEnums.PasswordResetStatus, {
		PWD_RESET_ACTIVE:{value:0, label:'Active', rbkey:'xa.enum.PasswordResetStatus.PWD_RESET_ACTIVE', tt: 'lbl.PasswordResetStatus_PWD_RESET_ACTIVE'},
		PWD_RESET_USED:{value:1, label:'Used', rbkey:'xa.enum.PasswordResetStatus.PWD_RESET_USED', tt: 'lbl.PasswordResetStatus_PWD_RESET_USED'},
		PWD_RESET_EXPIRED:{value:2, label:'Expired', rbkey:'xa.enum.PasswordResetStatus.PWD_RESET_EXPIRED', tt: 'lbl.PasswordResetStatus_PWD_RESET_EXPIRED'},
		PWD_RESET_DISABLED:{value:3, label:'Disabled', rbkey:'xa.enum.PasswordResetStatus.PWD_RESET_DISABLED', tt: 'lbl.PasswordResetStatus_PWD_RESET_DISABLED'}
	});

	XAEnums.PriorityType = mergeParams(XAEnums.PriorityType, {
		PRIORITY_NORMAL:{value:0, label:'Normal', rbkey:'xa.enum.PriorityType.PRIORITY_NORMAL', tt: 'lbl.PriorityType_PRIORITY_NORMAL'},
		PRIORITY_LOW:{value:1, label:'Low', rbkey:'xa.enum.PriorityType.PRIORITY_LOW', tt: 'lbl.PriorityType_PRIORITY_LOW'},
		PRIORITY_MEDIUM:{value:2, label:'Medium', rbkey:'xa.enum.PriorityType.PRIORITY_MEDIUM', tt: 'lbl.PriorityType_PRIORITY_MEDIUM'},
		PRIORITY_HIGH:{value:3, label:'High', rbkey:'xa.enum.PriorityType.PRIORITY_HIGH', tt: 'lbl.PriorityType_PRIORITY_HIGH'}
	});

	XAEnums.ProgressStatus = mergeParams(XAEnums.ProgressStatus, {
		PROGRESS_PENDING:{value:0, label:'Pending', rbkey:'xa.enum.ProgressStatus.PROGRESS_PENDING', tt: 'lbl.ProgressStatus_PROGRESS_PENDING'},
		PROGRESS_IN_PROGRESS:{value:1, label:'In Progress', rbkey:'xa.enum.ProgressStatus.PROGRESS_IN_PROGRESS', tt: 'lbl.ProgressStatus_PROGRESS_IN_PROGRESS'},
		PROGRESS_COMPLETE:{value:2, label:'Complete', rbkey:'xa.enum.ProgressStatus.PROGRESS_COMPLETE', tt: 'lbl.ProgressStatus_PROGRESS_COMPLETE'},
		PROGRESS_ABORTED:{value:3, label:'Aborted', rbkey:'xa.enum.ProgressStatus.PROGRESS_ABORTED', tt: 'lbl.ProgressStatus_PROGRESS_ABORTED'},
		PROGRESS_FAILED:{value:4, label:'Failed', rbkey:'xa.enum.ProgressStatus.PROGRESS_FAILED', tt: 'lbl.ProgressStatus_PROGRESS_FAILED'}
	});

	XAEnums.RelationType = mergeParams(XAEnums.RelationType, {
		REL_NONE:{value:0, label:'None', rbkey:'xa.enum.RelationType.REL_NONE', tt: 'lbl.RelationType_REL_NONE'},
		REL_SELF:{value:1, label:'Self', rbkey:'xa.enum.RelationType.REL_SELF', tt: 'lbl.RelationType_REL_SELF'}
	});

	XAEnums.ResourceType = mergeParams(XAEnums.ResourceType, {
		RESOURCE_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.ResourceType.RESOURCE_UNKNOWN', tt: 'lbl.ResourceType_RESOURCE_UNKNOWN'},
		RESOURCE_PATH:{value:1, label:'Path', rbkey:'xa.enum.ResourceType.RESOURCE_PATH', tt: 'lbl.ResourceType_RESOURCE_PATH'},
		RESOURCE_DB:{value:2, label:'Database', rbkey:'xa.enum.ResourceType.RESOURCE_DB', tt: 'lbl.ResourceType_RESOURCE_DB'},
		RESOURCE_TABLE:{value:3, label:'Table', rbkey:'xa.enum.ResourceType.RESOURCE_TABLE', tt: 'lbl.ResourceType_RESOURCE_TABLE'},
		RESOURCE_COL_FAM:{value:4, label:'Column Family', rbkey:'xa.enum.ResourceType.RESOURCE_COL_FAM', tt: 'lbl.ResourceType_RESOURCE_COL_FAM'},
		RESOURCE_COLUMN:{value:5, label:'Column', rbkey:'xa.enum.ResourceType.RESOURCE_COLUMN', tt: 'lbl.ResourceType_RESOURCE_COLUMN'},
		RESOURCE_VIEW:{value:6, label:'VIEW', rbkey:'xa.enum.ResourceType.RESOURCE_VIEW', tt: 'lbl.ResourceType_RESOURCE_VIEW'},
                RESOURCE_UDF:{value:7, label:'Udf', rbkey:'xa.enum.ResourceType.RESOURCE_UDF', tt: 'lbl.ResourceType_RESOURCE_UDF'},
		RESOURCE_VIEW_COL:{value:8, label:'View Column', rbkey:'xa.enum.ResourceType.RESOURCE_VIEW_COL', tt: 'lbl.ResourceType_RESOURCE_VIEW_COL'},
		RESOURCE_TOPOLOGY:{value:9, label:'Topology', rbkey:'xa.enum.ResourceType.RESOURCE_TOPOLOGY', tt: 'lbl.RESOURCE_TOPOLOGY'},
		RESOURCE_SERVICE:{value:10, label:'Service', rbkey:'xa.enum.ResourceType.RESOURCE_SERVICE', tt: 'lbl.RESOURCE_SERVICE'},
		RESOURCE_GLOBAL:{value:11, label:'Global', rbkey:'xa.enum.ResourceType.RESOURCE_GLOBAL', tt: 'lbl.RESOURCE_GLOBAL'}
	});

	XAEnums.ResponseStatus = mergeParams(XAEnums.ResponseStatus, {
		STATUS_SUCCESS:{value:0, label:'Success', rbkey:'xa.enum.ResponseStatus.STATUS_SUCCESS', tt: 'lbl.ResponseStatus_STATUS_SUCCESS'},
		STATUS_ERROR:{value:1, label:'Error', rbkey:'xa.enum.ResponseStatus.STATUS_ERROR', tt: 'lbl.ResponseStatus_STATUS_ERROR'},
		STATUS_VALIDATION:{value:2, label:'Validation Error', rbkey:'xa.enum.ResponseStatus.STATUS_VALIDATION', tt: 'lbl.ResponseStatus_STATUS_VALIDATION'},
		STATUS_WARN:{value:3, label:'Warning', rbkey:'xa.enum.ResponseStatus.STATUS_WARN', tt: 'lbl.ResponseStatus_STATUS_WARN'},
		STATUS_INFO:{value:4, label:'Information', rbkey:'xa.enum.ResponseStatus.STATUS_INFO', tt: 'lbl.ResponseStatus_STATUS_INFO'},
		STATUS_PARTIAL_SUCCESS:{value:5, label:'Partial Success', rbkey:'xa.enum.ResponseStatus.STATUS_PARTIAL_SUCCESS', tt: 'lbl.ResponseStatus_STATUS_PARTIAL_SUCCESS'}
	});

	XAEnums.UserSource = mergeParams(XAEnums.UserSource, {
		USER_APP:{value:0, label:'Application', rbkey:'xa.enum.UserSource.USER_APP', tt: 'lbl.UserSource_USER_APP'},
		USER_GOOGLE:{value:1, label:'Google', rbkey:'xa.enum.UserSource.USER_GOOGLE', tt: 'lbl.UserSource_USER_GOOGLE'},
		USER_FB:{value:2, label:'FaceBook', rbkey:'xa.enum.UserSource.USER_FB', tt: 'lbl.UserSource_USER_FB'}
	});

	XAEnums.XAAuditType = mergeParams(XAEnums.XAAuditType, {
		XA_AUDIT_TYPE_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_UNKNOWN', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_UNKNOWN'},
		XA_AUDIT_TYPE_ALL:{value:1, label:'All', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_ALL', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_ALL'},
		XA_AUDIT_TYPE_READ:{value:2, label:'Read', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_READ', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_READ'},
		XA_AUDIT_TYPE_WRITE:{value:3, label:'Write', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_WRITE', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_WRITE'},
		XA_AUDIT_TYPE_CREATE:{value:4, label:'Create', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_CREATE', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_CREATE'},
		XA_AUDIT_TYPE_DELETE:{value:5, label:'Delete', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_DELETE', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_DELETE'},
		XA_AUDIT_TYPE_LOGIN:{value:6, label:'Login', rbkey:'xa.enum.XAAuditType.XA_AUDIT_TYPE_LOGIN', tt: 'lbl.XAAuditType_XA_AUDIT_TYPE_LOGIN'}
	});

	XAEnums.XAGroupType = mergeParams(XAEnums.XAGroupType, {
		XA_GROUP_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.XAGroupType.XA_GROUP_UNKNOWN', tt: 'lbl.XAGroupType_XA_GROUP_UNKNOWN'},
		XA_GROUP_USER:{value:1, label:'User', rbkey:'xa.enum.XAGroupType.XA_GROUP_USER', tt: 'lbl.XAGroupType_XA_GROUP_USER'},
		XA_GROUP_GROUP:{value:2, label:'Group', rbkey:'xa.enum.XAGroupType.XA_GROUP_GROUP', tt: 'lbl.XAGroupType_XA_GROUP_GROUP'},
		XA_GROUP_ROLE:{value:3, label:'Role', rbkey:'xa.enum.XAGroupType.XA_GROUP_ROLE', tt: 'lbl.XAGroupType_XA_GROUP_ROLE'}
	});

	XAEnums.XAPermForType = mergeParams(XAEnums.XAPermForType, {
		XA_PERM_FOR_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.XAPermForType.XA_PERM_FOR_UNKNOWN', tt: 'lbl.XAPermForType_XA_PERM_FOR_UNKNOWN'},
		XA_PERM_FOR_USER:{value:1, label:'Permission for Users', rbkey:'xa.enum.XAPermForType.XA_PERM_FOR_USER', tt: 'lbl.XAPermForType_XA_PERM_FOR_USER'},
		XA_PERM_FOR_GROUP:{value:2, label:'Permission for Groups', rbkey:'xa.enum.XAPermForType.XA_PERM_FOR_GROUP', tt: 'lbl.XAPermForType_XA_PERM_FOR_GROUP'}
	});

	XAEnums.XAPermType = mergeParams(XAEnums.XAPermType, {
		XA_PERM_TYPE_UNKNOWN:{value:0, label:'Unknown', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_UNKNOWN', tt: 'lbl.XAPermType_XA_PERM_TYPE_UNKNOWN'},
		XA_PERM_TYPE_RESET:{value:1, label:'Reset', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_RESET', tt: 'lbl.XAPermType_XA_PERM_TYPE_RESET'},
		XA_PERM_TYPE_READ:{value:2, label:'Read', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_READ', tt: 'lbl.XAPermType_XA_PERM_TYPE_READ'},
		XA_PERM_TYPE_WRITE:{value:3, label:'Write', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_WRITE', tt: 'lbl.XAPermType_XA_PERM_TYPE_WRITE'},
		XA_PERM_TYPE_CREATE:{value:4, label:'Create', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_CREATE', tt: 'lbl.XAPermType_XA_PERM_TYPE_CREATE'},
		XA_PERM_TYPE_DELETE:{value:5, label:'Delete', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_DELETE', tt: 'lbl.XAPermType_XA_PERM_TYPE_DELETE'},
		XA_PERM_TYPE_ADMIN:{value:6, label:'Admin', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ADMIN', tt: 'lbl.XAPermType_XA_PERM_TYPE_ADMIN'},
		XA_PERM_TYPE_OBFUSCATE:{value:7, label:'Obfuscate', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_OBFUSCATE', tt: 'lbl.XAPermType_XA_PERM_TYPE_OBFUSCATE'},
		XA_PERM_TYPE_MASK:{value:8, label:'Mask', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_MASK', tt: 'lbl.XAPermType_XA_PERM_TYPE_MASK'},
		XA_PERM_TYPE_EXECUTE:{value:9, label:'Execute', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_EXECUTE', tt: 'lbl.XAPermType_XA_PERM_TYPE_EXECUTE'},
		XA_PERM_TYPE_SELECT:{value:10, label:'Select', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_SELECT', tt: 'lbl.XAPermType_XA_PERM_TYPE_SELECT'},
		XA_PERM_TYPE_UPDATE:{value:11, label:'Update', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_UPDATE', tt: 'lbl.XAPermType_XA_PERM_TYPE_UPDATE'},
		XA_PERM_TYPE_DROP:{value:12, label:'Drop', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_DROP', tt: 'lbl.XAPermType_XA_PERM_TYPE_DROP'},
		XA_PERM_TYPE_ALTER:{value:13, label:'Alter', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALTER', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALTER'},
		XA_PERM_TYPE_INDEX:{value:14, label:'Index', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_INDEX', tt: 'lbl.XAPermType_XA_PERM_TYPE_INDEX'},
		XA_PERM_TYPE_LOCK:{value:15, label:'Lock', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_LOCK', tt: 'lbl.XAPermType_XA_PERM_TYPE_LOCK'},
		XA_PERM_TYPE_ALL:{value:16, label:'All', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALL', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALL'},
		XA_PERM_TYPE_ALLOW:{value:17, label:'Allow', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_SUBMIT_TOPOLOGY:{value:18, label:'Submit Topology', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_FILE_UPLOAD:{value:19, label:'File Upload', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_GET_NIMBUS:{value:20, label:'Get Nimbus Conf', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_GET_CLUSTER_INFO:{value:21, label:'Get Cluster Info', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_FILE_DOWNLOAD:{value:22, label:'File Download', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_KILL_TOPOLOGY:{value:23, label:'Kill Topology', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_REBALANCE:{value:24, label:'Rebalance', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_ACTIVATE:{value:25, label:'Activate', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_DEACTIVATE:{value:26, label:'Deactivate', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_GET_TOPOLOGY_CONF:{value:27, label:'Get Topology Conf', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_GET_TOPOLOGY:{value:28, label:'Get Topology', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_GET_USER_TOPOLOGY:{value:29, label:'Get User Topology', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_GET_TOPOLOGY_INFO:{value:30, label:'Get Topology Info', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
		XA_PERM_TYPE_UPLOAD_NEW_CREDENTIAL:{value:31, label:'Upload New Credential', rbkey:'xa.enum.XAPermType.XA_PERM_TYPE_ALLOW', tt: 'lbl.XAPermType_XA_PERM_TYPE_ALLOW'},
	});

	XAEnums.PluginConfig = mergeParams(XAEnums.PluginConfig, {
		HDFS : {configName:"xasecure.add-hadoop-authorization"},
		YARN : {configName:"ranger.add-yarn-authorization"}
	})

	XAEnums.MenuPermissions =  mergeParams(XAEnums.MenuPermissions, {
                XA_RESOURCE_BASED_POLICIES:{value:1, label:'Resource Based Policies', rbkey:'xa.enum.MenuPermissions.XA_RESOURCE_BASED_POLICIES', tt: 'lbl.XAPermForType_XA_RESOURCE_BASED_POLICIES'},
                XA_USER_GROUPS:{value:2, label:'Users/Groups', rbkey:'xa.enum.MenuPermissions.XA_USER_GROUP', tt: 'lbl.XAPermForType_XA_USER_GROUPS'},
                XA_REPORTS:{value:3, label:'Reports', rbkey:'xa.enum.MenuPermissions.XA_REPORTS', tt: 'lbl.XAPermForType_XA_REPORTS'},
                XA_AUDITS:{value:4, label:'Audit', rbkey:'xa.enum.MenuPermissions.XA_AUDITS', tt: 'lbl.XAPermForType_XA_AUDITS'},
                XA_KEY_MANAGER:{value:5, label:'Key Manager', rbkey:'xa.enum.MenuPermissions.XA_KEY_MANAGER', tt: 'lbl.XAPermForType_XA_KEY_MANAGER'},
                XA_TAG_BASED_POLICIES:{value:6, label:'Tag Based Policies', rbkey:'xa.enum.MenuPermissions.XA_TAG_BASED_POLICIES', tt: 'lbl.XAPermForType_XA_TAG_BASED_POLICIES'}
	});

        XAEnums.UserSyncSource = mergeParams(XAEnums.UserSyncSource, {
                USER_SYNC_UNIX:{value:0, label:'Unix', rbkey:'xa.enum.UserSyncSource.USER_SYNC_UNIX', tt: 'lbl.USER_SYNC_UNIX'},
                USER_SYNC_LDAPAD:{value:1, label:'LDAP/AD', rbkey:'xa.enum.UserSyncSource.USER_SYNC_LDAPAD', tt: 'lbl.USER_SYNC_LDAPAD'},
                USER_SYNC_FILE:{value:2, label:'File', rbkey:'xa.enum.UserSyncSource.USER_SYNC_FILE', tt: 'lbl.USER_SYNC_FILE'}
        });

	return XAEnums;
});
