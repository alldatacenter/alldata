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

 package org.apache.ranger.common.view;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.RangerCommonEnums;

@XmlRootElement
public class VEnumElement extends ViewBaseBean implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Name of the element
	 */
	protected String elementName;
	/**
	 * Name of the enum
	 */
	protected String enumName;
	/**
	 * Value of the element
	 */
	protected int elementValue;
	/**
	 * Label for the element
	 */
	protected String elementLabel;
	/**
	 * Resource bundle key
	 */
	protected String rbKey;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VEnumElement ( ) {
	}

	/**
	 * This method sets the value to the member attribute <b>elementName</b>.
	 * You cannot set null to the attribute.
	 * @param elementName Value to set member attribute <b>elementName</b>
	 */
	public void setElementName( String elementName ) {
		this.elementName = elementName;
	}

	

	/**
	 * @return the elementName
	 */
	public String getElementName() {
		return elementName;
	}

	/**
	 * @return the enumName
	 */
	public String getEnumName() {
		return enumName;
	}

	/**
	 * @return the rbKey
	 */
	public String getRbKey() {
		return rbKey;
	}

	/**
	 * This method sets the value to the member attribute <b>enumName</b>.
	 * You cannot set null to the attribute.
	 * @param enumName Value to set member attribute <b>enumName</b>
	 */
	public void setEnumName( String enumName ) {
		this.enumName = enumName;
	}

	

	/**
	 * This method sets the value to the member attribute <b>elementValue</b>.
	 * You cannot set null to the attribute.
	 * @param elementValue Value to set member attribute <b>elementValue</b>
	 */
	public void setElementValue( int elementValue ) {
		this.elementValue = elementValue;
	}

	/**
	 * Returns the value for the member attribute <b>elementValue</b>
	 * @return int - value of member attribute <b>elementValue</b>.
	 */
	public int getElementValue( ) {
		return this.elementValue;
	}

	/**
	 * This method sets the value to the member attribute <b>elementLabel</b>.
	 * You cannot set null to the attribute.
	 * @param elementLabel Value to set member attribute <b>elementLabel</b>
	 */
	public void setElementLabel( String elementLabel ) {
		this.elementLabel = elementLabel;
	}

	/**
	 * Returns the value for the member attribute <b>elementLabel</b>
	 * @return String - value of member attribute <b>elementLabel</b>.
	 */
	public String getElementLabel( ) {
		return this.elementLabel;
	}

	/**
	 * This method sets the value to the member attribute <b>rbKey</b>.
	 * You cannot set null to the attribute.
	 * @param rbKey Value to set member attribute <b>rbKey</b>
	 */
	public void setRbKey( String rbKey ) {
		this.rbKey = rbKey;
	}

	

	@Override
	public int getMyClassType( ) {
	    return RangerCommonEnums.CLASS_TYPE_ENUM_ELEMENT;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VEnumElement={";
		str += super.toString();
		str += "elementName={" + elementName + "} ";
		str += "enumName={" + enumName + "} ";
		str += "elementValue={" + elementValue + "} ";
		str += "elementLabel={" + elementLabel + "} ";
		str += "rbKey={" + rbKey + "} ";
		str += "}";
		return str;
	}
}
