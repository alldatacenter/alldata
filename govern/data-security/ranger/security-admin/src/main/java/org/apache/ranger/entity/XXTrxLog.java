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

/**
 * Logging table for all DB create and update queries
 *
 */

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerConstants;


@Entity
@Table(name="x_trx_log")
@XmlRootElement
public class XXTrxLog extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_TRX_LOG_SEQ",sequenceName="X_TRX_LOG_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_TRX_LOG_SEQ")
	@Column(name="ID")
	protected Long id;
	@Override
	public void setId(Long id) {
		this.id=id;
	}
	@Override
	public Long getId() {
		return id;
	}
	/**
	 * Name of the class to which the object id belongs to
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ClassTypes
	 * </ul>
	 *
	 */
	@Column(name="CLASS_TYPE"  , nullable=false )
	protected int objectClassType = RangerConstants.CLASS_TYPE_NONE;

	/**
	 * Id of the object to which this notes refers to
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="OBJECT_ID"   )
	protected Long objectId;

	/**
	 * Object Id of the parent object
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="PARENT_OBJECT_ID"   )
	protected Long parentObjectId;

	/**
	 * Object Class Type of the parent object
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="PARENT_OBJECT_CLASS_TYPE"  , nullable=false )
	protected int parentObjectClassType;

	/**
	 * Name of the attribute that was changed
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="PARENT_OBJECT_NAME"   , length=1024)
	protected String parentObjectName;

	/**
	 * Name of the attribute that was changed
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="OBJECT_NAME"   , length=1024)
	protected String objectName;

	/**
	 * Name of the attribute that was changed
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="ATTR_NAME"   , length=255)
	protected String attributeName;

	/**
	 * Previous value
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="PREV_VAL"   , length=1024)
	protected String previousValue;

	/**
	 * New value
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="NEW_VAL"   , length=1024)
	protected String newValue;

	/**
	 * Transaction id
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="TRX_ID"   , length=1024)
	protected String transactionId;

	/**
	 * Action of the transaction
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="ACTION"   , length=255)
	protected String action;

	/**
	 * Session Id
	 * <ul>
	 * <li>The maximum length for this attribute is <b>512</b>.
	 * </ul>
	 *
	 */
	@Column(name="SESS_ID"   , length=512)
	protected String sessionId;

	/**
	 * Request Id
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="REQ_ID"   )
	protected String requestId;

	/**
	 * Session Type
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="SESS_TYPE"   )
	protected String sessionType;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXTrxLog ( ) {
		objectClassType = RangerConstants.CLASS_TYPE_NONE;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_TRX_LOG;
	}

	/**
	 * This method sets the value to the member attribute <b>objectClassType</b>.
	 * You cannot set null to the attribute.
	 * @param objectClassType Value to set member attribute <b>objectClassType</b>
	 */
	public void setObjectClassType( int objectClassType ) {
		this.objectClassType = objectClassType;
	}

	/**
	 * Returns the value for the member attribute <b>objectClassType</b>
	 * @return int - value of member attribute <b>objectClassType</b>.
	 */
	public int getObjectClassType( ) {
		return this.objectClassType;
	}

	/**
	 * This method sets the value to the member attribute <b>objectId</b>.
	 * You cannot set null to the attribute.
	 * @param objectId Value to set member attribute <b>objectId</b>
	 */
	public void setObjectId( Long objectId ) {
		this.objectId = objectId;
	}

	/**
	 * Returns the value for the member attribute <b>objectId</b>
	 * @return Long - value of member attribute <b>objectId</b>.
	 */
	public Long getObjectId( ) {
		return this.objectId;
	}

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
	 * This method sets the value to the member attribute <b>parentObjectName</b>.
	 * You cannot set null to the attribute.
	 * @param parentObjectName Value to set member attribute <b>parentObjectName</b>
	 */
	public void setParentObjectName( String parentObjectName ) {
		this.parentObjectName = parentObjectName;
	}

	/**
	 * Returns the value for the member attribute <b>parentObjectName</b>
	 * @return String - value of member attribute <b>parentObjectName</b>.
	 */
	public String getParentObjectName( ) {
		return this.parentObjectName;
	}

	/**
	 * This method sets the value to the member attribute <b>objectName</b>.
	 * You cannot set null to the attribute.
	 * @param objectName Value to set member attribute <b>objectName</b>
	 */
	public void setObjectName( String objectName ) {
		this.objectName = objectName;
	}

	/**
	 * Returns the value for the member attribute <b>objectName</b>
	 * @return String - value of member attribute <b>objectName</b>.
	 */
	public String getObjectName( ) {
		return this.objectName;
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
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXTrxLog={";
		str += super.toString();
		str += "objectClassType={" + objectClassType + "} ";
		str += "objectId={" + objectId + "} ";
		str += "parentObjectId={" + parentObjectId + "} ";
		str += "parentObjectClassType={" + parentObjectClassType + "} ";
		str += "parentObjectName={" + parentObjectName + "} ";
		str += "objectName={" + objectName + "} ";
		str += "attributeName={" + attributeName + "} ";
		str += "previousValue={" + previousValue + "} ";
		str += "newValue={" + newValue + "} ";
		str += "transactionId={" + transactionId + "} ";
		str += "action={" + action + "} ";
		str += "requestId={" + requestId + "} ";
		str += "}";
		return str;
	}

	/**
	 * Checks for all attributes except referenced db objects
	 * @return true if all attributes match
	*/
	@Override
	public boolean equals( Object obj) {
		if ( !super.equals(obj) ) {
			return false;
		}
		XXTrxLog other = (XXTrxLog) obj;
		if( this.objectClassType != other.objectClassType ) return false;
        	if ((this.objectId == null && other.objectId != null) || (this.objectId != null && !this.objectId.equals(other.objectId))) {
            		return false;
        	}
        	if ((this.parentObjectId == null && other.parentObjectId != null) || (this.parentObjectId != null && !this.parentObjectId.equals(other.parentObjectId))) {
            		return false;
        	}
		if( this.parentObjectClassType != other.parentObjectClassType ) return false;
        	if ((this.parentObjectName == null && other.parentObjectName != null) || (this.parentObjectName != null && !this.parentObjectName.equals(other.parentObjectName))) {
            		return false;
        	}
        	if ((this.objectName == null && other.objectName != null) || (this.objectName != null && !this.objectName.equals(other.objectName))) {
            		return false;
        	}
        	if ((this.attributeName == null && other.attributeName != null) || (this.attributeName != null && !this.attributeName.equals(other.attributeName))) {
            		return false;
        	}
        	if ((this.previousValue == null && other.previousValue != null) || (this.previousValue != null && !this.previousValue.equals(other.previousValue))) {
            		return false;
        	}
        	if ((this.newValue == null && other.newValue != null) || (this.newValue != null && !this.newValue.equals(other.newValue))) {
            		return false;
        	}
        	if ((this.transactionId == null && other.transactionId != null) || (this.transactionId != null && !this.transactionId.equals(other.transactionId))) {
            		return false;
        	}
        	if ((this.action == null && other.action != null) || (this.action != null && !this.action.equals(other.action))) {
            		return false;
        	}
        	if ((this.sessionId == null && other.sessionId != null) || (this.sessionId != null && !this.sessionId.equals(other.sessionId))) {
            		return false;
        	}
        	if ((this.requestId == null && other.requestId != null) || (this.requestId != null && !this.requestId.equals(other.requestId))) {
            		return false;
        	}
        	if ((this.sessionType == null && other.sessionType != null) || (this.sessionType != null && !this.sessionType.equals(other.sessionType))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "objectClassType".equals(fieldName) ) {
			return "CommonEnums.ClassTypes";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
