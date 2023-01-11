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
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.Map.Entry;
import java.security.PrivilegedExceptionAction;

import com.google.protobuf.Message;
import com.google.protobuf.Service;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.*;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.ipc.RpcServer;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos;
import org.apache.hadoop.hbase.protobuf.generated.AccessControlProtos.AccessControlService;
import org.apache.hadoop.hbase.quotas.GlobalQuotaSettings;
import org.apache.hadoop.hbase.regionserver.*;
import org.apache.hadoop.hbase.regionserver.compactions.CompactionLifeCycleTracker;
import org.apache.hadoop.hbase.regionserver.compactions.CompactionRequest;
import org.apache.hadoop.hbase.security.AccessDeniedException;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.security.UserProvider;
import org.apache.hadoop.hbase.security.access.*;
import org.apache.hadoop.hbase.security.access.Permission.Action;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.shaded.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.shaded.protobuf.generated.ClientProtos.CleanupBulkLoadRequest;
import org.apache.hadoop.hbase.shaded.protobuf.generated.ClientProtos.PrepareBulkLoadRequest;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.hadoop.hbase.wal.WALEdit;
import org.apache.hadoop.security.AccessControlException;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.AuditProviderFactory;
import org.apache.ranger.authorization.hadoop.constants.RangerHadoopConstants;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerAccessResultProcessor;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerResourceACLs;
import org.apache.ranger.plugin.policyengine.RangerResourceACLs.AccessResult;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import org.apache.ranger.plugin.util.RangerPerfTracer;

public class RangerAuthorizationCoprocessor implements AccessControlService.Interface, RegionCoprocessor, MasterCoprocessor, RegionServerCoprocessor, MasterObserver, RegionObserver, RegionServerObserver, EndpointObserver, BulkLoadObserver, Coprocessor {
	private static final Logger LOG = LoggerFactory.getLogger(RangerAuthorizationCoprocessor.class.getName());
	private static final Logger PERF_HBASEAUTH_REQUEST_LOG = RangerPerfTracer.getPerfLogger("hbaseauth.request");
	private static boolean UpdateRangerPoliciesOnGrantRevoke = RangerHadoopConstants.HBASE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_DEFAULT_VALUE;
	private static final String GROUP_PREFIX = "@";

	private UserProvider userProvider;
    private RegionCoprocessorEnvironment regionEnv;
	private Map<InternalScanner, String> scannerOwners = new MapMaker().weakKeys().makeMap();
	/** if we should check EXEC permissions */
	private boolean shouldCheckExecPermission;
	
	/*
	 * These are package level only for testability and aren't meant to be exposed outside via getters/setters or made available to derived classes.
	 */
	final HbaseFactory _factory = HbaseFactory.getInstance();
	final HbaseUserUtils _userUtils = _factory.getUserUtils();
	final HbaseAuthUtils _authUtils = _factory.getAuthUtils();
	private static volatile RangerHBasePlugin hbasePlugin = null;
	
	// Utilities Methods
	protected byte[] getTableName(RegionCoprocessorEnvironment e) {
		Region region = e.getRegion();
		byte[] tableName = null;
		if (region != null) {
			RegionInfo regionInfo = region.getRegionInfo();
			if (regionInfo != null) {
				tableName = regionInfo.getTable().getName();
			}
		}
		return tableName;
	}
	protected void requireSystemOrSuperUser(Configuration conf, ObserverContext<?> ctx) throws IOException {
		User user = User.getCurrent();
		if (user == null) {
			throw new IOException("Unable to obtain the current user, authorization checks for internal operations will not work correctly!");
		}
		String systemUser = user.getShortName();
		User activeUser = getActiveUser(ctx);
		if (!Objects.equals(systemUser, activeUser.getShortName()) && !_userUtils.isSuperUser(activeUser)) {
			throw new AccessDeniedException("User '" + user.getShortName() + "is not system or super user.");
		}
	}
	protected boolean isSpecialTable(RegionInfo regionInfo) {
		return isSpecialTable(regionInfo.getTable().getName());
	}
	protected boolean isSpecialTable(byte[] tableName) {
		return isSpecialTable(Bytes.toString(tableName));
	}
	protected boolean isSpecialTable(String input) {
		final String[] specialTables = new String[] { "hbase:meta", "-ROOT-", ".META.", "hbase:acl", "hbase:namespace"};
		for (String specialTable : specialTables ) {
			if (specialTable.equals(input)) {
				return true;
			}
		}
			
		return false;
	}
	protected boolean isAccessForMetaTables(RegionCoprocessorEnvironment env) {
		RegionInfo hri = env.getRegion().getRegionInfo();

		if (hri.isMetaRegion()) {
			return true;
		} else {
			return false;
		}
	}

	/*
	private User getActiveUser() {
		User user = null;
		try {
			user = RpcServer.getRequestUser().get();
		} catch (NoSuchElementException e) {
			LOG.info("Unable to get request user");
		}

		if (user == null) {
			// for non-rpc handling, fallback to system user
			try {
				user = User.getCurrent();
			} catch (IOException e) {
				LOG.error("Unable to find the current user");
				user = null;
			}
		}
		return user;
	}*/


	private User getActiveUser(ObserverContext<?> ctx) {
		User user = null;
		if (ctx != null) 	{
			try {
				Optional optionalUser = ctx.getCaller();
				user = optionalUser.isPresent() ? (User) optionalUser.get() : this.userProvider.getCurrent();
			} catch(Exception e){
				LOG.info("Unable to get request user using context" + ctx);
			}
		}

		if (user == null) {
			try {
				user = RpcServer.getRequestUser().get();
			} catch (NoSuchElementException e) {
				LOG.info("Unable to get request user via RPCServer");
			}
		}

		if (user == null) {
			// for non-rpc handling, fallback to system user
			try {
				user = User.getCurrent();
			} catch (IOException e) {
				LOG.error("Unable to find the current user");
				user = null;
			}
		}
		return user;
	}

	
	private String getRemoteAddress() {
		InetAddress remoteAddr = null;
		try {
			remoteAddr = RpcServer.getRemoteAddress().get();
		} catch (NoSuchElementException e) {
			LOG.info("Unable to get remote Address");
		}

		if(remoteAddr == null) {
			remoteAddr = RpcServer.getRemoteIp();
		}

		String strAddr = remoteAddr != null ? remoteAddr.getHostAddress() : null;

		return strAddr;
	}

	// Methods that are used within the CoProcessor
	private void requireScannerOwner(ObserverContext<?> ctx, InternalScanner s) throws AccessDeniedException {
     if (!RpcServer.isInRpcCallContext()) {
       return;
     }

     User user = getActiveUser(ctx);
	 String requestUserName = user.getShortName();
     String owner = scannerOwners.get(s);
     if (owner != null && !owner.equals(requestUserName)) {
       throw new AccessDeniedException("User '"+ requestUserName +"' is not the scanner owner!");
     }	
	}
	/**
	 * @param families
	 * @return empty map if families is null, would never have empty or null keys, would never have null values, values could be empty (non-null) set
	 */
	Map<String, Set<String>> getColumnFamilies(Map<byte[], ? extends Collection<?>> families) {
		if (families == null) {
			// null families map passed.  Ok, returning empty map.
			return Collections.<String, Set<String>>emptyMap();
		}
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		for (Map.Entry<byte[], ? extends Collection<?>> anEntry : families.entrySet()) {
			byte[] familyBytes = anEntry.getKey();
			String family = Bytes.toString(familyBytes);
			if (family == null || family.isEmpty()) {
				LOG.error("Unexpected Input: got null or empty column family (key) in families map! Ignoring...");
			} else {
				Collection<?> columnCollection = anEntry.getValue();
				if (CollectionUtils.isEmpty(columnCollection)) {
					// family points to null map, OK.
					result.put(family, Collections.<String> emptySet());
				} else {
					Iterator<String> columnIterator = new ColumnIterator(columnCollection);
					Set<String> columns = new HashSet<String>();
					try {
						while (columnIterator.hasNext()) {
							String column = columnIterator.next();
							columns.add(column);
						}
					} catch (Throwable t) {
						LOG.error("Exception encountered when converting family-map to set of columns. Ignoring and returning empty set of columns for family[" + family + "]", t);
						LOG.error("Ignoring exception and returning empty set of columns for family[" + family +"]");
						columns.clear();
					}
					result.put(family, columns);
				}
			}
		}
		return result;
	}
	
	static class ColumnFamilyAccessResult {
		final boolean _everythingIsAccessible;
		final boolean _somethingIsAccessible;
		final List<AuthzAuditEvent> _accessAllowedEvents;
		final List<AuthzAuditEvent> _familyLevelAccessEvents;
		final AuthzAuditEvent _accessDeniedEvent;
		final String _denialReason;
		final RangerAuthorizationFilter _filter;;

		ColumnFamilyAccessResult(boolean everythingIsAccessible, boolean somethingIsAccessible,
								 List<AuthzAuditEvent> accessAllowedEvents, List<AuthzAuditEvent> familyLevelAccessEvents, AuthzAuditEvent accessDeniedEvent, String denialReason,
								 RangerAuthorizationFilter filter) {
			_everythingIsAccessible = everythingIsAccessible;
			_somethingIsAccessible = somethingIsAccessible;
			// WARNING: we are just holding on to reference of the collection.  Potentially risky optimization
			_accessAllowedEvents = accessAllowedEvents;
			_familyLevelAccessEvents = familyLevelAccessEvents;
			_accessDeniedEvent = accessDeniedEvent;
			_denialReason = denialReason;
			// cached values of access results
			_filter = filter;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(getClass())
					.add("everythingIsAccessible", _everythingIsAccessible)
					.add("somethingIsAccessible", _somethingIsAccessible)
					.add("accessAllowedEvents", _accessAllowedEvents)
					.add("familyLevelAccessEvents", _familyLevelAccessEvents)
					.add("accessDeniedEvent", _accessDeniedEvent)
					.add("denialReason", _denialReason)
					.add("filter", _filter)
					.toString();
			
		}
	}
	
