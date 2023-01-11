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

 package org.apache.ranger.common;

/**
 *
 */


public class RangerCommonEnums {

	/***************************************************************
	 * Enum values for AllowedPermission
	 **************************************************************/
	/**
	 * IS_ALLOWED is an element of enum AllowedPermission. Its value is "IS_ALLOWED".
	 */
	public static final int IS_ALLOWED = 1;

	/***************************************************************
	 * Enum values for VisibilityStatus
	 **************************************************************/
	/**
	 * IS_VISIBLE is an element of enum VisibilityStatus. Its value is "IS_VISIBLE".
	 */
	public static final int IS_VISIBLE = 1;
	/**
	 * IS_HIDDEN is an element of enum VisibilityStatus. Its value is "IS_HIDDEN".
	 */
	public static final int IS_HIDDEN = 0;

	/***************************************************************
	 * Enum values for ActiveStatus
	 **************************************************************/
	/**
	 * STATUS_DISABLED is an element of enum ActiveStatus. Its value is "STATUS_DISABLED".
	 */
	public static final int STATUS_DISABLED = 0;
	/**
	 * STATUS_ENABLED is an element of enum ActiveStatus. Its value is "STATUS_ENABLED".
	 */
	public static final int STATUS_ENABLED = 1;
	/**
	 * STATUS_DELETED is an element of enum ActiveStatus. Its value is "STATUS_DELETED".
	 */
	public static final int STATUS_DELETED = 2;

	/**
	 * Max value for enum ActiveStatus_MAX
	 */
	public static final int ActiveStatus_MAX = 2;


	/***************************************************************
	 * Enum values for ActivationStatus
	 **************************************************************/
	/**
	 * ACT_STATUS_DISABLED is an element of enum ActivationStatus. Its value is "ACT_STATUS_DISABLED".
	 */
	public static final int ACT_STATUS_DISABLED = 0;
	/**
	 * ACT_STATUS_ACTIVE is an element of enum ActivationStatus. Its value is "ACT_STATUS_ACTIVE".
	 */
	public static final int ACT_STATUS_ACTIVE = 1;
	/**
	 * ACT_STATUS_PENDING_APPROVAL is an element of enum ActivationStatus. Its value is "ACT_STATUS_PENDING_APPROVAL".
	 */
	public static final int ACT_STATUS_PENDING_APPROVAL = 2;
	/**
	 * ACT_STATUS_PENDING_ACTIVATION is an element of enum ActivationStatus. Its value is "ACT_STATUS_PENDING_ACTIVATION".
	 */
	public static final int ACT_STATUS_PENDING_ACTIVATION = 3;
	/**
	 * ACT_STATUS_REJECTED is an element of enum ActivationStatus. Its value is "ACT_STATUS_REJECTED".
	 */
	public static final int ACT_STATUS_REJECTED = 4;
	/**
	 * ACT_STATUS_DEACTIVATED is an element of enum ActivationStatus. Its value is "ACT_STATUS_DEACTIVATED".
	 */
	public static final int ACT_STATUS_DEACTIVATED = 5;
	/**
	 * ACT_STATUS_PRE_REGISTRATION is an element of enum ActivationStatus. Its value is "ACT_STATUS_PRE_REGISTRATION".
	 */
	public static final int ACT_STATUS_PRE_REGISTRATION = 6;
	/**
	 * ACT_STATUS_NO_LOGIN is an element of enum ActivationStatus. Its value is "ACT_STATUS_NO_LOGIN".
	 */
	public static final int ACT_STATUS_NO_LOGIN = 7;

	/**
	 * Max value for enum ActivationStatus_MAX
	 */
	public static final int ActivationStatus_MAX = 7;


	/***************************************************************
	 * Enum values for BooleanValue
	 **************************************************************/
	/**
	 * BOOL_NONE is an element of enum BooleanValue. Its value is "BOOL_NONE".
	 */
	public static final int BOOL_NONE = 0;
	/**
	 * BOOL_TRUE is an element of enum BooleanValue. Its value is "BOOL_TRUE".
	 */
	public static final int BOOL_TRUE = 1;
	/**
	 * BOOL_FALSE is an element of enum BooleanValue. Its value is "BOOL_FALSE".
	 */
	public static final int BOOL_FALSE = 2;

	/**
	 * Max value for enum BooleanValue_MAX
	 */
	public static final int BooleanValue_MAX = 2;


	/***************************************************************
	 * Enum values for DataType
	 **************************************************************/
	/**
	 * DATA_TYPE_UNKNOWN is an element of enum DataType. Its value is "DATA_TYPE_UNKNOWN".
	 */
	public static final int DATA_TYPE_UNKNOWN = 0;
	/**
	 * DATA_TYPE_INTEGER is an element of enum DataType. Its value is "DATA_TYPE_INTEGER".
	 */
	public static final int DATA_TYPE_INTEGER = 1;
	/**
	 * DATA_TYPE_DOUBLE is an element of enum DataType. Its value is "DATA_TYPE_DOUBLE".
	 */
	public static final int DATA_TYPE_DOUBLE = 2;
	/**
	 * DATA_TYPE_STRING is an element of enum DataType. Its value is "DATA_TYPE_STRING".
	 */
	public static final int DATA_TYPE_STRING = 3;
	/**
	 * DATA_TYPE_BOOLEAN is an element of enum DataType. Its value is "DATA_TYPE_BOOLEAN".
	 */
	public static final int DATA_TYPE_BOOLEAN = 4;
	/**
	 * DATA_TYPE_DATE is an element of enum DataType. Its value is "DATA_TYPE_DATE".
	 */
	public static final int DATA_TYPE_DATE = 5;
	/**
	 * DATA_TYPE_STRING_ENUM is an element of enum DataType. Its value is "DATA_TYPE_STRING_ENUM".
	 */
	public static final int DATA_TYPE_STRING_ENUM = 6;
	/**
	 * DATA_TYPE_LONG is an element of enum DataType. Its value is "DATA_TYPE_LONG".
	 */
	public static final int DATA_TYPE_LONG = 7;
	/**
	 * DATA_TYPE_INTEGER_ENUM is an element of enum DataType. Its value is "DATA_TYPE_INTEGER_ENUM".
	 */
	public static final int DATA_TYPE_INTEGER_ENUM = 8;

	/**
	 * Max value for enum DataType_MAX
	 */
	public static final int DataType_MAX = 8;


