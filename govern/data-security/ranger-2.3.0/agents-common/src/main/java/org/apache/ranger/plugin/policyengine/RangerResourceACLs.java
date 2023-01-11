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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemDataMaskInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemRowFilterInfo;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator.ACCESS_ALLOWED;
import static org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator.ACCESS_DENIED;

public class RangerResourceACLs {
	final private Map<String, Map<String, AccessResult>> userACLs   = new HashMap<>();
	final private Map<String, Map<String, AccessResult>> groupACLs  = new HashMap<>();
	final private Map<String, Map<String, AccessResult>> roleACLs   = new HashMap<>();
	final private List<RowFilterResult>                  rowFilters = new ArrayList<>();
	final private List<DataMaskResult>                   dataMasks  = new ArrayList<>();

	public RangerResourceACLs() {
	}

	public Map<String, Map<String, AccessResult>> getUserACLs() {
		return userACLs;
	}

	public Map<String, Map<String, AccessResult>> getGroupACLs() {
		return groupACLs;
	}

	public Map<String, Map<String, AccessResult>> getRoleACLs() { return roleACLs; }

	public List<RowFilterResult> getRowFilters() { return rowFilters; }

	public List<DataMaskResult> getDataMasks() { return dataMasks; }

	public void finalizeAcls() {
		Map<String, AccessResult>  publicGroupAccessInfo = groupACLs.get(RangerPolicyEngine.GROUP_PUBLIC);
		if (publicGroupAccessInfo != null) {

			for (Map.Entry<String, AccessResult> entry : publicGroupAccessInfo.entrySet()) {
				String accessType = entry.getKey();
				AccessResult accessResult = entry.getValue();
				int access = accessResult.getResult();

				if (access == ACCESS_DENIED || access == ACCESS_ALLOWED) {
					for (Map.Entry<String, Map<String, AccessResult>> mapEntry : userACLs.entrySet()) {
						Map<String, AccessResult> mapValue = mapEntry.getValue();
						AccessResult savedAccessResult = mapValue.get(accessType);
						if (savedAccessResult != null && !savedAccessResult.getIsFinal()) {
							mapValue.remove(accessType);
						}
					}

					for (Map.Entry<String, Map<String, AccessResult>> mapEntry : groupACLs.entrySet()) {
						if (!StringUtils.equals(mapEntry.getKey(), RangerPolicyEngine.GROUP_PUBLIC)) {
							Map<String, AccessResult> mapValue = mapEntry.getValue();
							AccessResult savedAccessResult = mapValue.get(accessType);
							if (savedAccessResult != null && !savedAccessResult.getIsFinal()) {
								mapValue.remove(accessType);
							}
						}
					}
				}
			}
		}
		finalizeAcls(userACLs);
		finalizeAcls(groupACLs);
		finalizeAcls(roleACLs);
	}

	public void setUserAccessInfo(String userName, String accessType, Integer access, RangerPolicy policy) {
		Map<String, AccessResult> userAccessInfo = userACLs.get(userName);

		if (userAccessInfo == null) {
			userAccessInfo = new HashMap<>();

			userACLs.put(userName, userAccessInfo);
		}

		AccessResult accessResult = userAccessInfo.get(accessType);

		if (accessResult == null) {
			accessResult = new AccessResult(access, policy);

			userAccessInfo.put(accessType, accessResult);
		} else {
			accessResult.setResult(access);
			accessResult.setPolicy(policy);
		}
	}

	public void setGroupAccessInfo(String groupName, String accessType, Integer access, RangerPolicy policy) {
		Map<String, AccessResult> groupAccessInfo = groupACLs.get(groupName);

		if (groupAccessInfo == null) {
			groupAccessInfo = new HashMap<>();

			groupACLs.put(groupName, groupAccessInfo);
		}

		AccessResult accessResult = groupAccessInfo.get(accessType);

		if (accessResult == null) {
			accessResult = new AccessResult(access, policy);

			groupAccessInfo.put(accessType, accessResult);
		} else {
			accessResult.setResult(access);
			accessResult.setPolicy(policy);
		}
	}

