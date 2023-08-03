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

package org.apache.atlas.authorize;

import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.type.AtlasTypeRegistry;

import java.util.Set;

public class AtlasRelationshipAccessRequest extends AtlasAccessRequest {
    private final AtlasTypeRegistry typeRegistry;
    private final String            relationshipType ;
    private final AtlasEntityHeader end1Entity;
    private final AtlasEntityHeader end2Entity;


    public AtlasRelationshipAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, String relationshipType, AtlasEntityHeader end1Entity, AtlasEntityHeader end2Entity) {
        this(typeRegistry, action, relationshipType, end1Entity, end2Entity, null, null);
    }

    public AtlasRelationshipAccessRequest(AtlasTypeRegistry typeRegistry, AtlasPrivilege action, String relationshipType, AtlasEntityHeader end1Entity, AtlasEntityHeader end2Entity, String user, Set<String> userGroups) {
        super(action, user, userGroups);

        this.typeRegistry     = typeRegistry;
        this.relationshipType = relationshipType;
        this.end1Entity       = end1Entity;
        this.end2Entity       = end2Entity;
    }

    public AtlasEntityHeader getEnd1Entity() {
        return end1Entity;
    }

    public AtlasEntityHeader getEnd2Entity() {
        return end2Entity;
    }

    public String getRelationshipType() {
        return relationshipType;
    }


    public Set<String> getEnd1EntityTypeAndAllSuperTypes() {
        return super.getEntityTypeAndAllSuperTypes(end1Entity == null ? null : end1Entity.getTypeName(), typeRegistry);
    }

    public Set<String> getEnd1EntityClassifications() {
        return super.getClassificationNames(end1Entity);
    }

    public String getEnd1EntityId() {
        return super.getEntityId(end1Entity, typeRegistry);
    }

    public Set<String> getEnd2EntityTypeAndAllSuperTypes() {
        return super.getEntityTypeAndAllSuperTypes(end2Entity == null ? null : end2Entity.getTypeName(), typeRegistry);
    }

    public Set<String> getEnd2EntityClassifications() {
        return super.getClassificationNames(end2Entity);
    }

    public String getEnd2EntityId() {
        return super.getEntityId(end2Entity, typeRegistry);
    }

    public Set<String> getClassificationTypeAndAllSuperTypes(String classificationName) {
        return super.getClassificationTypeAndAllSuperTypes(classificationName, typeRegistry);
    }

    @Override
    public String toString() {
        return "AtlasRelationshipAccessRequest[relationshipType=" + relationshipType + ", end1Entity=" + end1Entity + ", end2Entity=" + end2Entity +
                ", action=" + getAction() + ", accessTime=" + getAccessTime() + ", user=" + getUser() +
                ", userGroups=" + getUserGroups() + ", clientIPAddress=" + getClientIPAddress() +
                ", forwardedAddresses=" + getForwardedAddresses() + ", remoteIPAddress=" + getRemoteIPAddress() + "]";
    }
}
