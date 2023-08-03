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

import java.util.Objects;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_security_zone_ref_service")
public class XXSecurityZoneRefService extends XXDBBase implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
  	@Id
    @SequenceGenerator(name = "x_sec_zone_ref_service_SEQ", sequenceName = "x_sec_zone_ref_service_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_sec_zone_ref_service_SEQ")
    @Column(name = "id")
    protected Long id;

  	/**
	 * zoneId of the XXSecurityZoneRefService
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "zone_id")
	protected Long zoneId;

  	/**
	 * serviceId of the XXSecurityZoneRefService
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "service_id")
	protected Long serviceId;

	/**
	 * serviceName of the XXSecurityZoneRefService
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "service_name")
	protected String serviceName;

	/**
	 * This method sets the value to the member attribute <b> id</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param id
	 *            Value to set member attribute <b> id</b>
	 */
	
    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
	 * This method sets the value to the member attribute <b> zoneId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param zoneId
	 *            Value to set member attribute <b> zoneId</b>
	 */
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}

	/**
	 * Returns the value for the member attribute <b>zoneId</b>
	 *
	 * @return Date - value of member attribute <b>zoneId</b> .
	 */
	public Long getZoneId() {
		return this.zoneId;
	}

	/**
	 * This method sets the value to the member attribute <b> serviceId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param serviceId
	 *            Value to set member attribute <b> serviceId</b>
	 */
	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * Returns the value for the member attribute <b>serviceId</b>
	 *
	 * @return Date - value of member attribute <b>serviceId</b> .
	 */
	public Long getServiceId() {
		return serviceId;
	}

	/**
	 * This method sets the value to the member attribute <b> serviceName</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param serviceName
	 *            Value to set member attribute <b> serviceName</b>
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Returns the value for the member attribute <b>serviceName</b>
	 *
	 * @return Date - value of member attribute <b>serviceName</b> .
	 */
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, zoneId, serviceId, serviceName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		XXSecurityZoneRefService other = (XXSecurityZoneRefService) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(zoneId, other.zoneId) &&
			   Objects.equals(serviceId, other.serviceId) &&
			   Objects.equals(serviceName, other.serviceName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXSecurityZoneRefService [" + super.toString() + " id=" + id + ", zoneId=" + zoneId + ", serviceId="
				+ serviceId + ", serviceName=" + serviceName +  "]";
	}

}