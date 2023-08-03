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

package org.apache.ranger.amazon.cloudwatch;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.view.VXAccessAudit;
import org.apache.ranger.view.VXAccessAuditList;
import org.apache.ranger.view.VXLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.FilteredLogEvent;

@Service
@Scope("singleton")
public class CloudWatchAccessAuditsService extends org.apache.ranger.AccessAuditsService {
	private static final Logger LOGGER = Logger.getLogger(CloudWatchAccessAuditsService.class);

	@Autowired
	CloudWatchMgr cloudWatchMgr;

	@Autowired
	CloudWatchUtil cloudWatchUtil;

	@Autowired
	JSONUtil jsonUtil;

	public VXAccessAuditList searchXAccessAudits(SearchCriteria searchCriteria) {

		final boolean hiveQueryVisibility = PropertiesUtil.getBooleanProperty("ranger.audit.hive.query.visibility", true);
		AWSLogs client = cloudWatchMgr.getClient();
		if (client == null) {
			LOGGER.warn("CloudWatch client is null, so not running the query.");
			throw restErrorUtil.createRESTException("Error connecting to cloudwatch", MessageEnums.ERROR_SYSTEM);
		}

		List<VXAccessAudit> xAccessAuditList = new ArrayList<VXAccessAudit>();
		Map<String, Object> paramList = searchCriteria.getParamList();
		updateUserExclusion(paramList);

		List<FilteredLogEvent> result;
		try {
			result = cloudWatchUtil.searchResources(client, searchCriteria, searchFields, sortFields);
		} catch (Exception e) {
			LOGGER.warn(String.format("CloudWatch query failed: %s", e.getMessage()));
			throw restErrorUtil.createRESTException("Error querying search engine", MessageEnums.ERROR_SYSTEM);
		}

		VXAccessAuditList returnList = new VXAccessAuditList();
		if (result != null && CollectionUtils.isNotEmpty(result)) {
			int recordCount = 0;
			int endIndex = result.size() - 1;
			endIndex = endIndex - searchCriteria.getStartIndex() < 0 ? endIndex : endIndex - searchCriteria.getStartIndex();
			for (int index = endIndex; recordCount < searchCriteria.getMaxRows() && index >=0 ; index--) {
				FilteredLogEvent event = result.get(index);
				AuthzAuditEvent auditEvent = null;
				try {
					auditEvent = MiscUtil.fromJson(event.getMessage(), AuthzAuditEvent.class);
				} catch (Exception ex) {
					LOGGER.error("Error while parsing json data" , ex);
				}
				VXAccessAudit vXAccessAudit = populateViewBean(auditEvent);
				if (vXAccessAudit != null) {
					String serviceType = vXAccessAudit.getServiceType();
					boolean isHive = "hive".equalsIgnoreCase(serviceType);
					if (!hiveQueryVisibility && isHive) {
						vXAccessAudit.setRequestData(null);
					} else if (isHive) {
						String accessType = vXAccessAudit.getAccessType();
						if ("grant".equalsIgnoreCase(accessType) || "revoke".equalsIgnoreCase(accessType)) {
							String requestData = vXAccessAudit.getRequestData();
							if (requestData != null) {
								try {
									vXAccessAudit.setRequestData(java.net.URLDecoder.decode(requestData, "UTF-8"));
								} catch (UnsupportedEncodingException e) {
									LOGGER.warn("Error while encoding request data: " + requestData, e);
								}
							} else {
								LOGGER.warn("Error in request data of audit from cloudwatch. AuditData: "+ vXAccessAudit.toString());
							}
						}
					}
				}
				xAccessAuditList.add(vXAccessAudit);
				recordCount++;
			}
			returnList.setResultSize(result.size());
			returnList.setTotalCount(result.size());
		}

		returnList.setPageSize(searchCriteria.getMaxRows());
		returnList.setStartIndex(searchCriteria.getStartIndex());
		returnList.setVXAccessAudits(xAccessAuditList);
		return returnList;
	}

	public void setRestErrorUtil(RESTErrorUtil restErrorUtil) {
		this.restErrorUtil = restErrorUtil;
	}