	/***************************************************************
	 * Enum values for DeviceType
	 **************************************************************/
	/**
	 * DEVICE_UNKNOWN is an element of enum DeviceType. Its value is "DEVICE_UNKNOWN".
	 */
	public static final int DEVICE_UNKNOWN = 0;
	/**
	 * DEVICE_BROWSER is an element of enum DeviceType. Its value is "DEVICE_BROWSER".
	 */
	public static final int DEVICE_BROWSER = 1;
	/**
	 * DEVICE_IPHONE is an element of enum DeviceType. Its value is "DEVICE_IPHONE".
	 */
	public static final int DEVICE_IPHONE = 2;
	/**
	 * DEVICE_IPAD is an element of enum DeviceType. Its value is "DEVICE_IPAD".
	 */
	public static final int DEVICE_IPAD = 3;
	/**
	 * DEVICE_IPOD is an element of enum DeviceType. Its value is "DEVICE_IPOD".
	 */
	public static final int DEVICE_IPOD = 4;
	/**
	 * DEVICE_ANDROID is an element of enum DeviceType. Its value is "DEVICE_ANDROID".
	 */
	public static final int DEVICE_ANDROID = 5;

	/**
	 * Max value for enum DeviceType_MAX
	 */
	public static final int DeviceType_MAX = 5;


	/***************************************************************
	 * Enum values for DiffLevel
	 **************************************************************/
	/**
	 * DIFF_UNKNOWN is an element of enum DiffLevel. Its value is "DIFF_UNKNOWN".
	 */
	public static final int DIFF_UNKNOWN = 0;
	/**
	 * DIFF_LOW is an element of enum DiffLevel. Its value is "DIFF_LOW".
	 */
	public static final int DIFF_LOW = 1;
	/**
	 * DIFF_MEDIUM is an element of enum DiffLevel. Its value is "DIFF_MEDIUM".
	 */
	public static final int DIFF_MEDIUM = 2;
	/**
	 * DIFF_HIGH is an element of enum DiffLevel. Its value is "DIFF_HIGH".
	 */
	public static final int DIFF_HIGH = 3;

	/**
	 * Max value for enum DiffLevel_MAX
	 */
	public static final int DiffLevel_MAX = 3;


	/***************************************************************
	 * Enum values for FileType
	 **************************************************************/
	/**
	 * FILE_FILE is an element of enum FileType. Its value is "FILE_FILE".
	 */
	public static final int FILE_FILE = 0;
	/**
	 * FILE_DIR is an element of enum FileType. Its value is "FILE_DIR".
	 */
	public static final int FILE_DIR = 1;

	/**
	 * Max value for enum FileType_MAX
	 */
	public static final int FileType_MAX = 1;


	/***************************************************************
	 * Enum values for FreqType
	 **************************************************************/
	/**
	 * FREQ_NONE is an element of enum FreqType. Its value is "FREQ_NONE".
	 */
	public static final int FREQ_NONE = 0;
	/**
	 * FREQ_MANUAL is an element of enum FreqType. Its value is "FREQ_MANUAL".
	 */
	public static final int FREQ_MANUAL = 1;
	/**
	 * FREQ_HOURLY is an element of enum FreqType. Its value is "FREQ_HOURLY".
	 */
	public static final int FREQ_HOURLY = 2;
	/**
	 * FREQ_DAILY is an element of enum FreqType. Its value is "FREQ_DAILY".
	 */
	public static final int FREQ_DAILY = 3;
	/**
	 * FREQ_WEEKLY is an element of enum FreqType. Its value is "FREQ_WEEKLY".
	 */
	public static final int FREQ_WEEKLY = 4;
	/**
	 * FREQ_BI_WEEKLY is an element of enum FreqType. Its value is "FREQ_BI_WEEKLY".
	 */
	public static final int FREQ_BI_WEEKLY = 5;
	/**
	 * FREQ_MONTHLY is an element of enum FreqType. Its value is "FREQ_MONTHLY".
	 */
	public static final int FREQ_MONTHLY = 6;

	/**
	 * Max value for enum FreqType_MAX
	 */
	public static final int FreqType_MAX = 6;


	/***************************************************************
	 * Enum values for MimeType
	 **************************************************************/
	/**
	 * MIME_UNKNOWN is an element of enum MimeType. Its value is "MIME_UNKNOWN".
	 */
	public static final int MIME_UNKNOWN = 0;
	/**
	 * MIME_TEXT is an element of enum MimeType. Its value is "MIME_TEXT".
	 */
	public static final int MIME_TEXT = 1;
	/**
	 * MIME_HTML is an element of enum MimeType. Its value is "MIME_HTML".
	 */
	public static final int MIME_HTML = 2;
	/**
	 * MIME_PNG is an element of enum MimeType. Its value is "MIME_PNG".
	 */
	public static final int MIME_PNG = 3;
	/**
	 * MIME_JPEG is an element of enum MimeType. Its value is "MIME_JPEG".
	 */
	public static final int MIME_JPEG = 4;

	/**
	 * Max value for enum MimeType_MAX
	 */
	public static final int MimeType_MAX = 4;


	/***************************************************************
	 * Enum values for NumberFormat
	 **************************************************************/
	/**
	 * NUM_FORMAT_NONE is an element of enum NumberFormat. Its value is "NUM_FORMAT_NONE".
	 */
	public static final int NUM_FORMAT_NONE = 0;
	/**
	 * NUM_FORMAT_NUMERIC is an element of enum NumberFormat. Its value is "NUM_FORMAT_NUMERIC".
	 */
	public static final int NUM_FORMAT_NUMERIC = 1;
	/**
	 * NUM_FORMAT_ALPHA is an element of enum NumberFormat. Its value is "NUM_FORMAT_ALPHA".
	 */
	public static final int NUM_FORMAT_ALPHA = 2;
	/**
	 * NUM_FORMAT_ROMAN is an element of enum NumberFormat. Its value is "NUM_FORMAT_ROMAN".
	 */
	public static final int NUM_FORMAT_ROMAN = 3;

	/**
	 * Max value for enum NumberFormat_MAX
	 */
	public static final int NumberFormat_MAX = 3;


	/***************************************************************
	 * Enum values for ObjectStatus
	 **************************************************************/
	/**
	 * OBJ_STATUS_ACTIVE is an element of enum ObjectStatus. Its value is "OBJ_STATUS_ACTIVE".
	 */
	public static final int OBJ_STATUS_ACTIVE = 0;
	/**
	 * OBJ_STATUS_DELETED is an element of enum ObjectStatus. Its value is "OBJ_STATUS_DELETED".
	 */
	public static final int OBJ_STATUS_DELETED = 1;
	/**
	 * OBJ_STATUS_ARCHIVED is an element of enum ObjectStatus. Its value is "OBJ_STATUS_ARCHIVED".
	 */
	public static final int OBJ_STATUS_ARCHIVED = 2;

