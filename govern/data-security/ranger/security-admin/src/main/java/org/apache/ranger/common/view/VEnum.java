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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.RangerCommonEnums;


@XmlRootElement
public class VEnum extends ViewBaseBean implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Name of the enum
	 */
	protected String enumName;
	/**
	 * List of elements for this enum
	 */
	protected List<VEnumElement> elementList;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VEnum() {
	}

	/**
	 * This method sets the value to the member attribute <b>enumName</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param enumName
	 *            Value to set member attribute <b>enumName</b>
	 */
	public void setEnumName(String enumName) {
		this.enumName = enumName;
	}

	/**
	 * Returns the value for the member attribute <b>enumName</b>
	 *
	 * @return String - value of member attribute <b>enumName</b>.
	 */
	public String getEnumName() {
		return this.enumName;
	}

	/**
	 * This method sets the value to the member attribute <b>elementList</b>.
	 * You cannot set null to the attribute.
	 *
	 * @param elementList
	 *            Value to set member attribute <b>elementList</b>
	 */
	public void setElementList(List<VEnumElement> elementList) {
		this.elementList = elementList;
	}

	/**
	 * Returns the value for the member attribute <b>elementList</b>
	 *
	 * @return List<VEnumElement> - value of member attribute
	 *         <b>elementList</b>.
	 */
	public List<VEnumElement> getElementList() {
		return this.elementList;
	}

	@Override
	public int getMyClassType() {
		return RangerCommonEnums.CLASS_TYPE_ENUM;
	}

	/**
	 * This return the bean content in string format
	 *
	 * @return formatedStr
	 */
	public String toString() {
		String str = "VEnum={";
		str += super.toString();
		str += "enumName={" + enumName + "} ";
		str += "elementList={" + elementList + "} ";
		str += "}";
		return str;
	}
}
