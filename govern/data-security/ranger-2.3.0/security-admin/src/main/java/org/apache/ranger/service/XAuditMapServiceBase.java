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

 package org.apache.ranger.service;

/**
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXAuditMap;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXAuditMapList;

public abstract class XAuditMapServiceBase<T extends XXAuditMap, V extends VXAuditMap>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XAuditMap";

	public XAuditMapServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setResourceId( vObj.getResourceId());
		mObj.setGroupId( vObj.getGroupId());
		mObj.setUserId( vObj.getUserId());
		mObj.setAuditType( vObj.getAuditType());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setResourceId( mObj.getResourceId());
		vObj.setGroupId( mObj.getGroupId());
		vObj.setUserId( mObj.getUserId());
		vObj.setAuditType( mObj.getAuditType());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXAuditMapList searchXAuditMaps(SearchCriteria searchCriteria) {
		VXAuditMapList returnList = new VXAuditMapList();
		List<VXAuditMap> xAuditMapList = new ArrayList<VXAuditMap>();

		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXAuditMap : resultList) {
			VXAuditMap vXAuditMap = populateViewBean(gjXAuditMap);
			xAuditMapList.add(vXAuditMap);
		}

		returnList.setVXAuditMaps(xAuditMapList);
		return returnList;
	}

}