	public VXLong getXAccessAuditSearchCount(SearchCriteria searchCriteria) {
		long count = 100;
		VXLong vXLong = new VXLong();
		vXLong.setValue(count);
		return vXLong;
	}

	private VXAccessAudit populateViewBean(AuthzAuditEvent auditEvent) {
		VXAccessAudit accessAudit = new VXAccessAudit();

		Object value = null;
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("doc=" + auditEvent.toString());
		}

		value = auditEvent.getEventId();
		if (value != null) {
			accessAudit.setId((long) value.hashCode());
			accessAudit.setEventId(value.toString());
		}

		value = auditEvent.getClusterName();
		if (value != null) {
			accessAudit.setClusterName(value.toString());
		}

		value = auditEvent.getZoneName();
		if (value != null) {
			accessAudit.setZoneName(value.toString());
		}

		value = auditEvent.getAgentHostname();
		if (value != null) {
			accessAudit.setAgentHost(value.toString());
		}

		value = auditEvent.getPolicyVersion();
		if (value != null) {
			accessAudit.setPolicyVersion(MiscUtil.toLong(value));
		}

		value = auditEvent.getAccessType();
		if (value != null) {
			accessAudit.setAccessType(value.toString());
		}

		value = auditEvent.getAclEnforcer();
		if (value != null) {
			accessAudit.setAclEnforcer(value.toString());
		}

		value = auditEvent.getAgentId();
		if (value != null) {
			accessAudit.setAgentId(value.toString());
		}

		value = auditEvent.getRepositoryName();
		if (value != null) {
			accessAudit.setRepoName(value.toString());
			XXService xxService = daoManager.getXXService().findByName(accessAudit.getRepoName());

			if(xxService != null) {
				accessAudit.setRepoDisplayName(xxService.getDisplayName());
			}
		}

		value = auditEvent.getSessionId();
		if (value != null) {
			accessAudit.setSessionId(value.toString());
		}

		value = auditEvent.getUser();
		if (value != null) {
			accessAudit.setRequestUser(value.toString());
		}

		value = auditEvent.getRequestData();
		if (value != null) {
			accessAudit.setRequestData(value.toString());
		}
		value = auditEvent.getResourcePath();
		if (value != null) {
			accessAudit.setResourcePath(value.toString());
		}

		value = auditEvent.getClientIP();
		if (value != null) {
			accessAudit.setClientIP(value.toString());
		}

		value = auditEvent.getAccessResult();
		if (value != null) {
			accessAudit.setAccessResult(MiscUtil.toInt(value));
		}

		value = auditEvent.getPolicyId();
		if (value != null) {
			accessAudit.setPolicyId(MiscUtil.toLong(value));
		}

		value = auditEvent.getRepositoryType();
		if (value != null) {
			accessAudit.setRepoType(MiscUtil.toInt(value));
			XXServiceDef xServiceDef = daoManager.getXXServiceDef().getById((long) accessAudit.getRepoType());
			if (xServiceDef != null) {
				accessAudit.setServiceType(xServiceDef.getName());
				accessAudit.setServiceTypeDisplayName(xServiceDef.getDisplayName());
			}
		}

		value = auditEvent.getResourceType();
		if (value != null) {
			accessAudit.setResourceType(value.toString());
		}

		value = auditEvent.getResultReason();
		if (value != null) {
			accessAudit.setResultReason(value.toString());
		}

		value = auditEvent.getAction();
		if (value != null) {
			accessAudit.setAction(value.toString());
		}

		value = auditEvent.getEventTime();
		if (value != null) {
			accessAudit.setEventTime(MiscUtil.toLocalDate(value));
		}

		value = auditEvent.getSeqNum();
		if (value != null) {
			accessAudit.setSequenceNumber(MiscUtil.toLong(value));
		}

		value = auditEvent.getEventCount();
		if (value != null) {
			accessAudit.setEventCount(MiscUtil.toLong(value));
		}

		value = auditEvent.getEventDurationMS();
		if (value != null) {
			accessAudit.setEventDuration(MiscUtil.toLong(value));
		}

		value = auditEvent.getTags();
		if (value != null) {
			accessAudit.setTags(value.toString());
		}

		return accessAudit;
	}

}