	/**
	 * Max value for enum ObjectStatus_MAX
	 */
	public static final int ObjectStatus_MAX = 2;


	/***************************************************************
	 * Enum values for PasswordResetStatus
	 **************************************************************/
	/**
	 * PWD_RESET_ACTIVE is an element of enum PasswordResetStatus. Its value is "PWD_RESET_ACTIVE".
	 */
	public static final int PWD_RESET_ACTIVE = 0;
	/**
	 * PWD_RESET_USED is an element of enum PasswordResetStatus. Its value is "PWD_RESET_USED".
	 */
	public static final int PWD_RESET_USED = 1;
	/**
	 * PWD_RESET_EXPIRED is an element of enum PasswordResetStatus. Its value is "PWD_RESET_EXPIRED".
	 */
	public static final int PWD_RESET_EXPIRED = 2;
	/**
	 * PWD_RESET_DISABLED is an element of enum PasswordResetStatus. Its value is "PWD_RESET_DISABLED".
	 */
	public static final int PWD_RESET_DISABLED = 3;

	/**
	 * Max value for enum PasswordResetStatus_MAX
	 */
	public static final int PasswordResetStatus_MAX = 3;


	/***************************************************************
	 * Enum values for PriorityType
	 **************************************************************/
	/**
	 * PRIORITY_NORMAL is an element of enum PriorityType. Its value is "PRIORITY_NORMAL".
	 */
	public static final int PRIORITY_NORMAL = 0;
	/**
	 * PRIORITY_LOW is an element of enum PriorityType. Its value is "PRIORITY_LOW".
	 */
	public static final int PRIORITY_LOW = 1;
	/**
	 * PRIORITY_MEDIUM is an element of enum PriorityType. Its value is "PRIORITY_MEDIUM".
	 */
	public static final int PRIORITY_MEDIUM = 2;
	/**
	 * PRIORITY_HIGH is an element of enum PriorityType. Its value is "PRIORITY_HIGH".
	 */
	public static final int PRIORITY_HIGH = 3;

	/**
	 * Max value for enum PriorityType_MAX
	 */
	public static final int PriorityType_MAX = 3;


	/***************************************************************
	 * Enum values for ProgressStatus
	 **************************************************************/
	/**
	 * PROGRESS_PENDING is an element of enum ProgressStatus. Its value is "PROGRESS_PENDING".
	 */
	public static final int PROGRESS_PENDING = 0;
	/**
	 * PROGRESS_IN_PROGRESS is an element of enum ProgressStatus. Its value is "PROGRESS_IN_PROGRESS".
	 */
	public static final int PROGRESS_IN_PROGRESS = 1;
	/**
	 * PROGRESS_COMPLETE is an element of enum ProgressStatus. Its value is "PROGRESS_COMPLETE".
	 */
	public static final int PROGRESS_COMPLETE = 2;
	/**
	 * PROGRESS_ABORTED is an element of enum ProgressStatus. Its value is "PROGRESS_ABORTED".
	 */
	public static final int PROGRESS_ABORTED = 3;
	/**
	 * PROGRESS_FAILED is an element of enum ProgressStatus. Its value is "PROGRESS_FAILED".
	 */
	public static final int PROGRESS_FAILED = 4;

	/**
	 * Max value for enum ProgressStatus_MAX
	 */
	public static final int ProgressStatus_MAX = 4;


	/***************************************************************
	 * Enum values for RelationType
	 **************************************************************/
	/**
	 * REL_NONE is an element of enum RelationType. Its value is "REL_NONE".
	 */
	public static final int REL_NONE = 0;
	/**
	 * REL_SELF is an element of enum RelationType. Its value is "REL_SELF".
	 */
	public static final int REL_SELF = 1;

	/**
	 * Max value for enum RelationType_MAX
	 */
	public static final int RelationType_MAX = 1;


	/***************************************************************
	 * Enum values for UserSource
	 **************************************************************/
	/**
	 * USER_APP is an element of enum UserSource. Its value is "USER_APP".
	 */
	public static final int USER_APP = 0;
	public static final int USER_EXTERNAL = 1;
	public static final int USER_AD= 2;
	public static final int USER_LDAP = 3;
	public static final int USER_UNIX = 4;
	public static final int USER_REPO = 5;
	
	public static final int GROUP_INTERNAL = 0;
	public static final int GROUP_EXTERNAL = 1;
	public static final int GROUP_AD= 2;
	public static final int GROUP_LDAP = 3;
	public static final int GROUP_UNIX = 4;
	public static final int GROUP_REPO = 5;
	/**
	 * USER_GOOGLE is an element of enum UserSource. Its value is "USER_GOOGLE".
	 */
	//public static final int USER_GOOGLE = 1;
	/**
	 * USER_FB is an element of enum UserSource. Its value is "USER_FB".
	 */
	//public static final int USER_FB = 2;

	/**
	 * Max value for enum UserSource_MAX
	 */
	public static final int UserSource_MAX = 5;


	/***************************************************************
	 * Enum values for AssetType
	 **************************************************************/
	/**
	 * ASSET_UNKNOWN is an element of enum AssetType. Its value is "ASSET_UNKNOWN".
	 */
	public static final int ASSET_UNKNOWN = 0;
	/**
	 * ASSET_HDFS is an element of enum AssetType. Its value is "ASSET_HDFS".
	 */
	public static final int ASSET_HDFS = 1;
	/**
	 * ASSET_HBASE is an element of enum AssetType. Its value is "ASSET_HBASE".
	 */
	public static final int ASSET_HBASE = 2;
	/**
	 * ASSET_HIVE is an element of enum AssetType. Its value is "ASSET_HIVE".
	 */
	public static final int ASSET_HIVE = 3;
	/**
	 * ASSET_HIVE is an element of enum AssetType. Its value is "ASSET_HIVE".
	 */
	public static final int ASSET_AGENT = 4;
	/**
	 * ASSET_HIVE is an element of enum AssetType. Its value is "ASSET_HIVE".
	 */
	public static final int ASSET_KNOX = 5;
	/**
	 * ASSET_HIVE is an element of enum AssetType. Its value is "ASSET_HIVE".
	 */
	public static final int ASSET_STORM = 6;

	/**
	 * Max value for enum AssetType_MAX
	 */
	public static final int AssetType_MAX = 6;


