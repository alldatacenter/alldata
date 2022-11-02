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

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TypesDef implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<EnumTypeDefinition>   enumTypes;
    private List<StructTypeDefinition> structTypes;
    private List<TraitTypeDefinition>  traitTypes;
    private List<ClassTypeDefinition>  classTypes;


    public TypesDef() {
    }

    public TypesDef(List<EnumTypeDefinition> enumTypes, List<StructTypeDefinition> structTypes, List<TraitTypeDefinition> traitTypes, List<ClassTypeDefinition> classTypes) {
        this.enumTypes   = enumTypes;
        this.structTypes = structTypes;
        this.traitTypes  = traitTypes;
        this.classTypes  = classTypes;
    }


    public List<EnumTypeDefinition> getEnumTypes() {
        return enumTypes;
    }

    public void setEnumTypes(List<EnumTypeDefinition> enumTypes) {
        this.enumTypes = enumTypes;
    }

    public List<StructTypeDefinition> getStructTypes() {
        return structTypes;
    }

    public void setStructTypes(List<StructTypeDefinition> structTypes) {
        this.structTypes = structTypes;
    }

    public List<TraitTypeDefinition> getTraitTypes() {
        return traitTypes;
    }

    public void setTraitTypes(List<TraitTypeDefinition> traitTypes) {
        this.traitTypes = traitTypes;
    }

    public List<ClassTypeDefinition> getClassTypes() {
        return classTypes;
    }

    public void setClassTypes(List<ClassTypeDefinition> classTypes) {
        this.classTypes = classTypes;
    }


    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("TypesDef{");
        sb.append("enumTypes=[");
        AtlasBaseTypeDef.dumpObjects(enumTypes, sb);
        sb.append("], structTypes=[");
        AtlasBaseTypeDef.dumpObjects(structTypes, sb);
        sb.append("], traitTypes=[");
        AtlasBaseTypeDef.dumpObjects(traitTypes, sb);
        sb.append("], classTypes=[");
        AtlasBaseTypeDef.dumpObjects(classTypes, sb);
        sb.append("]");
        sb.append("}");

        return sb;
    }
}
