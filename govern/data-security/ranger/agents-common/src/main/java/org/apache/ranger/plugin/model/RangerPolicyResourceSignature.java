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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerPolicyResourceSignature {

	static final int _SignatureVersion = 1;
	private static final Logger LOG = LoggerFactory.getLogger(RangerPolicyResourceSignature.class);
	static final RangerPolicyResourceSignature _EmptyResourceSignature = new RangerPolicyResourceSignature((RangerPolicy)null);
	
	private final String _string;
	private final String _hash;
	private final RangerPolicy _policy;

	public RangerPolicyResourceSignature(RangerPolicy policy) {
		_policy = policy;
		PolicySerializer serializer = new PolicySerializer(_policy);
		_string = serializer.toString();
		if (RangerAdminConfig.getInstance().isFipsEnabled()) {
			_hash = DigestUtils.sha512Hex(_string);
		} else {
			_hash = DigestUtils.sha256Hex(_string);
		}
	}

	/**
	 * Only added for testability.  Do not make public
	 * @param string
	 */
	RangerPolicyResourceSignature(String string) {
		_policy = null;
		if (string == null) {
			_string = "";
		} else {
			_string = string;
		}
		if (RangerAdminConfig.getInstance().isFipsEnabled()) {
			_hash = DigestUtils.sha384Hex(_string);
		} else {
			_hash = DigestUtils.sha256Hex(_string);
		}
	}
	
	String asString() {
		return _string;
	}

	public String getSignature() {
		return _hash;
	}
	
	@Override
	public int hashCode() {
		// we assume no collision
		return Objects.hashCode(_hash);
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof RangerPolicyResourceSignature)) {
			return false;
		}
		RangerPolicyResourceSignature that = (RangerPolicyResourceSignature)object;
		return Objects.equals(this._hash, that._hash);
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s", _hash, _string);
	}

	static class PolicySerializer {
		final RangerPolicy _policy;
		PolicySerializer(RangerPolicy policy) {
			_policy = policy;
		}

		boolean isPolicyValidForResourceSignatureComputation() {
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("==> RangerPolicyResourceSignature.isPolicyValidForResourceSignatureComputation(%s)", _policy));
			}
			
			boolean valid = false;
			if (_policy == null) {
				LOG.debug("isPolicyValidForResourceSignatureComputation: policy was null!");
			} else if (_policy.getResources() == null) {
				LOG.debug("isPolicyValidForResourceSignatureComputation: resources collection on policy was null!");
			} else if (_policy.getResources().containsKey(null)) {
				LOG.debug("isPolicyValidForResourceSignatureComputation: resources collection has resource with null name!");
			} else if (!_policy.getIsEnabled() && StringUtils.isEmpty(_policy.getGuid())) {
				   LOG.debug("isPolicyValidForResourceSignatureComputation: policy GUID is empty for a disabled policy!");
			} else {
				valid = true;
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("<== RangerPolicyResourceSignature.isPolicyValidForResourceSignatureComputation(%s): %s", _policy, valid));
			}
			return valid;
		}

		@Override
		public String toString() {
			// invalid/empty policy gets a deterministic signature as if it had an
			// empty resource string
			if (!isPolicyValidForResourceSignatureComputation()) {
				return "";
			}
			int type = RangerPolicy.POLICY_TYPE_ACCESS;
			if (_policy.getPolicyType() != null) {
				type = _policy.getPolicyType();
			}

			String resource = toSignatureString(_policy.getResources(), _policy.getAdditionalResources());

			if (CollectionUtils.isNotEmpty(_policy.getValiditySchedules())) {
				resource += _policy.getValiditySchedules().toString();
			}
			if (_policy.getPolicyPriority() != null && _policy.getPolicyPriority() != RangerPolicy.POLICY_PRIORITY_NORMAL) {
				resource += _policy.getPolicyPriority();
			}
			if (!StringUtils.isEmpty(_policy.getZoneName())) {
			    resource += _policy.getZoneName();
            }

			if (_policy.getConditions() != null) {
				CustomConditionSerialiser customConditionSerialiser = new CustomConditionSerialiser(_policy.getConditions());
				resource += customConditionSerialiser.toString();
			}
			if (!_policy.getIsEnabled()) {
				resource += _policy.getGuid();
			}

			String result = String.format("{version=%d,type=%d,resource=%s}", _SignatureVersion, type, resource);
			return result;
		}

	}

	public static String toSignatureString(Map<String, RangerPolicyResource> resource) {
		Map<String, ResourceSerializer> resources = new TreeMap<>();

		for (Map.Entry<String, RangerPolicyResource> entry : resource.entrySet()) {
			String             resourceName = entry.getKey();
			ResourceSerializer resourceView = new ResourceSerializer(entry.getValue());

			resources.put(resourceName, resourceView);
		}

		return resources.toString();
	}

	public static String toSignatureString(Map<String, RangerPolicyResource> resource, List<Map<String, RangerPolicyResource>> additionalResources) {
		String ret = toSignatureString(resource);

		if (additionalResources != null && !additionalResources.isEmpty()) {
			List<String> signatures = new ArrayList<>(additionalResources.size() + 1);

			signatures.add(ret);

			for (Map<String, RangerPolicyResource> additionalResource : additionalResources) {
				signatures.add(toSignatureString(additionalResource));
			}

			Collections.sort(signatures);

			ret = signatures.toString();
		}

		return ret;
	}

	static public class ResourceSerializer {
		final RangerPolicyResource _policyResource;

		public ResourceSerializer(RangerPolicyResource policyResource) {
			_policyResource = policyResource;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("{");
			if (_policyResource != null) {
				builder.append("values=");
				if (_policyResource.getValues() != null) {
					List<String> values = new ArrayList<String>(_policyResource.getValues());
					Collections.sort(values);
					builder.append(values);
				}
				builder.append(",excludes=");
				if (_policyResource.getIsExcludes() == null) { // null is same as false
					builder.append(Boolean.FALSE);
				} else {
					builder.append(_policyResource.getIsExcludes());
				}
				builder.append(",recursive=");
				if (_policyResource.getIsRecursive() == null) { // null is the same as false
					builder.append(Boolean.FALSE);
				} else {
					builder.append(_policyResource.getIsRecursive());
				}
			}
			builder.append("}");
			return builder.toString();
		}
	}

	static class CustomConditionSerialiser {
		final List<RangerPolicy.RangerPolicyItemCondition> rangerPolicyConditions;

		CustomConditionSerialiser(List<RangerPolicyItemCondition> rangerPolicyConditions) {
			this.rangerPolicyConditions = rangerPolicyConditions;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			Map<String, List<String>> conditionMap = new TreeMap<>();

			for(RangerPolicyItemCondition rangerPolicyCondition : rangerPolicyConditions) {
				if (rangerPolicyCondition.getType() != null) {
					String type = rangerPolicyCondition.getType();
					List<String> values = new ArrayList<>();
					if (rangerPolicyCondition.getValues() != null) {
						values.addAll(rangerPolicyCondition.getValues());
						Collections.sort(values);
					}
					conditionMap.put(type, values);
				}
			}

			if (MapUtils.isNotEmpty(conditionMap)) {
				builder.append("{");
				builder.append("RangerPolicyConditions=");
				builder.append(conditionMap);
				builder.append("}");
			}

			return builder.toString();
		}
	}
}
