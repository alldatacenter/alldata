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

import org.apache.ranger.authorization.utils.StringUtil;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Represents a TAG definition known to Ranger. In general, this will be provided
 * by some external system identified by 'source'.
 *
 */
@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerTagDef extends RangerBaseModelObject {
    private static final long serialVersionUID = 1L;

    private String name;
    private String source;

    private List<RangerTagAttributeDef> attributeDefs;

    public RangerTagDef() {
        this(null, "Internal");
    }

    public RangerTagDef(String name) {
        this(name, "Internal");
    }

    public RangerTagDef(String name, String source) {
        super();
        setName(name);
        setSource(source);
        setAttributeDefs(null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {

        this.name = name == null ? "" : name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source =  source == null ? "" : source;
    }

    public List<RangerTagAttributeDef> getAttributeDefs() {
        return attributeDefs;
    }

    public void setAttributeDefs(List<RangerTagAttributeDef> attributeDefs) {
        this.attributeDefs = attributeDefs;
    }

    public void dedupStrings(Map<String, String> strTbl) {
        super.dedupStrings(strTbl);

        name   = StringUtil.dedupString(name, strTbl);
        source = StringUtil.dedupString(source, strTbl);

        if (attributeDefs != null) {
            for (RangerTagAttributeDef attributeDef : attributeDefs) {
                attributeDef.dedupStrings(strTbl);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, source, attributeDefs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }

        RangerTagDef other = (RangerTagDef) obj;

        return Objects.equals(name, other.name) &&
               Objects.equals(source, other.source) &&
               Objects.equals(attributeDefs, other.attributeDefs);
    }

    /**
     * Represents one attribute for a TAG. TAG-Attribute consists of a name and type.
     * name provides a handle for possible specification of additional information
     * associated with the TAG.
     * Interpretation of type is up to the policy-engine.
     */

    @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)

    public static class RangerTagAttributeDef implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String type;

        public RangerTagAttributeDef() {
            this(null, null);
        }

        public RangerTagAttributeDef(String name, String type) {
            setName(name);
            setType(type);
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public void setName(String name) {
            this.name = name == null ? "" : name;
        }
        public void setType(String type) {
            this.type = type == null ? "" : type;
        }

        public void dedupStrings(Map<String, String> strTbl) {
            name = StringUtil.dedupString(name, strTbl);
            type = StringUtil.dedupString(type, strTbl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }

            RangerTagAttributeDef other = (RangerTagAttributeDef) obj;

            return Objects.equals(name, other.name) &&
                   Objects.equals(type, other.type);
        }
    }
}
