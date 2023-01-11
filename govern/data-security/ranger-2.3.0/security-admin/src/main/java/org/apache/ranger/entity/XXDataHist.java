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

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.DateUtil;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_data_hist")
public class XXDataHist implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_data_hist_SEQ", sequenceName = "x_data_hist_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_data_hist_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * Date/Time creation of this user.
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATE_TIME")
	protected Date createTime = DateUtil.getUTCDate();

	/**
	 * Date value.
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "UPDATE_TIME")
	protected Date updateTime = DateUtil.getUTCDate();

	/**
	 * version of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "version")
	protected Long version;
	
	/**
	 * type of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "obj_guid")
	protected String objectGuid;

	/**
	 * type of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "obj_class_type")
	protected Integer objectClassType;

	/**
	 * type of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "obj_id")
	protected Long objectId;

	/**
	 * name of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "obj_name")
	protected String objectName;

	/**
	 * action of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "action")
	protected String action;

	/**
	 * fromTime of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "from_time")
	protected Date fromTime;

	/**
	 * toTime of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "to_time")
	protected Date toTime;

	/**
	 * content of the XXDataHist
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "content")
	protected String content;

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
	 * @return the createTime
	 */
	public Date getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime
	 *            the createTime to set
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the updateTime
	 */
	public Date getUpdateTime() {
		return updateTime;
	}

	/**
	 * @param updateTime
	 *            the updateTime to set
	 */
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	
	/**
	 * Returns the value for the member attribute <b>version</b>
	 *
	 * @return Date - value of member attribute <b>version</b> .
	 */
	public Long getVersion() {
		return this.version;
	}
	
	/**
	 * This method sets the value to the member attribute <b> version</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param version
	 *            Value to set member attribute <b> version</b>
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * This method sets the value to the member attribute <b> action</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param action
	 *            Value to set member attribute <b> action</b>
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * @return the objectGuid
	 */
	public String getObjectGuid() {
		return objectGuid;
	}

	/**
	 * @param objectGuid the objectGuid to set
	 */
	public void setObjectGuid(String objectGuid) {
		this.objectGuid = objectGuid;
	}

	/**
	 * @return the objectId
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	/**
	 * This method sets the value to the member attribute <b> type</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param objectClassType
	 *            Value to set member attribute <b> type</b>
	 */
	public void setObjectClassType(Integer objectClassType) {
		this.objectClassType = objectClassType;
	}

	/**
	 * Returns the value for the member attribute <b>type</b>
	 *
	 * @return Date - value of member attribute <b>type</b> .
	 */
	public Integer getObjectClassType() {
		return this.objectClassType;
	}

	/**
	 * This method sets the value to the member attribute <b> name</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param name
	 *            Value to set member attribute <b> name</b>
	 */
	public void setObjectName(String name) {
		this.objectName = name;
	}

	/**
	 * Returns the value for the member attribute <b>name</b>
	 *
	 * @return Date - value of member attribute <b>name</b> .
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * Returns the value for the member attribute <b>action</b>
	 *
	 * @return Date - value of member attribute <b>action</b> .
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * This method sets the value to the member attribute <b> fromTime</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param fromTime
	 *            Value to set member attribute <b> fromTime</b>
	 */
	public void setFromTime(Date fromTime) {
		this.fromTime = fromTime;
	}

	/**
	 * Returns the value for the member attribute <b>fromTime</b>
	 *
	 * @return Date - value of member attribute <b>fromTime</b> .
	 */
	public Date getFromTime() {
		return this.fromTime;
	}

	/**
	 * This method sets the value to the member attribute <b> toTime</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param toTime
	 *            Value to set member attribute <b> toTime</b>
	 */
	public void setToTime(Date toTime) {
		this.toTime = toTime;
	}

	/**
	 * Returns the value for the member attribute <b>toTime</b>
	 *
	 * @return Date - value of member attribute <b>toTime</b> .
	 */
	public Date getToTime() {
		return this.toTime;
	}

	/**
	 * This method sets the value to the member attribute <b> content</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param content
	 *            Value to set member attribute <b> content</b>
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Returns the value for the member attribute <b>content</b>
	 *
	 * @return Date - value of member attribute <b>content</b> .
	 */
	public String getContent() {
		return this.content;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if ( !super.equals(obj) ) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		XXDataHist other = (XXDataHist) obj;
		if (action == null) {
			if (other.action != null) {
				return false;
			}
		} else if (!action.equals(other.action)) {
			return false;
		}
		if (content == null) {
			if (other.content != null) {
				return false;
			}
		} else if (!content.equals(other.content)) {
			return false;
		}
		if (createTime == null) {
			if (other.createTime != null) {
				return false;
			}
		} else if (!createTime.equals(other.createTime)) {
			return false;
		}
		if (fromTime == null) {
			if (other.fromTime != null) {
				return false;
			}
		} else if (!fromTime.equals(other.fromTime)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (objectName == null) {
			if (other.objectName != null) {
				return false;
			}
		} else if (!objectName.equals(other.objectName)) {
			return false;
		}
		if (toTime == null) {
			if (other.toTime != null) {
				return false;
			}
		} else if (!toTime.equals(other.toTime)) {
			return false;
		}
		if (objectClassType == null) {
			if (other.objectClassType != null) {
				return false;
			}
		} else if (!objectClassType.equals(other.objectClassType)) {
			return false;
		}
		if (updateTime == null) {
			if (other.updateTime != null) {
				return false;
			}
		} else if (!updateTime.equals(other.updateTime)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
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
		return "XXDataHist [id=" + id + ", createTime=" + createTime
				+ ", updateTime=" + updateTime + ", type=" + objectClassType + ", name="
				+ objectName + ", version=" + version + ", action=" + action
				+ ", fromTime=" + fromTime + ", toTime=" + toTime
				+ ", content=" + content + "]";
	}

}