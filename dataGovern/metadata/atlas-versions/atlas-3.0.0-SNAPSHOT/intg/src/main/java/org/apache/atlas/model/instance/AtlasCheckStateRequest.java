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

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * Request to run state-check of entities
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasCheckStateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> entityGuids;
    private Set<String> entityTypes;
    private boolean     fixIssues;


    public AtlasCheckStateRequest() {
    }

    public Set<String> getEntityGuids() {
        return entityGuids;
    }

    public void setEntityGuids(Set<String> entityGuids) {
        this.entityGuids = entityGuids;
    }

    public Set<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(Set<String> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public boolean getFixIssues() {
        return fixIssues;
    }

    public void setFixIssues(boolean fixIssues) {
        this.fixIssues = fixIssues;
    }


    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasCheckStateRequest{");
        sb.append("entityGuids=[");
        AtlasBaseTypeDef.dumpObjects(entityGuids, sb);
        sb.append("], entityTypes=[");
        AtlasBaseTypeDef.dumpObjects(entityTypes, sb);
        sb.append("]");
        sb.append(", fixIssues=").append(fixIssues);
        sb.append("}");

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
