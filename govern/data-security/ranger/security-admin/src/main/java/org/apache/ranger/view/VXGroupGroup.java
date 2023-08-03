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
 * Group of groups
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
public class VXGroupGroup extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Name
	 */
	protected String name;
	/**
	 * Id of the parent group
	 */
	protected Long parentGroupId;
	/**
	 * Id of the group
	 */
	protected Long groupId;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXGroupGroup ( ) {
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

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_GROUP_GROUP;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXGroupGroup={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "parentGroupId={" + parentGroupId + "} ";
		str += "groupId={" + groupId + "} ";
		str += "}";
		return str;
	}
}
