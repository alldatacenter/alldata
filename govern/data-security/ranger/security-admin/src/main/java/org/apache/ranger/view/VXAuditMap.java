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
 * Audi map
 *
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXAuditMap extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Id of the resource
	 */
	protected Long resourceId;
	/**
	 * Id of the group
	 */
	protected Long groupId;
	/**
	 * Id of the user
	 */
	protected Long userId;
	/**
	 * Type of audit
	 * This attribute is of type enum CommonEnums::XAAuditType
	 */
	protected int auditType = AppConstants.XA_AUDIT_TYPE_UNKNOWN;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXAuditMap ( ) {
		auditType = AppConstants.XA_AUDIT_TYPE_UNKNOWN;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceId</b>.
	 * You cannot set null to the attribute.
	 * @param resourceId Value to set member attribute <b>resourceId</b>
	 */
	public void setResourceId( Long resourceId ) {
		this.resourceId = resourceId;
	}

	/**
	 * Returns the value for the member attribute <b>resourceId</b>
	 * @return Long - value of member attribute <b>resourceId</b>.
	 */
	public Long getResourceId( ) {
		return this.resourceId;
	}

	/**
	 * This method sets the value to the member attribute <b>groupId</b>.
	 * You cannot set null to the attribute.
	 * @param groupId Value to set member attribute <b>groupId</b>
	 */
	public void setGroupId( Long groupId ) {
		this.groupId = groupId;
	}

	/**
	 * Returns the value for the member attribute <b>groupId</b>
	 * @return Long - value of member attribute <b>groupId</b>.
	 */
	public Long getGroupId( ) {
		return this.groupId;
	}

	/**
	 * This method sets the value to the member attribute <b>userId</b>.
	 * You cannot set null to the attribute.
	 * @param userId Value to set member attribute <b>userId</b>
	 */
	public void setUserId( Long userId ) {
		this.userId = userId;
	}

	/**
	 * Returns the value for the member attribute <b>userId</b>
	 * @return Long - value of member attribute <b>userId</b>.
	 */
	public Long getUserId( ) {
		return this.userId;
	}

	/**
	 * This method sets the value to the member attribute <b>auditType</b>.
	 * You cannot set null to the attribute.
	 * @param auditType Value to set member attribute <b>auditType</b>
	 */
	public void setAuditType( int auditType ) {
		this.auditType = auditType;
	}

	/**
	 * Returns the value for the member attribute <b>auditType</b>
	 * @return int - value of member attribute <b>auditType</b>.
	 */
	public int getAuditType( ) {
		return this.auditType;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_AUDIT_MAP;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXAuditMap={";
		str += super.toString();
		str += "resourceId={" + resourceId + "} ";
		str += "groupId={" + groupId + "} ";
		str += "userId={" + userId + "} ";
		str += "auditType={" + auditType + "} ";
		str += "}";
		return str;
	}
}
