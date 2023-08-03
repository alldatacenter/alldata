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
 * List wrapper class for VXPolicy
 */

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
public class VXPolicyList extends VList {
	private static final long serialVersionUID = 1L;
	List<VXPolicy> vXPolicies = new ArrayList<VXPolicy>();

	public VXPolicyList() {
		super();
	}

	public VXPolicyList(List<VXPolicy> objList) {
		super(objList);
		this.vXPolicies = objList;
	}

	/**
	 * @return the vXPolicies
	 */
	public List<VXPolicy> getVXPolicies() {
		return vXPolicies;
	}

	/**
	 * @param vXPolicies
	 *            the vXPolicies to set
	 */
	public void setVXPolicies(List<VXPolicy> vXPolicies) {
		this.vXPolicies = vXPolicies;
	}

	@Override
	public int getListSize() {
		if (vXPolicies != null) {
			return vXPolicies.size();
		}
		return 0;
	}

	@Override
	public List<VXPolicy> getList() {
		return vXPolicies;
	}

}
