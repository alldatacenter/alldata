/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.model;

import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AuditFilter {
    public enum AccessResult { DENIED, ALLOWED, NOT_DETERMINED }

    private AccessResult                      accessResult;
    private Map<String, RangerPolicyResource> resources;
    private List<String>                      accessTypes;
    private List<String>                      actions;
    private List<String>                      users;
    private List<String>                      groups;
    private List<String>                      roles;
    private Boolean                           isAudited;

    public AuditFilter() { }

    public AccessResult getAccessResult() {
        return accessResult;
    }

    public void setAccessResult(AccessResult accessResult) {
        this.accessResult = accessResult;
    }

    public Map<String, RangerPolicyResource> getResources() {
        return resources;
    }

    public void setResources(Map<String, RangerPolicyResource> resources) {
        this.resources = resources;
    }

    public List<String> getAccessTypes() {
        return accessTypes;
    }

    public void setAccessTypes(List<String> accessTypes) {
        this.accessTypes = accessTypes;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Boolean getIsAudited() {
        return isAudited;
    }

    public void setAction(Boolean isAudited) {
        this.isAudited = isAudited;
    }

    @Override
    public String toString() {
        return "{accessResult=" + accessResult
                + ", resources=" + resources
                + ", accessTypes=" + accessTypes
                + ", actions=" + actions
                + ", users=" + users
                + ", groups=" + groups
                + ", roles=" + roles
                + ", isAudited=" + isAudited
                + "}";
    }
}