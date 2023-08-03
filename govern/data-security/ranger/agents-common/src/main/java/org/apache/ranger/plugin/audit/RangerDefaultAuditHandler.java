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

package org.apache.ranger.plugin.audit;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.AuditHandler;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.authorization.hadoop.constants.RangerHadoopConstants;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.policyengine.*;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerRESTUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RangerDefaultAuditHandler implements RangerAccessResultProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(RangerDefaultAuditHandler.class);

	private static final String       CONF_AUDIT_ID_STRICT_UUID     = "xasecure.audit.auditid.strict.uuid";
	private static final boolean      DEFAULT_AUDIT_ID_STRICT_UUID  = false;


	private   final boolean         auditIdStrictUUID;
	protected final String          moduleName;
	private   final RangerRESTUtils restUtils      = new RangerRESTUtils();
	private         long            sequenceNumber = 0;
	private         String          UUID           = MiscUtil.generateUniqueId();
	private         AtomicInteger   counter        =  new AtomicInteger(0);



	public RangerDefaultAuditHandler() {
		auditIdStrictUUID = DEFAULT_AUDIT_ID_STRICT_UUID;
		moduleName        = RangerHadoopConstants.DEFAULT_RANGER_MODULE_ACL_NAME;
	}

	public RangerDefaultAuditHandler(Configuration config) {
		auditIdStrictUUID = config.getBoolean(CONF_AUDIT_ID_STRICT_UUID, DEFAULT_AUDIT_ID_STRICT_UUID);
		moduleName        = config.get(RangerHadoopConstants.AUDITLOG_RANGER_MODULE_ACL_NAME_PROP , RangerHadoopConstants.DEFAULT_RANGER_MODULE_ACL_NAME);
	}

	@Override
	public void processResult(RangerAccessResult result) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultAuditHandler.processResult(" + result + ")");
		}

		AuthzAuditEvent event = getAuthzEvents(result);

		logAuthzAudit(event);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultAuditHandler.processResult(" + result + ")");
		}
	}

	@Override
	public void processResults(Collection<RangerAccessResult> results) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultAuditHandler.processResults(" + results + ")");
		}

		Collection<AuthzAuditEvent> events = getAuthzEvents(results);

		if (events != null) {
			logAuthzAudits(events);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultAuditHandler.processResults(" + results + ")");
		}
	}


	public AuthzAuditEvent getAuthzEvents(RangerAccessResult result) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultAuditHandler.getAuthzEvents(" + result + ")");
		}

		AuthzAuditEvent ret = null;

		RangerAccessRequest request = result != null ? result.getAccessRequest() : null;

		if(request != null && result != null && result.getIsAudited()) {
			//RangerServiceDef     serviceDef   = result.getServiceDef();
			RangerAccessResource resource     = request.getResource();
			String               resourceType = resource == null ? null : resource.getLeafName();
			String               resourcePath = resource == null ? null : resource.getAsString();

			ret = createAuthzAuditEvent();

			ret.setRepositoryName(result.getServiceName());
			ret.setRepositoryType(result.getServiceType());
			ret.setResourceType(resourceType);
			ret.setResourcePath(resourcePath);
			ret.setRequestData(request.getRequestData());
			ret.setEventTime(request.getAccessTime() != null ? request.getAccessTime() : new Date());
			ret.setUser(request.getUser());
			ret.setAction(request.getAccessType());
			ret.setAccessResult((short) (result.getIsAllowed() ? 1 : 0));
			ret.setPolicyId(result.getPolicyId());
			ret.setAccessType(request.getAction());
			ret.setClientIP(request.getClientIPAddress());
			ret.setClientType(request.getClientType());
			ret.setSessionId(request.getSessionId());
			ret.setAclEnforcer(moduleName);
			Set<String> tags = getTags(request);
			if (tags != null) {
				ret.setTags(tags);
			}
			ret.setAdditionalInfo(getAdditionalInfo(request));
			ret.setClusterName(request.getClusterName());
			ret.setZoneName(result.getZoneName());
			ret.setAgentHostname(restUtils.getAgentHostname());
			ret.setPolicyVersion(result.getPolicyVersion());

			populateDefaults(ret);

			result.setAuditLogId(ret.getEventId());
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultAuditHandler.getAuthzEvents(" + result + "): " + ret);
		}

		return ret;
	}

	public Collection<AuthzAuditEvent> getAuthzEvents(Collection<RangerAccessResult> results) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultAuditHandler.getAuthzEvents(" + results + ")");
		}

		List<AuthzAuditEvent> ret = null;

		if(results != null) {
			// TODO: optimize the number of audit logs created
			for(RangerAccessResult result : results) {
				AuthzAuditEvent event = getAuthzEvents(result);

				if(event == null) {
					continue;
				}

				if(ret == null) {
					ret = new ArrayList<>();
				}

				ret.add(event);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultAuditHandler.getAuthzEvents(" + results + "): " + ret);
		}

		return ret;
	}

	public void logAuthzAudit(AuthzAuditEvent auditEvent) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultAuditHandler.logAuthzAudit(" + auditEvent + ")");
		}

		if(auditEvent != null) {
			populateDefaults(auditEvent);

			AuditHandler auditProvider = RangerBasePlugin.getAuditProvider(auditEvent.getRepositoryName());
			if (auditProvider == null || !auditProvider.log(auditEvent)) {
				MiscUtil.logErrorMessageByInterval(LOG, "fail to log audit event " + auditEvent);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultAuditHandler.logAuthzAudit(" + auditEvent + ")");
		}
	}

	private void populateDefaults(AuthzAuditEvent auditEvent) {
		if( auditEvent.getAclEnforcer() == null || auditEvent.getAclEnforcer().isEmpty()) {
			auditEvent.setAclEnforcer("ranger-acl"); // TODO: review
		}

		if (auditEvent.getAgentHostname() == null || auditEvent.getAgentHostname().isEmpty()) {
			auditEvent.setAgentHostname(MiscUtil.getHostname());
		}

		if (auditEvent.getLogType() == null || auditEvent.getLogType().isEmpty()) {
			auditEvent.setLogType("RangerAudit");
		}

		if (auditEvent.getEventId() == null || auditEvent.getEventId().isEmpty()) {
			auditEvent.setEventId(generateNextAuditEventId());
		}

		if (auditEvent.getAgentId() == null) {
			auditEvent.setAgentId(MiscUtil.getApplicationType());
		}

		auditEvent.setSeqNum(sequenceNumber++);
	}

	public void logAuthzAudits(Collection<AuthzAuditEvent> auditEvents) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerDefaultAuditHandler.logAuthzAudits(" + auditEvents + ")");
		}

		if(auditEvents != null) {
			for(AuthzAuditEvent auditEvent : auditEvents) {
				logAuthzAudit(auditEvent);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerDefaultAuditHandler.logAuthzAudits(" + auditEvents + ")");
		}
	}

	public AuthzAuditEvent createAuthzAuditEvent() {
		return new AuthzAuditEvent();
	}

	protected final Set<String> getTags(RangerAccessRequest request) {
		Set<String>     ret  = null;
		Set<RangerTagForEval> tags = RangerAccessRequestUtil.getRequestTagsFromContext(request.getContext());

		if (CollectionUtils.isNotEmpty(tags)) {
			ret = new HashSet<>();

			for (RangerTagForEval tag : tags) {
				ret.add(writeObjectAsString(tag));
			}
		}

		return ret;
	}

	public 	String getAdditionalInfo(RangerAccessRequest request) {
		if (StringUtils.isBlank(request.getRemoteIPAddress()) && CollectionUtils.isEmpty(request.getForwardedAddresses())) {
			return null;
		}
		Map<String,String> addInfomap=new HashMap<String,String>();
		addInfomap.put("forwarded-ip-addresses", "[" + StringUtils.join(request.getForwardedAddresses(), ", ") + "]");
		addInfomap.put("remote-ip-address", request.getRemoteIPAddress());
		String addInfojsonStr = JsonUtils.mapToJson(addInfomap);
		return addInfojsonStr;

	}

	private String generateNextAuditEventId() {
		final String ret;

		if (auditIdStrictUUID) {
			ret = MiscUtil.generateGuid();
		} else {
			int nextId = counter.getAndIncrement();

			if (nextId == Integer.MAX_VALUE) {
				// reset UUID and counter
				UUID    = MiscUtil.generateUniqueId();
				counter = new AtomicInteger(0);
			}

			ret = UUID + "-" + Integer.toString(nextId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("generateNextAuditEventId(): " + ret);
		}

		return ret;
	 }

	private String writeObjectAsString(Serializable obj) {
		String jsonStr = StringUtils.EMPTY;
		try {
			jsonStr = JsonUtilsV2.objToJson(obj);
		} catch (Exception e) {
			LOG.error("Cannot create JSON string for object:[" + obj + "]", e);
		}
		return jsonStr;
	}
}
