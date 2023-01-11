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
@Cacheable(false)
@Table(name="x_rms_notification")
@XmlRootElement
public class XXRMSNotification implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_RMS_NOTIFICATION_SEQ",sequenceName="X_RMS_NOTIFICATION_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_RMS_NOTIFICATION_SEQ")
	@Column(name="ID")
	protected Long id;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	@Column(name="hms_name", length=128)
	protected String hmsName;
	public String getHmsName() {
		return hmsName;
	}
	public void setHmsName(String hmsName) {
		this.hmsName = hmsName;
	}

	@Column(name="notification_id")
	protected Long notificationId;
	public Long getNotificationId() {
		return notificationId;
	}
	public void setNotificationId(Long notificationId) {
		this.notificationId = notificationId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="change_timestamp")
	protected Date changeTimestamp;
	public Date getChangeTimestamp() {
		return changeTimestamp;
	}
	public void setChangeTimestamp(Date changeTimestamp) {
		this.changeTimestamp = changeTimestamp;
	}

	@Column(name="change_type"  , length=64 )
	protected String changeType;
	public String getChangeType() {
		return changeType;
	}
	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	@Column(name="hl_resource_id")
	protected Long hlResourceId;
	public Long getHlResourceId() {
		return hlResourceId;
	}
	public void setHlResourceId(Long hlResourceId) {
		this.hlResourceId = hlResourceId;
	}

	@Column(name="hl_service_id")
	protected Long hlServiceId;
	public Long getHlServiceId() {
		return hlServiceId;
	}
	public void setHlServiceId(Long hlServiceId) {
		this.hlServiceId = hlServiceId;
	}

	@Column(name="ll_resource_id")
	protected Long llResourceId;
	public Long getLlResourceId() {
		return llResourceId;
	}
	public void setLlResourceId(Long llResourceId) {
		this.llResourceId = llResourceId;
	}

	@Column(name="ll_service_id")
	protected Long llServiceId;
	public Long getLlServiceId() { return llServiceId; }
	public void setLlServiceId(Long llServiceId) {
		this.llServiceId = llServiceId;
	}

	public int getMyClassType() {
		return AppConstants.CLASS_TYPE_RMS_MAPPING_PROVIDER;
	}

	public String toString( ) {
		String str = "XXNotification={";
		str += "hmsName={" + hmsName + "} ";
		str += "notificationId={" + notificationId + "} ";
		str += "changeTimestamp={" + changeTimestamp + "} ";
		str += "changeType={" + changeType + "} ";
		str += "hlResourceId={" + hlResourceId + "} ";
		str += "hlServiceId={" + hlServiceId + "} ";
		str += "llResourceId={" + llResourceId + "} ";
		str += "llServiceId={" + llServiceId + "} ";
		str += "}";

		return str;
	}

}