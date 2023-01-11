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

import org.apache.commons.collections.MapUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerRole extends RangerBaseModelObject implements java.io.Serializable {
    public static final String KEY_USER = "user";
    public static final String KEY_GROUP = "group";

    private static final long serialVersionUID = 1L;
    private String                  name;
    private String                  description;
    private Map<String, Object>     options;
    private List<RoleMember>        users;
    private List<RoleMember>        groups;
    private List<RoleMember>        roles;
    private String createdByUser;

    public RangerRole() {
        this(null, null, null, null, null, null);
    }

    public RangerRole(String name, String description, Map<String, Object> options, List<RoleMember> users, List<RoleMember> groups) {
        this(name, description, options, users, groups, null);
    }

    public RangerRole(String name, String description, Map<String, Object> options, List<RoleMember> users, List<RoleMember> groups, List<RoleMember> roles) {
        setName(name);
        setDescription(description);
        setOptions(options);
        setUsers(users);
        setGroups(groups);
        setRoles(roles);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options == null ? new HashMap<>() : options;
    }

    public List<RoleMember> getUsers() {
        return users;
    }

    public void setUsers(List<RoleMember> users) {
        this.users = users == null ? new ArrayList<>() : users;
    }

    public List<RoleMember> getGroups() {
        return groups;
    }

    public void setGroups(List<RoleMember> groups) {
        this.groups = groups == null ? new ArrayList<>() : groups;
    }

    public List<RoleMember> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleMember> roles) {
        this.roles = roles == null ? new ArrayList<>() : roles;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    @Override
    public String toString() {
        return "{name=" + name
                + ", description=" + description
                + ", options=" + getPrintableOptions(options)
                + ", users=" + users
                + ", groups=" + groups
                + ", roles=" + roles
                + ", createdByUser=" + createdByUser
                + "}";
    }

    @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoleMember implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String  name;
        private boolean isAdmin;

        public RoleMember() {
            this(null, false);
        }
        public RoleMember(String name, boolean isAdmin) {
            setName(name);
            setIsAdmin(isAdmin);
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setIsAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
        }
        public String getName() { return name; }
        public boolean getIsAdmin() { return isAdmin; }

        @Override
        public String toString() {
            return "{" + name + ", " + isAdmin + "}";
        }
        @Override
        public int hashCode() {
            return Objects.hash(name, isAdmin);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RoleMember other = (RoleMember) obj;
            return Objects.equals(name, other.name) && isAdmin == other.isAdmin;
        }
    }

    private String getPrintableOptions(Map<String, Object> options) {
        if (MapUtils.isEmpty(options)) return "{}";
        StringBuilder ret = new StringBuilder();
        ret.append("{");
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            ret.append(entry.getKey()).append(", ").append("[").append(entry.getValue()).append("]").append(",");
        }
        ret.append("}");
        return ret.toString();
    }

}