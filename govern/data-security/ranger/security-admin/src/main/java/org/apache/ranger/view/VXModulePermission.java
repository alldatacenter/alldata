/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement

public class VXModulePermission extends VXDataObject implements Serializable {

	private static final long serialVersionUID = 1L;
	protected String module;
	protected List<String> userNameList = new ArrayList<String>();
	protected List<String> groupNameList = new ArrayList<String>();

	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	public List<String> getUserNameList() {
		return userNameList;
	}
	public void setUserNameList(List<String> userNameList) {
		this.userNameList = userNameList;
	}
	public List<String> getGroupNameList() {
		return groupNameList;
	}
	public void setGroupNameList(List<String> groupNameList) {
		this.groupNameList = groupNameList;
	}

	@Override
	public String toString() {
		String str = "VXModulePermission={";
		str += super.toString();
		str += "id={" + id + "} ";
		str += "module={" + module + " } ";
		str += "userNameList={" + userNameList + " } ";
		str += "groupNameList={" + groupNameList + " } ";
		str += "}";
		return str;
	}
}
