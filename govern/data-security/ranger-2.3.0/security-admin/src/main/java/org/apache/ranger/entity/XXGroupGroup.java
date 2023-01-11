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
 * Group of groups
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


@Entity
@Table(name="x_group_groups")
@XmlRootElement
public class XXGroupGroup extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_GROUP_GROUPS_SEQ",sequenceName="X_GROUP_GROUPS_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_GROUP_GROUPS_SEQ")
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
	 * Id of the parent group
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="P_GROUP_ID"   )
	protected Long parentGroupId;


	/**
	 * Id of the group
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="GROUP_ID"   )
	protected Long groupId;


	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXGroupGroup ( ) {
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_GROUP_GROUP;
	}

	@Override
	public String getMyDisplayValue() {
		return getName( );
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
	 * This method sets the value to the member attribute <b>parentGroupId</b>.
	 * You cannot set null to the attribute.
	 * @param parentGroupId Value to set member attribute <b>parentGroupId</b>
	 */
	public void setParentGroupId( Long parentGroupId ) {
		this.parentGroupId = parentGroupId;
	}

	/**
	 * Returns the value for the member attribute <b>parentGroupId</b>
	 * @return Long - value of member attribute <b>parentGroupId</b>.
	 */
	public Long getParentGroupId( ) {
		return this.parentGroupId;
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
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXGroupGroup={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "parentGroupId={" + parentGroupId + "} ";
		str += "groupId={" + groupId + "} ";
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
		XXGroupGroup other = (XXGroupGroup) obj;
        	if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            		return false;
        	}
        	if ((this.parentGroupId == null && other.parentGroupId != null) || (this.parentGroupId != null && !this.parentGroupId.equals(other.parentGroupId))) {
            		return false;
        	}
        	if ((this.groupId == null && other.groupId != null) || (this.groupId != null && !this.groupId.equals(other.groupId))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
