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
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.view.VXResource;
import org.apache.ranger.view.VXResourceList;

public abstract class XResourceServiceBase<T extends XXResource, V extends VXResource>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XResource";

	public XResourceServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setName( vObj.getName());
		mObj.setDescription( vObj.getDescription());
		mObj.setResourceType( vObj.getResourceType());
		mObj.setAssetId( vObj.getAssetId());
		mObj.setParentId( vObj.getParentId());
		mObj.setParentPath( vObj.getParentPath());
		mObj.setIsEncrypt( vObj.getIsEncrypt());
		mObj.setIsRecursive( vObj.getIsRecursive());
		mObj.setResourceGroup( vObj.getResourceGroup());
		mObj.setDatabases( vObj.getDatabases());
		mObj.setTables( vObj.getTables());
		mObj.setColumnFamilies( vObj.getColumnFamilies());
		mObj.setColumns( vObj.getColumns());
		mObj.setUdfs( vObj.getUdfs());
		mObj.setResourceStatus( vObj.getResourceStatus());
		mObj.setTableType( vObj.getTableType());
		mObj.setColumnType( vObj.getColumnType());
		mObj.setPolicyName( vObj.getPolicyName());
		mObj.setTopologies( vObj.getTopologies());
		mObj.setServices( vObj.getServices());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setName( mObj.getName());
		vObj.setDescription( mObj.getDescription());
		vObj.setResourceType( mObj.getResourceType());
		vObj.setAssetId( mObj.getAssetId());
		vObj.setParentId( mObj.getParentId());
		vObj.setParentPath( mObj.getParentPath());
		vObj.setIsEncrypt( mObj.getIsEncrypt());
		vObj.setIsRecursive( mObj.getIsRecursive());
		vObj.setResourceGroup( mObj.getResourceGroup());
		vObj.setDatabases( mObj.getDatabases());
		vObj.setTables( mObj.getTables());
		vObj.setColumnFamilies( mObj.getColumnFamilies());
		vObj.setColumns( mObj.getColumns());
		vObj.setUdfs( mObj.getUdfs());
		vObj.setResourceStatus( mObj.getResourceStatus());
		vObj.setTableType( mObj.getTableType());
		vObj.setColumnType( mObj.getColumnType());
		vObj.setPolicyName( mObj.getPolicyName());
		vObj.setTopologies( mObj.getTopologies());
		vObj.setServices( mObj.getServices());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXResourceList searchXResources(SearchCriteria searchCriteria) {
		VXResourceList returnList = new VXResourceList();
		List<VXResource> xResourceList = new ArrayList<VXResource>();

		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXResource : resultList) {
			VXResource vXResource = populateViewBean(gjXResource);
			xResourceList.add(vXResource);
		}

		returnList.setVXResources(xResourceList);
		return returnList;
	}

}
