/** Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.atlas.authorize.simple;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasSimpleAuthzPolicy implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, AtlasAuthzRole> roles;
    private Map<String, List<String>>   userRoles;
    private Map<String, List<String>>   groupRoles;


    public Map<String, AtlasAuthzRole> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, AtlasAuthzRole> roles) {
        this.roles = roles;
    }

    public Map<String, List<String>> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Map<String, List<String>> userRoles) {
        this.userRoles = userRoles;
    }

    public Map<String, List<String>> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(Map<String, List<String>> groupRoles) {
        this.groupRoles = groupRoles;
    }


    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasAuthzRole implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<AtlasAdminPermission>        adminPermissions;
        private List<AtlasTypePermission>         typePermissions;
        private List<AtlasEntityPermission>       entityPermissions;
        private List<AtlasRelationshipPermission> relationshipPermissions;

        public AtlasAuthzRole() {
        }

        public AtlasAuthzRole(List<AtlasAdminPermission> adminPermissions, List<AtlasTypePermission> typePermissions, List<AtlasEntityPermission> entityPermissions, List<AtlasRelationshipPermission> relationshipPermissions) {
            this.adminPermissions        = adminPermissions;
            this.typePermissions         = typePermissions;
            this.entityPermissions       = entityPermissions;
            this.relationshipPermissions = relationshipPermissions;
        }

        public List<AtlasAdminPermission> getAdminPermissions() {
            return adminPermissions;
        }

        public void setAdminPermissions(List<AtlasAdminPermission> adminPermissions) {
            this.adminPermissions = adminPermissions;
        }

        public List<AtlasTypePermission> getTypePermissions() {
            return typePermissions;
        }

        public void setTypePermissions(List<AtlasTypePermission> typePermissions) {
            this.typePermissions = typePermissions;
        }

        public List<AtlasEntityPermission> getEntityPermissions() {
            return entityPermissions;
        }

        public void setEntityPermissions(List<AtlasEntityPermission> entityPermissions) {
            this.entityPermissions = entityPermissions;
        }

        public List<AtlasRelationshipPermission> getRelationshipPermissions() {
            return relationshipPermissions;
        }

        public void setRelationshipPermissions(List<AtlasRelationshipPermission> relationshipPermissions) {
            this.relationshipPermissions = relationshipPermissions;
        }

    }

    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasAdminPermission implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<String> privileges; // name of AtlasPrivilege enum, wildcards supported

        public AtlasAdminPermission() {
        }

        public AtlasAdminPermission(List<String> privileges) {
            this.privileges = privileges;
        }

        public List<String> getPrivileges() {
            return privileges;
        }

        public void setPrivileges(List<String> privileges) {
            this.privileges = privileges;
        }
    }

    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasTypePermission implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<String> privileges;     // name of AtlasPrivilege enum, wildcards supported
        private List<String> typeCategories; // category of the type (entity, classification, struct, enum, relationship), wildcards supported
        private List<String> typeNames;      // name of type, wildcards supported

        public AtlasTypePermission() {
        }

        public AtlasTypePermission(List<String> privileges, List<String> typeCategories, List<String> typeNames) {
            this.privileges     = privileges;
            this.typeCategories = typeCategories;
            this.typeNames      = typeNames;
        }

        public List<String> getPrivileges() {
            return privileges;
        }

        public void setPrivileges(List<String> privileges) {
            this.privileges = privileges;
        }

        public List<String> getTypeCategories() {
            return typeCategories;
        }

        public void setTypeCategories(List<String> typeCategories) {
            this.typeCategories = typeCategories;
        }

        public List<String> getTypeNames() {
            return typeNames;
        }

        public void setTypeNames(List<String> typeNames) {
            this.typeNames = typeNames;
        }
    }

    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasEntityPermission implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<String> privileges;            // name of AtlasPrivilege enum, wildcards supported
        private List<String> entityTypes;           // name of entity-type, wildcards supported
        private List<String> entityIds;             // value of entity-unique attribute, wildcards supported
        private List<String> entityClassifications; // name of entity classification-type, wildcards supported
        private List<String> labels;                // labels, wildcards supported
        private List<String> businessMetadata;      // name of business-metadata, wildcards supported
        private List<String> attributes;            // name of entity-attribute, wildcards supported
        private List<String> classifications;       // name of classification-type, wildcards supported

        public AtlasEntityPermission() {
        }

        public AtlasEntityPermission(List<String> privileges, List<String> entityTypes, List<String> entityIds, List<String> classifications, List<String> attributes) {
            this(privileges, entityTypes, entityIds, classifications, attributes, null, null, null);
        }

        public AtlasEntityPermission(List<String> privileges, List<String> entityTypes, List<String> entityIds, List<String> entityClassifications, List<String> labels, List<String> businessMetadata, List<String> attributes, List<String> classifications) {
            this.privileges            = privileges;
            this.entityTypes           = entityTypes;
            this.entityIds             = entityIds;
            this.entityClassifications = entityClassifications;
            this.labels                = labels;
            this.businessMetadata      = businessMetadata;
            this.attributes            = attributes;
            this.classifications       = classifications;
        }

        public List<String> getPrivileges() {
            return privileges;
        }

        public void setPrivileges(List<String> privileges) {
            this.privileges = privileges;
        }

        public List<String> getEntityTypes() {
            return entityTypes;
        }

        public void setEntityTypes(List<String> entityTypes) {
            this.entityTypes = entityTypes;
        }

        public List<String> getEntityIds() {
            return entityIds;
        }

        public void setEntityIds(List<String> entityIds) {
            this.entityIds = entityIds;
        }

        public List<String> getEntityClassifications() {
            return entityClassifications;
        }

        public void setEntityClassifications(List<String> entityClassifications) {
            this.entityClassifications = entityClassifications;
        }

        public List<String> getLabels() {
            return labels;
        }

        public void setLabels(List<String> labels) {
            this.labels = labels;
        }

        public List<String> getBusinessMetadata() {
            return businessMetadata;
        }

        public void setBusinessMetadata(List<String> businessMetadata) {
            this.businessMetadata = businessMetadata;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<String> attributes) {
            this.attributes = attributes;
        }

        public List<String> getClassifications() {
            return classifications;
        }

        public void setClassifications(List<String> classifications) {
            this.classifications = classifications;
        }
    }


    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasRelationshipPermission implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<String> privileges;               // name of AtlasPrivilege enum, wildcards supported
        private List<String> relationshipTypes;        // name of relationship-type, wildcards supported
        private List<String> end1EntityType;           // name of end1 entity-type, wildcards supported
        private List<String> end1EntityId;             // value of end1 entity-unique attribute, wildcards supported
        private List<String> end1EntityClassification; // name of end1 classification-type, wildcards supported
        private List<String> end2EntityType;           // name of end2 entity-type, wildcards supported
        private List<String> end2EntityId;             // value of end2 entity-unique attribute, wildcards supported
        private List<String> end2EntityClassification; // name of end2 classification-type, wildcards supported


        public AtlasRelationshipPermission() {
        }

        public AtlasRelationshipPermission(List<String> privileges, List<String> relationshipTypes, List<String> end1Entitytype, List<String> end1EntityId, List<String> end1EntityClassification, List<String> end2EntityType, List<String> end2EntityId, List<String> end2EntityClassification) {
            this.privileges               = privileges;
            this.relationshipTypes        = relationshipTypes;
            this.end1EntityType           = end1Entitytype;
            this.end1EntityId             = end1EntityId;
            this.end1EntityClassification = end1EntityClassification;
            this.end2EntityType           = end2EntityType;
            this.end2EntityId             = end2EntityId;
            this.end2EntityClassification = end2EntityClassification;
        }

        public List<String> getPrivileges() {
            return privileges;
        }

        public void setPrivileges(List<String> privileges) {
            this.privileges = privileges;
        }

        public List<String> getRelationshipTypes() {
            return relationshipTypes;
        }

        public void setRelationshipTypes(List<String> relationshipTypes) {
            this.relationshipTypes = relationshipTypes;
        }

        public List<String> getEnd1EntityType() {
            return end1EntityType;
        }

        public void setEnd1EntityType(List<String> end1EntityType) {
            this.end1EntityType = end1EntityType;
        }

        public List<String> getEnd1EntityId() {
            return end1EntityId;
        }

        public void setEnd1EntityId(List<String> end1EntityId) {
            this.end1EntityId = end1EntityId;
        }

        public List<String> getEnd1EntityClassification() {
            return end1EntityClassification;
        }

        public void setEnd1EntityClassification(List<String> end1EntityClassification) {
            this.end1EntityClassification = end1EntityClassification;
        }

        public List<String> getEnd2EntityType() {
            return end2EntityType;
        }

        public void setEnd2EntityType(List<String> end2EntityType) {
            this.end2EntityType = end2EntityType;
        }

        public List<String> getEnd2EntityId() {
            return end2EntityId;
        }

        public void setEnd2EntityId(List<String> end2EntityId) {
            this.end2EntityId = end2EntityId;
        }

        public List<String> getEnd2EntityClassification() {
            return end2EntityClassification;
        }

        public void setEnd2EntityClassification(List<String> end2EntityClassification) {
            this.end2EntityClassification = end2EntityClassification;
        }
    }
}
