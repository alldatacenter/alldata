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

package org.apache.ranger.plugin.util;


import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineImpl;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServicePolicies implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(ServicePolicies.class);

	private String             serviceName;
	private Long               serviceId;
	private Long               policyVersion;
	private Date               policyUpdateTime;
	private List<RangerPolicy> policies;
	private RangerServiceDef   serviceDef;
	private String             auditMode = RangerPolicyEngine.AUDIT_DEFAULT;
	private TagPolicies        tagPolicies;
	private Map<String, SecurityZoneInfo> securityZones;
	private List<RangerPolicyDelta> policyDeltas;
	private Map<String, String> serviceConfig;

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}
	/**
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	/**
	 * @return the serviceId
	 */
	public Long getServiceId() {
		return serviceId;
	}
	/**
	 * @param serviceId the serviceId to set
	 */
	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}
	/**
	 * @return the policyVersion
	 */
	public Long getPolicyVersion() {
		return policyVersion;
	}
	/**
	 * @param policyVersion the policyVersion to set
	 */
	public void setPolicyVersion(Long policyVersion) {
		this.policyVersion = policyVersion;
	}
	/**
	 * @return the policyUpdateTime
	 */
	public Date getPolicyUpdateTime() {
		return policyUpdateTime;
	}
	/**
	 * @param policyUpdateTime the policyUpdateTime to set
	 */
	public void setPolicyUpdateTime(Date policyUpdateTime) {
		this.policyUpdateTime = policyUpdateTime;
	}

	public Map<String, String> getServiceConfig() {
		return serviceConfig;
	}
	public void setServiceConfig(Map<String, String> serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	/**
	 * @return the policies
	 */
	public List<RangerPolicy> getPolicies() {
		return policies;
	}
	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(List<RangerPolicy> policies) {
		this.policies = policies;
	}
	/**
	 * @return the serviceDef
	 */
	public RangerServiceDef getServiceDef() {
		return serviceDef;
	}
	/**
	 * @param serviceDef the serviceDef to set
	 */
	public void setServiceDef(RangerServiceDef serviceDef) {
		this.serviceDef = serviceDef;
	}

	public String getAuditMode() {
		return auditMode;
	}

	public void setAuditMode(String auditMode) {
		this.auditMode = auditMode;
	}
	/**
	 * @return the tagPolicies
	 */
	public ServicePolicies.TagPolicies getTagPolicies() {
		return tagPolicies;
	}
	/**
	 * @param tagPolicies the tagPolicies to set
	 */
	public void setTagPolicies(ServicePolicies.TagPolicies tagPolicies) {
		this.tagPolicies = tagPolicies;
	}

	public Map<String, SecurityZoneInfo> getSecurityZones() { return securityZones; }

	public void setSecurityZones(Map<String, SecurityZoneInfo> securityZones) {
		this.securityZones = securityZones;
	}

	@Override
	public String toString() {
		return "serviceName=" + serviceName + ", "
				+ "serviceId=" + serviceId + ", "
			 	+ "policyVersion=" + policyVersion + ", "
			 	+ "policyUpdateTime=" + policyUpdateTime + ", "
			 	+ "policies=" + policies + ", "
			 	+ "tagPolicies=" + tagPolicies + ", "
			 	+ "policyDeltas=" + policyDeltas + ", "
			 	+ "serviceDef=" + serviceDef + ", "
			 	+ "auditMode=" + auditMode + ", "
				+ "securityZones=" + securityZones
				;
	}
	public List<RangerPolicyDelta> getPolicyDeltas() { return this.policyDeltas; }

	public void setPolicyDeltas(List<RangerPolicyDelta> policyDeltas) { this.policyDeltas = policyDeltas; }

	@JsonAutoDetect(fieldVisibility=Visibility.ANY)
	@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown=true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class TagPolicies implements java.io.Serializable {
		private static final long serialVersionUID = 1L;

		private String             serviceName;
		private Long               serviceId;
		private Long               policyVersion;
		private Date               policyUpdateTime;
		private List<RangerPolicy> policies;
		private RangerServiceDef   serviceDef;
		private String             auditMode = RangerPolicyEngine.AUDIT_DEFAULT;
		private Map<String, String> serviceConfig;

		/**
		 * @return the serviceName
		 */
		public String getServiceName() {
			return serviceName;
		}
		/**
		 * @param serviceName the serviceName to set
		 */
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}
		/**
		 * @return the serviceId
		 */
		public Long getServiceId() {
			return serviceId;
		}
		/**
		 * @param serviceId the serviceId to set
		 */
		public void setServiceId(Long serviceId) {
			this.serviceId = serviceId;
		}
		/**
		 * @return the policyVersion
		 */
		public Long getPolicyVersion() {
			return policyVersion;
		}
		/**
		 * @param policyVersion the policyVersion to set
		 */
		public void setPolicyVersion(Long policyVersion) {
			this.policyVersion = policyVersion;
		}
		/**
		 * @return the policyUpdateTime
		 */
		public Date getPolicyUpdateTime() {
			return policyUpdateTime;
		}
		/**
		 * @param policyUpdateTime the policyUpdateTime to set
		 */
		public void setPolicyUpdateTime(Date policyUpdateTime) {
			this.policyUpdateTime = policyUpdateTime;
		}
		/**
		 * @return the policies
		 */
		public List<RangerPolicy> getPolicies() {
			return policies;
		}
		/**
		 * @param policies the policies to set
		 */
		public void setPolicies(List<RangerPolicy> policies) {
			this.policies = policies;
		}
		/**
		 * @return the serviceDef
		 */
		public RangerServiceDef getServiceDef() {
			return serviceDef;
		}
		/**
		 * @param serviceDef the serviceDef to set
		 */
		public void setServiceDef(RangerServiceDef serviceDef) {
			this.serviceDef = serviceDef;
		}

		public String getAuditMode() {
			return auditMode;
		}

		public void setAuditMode(String auditMode) {
			this.auditMode = auditMode;
		}

		public Map<String, String> getServiceConfig() {
			return serviceConfig;
		}

		public void setServiceConfig(Map<String, String> serviceConfig) {
			this.serviceConfig = serviceConfig;
		}

		@Override
		public String toString() {
			return "serviceName=" + serviceName + ", "
					+ "serviceId=" + serviceId + ", "
					+ "policyVersion=" + policyVersion + ", "
					+ "policyUpdateTime=" + policyUpdateTime + ", "
					+ "policies=" + policies + ", "
					+ "serviceDef=" + serviceDef + ", "
					+ "auditMode=" + auditMode
					+ "serviceConfig=" + serviceConfig
					;
		}
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SecurityZoneInfo implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		private String                          zoneName;
		private List<HashMap<String, List<String>>> resources;
		private List<RangerPolicy>              policies;
		private List<RangerPolicyDelta>         policyDeltas;
		private Boolean                         containsAssociatedTagService;

		public String getZoneName() {
			return zoneName;
		}

		public List<HashMap<String, List<String>>> getResources() {
			return resources;
		}

		public List<RangerPolicy> getPolicies() {
			return policies;
		}

		public List<RangerPolicyDelta> getPolicyDeltas() { return policyDeltas; }

		public Boolean getContainsAssociatedTagService() { return containsAssociatedTagService; }

		public void setZoneName(String zoneName) {
			this.zoneName = zoneName;
		}

		public void setResources(List<HashMap<String, List<String>>> resources) {
			this.resources = resources;
		}

		public void setPolicies(List<RangerPolicy> policies) {
			this.policies = policies;
		}

		public void setPolicyDeltas(List<RangerPolicyDelta> policyDeltas) { this.policyDeltas = policyDeltas; }

		public void setContainsAssociatedTagService(Boolean containsAssociatedTagService) { this.containsAssociatedTagService = containsAssociatedTagService; }

		@Override
		public String toString() {
			return "zoneName=" + zoneName + ", "
					+ "resources=" + resources + ", "
					+ "policies=" + policies + ", "
					+ "policyDeltas=" + policyDeltas + ", "
					+ "containsAssociatedTagService=" + containsAssociatedTagService
					;
		}
	}

	static public ServicePolicies copyHeader(ServicePolicies source) {
		ServicePolicies ret = new ServicePolicies();

		ret.setServiceName(source.getServiceName());
		ret.setServiceId(source.getServiceId());
		ret.setPolicyVersion(source.getPolicyVersion());
		ret.setAuditMode(source.getAuditMode());
		ret.setServiceDef(source.getServiceDef());
		ret.setPolicyUpdateTime(source.getPolicyUpdateTime());
		ret.setSecurityZones(source.getSecurityZones());
		ret.setPolicies(Collections.emptyList());
		ret.setPolicyDeltas(null);
		if (source.getTagPolicies() != null) {
			TagPolicies tagPolicies = copyHeader(source.getTagPolicies(), source.getServiceDef().getName());
			ret.setTagPolicies(tagPolicies);
		}

		return ret;
	}

	static public TagPolicies copyHeader(TagPolicies source, String componentServiceName) {
		TagPolicies ret = new TagPolicies();

		ret.setServiceName(source.getServiceName());
		ret.setServiceId(source.getServiceId());
		ret.setPolicyVersion(source.getPolicyVersion());
		ret.setAuditMode(source.getAuditMode());
		ret.setServiceDef(ServiceDefUtil.normalizeAccessTypeDefs(source.getServiceDef(), componentServiceName));
		ret.setPolicyUpdateTime(source.getPolicyUpdateTime());
		ret.setPolicies(Collections.emptyList());

		return ret;
	}

	public static ServicePolicies applyDelta(final ServicePolicies servicePolicies, RangerPolicyEngineImpl policyEngine) {
		ServicePolicies ret = copyHeader(servicePolicies);

		List<RangerPolicy> oldResourcePolicies = policyEngine.getResourcePolicies();
		List<RangerPolicy> oldTagPolicies      = policyEngine.getTagPolicies();

		List<RangerPolicy> newResourcePolicies = RangerPolicyDeltaUtil.applyDeltas(oldResourcePolicies, servicePolicies.getPolicyDeltas(), servicePolicies.getServiceDef().getName());

		ret.setPolicies(newResourcePolicies);

		final List<RangerPolicy> newTagPolicies;
		if (servicePolicies.getTagPolicies() != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("applyingDeltas for tag policies");
			}
			newTagPolicies = RangerPolicyDeltaUtil.applyDeltas(oldTagPolicies, servicePolicies.getPolicyDeltas(), servicePolicies.getTagPolicies().getServiceDef().getName());
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No need to apply deltas for tag policies");
			}
			newTagPolicies = oldTagPolicies;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("New tag policies:[" + Arrays.toString(newTagPolicies.toArray()) + "]");
		}

		if (ret.getTagPolicies() != null) {
			ret.getTagPolicies().setPolicies(newTagPolicies);
		}

		if (MapUtils.isNotEmpty(servicePolicies.getSecurityZones())) {
			Map<String, SecurityZoneInfo> newSecurityZones = new HashMap<>();

			for (Map.Entry<String, SecurityZoneInfo> entry : servicePolicies.getSecurityZones().entrySet()) {
				String 			 zoneName = entry.getKey();
				SecurityZoneInfo zoneInfo = entry.getValue();

				List<RangerPolicy> zoneResourcePolicies = policyEngine.getResourcePolicies(zoneName);
				// There are no separate tag-policy-repositories for each zone

				if (LOG.isDebugEnabled()) {
					LOG.debug("Applying deltas for security-zone:[" + zoneName + "]");
				}

				final List<RangerPolicy> newZonePolicies = RangerPolicyDeltaUtil.applyDeltas(zoneResourcePolicies, zoneInfo.getPolicyDeltas(), servicePolicies.getServiceDef().getName());

				if (LOG.isDebugEnabled()) {
					LOG.debug("New resource policies for security-zone:[" + zoneName + "], zoneResourcePolicies:[" + Arrays.toString(newZonePolicies.toArray())+ "]");
				}

				SecurityZoneInfo newZoneInfo = new SecurityZoneInfo();

				newZoneInfo.setZoneName(zoneName);
				newZoneInfo.setResources(zoneInfo.getResources());
				newZoneInfo.setPolicies(newZonePolicies);

				newSecurityZones.put(zoneName, newZoneInfo);
			}

			ret.setSecurityZones(newSecurityZones);
		}

		return ret;
	}
}