	/***************************************************************
	 * Enum values for AccessResult
	 **************************************************************/
	/**
	 * ACCESS_RESULT_DENIED is an element of enum AccessResult. Its value is "ACCESS_RESULT_DENIED".
	 */
	public static final int ACCESS_RESULT_DENIED = 0;
	/**
	 * ACCESS_RESULT_ALLOWED is an element of enum AccessResult. Its value is "ACCESS_RESULT_ALLOWED".
	 */
	public static final int ACCESS_RESULT_ALLOWED = 1;

	/**
	 * Max value for enum AccessResult_MAX
	 */
	public static final int AccessResult_MAX = 1;


	/***************************************************************
	 * Enum values for PolicyType
	 **************************************************************/
	/**
	 * POLICY_INCLUSION is an element of enum PolicyType. Its value is "POLICY_INCLUSION".
	 */
	public static final int POLICY_INCLUSION = 0;
	/**
	 * POLICY_EXCLUSION is an element of enum PolicyType. Its value is "POLICY_EXCLUSION".
	 */
	public static final int POLICY_EXCLUSION = 1;

	/**
	 * Max value for enum PolicyType_MAX
	 */
	public static final int PolicyType_MAX = 1;


	/***************************************************************
	 * Enum values for XAAuditType
	 **************************************************************/
	/**
	 * XA_AUDIT_TYPE_UNKNOWN is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_UNKNOWN".
	 */
	public static final int XA_AUDIT_TYPE_UNKNOWN = 0;
	/**
	 * XA_AUDIT_TYPE_ALL is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_ALL".
	 */
	public static final int XA_AUDIT_TYPE_ALL = 1;
	/**
	 * XA_AUDIT_TYPE_READ is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_READ".
	 */
	public static final int XA_AUDIT_TYPE_READ = 2;
	/**
	 * XA_AUDIT_TYPE_WRITE is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_WRITE".
	 */
	public static final int XA_AUDIT_TYPE_WRITE = 3;
	/**
	 * XA_AUDIT_TYPE_CREATE is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_CREATE".
	 */
	public static final int XA_AUDIT_TYPE_CREATE = 4;
	/**
	 * XA_AUDIT_TYPE_DELETE is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_DELETE".
	 */
	public static final int XA_AUDIT_TYPE_DELETE = 5;
	/**
	 * XA_AUDIT_TYPE_LOGIN is an element of enum XAAuditType. Its value is "XA_AUDIT_TYPE_LOGIN".
	 */
	public static final int XA_AUDIT_TYPE_LOGIN = 6;

	/**
	 * Max value for enum XAAuditType_MAX
	 */
	public static final int XAAuditType_MAX = 6;


	/***************************************************************
	 * Enum values for ResourceType
	 **************************************************************/
	/**
	 * RESOURCE_UNKNOWN is an element of enum ResourceType. Its value is "RESOURCE_UNKNOWN".
	 */
	public static final int RESOURCE_UNKNOWN = 0;
	/**
	 * RESOURCE_PATH is an element of enum ResourceType. Its value is "RESOURCE_PATH".
	 */
	public static final int RESOURCE_PATH = 1;
	/**
	 * RESOURCE_DB is an element of enum ResourceType. Its value is "RESOURCE_DB".
	 */
	public static final int RESOURCE_DB = 2;
	/**
	 * RESOURCE_TABLE is an element of enum ResourceType. Its value is "RESOURCE_TABLE".
	 */
	public static final int RESOURCE_TABLE = 3;
	/**
	 * RESOURCE_COL_FAM is an element of enum ResourceType. Its value is "RESOURCE_COL_FAM".
	 */
	public static final int RESOURCE_COL_FAM = 4;
	/**
	 * RESOURCE_COLUMN is an element of enum ResourceType. Its value is "RESOURCE_COLUMN".
	 */
	public static final int RESOURCE_COLUMN = 5;
	/**
	 * RESOURCE_VIEW is an element of enum ResourceType. Its value is "RESOURCE_VIEW".
	 */
	public static final int RESOURCE_VIEW = 6;
	/**
	 * RESOURCE_UDF is an element of enum ResourceType. Its value is "RESOURCE_UDF".
	 */
	public static final int RESOURCE_UDF = 7;
	/**
	 * RESOURCE_VIEW_COL is an element of enum ResourceType. Its value is "RESOURCE_VIEW_COL".
	 */
	public static final int RESOURCE_VIEW_COL = 8;

	/**
	 * Max value for enum ResourceType_MAX
	 */
	public static final int ResourceType_MAX = 8;


	/***************************************************************
	 * Enum values for XAGroupType
	 **************************************************************/
	/**
	 * XA_GROUP_UNKNOWN is an element of enum XAGroupType. Its value is "XA_GROUP_UNKNOWN".
	 */
	public static final int XA_GROUP_UNKNOWN = 0;
	/**
	 * XA_GROUP_USER is an element of enum XAGroupType. Its value is "XA_GROUP_USER".
	 */
	public static final int XA_GROUP_USER = 1;
	/**
	 * XA_GROUP_GROUP is an element of enum XAGroupType. Its value is "XA_GROUP_GROUP".
	 */
	public static final int XA_GROUP_GROUP = 2;
	/**
	 * XA_GROUP_ROLE is an element of enum XAGroupType. Its value is "XA_GROUP_ROLE".
	 */
	public static final int XA_GROUP_ROLE = 3;

	/**
	 * Max value for enum XAGroupType_MAX
	 */
	public static final int XAGroupType_MAX = 3;


	/***************************************************************
	 * Enum values for XAPermForType
	 **************************************************************/
	/**
	 * XA_PERM_FOR_UNKNOWN is an element of enum XAPermForType. Its value is "XA_PERM_FOR_UNKNOWN".
	 */
	public static final int XA_PERM_FOR_UNKNOWN = 0;
	/**
	 * XA_PERM_FOR_USER is an element of enum XAPermForType. Its value is "XA_PERM_FOR_USER".
	 */
	public static final int XA_PERM_FOR_USER = 1;
	/**
	 * XA_PERM_FOR_GROUP is an element of enum XAPermForType. Its value is "XA_PERM_FOR_GROUP".
	 */
	public static final int XA_PERM_FOR_GROUP = 2;

	/**
	 * Max value for enum XAPermForType_MAX
	 */
	public static final int XAPermForType_MAX = 2;


