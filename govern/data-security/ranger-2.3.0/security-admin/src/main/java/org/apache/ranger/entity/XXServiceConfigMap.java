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

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_service_config_map")
public class XXServiceConfigMap extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXServiceConfigMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_service_config_map_SEQ", sequenceName = "x_service_config_map_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_service_config_map_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * service of the XXServiceConfigMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "service")
	protected Long serviceId;

	/**
	 * configKey of the XXServiceConfigMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "config_key")
	protected String configKey;

	/**
	 * configValue of the XXServiceConfigMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "config_value")
	protected String configValue;

	/**
	 * This method sets the value to the member attribute <b> id</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param id
	 *            Value to set member attribute <b> id</b>
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the value for the member attribute <b>id</b>
	 *
	 * @return Date - value of member attribute <b>id</b> .
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * This method sets the value to the member attribute <b> service</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param service
	 *            Value to set member attribute <b> service</b>
	 */
	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * Returns the value for the member attribute <b>service</b>
	 *
	 * @return Date - value of member attribute <b>service</b> .
	 */
	public Long getServiceId() {
		return this.serviceId;
	}

	/**
	 * This method sets the value to the member attribute <b> configKey</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param configKey
	 *            Value to set member attribute <b> configKey</b>
	 */
	public void setConfigkey(String configKey) {
		this.configKey = configKey;
	}

	/**
	 * Returns the value for the member attribute <b>configKey</b>
	 *
	 * @return Date - value of member attribute <b>configKey</b> .
	 */
	public String getConfigkey() {
		return this.configKey;
	}

	/**
	 * This method sets the value to the member attribute <b> configValue</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param configValue
	 *            Value to set member attribute <b> configValue</b>
	 */
	public void setConfigvalue(String configValue) {
		this.configValue = configValue;
	}

	/**
	 * Returns the value for the member attribute <b>configValue</b>
	 *
	 * @return Date - value of member attribute <b>configValue</b> .
	 */
	public String getConfigvalue() {
		return this.configValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		XXServiceConfigMap other = (XXServiceConfigMap) obj;
		if (configKey == null) {
			if (other.configKey != null) {
				return false;
			}
		} else if (!configKey.equals(other.configKey)) {
			return false;
		}
		if (configValue == null) {
			if (other.configValue != null) {
				return false;
			}
		} else if (!configValue.equals(other.configValue)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (serviceId == null) {
			if (other.serviceId != null) {
				return false;
			}
		} else if (!serviceId.equals(other.serviceId)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXServiceConfigMap [" + super.toString() + " id=" + id
				+ ", service=" + serviceId + ", configKey=" + configKey
				+ ", configValue=" + configValue + "]";
	}

}