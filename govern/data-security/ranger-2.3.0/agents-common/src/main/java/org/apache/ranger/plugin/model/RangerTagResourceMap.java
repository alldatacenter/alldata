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

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerTagResourceMap extends RangerBaseModelObject {
	private static final long serialVersionUID = 1L;

	private Long tagId;
	private Long resourceId;

	public RangerTagResourceMap() {
	}

	public Long getTagId() {
		return tagId;
	}

	public Long getResourceId() {
		return resourceId;
	}

	public void setTagId(Long tagId) {
		this.tagId = tagId;
	}

	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {

		sb.append("RangerTagResourceMap={ ");

		super.toString(sb);

		sb.append("resourceId=").append(resourceId).append(", ");

		sb.append("tagId=").append(tagId);

		sb.append(" }");

		return sb;
	}}

