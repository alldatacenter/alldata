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
 * Message class
 *
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.view.ViewBaseBean;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXMessage extends ViewBaseBean implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Message key
	 */
	protected String name;
	/**
	 * Resource bundle key
	 */
	protected String rbKey;
	/**
	 * Message description. Use rbKey for doing localized lookup
	 */
	protected String message;
	/**
	 * Id of the object to which this message is related to
	 */
	protected Long objectId;
	/**
	 * Name of the field or attribute to which this message is related to
	 */
	protected String fieldName;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXMessage ( ) {
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
	 * This method sets the value to the member attribute <b>rbKey</b>.
	 * You cannot set null to the attribute.
	 * @param rbKey Value to set member attribute <b>rbKey</b>
	 */
	public void setRbKey( String rbKey ) {
		this.rbKey = rbKey;
	}

	/**
	 * Returns the value for the member attribute <b>rbKey</b>
	 * @return String - value of member attribute <b>rbKey</b>.
	 */
	public String getRbKey( ) {
		return this.rbKey;
	}

	/**
	 * This method sets the value to the member attribute <b>message</b>.
	 * You cannot set null to the attribute.
	 * @param message Value to set member attribute <b>message</b>
	 */
	public void setMessage( String message ) {
		this.message = message;
	}

	/**
	 * Returns the value for the member attribute <b>message</b>
	 * @return String - value of member attribute <b>message</b>.
	 */
	public String getMessage( ) {
		return this.message;
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
	 * This method sets the value to the member attribute <b>fieldName</b>.
	 * You cannot set null to the attribute.
	 * @param fieldName Value to set member attribute <b>fieldName</b>
	 */
	public void setFieldName( String fieldName ) {
		this.fieldName = fieldName;
	}

	/**
	 * Returns the value for the member attribute <b>fieldName</b>
	 * @return String - value of member attribute <b>fieldName</b>.
	 */
	public String getFieldName( ) {
		return this.fieldName;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_MESSAGE;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXMessage={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "rbKey={" + rbKey + "} ";
		str += "message={" + message + "} ";
		str += "objectId={" + objectId + "} ";
		str += "fieldName={" + fieldName + "} ";
		str += "}";
		return str;
	}
}
