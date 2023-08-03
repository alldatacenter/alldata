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
package org.apache.atlas.model.typedef;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.atlas.model.PList;
import org.apache.atlas.model.SearchFilter.SortType;
import org.apache.atlas.model.TypeCategory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.util.StringUtils;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * class that captures details of an enum-type.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasEnumDef extends AtlasBaseTypeDef implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<AtlasEnumElementDef> elementDefs;
    private String                    defaultValue;

    public AtlasEnumDef() {
        this(null, null, null, null, null, null);
    }

    public AtlasEnumDef(String name) {
        this(name, null, null, null, null, null);
    }

    public AtlasEnumDef(String name, String description) {
        this(name, description, null, null, null, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion) {
        this(name, description, typeVersion, null, null, null);
    }

    public AtlasEnumDef(String name, String description, List<AtlasEnumElementDef> elementDefs) {
        this(name, description, null, elementDefs, null, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumElementDef> elementDefs) {
        this(name, description, typeVersion, elementDefs, null, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumElementDef> elementDefs,
                        String defaultValue) {
        this(name, description, typeVersion, elementDefs, defaultValue, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumElementDef> elementDefs,
                        String defaultValue, Map<String, String> options) {
        this(name, description, typeVersion, elementDefs, defaultValue, null, options);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumElementDef> elementDefs,
                        String defaultValue, String serviceType, Map<String, String> options) {
        super(TypeCategory.ENUM, name, description, typeVersion, serviceType, options);

        setElementDefs(elementDefs);
        setDefaultValue(defaultValue);
    }

    public AtlasEnumDef(AtlasEnumDef other) {
        super(other);

        if (other != null) {
            setElementDefs(other.getElementDefs());
            setDefaultValue(other.getDefaultValue());
        }
    }

    public List<AtlasEnumElementDef> getElementDefs() {
        return elementDefs;
    }

    public void setElementDefs(List<AtlasEnumElementDef> elementDefs) {
        if (elementDefs != null && this.elementDefs == elementDefs) {
            return;
        }

        if (CollectionUtils.isEmpty(elementDefs)) {
            this.elementDefs = new ArrayList<>();
        } else {
            // if multiple elements with same value are present, keep only the last entry
            List<AtlasEnumElementDef> tmpList       = new ArrayList<>(elementDefs.size());
            Set<String>               elementValues = new HashSet<>();

            ListIterator<AtlasEnumElementDef> iter = elementDefs.listIterator(elementDefs.size());
            while (iter.hasPrevious()) {
                AtlasEnumElementDef elementDef   = iter.previous();
                String              elementValue = elementDef != null ? elementDef.getValue() : null;

                if (elementValue != null) {
                    elementValue = elementValue.toLowerCase();

                    if (!elementValues.contains(elementValue)) {
                        tmpList.add(new AtlasEnumElementDef(elementDef));

                        elementValues.add(elementValue);
                    }
                }
            }
            Collections.reverse(tmpList);

            this.elementDefs = tmpList;
        }
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String value) {
        this.defaultValue = value;
    }

    public AtlasEnumElementDef getElement(String elemValue) {
        return findElement(this.elementDefs, elemValue);
    }

    public void addElement(AtlasEnumElementDef elementDef) {
        List<AtlasEnumElementDef> e = this.elementDefs;

        List<AtlasEnumElementDef> tmpList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(e)) {
            // copy existing elements, except ones having same value as the element being added
            for (AtlasEnumElementDef existingElem : e) {
                if (!StringUtils.equalsIgnoreCase(existingElem.getValue(), elementDef.getValue())) {
                    tmpList.add(existingElem);
                }
            }
        }
        tmpList.add(new AtlasEnumElementDef(elementDef));

        this.elementDefs = tmpList;
    }

    public void removeElement(String elemValue) {
        List<AtlasEnumElementDef> e = this.elementDefs;

        // if element doesn't exist, no need to create the tmpList below
        if (hasElement(e, elemValue)) {
            List<AtlasEnumElementDef> tmpList = new ArrayList<>();

            // copy existing elements, except ones having same value as the element being removed
            for (AtlasEnumElementDef existingElem : e) {
                if (!StringUtils.equalsIgnoreCase(existingElem.getValue(), elemValue)) {
                    tmpList.add(existingElem);
                }
            }

            this.elementDefs = tmpList;
        }
    }

    public boolean hasElement(String elemValue) {
        return getElement(elemValue) != null;
    }

    private static boolean hasElement(List<AtlasEnumElementDef> elementDefs, String elemValue) {
        return findElement(elementDefs, elemValue) != null;
    }

    private static AtlasEnumElementDef findElement(List<AtlasEnumElementDef> elementDefs, String elemValue) {
        AtlasEnumElementDef ret = null;

        if (CollectionUtils.isNotEmpty(elementDefs)) {
            for (AtlasEnumElementDef elementDef : elementDefs) {
                if (StringUtils.equalsIgnoreCase(elementDef.getValue(), elemValue)) {
                    ret = elementDef;
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasEnumDef{");
        super.toString(sb);
        sb.append(", elementDefs=[");
        dumpObjects(elementDefs, sb);
        sb.append("]");
        sb.append(", defaultValue {");
        sb.append(defaultValue);
        sb.append('}');
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AtlasEnumDef that = (AtlasEnumDef) o;
        return Objects.equals(elementDefs, that.elementDefs) &&
                Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elementDefs, defaultValue);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }


    /**
     * class that captures details of an enum-element.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasEnumElementDef implements Serializable {
        private static final long serialVersionUID = 1L;

        private String  value;
        private String  description;
        private Integer ordinal;

        public AtlasEnumElementDef() {
            this(null, null, null);
        }

        public AtlasEnumElementDef(String value, String description, Integer ordinal) {
            setValue(value);
            setDescription(description);
            setOrdinal(ordinal);
        }

        public AtlasEnumElementDef(AtlasEnumElementDef other) {
            if (other != null) {
                setValue(other.getValue());
                setDescription(other.getDescription());
                setOrdinal(other.getOrdinal());
            }
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(Integer ordinal) {
            this.ordinal = ordinal;
        }

        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("AtlasEnumElementDef{");
            sb.append("value='").append(value).append('\'');
            sb.append(", description='").append(description).append('\'');
            sb.append(", ordinal=").append(ordinal);
            sb.append('}');

            return sb;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AtlasEnumElementDef that = (AtlasEnumElementDef) o;
            return Objects.equals(value, that.value) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(ordinal, that.ordinal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, description, ordinal);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }


    /**
     * REST serialization friendly list.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlSeeAlso(AtlasEnumDef.class)
    public static class AtlasEnumDefs extends PList<AtlasEnumDef> {
        private static final long serialVersionUID = 1L;

        public AtlasEnumDefs() {
            super();
        }

        public AtlasEnumDefs(List<AtlasEnumDef> list) {
            super(list);
        }

        public AtlasEnumDefs(List list, long startIndex, int pageSize, long totalCount,
                             SortType sortType, String sortBy) {
            super(list, startIndex, pageSize, totalCount, sortType, sortBy);
        }
    }
}