	/***************************************************************
	 * Enum values for XAPermType
	 **************************************************************/
	/**
	 * XA_PERM_TYPE_UNKNOWN is an element of enum XAPermType. Its value is "XA_PERM_TYPE_UNKNOWN".
	 */
	public static final int XA_PERM_TYPE_UNKNOWN = 0;
	/**
	 * XA_PERM_TYPE_RESET is an element of enum XAPermType. Its value is "XA_PERM_TYPE_RESET".
	 */
	public static final int XA_PERM_TYPE_RESET = 1;
	/**
	 * XA_PERM_TYPE_READ is an element of enum XAPermType. Its value is "XA_PERM_TYPE_READ".
	 */
	public static final int XA_PERM_TYPE_READ = 2;
	/**
	 * XA_PERM_TYPE_WRITE is an element of enum XAPermType. Its value is "XA_PERM_TYPE_WRITE".
	 */
	public static final int XA_PERM_TYPE_WRITE = 3;
	/**
	 * XA_PERM_TYPE_CREATE is an element of enum XAPermType. Its value is "XA_PERM_TYPE_CREATE".
	 */
	public static final int XA_PERM_TYPE_CREATE = 4;
	/**
	 * XA_PERM_TYPE_DELETE is an element of enum XAPermType. Its value is "XA_PERM_TYPE_DELETE".
	 */
	public static final int XA_PERM_TYPE_DELETE = 5;
	/**
	 * XA_PERM_TYPE_ADMIN is an element of enum XAPermType. Its value is "XA_PERM_TYPE_ADMIN".
	 */
	public static final int XA_PERM_TYPE_ADMIN = 6;
	/**
	 * XA_PERM_TYPE_OBFUSCATE is an element of enum XAPermType. Its value is "XA_PERM_TYPE_OBFUSCATE".
	 */
	public static final int XA_PERM_TYPE_OBFUSCATE = 7;
	/**
	 * XA_PERM_TYPE_MASK is an element of enum XAPermType. Its value is "XA_PERM_TYPE_MASK".
	 */
	public static final int XA_PERM_TYPE_MASK = 8;
	/**
	 * XA_PERM_TYPE_EXECUTE is an element of enum XAPermType. Its value is "XA_PERM_TYPE_EXECUTE".
	 */
	public static final int XA_PERM_TYPE_EXECUTE = 9;
	/**
	 * XA_PERM_TYPE_SELECT is an element of enum XAPermType. Its value is "XA_PERM_TYPE_SELECT".
	 */
	public static final int XA_PERM_TYPE_SELECT = 10;
	/**
	 * XA_PERM_TYPE_UPDATE is an element of enum XAPermType. Its value is "XA_PERM_TYPE_UPDATE".
	 */
	public static final int XA_PERM_TYPE_UPDATE = 11;
	/**
	 * XA_PERM_TYPE_DROP is an element of enum XAPermType. Its value is "XA_PERM_TYPE_DROP".
	 */
	public static final int XA_PERM_TYPE_DROP = 12;
	/**
	 * XA_PERM_TYPE_ALTER is an element of enum XAPermType. Its value is "XA_PERM_TYPE_ALTER".
	 */
	public static final int XA_PERM_TYPE_ALTER = 13;
	/**
	 * XA_PERM_TYPE_INDEX is an element of enum XAPermType. Its value is "XA_PERM_TYPE_INDEX".
	 */
	public static final int XA_PERM_TYPE_INDEX = 14;
	/**
	 * XA_PERM_TYPE_LOCK is an element of enum XAPermType. Its value is "XA_PERM_TYPE_LOCK".
	 */
	public static final int XA_PERM_TYPE_LOCK = 15;
	/**
	 * XA_PERM_TYPE_ALL is an element of enum XAPermType. Its value is "XA_PERM_TYPE_ALL".
	 */
	public static final int XA_PERM_TYPE_ALL = 16;

	/**
	 * XA_PERM_TYPE_ALL is an element of enum XAPermType. Its value is "XA_PERM_TYPE_ALLOW".
	 */
	public static final int XA_PERM_TYPE_ALLOW = 17;

	/**
	 * Max value for enum XAPermType_MAX
	 */
	public static final int XAPermType_MAX = 17;


