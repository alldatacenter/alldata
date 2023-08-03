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

package org.apache.ranger.ldapusersync.process;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.Rdn;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.ugsyncutil.util.UgsyncCommonConstants;
import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.apache.ranger.ugsyncutil.model.LdapSyncSourceInfo;
import org.apache.ranger.ugsyncutil.model.UgsyncAuditInfo;
import org.apache.ranger.usergroupsync.UserGroupSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.ranger.usergroupsync.UserGroupSource;

public class LdapUserGroupBuilder implements UserGroupSource {
	
	private static final Logger LOG = LoggerFactory.getLogger(LdapUserGroupBuilder.class);

	private UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();

	private static final String DATA_TYPE_BYTEARRAY = "byte[]";
	private static final String DATE_FORMAT = "yyyyMMddHHmmss";
	private static final int PAGE_SIZE = 500;
	private static final String MEMBER_OF_ATTR = "memberof=";
	private static final String GROUP_NAME_ATTRIBUTE = "cn=";
	private static long deltaSyncUserTime = 0; // Used for AD uSNChanged
	private static long deltaSyncGroupTime = 0; // Used for AD uSNChanged
	private String deltaSyncUserTimeStamp; // Used for OpenLdap modifyTimestamp
	private String deltaSyncGroupTimeStamp; // Used for OpenLdap modifyTimestamp

  private String ldapUrl;
  private String ldapBindDn;
  private String ldapBindPassword;
  private String ldapAuthenticationMechanism;
  private String ldapReferral;
  private String searchBase;

  private String[] userSearchBase;
	private String userNameAttribute;
	private String userCloudIdAttribute;
  private int    userSearchScope;
  private String userObjectClass;
  private String userSearchFilter;
  private Set<String> groupNameSet;
  private String extendedUserSearchFilter;
  private SearchControls userSearchControls;
  private Set<String> userGroupNameAttributeSet;
  private Set<String> otherUserAttributes;

  private boolean pagedResultsEnabled = true;
  private int pagedResultsSize = PAGE_SIZE;

  private boolean groupSearchFirstEnabled = true;
  private boolean userSearchEnabled = true;
  private boolean groupSearchEnabled = true;
  private String[] groupSearchBase;
  private int    groupSearchScope;
  private String groupObjectClass;
  private String groupSearchFilter;
  private String extendedGroupSearchFilter;
  private String extendedAllGroupsSearchFilter;
  private SearchControls groupSearchControls;
  private String groupMemberAttributeName;
  private String groupNameAttribute;
	private String groupCloudIdAttribute;
	private Set<String> otherGroupAttributes;
	private int groupHierarchyLevels;
    private int deleteCycles;
    private String currentSyncSource;

	private LdapContext ldapContext;
	StartTlsResponse tls;

  private Table<String, String, String> groupUserTable;
	UgsyncAuditInfo ugsyncAuditInfo;
	LdapSyncSourceInfo ldapSyncSourceInfo;

	private Map<String, Map<String, String>> sourceUsers; // key is user DN and value is map of user attributes containing original name, DN, etc...
	private Map<String, Map<String, String>> sourceGroups; // key is group DN and value is map of group attributes containing original name, DN, etc...
	private Map<String, Set<String>> sourceGroupUsers; // key is group DN and value is set of user DNs (members)

	public static void main(String[] args) throws Throwable {
		LdapUserGroupBuilder ugBuilder = new LdapUserGroupBuilder();
		ugBuilder.init();
	}

	@Override
	public void init() throws Throwable{
		deltaSyncUserTime = 0;
		deltaSyncGroupTime = 0;
        deleteCycles = 1;
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		deltaSyncUserTimeStamp = dateFormat.format(new Date(0));
		deltaSyncGroupTimeStamp = dateFormat.format(new Date(0));
		setConfig();
		ugsyncAuditInfo = new UgsyncAuditInfo();
		ldapSyncSourceInfo = new LdapSyncSourceInfo();
		ldapSyncSourceInfo.setLdapUrl(ldapUrl);
		ldapSyncSourceInfo.setIncrementalSycn(Boolean.toString(config.isDeltaSyncEnabled()));
		ldapSyncSourceInfo.setUserSearchEnabled(Boolean.toString(userSearchEnabled));
		ldapSyncSourceInfo.setGroupSearchEnabled(Boolean.toString(groupSearchEnabled));
		ldapSyncSourceInfo.setGroupSearchFirstEnabled(Boolean.toString(groupSearchFirstEnabled));
		ldapSyncSourceInfo.setGroupHierarchyLevel(Integer.toString(groupHierarchyLevels));
		ugsyncAuditInfo.setSyncSource(currentSyncSource);
		ugsyncAuditInfo.setLdapSyncSourceInfo(ldapSyncSourceInfo);
	}

	private void createLdapContext() throws Throwable {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapUrl);
		if (ldapUrl.startsWith("ldaps") && (config.getSSLTrustStorePath() != null && !config.getSSLTrustStorePath().trim().isEmpty())) {
			env.put("java.naming.ldap.factory.socket", "org.apache.ranger.ldapusersync.process.CustomSSLSocketFactory");
		}

		if (StringUtils.isNotEmpty(userCloudIdAttribute)) {
			if (config.getUserCloudIdAttributeDataType().equals(DATA_TYPE_BYTEARRAY)) {
				env.put("java.naming.ldap.attributes.binary", userCloudIdAttribute);
			}
		}

		if (StringUtils.isNotEmpty(groupCloudIdAttribute)) {
			if (config.getGroupCloudIdAttributeDataType().equals(DATA_TYPE_BYTEARRAY)) {
				env.put("java.naming.ldap.attributes.binary", groupCloudIdAttribute);
			}
		}

		for (String otherUserAttribute : otherUserAttributes) {
			String attrType = config.getOtherUserAttributeDataType(otherUserAttribute);
			if (attrType.equals(DATA_TYPE_BYTEARRAY)) {
				env.put("java.naming.ldap.attributes.binary", otherUserAttribute);
			}
		}

