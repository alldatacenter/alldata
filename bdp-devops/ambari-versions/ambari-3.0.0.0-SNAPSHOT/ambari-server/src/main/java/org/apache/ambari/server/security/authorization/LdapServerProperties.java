/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Describes LDAP Server connection parameters
 */
public class LdapServerProperties {

  private String primaryUrl;
  private String secondaryUrl;
  private boolean useSsl;
  private boolean anonymousBind;
  private String managerDn;
  private String managerPassword;
  private String baseDN;
  private String dnAttribute;
  private String referralMethod;

  //LDAP group properties
  private String groupBase;
  private String groupObjectClass;
  private String groupMembershipAttr;
  private String groupNamingAttr;
  private String adminGroupMappingRules;
  private boolean groupMappingEnabled;

  //LDAP user properties
  private String userBase;
  private String userObjectClass;
  private String usernameAttribute;
  private boolean forceUsernameToLowercase = false;
  private String userSearchBase = "";

  private String syncGroupMemberReplacePattern = "";
  private String syncUserMemberReplacePattern = "";

  private String groupSearchFilter;
  private String userSearchFilter;
  private boolean alternateUserSearchFilterEnabled;
  private String alternateUserSearchFilter; // alternate user search filter to be used when users use their alternate login id (e.g. User Principal Name)

  private String syncUserMemberFilter = "";
  private String syncGroupMemberFilter = "";
  private boolean disableEndpointIdentification = false;
  //LDAP pagination properties
  private boolean paginationEnabled = true;
  private String adminGroupMappingMemberAttr = ""; // custom group search filter for admin mappings

  public List<String> getLdapUrls() {
    String protocol = useSsl ? "ldaps://" : "ldap://";

    if (StringUtils.isEmpty(primaryUrl)) {
      return Collections.emptyList();
    } else {
      List<String> list = new ArrayList<>();
      list.add(protocol + primaryUrl);
      if (!StringUtils.isEmpty(secondaryUrl)) {
        list.add(protocol + secondaryUrl);
      }
      return list;
    }
  }

  public String getPrimaryUrl() {
    return primaryUrl;
  }

  public void setPrimaryUrl(String primaryUrl) {
    this.primaryUrl = primaryUrl;
  }

  public String getSecondaryUrl() {
    return secondaryUrl;
  }

  public void setSecondaryUrl(String secondaryUrl) {
    this.secondaryUrl = secondaryUrl;
  }

  public boolean isUseSsl() {
    return useSsl;
  }

