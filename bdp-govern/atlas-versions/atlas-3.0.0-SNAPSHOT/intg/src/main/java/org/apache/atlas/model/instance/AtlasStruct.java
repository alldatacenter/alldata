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
package org.apache.atlas.model.instance;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.atlas.model.PList;
import org.apache.atlas.model.SearchFilter.SortType;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * Captures details of struct contents. Not instantiated directly, used only via AtlasEntity, AtlasClassification.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String KEY_TYPENAME   = "typeName";
    public static final String KEY_ATTRIBUTES = "attributes";

    public static final String     SERIALIZED_DATE_FORMAT_STR = "yyyyMMdd-HH:mm:ss.SSS-Z";
    @Deprecated
    public static final DateFormat DATE_FORMATTER             = new SimpleDateFormat(SERIALIZED_DATE_FORMAT_STR);

    private String              typeName;
    private Map<String, Object> attributes;

    public AtlasStruct() {
        this(null, null);
    }

    public AtlasStruct(String typeName) {
        this(typeName, null);
    }

    public AtlasStruct(String typeName, Map<String, Object> attributes) {
        setTypeName(typeName);
        setAttributes(attributes);
    }

    public AtlasStruct(String typeName, String attrName, Object attrValue) {
        setTypeName(typeName);
        setAttribute(attrName, attrValue);
    }

    public AtlasStruct(Map map) {
        if (map != null) {
            Object typeName   = map.get(KEY_TYPENAME);
            Map    attributes = (map.get(KEY_ATTRIBUTES) instanceof Map) ? (Map) map.get(KEY_ATTRIBUTES) : map;

            if (typeName != null) {
                setTypeName(typeName.toString());
            }

            setAttributes(new HashMap<>(attributes));
        }
    }

    public AtlasStruct(AtlasStruct other) {
        if (other != null) {
            setTypeName(other.getTypeName());
            setAttributes(new HashMap<>(other.getAttributes()));
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean hasAttribute(String name) {
        Map<String, Object> a = this.attributes;

        return a != null ? a.containsKey(name) : false;
    }

    public Object getAttribute(String name) {
        Map<String, Object> a = this.attributes;

        return a != null ? a.get(name) : null;
    }

    public void setAttribute(String name, Object value) {
        Map<String, Object> a = this.attributes;

        if (a != null) {
            a.put(name, value);
        } else {
            a = new HashMap<>();
            a.put(name, value);

            this.attributes = a;
        }
    }

    public Object removeAttribute(String name) {
        Map<String, Object> a = this.attributes;

        return a != null ? a.remove(name) : null;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasStruct{");
        sb.append("typeName='").append(typeName).append('\'');
        sb.append(", attributes=[");
        dumpObjects(attributes, sb);
        sb.append("]");
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtlasStruct that = (AtlasStruct) o;

        return Objects.equals(typeName, that.typeName) &&
               Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, attributes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }


    /**
     * REST serialization friendly list.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlSeeAlso(AtlasStruct.class)
    public static class AtlasStructs extends PList<AtlasStruct> {
        private static final long serialVersionUID = 1L;

        public AtlasStructs() {
            super();
        }

        public AtlasStructs(List<AtlasStruct> list) {
            super(list);
        }

        public AtlasStructs(List list, long startIndex, int pageSize, long totalCount,
                            SortType sortType, String sortBy) {
            super(list, startIndex, pageSize, totalCount, sortType, sortBy);
        }
    }


    public static StringBuilder dumpModelObjects(Collection<? extends AtlasStruct> objList, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (CollectionUtils.isNotEmpty(objList)) {
            int i = 0;
            for (AtlasStruct obj : objList) {
                if (i > 0) {
                    sb.append(", ");
                }

                obj.toString(sb);
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpObjects(Collection<?> objects, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (CollectionUtils.isNotEmpty(objects)) {
            int i = 0;
            for (Object obj : objects) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(obj);
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpObjects(Map<?, ?> objects, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (MapUtils.isNotEmpty(objects)) {
            int i = 0;
            for (Map.Entry<?, ?> e : objects.entrySet()) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(e.getKey()).append(":").append(e.getValue());
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpDateField(String prefix, Date value, StringBuilder sb) {
        sb.append(prefix);

        if (value == null) {
            sb.append(value);
        } else {
            sb.append(AtlasBaseTypeDef.getDateFormatter().format(value));
        }

        return sb;
    }
}
