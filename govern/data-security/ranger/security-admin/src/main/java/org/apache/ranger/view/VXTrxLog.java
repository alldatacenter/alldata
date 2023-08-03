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

 package org.apache.ranger.view;

/**
 * Logging table for all DB create and update queries
 *
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.RangerConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXTrxLog extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Name of the class to which the object id belongs to
	 * This attribute is of type enum CommonEnums::ClassTypes
	 */
	protected int objectClassType = RangerConstants.CLASS_TYPE_NONE;
	/**
	 * Id of the object to which this notes refers to
	 */
	protected Long objectId;
	/**
	 * Object Id of the parent object
	 */
	protected Long parentObjectId;
	/**
	 * Object Class Type of the parent object
	 */
	protected int parentObjectClassType;
	/**
	 * Name of the parent object name that was changed
	 */
	protected String parentObjectName;
	/**
	 * Name of the object name that was changed
	 */
	protected String objectName;
	/**
	 * Name of the attribute that was changed
	 */
	protected String attributeName;
	/**
	 * Previous value
	 */
	protected String previousValue;
	/**
	 * New value
	 */
	protected String newValue;
	/**
	 * Transaction id
	 */
	protected String transactionId;
	/**
	 * Action of the transaction
	 */
	protected String action;
	/**
	 * Session Id
	 */
	protected String sessionId;
	/**
	 * Request Id
	 */
	protected String requestId;
	/**
	 * Session Type
	 */
	protected String sessionType;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXTrxLog ( ) {
		objectClassType = RangerConstants.CLASS_TYPE_NONE;
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
	public String toString( ) {
		String str = "VXTrxLog={";
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
		str += "sessionId={" + sessionId + "} ";
		str += "requestId={" + requestId + "} ";
		str += "sessionType={" + sessionType + "} ";
		str += "}";
		return str;
	}
}
