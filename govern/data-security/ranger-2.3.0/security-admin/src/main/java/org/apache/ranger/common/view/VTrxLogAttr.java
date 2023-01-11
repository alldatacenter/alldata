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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;

@XmlRootElement
public class VTrxLogAttr extends ViewBaseBean implements Serializable{
	private static final long serialVersionUID = 1L;
	
	protected String attribName;
	protected String attribUserFriendlyName;
	protected boolean isEnum;
	
	public VTrxLogAttr(){}

	public VTrxLogAttr(String attribName, String attribUserFriendlyName,
			boolean isEnum) {
		super();
		this.attribName = attribName;
		this.attribUserFriendlyName = attribUserFriendlyName;
		this.isEnum = isEnum;
	}

	


	/**
	 * @return the attribUserFriendlyName
	 */
	public String getAttribUserFriendlyName() {
		return attribUserFriendlyName;
	}


	/**
	 * @return the isEnum
	 */
	public boolean isEnum() {
		return isEnum;
	}

	
	
	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE;
	}

	@Override
	public String toString(){
		String str = "VTrxLogAttr={";
		str += super.toString();
		str += "attribName={" + attribName + "} ";
		str += "attribUserFriendlyName={" + attribUserFriendlyName + "} ";
		str += "isEnum={" + isEnum + "} ";
		str += "}";
		return str;
	}
}
