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


import org.apache.ranger.plugin.util.ServiceTags;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerServiceTags implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final String OP_SET     = "set";     // set tags for the given serviceResources
    public static final String OP_DELETE  = "delete";  // delete tags associated with the given serviceResources
    public static final String OP_REPLACE = "replace"; // replace all resources and tags in the given serviceName with the given serviceResources and tags

    private String                      op = OP_SET;
    private String                      serviceName;
    private Map<Long, RangerTagDef>     tagDefinitions;
    private Map<Long, RangerTag>        tags;
    private List<RangerServiceResource> serviceResources;
    private Map<Long, List<Long>>       resourceToTagIds;
    private Long                        tagVersion;     // read-only field
    private Date                        tagUpdateTime;  // read-only field

    public RangerServiceTags() {
        this(OP_SET, null, null, null, null, null, null, null);
    }

    public RangerServiceTags(String op, String serviceName, Map<Long, RangerTagDef> tagDefinitions,
                             Map<Long, RangerTag> tags, List<RangerServiceResource> serviceResources,
                             Map<Long, List<Long>> resourceToTagIds, Long tagVersion, Date tagUpdateTime) {
        setOp(op);
        setServiceName(serviceName);
        setTagDefinitions(tagDefinitions);
        setTags(tags);
        setServiceResources(serviceResources);
        setResourceToTagIds(resourceToTagIds);
        setTagVersion(tagVersion);
        setTagUpdateTime(tagUpdateTime);
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

    public Map<Long, RangerTagDef> getTagDefinitions() {
        return tagDefinitions;
    }

    public void setTagDefinitions(Map<Long, RangerTagDef> tagDefinitions) {
        this.tagDefinitions = tagDefinitions == null ? new HashMap<>() : tagDefinitions;
    }

    public Map<Long, RangerTag> getTags() {
        return tags;
    }

    public void setTags(Map<Long, RangerTag> tags) {
        this.tags = tags == null ? new HashMap<>() : tags;
    }

    public List<RangerServiceResource> getServiceResources() {
        return serviceResources;
    }

    public void setServiceResources(List<RangerServiceResource> serviceResources) {
        this.serviceResources = serviceResources == null ? new ArrayList<>() : serviceResources;
    }

    public Map<Long, List<Long>> getResourceToTagIds() {
        return resourceToTagIds;
    }

    public void setResourceToTagIds(Map<Long, List<Long>> resourceToTagIds) {
        this.resourceToTagIds = resourceToTagIds == null ? new HashMap<>() : resourceToTagIds;
    }

    public Long getTagVersion() {
        return tagVersion;
    }

    public void setTagVersion(Long tagVersion) {
        this.tagVersion = tagVersion;
    }

    public Date getTagUpdateTime() {
        return tagUpdateTime;
    }

    public void setTagUpdateTime(Date tagUpdateTime) {
        this.tagUpdateTime = tagUpdateTime;
    }


    @Override
    public String toString( ) {
        StringBuilder sb = new StringBuilder();

        toString(sb);

        return sb.toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        sb.append("RangerServiceTags={")
          .append("op=").append(op).append(", ")
          .append("serviceName=").append(serviceName).append(", ")
          .append("}");

        return sb;
    }

    public static ServiceTags toServiceTags(RangerServiceTags tags) {
        ServiceTags ret = null;

        if (tags != null) {
            ret = new ServiceTags(toServiceTagsOp(tags.getOp()), tags.getServiceName(),
                                  tags.tagVersion, tags.getTagUpdateTime(), tags.getTagDefinitions(), tags.getTags(),
                                  tags.getServiceResources(), tags.getResourceToTagIds(), false,
                                  ServiceTags.TagsChangeExtent.ALL);
        }

        return ret;
    }

    public static RangerServiceTags toRangerServiceTags(ServiceTags tags) {
        RangerServiceTags ret = null;

        if (tags != null) {
            ret = new RangerServiceTags(toRangerServiceTagsOp(tags.getOp()), tags.getServiceName(),
                                        tags.getTagDefinitions(), tags.getTags(), tags.getServiceResources(),
                                        tags.getResourceToTagIds(), tags.getTagVersion(), tags.getTagUpdateTime());
        }

        return ret;
    }

    private static String toServiceTagsOp(String rangerServiceTagsOp) {
        String ret = rangerServiceTagsOp;

        if (RangerServiceTags.OP_SET.equals(rangerServiceTagsOp)) {
            ret = ServiceTags.OP_ADD_OR_UPDATE;
        }

        return ret;
    }

    private static String toRangerServiceTagsOp(String serviceTagsOp) {
        String ret = serviceTagsOp;

        if (ServiceTags.OP_ADD_OR_UPDATE.equals(serviceTagsOp)) {
            ret = RangerServiceTags.OP_SET;
        }

        return ret;
    }
}
