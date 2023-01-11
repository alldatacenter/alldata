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

 package org.apache.ranger.ugsyncutil.model;

import java.util.HashMap;
import java.util.Map;

public class XGroupInfo {
	
	private String id;
	private String name;
	private String description;
	private String groupType;
	private String isVisible;
	private String groupSource;
	private String otherAttributes;
	private String syncSource;
	private Map<String, String> otherAttrsMap = new HashMap<>();
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getGroupType() {
		return groupType;
	}
	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}

	public String getIsVisible() {
		return isVisible;
	}

	public void setIsVisible(String isVisible) {
		this.isVisible = isVisible;
	}

	public String getGroupSource() {
		return groupSource;
	}
	public void setGroupSource(String groupSource) {
		this.groupSource = groupSource;
	}

	public Map<String, String> getOtherAttrsMap() {
		return otherAttrsMap;
	}

	public void setOtherAttrsMap(Map<String, String> otherAttrsMap) {
		if (otherAttrsMap != null) {
			this.otherAttrsMap = otherAttrsMap;
		}
	}

	public String getOtherAttributes() {
		return otherAttributes;
	}

	public void setOtherAttributes(String otherAttributes) {
		this.otherAttributes = otherAttributes;
	}

	public String getSyncSource() { return syncSource; }

	public void setSyncSource(String syncSource) { this.syncSource = syncSource; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		XGroupInfo groupInfo = (XGroupInfo) o;
		if (name == null) {
			if (groupInfo.name != null)
				return false;
		} else if (!name.equals(groupInfo.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
}
