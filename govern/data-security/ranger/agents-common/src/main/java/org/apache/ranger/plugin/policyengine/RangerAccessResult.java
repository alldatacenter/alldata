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
package org.apache.ranger.plugin.policyengine;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.util.ServiceDefUtil;

import java.util.HashMap;
import java.util.Map;

public class RangerAccessResult {
	public final static String KEY_MASK_TYPE =      "maskType";
	public final static String KEY_MASK_CONDITION = "maskCondition";
	public final static String KEY_MASKED_VALUE =   "maskedValue";

	private static String KEY_FILTER_EXPR = "filterExpr";

	private final String              serviceName;
	private final RangerServiceDef    serviceDef;
	private final RangerAccessRequest request;

	private final int policyType;  // Really, policy-type for audit purpose
	private boolean isAccessDetermined;
	private boolean  isAllowed;
	private boolean isAuditedDetermined;
	private boolean  isAudited;
	private long     auditPolicyId  = -1;
	private String   auditLogId;
	private long     policyId  = -1;
	private int      policyPriority;
	private String   zoneName;
	private Long   policyVersion;
	private long     evaluatedPoliciesCount;
	private String   reason;
	private Map<String, Object> additionalInfo;

	public RangerAccessResult(final int policyType, final String serviceName, final RangerServiceDef serviceDef, final RangerAccessRequest request) {
		this.serviceName = serviceName;
		this.serviceDef  = serviceDef;
		this.request     = request;
		this.policyType = policyType;
		this.isAccessDetermined = false;
		this.isAllowed   = false;
		this.isAuditedDetermined = false;
		this.isAudited   = false;
		this.auditPolicyId = -1;
		this.policyId    = -1;
		this.zoneName    = null;
		this.policyVersion = null;
		this.policyPriority = RangerPolicy.POLICY_PRIORITY_NORMAL;
		this.evaluatedPoliciesCount = 0;
		this.reason      = null;
	}

	public void setAccessResultFrom(final RangerAccessResult other) {
		this.isAccessDetermined = other.getIsAccessDetermined();
		this.isAllowed   = other.getIsAllowed();
		this.policyId    = other.getPolicyId();
		this.policyPriority = other.getPolicyPriority();
		this.zoneName       = other.zoneName;
		this.policyVersion  = other.policyVersion;
		this.evaluatedPoliciesCount = other.evaluatedPoliciesCount;
		this.reason      = other.getReason();
		this.additionalInfo = other.additionalInfo == null ? new HashMap<String, Object>() : new HashMap<>(other.additionalInfo);
	}

	public void setAuditResultFrom(final RangerAccessResult other) {
		this.isAuditedDetermined = other.getIsAuditedDetermined();
		this.isAudited = other.getIsAudited();
		this.auditPolicyId = other.getAuditPolicyId();
		this.policyVersion = other.policyVersion;
	}

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @return the serviceDef
	 */
	public RangerServiceDef getServiceDef() {
		return serviceDef;
	}

	/**
	 * @return the request
	 */
	public RangerAccessRequest getAccessRequest() {
		return request;
	}

	public int getPolicyType() { return policyType; }

	public boolean getIsAccessDetermined() { return isAccessDetermined; }

	public void setIsAccessDetermined(boolean value) { isAccessDetermined = value; }

	/**
	 * @return the isAllowed
	 */
	public boolean getIsAllowed() {
		return isAllowed;
	}

	/**
	 * @param isAllowed the isAllowed to set
	 */
	public void setIsAllowed(boolean isAllowed) {
		if(! isAllowed) {
			setIsAccessDetermined(true);
		}

		this.isAllowed = isAllowed;
	}
	public int getPolicyPriority() { return policyPriority;}

	public void setPolicyPriority(int policyPriority) { this.policyPriority = policyPriority; }

	public String getZoneName() { return zoneName; }

	public void setZoneName(String zoneName) { this.zoneName = zoneName; }

	public Long getPolicyVersion() { return policyVersion; }

	public void setPolicyVersion(Long policyVersion) { this.policyVersion = policyVersion; }

	/**
	 * @param reason the reason to set
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean getIsAuditedDetermined() { return isAuditedDetermined; }

	public void setIsAuditedDetermined(boolean value) { isAuditedDetermined = value; }
	
	/**
	 * @return the isAudited
	 */
	public boolean getIsAudited() {
		return isAudited;
	}



	/**
	 * @param isAudited the isAudited to set
	 */
	public void setIsAudited(boolean isAudited) {
		setIsAuditedDetermined(true);
		this.isAudited = isAudited;
	}

	/**
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @return the policyId
	 */
	public long getPolicyId() {
		return policyId;
	}

	/**
	 * @return the auditPolicyId
	 */
	public long getAuditPolicyId() {
		return auditPolicyId;
	}

