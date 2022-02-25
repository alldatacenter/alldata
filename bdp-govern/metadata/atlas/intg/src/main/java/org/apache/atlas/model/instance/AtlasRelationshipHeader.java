/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.instance;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.PList;
import org.apache.atlas.model.SearchFilter.SortType;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasRelationshipHeader extends AtlasStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String                                  guid                = null;
    private AtlasEntity.Status                      status              = AtlasEntity.Status.ACTIVE;
    private AtlasRelationshipDef.PropagateTags      propagateTags       = AtlasRelationshipDef.PropagateTags.NONE;
    private String                                  label               = null;
    private AtlasObjectId                           end1                = null;
    private AtlasObjectId                           end2                = null;

    public AtlasRelationshipHeader() {

    }

    public AtlasRelationshipHeader(String typeName, String guid) {
        super(typeName);
        setGuid(guid);
    }

    public AtlasRelationshipHeader(String typeName, String guid, AtlasObjectId end1, AtlasObjectId end2, AtlasRelationshipDef.PropagateTags propagateTags) {
        this(typeName, guid);
        this.propagateTags = propagateTags;
        setEnd1(end1);
        setEnd2(end2);
    }

    public AtlasRelationshipHeader(AtlasRelationship relationship) {
        this(relationship.getTypeName(), relationship.getGuid(), relationship.getEnd1(), relationship.getEnd2(), relationship.getPropagateTags());

        setLabel(relationship.getLabel());
        switch (relationship.getStatus()) {
            case ACTIVE:
                setStatus(AtlasEntity.Status.ACTIVE);
                break;

            case DELETED:
                setStatus(AtlasEntity.Status.DELETED);
                break;
        }
    }


    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public AtlasEntity.Status getStatus() {
        return status;
    }

    public void setStatus(AtlasEntity.Status status) {
        this.status = status;
    }

    public AtlasRelationshipDef.PropagateTags getPropagateTags() {
        return propagateTags;
    }

    public void setPropagateTags(AtlasRelationshipDef.PropagateTags propagateTags) {
        this.propagateTags = propagateTags;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public AtlasObjectId getEnd1() {
        return end1;
    }

    public void setEnd1(AtlasObjectId end1) {
        this.end1 = end1;
    }

    public AtlasObjectId getEnd2() {
        return end2;
    }

    public void setEnd2(AtlasObjectId end2) {
        this.end2 = end2;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasRelationshipHeader{");
        sb.append("guid='").append(guid).append('\'');
        sb.append(", status=").append(status);
        sb.append(", label=").append(label);
        sb.append(", propagateTags=").append(propagateTags);
        sb.append(", end1=").append(end1);
        sb.append(", end2=").append(end2);
        super.toString(sb);
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AtlasRelationshipHeader that = (AtlasRelationshipHeader) o;
        return Objects.equals(guid, that.guid) &&
                       status == that.status &&
                       Objects.equals(label, that.label) &&
                       Objects.equals(propagateTags, that.propagateTags) &&
                       Objects.equals(end1, that.end1) &&
                       Objects.equals(end2, that.end2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), guid, status, label, propagateTags, end1, end2);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * REST serialization friendly list.
     */
    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlSeeAlso(AtlasEntity.class)
    public static class AtlasRelationshipHeaders extends PList<AtlasRelationshipHeader> {
        private static final long serialVersionUID = 1L;

        public AtlasRelationshipHeaders() {
            super();
        }

        public AtlasRelationshipHeaders(List<AtlasRelationshipHeader> list) {
            super(list);
        }

        public AtlasRelationshipHeaders(List list, long startIndex, int pageSize, long totalCount,
                                  SortType sortType, String sortBy) {
            super(list, startIndex, pageSize, totalCount, sortType, sortBy);
        }
    }
}
