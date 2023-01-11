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

package org.apache.ranger.solr;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ranger.AccessAuditsService;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.view.VXAccessAudit;
import org.apache.ranger.view.VXAccessAuditList;
import org.apache.ranger.view.VXLong;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class SolrAccessAuditsService extends AccessAuditsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrAccessAuditsService.class);

	@Autowired
	SolrMgr solrMgr;

	@Autowired
	SolrUtil solrUtil;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	RangerDaoManager daoManager;


	public VXAccessAuditList searchXAccessAudits(SearchCriteria searchCriteria) {

		// Make call to Solr
		SolrClient solrClient = solrMgr.getSolrClient();
		final boolean hiveQueryVisibility = PropertiesUtil.getBooleanProperty("ranger.audit.hive.query.visibility", true);
		if (solrClient == null) {
			LOGGER.warn("Solr client is null, so not running the query.");
			throw restErrorUtil.createRESTException(
					"Error connecting to search engine",
					MessageEnums.ERROR_SYSTEM);
		}
		List<VXAccessAudit> xAccessAuditList = new ArrayList<VXAccessAudit>();

		Map<String, Object> paramList = searchCriteria.getParamList();

		Object eventIdObj = paramList.get("eventId");
		if (eventIdObj != null) {
			paramList.put("id", eventIdObj.toString());
		}

		updateUserExclusion(paramList);

		QueryResponse response = solrUtil.searchResources(searchCriteria,
				searchFields, sortFields, solrClient);
		SolrDocumentList docs = response.getResults();
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			VXAccessAudit vXAccessAudit = populateViewBean(doc);
                        if (vXAccessAudit != null) {
                                if (!hiveQueryVisibility && "hive".equalsIgnoreCase(vXAccessAudit.getServiceType())) {
                                        vXAccessAudit.setRequestData(null);
                                }
                                else if("hive".equalsIgnoreCase(vXAccessAudit.getServiceType()) && ("grant".equalsIgnoreCase(vXAccessAudit.getAccessType()) || "revoke".equalsIgnoreCase(vXAccessAudit.getAccessType()))){
                                        try {
                                            if (vXAccessAudit.getRequestData() != null) {
                                                vXAccessAudit.setRequestData(java.net.URLDecoder.decode(vXAccessAudit.getRequestData(), "UTF-8"));
                                            } else {
                                                LOGGER.warn("Error in request data of audit from solr. AuditData: "  + vXAccessAudit.toString());
                                            }
                                        } catch (UnsupportedEncodingException e) {
                                                LOGGER.warn("Error while encoding request data");
                                        }
                                }
                        }
                        xAccessAuditList.add(vXAccessAudit);
		}

		VXAccessAuditList returnList = new VXAccessAuditList();
		returnList.setPageSize(searchCriteria.getMaxRows());
		returnList.setResultSize(docs.size());
		returnList.setTotalCount((int) docs.getNumFound());
		returnList.setStartIndex((int) docs.getStart());
		returnList.setVXAccessAudits(xAccessAuditList);
		return returnList;
	}

	/**
	 * @param doc
	 * @return
	 */
	private VXAccessAudit populateViewBean(SolrDocument doc) {
		VXAccessAudit accessAudit = new VXAccessAudit();

		Object value = null;
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("doc=" + doc.toString());
		}

		value = doc.getFieldValue("id");
		if (value != null) {
			// TODO: Converting ID to hashcode for now
			accessAudit.setId((long) value.hashCode());
			accessAudit.setEventId(value.toString());
		}
		
		value = doc.getFieldValue("cluster");
		if (value != null) {
			accessAudit.setClusterName(value.toString());
		}

		value = doc.getFieldValue("zoneName");
		if (value != null) {
			accessAudit.setZoneName(value.toString());
		}

		value = doc.getFieldValue("agentHost");
		if (value != null) {
			accessAudit.setAgentHost(value.toString());
		}

		value = doc.getFieldValue("policyVersion");
		if (value != null) {
			accessAudit.setPolicyVersion(MiscUtil.toLong(value));
		}

		value = doc.getFieldValue("access");
		if (value != null) {
			accessAudit.setAccessType(value.toString());
		}

		value = doc.getFieldValue("enforcer");
		if (value != null) {
			accessAudit.setAclEnforcer(value.toString());
		}
		value = doc.getFieldValue("agent");
		if (value != null) {
			accessAudit.setAgentId(value.toString());
		}
		value = doc.getFieldValue("repo");
		if (value != null) {
			accessAudit.setRepoName(value.toString());
			XXService xxService = daoManager.getXXService().findByName(accessAudit.getRepoName());

			if(xxService != null) {
				accessAudit.setRepoDisplayName(xxService.getDisplayName());
			}
		}
		value = doc.getFieldValue("sess");
		if (value != null) {
			accessAudit.setSessionId(value.toString());
		}
		value = doc.getFieldValue("reqUser");
		if (value != null) {
			accessAudit.setRequestUser(value.toString());
		}
		value = doc.getFieldValue("reqData");
		if (value != null) {
			accessAudit.setRequestData(value.toString());
		}
		value = doc.getFieldValue("resource");
		if (value != null) {
			accessAudit.setResourcePath(value.toString());
		}
		value = doc.getFieldValue("cliIP");
		if (value != null) {
			accessAudit.setClientIP(value.toString());
		}
		value = doc.getFieldValue("logType");
		//if (value != null) {
			// TODO: Need to see what logType maps to in UI
//			accessAudit.setAuditType(solrUtil.toInt(value));
		//}
		value = doc.getFieldValue("result");
		if (value != null) {
			accessAudit.setAccessResult(MiscUtil.toInt(value));
		}
		value = doc.getFieldValue("policy");
		if (value != null) {
			accessAudit.setPolicyId(MiscUtil.toLong(value));
		}
		value = doc.getFieldValue("repoType");
		if (value != null) {
			accessAudit.setRepoType(MiscUtil.toInt(value));
			XXServiceDef xServiceDef = daoManager.getXXServiceDef().getById((long) accessAudit.getRepoType());
			if (xServiceDef != null) {
				accessAudit.setServiceType(xServiceDef.getName());
				accessAudit.setServiceTypeDisplayName(xServiceDef.getDisplayName());
			}
		}
		value = doc.getFieldValue("resType");
		if (value != null) {
			accessAudit.setResourceType(value.toString());
		}
		value = doc.getFieldValue("reason");
		if (value != null) {
			accessAudit.setResultReason(value.toString());
		}
		value = doc.getFieldValue("action");
		if (value != null) {
			accessAudit.setAction(value.toString());
		}
		value = doc.getFieldValue("evtTime");
		if (value != null) {
			accessAudit.setEventTime(MiscUtil.toDate(value));
		}
		value = doc.getFieldValue("seq_num");
		if (value != null) {
			accessAudit.setSequenceNumber(MiscUtil.toLong(value));
		}
		value = doc.getFieldValue("event_count");
		if (value != null) {
			accessAudit.setEventCount(MiscUtil.toLong(value));
		}
		value = doc.getFieldValue("event_dur_ms");
		if (value != null) {
			accessAudit.setEventDuration(MiscUtil.toLong(value));
		}
		value = doc.getFieldValue("tags");
		if (value != null) {
			accessAudit.setTags(value.toString());
		}
		return accessAudit;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXLong getXAccessAuditSearchCount(SearchCriteria searchCriteria) {
		long count = 100;

		VXLong vXLong = new VXLong();
		vXLong.setValue(count);
		return vXLong;
	}

}
