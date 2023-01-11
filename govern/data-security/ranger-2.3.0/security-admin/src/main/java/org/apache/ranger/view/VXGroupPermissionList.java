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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.view.VList;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class VXGroupPermissionList extends VList {

	private static final long serialVersionUID = 1L;

	List<VXGroupPermission> vXGroupPermission = new ArrayList<VXGroupPermission>();

	public VXGroupPermissionList() {
		super();
	}

	public VXGroupPermissionList(List<VXGroupPermission> objList) {
		super(objList);
		this.vXGroupPermission = objList;
	}

	/**
	 * @return the vXGroupPermission
	 */
	public List<VXGroupPermission> getvXGroupPermission() {
		return vXGroupPermission;
	}

	/**
	 * @param vXGroupPermission the vXGroupPermission to set
	 */
	public void setvXGroupPermission(List<VXGroupPermission> vXGroupPermission) {
		this.vXGroupPermission = vXGroupPermission;
	}

	@Override
	public int getListSize() {
		if (vXGroupPermission != null) {
			return vXGroupPermission.size();
		}
		return 0;
	}

	@Override
	public List<VXGroupPermission> getList() {
		return vXGroupPermission;
	}
}