  public void setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
  }

  public boolean isAnonymousBind() {
    return anonymousBind;
  }

  public void setAnonymousBind(boolean anonymousBind) {
    this.anonymousBind = anonymousBind;
  }

  public String getManagerDn() {
    return managerDn;
  }

  public void setManagerDn(String managerDn) {
    this.managerDn = managerDn;
  }

  public String getManagerPassword() {
    return managerPassword;
  }

  public void setManagerPassword(String managerPassword) {
    this.managerPassword = managerPassword;
  }

  public String getBaseDN() {
    return baseDN;
  }

  public void setBaseDN(String baseDN) {
    this.baseDN = baseDN;
  }

  public String getUserSearchBase() {
    return userSearchBase;
  }

  public void setUserSearchBase(String userSearchBase) {
    this.userSearchBase = userSearchBase;
  }

  /**
   * Returns the LDAP filter to search users by.
   * @param useAlternateUserSearchFilter if true than return LDAP filter that expects user name in
   *                                  User Principal Name format to filter users constructed from {@link org.apache.ambari.server.configuration.Configuration#LDAP_ALT_USER_SEARCH_FILTER}.
   *                                  Otherwise the filter is constructed from {@link org.apache.ambari.server.configuration.Configuration#LDAP_USER_SEARCH_FILTER}
   * @return the LDAP filter string
   */
  public String getUserSearchFilter(boolean useAlternateUserSearchFilter) {
    String filter = useAlternateUserSearchFilter ? alternateUserSearchFilter : userSearchFilter;

    return resolveUserSearchFilterPlaceHolders(filter);
  }

  public String getUsernameAttribute() {
    return usernameAttribute;
  }

  public void setUsernameAttribute(String usernameAttribute) {
    this.usernameAttribute = usernameAttribute;
  }

  /**
   * Sets whether the username retrieved from the LDAP server during authentication is to be forced
   * to all lowercase characters before assigning to the authenticated user.
   *
   * @param forceUsernameToLowercase true to force the username to be lowercase; false to leave as
   *                                 it was when retrieved from the LDAP server
   */
  public void setForceUsernameToLowercase(boolean forceUsernameToLowercase) {
    this.forceUsernameToLowercase = forceUsernameToLowercase;
  }

  /**
   * Gets whether the username retrieved from the LDAP server during authentication is to be forced
   * to all lowercase characters before assigning to the authenticated user.
   *
   * @return true to force the username to be lowercase; false to leave as it was when retrieved from
   * the LDAP server
   */
  public boolean isForceUsernameToLowercase() {
    return forceUsernameToLowercase;
  }

  public String getGroupBase() {
    return groupBase;
  }

  public void setGroupBase(String groupBase) {
    this.groupBase = groupBase;
  }

  public String getGroupObjectClass() {
    return groupObjectClass;
  }

  public void setGroupObjectClass(String groupObjectClass) {
    this.groupObjectClass = groupObjectClass;
  }

  public String getGroupMembershipAttr() {
    return groupMembershipAttr;
  }

  public void setGroupMembershipAttr(String groupMembershipAttr) {
    this.groupMembershipAttr = groupMembershipAttr;
  }

  public String getGroupNamingAttr() {
    return groupNamingAttr;
  }

  public void setGroupNamingAttr(String groupNamingAttr) {
    this.groupNamingAttr = groupNamingAttr;
  }

  public String getAdminGroupMappingRules() {
    return adminGroupMappingRules;
  }

  public void setAdminGroupMappingRules(String adminGroupMappingRules) {
    this.adminGroupMappingRules = adminGroupMappingRules;
  }

  public String getGroupSearchFilter() {
    return groupSearchFilter;
  }

  public void setGroupSearchFilter(String groupSearchFilter) {
    this.groupSearchFilter = groupSearchFilter;
  }


  public void setUserSearchFilter(String userSearchFilter) {
    this.userSearchFilter = userSearchFilter;
  }

  public void setAlternateUserSearchFilterEnabled(boolean alternateUserSearchFilterEnabled) {
    this.alternateUserSearchFilterEnabled = alternateUserSearchFilterEnabled;
  }

  public boolean isAlternateUserSearchFilterEnabled() {
    return alternateUserSearchFilterEnabled;
  }

  public void setAlternateUserSearchFilter(String alternateUserSearchFilter) {
    this.alternateUserSearchFilter = alternateUserSearchFilter;
  }

  public boolean isGroupMappingEnabled() {
    return groupMappingEnabled;
  }

  public void setGroupMappingEnabled(boolean groupMappingEnabled) {
    this.groupMappingEnabled = groupMappingEnabled;
  }

  public void setUserBase(String userBase) {
    this.userBase = userBase;
  }

  public void setUserObjectClass(String userObjectClass) {
    this.userObjectClass = userObjectClass;
  }

  public String getUserBase() {
    return userBase;
  }

  public String getUserObjectClass() {
    return userObjectClass;
  }

  public String getDnAttribute() {
    return dnAttribute;
  }

  public void setDnAttribute(String dnAttribute) {
    this.dnAttribute = dnAttribute;
  }

  public void setReferralMethod(String referralMethod) {
    this.referralMethod = referralMethod;
  }

  public String getReferralMethod() {
    return referralMethod;
  }

  public boolean isDisableEndpointIdentification() {
    return disableEndpointIdentification;
  }

  public void setDisableEndpointIdentification(boolean disableEndpointIdentification) {
    this.disableEndpointIdentification = disableEndpointIdentification;
  }

  public boolean isPaginationEnabled() {
    return paginationEnabled;
  }

  public void setPaginationEnabled(boolean paginationEnabled) {
    this.paginationEnabled = paginationEnabled;
  }

  public String getSyncGroupMemberReplacePattern() {
    return syncGroupMemberReplacePattern;
  }

  public void setSyncGroupMemberReplacePattern(String syncGroupMemberReplacePattern) {
    this.syncGroupMemberReplacePattern = syncGroupMemberReplacePattern;
  }

  public String getSyncUserMemberReplacePattern() {
    return syncUserMemberReplacePattern;
  }

  public void setSyncUserMemberReplacePattern(String syncUserMemberReplacePattern) {
    this.syncUserMemberReplacePattern = syncUserMemberReplacePattern;
  }

  public String getSyncUserMemberFilter() {
    return syncUserMemberFilter;
  }

  public void setSyncUserMemberFilter(String syncUserMemberFilter) {
    this.syncUserMemberFilter = syncUserMemberFilter;
  }

  public String getSyncGroupMemberFilter() {
    return syncGroupMemberFilter;
  }

  public void setSyncGroupMemberFilter(String syncGroupMemberFilter) {
    this.syncGroupMemberFilter = syncGroupMemberFilter;
  }

  public String getAdminGroupMappingMemberAttr() {
    return adminGroupMappingMemberAttr;
  }

  public void setAdminGroupMappingMemberAttr(String adminGroupMappingMemberAttr) {
    this.adminGroupMappingMemberAttr = adminGroupMappingMemberAttr;
  }

  @Override
  public final boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj, false);
  }

  @Override
  public final int hashCode() {
    return HashCodeBuilder.reflectionHashCode(1, 31, this);
  }

  /**
   * Resolves known placeholders found within the given ldap user search ldap filter
   * @param filter
   * @return returns the filter with the resolved placeholders.
   */
  protected String resolveUserSearchFilterPlaceHolders(String filter) {
    return filter
      .replace("{usernameAttribute}", usernameAttribute)
      .replace("{userObjectClass}", userObjectClass);
  }
}
