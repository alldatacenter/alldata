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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Referenceable extends Struct implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String JSON_CLASS_REFERENCE = "org.apache.atlas.typesystem.json.InstanceSerialization$_Reference";

    private Id                    id;
    private Map<String, Struct>   traits     = new HashMap<>();
    private List<String>          traitNames = new ArrayList<>();
    private AtlasSystemAttributes systemAttributes;


    public Referenceable() {
        super();
    }

    public Referenceable(Referenceable that) {
        super(that);

        if (that != null) {
            this.id = new Id(that.id);

            if (that.traits != null) {
                this.traits.putAll(that.traits);
            }

            if (that.traitNames != null) {
                this.traitNames.addAll(that.traitNames);
            }

            this.systemAttributes = new AtlasSystemAttributes(that.systemAttributes);
        }
    }

    public Referenceable(String typeName, String... traitNames) {
        super(typeName);

        this.id               = new Id(typeName);
        this.systemAttributes = null;

        if (traitNames != null) {
            for (String traitName : traitNames) {
                this.traitNames.add(traitName);
                this.traits.put(traitName, new Struct(traitName));
            }
        }
    }

    public Referenceable(String typeName, Map<String, Object> values) {
        this(new Id(typeName), typeName, values, null, null);
    }

    public Referenceable(String guid, String typeName, Map<String, Object> values) {
        this(new Id(guid, 0, typeName), typeName, values, null, null, null);
    }

    public Referenceable(String guid, String typeName, Map<String, Object> values, AtlasSystemAttributes systemAttributes) {
        this(new Id(guid, 0, typeName), typeName, values, systemAttributes, null, null);
    }

    public Referenceable(String guid, String typeName, String state, Map<String, Object> values, AtlasSystemAttributes systemAttributes) {
        this(new Id(guid, 0, typeName, state), typeName, values, systemAttributes, null, null);
    }

    public Referenceable(String guid, String typeName, Map<String, Object> values, AtlasSystemAttributes systemAttributes, List<String> traitNames, Map<String, Struct> traits) {
        this(new Id(guid, 0, typeName), typeName, values, systemAttributes, traitNames, traits);
    }

    public Referenceable(String guid, String typeName, Map<String, Object> values, List<String> traitNames, Map<String, Struct> traits) {
        this(new Id(guid, 0, typeName), typeName, values, null, traitNames, traits);
    }

    public Referenceable(Id id, String typeName, Map<String, Object> values, List<String> traitNames, Map<String, Struct> traits) {
        this(id, typeName, values, null, traitNames, traits);
    }

    public Referenceable(Id id, String typeName, Map<String, Object> values, AtlasSystemAttributes systemAttributes, List<String> traitNames, Map<String, Struct> traits) {
        super(typeName, values);

        this.id               = id;
        this.systemAttributes = systemAttributes;

        if (traitNames != null) {
            this.traitNames = traitNames;
        }

        if (traits != null) {
            this.traits = traits;
        }
    }

    public Referenceable(Map<String, Object> map) {
        super(map);

        if (map != null) {
            this.id               = new Id((Map)map.get("id"));
            this.traitNames       = Id.asList(map.get("traitNames"));
            this.systemAttributes = new AtlasSystemAttributes((Map) map.get("systemAttributes"));

            Map traits = Id.asMap(map.get("traits"));

            if (MapUtils.isNotEmpty(traits)) {
                this.traits = new HashMap<>(traits.size());

                for (Object key : traits.keySet()) {
                    this.traits.put(Id.asString(key), new Struct(Id.asMap(traits.get(key))));
                }
            }
        }
    }


    // for serialization backward compatibility
    public String getJsonClass() {
        return JSON_CLASS_REFERENCE;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Map<String, Struct> getTraits() {
        return traits;
    }

    public void setTraits(Map<String, Struct> traits) {
        this.traits = traits;
    }

    public List<String> getTraitNames() {
        return traitNames;
    }

    public void setTraitNames(List<String> traitNames) {
        this.traitNames = traitNames;
    }

    public AtlasSystemAttributes getSystemAttributes() {
        return systemAttributes;
    }

    public void setSystemAttributes(AtlasSystemAttributes systemAttributes) {
        this.systemAttributes = systemAttributes;
    }

    @JsonIgnore
    public Struct getTrait(String name) {
        return traits != null ? traits.get(name) : null;
    }

    @JsonIgnore
    public String toShortString() {
        return String.format("entity[type=%s guid=%s]", getTypeName(), id._getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        Referenceable obj = (Referenceable)o;

        return Objects.equals(id, obj.id) &&
               Objects.equals(traits, obj.traits) &&
               Objects.equals(traitNames, obj.traitNames) &&
               Objects.equals(systemAttributes, obj.systemAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, traits, traitNames, systemAttributes);
    }


    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("Referenceable{");
        super.toString(sb);
        sb.append(", id=");
        if (id != null) {
            id.asString(sb);
        }
        sb.append(", triats={");
        AtlasBaseTypeDef.dumpObjects(this.traits, sb);
        sb.append("}, traitNames=[");
        AtlasBaseTypeDef.dumpObjects(traitNames, sb);
        sb.append("], systemAttributes=");
        if (systemAttributes != null) {
            systemAttributes.toString(sb);
        }
        sb.append("}");

        return sb;
    }
}
