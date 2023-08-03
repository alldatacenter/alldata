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
 * Credential Store
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
@Table(name="x_cred_store")
@XmlRootElement
public class XXCredentialStore extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_CRED_STORE_SEQ",sequenceName="X_CRED_STORE_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_CRED_STORE_SEQ")
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
	@Column(name="STORE_NAME"  , nullable=false , length=1024)
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
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXCredentialStore ( ) {
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_CRED_STORE;
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
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXCredentialStore={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "description={" + description + "} ";
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
		XXCredentialStore other = (XXCredentialStore) obj;
        	if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            		return false;
        	}
        	if ((this.description == null && other.description != null) || (this.description != null && !this.description.equals(other.description))) {
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
