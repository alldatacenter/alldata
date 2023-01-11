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

package org.apache.ranger.plugin.policyengine;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.util.MacroProcessor;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ranger.plugin.util.RangerCommonConstants.*;


public final class RangerRequestScriptEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerRequestScriptEvaluator.class);

	private static final Logger    PERF_POLICY_CONDITION_SCRIPT_TOJSON         = RangerPerfTracer.getPerfLogger("policy.condition.script.tojson");
	private static final Logger    PERF_POLICY_CONDITION_SCRIPT_EVAL           = RangerPerfTracer.getPerfLogger("policy.condition.script.eval");
	private static final String TAG_ATTR_DATE_FORMAT_PROP                   = "ranger.plugin.tag.attr.additional.date.formats";
	private static final String TAG_ATTR_DATE_FORMAT_SEPARATOR              = "||";
	private static final String TAG_ATTR_DATE_FORMAT_SEPARATOR_REGEX        = "\\|\\|";
	private static final String DEFAULT_RANGER_TAG_ATTRIBUTE_DATE_FORMAT    = "yyyy/MM/dd";
	private static final String DEFAULT_ATLAS_TAG_ATTRIBUTE_DATE_FORMAT_NAME = "ATLAS_DATE_FORMAT";
	private static final String DEFAULT_ATLAS_TAG_ATTRIBUTE_DATE_FORMAT     = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private static final String SCRIPT_PREEXEC                              = SCRIPT_VAR__CTX + "=JSON.parse(" + SCRIPT_VAR__CTX_JSON + "); J=JSON.stringify;" +
                                                                                 SCRIPT_VAR_REQ + "=" + SCRIPT_VAR__CTX + "." + SCRIPT_FIELD_REQUEST + ";" +
                                                                                 SCRIPT_VAR_RES + "=" + SCRIPT_VAR_REQ + "." + SCRIPT_FIELD_RESOURCE + ";" +
                                                                                 SCRIPT_VAR_USER + "=" + SCRIPT_VAR_REQ + "." + SCRIPT_FIELD_USER_ATTRIBUTES + ";" +
                                                                                 SCRIPT_VAR_UGNAMES + "=" + SCRIPT_VAR_REQ + "." + SCRIPT_FIELD_USER_GROUPS + ";" +
                                                                                 SCRIPT_VAR_UG + "=" + SCRIPT_VAR_REQ + "." + SCRIPT_FIELD_USER_GROUP_ATTRIBUTES + ";" +
                                                                                 SCRIPT_VAR_UGA + "=" + SCRIPT_VAR_REQ + "." + SCRIPT_FIELD_UGA + ";" +
                                                                                 SCRIPT_VAR_URNAMES + "=" + SCRIPT_VAR_REQ + "." + SCRIPT_FIELD_USER_ROLES + ";" +
                                                                                 SCRIPT_VAR_TAG + "=" + SCRIPT_VAR__CTX + "." + SCRIPT_FIELD_TAG + ";" +
                                                                                 SCRIPT_VAR_TAGS + "=" + SCRIPT_VAR__CTX + "." + SCRIPT_FIELD_TAGS + ";" +
                                                                                 SCRIPT_VAR_TAGNAMES + "=" + SCRIPT_VAR__CTX + "." + SCRIPT_FIELD_TAG_NAMES + ";";
	private static final Pattern JSON_VAR_NAMES_PATTERN   = Pattern.compile(getJsonVarNamesPattern());
	private static final Pattern USER_ATTRIBUTES_PATTERN  = Pattern.compile(getUserAttributesPattern());
	private static final Pattern GROUP_ATTRIBUTES_PATTERN = Pattern.compile(getGroupAttributesPattern());
	private static final Character CHAR_QUOTE = '\'';
	private static final Character CHAR_COMMA = ',';

	private static final MacroProcessor MACRO_PROCESSOR = new MacroProcessor(getMacrosMap());

	private static String[] dateFormatStrings = null;

	private final RangerAccessRequest              accessRequest;
	private       boolean                          initDone   = false;
	private       Map<String, String>              userAttrs  = Collections.emptyMap();
	private       Map<String, Map<String, String>> groupAttrs = Collections.emptyMap();
	private       Map<String, Map<String, Object>> tags       = Collections.emptyMap();
	private       Map<String, Object>              tag        = Collections.emptyMap();
	private       Collection<String>               userGroups = Collections.emptySet();
	private       Collection<String>               userRoles  = Collections.emptySet();
	private       Collection<String>               tagNames   = Collections.emptySet();
	private       Boolean                          result     = false;

	static {
		init(null);
	}

	private static final ThreadLocal<List<SimpleDateFormat>> THREADLOCAL_DATE_FORMATS =
			new ThreadLocal<List<SimpleDateFormat>>() {
				@Override protected List<SimpleDateFormat> initialValue() {
					List<SimpleDateFormat> ret = new ArrayList<>();

					for (String dateFormatString : dateFormatStrings) {
						try {
							if (StringUtils.isNotBlank(dateFormatString)) {
								if (StringUtils.equalsIgnoreCase(dateFormatString, DEFAULT_ATLAS_TAG_ATTRIBUTE_DATE_FORMAT_NAME)) {
									dateFormatString = DEFAULT_ATLAS_TAG_ATTRIBUTE_DATE_FORMAT;
								}
								SimpleDateFormat df = new SimpleDateFormat(dateFormatString);
								df.setLenient(false);
								ret.add(df);
							}
						} catch (Exception exception) {
							// Ignore
						}
					}

					return ret;
				}
			};


	public static boolean needsJsonCtxEnabled(String script) {
		Matcher matcher = JSON_VAR_NAMES_PATTERN.matcher(script);

		boolean ret = matcher.find();

		return ret;
	}

	public static boolean hasUserAttributeReference(String script) {
		Matcher matcher = USER_ATTRIBUTES_PATTERN.matcher(script);

		boolean ret = matcher.find();

		return ret;
	}

	public static boolean hasGroupAttributeReference(String script) {
		Matcher matcher = GROUP_ATTRIBUTES_PATTERN.matcher(script);

		boolean ret = matcher.find();

		return ret;
	}

	public static boolean hasUserGroupAttributeReference(String script) {
		return hasUserAttributeReference(script) || hasGroupAttributeReference(script);
	}

	public static boolean hasUserGroupAttributeReference(Collection<String> scripts) {
		boolean ret = false;

		if (scripts != null) {
			for (String script : scripts) {
				if (hasUserGroupAttributeReference(script)) {
					ret = true;

					break;
				}
			}
		}

		return ret;
	}

	public static String expandMacros(String script) {
		return MACRO_PROCESSOR.expandMacros(script);
	}

	public RangerRequestScriptEvaluator(final RangerAccessRequest accessRequest) {
		this.accessRequest = accessRequest.getReadOnlyCopy();
	}

	public Object evaluateScript(ScriptEngine scriptEngine, String script) {
		script = expandMacros(script);

		return evaluateScript(scriptEngine, script, needsJsonCtxEnabled(script));
	}

	public Object evaluateConditionScript(ScriptEngine scriptEngine, String script, boolean enableJsonCtx) {
		script = expandMacros(script);

		Object ret = evaluateScript(scriptEngine, script, enableJsonCtx);

		if (ret == null) {
			ret = getResult();
		}

		if (ret instanceof Boolean) {
			result = (Boolean) ret;
		}

		return ret;
	}

	private Object evaluateScript(ScriptEngine scriptEngine, String script, boolean enableJsonCtx) {
		Object              ret        = null;
		Bindings            bindings   = scriptEngine.createBindings();
		RangerTagForEval    currentTag = this.getCurrentTag();
		Map<String, String> tagAttribs = currentTag != null ? currentTag.getAttributes() : Collections.emptyMap();

		bindings.put(SCRIPT_VAR_ctx, this);
		bindings.put(SCRIPT_VAR_tag, currentTag);
		bindings.put(SCRIPT_VAR_tagAttr, tagAttribs);

		if (enableJsonCtx) {
			bindings.put(SCRIPT_VAR__CTX_JSON, this.toJson());

			script = SCRIPT_PREEXEC + script;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("RangerRequestScriptEvaluator.evaluateScript(): script={" + script + "}");
		}

		RangerPerfTracer perf = null;

		try {
			long requestHash = accessRequest.hashCode();

			if (RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_CONDITION_SCRIPT_EVAL)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_CONDITION_SCRIPT_EVAL, "RangerRequestScriptEvaluator.evaluateScript(requestHash=" + requestHash + ")");
			}

			ret = scriptEngine.eval(script, bindings);
		} catch (NullPointerException nullp) {
			LOG.error("RangerRequestScriptEvaluator.evaluateScript(): eval called with NULL argument(s)", nullp);

		} catch (ScriptException exception) {
			LOG.error("RangerRequestScriptEvaluator.evaluateScript(): failed to evaluate script," +
					" exception=" + exception);
		} finally {
			RangerPerfTracer.log(perf);
		}

		return ret;
	}

	private String toJson() {
		RangerPerfTracer perf = null;

		long requestHash = accessRequest.hashCode();

		if (RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_CONDITION_SCRIPT_TOJSON)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_CONDITION_SCRIPT_TOJSON, "RangerRequestScriptEvaluator.toJson(requestHash=" + requestHash + ")");
		}

		Map<String, Object> ret        = new HashMap<>();
		Map<String, Object> request    = new HashMap<>();
		Date                accessTime = accessRequest.getAccessTime();

		init();

		if (accessTime != null) {
			request.put(SCRIPT_FIELD_ACCESS_TIME, accessTime.getTime());
		}

		request.put(SCRIPT_FIELD_ACCESS_TYPE, accessRequest.getAccessType());
		request.put(SCRIPT_FIELD_ACTION, accessRequest.getAction());
		request.put(SCRIPT_FIELD_CLIENT_IP_ADDRESS, accessRequest.getClientIPAddress());
		request.put(SCRIPT_FIELD_CLIENT_TYPE, accessRequest.getClientType());
		request.put(SCRIPT_FIELD_CLUSTER_NAME, accessRequest.getClusterName());
		request.put(SCRIPT_FIELD_CLUSTER_TYPE, accessRequest.getClusterType());
		request.put(SCRIPT_FIELD_FORWARDED_ADDRESSES, accessRequest.getForwardedAddresses());
		request.put(SCRIPT_FIELD_REMOTE_IP_ADDRESS, accessRequest.getRemoteIPAddress());
		request.put(SCRIPT_FIELD_REQUEST_DATA, accessRequest.getRequestData());

		if (accessRequest.getResource() != null) {
			Map<String, Object> resource = new HashMap<>(accessRequest.getResource().getAsMap());

			resource.put(SCRIPT_FIELD__OWNER_USER, accessRequest.getResource().getOwnerUser());

			request.put(SCRIPT_FIELD_RESOURCE, resource);
		}

		request.put(SCRIPT_FIELD_RESOURCE_MATCHING_SCOPE, accessRequest.getResourceMatchingScope());

		request.put(SCRIPT_FIELD_USER, getUser());
		request.put(SCRIPT_FIELD_USER_GROUPS, userGroups);
		request.put(SCRIPT_FIELD_USER_ROLES, userRoles);

		request.put(SCRIPT_FIELD_USER_ATTRIBUTES, userAttrs);

		request.put(SCRIPT_FIELD_USER_GROUP_ATTRIBUTES, groupAttrs);
		request.put(SCRIPT_FIELD_UGA, new UserGroupsAttributes(userGroups, groupAttrs).getAttributes());

		ret.put(SCRIPT_FIELD_REQUEST, request);

		ret.put(SCRIPT_FIELD_TAGS, tags);
		ret.put(SCRIPT_FIELD_TAG_NAMES, tagNames);
		ret.put(SCRIPT_FIELD_TAG, tag);

		String strRet = JsonUtils.objectToJson(ret);

		RangerPerfTracer.log(perf);

		return strRet;
	}

	public static void init(Configuration config) {
		StringBuilder sb = new StringBuilder(DEFAULT_RANGER_TAG_ATTRIBUTE_DATE_FORMAT);

		sb.append(TAG_ATTR_DATE_FORMAT_SEPARATOR).append(DEFAULT_ATLAS_TAG_ATTRIBUTE_DATE_FORMAT_NAME);

		String additionalDateFormatsValue = config != null ? config.get(TAG_ATTR_DATE_FORMAT_PROP) : null;

		if (StringUtils.isNotBlank(additionalDateFormatsValue)) {
			sb.append(TAG_ATTR_DATE_FORMAT_SEPARATOR).append(additionalDateFormatsValue);
		}

		String[] formatStrings = sb.toString().split(TAG_ATTR_DATE_FORMAT_SEPARATOR_REGEX);

		Arrays.sort(formatStrings, new Comparator<String>() {
			@Override
			public int compare(String first, String second) {
				return Integer.compare(second.length(), first.length());
			}
		});

		RangerRequestScriptEvaluator.dateFormatStrings = formatStrings;
	}

	public String getResource() {
		String ret = null;
		RangerAccessResource val = RangerAccessRequestUtil.getCurrentResourceFromContext(getRequestContext());

		if(val != null) {
			ret = val.getAsString();
		}

		return ret;
	}

	public String getRequestContextAttribute(String attributeName) {
		String ret = null;

		if (StringUtils.isNotBlank(attributeName)) {
			Object val = getRequestContext().get(attributeName);

			if(val != null) {
				ret = val.toString();
			}
		}

		return ret;
	}

	public boolean isAccessTypeAny() { return accessRequest.isAccessTypeAny(); }

	public boolean isAccessTypeDelegatedAdmin() { return accessRequest.isAccessTypeDelegatedAdmin(); }

	public String getUser() { return accessRequest.getUser(); }

	public Set<String> getUserGroups() { return accessRequest.getUserGroups(); }

	public Set<String> getUserRoles() { return accessRequest.getUserRoles(); }

	public Date getAccessTime() { return accessRequest.getAccessTime() != null ? accessRequest.getAccessTime() : new Date(); }

	public String getClientIPAddress() { return accessRequest.getClientIPAddress(); }

	public String getClientType() { return accessRequest.getClientType(); }

	public String getAction() { return accessRequest.getAction(); }

	public String getRequestData() { return accessRequest.getRequestData(); }

	public String getSessionId() { return accessRequest.getSessionId(); }

	public RangerTagForEval getCurrentTag() {
		RangerTagForEval ret = RangerAccessRequestUtil.getCurrentTagFromContext(getRequestContext());

		if(ret == null ) {
			if (LOG.isDebugEnabled()) {
				logDebug("RangerRequestScriptEvaluator.getCurrentTag() - No current TAG object. Script execution must be for resource-based policy.");
			}
		}
		return ret;
	}

	public String getCurrentTagType() {
		RangerTagForEval tagObject = getCurrentTag();
		return (tagObject != null) ? tagObject.getType() : null;
	}

	public Set<String> getAllTagTypes() {
		Set<String>     allTagTypes   = null;
		Set<RangerTagForEval> tagObjectList = getAllTags();

		if (CollectionUtils.isNotEmpty(tagObjectList)) {
			for (RangerTagForEval tag : tagObjectList) {
				String tagType = tag.getType();
				if (allTagTypes == null) {
					allTagTypes = new HashSet<>();
				}
				allTagTypes.add(tagType);
			}
		}

		return allTagTypes;
	}

	public Map<String, String> getTagAttributes(final String tagType) {
		Map<String, String> ret = null;

		if (StringUtils.isNotBlank(tagType)) {
			Set<RangerTagForEval> tagObjectList = getAllTags();

			// Assumption: There is exactly one tag with given tagType in the list of tags - may not be true ***TODO***
			// This will get attributes of the first tagType that matches
			if (CollectionUtils.isNotEmpty(tagObjectList)) {
				for (RangerTagForEval tag : tagObjectList) {
					if (tag.getType().equals(tagType)) {
						ret = tag.getAttributes();
						break;
					}
				}
			}
		}

		return ret;
	}

	public List<Map<String, String>> getTagAttributesForAllMatchingTags(final String tagType) {
		List<Map<String, String>> ret = null;

		if (StringUtils.isNotBlank(tagType)) {
			Set<RangerTagForEval> tagObjectList = getAllTags();

			// Assumption: There is exactly one tag with given tagType in the list of tags - may not be true ***TODO***
			// This will get attributes of the first tagType that matches
			if (CollectionUtils.isNotEmpty(tagObjectList)) {
				for (RangerTagForEval tag : tagObjectList) {
					if (tag.getType().equals(tagType)) {
						Map<String, String> tagAttributes = tag.getAttributes();
						if (tagAttributes != null) {
							if (ret == null) {
								ret = new ArrayList<>();
							}
							ret.add(tagAttributes);
						}
						break;
					}
				}
			}
		}

		return ret;
	}

	public Set<String> getAttributeNames(final String tagType) {
		Set<String>         ret        = null;
		Map<String, String> attributes = getTagAttributes(tagType);

		if (attributes != null) {
			ret = attributes.keySet();
		}

		return ret;
	}

	public String getAttributeValue(final String tagType, final String attributeName) {
		String ret = null;

		if (StringUtils.isNotBlank(tagType) || StringUtils.isNotBlank(attributeName)) {
			Map<String, String> attributes = getTagAttributes(tagType);

			if (attributes != null) {
				ret = attributes.get(attributeName);
			}
		}
		return ret;
	}

	public List<String> getAttributeValueForAllMatchingTags(final String tagType, final String attributeName) {
		List<String> ret = null;

		if (StringUtils.isNotBlank(tagType) || StringUtils.isNotBlank(attributeName)) {
			Map<String, String> attributes = getTagAttributes(tagType);

			if (attributes != null && attributes.get(attributeName) != null) {
				if (ret == null) {
					ret = new ArrayList<>();
				}
				ret.add(attributes.get(attributeName));
			}
		}
		return ret;
	}

	public String getAttributeValue(final String attributeName) {
		String ret = null;

		if (StringUtils.isNotBlank(attributeName)) {
			RangerTagForEval tag = getCurrentTag();
			Map<String, String> attributes = null;
			if (tag != null) {
				attributes = tag.getAttributes();
			}
			if (attributes != null) {
				ret = attributes.get(attributeName);
			}
		}

		return ret;
	}

	public boolean getResult() {
		return result;

	}

	public void setResult(final boolean result) {
		this.result = result;
	}

	private Date getAsDate(String value, SimpleDateFormat df) {
		Date ret = null;

		TimeZone savedTimeZone = df.getTimeZone();
		try {
			ret = df.parse(value);
		} catch (ParseException exception) {
			// Ignore
		} finally {
			df.setTimeZone(savedTimeZone);
		}

		return ret;
	}

	public Date getAsDate(String value) {
		Date ret = null;

		if (StringUtils.isNotBlank(value)) {
			for (SimpleDateFormat simpleDateFormat : THREADLOCAL_DATE_FORMATS.get()) {
				ret = getAsDate(value, simpleDateFormat);
				if (ret != null) {
					if (LOG.isDebugEnabled()) {
						logDebug("RangerRequestScriptEvaluator.getAsDate() -The best match found for Format-String:[" + simpleDateFormat.toPattern() + "], date:[" + ret +"]");
					}
					break;
				}
			}
		}

		if (ret == null) {
			logError("RangerRequestScriptEvaluator.getAsDate() - Could not convert [" + value + "] to Date using any of the Format-Strings: " + Arrays.toString(dateFormatStrings));
		} else {
			ret = StringUtil.getUTCDateForLocalDate(ret);
		}

		return ret;
	}

	public Date getTagAttributeAsDate(String tagType, String attributeName) {
		String attrValue = getAttributeValue(tagType, attributeName);

		return getAsDate(attrValue);
	}

	public boolean isAccessedAfter(String tagType, String attributeName) {
		boolean ret        = false;
		Date    accessDate = getAccessTime();
		Date    expiryDate = getTagAttributeAsDate(tagType, attributeName);

		if (expiryDate == null || accessDate.after(expiryDate) || accessDate.equals(expiryDate)) {
			ret = true;
		}

		return ret;
	}

	public boolean isAccessedAfter(String attributeName) {
		boolean ret        = false;
		Date    accessDate = getAccessTime();
		Date    expiryDate = getAsDate(getAttributeValue(attributeName));

		if (expiryDate == null || accessDate.after(expiryDate) || accessDate.equals(expiryDate)) {
			ret = true;
		}

		return ret;
	}

	public boolean isAccessedBefore(String tagType, String attributeName) {
		boolean ret        = true;
		Date    accessDate = getAccessTime();
		Date    expiryDate = getTagAttributeAsDate(tagType, attributeName);

		if (expiryDate == null || accessDate.after(expiryDate)) {
			ret = false;
		}

		return ret;
	}

	public boolean isAccessedBefore(String attributeName) {
		boolean ret        = true;
		Date    accessDate = getAccessTime();
		Date    expiryDate = getAsDate(getAttributeValue(attributeName));

		if (expiryDate == null || accessDate.after(expiryDate)) {
			ret = false;
		}

		return ret;
	}

	public String ugNamesCsv() {
		init();

		return toCsv(userGroups);
	}

	public String ugNamesCsvQ() {
		init();

		return toCsvQ(userGroups);
	}

	public String urNamesCsv() {
		init();

		return toCsv(userRoles);
	}

	public String urNamesCsvQ() {
		init();

		return toCsvQ(userRoles);
	}

	public String tagNamesCsv() {
		init();

		return toCsv(tagNames);
	}

	public String tagNamesCsvQ() {
		init();

		return toCsvQ(tagNames);
	}

	public String userAttrNamesCsv() {
		init();

		return toCsv(getUserAttrNames());
	}

	public String userAttrNamesCsvQ() {
		init();

		return toCsvQ(getUserAttrNames());
	}

	public String ugAttrNamesCsv() {
		init();

		return toCsv(getUgAttrNames());
	}

	public String ugAttrNamesCsvQ() {
		return toCsvQ(getUgAttrNames());
	}

	public String tagAttrNamesCsv() {
		init();

		return toCsv(getTagAttrNames());
	}

	public String tagAttrNamesCsvQ() {
		init();

		return toCsvQ(getTagAttrNames());
	}

	public String ugAttrCsv(String attrName) {
		init();

		return toCsv(getUgAttr(attrName));
	}

	public String ugAttrCsvQ(String attrName) {
		init();

		return toCsvQ(getUgAttr(attrName));
	}

	public String tagAttrCsv(String attrName) {
		init();

		return toCsv(getTagAttr(attrName));
	}

	public String tagAttrCsvQ(String attrName) {
		init();

		return toCsvQ(getTagAttr(attrName));
	}

	public boolean hasTag(String tagName) {
		init();

		return tags.containsKey(tagName);
	}

	public boolean hasAnyTag() {
		init();

		return !tags.isEmpty();
	}

	public boolean hasUserAttr(String attrName) {
		init();

		return userAttrs.containsKey(attrName);
	}

	public boolean hasUgAttr(String attrName) {
		init();

		boolean ret = false;

		for (Map<String, String> attrs : groupAttrs.values()) {
			if (attrs.containsKey(attrName)) {
				ret = true;

				break;
			}
		}

		return ret;
	}

	public boolean hasTagAttr(String attrName) {
		init();

		boolean               ret  = false;
		Set<RangerTagForEval> tags = RangerAccessRequestUtil.getRequestTagsFromContext(accessRequest.getContext());

		if (tags != null) {
			for (RangerTagForEval tag : tags) {
				if (tag.getAttributes().containsKey(attrName)) {
					ret = true;

					break;
				}
			}
		}

		return ret;
	}

	public boolean isInGroup(String groupName) {
		init();

		return userGroups.contains(groupName);
	}

	public boolean isInRole(String roleName) {
		init();

		return userRoles.contains(roleName);
	}

	public boolean isInAnyGroup() {
		init();

		return !userGroups.isEmpty();
	}

	public boolean isInAnyRole() {
		init();

		return !userRoles.isEmpty();
	}

	private void init() {
		if (!initDone) {
			RangerUserStore                  userStore        = RangerAccessRequestUtil.getRequestUserStoreFromContext(accessRequest.getContext());
			Map<String, Map<String, String>> userAttrMapping  = userStore != null ? userStore.getUserAttrMapping() : Collections.emptyMap();
			Map<String, Map<String, String>> groupAttrMapping = userStore != null ? userStore.getGroupAttrMapping() : Collections.emptyMap();

			userGroups = getSorted(getUserGroups());
			userRoles  = getSorted(getUserRoles());
			userAttrs  = copyMap(userAttrMapping.get(accessRequest.getUser()));
			groupAttrs = new HashMap<>();

			userAttrs.put(SCRIPT_FIELD__NAME, getUser());

			for (String groupName : userGroups) {
				Map<String, String> attrs = groupAttrMapping.get(groupName);

				attrs = attrs != null ? new HashMap<>(attrs) : new HashMap<>();

				attrs.put(SCRIPT_FIELD__NAME, groupName);

				groupAttrs.put(groupName, attrs);
			}

			Set<RangerTagForEval> requestTags = RangerAccessRequestUtil.getRequestTagsFromContext(getRequestContext());

			if (CollectionUtils.isNotEmpty(requestTags)) {
				RangerTagForEval currentTag = RangerAccessRequestUtil.getCurrentTagFromContext(getRequestContext());

				tags = new HashMap();
				tag  = (currentTag != null) ? toMap(currentTag) : Collections.emptyMap();

				for (RangerTagForEval tag : requestTags) {
					tags.put(tag.getType(), toMap(tag));
				}

				tagNames = getSorted(tags.keySet());
			} else {
				tags     = Collections.emptyMap();
				tagNames = Collections.emptySet();
				tag      = Collections.emptyMap();
			}

			initDone = true;
		}
	}

	private Map<String, Object> getRequestContext() {
		return accessRequest.getContext();
	}

	private Set<RangerTagForEval> getAllTags() {
		Set<RangerTagForEval> ret = RangerAccessRequestUtil.getRequestTagsFromContext(accessRequest.getContext());
		if(ret == null) {
			if (LOG.isDebugEnabled()) {
				String resource = accessRequest.getResource().getAsString();

				logDebug("RangerRequestScriptEvaluator.getAllTags() - No TAGS. No TAGS for the RangerAccessResource=" + resource);
			}
		}

		return ret;
	}

	private static Map<String, Object> toMap(RangerTagForEval tag) {
		Map<String, Object> ret = new HashMap<>();

		if (tag.getAttributes() != null) {
			ret.putAll(tag.getAttributes());
		}

		ret.put(SCRIPT_FIELD__TYPE, tag.getType());
		ret.put(SCRIPT_FIELD__MATCH_TYPE, tag.getMatchType());

		return ret;
	}

	private Collection<String> getSorted(Collection<String> values) {
		final Collection<String> ret;

		if (values == null) {
			ret = Collections.emptyList();
		} else if (values.size() > 1) {
			List lst = new ArrayList<>(values);

			Collections.sort(lst);

			ret = lst;
		} else {
			ret = values;
		}

		return ret;
	}

	private Map<String, String> copyMap(Map<String, String> obj) { return obj == null ? new HashMap<>() : new HashMap<>(obj); }

	private List<Object> getUgAttr(String attrName) {
		List<Object> ret = new ArrayList<>();

		for (String groupName : userGroups) {
			Map<String, String> attrs = groupAttrs.get(groupName);
			Object      val           = attrs != null ? attrs.get(attrName) : null;

			if (val != null) {
				ret.add(val);
			}
		}

		return ret;
	}

	private List<Object> getTagAttr(String attrName) {
		List<Object> ret = new ArrayList<>();

		for (String tagName : tagNames) {
			Map<String, Object> attrs = tags.get(tagName);
			Object              val   = attrs != null ? attrs.get(attrName) : null;

			if (val != null) {
				ret.add(val);
			}
		}

		return ret;
	}

	private Collection<String> getUserAttrNames() {
		Collection<String> ret = getSorted(userAttrs.keySet());

		if (ret.contains(SCRIPT_FIELD__NAME)) {
			ret.remove(SCRIPT_FIELD__NAME);
		}

		return ret;
	}

	private Collection<String> getUgAttrNames() {
		Set<String> ret = new HashSet<>();

		for (Map<String, String> attrs : groupAttrs.values()) {
			ret.addAll(attrs.keySet());
		}

		ret.remove(SCRIPT_FIELD__NAME);

		return getSorted(ret);
	}

	private Collection<String> getTagAttrNames() {
		Set<String> ret = new HashSet<>();

		for (Map<String, Object> attrs : tags.values()) {
			ret.addAll(attrs.keySet());
		}

		ret.remove(SCRIPT_FIELD__TYPE);
		ret.remove(SCRIPT_FIELD__MATCH_TYPE);

		return getSorted(ret);
	}

	private String toCsv(Collection<? extends Object> values) {
		StringBuilder sb = new StringBuilder();

		for (Object value : values) {
			if (value == null) {
				continue;
			}

			if (sb.length() > 0) {
				sb.append(CHAR_COMMA);
			}

			sb.append(value);
		}

		return sb.toString();
	}

	private String toCsvQ(Collection<? extends Object> values) {
		StringBuilder sb = new StringBuilder();

		for (Object value : values) {
			if (value == null) {
				continue;
			}

			if (sb.length() > 0) {
				sb.append(CHAR_COMMA);
			}

			sb.append(CHAR_QUOTE).append(value).append(CHAR_QUOTE);
		}

		return sb.toString();
	}

	private static String getJsonVarNamesPattern() {
		List<String> varNames = new ArrayList<>();

		/* include only variables setup by JSON.parse()
		 *
		varNames.add(SCRIPT_VAR_ctx);
		varNames.add(SCRIPT_VAR_tag);
		varNames.add(SCRIPT_VAR_tagAttr);
		 *
		 */
		varNames.add(SCRIPT_VAR__CTX);
		varNames.add(SCRIPT_VAR_REQ);
		varNames.add(SCRIPT_VAR_RES);
		varNames.add(SCRIPT_VAR_TAG);
		varNames.add(SCRIPT_VAR_TAGNAMES);
		varNames.add(SCRIPT_VAR_TAGS);
		varNames.add(SCRIPT_VAR_UGA);
		varNames.add(SCRIPT_VAR_UG);
		varNames.add(SCRIPT_VAR_UGNAMES);
		varNames.add(SCRIPT_VAR_URNAMES);
		varNames.add(SCRIPT_VAR_USER);

		return "\\b(" + StringUtils.join(varNames, '|') + ")\\b";
	}

	private static String getUserAttributesPattern() {
		List<String> varNames = new ArrayList<>();

		varNames.add(SCRIPT_VAR_USER);
		varNames.add(SCRIPT_MACRO_USER_ATTR_NAMES_CSV);
		varNames.add(SCRIPT_MACRO_USER_ATTR_NAMES_Q_CSV);
		varNames.add(SCRIPT_MACRO_HAS_USER_ATTR);

		varNames.add("userAttrNamesCsv");
		varNames.add("userAttrNamesCsvQ");
		varNames.add("hasUserAttr");

		return "\\b(" + StringUtils.join(varNames, '|') + ")\\b";
	}

	private static String getGroupAttributesPattern() {
		List<String> varNames = new ArrayList<>();

		varNames.add(SCRIPT_VAR_UG);
		varNames.add(SCRIPT_VAR_UGA);

		varNames.add(SCRIPT_MACRO_GET_UG_ATTR_CSV);
		varNames.add(SCRIPT_MACRO_GET_UG_ATTR_Q_CSV);
		varNames.add(SCRIPT_MACRO_UG_ATTR_NAMES_CSV);
		varNames.add(SCRIPT_MACRO_UG_ATTR_NAMES_Q_CSV);
		varNames.add(SCRIPT_MACRO_HAS_UG_ATTR);

		varNames.add("ugAttrCsv");
		varNames.add("ugAttrCsvQ");
		varNames.add("ugAttrNamesCsv");
		varNames.add("ugAttrNamesCsvQ");
		varNames.add("hasUgAttr");

		return "\\b(" + StringUtils.join(varNames, '|') + ")\\b";
	}

	private static Map<String, String> getMacrosMap() {
		Map<String, String> ret = new HashMap<>();

		ret.put(SCRIPT_MACRO_GET_TAG_ATTR_CSV,      "ctx.tagAttrCsv");
		ret.put(SCRIPT_MACRO_GET_TAG_ATTR_Q_CSV,    "ctx.tagAttrCsvQ");
		ret.put(SCRIPT_MACRO_GET_UG_ATTR_CSV,       "ctx.ugAttrCsv");
		ret.put(SCRIPT_MACRO_GET_UG_ATTR_Q_CSV,     "ctx.ugAttrCsvQ");
		ret.put(SCRIPT_MACRO_TAG_ATTR_NAMES_CSV,    "ctx.tagAttrNamesCsv()");
		ret.put(SCRIPT_MACRO_TAG_ATTR_NAMES_Q_CSV,  "ctx.tagAttrNamesCsvQ()");
		ret.put(SCRIPT_MACRO_TAG_NAMES_CSV,         "ctx.tagNamesCsv()");
		ret.put(SCRIPT_MACRO_TAG_NAMES_Q_CSV,       "ctx.tagNamesCsvQ()");
		ret.put(SCRIPT_MACRO_UG_ATTR_NAMES_CSV,     "ctx.ugAttrNamesCsv()");
		ret.put(SCRIPT_MACRO_UG_ATTR_NAMES_Q_CSV,   "ctx.ugAttrNamesCsvQ()");
		ret.put(SCRIPT_MACRO_UG_NAMES_CSV,          "ctx.ugNamesCsv()");
		ret.put(SCRIPT_MACRO_UG_NAMES_Q_CSV,        "ctx.ugNamesCsvQ()");
		ret.put(SCRIPT_MACRO_UR_NAMES_CSV,          "ctx.urNamesCsv()");
		ret.put(SCRIPT_MACRO_UR_NAMES_Q_CSV,        "ctx.urNamesCsvQ()");
		ret.put(SCRIPT_MACRO_USER_ATTR_NAMES_CSV,   "ctx.userAttrNamesCsv()");
		ret.put(SCRIPT_MACRO_USER_ATTR_NAMES_Q_CSV, "ctx.userAttrNamesCsvQ()");
		ret.put(SCRIPT_MACRO_HAS_TAG,               "ctx.hasTag");
		ret.put(SCRIPT_MACRO_HAS_ANY_TAG,           "ctx.hasAnyTag()");
		ret.put(SCRIPT_MACRO_HAS_NO_TAG,            "!ctx.hasAnyTag()");
		ret.put(SCRIPT_MACRO_HAS_USER_ATTR,         "ctx.hasUserAttr");
		ret.put(SCRIPT_MACRO_HAS_UG_ATTR,           "ctx.hasUgAttr");
		ret.put(SCRIPT_MACRO_HAS_TAG_ATTR,          "ctx.hasTagAttr");
		ret.put(SCRIPT_MACRO_IS_IN_GROUP,           "ctx.isInGroup");
		ret.put(SCRIPT_MACRO_IS_IN_ROLE,            "ctx.isInRole");
		ret.put(SCRIPT_MACRO_IS_IN_ANY_GROUP,       "ctx.isInAnyGroup()");
		ret.put(SCRIPT_MACRO_IS_IN_ANY_ROLE,        "ctx.isInAnyRole()");
		ret.put(SCRIPT_MACRO_IS_NOT_IN_ANY_GROUP,   "!ctx.isInAnyGroup()");
		ret.put(SCRIPT_MACRO_IS_NOT_IN_ANY_ROLE,    "!ctx.isInAnyRole()");

		return ret;
	}


	public void logDebug(Object msg) {
		LOG.debug("", msg);
	}

	public void logInfo(Object msg) {
		LOG.info("", msg);
	}

	public void logWarn(Object msg) {
		LOG.warn("", msg);
	}

	public void logError(Object msg) {
		LOG.error("", msg);
	}

	public void logFatal(Object msg) {
		LOG.error("", msg);
	}

	public static class UserGroupsAttributes {
		private final Collection<String>               groupNames;
		private final Map<String, Map<String, String>> groupAttributes;

		public UserGroupsAttributes(Collection<String> groupNames, Map<String, Map<String, String>> groupAttributes) {
			this.groupNames      = groupNames;
			this.groupAttributes = groupAttributes;
		}

		/*
		  {
		    sVal: {
		      attr1: val1,
		      attr2: val2
		    },
		    mVal: {
		      attr1: [ val1, val1_2 ],
		      attr2: [ val2, val2_2 ]
		    }
		  }
		 */
		public Map<String, Map<String, Object>> getAttributes() {
			Map<String, Map<String, Object>> ret       = new HashMap<>();
			Map<String, String>              valueMap  = new HashMap<>();
			Map<String, List<String>>        valuesMap = new HashMap<>();

			ret.put("sVal", (Map) valueMap);
			ret.put("mVal", (Map) valuesMap);

			if (groupNames != null && groupAttributes != null) {
				for (String groupName : groupNames) {
					Map<String, String> attributes = groupAttributes.get(groupName);

					if (attributes != null) {
						for (Map.Entry<String, String> entry : attributes.entrySet()) {
							String attrName  = entry.getKey();
							String attrValue = entry.getValue();

							if (!valueMap.containsKey(attrName)) {
								valueMap.put(attrName, attrValue);
							}

							List<String> values = valuesMap.get(attrName);

							if (values == null) {
								values = new ArrayList<>();

								valuesMap.put(attrName, values);
							}

							values.add(attrValue);
						}
					}
				}
			}

			return ret;
		}
	}
}
