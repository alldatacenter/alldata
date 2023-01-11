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

import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_plugin_info")
public class XXPluginInfo implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_PLUGIN_INFO_SEQ", sequenceName = "X_PLUGIN_INFO_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_PLUGIN_INFO_SEQ")
	@Column(name = "id")
	protected Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_TIME"   )
	protected Date createTime;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="UPDATE_TIME"   )
	protected Date updateTime;

	@Column(name = "service_name")
	protected String serviceName;

	@Column(name = "app_type")
	protected String appType;

	@Column(name = "host_name")
	protected String hostName;

	@Column(name = "ip_address")
	protected String ipAddress;

	@Column(name = "info")
	protected String info;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXPluginInfo( ) {
	}

	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_NONE;
	}

	public String getMyDisplayValue() {
		return null;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	public void setCreateTime( Date createTime ) {
		this.createTime = createTime;
	}

	public Date getCreateTime( ) {
		return this.createTime;
	}

	public void setUpdateTime( Date updateTime ) {
		this.updateTime = updateTime;
	}

	public Date getUpdateTime( ) {
		return this.updateTime;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public void setAppType(String appType) {
		this.appType = appType;
	}

	public String getAppType() {
		return this.appType;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getHostName() {
		return this.hostName;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getInfo() {
		return this.info;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXPluginInfo={";
		str += "id={" + id + "} ";
		str += "createTime={" + createTime + "} ";
		str += "updateTime={" + updateTime + "} ";
		str += "serviceName={" + serviceName + "} ";
		str += "hostName={" + hostName + "} ";
		str += "appType={" + appType + "} ";
		str += "ipAddress={" + ipAddress + "} ";
		str += "info={" + info + "} ";
		str += "}";
		return str;
	}

	/**
	 * Checks for all attributes except referenced db objects
	 * @return true if all attributes match
	*/
	@Override
	public boolean equals( Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		XXPluginInfo other = (XXPluginInfo) obj;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		if ((this.createTime == null && other.createTime != null) || (this.createTime != null && !this.createTime.equals(other.createTime))) {
			return false;
		}
		if ((this.updateTime == null && other.updateTime != null) || (this.updateTime != null && !this.updateTime.equals(other.updateTime))) {
			return false;
		}
		if ((this.serviceName == null && other.serviceName != null) || (this.serviceName != null && !this.serviceName.equals(other.serviceName))) {
			return false;
		}
		if ((this.hostName == null && other.hostName != null) || (this.hostName != null && !this.hostName.equals(other.hostName))) {
			return false;
		}
		if ((this.appType == null && other.appType != null) || (this.appType != null && !this.appType.equals(other.appType))) {
			return false;
		}
		if ((this.ipAddress == null && other.ipAddress != null) || (this.ipAddress != null && !this.ipAddress.equals(other.ipAddress))) {
			return false;
		}
		if ((this.info == null && other.info != null) || (this.info != null && !this.info.equals(other.info))) {
			return false;
		}
		return true;
	}

	public static boolean equals(Object object1, Object object2) {
		if (object1 == object2) {
			return true;
		}
		if ((object1 == null) || (object2 == null)) {
			return false;
		}
		return object1.equals(object2);
	}

}
