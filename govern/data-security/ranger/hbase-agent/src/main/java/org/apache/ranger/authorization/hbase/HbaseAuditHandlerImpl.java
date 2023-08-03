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

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbaseAuditHandlerImpl extends RangerDefaultAuditHandler implements HbaseAuditHandler {

	private static final Logger LOG = LoggerFactory.getLogger(HbaseAuditHandlerImpl.class);
	static final List<AuthzAuditEvent> _EmptyList = new ArrayList<AuthzAuditEvent>();
	final List<AuthzAuditEvent> _allEvents = new ArrayList<AuthzAuditEvent>();
	// we replace its contents anytime new audit events are generated.
	AuthzAuditEvent _mostRecentEvent = null;
	boolean _superUserOverride = false;
	
	@Override
	public AuthzAuditEvent getAuthzEvents(RangerAccessResult result) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.getAuthzEvents(" + result + ")");
		}

		resetResourceForAudit(result.getAccessRequest());
		AuthzAuditEvent event = super.getAuthzEvents(result);
		// first accumulate last set of events and then capture these as the most recent ones
		if (_mostRecentEvent != null) {
			LOG.debug("getAuthzEvents: got one event from default audit handler");
			_allEvents.add(_mostRecentEvent);
		} else {
			LOG.debug("getAuthzEvents: no event produced by default audit handler");
		}
		_mostRecentEvent = event;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> getAuthzEvents: mostRecentEvent:" + _mostRecentEvent);
		}
		// We return null because we don't want default audit handler to audit anything!
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.getAuthzEvents(" + result + "): null");
		}
		return null;
	}
	
	@Override
	public List<AuthzAuditEvent> getCapturedEvents() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.getCapturedEvents()");
		}

		// construct a new collection since we don't want to lose track of which were the most recent events;
		List<AuthzAuditEvent> result = new ArrayList<AuthzAuditEvent>(_allEvents);
		if (_mostRecentEvent != null) {
			result.add(_mostRecentEvent);
		}
		applySuperUserOverride(result);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.getAuthzEvents(): count[" + result.size() + "] :result : " + result);
		}
		return result;
	}

	@Override
	public AuthzAuditEvent getAndDiscardMostRecentEvent() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.getAndDiscardMostRecentEvent():");
		}

		AuthzAuditEvent result = _mostRecentEvent;
		applySuperUserOverride(result);
		_mostRecentEvent = null;

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.getAndDiscardMostRecentEvent(): " + result);
		}
		return result;
	}

	@Override
	public void setMostRecentEvent(AuthzAuditEvent event) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.setMostRecentEvent(" + event + ")");
		}
		_mostRecentEvent = event;
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.setMostRecentEvent(...)");
		}
	}

	@Override
	public void setSuperUserOverride(boolean override) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.setSuperUserOverride(" + override + ")");
		}

		_superUserOverride = override;

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.setSuperUserOverride(...)");
		}
	}
	
	void applySuperUserOverride(List<AuthzAuditEvent> events) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.applySuperUserOverride(" + events + ")");
		}

		for (AuthzAuditEvent event : events) {
			applySuperUserOverride(event);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.applySuperUserOverride(...)");
		}
	}
	
	void applySuperUserOverride(AuthzAuditEvent event) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.applySuperUserOverride(" + event + ")");
		}
		if (event != null && _superUserOverride) {
			event.setAccessResult((short) 1);
			event.setPolicyId(-1);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.applySuperUserOverride(...)");
		}
	}

	private void resetResourceForAudit(RangerAccessRequest request) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> HbaseAuditHandlerImpl.resetResourceForAudit(" + request + ")");
		}
		if (request != null && request.getResource() instanceof RangerHBaseResource) {
			RangerHBaseResource hbaseResource = (RangerHBaseResource) request.getResource();
			hbaseResource.resetValue(RangerHBaseResource.KEY_TABLE);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== HbaseAuditHandlerImpl.resetResourceForAudit(" + request + ")");
		}
	}
}
