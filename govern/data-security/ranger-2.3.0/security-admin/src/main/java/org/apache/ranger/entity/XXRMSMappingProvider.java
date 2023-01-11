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

package org.apache.ranger.entity;

import org.apache.ranger.common.AppConstants;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Cacheable
@Table(name="x_rms_mapping_provider")
@XmlRootElement
public class XXRMSMappingProvider implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "X_RMS_MAPPING_PROVIDER_SEQ", sequenceName = "X_RMS_MAPPING_PROVIDER_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "X_RMS_MAPPING_PROVIDER_SEQ")
    @Column(name = "id")
    protected Long id;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="change_timestamp")
    protected Date changeTimestamp;
    public Date getChangeTimestamp() {
        return changeTimestamp;
    }
    public void setChangeTimestamp(Date changeTimestamp) {
        this.changeTimestamp = changeTimestamp;
    }

    @Column(name = "name")
    protected String name;

    @Column(name = "last_known_version")
    protected Long lastKnownVersion;

    public XXRMSMappingProvider() {}

    public XXRMSMappingProvider(String name) {
        setName(name);
        setLastKnownVersion(-1L);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the serviceId to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the resourceSignature
     */
    public Long getLastKnownVersion() {
        return lastKnownVersion;
    }

    /**
     * @param lastKnownVersion
     *            the lastKnownVersion to set
     */
    public void setLastKnownVersion(Long lastKnownVersion) {
        this.lastKnownVersion = lastKnownVersion;
    }

    public int getMyClassType() {
        return AppConstants.CLASS_TYPE_RMS_MAPPING_PROVIDER;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((lastKnownVersion == null) ? 0 : lastKnownVersion.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        XXRMSMappingProvider other = (XXRMSMappingProvider) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (lastKnownVersion == null) {
            if (other.lastKnownVersion != null)
                return false;
        } else if (!lastKnownVersion.equals(other.lastKnownVersion))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        sb.append("{ ");
        sb.append("id={").append(id).append("} ");
        sb.append("changeTimestamp={" + changeTimestamp + "} ");
        sb.append("resourceSignature={").append(name).append("} ");
        sb.append("serviceId={").append(lastKnownVersion).append("} ");
        sb.append(" }");

        return sb;
    }

}