	/***************************************************************
	 * Enum values for ClassTypes
	 **************************************************************/
	/**
	 * CLASS_TYPE_NONE is an element of enum ClassTypes. Its value is "CLASS_TYPE_NONE".
	 */
	public static final int CLASS_TYPE_NONE = 0;
	/**
	 * CLASS_TYPE_MESSAGE is an element of enum ClassTypes. Its value is "CLASS_TYPE_MESSAGE".
	 */
	public static final int CLASS_TYPE_MESSAGE = 1;
	/**
	 * CLASS_TYPE_USER_PROFILE is an element of enum ClassTypes. Its value is "CLASS_TYPE_USER_PROFILE".
	 */
	public static final int CLASS_TYPE_USER_PROFILE = 2;
	/**
	 * CLASS_TYPE_AUTH_SESS is an element of enum ClassTypes. Its value is "CLASS_TYPE_AUTH_SESS".
	 */
	public static final int CLASS_TYPE_AUTH_SESS = 3;
	/**
	 * CLASS_TYPE_DATA_OBJECT is an element of enum ClassTypes. Its value is "CLASS_TYPE_DATA_OBJECT".
	 */
	public static final int CLASS_TYPE_DATA_OBJECT = 4;
	/**
	 * CLASS_TYPE_NAMEVALUE is an element of enum ClassTypes. Its value is "CLASS_TYPE_NAMEVALUE".
	 */
	public static final int CLASS_TYPE_NAMEVALUE = 5;
	/**
	 * CLASS_TYPE_LONG is an element of enum ClassTypes. Its value is "CLASS_TYPE_LONG".
	 */
	public static final int CLASS_TYPE_LONG = 6;
	/**
	 * CLASS_TYPE_PASSWORD_CHANGE is an element of enum ClassTypes. Its value is "CLASS_TYPE_PASSWORD_CHANGE".
	 */
	public static final int CLASS_TYPE_PASSWORD_CHANGE = 7;
	/**
	 * CLASS_TYPE_STRING is an element of enum ClassTypes. Its value is "CLASS_TYPE_STRING".
	 */
	public static final int CLASS_TYPE_STRING = 8;
	/**
	 * CLASS_TYPE_ENUM is an element of enum ClassTypes. Its value is "CLASS_TYPE_ENUM".
	 */
	public static final int CLASS_TYPE_ENUM = 9;
	/**
	 * CLASS_TYPE_ENUM_ELEMENT is an element of enum ClassTypes. Its value is "CLASS_TYPE_ENUM_ELEMENT".
	 */
	public static final int CLASS_TYPE_ENUM_ELEMENT = 10;
	/**
	 * CLASS_TYPE_RESPONSE is an element of enum ClassTypes. Its value is "CLASS_TYPE_RESPONSE".
	 */
	public static final int CLASS_TYPE_RESPONSE = 11;
	/**
	 * CLASS_TYPE_XA_ASSET is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_ASSET".
	 */
	public static final int CLASS_TYPE_XA_ASSET = 1000;
	/**
	 * CLASS_TYPE_XA_RESOURCE is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_RESOURCE".
	 */
	public static final int CLASS_TYPE_XA_RESOURCE = 1001;
	/**
	 * CLASS_TYPE_XA_GROUP is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_GROUP".
	 */
	public static final int CLASS_TYPE_XA_GROUP = 1002;
	/**
	 * CLASS_TYPE_XA_USER is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_USER".
	 */
	public static final int CLASS_TYPE_XA_USER = 1003;
	/**
	 * CLASS_TYPE_XA_GROUP_USER is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_GROUP_USER".
	 */
	public static final int CLASS_TYPE_XA_GROUP_USER = 1004;
	/**
	 * CLASS_TYPE_XA_GROUP_GROUP is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_GROUP_GROUP".
	 */
	public static final int CLASS_TYPE_XA_GROUP_GROUP = 1005;
	/**
	 * CLASS_TYPE_XA_PERM_MAP is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_PERM_MAP".
	 */
	public static final int CLASS_TYPE_XA_PERM_MAP = 1006;
	/**
	 * CLASS_TYPE_XA_AUDIT_MAP is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_AUDIT_MAP".
	 */
	public static final int CLASS_TYPE_XA_AUDIT_MAP = 1007;
	/**
	 * CLASS_TYPE_XA_CRED_STORE is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_CRED_STORE".
	 */
	public static final int CLASS_TYPE_XA_CRED_STORE = 1008;
	/**
	 * CLASS_TYPE_XA_POLICY_EXPORT_AUDIT is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_POLICY_EXPORT_AUDIT".
	 */
	public static final int CLASS_TYPE_XA_POLICY_EXPORT_AUDIT = 1009;
	/**
	 * CLASS_TYPE_TRX_LOG is an element of enum ClassTypes. Its value is "CLASS_TYPE_TRX_LOG".
	 */
	public static final int CLASS_TYPE_TRX_LOG = 1010;
	/**
	 * CLASS_TYPE_XA_ACCESS_AUDIT is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_ACCESS_AUDIT".
	 */
	public static final int CLASS_TYPE_XA_ACCESS_AUDIT = 1011;
	/**
	 * CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE is an element of enum ClassTypes. Its value is "CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE".
	 */
	public static final int CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE = 1012;

	/**
	 * Max value for enum ClassTypes_MAX
	 */
	public static final int ClassTypes_MAX = 1012;

	static public String getLabelFor_VisibilityStatus( int elementValue ) {
		if( elementValue == 0 ) {
			return "Hidden"; //IS_HIDDEN
		}
		if( elementValue == 1 ) {
			return "Visible"; //IS_VISIBLE
		}
		return null;
	}

	static public String getLabelFor_ActiveStatus( int elementValue ) {
		if( elementValue == 0 ) {
			return "Disabled"; //STATUS_DISABLED
		}
		if( elementValue == 1 ) {
			return "Enabled"; //STATUS_ENABLED
		}
		if( elementValue == 2 ) {
			return "Deleted"; //STATUS_DELETED
		}
		return null;
	}

	static public String getLabelFor_ActivationStatus( int elementValue ) {
		if( elementValue == 0 ) {
			return "Disabled"; //ACT_STATUS_DISABLED
		}
		if( elementValue == 1 ) {
			return "Active"; //ACT_STATUS_ACTIVE
		}
		if( elementValue == 2 ) {
			return "Pending Approval"; //ACT_STATUS_PENDING_APPROVAL
		}
		if( elementValue == 3 ) {
			return "Pending Activation"; //ACT_STATUS_PENDING_ACTIVATION
		}
		if( elementValue == 4 ) {
			return "Rejected"; //ACT_STATUS_REJECTED
		}
		if( elementValue == 5 ) {
			return "Deactivated"; //ACT_STATUS_DEACTIVATED
		}
		if( elementValue == 6 ) {
			return "Registration Pending"; //ACT_STATUS_PRE_REGISTRATION
		}
		if( elementValue == 7 ) {
			return "No login privilege"; //ACT_STATUS_NO_LOGIN
		}
		return null;
	}

	static public String getLabelFor_BooleanValue( int elementValue ) {
		if( elementValue == 0 ) {
			return "None"; //BOOL_NONE
		}
		if( elementValue == 1 ) {
			return "True"; //BOOL_TRUE
		}
		if( elementValue == 2 ) {
			return "False"; //BOOL_FALSE
		}
		return null;
	}

