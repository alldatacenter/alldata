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


import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceTags implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	public static final String OP_ADD_OR_UPDATE = "add_or_update";
	public static final String OP_DELETE        = "delete";
	public static final String OP_REPLACE       = "replace";

	public enum TagsChangeExtent { NONE, TAGS, SERVICE_RESOURCE, ALL }
	public enum TagsChangeType { NONE, SERVICE_RESOURCE_UPDATE, TAG_UPDATE, TAG_RESOURCE_MAP_UPDATE, RANGER_ADMIN_START, INVALIDATE_TAG_DELTAS, ALL }

	private String                      op = OP_ADD_OR_UPDATE;
	private String                      serviceName;
	private Long                        tagVersion;
	private Date                        tagUpdateTime;
	private Map<Long, RangerTagDef>     tagDefinitions;
	private Map<Long, RangerTag>        tags;
	private List<RangerServiceResource> serviceResources;
	private Map<Long, List<Long>>       resourceToTagIds;
	private Boolean					 	isDelta;
	private TagsChangeExtent			tagsChangeExtent;

	@JsonIgnore
	Map<RangerTag, Long>                cachedTags  = new HashMap<>();

	public ServiceTags() {
		this(OP_ADD_OR_UPDATE, null, 0L, null, null, null, null, null);
	}

	public ServiceTags(String op, String serviceName, Long tagVersion, Date tagUpdateTime, Map<Long, RangerTagDef> tagDefinitions,
					   Map<Long, RangerTag> tags, List<RangerServiceResource> serviceResources, Map<Long, List<Long>> resourceToTagIds) {
		this(op, serviceName, tagVersion, tagUpdateTime, tagDefinitions, tags, serviceResources, resourceToTagIds, false, TagsChangeExtent.ALL);
	}
	public ServiceTags(String op, String serviceName, Long tagVersion, Date tagUpdateTime, Map<Long, RangerTagDef> tagDefinitions,
					   Map<Long, RangerTag> tags, List<RangerServiceResource> serviceResources, Map<Long, List<Long>> resourceToTagIds, Boolean isDelta, TagsChangeExtent tagsChangeExtent) {
		setOp(op);
		setServiceName(serviceName);
		setTagVersion(tagVersion);
		setTagUpdateTime(tagUpdateTime);
		setTagDefinitions(tagDefinitions);
		setTags(tags);
		setServiceResources(serviceResources);
		setResourceToTagIds(resourceToTagIds);
		setIsDelta(isDelta);
		setTagsChangeExtent(tagsChangeExtent);
	}
	/**
	 * @return the op
	 */
	public String getOp() {
		return op;
	}

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @param op the op to set
	 */
	public void setOp(String op) {
		this.op = op;
	}

	/**
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * @return the tagVersion
	 */
	public Long getTagVersion() {
		return tagVersion;
	}

	/**
	 * @param tagVersion the version to set
	 */
	public void setTagVersion(Long tagVersion) {
		this.tagVersion = tagVersion;
	}

	/**
	 * @return the tagUpdateTime
	 */
	public Date getTagUpdateTime() {
		return tagUpdateTime;
	}

	/**
	 * @param tagUpdateTime the tagUpdateTime to set
	 */
	public void setTagUpdateTime(Date tagUpdateTime) {
		this.tagUpdateTime = tagUpdateTime;
	}

	public Map<Long, RangerTagDef> getTagDefinitions() {
		return tagDefinitions;
	}

	public void setTagDefinitions(Map<Long, RangerTagDef> tagDefinitions) {
		this.tagDefinitions = tagDefinitions == null ? new HashMap<Long, RangerTagDef>() : tagDefinitions;
	}

	public Map<Long, RangerTag> getTags() {
		return tags;
	}

	public void setTags(Map<Long, RangerTag> tags) {
		this.tags = tags == null ? new HashMap<Long, RangerTag>() : tags;
	}

	public List<RangerServiceResource> getServiceResources() {
		return serviceResources;
	}

	public void setServiceResources(List<RangerServiceResource> serviceResources) {
		this.serviceResources = serviceResources == null ? new ArrayList<RangerServiceResource>() : serviceResources;
	}

	public Map<Long, List<Long>> getResourceToTagIds() {
		return resourceToTagIds;
	}

	public void setResourceToTagIds(Map<Long, List<Long>> resourceToTagIds) {
		this.resourceToTagIds = resourceToTagIds == null ? new HashMap<Long, List<Long>>() : resourceToTagIds;
	}

	public Boolean getIsDelta() {
		return isDelta == null ? Boolean.FALSE : isDelta;
	}

	public void setIsDelta(Boolean isDelta) {
		this.isDelta = isDelta;
	}

	public TagsChangeExtent getTagsChangeExtent() {
		return tagsChangeExtent;
	}

	public void setTagsChangeExtent(TagsChangeExtent tagsChangeExtent) {
		this.tagsChangeExtent = tagsChangeExtent;
	}
	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("ServiceTags={")
				.append("op=").append(op).append(", ")
				.append("serviceName=").append(serviceName).append(", ")
				.append("tagVersion=").append(tagVersion).append(", ")
				.append("tagUpdateTime={").append(tagUpdateTime).append("}")
				.append("isDelta={").append(isDelta).append("}")
				.append("tagsChangeExtent={").append(tagsChangeExtent).append("}")
				.append("}");

		return sb;
	}

	public int dedupTags() {
		final int ret;

		Map<Long, Long>                      replacedIds = new HashMap<>();
		Iterator<Map.Entry<Long, RangerTag>> iter        = tags.entrySet().iterator();

		final int initialTagsCount = tags.size();

		while (iter.hasNext()) {
			Map.Entry<Long, RangerTag> entry       = iter.next();
			Long                       tagId       = entry.getKey();
			RangerTag                  tag         = entry.getValue();
			Long                       cachedTagId = cachedTags.get(tag);

			if (cachedTagId == null) {
				cachedTags.put(tag, tagId);
			} else {
				replacedIds.put(tagId, cachedTagId);
				iter.remove();
			}
		}

		final int finalTagsCount = tags.size();

		for (Map.Entry<Long, List<Long>> resourceEntry : resourceToTagIds.entrySet()) {
			ListIterator<Long> listIter = resourceEntry.getValue().listIterator();

			while (iter.hasNext()) {
				Long tagId         = listIter.next();
				Long replacerTagId = replacedIds.get(tagId);

				if (replacerTagId != null) {
					listIter.set(replacerTagId);
				}
			}
		}

		ret = initialTagsCount - finalTagsCount;

		return ret;
	}
}
