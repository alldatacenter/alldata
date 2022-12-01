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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.commons.collections.MapUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Struct implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String JSON_CLASS_STRUCT = "org.apache.atlas.typesystem.json.InstanceSerialization$_Struct";

    private String              typeName;
    private Map<String, Object> values;


    public Struct() {
    }

    public Struct(Struct that) {
        if (that != null) {
            this.typeName = that.typeName;

            if (that.values != null) {
                this.values = new HashMap<>(that.values);
            }
        }
    }

    public Struct(String typeName) {
        this(typeName, null);
    }

    public Struct(String typeName, Map<String, Object> values) {
        this.typeName = typeName;
        this.values   = values;
    }

    public Struct(Map<String, Object> map) {
        this();

        if (map != null) {
            this.typeName = Id.asString(map.get("typeName"));
            this.values   = Id.asMap(map.get("values"));

            this.normalize();
        }
    }

    // for serialization backward compatibility
    public String getJsonClass() {
        return JSON_CLASS_STRUCT;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    @JsonIgnore
    public Map<String, Object> getValuesMap() {
        return values;
    }

    @JsonIgnore
    public void set(String attrName, Object attrValue) {
        if (values == null) {
            values = new HashMap<>();
        }

        values.put(attrName, attrValue);
    }

    @JsonIgnore
    public Object get(String attrName) {
        return values != null ? values.get(attrName) : null;
    }

    public void normalize() {
        if (MapUtils.isEmpty(values)) {
            return;
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            entry.setValue(normalizeAttributeValue(entry.getValue()));
        }
    }

    private Object normalizeAttributeValue(Object value) {
        if (value instanceof Map) {
            Map    mapValue  = (Map) value;
            String jsonClass = (String)mapValue.get("jsonClass");

            if (jsonClass != null) {
                if (Id.JSON_CLASS_ID.equals(jsonClass)) {
                    value = new Id(mapValue);
                } else if (Struct.JSON_CLASS_STRUCT.equals(jsonClass)) {
                    value = new Struct(mapValue);
                } else if (Referenceable.JSON_CLASS_REFERENCE.equals(jsonClass)) {
                    value = new Referenceable(mapValue);
                }
            }
        } else if (value instanceof List) {
            List<Object> listValue       = (List) value;
            List<Object> normalizedValue = new ArrayList<>(listValue.size());

            for (Object val : listValue) {
                normalizedValue.add(normalizeAttributeValue(val));
            }

            value = normalizedValue;
        }

        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        Struct obj = (Struct)o;

        return Objects.equals(typeName, obj.typeName) &&
               Objects.equals(values, obj.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, values);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("Struct{");
        sb.append("typeName=").append(typeName);
        sb.append(", values={");
        AtlasBaseTypeDef.dumpObjects(values, sb);
        sb.append("}");
        sb.append("}");

        return sb;
    }
}
