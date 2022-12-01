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

package org.apache.atlas.v1.model.typedef;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.typedef.AtlasStructDef;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.DEFAULT_SEARCHWEIGHT;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AttributeDefinition implements Serializable {
    private static final long                          serialVersionUID = 1L;
    private String                                     name;
    private String                                     dataTypeName;
    private Multiplicity                               multiplicity;
    private boolean                                    isComposite; // A composite is the one whose lifecycle is dependent on the enclosing type and is not just a reference
    private boolean                                    isUnique;
    private boolean                                    isIndexable;
    private String                                     reverseAttributeName; // If this is a reference attribute, then the name of the attribute on the Class that this refers to.
    private String                                     defaultValue;
    private String                                     description;
    private Map<String, String>                        options;
    private int                                        searchWeight = DEFAULT_SEARCHWEIGHT;
    private AtlasStructDef.AtlasAttributeDef.IndexType indexType    = null;

    public AttributeDefinition() {
    }

    public AttributeDefinition(String name, String dataTypeName, Multiplicity multiplicity) {
        this(name, dataTypeName, multiplicity, false, false, true, null, null, DEFAULT_SEARCHWEIGHT, null);
    }

    public AttributeDefinition(String name, String dataTypeName, Multiplicity multiplicity, boolean isComposite,
                               String reverseAttributeName) {
        this(name, dataTypeName, multiplicity, isComposite, reverseAttributeName,  DEFAULT_SEARCHWEIGHT, null);
    }

    public AttributeDefinition(String name, String dataTypeName, Multiplicity multiplicity, boolean isComposite,
                               String reverseAttributeName, int searchWeight, AtlasStructDef.AtlasAttributeDef.IndexType indexType) {
        this(name, dataTypeName, multiplicity, isComposite, false, false, reverseAttributeName, null, searchWeight, indexType);
    }

    public AttributeDefinition(String name, String dataTypeName, Multiplicity multiplicity, boolean isComposite,
                               boolean isUnique, boolean isIndexable, String reverseAttributeName,
                               Map<String, String> options) {
        this(name, dataTypeName, multiplicity, isComposite, isUnique, isIndexable,reverseAttributeName, options, DEFAULT_SEARCHWEIGHT, null);
    }

    public AttributeDefinition(String name, String dataTypeName, Multiplicity multiplicity, boolean isComposite,
                               boolean isUnique, boolean isIndexable, String reverseAttributeName,
                               Map<String, String> options, int searchWeight, AtlasStructDef.AtlasAttributeDef.IndexType indexType) {
        this.name                 = name;
        this.dataTypeName         = dataTypeName;
        this.multiplicity         = multiplicity;
        this.isComposite          = isComposite;
        this.isUnique             = isUnique;
        this.isIndexable          = isIndexable;
        this.reverseAttributeName = reverseAttributeName;
        this.options              = options;
        this.searchWeight         = searchWeight;
        this.indexType            = indexType;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataTypeName() {
        return dataTypeName;
    }

    public void setDataTypeName(String dataTypeName) {
        this.dataTypeName = dataTypeName;
    }

    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(Multiplicity multiplicity) {
        this.multiplicity = multiplicity;
    }

    public boolean getIsComposite() {
        return isComposite;
    }

    public void setIsComposite(boolean isComposite) {
        this.isComposite = isComposite;
    }

    public boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(boolean isUnique) {
        this.isUnique = isUnique;
    }

    public boolean getIsIndexable() {
        return isIndexable;
    }

    public void setIsIndexable(boolean isIndexable) {
        this.isIndexable = isIndexable;
    }

    public String getReverseAttributeName() {
        return reverseAttributeName;
    }

    public void setReverseAttributeName(String reverseAttributeName) {
        this.reverseAttributeName = reverseAttributeName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        if (options != null) {
            this.options = new HashMap<>(options);
        } else {
            this.options = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AttributeDefinition that = (AttributeDefinition) o;

        return isComposite == that.isComposite &&
               isUnique == that.isUnique &&
               isIndexable == that.isIndexable &&
               Objects.equals(name, that.name) &&
               Objects.equals(dataTypeName, that.dataTypeName) &&
               Objects.equals(multiplicity, that.multiplicity) &&
               Objects.equals(defaultValue, that.defaultValue) &&
               Objects.equals(description, that.description) &&
               Objects.equals(reverseAttributeName, that.reverseAttributeName) &&
               Objects.equals(options, that.options) &&
               Objects.equals(searchWeight, that.searchWeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataTypeName, multiplicity, isComposite, isUnique, isIndexable,
                            reverseAttributeName, defaultValue, description, options, searchWeight);
    }

  public void setSearchWeight(int searchWeight) {
        this.searchWeight = searchWeight;
  }

  public int getSearchWeight() {
        return searchWeight;
  }

  public void setIndexType(AtlasStructDef.AtlasAttributeDef.IndexType indexType) {
        this.indexType = indexType;
  }

  public AtlasStructDef.AtlasAttributeDef.IndexType getIndexType() {
        return this.indexType;
  }
}