	static public String getLabelFor_DataType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //DATA_TYPE_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Integer"; //DATA_TYPE_INTEGER
		}
		if( elementValue == 2 ) {
			return "Double"; //DATA_TYPE_DOUBLE
		}
		if( elementValue == 3 ) {
			return "String"; //DATA_TYPE_STRING
		}
		if( elementValue == 4 ) {
			return "Boolean"; //DATA_TYPE_BOOLEAN
		}
		if( elementValue == 5 ) {
			return "Date"; //DATA_TYPE_DATE
		}
		if( elementValue == 6 ) {
			return "String enumeration"; //DATA_TYPE_STRING_ENUM
		}
		if( elementValue == 7 ) {
			return "Long"; //DATA_TYPE_LONG
		}
		if( elementValue == 8 ) {
			return "Integer enumeration"; //DATA_TYPE_INTEGER_ENUM
		}
		return null;
	}

	static public String getLabelFor_DeviceType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //DEVICE_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Browser"; //DEVICE_BROWSER
		}
		if( elementValue == 2 ) {
			return "iPhone"; //DEVICE_IPHONE
		}
		if( elementValue == 3 ) {
			return "iPad"; //DEVICE_IPAD
		}
		if( elementValue == 4 ) {
			return "iPod"; //DEVICE_IPOD
		}
		if( elementValue == 5 ) {
			return "Android"; //DEVICE_ANDROID
		}
		return null;
	}

	static public String getLabelFor_DiffLevel( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //DIFF_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Low"; //DIFF_LOW
		}
		if( elementValue == 2 ) {
			return "Medium"; //DIFF_MEDIUM
		}
		if( elementValue == 3 ) {
			return "High"; //DIFF_HIGH
		}
		return null;
	}

	static public String getLabelFor_FileType( int elementValue ) {
		if( elementValue == 0 ) {
			return "File"; //FILE_FILE
		}
		if( elementValue == 1 ) {
			return "Directory"; //FILE_DIR
		}
		return null;
	}

	static public String getLabelFor_FreqType( int elementValue ) {
		if( elementValue == 0 ) {
			return "None"; //FREQ_NONE
		}
		if( elementValue == 1 ) {
			return "Manual"; //FREQ_MANUAL
		}
		if( elementValue == 2 ) {
			return "Hourly"; //FREQ_HOURLY
		}
		if( elementValue == 3 ) {
			return "Daily"; //FREQ_DAILY
		}
		if( elementValue == 4 ) {
			return "Weekly"; //FREQ_WEEKLY
		}
		if( elementValue == 5 ) {
			return "Bi Weekly"; //FREQ_BI_WEEKLY
		}
		if( elementValue == 6 ) {
			return "Monthly"; //FREQ_MONTHLY
		}
		return null;
	}

	static public String getLabelFor_MimeType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //MIME_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Text"; //MIME_TEXT
		}
		if( elementValue == 2 ) {
			return "Html"; //MIME_HTML
		}
		if( elementValue == 3 ) {
			return "png"; //MIME_PNG
		}
		if( elementValue == 4 ) {
			return "jpeg"; //MIME_JPEG
		}
		return null;
	}

	static public String getLabelFor_NumberFormat( int elementValue ) {
		if( elementValue == 0 ) {
			return "None"; //NUM_FORMAT_NONE
		}
		if( elementValue == 1 ) {
			return "Numeric"; //NUM_FORMAT_NUMERIC
		}
		if( elementValue == 2 ) {
			return "Alphabhet"; //NUM_FORMAT_ALPHA
		}
		if( elementValue == 3 ) {
			return "Roman"; //NUM_FORMAT_ROMAN
		}
		return null;
	}

	static public String getLabelFor_ObjectStatus( int elementValue ) {
		if( elementValue == 0 ) {
			return "Active"; //OBJ_STATUS_ACTIVE
		}
		if( elementValue == 1 ) {
			return "Deleted"; //OBJ_STATUS_DELETED
		}
		if( elementValue == 2 ) {
			return "Archived"; //OBJ_STATUS_ARCHIVED
		}
		return null;
	}

	static public String getLabelFor_PasswordResetStatus( int elementValue ) {
		if( elementValue == 0 ) {
			return "Active"; //PWD_RESET_ACTIVE
		}
		if( elementValue == 1 ) {
			return "Used"; //PWD_RESET_USED
		}
		if( elementValue == 2 ) {
			return "Expired"; //PWD_RESET_EXPIRED
		}
		if( elementValue == 3 ) {
			return "Disabled"; //PWD_RESET_DISABLED
		}
		return null;
	}

	static public String getLabelFor_PriorityType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Normal"; //PRIORITY_NORMAL
		}
		if( elementValue == 1 ) {
			return "Low"; //PRIORITY_LOW
		}
		if( elementValue == 2 ) {
			return "Medium"; //PRIORITY_MEDIUM
		}
		if( elementValue == 3 ) {
			return "High"; //PRIORITY_HIGH
		}
		return null;
	}

	static public String getLabelFor_ProgressStatus( int elementValue ) {
		if( elementValue == 0 ) {
			return "Pending"; //PROGRESS_PENDING
		}
		if( elementValue == 1 ) {
			return "In Progress"; //PROGRESS_IN_PROGRESS
		}
		if( elementValue == 2 ) {
			return "Complete"; //PROGRESS_COMPLETE
		}
		if( elementValue == 3 ) {
			return "Aborted"; //PROGRESS_ABORTED
		}
		if( elementValue == 4 ) {
			return "Failed"; //PROGRESS_FAILED
		}
		return null;
	}

	static public String getLabelFor_RelationType( int elementValue ) {
		if( elementValue == 0 ) {
			return "None"; //REL_NONE
		}
		if( elementValue == 1 ) {
			return "Self"; //REL_SELF
		}
		return null;
	}

	static public String getLabelFor_UserSource( int elementValue ) {
		if( elementValue == 0 ) {
			return "Application"; //USER_APP
		}
		if( elementValue == 1 ) {
			return "External"; //USER_EXTERNAL
		}
		/*if( elementValue == 1 ) {
			return "Google"; //USER_GOOGLE
		}
		if( elementValue == 2 ) {
			return "FaceBook"; //USER_FB
		}*/
		return null;
	}

	static public String getLabelFor_AssetType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //ASSET_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "HDFS"; //ASSET_HDFS
		}
		if( elementValue == 2 ) {
			return "HBase"; //ASSET_HBASE
		}
		if( elementValue == 3 ) {
			return "Hive"; //ASSET_HIVE
		}
		if( elementValue == 4 ) {
			return "Agent"; //ASSET_HIVE
		}
		if( elementValue == 5 ) {
			return "Knox"; //ASSET_HIVE
		}
		if( elementValue == 6 ) {
			return "Storm"; //ASSET_HIVE
		}
		return null;
	}

	static public String getLabelFor_AccessResult( int elementValue ) {
		if( elementValue == 0 ) {
			return "Denied"; //ACCESS_RESULT_DENIED
		}
		if( elementValue == 1 ) {
			return "Allowed"; //ACCESS_RESULT_ALLOWED
		}
		return null;
	}

	static public String getLabelFor_PolicyType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Inclusion"; //POLICY_INCLUSION
		}
		if( elementValue == 1 ) {
			return "Exclusion"; //POLICY_EXCLUSION
		}
		return null;
	}

	static public String getLabelFor_XAAuditType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //XA_AUDIT_TYPE_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "All"; //XA_AUDIT_TYPE_ALL
		}
		if( elementValue == 2 ) {
			return "Read"; //XA_AUDIT_TYPE_READ
		}
		if( elementValue == 3 ) {
			return "Write"; //XA_AUDIT_TYPE_WRITE
		}
		if( elementValue == 4 ) {
			return "Create"; //XA_AUDIT_TYPE_CREATE
		}
		if( elementValue == 5 ) {
			return "Delete"; //XA_AUDIT_TYPE_DELETE
		}
		if( elementValue == 6 ) {
			return "Login"; //XA_AUDIT_TYPE_LOGIN
		}
		return null;
	}

	static public String getLabelFor_ResourceType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //RESOURCE_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Path"; //RESOURCE_PATH
		}
		if( elementValue == 2 ) {
			return "Database"; //RESOURCE_DB
		}
		if( elementValue == 3 ) {
			return "Table"; //RESOURCE_TABLE
		}
		if( elementValue == 4 ) {
			return "Column Family"; //RESOURCE_COL_FAM
		}
		if( elementValue == 5 ) {
			return "Column"; //RESOURCE_COLUMN
		}
		if( elementValue == 6 ) {
			return "VIEW"; //RESOURCE_VIEW
		}
		if( elementValue == 7 ) {
			return "UDF"; //RESOURCE_UDF
		}
		if( elementValue == 8 ) {
			return "View Column"; //RESOURCE_VIEW_COL
		}
		return null;
	}

	static public String getLabelFor_XAGroupType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //XA_GROUP_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "User"; //XA_GROUP_USER
		}
		if( elementValue == 2 ) {
			return "Group"; //XA_GROUP_GROUP
		}
		if( elementValue == 3 ) {
			return "Role"; //XA_GROUP_ROLE
		}
		return null;
	}

	static public String getLabelFor_XAPermForType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //XA_PERM_FOR_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Permission for Users"; //XA_PERM_FOR_USER
		}
		if( elementValue == 2 ) {
			return "Permission for Groups"; //XA_PERM_FOR_GROUP
		}
		return null;
	}

	static public String getLabelFor_XAPermType( int elementValue ) {
		if( elementValue == 0 ) {
			return "Unknown"; //XA_PERM_TYPE_UNKNOWN
		}
		if( elementValue == 1 ) {
			return "Reset"; //XA_PERM_TYPE_RESET
		}
		if( elementValue == 2 ) {
			return "Read"; //XA_PERM_TYPE_READ
		}
		if( elementValue == 3 ) {
			return "Write"; //XA_PERM_TYPE_WRITE
		}
		if( elementValue == 4 ) {
			return "Create"; //XA_PERM_TYPE_CREATE
		}
		if( elementValue == 5 ) {
			return "Delete"; //XA_PERM_TYPE_DELETE
		}
		if( elementValue == 6 ) {
			return "Admin"; //XA_PERM_TYPE_ADMIN
		}
		if( elementValue == 7 ) {
			return "Obfuscate"; //XA_PERM_TYPE_OBFUSCATE
		}
		if( elementValue == 8 ) {
			return "Mask"; //XA_PERM_TYPE_MASK
		}
		if( elementValue == 9 ) {
			return "Execute"; //XA_PERM_TYPE_EXECUTE
		}
		if( elementValue == 10 ) {
			return "Select"; //XA_PERM_TYPE_SELECT
		}
		if( elementValue == 11 ) {
			return "Update"; //XA_PERM_TYPE_UPDATE
		}
		if( elementValue == 12 ) {
			return "Drop"; //XA_PERM_TYPE_DROP
		}
		if( elementValue == 13 ) {
			return "Alter"; //XA_PERM_TYPE_ALTER
		}
		if( elementValue == 14 ) {
			return "Index"; //XA_PERM_TYPE_INDEX
		}
		if( elementValue == 15 ) {
			return "Lock"; //XA_PERM_TYPE_LOCK
		}
		if( elementValue == 16 ) {
			return "All"; //XA_PERM_TYPE_ALL
		}
		if( elementValue == 17 ) {
			return "Allow"; //XA_PERM_TYPE_ALLOW
		}
		return null;
	}

	static public String getLabelFor_ClassTypes( int elementValue ) {
		if( elementValue == 0 ) {
			return "None"; //CLASS_TYPE_NONE
		}
		if( elementValue == 1 ) {
			return "Message"; //CLASS_TYPE_MESSAGE
		}
		if( elementValue == 2 ) {
			return "User Profile"; //CLASS_TYPE_USER_PROFILE
		}
		if( elementValue == 3 ) {
			return "Authentication Session"; //CLASS_TYPE_AUTH_SESS
		}
		if( elementValue == 4 ) {
			return null; //CLASS_TYPE_DATA_OBJECT
		}
		if( elementValue == 5 ) {
			return null; //CLASS_TYPE_NAMEVALUE
		}
		if( elementValue == 6 ) {
			return null; //CLASS_TYPE_LONG
		}
		if( elementValue == 7 ) {
			return null; //CLASS_TYPE_PASSWORD_CHANGE
		}
		if( elementValue == 8 ) {
			return null; //CLASS_TYPE_STRING
		}
		if( elementValue == 9 ) {
			return null; //CLASS_TYPE_ENUM
		}
		if( elementValue == 10 ) {
			return null; //CLASS_TYPE_ENUM_ELEMENT
		}
		if( elementValue == 11 ) {
			return "Response"; //CLASS_TYPE_RESPONSE
		}
		if( elementValue == 1000 ) {
			return "Asset"; //CLASS_TYPE_XA_ASSET
		}
		if( elementValue == 1001 ) {
			return "Resource"; //CLASS_TYPE_XA_RESOURCE
		}
		if( elementValue == 1002 ) {
			return "XA Group"; //CLASS_TYPE_XA_GROUP
		}
		if( elementValue == 1003 ) {
			return "XA User"; //CLASS_TYPE_XA_USER
		}
		if( elementValue == 1004 ) {
			return "XA Group of Users"; //CLASS_TYPE_XA_GROUP_USER
		}
		if( elementValue == 1005 ) {
			return "XA Group of groups"; //CLASS_TYPE_XA_GROUP_GROUP
		}
		if( elementValue == 1006 ) {
			return "XA permissions for resource"; //CLASS_TYPE_XA_PERM_MAP
		}
		if( elementValue == 1007 ) {
			return "XA audits for resource"; //CLASS_TYPE_XA_AUDIT_MAP
		}
		if( elementValue == 1008 ) {
			return "XA credential store"; //CLASS_TYPE_XA_CRED_STORE
		}
		if( elementValue == 1009 ) {
			return "XA Policy Export Audit"; //CLASS_TYPE_XA_POLICY_EXPORT_AUDIT
		}
		if( elementValue == 1010 ) {
			return "Transaction log"; //CLASS_TYPE_TRX_LOG
		}
		if( elementValue == 1011 ) {
			return "Access Audit"; //CLASS_TYPE_XA_ACCESS_AUDIT
		}
		if( elementValue == 1012 ) {
			return "Transaction log attribute"; //CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE
		}
		return null;
	}


}

