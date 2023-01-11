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

package org.apache.ranger.plugin.util;

import org.apache.ranger.plugin.model.RangerRole;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerRoles implements Serializable {
    private static final long serialVersionUID = 1L;

    private String           serviceName;
    private Long             roleVersion;
    private Date             roleUpdateTime;
    private Set<RangerRole>  rangerRoles;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getRoleVersion() {
        return roleVersion;
    }

    public void setRoleVersion(Long roleVersion) {
        this.roleVersion = roleVersion;
    }

    public Date getRoleUpdateTime() {
        return roleUpdateTime;
    }

    public void setRoleUpdateTime(Date roleUpdateTime) {
        this.roleUpdateTime = roleUpdateTime;
    }

    public Set<RangerRole> getRangerRoles(){
        return this.rangerRoles;
    }

    public void setRangerRoles(Set<RangerRole> rangerRoles){
        this.rangerRoles = rangerRoles;
    }
}
