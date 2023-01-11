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

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXModuleDef;
import org.apache.ranger.view.VXModuleDef;
import org.apache.ranger.view.VXModuleDefList;
import java.util.Map;
import java.util.HashMap;
public abstract class XModuleDefServiceBase<T extends XXModuleDef, V extends VXModuleDef>
		extends AbstractBaseResourceService<T, V> {

	public static final String NAME = "XModuleDef";

	public XModuleDefServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj,
			int OPERATION_CONTEXT) {
		mObj.setModule(vObj.getModule());
		mObj.setUrl(vObj.getUrl());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setModule(mObj.getModule());
		vObj.setUrl(mObj.getUrl());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXModuleDefList searchModuleDef(SearchCriteria searchCriteria) {
		VXModuleDefList returnList = new VXModuleDefList();
		List<VXModuleDef> vXModuleDefList = new ArrayList<VXModuleDef>();
                searchCriteria.setMaxRows(Integer.MAX_VALUE);
		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);
                Map<Long, T> matchModule = new HashMap<Long,T>();
                for (T moduleDef : resultList) {
                        matchModule.put(moduleDef.getId(),  moduleDef);
                }

                List <T> moduleDefList=new ArrayList<T>(matchModule.values());
		// Iterate over the result list and create the return list
                for (T gjXModuleDef : moduleDefList) {
			VXModuleDef vXModuleDef = populateViewBean(gjXModuleDef);
			vXModuleDefList.add(vXModuleDef);
		}
                returnList.setTotalCount(vXModuleDefList.size());
		returnList.setvXModuleDef(vXModuleDefList);
		return returnList;
	}
}
