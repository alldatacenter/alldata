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
 * Base object class
 *
 */

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.view.ViewBaseBean;
import org.apache.ranger.json.JsonDateSerializer;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXDataObject extends ViewBaseBean implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Id of the data
	 */
	protected Long id;
	/**
	 * Date when this data was created
	 */
	@JsonSerialize(using=JsonDateSerializer.class)
	protected Date createDate;
	/**
	 * Date when this data was updated
	 */
	@JsonSerialize(using=JsonDateSerializer.class)
	protected Date updateDate;
	/**
	 * Owner
	 */
	protected String owner;
	/**
	 * Updated By
	 */
	protected String updatedBy;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXDataObject ( ) {
	}

	/**
	 * This method sets the value to the member attribute <b>id</b>.
	 * You cannot set null to the attribute.
	 * @param id Value to set member attribute <b>id</b>
	 */
	public void setId( Long id ) {
		this.id = id;
	}

	/**
	 * Returns the value for the member attribute <b>id</b>
	 * @return Long - value of member attribute <b>id</b>.
	 */
	public Long getId( ) {
		return this.id;
	}

	/**
	 * This method sets the value to the member attribute <b>createDate</b>.
	 * You cannot set null to the attribute.
	 * @param createDate Value to set member attribute <b>createDate</b>
	 */
	public void setCreateDate( Date createDate ) {
		this.createDate = createDate;
	}

	/**
	 * Returns the value for the member attribute <b>createDate</b>
	 * @return Date - value of member attribute <b>createDate</b>.
	 */
	public Date getCreateDate( ) {
		return this.createDate;
	}

	/**
	 * This method sets the value to the member attribute <b>updateDate</b>.
	 * You cannot set null to the attribute.
	 * @param updateDate Value to set member attribute <b>updateDate</b>
	 */
	public void setUpdateDate( Date updateDate ) {
		this.updateDate = updateDate;
	}

	/**
	 * Returns the value for the member attribute <b>updateDate</b>
	 * @return Date - value of member attribute <b>updateDate</b>.
	 */
	public Date getUpdateDate( ) {
		return this.updateDate;
	}

	/**
	 * This method sets the value to the member attribute <b>owner</b>.
	 * You cannot set null to the attribute.
	 * @param owner Value to set member attribute <b>owner</b>
	 */
	public void setOwner( String owner ) {
		this.owner = owner;
	}

	/**
	 * Returns the value for the member attribute <b>owner</b>
	 * @return String - value of member attribute <b>owner</b>.
	 */
	public String getOwner( ) {
		return this.owner;
	}

	/**
	 * This method sets the value to the member attribute <b>updatedBy</b>.
	 * You cannot set null to the attribute.
	 * @param updatedBy Value to set member attribute <b>updatedBy</b>
	 */
	public void setUpdatedBy( String updatedBy ) {
		this.updatedBy = updatedBy;
	}

	/**
	 * Returns the value for the member attribute <b>updatedBy</b>
	 * @return String - value of member attribute <b>updatedBy</b>.
	 */
	public String getUpdatedBy( ) {
		return this.updatedBy;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_DATA_OBJECT;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXDataObject={";
		str += super.toString();
		str += "id={" + id + "} ";
		str += "createDate={" + createDate + "} ";
		str += "updateDate={" + updateDate + "} ";
		str += "owner={" + owner + "} ";
		str += "updatedBy={" + updatedBy + "} ";
		str += "}";
		return str;
	}
}
