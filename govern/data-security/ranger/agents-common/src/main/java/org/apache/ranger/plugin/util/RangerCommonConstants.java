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

package org.apache.ranger.plugin.util;

public class RangerCommonConstants {

	public static final String PROP_COOKIE_NAME                             = "ranger.admin.cookie.name";
	public static final String DEFAULT_COOKIE_NAME                          = "RANGERADMINSESSIONID";

	public static final String RANGER_ADMIN_SUFFIX_POLICY_DELTA             = ".supports.policy.deltas";
	public static final String PLUGIN_CONFIG_SUFFIX_POLICY_DELTA            = ".supports.policy.deltas";

	public static final String RANGER_ADMIN_SUFFIX_TAG_DELTA                = ".supports.tag.deltas";
	public static final String PLUGIN_CONFIG_SUFFIX_TAG_DELTA               = ".supports.tag.deltas";

	public static final String RANGER_ADMIN_SUFFIX_IN_PLACE_POLICY_UPDATES  = ".supports.in.place.policy.updates";
	public static final String PLUGIN_CONFIG_SUFFIX_IN_PLACE_POLICY_UPDATES = ".supports.in.place.policy.updates";

	public static final String RANGER_ADMIN_SUFFIX_IN_PLACE_TAG_UPDATES     = ".supports.in.place.tag.updates";
	public static final String PLUGIN_CONFIG_SUFFIX_IN_PLACE_TAG_UPDATES    = ".supports.in.place.tag.updates";

	public static final String  RANGER_ADMIN_SUPPORTS_TAGS_DEDUP            = ".supports.tags.dedup";

	public static final boolean RANGER_ADMIN_SUFFIX_POLICY_DELTA_DEFAULT             = false;
	public static final boolean PLUGIN_CONFIG_SUFFIX_POLICY_DELTA_DEFAULT            = false;

	public static final boolean RANGER_ADMIN_SUFFIX_TAG_DELTA_DEFAULT                = false;
	public static final boolean PLUGIN_CONFIG_SUFFIX_TAG_DELTA_DEFAULT               = false;

	public static final boolean RANGER_ADMIN_SUFFIX_IN_PLACE_POLICY_UPDATES_DEFAULT  = false;
	public static final boolean PLUGIN_CONFIG_SUFFIX_IN_PLACE_POLICY_UPDATES_DEFAULT = false;

	public static final boolean RANGER_ADMIN_SUFFIX_IN_PLACE_TAG_UPDATES_DEFAULT     = false;
	public static final boolean PLUGIN_CONFIG_SUFFIX_IN_PLACE_TAG_UPDATES_DEFAULT    = false;

	public static final boolean RANGER_ADMIN_SUPPORTS_TAGS_DEDUP_DEFAULT             = true;

	public static final boolean POLICY_REST_CLIENT_SESSION_COOKIE_ENABLED            = true;

	public static final String SCRIPT_OPTION_ENABLE_JSON_CTX        = "enableJsonCtx";

	public static final String SCRIPT_VAR_ctx                       = "ctx";
	public static final String SCRIPT_VAR_tag                       = "tag";
	public static final String SCRIPT_VAR_tagAttr                   = "tagAttr";
	public static final String SCRIPT_VAR__CTX                      = "_ctx";
	public static final String SCRIPT_VAR__CTX_JSON                 = "_ctx_json";
	public static final String SCRIPT_VAR_REQ                       = "REQ";
	public static final String SCRIPT_VAR_RES                       = "RES";
	public static final String SCRIPT_VAR_TAG                       = "TAG";
	public static final String SCRIPT_VAR_TAGNAMES                  = "TAGNAMES";
	public static final String SCRIPT_VAR_TAGS                      = "TAGS";
	public static final String SCRIPT_VAR_UGA                       = "UGA";
	public static final String SCRIPT_VAR_UG                        = "UG";
	public static final String SCRIPT_VAR_UGNAMES                   = "UGNAMES";
	public static final String SCRIPT_VAR_URNAMES                   = "URNAMES";
	public static final String SCRIPT_VAR_USER                      = "USER";

