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

package org.apache.ranger.plugin.model;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerPluginInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final int ENTITY_TYPE_POLICIES 	= 0;
	public static final int ENTITY_TYPE_TAGS     	= 1;
	public static final int ENTITY_TYPE_ROLES	 	= 2;
	public static final int ENTITY_TYPE_USERSTORE	= 3;

	public static final String PLUGIN_INFO_POLICY_DOWNLOAD_TIME      = "policyDownloadTime";
	public static final String PLUGIN_INFO_POLICY_DOWNLOADED_VERSION = "policyDownloadedVersion";
	public static final String PLUGIN_INFO_POLICY_ACTIVATION_TIME    = "policyActivationTime";
	public static final String PLUGIN_INFO_POLICY_ACTIVE_VERSION     = "policyActiveVersion";
	public static final String PLUGIN_INFO_TAG_DOWNLOAD_TIME         = "tagDownloadTime";
	public static final String PLUGIN_INFO_TAG_DOWNLOADED_VERSION    = "tagDownloadedVersion";
	public static final String PLUGIN_INFO_TAG_ACTIVATION_TIME       = "tagActivationTime";
	public static final String PLUGIN_INFO_TAG_ACTIVE_VERSION        = "tagActiveVersion";

	public static final String PLUGIN_INFO_ROLE_DOWNLOAD_TIME         = "roleDownloadTime";
	public static final String PLUGIN_INFO_ROLE_DOWNLOADED_VERSION    = "roleDownloadedVersion";
	public static final String PLUGIN_INFO_ROLE_ACTIVATION_TIME       = "roleActivationTime";
	public static final String PLUGIN_INFO_ROLE_ACTIVE_VERSION        = "roleActiveVersion";

	public static final String PLUGIN_INFO_USERSTORE_DOWNLOAD_TIME         = "userstoreDownloadTime";
	public static final String PLUGIN_INFO_USERSTORE_DOWNLOADED_VERSION    = "userstoreDownloadedVersion";
	public static final String PLUGIN_INFO_USERSTORE_ACTIVATION_TIME       = "userstoreActivationTime";
	public static final String PLUGIN_INFO_USERSTORE_ACTIVE_VERSION        = "userstoreActiveVersion";

	public static final String RANGER_ADMIN_LAST_POLICY_UPDATE_TIME  = "lastPolicyUpdateTime";
	public static final String RANGER_ADMIN_LATEST_POLICY_VERSION    = "latestPolicyVersion";
	public static final String RANGER_ADMIN_LAST_TAG_UPDATE_TIME     = "lastTagUpdateTime";
	public static final String RANGER_ADMIN_LATEST_TAG_VERSION       = "latestTagVersion";

	public static final String RANGER_ADMIN_CAPABILITIES             = "adminCapabilities";
	public static final String PLUGIN_INFO_CAPABILITIES              = "pluginCapabilities";

	private Long    id;
	private Date    createTime;
	private Date    updateTime;

	private String serviceName;
	private String serviceDisplayName;
	private String serviceType;
	private String serviceTypeDisplayName;
	private String hostName;
	private String appType;
	private String ipAddress;
	private Map<String, String> info;

	//FIXME UNUSED
	public RangerPluginInfo(Long id, Date createTime, Date updateTime, String serviceName, String appType, String hostName, String ipAddress, Map<String, String> info) {
		super();

		setId(id);
		setCreateTime(createTime);
		setUpdateTime(updateTime);
		setServiceName(serviceName);
		setAppType(appType);
		setHostName(hostName);
		setIpAddress(ipAddress);
		setInfo(info);
	}

	//FIXME UNUSED
	public RangerPluginInfo() {
		this(null, null, null, null, null, null, null, null);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getServiceTypeDisplayName() {
		return serviceTypeDisplayName;
	}

	public void setServiceTypeDisplayName(String serviceTypeDisplayName) {
		this.serviceTypeDisplayName = serviceTypeDisplayName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceDisplayName() {
		return serviceDisplayName;
	}

	public void setServiceDisplayName(String serviceDisplayName) {
		this.serviceDisplayName = serviceDisplayName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getAppType() {
		return appType;
	}

	public void setAppType(String appType) {
		this.appType = appType;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public Map<String, String> getInfo() {
		return info;
	}

	public void setInfo(Map<String, String> info) {
		this.info = info == null ? new HashMap<String, String>() : info;
	}

	@JsonIgnore
	public void setPolicyDownloadTime(Long policyDownloadTime) {
		getInfo().put(PLUGIN_INFO_POLICY_DOWNLOAD_TIME, policyDownloadTime == null ? null : Long.toString(policyDownloadTime));
	}

	@JsonIgnore
	public Long getPolicyDownloadTime() {
		String downloadTimeString = getInfo().get(PLUGIN_INFO_POLICY_DOWNLOAD_TIME);
		return StringUtils.isNotBlank(downloadTimeString) ? Long.valueOf(downloadTimeString) : null;
	}

	@JsonIgnore
	public void setPolicyDownloadedVersion(Long policyDownloadedVersion) {
		getInfo().put(PLUGIN_INFO_POLICY_DOWNLOADED_VERSION, policyDownloadedVersion == null ? null : Long.toString(policyDownloadedVersion));
	}

	@JsonIgnore
	public Long getPolicyDownloadedVersion() {
		String downloadedVersionString = getInfo().get(PLUGIN_INFO_POLICY_DOWNLOADED_VERSION);
		return StringUtils.isNotBlank(downloadedVersionString) ? Long.valueOf(downloadedVersionString) : null;
	}

	@JsonIgnore
	public void setPolicyActivationTime(Long policyActivationTime) {
		getInfo().put(PLUGIN_INFO_POLICY_ACTIVATION_TIME, policyActivationTime == null ? null : Long.toString(policyActivationTime));
	}

	@JsonIgnore
	public Long getPolicyActivationTime() {
		String activationTimeString = getInfo().get(PLUGIN_INFO_POLICY_ACTIVATION_TIME);
		return StringUtils.isNotBlank(activationTimeString) ? Long.valueOf(activationTimeString) : null;
	}

	@JsonIgnore
	public void setPolicyActiveVersion(Long policyActiveVersion) {
		getInfo().put(PLUGIN_INFO_POLICY_ACTIVE_VERSION, policyActiveVersion == null ? null : Long.toString(policyActiveVersion));
	}

	@JsonIgnore
	public Long getPolicyActiveVersion() {
		String activeVersionString = getInfo().get(PLUGIN_INFO_POLICY_ACTIVE_VERSION);
		return StringUtils.isNotBlank(activeVersionString) ? Long.valueOf(activeVersionString) : null;
	}

	@JsonIgnore
	public void setTagDownloadTime(Long tagDownloadTime) {
		getInfo().put(PLUGIN_INFO_TAG_DOWNLOAD_TIME, tagDownloadTime == null ? null : Long.toString(tagDownloadTime));
	}

	@JsonIgnore
	public Long getTagDownloadTime() {
		String downloadTimeString = getInfo().get(PLUGIN_INFO_TAG_DOWNLOAD_TIME);
		return StringUtils.isNotBlank(downloadTimeString) ? Long.valueOf(downloadTimeString) : null;
	}

	@JsonIgnore
	public void setTagDownloadedVersion(Long tagDownloadedVersion) {
		getInfo().put(PLUGIN_INFO_TAG_DOWNLOADED_VERSION, tagDownloadedVersion == null ? null : Long.toString(tagDownloadedVersion));
	}

	@JsonIgnore
	public Long getTagDownloadedVersion() {
		String downloadedVersion = getInfo().get(PLUGIN_INFO_TAG_DOWNLOADED_VERSION);
		return StringUtils.isNotBlank(downloadedVersion) ? Long.valueOf(downloadedVersion) : null;
	}

	@JsonIgnore
	public void setTagActivationTime(Long tagActivationTime) {
		getInfo().put(PLUGIN_INFO_TAG_ACTIVATION_TIME, tagActivationTime == null ? null : Long.toString(tagActivationTime));
	}

	@JsonIgnore
	public Long getTagActivationTime() {
		String activationTimeString = getInfo().get(PLUGIN_INFO_TAG_ACTIVATION_TIME);
		return StringUtils.isNotBlank(activationTimeString) ? Long.valueOf(activationTimeString) : null;
	}

	@JsonIgnore
	public void setTagActiveVersion(Long tagActiveVersion) {
		getInfo().put(PLUGIN_INFO_TAG_ACTIVE_VERSION, tagActiveVersion == null ? null : Long.toString(tagActiveVersion));
	}

	@JsonIgnore
	public Long getTagActiveVersion() {
		String activeVersionString = getInfo().get(PLUGIN_INFO_TAG_ACTIVE_VERSION);
		return StringUtils.isNotBlank(activeVersionString) ? Long.valueOf(activeVersionString) : null;
	}

	@JsonIgnore
	public Long getLatestPolicyVersion() {
		String latestPolicyVersionString = getInfo().get(RANGER_ADMIN_LATEST_POLICY_VERSION);
		return StringUtils.isNotBlank(latestPolicyVersionString) ? Long.valueOf(latestPolicyVersionString) : null;
	}

	@JsonIgnore
	public Long getLastPolicyUpdateTime() {
		String updateTimeString = getInfo().get(RANGER_ADMIN_LAST_POLICY_UPDATE_TIME);
		return StringUtils.isNotBlank(updateTimeString) ? Long.valueOf(updateTimeString) : null;
	}

	@JsonIgnore
	public Long getLatestTagVersion() {
		String latestTagVersionString = getInfo().get(RANGER_ADMIN_LATEST_TAG_VERSION);
		return StringUtils.isNotBlank(latestTagVersionString) ? Long.valueOf(latestTagVersionString) : null;
	}

	@JsonIgnore
	public Long getLastTagUpdateTime() {
		String updateTimeString = getInfo().get(RANGER_ADMIN_LAST_TAG_UPDATE_TIME);
		return StringUtils.isNotBlank(updateTimeString) ? Long.valueOf(updateTimeString) : null;
	}

	@JsonIgnore
	public void setRoleDownloadTime(Long roleDownloadTime) {
		getInfo().put(PLUGIN_INFO_ROLE_DOWNLOAD_TIME, roleDownloadTime == null ? null : Long.toString(roleDownloadTime));
	}

	@JsonIgnore
	public Long getRoleDownloadTime() {
		String downloadTimeString = getInfo().get(PLUGIN_INFO_ROLE_DOWNLOAD_TIME);
		return StringUtils.isNotBlank(downloadTimeString) ? Long.valueOf(downloadTimeString) : null;
	}

	@JsonIgnore
	public void setRoleDownloadedVersion(Long roleDownloadedVersion) {
		getInfo().put(PLUGIN_INFO_ROLE_DOWNLOADED_VERSION, roleDownloadedVersion == null ? null : Long.toString(roleDownloadedVersion));
	}

	@JsonIgnore
	public Long getRoleDownloadedVersion() {
		String downloadedVersionString = getInfo().get(PLUGIN_INFO_ROLE_DOWNLOADED_VERSION);
		return StringUtils.isNotBlank(downloadedVersionString) ? Long.valueOf(downloadedVersionString) : null;
	}

	@JsonIgnore
	public void setRoleActivationTime(Long roleActivationTime) {
		getInfo().put(PLUGIN_INFO_ROLE_ACTIVATION_TIME, roleActivationTime == null ? null : Long.toString(roleActivationTime));
	}

	@JsonIgnore
	public Long getRoleActivationTime() {
		String activationTimeString = getInfo().get(PLUGIN_INFO_ROLE_ACTIVATION_TIME);
		return StringUtils.isNotBlank(activationTimeString) ? Long.valueOf(activationTimeString) : null;
	}

	@JsonIgnore
	public void setRoleActiveVersion(Long roleActiveVersion) {
		getInfo().put(PLUGIN_INFO_ROLE_ACTIVE_VERSION, roleActiveVersion == null ? null : Long.toString(roleActiveVersion));
	}

	@JsonIgnore
	public Long getRoleActiveVersion() {
		String activeVersionString = getInfo().get(PLUGIN_INFO_POLICY_ACTIVE_VERSION);
		return StringUtils.isNotBlank(activeVersionString) ? Long.valueOf(activeVersionString) : null;
	}

	@JsonIgnore
	public void setUserStoreDownloadTime(Long userstoreDownloadTime) {
		getInfo().put(PLUGIN_INFO_USERSTORE_DOWNLOAD_TIME, userstoreDownloadTime == null ? null : Long.toString(userstoreDownloadTime));
	}

	@JsonIgnore
	public Long getUserStoreDownloadTime() {
		String downloadTimeString = getInfo().get(PLUGIN_INFO_USERSTORE_DOWNLOAD_TIME);
		return StringUtils.isNotBlank(downloadTimeString) ? Long.valueOf(downloadTimeString) : null;
	}

	@JsonIgnore
	public void setUserStoreDownloadedVersion(Long userstoreDownloadedVersion) {
		getInfo().put(PLUGIN_INFO_USERSTORE_DOWNLOADED_VERSION, userstoreDownloadedVersion == null ? null : Long.toString(userstoreDownloadedVersion));
	}

	@JsonIgnore
	public Long getUserStoreDownloadedVersion() {
		String downloadedVersionString = getInfo().get(PLUGIN_INFO_USERSTORE_DOWNLOADED_VERSION);
		return StringUtils.isNotBlank(downloadedVersionString) ? Long.valueOf(downloadedVersionString) : null;
	}

	@JsonIgnore
	public void setUserStoreActivationTime(Long userstoreActivationTime) {
		getInfo().put(PLUGIN_INFO_USERSTORE_ACTIVATION_TIME, userstoreActivationTime == null ? null : Long.toString(userstoreActivationTime));
	}

	@JsonIgnore
	public Long getUserStoreActivationTime() {
		String activationTimeString = getInfo().get(PLUGIN_INFO_USERSTORE_ACTIVATION_TIME);
		return StringUtils.isNotBlank(activationTimeString) ? Long.valueOf(activationTimeString) : null;
	}

	@JsonIgnore
	public void setUserStoreActiveVersion(Long userstoreActiveVersion) {
		getInfo().put(PLUGIN_INFO_USERSTORE_ACTIVE_VERSION, userstoreActiveVersion == null ? null : Long.toString(userstoreActiveVersion));
	}

	@JsonIgnore
	public Long getUserStoreActiveVersion() {
		String activeVersionString = getInfo().get(PLUGIN_INFO_USERSTORE_ACTIVE_VERSION);
		return StringUtils.isNotBlank(activeVersionString) ? Long.valueOf(activeVersionString) : null;
	}

	@JsonIgnore
	public void setPluginCapabilities(String capabilities) {
		setCapabilities(PLUGIN_INFO_CAPABILITIES, capabilities);
	}

	@JsonIgnore
	public String getPluginCapabilities() {
		return getCapabilities(PLUGIN_INFO_CAPABILITIES);
	}

	@JsonIgnore
	public void setAdminCapabilities(String capabilities) {
		setCapabilities(RANGER_ADMIN_CAPABILITIES, capabilities);
	}

	@JsonIgnore
	public String getAdminCapabilities() {
		return getCapabilities(RANGER_ADMIN_CAPABILITIES);
	}

	@JsonIgnore
	private void setCapabilities(String optionName, String capabilities) {
		getInfo().put(optionName, capabilities == null ? null : capabilities);
	}

	@JsonIgnore
	private String getCapabilities(String optionName) {
		String capabilitiesString = getInfo().get(optionName);
		return StringUtils.isNotBlank(capabilitiesString) ? capabilitiesString : null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerPluginInfo={");

		sb.append("id={").append(id).append("} ");
		sb.append("createTime={").append(createTime).append("} ");
		sb.append("updateTime={").append(updateTime).append("} ");
		sb.append("serviceName={").append(serviceName).append("} ");
		sb.append("serviceType={").append(serviceType).append("} ");
		sb.append("serviceTypeDisplayName{").append(serviceTypeDisplayName).append("} ");
		sb.append("serviceDisplayName={").append(serviceDisplayName).append("} ");
		sb.append("hostName={").append(hostName).append("} ");
		sb.append("appType={").append(appType).append("} ");
		sb.append("ipAddress={").append(ipAddress).append("} ");
		sb.append("info={").append(info).append("} ");

		sb.append(" }");

		return sb;
	}
}

