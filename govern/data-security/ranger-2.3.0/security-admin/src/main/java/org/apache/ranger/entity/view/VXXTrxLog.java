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

 package org.apache.ranger.entity.view;


import java.util.Date;

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

import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.RangerConstants;

@Entity
@Table(name="vx_trx_log")
@XmlRootElement
public class VXXTrxLog implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	public static final int SHARE_PREF_DEFAULT = 0;

	@Id
	@SequenceGenerator(name="V_TRX_LOG_SEQ",sequenceName="V_TRX_LOG_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="V_TRX_LOG_SEQ")
	@Column(name="ID")
	protected Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_TIME"  , nullable=false )
	protected Date createTime = DateUtil.getUTCDate();

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="UPDATE_TIME"  , nullable=false )
	protected Date updateTime = DateUtil.getUTCDate();

	@Column(name="ADDED_BY_ID"   )
	protected Long addedByUserId;

	@Column(name="UPD_BY_ID"   )
	protected Long updatedByUserId;

	@Column(name="CLASS_TYPE"  , nullable=false )
	protected int objectClassType = RangerConstants.CLASS_TYPE_NONE;

	@Column(name="OBJECT_ID"   )
	protected Long objectId;

	@Column(name="PARENT_OBJECT_ID"  , nullable=false )
	protected Long parentObjectId;

	@Column(name="PARENT_OBJECT_CLASS_TYPE"  , nullable=false )
	protected int parentObjectClassType;

	@Column(name="ATTR_NAME"  , nullable=false , length=255)
	protected String attributeName;

	@Column(name="PARENT_OBJECT_NAME"   , length=1024)
	protected String parentObjectName;

	@Column(name="OBJECT_NAME"   , length=1024)
	protected String objectName;
	
	@Column(name="PREV_VAL"   , length=1024)
	protected String previousValue;

	@Column(name="NEW_VAL"  , nullable=true , length=1024)
	protected String newValue;

	@Column(name="TRX_ID"  , nullable=false , length=1024)
	protected String transactionId;

	@Column(name="ACTION"   , length=255)
	protected String action;

	@Column(name="SESS_ID"   )
	protected String sessionId;

	@Column(name="REQ_ID"  , nullable=false )
	protected String requestId;

	@Column(name="SESS_TYPE"  , nullable=false )
	protected String sessionType;
	

	/**
	 * This method sets the value to the member attribute <b>parentObjectId</b>.
	 * You cannot set null to the attribute.
	 * @param parentObjectId Value to set member attribute <b>parentObjectId</b>
	 */
	public void setParentObjectId( Long parentObjectId ) {
		this.parentObjectId = parentObjectId;
	}

	/**
	 * Returns the value for the member attribute <b>parentObjectId</b>
	 * @return Long - value of member attribute <b>parentObjectId</b>.
	 */
	public Long getParentObjectId( ) {
		return this.parentObjectId;
	}

	/**
	 * This method sets the value to the member attribute <b>parentObjectClassType</b>.
	 * You cannot set null to the attribute.
	 * @param parentObjectClassType Value to set member attribute <b>parentObjectClassType</b>
	 */
	public void setParentObjectClassType( int parentObjectClassType ) {
		this.parentObjectClassType = parentObjectClassType;
	}

	/**
	 * Returns the value for the member attribute <b>parentObjectClassType</b>
	 * @return int - value of member attribute <b>parentObjectClassType</b>.
	 */
	public int getParentObjectClassType( ) {
		return this.parentObjectClassType;
	}

	/**
	 * This method sets the value to the member attribute <b>attributeName</b>.
	 * You cannot set null to the attribute.
	 * @param attributeName Value to set member attribute <b>attributeName</b>
	 */
	public void setAttributeName( String attributeName ) {
		this.attributeName = attributeName;
	}

	/**
	 * Returns the value for the member attribute <b>attributeName</b>
	 * @return String - value of member attribute <b>attributeName</b>.
	 */
	public String getAttributeName( ) {
		return this.attributeName;
	}

	/**
	 * This method sets the value to the member attribute <b>previousValue</b>.
	 * You cannot set null to the attribute.
	 * @param previousValue Value to set member attribute <b>previousValue</b>
	 */
	public void setPreviousValue( String previousValue ) {
		this.previousValue = previousValue;
	}

	/**
	 * Returns the value for the member attribute <b>previousValue</b>
	 * @return String - value of member attribute <b>previousValue</b>.
	 */
	public String getPreviousValue( ) {
		return this.previousValue;
	}

	/**
	 * This method sets the value to the member attribute <b>newValue</b>.
	 * You cannot set null to the attribute.
	 * @param newValue Value to set member attribute <b>newValue</b>
	 */
	public void setNewValue( String newValue ) {
		this.newValue = newValue;
	}

	/**
	 * Returns the value for the member attribute <b>newValue</b>
	 * @return String - value of member attribute <b>newValue</b>.
	 */
	public String getNewValue( ) {
		return this.newValue;
	}

	/**
	 * This method sets the value to the member attribute <b>transactionId</b>.
	 * You cannot set null to the attribute.
	 * @param transactionId Value to set member attribute <b>transactionId</b>
	 */
	public void setTransactionId( String transactionId ) {
		this.transactionId = transactionId;
	}

	/**
	 * Returns the value for the member attribute <b>transactionId</b>
	 * @return String - value of member attribute <b>transactionId</b>.
	 */
	public String getTransactionId( ) {
		return this.transactionId;
	}

	/**
	 * This method sets the value to the member attribute <b>action</b>.
	 * You cannot set null to the attribute.
	 * @param action Value to set member attribute <b>action</b>
	 */
	public void setAction( String action ) {
		this.action = action;
	}

	/**
	 * Returns the value for the member attribute <b>action</b>
	 * @return String - value of member attribute <b>action</b>.
	 */
	public String getAction( ) {
		return this.action;
	}

	/**
	 * This method sets the value to the member attribute <b>sessionId</b>.
	 * You cannot set null to the attribute.
	 * @param sessionId Value to set member attribute <b>sessionId</b>
	 */
	public void setSessionId( String sessionId ) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns the value for the member attribute <b>sessionId</b>
	 * @return String - value of member attribute <b>sessionId</b>.
	 */
	public String getSessionId( ) {
		return this.sessionId;
	}

	/**
	 * This method sets the value to the member attribute <b>requestId</b>.
	 * You cannot set null to the attribute.
	 * @param requestId Value to set member attribute <b>requestId</b>
	 */
	public void setRequestId( String requestId ) {
		this.requestId = requestId;
	}

	/**
	 * Returns the value for the member attribute <b>requestId</b>
	 * @return String - value of member attribute <b>requestId</b>.
	 */
	public String getRequestId( ) {
		return this.requestId;
	}

	/**
	 * This method sets the value to the member attribute <b>sessionType</b>.
	 * You cannot set null to the attribute.
	 * @param sessionType Value to set member attribute <b>sessionType</b>
	 */
	public void setSessionType( String sessionType ) {
		this.sessionType = sessionType;
	}

	/**
	 * Returns the value for the member attribute <b>sessionType</b>
	 * @return String - value of member attribute <b>sessionType</b>.
	 */
	public String getSessionType( ) {
		return this.sessionType;
	}

	/**
	 * @return the parentObjectName
	 */
	public String getParentObjectName() {
		return parentObjectName;
	}

	/**
	 * @param parentObjectName the parentObjectName to set
	 */
	public void setParentObjectName(String parentObjectName) {
		this.parentObjectName = parentObjectName;
	}

	/**
	 * @return the objectName
	 */
	public String getObjectName() {
		return objectName;
	}

	/**
	 * @param objectName the objectName to set
	 */
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the createTime
	 */
	public Date getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime the createTime to set
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
	 * @param updateTime the updateTime to set
	 */
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * @return the addedByUserId
	 */
	public Long getAddedByUserId() {
		return addedByUserId;
	}

	/**
	 * @param addedByUserId the addedByUserId to set
	 */
	public void setAddedByUserId(Long addedByUserId) {
		this.addedByUserId = addedByUserId;
	}


	/**
	 * @return the updatedByUserId
	 */
	public Long getUpdatedByUserId() {
		return updatedByUserId;
	}

	/**
	 * @param updatedByUserId the updatedByUserId to set
	 */
	public void setUpdatedByUserId(Long updatedByUserId) {
		this.updatedByUserId = updatedByUserId;
	}

	/**
	 * @return the objectClassType
	 */
	public int getObjectClassType() {
		return objectClassType;
	}

	/**
	 * @param objectClassType the objectClassType to set
	 */
	public void setObjectClassType(int objectClassType) {
		this.objectClassType = objectClassType;
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
}
