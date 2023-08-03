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
 * Group
 *
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerCommonEnums;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXGroup extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Name
	 */
	protected String name;
	/**
	 * Description
	 */
	protected String description;
	/**
	 * Type of group
	 * This attribute is of type enum CommonEnums::XAGroupType
	 */
	protected int groupType = AppConstants.XA_GROUP_UNKNOWN;
	
	protected int groupSource = RangerCommonEnums.GROUP_INTERNAL;
	/**
	 * Id of the credential store
	 */
	protected Long credStoreId;

	/**
	 * Group visibility
	 */
	protected Integer isVisible;

	/**
	 * Additional store attributes.
	 *
	 */
	protected String otherAttributes;

	/**
	 * Sync Source Attribute
	 * */
	protected String syncSource;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXGroup ( ) {
		groupType = AppConstants.XA_GROUP_UNKNOWN;
		isVisible = RangerCommonEnums.IS_VISIBLE;
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

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_GROUP;
	}

	
	

	public int getGroupSource() {
		return groupSource;
	}

	public void setGroupSource(int groupSource) {
		this.groupSource = groupSource;
	}

	/**
	 * @return {@link String} - additional attributes.
	 */
	public String getOtherAttributes() {
		return otherAttributes;
	}

	/**
	 * This method sets additional attributes.
	 * @param otherAttributes
	 */
	public void setOtherAttributes(final String otherAttributes) {
		this.otherAttributes = otherAttributes;
	}

	/**
	 * This method sets sync source attribute.
	 * @param syncSource
	 */
	public void setSyncSource(String syncSource) {
		this.syncSource = syncSource;
	}

	/**
	 * @return {@link String} sync source attribute
	 */
	public String getSyncSource() { return syncSource; }

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXGroup={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "description={" + description + "} ";
		str += "groupType={" + groupType + "} ";
		str += "credStoreId={" + credStoreId + "} ";
		str += "isVisible={" + isVisible + "} ";
		str += "groupSrc={" + groupSource + "} ";
		str += "otherAttributes={" + otherAttributes + "} ";
		str += "syncSource={" + syncSource + "} ";
		str += "}";
		return str;
	}
}
