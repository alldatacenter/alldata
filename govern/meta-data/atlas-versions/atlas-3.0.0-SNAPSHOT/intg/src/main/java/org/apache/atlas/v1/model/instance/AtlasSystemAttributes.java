/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.v1.model.instance;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasSystemAttributes implements Serializable {
    private static final long serialVersionUID = 1L;

    private String createdBy;
    private String modifiedBy;
    private Date   createdTime;
    private Date   modifiedTime;


    public AtlasSystemAttributes() {
    }

    public AtlasSystemAttributes(AtlasSystemAttributes that) {
        if (that != null) {
            this.createdBy    = that.createdBy;
            this.modifiedBy   = that.modifiedBy;
            this.createdTime  = that.createdTime;
            this.modifiedTime = that.modifiedTime;
        }
    }

    public AtlasSystemAttributes(String createdBy, String modifiedBy, Date createdTime, Date modifiedTime){
        this.createdBy    = createdBy;
        this.modifiedBy   = modifiedBy;
        this.createdTime  = createdTime;
        this.modifiedTime = modifiedTime;
    }

    public AtlasSystemAttributes(Map<String, Object> map) {
        this();

        if (map != null) {
            this.createdBy    = Id.asString(map.get("createdBy"));
            this.modifiedBy   = Id.asString(map.get("modifiedBy"));
            this.createdTime  = Id.asDate(map.get("createdTime"));
            this.modifiedTime = Id.asDate(map.get("modifiedTime"));
        }
    }

    public String getCreatedBy(){
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy(){
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getCreatedTime(){
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getModifiedTime(){
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AtlasSystemAttributes obj = (AtlasSystemAttributes) o;

        return Objects.equals(createdBy, obj.createdBy) &&
               Objects.equals(modifiedBy, obj.modifiedBy) &&
               Objects.equals(createdTime, obj.createdTime) &&
               Objects.equals(modifiedTime, obj.modifiedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdBy, modifiedBy, createdTime, modifiedTime);
    }


    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasSystemAttributes{")
                .append("createdBy=").append(createdBy)
                .append(", modifiedBy=").append(modifiedBy)
                .append(", createdTime=").append(createdTime)
                .append(", modifiedTime=").append(modifiedTime)
                .append("}");

        return sb;
    }
}
