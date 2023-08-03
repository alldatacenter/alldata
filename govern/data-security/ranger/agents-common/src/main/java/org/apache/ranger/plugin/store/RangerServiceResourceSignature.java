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

package org.apache.ranger.plugin.store;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceResource;

import java.util.*;

public class RangerServiceResourceSignature {
	private final String _string;
	private final String _hash;

	public RangerServiceResourceSignature(RangerServiceResource serviceResource) {
		_string = ServiceResourceSerializer.toString(serviceResource);
		if (RangerAdminConfig.getInstance().isFipsEnabled()) {
			_hash = DigestUtils.sha512Hex(_string);
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

	static class ServiceResourceSerializer {

		static final int _SignatureVersion = 1;

		static public String toString(final RangerServiceResource serviceResource) {
			// invalid/empty serviceResource gets a deterministic signature as if it had an
			// empty resource string
			Map<String, RangerPolicy.RangerPolicyResource> resource = serviceResource.getResourceElements();
			Map<String, ResourceSerializer> resources = new TreeMap<>();
			for (Map.Entry<String, RangerPolicy.RangerPolicyResource> entry : resource.entrySet()) {
				String resourceName = entry.getKey();
				ResourceSerializer resourceView = new ResourceSerializer(entry.getValue());
				resources.put(resourceName, resourceView);
			}
			String resourcesAsString = resources.toString();
			return String.format("{version=%d,resource=%s}", _SignatureVersion, resourcesAsString);
		}

		static class ResourceSerializer {
			final RangerPolicy.RangerPolicyResource _policyResource;

			ResourceSerializer(RangerPolicy.RangerPolicyResource policyResource) {
				_policyResource = policyResource;
			}

			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				builder.append("{");
				if (_policyResource != null) {
					builder.append("values=");
					if (_policyResource.getValues() != null) {
						List<String> values = new ArrayList<>(_policyResource.getValues());
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
	}
}

