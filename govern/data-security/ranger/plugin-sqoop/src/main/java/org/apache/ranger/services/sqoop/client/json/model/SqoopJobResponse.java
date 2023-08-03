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

package org.apache.ranger.services.sqoop.client.json.model;

import com.google.gson.annotations.SerializedName;

public class SqoopJobResponse {
	private Long id;

	private String name;

	@SerializedName("from-link-name")
	private String fromLinkName;

	@SerializedName("to-link-name")
	private String toLinkName;

	@SerializedName("from-connector-name")
	private String fromConnectorName;

	@SerializedName("to-connector-name")
	private String toConnectorName;

	@SerializedName("creation-user")
	private String creationUser;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFromLinkName() {
		return fromLinkName;
	}

	public void setFromLinkName(String fromLinkName) {
		this.fromLinkName = fromLinkName;
	}

	public String getToLinkName() {
		return toLinkName;
	}

	public void setToLinkName(String toLinkName) {
		this.toLinkName = toLinkName;
	}

	public String getFromConnectorName() {
		return fromConnectorName;
	}

	public void setFromConnectorName(String fromConnectorName) {
		this.fromConnectorName = fromConnectorName;
	}

	public String getToConnectorName() {
		return toConnectorName;
	}

	public void setToConnectorName(String toConnectorName) {
		this.toConnectorName = toConnectorName;
	}

	public String getCreationUser() {
		return creationUser;
	}

	public void setCreationUser(String creationUser) {
		this.creationUser = creationUser;
	}

	@Override
	public String toString() {
		return "SqoopJobResponse [id=" + id + ", name=" + name + ", fromLinkName=" + fromLinkName + ", toLinkName="
				+ toLinkName + ", fromConnectorName=" + fromConnectorName + ", toConnectorName=" + toConnectorName
				+ ", creationUser=" + creationUser + "]";
	}
}
