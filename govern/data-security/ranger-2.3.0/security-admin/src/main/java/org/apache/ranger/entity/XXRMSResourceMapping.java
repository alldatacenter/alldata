/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.entity;

import org.apache.ranger.common.AppConstants;

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
import java.io.Serializable;
import java.util.Date;

@Entity
@Cacheable(false)
@Table(name="x_rms_resource_mapping")
@XmlRootElement
public class XXRMSResourceMapping implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_RMS_RESOURCE_MAPPING_SEQ",sequenceName="X_RMS_RESOURCE_MAPPING_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_RMS_RESOURCE_MAPPING_SEQ")
	@Column(name="id")
	protected Long id;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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

	@Column(name="hl_resource_id")
	protected Long hlResourceId;
	public Long getHlResourceId() {
		return hlResourceId;
	}
	public void setHlResourceId(Long hlResourceId) {
		this.hlResourceId = hlResourceId;
	}

	@Column(name="ll_resource_id")
	protected Long llResourceId;
	public Long getLlResourceId() {
		return llResourceId;
	}
	public void setLlResourceId(Long llResourceId) {
		this.llResourceId = llResourceId;
	}

	public int getMyClassType() {
		return AppConstants.CLASS_TYPE_RMS_RESOURCE_MAPPING;
	}

	public String toString( ) {
		String str = "XXResourceMapping={";
		str += "id={" + id + "} ";
		str += "changeTimestamp={" + changeTimestamp + "} ";
		str += "hlResourceId={" + hlResourceId + "} ";
		str += "llResourceId={" + llResourceId + "} ";
		str += "}";

		return str;
	}

}
