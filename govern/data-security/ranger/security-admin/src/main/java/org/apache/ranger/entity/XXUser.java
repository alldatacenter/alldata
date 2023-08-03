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
 * User
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
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.RangerConstants;


@Entity
@Table(name="x_user")
@XmlRootElement
public class XXUser extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@SequenceGenerator(name="X_USER_SEQ",sequenceName="X_USER_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_USER_SEQ")
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
	 * Name
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="USER_NAME"  , nullable=false , length=1024)
	protected String name;

	/**
	 * Description
	 * <ul>
	 * <li>The maximum length for this attribute is <b>4000</b>.
	 * </ul>
	 *
	 */
	@Column(name="DESCR"  , nullable=false , length=4000)
	protected String description;

	/**
	 * Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActiveStatus
	 * </ul>
	 *
	 */
	@Column(name="STATUS"  , nullable=false )
	protected int status = RangerConstants.STATUS_DISABLED;

	/**
	 * Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActiveStatus
	 * </ul>
	 *
	 */
	@Column(name="IS_VISIBLE"  , nullable=false )
	protected Integer isVisible;
	/**
	 * Id of the credential store
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="CRED_STORE_ID"   )
	protected Long credStoreId;

	/**
	 * Additional store attributes.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="OTHER_ATTRIBUTES")
	protected String otherAttributes;

	/**
	 * Sync Source attribute.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="SYNC_SOURCE")
	protected String syncSource;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXUser ( ) {
		status = RangerConstants.STATUS_DISABLED;
		isVisible = RangerCommonEnums.IS_VISIBLE;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_USER;
	}

	@Override
	public String getMyDisplayValue() {
		return getDescription( );
	}

	/**
	 * This method sets the value to the member attribute <b>name</b>.
	 * You cannot set null to the attribute.
	 * @param name Value to set member attribute <b>name</b>
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Returns the value for the member attribute <b>name</b>
	 * @return String - value of member attribute <b>name</b>.
	 */
	public String getName( ) {
		return this.name;
	}

	/**
	 * This method sets the value to the member attribute <b>description</b>.
	 * You cannot set null to the attribute.
	 * @param description Value to set member attribute <b>description</b>
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 * Returns the value for the member attribute <b>description</b>
	 * @return String - value of member attribute <b>description</b>.
	 */
	public String getDescription( ) {
		return this.description;
	}

	/**
	 * This method sets the value to the member attribute <b>status</b>.
	 * You cannot set null to the attribute.
	 * @param status Value to set member attribute <b>status</b>
	 */
	public void setStatus( int status ) {
		this.status = status;
	}

	/**
	 * Returns the value for the member attribute <b>status</b>
	 * @return int - value of member attribute <b>status</b>.
	 */
	public int getStatus( ) {
		return this.status;
	}

	/**
	 * This method sets the value to the member attribute <b>isVisible</b>.
	 * You cannot set null to the attribute.
	 * @param status Value to set member attribute <b>isVisible</b>
	 */
	public void setIsVisible(Integer isVisible) {
		this.isVisible = isVisible;
	}
	
	/**
	 * Returns the value for the member attribute <b>isVisible</b>
	 * @return int - value of member attribute <b>isVisible</b>.
	 */
	public Integer getIsVisible() {
		return isVisible;
	}
	
	/**
	 * This method sets the value to the member attribute <b>credStoreId</b>.
	 * You cannot set null to the attribute.
	 * @param credStoreId Value to set member attribute <b>credStoreId</b>
	 */
	public void setCredStoreId( Long credStoreId ) {
		this.credStoreId = credStoreId;
	}

	/**
	 * Returns the value for the member attribute <b>credStoreId</b>
	 * @return Long - value of member attribute <b>credStoreId</b>.
	 */
	public Long getCredStoreId( ) {
		return this.credStoreId;
	}


	/**
	 * This method sets JSON {@link String} representation of additional store attributes.
	 * This method accepts null values.
	 * @param otherAttributes
	 */
	public void setOtherAttributes(String otherAttributes) {
		this.otherAttributes = otherAttributes;
	}

	/**
	 * @return JSON {@link String} representation of additional store attributes if available,
	 * <code>null</code> otherwise.
	 */
	public String getOtherAttributes() {
		return otherAttributes;
	}

	/**
	 * This method sets JSON {@link String} representation of sync source attribute.
	 * This method accepts null values.
	 * @param syncSource
	 */
	public void setSyncSource(String syncSource) {
		this.syncSource = syncSource;
	}

	/**
	 * @return JSON {@link String} representation of sync source attribute if available,
	 * <code>null</code> otherwise.
	 */
	public String getSyncSource() { return syncSource; }

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXUser={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "description={" + description + "} ";
		str += "status={" + status + "} ";
		str += "isvisible={" + isVisible + "} ";
		str += "credStoreId={" + credStoreId + "} ";
		str += "otherAttributes={" + otherAttributes + "} ";
		str += "syncSource={" + syncSource + "} ";
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
		XXUser other = (XXUser) obj;
        	if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            		return false;
        	}
        	if ((this.description == null && other.description != null) || (this.description != null && !this.description.equals(other.description))) {
            		return false;
        	}
		if( this.status != other.status ) return false;
        	if ((this.credStoreId == null && other.credStoreId != null) || (this.credStoreId != null && !this.credStoreId.equals(other.credStoreId))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "status".equals(fieldName) ) {
			return "CommonEnums.ActiveStatus";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
