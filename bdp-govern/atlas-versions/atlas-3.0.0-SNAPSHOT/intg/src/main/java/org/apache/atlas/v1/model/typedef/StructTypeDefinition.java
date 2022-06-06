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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class StructTypeDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String                    typeName;
    private String                    typeDescription;
    private String                    typeVersion;
    private List<AttributeDefinition> attributeDefinitions;


    public StructTypeDefinition() {
    }

    public StructTypeDefinition(String typeName, String typeDescription, List<AttributeDefinition> attributeDefinitions) {
        this(typeName, typeDescription, "1.0", attributeDefinitions);
    }

    public StructTypeDefinition(String typeName, String typeDescription, String typeVersion, List<AttributeDefinition> attributeDefinitions) {
        this.typeName             = typeName;
        this.typeDescription      = typeDescription;
        this.typeVersion          = typeVersion;
        this.attributeDefinitions = attributeDefinitions;
    }


    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getTypeVersion() {
        return typeVersion;
    }

    public void setTypeVersion(String typeVersion) {
        this.typeVersion = typeVersion;
    }

    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
        this.attributeDefinitions = attributeDefinitions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StructTypeDefinition that = (StructTypeDefinition) o;

        return Objects.equals(typeName, that.typeName) &&
               Objects.equals(typeDescription, that.typeDescription) &&
               Objects.equals(typeVersion, that.typeVersion) &&
               Objects.equals(attributeDefinitions, that.attributeDefinitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, typeDescription, typeVersion, attributeDefinitions);
    }
}
