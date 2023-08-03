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
package org.apache.ranger.authorization.hbase;


import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.security.AccessDeniedException;
import org.apache.hadoop.hbase.security.User;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class AuthorizationSession {

	private static final Logger LOG = LoggerFactory.getLogger(AuthorizationSession.class.getName());
	// collaborator objects
	final HbaseFactory _factory = HbaseFactory.getInstance();
	final HbaseUserUtils _userUtils = _factory.getUserUtils();
	final HbaseAuthUtils _authUtils = _factory.getAuthUtils();
	// immutable state
	final RangerBasePlugin _authorizer;
	// Mutable state: Use supplied state information
	String _operation;
	String _otherInformation;
	String _access;
	String _table;
	String _column;
	String _columnFamily;
	String _remoteAddress;

	User _user;
	Set<String> _groups; // this exits to avoid having to get group for a user repeatedly.  It is kept in sync with _user;
	// Passing a null handler to policy engine would suppress audit logging.
	HbaseAuditHandler _auditHandler = null;
	boolean _superUser = false; // is this session for a super user?
	private RangerAccessRequest.ResourceMatchingScope _resourceMatchingScope = RangerAccessRequest.ResourceMatchingScope.SELF;

	// internal state per-authorization
	RangerAccessRequest _request;
	RangerAccessResult _result;
	
	public AuthorizationSession(RangerBasePlugin authorizer) {
		_authorizer = authorizer;
	}

	AuthorizationSession operation(String anOperation) {
		_operation = anOperation;
		return this;
	}

	AuthorizationSession otherInformation(String information) {
		_otherInformation = information;
		return this;
	}
	
	AuthorizationSession remoteAddress(String ipAddress) {
		_remoteAddress = ipAddress;
		return this;
	}
	
	AuthorizationSession access(String anAccess) {
		_access = anAccess;
		return this;
	}

	AuthorizationSession user(User aUser) {
		_user = aUser;
		if (_user == null) {
			LOG.warn("AuthorizationSession.user: user is null!");
			_groups = null;
		} else {
			_groups = _userUtils.getUserGroups(_user);
			if (_groups.isEmpty() && _user.getUGI() != null) {
				String[] groups = _user.getUGI().getGroupNames();
				if (groups != null) {
					_groups = Sets.newHashSet(groups);
				}
			}
			_superUser = _userUtils.isSuperUser(_user);
		}
		return this;
	}
	AuthorizationSession table(String aTable) {
		_table = aTable;
		return this;
	}

	AuthorizationSession columnFamily(String aColumnFamily) {
		_columnFamily = aColumnFamily;
		return this;
	}

	AuthorizationSession column(String aColumn) {
		_column = aColumn;
		return this;
	}

	void verifyBuildable() {
		
		String template = "Internal error: Incomplete/inconsisten state: [%s]. Can't build auth request!";
		if (_factory == null) {
			String message = String.format(template, "factory is null");
			LOG.error(message);
			throw new IllegalStateException(message);
		}
		if (_access == null || _access.isEmpty()) {
			String message = String.format(template, "access is null");
			LOG.error(message);
			throw new IllegalStateException(message);
		}
		if (_user == null) {
			String message = String.format(template, "user is null");
			LOG.error(message);
			throw new IllegalStateException(message);
		}
		if (isProvided(_columnFamily) && !isProvided(_table)) {
			String message = String.format(template, "Table must be provided if column-family is provided");
			LOG.error(message);
			throw new IllegalStateException(message);
		}
		if (isProvided(_column) && !isProvided(_columnFamily)) {
			String message = String.format(template, "Column family must be provided if column is provided");
			LOG.error(message);
			throw new IllegalStateException(message);
		}
	}

	void zapAuthorizationState() {
		_request = null;
		_result = null;
	}

	boolean isProvided(String aString) {
		return aString != null && !aString.isEmpty();
	}

	boolean isNameSpaceOperation() {
		return StringUtils.equals(_operation, "createNamespace") ||
				StringUtils.equals(_operation, "deleteNamespace") ||
				StringUtils.equals(_operation, "modifyNamespace") ||
				StringUtils.equals(_operation, "setUserNamespaceQuota") ||
				StringUtils.equals(_operation, "setNamespaceQuota") ||
				StringUtils.equals(_operation, "getUserPermissionForNamespace");
	}

	AuthorizationSession buildRequest() {

		verifyBuildable();
		// session can be reused so reset its state
		zapAuthorizationState();
		// TODO get this via a factory instead
		RangerAccessResourceImpl resource = new RangerHBaseResource();
		// policy engine should deal sensibly with null/empty values, if any
		if (isNameSpaceOperation() && StringUtils.isNotBlank(_otherInformation)) {
				resource.setValue(RangerHBaseResource.KEY_TABLE, _otherInformation + RangerHBaseResource.NAMESPACE_SEPARATOR);
		} else {
			resource.setValue(RangerHBaseResource.KEY_TABLE, _table);
		}
		resource.setValue(RangerHBaseResource.KEY_COLUMN_FAMILY, _columnFamily);
		resource.setValue(RangerHBaseResource.KEY_COLUMN, _column);
		
		String user = _userUtils.getUserAsString(_user);
		RangerAccessRequestImpl request = new RangerAccessRequestImpl(resource, _access, user, _groups, null);
		request.setAction(_operation);
		request.setRequestData(_otherInformation);
		request.setClientIPAddress(_remoteAddress);
		request.setResourceMatchingScope(_resourceMatchingScope);
		request.setAccessTime(new Date());
		
		_request = request;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Built request: " + request.toString());
		}
		return this;
	}
	
	AuthorizationSession authorize() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AuthorizationSession.authorize: " + getRequestMessage());
		}
		
		if (_request == null) {
			String message = String.format("Invalid state transition: buildRequest() must be called before authorize().  This request would ultimately get denied.!");
			throw new IllegalStateException(message);
		} else {
			// ok to pass potentially null handler to policy engine.  Null handler effectively suppresses the audit.
			if (_auditHandler != null && _superUser) {
				if (LOG.isDebugEnabled()) {
	                LOG.debug("Setting super-user override on audit handler");
				}
				_auditHandler.setSuperUserOverride(_superUser);
			}
			_result = _authorizer.isAccessAllowed(_request, _auditHandler);
		}
		
		if (LOG.isDebugEnabled()) {
			boolean allowed = isAuthorized();
			String reason = getDenialReason();
			LOG.debug("<== AuthorizationSession.authorize: " + getLogMessage(allowed, reason));
		}
		return this;
	}
	
	void logCapturedEvents() {
		if (_auditHandler != null) {
			List<AuthzAuditEvent> events = _auditHandler.getCapturedEvents();
			_auditHandler.logAuthzAudits(events);
		}
	}
	
	void publishResults() throws AccessDeniedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AuthorizationSession.publishResults()");
		}
		
		boolean authorized = isAuthorized();
		if (_auditHandler != null && isAudited()) {
			List<AuthzAuditEvent> events = null;
			/*
			 * What we log to audit depends on authorization status.  For success we log all accumulated events.  In case of failure
			 * we log just the last set of audit messages as we only need to record the cause of overall denial.
			 */
			if (authorized) {
				List<AuthzAuditEvent> theseEvents = _auditHandler.getCapturedEvents();
				if (theseEvents != null && !theseEvents.isEmpty()) {
					events = theseEvents;
				}
			} else {
				AuthzAuditEvent event = _auditHandler.getAndDiscardMostRecentEvent();
				if (event != null) {
					events = Lists.newArrayList(event);
				}
			}
			if (LOG.isDebugEnabled()) {
				int size = events == null ? 0 : events.size();
				String auditMessage = events == null ? "" : events.toString();
				String message = String.format("Writing %d messages to audit: [%s]", size, auditMessage);
				LOG.debug(message);
			}
			_auditHandler.logAuthzAudits(events);
		}
		if (!authorized) {
			// and throw and exception... callers expect this behavior
			String reason = getDenialReason();
			String message = getLogMessage(false, reason);
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== AuthorizationSession.publishResults: throwing exception: " + message);
			}
			throw new AccessDeniedException("Insufficient permissions for user '" + _user.getName() + "' (action=" + _access + ")");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AuthorizationSession.publishResults()");
		}
	}
	
	boolean isAudited() {

		boolean audited = false;
		if (_result == null) {
			String message = String.format("Internal error: _result was null!  Assuming no audit. Request[%s]", _request.toString());
			LOG.error(message);
		} else {
			audited = _result.getIsAudited();
		}
		return audited;
	}

	boolean isAuthorized() {
		boolean allowed = false;
		if (_result == null) {
			String message = String.format("Internal error: _result was null! Returning false.");
			LOG.error(message);
		} else {
			allowed = _result.getIsAllowed();
		}
		if (!allowed && _superUser) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("User [" + _user + "] is a superUser!  Overriding policy engine's decision.  Request is deemed authorized!");
			}
			allowed = true;
		}
		return allowed;
	}
	
	String getDenialReason() {
		String reason = "";
		if (_result == null) {
			String message = String.format("Internal error: _result was null!  Returning empty reason.");
			LOG.error(message);
		} else {
			boolean allowed = _result.getIsAllowed();
			if (!allowed) {
				reason = _result.getReason();
			}
		}
		return reason;
	}
	
	String requestToString() {
		return MoreObjects.toStringHelper(_request.getClass())
			.add("operation", _operation)
			.add("otherInformation", _otherInformation)
			.add("access", _access)
			.add("user", _user == null ? null : _user.getName())
			.add("groups", _groups)
			.add("auditHandler", _auditHandler == null ? null : _auditHandler.getClass().getSimpleName())
			.add(RangerHBaseResource.KEY_TABLE, _table)
			.add(RangerHBaseResource.KEY_COLUMN, _column)
			.add(RangerHBaseResource.KEY_COLUMN_FAMILY, _columnFamily)
			.add("resource-matching-scope", _resourceMatchingScope)
			.toString();
	}

	String getPrintableValue(String value) {
		if (isProvided(value)) {
			return value;
		} else {
			return "";
		}
	}
	
	String getRequestMessage() {
		String format = "Access[%s] by user[%s] belonging to groups[%s] to table[%s] for column-family[%s], column[%s] triggered by operation[%s], otherInformation[%s]";
		String user = _userUtils.getUserAsString();
		String message = String.format(format, getPrintableValue(_access), getPrintableValue(user), _groups, getPrintableValue(_table),
				getPrintableValue(_columnFamily), getPrintableValue(_column), getPrintableValue(_operation), getPrintableValue(_otherInformation));
		return message;
	}
	
	String getLogMessage(boolean allowed, String reason) {
		String format = " %s: status[%s], reason[%s]";
		String message = String.format(format, getRequestMessage(), allowed ? "allowed" : "denied", reason);
		return message;
	}

	/**
	 * This method could potentially null out an earlier audit handler -- which effectively would suppress audits.
	 * @param anAuditHandler
	 * @return
	 */
	AuthorizationSession auditHandler(HbaseAuditHandler anAuditHandler) {
		_auditHandler = anAuditHandler;
		return this;
	}

	AuthorizationSession resourceMatchingScope(RangerAccessRequest.ResourceMatchingScope scope) {
		_resourceMatchingScope = scope;
		return this;
	}
}