	public void setRoleAccessInfo(String roleName, String accessType, Integer access, RangerPolicy policy) {
		Map<String, AccessResult> roleAccessInfo = roleACLs.get(roleName);

		if (roleAccessInfo == null) {
			roleAccessInfo = new HashMap<>();

			roleACLs.put(roleName, roleAccessInfo);
		}

		AccessResult accessResult = roleAccessInfo.get(accessType);

		if (accessResult == null) {
			accessResult = new AccessResult(access, policy);

			roleAccessInfo.put(accessType, accessResult);
		} else {
			accessResult.setResult(access);
			accessResult.setPolicy(policy);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{");

		sb.append("UserACLs={");
		for (Map.Entry<String, Map<String, AccessResult>> entry : userACLs.entrySet()) {
			sb.append("user=").append(entry.getKey()).append(":");
			sb.append("permissions={");
			for (Map.Entry<String, AccessResult> permission : entry.getValue().entrySet()) {
				sb.append("{Permission=").append(permission.getKey()).append(", value=").append(permission.getValue()).append("},");
				sb.append("{RangerPolicyID=").append(permission.getValue().getPolicy() == null ? null : permission.getValue().getPolicy().getId()).append("},");
			}
			sb.append("},");
		}
		sb.append("}");

		sb.append(", GroupACLs={");
		for (Map.Entry<String, Map<String, AccessResult>> entry : groupACLs.entrySet()) {
			sb.append("group=").append(entry.getKey()).append(":");
			sb.append("permissions={");
			for (Map.Entry<String, AccessResult> permission : entry.getValue().entrySet()) {
				sb.append("{Permission=").append(permission.getKey()).append(", value=").append(permission.getValue()).append("}, ");
				sb.append("{RangerPolicy ID=").append(permission.getValue().getPolicy() == null ? null : permission.getValue().getPolicy().getId()).append("},");
			}
			sb.append("},");
		}
		sb.append("}");

		sb.append(", RoleACLs={");
		for (Map.Entry<String, Map<String, AccessResult>> entry : roleACLs.entrySet()) {
			sb.append("role=").append(entry.getKey()).append(":");
			sb.append("permissions={");
			for (Map.Entry<String, AccessResult> permission : entry.getValue().entrySet()) {
				sb.append("{Permission=").append(permission.getKey()).append(", value=").append(permission.getValue()).append("}, ");
				sb.append("{RangerPolicy ID=").append(permission.getValue().getPolicy() == null ? null : permission.getValue().getPolicy().getId()).append("},");
			}
			sb.append("},");
		}
		sb.append("}");

		sb.append("}");

		sb.append(", rowFilters=[");
		for (RowFilterResult rowFilter : rowFilters) {
			rowFilter.toString(sb);
			sb.append(" ");
		}
		sb.append("]");

		sb.append(", dataMasks=[");
		for (DataMaskResult dataMask : dataMasks) {
			dataMask.toString(sb);
			sb.append(" ");
		}
		sb.append("]");

		return sb.toString();
	}

	private void finalizeAcls(Map<String, Map<String, AccessResult>> acls) {
		List<String> keysToRemove = new ArrayList<>();
		for (Map.Entry<String, Map<String, AccessResult>> entry : acls.entrySet()) {
			if (entry.getValue().isEmpty()) {
				keysToRemove.add(entry.getKey());
			} else {
				for (Map.Entry<String, AccessResult> permission : entry.getValue().entrySet()) {
					permission.getValue().setIsFinal(true);
				}
			}

		}
		for (String keyToRemove : keysToRemove) {
			acls.remove(keyToRemove);
		}
	}

	@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
	@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown=true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class AccessResult {
		private int     result;
		private boolean isFinal;
		private RangerPolicy  policy;

		public AccessResult() {
			this(-1, null);
		}

		public AccessResult(int result, RangerPolicy policy) {
			this(result, false, policy);
		}

		public AccessResult(int result, boolean isFinal, RangerPolicy policy) {
			setIsFinal(isFinal);
			setResult(result);
			setPolicy(policy);
		}

		public int getResult() { return result; }

		public void setResult(int result) {
			if (!isFinal) {
				this.result = result;

				if (this.result == ACCESS_DENIED) {
					isFinal = true;
				}
			}
		}

		public boolean getIsFinal() { return isFinal; }

		public void setIsFinal(boolean isFinal) { this.isFinal = isFinal; }

		public RangerPolicy getPolicy() {
			return policy;
		}

		public void setPolicy(RangerPolicy policy){
			this.policy = policy;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (other instanceof AccessResult) {
				AccessResult otherObject = (AccessResult)other;
				return result == otherObject.result && isFinal == otherObject.isFinal;
			} else
				return false;

		}
		@Override
		public String toString() {
			if (result == ACCESS_ALLOWED) {
				return "ALLOWED, final=" + isFinal;
			}
			if (result == ACCESS_DENIED) {
				return "NOT_ALLOWED, final=" + isFinal;
			}
			return "CONDITIONAL_ALLOWED, final=" + isFinal;
		}
	}

	@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
	@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown=true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class DataMaskResult implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Set<String>                  users;
		private final Set<String>                  groups;
		private final Set<String>                  roles;
		private final Set<String>                  accessTypes;
		private final RangerPolicyItemDataMaskInfo maskInfo;
		private       boolean                      isConditional = false;

		public DataMaskResult(Set<String> users, Set<String> groups, Set<String> roles, Set<String> accessTypes, RangerPolicyItemDataMaskInfo maskInfo) {
			this.users       = users;
			this.groups      = groups;
			this.roles       = roles;
			this.accessTypes = accessTypes;
			this.maskInfo    = maskInfo;
		}

		public DataMaskResult(DataMaskResult that) {
			this.users         = that.users;
			this.groups        = that.groups;
			this.roles         = that.roles;
			this.accessTypes   = that.accessTypes;
			this.maskInfo      = that.maskInfo;
			this.isConditional = that.isConditional;
		}

		public Set<String> getUsers() { return users; }

		public Set<String> getGroups() { return groups; }

		public Set<String> getRoles() { return roles; }

		public Set<String> getAccessTypes() { return accessTypes; }

		public RangerPolicyItemDataMaskInfo getMaskInfo() { return maskInfo; }

		public boolean getIsConditional() { return isConditional; }

		public void setIsConditional(boolean isConditional) { this.isConditional = isConditional; }

		@Override
		public int hashCode() { return Objects.hash(users, groups, roles, accessTypes, maskInfo, isConditional); }

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			} else if (other == null || getClass() != other.getClass()) {
				return false;
			} else {
				DataMaskResult that = (DataMaskResult) other;

				return Objects.equals(users, that.users) &&
					   Objects.equals(groups, that.groups) &&
					   Objects.equals(roles, that.roles) &&
					   Objects.equals(accessTypes, that.accessTypes) &&
					   Objects.equals(maskInfo, that.maskInfo) &&
					   isConditional == that.isConditional;
			}
		}

		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}

		public StringBuilder toString(StringBuilder sb) {
			sb.append("{");

			if (users != null && !users.isEmpty()) {
				sb.append("users:[");
				for (String user : users) {
					sb.append(user).append(' ');
				}
				sb.append("] ");
			}

			if (groups != null && !groups.isEmpty()) {
				sb.append("groups:[");
				for (String group : groups) {
					sb.append(group).append(' ');
				}
				sb.append("] ");
			}

			if (roles != null && !roles.isEmpty()) {
				sb.append("roles:[");
				for (String role : roles) {
					sb.append(role).append(' ');
				}
				sb.append("] ");
			}

			if (accessTypes != null && !accessTypes.isEmpty()) {
				sb.append("accessTypes:[");
				for (String accessType : accessTypes) {
					sb.append(accessType).append(' ');
				}
				sb.append("] ");
			}

			sb.append("maskInfo=");
			maskInfo.toString(sb);
			sb.append(" isConditional=").append(isConditional);

			sb.append("}");

			return sb;
		}
	}

	@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
	@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown=true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class RowFilterResult implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Set<String>                   users;
		private final Set<String>                   groups;
		private final Set<String>                   roles;
		private final Set<String>                   accessTypes;
		private final RangerPolicyItemRowFilterInfo filterInfo;
		private       boolean                       isConditional = false;

		public RowFilterResult(Set<String> users, Set<String> groups, Set<String> roles, Set<String> accessTypes, RangerPolicyItemRowFilterInfo filterInfo) {
			this.users       = users;
			this.groups      = groups;
			this.roles       = roles;
			this.accessTypes = accessTypes;
			this.filterInfo  = filterInfo;
		}

		public RowFilterResult(RowFilterResult that) {
			this.users         = that.users;
			this.groups        = that.groups;
			this.roles         = that.roles;
			this.accessTypes   = that.accessTypes;
			this.filterInfo    = that.filterInfo;
			this.isConditional = that.isConditional;
		}

		public Set<String> getUsers() { return users; }

		public Set<String> getGroups() { return groups; }

		public Set<String> getRoles() { return roles; }

		public Set<String> getAccessTypes() { return accessTypes; }

		public RangerPolicyItemRowFilterInfo getFilterInfo() { return filterInfo; }

		public boolean getIsConditional() { return isConditional; }

		public void setIsConditional(boolean isConditional) { this.isConditional = isConditional; }

		@Override
		public int hashCode() { return Objects.hash(users, groups, roles, accessTypes, filterInfo, isConditional); }

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			} else if (other == null || getClass() != other.getClass()) {
				return false;
			} else {
				RowFilterResult that = (RowFilterResult) other;

				return Objects.equals(users, that.users) &&
						Objects.equals(groups, that.groups) &&
						Objects.equals(roles, that.roles) &&
						Objects.equals(accessTypes, that.accessTypes) &&
						Objects.equals(filterInfo, that.filterInfo) &&
						isConditional == that.isConditional;
			}
		}

		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}

		public StringBuilder toString(StringBuilder sb) {
			sb.append("{");

			if (users != null && !users.isEmpty()) {
				sb.append("users:[");
				for (String user : users) {
					sb.append(user).append(' ');
				}
				sb.append("] ");
			}

			if (groups != null && !groups.isEmpty()) {
				sb.append("groups:[");
				for (String group : groups) {
					sb.append(group).append(' ');
				}
				sb.append("] ");
			}

			if (roles != null && !roles.isEmpty()) {
				sb.append("roles:[");
				for (String role : roles) {
					sb.append(role).append(' ');
				}
				sb.append("] ");
			}

			if (accessTypes != null && !accessTypes.isEmpty()) {
				sb.append("accessTypes:[");
				for (String accessType : accessTypes) {
					sb.append(accessType).append(' ');
				}
				sb.append("] ");
			}

			sb.append("filterInfo=");
			filterInfo.toString(sb);
			sb.append(" isConditional=").append(isConditional);

			sb.append("}");

			return sb;
		}
	}
}
