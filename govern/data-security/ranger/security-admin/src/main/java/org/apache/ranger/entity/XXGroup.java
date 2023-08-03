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
 * Group
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
@Table(name="x_group")
@XmlRootElement
public class XXGroup extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_GROUP_SEQ",sequenceName="X_GROUP_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_GROUP_SEQ")
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
	@Column(name="GROUP_NAME"  , nullable=false , length=1024)
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
	 * IsVisible
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActiveVisiblility
	 * </ul>
	 *
	 */
	@Column(name="IS_VISIBLE"  , nullable=false )
	protected Integer isVisible;

	/**
	 * Type of group
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::XAGroupType
	 * </ul>
	 *
	 */
	@Column(name="GROUP_TYPE"  , nullable=false )
	protected int groupType = AppConstants.XA_GROUP_UNKNOWN;
	
	@Column(name="GROUP_SRC"  , nullable=false )
	protected int groupSource = RangerCommonEnums.GROUP_INTERNAL;

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
	 * Sync Source Attribute.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="SYNC_SOURCE")
	protected String syncSource;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXGroup ( ) {
		status = RangerConstants.STATUS_DISABLED;
		groupType = AppConstants.XA_GROUP_UNKNOWN;
		groupSource = RangerCommonEnums.GROUP_INTERNAL;
		isVisible = RangerCommonEnums.IS_VISIBLE;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_GROUP;
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
	 * @return the isVisible
	 */
	public Integer getIsVisible() {
		return isVisible;
	}
	
	/**
	 * @param isVisible the isVisible to set
	 */
	public void setIsVisible(Integer isVisible) {
		this.isVisible = isVisible;
	}
	
	/**
	 * This method sets the value to the member attribute <b>groupType</b>.
	 * You cannot set null to the attribute.
	 * @param groupType Value to set member attribute <b>groupType</b>
	 */
	public void setGroupType( int groupType ) {
		this.groupType = groupType;
	}

	/**
	 * Returns the value for the member attribute <b>groupType</b>
	 * @return int - value of member attribute <b>groupType</b>.
	 */
	public int getGroupType( ) {
		return this.groupType;
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
		String str = "XXGroup={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "description={" + description + "} ";
		str += "status={" + status + "} ";
		str += "isvisible={" + isVisible + "} ";
		str += "groupType={" + groupType + "} ";
		str += "credStoreId={" + credStoreId + "} ";
		str += "groupSrc={" + groupSource + "} ";
		str += "otherAttributes={" + otherAttributes + "} ";
		str += "syncSource={" + syncSource + "} ";
		str += "}";
		return str;
	}

	public int getGroupSource() {
		return groupSource;
	}

	public void setGroupSource(int groupSource) {
		this.groupSource = groupSource;
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
		XXGroup other = (XXGroup) obj;
        	if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            		return false;
        	}
        	if ((this.description == null && other.description != null) || (this.description != null && !this.description.equals(other.description))) {
            		return false;
        	}
		if( this.status != other.status ) return false;
		if( this.groupType != other.groupType ) return false;
        	if ((this.credStoreId == null && other.credStoreId != null) || (this.credStoreId != null && !this.credStoreId.equals(other.credStoreId))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "status".equals(fieldName) ) {
			return "CommonEnums.ActiveStatus";
		}
		if( "groupType".equals(fieldName) ) {
			return "CommonEnums.XAGroupType";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