	public static final String SCRIPT_FIELD_ACCESS_TIME             = "accessTime";
	public static final String SCRIPT_FIELD_ACCESS_TYPE             = "accessType";
	public static final String SCRIPT_FIELD_ACTION                  = "action";
	public static final String SCRIPT_FIELD_CLIENT_IP_ADDRESS       = "clientIPAddress";
	public static final String SCRIPT_FIELD_CLIENT_TYPE             = "clientType";
	public static final String SCRIPT_FIELD_CLUSTER_NAME            = "clusterName";
	public static final String SCRIPT_FIELD_CLUSTER_TYPE            = "clusterType";
	public static final String SCRIPT_FIELD_FORWARDED_ADDRESSES     = "forwardedAddresses";
	public static final String SCRIPT_FIELD__MATCH_TYPE             = "_matchType";
	public static final String SCRIPT_FIELD__NAME                   = "_name";
	public static final String SCRIPT_FIELD__OWNER_USER             = "_ownerUser";
	public static final String SCRIPT_FIELD_REMOTE_IP_ADDRESS       = "remoteIPAddress";
	public static final String SCRIPT_FIELD_REQUEST                 = "request";
	public static final String SCRIPT_FIELD_REQUEST_DATA            = "requestData";
	public static final String SCRIPT_FIELD_RESOURCE                = "resource";
	public static final String SCRIPT_FIELD_RESOURCE_MATCHING_SCOPE = "resourceMatchingScope";
	public static final String SCRIPT_FIELD_TAG                     = "tag";
	public static final String SCRIPT_FIELD_TAGS                    = "tags";
	public static final String SCRIPT_FIELD_TAG_NAMES               = "tagNames";
	public static final String SCRIPT_FIELD__TYPE                   = "_type";
	public static final String SCRIPT_FIELD_USER                    = "user";
	public static final String SCRIPT_FIELD_USER_ATTRIBUTES         = "userAttributes";
	public static final String SCRIPT_FIELD_USER_GROUPS             = "userGroups";
	public static final String SCRIPT_FIELD_USER_GROUP_ATTRIBUTES   = "userGroupAttributes";
	public static final String SCRIPT_FIELD_UGA                     = "uga";
	public static final String SCRIPT_FIELD_USER_ROLES              = "userRoles";

	public static final String SCRIPT_MACRO_GET_TAG_NAMES         = "GET_TAG_NAMES";
	public static final String SCRIPT_MACRO_GET_TAG_NAMES_Q       = "GET_TAG_NAMES_Q";
	public static final String SCRIPT_MACRO_GET_TAG_ATTR_NAMES    = "GET_TAG_ATTR_NAMES";
	public static final String SCRIPT_MACRO_GET_TAG_ATTR_NAMES_Q  = "GET_TAG_ATTR_NAMES_Q";
	public static final String SCRIPT_MACRO_GET_TAG_ATTR          = "GET_TAG_ATTR";
	public static final String SCRIPT_MACRO_GET_TAG_ATTR_Q        = "GET_TAG_ATTR_Q";
	public static final String SCRIPT_MACRO_GET_UG_NAMES          = "GET_UG_NAMES";
	public static final String SCRIPT_MACRO_GET_UG_NAMES_Q        = "GET_UG_NAMES_Q";
	public static final String SCRIPT_MACRO_GET_UG_ATTR_NAMES     = "GET_UG_ATTR_NAMES";
	public static final String SCRIPT_MACRO_GET_UG_ATTR_NAMES_Q   = "GET_UG_ATTR_NAMES_Q";
	public static final String SCRIPT_MACRO_GET_UG_ATTR           = "GET_UG_ATTR";
	public static final String SCRIPT_MACRO_GET_UG_ATTR_Q         = "GET_UG_ATTR_Q";
	public static final String SCRIPT_MACRO_GET_UR_NAMES          = "GET_UR_NAMES";
	public static final String SCRIPT_MACRO_GET_UR_NAMES_Q        = "GET_UR_NAMES_Q";
	public static final String SCRIPT_MACRO_GET_USER_ATTR_NAMES   = "GET_USER_ATTR_NAMES";
	public static final String SCRIPT_MACRO_GET_USER_ATTR_NAMES_Q = "GET_USER_ATTR_NAMES_Q";
	public static final String SCRIPT_MACRO_GET_USER_ATTR         = "GET_USER_ATTR";
	public static final String SCRIPT_MACRO_GET_USER_ATTR_Q       = "GET_USER_ATTR_Q";

