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

package org.apache.ranger.authorization.hive.authorizer;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class RangerHiveAuditHandler extends RangerDefaultAuditHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RangerDefaultAuditHandler.class);

	public static final String  ACCESS_TYPE_ROWFILTER = "ROW_FILTER";
	public static final String  ACCESS_TYPE_INSERT    = "INSERT";
	public static final String  ACCESS_TYPE_UPDATE    = "UPDATE";
	public static final String  ACCESS_TYPE_DELETE    = "DELETE";
	public static final String  ACCESS_TYPE_TRUNCATE  = "TRUNCATE";
	public static final String  ACTION_TYPE_METADATA_OPERATION = "METADATA OPERATION";
	public static final String  URL_RESOURCE_TYPE = "url";

	public static final String CONF_AUDIT_QUERY_REQUEST_SIZE = "xasecure.audit.solr.limit.query.req.size";
	public static final int DEFAULT_CONF_AUDIT_QUERY_REQUEST_SIZE = Integer.MAX_VALUE;
	private int requestQuerySize;
	Collection<AuthzAuditEvent> auditEvents  = null;
	boolean                     deniedExists = false;

	Set<String> roleOperationCmds = new HashSet<>(Arrays.asList(HiveOperationType.CREATEROLE.name(), HiveOperationType.DROPROLE.name(),
			HiveOperationType.SHOW_ROLES.name(), HiveOperationType.SHOW_ROLE_GRANT.name(),
			HiveOperationType.SHOW_ROLE_PRINCIPALS.name(), HiveOperationType.GRANT_ROLE.name(),
			HiveOperationType.REVOKE_ROLE.name()));

	public RangerHiveAuditHandler() {
		super();
		requestQuerySize = DEFAULT_CONF_AUDIT_QUERY_REQUEST_SIZE;
	}

	public RangerHiveAuditHandler(Configuration config) {
		super(config);
		requestQuerySize = config.getInt(CONF_AUDIT_QUERY_REQUEST_SIZE, DEFAULT_CONF_AUDIT_QUERY_REQUEST_SIZE);
		requestQuerySize = (requestQuerySize < 1) ? DEFAULT_CONF_AUDIT_QUERY_REQUEST_SIZE : requestQuerySize;
	}

	AuthzAuditEvent createAuditEvent(RangerAccessResult result, String accessType, String resourcePath) {
		RangerAccessRequest  request      = result.getAccessRequest();
		RangerAccessResource resource     = request.getResource();
		String               resourceType = resource != null ? resource.getLeafName() : null;

		AuthzAuditEvent auditEvent = super.getAuthzEvents(result);

		String resourcePathComputed = resourcePath;
		if (URL_RESOURCE_TYPE.equals(resourceType)) {
			resourcePathComputed = getURLPathString(resource, resourcePathComputed);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("requestQuerySize = " + requestQuerySize);
		}
		if (StringUtils.isNotBlank(request.getRequestData()) && request.getRequestData().length()>requestQuerySize) {
			auditEvent.setRequestData(request.getRequestData().substring(0, requestQuerySize));
		} else {
			auditEvent.setRequestData(request.getRequestData());
		}
		auditEvent.setAccessType(accessType);
		auditEvent.setResourcePath(resourcePathComputed);
		auditEvent.setResourceType("@" + resourceType); // to be consistent with earlier release

		if (request instanceof RangerHiveAccessRequest && resource instanceof RangerHiveResource) {
			RangerHiveAccessRequest hiveAccessRequest = (RangerHiveAccessRequest) request;
			RangerHiveResource hiveResource = (RangerHiveResource) resource;
			HiveAccessType hiveAccessType = hiveAccessRequest.getHiveAccessType();

			if (hiveAccessType == HiveAccessType.USE && hiveResource.getObjectType() == HiveObjectType.DATABASE && StringUtils.isBlank(hiveResource.getDatabase())) {
				// this should happen only for SHOWDATABASES
				auditEvent.setTags(null);
			}

			if (hiveAccessType == HiveAccessType.REPLADMIN ) {
				// In case of REPL commands Audit should show what REPL Command instead of REPLADMIN access type
				String context = request.getRequestData();
					String replAccessType = getReplCmd(context);
					auditEvent.setAccessType(replAccessType);
			}

			if (hiveAccessType == HiveAccessType.SERVICEADMIN) {
				String hiveOperationType = request.getAction();
				String commandStr = request.getRequestData();
				if (HiveOperationType.KILL_QUERY.name().equalsIgnoreCase(hiveOperationType)) {
					String queryId = getServiceAdminQueryId(commandStr);
					if (!StringUtils.isEmpty(queryId)) {
						auditEvent.setRequestData(queryId);
					}
					commandStr = getServiceAdminCmd(commandStr);
					if (StringUtils.isEmpty(commandStr)) {
						commandStr = hiveAccessType.name();
					}
				}
				auditEvent.setAccessType(commandStr);
			}

			String action = request.getAction();
			if (hiveResource.getObjectType() == HiveObjectType.GLOBAL && isRoleOperation(action)) {
				auditEvent.setAccessType(action);
			}
		}

		return auditEvent;
	}
	
	AuthzAuditEvent createAuditEvent(RangerAccessResult result) {

		AuthzAuditEvent ret = null;

		RangerAccessRequest  request  = result.getAccessRequest();
		RangerAccessResource resource = request.getResource();
		String               resourcePath = resource != null ? resource.getAsString() : null;
		int                  policyType = result.getPolicyType();

		if (policyType == RangerPolicy.POLICY_TYPE_DATAMASK && result.isMaskEnabled()) {
		    ret = createAuditEvent(result, result.getMaskType(), resourcePath);
        } else if (policyType == RangerPolicy.POLICY_TYPE_ROWFILTER) {
            ret = createAuditEvent(result, ACCESS_TYPE_ROWFILTER, resourcePath);
		} else if (policyType == RangerPolicy.POLICY_TYPE_ACCESS) {
			String accessType = null;

			if (request instanceof RangerHiveAccessRequest) {
				RangerHiveAccessRequest hiveRequest = (RangerHiveAccessRequest) request;

				accessType = hiveRequest.getHiveAccessType().toString();

				String action = request.getAction();
				if (ACTION_TYPE_METADATA_OPERATION.equals(action)) {
					accessType = ACTION_TYPE_METADATA_OPERATION;
				} else if (HiveAccessType.UPDATE.toString().equalsIgnoreCase(accessType)) {
					String commandStr = request.getRequestData();
					if (StringUtils.isNotBlank(commandStr)) {
						if (StringUtils.startsWithIgnoreCase(commandStr, ACCESS_TYPE_INSERT)) {
							accessType = ACCESS_TYPE_INSERT;
						} else if (StringUtils.startsWithIgnoreCase(commandStr, ACCESS_TYPE_UPDATE)) {
							accessType = ACCESS_TYPE_UPDATE;
						} else if (StringUtils.startsWithIgnoreCase(commandStr, ACCESS_TYPE_DELETE)) {
							accessType = ACCESS_TYPE_DELETE;
						} else if (StringUtils.startsWithIgnoreCase(commandStr, ACCESS_TYPE_TRUNCATE)) {
							accessType = ACCESS_TYPE_TRUNCATE;
						}
					}
				}
			}

			if (StringUtils.isEmpty(accessType)) {
				accessType = request.getAccessType();
			}

			ret = createAuditEvent(result, accessType, resourcePath);
		}

		return ret;
	}

	List<AuthzAuditEvent> createAuditEvents(Collection<RangerAccessResult> results) {

		Map<Long, AuthzAuditEvent> auditEvents = new HashMap<Long, AuthzAuditEvent>();
		Iterator<RangerAccessResult> iterator = results.iterator();
		AuthzAuditEvent deniedAuditEvent = null;
		while (iterator.hasNext() && deniedAuditEvent == null) {
			RangerAccessResult result = iterator.next();
			if(result.getIsAudited()) {
				if (!result.getIsAllowed()) {
					deniedAuditEvent = createAuditEvent(result);
				} else {
					long policyId = result.getPolicyId();
					if (auditEvents.containsKey(policyId)) { // add this result to existing event by updating column values
						AuthzAuditEvent auditEvent = auditEvents.get(policyId);
						RangerHiveAccessRequest request    = (RangerHiveAccessRequest)result.getAccessRequest();
						RangerHiveResource resource   = (RangerHiveResource)request.getResource();
						String resourcePath = auditEvent.getResourcePath() + "," + resource.getColumn();
						auditEvent.setResourcePath(resourcePath);
						Set<String> tags = getTags(request);
						if (tags != null) {
							auditEvent.getTags().addAll(tags);
						}
					} else { // new event as this approval was due to a different policy.
						AuthzAuditEvent auditEvent = createAuditEvent(result);

						if(auditEvent != null) {
							auditEvents.put(policyId, auditEvent);
						}
					}
				}
			}
		}
		List<AuthzAuditEvent> result;
		if (deniedAuditEvent == null) {
			result = new ArrayList<>(auditEvents.values());
		} else {
			result = Lists.newArrayList(deniedAuditEvent);
		}
		
		return result;
	}

	@Override
	public void processResult(RangerAccessResult result) {
		if(! result.getIsAudited()) {
			return;
		}

		if  (skipFilterOperationAuditing(result)) {
			return;
		}

		AuthzAuditEvent auditEvent = createAuditEvent(result);

		if(auditEvent != null) {
			addAuthzAuditEvent(auditEvent);
		}
	}

	/**
	 * This method is expected to be called ONLY to process the results for multiple-columns in a table.
	 * To ensure this, RangerHiveAuthorizer should call isAccessAllowed(Collection<requests>) only for this condition
	 */
	@Override
	public void processResults(Collection<RangerAccessResult> results) {
		List<AuthzAuditEvent> auditEvents = createAuditEvents(results);
		for(AuthzAuditEvent auditEvent : auditEvents) {
			addAuthzAuditEvent(auditEvent);
		}
	}

	public void logAuditEventForDfs(String userName, String dfsCommand, boolean accessGranted, int repositoryType, String repositoryName) {
		AuthzAuditEvent auditEvent = new AuthzAuditEvent();

		auditEvent.setAclEnforcer(moduleName);
		auditEvent.setResourceType("@dfs"); // to be consistent with earlier release
		auditEvent.setAccessType("DFS");
		auditEvent.setAction("DFS");
		auditEvent.setUser(userName);
		auditEvent.setAccessResult((short)(accessGranted ? 1 : 0));
		auditEvent.setEventTime(new Date());
		auditEvent.setRepositoryType(repositoryType);
		auditEvent.setRepositoryName(repositoryName);
		auditEvent.setRequestData(dfsCommand);

		auditEvent.setResourcePath(dfsCommand);

		if(LOG.isDebugEnabled()){
			LOG.debug("Logging DFS event " + auditEvent.toString());
		}

		addAuthzAuditEvent(auditEvent);
    }

    public void flushAudit() {
    	if(auditEvents == null) {
    		return;
    	}

    	for(AuthzAuditEvent auditEvent : auditEvents) {
    		if(deniedExists && auditEvent.getAccessResult() != 0) { // if deny exists, skip logging for allowed results
    			continue;
    		}

    		super.logAuthzAudit(auditEvent);
    	}
    }

    private void addAuthzAuditEvent(AuthzAuditEvent auditEvent) {
    	if(auditEvent != null) {
    		if(auditEvents == null) {
    			auditEvents = new ArrayList<AuthzAuditEvent>();
    		}
    		
    		auditEvents.add(auditEvent);
    		
    		if(auditEvent.getAccessResult() == 0) {
    			deniedExists = true;
    		}
    	}
    }

    private String getReplCmd(String cmdString) {
		String ret = "REPL";
		if (cmdString != null) {
			String[] cmd = cmdString.trim().split("\\s+");
			if (!ArrayUtils.isEmpty(cmd) && cmd.length > 2) {
				ret = cmd[0] + " " + cmd[1];
			}
		}
		return ret;
	}

	private String getServiceAdminCmd(String cmdString) {
		String ret = "SERVICE ADMIN";
		if (cmdString != null) {
			String[] cmd = cmdString.trim().split("\\s+");
			if (!ArrayUtils.isEmpty(cmd) && cmd.length > 1) {
				ret = cmd[0] + " " + cmd[1];
			}
		}
		return ret;
	}

	private String getServiceAdminQueryId(String cmdString) {
		String ret = "QUERY ID = ";
		if (cmdString != null) {
			String[] cmd = cmdString.trim().split("\\s+");
			if (!ArrayUtils.isEmpty(cmd) && cmd.length > 2) {
				ret = ret + cmd[2];
			}
		}
		return ret;
	}

	private boolean skipFilterOperationAuditing(RangerAccessResult result) {
		boolean ret = false;
		RangerAccessRequest accessRequest = result.getAccessRequest();
		if (accessRequest != null) {
			String action = accessRequest.getAction();
			if (ACTION_TYPE_METADATA_OPERATION.equals(action) && !result.getIsAllowed()) {
				ret = true;
			}
		}
		return ret;
	}

	private boolean isRoleOperation(String action) {
		boolean ret = false;
		if (roleOperationCmds.contains(action)) {
			ret = true;
		}
		return ret;
	}

	private String getURLPathString(RangerAccessResource resource, String resourcePath) {
		String ret = resourcePath;
		ArrayList<String> resourcePathVal;
		Object val = resource.getValue(URL_RESOURCE_TYPE);
		if (val instanceof List<?>) {
			resourcePathVal = (ArrayList<String>) val;
			if (CollectionUtils.isNotEmpty(resourcePathVal)) {
				ret = resourcePathVal.get(0);
			}
		}
		return ret;
	}
}