	public long getEvaluatedPoliciesCount() { return this.evaluatedPoliciesCount; }
	/**
	 * @param policyId the policyId to set
	 */
	public void setPolicyId(long policyId) {
		this.policyId = policyId;
	}

	public String getAuditLogId() {
		return auditLogId;
	}

	public void setAuditLogId(String auditLogId) {
		this.auditLogId = auditLogId;
	}


	/**
	 * @param policyId the auditPolicyId to set
	 */
	public void setAuditPolicyId(long policyId) {
		this.auditPolicyId = policyId;
	}

	public void incrementEvaluatedPoliciesCount() { this.evaluatedPoliciesCount++; }

	public int getServiceType() {
		int ret = -1;

		if(serviceDef != null && serviceDef.getId() != null) {
			ret = serviceDef.getId().intValue();
		}

		return ret;
	}

	public void setAdditionalInfo(Map<String, Object> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public Map<String, Object> getAdditionalInfo() {
		return additionalInfo;
	}

	public void addAdditionalInfo(String key, Object value) {
		if (additionalInfo == null) {
			additionalInfo = new HashMap<String, Object>();
		}
		additionalInfo.put(key, value);
	}

	public void removeAdditionalInfo(String key) {
		if (MapUtils.isNotEmpty(additionalInfo)) {
			additionalInfo.remove(key);
		}
	}

	/**
	 * @return the maskType
	 */
	public String getMaskType() {
		return additionalInfo == null ? null :  (String) additionalInfo.get(KEY_MASK_TYPE);
	}

	/**
	 * @param maskType the maskType to set
	 */
	public void setMaskType(String maskType) {
		addAdditionalInfo(KEY_MASK_TYPE, maskType);
	}

	/**
	 * @return the maskCondition
	 */
	public String getMaskCondition() {
		return additionalInfo == null ? null : (String) additionalInfo.get(KEY_MASK_CONDITION);
	}

	/**
	 * @param maskCondition the maskCondition to set
	 */
	public void setMaskCondition(String maskCondition) {
		addAdditionalInfo(KEY_MASK_CONDITION, maskCondition);
	}

	/**
	 * @return the maskedValue
	 */
	public String getMaskedValue() {
		return additionalInfo == null ? null : (String) additionalInfo.get(KEY_MASKED_VALUE);
	}
	/**
	 * @param maskedValue the maskedValue to set
	 */
	public void setMaskedValue(String maskedValue) {
		addAdditionalInfo(KEY_MASKED_VALUE, maskedValue);
	}

	public boolean isMaskEnabled() {
		return StringUtils.isNotEmpty(this.getMaskType()) && !StringUtils.equalsIgnoreCase(this.getMaskType(), RangerPolicy.MASK_TYPE_NONE);
	}

	public RangerServiceDef.RangerDataMaskTypeDef getMaskTypeDef() {
		RangerServiceDef.RangerDataMaskTypeDef ret = null;

		String maskType = getMaskType();
		if(StringUtils.isNotEmpty(maskType)) {
			ret = ServiceDefUtil.getDataMaskType(getServiceDef(), maskType);
		}

		return ret;
	}

	/**
	 * @return the filterExpr
	 */
	public String getFilterExpr() {
		return additionalInfo == null ? null : (String) additionalInfo.get(KEY_FILTER_EXPR);
	}

	/**
	 * @param filterExpr the filterExpr to set
	 */
	public void setFilterExpr(String filterExpr) {
		addAdditionalInfo(KEY_FILTER_EXPR, filterExpr);
	}

	public boolean isRowFilterEnabled() {
		return StringUtils.isNotEmpty(getFilterExpr());
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerAccessResult={");

        sb.append("isAccessDetermined={").append(isAccessDetermined).append("} ");
		sb.append("isAllowed={").append(isAllowed).append("} ");
		sb.append("isAuditedDetermined={").append(isAuditedDetermined).append("} ");
		sb.append("isAudited={").append(isAudited).append("} ");
		sb.append("auditLogId={").append(auditLogId).append("} ");
		sb.append("policyType={").append(policyType).append("} ");
		sb.append("policyId={").append(policyId).append("} ");
		sb.append("zoneName={").append(zoneName).append("} ");
		sb.append("auditPolicyId={").append(auditPolicyId).append("} ");
		sb.append("policyVersion={").append(policyVersion).append("} ");
		sb.append("evaluatedPoliciesCount={").append(evaluatedPoliciesCount).append("} ");
		sb.append("reason={").append(reason).append("} ");
		sb.append("additionalInfo={");
		if (additionalInfo != null) {
			for (Map.Entry<String, Object> entry : additionalInfo.entrySet()) {
				sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
			}
		}
		sb.append("}");

		sb.append("}");

		return sb;
	}
}
