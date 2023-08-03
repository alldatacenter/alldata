/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.typedef;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.TypeCategory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasTypeDefHeader implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String guid;
    private String name;
    private String serviceType = null;
    private TypeCategory category;

    public AtlasTypeDefHeader() {
        this(null, null, null);
    }

    public AtlasTypeDefHeader(String guid, String name, TypeCategory category) {
        this.guid = guid;
        this.name = name;
        this.category = category;
    }

    public AtlasTypeDefHeader(String guid, String name, TypeCategory category, String serviceType) {
        this(guid, name, category);
        this.serviceType = serviceType;
    }

    public AtlasTypeDefHeader(AtlasBaseTypeDef typeDef) {
        this(typeDef.getGuid(), typeDef.getName(), typeDef.getCategory(), typeDef.getServiceType());
    }

    public AtlasTypeDefHeader(AtlasTypeDefHeader other) {
        super();
        if (other == null) {
            setGuid(null);
            setName(null);
            setCategory(null);
            setServiceType(null);
        } else {
            setGuid(other.getGuid());
            setName(other.getName());
            setCategory(other.getCategory());
            setServiceType(other.getServiceType());
        }
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeCategory getCategory() {
        return category;
    }

    public void setCategory(TypeCategory category) {
        this.category = category;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtlasTypeDefHeader that = (AtlasTypeDefHeader) o;
        return Objects.equals(guid, that.guid) &&
                Objects.equals(name, that.name) &&
                Objects.equals(serviceType, that.serviceType) &&
                category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(guid, name, category, serviceType);
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasTypeDefHeader{");
        sb.append("guid='").append(guid).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", typeCategory='").append(category).append('\'');
        sb.append(", serviceType='").append(serviceType).append('\'');
        sb.append('}');

        return sb;
    }
}
