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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class RangerRequestedResources {
	private List<RangerAccessResource> requestedResources = new ArrayList<>();

	public RangerRequestedResources() {
	}

	public void addRequestedResource(RangerAccessResource requestedResource) {
		if (requestedResource != null) {

			if (CollectionUtils.isEmpty(requestedResources)) {
				requestedResources = new ArrayList<>();
			}

			boolean exists = requestedResources.contains(requestedResource);

			if (!exists) {
				requestedResources.add(requestedResource);
			}
		}
	}

	public boolean isMutuallyExcluded(final List<RangerPolicyResourceMatcher> matchers, final Map<String, Object> evalContext) {
		boolean ret = true;

		int matchedCount = 0;

		if (!CollectionUtils.isEmpty(matchers) && !CollectionUtils.isEmpty(requestedResources) && requestedResources.size() > 1) {

			for (RangerAccessResource resource : requestedResources) {

				for (RangerPolicyResourceMatcher matcher : matchers) {

					if (matcher.isMatch(resource, evalContext) && matchedCount++ > 0) {
						ret = false;
						break;
					}
				}
			}
		}

		return ret;
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	StringBuilder toString(StringBuilder sb) {
		sb.append("AllRequestedHiveResources={");
		for (RangerAccessResource resource : requestedResources) {
			if (resource != null) {
				sb.append(resource.getAsString());
				sb.append("; ");
			}
		}
		sb.append("} ");

		return sb;
	}
}


