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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAccessRequestUtil {
	private static final Logger LOG = LoggerFactory.getLogger(RangerAccessRequestUtil.class);

	public static final String KEY_CONTEXT_TAGS                = "TAGS";
	public static final String KEY_CONTEXT_TAG_OBJECT          = "TAG_OBJECT";
	public static final String KEY_CONTEXT_RESOURCE            = "RESOURCE";
	public static final String KEY_CONTEXT_REQUESTED_RESOURCES = "REQUESTED_RESOURCES";
	public static final String KEY_CONTEXT_USERSTORE           = "USERSTORE";
	public static final String KEY_TOKEN_NAMESPACE = "token:";
	public static final String KEY_USER = "USER";
	public static final String KEY_OWNER = "OWNER";
	public static final String KEY_ROLES = "ROLES";
	public static final String KEY_CONTEXT_ACCESSTYPES = "ACCESSTYPES";
	public static final String KEY_CONTEXT_IS_ANY_ACCESS = "ISANYACCESS";
	public static final String KEY_CONTEXT_REQUEST       = "_REQUEST";

	public static void setRequestTagsInContext(Map<String, Object> context, Set<RangerTagForEval> tags) {
		if(CollectionUtils.isEmpty(tags)) {
			context.remove(KEY_CONTEXT_TAGS);
		} else {
			context.put(KEY_CONTEXT_TAGS, tags);
		}
	}

	public static Set<RangerTagForEval> getRequestTagsFromContext(Map<String, Object> context) {
		Set<RangerTagForEval> ret = null;
		Object          val = context.get(RangerAccessRequestUtil.KEY_CONTEXT_TAGS);

		if (val instanceof Set<?>) {
			try {
				@SuppressWarnings("unchecked")
				Set<RangerTagForEval> tags = (Set<RangerTagForEval>) val;

				ret = tags;
			} catch (Throwable t) {
				LOG.error("getRequestTags(): failed to get tags from context", t);
			}
		}

		return ret;
	}

	public static void setCurrentTagInContext(Map<String, Object> context, RangerTagForEval tag) {
		context.put(KEY_CONTEXT_TAG_OBJECT, tag);
	}

	public static RangerTagForEval getCurrentTagFromContext(Map<String, Object> context) {
		RangerTagForEval ret = null;
		Object    val = context.get(KEY_CONTEXT_TAG_OBJECT);

		if(val instanceof RangerTagForEval) {
			ret = (RangerTagForEval)val;
		}

		return ret;
	}

	public static void setRequestedResourcesInContext(Map<String, Object> context, RangerRequestedResources resources) {
		context.put(KEY_CONTEXT_REQUESTED_RESOURCES, resources);
	}

	public static RangerRequestedResources getRequestedResourcesFromContext(Map<String, Object> context) {
		RangerRequestedResources ret = null;
		Object                   val = context.get(KEY_CONTEXT_REQUESTED_RESOURCES);

		if(val instanceof RangerRequestedResources) {
			ret = (RangerRequestedResources)val;
		}

		return ret;
	}

	public static void setCurrentResourceInContext(Map<String, Object> context, RangerAccessResource resource) {
		context.put(KEY_CONTEXT_RESOURCE, resource);
	}

	public static RangerAccessResource getCurrentResourceFromContext(Map<String, Object> context) {
		RangerAccessResource ret = null;
		Object               val = MapUtils.isNotEmpty(context) ? context.get(KEY_CONTEXT_RESOURCE) : null;

		if(val instanceof RangerAccessResource) {
			ret = (RangerAccessResource)val;
		}

		return ret;
	}

	public static Map<String, Object> copyContext(Map<String, Object> context) {
		final Map<String, Object> ret;

		if(MapUtils.isEmpty(context)) {
			ret = new HashMap<>();
		} else {
			ret = new HashMap<>(context);

			ret.remove(KEY_CONTEXT_TAGS);
			ret.remove(KEY_CONTEXT_TAG_OBJECT);
			ret.remove(KEY_CONTEXT_RESOURCE);
			ret.remove(KEY_CONTEXT_REQUEST);
			// don't remove REQUESTED_RESOURCES
		}

		return ret;
	}

	public static void setCurrentUserInContext(Map<String, Object> context, String user) {
		setTokenInContext(context, KEY_USER, user);
	}
	public static void setOwnerInContext(Map<String, Object> context, String owner) {
		setTokenInContext(context, KEY_OWNER, owner);
	}
	public static String getCurrentUserFromContext(Map<String, Object> context) {
		Object ret = getTokenFromContext(context, KEY_USER);
		return ret != null ? ret.toString() : "";
	}

	public static void setTokenInContext(Map<String, Object> context, String tokenName, Object tokenValue) {
		String tokenNameWithNamespace = KEY_TOKEN_NAMESPACE + tokenName;
		context.put(tokenNameWithNamespace, tokenValue);
	}
	public static Object getTokenFromContext(Map<String, Object> context, String tokenName) {
		String tokenNameWithNamespace = KEY_TOKEN_NAMESPACE + tokenName;
		return MapUtils.isNotEmpty(context) ? context.get(tokenNameWithNamespace) : null;
	}

	public static void setCurrentUserRolesInContext(Map<String, Object> context, Set<String> roles) {
		setTokenInContext(context, KEY_ROLES, roles);
	}
	public static Set<String> getCurrentUserRolesFromContext(Map<String, Object> context) {
		Object ret = getTokenFromContext(context, KEY_ROLES);
		return ret != null ? (Set<String>) ret : Collections.EMPTY_SET;
	}

	public static void setRequestUserStoreInContext(Map<String, Object> context, RangerUserStore rangerUserStore) {
		context.put(KEY_CONTEXT_USERSTORE, rangerUserStore);
	}

	public static RangerUserStore getRequestUserStoreFromContext(Map<String, Object> context) {
		RangerUserStore ret = null;
		Object    val = context.get(KEY_CONTEXT_USERSTORE);

		if(val instanceof RangerUserStore) {
			ret = (RangerUserStore) val;
		}

		return ret;
	}

	public static void setIsAnyAccessInContext(Map<String, Object> context, Boolean value) {
		context.put(KEY_CONTEXT_IS_ANY_ACCESS, value);
	}

	public static Boolean getIsAnyAccessInContext(Map<String, Object> context) {
		return (Boolean)context.get(KEY_CONTEXT_IS_ANY_ACCESS);
	}

	public static void setRequestInContext(RangerAccessRequest request) {
		Map<String, Object> context = request.getContext();

		if (context != null) {
			context.put(KEY_CONTEXT_REQUEST, request);
		}
	}

	public static RangerAccessRequest getRequestFromContext(Map<String, Object> context) {
		RangerAccessRequest ret = null;

		if (context != null) {
			Object val = context.get(KEY_CONTEXT_REQUEST);

			if (val != null) {
				if (val instanceof RangerAccessRequest) {
					ret = (RangerAccessRequest) val;
				} else {
					LOG.error("getRequestFromContext(): expected RangerAccessRequest, but found " + val.getClass().getCanonicalName());
				}
			}
		}

		return ret;
	}

}