	ColumnFamilyAccessResult evaluateAccess(ObserverContext<?> ctx, String operation, Action action, final RegionCoprocessorEnvironment env,
											final Map<byte[], ? extends Collection<?>> familyMap, String commandStr) throws AccessDeniedException {

		String access = _authUtils.getAccess(action);
		User user = getActiveUser(ctx);
		String userName = _userUtils.getUserAsString(user);

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("evaluateAccess: entered: user[%s], Operation[%s], access[%s], families[%s]",
					userName, operation, access, getColumnFamilies(familyMap).toString()));
		}

		byte[] tableBytes = getTableName(env);
		if (tableBytes == null || tableBytes.length == 0) {
			String message = "evaluateAccess: Unexpected: Couldn't get table from RegionCoprocessorEnvironment. Access denied, not audited";
			LOG.debug(message);
			throw new AccessDeniedException("Insufficient permissions for operation '" + operation + "',action: " + action);
		}
		String table = Bytes.toString(tableBytes);

		final String messageTemplate = "evaluateAccess: exiting: user[%s], Operation[%s], access[%s], families[%s], verdict[%s]";
		ColumnFamilyAccessResult result;
		if (canSkipAccessCheck(user, operation, access, table) || canSkipAccessCheck(user, operation, access, env)) {
			LOG.debug("evaluateAccess: exiting: isKnownAccessPattern returned true: access allowed, not audited");
			result = new ColumnFamilyAccessResult(true, true, null, null, null, null, null);
			if (LOG.isDebugEnabled()) {
				Map<String, Set<String>> families = getColumnFamilies(familyMap);
				String message = String.format(messageTemplate, userName, operation, access, families.toString(), result.toString());
				LOG.debug(message);
			}
			return result;
		}

		// let's create a session that would be reused.  Set things on it that won't change.
		HbaseAuditHandler auditHandler = _factory.getAuditHandler();
		AuthorizationSession session = new AuthorizationSession(hbasePlugin)
				.operation(operation)
				.otherInformation(commandStr)
				.remoteAddress(getRemoteAddress())
				.auditHandler(auditHandler)
				.user(user)
				.access(access)
				.table(table);
		Map<String, Set<String>> families = getColumnFamilies(familyMap);
		if (LOG.isDebugEnabled()) {
			LOG.debug("evaluateAccess: families to process: " + families.toString());
		}
		if (families == null || families.isEmpty()) {
			LOG.debug("evaluateAccess: Null or empty families collection, ok.  Table level access is desired");
			session.buildRequest()
				.authorize();
			boolean authorized = session.isAuthorized();
			String reason = "";
			if (authorized) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("evaluateAccess: table level access granted [" + table + "]");
				}
			} else {
				reason = String.format("Insufficient permissions for user ‘%s',action: %s, tableName:%s, no column families found.", user.getName(), operation, table);
			}
			AuthzAuditEvent event = auditHandler.getAndDiscardMostRecentEvent(); // this could be null, of course, depending on audit settings of table.
			// if authorized then pass captured events as access allowed set else as access denied set.
			result = new ColumnFamilyAccessResult(authorized, authorized,
						authorized ? Collections.singletonList(event) : null,
						null, authorized ? null : event, reason, null);
			if (LOG.isDebugEnabled()) {
				String message = String.format(messageTemplate, userName, operation, access, families.toString(), result.toString());
				LOG.debug(message);
			}
			return result;
		} else {
			LOG.debug("evaluateAccess: Families collection not null.  Skipping table-level check, will do finer level check");
		}
		
		boolean everythingIsAccessible = true;
		boolean somethingIsAccessible = false;
		/*
		 * we would have to accumulate audits of all successful accesses and any one denial (which in our case ends up being the last denial)
		 * We need to keep audit events for family level access check seperate because we don't want them logged in some cases.
		 */
		List<AuthzAuditEvent> authorizedEvents = new ArrayList<AuthzAuditEvent>();
		List<AuthzAuditEvent> familyLevelAccessEvents = new ArrayList<AuthzAuditEvent>();
		AuthzAuditEvent deniedEvent = null;
		String denialReason = null;
		// we need to cache the auths results so that we can create a filter, if needed
		Map<String, Set<String>> columnsAccessAllowed = new HashMap<String, Set<String>>();
		Set<String> familesAccessAllowed = new HashSet<String>();
		Set<String> familesAccessDenied = new HashSet<String>();
		Set<String> familesAccessIndeterminate = new HashSet<String>();

		for (Map.Entry<String, Set<String>> anEntry : families.entrySet()) {
			String family = anEntry.getKey();
			session.columnFamily(family);
			if (LOG.isDebugEnabled()) {
				LOG.debug("evaluateAccess: Processing family: " + family);
			}
			Set<String> columns = anEntry.getValue();
			if (columns == null || columns.isEmpty()) {
				LOG.debug("evaluateAccess: columns collection null or empty, ok.  Family level access is desired.");

				session.column(null) // zap stale column from prior iteration of this loop, if any
						.buildRequest()
						.authorize();
				AuthzAuditEvent auditEvent = auditHandler.getAndDiscardMostRecentEvent(); // capture it only for success

				final boolean isColumnFamilyAuthorized = session.isAuthorized();

				if (auditEvent != null) {
					if (isColumnFamilyAuthorized) {
						familyLevelAccessEvents.add(auditEvent);
					} else {
						if (deniedEvent == null) { // we need to capture just one denial event
							LOG.debug("evaluateAccess: Setting denied access audit event with last auth failure audit event.");
							deniedEvent = auditEvent;
						}
					}
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("evaluateAccess: family level access for [" + family + "] is evaluated to " + isColumnFamilyAuthorized + ". Checking if [" + family + "] descendants have access.");
				}
				session.resourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF_OR_DESCENDANTS)
						.buildRequest()
						.authorize();
				auditEvent = auditHandler.getAndDiscardMostRecentEvent(); // capture it only for failure
				if (session.isAuthorized()) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("evaluateAccess: [" + family + "] descendants have access");
					}
					somethingIsAccessible = true;
					if (isColumnFamilyAuthorized) {
						familesAccessAllowed.add(family);
						if (auditEvent != null) {
							LOG.debug("evaluateAccess: adding to family-level-access-granted-event-set");
							familyLevelAccessEvents.add(auditEvent);
						}
					} else {
						familesAccessIndeterminate.add(family);
						if (LOG.isDebugEnabled()) {
							LOG.debug("evaluateAccess: has partial access (of some type) in family [" + family + "]");
						}
						everythingIsAccessible = false;
						if (auditEvent != null && deniedEvent == null) { // we need to capture just one denial event
							LOG.debug("evaluateAccess: Setting denied access audit event with last auth failure audit event.");
							deniedEvent = auditEvent;
						}
					}
				} else {
					everythingIsAccessible = false;
					if (isColumnFamilyAuthorized) {
						somethingIsAccessible = true;
						familesAccessIndeterminate.add(family);
						if (LOG.isDebugEnabled()) {
							LOG.debug("evaluateAccess: has partial access (of some type) in family [" + family + "]");
						}
						if (auditEvent != null && deniedEvent == null) { // we need to capture just one denial event
							LOG.debug("evaluateAccess: Setting denied access audit event with last auth failure audit event.");
							deniedEvent = auditEvent;
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("evaluateAccess: has no access of [" + access + "] type in family [" + family + "]");
						}
						familesAccessDenied.add(family);
						denialReason = String.format("Insufficient permissions for user ‘%s',action: %s, tableName:%s, family:%s.", user.getName(), operation, table, family);
					}
				}
				// Restore the headMatch setting
				session.resourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF);
			} else {
				LOG.debug("evaluateAccess: columns collection not empty.  Skipping Family level check, will do finer level access check.");
				Set<String> accessibleColumns = new HashSet<String>(); // will be used in to populate our results cache for the filter
 				for (String column : columns) {
 					if (LOG.isDebugEnabled()) {
 						LOG.debug("evaluateAccess: Processing column: " + column);
 					}
 					session.column(column)
 						.buildRequest()
 						.authorize();
					AuthzAuditEvent auditEvent = auditHandler.getAndDiscardMostRecentEvent();
 					if (session.isAuthorized()) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("evaluateAccess: has column level access [" + family + ", " + column + "]");
						}
 						// we need to do 3 things: housekeeping, capturing audit events, building the results cache for filter
 						somethingIsAccessible = true;
 						accessibleColumns.add(column);
						if (auditEvent != null) {
							LOG.debug("evaluateAccess: adding to access-granted-audit-event-set");
							authorizedEvents.add(auditEvent);
						}
 					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("evaluateAccess: no column level access [" + family + ", " + column + "]");
						}
						somethingIsAccessible = false;
 						everythingIsAccessible = false;
 						denialReason = String.format("Insufficient permissions for user ‘%s',action: %s, tableName:%s, family:%s, column: %s", user.getName(), operation, table, family, column);
						if (auditEvent != null && deniedEvent == null) { // we need to capture just one denial event
							LOG.debug("evaluateAccess: Setting denied access audit event with last auth failure audit event.");
							deniedEvent = auditEvent;
						}
 					}
					if (!accessibleColumns.isEmpty()) {
						columnsAccessAllowed.put(family, accessibleColumns);
					}
				}
			}
		}
		// Cache of auth results are encapsulated the in the filter. Not every caller of the function uses it - only preGet and preOpt will.
		RangerAuthorizationFilter filter = new RangerAuthorizationFilter(session, familesAccessAllowed, familesAccessDenied, familesAccessIndeterminate, columnsAccessAllowed);
		result = new ColumnFamilyAccessResult(everythingIsAccessible, somethingIsAccessible, authorizedEvents, familyLevelAccessEvents, deniedEvent, denialReason, filter);
		if (LOG.isDebugEnabled()) {
			String message = String.format(messageTemplate, userName, operation, access, families.toString(), result.toString());
			LOG.debug(message);
		}
		return result;
	}

	Filter authorizeAccess(ObserverContext<?> ctx, String operation, Action action, final RegionCoprocessorEnvironment env, final Map<byte[], NavigableSet<byte[]>> familyMap, String commandStr) throws AccessDeniedException {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> authorizeAccess");
		}
		RangerPerfTracer perf = null;

		try {
			perf = RangerPerfTracer.getPerfTracer(PERF_HBASEAUTH_REQUEST_LOG, "RangerAuthorizationCoprocessor.authorizeAccess(request=Operation[" + operation + "]");

			ColumnFamilyAccessResult accessResult = evaluateAccess(ctx, operation, action, env, familyMap, commandStr);
			RangerDefaultAuditHandler auditHandler = new RangerDefaultAuditHandler(hbasePlugin.getConfig());
			if (accessResult._everythingIsAccessible) {
				auditHandler.logAuthzAudits(accessResult._accessAllowedEvents);
				auditHandler.logAuthzAudits(accessResult._familyLevelAccessEvents);
				LOG.debug("authorizeAccess: exiting: No filter returned since all access was allowed");
				return null; // no filter needed since we are good to go.
			} else if (accessResult._somethingIsAccessible) {
				// NOTE: audit logging is split beween logging here (in scope of preOp/preGet) and logging in the filter component for those that couldn't be determined
				auditHandler.logAuthzAudits(accessResult._accessAllowedEvents);
				LOG.debug("authorizeAccess: exiting: Filter returned since some access was allowed");
				return accessResult._filter;
			} else {
				// If we are here then it means nothing was accessible!  So let's log one denial (in our case, the last denial) and throw an exception
				auditHandler.logAuthzAudit(accessResult._accessDeniedEvent);
				LOG.debug("authorizeAccess: exiting: Throwing exception since nothing was accessible");
				throw new AccessDeniedException(accessResult._denialReason);
			}
		} finally {
			RangerPerfTracer.log(perf);
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== authorizeAccess");
			}
		}
	}
	
	Filter combineFilters(Filter filter, Filter existingFilter) {
		Filter combinedFilter = filter;
		if (existingFilter != null) {
			combinedFilter = new FilterList(FilterList.Operator.MUST_PASS_ALL, Lists.newArrayList(filter, existingFilter));
		}
		return combinedFilter;
	}

	void requirePermission(final ObserverContext<?> ctx, final String operation, final Action action, final RegionCoprocessorEnvironment regionServerEnv, final Map<byte[], ? extends Collection<?>> familyMap)
			throws AccessDeniedException {

		RangerPerfTracer perf = null;

		try {
			if (RangerPerfTracer.isPerfTraceEnabled(PERF_HBASEAUTH_REQUEST_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_HBASEAUTH_REQUEST_LOG, "RangerAuthorizationCoprocessor.requirePermission(request=Operation[" + operation + "]");
			}
			ColumnFamilyAccessResult accessResult = evaluateAccess(ctx, operation, action, regionServerEnv, familyMap, null);
			RangerDefaultAuditHandler auditHandler = new RangerDefaultAuditHandler(hbasePlugin.getConfig());
			if (accessResult._everythingIsAccessible) {
				auditHandler.logAuthzAudits(accessResult._accessAllowedEvents);
				auditHandler.logAuthzAudits(accessResult._familyLevelAccessEvents);
				LOG.debug("requirePermission: exiting: all access was allowed");
				return;
			} else {
				auditHandler.logAuthzAudit(accessResult._accessDeniedEvent);
				LOG.debug("requirePermission: exiting: throwing exception as everything wasn't accessible");
				throw new AccessDeniedException(accessResult._denialReason);
			}
		} finally {
			RangerPerfTracer.log(perf);
		}
	}
	
	/**
	 * This could run s
	 * @param operation
	 * @param otherInformation
	 * @param table
	 * @param columnFamily
	 * @param column
	 * @return
	 * @throws AccessDeniedException
	 */
	void authorizeAccess(ObserverContext<?> ctx, String operation, String otherInformation, Action action, String table, String columnFamily, String column) throws AccessDeniedException {
		User   user   = getActiveUser(ctx);
		String access = _authUtils.getAccess(action);
		if (LOG.isDebugEnabled()) {
			final String format = "authorizeAccess: %s: Operation[%s], Info[%s], access[%s], table[%s], columnFamily[%s], column[%s]";
			String message = String.format(format, "Entering", operation, otherInformation, access, table, columnFamily, column);
			LOG.debug(message);
		}
		
		final String format =  "authorizeAccess: %s: Operation[%s], Info[%s], access[%s], table[%s], columnFamily[%s], column[%s], allowed[%s], reason[%s]";
		if (canSkipAccessCheck(user, operation, access, table)) {
			if (LOG.isDebugEnabled()) {
				String message = String.format(format, "Exiting", operation, otherInformation, access, table, columnFamily, column, true, "can skip auth check");
				LOG.debug(message);
			}
			return;
		}

		
		HbaseAuditHandler auditHandler = _factory.getAuditHandler();
		AuthorizationSession session = new AuthorizationSession(hbasePlugin)
			.operation(operation)
			.otherInformation(otherInformation)
			.remoteAddress(getRemoteAddress())
			.auditHandler(auditHandler)
			.user(user)
			.access(access)
			.table(table)
			.columnFamily(columnFamily)
			.column(column)
			.buildRequest()
			.authorize();
		
		if (LOG.isDebugEnabled()) {
			boolean allowed = session.isAuthorized();
			String reason = session.getDenialReason();
			String message = String.format(format, "Exiting", operation, otherInformation, access, table, columnFamily, column, allowed, reason);
			LOG.debug(message);
		}
		
		session.publishResults();
	}
	
	boolean canSkipAccessCheck(User user, final String operation, String access, final String table)
			throws AccessDeniedException {
		
		boolean result = false;
		if (user == null) {
			String message = "Unexpeceted: User is null: access denied, not audited!";
			LOG.warn("canSkipAccessCheck: exiting" + message);
			throw new AccessDeniedException("No user associated with request (" + operation + ") for action: " + access + "on table:" + table);
		} else if (isAccessForMetadataRead(access, table)) {
			LOG.debug("canSkipAccessCheck: true: metadata read access always allowed, not audited");
			result = true;
		} else {
			LOG.debug("Can't skip access checks");
		}
		
		return result;
	}
	
	boolean canSkipAccessCheck(User user, final String operation, String access, final RegionCoprocessorEnvironment regionServerEnv) throws AccessDeniedException {

		// read access to metadata tables is always allowed and isn't audited.
		if (isAccessForMetaTables(regionServerEnv) && _authUtils.isReadAccess(access)) {
			LOG.debug("isKnownAccessPattern: exiting: Read access for metadata tables allowed, not audited!");
			return true;
		}
		// if write access is desired to metatables then global create access is sufficient
		if (_authUtils.isWriteAccess(access) && isAccessForMetaTables(regionServerEnv)) {
			String createAccess = _authUtils.getAccess(Action.CREATE);
			AuthorizationSession session = new AuthorizationSession(hbasePlugin)
				.operation(operation)
				.remoteAddress(getRemoteAddress())
				.user(user)
				.access(createAccess)
				.buildRequest()
				.authorize();
			if (session.isAuthorized()) {
				// NOTE: this access isn't logged
				LOG.debug("isKnownAccessPattern: exiting: User has global create access, allowed!");
				return true;
			}
		}
		return false;
	}
	
	boolean isAccessForMetadataRead(String access, String table) {
		if (_authUtils.isReadAccess(access) && isSpecialTable(table)) {
			LOG.debug("isAccessForMetadataRead: Metadata tables read: access allowed!");
			return true;
		}
		return false;
	}

	// Check if the user has global permission ...
	protected void requireGlobalPermission(ObserverContext<?> ctx, String request, String objName, Permission.Action action) throws AccessDeniedException {
		authorizeAccess(ctx, request, objName, action, null, null, null);
	}

	protected void requirePermission(ObserverContext<?> ctx, String request, Permission.Action action) throws AccessDeniedException {
		requirePermission(ctx, request, null, action);
	}

	protected void requirePermission(ObserverContext<?> ctx, String request, byte[] tableName, Permission.Action action) throws AccessDeniedException {
		String table = Bytes.toString(tableName);

		authorizeAccess(ctx, request, null, action, table, null, null);
	}
	
	protected void requirePermission(ObserverContext<?> ctx, String request, byte[] aTableName, byte[] aColumnFamily, byte[] aQualifier, Permission.Action action) throws AccessDeniedException {

		String table = Bytes.toString(aTableName);
		String columnFamily = Bytes.toString(aColumnFamily);
		String column = Bytes.toString(aQualifier);

		authorizeAccess(ctx, request, null, action, table, columnFamily, column);
	}
	
	protected void requirePermission(ObserverContext<?> ctx, String request, Permission.Action perm, RegionCoprocessorEnvironment env, Collection<byte[]> families) throws IOException {
		HashMap<byte[], Set<byte[]>> familyMap = new HashMap<byte[], Set<byte[]>>();

		if(families != null) {
			for (byte[] family : families) {
				familyMap.put(family, null);
			}
		}
		requirePermission(ctx, request, perm, env, familyMap);
	}

	@Override
	public Optional<RegionObserver> getRegionObserver() {
		return Optional.<RegionObserver>of(this);
	}

	@Override
	public Optional<MasterObserver> getMasterObserver() {
		return Optional.<MasterObserver>of(this);
	}

	@Override
	public Optional<EndpointObserver> getEndpointObserver() {
		return Optional.<EndpointObserver>of(this);
	}

	@Override
	public Optional<BulkLoadObserver> getBulkLoadObserver() {
		return Optional.<BulkLoadObserver>of(this);
	}

	@Override
	public Optional<RegionServerObserver> getRegionServerObserver() {
		return Optional.<RegionServerObserver>of(this);
	}

	@Override
	public void postScannerClose(ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s) throws IOException {
		scannerOwners.remove(s);
	}
	@Override
	public RegionScanner postScannerOpen(ObserverContext<RegionCoprocessorEnvironment> c, Scan scan, RegionScanner s) throws IOException {
		User user = getActiveUser(c);
		if (user != null && user.getShortName() != null) {
			scannerOwners.put(s, user.getShortName());
		}
		return s;
	}

	@Override
	public void postStartMaster(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException {
		if(UpdateRangerPoliciesOnGrantRevoke) {
			LOG.debug("Calling create ACL table ...");
			Admin admin = (ctx.getEnvironment()).getConnection().getAdmin();
			Throwable var3 = null;

			try {
				if(!admin.tableExists(AccessControlLists.ACL_TABLE_NAME)) {
					createACLTable(admin);
				}
			} catch (Throwable var12) {
				var3 = var12;
				throw var12;
			} finally {
				if(admin != null) {
					if(var3 != null) {
						try {
							admin.close();
						} catch (Throwable var11) {
							var3.addSuppressed(var11);
						}
					} else {
						admin.close();
					}
				}

			}
		}
	}

	private static void createACLTable(Admin admin) throws IOException {
		ColumnFamilyDescriptor cfd = ColumnFamilyDescriptorBuilder.newBuilder(AccessControlLists.ACL_LIST_FAMILY).setMaxVersions(1).setInMemory(true).setBlockCacheEnabled(true).setBlocksize(8192).setBloomFilterType(BloomType.NONE).setScope(0).build();
		TableDescriptor td = TableDescriptorBuilder.newBuilder(AccessControlLists.ACL_TABLE_NAME).addColumnFamily(cfd).build();
		admin.createTable(td);
	}

	@Override
	public Iterable<Service> getServices() {
		return Collections.singleton(AccessControlService.newReflectiveService(this));
	}

	@Override
	public Result preAppend(ObserverContext<RegionCoprocessorEnvironment> c, Append append) throws IOException {
		requirePermission(c, "append", TablePermission.Action.WRITE, c.getEnvironment(), append.getFamilyCellMap());
		return null;
	}
	@Override
	public void preAssign(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo regionInfo) throws IOException {
		requirePermission(c, "assign", regionInfo.getTable().getName(), null, null, Action.ADMIN);
	}
	@Override
	public void preBalance(ObserverContext<MasterCoprocessorEnvironment> c) throws IOException {
		requirePermission(c,"balance", Permission.Action.ADMIN);
	}
	@Override
	public void preBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> c, boolean newValue) throws IOException {
		requirePermission(c, "balanceSwitch", Permission.Action.ADMIN);
	}
	@Override
	public void preBulkLoadHFile(ObserverContext<RegionCoprocessorEnvironment> ctx, List<Pair<byte[], String>> familyPaths) throws IOException {
		List<byte[]> cfs = new LinkedList<byte[]>();
		for (Pair<byte[], String> el : familyPaths) {
			cfs.add(el.getFirst());
		}
		requirePermission(ctx, "bulkLoadHFile", Permission.Action.WRITE, ctx.getEnvironment(), cfs);
	}
	@Override
	public boolean preCheckAndDelete(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp, ByteArrayComparable comparator, Delete delete, boolean result) throws IOException {
		Collection<byte[]> familyMap = Arrays.asList(new byte[][] { family });
		requirePermission(c, "checkAndDelete", TablePermission.Action.READ, c.getEnvironment(), familyMap);
		requirePermission(c, "checkAndDelete", TablePermission.Action.WRITE, c.getEnvironment(), familyMap);
		return result;
	}
	@Override
	public boolean preCheckAndPut(ObserverContext<RegionCoprocessorEnvironment> c, byte[] row, byte[] family, byte[] qualifier, CompareOperator compareOp, ByteArrayComparable comparator, Put put, boolean result) throws IOException {
		Collection<byte[]> familyMap = Arrays.asList(new byte[][] { family });
		requirePermission(c, "checkAndPut", TablePermission.Action.READ, c.getEnvironment(), familyMap);
		requirePermission(c, "checkAndPut", TablePermission.Action.WRITE, c.getEnvironment(), familyMap);
		return result;
	}
	@Override
	public void preCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		requirePermission(ctx, "cloneSnapshot", hTableDescriptor.getTableName().getName(), Permission.Action.ADMIN);
	}
	@Override
	public void preClose(ObserverContext<RegionCoprocessorEnvironment> e, boolean abortRequested) throws IOException {
		requirePermission(e, "close", getTableName(e.getEnvironment()), Permission.Action.ADMIN);
	}
	@Override
	public InternalScanner preCompact(ObserverContext<RegionCoprocessorEnvironment> e, Store store, InternalScanner scanner,ScanType scanType, CompactionLifeCycleTracker tracker, CompactionRequest request) throws IOException {
		requirePermission(e, "compact", getTableName(e.getEnvironment()), null, null, Action.CREATE);
		return scanner;
	}
	@Override
	public void preCompactSelection(ObserverContext<RegionCoprocessorEnvironment> e, Store store, List<? extends StoreFile> candidates, CompactionLifeCycleTracker tracker) throws IOException {
		requirePermission(e, "compactSelection", getTableName(e.getEnvironment()), null, null, Action.CREATE);
	}

	@Override
	public void preCreateTable(ObserverContext<MasterCoprocessorEnvironment> c, TableDescriptor desc, RegionInfo[] regions) throws IOException {
		requirePermission(c, "createTable", desc.getTableName().getName(), Permission.Action.CREATE);
	}


	@Override
	public void preDelete(ObserverContext<RegionCoprocessorEnvironment> c, Delete delete, WALEdit edit, Durability durability) throws IOException {
		requirePermission(c, "delete", TablePermission.Action.WRITE, c.getEnvironment(), delete.getFamilyCellMap());
	}

	@Override
	public void preDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot) throws IOException {
		requirePermission(ctx, "deleteSnapshot", snapshot.getTableName().getName(), Permission.Action.ADMIN);
	}
	@Override
	public void preDeleteTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName) throws IOException {
		requirePermission(c, "deleteTable", tableName.getName(), null, null, Action.CREATE);
	}
	@Override
	public void preDisableTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName) throws IOException {
		requirePermission(c, "disableTable", tableName.getName(), null, null, Action.CREATE);
	}
	@Override
	public void preEnableTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName) throws IOException {
		requirePermission(c, "enableTable", tableName.getName(), null, null, Action.CREATE);
	}
	@Override
	public boolean preExists(ObserverContext<RegionCoprocessorEnvironment> c, Get get, boolean exists) throws IOException {
		requirePermission(c, "exists", TablePermission.Action.READ, c.getEnvironment(), get.familySet());
		return exists;
	}
	@Override
	public void preFlush(ObserverContext<RegionCoprocessorEnvironment> e, FlushLifeCycleTracker tracker) throws IOException {
		requirePermission(e, "flush", getTableName(e.getEnvironment()), null, null, Action.CREATE);
	}

	@Override
	public Result preIncrement(ObserverContext<RegionCoprocessorEnvironment> c, Increment increment) throws IOException {
		requirePermission(c, "increment", TablePermission.Action.WRITE, c.getEnvironment(), increment.getFamilyCellMap().keySet());
		
		return null;
	}

	@Override
	public void preModifyTable(ObserverContext<MasterCoprocessorEnvironment> c, TableName tableName, TableDescriptor htd) throws IOException {
		requirePermission(c, "modifyTable", tableName.getName(), null, null, Action.CREATE);
	}
	@Override
	public void preMove(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo region, ServerName srcServer, ServerName destServer) throws IOException {
		requirePermission(c, "move", region.getTable().getName() , null, null, Action.ADMIN);
	}

	@Override
	public void preAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> observerContext, long procId) throws IOException {
		//if(!procEnv.isProcedureOwner(procId, this.getActiveUser())) {
		requirePermission(observerContext, "abortProcedure", Action.ADMIN);
		//}
	}

	@Override
	public void postGetProcedures(ObserverContext<MasterCoprocessorEnvironment> observerContext) throws IOException {
		/*if(!procInfoList.isEmpty()) {
			Iterator<Procedure<?>> itr = procInfoList.iterator();
			User user = this.getActiveUser();

			while(itr.hasNext()) {
				Procedure procInfo = itr.next();
				try {
					String owner = procInfo.getOwner();
					if (owner == null || !owner.equals(user.getShortName())) {
						requirePermission("getProcedures", Action.ADMIN);
					}
				} catch (AccessDeniedException var7) {
					itr.remove();
				}
			}

		}*/
		requirePermission(observerContext, "getProcedures", Action.ADMIN);
	}

	@Override
	public void preOpen(ObserverContext<RegionCoprocessorEnvironment> e) throws IOException {
		RegionCoprocessorEnvironment env = e.getEnvironment();
		final Region region = env.getRegion();
		if (region == null) {
			LOG.error("NULL region from RegionCoprocessorEnvironment in preOpen()");
		} else {
			RegionInfo regionInfo = region.getRegionInfo();
			if (isSpecialTable(regionInfo)) {
				requireSystemOrSuperUser(regionEnv.getConfiguration(),e);
			} else {
				requirePermission(e, "open", getTableName(e.getEnvironment()), Action.ADMIN);
			}
		}
	}
	@Override
	public void preRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		requirePermission(ctx, "restoreSnapshot", hTableDescriptor.getTableName().getName(), Permission.Action.ADMIN);
	}

	@Override
	public void preScannerClose(ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s) throws IOException {
		requireScannerOwner(c,s);
	}
	@Override
	public boolean preScannerNext(ObserverContext<RegionCoprocessorEnvironment> c, InternalScanner s, List<Result> result, int limit, boolean hasNext) throws IOException {
		requireScannerOwner(c,s);
		return hasNext;
	}
	@Override
	public void preScannerOpen(ObserverContext<RegionCoprocessorEnvironment> c, Scan scan) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> preScannerOpen");
		}
		String commandStr = null;
		try {
			RegionCoprocessorEnvironment e = c.getEnvironment();

			Map<byte[], NavigableSet<byte[]>> familyMap = scan.getFamilyMap();
			String operation    = "scannerOpen";
			byte[] tableName    = getTableName(e);
			String tableNameStr = tableName != null ?  new String(tableName):" ";
			commandStr          = getCommandString(HbaseConstants.SCAN, tableNameStr, scan.toMap());
			Filter filter       = authorizeAccess(c, operation, Action.READ, e, familyMap, commandStr);
			if (filter == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("preScannerOpen: Access allowed for all families/column.  No filter added");
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("preScannerOpen: Access allowed for some of the families/column. New filter added.");
				}
				Filter existingFilter = scan.getFilter();
				Filter combinedFilter = combineFilters(filter, existingFilter);
				scan.setFilter(combinedFilter);
			}
		} finally {
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== preScannerOpen: commandStr: " + commandStr);
			}
		}
	}
	@Override
	public void preShutdown(ObserverContext<MasterCoprocessorEnvironment> c) throws IOException {
		requirePermission(c, "shutdown", Permission.Action.ADMIN);
		cleanUp_HBaseRangerPlugin();
	}
	@Override
	public void preSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot, TableDescriptor hTableDescriptor) throws IOException {
		requirePermission(ctx, "snapshot", hTableDescriptor.getTableName().getName(), Permission.Action.ADMIN);
	}

	@Override
	public void preStopMaster(ObserverContext<MasterCoprocessorEnvironment> c) throws IOException {
		requirePermission(c, "stopMaster", Permission.Action.ADMIN);
		cleanUp_HBaseRangerPlugin();
	}
	@Override
	public void preStopRegionServer(ObserverContext<RegionServerCoprocessorEnvironment> env) throws IOException {
		requirePermission(env, "stop", Permission.Action.ADMIN);
		cleanUp_HBaseRangerPlugin();
	}
	@Override
	public void preUnassign(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo regionInfo, boolean force) throws IOException {
		requirePermission(c, "unassign", regionInfo.getTable().getName(), null, null, Action.ADMIN);
	}

  @Override
  public void preSetUserQuota(final ObserverContext<MasterCoprocessorEnvironment> ctx,
      final String userName, final GlobalQuotaSettings quotas) throws IOException {
    requireGlobalPermission(ctx, "setUserQuota", null, Action.ADMIN);
  }

  @Override
  public void preSetUserQuota(final ObserverContext<MasterCoprocessorEnvironment> ctx,
      final String userName, final TableName tableName, final GlobalQuotaSettings quotas) throws IOException {
    requirePermission(ctx, "setUserTableQuota", tableName.getName(), null, null, Action.ADMIN);
  }

  @Override
  public void preSetUserQuota(final ObserverContext<MasterCoprocessorEnvironment> ctx,
      final String userName, final String namespace, final GlobalQuotaSettings quotas) throws IOException {
    requireGlobalPermission(ctx, "setUserNamespaceQuota", namespace, Action.ADMIN);
  }

  @Override
  public void preSetTableQuota(final ObserverContext<MasterCoprocessorEnvironment> ctx,
      final TableName tableName, final GlobalQuotaSettings quotas) throws IOException {
    requirePermission(ctx, "setTableQuota", tableName.getName(), null, null, Action.ADMIN);
  }

  @Override
  public void preSetNamespaceQuota(final ObserverContext<MasterCoprocessorEnvironment> ctx,
      final String namespace, final GlobalQuotaSettings quotas) throws IOException {
    requireGlobalPermission(ctx, "setNamespaceQuota", namespace, Action.ADMIN);
  }

	private String coprocessorType = "unknown";
	private static final String MASTER_COPROCESSOR_TYPE = "master";
	private static final String REGIONAL_COPROCESSOR_TYPE = "regional";
	private static final String REGIONAL_SERVER_COPROCESSOR_TYPE = "regionalServer";

	@Override
	public void start(CoprocessorEnvironment env) throws IOException {
		String appType = "unknown";

		shouldCheckExecPermission = env.getConfiguration().getBoolean(
				AccessControlConstants.EXEC_PERMISSION_CHECKS_KEY,
				AccessControlConstants.DEFAULT_EXEC_PERMISSION_CHECKS);
		if (env instanceof MasterCoprocessorEnvironment) {
			coprocessorType = MASTER_COPROCESSOR_TYPE;
			appType = "hbaseMaster";
		} else if (env instanceof RegionServerCoprocessorEnvironment) {
			coprocessorType = REGIONAL_SERVER_COPROCESSOR_TYPE;
			appType = "hbaseRegional";
		} else if (env instanceof RegionCoprocessorEnvironment) {
			regionEnv = (RegionCoprocessorEnvironment) env;
			coprocessorType = REGIONAL_COPROCESSOR_TYPE;
			appType = "hbaseRegional";
		}

		this.userProvider = UserProvider.instantiate(env.getConfiguration());

		Configuration conf = env.getConfiguration();
		HbaseFactory.initialize(conf);

		// create and initialize the plugin class
		RangerHBasePlugin plugin = hbasePlugin;

		if(plugin == null) {
			synchronized(RangerAuthorizationCoprocessor.class) {
				plugin = hbasePlugin;
				
				if(plugin == null) {
					plugin = new RangerHBasePlugin(appType);

					plugin.init();

					UpdateRangerPoliciesOnGrantRevoke = plugin.getConfig().getBoolean(RangerHadoopConstants.HBASE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_PROP, RangerHadoopConstants.HBASE_UPDATE_RANGER_POLICIES_ON_GRANT_REVOKE_DEFAULT_VALUE);

					hbasePlugin = plugin;
				}
			}
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Start of Coprocessor: [" + coprocessorType + "]");
		}
	}

	@Override
	public void prePut(ObserverContext<RegionCoprocessorEnvironment> c, Put put, WALEdit edit, Durability durability) throws IOException {
		requirePermission(c, "put", TablePermission.Action.WRITE, c.getEnvironment(), put.getFamilyCellMap());
	}
	
	@Override
	public void preGetOp(final ObserverContext<RegionCoprocessorEnvironment> rEnv, final Get get, final List<Cell> result) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> preGetOp");
		}
		String commandStr = null;
		try {
			RegionCoprocessorEnvironment e = rEnv.getEnvironment();
			Map<byte[], NavigableSet<byte[]>> familyMap = get.getFamilyMap();

			String operation    = "get";
			byte[] tableName    = getTableName(e);
			String tableNameStr = tableName != null ?  new String(tableName):" ";
			commandStr          = getCommandString(HbaseConstants.GET, tableNameStr, get.toMap());
			Filter filter       = authorizeAccess(rEnv, operation, Action.READ, e, familyMap, commandStr);
			if (filter == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("preGetOp: all access allowed, no filter returned");
				}
			} else {
				Filter existingFilter = get.getFilter();
				Filter combinedFilter = combineFilters(filter, existingFilter);
				get.setFilter(combinedFilter);
				if (LOG.isDebugEnabled()) {
					LOG.debug("preGetOp: partial access, new filter added");
				}
			}
		} finally {
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== preGetOp: commandStr: " + commandStr );
			}
		}
	}
	@Override
	public void preRegionOffline(ObserverContext<MasterCoprocessorEnvironment> c, RegionInfo regionInfo) throws IOException {
	    requirePermission(c, "regionOffline", regionInfo.getTable().getName(), null, null, Action.ADMIN);
	}
	@Override
	public void preCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns) throws IOException {
		requireGlobalPermission(ctx, "createNamespace", ns.getName(), Action.ADMIN);
	}
	@Override
	public void preDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace) throws IOException {
		requireGlobalPermission(ctx, "deleteNamespace", namespace, Action.ADMIN);
	}
	@Override
	public void preModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns) throws IOException {
		requireGlobalPermission(ctx, "modifyNamespace", ns.getName(), Action.ADMIN);
	}

	@Override
	public void postGetTableNames(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableDescriptor> descriptors, String regex) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> postGetTableNames(count(descriptors)=%s, regex=%s)", descriptors == null ? 0 : descriptors.size(), regex));
		}
		checkGetTableInfoAccess(ctx, "getTableNames", descriptors, regex, RangerPolicyEngine.ANY_ACCESS);

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== postGetTableNames(count(descriptors)=%s, regex=%s)", descriptors == null ? 0 : descriptors.size(), regex));
		}
	}

	@Override
	public void postGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableName> tableNamesList, List<TableDescriptor> descriptors, String regex) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> postGetTableDescriptors(count(tableNamesList)=%s, count(descriptors)=%s, regex=%s)", tableNamesList == null ? 0 : tableNamesList.size(),
					descriptors == null ? 0 : descriptors.size(), regex));
		}

		checkGetTableInfoAccess(ctx, "getTableDescriptors", descriptors, regex, _authUtils.getAccess(Action.CREATE));
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("<== postGetTableDescriptors(count(tableNamesList)=%s, count(descriptors)=%s, regex=%s)", tableNamesList == null ? 0 : tableNamesList.size(),
					descriptors == null ? 0 : descriptors.size(), regex));
		}
	}

	@Override
	public void postListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx,	List<NamespaceDescriptor> descriptors) throws IOException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.postListNamespaceDescriptors()");
		}

		checkAccessForNamespaceDescriptor(ctx, "getNameSpaceDescriptors", descriptors);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.postListNamespaceDescriptors()");
		}
	}


	public void prePrepareBulkLoad(ObserverContext<RegionCoprocessorEnvironment> ctx, PrepareBulkLoadRequest request) throws IOException {
		List<byte[]> cfs = null;

		requirePermission(ctx, "prePrepareBulkLoad", Permission.Action.WRITE, ctx.getEnvironment(), cfs);
	}

	public void preCleanupBulkLoad(ObserverContext<RegionCoprocessorEnvironment> ctx, CleanupBulkLoadRequest request) throws IOException {
		List<byte[]> cfs = null;

		requirePermission(ctx, "preCleanupBulkLoad", Permission.Action.WRITE, ctx.getEnvironment(), cfs);
	}

	/* ---- EndpointObserver implementation ---- */

	@Override
	public Message preEndpointInvocation(ObserverContext<RegionCoprocessorEnvironment> ctx,
										 Service service, String methodName, Message request) throws IOException {
		// Don't intercept calls to our own AccessControlService, we check for
		// appropriate permissions in the service handlers
		if (shouldCheckExecPermission && !(service instanceof AccessControlService)) {
			requirePermission(ctx,
					"invoke(" + service.getDescriptorForType().getName() + "." + methodName + ")",
					getTableName(ctx.getEnvironment()), null, null,
					Action.EXEC);
		}
		return request;
	}

	@Override
	public void grant(RpcController controller, AccessControlProtos.GrantRequest request, RpcCallback<AccessControlProtos.GrantResponse> done) {
		boolean isSuccess = false;

		if(UpdateRangerPoliciesOnGrantRevoke) {
			GrantRevokeRequest grData = null;
	
			try {
				grData = createGrantData(request);

				RangerHBasePlugin plugin = hbasePlugin;

				if(plugin != null) {

					RangerAccessResultProcessor auditHandler = new RangerDefaultAuditHandler(hbasePlugin.getConfig());

					plugin.grantAccess(grData, auditHandler);

					isSuccess = true;
				}
			} catch(AccessControlException excp) {
				LOG.warn("grant() failed", excp);

				ResponseConverter.setControllerException(controller, new AccessDeniedException(excp));
			} catch(IOException excp) {
				LOG.warn("grant() failed", excp);

				ResponseConverter.setControllerException(controller, excp);
			} catch (Exception excp) {
				LOG.warn("grant() failed", excp);

				ResponseConverter.setControllerException(controller, new CoprocessorException(excp.getMessage()));
			}
		}

		AccessControlProtos.GrantResponse response = isSuccess ? AccessControlProtos.GrantResponse.getDefaultInstance() : null;

		done.run(response);
	}

	@Override
	public void revoke(RpcController controller, AccessControlProtos.RevokeRequest request, RpcCallback<AccessControlProtos.RevokeResponse> done) {
		boolean isSuccess = false;

		if(UpdateRangerPoliciesOnGrantRevoke) {
			GrantRevokeRequest grData = null;

			try {
				grData = createRevokeData(request);

				RangerHBasePlugin plugin = hbasePlugin;

				if(plugin != null) {

					RangerAccessResultProcessor auditHandler = new RangerDefaultAuditHandler(hbasePlugin.getConfig());

					plugin.revokeAccess(grData, auditHandler);

					isSuccess = true;
				}
			} catch(AccessControlException excp) {
				LOG.warn("revoke() failed", excp);

				ResponseConverter.setControllerException(controller, new AccessDeniedException(excp));
			} catch(IOException excp) {
				LOG.warn("revoke() failed", excp);

				ResponseConverter.setControllerException(controller, excp);
			} catch (Exception excp) {
				LOG.warn("revoke() failed", excp);

				ResponseConverter.setControllerException(controller, new CoprocessorException(excp.getMessage()));
			}
		}

		AccessControlProtos.RevokeResponse response = isSuccess ? AccessControlProtos.RevokeResponse.getDefaultInstance() : null;

		done.run(response);
	}

	@Override
	public void hasPermission(RpcController controller, AccessControlProtos.HasPermissionRequest request, RpcCallback<AccessControlProtos.HasPermissionResponse> done) {
		LOG.debug("hasPermission(): ");
	}

	@Override
	public void checkPermissions(RpcController controller, AccessControlProtos.CheckPermissionsRequest request, RpcCallback<AccessControlProtos.CheckPermissionsResponse> done) {
		LOG.debug("checkPermissions(): ");
	}

	@Override
	public void getUserPermissions(RpcController controller, AccessControlProtos.GetUserPermissionsRequest request,
			RpcCallback<AccessControlProtos.GetUserPermissionsResponse> done) {
		AccessControlProtos.GetUserPermissionsResponse response = null;
		try {
			String operation = "userPermissions";
			final RangerAccessResourceImpl resource = new RangerAccessResourceImpl();
			User user = getActiveUser(null);
			Set<String> groups = _userUtils.getUserGroups(user);
			if (groups.isEmpty() && user.getUGI() != null) {
				String[] groupArray = user.getUGI().getGroupNames();
				if (groupArray != null) {
					groups = Sets.newHashSet(groupArray);
				}
			}
			RangerAccessRequestImpl rangerAccessrequest = new RangerAccessRequestImpl(resource, null,
					_userUtils.getUserAsString(user), groups, null);
			rangerAccessrequest.setAction(operation);
			rangerAccessrequest.setClientIPAddress(getRemoteAddress());
			rangerAccessrequest.setResourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF);
			List<UserPermission> perms = null;
			if (request.getType() == AccessControlProtos.Permission.Type.Table) {
				final TableName table = request.hasTableName() ? ProtobufUtil.toTableName(request.getTableName()) : null;
				requirePermission(null, operation, table.getName(), Action.ADMIN);
				resource.setValue(RangerHBaseResource.KEY_TABLE, table.getNameAsString());
				perms = User.runAsLoginUser(new PrivilegedExceptionAction<List<UserPermission>>() {
					@Override
					public List<UserPermission> run() throws Exception {
						return getUserPermissions(
								hbasePlugin.getResourceACLs(rangerAccessrequest),
								table.getNameAsString(), false);
					}
				});
			} else if (request.getType() == AccessControlProtos.Permission.Type.Namespace) {
				final String namespace = request.getNamespaceName().toStringUtf8();
				requireGlobalPermission(null, "getUserPermissionForNamespace", namespace, Action.ADMIN);
				resource.setValue(RangerHBaseResource.KEY_TABLE, namespace + RangerHBaseResource.NAMESPACE_SEPARATOR);
				rangerAccessrequest.setRequestData(namespace);
				perms = User.runAsLoginUser(new PrivilegedExceptionAction<List<UserPermission>>() {
					@Override
					public List<UserPermission> run() throws Exception {
						return getUserPermissions(
								hbasePlugin.getResourceACLs(rangerAccessrequest),
								namespace, true);
					}
				});
			} else {
				requirePermission(null, "userPermissions", Action.ADMIN);
				perms = User.runAsLoginUser(new PrivilegedExceptionAction<List<UserPermission>>() {
					@Override
					public List<UserPermission> run() throws Exception {
						return getUserPermissions(
								hbasePlugin.getResourceACLs(rangerAccessrequest), null,
								false);
					}
				});
				if (_userUtils.isSuperUser(user)) {
					perms.add(new UserPermission(_userUtils.getUserAsString(user),
					                             Permission.newBuilder(AccessControlLists.ACL_TABLE_NAME).withActions(Action.values()).build()));
				}
			}
			response = AccessControlUtil.buildGetUserPermissionsResponse(perms);
		} catch (IOException ioe) {
			// pass exception back up
			ResponseConverter.setControllerException(controller, ioe);
		}
		done.run(response);
	}

	private List<UserPermission> getUserPermissions(RangerResourceACLs rangerResourceACLs, String resource,
                                                    boolean isNamespace) {
		List<UserPermission> userPermissions = new ArrayList<UserPermission>();
		Action[] hbaseActions = Action.values();
		List<String> hbaseActionsList = new ArrayList<String>();
		for (Action action : hbaseActions) {
			hbaseActionsList.add(action.name());
		}
		addPermission(rangerResourceACLs.getUserACLs(), isNamespace, hbaseActionsList, userPermissions, resource,
				false);
		addPermission(rangerResourceACLs.getGroupACLs(), isNamespace, hbaseActionsList, userPermissions, resource,
				true);
		return userPermissions;
	}

	private void addPermission(Map<String, Map<String, AccessResult>> acls, boolean isNamespace,
			List<String> hbaseActionsList, List<UserPermission> userPermissions, String resource, boolean isGroup) {
		for (Entry<String, Map<String, AccessResult>> userAcls : acls.entrySet()) {
			String user = !isGroup ? userAcls.getKey() : AuthUtil.toGroupEntry(userAcls.getKey());
			List<Action> allowedPermissions = new ArrayList<Action>();
			for (Entry<String, AccessResult> permissionAccess : userAcls.getValue().entrySet()) {
				String permission = _authUtils.getActionName(permissionAccess.getKey());
				if (hbaseActionsList.contains(permission)
						&& permissionAccess.getValue().getResult() == RangerPolicyEvaluator.ACCESS_ALLOWED) {
					allowedPermissions.add(Action.valueOf(permission));
				}

			}
			if (!allowedPermissions.isEmpty()) {
				UserPermission up = null;
				if (isNamespace) {
					up = new UserPermission(user,
                                                                Permission.newBuilder(resource).withActions(allowedPermissions.toArray(new Action[allowedPermissions.size()])).build());
				} else {
					up = new UserPermission(user,
                                                                Permission.newBuilder(TableName.valueOf(resource)).withActions(allowedPermissions.toArray(new Action[allowedPermissions.size()])).build());
				}
				userPermissions.add(up);
			}
		}
	}

	private GrantRevokeRequest createGrantData(AccessControlProtos.GrantRequest request) throws Exception {
		AccessControlProtos.UserPermission up   = request.getUserPermission();
		AccessControlProtos.Permission     perm = up == null ? null : up.getPermission();

		UserPermission      userPerm  = up == null ? null : AccessControlUtil.toUserPermission(up);
		Permission.Action[] actions   = userPerm == null ? null : userPerm.getPermission().getActions();
		String              userName  = userPerm == null ? null : userPerm.getUser();
		String              nameSpace = null;
		String              tableName = null;
		String              colFamily = null;
		String              qualifier = null;

		if(perm == null) {
			throw new Exception("grant(): invalid data - permission is null");
		}

		if(StringUtil.isEmpty(userName)) {
			throw new Exception("grant(): invalid data - username empty");
		}

		if ((actions == null) || (actions.length == 0)) {
			throw new Exception("grant(): invalid data - no action specified");
		}

		switch(perm.getType()) {
			case Global:
				tableName = colFamily = qualifier = RangerHBaseResource.WILDCARD;
			break;

			case Table:
				TablePermission tablePerm = (TablePermission)userPerm.getPermission();
				tableName = Bytes.toString(tablePerm.getTableName().getName());
				colFamily = Bytes.toString(tablePerm.getFamily());
				qualifier = Bytes.toString(tablePerm.getQualifier());
			break;

			case Namespace:
				NamespacePermission namepsacePermission = (NamespacePermission)userPerm.getPermission();
				nameSpace = namepsacePermission.getNamespace();
			break;
		}
		
		if(StringUtil.isEmpty(nameSpace) && StringUtil.isEmpty(tableName) && StringUtil.isEmpty(colFamily) && StringUtil.isEmpty(qualifier)) {
			throw new Exception("grant(): namespace/table/columnFamily/columnQualifier not specified");
		}

		tableName = StringUtil.isEmpty(tableName) ? RangerHBaseResource.WILDCARD : tableName;
		colFamily = StringUtil.isEmpty(colFamily) ? RangerHBaseResource.WILDCARD : colFamily;
		qualifier = StringUtil.isEmpty(qualifier) ? RangerHBaseResource.WILDCARD : qualifier;

		if(! StringUtil.isEmpty(nameSpace)) {
			tableName = nameSpace + RangerHBaseResource.NAMESPACE_SEPARATOR + tableName;
		}

		User   activeUser = getActiveUser(null);
		String grantor    = activeUser != null ? activeUser.getShortName() : null;
		String[] groups   = activeUser != null ? activeUser.getGroupNames() : null;

		Set<String> grantorGroups = null;

		if (groups != null && groups.length > 0) {
			grantorGroups = new HashSet<>(Arrays.asList(groups));
		}

		Map<String, String> mapResource = new HashMap<String, String>();
		mapResource.put(RangerHBaseResource.KEY_TABLE, tableName);
		mapResource.put(RangerHBaseResource.KEY_COLUMN_FAMILY, colFamily);
		mapResource.put(RangerHBaseResource.KEY_COLUMN, qualifier);

		GrantRevokeRequest ret = new GrantRevokeRequest();

		ret.setGrantor(grantor);
		ret.setGrantorGroups(grantorGroups);
		ret.setDelegateAdmin(Boolean.FALSE);
		ret.setEnableAudit(Boolean.TRUE);
		ret.setReplaceExistingPermissions(Boolean.TRUE);
		ret.setResource(mapResource);
		ret.setClientIPAddress(getRemoteAddress());
		ret.setForwardedAddresses(null);//TODO: Need to check with Knox proxy how they handle forwarded add.
		ret.setRemoteIPAddress(getRemoteAddress());
		ret.setRequestData(up.toString());

		if(userName.startsWith(GROUP_PREFIX)) {
			ret.getGroups().add(userName.substring(GROUP_PREFIX.length()));
		} else {
			ret.getUsers().add(userName);
		}

		for (Permission.Action action : actions) {
			switch(action.code()) {
				case 'R':
					ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_READ);
				break;

				case 'W':
					ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_WRITE);
				break;

				case 'C':
					ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_CREATE);
				break;

				case 'A':
					ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_ADMIN);
					ret.setDelegateAdmin(Boolean.TRUE);
				break;
				case 'X':
					ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_EXECUTE);
				break;
				default:
					LOG.warn("grant(): ignoring action '" + action.name() + "' for user '" + userName + "'");
			}
		}

		return ret;
	}

	private GrantRevokeRequest createRevokeData(AccessControlProtos.RevokeRequest request) throws Exception {
		AccessControlProtos.UserPermission up   = request.getUserPermission();
		AccessControlProtos.Permission     perm = up == null ? null : up.getPermission();

		UserPermission      userPerm  = up == null ? null : AccessControlUtil.toUserPermission(up);
		String              userName  = userPerm == null ? null : userPerm.getUser();
		String              nameSpace = null;
		String              tableName = null;
		String              colFamily = null;
		String              qualifier = null;

		if(perm == null) {
			throw new Exception("revoke(): invalid data - permission is null");
		}

		if(StringUtil.isEmpty(userName)) {
			throw new Exception("revoke(): invalid data - username empty");
		}

		switch(perm.getType()) {
			case Global :
				tableName = colFamily = qualifier = RangerHBaseResource.WILDCARD;
			break;

			case Table :
				TablePermission tablePerm = (TablePermission)userPerm.getPermission();
				tableName = Bytes.toString(tablePerm.getTableName().getName());
				colFamily = Bytes.toString(tablePerm.getFamily());
				qualifier = Bytes.toString(tablePerm.getQualifier());
			break;

			case Namespace:
				NamespacePermission namespacePermission = (NamespacePermission)userPerm.getPermission();
				nameSpace = namespacePermission.getNamespace();
			break;
		}

		if(StringUtil.isEmpty(nameSpace) && StringUtil.isEmpty(tableName) && StringUtil.isEmpty(colFamily) && StringUtil.isEmpty(qualifier)) {
			throw new Exception("revoke(): table/columnFamily/columnQualifier not specified");
		}

		tableName = StringUtil.isEmpty(tableName) ? RangerHBaseResource.WILDCARD : tableName;
		colFamily = StringUtil.isEmpty(colFamily) ? RangerHBaseResource.WILDCARD : colFamily;
		qualifier = StringUtil.isEmpty(qualifier) ? RangerHBaseResource.WILDCARD : qualifier;

		if(! StringUtil.isEmpty(nameSpace)) {
			tableName = nameSpace + RangerHBaseResource.NAMESPACE_SEPARATOR + tableName;
		}

		User   activeUser = getActiveUser(null);
		String grantor    = activeUser != null ? activeUser.getShortName() : null;
		String[] groups   = activeUser != null ? activeUser.getGroupNames() : null;

		Set<String> grantorGroups = null;

		if (groups != null && groups.length > 0) {
			grantorGroups = new HashSet<>(Arrays.asList(groups));
		}

		Map<String, String> mapResource = new HashMap<String, String>();
		mapResource.put(RangerHBaseResource.KEY_TABLE, tableName);
		mapResource.put(RangerHBaseResource.KEY_COLUMN_FAMILY, colFamily);
		mapResource.put(RangerHBaseResource.KEY_COLUMN, qualifier);

		GrantRevokeRequest ret = new GrantRevokeRequest();

		ret.setGrantor(grantor);
		ret.setGrantorGroups(grantorGroups);
		ret.setDelegateAdmin(Boolean.TRUE); // remove delegateAdmin privilege as well
		ret.setEnableAudit(Boolean.TRUE);
		ret.setReplaceExistingPermissions(Boolean.TRUE);
		ret.setResource(mapResource);
		ret.setClientIPAddress(getRemoteAddress());
		ret.setForwardedAddresses(null);//TODO: Need to check with Knox proxy how they handle forwarded add.
		ret.setRemoteIPAddress(getRemoteAddress());
		ret.setRequestData(up.toString());
		
		if(userName.startsWith(GROUP_PREFIX)) {
			ret.getGroups().add(userName.substring(GROUP_PREFIX.length()));
		} else {
			ret.getUsers().add(userName);
		}

		// revoke removes all permissions
		ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_READ);
		ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_WRITE);
		ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_CREATE);
		ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_ADMIN);
		ret.getAccessTypes().add(HbaseAuthUtils.ACCESS_TYPE_EXECUTE);

		return ret;
	}

	private void cleanUp_HBaseRangerPlugin() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAuthorizationCoprocessor.cleanUp_HBaseRangerPlugin()");
		}
		if (hbasePlugin != null) {
			hbasePlugin.setHBaseShuttingDown(true);
			hbasePlugin.cleanup();
			AuditProviderFactory auditProviderFactory = hbasePlugin.getAuditProviderFactory();
			if (auditProviderFactory != null) {
				auditProviderFactory.shutdown();
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAuthorizationCoprocessor.cleanUp_HBaseRangerPlugin() completed!");
		}
	}

	private String getCommandString(String operationName, String tableNameStr, Map<String,Object> opMetaData) {
		StringBuilder ret = new StringBuilder();
		if (!HbaseConstants.HBASE_META_TABLE.equals(tableNameStr)) {
			ret.append(operationName);
			ret.append(HbaseConstants.SPACE);
			ret.append(tableNameStr).append(HbaseConstants.COMMA).append(HbaseConstants.SPACE);
			ret.append(getPredicates(operationName, opMetaData));
		}
		return ret.toString();
	}

	private String getPredicates(String operationName, Map<String,Object> opMetaData) {
		StringBuilder ret = new StringBuilder();

		if (MapUtils.isNotEmpty(opMetaData)) {
			HashMap<String, ArrayList<?>> families    = (HashMap<String, ArrayList<?>>) opMetaData.get(HbaseConstants.FAMILIES);
			String 						  startRowVal = (String) opMetaData.get(HbaseConstants.STARTROW);
			String                        stopRowVal  = (String) opMetaData.get(HbaseConstants.STOPROW);
			String                        filterVal   = (String) opMetaData.get(HbaseConstants.FILTER);
			String                        rowVal      = (String) opMetaData.get(HbaseConstants.ROW);

			if (!isQueryforInfo(families)) {
				ret.append(HbaseConstants.OPEN_BRACES);
				if (HbaseConstants.SCAN.equals(operationName)) {
					if (StringUtils.isNotEmpty(startRowVal)) {
						ret.append(formatPredicate(ret, PredicateType.STARTROW, startRowVal));
					}
					if (StringUtils.isNotEmpty(stopRowVal)) {
						ret.append(formatPredicate(ret, PredicateType.STOPROW, stopRowVal));
					}
				} else {
					if(StringUtils.isNotEmpty(rowVal)) {
						ret.append(formatPredicate(ret, PredicateType.ROW, rowVal));
					}
				}
				if (StringUtils.isNotEmpty(filterVal)) {
					ret.append(formatPredicate(ret, PredicateType.FILTER, filterVal));
				}
				if (MapUtils.isNotEmpty(families)) {
					String colfamily = families.toString();
					ret.append(formatPredicate(ret, PredicateType.COLUMNS, colfamily));
				}
				ret.append(HbaseConstants.SPACE).append(HbaseConstants.CLOSED_BRACES);
			}
		}
		return ret.toString();
	}

	private boolean isQueryforInfo(HashMap<String, ArrayList<?>> families) {
		boolean ret = false;
		for(HashMap.Entry family : families.entrySet()) {
			String familyKey = (String) family.getKey();
			if (HbaseConstants.INFO.equals(familyKey)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private String formatPredicate(StringBuilder commandStr, PredicateType predicateType, String val) {
		StringBuilder ret = new StringBuilder();
		if (HbaseConstants.OPEN_BRACES.equals(commandStr.toString())) {
			ret.append(HbaseConstants.SPACE);
		} else {
			ret.append(HbaseConstants.COMMA).append(HbaseConstants.SPACE);
		}
		ret.append(buildPredicate(predicateType, val));
		return ret.toString();
	}

	private String buildPredicate(PredicateType predicateType, String val) {
		StringBuilder ret = new StringBuilder();
		switch (predicateType) {
			case STARTROW:
				ret.append(PredicateType.STARTROW.name().toUpperCase());
				ret.append(HbaseConstants.ARROW);
				ret.append(HbaseConstants.SINGLE_QUOTES).append(val).append(HbaseConstants.SINGLE_QUOTES);
				break;
			case STOPROW:
				ret.append(PredicateType.STOPROW.name().toUpperCase());
				ret.append(HbaseConstants.ARROW);
				ret.append(HbaseConstants.SINGLE_QUOTES).append(val).append(HbaseConstants.SINGLE_QUOTES);
				break;
			case FILTER:
				ret.append(PredicateType.FILTER.name().toUpperCase());
				ret.append(HbaseConstants.ARROW);
				ret.append(HbaseConstants.SINGLE_QUOTES).append(val).append(HbaseConstants.SINGLE_QUOTES);
				break;
			case COLUMNS:
				ret.append(PredicateType.COLUMNS.name().toUpperCase());
				ret.append(HbaseConstants.ARROW);
				ret.append(HbaseConstants.SINGLE_QUOTES).append(val).append(HbaseConstants.SINGLE_QUOTES);
				break;
			case ROW:
				ret.append(val);
				break;
		}
		return ret.toString();
	}

	private void checkGetTableInfoAccess(ObserverContext<MasterCoprocessorEnvironment> ctx, String operation, List<TableDescriptor> descriptors, String regex, String accessPermission) {

		if (CollectionUtils.isNotEmpty(descriptors)) {
			// Retains only those which passes authorization checks
			User user = getActiveUser(ctx);
			String access = accessPermission;
			HbaseAuditHandler auditHandler = _factory.getAuditHandler();  // this will accumulate audits for all tables that succeed.
			AuthorizationSession session = new AuthorizationSession(hbasePlugin)
					.operation(operation)
					.otherInformation("regex=" + regex)
					.remoteAddress(getRemoteAddress())
					.auditHandler(auditHandler)
					.user(user)
					.access(access);

			Iterator<TableDescriptor> itr = descriptors.iterator();
			while (itr.hasNext()) {
				TableDescriptor htd = itr.next();
				String tableName = htd.getTableName().getNameAsString();
				session.table(tableName).buildRequest().authorize();
				if (!session.isAuthorized()) {
					List<AuthzAuditEvent> events = null;
					itr.remove();
					AuthzAuditEvent event = auditHandler.getAndDiscardMostRecentEvent();
					if (event != null) {
						events = Lists.newArrayList(event);
					}
					auditHandler.logAuthzAudits(events);
				}
			}
			if (descriptors.size() > 0) {
				session.logCapturedEvents();
			}
		}
	}

	private void checkAccessForNamespaceDescriptor(ObserverContext<MasterCoprocessorEnvironment> ctx, String operation, List<NamespaceDescriptor> descriptors) {

		if (CollectionUtils.isNotEmpty(descriptors)) {
			// Retains only those which passes authorization checks
			User user = getActiveUser(ctx);
			String access = _authUtils.getAccess(Action.ADMIN);
			HbaseAuditHandler auditHandler = _factory.getAuditHandler();  // this will accumulate audits for all tables that succeed.
			AuthorizationSession session = new AuthorizationSession(hbasePlugin)
					.operation(operation)
					.remoteAddress(getRemoteAddress())
					.auditHandler(auditHandler)
					.user(user)
					.access(access);

			Iterator<NamespaceDescriptor> itr = descriptors.iterator();
			while (itr.hasNext()) {
				NamespaceDescriptor namespaceDescriptor = itr.next();
				String namespace = namespaceDescriptor.getName();
				session.table(namespace).buildRequest().authorize();
				if (!session.isAuthorized()) {
					List<AuthzAuditEvent> events = null;
					itr.remove();
					AuthzAuditEvent event = auditHandler.getAndDiscardMostRecentEvent();
					if (event != null) {
						events = Lists.newArrayList(event);
					}
					auditHandler.logAuthzAudits(events);
				}
			}
			if (descriptors.size() > 0) {
				session.logCapturedEvents();
			}
		}
	}

	enum PredicateType {STARTROW, STOPROW, FILTER, COLUMNS, ROW};
}


class RangerHBasePlugin extends RangerBasePlugin {
	private static final Logger LOG = LoggerFactory.getLogger(RangerHBasePlugin.class);
	boolean isHBaseShuttingDown  = false;

	public RangerHBasePlugin(String appType) {
		super("hbase", appType);
	}

	public void setHBaseShuttingDown(boolean hbaseShuttingDown) {
		isHBaseShuttingDown = hbaseShuttingDown;
	}

	@Override
	public RangerAccessResult isAccessAllowed(RangerAccessRequest request, RangerAccessResultProcessor resultProcessor) {
		RangerAccessResult ret = null;
		if (isHBaseShuttingDown) {
			ret = new RangerAccessResult(RangerPolicy.POLICY_TYPE_ACCESS, this.getServiceName(), this.getServiceDef(), request);
			ret.setIsAllowed(true);
			ret.setIsAudited(false);
			LOG.warn("Auth request came after HBase shutdown....");
		} else {
			ret = super.isAccessAllowed(request, resultProcessor);
		}
		return ret;
	}
}


