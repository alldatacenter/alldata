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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerServiceResource extends RangerBaseModelObject {
	private static final long serialVersionUID = 1L;

	private String                                         serviceName;
	private Map<String, RangerPolicy.RangerPolicyResource> resourceElements;
	private String                                         ownerUser;
	private String                                         resourceSignature;
	private Map<String, String>							   additionalInfo;

	public RangerServiceResource(String guid, String serviceName, Map<String, RangerPolicy.RangerPolicyResource> resourceElements, String resourceSignature, String ownerUser, Map<String, String> additionalInfo) {
		super();
		setGuid(guid);
		setServiceName(serviceName);
		setResourceElements(resourceElements);
		setResourceSignature(resourceSignature);
		setOwnerUser(ownerUser);
		setAdditionalInfo(additionalInfo);
	}
	public RangerServiceResource(String guid, String serviceName, Map<String, RangerPolicy.RangerPolicyResource> resourceElements, String resourceSignature, String ownerUser) {
		this(guid, serviceName, resourceElements, resourceSignature,ownerUser, null);
	}
	public RangerServiceResource(String guid, String serviceName, Map<String, RangerPolicy.RangerPolicyResource> resourceElements, String resourceSignature) {
		this(guid, serviceName, resourceElements, resourceSignature, null);

	}

	public RangerServiceResource(String guid, String serviceName, Map<String, RangerPolicy.RangerPolicyResource> resourceElements) {
		this(guid, serviceName, resourceElements, null, null);
	}
	public RangerServiceResource(String serviceName, Map<String, RangerPolicy.RangerPolicyResource> resourceElements) {
		this(null, serviceName, resourceElements, null, null);
	}

	public RangerServiceResource() {
		this(null, null, null, null, null);
	}

	public String getServiceName() { return serviceName; }

	public Map<String, RangerPolicy.RangerPolicyResource> getResourceElements() { return resourceElements; }

	public String getResourceSignature() {
		return resourceSignature;
	}

	public String getOwnerUser() {
		return ownerUser;
	}

	public Map<String, String> getAdditionalInfo() {
		return additionalInfo;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setResourceElements(Map<String, RangerPolicy.RangerPolicyResource> resource) {
		this.resourceElements = resource == null ? new HashMap<String, RangerPolicy.RangerPolicyResource>() : resource;
	}

	public void setResourceSignature(String resourceSignature) {
		this.resourceSignature = resourceSignature;
	}

	public void setOwnerUser(String ownerUser) {
		this.ownerUser = ownerUser;
	}

	public void setAdditionalInfo(Map<String, String> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {

		sb.append("RangerServiceResource={ ");

		super.toString(sb);

		sb.append("guid={").append(getGuid()).append("} ");
		sb.append("serviceName={").append(serviceName).append("} ");

		sb.append("resourceElements={");
		if(resourceElements != null) {
			for(Map.Entry<String, RangerPolicy.RangerPolicyResource> e : resourceElements.entrySet()) {
				sb.append(e.getKey()).append("={");
				e.getValue().toString(sb);
				sb.append("} ");
			}
		}
		sb.append("} ");

		sb.append("ownerUser={").append(ownerUser).append("} ");

		sb.append("additionalInfo={");
		if(additionalInfo != null) {
			for(Map.Entry<String, String> e : additionalInfo.entrySet()) {
				sb.append(e.getKey()).append("={").append(e.getValue()).append("} ");
			}
		}
		sb.append("} ");

		sb.append("resourceSignature={").append(resourceSignature).append("} ");

		sb.append(" }");

		return sb;
	}
}