		for (String otherGroupAttribute : otherGroupAttributes) {
			String attrType = config.getOtherGroupAttributeDataType(otherGroupAttribute);
			if (attrType.equals(DATA_TYPE_BYTEARRAY)) {
				env.put("java.naming.ldap.attributes.binary", otherGroupAttribute);
			}
		}
		ldapContext = new InitialLdapContext(env, null);
		if (!ldapUrl.startsWith("ldaps")) {
			if (config.isStartTlsEnabled()) {
				tls = (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
				if (config.getSSLTrustStorePath() != null && !config.getSSLTrustStorePath().trim().isEmpty()) {
					tls.negotiate(CustomSSLSocketFactory.getDefault());
				} else {
					tls.negotiate();
				}
				LOG.info("Starting TLS session...");
			}
		}

		ldapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, ldapBindDn);
		ldapContext.addToEnvironment(Context.SECURITY_CREDENTIALS, ldapBindPassword);
		ldapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, ldapAuthenticationMechanism);
		ldapContext.addToEnvironment(Context.REFERRAL, ldapReferral);
	}

	private void setConfig() throws Throwable {
		LOG.info("LdapUserGroupBuilder initialization started");

		currentSyncSource = config.getCurrentSyncSource();
		groupSearchFirstEnabled = true;
		userSearchEnabled = config.isUserSearchEnabled();
		groupSearchEnabled = config.isGroupSearchEnabled();
		ldapUrl = config.getLdapUrl();
		ldapBindDn = config.getLdapBindDn();
		ldapBindPassword = config.getLdapBindPassword();
		//ldapBindPassword = "admin-password";
		ldapAuthenticationMechanism = config.getLdapAuthenticationMechanism();
		ldapReferral = config.getContextReferral();
		searchBase = config.getSearchBase();

		userSearchBase = config.getUserSearchBase().split(";");
		userSearchScope = config.getUserSearchScope();
		userObjectClass = config.getUserObjectClass();
		userSearchFilter = config.getUserSearchFilter();
		userNameAttribute = config.getUserNameAttribute();
		userCloudIdAttribute = config.getUserCloudIdAttribute();

		Set<String> userSearchAttributes = new HashSet<String>();
		userSearchAttributes.add(userNameAttribute);
		userGroupNameAttributeSet = config.getUserGroupNameAttributeSet();
		for (String useGroupNameAttribute : userGroupNameAttributeSet) {
			userSearchAttributes.add(useGroupNameAttribute);
		}
		userSearchAttributes.add(userCloudIdAttribute);
		otherUserAttributes = config.getOtherUserAttributes();
		for (String otherUserAttribute : otherUserAttributes) {
			userSearchAttributes.add(otherUserAttribute);
		}
		userSearchAttributes.add("uSNChanged");
		userSearchAttributes.add("modifytimestamp");
		userSearchControls = new SearchControls();
		userSearchControls.setSearchScope(userSearchScope);
		userSearchControls.setReturningAttributes(userSearchAttributes.toArray(
				new String[userSearchAttributes.size()]));

		pagedResultsEnabled = config.isPagedResultsEnabled();
		pagedResultsSize = config.getPagedResultsSize();

		groupSearchBase = config.getGroupSearchBase().split(";");
		groupSearchScope = config.getGroupSearchScope();
		groupObjectClass = config.getGroupObjectClass();
		groupSearchFilter = config.getGroupSearchFilter();
		groupMemberAttributeName = config.getUserGroupMemberAttributeName();
		groupNameAttribute = config.getGroupNameAttribute();
		groupCloudIdAttribute = config.getGroupCloudIdAttribute();
		groupHierarchyLevels = config.getGroupHierarchyLevels();

		extendedGroupSearchFilter = "(&" + extendedGroupSearchFilter + "(|(" + groupMemberAttributeName + "={0})(" + groupMemberAttributeName + "={1})))";

		groupSearchControls = new SearchControls();
		groupSearchControls.setSearchScope(groupSearchScope);

		Set<String> groupSearchAttributes = new HashSet<String>();
		groupSearchAttributes.add(groupNameAttribute);
		groupSearchAttributes.add(groupCloudIdAttribute);
		groupSearchAttributes.add(groupMemberAttributeName);
		groupSearchAttributes.add("uSNChanged");
		groupSearchAttributes.add("modifytimestamp");
		otherGroupAttributes = config.getOtherGroupAttributes();
		for (String otherGroupAttribute : otherGroupAttributes) {
			groupSearchAttributes.add(otherGroupAttribute);
		}
		groupSearchControls.setReturningAttributes(groupSearchAttributes.toArray(
				new String[groupSearchAttributes.size()]));

		if (StringUtils.isEmpty(userSearchFilter)) {
			groupNameSet = config.getGroupNameSet();
			String computedSearchFilter = "";
			for (String groupName : groupNameSet) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("groupName = " + groupName);
				}
				if (!groupName.startsWith(MEMBER_OF_ATTR) && !groupName.startsWith(GROUP_NAME_ATTRIBUTE)){
					LOG.info("Ignoring unsupported format for " + groupName );
					continue;
				}
				String searchFilter = groupName;
				if (groupName.startsWith(MEMBER_OF_ATTR)) {
					searchFilter = groupName.substring(MEMBER_OF_ATTR.length());
				}
				searchFilter = getFirstRDN(searchFilter);
				computedSearchFilter += getDNForMemberOf(searchFilter);
			}
			if (StringUtils.isNotEmpty(computedSearchFilter)) {
				computedSearchFilter = "(|" + computedSearchFilter + ")";
			}
			LOG.info("Final computedSearchFilter = " + computedSearchFilter);
			userSearchFilter = computedSearchFilter;
		}

		LOG.info("LdapUserGroupBuilder initialization completed with --  "
				+ "ldapUrl: " + ldapUrl
				+ ",  ldapBindDn: " + ldapBindDn
				+ ",  ldapBindPassword: ***** "
				+ ",  ldapAuthenticationMechanism: " + ldapAuthenticationMechanism
				+ ",  searchBase: " + searchBase
				+ ",  userSearchBase: " + Arrays.toString(userSearchBase)
				+ ",  userSearchScope: " + userSearchScope
				+ ",  userObjectClass: " + userObjectClass
				+ ",  userSearchFilter: " + userSearchFilter
				+ ",  extendedUserSearchFilter: " + extendedUserSearchFilter
				+ ",  userNameAttribute: " + userNameAttribute
				+ ",  userSearchAttributes: " + userSearchAttributes
				+ ",  userGroupNameAttributeSet: " + userGroupNameAttributeSet
				+ ",  otherUserAttributes: " + otherUserAttributes
				+ ",  pagedResultsEnabled: " + pagedResultsEnabled
				+ ",  pagedResultsSize: " + pagedResultsSize
				+ ",  groupSearchEnabled: " + groupSearchEnabled
				+ ",  groupSearchBase: " + Arrays.toString(groupSearchBase)
				+ ",  groupSearchScope: " + groupSearchScope
				+ ",  groupObjectClass: " + groupObjectClass
				+ ",  groupSearchFilter: " + groupSearchFilter
				+ ",  extendedGroupSearchFilter: " + extendedGroupSearchFilter
				+ ",  extendedAllGroupsSearchFilter: " + extendedAllGroupsSearchFilter
				+ ",  groupMemberAttributeName: " + groupMemberAttributeName
				+ ",  groupNameAttribute: " + groupNameAttribute
				+ ", groupSearchAttributes: " + groupSearchAttributes
				+ ", groupSearchFirstEnabled: " + groupSearchFirstEnabled
				+ ", userSearchEnabled: " + userSearchEnabled
				+ ",  ldapReferral: " + ldapReferral
		);
	}

	private void closeLdapContext() throws Throwable {
		if (tls != null) {
			tls.close();
		}
		if (ldapContext != null) {
			ldapContext.close();
		}
	}

	@Override
	public boolean isChanged() {
		// we do not want to get the full ldap dit and check whether anything has changed
		return true;
	}

	@Override
	public void updateSink(UserGroupSink sink) throws Throwable {
		LOG.info("LdapUserGroupBuilder updateSink started");
        boolean computeDeletes = false;
		groupUserTable = HashBasedTable.create();
		sourceGroups = new HashMap<>();
		sourceUsers = new HashMap<>();
		sourceGroupUsers = new HashMap<>();
		long highestdeltaSyncUserTime = 0;
		long highestdeltaSyncGroupTime = 0;

		if (config.isUserSyncDeletesEnabled() && deleteCycles >= config.getUserSyncDeletesFrequency()) {
			deleteCycles = 1;
			computeDeletes = true;
			if (LOG.isDebugEnabled()) {
				LOG.debug("Compute deleted users/groups is enabled for this sync cycle");
			}
		}
		if (config.isUserSyncDeletesEnabled()) {
			deleteCycles++;
		}
		if (groupSearchEnabled) {
			highestdeltaSyncGroupTime = getGroups(computeDeletes);
		}
		if (userSearchEnabled) {
			LOG.info("Performing user search to retrieve users from AD/LDAP");
			highestdeltaSyncUserTime = getUsers(computeDeletes);
		}

		if (groupHierarchyLevels > 0) {
			LOG.info("Going through group hierarchy for nested group evaluation");
            Set<String> groupFullNames = sourceGroups.keySet();
			for(String group : groupFullNames) {
				Set<String> nextLevelGroups = groupUserTable.column(group).keySet();
				goUpGroupHierarchy(nextLevelGroups, groupHierarchyLevels-1, group);
			}
			LOG.info("Completed group hierarchy computation");
		}

		Iterator<String> groupUserTableIterator = groupUserTable.rowKeySet().iterator();
		while (groupUserTableIterator.hasNext()) {
			String groupName = groupUserTableIterator.next();
			Map<String,String> groupUsersMap =  groupUserTable.row(groupName);
			Set<String> userSet = new HashSet<String>();
			for(Map.Entry<String, String> entry : groupUsersMap.entrySet()){
				if (sourceUsers.containsKey(entry.getValue())) {
					userSet.add(entry.getValue());
				}
		    }
			sourceGroupUsers.put(groupName, userSet);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Users = " + sourceUsers.keySet());
			LOG.debug("Groups = " + sourceGroups.keySet());
			LOG.debug("GroupUsers = " + sourceGroupUsers.keySet());
		}

		try {
			sink.addOrUpdateUsersGroups(sourceGroups, sourceUsers, sourceGroupUsers, computeDeletes);
			DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			LOG.info("deltaSyncUserTime = " + deltaSyncUserTime + " and highestdeltaSyncUserTime = " + highestdeltaSyncUserTime);
			if (deltaSyncUserTime < highestdeltaSyncUserTime) {
				// Incrementing highestdeltaSyncUserTime (for AD) in order to avoid search record repetition for next sync cycle.
				deltaSyncUserTime = highestdeltaSyncUserTime + 1;
				// Incrementing the highest timestamp value (for Openldap) with 1sec in order to avoid search record repetition for next sync cycle.
				deltaSyncUserTimeStamp = dateFormat.format(new Date(highestdeltaSyncUserTime + 60l));
			}

			LOG.info("deltaSyncGroupTime = " + deltaSyncGroupTime + " and highestdeltaSyncGroupTime = " + highestdeltaSyncGroupTime);
			// Update deltaSyncUserTime/deltaSyncUserTimeStamp here so that in case of failures, we get updates in next cycle
			if (deltaSyncGroupTime < highestdeltaSyncGroupTime) {
				// Incrementing highestdeltaSyncGroupTime (for AD) in order to avoid search record repetition for next sync cycle.
				deltaSyncGroupTime = highestdeltaSyncGroupTime+1;
				// Incrementing the highest timestamp value (for OpenLdap) with 1min in order to avoid search record repetition for next sync cycle.
				deltaSyncGroupTimeStamp = dateFormat.format(new Date(highestdeltaSyncGroupTime + 60l));
			}
		} catch (Throwable t) {
			LOG.error("Failed to update ranger admin. Will retry in next sync cycle!!", t);
		}

		ldapSyncSourceInfo.setUserSearchFilter(extendedUserSearchFilter);
		ldapSyncSourceInfo.setGroupSearchFilter(extendedAllGroupsSearchFilter);

		try {
			sink.postUserGroupAuditInfo(ugsyncAuditInfo);
		} catch (Throwable t) {
			LOG.error("sink.postUserGroupAuditInfo failed with exception: " + t.getMessage());
		}
	}

	private long getUsers(boolean computeDeletes) throws Throwable {
		NamingEnumeration<SearchResult> userSearchResultEnum = null;
		NamingEnumeration<SearchResult> groupSearchResultEnum = null;
		long highestdeltaSyncUserTime;
		try {
			createLdapContext();
			int total;
			// Activate paged results
			if (pagedResultsEnabled)   {
				ldapContext.setRequestControls(new Control[]{
						new PagedResultsControl(pagedResultsSize, Control.NONCRITICAL) });
			}
			DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			if (groupUserTable.rowKeySet().size() != 0 || !config.isDeltaSyncEnabled() || (computeDeletes)) {
				// Fix RANGER-1957: Perform full sync when there are updates to the groups or when incremental sync is not enabled
				deltaSyncUserTime = 0;
				deltaSyncUserTimeStamp = dateFormat.format(new Date(0));
			}

			extendedUserSearchFilter = "(objectclass=" + userObjectClass + ")(|(uSNChanged>=" + deltaSyncUserTime + ")(modifyTimestamp>=" + deltaSyncUserTimeStamp + "Z))";

			if (userSearchFilter != null && !userSearchFilter.trim().isEmpty()) {
				String customFilter = userSearchFilter.trim();
				if (!customFilter.startsWith("(")) {
					customFilter = "(" + customFilter + ")";
				}

				extendedUserSearchFilter = "(&" + extendedUserSearchFilter + customFilter + ")";
			} else {
				extendedUserSearchFilter = "(&" + extendedUserSearchFilter + ")";
			}
			LOG.info("extendedUserSearchFilter = " + extendedUserSearchFilter);

			highestdeltaSyncUserTime = deltaSyncUserTime;

			// When multiple OUs are configured, go through each OU as the user search base to search for users.
			for (int ou=0; ou<userSearchBase.length; ou++) {
				byte[] cookie = null;
				int counter = 0;
				try {
				int paged = 0;
				do {
					userSearchResultEnum = ldapContext
							.search(userSearchBase[ou], extendedUserSearchFilter,
									userSearchControls);

					while (userSearchResultEnum.hasMore()) {
						// searchResults contains all the user entries
						final SearchResult userEntry = userSearchResultEnum.next();

						if (userEntry == null)  {
							LOG.info("userEntry null, skipping sync for the entry");
							continue;
						}

						Attributes attributes =   userEntry.getAttributes();
						if (attributes == null)  {
							LOG.info("attributes  missing for entry " + userEntry.getNameInNamespace() +
									", skipping sync");
							continue;
						}

						Attribute userNameAttr  = attributes.get(userNameAttribute);
						if (userNameAttr == null)  {
							LOG.info(userNameAttribute + " missing for entry " + userEntry.getNameInNamespace() +
									", skipping sync");
							continue;
						}

						String userFullName = (userEntry.getNameInNamespace());
						String userName = (String) userNameAttr.get();

						if (userName == null || userName.trim().isEmpty())  {
							LOG.info(userNameAttribute + " empty for entry " + userEntry.getNameInNamespace() +
									", skipping sync");
							continue;
						}

						Attribute timeStampAttr  = attributes.get("uSNChanged");
						if (timeStampAttr != null) {
							String uSNChangedVal = (String) timeStampAttr.get();
							long currentDeltaSyncTime = Long.parseLong(uSNChangedVal);
							LOG.info("uSNChangedVal = " + uSNChangedVal + "and currentDeltaSyncTime = " + currentDeltaSyncTime);
							if (currentDeltaSyncTime > highestdeltaSyncUserTime) {
								highestdeltaSyncUserTime = currentDeltaSyncTime;
							}
						} else {
							timeStampAttr = attributes.get("modifytimestamp");
							if (timeStampAttr != null) {
								String timeStampVal = (String) timeStampAttr.get();
								Date parseDate = dateFormat.parse(timeStampVal);
								long currentDeltaSyncTime = parseDate.getTime();
								LOG.info("timeStampVal = " + timeStampVal + "and currentDeltaSyncTime = " + currentDeltaSyncTime);
								if (currentDeltaSyncTime > highestdeltaSyncUserTime) {
									highestdeltaSyncUserTime = currentDeltaSyncTime;
									deltaSyncUserTimeStamp = timeStampVal;
								}
							}
						}

						// Get all the groups from the group name attribute of the user only when group search is not enabled.
						if (!groupSearchEnabled) {
							for (String useGroupNameAttribute : userGroupNameAttributeSet) {
								Attribute userGroupfAttribute = userEntry.getAttributes().get(useGroupNameAttribute);
								if (userGroupfAttribute != null) {
									NamingEnumeration<?> groupEnum = userGroupfAttribute.getAll();
									while (groupEnum.hasMore()) {
										String groupDN = (String) groupEnum.next();
										if (LOG.isDebugEnabled()) {
											LOG.debug("Adding " + groupDN + " to " + userName);
										}
										Map<String, String> groupAttrMap = new HashMap<>();
										String groupName = getShortName(groupDN);
										groupAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, groupName);
										groupAttrMap.put(UgsyncCommonConstants.FULL_NAME, groupDN);
										groupAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
										groupAttrMap.put(UgsyncCommonConstants.LDAP_URL, config.getLdapUrl());
										sourceGroups.put(groupDN, groupAttrMap);
										if (LOG.isDebugEnabled()) {
											LOG.debug("As groupsearch is disabled, adding group " + groupName + " from user memberof attribute for user " + userName);
										}
										groupUserTable.put(groupDN, userFullName, userFullName);
									}
								}
							}
						}

						Map<String, String> userAttrMap = new HashMap<>();
						userAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, userName);
						userAttrMap.put(UgsyncCommonConstants.FULL_NAME, userFullName);
						userAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
						userAttrMap.put(UgsyncCommonConstants.LDAP_URL, config.getLdapUrl());
						Attribute userCloudIdAttr = attributes.get(userCloudIdAttribute);
						if (userCloudIdAttr != null) {
							addToAttrMap(userAttrMap, "cloud_id", userCloudIdAttr, config.getUserCloudIdAttributeDataType());
						}
						for (String otherUserAttribute : otherUserAttributes) {
							if (attributes.get(otherUserAttribute) != null) {
								String attrType = config.getOtherUserAttributeDataType(otherUserAttribute);
								addToAttrMap(userAttrMap, otherUserAttribute, attributes.get(otherUserAttribute), attrType);
							}
						}

						sourceUsers.put(userFullName, userAttrMap);
						if ((groupUserTable.containsColumn(userFullName) || groupUserTable.containsColumn(userName))) {
							//Update the username in the groupUserTable with the one from username attribute.
							Map<String, String> userMap = groupUserTable.column(userFullName);
							if (MapUtils.isEmpty(userMap)) {
								userMap = groupUserTable.column(userName);
							}
							for (Map.Entry<String, String> entry : userMap.entrySet()) {
								if (LOG.isDebugEnabled()) {
									LOG.debug("Updating groupUserTable " + entry.getValue() + " with: " + userName + " for " + entry.getKey());
								}
								groupUserTable.put(entry.getKey(), userFullName, userFullName);
							}
						}
						counter++;

                        if (counter <= 2000) {
                            LOG.info("Updating user count: " + counter + ", userName: " + userName);
                            if ( counter == 2000 ) {
                                LOG.info("===> 2000 user records have been synchronized so far. From now on, only a summary progress log will be written for every 100 users. To continue to see detailed log for every user, please enable Trace level logging. <===");
                            }
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Updating user count: " + counter
                                        + ", userName: " + userName);
                            } else  {
                                if ( counter % 100 == 0) {
                                    LOG.info("Synced " + counter + " users till now");
                                }
                            }
                        }

					}

					// Examine the paged results control response
					Control[] controls = ldapContext.getResponseControls();
					if (controls != null) {
						for (int i = 0; i < controls.length; i++) {
							if (controls[i] instanceof PagedResultsResponseControl) {
								PagedResultsResponseControl prrc =
										(PagedResultsResponseControl)controls[i];
								total = prrc.getResultSize();
								if (total != 0) {
									if (LOG.isDebugEnabled()) {
										LOG.debug("END-OF-PAGE total : " + total);
									}
								} else {
									if (LOG.isDebugEnabled()) {
										LOG.debug("END-OF-PAGE total : unknown");
									}
								}
								cookie = prrc.getCookie();
							}
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("No controls were sent from the server");
						}
					}
					// Re-activate paged results
					if (pagedResultsEnabled)   {
						if (LOG.isDebugEnabled()) {
							LOG.debug(String.format("Fetched paged results round: %s", ++paged));
						}
						ldapContext.setRequestControls(new Control[]{
								new PagedResultsControl(pagedResultsSize, cookie, Control.CRITICAL) });
					}
				} while (cookie != null);
				LOG.info("LdapUserGroupBuilder.getUsers() completed with user count: "
						+ counter);
				} catch (Exception t) {
					LOG.error("LdapUserGroupBuilder.getUsers() failed with exception: ", t);
					LOG.info("LdapUserGroupBuilder.getUsers() user count: "
							+ counter);
				}
			}
		} finally {
			if (userSearchResultEnum != null) {
				userSearchResultEnum.close();
			}
			if (groupSearchResultEnum != null) {
				groupSearchResultEnum.close();
			}
			closeLdapContext();
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("highestdeltaSyncUserTime = " + highestdeltaSyncUserTime);
		}
		return highestdeltaSyncUserTime;
	}

	private long getGroups(boolean computeDeletes) throws Throwable {
		NamingEnumeration<SearchResult> groupSearchResultEnum = null;
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        long highestdeltaSyncGroupTime = deltaSyncGroupTime;
		try {
			createLdapContext();
			int total;
			// Activate paged results
			if (pagedResultsEnabled)   {
				ldapContext.setRequestControls(new Control[]{
						new PagedResultsControl(pagedResultsSize, Control.NONCRITICAL) });
			}
			extendedGroupSearchFilter = "(objectclass=" + groupObjectClass + ")";
			if (groupSearchFilter != null && !groupSearchFilter.trim().isEmpty()) {
				String customFilter = groupSearchFilter.trim();
				if (!customFilter.startsWith("(")) {
					customFilter = "(" + customFilter + ")";
				}
				extendedGroupSearchFilter = extendedGroupSearchFilter + customFilter;
			}

			if (!config.isDeltaSyncEnabled() || (computeDeletes)) {
				// Perform full sync when incremental sync is not enabled
				deltaSyncGroupTime = 0;
				deltaSyncGroupTimeStamp = dateFormat.format(new Date(0));
			}

			extendedAllGroupsSearchFilter = "(&"  + extendedGroupSearchFilter + "(|(uSNChanged>=" + deltaSyncGroupTime + ")(modifyTimestamp>=" + deltaSyncGroupTimeStamp + "Z)))";

			LOG.info("extendedAllGroupsSearchFilter = " + extendedAllGroupsSearchFilter);
			for (int ou=0; ou<groupSearchBase.length; ou++) {
				byte[] cookie = null;
				int counter = 0;
				try {
					int paged = 0;
					do {
						groupSearchResultEnum = ldapContext
								.search(groupSearchBase[ou], extendedAllGroupsSearchFilter,
										groupSearchControls);
						while (groupSearchResultEnum.hasMore()) {
							final SearchResult groupEntry = groupSearchResultEnum.next();
							if (groupEntry == null) {
								LOG.info("groupEntry null, skipping sync for the entry");
								continue;
							}
							counter++;
							Attributes attributes =   groupEntry.getAttributes();
							Attribute groupNameAttr = attributes.get(groupNameAttribute);
							if (groupNameAttr == null) {
								LOG.info(groupNameAttribute + " empty for entry " + groupEntry.getNameInNamespace() +
										", skipping sync");
								continue;
							}
							String groupFullName = (groupEntry.getNameInNamespace());
							String gName = (String) groupNameAttr.get();
							Map<String, String> groupAttrMap = new HashMap<>();
                            groupAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, gName);
                            groupAttrMap.put(UgsyncCommonConstants.FULL_NAME, groupFullName);
                            groupAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
                            groupAttrMap.put(UgsyncCommonConstants.LDAP_URL, config.getLdapUrl());
							Attribute groupCloudIdAttr = attributes.get(groupCloudIdAttribute);
							if (groupCloudIdAttr != null) {
								addToAttrMap(groupAttrMap, "cloud_id", groupCloudIdAttr, config.getGroupCloudIdAttributeDataType());
							}
							for (String otherGroupAttribute : otherGroupAttributes) {
								if (attributes.get(otherGroupAttribute) != null) {
									String attrType = config.getOtherGroupAttributeDataType(otherGroupAttribute);
									addToAttrMap(groupAttrMap, otherGroupAttribute, attributes.get(otherGroupAttribute), attrType);
								}
							}
							sourceGroups.put(groupFullName, groupAttrMap);

							Attribute timeStampAttr  = attributes.get("uSNChanged");
							if (timeStampAttr != null) {
								String uSNChangedVal = (String) timeStampAttr.get();
								long currentDeltaSyncTime = Long.parseLong(uSNChangedVal);
								if (currentDeltaSyncTime > highestdeltaSyncGroupTime) {
									highestdeltaSyncGroupTime = currentDeltaSyncTime;
								}
							} else {
								timeStampAttr = attributes.get("modifytimestamp");
								if (timeStampAttr != null) {
									String timeStampVal = (String) timeStampAttr.get();
									Date parseDate = dateFormat.parse(timeStampVal);
									long currentDeltaSyncTime = parseDate.getTime();
									LOG.info("timeStampVal = " + timeStampVal + "and currentDeltaSyncTime = " + currentDeltaSyncTime);
									if (currentDeltaSyncTime > highestdeltaSyncGroupTime) {
										highestdeltaSyncGroupTime = currentDeltaSyncTime;
										deltaSyncGroupTimeStamp = timeStampVal;
									}
								}
							}
							Attribute groupMemberAttr = attributes.get(groupMemberAttributeName);
							int userCount = 0;
							if (groupMemberAttr == null || groupMemberAttr.size() <= 0) {
								LOG.info("No members available for " + gName);
								sourceGroupUsers.put(groupFullName, new HashSet<>());
								continue;
							}

							NamingEnumeration<?> userEnum = groupMemberAttr.getAll();
							while (userEnum.hasMore()) {
								String originalUserFullName = (String) userEnum.next();
								if (originalUserFullName == null || originalUserFullName.trim().isEmpty()) {
									sourceGroupUsers.put(groupFullName, new HashSet<>());
									continue;
								}
								userCount++;

								if (!userSearchEnabled) {
									Map<String, String> userAttrMap = new HashMap<>();
									String userName = getShortName(originalUserFullName);
                                    userAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, userName);
                                    userAttrMap.put(UgsyncCommonConstants.FULL_NAME, originalUserFullName);
                                    userAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
                                    userAttrMap.put(UgsyncCommonConstants.LDAP_URL, config.getLdapUrl());
									sourceUsers.put(originalUserFullName, userAttrMap);
									if (LOG.isDebugEnabled()) {
										LOG.debug("As usersearch is disabled, adding user " + userName + " from group member attribute for group " + gName);
									}
								}

								groupUserTable.put(groupFullName, originalUserFullName, originalUserFullName);
							}

							LOG.info("No. of members in the group " + gName + " = " + userCount);
						}
						// Examine the paged results control response
						Control[] controls = ldapContext.getResponseControls();
						if (controls != null) {
							for (int i = 0; i < controls.length; i++) {
								if (controls[i] instanceof PagedResultsResponseControl) {
									PagedResultsResponseControl prrc =
											(PagedResultsResponseControl)controls[i];
									total = prrc.getResultSize();
									if (total != 0) {
										if (LOG.isDebugEnabled()) {
											LOG.debug("END-OF-PAGE total : " + total);
										}
									} else {
										if (LOG.isDebugEnabled()) {
											LOG.debug("END-OF-PAGE total : unknown");
										}
									}
									cookie = prrc.getCookie();
								}
							}
						} else {
							if (LOG.isDebugEnabled()) {
								LOG.debug("No controls were sent from the server");
							}
						}
						// Re-activate paged results
						if (pagedResultsEnabled)   {
							if (LOG.isDebugEnabled()) {
								LOG.debug(String.format("Fetched paged results round: %s", ++paged));
							}
							ldapContext.setRequestControls(new Control[]{
									new PagedResultsControl(pagedResultsSize, cookie, Control.CRITICAL) });
						}
					} while (cookie != null);
					LOG.info("LdapUserGroupBuilder.getGroups() completed with group count: "
							+ counter);
				} catch (Exception t) {
					LOG.error("LdapUserGroupBuilder.getGroups() failed with exception: " + t);
					LOG.info("LdapUserGroupBuilder.getGroups() group count: "
							+ counter);
				}
			}

		} finally {
			if (groupSearchResultEnum != null) {
				groupSearchResultEnum.close();
			}
			closeLdapContext();
		}

        if (groupHierarchyLevels > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("deltaSyncGroupTime = " + deltaSyncGroupTime);
			}
            if (deltaSyncGroupTime > 0) {
				LOG.info("LdapUserGroupBuilder.getGroups(): Going through group hierarchy for nested group evaluation for deltasync");
				goUpGroupHierarchyLdap(sourceGroups.keySet(), groupHierarchyLevels-1);
            }
        }

        if (LOG.isDebugEnabled()) {
        	LOG.debug("highestdeltaSyncGroupTime = " + highestdeltaSyncGroupTime);
		}

        return highestdeltaSyncGroupTime;
	}

	private void goUpGroupHierarchy(Set<String> groups, int groupHierarchyLevels, String groupSName) throws InvalidNameException {
		if (groupHierarchyLevels <= 0 || groups.isEmpty()) {
			return;
		}
        LOG.info("nextLevelGroups = " + groups + " for group = " + groupSName);
		Set<String> nextLevelGroups;

		for (String group : groups) {

			// Add all members of sub group to the parent groups if the member is not a group in turn
			Set<String> allMembers = groupUserTable.row(groupSName).keySet();
			LOG.info("members of " + groupSName + " = " + allMembers);
			for(String member : allMembers) {
				if (!groupUserTable.containsRow(member)) { //Check if the member of a group is in turn a group
					LOG.info("Adding " + member + " to " + group);
					String userSName = groupUserTable.get(groupSName, member);
					LOG.info("Short name of " + member + " = " + userSName);
					if (userSName != null) {
						groupUserTable.put(group, member, userSName); //Add users from the nested group to parent group
					}
				}
			}
			nextLevelGroups = groupUserTable.column(group).keySet();
			goUpGroupHierarchy(nextLevelGroups, groupHierarchyLevels - 1, group);
		}
	}

	private void goUpGroupHierarchyLdap(Set<String> groupDNs, int groupHierarchyLevels) throws Throwable {
		if (groupHierarchyLevels <= 0 || groupDNs.isEmpty()) {
			return;
		}
		Set<String> nextLevelGroups = new HashSet<String>();

		NamingEnumeration<SearchResult> groupSearchResultEnum = null;
		try {
			createLdapContext();
			int total;
			// Activate paged results
			if (pagedResultsEnabled)   {
				ldapContext.setRequestControls(new Control[]{
						new PagedResultsControl(pagedResultsSize, Control.NONCRITICAL) });
			}
			String groupFilter = "(&(objectclass=" + groupObjectClass + ")";
			if (groupSearchFilter != null && !groupSearchFilter.trim().isEmpty()) {
				String customFilter = groupSearchFilter.trim();
				if (!customFilter.startsWith("(")) {
					customFilter = "(" + customFilter + ")";
				}
				groupFilter += customFilter + "(|";
			}
			StringBuilder filter = new StringBuilder();

			for (String groupDN : groupDNs) {
				filter.append("(").append(groupMemberAttributeName).append("=")
						.append(groupDN).append(")");
			}
			filter.append("))");
			groupFilter += filter;

			LOG.info("extendedAllGroupsSearchFilter = " + groupFilter);
			for (int ou=0; ou<groupSearchBase.length; ou++) {
				byte[] cookie = null;
				int counter = 0;
				try {
					do {
						groupSearchResultEnum = ldapContext
								.search(groupSearchBase[ou], groupFilter,
										groupSearchControls);
						while (groupSearchResultEnum.hasMore()) {
							final SearchResult groupEntry = groupSearchResultEnum.next();
							if (groupEntry == null) {
								LOG.info("groupEntry null, skipping sync for the entry");
								continue;
							}
							counter++;
							Attribute groupNameAttr = groupEntry.getAttributes().get(groupNameAttribute);
							if (groupNameAttr == null) {
								LOG.info(groupNameAttribute + " empty for entry " + groupEntry.getNameInNamespace() +
										", skipping sync");
								continue;
							}
							String groupFullName = (groupEntry.getNameInNamespace());
							nextLevelGroups.add(groupFullName);
							String gName = (String) groupNameAttr.get();

							Attribute groupMemberAttr = groupEntry.getAttributes().get(groupMemberAttributeName);
							int userCount = 0;
							if (groupMemberAttr == null || groupMemberAttr.size() <= 0) {
								LOG.info("No members available for " + gName);
								continue;
							}


							Map<String, String> groupAttrMap = new HashMap<>();
                            groupAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, gName);
                            groupAttrMap.put(UgsyncCommonConstants.FULL_NAME, groupFullName);
                            groupAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
                            groupAttrMap.put(UgsyncCommonConstants.LDAP_URL, config.getLdapUrl());
							for (String otherGroupAttribute : otherGroupAttributes) {
								Attribute otherGroupAttr = groupEntry.getAttributes().get(otherGroupAttribute);
								if (otherGroupAttr != null) {
									groupAttrMap.put(otherGroupAttribute, (String) otherGroupAttr.get());
								}
							}
							sourceGroups.put(groupFullName, groupAttrMap);

							NamingEnumeration<?> userEnum = groupMemberAttr.getAll();
							while (userEnum.hasMore()) {
								String originalUserFullName = (String) userEnum.next();
								if (originalUserFullName == null || originalUserFullName.trim().isEmpty()) {
									continue;
								}
								userCount++;
								if (!userSearchEnabled && !sourceGroups.containsKey(originalUserFullName)) {
									Map<String, String> userAttrMap = new HashMap<>();
									String userName = getShortName(originalUserFullName);
                                    userAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, userName);
                                    userAttrMap.put(UgsyncCommonConstants.FULL_NAME, originalUserFullName);
                                    userAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
                                    userAttrMap.put(UgsyncCommonConstants.LDAP_URL, config.getLdapUrl());
									sourceUsers.put(originalUserFullName, userAttrMap);
								}
								groupUserTable.put(groupFullName, originalUserFullName, originalUserFullName);

							}
							LOG.info("No. of members in the group " + gName + " = " + userCount);
						}
						// Examine the paged results control response
						Control[] controls = ldapContext.getResponseControls();
						if (controls != null) {
							for (int i = 0; i < controls.length; i++) {
								if (controls[i] instanceof PagedResultsResponseControl) {
									PagedResultsResponseControl prrc =
											(PagedResultsResponseControl)controls[i];
									total = prrc.getResultSize();
									if (total != 0) {
										if (LOG.isDebugEnabled()) {
											LOG.debug("END-OF-PAGE total : " + total);
										}
									} else {
										if (LOG.isDebugEnabled()) {
											LOG.debug("END-OF-PAGE total : unknown");
										}
									}
									cookie = prrc.getCookie();
								}
							}
						} else {
							if (LOG.isDebugEnabled()) {
								LOG.debug("No controls were sent from the server");
							}
						}
						// Re-activate paged results
						if (pagedResultsEnabled)   {
							ldapContext.setRequestControls(new Control[]{
									new PagedResultsControl(pagedResultsSize, cookie, Control.CRITICAL) });
						}
					} while (cookie != null);
					LOG.info("LdapUserGroupBuilder.goUpGroupHierarchyLdap() completed with group count: "
							+ counter);
				} catch (RuntimeException re) {
					LOG.error("LdapUserGroupBuilder.goUpGroupHierarchyLdap() failed with runtime exception: ", re);
					throw re;
				} catch (Exception t) {
					LOG.error("LdapUserGroupBuilder.goUpGroupHierarchyLdap() failed with exception: ", t);
					LOG.info("LdapUserGroupBuilder.goUpGroupHierarchyLdap() group count: "
							+ counter);
				}
			}

		} catch (RuntimeException re) {
			LOG.error("LdapUserGroupBuilder.goUpGroupHierarchyLdap() failed with exception: ", re);
			throw re;
		} finally {
			if (groupSearchResultEnum != null) {
				groupSearchResultEnum.close();
			}
			closeLdapContext();
		}
		goUpGroupHierarchyLdap(nextLevelGroups, groupHierarchyLevels-1);
	}

	private void addToAttrMap(Map<String, String> userAttrMap, String attrName, Attribute attr, String attrType) throws Throwable{
		if (attrType.equals(DATA_TYPE_BYTEARRAY)) {
			try {
				byte[] otherUserAttrBytes = (byte[]) attr.get();
				//Convert objectGUID into string and add to userAttrMap
				String attrVal = UUID.nameUUIDFromBytes(otherUserAttrBytes).toString();
				userAttrMap.put(attrName, attrVal);
			} catch (ClassCastException e) {
				LOG.error(attrName + " type is not set properly " + e.getMessage());
			}
		} else if (attrType.equals("String")) {
			userAttrMap.put(attrName, (String) attr.get());
		} else {
			// This should not be reached.
			LOG.warn("Attribute Type " + attrType + " not supported for " + attrName);
		}
	}

	private static String getShortName(String longName) {
		if (StringUtils.isEmpty(longName)) {
			return null;
		}
		String shortName = "";
		try {
			LdapName subjectDN = new LdapName(longName);
			List<Rdn> rdns = subjectDN.getRdns();
			for (int i = rdns.size() - 1; i >= 0; i--) {
				if (StringUtils.isNotEmpty(shortName)) {
					break;
				}
				Rdn rdn = rdns.get(i);
				Attributes attributes = rdn.toAttributes();
				try {
					Attribute uid = attributes.get("uid");
					if (uid != null) {
						Object value = uid.get();
						if (value != null) {
							shortName = value.toString();
						}
					} else {
						Attribute cn = attributes.get("cn");
						if (cn != null) {
							Object value = cn.get();
							if (value != null) {
								shortName = value.toString();
							}
						}
					}
				} catch (NoSuchElementException ignore) {
					shortName = longName;
				} catch (NamingException ignore) {
					shortName = longName;
				}
			}
		} catch (InvalidNameException ex) {
			shortName = longName;
		}
		LOG.info("longName: " + longName + ", userName: " + shortName);
		return shortName;
	}

	private String getFirstRDN(String name) {
		if (StringUtils.isEmpty(name)) {
			return null;
		}
		String shortName = "";
		try {
			LdapName subjectDN = new LdapName(name);
			List<Rdn> rdns = subjectDN.getRdns();
			for (int i = rdns.size() - 1; i >= 0; i--) {
				if (StringUtils.isNotEmpty(shortName)) {
					break;
				}
				Rdn rdn = rdns.get(i);
				Attributes attributes = rdn.toAttributes();
				try {
					Attribute cn = attributes.get("cn");
					if (cn != null) {
						Object value = cn.get();
						if (value != null) {
							shortName = GROUP_NAME_ATTRIBUTE + value.toString();
						}
					}
				} catch (NoSuchElementException ignore) {
					LOG.warn("NoSuchElementException while retrieving first RDN for " + name);
				} catch (NamingException ignore) {
					LOG.warn("NamingException while retrieving first RDN for " + name);
				}
			}
		} catch (InvalidNameException ex) {
			LOG.warn("InvalidNameException while retrieving first RDN for " + name);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Input group name: " + name + ", first RDN: " + shortName);
		}
		return shortName;
	}

	private String getDNForMemberOf(String searchFilter) throws Throwable {
		NamingEnumeration<SearchResult> userSearchResultEnum = null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("getDNForMemberOf(" + searchFilter + ")");
		}
		String computedSearchFilter = "";
		try {
			createLdapContext();
			int total;
			// Activate paged results
			if (pagedResultsEnabled)   {
				ldapContext.setRequestControls(new Control[]{
						new PagedResultsControl(pagedResultsSize, Control.NONCRITICAL) });
			}
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(groupSearchScope);

			Set<String> searchAttributes = new HashSet<String>();
			searchAttributes.add(groupNameAttribute);
			searchControls.setReturningAttributes(searchAttributes.toArray(
					new String[searchAttributes.size()]));


			// When multiple OUs are configured, go through each OU as the user search base to search for users.
			for (int ou=0; ou<groupSearchBase.length; ou++) {
				byte[] cookie = null;
				int counter = 0;
				try {
					int paged = 0;
					do {
						userSearchResultEnum = ldapContext
								.search(groupSearchBase[ou], "(&(objectclass=" + groupObjectClass + ")(" + searchFilter + "))",
										searchControls);

						while (userSearchResultEnum.hasMore()) {
							// searchResults contains all the user entries
							final SearchResult userEntry = userSearchResultEnum.next();

							if (userEntry == null)  {
								LOG.info("userEntry null, skipping sync for the entry");
								continue;
							}

							Attributes attributes =   userEntry.getAttributes();
							if (attributes == null)  {
								LOG.info("attributes  missing for entry " + userEntry.getNameInNamespace() +
											", skipping sync");
								continue;
							}

							Attribute groupNameAttr  = attributes.get(groupNameAttribute);
							if (groupNameAttr == null)  {
								LOG.info(groupNameAttribute + " missing for entry " + userEntry.getNameInNamespace() +
											", skipping sync");
								continue;
							}

							String groupFullName = (userEntry.getNameInNamespace());
							LOG.info("groupFullName = " + groupFullName);
							computedSearchFilter += "(" + MEMBER_OF_ATTR + groupFullName + ")";
							counter++;

							if (counter <= 2000) {
								LOG.info("Updating group count: " + counter
											+ ", groupName: " + groupFullName);
								if ( counter == 2000 ) {
									LOG.info("===> 2000 group records have been synchronized so far. From now on, only a summary progress log will be written for every 100 users. To continue to see detailed log for every user, please enable Trace level logging. <===");
								}
							} else {
								if (LOG.isTraceEnabled()) {
									LOG.trace("Updating group count: " + counter
											+ ", groupName: " + groupFullName);
								} else  {
									if ( counter % 100 == 0) {
										LOG.info("Synced " + counter + " groups till now");
									}
								}
							}

						}

						// Examine the paged results control response
						Control[] controls = ldapContext.getResponseControls();
						if (controls != null) {
							for (int i = 0; i < controls.length; i++) {
								if (controls[i] instanceof PagedResultsResponseControl) {
									PagedResultsResponseControl prrc =
											(PagedResultsResponseControl)controls[i];
									total = prrc.getResultSize();
									if (total != 0) {
										if (LOG.isDebugEnabled()) {
											LOG.debug("END-OF-PAGE total : " + total);
										}
									} else {
										if (LOG.isDebugEnabled()) {
											LOG.debug("END-OF-PAGE total : unknown");
										}
									}
									cookie = prrc.getCookie();
								}
							}
						} else {
							if (LOG.isDebugEnabled()) {
								LOG.debug("No controls were sent from the server");
							}
						}
						// Re-activate paged results
						if (pagedResultsEnabled)   {
							if (LOG.isDebugEnabled()) {
								LOG.debug(String.format("Fetched paged results round: %s", ++paged));
							}
							ldapContext.setRequestControls(new Control[]{
									new PagedResultsControl(pagedResultsSize, cookie, Control.CRITICAL) });
						}
					} while (cookie != null);
					LOG.info("LdapUserGroupBuilder.getDNForMemberOf() completed with group count: "
							+ counter);
				} catch (Exception t) {
					LOG.error("LdapUserGroupBuilder.getDNForMemberOf() failed with exception: ", t);
					LOG.info("LdapUserGroupBuilder.getDNForMemberOf() group count: "
							+ counter);
				}
			}
		} finally {
			if (userSearchResultEnum != null) {
				userSearchResultEnum.close();
			}
			closeLdapContext();
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("computedSearchFilter = " + computedSearchFilter);
		}
		return computedSearchFilter;
	}
}