	public static final String SCRIPT_MACRO_GET_TAG_ATTR_CSV      = "GET_TAG_ATTR_CSV";
	public static final String SCRIPT_MACRO_GET_TAG_ATTR_Q_CSV    = "GET_TAG_ATTR_Q_CSV";
	public static final String SCRIPT_MACRO_GET_UG_ATTR_CSV       = "GET_UG_ATTR_CSV";
	public static final String SCRIPT_MACRO_GET_UG_ATTR_Q_CSV     = "GET_UG_ATTR_Q_CSV";
	public static final String SCRIPT_MACRO_TAG_ATTR_NAMES_CSV    = "TAG_ATTR_NAMES_CSV";
	public static final String SCRIPT_MACRO_TAG_ATTR_NAMES_Q_CSV  = "TAG_ATTR_NAMES_Q_CSV";
	public static final String SCRIPT_MACRO_TAG_NAMES_CSV         = "TAG_NAMES_CSV";
	public static final String SCRIPT_MACRO_TAG_NAMES_Q_CSV       = "TAG_NAMES_Q_CSV";
	public static final String SCRIPT_MACRO_UG_ATTR_NAMES_CSV     = "UG_ATTR_NAMES_CSV";
	public static final String SCRIPT_MACRO_UG_ATTR_NAMES_Q_CSV   = "UG_ATTR_NAMES_Q_CSV";
	public static final String SCRIPT_MACRO_UG_NAMES_CSV          = "UG_NAMES_CSV";
	public static final String SCRIPT_MACRO_UG_NAMES_Q_CSV        = "UG_NAMES_Q_CSV";
	public static final String SCRIPT_MACRO_UR_NAMES_CSV          = "UR_NAMES_CSV";
	public static final String SCRIPT_MACRO_UR_NAMES_Q_CSV        = "UR_NAMES_Q_CSV";
	public static final String SCRIPT_MACRO_USER_ATTR_NAMES_CSV   = "USER_ATTR_NAMES_CSV";
	public static final String SCRIPT_MACRO_USER_ATTR_NAMES_Q_CSV = "USER_ATTR_NAMES_Q_CSV";

	public static final String SCRIPT_MACRO_HAS_TAG               = "HAS_TAG";
	public static final String SCRIPT_MACRO_HAS_ANY_TAG           = "HAS_ANY_TAG";
	public static final String SCRIPT_MACRO_HAS_NO_TAG            = "HAS_NO_TAG";
	public static final String SCRIPT_MACRO_HAS_USER_ATTR         = "HAS_USER_ATTR";
	public static final String SCRIPT_MACRO_HAS_UG_ATTR           = "HAS_UG_ATTR";
	public static final String SCRIPT_MACRO_HAS_TAG_ATTR          = "HAS_TAG_ATTR";
	public static final String SCRIPT_MACRO_IS_IN_GROUP           = "IS_IN_GROUP";
	public static final String SCRIPT_MACRO_IS_IN_ROLE            = "IS_IN_ROLE";
	public static final String SCRIPT_MACRO_IS_IN_ANY_GROUP       = "IS_IN_ANY_GROUP";
	public static final String SCRIPT_MACRO_IS_IN_ANY_ROLE        = "IS_IN_ANY_ROLE";
	public static final String SCRIPT_MACRO_IS_NOT_IN_ANY_GROUP   = "IS_NOT_IN_ANY_GROUP";
	public static final String SCRIPT_MACRO_IS_NOT_IN_ANY_ROLE    = "IS_NOT_IN_ANY_ROLE";

	public static final String SCRIPT_POLYFILL_INCLUDES = "if (!Array.prototype.includes) {\n" +
			"    Object.defineProperty(\n" +
			"      Array.prototype, 'includes', {\n" +
			"        value: function(valueToFind) {\n" +
			"                var o = Object(this); \n" +
			"                var len = o.length;\n" +
			"                if (len === 0) { return false; }\n" +
			"                for (var k=0; k < len; k++) {\n" +
			"                    if (o[k]==valueToFind) {return true; }\n" +
			"                }   \n" +
			"                return false;\n" +
			"            }\n" +
			"        }\n" +
			"    );\n" +
			" }; ";

	public static final String SCRIPT_POLYFILL_INTERSECTS = "if (!Array.prototype.intersects) {\n" +
			"    Object.defineProperty(\n" +
			"      Array.prototype, 'intersects', {\n" +
			"          value: function (x) {\n" +
			"           if (x == null) {return false;}\n" +
			"                        var o = Object(this);\n" +
			"                        var len = o.length >>> 0;\n" +
			"            if (len === 0) { return false; }\n" +
			"            var result = o.filter(function(n) { return x.indexOf(n) > -1;})\n" +
			"            return result.length != 0;\n" +
			"        }\n" +
			"      }\n" +
			"    )\n" +
			"}; ";
}
