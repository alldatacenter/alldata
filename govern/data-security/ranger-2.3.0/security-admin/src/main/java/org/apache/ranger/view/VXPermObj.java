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
 * Permission map
 */

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class VXPermObj implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * List of userName
	 */
	protected List<String> userList;
	/**
	 * List of groupName
	 */
	protected List<String> groupList;
	/**
	 * List of permission
	 */
	protected List<String> permList;
	/**
	 * IP address for groups
	 */
	protected String ipAddress;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXPermObj() {

	}

	/**
	 * @return the userList
	 */
	public List<String> getUserList() {
		return userList;
	}

	/**
	 * @param userList
	 *            the userList to set
	 */
	public void setUserList(List<String> userList) {
		this.userList = userList;
	}

	/**
	 * @return the groupList
	 */
	public List<String> getGroupList() {
		return groupList;
	}

	/**
	 * @param groupList
	 *            the groupList to set
	 */
	public void setGroupList(List<String> groupList) {
		this.groupList = groupList;
	}

	/**
	 * @return the permList
	 */
	public List<String> getPermList() {
		return permList;
	}

	/**
	 * @param permList
	 *            the permList to set
	 */
	public void setPermList(List<String> permList) {
		this.permList = permList;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress
	 *            the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * This return the bean content in string format
	 *
	 * @return formatedStr
	 */
	public String toString() {
		String str = "VXPermMap={";
		str += super.toString();
		str += "userList={" + userList + "} ";
		str += "groupList={" + groupList + "} ";
		str += "permList={" + permList + "} ";
		str += "ipAddress={" + ipAddress + "} ";
		str += "}";
		return str;
	}
}
